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
    echo=True  # 显示所有SQL操作日志
)

# Session本地化
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()


def get_db():
    """
    获取数据库会话的依赖项
    每次请求时检查数据库和表，必要时自动创建
    """
    db = SessionLocal()
    try:
        # 每次连接时检查数据库和表
        ensure_database_and_tables()
        yield db
    finally:
        db.close()


def ensure_database_exists():
    """
    检查数据库是否存在，不存在则创建
    """
    logger.info("=" * 50)
    logger.info(f"[数据库检查] 正在检查数据库: {settings.DB_NAME}")

    try:
        # 使用不带数据库名的URL创建engine
        engine_temp = create_engine(
            settings.DATABASE_URL,
            poolclass=QueuePool,
            pool_size=2,
            max_overflow=5,
            pool_pre_ping=True,
            echo=True
        )

        with engine_temp.connect() as conn:
            # 检查数据库是否存在
            result = conn.execute(
                text(f"SHOW DATABASES LIKE '{settings.DB_NAME}'")
            )
            exists = result.fetchone()

            if not exists:
                logger.info(f"[数据库创建] 数据库 {settings.DB_NAME} 不存在，开始创建...")
                # 使用autocommit模式执行创建数据库语句
                with engine_temp.begin() as conn:
                    conn.execute(text(f"CREATE DATABASE `{settings.DB_NAME}` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"))
                logger.info(f"[数据库创建] 数据库 {settings.DB_NAME} 创建成功！")
            else:
                logger.info(f"[数据库检查] 数据库 {settings.DB_NAME} 已存在")

        engine_temp.dispose()

    except Exception as e:
        logger.error(f"[数据库错误] 检查/创建数据库失败: {e}")
        raise


def ensure_tables_exist():
    """
    检查表是否存在，不存在则通过 SQLAlchemy metadata 创建
    """
    logger.info("=" * 50)
    logger.info("[表检查] 正在检查并创建所有数据表...")

    # 导入所有模型，确保 Base.metadata 包含所有表定义
    from models.user import User
    from models.post import Post, PostImage, PostVideo, PostLike, PostComment
    from models.digital_config import DigitalHumanConfig  # 管理端共享表

    try:
        inspector = inspect(engine)
        existing_tables = inspector.get_table_names()
        logger.info(f"[表检查] 当前已有表: {existing_tables}")

        # 使用 create_all 自动创建所有未存在的表（checkfirst=True 表示已存在则跳过）
        Base.metadata.create_all(bind=engine, checkfirst=True)

        new_tables = inspector.get_table_names()
        created = set(new_tables) - set(existing_tables)
        if created:
            logger.info(f"[表创建] 新建表: {created}")
        else:
            logger.info("[表检查] 所有表均已存在，无需创建")

        logger.info("[表检查] 完成")
        logger.info("=" * 50)

    except Exception as e:
        logger.error(f"[表错误] 检查/创建表失败: {e}")
        raise


def ensure_database_and_tables():
    """
    确保数据库和表都存在
    每次连接时调用此函数进行检测
    """
    ensure_database_exists()
    ensure_tables_exist()