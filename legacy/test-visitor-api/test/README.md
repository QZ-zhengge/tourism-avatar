# 后端接口测试套件

独立的 pytest 测试套件，**与源码完全解耦**，全程通过 HTTP 请求调用运行中的后端服务。

## 目录结构

```
test/
├── conftest.py          公共 Fixtures（基础 URL、测试用户、测试帖子、请求工具函数）
├── pytest.ini           pytest 全局配置
├── README.md            本文档
├── test_01_health.py    健康检查（6 个测试）
├── test_02_user.py      用户模块 — 注册/登录（17 个测试）
├── test_03_post.py      帖子模块 — CRUD/分页/媒体（29 个测试）
├── test_04_like.py      点赞模块 — Toggle/计数/查询/评论赞（22 个测试）
├── test_05_comment.py   评论模块 — 树形结构/回复/删除（24 个测试）
├── test_06_upload.py    上传模块 — 图片/批量/视频/格式校验（22 个测试）
└── test_07_avatar.py    虚拟人模块 — 静态/校验/可达性/全流程（30 个测试）
```

**总计 ~150 个测试用例**，覆盖全部业务逻辑分支。

---

## 快速开始

### 1. 安装依赖

```bash
# 只需 pytest 和 requests，无需安装后端任何依赖
pip install pytest requests
```

### 2. 启动后端服务

```bash
# 必须先启动后端，测试才能运行
cd z:/SoftwareCupAI
.venv/Scripts/python.exe main.py
# 或：
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### 3. 运行测试

```bash
cd z:/SoftwareCupAI/test

# 运行全部测试
pytest -v

# 运行单个模块
pytest test_02_user.py -v

# 运行指定 class
pytest test_03_post.py::TestDeletePost -v

# 运行指定测试
pytest test_04_like.py::TestTogglePostLike::test_cancel_like_decrements_count -v

# 只跑前 5 个模块（跳过虚拟人，不需要讯飞凭证）
pytest test_01_health.py test_02_user.py test_03_post.py test_04_like.py test_05_comment.py -v
```

---

## 测试分层说明

| 模块 | 外部依赖 | 说明 |
|---|---|---|
| `test_01` ~ `test_05` | MySQL（后端本地） | 完全本地，无第三方 API |
| `test_06_upload` | 本地文件系统 | 内存构造文件，无需准备测试素材 |
| `test_07_avatar` A/B/C 节 | 无 / 讯飞服务（可达性） | A 节完全本地；B/C 节验证可达性和 Pydantic 校验 |
| `test_07_avatar` D 节 | 讯飞账号 + 形象权益 | 需配置凭证且形象已激活，否则自动 skip |

---

## 讯飞凭证配置

在 `z:/SoftwareCupAI/config.py` 或 `.env` 中设置：

```
XFYUN_APP_ID     = "your_app_id"
XFYUN_API_KEY    = "CHANGE_ME"
XFYUN_API_SECRET = "CHANGE_ME"
```

未配置时虚拟人 7-B 节测试会验证返回 503，7-D 节会自动 skip。

---

## 常见问题

| 错误 | 原因 | 解决 |
|---|---|---|
| `ConnectionRefusedError` | 后端未启动 | 先启动 `main.py` |
| `AssertionError: 服务响应异常` | 后端端口冲突 | 确认 8000 端口已被后端占用 |
| 大量 SKIP（7-D 节） | 讯飞凭证未配置 | 配置凭证后重跑，或忽略（7-A/B/C 节均已覆盖） |
| `test_batch_upload_partial_invalid_type` FAIL | 上传接口内容类型判断逻辑变化 | 检查 `utils/file_upload.py` 的 `ALLOWED_IMAGE_TYPES` |
