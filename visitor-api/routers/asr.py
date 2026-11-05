"""
语音转文字（ASR）路由模块 —— Vosk 真正流式版

接口列表：
  WS   /asr/stream       Vosk 真正流式语音转文字（逐帧解码，< 500ms 延迟）
  GET  /asr/status      查询 Vosk 模型加载状态
  POST /asr/preload     预加载模型（可选，提速首次识别）
  GET  /asr/models      列出本地已下载的可用模型

协议说明（/asr/stream）：
  ── 客户端 → 服务端 ─────────────────────────────────────
  1. 连接后先发送一条 JSON 配置帧：
       {"type": "config", "language": "zh", "format": "wav", "sampleRate": 16000}
     language:   "zh"|"en"|"ja"|"ko"|"auto"（默认 "zh"）
     format:     音频格式，如 "wav"/"mp3"/"m4a"（默认 "wav"）
     sampleRate: 采样率，默认 16000（推荐 16000 或 8000）

  2. 逐块发送 base64 编码的音频数据（建议每块 4096 字节 ≈ 128ms@16kHz）：
       {"type": "audio", "data": "<base64>"}

  3. 录音结束后发送结束帧：
       {"type": "end"}

  ── 服务端 → 客户端 ─────────────────────────────────────
  • 每收到一块音频就可能推送（延迟 < 500ms）：
       {"type": "partial", "text": "你好今天", "final": false, "seq": 1}
  • 当前语音段结束时推送（Vosk 自动检测句末）：
       {"type": "partial", "text": "你好今天天气", "final": true, "seq": 2}
       注意：当 final=true 时，该段文字已稳定，客户端可追加到最终文本
  • 收到 end 帧后推送会话完整结果（拼接所有已识别片段）：
       {"type": "final", "text": "你好今天天气真不错。", "seq": 3}
  • 出错时推送：
       {"type": "error", "msg": "..."}

  Vosk 的真正流式特性：
    - 不需要等待整段音频，每 10~20ms 音频帧即可解码输出
    - AcceptWaveform() 每调用一次都可能产生 partial 结果
    - 比 Whisper 滑动窗口方案的延迟低一个数量级（< 500ms vs 2~3s）
"""
import asyncio
import base64
import json
import logging
import os
import time
import struct

from fastapi import APIRouter, WebSocket, WebSocketDisconnect, BackgroundTasks
from fastapi.responses import JSONResponse

from utils.asr import (
    VoskStreamingSession,
    _decode_audio_to_pcm,
    get_model_info,
    is_model_ready,
    get_vosk_model,
    MODEL_DIR,
    LANG_MODEL_MAP,
)
from schemas.asr import ASRStatusResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/asr", tags=["语音转文字 ASR（Vosk 流式）"])

# 每次喂入 Vosk 的 PCM 块大小（字节），决定 partial 推送频率
# 4096 bytes = 2048 个 int16 采样点 ≈ 128ms @ 16kHz
VOSK_CHUNK_BYTES = 4096


# ─────────────────────────────────────────────────────────────────
# WS /asr/stream  ——  Vosk 真正流式语音转文字
# ─────────────────────────────────────────────────────────────────

