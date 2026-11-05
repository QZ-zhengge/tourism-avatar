# 景区数字人管理平台 — 管理端（统一接口文档）

> 无锡灵山胜境 · Vue 3 前端配套后端  
> 与主项目 `SoftwareCupAI` 共享同一 MySQL 数据库 `hbuilder_app`  
> 服务端口：8005　Swagger：`http://服务器IP:8005/docs`  
> **本文档为管理端唯一的接口文档，包含本端全部端口（共 25 个）及详细调用方法。** 零散文档已合并删除，无需再参考其他文件。

---

## 一、快速启动

```bash
Z:\SoftwareCupAI_admin\.venv\Scripts\python.exe main.py
# Swagger: http://localhost:8005/docs
```

---

## 二、项目结构

```
Z:\SoftwareCupAI_admin/
├── main.py                    # FastAPI 入口，端口 8005
├── config.py                  # 共享主项目数据库配置
├── requirements.txt
├── database/
│   └── database.py            # SQLAlchemy + 自动建管理端相关表（含 avatar_catalog / avatar_chat_*）
├── models/
│   └── admin.py               # ORM 模型（含 AvatarCatalog、AvatarChatSession、AvatarChatMessage、UserSuggestion 等）
├── schemas/
│   └── admin.py               # 请求/响应 Pydantic 模型
├── routers/
│   ├── digital.py             # 数字人配置 + 形象列表（含 base64 头像）
│   ├── content.py             # 景点图片上传/列表/删除
│   ├── knowledge.py           # 知识库源文件管理
│   ├── dialogue.py            # 对话分析（读取游客端写入的 avatar_chat_* 表，2026-07-08 改为新结构）
│   ├── vcn.py                # 发音人管理（含试听音频）
│   ├── auth.py               # 管理员登录/注册/JWT
│   └── suggestion.py          # 用户建议信箱
└── uploads/
    ├── avatars/               # 数字人头像（{avatar_id}.png）
    ├── vcns/                  # 发音人试听音频（{vcn}.mp3）
    ├── images/                # 景点图片
    └── documents/             # 知识库源文件
```

---

## 三、通用约定

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

> 登录注册 / 信箱模块使用 `code: 0` 表示成功，与前端约定对齐。

### 错误响应

| HTTP 状态码 | code | 说明 |
|:---:|:---:|------|
| 200 | 200 | 成功 |
| 400 | — | 参数校验失败 |
| 404 | — | 资源不存在 |
| 500 | — | 服务器内部错误 |

### 鉴权

- 认证方式：Bearer Token（JWT），过期时间 24 小时。
- 除 `/health`、`/` 外，所有业务接口需在请求头携带 `Authorization: Bearer {token}`。
- 响应格式：登录注册 / 信箱模块成功为 `code: 0`（不同于其他模块的 `code: 200`）。

### 编码与时间

- 数据格式：JSON（文件上传接口除外，为 `multipart/form-data`）。
- 字符编码：UTF-8。
- 时间格式：ISO 8601（`YYYY-MM-DDTHH:MM:SS`）。

---

## 四、接口总览

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录注册 | POST | `/api/v1/auth/login` | 管理员登录（返回 JWT） |
| 登录注册 | POST | `/api/v1/auth/register` | 管理员注册 |
| 登录注册 | GET | `/api/v1/auth/me` | 获取当前用户信息 |
| 登录注册 | POST | `/api/v1/auth/logout` | 退出登录 |
| 信箱 | GET | `/api/v1/admin/suggestions` | 用户建议列表（游客端写入，连通；响应含 user_id/contact） |
| 信箱 | PUT | `/api/v1/admin/suggestions/{id}/read` | 标记已读 |
| 数字人 | GET | `/api/v1/admin/digital/config` | 获取数字人配置（含当前头像 base64） |
| 数字人 | POST | `/api/v1/admin/digital/config` | 保存数字人配置 |
| 数字人 | GET | `/api/v1/admin/digital/avatars` | 获取数字人形象列表（来自 avatar_catalog 表，含头像 base64） |
| 发音人 | GET | `/api/v1/admin/vcn/voices` | 获取发音人列表（含试听 base64） |
| 发音人 | POST | `/api/v1/admin/vcn/voices` | 添加发音人 |
| 发音人 | DELETE | `/api/v1/admin/vcn/voices/{id}` | 删除发音人 |
| 发音人 | POST | `/api/v1/admin/vcn/voices/{vcn}/audio` | 上传试听音频 |
| 内容 | POST | `/api/v1/admin/content/image/upload` | 上传景点图片 |
| 内容 | GET | `/api/v1/admin/content/images` | 获取图片列表 |
| 内容 | DELETE | `/api/v1/admin/content/image/{id}` | 删除图片 |
| 知识库 | GET | `/api/v1/admin/knowledge/files` | 源文件列表（分页+搜索+格式筛选） |
| 知识库 | GET | `/api/v1/admin/knowledge/files/{id}` | 获取文件元数据 |
| 知识库 | GET | `/api/v1/admin/knowledge/files/{id}/content` | 获取文件内容（前端展示） |
| 知识库 | POST | `/api/v1/admin/knowledge/upload` | 上传源文件 |
| 知识库 | DELETE | `/api/v1/admin/knowledge/files/{id}` | 删除源文件 |
| 对话分析 | GET | `/api/v1/admin/dialogue/sessions` | 数字人对话历史-会话列表 |
| 对话分析 | GET | `/api/v1/admin/dialogue/sessions/{id}/messages` | 数字人对话历史-会话消息详情 |
| 健康检查 | GET | `/health` | 服务健康检查（根路径，返回 `{"status":"ok"}`） |
| 健康检查 | GET | `/` | 根路径，返回服务信息 |

