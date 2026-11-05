"""
用户路由模块
"""
import logging
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from database import get_db
from models.user import User
from schemas.user import UserRegisterRequest, UserRegisterResponse, UserLoginRequest, UserLoginResponse
from schemas.user import UserUpdateProfileRequest, UserUpdatePasswordRequest

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/user", tags=["用户模块"])


@router.post("/register/", response_model=UserRegisterResponse)
async def register(user_data: UserRegisterRequest, db: Session = Depends(get_db)):
    """
    用户注册接口

    请求示例：
    {
        "username": "wujunhui",
        "password": "CHANGE_ME",
        "confirm_password": "CHANGE_ME",
        "email": "wujunhui@example.com",
        "phone_number": "13800138000"
    }

    响应：
    - 注册成功：{"code": 200, "msg": "注册成功"}
    - 用户名已存在：{"code": 400, "msg": "用户名已存在"}
    - 邮箱已存在：{"code": 400, "msg": "邮箱已被注册"}
    - 手机号已存在：{"code": 400, "msg": "手机号已被注册"}
    - 密码不一致：{"code": 400, "msg": "两次输入的密码不一致"}
    """
    logger.info(f"[注册] username={user_data.username}")

    # 检查密码确认
    if user_data.password != user_data.confirm_password:
        logger.warning(f"[注册失败] 密码不一致: username={user_data.username}")
        return UserRegisterResponse(code=400, msg="两次输入的密码不一致")

    # 检查用户名是否已存在
    existing_user = db.query(User).filter(User.username == user_data.username).first()
    if existing_user:
        logger.warning(f"[注册失败] 用户名已存在: username={user_data.username}")
        return UserRegisterResponse(code=400, msg="用户名已存在")

    # 检查邮箱是否已存在
    existing_email = db.query(User).filter(User.email == user_data.email).first()
    if existing_email:
        logger.warning(f"[注册失败] 邮箱已被注册: email={user_data.email}")
        return UserRegisterResponse(code=400, msg="邮箱已被注册")

    # 检查手机号是否已存在
    existing_phone = db.query(User).filter(User.phone_number == user_data.phone_number).first()
    if existing_phone:
        logger.warning(f"[注册失败] 手机号已被注册: phone={user_data.phone_number}")
        return UserRegisterResponse(code=400, msg="手机号已被注册")

    # 创建新用户
    try:
        new_user = User(
            username=user_data.username,
            password=user_data.password,  # 生产环境应加密存储
            email=user_data.email,
            phone_number=user_data.phone_number,
            nickname=user_data.username,  # 默认昵称同用户名
            status=1
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)

        logger.info(f"[注册成功] id={new_user.id}")

        return UserRegisterResponse(code=200, msg="注册成功")

    except IntegrityError as e:
        db.rollback()
        logger.error(f"[注册错误] 数据库完整性错误: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="注册失败，请稍后重试"
        )
    except Exception as e:
        db.rollback()
        logger.error(f"[注册错误] 注册时发生未知错误: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="注册失败，请稍后重试"
        )


@router.post("/login/", response_model=UserLoginResponse)
async def login(login_data: UserLoginRequest, db: Session = Depends(get_db)):
    """
    用户登录接口

    请求示例：
    {
        "username": "wujunhui",
        "password": "CHANGE_ME"
    }

    响应：
    - 登录成功：{"code": 200, "msg": "登录成功", "data": {"userId": 1, "username": "wujunhui", "nickname": "wujunhui"}}
    - 用户不存在：{"code": 400, "msg": "用户名或密码错误"}
    - 密码错误：{"code": 400, "msg": "用户名或密码错误"}
    - 账户禁用：{"code": 400, "msg": "账户已被禁用"}
    """
    logger.info(f"[登录] username={login_data.username}")

    # 根据用户名查找用户
    user = db.query(User).filter(User.username == login_data.username).first()

    # 检查用户是否存在
    if not user:
        logger.warning(f"[登录失败] 用户不存在: username={login_data.username}")
        return UserLoginResponse(code=400, msg="用户名或密码错误")

    # 检查密码（明文对比，生产环境应加密后比对）
    if user.password != login_data.password:
        logger.warning(f"[登录失败] 密码错误: username={login_data.username}")
        return UserLoginResponse(code=400, msg="用户名或密码错误")

    # 检查账户状态
    if user.status != 1:
        logger.warning(f"[登录失败] 账户已被禁用: username={login_data.username}, status={user.status}")
        return UserLoginResponse(code=400, msg="账户已被禁用")

    # 登录成功
    logger.info(f"[登录成功] id={user.id}")

    user_info = {
        "userId": user.id,
        "username": user.username,
        "nickname": user.nickname or user.username,
        "email": user.email,
        "phoneNumber": user.phone_number
    }

    return UserLoginResponse(code=200, msg="登录成功", data=user_info)


