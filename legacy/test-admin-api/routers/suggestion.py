"""
模块：信箱（用户建议）
"""
import logging

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from database.database import get_db
from models.admin import UserSuggestion

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin", tags=["信箱"])


# ═══════════════════════════════════════════════════
# 1. 获取建议列表
# ═══════════════════════════════════════════════════

@router.get("/suggestions", summary="获取建议列表")
def list_suggestions(db: Session = Depends(get_db)):
    rows = db.query(UserSuggestion).order_by(UserSuggestion.created_at.desc()).all()
    items = [
        {
            "id": r.id,
            "user_name": r.user_name or "匿名用户",
            "content": r.content,
            "status": r.status,
            "created_at": r.created_at.isoformat() if r.created_at else None,
        }
        for r in rows
    ]
    # 前端兼容两种格式：顶层 items 和 data.suggestions
    return {"items": items, "data": {"suggestions": items}}


# ═══════════════════════════════════════════════════
# 2. 标记已读
# ═══════════════════════════════════════════════════

@router.put("/suggestions/{suggestion_id}/read", summary="标记已读")
def mark_read(suggestion_id: str, db: Session = Depends(get_db)):
    sg = db.query(UserSuggestion).filter(UserSuggestion.id == suggestion_id).first()
    if not sg:
        raise HTTPException(404, detail="建议不存在")
    sg.status = "read"
    db.commit()
    return {"code": 0, "message": "ok"}