> 以上路径均相对于 `/api/v1/admin`，完整路径示例：`/api/v1/admin/digital/config`。  
> **例外**：登录注册模块实际挂载在 `/api/v1/auth`（无 `/admin` 前缀），如 `/api/v1/auth/login`、`/api/v1/auth/register`。  
> 健康检查 `/health`、`/` 位于根路径，无 `/api/v1/admin` 前缀，无需鉴权。

---

## 五、登录注册

### 5.1 注册

```
POST /api/v1/auth/register
Content-Type: application/json

{"username":"admin","email":"user@example.com","password":"CHANGE_ME"}
```

| 字段 | 规则 |
|------|------|
| username | 必填，不能为空 |
| email | 必填，邮箱格式 |
| password | 必填，最少 6 位 |

**成功：** `{"code":0,"message":"注册成功"}`  
**冲突：** `{"code":409,"message":"该邮箱已注册"}` 或 `"该用户名已被占用"`

### 5.2 登录

```
POST /api/v1/auth/login
Content-Type: application/json

{"username":"admin","password":"CHANGE_ME"}
```

**成功：** `{"code":0,"message":"登录成功","data":{"token":"CHANGE_ME","user":{"id":1,"username":"admin","email":"...","role":"admin","avatar":null}}}`  
**失败：** `{"code":401,"message":"用户名或密码错误"}`

### 5.3 获取当前用户

```
GET /api/v1/auth/me
Authorization: Bearer {token}
```

**成功：** `{"code":0,"data":{"id":1,"username":"admin","email":"...","role":"admin"}}`

### 5.4 退出登录

```
POST /api/v1/auth/logout
Authorization: Bearer {token}
```

**成功：** `{"code":0,"message":"已退出"}`

---

## 六、信箱（用户建议）

> 数据来源：游客端 `POST /suggestions/`（及 `GET /suggestions/user/{user_id}`）写入的 `user_suggestions` 表，管理端仅读取与标记。  
> 响应新增字段（2026-07-09）：`user_id`（int，提交用户 ID，可能为 null）、`contact`（string，联系方式，可能为 null）。`user_name`/`content`/`status`/`created_at` 保持原样。

### 6.1 获取建议列表

```
GET /api/v1/admin/suggestions
Authorization: Bearer {token}
```

**响应：**

```json
{
  "items": [
    {
      "id": "sg_001",
      "user_id": 12,
      "user_name": "张三",
      "contact": "138xxxx8888",
      "content": "建议增加夜间游览路线",
      "status": "unread",
      "created_at": "2026-06-14T10:30:00"
    }
  ],
  "data": { "suggestions": [ "（同上）" ] }
}
```

> 同时兼容 `data.suggestions` 格式，前端两种均可读取。

### 6.2 标记已读

```
PUT /api/v1/admin/suggestions/{id}/read
Authorization: Bearer {token}
```

**响应：** `{"code":0,"message":"ok"}`  
**建议不存在：** `404 {"detail":"建议不存在"}`

---

## 七、数字人管理

### 7.1 获取数字人配置