@router.websocket("/stream")
async def asr_stream(websocket: WebSocket):
    """
    WebSocket 真正流式 ASR 接口（Vosk 引擎）

    延迟特征：
      - 每收到 ~128ms 音频即可解码输出（Vosk 逐帧解码）
      - 端到端延迟 < 500ms（CPU 环境）
      - 相比 Whisper 滑动窗口方案（2~3s），延迟降低一个数量级
    """
    await websocket.accept()
    peer = websocket.client.host if hasattr(websocket, "client") else "unknown"
    logger.info(f"[ASR-WS] 客户端连接：{peer}")

    # ── 1. 读取配置帧 ────────────────────────────────────
    language = "zh"
    audio_format = "wav"
    sample_rate = 16000

    try:
        raw = await asyncio.wait_for(websocket.receive_text(), timeout=10.0)
        msg = json.loads(raw)
        if msg.get("type") == "config":
            language = msg.get("language", "zh")
            audio_format = msg.get("format", "wav")
            sample_rate = int(msg.get("sampleRate", 16000))
            logger.info(
                f"[ASR-WS] 配置：language={language}, "
                f"format={audio_format}, sampleRate={sample_rate}"
            )
        else:
            # 第一帧就是音频，缓存起来稍后处理
            first_msg = msg
    except asyncio.TimeoutError:
        await websocket.send_json({"type": "error", "msg": "等待配置帧超时（10s）"})
        await websocket.close()
        return
    except Exception as e:
        await websocket.send_json({"type": "error", "msg": f"配置帧解析失败：{e}"})
        await websocket.close()
        return

    # ── 2. 加载 Vosk 模型（同步加载，首次需 1~3s）────────
    try:
        loop = asyncio.get_event_loop()
        model, _ = await loop.run_in_executor(None, lambda: get_vosk_model(language))
        logger.info(f"[ASR-WS] 模型加载完成：{language}")
    except FileNotFoundError as e:
        await websocket.send_json({"type": "error", "msg": str(e)})
        await websocket.close()
        return
    except RuntimeError as e:
        await websocket.send_json({"type": "error", "msg": str(e)})
        await websocket.close()
        return

    # ── 3. 创建 Vosk 流式会话 ───────────────────────────
    session = VoskStreamingSession(lang=language)
    seq = 0
    pending_partial = ""   # 上一次 partial 文字，用于去重

    async def _push_partial(text: str, is_final: bool = False):
        """推送 partial/final 识别结果给客户端"""
        nonlocal seq
        seq += 1
        await websocket.send_json({
            "type": "partial",
            "text": text,
            "final": is_final,
            "seq": seq,
        })
        logger.debug(f"[ASR-WS] push {'final' if is_final else 'partial'} #{seq}: '{text[:40]}'")

    async def _process_pcm_chunk(pcm_bytes: bytes):
        """
        将一段 int16 PCM 字节分块喂入 Vosk，实时推送 partial 结果。
        Vosk 每收到一块就可能输出新结果，真正实现低延迟流式。
        """
        nonlocal pending_partial
        offset = 0
        while offset < len(pcm_bytes):
            chunk = pcm_bytes[offset:offset + VOSK_CHUNK_BYTES]
            offset += len(chunk)

            # 喂给 Vosk（同步调用，在线程池执行）
            loop = asyncio.get_event_loop()
            result = await loop.run_in_executor(None, session.feed_pcm, chunk)

            if result is None:
                continue

            text = result.get("text", "").strip()
            if not text or text == pending_partial:
                continue

            # result["type"] == "final"  表示该语音段已结束
            # result["type"] == "partial" 表示中间结果更新
            is_final = result["type"] == "final"
            pending_partial = text
            await _push_partial(text, is_final=is_final)

    # ── 4. 处理「第一帧就是音频」的特殊情况 ─────────────
    if "first_msg" in dir():
        pass  # 下面统一进主循环处理

    # ── 5. 主接收循环 ─────────────────────────────────────
    raw_pcm_buffer = b""   # 累积未解码的原始音频字节
    last_feed_time = time.time()

    try:
        while True:
            try:
                # 等待客户端消息（30s 超时）
                ws_result = await asyncio.wait_for(
                    websocket.receive(), timeout=30.0
                )
            except asyncio.TimeoutError:
                # 30s 无消息，发送心跳或断开
                try:
                    await websocket.send_json({"type": "ping"})
                except Exception:
                    break
                continue

            # 判断消息类型
            if "text" in ws_result:
                text_data = ws_result["text"]
                msg = json.loads(text_data)
                msg_type = msg.get("type")

                if msg_type == "audio":
                    b64_data = msg.get("data", "")
                    if not b64_data:
                        continue
                    try:
                        chunk_bytes = base64.b64decode(b64_data)
                    except Exception:
                        await websocket.send_json({"type": "error", "msg": "base64 解码失败"})
                        continue

                    raw_pcm_buffer += chunk_bytes

                    # 积累到足够数据（> 4096 字节）就解码并喂入 Vosk
                    # 这样既能处理 base64 分块，又能降低 ffmpeg 调用频率
                    if len(raw_pcm_buffer) >= VOSK_CHUNK_BYTES:
                        # 需要解码（非 WAV/PCM 原始格式时）
                        if audio_format.lower() != "pcm":
                            try:
                                loop = asyncio.get_event_loop()
                                pcm_bytes = await loop.run_in_executor(
                                    None,
                                    lambda: _decode_audio_to_pcm(
                                        raw_pcm_buffer, file_ext=f".{audio_format}"
                                    )
                                )
                                raw_pcm_buffer = b""  # 清空原始缓冲
                                await _process_pcm_chunk(pcm_bytes)
                            except Exception as e:
                                logger.warning(f"[ASR-WS] 音频解码失败（跳过）：{e}")
                                raw_pcm_buffer = b""  # 丢弃损坏数据
                        else:
                            # 已经是 PCM 原始字节，直接喂入
                            pcm_to_feed = raw_pcm_buffer
                            raw_pcm_buffer = b""
                            await _process_pcm_chunk(pcm_to_feed)

                elif msg_type == "end":
                    logger.info("[ASR-WS] 收到 end 帧，最终识别中...")

                    # 处理剩余未解码的缓冲
                    if raw_pcm_buffer:
                        if audio_format.lower() != "pcm":
                            try:
                                loop = asyncio.get_event_loop()
                                pcm_bytes = await loop.run_in_executor(
                                    None,
                                    lambda: _decode_audio_to_pcm(
                                        raw_pcm_buffer, file_ext=f".{audio_format}"
                                    )
                                )
                                await _process_pcm_chunk(pcm_bytes)
                            except Exception as e:
                                logger.warning(f"[ASR-WS] 最终解码失败：{e}")
                        else:
                            await _process_pcm_chunk(raw_pcm_buffer)

                    # 最终拼接结果
                    loop = asyncio.get_event_loop()
                    full_text = await loop.run_in_executor(None, session.finalize)

                    seq += 1
                    await websocket.send_json({
                        "type": "final",
                        "text": full_text or "",
                        "seq": seq,
                    })
                    logger.info(f"[ASR-WS] 最终结果：'{full_text[:80]}'")
                    break

                else:
                    logger.warning(f"[ASR-WS] 未知消息类型：{msg_type}")

            elif "bytes" in ws_result:
                # 也支持直接发二进制 PCM 帧（更低延迟）
                binary_data = ws_result["bytes"]
                if binary_data:
                    loop = asyncio.get_event_loop()
                    await _process_pcm_chunk(binary_data)

    except (WebSocketDisconnect, RuntimeError):
        logger.info(f"[ASR-WS] 客户端断开：{peer}")
    except Exception as e:
        logger.error(f"[ASR-WS] 未知错误：{e}", exc_info=True)
        try:
            await websocket.send_json({"type": "error", "msg": f"服务内部错误：{e}"})
        except Exception:
            pass
    finally:
        try:
            await websocket.close()
        except Exception:
            pass
        logger.info(f"[ASR-WS] 连接关闭：{peer}")


