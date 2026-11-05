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

VALID_AVATARS = {
    "118801001", "110026010", "110022010", "110017006", "110005018",
    "110029003", "110029002", "110021007", "110021006", "110026013",
    "110023004", "111260002",
}

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
    user_name: Optional[str] = "匿名用户"
    content: str
    status: str  # unread / read
    created_at: Optional[str] = None
