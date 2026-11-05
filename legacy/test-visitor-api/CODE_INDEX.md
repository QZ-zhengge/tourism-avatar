# 文旅游客 API 测试快照 · 代码索引

从 [项目 README](README.md) 了解主流程。下表把当前快照中的源码、页面、脚本与主要配置按目录列出。职责根据代码声明与目录用途概括。

| 文件 | 职责或阅读线索 |
| --- | --- |
| [config.py](config.py) | 配置构建、应用启动或环境参数。可从 `Settings` 、 `DATABASE_URL` 、 `DATABASE_URL_WITH_DB` 、 `Config` 开始 |
| [database/__init__.py](database/__init__.py) | 该目录下的业务实现或工具代码 |
| [database/database.py](database/database.py) | 数据库入口。可从 `get_db` 、 `ensure_database_exists` 、 `ensure_tables_exist` 、 `ensure_database_and_tables` 开始 |
| [main.py](main.py) | 历史应用入口。可从 `lifespan` 、 `log_requests` 、 `global_exception_handler` 、 `root` 开始 |
| [models/__init__.py](models/__init__.py) | 定义接口或业务数据结构 |
| [models/digital_config.py](models/digital_config.py) | 定义接口或业务数据结构。可从 `DigitalHumanConfig` 开始 |
| [models/post.py](models/post.py) | 定义接口或业务数据结构。可从 `Post` 、 `PostImage` 、 `PostVideo` 、 `PostLike` 开始 |
| [models/user.py](models/user.py) | 定义接口或业务数据结构。可从 `User` 、 `to_dict` 开始 |
| [routers/__init__.py](routers/__init__.py) | 定义 HTTP 请求入口、参数或响应 |
| [routers/asr.py](routers/asr.py) | 定义 HTTP 请求入口、参数或响应。可从 `asr_stream` 、 `_push_partial` 、 `_process_pcm_chunk` 、 `asr_status` 开始 |
| [routers/avatar.py](routers/avatar.py) | 数字人接口。可从 `_check_config` 、 `_post_to_xfyun` 、 `avatar_start` 、 `avatar_text_drive` 开始 |
| [routers/comment.py](routers/comment.py) | 定义 HTTP 请求入口、参数或响应。可从 `_get_username` 、 `_build_comment` 、 `create_comment` 、 `get_post_comments` 开始 |
| [routers/like.py](routers/like.py) | 定义 HTTP 请求入口、参数或响应。可从 `toggle_post_like` 、 `get_post_likes` 、 `toggle_comment_like` 、 `check_like_status` 开始 |
| [routers/post.py](routers/post.py) | 动态业务。可从 `MediaImageItem` 、 `MediaVideoItem` 、 `PostMediaRequest` 、 `_build_post_response` 开始 |
| [routers/upload.py](routers/upload.py) | 定义 HTTP 请求入口、参数或响应。可从 `upload_single_image` 、 `upload_multiple_images` 、 `upload_single_video` 开始 |
| [routers/user.py](routers/user.py) | 定义 HTTP 请求入口、参数或响应。可从 `register` 、 `login` 开始 |
| [routers/weather.py](routers/weather.py) | 定义 HTTP 请求入口、参数或响应。可从 `_check_amap_config` 、 `_call_amap` 、 `weather_current` 、 `weather_forecast` 开始 |
| [schemas/__init__.py](schemas/__init__.py) | 定义接口或业务数据结构 |
| [schemas/asr.py](schemas/asr.py) | 定义接口或业务数据结构。可从 `ASRStatusResponse` 、 `ASRPreloadResponse` 、 `ASRLocalModel` 、 `ASRModelsResponse` 开始 |
| [schemas/avatar.py](schemas/avatar.py) | 定义接口或业务数据结构。可从 `AvatarBaseResponse` 、 `AvatarStreamConfig` 、 `AvatarStartRequest` 、 `Config` 开始 |
| [schemas/post.py](schemas/post.py) | 定义接口或业务数据结构。可从 `PostCreateRequest` 、 `Config` 、 `PostUpdateRequest` 、 `PostImageResponse` 开始 |
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
| [test_api.py](test_api.py) | 验证功能或回归场景。可从 `req` 、 `section` 、 `req_avatar` 开始 |
| [utils/asr.py](utils/asr.py) | 该目录下的业务实现或工具代码。可从 `_get_model_path` 、 `get_vosk_model` 、 `is_model_ready` 、 `get_model_info` 开始 |
| [utils/crypto.py](utils/crypto.py) | 该目录下的业务实现或工具代码。可从 `hash_password` 、 `verify_password` 开始 |
| [utils/file_upload.py](utils/file_upload.py) | 该目录下的业务实现或工具代码。可从 `ensure_upload_dirs` 、 `get_file_extension` 、 `generate_unique_filename` 、 `save_upload_file` 开始 |
| [utils/xfyun_auth.py](utils/xfyun_auth.py) | 该目录下的业务实现或工具代码。可从 `_get_rfc1123_date` 、 `build_auth_url` 、 `build_auth_query_url` 、 `build_wss_auth_url` 开始 |

本页列出 38 个可阅读的项目文件，以及 0 个第三方或生成文件。