```
GET /api/v1/admin/digital/config?scenic_id=lingshan
Authorization: Bearer {token}
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区 ID |

**响应：**

```json
{
  "scenic_id": "lingshan",
  "avatar_id": "111260002",
  "avatar_name": "雨菲",
  "avatar_base64": "data:image/png;base64,iVBORw0KGgo...",
  "vcn": "x4_xiaoxuan",
  "speed": 50,
  "pitch": 50,
  "volume": 50,
  "service_id": "315837983779590144",
  "layout": "fullscreen",
  "display_mode": "full",
  "sync_status": "saved",
  "updated_at": "2026-06-13T12:00:00.000Z"
}
```

> `avatar_base64` 为 data URI 格式，前端可直接用作 `<img src="...">` 的 `src` 值。若头像文件未上传则为 `null`。  
> 首次请求（未保存过配置）时返回默认值。

---

### 7.2 保存数字人配置

```
POST /api/v1/admin/digital/config
Content-Type: application/json
Authorization: Bearer {token}
```

**请求体：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区 ID |
| avatar_id | string | 是 | 111260002 | 数字人形象 ID |
| vcn | string | 是 | x4_xiaoxuan | 发音人 ID |
| speed | int | 是 | 50 | 语速，0~100 |
| pitch | int | 是 | 50 | 音调，0~100 |
| volume | int | 是 | 50 | 音量，0~100 |
| service_id | string | 是 | 315837983779590144 | 讯飞服务 ID |
| layout | string | 是 | fullscreen | 布局：`fullscreen` / `left` / `right` |
| display_mode | string | 是 | full | 显示：`head` / `upper` / `full` |

**请求示例：**

```json
{
  "scenic_id": "lingshan",
  "avatar_id": "111260002",
  "vcn": "x4_xiaoxuan",
  "speed": 50,
  "pitch": 50,
  "volume": 50,
  "service_id": "315837983779590144",
  "layout": "fullscreen",
  "display_mode": "full"
}
```

**校验规则：**

| 字段 | 规则 |
|------|------|
| avatar_id | 必须是 `avatar_catalog` 表中的有效形象 ID（当前共 55 个） |
| speed / pitch / volume | 整数，0~100 |
| layout | `fullscreen` / `left` / `right` |
| display_mode | `head` / `upper` / `full` |

**响应：**

```json
{
  "code": 200,
  "message": "配置保存成功",
  "data": {
    "saved_at": "2026-06-13T14:00:00.000Z",
    "sync_status": "saved"
  }
}
```

---

### 7.3 获取数字人形象列表（含头像）

```
GET /api/v1/admin/digital/avatars
Authorization: Bearer {token}
```

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "avatar_id": "118801001",
      "name": "依丹",
      "category": "通用",
      "status": "已授权",
      "avatar_base64": "data:image/png;base64,iVBORw0KGgo..."
    },
    {
      "avatar_id": "111260002",
      "name": "雨菲",
      "category": "通用",
      "status": "已授权",
      "avatar_base64": "data:image/png;base64,..."
    }
  ]
}
```

> 返回 `avatar_catalog` 表中的**全部数字人形象**（当前 55 个），含 base64 头像（data URI）。前端可直接用 `<img :src="item.avatar_base64">` 渲染。头像文件未上传的项 `avatar_base64` 为 `null`。  
> **字段说明**：`gender` 字段已移除，改为 `category`（资产分类，如"通用"）与 `status`（授权状态，如"已授权"）。  
> **头像文件命名规则**：`{avatar_id}.png`（或 `.jpg`），放入 `uploads/avatars/` 目录（如 `uploads/avatars/111260002.png`）。  
> **数字人形象目录（avatar_catalog 表）**：形象列表不再写死在代码里，而是从 `avatar_catalog` 表读取。该表在管理端（及用户端）首次启动时由 `data/avatar_catalog.json` 自动建表并写入全部 55 条种子数据（**幂等**：仅当表为空才写入）。如需增减形象，直接改数据库或重新播种即可。游客端获取默认形象名称（`/avatar/config`、`/avatar/start` 等）也从此表解析。

---

## 八、发音人管理

> 试听音频文件命名：`{vcn}.mp3`（或 `.wav`），放入 `uploads/vcns/` 目录。

### 8.1 发音人列表（含试听音频 base64）

```
GET /api/v1/admin/vcn/voices
Authorization: Bearer {token}
```

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "vcn": "x4_xiaoxuan",
      "name": "小璇",
      "gender": "女",
      "category": "中文普通话·交互",
      "language": "中文普通话",
      "audio_base64": "data:audio/mpeg;base64,..."
    }
  ]
}
```

> `audio_base64` 为 data URI，前端可直接用作 `<audio src="...">` 的 src。未上传试听音频的项为 `null`。

### 8.2 添加发音人

```
POST /api/v1/admin/vcn/voices
Content-Type: multipart/form-data
Authorization: Bearer {token}

