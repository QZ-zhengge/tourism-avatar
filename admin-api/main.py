"""
景区数字人管理平台 — 管理端后端

模块：
  /api/v1/admin/digital/...   数字人配置
  /api/v1/admin/content/...   景点图片管理
  /api/v1/admin/knowledge/... 知识库（上传/下载源文件）
  /api/v1/admin/dialogue/...  对话分析
"""
import os
import sys
import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from config import get_settings
from database.database import ensure_static_dirs

settings = get_settings()
logging.basicConfig(
    level=getattr(logging, settings.LOG_LEVEL),
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    stream=sys.stdout,
)
logger = logging.getLogger(__name__)

ensure_static_dirs()

app = FastAPI(
    title="景区数字人管理平台",
    description="无锡灵山胜境 · 数字人配置 / 内容管理 / 知识库 / 对话分析",
    version="1.0.0",
    docs_url="/docs",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 静态文件访问（景点图片 + 文档）
app.mount("/api/v1/admin/uploads", StaticFiles(directory="uploads"), name="uploads")


# 请求日志
@app.middleware("http")
async def log_requests(request, call_next):
    logger.info("=" * 60)
    logger.info(f"[请求] {request.method} {request.url.path}")
    response = await call_next(request)
    logger.info(f"[响应] {response.status_code}")
    logger.info("=" * 60)
    return response


# 注册路由
from routers.digital import router as digital_router
from routers.content import router as content_router
from routers.knowledge import router as knowledge_router
from routers.dialogue import router as dialogue_router
from routers.vcn import router as vcn_router
from routers.auth import router as auth_router
from routers.suggestion import router as suggestion_router

app.include_router(digital_router)
app.include_router(content_router)
app.include_router(knowledge_router)
app.include_router(dialogue_router)
app.include_router(vcn_router)
app.include_router(auth_router)
app.include_router(suggestion_router)


@app.get("/", tags=["健康"])
def root():
    return {"service": "景区数字人管理平台", "version": "1.0.0"}


@app.get("/health", tags=["健康"])
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    port = settings.PORT
    logger.info(f"管理端启动 → http://0.0.0.0:{port}")
    logger.info(f"Swagger → http://0.0.0.0:{port}/docs")
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
