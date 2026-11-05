"""
驴友圈 - 点赞路由模块
"""
import logging
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from database import get_db
from models.post import Post, PostLike, PostComment
from schemas.post import LikeRequest

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/circle/likes", tags=["驴友圈-点赞"])


@router.post("/post", summary="帖子点赞/取消点赞")
async def toggle_post_like(data: LikeRequest, db: Session = Depends(get_db)):
    """
    点赞或取消点赞帖子（toggle逻辑）
    - 未点赞 → 点赞，like_count +1
    - 已点赞 → 取消，like_count -1
    """
    logger.info(f"[点赞] 用户 {data.user_id} 操作帖子 {data.post_id}")

    post = db.query(Post).filter(Post.id == data.post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在"}

    existing = db.query(PostLike).filter(
        PostLike.post_id == data.post_id,
        PostLike.user_id == data.user_id
    ).first()

    if existing:
        # 已点赞 → 取消点赞
        db.delete(existing)
        if post.like_count > 0:
            post.like_count -= 1
        db.commit()
        logger.info(f"[取消点赞] 用户 {data.user_id} 取消帖子 {data.post_id} 点赞，当前数: {post.like_count}")
        return {"code": 200, "msg": "已取消点赞", "is_liked": False, "like_count": post.like_count}
    else:
        # 未点赞 → 点赞
        try:
            like = PostLike(post_id=data.post_id, user_id=data.user_id)
            db.add(like)
            post.like_count += 1
            db.commit()
            logger.info(f"[点赞] 用户 {data.user_id} 点赞帖子 {data.post_id}，当前数: {post.like_count}")
            return {"code": 200, "msg": "点赞成功", "is_liked": True, "like_count": post.like_count}
        except IntegrityError:
            db.rollback()
            return {"code": 400, "msg": "操作失败，请勿重复点赞"}


@router.get("/post/{post_id}", summary="获取帖子点赞用户列表")
async def get_post_likes(
    post_id: int,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """获取指定帖子的点赞用户列表"""
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在"}

    offset = (page - 1) * page_size
    likes = (
        db.query(PostLike)
        .filter(PostLike.post_id == post_id)
        .offset(offset).limit(page_size)
        .all()
    )
    data = [{"user_id": like.user_id, "like_time": str(like.create_time)} for like in likes]
    return {"code": 200, "msg": "ok", "data": data, "total": post.like_count}


@router.post("/comment", summary="评论点赞/取消点赞")
async def toggle_comment_like(
    comment_id: int = Query(..., description="评论ID"),
    user_id: int = Query(..., description="用户ID"),
    db: Session = Depends(get_db)
):
    """对评论点赞或取消点赞"""
    comment = db.query(PostComment).filter(PostComment.id == comment_id, PostComment.status == 1).first()
    if not comment:
        return {"code": 404, "msg": "评论不存在"}

    # 简单 toggle：用 like_count 字段，实际可扩展 comment_likes 独立表
    # 此处演示直接增减（生产环境建议用独立表防重复）
    comment.like_count += 1
    db.commit()
    logger.info(f"[评论点赞] 用户 {user_id} 点赞评论 {comment_id}")
    return {"code": 200, "msg": "点赞成功", "like_count": comment.like_count}


@router.get("/check", summary="查询用户是否点赞")
async def check_like_status(
    post_id: int = Query(..., description="帖子ID"),
    user_id: int = Query(..., description="用户ID"),
    db: Session = Depends(get_db)
):
    """查询当前用户是否已对该帖子点赞"""
    existing = db.query(PostLike).filter(
        PostLike.post_id == post_id,
        PostLike.user_id == user_id
    ).first()
    return {"code": 200, "msg": "ok", "is_liked": existing is not None}