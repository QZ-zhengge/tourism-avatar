"""
SoftwareCup APK 下载服务
FastAPI + SQLite + 静态下载页面
"""
import os
import time
import aiosqlite
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import FileResponse, HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

# ── 路径 ───────────────────────────────────────────
BASE_DIR  = os.path.dirname(os.path.abspath(__file__))
APK_DIR   = os.path.join(BASE_DIR, "uploads", "apk")
STATIC_DIR = os.path.join(BASE_DIR, "static")
DB_PATH   = os.path.join(BASE_DIR, "downloads.db")

os.makedirs(APK_DIR, exist_ok=True)

# ── 数据库初始化 ────────────────────────────────────
async def init_db():
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute("""
            CREATE TABLE IF NOT EXISTS downloads (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                version TEXT    NOT NULL,
                ip      TEXT    NOT NULL,
                ua      TEXT    DEFAULT '',
                time    REAL    NOT NULL
            )
        """)
        await db.commit()

@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    yield

app = FastAPI(title="SoftwareCup Download", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_credentials=True,
                   allow_methods=["*"], allow_headers=["*"])
app.mount("/static", StaticFiles(directory=STATIC_DIR), name="static")

# ── APK 元数据（部署时在这里更新）─────────────────────
APK_META = {
    "version": "1.0.0",
    "version_code": 1,
    "filename": "SoftwareCup-v1.0.0.apk",
    "size": 1024000,   # 示例大小，部署后替换为真实值
    "changelog": "• 数字人交互功能\n• 天气查询\n• 语音识别",
}

# ═══════════════════════════════════════════════════
# API
# ═══════════════════════════════════════════════════

@app.get("/api/apk/info")
async def apk_info():
    """获取最新 APK 版本信息"""
    filepath = os.path.join(APK_DIR, APK_META["filename"])
    if os.path.exists(filepath):
        APK_META["size"] = os.path.getsize(filepath)

    async with aiosqlite.connect(DB_PATH) as db:
        row = await db.execute("SELECT COUNT(*) FROM downloads")
        total = (await row.fetchone())[0]

    return {
        "code": 200,
        "data": {
            **APK_META,
            "download_count": total,
            "apk_exists": os.path.exists(filepath),
        }
    }


@app.get("/api/apk/download")
async def apk_download(request: Request=None):
    """下载最新 APK 文件"""
    filepath = os.path.join(APK_DIR, APK_META["filename"])
    if not os.path.exists(filepath):
        raise HTTPException(status_code=404, detail="APK 文件尚未上传，请联系管理员")

    # 记录下载
    ip = request.client.host if request else "unknown"
    ua = request.headers.get("user-agent", "")[:200] if request else ""
    async with aiosqlite.connect(DB_PATH) as db:
        await db.execute(
            "INSERT INTO downloads (version, ip, ua, time) VALUES (?, ?, ?, ?)",
            (APK_META["version"], ip, ua, time.time())
        )
        await db.commit()

    return FileResponse(
        filepath,
        filename=APK_META["filename"],
        media_type="application/vnd.android.package-archive",
    )


@app.get("/api/downloads/count")
async def download_count():
    """获取总下载次数"""
    async with aiosqlite.connect(DB_PATH) as db:
        row = await db.execute("SELECT COUNT(*) FROM downloads")
        total = (await row.fetchone())[0]
    return {"code": 200, "download_count": total}

# ═══════════════════════════════════════════════════
# 前端页面 + 静态资源
# ═══════════════════════════════════════════════════

@app.get("/logo")
async def serve_logo():
    """返回 logo 图片"""
    logo_path = os.path.join(BASE_DIR, "uploads", "logo", "logo.jpg")
    if not os.path.exists(logo_path):
        raise HTTPException(status_code=404, detail="logo 未上传")
    return FileResponse(logo_path, media_type="image/jpeg")


@app.get("/", response_class=HTMLResponse)
async def index():
    with open(os.path.join(STATIC_DIR, "index.html"), "r", encoding="utf-8") as f:
        return f.read()


# ── 启动 ───────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=18080)
