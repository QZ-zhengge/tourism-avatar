"""
数据库连接 — 管理端新建自己的表，主项目已有表不重复创建
"""
import logging
import os
from sqlalchemy import create_engine, text, inspect
from sqlalchemy.orm import sessionmaker, declarative_base
from sqlalchemy.pool import QueuePool

from config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

engine = create_engine(
    settings.DATABASE_URL_WITH_DB,
    poolclass=QueuePool,
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,
    echo=False,
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    db = SessionLocal()
    try:
        ensure_admin_tables()
        yield db
    finally:
        db.close()


def ensure_admin_tables():
    """只创建管理端自己的表，不重复创建主项目的 users/posts 等表"""
    _migrate_existing_tables()  # 先迁移已存在但缺少列的表

    # 导入所有管理端模型以注册到 Base.metadata
    from models.admin import (  # noqa: F401
        AdminUser,
        DigitalHumanConfig,
        ScenicImage,
        KnowledgeDocument,
        VcnVoice,
        DialogueSession,
        DialogueMessage,
        UserSuggestion,
    )
    inspector = inspect(engine)
    existing = inspector.get_table_names()
    needed = [t for t in Base.metadata.tables if t not in existing]
    if needed:
        logger.info(f"[Admin] 新建管理端表: {needed}")
        Base.metadata.create_all(bind=engine, checkfirst=True)


def _migrate_existing_tables():
    """对已存在的表做增量迁移（添加缺失列）"""
    inspector = inspect(engine)
    existing = inspector.get_table_names()

    if "digital_human_config" in existing:
        cols = {c["name"] for c in inspector.get_columns("digital_human_config")}
        with engine.begin() as conn:
            if "updated_at" not in cols:
                logger.info("[Migrate] digital_human_config 添加 updated_at")
                conn.execute(text(
                    "ALTER TABLE digital_human_config ADD COLUMN updated_at DATETIME NULL"
                ))
            if "sync_status" not in cols:
                logger.info("[Migrate] digital_human_config 添加 sync_status")
                conn.execute(text(
                    "ALTER TABLE digital_human_config ADD COLUMN sync_status VARCHAR(20) DEFAULT 'saved'"
                ))


def ensure_static_dirs():
    """确保上传目录存在"""
    for d in ["uploads/images", "uploads/documents"]:
        os.makedirs(d, exist_ok=True)
