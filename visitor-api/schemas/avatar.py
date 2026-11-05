"""
讯飞在线虚拟人驱动 — Pydantic 请求/响应模型
官方文档：https://www.xfyun.cn/doc/tts/virtual_human/API.html
"""
from typing import Optional, List, Any, Union
from pydantic import BaseModel, Field, field_validator


# ─────────────────────────────────────────────────────────────
# 通用响应
# ─────────────────────────────────────────────────────────────

class AvatarBaseResponse(BaseModel):
    code: int = Field(..., description="业务状态码，200=成功，其他=失败")
    msg: str = Field(..., description="提示信息")


# ─────────────────────────────────────────────────────────────
# 1. 启动接口 (POST /avatar/start)
# ─────────────────────────────────────────────────────────────

class AvatarStreamConfig(BaseModel):
    """拉流协议配置"""
    protocol: str = Field(
        default="xrtc",
        description="拉流协议：xrtc（默认，延迟低）或 rtmp"
    )


class AvatarStartRequest(BaseModel):
    """
    启动虚拟人会话请求
    对应讯飞接口：POST /v1/private/vms2d_start
    """
    avatar_id: str = Field(
        default="110005011",
        description=(
            "形象ID；游客端可留空，服务端始终以数字人配置表"
            "(digital_human_config) 中的默认值覆盖此项，配置表为唯一可信来源"
        )
    )
    width: int = Field(
        default=1280,
        description="视频宽度，单位px"
    )
    height: int = Field(
        default=720,
        description="视频高度，单位px"
    )
    protocol: str = Field(
        default="xrtc",
        description="拉流协议：xrtc（低延迟推荐，支持透明通道）或 rtmp（不支持透明通道）"
    )
    bg_transparent: bool = Field(
        default=False,
        description="是否开启透明通道（背景透明）。默认False，仅显示数字人形象无背景。须配合 xrtc 协议使用。"
    )
    user_id: Optional[Union[int, str]] = Field(
        default=None,
        description=(
            "游客ID；由前端在启动请求中附带，用于「数字人对话历史」归属。"
            "未登录态可留空（历史仍会记录，仅 user_id 为空）。"
            "支持传入数字（Long）或字符串，后端统一归一为去空白的字符串。"
        )
    )

    @field_validator("user_id", mode="before")
    @classmethod
    def _norm_user_id(cls, v):
        # 允许前端传 Long（数字）或字符串；统一转为去首尾空白的字符串，空值视为未登录
        if v is None:
            return None
        s = str(v).strip()
        return s or None

    class Config:
        json_schema_extra = {
            "example": {
                "avatar_id": "110005011",
                "width": 1280,
                "height": 720,
                "protocol": "xrtc",
                "user_id": "u_123456"
            }
        }


class AvatarStartData(BaseModel):
    session: str = Field(..., description="会话ID，后续所有接口必须携带")
    stream_url: str = Field(..., description="拉流地址，uniapp 直接传给播放器")
    xfyun_code: int = Field(..., description="讯飞原始返回code，0=成功")
    xfyun_message: str = Field(..., description="讯飞原始返回message")
    warnings: list[str] = Field(
        default_factory=list,
        description="警告信息列表（如协议不兼容自动降级）",
    )


class AvatarStartResponse(AvatarBaseResponse):
    data: Optional[AvatarStartData] = None


# ─────────────────────────────────────────────────────────────
# 2. 文本驱动接口 (POST /avatar/drive/text)
# ─────────────────────────────────────────────────────────────

class AvatarActionItem(BaseModel):
    """动作控制指令，在文本驱动中通过 wb 指定动作起始字偏移"""
    type: str = Field(default="action", description="控制类型，固定 action")
    value: str = Field(..., description="动作名称，如 A_LH_introduced_O")
    wb: Optional[int] = Field(
        default=None,
        description="动作起始字偏移（文本驱动时有效，音频驱动不需要）"
    )


class AvatarActionControl(BaseModel):
    """动作控制 JSON，对应请求中的 ctrl_w 字段"""
    avatar: List[AvatarActionItem] = Field(
        default_factory=list,
        description="动作控制列表"
    )


