"""
配置文件 - 集中管理所有可配置参数

所有敏感数据（密码、API密钥）存放在 .env 文件中，不提交到 Git。
.env.example 提供了模板，组员复制为 .env 后填入自己的凭证即可。
具体步骤见 README.md。
"""
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """应用配置 — 敏感值无默认值，强制从 .env 读取"""

    # ── 数据库配置 ─────────────────────────────────────
    DB_HOST: str = "localhost"
    DB_PORT: int = 3306
    DB_USER: str = "root"
    DB_PASSWORD: str = ""   # 【敏感】从 .env 读取
    DB_NAME: str = "hbuilder_app"

    # ── 服务器配置 ─────────────────────────────────────
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    # ── 日志配置 ───────────────────────────────────────
    LOG_LEVEL: str = "DEBUG"

    # ── 讯飞虚拟人配置 ─────────────────────────────────
    #   到控制台 https://console.xfyun.cn 创建应用后获取
    XFYUN_APP_ID: str = ""        # 【敏感】从 .env 读取
    XFYUN_API_KEY: str = ""       # 【敏感】从 .env 读取
    XFYUN_API_SECRET: str = ""    # 【敏感】从 .env 读取

    # 虚拟人服务地址 —— 生产环境一般不需要改动
    XFYUN_VMS_HOST: str = "vms.cn-huadong-1.xf-yun.com"
    XFYUN_VMS_BASE_URL: str = "https://vms.cn-huadong-1.xf-yun.com"
    XFYUN_VMS_WSS_URL: str = "wss://vms.cn-huadong-1.xf-yun.com"

    # 默认虚拟人形象ID（依丹）
    XFYUN_DEFAULT_AVATAR_ID: str = "118801001"
    # 默认发音人
    XFYUN_DEFAULT_VCN: str = "x4_lingxiaoyu_assist"

    # ── 高德地图 API 配置 ──────────────────────────────
    #   到控制台 https://console.amap.com/dev/key 创建「Web服务」类型应用后获取
    AMAP_API_KEY: str = ""        # 【敏感】从 .env 读取，Web服务 API Key

    # ── 派生属性 ───────────────────────────────────────

    @property
    def DATABASE_URL(self) -> str:
        return f"mysql+pymysql://user:CHANGE_ME@{self.DB_HOST}:{self.DB_PORT}/"

    @property
    def DATABASE_URL_WITH_DB(self) -> str:
        return f"mysql+pymysql://user:CHANGE_ME@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"


@lru_cache()
def get_settings() -> Settings:
    return Settings()