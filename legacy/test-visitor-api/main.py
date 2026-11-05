"""
FastAPI 主入口
"""
import logging
import sys
import os
from contextlib import asynccontextmanager
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

# 配置日志 - 控制台全输出
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger(__name__)
settings = get_settings()


# Lifespan事件处理（替代废弃的on_event）
@asynccontextmanager
async def lifespan(app: FastAPI):
    """启动和关闭事件"""
    # 启动时：初始化数据库和表
    from database import ensure_database_and_tables
    logger.info("=" * 60)
    logger.info("[服务启动] HBuilderX APP 后端服务正在启动...")
    logger.info(f"[服务配置] 主机: {settings.HOST}, 端口: {settings.PORT}")
    logger.info(f"[数据库] {settings.DB_HOST}:{settings.DB_PORT}/{settings.DB_NAME}")

    # 初始化数据库和表
    try:
        ensure_database_and_tables()
        logger.info("[数据库初始化] 数据库和表检查/创建完成")
    except Exception as e:
        logger.error(f"[数据库初始化失败] {e}")
        raise

    logger.info("[服务启动] 启动完成！")
    logger.info("=" * 60)
    yield
    logger.info("=" * 60)
    logger.info("[服务关闭] 后端服务正在关闭...")
    logger.info("=" * 60)


# 创建FastAPI应用（带lifespan）
app = FastAPI(
    title="HBuilderX APP 后端API",
    description="HBuilderX制作的安卓APP后端服务",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# 配置CORS - 允许跨域访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制具体域名
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# 请求日志中间件
@app.middleware("http")
async def log_requests(request: Request, call_next):
    logger.info("=" * 60)
    logger.info(f"[请求开始] {request.method} {request.url.path}")
    logger.info(f"[请求头] {dict(request.headers)}")

    if request.method in ["POST", "PUT", "PATCH"]:
        try:
            body = await request.body()
            logger.info(f"[请求体] {body.decode('utf-8') if body else 'Empty'}")
        except Exception as e:
            logger.warning(f"[请求体读取失败] {e}")

    response = await call_next(request)

    logger.info(f"[响应状态] {response.status_code}")
    logger.info("[请求结束]" + "=" * 60)
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
        log_level="debug"
    )