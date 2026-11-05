# 景区数字人管理平台 — 后端 API 接口文档

> **基础路径**：`/api/v1/admin`  
> **数据格式**：JSON（文件上传除外）  
> **字符编码**：UTF-8  
> **时间格式**：ISO 8601  
> **服务端口**：8005  
> **Swagger**：http://服务器IP:8001/docs

---

## 通用约定

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

> 登录注册/信箱模块使用 `code: 0` 表示成功，与前端约定对齐。

### 错误响应

| HTTP 状态码 | code | 说明 |
|:---:|:---:|------|
| 200 | 200 | 成功 |
| 400 | — | 参数校验失败 |
| 404 | — | 资源不存在 |
| 500 | — | 服务器内部错误 |

---

## 接口总览

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 登录注册 | POST | `/auth/login` | 管理员登录（返回JWT） |
| 登录注册 | POST | `/auth/register` | 管理员注册 |
| 登录注册 | GET | `/auth/me` | 获取当前用户信息 |
| 登录注册 | POST | `/auth/logout` | 退出登录 |
| 信箱 | GET | `/admin/suggestions` | 用户建议列表 |
| 信箱 | PUT | `/admin/suggestions/{id}/read` | 标记已读 |
| 数字人 | GET | `/digital/config` | 获取数字人配置（含当前头像base64） |
| 数字人 | POST | `/digital/config` | 保存数字人配置 |
| 数字人 | GET | `/digital/avatars` | 获取12个形象列表（含头像base64） |
| 发音人 | GET | `/vcn/voices` | 获取发音人列表（含试听base64） |
| 发音人 | POST | `/vcn/voices` | 添加发音人 |
| 发音人 | DELETE | `/vcn/voices/{id}` | 删除发音人 |
| 发音人 | POST | `/vcn/voices/{vcn}/audio` | 上传试听音频 |
| 内容 | POST | `/content/image/upload` | 上传景点图片 |
| 内容 | GET | `/content/images` | 获取图片列表 |
| 内容 | DELETE | `/content/image/{id}` | 删除图片 |
| 知识库 | GET | `/knowledge/files` | 源文件列表（分页+搜索+格式筛选） |
| 知识库 | GET | `/knowledge/files/{id}` | 获取文件元数据 |
| 知识库 | GET | `/knowledge/files/{id}/content` | 获取文件内容（前端展示） |
| 知识库 | POST | `/knowledge/upload` | 上传源文件 |
| 知识库 | DELETE | `/knowledge/files/{id}` | 删除源文件 |
| 对话 | GET | `/dialogue/sessions` | 会话列表 |
| 对话 | GET | `/dialogue/sessions/{id}/messages` | 会话消息详情 |

> 以上路径均相对于 `/api/v1/admin`，完整路径示例：`/api/v1/admin/digital/config`

---

## 零、登录注册

> 认证方式：Bearer Token（JWT），过期时间 24 小时  
> 响应格式：`code: 0` 为成功（不同于其他模块的 code:200）

### 注册

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

### 登录

```
POST /api/v1/auth/login
Content-Type: application/json

{"username":"admin","password":"CHANGE_ME"}
```

**成功：** `{"code":0,"message":"登录成功","data":{"token":"CHANGE_ME","user":{"id":1,"username":"admin","email":"...","role":"admin","avatar":null}}}`  
**失败：** `{"code":401,"message":"用户名或密码错误"}`

### 获取当前用户

```
GET /api/v1/auth/me
Authorization: Bearer {token}
```

**成功：** `{"code":0,"data":{"id":1,"username":"admin","email":"...","role":"admin"}}`

### 退出登录

```
POST /api/v1/auth/logout
Authorization: Bearer {token}
```

**成功：** `{"code":0,"message":"已退出"}`

---

## 〇·五、信箱（用户建议）

### 获取建议列表

```
GET /api/v1/admin/suggestions
```

**响应：** `{"items":[{"id":"sg_001","user_name":"张三","content":"建议增加夜间游览路线","status":"unread","created_at":"2026-06-14T10:30:00"}]}`

> 同时兼容 `data.suggestions` 格式，前端两种均可读取。

### 标记已读

```
PUT /api/v1/admin/suggestions/{id}/read
```

**响应：** `{"code":0,"message":"ok"}`

---

## 一、数字人管理

### 1.1 获取数字人配置

