"""
管理端 Pydantic 请求/响应模型
严格按照规范文档字段名对齐
"""
from typing import Optional, List, Any
from datetime import datetime
from pydantic import BaseModel, Field, field_validator


# ═══════════════════════════════════════════════════
# 通用
# ═══════════════════════════════════════════════════

class BaseResponse(BaseModel):
    code: int = 200
    message: str = "success"


# ═══════════════════════════════════════════════════
# 模块一：数字人配置
# ═══════════════════════════════════════════════════

# 注意：数字人形象列表(avatar_id/name)不再写死在代码里，
# 改由 avatar_catalog 表（种子数据 data/avatar_catalog.json）提供，
# 运行时从数据库读取。下方仅保留发音人(vcn)的软校验集合。
VALID_VCNS = {
    "x4_xiaoxuan", "x4_lingxiaoyu_assist", "x4_lingxiaozhen_eclives",
    "x4_lingyuyan", "x4_EnUs_Luna_assist", "x4_EnUs_Gavin_assist",
    "x4_EnUk_Amanda_education",
}

VALID_LAYOUTS = {"fullscreen", "left", "right"}
VALID_DISPLAY_MODES = {"head", "upper", "full"}


class DigitalConfigRequest(BaseModel):
    scenic_id: str = Field(default="lingshan")
    avatar_id: str = Field(default="111260002")
    vcn: str = Field(default="x4_xiaoxuan")
    speed: int = Field(default=50, ge=0, le=100)
    pitch: int = Field(default=50, ge=0, le=100)
    volume: int = Field(default=50, ge=0, le=100)
    service_id: str = Field(default="315837983779590144")
    layout: str = Field(default="fullscreen")
    display_mode: str = Field(default="full")

    @field_validator("layout")
    @classmethod
    def check_layout(cls, v):
        if v not in VALID_LAYOUTS:
            raise ValueError(f"layout 必须是 {VALID_LAYOUTS} 之一")
        return v

    @field_validator("display_mode")
    @classmethod
    def check_display_mode(cls, v):
        if v not in VALID_DISPLAY_MODES:
            raise ValueError(f"display_mode 必须是 {VALID_DISPLAY_MODES} 之一")
        return v


class DigitalConfigResponse(BaseModel):
    scenic_id: str
    avatar_id: str
    vcn: str
    speed: int
    pitch: int
    volume: int
    service_id: str
    layout: str
    display_mode: str
    sync_status: str = "saved"
    updated_at: Optional[str] = None


# ═══════════════════════════════════════════════════
# 模块二：内容管理
# ═══════════════════════════════════════════════════

class ImageUploadResponse(BaseModel):
    id: int
    spot_name: str
    file_name: str
    file_url: str
    file_size: int
    created_at: Optional[str] = None


class ImageListItem(BaseModel):
    id: int
    spot_name: str
    file_name: str
    file_url: str
    file_size: int
    created_at: Optional[str] = None


# ═══════════════════════════════════════════════════
# 模块四：对话分析
# ═══════════════════════════════════════════════════

class DialogueSessionItem(BaseModel):
    session_id: str
    user_name: Optional[str] = None
    satisfaction_score: float = 0
    message_count: int = 0
    categories: Optional[List[str]] = None
    last_updated: Optional[str] = None
    reason: Optional[str] = None
    suggestion: Optional[str] = None


class DialogueMessageItem(BaseModel):
    seq_no: int
    role: str
    content: str
    sentiment: Optional[str] = None
    created_at: Optional[str] = None


class DialogueDetailResponse(BaseModel):
    session_id: str
    satisfaction_score: float = 0
    reason: Optional[str] = None
    suggestion: Optional[str] = None
    messages: List[DialogueMessageItem] = []


class DialogueListResponse(BaseModel):
    total: int = 0
    page: int = 1
    page_size: int = 50
    items: List[DialogueSessionItem] = []


# ═══════════════════════════════════════════════════
# 模块四（新）：数字人对话历史 — 读取 avatar_chat_* 表
# 注意：response 结构已与本功能对齐，前端按此文档对接
# ═══════════════════════════════════════════════════

class AvatarChatSessionItem(BaseModel):
    session_id: str = Field(..., description="会话ID")
    user_id: Optional[str] = Field(None, description="游客ID（未登录可为空）")
    scenic_id: Optional[str] = Field(None, description="景区ID")
    avatar_id: Optional[str] = Field(None, description="数字人形象ID")
    avatar_name: Optional[str] = Field(None, description="数字人形象名称（由 avatar_catalog 关联得到）")
    message_count: int = Field(0, description="消息条数")
    started_at: Optional[str] = Field(None, description="会话开始时间（ISO8601）")
    ended_at: Optional[str] = Field(None, description="会话结束时间（ISO8601），未结束为空")


class AvatarChatMessageItem(BaseModel):
    seq_no: int = Field(..., description="消息序号（从1递增）")
    role: str = Field(..., description="user=游客提问 / assistant=数字人回答")
    content: str = Field(..., description="消息文本")
    source: Optional[str] = Field(None, description="text / audio（来源）")
    created_at: Optional[str] = Field(None, description="消息时间（ISO8601）")


class AvatarChatDetailResponse(BaseModel):
    session_id: str
    user_id: Optional[str] = None
    scenic_id: Optional[str] = None
    avatar_id: Optional[str] = None
    avatar_name: Optional[str] = None
    message_count: int = 0
    started_at: Optional[str] = None
    ended_at: Optional[str] = None
    messages: List[AvatarChatMessageItem] = []


class AvatarChatListResponse(BaseModel):
    total: int = 0
    page: int = 1
    page_size: int = 50
    items: List[AvatarChatSessionItem] = []


# ═══════════════════════════════════════════════════
# 模块：登录注册
# ═══════════════════════════════════════════════════

class AuthRegisterRequest(BaseModel):
    username: str = Field(..., min_length=1)
    email: str = Field(..., pattern=r'^[^\s@]+@[^\s@]+\.[^\s@]+$')
    password: str = Field(..., min_length=6)


class AuthLoginRequest(BaseModel):
    username: str = Field(..., min_length=1)
    password: str = Field(...)


class AuthUserInfo(BaseModel):
    id: int
    username: str
    email: str
    role: str
    avatar: Optional[str] = None


# ═══════════════════════════════════════════════════
# 模块：信箱（用户建议）
# ═══════════════════════════════════════════════════

class SuggestionItem(BaseModel):
    id: str
    user_id: Optional[int] = None
    user_name: Optional[str] = "匿名用户"
    contact: Optional[str] = None
    content: str
    status: str  # unread / read
    created_at: Optional[str] = None
