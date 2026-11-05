"""
模块二：内容管理 — 景点图片上传/列表/删除
"""
import os
import uuid
import logging
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, UploadFile, File, Form, Query, HTTPException
from sqlalchemy.orm import Session

from database.database import get_db
from models.admin import ScenicImage
from schemas.admin import BaseResponse, ImageUploadResponse, ImageListItem

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin/content", tags=["内容管理"])

IMAGES_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads", "images")
os.makedirs(IMAGES_DIR, exist_ok=True)

ALLOWED_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"}


@router.post("/image/upload", summary="上传景点图片")
async def upload_image(
    spot_name: str = Form(...),
    scenic_id: str = Form("lingshan"),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
):
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(400, detail=f"不支持的图片类型: {file.content_type}")

    content = await file.read()
    file_size = len(content)

    ext = os.path.splitext(file.filename or "image.jpg")[1] or ".jpg"
    saved_name = f"{datetime.now().strftime('%Y%m%d%H%M%S')}_{uuid.uuid4().hex[:8]}{ext}"
    saved_path = os.path.join(IMAGES_DIR, saved_name)
    with open(saved_path, "wb") as f:
        f.write(content)

    img = ScenicImage(
        scenic_id=scenic_id,
        spot_name=spot_name,
        file_name=file.filename or saved_name,
        file_path=saved_name,
        file_size=file_size,
    )
    db.add(img)
    db.commit()
    db.refresh(img)

    return BaseResponse(
        code=200, message="上传成功",
        data=ImageUploadResponse(
            id=img.id,
            spot_name=img.spot_name,
            file_name=img.file_name,
            file_url=f"/api/v1/admin/uploads/images/{img.file_path}",
            file_size=img.file_size,
            created_at=img.created_at.isoformat() if img.created_at else None,
        ).model_dump(),
    )


@router.get("/images", summary="获取图片列表")
def list_images(
    scenic_id: str = Query("lingshan"),
    db: Session = Depends(get_db),
):
    images = db.query(ScenicImage).filter(
        ScenicImage.scenic_id == scenic_id
    ).order_by(ScenicImage.id.desc()).all()

    items = [
        {
            "id": i.id,
            "spot_name": i.spot_name,
            "file_name": i.file_name,
            "file_url": f"/api/v1/admin/uploads/images/{i.file_path}",
            "file_size": i.file_size,
            "created_at": i.created_at.isoformat() if i.created_at else None,
        }
        for i in images
    ]
    return {"code": 200, "data": items}


@router.delete("/image/{image_id}", summary="删除图片")
def delete_image(image_id: int, db: Session = Depends(get_db)):
    img = db.query(ScenicImage).filter(ScenicImage.id == image_id).first()
    if not img:
        raise HTTPException(404, detail="图片不存在")
    fp = os.path.join(IMAGES_DIR, img.file_path)
    if os.path.exists(fp):
        os.remove(fp)
    db.delete(img)
    db.commit()
    return BaseResponse(code=200, message="删除成功")
