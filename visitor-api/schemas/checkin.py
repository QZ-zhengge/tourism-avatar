"""
地图打卡 请求/响应模型
"""
from typing import Optional, List
from pydantic import BaseModel


class CheckinAddRequest(BaseModel):
    """添加一次省份打卡"""
    user_id: int
    province_code: str


class CheckinAddData(BaseModel):
    """打卡结果详情"""
    province_code: str
    province_name: Optional[str] = None
    newly_added: bool  # True=本次新点亮；False=之前已点亮（幂等）


class CheckinAddResponse(BaseModel):
    code: int
    msg: str
    data: Optional[CheckinAddData] = None


class ProvinceItem(BaseModel):
    """省份编号项"""
    province_code: str
    province_name: str


class ProvinceListResponse(BaseModel):
    code: int
    msg: str
    data: List[ProvinceItem] = []


class UserCheckinListResponse(BaseModel):
    """用户已打卡的省份编号列表（前端用于地图点亮）"""
    code: int
    msg: str
    data: List[str] = []
