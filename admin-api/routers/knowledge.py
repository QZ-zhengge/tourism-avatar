"""
模块三：知识库 — 源文件管理（增删查、列表、内容展示）
"""
import os
import uuid
import logging
from datetime import datetime

from fastapi import APIRouter, Depends, UploadFile, File, Form, Query, HTTPException
from fastapi.responses import FileResponse, Response
from sqlalchemy.orm import Session
from sqlalchemy import func as sa_func

from database.database import get_db
from models.admin import KnowledgeDocument
from schemas.admin import BaseResponse

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/api/v1/admin/knowledge", tags=["知识库"])

DOC_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads", "documents")
os.makedirs(DOC_DIR, exist_ok=True)

ALLOWED_EXTS = {".txt", ".md", ".docx", ".pdf"}
CONTENT_TYPES = {
    "txt":  "text/plain; charset=utf-8",
    "md":   "text/plain; charset=utf-8",
    "pdf":  "application/pdf",
    "docx": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
}

# ═══════════════════════════════════════════════════
# 1. 文件列表（分页 + 搜索）
# ═══════════════════════════════════════════════════

@router.get("/files", summary="获取源文件列表")
def list_files(
    scenic_id: str = Query("lingshan"),
    search: str = Query("", description="按文件名模糊搜索"),
    file_ext: str = Query("", description="格式筛选: txt/md/docx/pdf"),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=200),
    db: Session = Depends(get_db),
):
    q = db.query(KnowledgeDocument).filter(KnowledgeDocument.scenic_id == scenic_id)
    if search:
        q = q.filter(KnowledgeDocument.name.like(f"%{search}%"))
    if file_ext:
        q = q.filter(KnowledgeDocument.file_ext == file_ext)

    total = q.count()
    docs = q.order_by(KnowledgeDocument.id.desc()).offset(
        (page - 1) * page_size
    ).limit(page_size).all()

    items = [
        {
            "id": d.id,
            "name": d.name,
            "file_size": d.file_size,
            "file_ext": d.file_ext,
            "created_at": d.created_at.isoformat() if d.created_at else None,
        }
        for d in docs
    ]
    return {
        "code": 200,
        "data": {"total": total, "page": page, "page_size": page_size, "items": items},
    }


# ═══════════════════════════════════════════════════
# 2. 获取单个文件元数据
# ═══════════════════════════════════════════════════

@router.get("/files/{file_id}", summary="获取文件元数据")
def get_file(file_id: int, db: Session = Depends(get_db)):
    doc = db.query(KnowledgeDocument).filter(KnowledgeDocument.id == file_id).first()
    if not doc:
        raise HTTPException(404, detail="文件不存在")
    return {
        "code": 200,
        "data": {
            "id": doc.id,
            "name": doc.name,
            "file_size": doc.file_size,
            "file_ext": doc.file_ext,
            "created_at": doc.created_at.isoformat() if doc.created_at else None,
        },
    }


# ═══════════════════════════════════════════════════
# 3. 获取文件内容（前端页面展示用）
# ═══════════════════════════════════════════════════

@router.get("/files/{file_id}/content", summary="获取文件内容（前端展示）")
def get_file_content(file_id: int, db: Session = Depends(get_db)):
    doc = db.query(KnowledgeDocument).filter(KnowledgeDocument.id == file_id).first()
    if not doc:
        raise HTTPException(404, detail="文件不存在")

    filepath = os.path.join(DOC_DIR, doc.file_path)
    if not os.path.exists(filepath):
        raise HTTPException(404, detail="文件已被移除")

    ext = doc.file_ext.lower()
    content_type = CONTENT_TYPES.get(ext, "application/octet-stream")

    if ext in ("txt", "md"):
        # 文本文件直接返回 UTF-8 文本内容
        with open(filepath, "r", encoding="utf-8", errors="replace") as f:
            text = f.read()
        return Response(content=text, media_type=content_type)
    else:
        # PDF / DOCX 以 inline 方式返回，浏览器可直接预览
        return FileResponse(path=filepath, media_type=content_type, filename=doc.name)


# ═══════════════════════════════════════════════════
# 4. 上传源文件
# ═══════════════════════════════════════════════════

@router.post("/upload", summary="上传知识库源文件")
async def upload_file(
    scenic_id: str = Form("lingshan"),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
):
    ext = os.path.splitext(file.filename or "file.txt")[1].lower()
    if ext not in ALLOWED_EXTS:
        raise HTTPException(400, detail=f"仅支持 {', '.join(ALLOWED_EXTS)} 格式")

    content = await file.read()
    file_size = len(content)

    saved_name = f"{datetime.now().strftime('%Y%m%d%H%M%S')}_{uuid.uuid4().hex[:8]}{ext}"
    saved_path = os.path.join(DOC_DIR, saved_name)
    with open(saved_path, "wb") as f:
        f.write(content)

    doc = KnowledgeDocument(
        scenic_id=scenic_id,
        name=file.filename or saved_name,
        file_size=file_size,
        file_ext=ext.lstrip("."),
        file_path=saved_name,
    )
    db.add(doc)
    db.commit()
    db.refresh(doc)

    logger.info(f"[知识库] 上传: {doc.name} ({file_size} bytes)")
    return BaseResponse(
        code=200, message="上传成功",
        data={
            "id": doc.id,
            "name": doc.name,
            "file_size": doc.file_size,
            "file_ext": doc.file_ext,
            "created_at": doc.created_at.isoformat() if doc.created_at else None,
        },
    )


# ═══════════════════════════════════════════════════
# 5. 删除文件
# ═══════════════════════════════════════════════════

@router.delete("/files/{file_id}", summary="删除源文件")
def delete_file(file_id: int, db: Session = Depends(get_db)):
    doc = db.query(KnowledgeDocument).filter(KnowledgeDocument.id == file_id).first()
    if not doc:
        raise HTTPException(404, detail="文件不存在")

    fp = os.path.join(DOC_DIR, doc.file_path)
    if os.path.exists(fp):
        os.remove(fp)

    db.delete(doc)
    db.commit()
    return BaseResponse(code=200, message="删除成功")
