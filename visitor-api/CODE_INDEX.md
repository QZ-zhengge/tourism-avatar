# 文旅数字人游客 API · 代码索引

从 [项目 README](README.md) 了解主流程。下表把当前快照中的源码、页面、脚本与主要配置按目录列出。职责根据代码声明与目录用途概括。

| 文件 | 职责或阅读线索 |
| --- | --- |
| [config.py](config.py) | 配置构建、应用启动或环境参数。可从 `Settings` 、 `DATABASE_URL` 、 `DATABASE_URL_WITH_DB` 、 `Config` 开始 |
| [data/province_codes.json](data/province_codes.json) | 提供工程配置、界面资源或结构化数据 |
| [database/__init__.py](database/__init__.py) | 该目录下的业务实现或工具代码 |
| [database/database.py](database/database.py) | 处理存储连接、表结构或数据迁移。可从 `get_db` 、 `ensure_database_exists` 、 `ensure_tables_exist` 、 `_seed_avatar_catalog` 开始 |
| [gen_docx.mjs](gen_docx.mjs) | 该目录下的业务实现或工具代码。可从 `t` 、 `p` 、 `flushCodes` 、 `flushTable` 开始 |
| [main.py](main.py) | FastAPI 入口。可从 `startup` 、 `log_requests` 、 `global_exception_handler` 、 `root` 开始 |
| [models/__init__.py](models/__init__.py) | 定义接口或业务数据结构 |
| [models/avatar_catalog.py](models/avatar_catalog.py) | 定义接口或业务数据结构。可从 `AvatarCatalog` 开始 |
| [models/avatar_chat.py](models/avatar_chat.py) | 会话数据。可从 `AvatarChatSession` 、 `AvatarChatMessage` 开始 |
| [models/checkin.py](models/checkin.py) | 定义接口或业务数据结构。可从 `ProvinceCode` 、 `UserCheckin` 开始 |
| [models/digital_config.py](models/digital_config.py) | 定义接口或业务数据结构。可从 `DigitalHumanConfig` 开始 |
| [models/post.py](models/post.py) | 定义接口或业务数据结构。可从 `Post` 、 `PostImage` 、 `PostVideo` 、 `PostLike` 开始 |
| [models/realtime_explain.py](models/realtime_explain.py) | 定义接口或业务数据结构。可从 `RealtimeExplainHistory` 开始 |
| [models/suggestion.py](models/suggestion.py) | 定义接口或业务数据结构。可从 `UserSuggestion` 开始 |
| [models/user.py](models/user.py) | 定义接口或业务数据结构。可从 `User` 、 `to_dict` 开始 |
| [package.json](package.json) | 提供工程配置、界面资源或结构化数据 |
| [routers/__init__.py](routers/__init__.py) | 定义 HTTP 请求入口、参数或响应 |
| [routers/asr.py](routers/asr.py) | 定义 HTTP 请求入口、参数或响应。可从 `asr_stream` 、 `_push_partial` 、 `_process_pcm_chunk` 、 `asr_status` 开始 |
| [routers/avatar.py](routers/avatar.py) | 数字人交互路由。可从 `_avatar_name` 、 `_get_default_config` 、 `_session_write_lock` 、 `_chat_log_session` 开始 |
| [routers/checkin.py](routers/checkin.py) | 打卡接口。可从 `add_checkin` 、 `user_checkins` 、 `province_list` 开始 |
| [routers/comment.py](routers/comment.py) | 定义 HTTP 请求入口、参数或响应。可从 `_get_username` 、 `_build_comment` 、 `create_comment` 、 `get_post_comments` 开始 |
| [routers/like.py](routers/like.py) | 定义 HTTP 请求入口、参数或响应。可从 `toggle_post_like` 、 `get_post_likes` 、 `toggle_comment_like` 、 `check_like_status` 开始 |
| [routers/post.py](routers/post.py) | 定义 HTTP 请求入口、参数或响应。可从 `MediaImageItem` 、 `MediaVideoItem` 、 `PostMediaRequest` 、 `_build_post_response` 开始 |
| [routers/realtime_explain.py](routers/realtime_explain.py) | 定义 HTTP 请求入口、参数或响应。可从 `add_realtime_explain` 、 `list_realtime_explain` 开始 |
| [routers/suggestion.py](routers/suggestion.py) | 定义 HTTP 请求入口、参数或响应。可从 `_resolve_username` 、 `create_suggestion` 、 `my_suggestions` 开始 |
| [routers/upload.py](routers/upload.py) | 定义 HTTP 请求入口、参数或响应。可从 `upload_single_image` 、 `upload_multiple_images` 、 `upload_single_video` 开始 |
| [routers/user.py](routers/user.py) | 定义 HTTP 请求入口、参数或响应。可从 `register` 、 `login` 、 `get_profile` 、 `update_profile` 开始 |
| [routers/weather.py](routers/weather.py) | 定义 HTTP 请求入口、参数或响应。可从 `_check_amap_config` 、 `_call_amap` 、 `weather_current` 、 `weather_forecast` 开始 |
| [schemas/__init__.py](schemas/__init__.py) | 定义接口或业务数据结构 |
| [schemas/asr.py](schemas/asr.py) | 定义接口或业务数据结构。可从 `ASRStatusResponse` 、 `ASRPreloadResponse` 、 `ASRLocalModel` 、 `ASRModelsResponse` 开始 |
| [schemas/avatar.py](schemas/avatar.py) | 定义接口或业务数据结构。可从 `AvatarBaseResponse` 、 `AvatarStreamConfig` 、 `AvatarStartRequest` 、 `_norm_user_id` 开始 |
| [schemas/checkin.py](schemas/checkin.py) | 定义接口或业务数据结构。可从 `CheckinAddRequest` 、 `CheckinAddData` 、 `CheckinAddResponse` 、 `ProvinceItem` 开始 |
| [schemas/post.py](schemas/post.py) | 定义接口或业务数据结构。可从 `PostCreateRequest` 、 `Config` 、 `PostUpdateRequest` 、 `PostImageResponse` 开始 |
| [schemas/realtime_explain.py](schemas/realtime_explain.py) | 定义接口或业务数据结构。可从 `RealtimeExplainAddRequest` 、 `_norm_user_id` 、 `Config` 、 `RealtimeExplainItem` 开始 |
| [schemas/suggestion.py](schemas/suggestion.py) | 定义接口或业务数据结构。可从 `SuggestionCreateRequest` 、 `Config` 、 `SuggestionItem` 开始 |
| [schemas/user.py](schemas/user.py) | 定义接口或业务数据结构。可从 `UserRegisterRequest` 、 `validate_username` 、 `validate_password` 、 `validate_email` 开始 |
| [schemas/weather.py](schemas/weather.py) | 定义接口或业务数据结构。可从 `WeatherQuery` 、 `Config` 、 `WeatherLiveItem` 、 `WeatherLiveResponse` 开始 |
| [test/conftest.py](test/conftest.py) | 验证功能或回归场景。可从 `api` 、 `rand_str` 、 `base_url` 、 `check_server` 开始 |
| [test/test_01_health.py](test/test_01_health.py) | 该目录下的业务实现或工具代码。可从 `TestHealth` 、 `test_root_endpoint_returns_200` 、 `test_root_endpoint_status_ok` 、 `test_root_endpoint_has_message` 开始 |
| [test/test_02_user.py](test/test_02_user.py) | 该目录下的业务实现或工具代码。可从 `_new_user_payload` 、 `TestUserRegister` 、 `test_register_success` 、 `test_register_response_has_code_and_msg` 开始 |
| [test/test_03_post.py](test/test_03_post.py) | 该目录下的业务实现或工具代码。可从 `_create_post` 、 `TestCreatePost` 、 `test_create_post_success` 、 `test_create_post_response_structure` 开始 |
| [test/test_04_like.py](test/test_04_like.py) | 该目录下的业务实现或工具代码。可从 `_create_post` 、 `_create_comment` 、 `TestTogglePostLike` 、 `fresh_post` 开始 |
| [test/test_05_comment.py](test/test_05_comment.py) | 该目录下的业务实现或工具代码。可从 `_create_post` 、 `TestCreateComment` 、 `post_id` 、 `test_create_top_level_comment_success` 开始 |
| [test/test_06_upload.py](test/test_06_upload.py) | 该目录下的业务实现或工具代码。可从 `_make_image` 、 `_upload_image` 、 `_upload_video` 、 `TestUploadSingleImage` 开始 |
| [test/test_07_avatar.py](test/test_07_avatar.py) | 该目录下的业务实现或工具代码。可从 `TestAvatarStatic` 、 `test_vcn_list_returns_200` 、 `test_vcn_list_data_is_nonempty_list` 、 `test_vcn_list_item_structure` 开始 |
| [test/test_08_asr.py](test/test_08_asr.py) | 该目录下的业务实现或工具代码。可从 `generate_silence_wav` 、 `generate_tone_wav` 、 `wav_to_base64_chunks` 、 `server_is_up` 开始 |
| [test/test_09_weather.py](test/test_09_weather.py) | 该目录下的业务实现或工具代码。可从 `run_test` 、 `test_current_beijing` 、 `test_current_adcode` 、 `test_current_multi_city` 开始 |
| [test/test_10_avatar_chat.py](test/test_10_avatar_chat.py) | 该目录下的业务实现或工具代码。可从 `_cleanup` 、 `test_write` 、 `test_admin_read` 、 `main` 开始 |
| [test/test_11_personal_center.py](test/test_11_personal_center.py) | 该目录下的业务实现或工具代码。可从 `_cleanup` 、 `test_suggestion` 、 `test_my_likes_and_comments` 、 `test_admin_read` 开始 |
| [test/test_12_checkin.py](test/test_12_checkin.py) | 该目录下的业务实现或工具代码。可从 `setup_user` 、 `teardown_user` 、 `main_test` 开始 |
| [test/test_13_realtime_explain.py](test/test_13_realtime_explain.py) | 该目录下的业务实现或工具代码。可从 `_cleanup` 、 `main` 开始 |
| [test/test_14_avatar_chat_history.py](test/test_14_avatar_chat_history.py) | 该目录下的业务实现或工具代码。可从 `_cleanup` 、 `main` 开始 |
| [test/test_15_user_update.py](test/test_15_user_update.py) | 该目录下的业务实现或工具代码。可从 `_cleanup` 、 `main` 开始 |
| [test_api.py](test_api.py) | 验证功能或回归场景。可从 `req` 、 `section` 、 `req_avatar` 开始 |
| [utils/asr.py](utils/asr.py) | 该目录下的业务实现或工具代码。可从 `_get_model_path` 、 `get_vosk_model` 、 `is_model_ready` 、 `get_model_info` 开始 |
| [utils/crypto.py](utils/crypto.py) | 该目录下的业务实现或工具代码。可从 `hash_password` 、 `verify_password` 开始 |
| [utils/file_upload.py](utils/file_upload.py) | 该目录下的业务实现或工具代码。可从 `ensure_upload_dirs` 、 `get_file_extension` 、 `generate_unique_filename` 、 `save_upload_file` 开始 |
| [utils/xfyun_auth.py](utils/xfyun_auth.py) | 外部服务鉴权。可从 `_get_rfc1123_date` 、 `build_auth_url` 、 `build_auth_query_url` 、 `build_wss_auth_url` 开始 |

本页列出 58 个可阅读的项目文件，以及 0 个第三方或生成文件。
