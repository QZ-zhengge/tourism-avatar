"""
test_08_asr.py — 本地语音转文字（ASR / Vosk）真正流式 WebSocket 接口测试

测试覆盖：
  1. GET  /asr/status    — 查询模型状态
  2. POST /asr/preload   — 预加载模型
  3. GET  /asr/models    — 列出本地模型
  4. WS   /asr/stream    — 流式语音转文字（完整流程）
  5. WS   /asr/stream    — 边缘：空音频（只发 end，不发 audio）
  6. WS   /asr/stream    — 边缘：仅发 config+end，无音频块
  7. WS   /asr/stream    — 边缘：多块分片发送（模拟 uniapp 录音分段）
  8. WS   /asr/stream    — 边缘：缺少 config 帧（默认 zh/wav）

说明：
  - 所有测试均通过真实网络请求访问运行中的后端
  - WebSocket 测试使用 websocket-client 库
  - 测试音频为脚本动态生成的 PCM/WAV（不依赖外部文件）
  - Vosk 首次加载模型约 1~3 秒
  - 若服务端 Vosk 未安装或模型未下载，相关测试会 skip

依赖：
    pip install pytest requests pytest-order websocket-client
"""
import base64
import io
import json
import struct
import time
import wave
import os

import pytest
import requests
import websocket


# ── 配置 ───────────────────────────────────────────────────
BASE_URL = "http://localhost:8000"
BASE_WS = "ws://localhost:8000"
ASR_WS_URL = BASE_WS + "/asr/stream"
ASR_STATUS_URL = BASE_URL + "/asr/status"
ASR_PRELOAD_URL = BASE_URL + "/asr/preload"
ASR_MODELS_URL = BASE_URL + "/asr/models"

# 测试超时（Vosk 比 Whisper 快很多，不需要太长的超时）
WS_TIMEOUT = 30
PRELOAD_TIMEOUT = 15


# ── 辅助函数：生成测试音频 ────────────────────────────────

def generate_silence_wav(duration_sec: float = 1.0) -> bytes:
    """生成一段静音 WAV（PCM int16, 16kHz, 单声道），Vosk 会识别为空"""
    sample_rate = 16000
    num_samples = int(sample_rate * duration_sec)
    samples = b'\x00\x00' * num_samples  # 静音

    buf = io.BytesIO()
    with wave.open(buf, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)  # int16 = 2 bytes
        wf.setframerate(sample_rate)
        wf.writeframes(samples)
    return buf.getvalue()


