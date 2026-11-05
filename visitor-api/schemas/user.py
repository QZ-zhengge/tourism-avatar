"""
Pydantic模型 - 请求/响应数据验证
"""
from pydantic import BaseModel, Field, field_validator
from typing import Optional
import re


class UserRegisterRequest(BaseModel):
    """用户注册请求"""
    username: str = Field(..., min_length=3, max_length=50, description="用户名")
    password: str = Field(..., min_length=6, max_length=50, description="密码")
    confirm_password: str = Field(..., description="确认密码")
    email: str = Field(..., description="邮箱")
    phone_number: str = Field(..., description="手机号")

    @field_validator('username')
    @classmethod
    def validate_username(cls, v):
        if not re.match(r'^[\w\u4e00-\u9fa5]{3,50}$', v):
            raise ValueError('用户名只能包含字母、数字、下划线、中文，长度3-50位')
        return v

    @field_validator('password')
    @classmethod
    def validate_password(cls, v):
        if len(v) < 6:
            raise ValueError('密码长度不能少于6位')
        return v

    @field_validator('email')
    @classmethod
    def validate_email(cls, v):
        if not re.match(r'^[\w\.-]+@[\w\.-]+\.\w+$', v):
            raise ValueError('邮箱格式不正确')
        return v

    @field_validator('phone_number')
    @classmethod
    def validate_phone_number(cls, v):
        if not re.match(r'^1[3-9]\d{9}$', v):
            raise ValueError('手机号格式不正确，应为1开头的11位数字')
        return v

    class Config:
        json_schema_extra = {
            "example": {
                "username": "wujunhui",
                "password": "CHANGE_ME",
                "confirm_password": "CHANGE_ME",
                "email": "wujunhui@example.com",
                "phone_number": "13800138000"
            }
        }


class UserRegisterResponse(BaseModel):
    """用户注册响应"""
    code: int = Field(..., description="状态码，200/201成功，400失败")
    msg: str = Field(..., description="消息")


class UserLoginRequest(BaseModel):
    """用户登录请求"""
    username: str = Field(..., description="用户名")
    password: str = Field(..., description="密码")


class UserLoginResponse(BaseModel):
    """用户登录响应"""
    code: int = Field(..., description="状态码")
    msg: str = Field(..., description="消息")
    data: Optional[dict] = Field(None, description="用户信息")


class UserInfo(BaseModel):
    """用户信息"""
    id: int
    username: str
    email: str
    phone_number: str
    nickname: Optional[str] = None
    avatar: Optional[str] = None
    status: int


# ─────────────────────────────────────────────────────────────
# 修改个人信息
# ─────────────────────────────────────────────────────────────

class UserUpdateProfileRequest(BaseModel):
    """修改个人信息请求（仅传需要修改的字段，不传的字段保持不变）"""
    user_id: int = Field(..., description="用户ID")
    nickname: Optional[str] = Field(None, max_length=50, description="新昵称（不传则不修改）")
    avatar: Optional[str] = Field(None, max_length=255, description="头像URL（不传则不修改）")
    email: Optional[str] = Field(None, description="新邮箱（不传则不修改）")
    phone_number: Optional[str] = Field(None, description="新手机号（不传则不修改）")

    @field_validator('email')
    @classmethod
    def validate_email(cls, v):
        if v is not None and not re.match(r'^[\w\.-]+@[\w\.-]+\.\w+$', v):
            raise ValueError('邮箱格式不正确')
        return v

    @field_validator('phone_number')
    @classmethod
    def validate_phone_number(cls, v):
        if v is not None and not re.match(r'^1[3-9]\d{9}$', v):
            raise ValueError('手机号格式不正确，应为1开头的11位数字')
        return v

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": 4,
                "nickname": "小吴",
                "avatar": "https://cdn.example.com/avatar.png",
                "email": "newemail@example.com",
                "phone_number": "13800138000"
            }
        }


# ─────────────────────────────────────────────────────────────
# 修改密码
# ─────────────────────────────────────────────────────────────

class UserUpdatePasswordRequest(BaseModel):
    """修改密码请求"""
    user_id: int = Field(..., description="用户ID")
    old_password: str = Field(..., min_length=6, max_length=50, description="旧密码")
    new_password: str = Field(..., min_length=6, max_length=50, description="新密码")
    confirm_password: str = Field(..., description="确认新密码")

    @field_validator('new_password')
    @classmethod
    def validate_new_password(cls, v):
        if len(v) < 6:
            raise ValueError('新密码长度不能少于6位')
        return v

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": 4,
                "old_password": "CHANGE_ME",
                "new_password": "CHANGE_ME",
                "confirm_password": "CHANGE_ME"
            }
        }