```
GET /api/v1/admin/digital/config?scenic_id=lingshan
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区ID |

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

### 1.3 获取数字人形象列表（含头像）

```
GET /api/v1/admin/digital/avatars
```

**响应：**

```json
{
  "code": 200,
  "data": [
    {
      "avatar_id": "118801001",
      "name": "依丹",
      "gender": "女",
      "avatar_base64": "data:image/png;base64,iVBORw0KGgo..."
    },
    {
      "avatar_id": "111260002",
      "name": "雨菲",
      "gender": "女",
      "avatar_base64": "data:image/png;base64,..."
    }
  ]
}
```

> 返回全部 12 个形象，含 base64 头像（data URI）。前端可直接用 `<img :src="item.avatar_base64">` 渲染。头像文件未上传的项 `avatar_base64` 为 `null`。

> **头像文件命名规则**：`{avatar_id}.png`（或 `.jpg`），放入 `uploads/avatars/` 目录。  
> 例如：`uploads/avatars/111260002.png`、`uploads/avatars/118801001.jpg`

---

### 1.2 保存数字人配置

```
POST /api/v1/admin/digital/config
Content-Type: application/json
```

**请求体：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区ID |
| avatar_id | string | 是 | 111260002 | 数字人形象ID |
| vcn | string | 是 | x4_xiaoxuan | 发音人ID |
| speed | int | 是 | 50 | 语速，0~100 |
| pitch | int | 是 | 50 | 音调，0~100 |
| volume | int | 是 | 50 | 音量，0~100 |
| service_id | string | 是 | 315837983779590144 | 讯飞服务ID |
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
| avatar_id | 必须在 12 个已知形象ID 内 |
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

## 一·二、发音人管理

> 试听音频文件命名：`{vcn}.mp3`（或 `.wav`），放入 `uploads/vcns/` 目录

### 发音人列表（含试听音频 base64）

```
GET /api/v1/admin/vcn/voices
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

### 添加发音人

```
POST /api/v1/admin/vcn/voices
Content-Type: multipart/form-data

vcn=x4_xiaoxuan&name=小璇&gender=女&category=中文普通话·交互&language=中文普通话
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| vcn | string | 是 | 发音人ID，如 `x4_xiaoxuan` |
| name | string | 是 | 发音人名称 |
| gender | string | 否 | 性别 |
| category | string | 否 | 分类标签 |
| language | string | 否 | 语言 |

### 上传试听音频

```
POST /api/v1/admin/vcn/voices/{vcn}/audio
Content-Type: multipart/form-data

file=@x4_xiaoxuan.mp3
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| file | file | 是 | 音频文件，支持 `.mp3` `.wav` `.ogg` `.m4a` |

### 删除发音人

```
DELETE /api/v1/admin/vcn/voices/{id}
```

### 手动存数据方式

| 步骤 | 操作 |
|------|------|
| 1 | 调 `POST /vcn/voices` 添加发音人元数据 |
| 2 | 把试听音频文件放到 `uploads/vcns/`，命名为 `{vcn}.mp3` |
| 3 | 或直接调 `POST /vcn/voices/{vcn}/audio` 上传音频 |

---

## 二、内容管理（景点图片）

### 2.1 上传景点图片

```
POST /api/v1/admin/content/image/upload
Content-Type: multipart/form-data
```

**请求体：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| file | file | 是 | — | 图片文件，支持 JPEG/PNG/GIF/WEBP/BMP |
| spot_name | string | 是 | — | 景点名称，如"灵山大佛" |
| scenic_id | string | 否 | lingshan | 景区ID |

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
> `http://服务器IP:8001/api/v1/admin/uploads/20260613142030_abc12345.jpg`

---

### 2.2 获取图片列表

```
GET /api/v1/admin/content/images?scenic_id=lingshan
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区ID |

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

### 2.3 删除图片

```
DELETE /api/v1/admin/content/image/{id}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 图片ID |

**响应：**

```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

## 三、知识库（源文件管理）

支持格式：`.txt` / `.md` / `.docx` / `.pdf`

### 3.1 获取源文件列表

```
GET /api/v1/admin/knowledge/files?scenic_id=lingshan&search=&file_ext=&page=1&page_size=20
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区ID |
| search | string | 否 | — | 按文件名模糊搜索 |
| file_ext | string | 否 | — | 格式筛选：`txt` / `md` / `docx` / `pdf` |
| page | int | 否 | 1 | 页码（从1开始） |
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