vcn=x4_xiaoxuan&name=小璇&gender=女&category=中文普通话·交互&language=中文普通话
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| vcn | string | 是 | 发音人 ID，如 `x4_xiaoxuan` |
| name | string | 是 | 发音人名称 |
| gender | string | 否 | 性别 |
| category | string | 否 | 分类标签 |
| language | string | 否 | 语言 |

### 8.3 上传试听音频

```
POST /api/v1/admin/vcn/voices/{vcn}/audio
Content-Type: multipart/form-data
Authorization: Bearer {token}

file=@x4_xiaoxuan.mp3
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| file | file | 是 | 音频文件，支持 `.mp3` `.wav` `.ogg` `.m4a` |

### 8.4 删除发音人

```
DELETE /api/v1/admin/vcn/voices/{id}
Authorization: Bearer {token}
```

### 8.5 手动存数据方式

| 步骤 | 操作 |
|------|------|
| 1 | 调 `POST /vcn/voices` 添加发音人元数据 |
| 2 | 把试听音频文件放到 `uploads/vcns/`，命名为 `{vcn}.mp3` |
| 3 | 或直接调 `POST /vcn/voices/{vcn}/audio` 上传音频 |

---

## 九、内容管理（景点图片）

### 9.1 上传景点图片

```
POST /api/v1/admin/content/image/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

**请求体：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| file | file | 是 | — | 图片文件，支持 JPEG/PNG/GIF/WEBP/BMP |
| spot_name | string | 是 | — | 景点名称，如"灵山大佛" |
| scenic_id | string | 否 | lingshan | 景区 ID |

**响应：**

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 1,
    "spot_name": "灵山大佛",
    "file_name": "photo_20260613.jpg",
    "file_url": "/api/v1/admin/uploads/20260613142030_abc12345.jpg",
    "file_size": 204800,
    "created_at": "2026-06-13T12:00:00.000Z"
  }
}
```

> `file_url` 为图片访问路径，拼接服务器地址即可访问：  
> `http://服务器IP:8005/api/v1/admin/uploads/20260613142030_abc12345.jpg`

---

### 9.2 获取图片列表

```
GET /api/v1/admin/content/images?scenic_id=lingshan
Authorization: Bearer {token}
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区 ID |

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "spot_name": "灵山大佛",
      "file_name": "photo_20260613.jpg",
      "file_url": "/api/v1/admin/uploads/20260613142030_abc12345.jpg",
      "file_size": 204800,
      "created_at": "2026-06-13T12:00:00.000Z"
    }
  ]
}
```

> 按上传时间倒序排列。

---

### 9.3 删除图片

```
DELETE /api/v1/admin/content/image/{id}
Authorization: Bearer {token}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 图片 ID |

**响应：** `{"code":200,"message":"删除成功"}`

---

## 十、知识库（源文件管理）

支持格式：`.txt` / `.md` / `.docx` / `.pdf`

### 10.1 获取源文件列表

```
GET /api/v1/admin/knowledge/files?scenic_id=lingshan&search=&file_ext=&page=1&page_size=20
Authorization: Bearer {token}
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区 ID |
| search | string | 否 | — | 按文件名模糊搜索 |
| file_ext | string | 否 | — | 格式筛选：`txt` / `md` / `docx` / `pdf` |
| page | int | 否 | 1 | 页码（从 1 开始） |
| page_size | int | 否 | 20 | 每页条数 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 5,
    "page": 1,
    "page_size": 20,
    "items": [
      {
        "id": 3,
        "name": "景区介绍.txt",
        "file_size": 2355,
        "file_ext": "txt",
        "created_at": "2026-06-13T12:00:00.000Z"
      },
      {
        "id": 2,
        "name": "导游手册.pdf",
        "file_size": 1024000,
        "file_ext": "pdf",
        "created_at": "2026-06-12T10:00:00.000Z"
      }
    ]
  }
}
```

> 按上传时间倒序排列。

---

### 10.2 获取文件元数据

```
GET /api/v1/admin/knowledge/files/{id}
Authorization: Bearer {token}
```

**路径参数：** `id`（int，文件 ID）

**响应：**

```json
{
  "code": 200,
  "data": {
    "id": 3,
    "name": "景区介绍.txt",
    "file_size": 2355,
    "file_ext": "txt",
    "created_at": "2026-06-13T12:00:00.000Z"
  }
}
```

---

### 10.3 获取文件内容（前端页面展示）

```
GET /api/v1/admin/knowledge/files/{id}/content
Authorization: Bearer {token}
```

**路径参数：** `id`（int，文件 ID）

**响应行为：**

| 文件格式 | Content-Type | 前端展示方式 |
|---------|-------------|-------------|
| `.txt` `.md` | `text/plain; charset=utf-8` | 直接返回文本内容，前端用 `<pre>` 或富文本展示 |
| `.pdf` | `application/pdf` | 浏览器内嵌预览（`<iframe>` / `<embed>`） |
| `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | 触发下载，或用第三方库渲染 |

