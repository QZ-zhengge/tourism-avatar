"""
驴友圈 - 帖子路由模块
包含：发帖、帖子列表、帖子详情、删除帖子
"""
import logging
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import desc

from database import get_db
from models.post import Post, PostImage, PostVideo, PostLike, PostComment
from models.user import User
from typing import Optional, List
from pydantic import BaseModel

from schemas.post import (
    PostCreateRequest, PostUpdateRequest,
    PostResponse, PostListResponse, PostImageResponse, PostVideoResponse
)


class MediaImageItem(BaseModel):
    url: str
    sort_order: int = 0
    width: Optional[int] = None
    height: Optional[int] = None


class MediaVideoItem(BaseModel):
    url: str
    cover_url: Optional[str] = None
    duration: Optional[int] = None
    width: Optional[int] = None
    height: Optional[int] = None
    size: Optional[int] = None


class PostMediaRequest(BaseModel):
    images: Optional[List[MediaImageItem]] = None
    videos: Optional[List[MediaVideoItem]] = None

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/circle/posts", tags=["驴友圈-帖子"])


def _build_post_response(post: Post, current_user_id: int = 0) -> dict:
    """构建帖子响应数据"""
    is_liked = any(like.user_id == current_user_id for like in post.likes)
    images = [
        {"id": img.id, "url": img.url, "sort_order": img.sort_order,
         "width": img.width, "height": img.height}
        for img in sorted(post.images, key=lambda x: x.sort_order)
    ]
    videos = [
        {"id": v.id, "url": v.url, "cover_url": v.cover_url,
         "duration": v.duration, "width": v.width, "height": v.height, "size": v.size}
        for v in post.videos
    ]
    return {
        "id": post.id,
        "user_id": post.user_id,
        "content": post.content,
        "location_name": post.location_name,
        "latitude": post.latitude,
        "longitude": post.longitude,
        "visibility": post.visibility,
        "like_count": post.like_count,
        "comment_count": post.comment_count,
        "is_liked": is_liked,
        "images": images,
        "videos": videos,
        "create_time": str(post.create_time) if post.create_time else None,
        "update_time": str(post.update_time) if post.update_time else None,
    }


@router.post("/", summary="发布帖子")
async def create_post(data: PostCreateRequest, db: Session = Depends(get_db)):
    """
    发布新帖子（文字内容）
    图片和视频需先调用 /upload/image 或 /upload/video 获取URL，
    再调用 /circle/posts/{post_id}/media 绑定到帖子。
    """
    logger.info(f"[发帖] 用户 {data.user_id} 正在发布帖子")

    # 内容和媒体不能都为空
    if not data.content and not data.location_name:
        return {"code": 400, "msg": "帖子内容不能为空"}

    post = Post(
        user_id=data.user_id,
        content=data.content,
        location_name=data.location_name,
        latitude=data.latitude,
        longitude=data.longitude,
        visibility=data.visibility,
        like_count=0,
        comment_count=0,
        status=1
    )
    db.add(post)
    db.commit()
    db.refresh(post)

    logger.info(f"[发帖成功] 帖子ID: {post.id}, 用户ID: {data.user_id}")
    return {"code": 200, "msg": "发布成功", "data": {"post_id": post.id}}


@router.post("/{post_id}/media", summary="为帖子绑定图片/视频")
async def add_post_media(
    post_id: int,
    data: PostMediaRequest,
    db: Session = Depends(get_db)
):
    """
    为已发布的帖子绑定图片或视频资源
    images: [{"url": "/uploads/images/xxx.jpg", "sort_order": 0}]
    videos: [{"url": "/uploads/videos/xxx.mp4", "cover_url": "...", "duration": 60}]
    """
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在"}

    if data.images:
        for idx, img in enumerate(data.images):
            post_img = PostImage(
                post_id=post_id,
                url=img.url,
                sort_order=img.sort_order if img.sort_order is not None else idx,
                width=img.width,
                height=img.height
            )
            db.add(post_img)

    if data.videos:
        for v in data.videos:
            post_video = PostVideo(
                post_id=post_id,
                url=v.url,
                cover_url=v.cover_url,
                duration=v.duration,
                width=v.width,
                height=v.height,
                size=v.size
            )
            db.add(post_video)

    db.commit()
    logger.info(f"[绑定媒体] 帖子 {post_id} 绑定媒体完成")
    return {"code": 200, "msg": "绑定成功"}


