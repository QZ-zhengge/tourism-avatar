"""
讯飞在线虚拟人驱动 — 转发路由
官方文档：https://www.xfyun.cn/doc/tts/virtual_human/API.html

【功能说明】
本模块作为 uniapp 与讯飞虚拟人服务之间的「中间层代理」：
  1. 保护 APPID / APIKey / APISecret 不暴露给客户端
  2. 服务端完成 HMAC-SHA256 鉴权签名
  3. 转发请求到讯飞虚拟人服务
  4. 返回统一格式的响应给 uniapp

【接口一览】
  POST /avatar/start              启动虚拟人会话
  POST /avatar/drive/text         文本驱动（TTS 播报）
  GET  /avatar/drive/audio/url    获取音频驱动 WSS 鉴权 URL（供 uniapp 直连）
  POST /avatar/drive/audio/proxy  音频帧代理转发（服务端中转模式）
  POST /avatar/stop               停止会话
  POST /avatar/ping               心跳保活
  GET  /avatar/session/{session}  查询会话状态（本地缓存）
  GET  /avatar/actions/{avatar_id} 获取指定形象的动作列表
  GET  /avatar/vcn/list           获取发音人列表
"""
import base64
import json
import logging
import asyncio
import httpx
import websockets
from datetime import datetime
from typing import Optional, Dict

from fastapi import APIRouter, HTTPException, Depends, Query