---

### 10.4 上传源文件

```
POST /api/v1/admin/knowledge/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}
```

**请求体：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| file | file | 是 | — | 文档文件，支持 `.txt` / `.md` / `.docx` / `.pdf` |
| scenic_id | string | 否 | lingshan | 景区 ID |

**响应：**

```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 3,
    "name": "景区介绍.txt",
    "file_size": 2355,
    "file_ext": "txt",
    "created_at": "2026-06-13T12:00:00.000Z"
  }
}
```

---

### 10.5 删除源文件

```
DELETE /api/v1/admin/knowledge/files/{id}
Authorization: Bearer {token}
```

**路径参数：** `id`（int，文件 ID）

**响应：** `{"code":200,"message":"删除成功"}`  
> 同时删除服务器磁盘上的文件。

---

## 十一、对话分析（数字人对话历史）

> **2026-07-08 变更**：响应字段已从旧的 `dialogue_*` 结构（`user_name` / `satisfaction_score` / `categories` / `sentiment` 等）替换为新的「数字人对话历史」结构。数据来源为游客端写入的 `avatar_chat_sessions` / `avatar_chat_messages` 两张新表，原 `dialogue_sessions` / `dialogue_messages` 表未改动、留空。

### 11.1 获取数字人对话历史会话列表

```
GET /api/v1/admin/dialogue/sessions?scenic_id=lingshan&search=&page=1&page_size=50
Authorization: Bearer {token}
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区 ID |
| search | string | 否 | — | 模糊搜索，匹配 `session_id` / `user_id` / `avatar_id`（**替代原 `user_name` 搜索**） |
| page | int | 否 | 1 | 页码（从 1 开始） |
| page_size | int | 否 | 50 | 每页条数（1–200） |

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 1,
    "page": 1,
    "page_size": 50,
    "items": [
      {
        "session_id": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
        "user_id": "u_123456",
        "scenic_id": "lingshan",
        "avatar_id": "118801001",
        "avatar_name": "依丹",
        "message_count": 4,
        "started_at": "2026-07-08T14:30:00",
        "ended_at": "2026-07-08T14:35:12"
      }
    ]
  }
}
```

---

### 11.2 获取会话消息详情

