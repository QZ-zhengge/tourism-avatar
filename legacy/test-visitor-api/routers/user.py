"""
用户路由模块
"""
import logging
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from database import get_db
from models.user import User
from schemas.user import UserRegisterRequest, UserRegisterResponse, UserLoginRequest, UserLoginResponse

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
    logger.info("=" * 50)
    logger.info(f"[注册请求] 收到注册请求: username={user_data.username}, email={user_data.email}, phone={user_data.phone_number}")

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

        logger.info(f"[注册成功] 新用户创建成功: id={new_user.id}, username={new_user.username}")
        logger.info("=" * 50)

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
    logger.info("=" * 50)
    logger.info(f"[登录请求] 收到登录请求: username={login_data.username}")

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
    logger.info(f"[登录成功] 用户登录成功: id={user.id}, username={user.username}")

    # 构建返回数据
    user_info = {
        "userId": user.id,
        "username": user.username,
        "nickname": user.nickname or user.username,
        "email": user.email,
        "phoneNumber": user.phone_number
    }

    logger.info("=" * 50)
    return UserLoginResponse(code=200, msg="登录成功", data=user_info)