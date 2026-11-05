"""
管理端共享模型 — 数字人配置表（由管理端写入，主项目只读）
"""
from sqlalchemy import Column, Integer, String, DateTime
from database.database import Base


class DigitalHumanConfig(Base):
    __tablename__ = "digital_human_config"

    id = Column(Integer, primary_key=True, autoincrement=True)
    scenic_id = Column(String(50), nullable=False, default="lingshan")
    avatar_id = Column(String(50), nullable=False, default="111260002")
    vcn = Column(String(50), nullable=False, default="x4_xiaoxuan")
    speed = Column(Integer, nullable=False, default=50)
    pitch = Column(Integer, nullable=False, default=50)
    volume = Column(Integer, nullable=False, default=50)
    service_id = Column(String(100), nullable=False)
    layout = Column(String(20), nullable=False)
    display_mode = Column(String(20), nullable=False)
    updated_at = Column(DateTime, nullable=True)
    sync_status = Column(String(20), nullable=True)
