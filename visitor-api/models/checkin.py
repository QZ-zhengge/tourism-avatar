"""
地图打卡数据模型

- ProvinceCode：标准省份编号表（GB/T 2260，6 位行政区划代码）
- UserCheckin ：用户省份打卡记录（user_id + province_code 唯一约束，避免重复）
"""
from sqlalchemy import Column, Integer, String, DateTime, UniqueConstraint
from sqlalchemy.sql import func
from database.database import Base


class ProvinceCode(Base):
    """标准省份编号表（GB/T 2260，6 位行政区划代码，如 110000 北京市）"""

    __tablename__ = "province_codes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    province_code = Column(String(20), nullable=False, unique=True,
                           comment="标准省份编号 GB/T 2260（6 位）")
    province_name = Column(String(50), nullable=False, comment="省份名称")
    created_at = Column(DateTime, server_default=func.now(), comment="入库时间")


class UserCheckin(Base):
    """用户省份打卡记录（地图点亮）"""

    __tablename__ = "user_checkins"
    __table_args__ = (
        UniqueConstraint("user_id", "province_code", name="uq_user_province"),
    )

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, nullable=False, index=True, comment="用户ID（对应 users.id）")
    province_code = Column(String(20), nullable=False, comment="标准省份编号")
    created_at = Column(DateTime, server_default=func.now(), comment="打卡时间")
