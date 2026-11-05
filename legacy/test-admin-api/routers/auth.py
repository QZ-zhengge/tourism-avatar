"""
模块：管理员登录/注册/信息/退出
"""
import hashlib
import logging
from datetime import datetime, timedelta, timezone

import jwt
from fastapi import APIRouter, Depends, HTTPException, Header
from sqlalchemy.orm import Session

from config import get_settings, Settings
from database.database import get_db
from models.admin import AdminUser
from schemas.admin import AuthLoginRequest, AuthRegisterRequest, AuthUserInfo

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/auth", tags=["登录注册"])


def _hash(password: str) -> str:
    return hashlib.sha256(password.encode()).hexdigest()


def _create_jwt(user: AdminUser, settings: Settings) -> str:
    payload = {
        "sub": str(user.id),
        "username": user.username,
        "role": user.role,
        "exp": datetime.now(timezone.utc) + timedelta(hours=settings.JWT_EXPIRE_HOURS),
        "iat": datetime.now(timezone.utc),
    }
    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm="HS256")


def _decode_jwt(token: str, settings: Settings) -> dict:
    try:
        return jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        raise HTTPException(401, detail="令牌已过期")
    except jwt.InvalidTokenError:
        raise HTTPException(401, detail="无效的令牌")


def _get_current_user(
    authorization: str = Header(...),
    settings: Settings = Depends(get_settings),
    db: Session = Depends(get_db),
) -> AdminUser:
    if not authorization.startswith("Bearer "):
        raise HTTPException(401, detail="缺少认证令牌")
    payload = _decode_jwt(authorization[7:], settings)
    user = db.query(AdminUser).filter(AdminUser.id == int(payload["sub"])).first()
    if not user:
        raise HTTPException(401, detail="用户不存在")
    return user


# ═══════════════════════════════════════════════════
# 1. 注册
# ═══════════════════════════════════════════════════

@router.post("/register", summary="注册")
def register(req: AuthRegisterRequest, db: Session = Depends(get_db)):
    if db.query(AdminUser).filter(AdminUser.email == req.email).first():
        return {"code": 409, "message": "该邮箱已注册"}
    if db.query(AdminUser).filter(AdminUser.username == req.username).first():
        return {"code": 409, "message": "该用户名已被占用"}

    user = AdminUser(
        username=req.username,
        email=req.email,
        password=_hash(req.password),
        role="admin",
    )
    db.add(user)
    db.commit()
    return {"code": 0, "message": "注册成功"}


# ═══════════════════════════════════════════════════
# 2. 登录
# ═══════════════════════════════════════════════════

@router.post("/login", summary="登录")
def login(req: AuthLoginRequest, settings: Settings = Depends(get_settings), db: Session = Depends(get_db)):
    user = db.query(AdminUser).filter(AdminUser.username == req.username).first()
    if not user or user.password != _hash(req.password):
        return {"code": 401, "message": "用户名或密码错误"}

    token = _create_jwt(user, settings)
    return {
        "code": 0,
        "message": "登录成功",
        "data": {
            "token": token,
            "user": {
                "id": user.id,
                "username": user.username,
                "email": user.email,
                "role": user.role,
                "avatar": user.avatar_url,
            },
        },
    }


# ═══════════════════════════════════════════════════
# 3. 获取当前用户信息
# ═══════════════════════════════════════════════════

@router.get("/me", summary="获取当前用户")
def get_me(user: AdminUser = Depends(_get_current_user)):
    return {
        "code": 0,
        "data": {
            "id": user.id,
            "username": user.username,
            "email": user.email,
            "role": user.role,
        },
    }


# ═══════════════════════════════════════════════════
# 4. 退出登录
# ═══════════════════════════════════════════════════

@router.post("/logout", summary="退出登录")
def logout(user: AdminUser = Depends(_get_current_user)):
    return {"code": 0, "message": "已退出"}