class AvatarTextDriveRequest(BaseModel):
    """
    文本驱动（TTS播报）请求
    对应讯飞接口：POST /v1/private/vms2d_ctrl
    """
    session: str = Field(..., description="启动接口返回的 session")
    text: str = Field(..., min_length=1, description="要朗读的文本，最大 64KB")
    vcn: str = Field(
        default="x4_lingxiaoyu_assist",
        description=(
            "发音人；游客端可留空，服务端始终以数字人配置表中的默认值覆盖此项，"
            "配置表为唯一可信来源"
        )
    )
    speed: int = Field(
        default=50,
        ge=0, le=100,
        description="语速，0-100，默认50"
    )
    pitch: int = Field(
        default=50,
        ge=0, le=100,
        description="语调，0-100，默认50"
    )
    volume: int = Field(
        default=50,
        ge=0, le=100,
        description="音量，0-100，默认50"
    )
    actions: Optional[List[AvatarActionItem]] = Field(
        default=None,
        description="动作控制指令列表（可选），配合 wb 字偏移触发动作"
    )
    user_id: Optional[Union[int, str]] = Field(
        default=None,
        description=(
            "游客ID；与 /avatar/start 携带的 user_id 保持一致，用于「数字人对话历史」归属。"
            "支持数字（Long）或字符串，后端统一归一为去空白的字符串。"
        )
    )

    @field_validator("user_id", mode="before")
    @classmethod
    def _norm_user_id(cls, v):
        if v is None:
            return None
        s = str(v).strip()
        return s or None

    user_text: Optional[str] = Field(
        default=None,
        description=(
            "游客语音转文字后的【提问】文本。传入时，会在「数字人对话历史」中"
            "额外记录一条 role=user 的消息；不传则只记录数字人回答(role=assistant)。"
            "即：user_text=游客说的话，text=数字人回答（丢给 TTS 的文本）。"
        )
    )

    class Config:
        json_schema_extra = {
            "example": {
                "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                "text": "你好，欢迎使用讯飞虚拟人服务！",
                "vcn": "x4_lingxiaoyu_assist",
                "speed": 50,
                "pitch": 50,
                "volume": 50,
                "user_id": "u_123456",
                "user_text": "你知道灵山有哪些必看景点吗？",
                "actions": [
                    {"type": "action", "value": "A_LH_introduced_O", "wb": 3}
                ]
            }
        }


class AvatarTextDriveResponse(AvatarBaseResponse):
    xfyun_code: Optional[int] = Field(None, description="讯飞原始 code")
    xfyun_message: Optional[str] = Field(None, description="讯飞原始 message")


# ─────────────────────────────────────────────────────────────
# 3. 音频驱动接口 — 获取 WSS 鉴权 URL (GET /avatar/drive/audio/url)
# ─────────────────────────────────────────────────────────────

class AvatarAudioUrlRequest(BaseModel):
    """
    获取音频驱动 WSS 鉴权 URL 请求
    uniapp 拿到此 URL 后，直接用 WebSocket 连接讯飞服务进行音频驱动
    """
    session: str = Field(..., description="启动接口返回的 session")

    class Config:
        json_schema_extra = {
            "example": {
                "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            }
        }


class AvatarAudioUrlData(BaseModel):
    wss_url: str = Field(..., description="带鉴权的 WSS URL，uniapp 直接连接此地址")
    session: str = Field(..., description="会话 session，发送音频帧时需携带")
    app_id: str = Field(..., description="APPID，发送音频帧时需携带")
    encoding: str = Field(default="raw", description="推荐音频编码：raw(PCM)/lame/opus-wb/speex-wb")
    sample_rate: int = Field(default=16000, description="采样率，固定16000")


class AvatarAudioUrlResponse(AvatarBaseResponse):
    data: Optional[AvatarAudioUrlData] = None


# ─────────────────────────────────────────────────────────────
# 4. 音频驱动代理接口 (POST /avatar/drive/audio/proxy)
#    服务端代理：uniapp 将 PCM 音频分帧发到此接口，由服务端转发给讯飞 WSS
# ─────────────────────────────────────────────────────────────

class AvatarAudioFrameRequest(BaseModel):
    """
    音频帧代理转发请求
    uniapp 将 PCM 音频 Base64 编码后，分帧逐帧发送此接口
    """
    session: str = Field(..., description="会话 session")
    audio_base64: str = Field(
        ...,
        description="Base64 编码的音频数据（PCM raw，16kHz，单声道）"
    )
    status: int = Field(
        ...,
        ge=0, le=2,
        description="帧状态：0=开始帧，1=中间帧，2=结束帧"
    )
    seq: int = Field(
        default=1,
        description="帧序号，从1开始递增"
    )
    encoding: str = Field(
        default="raw",
        description="音频编码：raw(PCM)/lame/opus-wb/speex-wb，默认raw"
    )
    sample_rate: int = Field(
        default=16000,
        description="采样率，固定16000"
    )
    actions: Optional[List[AvatarActionItem]] = Field(
        default=None,
        description="动作控制指令（音频驱动时 wb 字段无效，不需要填写）"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
                "audio_base64": "UklGRiQAAABXQVZF...",
                "status": 0,
                "seq": 1,
                "encoding": "raw",
                "sample_rate": 16000
            }
        }


class AvatarAudioFrameResponse(AvatarBaseResponse):
    xfyun_code: Optional[int] = None
    xfyun_message: Optional[str] = None


# ─────────────────────────────────────────────────────────────
# 5. 停止接口 (POST /avatar/stop)
# ─────────────────────────────────────────────────────────────