# ─────────────────────────────────────────────────────────────────
# GET /asr/status  ——  Vosk 模型状态查询
# ─────────────────────────────────────────────────────────────────

@router.get(
    "/status",
    summary="查询 ASR 模型状态（Vosk）",
    response_model=ASRStatusResponse,
)
async def asr_status():
    """返回 Vosk 模型的当前加载状态"""
    info = get_model_info()
    return ASRStatusResponse(
        code=200,
        msg="ok",
        model_name="vosk",
        loaded=len(info["loaded_langs"]) > 0,
        backend=info["backend"],
        loaded_langs=info["loaded_langs"],
    )


# ─────────────────────────────────────────────────────────────────
# POST /asr/preload  ——  预加载模型
# ─────────────────────────────────────────────────────────────────

@router.post(
    "/preload",
    summary="预加载 Vosk 模型",
    description="触发 Vosk 模型的后台加载，避免首次识别时等待。建议在 APP 启动时调用一次。",
)
async def preload_model(background_tasks: BackgroundTasks, language: str = "zh"):
    """后台预加载指定语言的模型，立即返回"""
    if is_model_ready(language):
        return {"code": 200, "msg": f"语言 {language} 的模型已加载，无需重复操作"}

    def _load():
        try:
            get_vosk_model(language)
            logger.info(f"[ASR] 后台预加载完成：{language}")
        except Exception as e:
            logger.error(f"[ASR] 后台预加载失败：{e}")

    background_tasks.add_task(_load)
    return {"code": 200, "msg": f"语言 {language} 的模型预加载已在后台启动，请稍后通过 GET /asr/status 确认"}


# ─────────────────────────────────────────────────────────────────
# GET /asr/models  ——  列出本地可用模型
# ─────────────────────────────────────────────────────────────────

@router.get(
    "/models",
    summary="列出本地已下载的 Vosk 模型",
)
async def list_local_models():
    """扫描 MODEL_DIR 目录，列出所有已下载解压的模型"""
    import os
    if not os.path.isdir(MODEL_DIR):
        return {"code": 200, "msg": "模型目录不存在", "models": [], "model_dir": MODEL_DIR}

    models = []
    for name, subdir in LANG_MODEL_MAP.items():
        path = os.path.join(MODEL_DIR, subdir)
        if os.path.isdir(path):
            # 检查关键文件是否存在
            has_model = (
                os.path.exists(os.path.join(path, "am", "final.mdl"))
                or os.path.exists(os.path.join(path, "model"))  # 新版结构
                or os.path.exists(os.path.join(path, "conf", "model.conf"))
            )
            models.append({
                "lang": name,
                "dir_name": subdir,
                "path": path,
                "ready": has_model,
            })

    return {
        "code": 200,
        "msg": "ok",
        "model_dir": MODEL_DIR,
        "models": models,
    }
