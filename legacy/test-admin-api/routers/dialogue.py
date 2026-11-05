"""
模块四：对话分析 — 会话列表 / 消息详情
"""
import logging

from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session
from sqlalchemy import func as sa_func

from database.database import get_db
from models.admin import DialogueSession, DialogueMessage
from schemas.admin import (
    BaseResponse, DialogueListResponse, DialogueSessionItem,
    DialogueDetailResponse, DialogueMessageItem,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin/dialogue", tags=["对话分析"])


@router.get("/sessions", summary="获取会话列表")
def list_sessions(
    scenic_id: str = Query("lingshan"),
    search: str = Query(""),
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
):
    q = db.query(DialogueSession).filter(DialogueSession.scenic_id == scenic_id)
    if search:
        q = q.filter(
            (DialogueSession.session_id.like(f"%{search}%")) |
            (DialogueSession.user_name.like(f"%{search}%"))
        )

    total = q.count()
    sessions = q.order_by(DialogueSession.last_updated.desc()).offset(
        (page - 1) * page_size
    ).limit(page_size).all()

    items = [
        DialogueSessionItem(
            session_id=s.session_id,
            user_name=s.user_name,
            satisfaction_score=s.satisfaction_score or 0,
            message_count=s.message_count or 0,
            categories=s.categories if s.categories else None,
            last_updated=s.last_updated.isoformat() if s.last_updated else None,
            reason=s.reason,
            suggestion=s.suggestion,
        )
        for s in sessions
    ]
    return {"code": 200, "data": DialogueListResponse(
        total=total, page=page, page_size=page_size, items=items,
    ).model_dump()}


@router.get("/sessions/{session_id}/messages", summary="获取会话消息详情")
def get_session_messages(session_id: str, db: Session = Depends(get_db)):
    session = db.query(DialogueSession).filter(
        DialogueSession.session_id == session_id
    ).first()
    if not session:
        raise HTTPException(404, detail="会话不存在")

    messages = db.query(DialogueMessage).filter(
        DialogueMessage.session_id == session_id
    ).order_by(DialogueMessage.seq_no).all()

    msg_items = [
        DialogueMessageItem(
            seq_no=m.seq_no,
            role=m.role,
            content=m.content,
            sentiment=m.sentiment,
            created_at=m.created_at.isoformat() if m.created_at else None,
        )
        for m in messages
    ]
    return {"code": 200, "data": DialogueDetailResponse(
        session_id=session.session_id,
        satisfaction_score=session.satisfaction_score or 0,
        reason=session.reason,
        suggestion=session.suggestion,
        messages=msg_items,
    ).model_dump()}