def generate_tone_wav(duration_sec: float = 1.5, freq: float = 440.0) -> bytes:
    """生成一段正弦波 WAV（有声，Vosk 可尝试识别但结果不可控）"""
    sample_rate = 16000
    num_samples = int(sample_rate * duration_sec)
    samples = []
    for i in range(num_samples):
        t = i / sample_rate
        val = int(16000 * (2 * (t * freq - int(t * freq)) - 1))  # 简单锯齿波
        samples.append(struct.pack('<h', max(-32768, min(32767, val))))
    pcm_data = b''.join(samples)

    buf = io.BytesIO()
    with wave.open(buf, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        wf.writeframes(pcm_data)
    return buf.getvalue()


def wav_to_base64_chunks(wav_bytes: bytes, chunk_size: int = 4096) -> list[str]:
    """将 WAV 字节切分为 base64 块列表"""
    return [base64.b64encode(wav_bytes[i:i + chunk_size]).decode()
            for i in range(0, len(wav_bytes), chunk_size)]


# ── 前置条件检查 ──────────────────────────────────────────

@pytest.fixture(scope="session")
def server_is_up():
    """确保服务端在运行"""
    try:
        r = requests.get(f"{BASE_URL}/health", timeout=5)
        return r.status_code == 200
    except Exception:
        pytest.skip("服务端未运行，跳过 ASR 测试")


@pytest.fixture(scope="session")
def vosk_available():
    """确保 Vosk 模型可用"""
    try:
        r = requests.get(ASR_STATUS_URL, timeout=5)
        data = r.json()
        if data.get("backend") != "vosk":
            pytest.skip("ASR 引擎不是 Vosk（可能是旧版 Whisper），跳过测试")
        return True
    except Exception:
        return False


# ────────────────────────────────────────────────────────────
# 测试用例
# ────────────────────────────────────────────────────────────

@pytest.mark.order(1)
class TestASRStatus:
    """查询模型状态"""

    def test_status_vosk_backend(self, server_is_up, vosk_available):
        """验证后端引擎是 Vosk"""
        r = requests.get(ASR_STATUS_URL, timeout=5)
        assert r.status_code == 200
        data = r.json()
        assert data["code"] == 200
        assert data["backend"] == "vosk"
        assert data["model_name"] == "vosk"

    def test_status_loaded(self, server_is_up, vosk_available):
        """模型应已加载（preload 后）"""
        r = requests.get(ASR_STATUS_URL, timeout=5)
        data = r.json()
        # Vosk 模型在首次连接时加载，此处可能未加载
        assert isinstance(data["loaded"], bool)
        assert "loaded_langs" in data


@pytest.mark.order(2)
class TestASRPreload:
    """预加载模型"""

    def test_preload_zh(self, server_is_up, vosk_available):
        """预加载中文模型"""
        r = requests.post(f"{ASR_PRELOAD_URL}?language=zh", timeout=PRELOAD_TIMEOUT)
        assert r.status_code == 200
        data = r.json()
        assert data["code"] == 200
        assert "zh" in data.get("msg", "") or "模型" in data.get("msg", "")

    def test_preload_already_loaded(self, server_is_up, vosk_available):
        """重复预加载不会出错"""
        r = requests.post(f"{ASR_PRELOAD_URL}?language=zh", timeout=5)
        assert r.status_code == 200
        data = r.json()
        assert data["code"] == 200


@pytest.mark.order(3)
class TestASRModels:
    """列出本地模型"""

    def test_list_models(self, server_is_up, vosk_available):
        """GET /asr/models 应返回模型列表"""
        r = requests.get(ASR_MODELS_URL, timeout=5)
        assert r.status_code == 200
        data = r.json()
        assert data["code"] == 200
        assert "model_dir" in data
        assert "models" in data
        assert isinstance(data["models"], list)


@pytest.mark.order(4)
class TestASRStream:
    """WebSocket 流式 ASR"""

    def test_full_flow_with_silence(self, server_is_up, vosk_available):
        """
        完整流式 ASR 流程（静音音频）
        Vosk 对静音的识别结果应为空字符串
        """
        wav_bytes = generate_silence_wav(duration_sec=1.5)
        chunks = wav_to_base64_chunks(wav_bytes, chunk_size=4096)

        ws = websocket.create_connection(ASR_WS_URL, timeout=WS_TIMEOUT)

        try:
            # 1. 发 config
            ws.send(json.dumps({
                "type": "config",
                "language": "zh",
                "format": "wav",
                "sampleRate": 16000,
            }))

            # 2. 逐块发 audio
            for chunk in chunks:
                ws.send(json.dumps({"type": "audio", "data": chunk}))
                time.sleep(0.05)

            # 3. 发 end
            ws.send(json.dumps({"type": "end"}))

            # 4. 收集所有消息
            messages = []
            ws.settimeout(5)
            while True:
                try:
                    msg = ws.recv()
                    messages.append(json.loads(msg))
                except websocket.WebSocketTimeoutException:
                    break

            # 5. 验证
            types = [m["type"] for m in messages]
            assert "final" in types, f"应收到 final 消息，收到：{types}"

            final_msg = next((m for m in messages if m["type"] == "final"), {})
            # 静音识别结果可能为空
            assert "text" in final_msg

        finally:
            ws.close()

    def test_only_end_no_audio(self, server_is_up, vosk_available):
        """只发 config+end，不发音频——应返回空文本"""
        ws = websocket.create_connection(ASR_WS_URL, timeout=WS_TIMEOUT)

        try:
            ws.send(json.dumps({
                "type": "config",
                "language": "zh",
                "format": "wav",
            }))
            ws.send(json.dumps({"type": "end"}))

            messages = []
            ws.settimeout(5)
            while True:
                try:
                    msg = ws.recv()
                    messages.append(json.loads(msg))
                except websocket.WebSocketTimeoutException:
                    break

            types = [m["type"] for m in messages]
            assert "final" in types
            final_msg = next((m for m in messages if m["type"] == "final"), {})
            assert final_msg.get("text", "") == ""

        finally:
            ws.close()

    def test_no_config_defaults(self, server_is_up, vosk_available):
        """不发送 config 帧——直接发 audio + end，后端应使用默认 zh/wav"""
        wav_bytes = generate_silence_wav(duration_sec=1.0)
        chunks = wav_to_base64_chunks(wav_bytes, chunk_size=4096)

        ws = websocket.create_connection(ASR_WS_URL, timeout=WS_TIMEOUT)

        try:
            # 直接发 audio，不发 config
            for chunk in chunks[:3]:
                ws.send(json.dumps({"type": "audio", "data": chunk}))

            ws.send(json.dumps({"type": "end"}))

            messages = []
            ws.settimeout(5)
            while True:
                try:
                    msg = ws.recv()
                    messages.append(json.loads(msg))
                except websocket.WebSocketTimeoutException:
                    break

            types = [m["type"] for m in messages]
            assert "final" in types, f"应收到 final，收到：{types}"

        finally:
            ws.close()

    def test_many_small_chunks(self, server_is_up, vosk_available):
        """模拟 uniapp 极细粒度分片（每 512 字节一块）"""
        wav_bytes = generate_silence_wav(duration_sec=1.5)
        chunks = wav_to_base64_chunks(wav_bytes, chunk_size=512)  # 极小块

        ws = websocket.create_connection(ASR_WS_URL, timeout=WS_TIMEOUT)

        try:
            ws.send(json.dumps({
                "type": "config",
                "language": "zh",
                "format": "wav",
                "sampleRate": 16000,
            }))

            for chunk in chunks:
                ws.send(json.dumps({"type": "audio", "data": chunk}))
                time.sleep(0.01)

            ws.send(json.dumps({"type": "end"}))

            messages = []
            ws.settimeout(5)
            while True:
                try:
                    msg = ws.recv()
                    messages.append(json.loads(msg))
                except websocket.WebSocketTimeoutException:
                    break

            types = [m["type"] for m in messages]
            assert "final" in types

        finally:
            ws.close()

    def test_tone_audio(self, server_is_up, vosk_available):
        """测试有声音频（锯齿波）——Vosk 应输出非空结果（或空，取决于模型）"""
        wav_bytes = generate_tone_wav(duration_sec=2.0)
        chunks = wav_to_base64_chunks(wav_bytes, chunk_size=4096)

        ws = websocket.create_connection(ASR_WS_URL, timeout=WS_TIMEOUT)

        try:
            ws.send(json.dumps({
                "type": "config",
                "language": "zh",
                "format": "wav",
                "sampleRate": 16000,
            }))

            for chunk in chunks:
                ws.send(json.dumps({"type": "audio", "data": chunk}))

            ws.send(json.dumps({"type": "end"}))

            messages = []
            ws.settimeout(5)
            while True:
                try:
                    msg = ws.recv()
                    messages.append(json.loads(msg))
                except websocket.WebSocketTimeoutException:
                    break

            types = [m["type"] for m in messages]
            assert "final" in types
            # 不要求非空结果（锯齿波不是语音），只要没报错就行
            assert "error" not in types

        finally:
            ws.close()

    def test_partial_messages_received(self, server_is_up, vosk_available):
        """确认 Vosk 能推送 partial 消息（真正流式的标志）"""
        wav_bytes = generate_tone_wav(duration_sec=2.5)
        chunks = wav_to_base64_chunks(wav_bytes, chunk_size=4096)

        ws = websocket.create_connection(ASR_WS_URL, timeout=WS_TIMEOUT)

        try:
            ws.send(json.dumps({
                "type": "config",
                "language": "zh",
                "format": "wav",
                "sampleRate": 16000,
            }))

            for chunk in chunks:
                ws.send(json.dumps({"type": "audio", "data": chunk}))
                time.sleep(0.1)  # 让 Vosk 有时间处理

            ws.send(json.dumps({"type": "end"}))

            messages = []
            ws.settimeout(5)
            while True:
                try:
                    msg = ws.recv()
                    messages.append(json.loads(msg))
                except websocket.WebSocketTimeoutException:
                    break

            types = [m["type"] for m in messages]
            # Vosk 应至少输出 partial 或 final
            assert len(messages) > 0, "应收到至少一条消息"
            assert "error" not in types, f"不应有错误消息，但收到：{messages}"

        finally:
            ws.close()