```
GET /api/v1/admin/dialogue/sessions/{session_id}/messages
Authorization: Bearer {token}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| session_id | string | 会话 ID，如 `f81d4fae-7dec-11d0-a765-00a0c91e6bf6`（来自 11.1 列表的 `session_id`） |

> **404**：`session_id` 不存在时返回 `{"detail":"会话不存在"}`，前端需处理该异常状态。

**响应：**

```json
{
  "code": 200,
  "data": {
    "session_id": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
    "user_id": "u_123456",
    "scenic_id": "lingshan",
    "avatar_id": "118801001",
    "avatar_name": "依丹",
    "message_count": 4,
    "started_at": "2026-07-08T14:30:00",
    "ended_at": "2026-07-08T14:35:12",
    "messages": [
      {
        "seq_no": 1,
        "role": "user",
        "content": "你知道灵山有哪些必看景点吗？",
        "source": "audio",
        "created_at": "2026-07-08T14:30:05"
      },
      {
        "seq_no": 2,
        "role": "assistant",
        "content": "灵山胜境必看景点有灵山大佛、九龙灌浴、梵宫……",
        "source": "text",
        "created_at": "2026-07-08T14:30:08"
      },
      {
        "seq_no": 3,
        "role": "user",
        "content": "那门票多少钱？",
        "source": "audio",
        "created_at": "2026-07-08T14:34:50"
      },
      {
        "seq_no": 4,
        "role": "assistant",
        "content": "成人票 210 元，学生票半价……",
        "source": "text",
        "created_at": "2026-07-08T14:34:53"
      }
    ]
  }
}
```

**响应字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| session_id / user_id / scenic_id / avatar_id / avatar_name | string\|null | 会话元信息（同 11.1） |
| message_count | int | 消息条数 |
| started_at / ended_at | string\|null | 会话起止时间（ISO 8601） |
| messages[].seq_no | int | 消息序号（从 1 递增） |
| messages[].role | string | `user`=游客提问 / `assistant`=数字人回答 |
| messages[].content | string | 消息文本（user 为提问，assistant 为播报文本） |
| messages[].source | string\|null | `audio`=语音输入 / `text`=文本输入 |
| messages[].created_at | string\|null | 消息时间（ISO 8601） |

---

## 十二、健康检查

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 健康检查 | GET | `/health` | 服务健康检查，返回 `{"status":"ok"}` |
| 健康检查 | GET | `/` | 根路径，返回服务信息 `{"service":"景区数字人管理平台","version":"1.0.0"}` |

**响应示例（`GET /health`）：**

```json
{ "status": "ok" }
```

> 供运维 / 容器探活（liveness probe）与网关健康检查使用，无需鉴权。

---

## 附录 A、数据库表

与主项目共享 `hbuilder_app` 库，管理端相关表（含与游客端共享的对话历史表，共 11 张）：

| 表名 | 说明 | 所属 |
|------|------|:---:|
| `admin_users` | 管理员账号（username/email/password/role） | 管理端 |
| `digital_human_config` | 数字人配置（avatar_id/vcn/speed 等） | 管理端 |
| `avatar_catalog` | 数字人形象目录（55 条种子：avatar_id/name/category/status），形象列表与名称解析的数据源 | 管理端 |
| `vcn_voices` | 发音人元数据 | 管理端 |
| `scenic_images` | 景点图片 | 管理端 |
| `knowledge_documents` | 知识库源文件 | 管理端 |
| `dialogue_sessions` | 对话会话（旧表，已停用，保留不删） | 管理端 |
| `dialogue_messages` | 对话消息（旧表，已停用，保留不删） | 管理端 |
| `avatar_chat_sessions` | **数字人对话历史-会话主表（2026-07-08 新增）**：游客端 `/avatar/start` 写入、`/avatar/stop` 更新结束时间；管理端「对话分析」只读 | 共享 |
| `avatar_chat_messages` | **数字人对话历史-消息明细表（2026-07-08 新增）**：游客端 `/avatar/drive/text` 写入 user+assistant 两轮；管理端只读 | 共享 |
| `user_suggestions` | 用户建议信箱（id/user_id/contact/content/status/created_at；**游客端写入、管理端读取**，2026-07-09 新增 user_id/contact 列） | 共享 |

> `avatar_catalog` 表在管理端（及用户端）启动时由 `data/avatar_catalog.json` 自动建表并播种（幂等）。  
> 主项目的 `/avatar/start` 和 `/avatar/drive/text` 每次请求实时读取 `digital_human_config` 表获取 avatar_id 和 vcn，管理端修改后自动同步到用户端；游客端默认形象名称则从 `avatar_catalog` 表解析。  
> **数字人对话历史（2026-07-08）**：游客端与数字人每轮对话（游客提问 + 数字人回答）自动写入 `avatar_chat_sessions` / `avatar_chat_messages` 两张新表，原 `dialogue_sessions` / `dialogue_messages` 表完全不动。管理端「对话分析」接口（`GET /api/v1/admin/dialogue/sessions` 等）改为读取新表。  
> **建议信箱（2026-07-09）**：游客端「消息通知」改为建议信箱，由游客端 `POST /suggestions/`（写入）+ `GET /suggestions/user/{user_id}`（我的建议）连通本端 `user_suggestions` 表；管理端读取接口 `GET /api/v1/admin/suggestions` 响应新增 `user_id` / `contact` 字段。

---

## 附录 B、手动配置数据

### 数字人头像

将头像图片命名为 `{avatar_id}.png` 放入 `uploads/avatars/`：

```
uploads/avatars/
├── 118801001.png   (依丹)
├── 111260002.png   (雨菲)
├── 110026010.png   (伊凡)
...
```

### 发音人试听音频

将音频文件命名为 `{vcn}.mp3` 放入 `uploads/vcns/`，或在管理端通过 `POST /vcn/voices` + `POST /vcn/voices/{vcn}/audio` 上传。
