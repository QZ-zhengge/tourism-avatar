"""
信箱（用户建议）模型 — 游客端写入，管理端读取，共享同一张 user_suggestions 表。
表结构与管理端 models.admin.UserSuggestion 保持一致（user_id / contact 为本次新增列）。
"""
from sqlalchemy import Column, Integer, String, Text, DateTime
from sqlalchemy.sql import func
from database.database import Base


class UserSuggestion(Base):
    """用户建议表（信箱）"""
    __tablename__ = "user_suggestions"

    id = Column(String(20), primary_key=True, comment="唯一ID，如 sg_xxx")
    user_id = Column(Integer, nullable=True, index=True, comment="建议用户ID（游客端传入）")
    user_name = Column(String(100), nullable=True, default="匿名用户")
    contact = Column(String(100), nullable=True, comment="联系方式（可选，游客端传入）")
    content = Column(Text, nullable=False, comment="建议内容")
    status = Column(String(10), nullable=False, default="unread", comment="unread/read")
    created_at = Column(DateTime, server_default=func.now(), comment="提交时间")