@router.get("/", summary="帖子广场（全部公开帖子）")
async def get_post_list(
    current_user_id: int = Query(0, description="当前用户ID（用于判断点赞状态）"),
    page: int = Query(1, ge=1, description="页码"),
    page_size: int = Query(10, ge=1, le=50, description="每页数量"),
    db: Session = Depends(get_db)
):
    """获取所有公开帖子，按时间倒序"""
    offset = (page - 1) * page_size
    total = db.query(Post).filter(Post.status == 1, Post.visibility == 0).count()
    posts = (
        db.query(Post)
        .filter(Post.status == 1, Post.visibility == 0)
        .order_by(desc(Post.create_time))
        .offset(offset).limit(page_size)
        .all()
    )
    data = [_build_post_response(p, current_user_id) for p in posts]
    return {"code": 200, "msg": "ok", "data": data, "total": total, "page": page, "page_size": page_size}


@router.get("/user/{user_id}", summary="获取指定用户的帖子列表")
async def get_user_posts(
    user_id: int,
    current_user_id: int = Query(0, description="当前登录用户ID"),
    page: int = Query(1, ge=1),
    page_size: int = Query(10, ge=1, le=50),
    db: Session = Depends(get_db)
):
    """获取指定用户发布的帖子（公开和仅好友）"""
    offset = (page - 1) * page_size
    total = db.query(Post).filter(Post.user_id == user_id, Post.status == 1, Post.visibility != 2).count()
    posts = (
        db.query(Post)
        .filter(Post.user_id == user_id, Post.status == 1, Post.visibility != 2)
        .order_by(desc(Post.create_time))
        .offset(offset).limit(page_size)
        .all()
    )
    data = [_build_post_response(p, current_user_id) for p in posts]
    return {"code": 200, "msg": "ok", "data": data, "total": total, "page": page, "page_size": page_size}


@router.get("/{post_id}", summary="获取帖子详情")
async def get_post_detail(
    post_id: int,
    current_user_id: int = Query(0, description="当前登录用户ID"),
    db: Session = Depends(get_db)
):
    """获取单条帖子详细信息"""
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在或已删除"}
    return {"code": 200, "msg": "ok", "data": _build_post_response(post, current_user_id)}


@router.put("/{post_id}", summary="编辑帖子")
async def update_post(
    post_id: int,
    data: PostUpdateRequest,
    user_id: int = Query(..., description="操作用户ID"),
    db: Session = Depends(get_db)
):
    """编辑帖子（仅允许帖子作者操作）"""
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在"}
    if post.user_id != user_id:
        return {"code": 403, "msg": "无权限操作该帖子"}

    if data.content is not None:
        post.content = data.content
    if data.location_name is not None:
        post.location_name = data.location_name
    if data.latitude is not None:
        post.latitude = data.latitude
    if data.longitude is not None:
        post.longitude = data.longitude
    if data.visibility is not None:
        post.visibility = data.visibility

    db.commit()
    logger.info(f"[编辑帖子] 帖子 {post_id} 已更新")
    return {"code": 200, "msg": "编辑成功"}


@router.delete("/{post_id}", summary="删除帖子")
async def delete_post(
    post_id: int,
    user_id: int = Query(..., description="操作用户ID"),
    db: Session = Depends(get_db)
):
    """删除帖子（软删除，仅允许帖子作者操作）"""
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在"}
    if post.user_id != user_id:
        return {"code": 403, "msg": "无权限删除该帖子"}

    post.status = 0
    db.commit()
    logger.info(f"[删除帖子] 帖子 {post_id} 已软删除, 操作用户 {user_id}")
    return {"code": 200, "msg": "删除成功"}