### 3.2 获取文件元数据

```
GET /api/v1/admin/knowledge/files/{id}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 文件ID |

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

### 3.3 获取文件内容（前端页面展示）

```
GET /api/v1/admin/knowledge/files/{id}/content
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 文件ID |

**响应行为：**

| 文件格式 | Content-Type | 前端展示方式 |
|---------|-------------|-------------|
| `.txt` `.md` | `text/plain; charset=utf-8` | 直接返回文本内容，前端用 `<pre>` 或富文本展示 |
| `.pdf` | `application/pdf` | 浏览器内嵌预览（`<iframe>` / `<embed>`） |
| `.docx` | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` | 触发下载，或用第三方库渲染 |

---

### 3.4 上传源文件

```
POST /api/v1/admin/knowledge/upload
Content-Type: multipart/form-data
```

**请求体：**

| 字段 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| file | file | 是 | — | 文档文件，支持 `.txt` / `.md` / `.docx` / `.pdf` |
| scenic_id | string | 否 | lingshan | 景区ID |

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

### 3.5 删除源文件

```
DELETE /api/v1/admin/knowledge/files/{id}
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | int | 文件ID |

**响应：**

```json
{
  "code": 200,
  "message": "删除成功"
}
```

> 同时删除服务器磁盘上的文件。

---

## 四、对话分析

### 4.1 获取会话列表

```
GET /api/v1/admin/dialogue/sessions?scenic_id=lingshan&search=&page=1&page_size=50
```

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:---:|--------|------|
| scenic_id | string | 否 | lingshan | 景区ID |
| search | string | 否 | — | 按 session_id 或 user_name 模糊搜索 |
| page | int | 否 | 1 | 页码（从1开始） |
| page_size | int | 否 | 50 | 每页条数 |

**响应：**

```json
{
  "code": 200,
  "data": {
    "total": 50,
    "page": 1,
    "page_size": 50,
    "items": [
      {
        "session_id": "sess_abc123",
        "user_name": "游客ABC",
        "satisfaction_score": 85.0,
        "message_count": 3,
        "categories": ["门票咨询", "开放时间"],
        "last_updated": "2026-06-13T10:30:00.000Z",
        "reason": "【服务评价】游客对回答速度满意【改进方向】建议增加票价详情",
        "suggestion": "【优化方向】补充票价表【预期效果】减少重复提问"
      }
    ]
  }
}
```

---

### 4.2 获取会话消息详情

```
GET /api/v1/admin/dialogue/sessions/{session_id}/messages
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| session_id | string | 会话ID，如 `sess_abc123` |

**响应：**

```json
{
  "code": 200,
  "data": {
    "session_id": "sess_abc123",
    "satisfaction_score": 85.0,
    "reason": "【服务评价】游客对回答速度满意【改进方向】建议增加票价详情",
    "suggestion": "【优化方向】补充票价表【预期效果】减少重复提问",
    "messages": [
      {
        "seq_no": 1,
        "role": "user",
        "content": "景区几点开门？",
        "sentiment": "neutral",
        "created_at": "2026-06-13T10:30:00.000Z"
      },
      {
        "seq_no": 2,
        "role": "assistant",
        "content": "景区早上8点开门，成人票210元...",
        "sentiment": "positive",
        "created_at": "2026-06-13T10:32:00.000Z"
      }
    ]
  }
}
```

---

## 附录

### 数据库表

| 表名 | 说明 | 所属 |
|------|------|:---:|
| admin_users | 管理员账号 | 管理端 |
| digital_human_config | 数字人配置 | 管理端 |
| vcn_voices | 发音人 | 管理端 |
| scenic_images | 景点图片 | 管理端 |
| knowledge_documents | 知识库源文件 | 管理端 |
| dialogue_sessions | 对话会话 | 管理端 |
| dialogue_messages | 对话消息 | 管理端 |
| user_suggestions | 用户建议信箱 | 管理端 |
| users | 用户表 | 主项目 |
| posts | 帖子表 | 主项目 |
| post_images / post_videos / post_likes / post_comments | 社交模块 | 主项目 |

> 管理端 8 张表与主项目共享同一 MySQL 数据库 `hbuilder_app`。
