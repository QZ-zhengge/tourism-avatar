"""
管理端数据库模型 — 7 张表
"""
from sqlalchemy import Column, Integer, BigInteger, String, Text, Float, Boolean, DateTime, ForeignKey, JSON
from sqlalchemy.sql import func
from database.database import Base


# ═══════════════════════════════════════════════════
# 0. 管理员用户
# ═══════════════════════════════════════════════════

class AdminUser(Base):
    __tablename__ = "admin_users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(50), nullable=False, unique=True, comment="用户名")
    email = Column(String(100), nullable=False, unique=True, comment="邮箱")
    password = Column(String(255), nullable=False, comment="密码（SHA256哈希）")
    role = Column(String(20), nullable=False, default="admin", comment="角色: admin/editor")
    avatar_url = Column(String(500), nullable=True, comment="头像URL")
    created_at = Column(DateTime, server_default=func.now())


# ═══════════════════════════════════════════════════
# 1. 数字人配置
# ═══════════════════════════════════════════════════

class DigitalHumanConfig(Base):
    __tablename__ = "digital_human_config"

    id = Column(Integer, primary_key=True, autoincrement=True)
    scenic_id = Column(String(50), nullable=False, default="lingshan", comment="景区ID")
    avatar_id = Column(String(50), nullable=False, default="111260002", comment="数字人形象ID")
    vcn = Column(String(50), nullable=False, default="x4_xiaoxuan", comment="发音人ID")
    speed = Column(Integer, nullable=False, default=50, comment="语速 0-100")
    pitch = Column(Integer, nullable=False, default=50, comment="音调 0-100")
    volume = Column(Integer, nullable=False, default=50, comment="音量 0-100")
    service_id = Column(String(100), nullable=False, default="315837983779590144", comment="讯飞服务ID")
    layout = Column(String(20), nullable=False, default="fullscreen", comment="布局: fullscreen/left/right")
    display_mode = Column(String(20), nullable=False, default="full", comment="显示: head/upper/full")
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now(), comment="最后更新时间")
    sync_status = Column(String(20), default="saved", comment="同步状态: saved/saving")


# ═══════════════════════════════════════════════════
# 2. 景点图片
# ═══════════════════════════════════════════════════

class ScenicImage(Base):
    __tablename__ = "scenic_images"

    id = Column(Integer, primary_key=True, autoincrement=True)
    scenic_id = Column(String(50), nullable=False, default="lingshan")
    spot_name = Column(String(200), nullable=False, comment="景点名称")
    file_name = Column(String(255), nullable=False, comment="原始文件名")
    file_path = Column(String(500), nullable=False, comment="服务器存储路径")
    file_size = Column(BigInteger, nullable=False, comment="文件大小（字节）")
    created_at = Column(DateTime, server_default=func.now(), comment="上传时间")


# ═══════════════════════════════════════════════════
# 3. 知识文档（仅存储源文件，不做切块）
# ═══════════════════════════════════════════════════

class KnowledgeDocument(Base):
    __tablename__ = "knowledge_documents"

    id = Column(Integer, primary_key=True, autoincrement=True)
    scenic_id = Column(String(50), nullable=False, default="lingshan")
    name = Column(String(255), nullable=False, comment="原始文件名")
    file_size = Column(BigInteger, nullable=False, comment="文件大小（字节）")
    file_ext = Column(String(10), nullable=False, comment="扩展名: txt/md/docx/pdf")
    file_path = Column(String(500), nullable=False, comment="服务器存储路径")
    created_at = Column(DateTime, server_default=func.now(), comment="上传时间")


# ═══════════════════════════════════════════════════
# 4. 发音人
# ═══════════════════════════════════════════════════

class VcnVoice(Base):
    __tablename__ = "vcn_voices"

    id = Column(Integer, primary_key=True, autoincrement=True)
    vcn = Column(String(50), nullable=False, unique=True, comment="发音人ID，如 x4_xiaoxuan")
    name = Column(String(50), nullable=False, comment="发音人名称")
    gender = Column(String(10), nullable=True, comment="性别")
    category = Column(String(50), nullable=True, comment="分类，如 中文普通话·交互")
    language = Column(String(50), nullable=True, comment="语言")
    audio_path = Column(String(500), nullable=True, comment="试听音频文件路径")
    created_at = Column(DateTime, server_default=func.now())


# ═══════════════════════════════════════════════════
# 5. 对话会话
# ═══════════════════════════════════════════════════