@router.get("/profile/", summary="获取用户个人资料")
async def get_profile(user_id: int = Query(..., description="用户ID"), db: Session = Depends(get_db)):
    """
    个人中心 - 根据 user_id 获取用户个人资料（昵称/头像/邮箱/手机号等）。

    - 存在：{"code": 200, "msg": "ok", "data": {...User.to_dict()...}}
    - 不存在：{"code": 400, "msg": "用户不存在", "data": None}
    """
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        return {"code": 400, "msg": "用户不存在", "data": None}
    return {"code": 200, "msg": "ok", "data": user.to_dict()}


@router.put("/profile/", summary="修改个人信息")
async def update_profile(data: UserUpdateProfileRequest, db: Session = Depends(get_db)):
    """
    修改个人资料（昵称/头像/邮箱/手机号）。
    仅更新传入的非空字段，不传的字段保持不变。

    - 邮箱/手机号变更时会做唯一性检查
    - 返回更新后的完整个人信息
    """
    user = db.query(User).filter(User.id == data.user_id).first()
    if not user:
        return {"code": 400, "msg": "用户不存在", "data": None}

    updated_fields = []

    if data.nickname is not None:
        user.nickname = data.nickname
        updated_fields.append("nickname")

    if data.avatar is not None:
        user.avatar = data.avatar
        updated_fields.append("avatar")

    if data.email is not None:
        existing = db.query(User).filter(
            User.email == data.email, User.id != data.user_id
        ).first()
        if existing:
            return {"code": 400, "msg": "邮箱已被其他用户使用", "data": None}
        user.email = data.email
        updated_fields.append("email")

    if data.phone_number is not None:
        existing = db.query(User).filter(
            User.phone_number == data.phone_number, User.id != data.user_id
        ).first()
        if existing:
            return {"code": 400, "msg": "手机号已被其他用户使用", "data": None}
        user.phone_number = data.phone_number
        updated_fields.append("phone_number")

    if not updated_fields:
        return {"code": 400, "msg": "未提供任何需要修改的字段", "data": None}

    try:
        db.commit()
        db.refresh(user)
        logger.info(f"[修改个人信息] user_id={data.user_id} 修改了: {', '.join(updated_fields)}")
    except Exception as e:
        db.rollback()
        logger.error(f"[修改个人信息] 失败 user_id={data.user_id}: {e}")
        return {"code": 500, "msg": "保存失败，请稍后重试", "data": None}

    return {"code": 200, "msg": "修改成功", "data": user.to_dict()}


@router.put("/password/", summary="修改密码")
async def update_password(data: UserUpdatePasswordRequest, db: Session = Depends(get_db)):
    """
    修改密码：验证旧密码后更新为新密码。

    - 旧密码错误：{"code": 400, "msg": "旧密码不正确"}
    - 两次新密码不一致：{"code": 400, "msg": "两次输入的新密码不一致"}
    - 修改成功：{"code": 200, "msg": "密码修改成功", "data": null}
    """
    user = db.query(User).filter(User.id == data.user_id).first()
    if not user:
        return {"code": 400, "msg": "用户不存在", "data": None}

    if user.password != data.old_password:
        return {"code": 400, "msg": "旧密码不正确", "data": None}

    if data.new_password != data.confirm_password:
        return {"code": 400, "msg": "两次输入的新密码不一致", "data": None}

    try:
        user.password = data.new_password
        db.commit()
        logger.info(f"[修改密码] user_id={data.user_id} 密码已更新")
    except Exception as e:
        db.rollback()
        logger.error(f"[修改密码] 失败 user_id={data.user_id}: {e}")
        return {"code": 500, "msg": "保存失败，请稍后重试", "data": None}

    return {"code": 200, "msg": "密码修改成功", "data": None}