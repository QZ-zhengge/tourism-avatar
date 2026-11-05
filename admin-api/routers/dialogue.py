"""
模块四：对话分析 — 数字人对话历史（读取游客端写入的 avatar_chat_* 表）

【读取来源】
  游客端（主项目）在 /avatar/start、/avatar/drive/text、/avatar/stop 三个埋点
  把对话历史写入 avatar_chat_sessions / avatar_chat_messages 两张新表。
  本模块只做只读查询，原 dialogue_sessions / dialogue_messages 表完全不动。

【接口】
  GET /api/v1/admin/dialogue/sessions                  会话列表（分页 + 搜索）
  GET /api/v1/admin/dialogue/sessions/{id}/messages    单会话消息详情
"""
import logging
from typing import Optional

from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session

from database.database import get_db
from models.admin import AvatarChatSession, AvatarChatMessage, AvatarCatalog
from schemas.admin import (
    AvatarChatListResponse, AvatarChatSessionItem,
    AvatarChatDetailResponse, AvatarChatMessageItem,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin/dialogue", tags=["对话分析"])


def _avatar_name(db: Session, avatar_id: Optional[str]) -> Optional[str]:
    if not avatar_id:
        return None
    return db.query(AvatarCatalog.name).filter(
        AvatarCatalog.avatar_id == avatar_id
    ).scalar()


@router.get("/sessions", summary="获取数字人对话历史会话列表")
def list_sessions(
    scenic_id: str = Query("lingshan"),
    search: str = Query(""),
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
):
    q = db.query(AvatarChatSession).filter(AvatarChatSession.scenic_id == scenic_id)
    if search:
        q = q.filter(
            (AvatarChatSession.session_id.like(f"%{search}%")) |
            (AvatarChatSession.user_id.like(f"%{search}%")) |
            (AvatarChatSession.avatar_id.like(f"%{search}%"))
        )

    total = q.count()
    sessions = q.order_by(AvatarChatSession.started_at.desc()).offset(
        (page - 1) * page_size
    ).limit(page_size).all()

    items = [
        AvatarChatSessionItem(
            session_id=s.session_id,
            user_id=s.user_id,
            scenic_id=s.scenic_id,
            avatar_id=s.avatar_id,
            avatar_name=_avatar_name(db, s.avatar_id),
            message_count=s.message_count or 0,
            started_at=s.started_at.isoformat() if s.started_at else None,
            ended_at=s.ended_at.isoformat() if s.ended_at else None,
        )
        for s in sessions
    ]
    return {
        "code": 200,
        "data": AvatarChatListResponse(
            total=total, page=page, page_size=page_size, items=items,
        ).model_dump(),
    }


@router.get("/sessions/{session_id}/messages", summary="获取会话消息详情")
def get_session_messages(session_id: str, db: Session = Depends(get_db)):
    session = db.query(AvatarChatSession).filter(
        AvatarChatSession.session_id == session_id
    ).first()
    if not session:
        raise HTTPException(404, detail="会话不存在")

    messages = db.query(AvatarChatMessage).filter(
        AvatarChatMessage.session_id == session_id
    ).order_by(AvatarChatMessage.seq_no).all()

    msg_items = [
        AvatarChatMessageItem(
            seq_no=m.seq_no,
            role=m.role,
            content=m.content,
            source=m.source,
            created_at=m.created_at.isoformat() if m.created_at else None,
        )
        for m in messages
    ]
    return {
        "code": 200,
        "data": AvatarChatDetailResponse(
            session_id=session.session_id,
            user_id=session.user_id,
            scenic_id=session.scenic_id,
            avatar_id=session.avatar_id,
            avatar_name=_avatar_name(db, session.avatar_id),
            message_count=session.message_count or 0,
            started_at=session.started_at.isoformat() if session.started_at else None,
            ended_at=session.ended_at.isoformat() if session.ended_at else None,
            messages=msg_items,
        ).model_dump(),
    }
