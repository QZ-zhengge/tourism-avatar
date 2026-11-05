"""
ASR 模块 Pydantic 模型（Vosk 流式版）

注意：
  - /asr/stream 是 WebSocket 接口，不通过 HTTP Request/Response 模型，
    而是通过 JSON 文本帧通信（见 routers/asr.py 顶部协议说明）。
  - 下方模型仅用于 HTTP 接口（status / preload / models）的请求/响应校验。
"""
from pydantic import BaseModel, Field


# ──────────────────────────────────────────────────────────────────
# 响应模型
# ──────────────────────────────────────────────────────────────────

class ASRStatusResponse(BaseModel):
    """GET /asr/status 响应"""
    code: int = 200
    msg: str = "ok"
    model_name: str = "vosk"
    loaded: bool = False
    backend: str = "vosk"
    loaded_langs: list[str] = Field(default_factory=list)


class ASRPreloadResponse(BaseModel):
    """POST /asr/preload 响应"""
    code: int = 200
    msg: str = ""
    language: str = "zh"


class ASRLocalModel(BaseModel):
    """GET /asr/models 单个模型信息"""
    lang: str
    dir_name: str
    path: str
    ready: bool


class ASRModelsResponse(BaseModel):
    """GET /asr/models 响应"""
    code: int = 200
    msg: str = "ok"
    model_dir: str
    models: list[ASRLocalModel]


# ──────────────────────────────────────────────────────────────────
# WebSocket 协议消息定义（参考，非 Pydantic 校验用）
# ──────────────────────────────────────────────────────────────────
# 客户端 → 服务端：
#   {"type": "config",  "language": "zh", "format": "wav", "sampleRate": 16000}
#   {"type": "audio",   "data": "<base64>"}
#   {"type": "end"}
#
# 服务端 → 客户端：
#   {"type": "partial", "text": "...", "final": false, "seq": N}   ← 中间结果（每 ~128ms 推送）
#   {"type": "partial", "text": "...", "final": true,  "seq": N}   ← 该语音段结束，文字已稳定
#   {"type": "final",   "text": "...",                "seq": N}   ← 会话结束，完整拼接结果
#   {"type": "error",   "msg": "..."}                             ← 出错
#   {"type": "ping"}                                                  ← 心跳
