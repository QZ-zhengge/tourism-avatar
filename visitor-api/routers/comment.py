"""
驴友圈 - 评论路由模块
"""
import logging
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import desc

from database import get_db
from models.post import Post, PostComment
from models.user import User
from schemas.post import CommentCreateRequest

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/circle/comments", tags=["驴友圈-评论"])


def _get_username(db: Session, user_id: int) -> dict:
    """获取用户名和昵称"""
    user = db.query(User).filter(User.id == user_id).first()
    if user:
        return {"username": user.username, "nickname": user.nickname or user.username}
    return {"username": f"用户{user_id}", "nickname": f"用户{user_id}"}


def _build_comment(comment: PostComment, db: Session) -> dict:
    """构建单条评论数据"""
    user_info = _get_username(db, comment.user_id)
    return {
        "id": comment.id,
        "post_id": comment.post_id,
        "parent_id": comment.parent_id,
        "user_id": comment.user_id,
        "username": user_info["username"],
        "nickname": user_info["nickname"],
        "content": comment.content,
        "like_count": comment.like_count,
        "create_time": str(comment.create_time) if comment.create_time else None,
    }


@router.post("/", summary="发布评论或回复")
async def create_comment(data: CommentCreateRequest, db: Session = Depends(get_db)):
    """
    发布评论（或回复某条评论）
    - parent_id = null → 一级评论
    - parent_id = 某评论ID → 回复该评论
    """
    logger.info(f"[评论] 用户 {data.user_id} 评论帖子 {data.post_id}")

    post = db.query(Post).filter(Post.id == data.post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在"}

    # 校验父评论是否存在
    if data.parent_id:
        parent = db.query(PostComment).filter(
            PostComment.id == data.parent_id,
            PostComment.post_id == data.post_id,
            PostComment.status == 1
        ).first()
        if not parent:
            return {"code": 400, "msg": "回复的评论不存在"}

    comment = PostComment(
        post_id=data.post_id,
        parent_id=data.parent_id,
        user_id=data.user_id,
        content=data.content,
        like_count=0,
        status=1
    )
    db.add(comment)

    # 更新帖子评论数
    post.comment_count += 1
    db.commit()
    db.refresh(comment)

    logger.info(f"[评论成功] 评论ID: {comment.id}")
    return {"code": 200, "msg": "评论成功", "data": _build_comment(comment, db)}


@router.get("/{post_id}", summary="获取帖子评论列表（树形结构）")
async def get_post_comments(
    post_id: int,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """
    获取帖子的一级评论及其所有回复（树形结构）
    返回：一级评论列表，每条评论包含 replies 回复列表
    """
    post = db.query(Post).filter(Post.id == post_id, Post.status == 1).first()
    if not post:
        return {"code": 404, "msg": "帖子不存在"}

    offset = (page - 1) * page_size

    # 获取一级评论
    top_comments = (
        db.query(PostComment)
        .filter(
            PostComment.post_id == post_id,
            PostComment.parent_id == None,
            PostComment.status == 1
        )
        .order_by(PostComment.create_time)
        .offset(offset).limit(page_size)
        .all()
    )

    total = db.query(PostComment).filter(
        PostComment.post_id == post_id,
        PostComment.parent_id == None,
        PostComment.status == 1
    ).count()

    result = []
    for c in top_comments:
        comment_data = _build_comment(c, db)

        # 获取该评论的所有回复（子评论，不分页，最多100条）
        replies = (
            db.query(PostComment)
            .filter(
                PostComment.parent_id == c.id,
                PostComment.status == 1
            )
            .order_by(PostComment.create_time)
            .limit(100)
            .all()
        )
        comment_data["replies"] = [_build_comment(r, db) for r in replies]
        result.append(comment_data)

    return {
        "code": 200, "msg": "ok",
        "data": result, "total": total,
        "page": page, "page_size": page_size
    }


@router.delete("/{comment_id}", summary="删除评论")
async def delete_comment(
    comment_id: int,
    user_id: int = Query(..., description="操作用户ID"),
    db: Session = Depends(get_db)
):
    """删除自己的评论（软删除）"""
    comment = db.query(PostComment).filter(
        PostComment.id == comment_id,
        PostComment.status == 1
    ).first()
    if not comment:
        return {"code": 404, "msg": "评论不存在"}
    if comment.user_id != user_id:
        return {"code": 403, "msg": "无权限删除该评论"}

    comment.status = 0
    # 更新帖子评论数
    post = db.query(Post).filter(Post.id == comment.post_id).first()
    if post and post.comment_count > 0:
        post.comment_count -= 1

    db.commit()
    logger.info(f"[删除评论] 评论 {comment_id} 已软删除")
    return {"code": 200, "msg": "删除成功"}


@router.get("/user/{user_id}", summary="获取我发过的评论列表（我的评论）")
async def my_comments(
    user_id: int,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """
    按 user_id 反查该用户发过的所有评论（仅正常状态），按时间倒序。
    返回评论内容、所属帖子信息，以及若为回复时的父评论内容，便于「我的评论」页展示与跳转。
    """
    offset = (page - 1) * page_size
    comments = (
        db.query(PostComment)
        .filter(PostComment.user_id == user_id, PostComment.status == 1)
        .order_by(desc(PostComment.create_time))
        .offset(offset).limit(page_size)
        .all()
    )

    post_ids = [c.post_id for c in comments]
    posts = (
        {p.id: p for p in db.query(Post).filter(Post.id.in_(post_ids)).all()}
        if post_ids else {}
    )

    data = []
    for c in comments:
        p = posts.get(c.post_id)
        parent_content = None
        if c.parent_id:
            parent = db.query(PostComment).filter(PostComment.id == c.parent_id).first()
            parent_content = parent.content if parent else None
        data.append({
            "comment_id": c.id,
            "post_id": c.post_id,
            "content": c.content,
            "like_count": c.like_count,
            "create_time": str(c.create_time),
            "parent_id": c.parent_id,
            "parent_content": parent_content,
            "post": {
                "id": p.id if p else c.post_id,
                "content": p.content if p else None,
                "location_name": p.location_name if p else None,
            },
        })

    total = db.query(PostComment).filter(
        PostComment.user_id == user_id, PostComment.status == 1
    ).count()
    return {
        "code": 200, "msg": "ok",
        "data": data, "total": total,
        "page": page, "page_size": page_size,
    }