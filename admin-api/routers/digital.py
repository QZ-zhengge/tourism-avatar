"""
模块一：数字人配置管理
"""
import os
import base64
import logging
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, Query, HTTPException
from sqlalchemy.orm import Session

from database.database import get_db
from models.admin import DigitalHumanConfig, AvatarCatalog
from schemas.admin import (
    DigitalConfigRequest, DigitalConfigResponse, BaseResponse,
    VALID_VCNS,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin/digital", tags=["数字人管理"])

AVATAR_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads", "avatars")
os.makedirs(AVATAR_DIR, exist_ok=True)


def _load_avatar_base64(avatar_id: str) -> str | None:
    """读取头像图片并返回 base64 data URI，文件不存在则返回 None"""
    for ext in (".png", ".jpg", ".jpeg", ".webp"):
        path = os.path.join(AVATAR_DIR, f"{avatar_id}{ext}")
        if os.path.exists(path):
            with open(path, "rb") as f:
                data = base64.b64encode(f.read()).decode("utf-8")
            mime = "image/png" if ext == ".png" else "image/jpeg" if ext in (".jpg", ".jpeg") else "image/webp"
            return f"data:{mime};base64,{data}"
    return None


# ═══════════════════════════════════════════════════
# 数字人形象列表（含 base64 头像）
# ═══════════════════════════════════════════════════

@router.get("/avatars", summary="获取数字人形象列表（含头像）")
def get_avatars(db: Session = Depends(get_db)):
    """返回数字人形象目录（avatar_catalog 表）中的全部形象，每人附带 base64 头像"""
    rows = db.query(AvatarCatalog).order_by(AvatarCatalog.id).all()
    items = []
    for r in rows:
        items.append({
            "avatar_id": r.avatar_id,
            "name": r.name,
            "category": r.category,
            "status": r.status,
            "avatar_base64": _load_avatar_base64(r.avatar_id),
        })
    return {"code": 200, "data": items}


def _avatar_name(avatar_id: str, db: Session) -> str:
    """从数字人形象目录表查形象名称，查不到则返回原始 ID"""
    row = db.query(AvatarCatalog).filter(AvatarCatalog.avatar_id == avatar_id).first()
    return row.name if row else avatar_id


# ═══════════════════════════════════════════════════
# 获取 / 保存配置
# ═══════════════════════════════════════════════════

_DEFAULT_AVATAR_ID = "111260002"  # 管理端未配置时的兜底形象（仅降级用）

@router.get("/config", summary="获取数字人配置")
def get_config(
    scenic_id: str = Query("lingshan"),
    db: Session = Depends(get_db),
):
    cfg = db.query(DigitalHumanConfig).filter(
        DigitalHumanConfig.scenic_id == scenic_id
    ).first()
    if not cfg:
        return {
            "scenic_id": scenic_id,
            "avatar_id": _DEFAULT_AVATAR_ID,
            "avatar_name": _avatar_name(_DEFAULT_AVATAR_ID, db),
            "avatar_base64": _load_avatar_base64(_DEFAULT_AVATAR_ID),
            "vcn": "x4_xiaoxuan",
            "speed": 50, "pitch": 50, "volume": 50,
            "service_id": "315837983779590144",
            "layout": "fullscreen", "display_mode": "full",
            "sync_status": "saved",
        }
    return {
        "scenic_id": cfg.scenic_id,
        "avatar_id": cfg.avatar_id,
        "avatar_name": _avatar_name(cfg.avatar_id, db),
        "avatar_base64": _load_avatar_base64(cfg.avatar_id),
        "vcn": cfg.vcn,
        "speed": cfg.speed,
        "pitch": cfg.pitch,
        "volume": cfg.volume,
        "service_id": cfg.service_id,
        "layout": cfg.layout,
        "display_mode": cfg.display_mode,
        "sync_status": cfg.sync_status or "saved",
        "updated_at": cfg.updated_at.isoformat() if cfg.updated_at else None,
    }


@router.post("/config", summary="保存数字人配置")
def save_config(req: DigitalConfigRequest, db: Session = Depends(get_db)):
    # avatar_id 校验：必须是数字人形象目录(avatar_catalog)中的有效 ID
    catalog_row = db.query(AvatarCatalog).filter(
        AvatarCatalog.avatar_id == req.avatar_id
    ).first()
    if not catalog_row:
        raise HTTPException(400, detail=f"avatar_id 无效，请使用数字人形象目录中的有效 ID（共 {db.query(AvatarCatalog).count()} 个）")
    if req.vcn not in VALID_VCNS:
        logger.warning(f"vcn={req.vcn} 不在预定义列表中，仍然保存")

    cfg = db.query(DigitalHumanConfig).filter(
        DigitalHumanConfig.scenic_id == req.scenic_id
    ).first()

    now = datetime.now(timezone.utc)
    if cfg:
        cfg.avatar_id = req.avatar_id
        cfg.vcn = req.vcn
        cfg.speed = req.speed
        cfg.pitch = req.pitch
        cfg.volume = req.volume
        cfg.service_id = req.service_id
        cfg.layout = req.layout
        cfg.display_mode = req.display_mode
        cfg.sync_status = "saving"
        cfg.updated_at = now
    else:
        cfg = DigitalHumanConfig(
            scenic_id=req.scenic_id,
            avatar_id=req.avatar_id, vcn=req.vcn,
            speed=req.speed, pitch=req.pitch, volume=req.volume,
            service_id=req.service_id,
            layout=req.layout, display_mode=req.display_mode,
            sync_status="saving", updated_at=now,
        )
        db.add(cfg)

    db.commit()
    db.refresh(cfg)
    cfg.sync_status = "saved"
    db.commit()

    return BaseResponse(
        code=200, message="配置保存成功",
        data={"saved_at": now.isoformat(), "sync_status": "saved"},
    )
