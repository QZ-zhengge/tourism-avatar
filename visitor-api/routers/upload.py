"""
文件上传路由模块
支持图片、视频上传，返回访问URL
"""
import logging
from fastapi import APIRouter, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from typing import List
import os

from utils.file_upload import upload_image, upload_video

logger = logging.getLogger(__name__)
router = APIRouter(prefix="/upload", tags=["文件上传"])


@router.post("/image", summary="上传单张图片")
async def upload_single_image(file: UploadFile = File(...)):
    """
    上传单张图片，返回可访问的URL

    - 支持格式：JPG、PNG、GIF、WEBP
    - 大小限制：10MB
    - 返回 url 字段即为图片访问地址，配合发帖时图片列表使用
    """
    logger.info(f"[图片上传] 文件名: {file.filename}, 类型: {file.content_type}")
    try:
        result = await upload_image(file)
        return {
            "code": 200,
            "msg": "上传成功",
            "url": result["url"],
            "filename": result["filename"],
            "size": result["size"]
        }
    except HTTPException as e:
        return {"code": 400, "msg": e.detail}
    except Exception as e:
        logger.error(f"[图片上传] 未知错误: {e}")
        return {"code": 500, "msg": "上传失败，请稍后重试"}


@router.post("/images", summary="批量上传图片（最多9张）")
async def upload_multiple_images(files: List[UploadFile] = File(...)):
    """
    批量上传图片（最多9张，对应微信朋友圈规格）

    返回每张图片的 url，按序存储，用于发帖时的 images 字段
    """
    if len(files) > 9:
        return {"code": 400, "msg": "最多同时上传9张图片"}

    results = []
    failed = []
    for idx, file in enumerate(files):
        try:
            result = await upload_image(file)
            results.append({
                "sort_order": idx,
                "url": result["url"],
                "filename": result["filename"],
                "size": result["size"]
            })
        except HTTPException as e:
            failed.append({"filename": file.filename, "reason": e.detail})

    return {
        "code": 200,
        "msg": f"上传完成，成功 {len(results)} 张，失败 {len(failed)} 张",
        "files": results,
        "failed": failed
    }


@router.post("/video", summary="上传视频")
async def upload_single_video(file: UploadFile = File(...)):
    """
    上传单个视频文件

    - 支持格式：MP4、MOV、AVI、WEBM
    - 大小限制：100MB
    - 返回 url 即为视频访问地址
    """
    logger.info(f"[视频上传] 文件名: {file.filename}, 类型: {file.content_type}")
    try:
        result = await upload_video(file)
        return {
            "code": 200,
            "msg": "上传成功",
            "url": result["url"],
            "filename": result["filename"],
            "size": result["size"]
        }
    except HTTPException as e:
        return {"code": 400, "msg": e.detail}
    except Exception as e:
        logger.error(f"[视频上传] 未知错误: {e}")
        return {"code": 500, "msg": "上传失败，请稍后重试"}