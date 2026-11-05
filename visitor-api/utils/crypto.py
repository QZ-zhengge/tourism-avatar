"""
工具模块 - 密码加密和其他工具函数
"""
import hashlib
import logging
from typing import Optional

logger = logging.getLogger(__name__)


def hash_password(password: str, salt: Optional[str] = None) -> str:
    """
    密码哈希加密

    Args:
        password: 原始密码
        salt: 盐值，默认使用固定盐（生产环境应动态生成）

    Returns:
        str: 加密后的密码
    """
    if salt is None:
        salt = "hbuilder_app_salt_2024"  # 生产环境应使用动态盐

    # 双重SHA256加密
    first_hash = hashlib.sha256((password + salt).encode()).hexdigest()
    second_hash = hashlib.sha256((first_hash + salt).encode()).hexdigest()

    logger.debug(f"[密码加密] 密码加密完成")
    return second_hash


def verify_password(password: str, hashed_password: str, salt: Optional[str] = None) -> bool:
    """
    验证密码

    Args:
        password: 原始密码
        hashed_password: 加密后的密码
        salt: 盐值

    Returns:
        bool: 密码是否匹配
    """
    if salt is None:
        salt = "hbuilder_app_salt_2024"

    computed_hash = hash_password(password, salt)
    return computed_hash == hashed_password