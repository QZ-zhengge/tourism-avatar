"""
FastAPI 主入口
"""
import logging
import sys
import os
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

from config import get_settings
from database import get_db
from routers import user
from routers import post, like, comment, upload
from routers import avatar  # 讯飞虚拟人模块
from routers import asr     # 本地语音转文字 ASR 模块
from routers import weather # 高德天气查询模块
from routers import suggestion  # 建议信箱（游客端写入，连通管理端）
from routers import checkin    # 地图打卡（省份点亮）
from routers import realtime_explain  # 实时讲解历史（数字人对话历史分支）

# 日志配置
logging.basicConfig(
    level=logging.WARNING,
    format='%(asctime)s %(levelname)s: %(message)s',
    handlers=[logging.StreamHandler(sys.stdout)]
)

# 仅保留业务相关模块的 INFO 日志
for mod in ["routers.asr", "routers.avatar", "main"]:
    logging.getLogger(mod).setLevel(logging.INFO)

logger = logging.getLogger(__name__)
settings = get_settings()

# 创建 FastAPI 应用
app = FastAPI(
    title="随行APP 后端API",
    version="1.0.0",
    docs_url="/docs",
)

# 启动
@app.on_event("startup")
async def startup():
    from database import ensure_database_and_tables
    ensure_database_and_tables()
    print(f"[启动] 服务就绪 on {settings.HOST}:{settings.PORT}")

# 配置CORS - 允许跨域访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 请求日志（精简版）
@app.middleware("http")
async def log_requests(request: Request, call_next):
    response = await call_next(request)
    if response.status_code >= 400:
        logger.warning(f"{request.method} {request.url.path} → {response.status_code}")
    return response


# 全局异常处理器
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"[全局异常] {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"code": 500, "msg": f"服务器内部错误: {str(exc)}"}
    )


# 健康检查端点
@app.get("/", tags=["健康检查"])
async def root():
    return {"status": "ok", "message": "HBuilderX APP 后端服务正在运行"}


@app.get("/health", tags=["健康检查"])
async def health_check():
    return {"status": "healthy", "service": "hbuilder-app-backend"}


# 注册路由
app.include_router(user.router)
app.include_router(post.router)
app.include_router(like.router)
app.include_router(comment.router)
app.include_router(upload.router)
app.include_router(avatar.router)  # 讯飞在线虚拟人驱动
app.include_router(asr.router)     # 本地流式语音转文字（Vosk，真正逐帧解码）
app.include_router(weather.router)  # 高德天气查询（Web服务 API）
app.include_router(suggestion.router)  # 建议信箱（游客端）
app.include_router(checkin.router)      # 地图打卡（省份点亮）
app.include_router(realtime_explain.router)  # 实时讲解历史（数字人对话历史分支）

# 挂载静态文件目录（图片/视频访问）
UPLOAD_DIR = os.path.join(os.path.dirname(__file__), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")


# 直接运行
if __name__ == "__main__":
    import uvicorn
    logger.info(f"[直接运行] 启动服务器 at {settings.HOST}:{settings.PORT}")
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=True,
    )