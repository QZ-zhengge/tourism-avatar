"""
驴友圈帖子模型
"""
from sqlalchemy import Column, Integer, String, DateTime, Text, ForeignKey, Index
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from database.database import Base


class Post(Base):
    """帖子表"""
    __tablename__ = "posts"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="帖子ID")
    user_id = Column(Integer, nullable=False, comment="发布者用户ID")
    content = Column(Text, nullable=True, comment="文字内容")
    location_name = Column(String(100), nullable=True, comment="定位名称（如：北京市朝阳区）")
    latitude = Column(String(20), nullable=True, comment="纬度")
    longitude = Column(String(20), nullable=True, comment="经度")
    visibility = Column(Integer, default=0, comment="可见性：0公开 1仅好友 2私密")
    like_count = Column(Integer, default=0, comment="点赞数")
    comment_count = Column(Integer, default=0, comment="评论数")
    status = Column(Integer, default=1, comment="状态：1正常 0删除")
    create_time = Column(DateTime, server_default=func.now(), comment="创建时间")
    update_time = Column(DateTime, server_default=func.now(), onupdate=func.now(), comment="更新时间")

    # 关系
    images = relationship("PostImage", back_populates="post", cascade="all, delete-orphan")
    videos = relationship("PostVideo", back_populates="post", cascade="all, delete-orphan")
    likes = relationship("PostLike", back_populates="post", cascade="all, delete-orphan")
    comments = relationship("PostComment", back_populates="post", cascade="all, delete-orphan")

    # 索引
    __table_args__ = (
        Index('idx_user_id', 'user_id'),
        Index('idx_create_time', 'create_time'),
    )


class PostImage(Base):
    """帖子图片表"""
    __tablename__ = "post_images"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="图片ID")
    post_id = Column(Integer, ForeignKey('posts.id', ondelete='CASCADE'), nullable=False, comment="帖子ID")
    url = Column(String(500), nullable=False, comment="图片URL/路径")
    sort_order = Column(Integer, default=0, comment="排序顺序")
    width = Column(Integer, nullable=True, comment="图片宽度")
    height = Column(Integer, nullable=True, comment="图片高度")
    create_time = Column(DateTime, server_default=func.now(), comment="创建时间")

    # 关系
    post = relationship("Post", back_populates="images")

    __table_args__ = (
        Index('idx_post_id', 'post_id'),
    )


class PostVideo(Base):
    """帖子视频表"""
    __tablename__ = "post_videos"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="视频ID")
    post_id = Column(Integer, ForeignKey('posts.id', ondelete='CASCADE'), nullable=False, comment="帖子ID")
    url = Column(String(500), nullable=False, comment="视频URL/路径")
    cover_url = Column(String(500), nullable=True, comment="视频封面URL")
    duration = Column(Integer, nullable=True, comment="视频时长（秒）")
    width = Column(Integer, nullable=True, comment="视频宽度")
    height = Column(Integer, nullable=True, comment="视频高度")
    size = Column(Integer, nullable=True, comment="文件大小（字节）")
    sort_order = Column(Integer, default=0, comment="排序顺序")
    create_time = Column(DateTime, server_default=func.now(), comment="创建时间")

    # 关系
    post = relationship("Post", back_populates="videos")

    __table_args__ = (
        Index('idx_post_id', 'post_id'),
    )


class PostLike(Base):
    """帖子点赞表"""
    __tablename__ = "post_likes"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="点赞ID")
    post_id = Column(Integer, ForeignKey('posts.id', ondelete='CASCADE'), nullable=False, comment="帖子ID")
    user_id = Column(Integer, nullable=False, comment="点赞用户ID")
    create_time = Column(DateTime, server_default=func.now(), comment="点赞时间")

    # 关系
    post = relationship("Post", back_populates="likes")

    __table_args__ = (
        Index('idx_post_id', 'post_id'),
        Index('idx_user_id', 'user_id'),
        Index('idx_post_user', 'post_id', 'user_id', unique=True),  # 防止重复点赞
    )


class PostComment(Base):
    """帖子评论表"""
    __tablename__ = "post_comments"

    id = Column(Integer, primary_key=True, autoincrement=True, comment="评论ID")
    post_id = Column(Integer, ForeignKey('posts.id', ondelete='CASCADE'), nullable=False, comment="帖子ID")
    parent_id = Column(Integer, ForeignKey('post_comments.id', ondelete='CASCADE'), nullable=True, comment="父评论ID（用于回复）")
    user_id = Column(Integer, nullable=False, comment="评论用户ID")
    content = Column(Text, nullable=False, comment="评论内容")
    like_count = Column(Integer, default=0, comment="点赞数")
    status = Column(Integer, default=1, comment="状态：1正常 0删除")
    create_time = Column(DateTime, server_default=func.now(), comment="评论时间")
    update_time = Column(DateTime, server_default=func.now(), onupdate=func.now(), comment="更新时间")

    # 关系
    post = relationship("Post", back_populates="comments")
    # 自关联：当前评论的子回复列表（parent_id 指向本记录 id）
    # remote_side 指向父端(id)，表示"本记录的 id 是 '一' 那侧"
    replies = relationship(
        "PostComment",
        primaryjoin="PostComment.parent_id == PostComment.id",
        foreign_keys="[PostComment.parent_id]",
        remote_side="[PostComment.id]",
        uselist=True,
        lazy="select"
    )

    __table_args__ = (
        Index('idx_post_id', 'post_id'),
        Index('idx_user_id', 'user_id'),
        Index('idx_parent_id', 'parent_id'),
    )