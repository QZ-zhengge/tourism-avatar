"""
数字人形象目录模型 — 与管理端共享同一张表(avatar_catalog)
由管理端写入种子数据（data/avatar_catalog.json），主项目只读，
用于 /avatar/config 等接口返回形象名称，避免把形象ID/名称写死在代码里。
"""
from sqlalchemy import Column, Integer, String, DateTime
from sqlalchemy.sql import func
from database.database import Base


class AvatarCatalog(Base):
    __tablename__ = "avatar_catalog"

    id = Column(Integer, primary_key=True, autoincrement=True)
    avatar_id = Column(String(50), nullable=False, unique=True, comment="数字人形象ID（讯飞）")
    name = Column(String(100), nullable=False, comment="形象名称")
    category = Column(String(50), nullable=True, comment="资产分类")
    status = Column(String(50), nullable=True, comment="授权状态")
    created_at = Column(DateTime, server_default=func.now(), comment="入库时间")
