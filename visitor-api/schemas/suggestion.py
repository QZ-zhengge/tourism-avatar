"""
信箱（用户建议）相关 Pydantic 模型
"""
from pydantic import BaseModel, Field
from typing import Optional


class SuggestionCreateRequest(BaseModel):
    """提交建议请求（游客端）"""
    user_id: int = Field(..., description="提交用户ID")
    content: str = Field(..., description="建议内容")
    contact: Optional[str] = Field(None, description="联系方式（可选）")
    user_name: Optional[str] = Field(None, description="用户昵称（可选，缺省按匿名处理）")

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": 1,
                "content": "希望增加离线地图功能",
                "contact": "138xxxx8888",
                "user_name": "小李"
            }
        }


class SuggestionItem(BaseModel):
    """建议条目响应"""
    id: str
    user_id: Optional[int] = None
    user_name: Optional[str] = "匿名用户"
    contact: Optional[str] = None
    content: str
    status: str
    created_at: Optional[str] = None