from config import get_settings, Settings
from database.database import get_db
from sqlalchemy.orm import Session
from models.digital_config import DigitalHumanConfig
from utils.xfyun_auth import build_auth_query_url, build_wss_auth_url
from schemas.avatar import (
    AvatarStartRequest, AvatarStartResponse, AvatarStartData,
    AvatarTextDriveRequest, AvatarTextDriveResponse,
    AvatarAudioUrlRequest, AvatarAudioUrlResponse, AvatarAudioUrlData,
    AvatarAudioFrameRequest, AvatarAudioFrameResponse,
    AvatarStopRequest, AvatarStopResponse,
    AvatarPingRequest, AvatarPingResponse,
    AvatarSessionResponse, AvatarSessionInfo,
    AvatarActionsResponse, AvatarActionInfo,
    AvatarVcnListResponse, VcnInfo,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/avatar", tags=["虚拟人模块（讯飞在线虚拟人驱动）"])

# ─────────────────────────────────────────────────────────────
# 讯飞接口路径常量
# ─────────────────────────────────────────────────────────────
_PATH_START = "/v1/private/vms2d_start"
_PATH_CTRL = "/v1/private/vms2d_ctrl"
_PATH_AUDIO_CTRL = "/v1/private/vms2d_audio_ctrl"
_PATH_STOP = "/v1/private/vms2d_stop"
_PATH_PING = "/v1/private/vms2d_ping"

# ─────────────────────────────────────────────────────────────
# 本地会话缓存（内存级，服务重启后清空；可替换为 Redis 实现持久化）
# key=session_id, value=AvatarSessionInfo dict
# ─────────────────────────────────────────────────────────────
_session_store: Dict[str, dict] = {}

# ─────────────────────────────────────────────────────────────
# 音频驱动代理用的 WSS 长连接缓存
# key=session_id, value=websockets.ClientConnection
# ─────────────────────────────────────────────────────────────
_audio_ws_store: Dict[str, object] = {}


def _check_config(settings: Settings) -> None:
    """检查讯飞配置是否已填写"""
    if not settings.XFYUN_APP_ID or not settings.XFYUN_API_KEY or not settings.XFYUN_API_SECRET:
        raise HTTPException(
            status_code=503,
            detail="讯飞虚拟人服务未配置，请在 config.py 或 .env 中设置 XFYUN_APP_ID / XFYUN_API_KEY / XFYUN_API_SECRET"
        )


async def _post_to_xfyun(
    settings: Settings,
    path: str,
    body: dict,
) -> dict:
    """
    向讯飞虚拟人 HTTPS 接口发送带鉴权的 POST 请求，返回响应 JSON。
    所有 HTTP 错误均转换为业务层错误。
    """
    url = build_auth_query_url(
        base_url=settings.XFYUN_VMS_BASE_URL,
        host=settings.XFYUN_VMS_HOST,
        path=path,
        api_key=settings.XFYUN_API_KEY,
        api_secret=settings.XFYUN_API_SECRET,
    )
    logger.debug(f"[xfyun] POST {path} body={json.dumps(body, ensure_ascii=False)[:200]}")
    async with httpx.AsyncClient(timeout=30.0) as client:
        try:
            resp = await client.post(url, json=body, headers={"Content-Type": "application/json"})
        except httpx.RequestError as e:
            raise HTTPException(status_code=502, detail=f"讯飞服务请求失败：{str(e)}")

    if resp.status_code == 401:
        raise HTTPException(status_code=401, detail="讯飞鉴权失败，请检查 APIKey/APISecret/时钟偏移")
    if resp.status_code == 403:
        raise HTTPException(status_code=403, detail="讯飞鉴权失败（时钟偏移>300秒）")

    # 讯飞对「业务错误」（如 session 无效、参数错误）可能返回 HTTP 400 或 500
    # 此时响应体仍是合法 JSON，包含 header.code 和 header.message
    # 应解析出来作为业务层错误返回给调用方，而不是抛 502
    if resp.status_code != 200:
        try:
            err_json = resp.json()
            xfyun_code = err_json.get("header", {}).get("code")
            xfyun_msg  = err_json.get("header", {}).get("message", "unknown error")
            # 有明确的讯飞 code → 业务错误，以结构化 dict 返回供上层处理
            if xfyun_code is not None:
                logger.warning(
                    f"[xfyun] 业务错误 HTTP {resp.status_code} "
                    f"code={xfyun_code} message={xfyun_msg}"
                )
                return err_json   # 上层通过 header.code != 0 判断失败
        except Exception:
            pass
        # 无法解析 JSON（网关错误、HTML 页面等）→ 抛 502
        raise HTTPException(
            status_code=502,
            detail=f"讯飞服务异常，HTTP {resp.status_code}: {resp.text[:200]}"
        )

    try:
        return resp.json()
    except Exception:
        raise HTTPException(status_code=502, detail=f"讯飞服务返回非 JSON 响应：{resp.text[:200]}")


# ─────────────────────────────────────────────────────────────
# 1. 启动虚拟人会话
# ─────────────────────────────────────────────────────────────

@router.post(
    "/start",
    response_model=AvatarStartResponse,
    summary="启动虚拟人会话（支持背景透明）",
    description="""
创建讯飞在线虚拟人会话，返回：
- `session`：后续所有接口必须携带的会话标识
- `stream_url`：拉流地址，uniapp 传给 `<live-player>` 或 xrtc 播放器播放

**请求参数说明：**
- `avatar_id` / `width` / `height` / `protocol`：原有参数，默认值不变
- `bg_transparent`：**新增可选参数**，默认 `false`（不开透明通道，完全向后兼容）
- `bg_custom`：**新增可选参数**，默认 `false`。设为 `true` 时后端从 .env 读取 `AVATAR_BG_RES_ID` 发送给讯飞，resKey 不暴露给前端。

**向后兼容：**
- uniapp 原有请求不带新增字段，行为与之前完全一致

对应讯飞接口：`POST /v1/private/vms2d_start`
""",
)
async def avatar_start(
    req: AvatarStartRequest,
    settings: Settings = Depends(get_settings),
    db: Session = Depends(get_db),
):
    _check_config(settings)

    # ★ 从管理端配置表读取当前使用的 avatar_id（覆盖 uniapp 传的值）
    db_config = db.query(DigitalHumanConfig).filter(
        DigitalHumanConfig.scenic_id == "lingshan"
    ).first()
    avatar_id = db_config.avatar_id if db_config else req.avatar_id
    logger.debug(f"[avatar/start] avatar_id={avatar_id} (DB override)")

    # 协议兼容性检查：rtmp 不支持透明通道
    _warnings = []
    if req.bg_transparent and req.protocol == "rtmp":
        _warnings.append(
            "bg_transparent（透明背景）需要 xrtc 协议，rtmp 不支持 Alpha 通道，"
            "已自动关闭透明通道。如需透明背景，请将 protocol 切换为 xrtc "
            "并在 uniapp 端使用讯飞原生 Android/iOS SDK 渲染。"
        )
        logger.warning(f"[avatar/start] {_warnings[0]}")
        req.bg_transparent = False

    body = {
        "header": {
            "app_id": settings.XFYUN_APP_ID,
            "status": 0,
        },
        "parameter": {
            "vmr": {
                "avatar_id": avatar_id,
                "width": req.width,
                "height": req.height,
                "stream": {
                    "protocol": req.protocol,
                },
            }
        },
        "payload": {},
    }
    # 仅 bg_transparent=True 时才添加（讯飞 API 要求数字 0/1，不用 boolean）
    if req.bg_transparent:
        body["parameter"]["vmr"]["transparent"] = 1

    result = await _post_to_xfyun(settings, _PATH_START, body)

    header = result.get("header", {})
    xf_code = header.get("code", -1)
    xf_msg = header.get("message", "")

    if xf_code != 0:
        return AvatarStartResponse(code=400, msg=f"讯飞启动失败：{xf_msg}（code={xf_code}）")

    session = header.get("session", "")
    # 拉流地址：优先从 header.stream_url 取，其次从 payload 取
    stream_url = header.get("stream_url", "")
    if not stream_url:
        payload = result.get("payload", {})
        stream_url_b64 = payload.get("stream_url", {}).get("text", "")
        if stream_url_b64:
            try:
                stream_url = base64.b64decode(stream_url_b64).decode("utf-8")
            except Exception:
                stream_url = stream_url_b64

    # 本地缓存会话信息
    _session_store[session] = {
        "session": session,
        "avatar_id": req.avatar_id,
        "stream_url": stream_url,
        "protocol": req.protocol,
        "width": req.width,
        "height": req.height,
        "bg_transparent": req.bg_transparent,
        "create_time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "last_ping_time": None,
        "is_active": True,
    }

    logger.info(f"[avatar] 会话启动成功 session={session} stream_url={stream_url[:60] if stream_url else ''}")

    return AvatarStartResponse(
        code=200,
        msg="启动成功",
        data=AvatarStartData(
            session=session,
            stream_url=stream_url,
            xfyun_code=xf_code,
            xfyun_message=xf_msg,
            warnings=_warnings,
        ),
    )


# ─────────────────────────────────────────────────────────────
# 2. 文本驱动（TTS 播报）
# ─────────────────────────────────────────────────────────────

@router.post(
    "/drive/text",
    response_model=AvatarTextDriveResponse,
    summary="文本驱动（TTS播报）",
    description="""
向虚拟人发送文本，虚拟人进行 TTS 语音合成并口播（嘴型+表情同步）。

- 文本最大 64KB
- 可选传入 `actions` 动作控制列表，通过 `wb`（字偏移）精确触发动作
- 对应讯飞接口：`POST /v1/private/vms2d_ctrl`
""",
)
async def avatar_text_drive(
    req: AvatarTextDriveRequest,
    settings: Settings = Depends(get_settings),
    db: Session = Depends(get_db),
):
    _check_config(settings)

    # ★ 从管理端配置表读取当前使用的 vcn（覆盖 uniapp 传的值）
    db_config = db.query(DigitalHumanConfig).filter(
        DigitalHumanConfig.scenic_id == "lingshan"
    ).first()
    vcn = db_config.vcn if db_config else req.vcn
    logger.debug(f"[avatar/drive/text] vcn={vcn} (DB override)")

    # ── 诊断：检查前端是否已经对文本做了 base64 编码 ──
    raw_text = req.text
    try:
        # 尝试 base64 解码 → 如果解码后是合法 UTF-8 文本，说明前端已经编码过
        decoded = base64.b64decode(raw_text).decode("utf-8")
        # 验证：解码后的文本必须全部是可打印字符
        import string
        printable_count = sum(1 for c in decoded if c in string.printable or '\u4e00' <= c <= '\u9fff')
        if printable_count / max(len(decoded), 1) > 0.5:
            logger.warning(
                f"[avatar] 检测到前端已对文本进行 base64 编码！已自动解码。"
                f" 编码文本前60字符: {raw_text[:60]}"
            )
            raw_text = decoded
    except Exception:
        # 解码失败 → 文本本来就是明文，直接用
        pass

    logger.info(f"[avatar] 文本驱动 原始文本({len(raw_text)}字符): {raw_text[:200]!r}")
    # ── 诊断 end ──

    # Base64 编码文本（讯飞 vms2d_ctrl 接口要求）
    text_b64 = base64.b64encode(raw_text.encode("utf-8")).decode("utf-8")
    logger.info(f"[avatar] 文本驱动 base64编码后({len(text_b64)}字符): {text_b64[:100]}...")

    body = {
        "header": {
            "app_id": settings.XFYUN_APP_ID,
            "session": req.session,
            "status": 3,
        },
        "parameter": {
            "tts": {
                "vcn": vcn,
                "speed": req.speed,
                "pitch": req.pitch,
                "volume": req.volume,
            }
        },
        "payload": {
            "text": {
                "encoding": "utf8",  # 文本编码声明
                "status": 3,         # 讯飞要求必须为 3（完整传输）
                "text": text_b64,
            }
        },
    }

    # 动作控制指令（可选）
    if req.actions:
        ctrl_w = {
            "avatar": [
                {"type": a.type, "value": a.value, **({"wb": a.wb} if a.wb is not None else {})}
                for a in req.actions
            ]
        }
        body["payload"]["ctrl_w"] = {
            "text": base64.b64encode(
                json.dumps(ctrl_w, ensure_ascii=False).encode("utf-8")
            ).decode("utf-8")
        }

    result = await _post_to_xfyun(settings, _PATH_CTRL, body)

    header = result.get("header", {})
    xf_code = header.get("code", -1)
    xf_msg = header.get("message", "")

    if xf_code != 0:
        return AvatarTextDriveResponse(
            code=400,
            msg=f"文本驱动失败：{xf_msg}（code={xf_code}）",
            xfyun_code=xf_code,
            xfyun_message=xf_msg,
        )

    return AvatarTextDriveResponse(
        code=200,
        msg="文本驱动成功，虚拟人正在口播",
        xfyun_code=xf_code,
        xfyun_message=xf_msg,
    )


# ─────────────────────────────────────────────────────────────
# 3. 音频驱动 — 返回 WSS 鉴权 URL 供 uniapp 直连
# ─────────────────────────────────────────────────────────────

@router.post(
    "/drive/audio/url",
    response_model=AvatarAudioUrlResponse,
    summary="获取音频驱动 WSS 鉴权 URL",
    description="""
获取带讯飞签名的 WSS 鉴权 URL，uniapp 拿到此 URL 后，
直接用 `uni.connectSocket()` 建立 WebSocket 连接，
然后按帧发送如下格式的 JSON 音频数据：

```json
{
  "header": {
    "app_id": "你的APPID",
    "session": "会话session",
    "status": 0
  },
  "payload": {
    "audio": {
      "encoding": "raw",
      "sample_rate": 16000,
      "status": 0,
      "seq": 1,
      "audio": "base64编码的PCM音频数据"
    }
  }
}
```

`status` 含义：0=开始帧，1=中间帧，2=结束帧（最后一帧附带一个假字节）

对应讯飞接口：`WSS /v1/private/vms2d_audio_ctrl`
""",
)
async def avatar_audio_url(
    req: AvatarAudioUrlRequest,
    settings: Settings = Depends(get_settings),
):
    _check_config(settings)

    wss_url = build_wss_auth_url(
        wss_base=settings.XFYUN_VMS_WSS_URL,
        host=settings.XFYUN_VMS_HOST,
        path=_PATH_AUDIO_CTRL,
        api_key=settings.XFYUN_API_KEY,
        api_secret=settings.XFYUN_API_SECRET,
    )

    return AvatarAudioUrlResponse(
        code=200,
        msg="获取成功",
        data=AvatarAudioUrlData(
            wss_url=wss_url,
            session=req.session,
            app_id=settings.XFYUN_APP_ID,
            encoding="raw",
            sample_rate=16000,
        ),
    )


# ─────────────────────────────────────────────────────────────
# 4. 音频驱动代理（服务端中转，适合不支持 WSS 的场景）
# ─────────────────────────────────────────────────────────────

@router.post(
    "/drive/audio/proxy",
    response_model=AvatarAudioFrameResponse,
    summary="音频帧代理转发（服务端中转模式）",
    description="""
【服务端中转模式】：uniapp 将 PCM 音频 Base64 分帧发送到此接口，
服务端持有与讯飞的 WSS 长连接，负责转发。

**帧发送规则：**
- `status=0`：开始帧（首帧），服务端建立新 WSS 连接
- `status=1`：中间帧，持续发送
- `status=2`：结束帧，服务端发送后关闭连接

**注意：** 此模式下服务端维护每个 session 的 WSS 连接，
并发量大时建议改用 `/drive/audio/url` 直连模式。

对应讯飞接口：`WSS /v1/private/vms2d_audio_ctrl`
""",
)
async def avatar_audio_proxy(
    req: AvatarAudioFrameRequest,
    settings: Settings = Depends(get_settings),
):
    _check_config(settings)

    try:
        # 构造发送给讯飞的音频帧消息
        ctrl_payload: dict = {}
        if req.actions:
            ctrl_t = {
                "avatar": [
                    {"type": a.type, "value": a.value}
                    for a in req.actions
                ]
            }
            ctrl_payload["ctrl_t"] = {
                "text": base64.b64encode(
                    json.dumps(ctrl_t, ensure_ascii=False).encode("utf-8")
                ).decode("utf-8")
            }

        frame_msg = {
            "header": {
                "app_id": settings.XFYUN_APP_ID,
                "session": req.session,
                "status": req.status,
            },
            "payload": {
                "audio": {
                    "encoding": req.encoding,
                    "sample_rate": req.sample_rate,
                    "status": req.status,
                    "seq": req.seq,
                    "audio": req.audio_base64,
                },
                **ctrl_payload,
            },
        }

        if req.status == 0:
            # 开始帧：建立新 WSS 连接
            wss_url = build_wss_auth_url(
                wss_base=settings.XFYUN_VMS_WSS_URL,
                host=settings.XFYUN_VMS_HOST,
                path=_PATH_AUDIO_CTRL,
                api_key=settings.XFYUN_API_KEY,
                api_secret=settings.XFYUN_API_SECRET,
            )
            # 关闭旧连接（如有）
            old_ws = _audio_ws_store.pop(req.session, None)
            if old_ws:
                try:
                    await old_ws.close()
                except Exception:
                    pass

            ws = await websockets.connect(wss_url, ping_interval=None)
            _audio_ws_store[req.session] = ws
            await ws.send(json.dumps(frame_msg, ensure_ascii=False))
            resp_raw = await ws.recv()

        elif req.status == 1:
            # 中间帧：复用已有连接
            ws = _audio_ws_store.get(req.session)
            if ws is None:
                return AvatarAudioFrameResponse(code=400, msg="会话 WSS 连接不存在，请先发送 status=0 的开始帧")
            await ws.send(json.dumps(frame_msg, ensure_ascii=False))
            resp_raw = await ws.recv()

        else:
            # status=2 结束帧
            ws = _audio_ws_store.pop(req.session, None)
            if ws is None:
                return AvatarAudioFrameResponse(code=400, msg="会话 WSS 连接不存在，请先发送 status=0 的开始帧")
            # 讯飞要求结束帧附带一个假字节确保 status 信号被接收
            frame_msg["payload"]["audio"]["audio"] = req.audio_base64 + base64.b64encode(b"\x00").decode()
            await ws.send(json.dumps(frame_msg, ensure_ascii=False))
            try:
                resp_raw = await asyncio.wait_for(ws.recv(), timeout=5.0)
            except asyncio.TimeoutError:
                resp_raw = "{}"
            await ws.close()

        # 解析讯飞响应
        try:
            resp_json = json.loads(resp_raw)
        except Exception:
            resp_json = {}

        xf_header = resp_json.get("header", {})
        xf_code = xf_header.get("code", 0)
        xf_msg = xf_header.get("message", "ok")

        if xf_code != 0:
            return AvatarAudioFrameResponse(
                code=400,
                msg=f"音频驱动失败：{xf_msg}（code={xf_code}）",
                xfyun_code=xf_code,
                xfyun_message=xf_msg,
            )

        return AvatarAudioFrameResponse(
            code=200,
            msg="音频帧转发成功",
            xfyun_code=xf_code,
            xfyun_message=xf_msg,
        )

    except websockets.exceptions.WebSocketException as e:
        # 清理失效连接
        _audio_ws_store.pop(req.session, None)
        raise HTTPException(status_code=502, detail=f"WSS 连接异常：{str(e)}")
    except Exception as e:
        _audio_ws_store.pop(req.session, None)
        raise HTTPException(status_code=500, detail=f"音频代理内部错误：{str(e)}")


# ─────────────────────────────────────────────────────────────
# 5. 停止会话
# ─────────────────────────────────────────────────────────────

@router.post(
    "/stop",
    response_model=AvatarStopResponse,
    summary="停止虚拟人会话",
    description="""
结束指定会话，释放讯飞服务端资源。
**使用完毕后务必调用此接口**，否则会话资源不会及时释放，影响计费。

对应讯飞接口：`POST /v1/private/vms2d_stop`
""",
)
async def avatar_stop(
    req: AvatarStopRequest,
    settings: Settings = Depends(get_settings),
):
    _check_config(settings)

    body = {
        "header": {
            "app_id": settings.XFYUN_APP_ID,
            "session": req.session,
        },
    }

    result = await _post_to_xfyun(settings, _PATH_STOP, body)

    header = result.get("header", {})
    xf_code = header.get("code", -1)
    xf_msg = header.get("message", "")

    # 无论成功失败，本地清理
    _session_store.pop(req.session, None)
    old_ws = _audio_ws_store.pop(req.session, None)
    if old_ws:
        try:
            await old_ws.close()
        except Exception:
            pass

    if xf_code != 0:
        return AvatarStopResponse(
            code=400,
            msg=f"停止失败：{xf_msg}（code={xf_code}）",
            xfyun_code=xf_code,
            xfyun_message=xf_msg,
        )

    logger.info(f"[avatar] 会话已停止 session={req.session}")
    return AvatarStopResponse(
        code=200,
        msg="会话已停止，资源已释放",
        xfyun_code=xf_code,
        xfyun_message=xf_msg,
    )


# ─────────────────────────────────────────────────────────────
# 6. 心跳保活
# ─────────────────────────────────────────────────────────────

@router.post(
    "/ping",
    response_model=AvatarPingResponse,
    summary="心跳保活",
    description="""
讯飞服务端 **60 秒无请求** 会自动结束会话，建议 uniapp 每 **30 秒**调用一次此接口。

对应讯飞接口：`POST /v1/private/vms2d_ping`
""",
)
async def avatar_ping(
    req: AvatarPingRequest,
    settings: Settings = Depends(get_settings),
):
    _check_config(settings)

    body = {
        "header": {
            "app_id": settings.XFYUN_APP_ID,
            "session": req.session,
        },
    }

    result = await _post_to_xfyun(settings, _PATH_PING, body)

    header = result.get("header", {})
    xf_code = header.get("code", -1)
    xf_msg = header.get("message", "")

    # 更新本地会话最后心跳时间
    if req.session in _session_store:
        _session_store[req.session]["last_ping_time"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    if xf_code != 0:
        return AvatarPingResponse(
            code=400,
            msg=f"心跳失败（会话可能已过期）：{xf_msg}（code={xf_code}）",
            xfyun_code=xf_code,
            xfyun_message=xf_msg,
        )

    return AvatarPingResponse(
        code=200,
        msg="心跳成功",
        xfyun_code=xf_code,
        xfyun_message=xf_msg,
    )


# ─────────────────────────────────────────────────────────────
# 7. 查询会话状态（本地缓存）
# ─────────────────────────────────────────────────────────────

@router.get(
    "/session/{session}",
    response_model=AvatarSessionResponse,
    summary="查询会话状态",
    description="""
查询本地缓存的会话信息（stream_url、avatar_id 等）。
**注意**：此接口查询的是本地内存缓存，服务重启后缓存清空。
""",
)
async def avatar_session_info(session: str):
    info = _session_store.get(session)
    if not info:
        return AvatarSessionResponse(code=404, msg="会话不存在或已过期")
    return AvatarSessionResponse(
        code=200,
        msg="ok",
        data=AvatarSessionInfo(**info),
    )


# ─────────────────────────────────────────────────────────────
# 8. 动作列表（内置常用动作，按 avatar_id 分类）
# ─────────────────────────────────────────────────────────────

# 内置常用动作字典（来自讯飞官方文档）
_AVATAR_ACTIONS: Dict[str, list] = {
    "118801001": [  # 依丹
        {"value": "A_TALKING_LONG", "description": "说话段（长）"},
        {"value": "A_LISTENING", "description": "聆听"},
        {"value": "A_TALKING", "description": "交互说话段"},
        {"value": "A_LH_introduced_O", "description": "左手向左上介绍"},
        {"value": "A_RH_bye_O", "description": "右手挥手再见"},
        {"value": "A_RH_good_O", "description": "右手点赞夸奖"},
        {"value": "A_RH_please1_O", "description": "右手向前有请"},
        {"value": "A_RH_emphasize2_O", "description": "右手手掌强调"},
        {"value": "A_LRH_welcome_O", "description": "双手打开欢迎"},
    ],
    "110026010": [  # 伊凡
        {"value": "A_LH_introduced_O", "description": "左手左上介绍"},
        {"value": "A_RH_emphasize2_O", "description": "右手手掌强调"},
        {"value": "A_RH_hello_O", "description": "右手打招呼"},
        {"value": "A_RLH_welcome_O", "description": "双手打开"},
        {"value": "A_LH_bye_O", "description": "挥手再见"},
    ],
    "default": [
        {"value": "A_TALKING_LONG", "description": "说话段"},
        {"value": "A_LISTENING", "description": "聆听"},
        {"value": "A_LH_introduced_O", "description": "左手向左上介绍"},
        {"value": "A_RH_bye_O", "description": "右手挥手再见"},
        {"value": "A_RH_good_O", "description": "右手点赞夸奖"},
    ],
}


@router.get(
    "/actions/{avatar_id}",
    response_model=AvatarActionsResponse,
    summary="获取指定形象的常用动作列表",
    description="""
返回指定形象支持的常用动作列表，用于 `text_drive` 接口的 `actions` 参数。
完整动作列表请参考讯飞官方文档。
""",
)
async def avatar_actions(avatar_id: str):
    actions_raw = _AVATAR_ACTIONS.get(avatar_id, _AVATAR_ACTIONS["default"])
    return AvatarActionsResponse(
        code=200,
        msg="ok",
        avatar_id=avatar_id,
        actions=[AvatarActionInfo(**a) for a in actions_raw],
    )


# ─────────────────────────────────────────────────────────────
# 9. 发音人列表（内置，来自讯飞官方文档）
# ─────────────────────────────────────────────────────────────

_VCN_LIST = [
    {"vcn": "x4_lingxiaoyu_assist", "name": "聆小瑜-助手", "language": "中文普通话", "scene": "交互", "gender": "女"},
    {"vcn": "x4_lingxiaozhen_eclives", "name": "聆小臻-直播", "language": "中文普通话", "scene": "直播", "gender": "女"},
    {"vcn": "x4_lingyuyan", "name": "聆玉言", "language": "中文普通话", "scene": "通用", "gender": "女"},
    {"vcn": "x4_lingxiaoshan_profnews", "name": "聆小珊-新闻", "language": "中文普通话", "scene": "新闻", "gender": "女"},
    {"vcn": "x4_lingxiaolu_assist", "name": "聆小璐-助手", "language": "中文普通话", "scene": "交互", "gender": "女"},
    {"vcn": "x4_lingfeizhe_assist", "name": "聆飞哲-助手", "language": "中文普通话", "scene": "交互", "gender": "男"},
    {"vcn": "x4_lingfeichen_emo", "name": "聆飞晨-情感", "language": "中文普通话", "scene": "情感", "gender": "男"},
    {"vcn": "x4_yezi", "name": "小露", "language": "中文普通话", "scene": "通用", "gender": "女"},
    {"vcn": "x4_EnUs_Luna_assist", "name": "Luna-助手", "language": "美式英语", "scene": "交互", "gender": "女"},
    {"vcn": "x4_EnUs_Gavin_assist", "name": "Gavin-助手", "language": "美式英语", "scene": "交互", "gender": "男"},
    {"vcn": "x3_qianxue", "name": "晴雪", "language": "中文普通话", "scene": "通用", "gender": "女"},
    {"vcn": "aisjiuxu", "name": "九旭", "language": "中文普通话", "scene": "通用", "gender": "男"},
]


@router.get(
    "/vcn/list",
    response_model=AvatarVcnListResponse,
    summary="获取支持的发音人列表",
    description="""
返回讯飞虚拟人支持的发音人列表，`vcn` 字段用于 `/avatar/drive/text` 接口的 `vcn` 参数。
""",
)
async def avatar_vcn_list():
    return AvatarVcnListResponse(
        code=200,
        msg="ok",
        data=[VcnInfo(**v) for v in _VCN_LIST],
    )
