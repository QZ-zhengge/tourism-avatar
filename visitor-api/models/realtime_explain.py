"""
实时讲解历史记录 数据模型

- RealtimeExplainHistory：实时讲解历史（用户上传图片 + AI 返回的含 JSON 文本，后端原样存储）

属于「数字人对话历史记录」的分支功能。后端不做任何图像 / JSON 分析，
两个字段均由前端主动传入，仅落库、供后续原样返回。
"""
from sqlalchemy import Column, Integer, String, Text, DateTime
from sqlalchemy.sql import func
from database.database import Base


class RealtimeExplainHistory(Base):
    """实时讲解历史记录（数字人对话历史的分支功能）

    前端主动上传图片（base64 或 URL 字符串）与 AI 返回的含 JSON 文本，
    后端不做任何分析，原样存储、原样返回。
    """

    __tablename__ = "realtime_explain_history"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(100), nullable=True, index=True,
                     comment="用户ID（前端主动传入，未登录可留空）")
    image = Column(Text(4294967295), nullable=False,
                   comment="前端上传的图片（base64 或 URL 字符串，原样存储，后端不分析）— LONGTEXT，支持最大 ~4GB")
    ai_response = Column(Text, nullable=False,
                         comment="AI 返回的含 JSON 文本，原样存储，后端不解析")
    scenic_id = Column(String(50), nullable=True, comment="景区ID（可选）")
    created_at = Column(DateTime, server_default=func.now(), comment="入库时间")
