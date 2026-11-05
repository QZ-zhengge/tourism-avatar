"""
驴友圈相关Pydantic模型
"""
from pydantic import BaseModel, Field
from typing import Optional, List
from datetime import datetime


# ========== 帖子相关 ==========

class PostCreateRequest(BaseModel):
    """创建帖子请求"""
    user_id: int = Field(..., description="发布者用户ID")
    content: Optional[str] = Field(None, description="文字内容")
    location_name: Optional[str] = Field(None, description="定位名称")
    latitude: Optional[str] = Field(None, description="纬度")
    longitude: Optional[str] = Field(None, description="经度")
    visibility: int = Field(0, description="可见性：0公开 1仅好友 2私密")

    class Config:
        json_schema_extra = {
            "example": {
                "user_id": 1,
                "content": "今天天气真好，来了一场说走就走的旅行！",
                "location_name": "北京市朝阳区",
                "latitude": "39.908823",
                "longitude": "116.397470",
                "visibility": 0
            }
        }


class PostUpdateRequest(BaseModel):
    """更新帖子请求"""
    content: Optional[str] = Field(None, description="文字内容")
    location_name: Optional[str] = Field(None, description="定位名称")
    latitude: Optional[str] = Field(None, description="纬度")
    longitude: Optional[str] = Field(None, description="经度")
    visibility: Optional[int] = Field(None, description="可见性：0公开 1仅好友 2私密")


class PostImageResponse(BaseModel):
    """帖子图片响应"""
    id: int
    url: str
    sort_order: int
    width: Optional[int] = None
    height: Optional[int] = None


class PostVideoResponse(BaseModel):
    """帖子视频响应"""
    id: int
    url: str
    cover_url: Optional[str] = None
    duration: Optional[int] = None
    width: Optional[int] = None
    height: Optional[int] = None
    size: Optional[int] = None


class PostResponse(BaseModel):
    """帖子响应"""
    id: int
    user_id: int
    content: Optional[str] = None
    location_name: Optional[str] = None
    latitude: Optional[str] = None
    longitude: Optional[str] = None
    visibility: int
    like_count: int
    comment_count: int
    is_liked: bool = False  # 当前用户是否点赞
    images: List[PostImageResponse] = []
    videos: List[PostVideoResponse] = []
    create_time: Optional[str] = None
    update_time: Optional[str] = None

    class Config:
        from_attributes = True


class PostListResponse(BaseModel):
    """帖子列表响应"""
    code: int
    msg: str
    data: List[PostResponse]
    total: int = 0
    page: int = 1
    page_size: int = 10


class PostSimpleResponse(BaseModel):
    """帖子简单响应（用于评论中的帖子信息）"""
    id: int
    user_id: int
    content: Optional[str] = None


# ========== 评论相关 ==========

class CommentCreateRequest(BaseModel):
    """创建评论请求"""
    post_id: int = Field(..., description="帖子ID")
    user_id: int = Field(..., description="评论用户ID")
    content: str = Field(..., min_length=1, max_length=1000, description="评论内容")
    parent_id: Optional[int] = Field(None, description="父评论ID（回复时传入）")

    class Config:
        json_schema_extra = {
            "example": {
                "post_id": 1,
                "user_id": 1,
                "content": "写得真好！",
                "parent_id": None
            }
        }


class CommentResponse(BaseModel):
    """评论响应"""
    id: int
    post_id: int
    parent_id: Optional[int] = None
    user_id: int
    username: Optional[str] = None
    nickname: Optional[str] = None
    content: str
    like_count: int
    is_liked: bool = False
    replies: List["CommentResponse"] = []
    create_time: Optional[str] = None
    update_time: Optional[str] = None

    class Config:
        from_attributes = True


class CommentListResponse(BaseModel):
    """评论列表响应"""
    code: int
    msg: str
    data: List[CommentResponse]
    total: int = 0


# ========== 点赞相关 ==========

class LikeRequest(BaseModel):
    """点赞请求"""
    post_id: int = Field(..., description="帖子ID")
    user_id: int = Field(..., description="用户ID")


class LikeResponse(BaseModel):
    """点赞响应"""
    code: int
    msg: str
    is_liked: bool  # 当前是否点赞状态


class LikeListResponse(BaseModel):
    """点赞列表响应"""
    code: int
    msg: str
    data: List[dict]
    total: int = 0


# ========== 文件上传相关 ==========

class FileUploadResponse(BaseModel):
    """文件上传响应"""
    code: int
    msg: str
    url: str  # 文件访问路径
    filename: str  # 原始文件名
    size: int  # 文件大小（字节）


class MultiFileUploadResponse(BaseModel):
    """多文件上传响应"""
    code: int
    msg: str
    files: List[FileUploadResponse]


# ========== 用户信息（用于展示）==========

class UserSimpleInfo(BaseModel):
    """用户简单信息"""
    user_id: int
    username: str
    nickname: Optional[str] = None
    avatar: Optional[str] = None


# 更新自身引用
CommentResponse.model_rebuild()