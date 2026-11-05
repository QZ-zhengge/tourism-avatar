"""
模块：发音人管理 — 列表、增删、试听音频
"""
import os
import base64
import uuid
import logging
from datetime import datetime

from fastapi import APIRouter, Depends, UploadFile, File, Form, Query, HTTPException
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from database.database import get_db
from models.admin import VcnVoice
from schemas.admin import BaseResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin/vcn", tags=["发音人管理"])

AUDIO_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads", "vcns")
os.makedirs(AUDIO_DIR, exist_ok=True)


def _load_audio_base64(vcn_id: str) -> str | None:
    """读取试听音频并返回 base64 data URI"""
    for ext in (".mp3", ".wav", ".ogg", ".m4a"):
        path = os.path.join(AUDIO_DIR, f"{vcn_id}{ext}")
        if os.path.exists(path):
            with open(path, "rb") as f:
                data = base64.b64encode(f.read()).decode("utf-8")
            mime_map = {".mp3": "audio/mpeg", ".wav": "audio/wav", ".ogg": "audio/ogg", ".m4a": "audio/mp4"}
            return f"data:{mime_map.get(ext, 'audio/mpeg')};base64,{data}"
    return None


# ═══════════════════════════════════════════════════
# 1. 发音人列表（含试听 base64）
# ═══════════════════════════════════════════════════

@router.get("/voices", summary="获取发音人列表（含试听音频）")
def list_voices(db: Session = Depends(get_db)):
    voices = db.query(VcnVoice).order_by(VcnVoice.id).all()
    items = [
        {
            "id": v.id,
            "vcn": v.vcn,
            "name": v.name,
            "gender": v.gender,
            "category": v.category,
            "language": v.language,
            "audio_base64": _load_audio_base64(v.vcn),
        }
        for v in voices
    ]
    return {"code": 200, "data": items}


# ═══════════════════════════════════════════════════
# 2. 添加发音人
# ═══════════════════════════════════════════════════

@router.post("/voices", summary="添加发音人")
def add_voice(
    vcn: str = Form(...),
    name: str = Form(...),
    gender: str = Form(""),
    category: str = Form(""),
    language: str = Form(""),
    db: Session = Depends(get_db),
):
    exist = db.query(VcnVoice).filter(VcnVoice.vcn == vcn).first()
    if exist:
        raise HTTPException(400, detail=f"发音人 {vcn} 已存在")

    voice = VcnVoice(vcn=vcn, name=name, gender=gender, category=category, language=language)
    db.add(voice)
    db.commit()
    db.refresh(voice)

    logger.info(f"[VCN] 添加发音人: {voice.vcn} - {voice.name}")
    return BaseResponse(code=200, message="添加成功", data={
        "id": voice.id, "vcn": voice.vcn, "name": voice.name,
    })


# ═══════════════════════════════════════════════════
# 3. 上传试听音频（命名规则：{vcn}.mp3）
# ═══════════════════════════════════════════════════

@router.post("/voices/{vcn}/audio", summary="上传试听音频")
async def upload_audio(
    vcn: str,
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
):
    voice = db.query(VcnVoice).filter(VcnVoice.vcn == vcn).first()
    if not voice:
        raise HTTPException(404, detail=f"发音人 {vcn} 不存在，请先添加")

    ext = os.path.splitext(file.filename or "audio.mp3")[1].lower()
    if ext not in {".mp3", ".wav", ".ogg", ".m4a"}:
        raise HTTPException(400, detail="仅支持 .mp3 / .wav / .ogg / .m4a 格式")

    content = await file.read()
    saved_name = f"{vcn}{ext}"
    saved_path = os.path.join(AUDIO_DIR, saved_name)
    with open(saved_path, "wb") as f:
        f.write(content)

    voice.audio_path = saved_name
    db.commit()

    logger.info(f"[VCN] 上传试听音频: {vcn}{ext} ({len(content)} bytes)")
    return BaseResponse(code=200, message="试听音频上传成功")


# ═══════════════════════════════════════════════════
# 4. 删除发音人
# ═══════════════════════════════════════════════════

@router.delete("/voices/{voice_id}", summary="删除发音人")
def delete_voice(voice_id: int, db: Session = Depends(get_db)):
    voice = db.query(VcnVoice).filter(VcnVoice.id == voice_id).first()
    if not voice:
        raise HTTPException(404, detail="发音人不存在")

    # 删除试听音频文件
    if voice.audio_path:
        fp = os.path.join(AUDIO_DIR, voice.audio_path)
        if os.path.exists(fp):
            os.remove(fp)
    # 同时清理其他可能的扩展名
    for ext in (".mp3", ".wav", ".ogg", ".m4a"):
        fp = os.path.join(AUDIO_DIR, f"{voice.vcn}{ext}")
        if os.path.exists(fp):
            os.remove(fp)

    db.delete(voice)
    db.commit()
    return BaseResponse(code=200, message="删除成功")
