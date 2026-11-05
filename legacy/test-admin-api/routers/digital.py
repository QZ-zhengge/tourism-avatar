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
from models.admin import DigitalHumanConfig
from schemas.admin import (
    DigitalConfigRequest, DigitalConfigResponse, BaseResponse,
    VALID_AVATARS, VALID_VCNS,
)

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin/digital", tags=["数字人管理"])

AVATAR_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads", "avatars")
os.makedirs(AVATAR_DIR, exist_ok=True)

# 数字人形象信息（前端展示用）
AVATAR_INFO = {
    "118801001": {"name": "依丹",             "gender": "女"},
    "110026010": {"name": "伊凡",             "gender": "男"},
    "110022010": {"name": "晓云",             "gender": "女"},
    "110017006": {"name": "马可",             "gender": "男"},
    "110005018": {"name": "知新",             "gender": "男"},
    "110029003": {"name": "晓姿(粉色衬衫)",   "gender": "女"},
    "110029002": {"name": "晓姿(白色西装)",   "gender": "女"},
    "110021007": {"name": "晓娴(黑裙)",       "gender": "女"},
    "110021006": {"name": "晓娴(灰色西装)",   "gender": "女"},
    "110026013": {"name": "伊凡(蓝色西装)",   "gender": "男"},
    "110023004": {"name": "小雅",             "gender": "女"},
    "111260002": {"name": "雨菲",             "gender": "女"},
}


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
def get_avatars():
    """返回 12 个形象，每人附带 base64 头像"""
    items = []
    for aid in VALID_AVATARS:
        info = AVATAR_INFO.get(aid, {"name": aid, "gender": "未知"})
        items.append({
            "avatar_id": aid,
            "name": info["name"],
            "gender": info["gender"],
            "avatar_base64": _load_avatar_base64(aid),
        })
    return {"code": 200, "data": items}


# ═══════════════════════════════════════════════════
# 获取 / 保存配置
# ═══════════════════════════════════════════════════

@router.get("/config", summary="获取数字人配置")
def get_config(
    scenic_id: str = Query("lingshan"),
    db: Session = Depends(get_db),
):
    cfg = db.query(DigitalHumanConfig).filter(
        DigitalHumanConfig.scenic_id == scenic_id
    ).first()
    if not cfg:
        ad = AVATAR_INFO.get("111260002", {"name": "111260002"})
        return {
            "scenic_id": scenic_id,
            "avatar_id": "111260002",
            "avatar_name": ad["name"],
            "avatar_base64": _load_avatar_base64("111260002"),
            "vcn": "x4_xiaoxuan",
            "speed": 50, "pitch": 50, "volume": 50,
            "service_id": "315837983779590144",
            "layout": "fullscreen", "display_mode": "full",
            "sync_status": "saved",
        }
    ad = AVATAR_INFO.get(cfg.avatar_id, {"name": cfg.avatar_id})
    return {
        "scenic_id": cfg.scenic_id,
        "avatar_id": cfg.avatar_id,
        "avatar_name": ad["name"],
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
    if req.avatar_id not in VALID_AVATARS:
        raise HTTPException(400, detail=f"avatar_id 无效，可选值: {VALID_AVATARS}")
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
