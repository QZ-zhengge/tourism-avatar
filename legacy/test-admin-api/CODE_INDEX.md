# 文旅管理 API 测试快照 · 代码索引

从 [项目 README](README.md) 了解主流程。下表把当前快照中的源码、页面、脚本与主要配置按目录列出。职责根据代码声明与目录用途概括。

| 文件 | 职责或阅读线索 |
| --- | --- |
| [config.py](config.py) | 配置构建、应用启动或环境参数。可从 `Settings` 、 `DATABASE_URL` 、 `DATABASE_URL_WITH_DB` 、 `Config` 开始 |
| [database/__init__.py](database/__init__.py) | 该目录下的业务实现或工具代码 |
| [database/database.py](database/database.py) | 处理存储连接、表结构或数据迁移。可从 `get_db` 、 `ensure_admin_tables` 、 `_migrate_existing_tables` 、 `ensure_static_dirs` 开始 |
| [main.py](main.py) | 应用入口。可从 `log_requests` 、 `root` 、 `health` 开始 |
| [models/admin.py](models/admin.py) | 管理数据模型。可从 `AdminUser` 、 `DigitalHumanConfig` 、 `ScenicImage` 、 `KnowledgeDocument` 开始 |
| [routers/auth.py](routers/auth.py) | 定义 HTTP 请求入口、参数或响应。可从 `_hash` 、 `_create_jwt` 、 `_decode_jwt` 、 `_get_current_user` 开始 |
| [routers/content.py](routers/content.py) | 定义 HTTP 请求入口、参数或响应。可从 `upload_image` 、 `list_images` 、 `delete_image` 开始 |
| [routers/dialogue.py](routers/dialogue.py) | 定义 HTTP 请求入口、参数或响应。可从 `list_sessions` 、 `get_session_messages` 开始 |
| [routers/digital.py](routers/digital.py) | 定义 HTTP 请求入口、参数或响应。可从 `_load_avatar_base64` 、 `get_avatars` 、 `get_config` 、 `save_config` 开始 |
| [routers/knowledge.py](routers/knowledge.py) | 定义 HTTP 请求入口、参数或响应。可从 `list_files` 、 `get_file` 、 `get_file_content` 、 `upload_file` 开始 |
| [routers/suggestion.py](routers/suggestion.py) | 定义 HTTP 请求入口、参数或响应。可从 `list_suggestions` 、 `mark_read` 开始 |
| [routers/vcn.py](routers/vcn.py) | 定义 HTTP 请求入口、参数或响应。可从 `_load_audio_base64` 、 `list_voices` 、 `add_voice` 、 `upload_audio` 开始 |
| [schemas/admin.py](schemas/admin.py) | 定义接口或业务数据结构。可从 `BaseResponse` 、 `DigitalConfigRequest` 、 `check_layout` 、 `check_display_mode` 开始 |

本页列出 13 个可阅读的项目文件，以及 0 个第三方或生成文件。
