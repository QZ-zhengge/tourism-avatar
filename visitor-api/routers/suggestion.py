"""
个人中心 - 建议信箱路由（游客端写入，连通管理端信箱）
"""
import logging
import uuid
from typing import Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from database import get_db
from models.suggestion import UserSuggestion
from models.user import User
from schemas.suggestion import SuggestionCreateRequest

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/suggestions", tags=["建议信箱"])


def _resolve_username(db: Session, user_id: int, fallback: Optional[str]) -> str:
    """解析展示用昵称：优先用传入 user_name，否则从 users 表查昵称/用户名"""
    if fallback:
        return fallback
    u = db.query(User).filter(User.id == user_id).first()
    return (u.nickname or u.username) if u else "匿名用户"


@router.post("/", summary="提交建议（游客端）")
async def create_suggestion(data: SuggestionCreateRequest, db: Session = Depends(get_db)):
    """
    游客端提交一条建议/反馈，写入 user_suggestions 表（与管理端信箱同一张表）。
    - user_id：必填，用于「我的建议」列表归属
    - content：必填，建议正文
    - contact：可选，联系方式
    - user_name：可选，展示昵称（缺省自动按 user_id 解析或匿名）
    """
    suggestion_id = "sg_" + uuid.uuid4().hex[:14]
    user_name = _resolve_username(db, data.user_id, data.user_name)

    sg = UserSuggestion(
        id=suggestion_id,
        user_id=data.user_id,
        user_name=user_name,
        contact=data.contact,
        content=data.content,
        status="unread",
    )
    db.add(sg)
    db.commit()
    db.refresh(sg)
    logger.info(f"[建议] 用户 {data.user_id} 提交建议 {sg.id}")

    return {
        "code": 200,
        "msg": "提交成功",
        "data": {
            "id": sg.id,
            "user_id": sg.user_id,
            "user_name": sg.user_name,
            "content": sg.content,
            "status": sg.status,
            "created_at": str(sg.created_at),
        },
    }


@router.get("/user/{user_id}", summary="获取我的建议列表（游客端）")
async def my_suggestions(
    user_id: int,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """按 user_id 反查该用户提交的所有建议，按时间倒序"""
    offset = (page - 1) * page_size
    query = db.query(UserSuggestion).filter(UserSuggestion.user_id == user_id)
    total = query.count()
    rows = (
        query.order_by(UserSuggestion.created_at.desc())
        .offset(offset).limit(page_size).all()
    )
    items = [
        {
            "id": r.id,
            "user_id": r.user_id,
            "content": r.content,
            "contact": r.contact,
            "status": r.status,
            "created_at": r.created_at.isoformat() if r.created_at else None,
        }
        for r in rows
    ]
    return {
        "code": 200, "msg": "ok",
        "data": items, "total": total,
        "page": page, "page_size": page_size,
    }
