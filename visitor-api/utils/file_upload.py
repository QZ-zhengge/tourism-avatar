"""
文件上传工具模块
支持图片和视频上传，存储到本地uploads目录
"""
import os
import uuid
import logging
from datetime import datetime
from typing import List, Optional, Tuple
from fastapi import UploadFile, HTTPException
from config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

# 上传目录配置
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads")
IMAGE_DIR = os.path.join(UPLOAD_DIR, "images")
VIDEO_DIR = os.path.join(UPLOAD_DIR, "videos")

# 允许的文件类型
ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"}
ALLOWED_VIDEO_TYPES = {"video/mp4", "video/quicktime", "video/x-msvideo", "video/webm"}

# 文件大小限制（字节）
MAX_IMAGE_SIZE = 10 * 1024 * 1024  # 10MB
MAX_VIDEO_SIZE = 100 * 1024 * 1024  # 100MB


def ensure_upload_dirs():
    """确保上传目录存在"""
    os.makedirs(IMAGE_DIR, exist_ok=True)
    os.makedirs(VIDEO_DIR, exist_ok=True)
    logger.info(f"[上传目录] 图片目录: {IMAGE_DIR}, 视频目录: {VIDEO_DIR}")


def get_file_extension(filename: str) -> str:
    """获取文件扩展名"""
    if "." in filename:
        return filename.rsplit(".", 1)[1].lower()
    return ""


def generate_unique_filename(original_filename: str) -> str:
    """生成唯一文件名"""
    ext = get_file_extension(original_filename)
    unique_id = uuid.uuid4().hex
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    if ext:
        return f"{timestamp}_{unique_id}.{ext}"
    return f"{timestamp}_{unique_id}"


async def save_upload_file(file: UploadFile, target_dir: str, max_size: int) -> Tuple[str, int]:
    """
    保存上传文件

    Args:
        file: 上传的文件
        target_dir: 目标目录
        max_size: 最大文件大小（字节）

    Returns:
        (文件路径, 文件大小)
    """
    ensure_upload_dirs()

    # 检查文件大小
    file.file.seek(0, 2)  # 定位到文件末尾
    size = file.file.tell()
    file.file.seek(0)  # 重置到开头

    if size > max_size:
        raise HTTPException(status_code=400, detail=f"文件大小超过限制 ({max_size // (1024*1024)}MB)")

    # 生成唯一文件名
    filename = generate_unique_filename(file.filename or "unnamed")
    filepath = os.path.join(target_dir, filename)

    # 保存文件
    try:
        content = await file.read()
        with open(filepath, "wb") as f:
            f.write(content)
        logger.info(f"[文件上传] 保存文件: {filepath}, 大小: {size} bytes")
        return filepath, size
    except Exception as e:
        logger.error(f"[文件上传] 保存文件失败: {e}")
        raise HTTPException(status_code=500, detail="文件保存失败")


async def upload_image(file: UploadFile) -> dict:
    """
    上传图片

    Args:
        file: 图片文件

    Returns:
        {"url": 文件访问路径, "filename": 原文件名, "size": 文件大小}
    """
    # 检查文件类型
    if file.content_type not in ALLOWED_IMAGE_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的图片格式: {file.content_type}，支持的格式: JPG, PNG, GIF, WEBP"
        )

    filepath, size = await save_upload_file(file, IMAGE_DIR, MAX_IMAGE_SIZE)

    # 返回访问路径（相对路径）
    relative_url = f"/uploads/images/{os.path.basename(filepath)}"

    return {
        "url": relative_url,
        "filename": file.filename or "unnamed",
        "size": size
    }


async def upload_video(file: UploadFile) -> dict:
    """
    上传视频

    Args:
        file: 视频文件

    Returns:
        {"url": 文件访问路径, "filename": 原文件名, "size": 文件大小}
    """
    # 检查文件类型
    if file.content_type not in ALLOWED_VIDEO_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"不支持的视频格式: {file.content_type}，支持的格式: MP4, MOV, AVI, WEBM"
        )

    filepath, size = await save_upload_file(file, VIDEO_DIR, MAX_VIDEO_SIZE)

    # 返回访问路径（相对路径）
    relative_url = f"/uploads/videos/{os.path.basename(filepath)}"

    return {
        "url": relative_url,
        "filename": file.filename or "unnamed",
        "size": size
    }


async def upload_images(files: List[UploadFile]) -> List[dict]:
    """批量上传图片"""
    results = []
    for file in files:
        try:
            result = await upload_image(file)
            results.append(result)
        except HTTPException:
            # 跳过上传失败的文件
            continue
    return results


async def upload_videos(files: List[UploadFile]) -> List[dict]:
    """批量上传视频"""
    results = []
    for file in files:
        try:
            result = await upload_video(file)
            results.append(result)
        except HTTPException:
            continue
    return results


def delete_file(filepath: str) -> bool:
    """删除文件"""
    try:
        if os.path.exists(filepath):
            os.remove(filepath)
            logger.info(f"[文件删除] 删除文件: {filepath}")
            return True
        return False
    except Exception as e:
        logger.error(f"[文件删除] 删除文件失败: {e}")
        return False


def get_file_url(filename: str, file_type: str = "images") -> str:
    """获取文件的访问URL"""
    return f"/uploads/{file_type}/{filename}"