class DialogueSession(Base):
    __tablename__ = "dialogue_sessions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    session_id = Column(String(100), nullable=False, unique=True, comment="会话ID")
    scenic_id = Column(String(50), nullable=False, default="lingshan")
    user_name = Column(String(100), nullable=True, comment="游客显示名")
    satisfaction_score = Column(Float, nullable=False, default=0, comment="满意度 0-100")
    message_count = Column(Integer, nullable=False, default=0, comment="消息总条数")
    categories = Column(JSON, nullable=True, comment="对话分类标签")
    reason = Column(Text, nullable=True, comment="AI分析原因")
    suggestion = Column(Text, nullable=True, comment="AI改进建议")
    last_updated = Column(DateTime, nullable=False, comment="最后消息时间")


# ═══════════════════════════════════════════════════
# 6. 对话消息
# ═══════════════════════════════════════════════════

class DialogueMessage(Base):
    __tablename__ = "dialogue_messages"

    id = Column(Integer, primary_key=True, autoincrement=True)
    session_id = Column(String(100), ForeignKey("dialogue_sessions.session_id", ondelete="CASCADE"), nullable=False)
    seq_no = Column(Integer, nullable=False, comment="消息序号")
    role = Column(String(20), nullable=False, comment="user/assistant")
    content = Column(Text, nullable=False, comment="消息文本")
    sentiment = Column(String(20), nullable=True, comment="positive/neutral/negative")
    intent = Column(String(50), nullable=True, comment="意图类型")
    created_at = Column(DateTime, nullable=False, comment="消息时间")


# ═══════════════════════════════════════════════════
# 7. 用户建议（信箱）
# ═══════════════════════════════════════════════════

class UserSuggestion(Base):
    __tablename__ = "user_suggestions"

    id = Column(String(20), primary_key=True, comment="唯一ID，如 sg_xxx")
    user_id = Column(Integer, nullable=True, index=True, comment="建议用户ID（游客端传入）")
    user_name = Column(String(100), nullable=True, default="匿名用户")
    contact = Column(String(100), nullable=True, comment="联系方式（可选，游客端传入）")
    content = Column(Text, nullable=False, comment="建议内容")
    status = Column(String(10), nullable=False, default="unread", comment="unread/read")
    created_at = Column(DateTime, server_default=func.now(), comment="提交时间")


# ═══════════════════════════════════════════════════
# 8. 数字人形象目录（讯飞完整形象列表）
#    由种子数据(data/avatar_catalog.json)写入，运行时从库读取，
#    不再写死在代码里。游客端与管理端共享此表。
# ═══════════════════════════════════════════════════

class AvatarCatalog(Base):
    __tablename__ = "avatar_catalog"

    id = Column(Integer, primary_key=True, autoincrement=True)
    avatar_id = Column(String(50), nullable=False, unique=True, comment="数字人形象ID（讯飞）")
    name = Column(String(100), nullable=False, comment="形象名称")
    category = Column(String(50), nullable=True, comment="资产分类，如 通用")
    status = Column(String(50), nullable=True, comment="授权状态，如 已授权")
    created_at = Column(DateTime, server_default=func.now(), comment="入库时间")


# ═══════════════════════════════════════════════════
# 9. 数字人对话历史（游客端写入，管理端只读）
#    与原有 dialogue_sessions/dialogue_messages 完全解耦，
#    新建两张表，原表一个字都不动。字段与游客端 models/avatar_chat 完全一致。
# ═══════════════════════════════════════════════════

class AvatarChatSession(Base):
    __tablename__ = "avatar_chat_sessions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    session_id = Column(String(100), nullable=False, unique=True, comment="讯飞会话ID")
    user_id = Column(String(100), nullable=True, comment="游客ID")
    scenic_id = Column(String(50), nullable=True, comment="景区ID")
    avatar_id = Column(String(50), nullable=True, comment="数字人形象ID")
    started_at = Column(DateTime, server_default=func.now(), comment="会话开始时间")
    ended_at = Column(DateTime, nullable=True, comment="会话结束时间")
    message_count = Column(Integer, default=0, comment="消息条数")
    created_at = Column(DateTime, server_default=func.now(), comment="入库时间")


class AvatarChatMessage(Base):
    __tablename__ = "avatar_chat_messages"

    id = Column(Integer, primary_key=True, autoincrement=True)
    session_id = Column(String(100), nullable=False, index=True, comment="会话ID，关联 avatar_chat_sessions")
    seq_no = Column(Integer, nullable=False, comment="消息序号（同一会话内从1递增）")
    role = Column(String(20), nullable=False, comment="user=游客提问 / assistant=数字人回答")
    content = Column(Text, nullable=False, comment="消息文本")
    source = Column(String(20), nullable=True, comment="text / audio（来源）")
    created_at = Column(DateTime, server_default=func.now(), comment="消息时间")
