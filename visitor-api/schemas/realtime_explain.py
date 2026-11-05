"""
实时讲解历史 请求/响应模型

后端作为「存储代理」，image / ai_response 两个字段均为前端主动传入，
后端原样存储、原样返回，不做任何图像解码或 JSON 解析。
"""
from typing import Optional, List, Union
from pydantic import BaseModel, Field, field_validator


class RealtimeExplainAddRequest(BaseModel):
    """添加一条实时讲解历史（前端主动传入 image + ai_response，后端原样存储）"""

    user_id: Optional[Union[int, str]] = Field(
        None, description="用户ID（前端传入，未登录可留空；支持数字 Long 或字符串）")

    @field_validator("user_id", mode="before")
    @classmethod
    def _norm_user_id(cls, v):
        # 允许前端传 Long（数字）或字符串；统一转为去首尾空白的字符串，空值视为未登录
        if v is None:
            return None
        s = str(v).strip()
        return s or None
    image: str = Field(
        ..., description="前端上传的图片：base64 字符串或图片 URL，原样存储，后端不分析")
    ai_response: str = Field(
        ..., description="AI 返回的文本内容（其中包含 JSON 数据），原样存储，后端不解析")
    scenic_id: Optional[str] = Field(None, description="景区ID（可选）")

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": "u_123456",
                "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDA...",
                "ai_response": "{\"spots\":[{\"name\":\"灵山大佛\",\"desc\":\"...\",\"duration\":30}],\"summary\":\"本次讲解共3处景点\"}",
                "scenic_id": "lingshan"
            }
        }


class RealtimeExplainItem(BaseModel):
    """实时讲解历史条目（原样返回）"""

    id: int
    user_id: Optional[str] = None
    image: str
    ai_response: str
    scenic_id: Optional[str] = None
    created_at: Optional[str] = None


class RealtimeExplainAddResponse(BaseModel):
    code: int
    msg: str
    data: Optional[RealtimeExplainItem] = None


class RealtimeExplainListData(BaseModel):
    total: int
    page: int
    page_size: int
    items: List[RealtimeExplainItem] = []


class RealtimeExplainListResponse(BaseModel):
    code: int
    msg: str
    data: Optional[RealtimeExplainListData] = None
