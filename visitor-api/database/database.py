"""
数据库连接模块
每次连接时检测并创建数据库和表
"""
import logging
from sqlalchemy import create_engine, text, inspect
from sqlalchemy.orm import sessionmaker, declarative_base
from sqlalchemy.pool import QueuePool

from config import get_settings

logger = logging.getLogger(__name__)
settings = get_settings()

# 创建数据库引擎（带数据库名）
# 注意：创建时不会立即连接，连接在首次使用时发生
engine = create_engine(
    settings.DATABASE_URL_WITH_DB,
    poolclass=QueuePool,
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,
    echo=False  # 关闭 SQL 日志
)

# Session本地化
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    """
    获取数据库会话的依赖项
    建表/迁移由 main.py startup 事件执行，这里只提供干净会话
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def ensure_database_exists():
    """检查数据库是否存在，不存在则创建"""
    try:
        engine_temp = create_engine(
            settings.DATABASE_URL,
            pool_pre_ping=True,
            echo=False
        )
        with engine_temp.connect() as conn:
            result = conn.execute(
                text(f"SHOW DATABASES LIKE '{settings.DB_NAME}'")
            )
            if not result.fetchone():
                logger.info(f"[DB] 创建数据库: {settings.DB_NAME}")
                with engine_temp.begin() as conn:
                    conn.execute(text(
                        f"CREATE DATABASE `{settings.DB_NAME}` "
                        "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
                    ))
        engine_temp.dispose()
    except Exception as e:
        logger.error(f"[DB] 检查/创建失败: {e}")
        raise


def ensure_tables_exist():
    """检查表是否存在，不存在则自动创建；同时保证数字人形象目录已灌入种子数据"""
    from models.user import User
    from models.post import Post, PostImage, PostVideo, PostLike, PostComment
    from models.digital_config import DigitalHumanConfig
    from models.avatar_catalog import AvatarCatalog
    from models.avatar_chat import AvatarChatSession, AvatarChatMessage
    from models.suggestion import UserSuggestion
    from models.checkin import ProvinceCode, UserCheckin
    from models.realtime_explain import RealtimeExplainHistory

    try:
        Base.metadata.create_all(bind=engine, checkfirst=True)
    except Exception as e:
        logger.error(f"[DB] 建表失败: {e}")
        raise

    # 建议信箱：若 user_suggestions 已存在但缺少 user_id/contact 列，做增量迁移（连通管理端同一张表）
    _migrate_user_suggestions_columns()

    # 数字人对话历史：讯飞 session_id 实际长度可达 108+，若旧表 session_id 仍为 VARCHAR(100) 则加宽到 255
    _migrate_avatar_chat_session_id_length()

    # 实时讲解历史：image 列若仍是普通 TEXT(64KB) 则加宽到 LONGTEXT(~4GB)，支持拍照 base64 大图
    _migrate_realtime_explain_image_longtext()

    # 数字人形象目录：建表与灌表写在一起，启动即保证有数据（幂等，不影响其他功能）
    _seed_avatar_catalog()

    # 标准省份编号：建表后灌入 GB/T 2260 省份数据（幂等）
    _seed_province_codes()


def _seed_avatar_catalog():
    """首次启动写入数字人形象目录（讯飞完整形象列表）。

    与建表逻辑同处 ensure_tables_exist，建表后立刻灌数据；
    数据来自 data/avatar_catalog.json（不写死在代码逻辑里），
    仅在 avatar_catalog 表为空时写入，重复调用安全（幂等）。
    """
    import json
    import os
    from models.avatar_catalog import AvatarCatalog

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


def _migrate_user_suggestions_columns():
    """对已存在的 user_suggestions 表做增量迁移（添加缺失的 user_id / contact 列）"""
    inspector = inspect(engine)
    existing = inspector.get_table_names()
    if "user_suggestions" not in existing:
        return
    cols = {c["name"] for c in inspector.get_columns("user_suggestions")}
    with engine.begin() as conn:
        if "user_id" not in cols:
            logger.info("[Migrate] user_suggestions 添加 user_id")
            conn.execute(text("ALTER TABLE user_suggestions ADD COLUMN user_id INT NULL"))
        if "contact" not in cols:
            logger.info("[Migrate] user_suggestions 添加 contact")
            conn.execute(text("ALTER TABLE user_suggestions ADD COLUMN contact VARCHAR(100) NULL"))


def _migrate_avatar_chat_session_id_length():
    """将 avatar_chat_sessions / avatar_chat_messages 的 session_id 由 VARCHAR(100) 加宽到 VARCHAR(255)。

    讯飞返回的 session_id 实际长度可达 108 位，超过旧表的 VARCHAR(100)，会导致
    写入报错 (1406, "Data too long for column 'session_id'")，历史无法落库。
    仅当当前列长度 < 255 时执行 ALTER，重复调用安全（幂等）。
    """
    inspector = inspect(engine)
    existing = inspector.get_table_names()
    targets = {
        "avatar_chat_sessions": "session_id VARCHAR(255) NOT NULL",
        "avatar_chat_messages": "session_id VARCHAR(255) NOT NULL",
    }
    for table, col_def in targets.items():
        if table not in existing:
            continue
        try:
            col = next(
                (c for c in inspector.get_columns(table) if c["name"] == "session_id"),
                None,
            )
            length = getattr(col["type"], "length", None) if col else None
            if length is not None and length >= 255:
                continue
            with engine.begin() as conn:
                logger.info(f"[Migrate] {table}.session_id 加宽到 VARCHAR(255)")
                conn.execute(text(f"ALTER TABLE {table} MODIFY COLUMN {col_def}"))
        except Exception as e:
            logger.error(f"[Migrate] {table}.session_id 加宽失败: {e}")


def _migrate_realtime_explain_image_longtext():
    """将 realtime_explain_history.image 由 TEXT(64KB) 加宽到 LONGTEXT(~4GB)。

    前端调用实时讲解（takePhoto）时上传的 base64 JPEG 可能达到 400KB+，
    远超 MySQL TEXT 上限 65535 字节，导致写入 (1406, "Data too long")。
    仅当当前列类型仍为普通 TEXT（非 LONGTEXT）时执行 ALTER，重复调用安全（幂等）。
    """
    table = "realtime_explain_history"
    col_name = "image"
    inspector = inspect(engine)
    if table not in inspector.get_table_names():
        return
    try:
        col = next(
            (c for c in inspector.get_columns(table) if c["name"] == col_name),
            None,
        )
        if col is None:
            return
        col_type = col["type"]
        cur_len = getattr(col_type, "length", None)  # None = 普通 TEXT (64KB)
        if cur_len is not None and cur_len >= 65536:
            return  # 已经是 MEDIUMTEXT/LONGTEXT，跳过
        with engine.begin() as conn:
            logger.info(f"[Migrate] {table}.{col_name} 加宽到 LONGTEXT")
            conn.execute(text(f"ALTER TABLE {table} MODIFY COLUMN {col_name} LONGTEXT NOT NULL"))
    except Exception as e:
        logger.error(f"[Migrate] {table}.{col_name} 加宽失败: {e}")


def _seed_province_codes():
    """
    首次启动写入标准省份编号（GB/T 2260，6 位行政区划代码）。
    数据来自 data/province_codes.json，仅在 province_codes 表为空时写入，重复调用安全（幂等）。
    """
    import json
    import os
    from models.checkin import ProvinceCode

    data_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)), "data", "province_codes.json"
    )
    if not os.path.exists(data_path):
        logger.warning("[Seed] 未找到 data/province_codes.json，跳过省份编号种子")
        return

    db = SessionLocal()
    try:
        if db.query(ProvinceCode).count() > 0:
            return
        with open(data_path, encoding="utf-8") as f:
            rows = json.load(f)
        for r in rows:
            db.add(ProvinceCode(
                province_code=str(r["province_code"]),
                province_name=r["province_name"],
            ))
        db.commit()
        logger.info(f"[Seed] 写入标准省份编号 {len(rows)} 条")
    except Exception as e:
        db.rollback()
        logger.error(f"[Seed] 省份编号写入失败: {e}")
    finally:
        db.close()


def ensure_database_and_tables():
    """
    确保数据库和表都存在
    每次连接时调用此函数进行检测
    """
    ensure_database_exists()
    ensure_tables_exist()