class AvatarStopRequest(BaseModel):
    """
    停止虚拟人会话请求
    对应讯飞接口：POST /v1/private/vms2d_stop
    """
    session: str = Field(..., description="要停止的会话 session")

    class Config:
        json_schema_extra = {
            "example": {
                "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            }
        }


class AvatarStopResponse(AvatarBaseResponse):
    xfyun_code: Optional[int] = None
    xfyun_message: Optional[str] = None


# ─────────────────────────────────────────────────────────────
# 6. 心跳接口 (POST /avatar/ping)
# ─────────────────────────────────────────────────────────────

class AvatarPingRequest(BaseModel):
    """
    会话保活心跳请求
    对应讯飞接口：POST /v1/private/vms2d_ping
    讯飞服务端 60 秒无请求会自动断开，需客户端定期（建议每30s）发送一次
    """
    session: str = Field(..., description="会话 session")

    class Config:
        json_schema_extra = {
            "example": {
                "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
            }
        }


class AvatarPingResponse(AvatarBaseResponse):
    xfyun_code: Optional[int] = None
    xfyun_message: Optional[str] = None


# ─────────────────────────────────────────────────────────────
# 7. 会话状态查询 (GET /avatar/session/{session})
# ─────────────────────────────────────────────────────────────

class AvatarSessionInfo(BaseModel):
    session: str
    avatar_id: str
    stream_url: str
    protocol: str
    width: int
    height: int
    bg_transparent: bool = False
    create_time: str
    last_ping_time: Optional[str] = None
    is_active: bool


class AvatarSessionResponse(AvatarBaseResponse):
    data: Optional[AvatarSessionInfo] = None


# ─────────────────────────────────────────────────────────────
# 8. 动作列表查询 (GET /avatar/actions/{avatar_id})
# ─────────────────────────────────────────────────────────────

class AvatarActionInfo(BaseModel):
    value: str = Field(..., description="动作名称")
    description: str = Field(..., description="动作描述")


class AvatarActionsResponse(AvatarBaseResponse):
    avatar_id: Optional[str] = None
    actions: Optional[List[AvatarActionInfo]] = None


# ─────────────────────────────────────────────────────────────
# 8.1 当前默认数字人配置（来自数据库，供游客端随时读取）
# ─────────────────────────────────────────────────────────────

class AvatarConfigData(BaseModel):
    scenic_id: str = Field(..., description="景区ID")
    avatar_id: str = Field(..., description="当前默认形象ID（来自数字人配置表）")
    avatar_name: Optional[str] = Field(None, description="形象名称")
    vcn: str = Field(..., description="当前默认发音人")
    speed: int = Field(..., description="语速 0-100")
    pitch: int = Field(..., description="语调 0-100")
    volume: int = Field(..., description="音量 0-100")
    service_id: Optional[str] = Field(None, description="讯飞虚拟人服务ID")
    layout: Optional[str] = Field(None, description="布局")
    display_mode: Optional[str] = Field(None, description="显示模式")
    updated_at: Optional[str] = Field(None, description="配置更新时间（ISO8601）")
    sync_status: Optional[str] = Field(None, description="同步状态")
    source: str = Field(
        ...,
        description="配置来源：db=来自数据库（管理端已配置）；builtin_default=内置兜底默认值（管理端尚未配置）"
    )


class AvatarConfigResponse(AvatarBaseResponse):
    data: Optional[AvatarConfigData] = None


# ─────────────────────────────────────────────────────────────
# 9. 发音人列表 (GET /avatar/vcn/list)
# ─────────────────────────────────────────────────────────────

class VcnInfo(BaseModel):
    vcn: str = Field(..., description="发音人代码，用于 text_drive 接口的 vcn 参数")
    name: str = Field(..., description="发音人名称")
    language: str = Field(..., description="语言")
    scene: str = Field(..., description="场景")
    gender: str = Field(..., description="性别")


class AvatarVcnListResponse(AvatarBaseResponse):
    data: Optional[List[VcnInfo]] = None


# ─────────────────────────────────────────────────────────────
# 10. 数字人对话历史列表 (GET /avatar/chat/history/user/{user_id})
# ─────────────────────────────────────────────────────────────

class AvatarChatSessionItem(BaseModel):
    """数字人对话历史 — 单个会话条目（列表展示用）"""
    session_id: str = Field(..., description="会话ID")
    avatar_id: Optional[str] = Field(None, description="驱动的数字人形象ID")
    started_at: Optional[str] = Field(None, description="会话开始时间（ISO8601）")
    ended_at: Optional[str] = Field(None, description="会话结束时间（ISO8601）；未结束为 null")
    message_count: int = Field(0, description="消息条数（冗余计数）")
    last_message: Optional[str] = Field(
        None,
        description="该会话最后一条消息文本（user/assistant 中最新的一条），便于列表预览"
    )


class AvatarChatHistoryData(BaseModel):
    """数字人对话历史列表分页数据"""
    total: int = Field(..., description="符合条件的总会话数")
    page: int = Field(..., description="当前页码（从1开始）")
    page_size: int = Field(..., description="每页条数")
    items: List[AvatarChatSessionItem] = Field(default_factory=list, description="会话列表")


class AvatarChatHistoryResponse(AvatarBaseResponse):
    data: Optional[AvatarChatHistoryData] = None
