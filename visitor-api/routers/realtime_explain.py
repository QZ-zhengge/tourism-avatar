"""
实时讲解历史路由（数字人对话历史的分支功能）

接口：
- POST /realtime-explain/history                      添加一条实时讲解历史（前端传 image + ai_response，原样存）
- GET  /realtime-explain/history/user/{user_id}       按用户返回历史列表（原样返回，分页）

设计要点：
- image / ai_response 两个字段均由前端主动传入，后端不做任何图像解码或 JSON 解析，
  仅落库、供后续原样返回。
- 适合「前端调用 AI（如视觉模型）得到讲解结果，再把图片 + 结果交后端存档」的场景。
"""
import logging

from fastapi import APIRouter, Depends, Body, Query
from sqlalchemy.orm import Session

from database import get_db
from models.realtime_explain import RealtimeExplainHistory
from schemas.realtime_explain import (
    RealtimeExplainAddRequest,
    RealtimeExplainAddResponse,
    RealtimeExplainItem,
    RealtimeExplainListData,
    RealtimeExplainListResponse,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/realtime-explain", tags=["实时讲解历史"])


@router.post("/history", summary="添加一条实时讲解历史（原样存储）")
async def add_realtime_explain(
    data: RealtimeExplainAddRequest = Body(...),
    db: Session = Depends(get_db),
):
    """
    前端在「实时讲解」结束后调用。
    - image：前端上传的图片（base64 或 URL），后端**原样存储，不做任何分析**
    - ai_response：AI 返回的含 JSON 文本，后端**原样存储，不解析**
    两个字段都由前端主动传入，后端仅落库并返回。
    """
    # 归一化 user_id（去首尾空白，None 保持 None），与查询逻辑保持一致，
    # 避免前后端因 user_id 携带空白导致「存得进、查不出」
    uid_norm = data.user_id.strip() if isinstance(data.user_id, str) else data.user_id
    rec = RealtimeExplainHistory(
        user_id=uid_norm,
        image=data.image,
        ai_response=data.ai_response,
        scenic_id=data.scenic_id,
    )
    db.add(rec)
    try:
        db.commit()
    except Exception as e:
        db.rollback()
        logger.error(f"[实时讲解历史] 写入失败 user_id={uid_norm}: {e}")
        return RealtimeExplainAddResponse(
            code=500,
            msg=f"保存失败：{str(e)[:200]}",
        )
    db.refresh(rec)
    logger.info(f"[实时讲解历史] 用户 {uid_norm} 新增一条记录 id={rec.id}")

    return RealtimeExplainAddResponse(
        code=200,
        msg="ok",
        data=RealtimeExplainItem(
            id=rec.id,
            user_id=rec.user_id,
            image=rec.image,
            ai_response=rec.ai_response,
            scenic_id=rec.scenic_id,
            created_at=rec.created_at.isoformat() if rec.created_at else None,
        ),
    )


@router.get("/history/user/{user_id}", summary="获取某用户的实时讲解历史列表")
async def list_realtime_explain(
    user_id: str,
    page: int = Query(1, ge=1),
    page_size: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
):
    """
    返回该用户的全部实时讲解历史（按时间倒序），原样返回 image / ai_response。
    前端据此渲染历史列表。
    """
    # 归一化 user_id（去首尾空白），与保存逻辑保持一致
    uid = (user_id or "").strip()
    total = db.query(RealtimeExplainHistory).filter(
        RealtimeExplainHistory.user_id == uid
    ).count()
    rows = (
        db.query(RealtimeExplainHistory)
        .filter(RealtimeExplainHistory.user_id == uid)
        .order_by(RealtimeExplainHistory.created_at.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )
    items = [
        RealtimeExplainItem(
            id=r.id,
            user_id=r.user_id,
            image=r.image,
            ai_response=r.ai_response,
            scenic_id=r.scenic_id,
            created_at=r.created_at.isoformat() if r.created_at else None,
        )
        for r in rows
    ]
    return RealtimeExplainListResponse(
        code=200,
        msg="ok",
        data=RealtimeExplainListData(
            total=total, page=page, page_size=page_size, items=items
        ),
    )
