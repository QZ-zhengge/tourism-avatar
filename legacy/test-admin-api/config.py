"""
管理后台配置文件 — 与主项目 SoftwareCupAI 共享同一 MySQL 数据库
"""
from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # 数据库（与主项目共享 hbuilder_app）
    DB_HOST: str = "localhost"
    DB_PORT: int = 3306
    DB_USER: str = "root"
    DB_PASSWORD: str = ""
    DB_NAME: str = "hbuilder_app"

    # 服务器
    HOST: str = "0.0.0.0"
    PORT: int = 8005

    # 日志
    LOG_LEVEL: str = "DEBUG"

    # 静态文件
    UPLOAD_DIR: str = "uploads"

    # JWT
    JWT_SECRET_KEY: str = "change-me-in-production"
    JWT_EXPIRE_HOURS: int = 24

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
