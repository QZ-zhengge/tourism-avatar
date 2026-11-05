"""
用户数据模型
"""
from sqlalchemy import Column, Integer, String, DateTime, Text
from sqlalchemy.sql import func
from database.database import Base


class User(Base):
    """用户表模型"""
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="用户ID")
    username = Column(String(50), unique=True, nullable=False, comment="用户名")
    password = Column(String(255), nullable=False, comment="密码(加密存储)")
    email = Column(String(100), unique=True, nullable=False, comment="邮箱")
    phone_number = Column(String(20), unique=True, nullable=False, comment="手机号")
    nickname = Column(String(50), nullable=True, comment="昵称")
    avatar = Column(String(255), nullable=True, comment="头像URL")
    status = Column(Integer, default=1, comment="状态: 1正常 0禁用")
    create_time = Column(DateTime, server_default=func.now(), comment="创建时间")
    update_time = Column(DateTime, server_default=func.now(), onupdate=func.now(), comment="更新时间")

    def to_dict(self):
        return {
            "id": self.id,
            "username": self.username,
            "email": self.email,
            "phone_number": self.phone_number,
            "nickname": self.nickname or self.username,
            "avatar": self.avatar,
            "status": self.status,
            "create_time": str(self.create_time) if self.create_time else None,
            "update_time": str(self.update_time) if self.update_time else None,
        }