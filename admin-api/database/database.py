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
        AvatarCatalog,
        AvatarChatSession,
        AvatarChatMessage,
    )
    inspector = inspect(engine)
    existing = inspector.get_table_names()
    needed = [t for t in Base.metadata.tables if t not in existing]
    if needed:
        logger.info(f"[Admin] 新建管理端表: {needed}")
        Base.metadata.create_all(bind=engine, checkfirst=True)

    # 写入数字人形象目录（仅表为空时，幂等）
    _seed_avatar_catalog()


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

    if "user_suggestions" in existing:
        cols = {c["name"] for c in inspector.get_columns("user_suggestions")}
        with engine.begin() as conn:
            if "user_id" not in cols:
                logger.info("[Migrate] user_suggestions 添加 user_id")
                conn.execute(text(
                    "ALTER TABLE user_suggestions ADD COLUMN user_id INT NULL"
                ))
            if "contact" not in cols:
                logger.info("[Migrate] user_suggestions 添加 contact")
                conn.execute(text(
                    "ALTER TABLE user_suggestions ADD COLUMN contact VARCHAR(100) NULL"
                ))


def ensure_static_dirs():
    """确保上传目录存在"""
    for d in ["uploads/images", "uploads/documents"]:
        os.makedirs(d, exist_ok=True)


def _seed_avatar_catalog():
    """首次启动写入数字人形象目录（讯飞完整形象列表）。

    数据来自 data/avatar_catalog.json（不写死在代码逻辑里），
    仅在 avatar_catalog 表为空时写入，重复调用安全（幂等）。
    """
    import json

    from models.admin import AvatarCatalog

    data_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "data", "avatar_catalog.json"
    )
    if not os.path.exists(data_path):
        logger.warning("[Seed] 未找到 data/avatar_catalog.json，跳过数字人形象目录种子")
        return

    db = SessionLocal()
    try:
        if db.query(AvatarCatalog).count() > 0:
            return
        with open(data_path, encoding="utf-8") as f:
            rows = json.load(f)
        for r in rows:
            db.add(AvatarCatalog(
                avatar_id=str(r["avatar_id"]),
                name=r["name"],
                category=r.get("category"),
                status=r.get("status"),
            ))
        db.commit()
        logger.info(f"[Seed] 写入数字人形象目录 {len(rows)} 条")
    except Exception as e:
        db.rollback()
        logger.error(f"[Seed] 数字人形象目录写入失败: {e}")
    finally:
        db.close()
