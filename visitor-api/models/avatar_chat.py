"""
数字人对话历史记录 — 新建表（不在任何原有表上加列）
设计原则：与原有 6 张业务表、管理端 dialogue_sessions/dialogue_messages 完全解耦，
本功能独立建两张新表，原表一个字都不动。

写入方：主项目（游客端）routers/avatar.py 的 start / drive/text / stop 三个埋点。
读取方：管理端 routers/dialogue.py（只读）。

表结构：
  avatar_chat_sessions   会话主表（一次虚拟人对话 = 一行）
  avatar_chat_messages   消息明细表（每轮问答 = 若干行，role=user/assistant）
"""
from sqlalchemy import Column, Integer, String, Text, DateTime
from sqlalchemy.sql import func
from database.database import Base


class AvatarChatSession(Base):
    __tablename__ = "avatar_chat_sessions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    session_id = Column(String(255), nullable=False, unique=True, comment="讯飞会话ID")
    user_id = Column(String(100), nullable=True, comment="游客ID（前端在请求中附带，未登录可为空）")
    scenic_id = Column(String(50), nullable=True, comment="景区ID")
    avatar_id = Column(String(50), nullable=True, comment="实际驱动的数字人形象ID")
    started_at = Column(DateTime, server_default=func.now(), comment="会话开始时间")
    ended_at = Column(DateTime, nullable=True, comment="会话结束时间")
    message_count = Column(Integer, default=0, comment="消息条数（冗余计数，便于列表展示）")
    created_at = Column(DateTime, server_default=func.now(), comment="入库时间")


class AvatarChatMessage(Base):
    __tablename__ = "avatar_chat_messages"

    id = Column(Integer, primary_key=True, autoincrement=True)
    session_id = Column(String(255), nullable=False, index=True, comment="会话ID，关联 avatar_chat_sessions")
    seq_no = Column(Integer, nullable=False, comment="消息序号（同一会话内从1递增）")
    role = Column(String(20), nullable=False, comment="user=游客提问 / assistant=数字人回答")
    content = Column(Text, nullable=False, comment="消息文本")
    source = Column(String(20), nullable=True, comment="text / audio（来源：文本输入 或 语音转写）")
    created_at = Column(DateTime, server_default=func.now(), comment="消息时间")
