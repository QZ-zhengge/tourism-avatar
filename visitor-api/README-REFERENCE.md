# HBuilderX APP 后端服务

## 项目说明

FastAPI + MySQL 后端服务，为HBuilderX安卓APP提供API支持。

> **Swagger在线文档**: 启动服务后访问 http://localhost:8000/docs

---

## 快速开始

### 安装依赖

```bash
pip install -r requirements.txt
```

### 配置环境变量

> **所有敏感配置（密码、API密钥）已迁移至 `.env` 文件，`config.py` 不再包含任何凭证。**

1. 复制模板文件：
   ```bash
   cp .env.example .env
   ```

2. 编辑 `.env` 填入你的实际配置：
   ```ini
   DB_PASSWORD=你的MySQL密码
   XFYUN_APP_ID=你的讯飞APP_ID
   XFYUN_API_KEY=你的讯飞API_KEY
   XFYUN_API_SECRET=你的讯飞API_SECRET
   ```

3. `.env` 已在 `.gitignore` 中排除，不会被提交到 Git。

### 启动服务

```bash
python main.py
# 或
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

> 服务启动时自动检测并创建所有数据库表

---

## 接口总览

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 健康检查 | GET | `/health` | 服务状态 |
| **用户模块** | POST | `/user/register/` | 用户注册 |
| 用户模块 | POST | `/user/login/` | 用户登录 |
| 用户模块 | GET | `/user/profile/` | 获取用户个人资料 |
| 用户模块 | PUT | `/user/profile/` | 修改个人信息（昵称/头像/邮箱/手机号）|
| 用户模块 | PUT | `/user/password/` | 修改密码（验证旧密码）|
| **文件上传** | POST | `/upload/image` | 上传单张图片 |
| 文件上传 | POST | `/upload/images` | 批量上传图片（≤9张）|
| 文件上传 | POST | `/upload/video` | 上传视频 |
| **驴友圈-帖子** | POST | `/circle/posts/` | 发布帖子 |
| 驴友圈-帖子 | POST | `/circle/posts/{post_id}/media` | 帖子绑定图片/视频 |
| 驴友圈-帖子 | GET | `/circle/posts/` | 帖子广场（公开列表）|
| 驴友圈-帖子 | GET | `/circle/posts/user/{user_id}` | 用户帖子列表 |
| 驴友圈-帖子 | GET | `/circle/posts/{post_id}` | 帖子详情 |
| 驴友圈-帖子 | PUT | `/circle/posts/{post_id}` | 编辑帖子 |
| 驴友圈-帖子 | DELETE | `/circle/posts/{post_id}` | 删除帖子 |
| **驴友圈-点赞** | POST | `/circle/likes/post` | 帖子点赞/取消 |
| 驴友圈-点赞 | GET | `/circle/likes/post/{post_id}` | 点赞用户列表 |
| 驴友圈-点赞 | POST | `/circle/likes/comment` | 评论点赞 |
| 驴友圈-点赞 | GET | `/circle/likes/check` | 查询是否已点赞 |
| 驴友圈-点赞 | GET | `/circle/likes/user/{user_id}` | 我的点赞列表 |
| **驴友圈-评论** | POST | `/circle/comments/` | 发布评论/回复 |
| 驴友圈-评论 | GET | `/circle/comments/{post_id}` | 获取评论列表 |
| 驴友圈-评论 | DELETE | `/circle/comments/{comment_id}` | 删除评论 |
| 驴友圈-评论 | GET | `/circle/comments/user/{user_id}` | 我的评论列表 |
| **建议信箱** | POST | `/suggestions/` | 提交建议（游客端，连通管理端信箱） |
| 建议信箱 | GET | `/suggestions/user/{user_id}` | 我的建议列表 |
| **地图打卡** | POST | `/checkin/` | 添加一次省份打卡（幂等，地图点亮） |
| 地图打卡 | GET | `/checkin/user/{user_id}` | 获取用户已打卡省份编号列表 |
| 地图打卡 | GET | `/checkin/provinces` | 获取标准省份编号列表（地图底图） |
| **实时讲解历史** | POST | `/realtime-explain/history` | 添加一条实时讲解历史（数字人对话历史分支，原样存） |
| 实时讲解历史 | GET | `/realtime-explain/history/user/{user_id}` | 获取用户实时讲解历史列表（原样返回） |
| **虚拟人-会话** | POST | `/avatar/start` | 启动虚拟人会话（支持背景透明） |
| 虚拟人-驱动 | POST | `/avatar/drive/text` | 文本驱动（TTS播报）|
| 虚拟人-驱动 | POST | `/avatar/drive/audio/url` | 获取音频驱动 WSS 鉴权 URL |
| 虚拟人-驱动 | POST | `/avatar/drive/audio/proxy` | 音频帧代理转发 |
| 虚拟人-会话 | POST | `/avatar/stop` | 停止会话 |
| 虚拟人-保活 | POST | `/avatar/ping` | 心跳保活 |
| 虚拟人-查询 | GET | `/avatar/session/{session}` | 查询会话状态 |
| 虚拟人-配置 | GET | `/avatar/config` | 获取当前默认数字人配置（来自数据库，游客端随时读取）|
| 虚拟人-参考 | GET | `/avatar/actions/{avatar_id}` | 获取形象动作列表 |
| 虚拟人-参考 | GET | `/avatar/vcn/list` | 获取发音人列表 |
| 虚拟人-历史 | GET | `/avatar/chat/history/user/{user_id}` | 获取某用户的数字人对话历史列表（分页，含每条会话最后一条消息）|
| **语音识别-ASR** | WS | `/asr/stream` | Vosk 真正流式语音转文字（逐帧解码，< 500ms 延迟）|
| 语音识别-ASR | GET | `/asr/status` | 查询 Vosk 模型加载状态 |
| 语音识别-ASR | POST | `/asr/preload` | 预加载模型（提速首次识别）|
| 语音识别-ASR | GET | `/asr/models` | 列出本地已下载的 Vosk 模型 |
| **天气查询** | GET | `/weather/current` | 实况天气（高德API）|
| 天气查询 | GET | `/weather/forecast` | 天气预报（高德API）|
| 天气查询 | GET | `/weather/query` | 通用天气查询（可指定base/all）|

---

## 接口详细文档

---

### 一、用户模块

#### 1.1 用户注册 POST /user/register/

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | ✅ | 用户名（3-50位，字母/数字/下划线/中文）|
| password | string | ✅ | 密码（≥6位）|
| confirm_password | string | ✅ | 确认密码 |
| email | string | ✅ | 邮箱 |
| phone_number | string | ✅ | 手机号（1开头11位）|

**请求示例：**
```json
{
  "username": "wujunhui",
  "password": "CHANGE_ME",
  "confirm_password": "CHANGE_ME",
  "email": "wujunhui@example.com",
  "phone_number": "13800138000"
}
```

**响应示例：**
```json
// 成功
{"code": 200, "msg": "注册成功"}
// 失败
{"code": 400, "msg": "用户名已存在"}
```

---

#### 1.2 用户登录 POST /user/login/

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | ✅ | 用户名 |
| password | string | ✅ | 密码 |

**请求示例：**
```json
{"username": "wujunhui", "password": "CHANGE_ME"}
```

**响应示例：**
```json
// 成功
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "userId": 1,
    "username": "wujunhui",
    "nickname": "wujunhui",
    "email": "wujunhui@example.com",
    "phoneNumber": "13800138000"
  }
}
// 失败
{"code": 400, "msg": "用户名或密码错误"}
```

---

#### 1.3 获取用户个人资料 GET /user/profile/

**Query 参数**：`user_id`（number，必填）

**响应示例：**
```json
{
  "code": 200, "msg": "ok",
  "data": {
    "id": 4, "username": "xiaowu", "nickname": "小吴",
    "email": "xiaowu@example.com", "phone_number": "13800138000",
    "avatar": "https://cdn.example.com/avatar.png",
    "status": 1, "create_time": "2026-07-09 10:00:00"
  }
}
```

#### 1.4 修改个人信息 PUT /user/profile/

**请求体（JSON）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| user_id | number | ✅ | 用户ID |
| nickname | string | ❌ | 新昵称（不传则不修改）|
| avatar | string | ❌ | 头像URL（不传则不修改）|
| email | string | ❌ | 新邮箱（不传则不修改，变更时做唯一性检查）|
| phone_number | string | ❌ | 新手机号（不传则不修改，变更时做唯一性检查）|

**请求示例：**
```json
{"user_id": 4, "nickname": "小吴"}
```

**响应示例：**
```json
{
  "code": 200, "msg": "修改成功",
  "data": { ... 完整用户信息，同 1.3 ... }
}
// 失败
{"code": 400, "msg": "邮箱已被其他用户使用"}
```

#### 1.5 修改密码 PUT /user/password/

**请求体（JSON）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| user_id | number | ✅ | 用户ID |
| old_password | string | ✅ | 旧密码（≥6位）|
| new_password | string | ✅ | 新密码（≥6位）|
| confirm_password | string | ✅ | 确认新密码 |

**请求示例：**
```json
{"user_id": 4, "old_password": "CHANGE_ME", "new_password": "CHANGE_ME", "confirm_password": "CHANGE_ME"}
```

**响应示例：**
```json
// 成功
{"code": 200, "msg": "密码修改成功", "data": null}
// 失败
{"code": 400, "msg": "旧密码不正确"}
{"code": 400, "msg": "两次输入的新密码不一致"}
```

---

### 二、文件上传模块

> 上传成功后，返回的 `url` 即为文件访问地址，可直接在APP中使用。
> 文件存储于服务器 `uploads/` 目录，通过 `http://服务器IP:8000/uploads/...` 直接访问。

#### 2.1 上传单张图片 POST /upload/image

**请求方式：** `multipart/form-data`

| 字段 | 类型 | 说明 |
|------|------|------|
| file | File | 图片文件（JPG/PNG/GIF/WEBP，≤10MB）|

**响应示例：**
```json
{
  "code": 200,
  "msg": "上传成功",
  "url": "/uploads/images/20260519143022_abc123.jpg",
  "filename": "photo.jpg",
  "size": 204800
}
```

---

#### 2.2 批量上传图片 POST /upload/images

**请求方式：** `multipart/form-data`

| 字段 | 类型 | 说明 |
|------|------|------|
| files | File[] | 图片文件列表（最多9张）|

**响应示例：**
```json
{
  "code": 200,
  "msg": "上传完成，成功 3 张，失败 0 张",
  "files": [
    {"sort_order": 0, "url": "/uploads/images/xxx1.jpg", "filename": "1.jpg", "size": 102400},
    {"sort_order": 1, "url": "/uploads/images/xxx2.jpg", "filename": "2.jpg", "size": 204800},
    {"sort_order": 2, "url": "/uploads/images/xxx3.jpg", "filename": "3.jpg", "size": 153600}
  ],
  "failed": []
}
```

---

#### 2.3 上传视频 POST /upload/video

**请求方式：** `multipart/form-data`

| 字段 | 类型 | 说明 |
|------|------|------|
| file | File | 视频文件（MP4/MOV/AVI/WEBM，≤100MB）|

**响应示例：**
```json
{
  "code": 200,
  "msg": "上传成功",
  "url": "/uploads/videos/20260519143022_xyz789.mp4",
  "filename": "travel.mp4",
  "size": 52428800
}
```

---

### 三、驴友圈-帖子模块

#### 发帖完整流程

```
1. 先上传图片/视频（可选） → 获得 url
2. POST /circle/posts/       → 创建帖子，获得 post_id
3. POST /circle/posts/{post_id}/media → 绑定图片/视频（可选）
```

---

#### 3.1 发布帖子 POST /circle/posts/

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | int | ✅ | 发布者用户ID |
| content | string | ❌ | 文字内容 |
| location_name | string | ❌ | 定位名称（如：北京市朝阳区）|
| latitude | string | ❌ | 纬度（如：39.908823）|
| longitude | string | ❌ | 经度（如：116.397470）|
| visibility | int | ❌ | 可见性：0公开（默认）、1仅好友、2私密 |

**请求示例：**
```json
{
  "user_id": 1,
  "content": "今天登顶泰山，风景绝美！",
  "location_name": "泰山风景区",
  "latitude": "36.256890",
  "longitude": "117.105835",
  "visibility": 0
}
```

**响应示例：**
```json
{"code": 200, "msg": "发布成功", "data": {"post_id": 5}}
```

---

#### 3.2 帖子绑定图片/视频 POST /circle/posts/{post_id}/media

> 发帖后，将已上传的文件URL绑定到帖子

**Query参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| post_id | int（路径）| 帖子ID |

**请求体（JSON）：**
```json
{
  "images": [
    {"url": "/uploads/images/xxx1.jpg", "sort_order": 0},
    {"url": "/uploads/images/xxx2.jpg", "sort_order": 1}
  ],
  "videos": [
    {
      "url": "/uploads/videos/xxx.mp4",
      "cover_url": "/uploads/images/cover.jpg",
      "duration": 60
    }
  ]
}
```

> `images` 和 `videos` 可只传其中一个，不可同时传（仿微信朋友圈，图片和视频互斥）

**响应示例：**
```json
{"code": 200, "msg": "绑定成功"}
```

---

#### 3.3 帖子广场 GET /circle/posts/

> 获取全部公开帖子，按发布时间倒序

**Query参数：**

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| current_user_id | int | 0 | 当前登录用户ID（用于判断是否已点赞）|
| page | int | 1 | 页码 |
| page_size | int | 10 | 每页数量（≤50）|

**响应示例：**
```json
{
  "code": 200, "msg": "ok",
  "total": 120, "page": 1, "page_size": 10,
  "data": [
    {
      "id": 5,
      "user_id": 1,
      "content": "今天登顶泰山，风景绝美！",
      "location_name": "泰山风景区",
      "latitude": "36.256890",
      "longitude": "117.105835",
      "visibility": 0,
      "like_count": 8,
      "comment_count": 3,
      "is_liked": false,
      "images": [
        {"id": 1, "url": "/uploads/images/xxx1.jpg", "sort_order": 0}
      ],
      "videos": [],
      "create_time": "2026-05-19 14:30:22",
      "update_time": "2026-05-19 14:30:22"
    }
  ]
}
```

---

#### 3.4 获取用户帖子列表 GET /circle/posts/user/{user_id}

**路径参数：** `user_id` - 目标用户ID

**Query参数：**

| 参数 | 类型 | 默认 | 说明 |
|------|------|------|------|
| current_user_id | int | 0 | 当前登录用户ID |
| page | int | 1 | 页码 |
| page_size | int | 10 | 每页数量 |

**响应结构**：同帖子广场。

---

#### 3.5 帖子详情 GET /circle/posts/{post_id}

**路径参数：** `post_id` - 帖子ID

**Query参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| current_user_id | int | 当前登录用户ID |

**响应示例：**
```json
{
  "code": 200, "msg": "ok",
  "data": { /* 同帖子列表中单条数据结构 */ }
}
```

---

#### 3.6 编辑帖子 PUT /circle/posts/{post_id}

**路径参数：** `post_id`

**Query参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | int | ✅ | 操作用户ID（必须是帖子作者）|

**请求体：**
```json
{
  "content": "修改后的内容",
  "location_name": "新定位",
  "visibility": 1
}
```

**响应示例：**
```json
{"code": 200, "msg": "编辑成功"}
{"code": 403, "msg": "无权限操作该帖子"}
```

---

#### 3.7 删除帖子 DELETE /circle/posts/{post_id}

**路径参数：** `post_id`

**Query参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | int | ✅ | 操作用户ID（必须是帖子作者）|

**响应示例：**
```json
{"code": 200, "msg": "删除成功"}
```

---

### 四、驴友圈-点赞模块

#### 4.1 帖子点赞/取消点赞 POST /circle/likes/post

> Toggle 机制：已点赞则取消，未点赞则点赞

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| post_id | int | ✅ | 帖子ID |
| user_id | int | ✅ | 用户ID |

**请求示例：**
```json
{"post_id": 5, "user_id": 1}
```

**响应示例：**
```json
// 点赞成功
{"code": 200, "msg": "点赞成功", "is_liked": true, "like_count": 9}
// 取消点赞
{"code": 200, "msg": "已取消点赞", "is_liked": false, "like_count": 8}
```

---

#### 4.2 获取点赞用户列表 GET /circle/likes/post/{post_id}

**路径参数：** `post_id`

**Query参数：** `page`、`page_size`（默认20）

**响应示例：**
```json
{
  "code": 200, "msg": "ok", "total": 8,
  "data": [
    {"user_id": 2, "like_time": "2026-05-19 15:00:00"},
    {"user_id": 3, "like_time": "2026-05-19 15:05:00"}
  ]
}
```

---

#### 4.3 评论点赞 POST /circle/likes/comment

**Query参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| comment_id | int | ✅ | 评论ID |
| user_id | int | ✅ | 用户ID |

**响应示例：**
```json
{"code": 200, "msg": "点赞成功", "like_count": 3}
```

---

#### 4.4 查询是否已点赞 GET /circle/likes/check

**Query参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| post_id | int | 帖子ID |
| user_id | int | 用户ID |

**响应示例：**
```json
{"code": 200, "msg": "ok", "is_liked": true}
```

---

#### 4.5 我的点赞列表 GET /circle/likes/user/{user_id}

> 按 user_id 反查该用户点赞过的帖子（仅正常状态），按点赞时间倒序，便于「我的点赞」页展示。

**路径参数：** `user_id`（int）

**Query参数：** `page`（默认1）、`page_size`（默认20，最大100）

**响应示例：**
```json
{
  "code": 200, "msg": "ok",
  "data": [
    {
      "post_id": 217,
      "like_time": "2026-07-09 15:58:34.891234",
      "post": {
        "id": 217, "content": "今天去爬山了", "location_name": "灵山",
        "like_count": 12, "comment_count": 3,
        "create_time": "2026-07-09 15:50:00.123456",
        "images": ["/uploads/images/xxx.jpg"]
      }
    }
  ],
  "total": 1, "page": 1, "page_size": 20
}
```

---

### 五、驴友圈-评论模块

#### 5.1 发布评论或回复 POST /circle/comments/

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| post_id | int | ✅ | 帖子ID |
| user_id | int | ✅ | 评论者用户ID |
| content | string | ✅ | 评论内容（1-1000字）|
| parent_id | int | ❌ | 回复的父评论ID（null表示一级评论）|

**请求示例（一级评论）：**
```json
{
  "post_id": 5,
  "user_id": 2,
  "content": "泰山真的很壮观！",
  "parent_id": null
}
```

**请求示例（回复评论）：**
```json
{
  "post_id": 5,
  "user_id": 3,
  "content": "是的，我也去过！",
  "parent_id": 10
}
```

**响应示例：**
```json
{
  "code": 200, "msg": "评论成功",
  "data": {
    "id": 12,
    "post_id": 5,
    "parent_id": null,
    "user_id": 2,
    "username": "zhangsan",
    "nickname": "小张",
    "content": "泰山真的很壮观！",
    "like_count": 0,
    "create_time": "2026-05-19 15:10:00"
  }
}
```

---

#### 5.2 获取评论列表 GET /circle/comments/{post_id}

> 返回树形结构：一级评论 + 每条下的全部回复

**路径参数：** `post_id`

**Query参数：** `page`（默认1）、`page_size`（默认20）

**响应示例：**
```json
{
  "code": 200, "msg": "ok", "total": 5,
  "data": [
    {
      "id": 10,
      "post_id": 5,
      "parent_id": null,
      "user_id": 2,
      "username": "zhangsan",
      "nickname": "小张",
      "content": "泰山真的很壮观！",
      "like_count": 2,
      "create_time": "2026-05-19 15:10:00",
      "replies": [
        {
          "id": 12,
          "post_id": 5,
          "parent_id": 10,
          "user_id": 3,
          "username": "lisi",
          "nickname": "小李",
          "content": "是的，我也去过！",
          "like_count": 0,
          "create_time": "2026-05-19 15:12:00",
          "replies": []
        }
      ]
    }
  ]
}
```

---

#### 5.3 删除评论 DELETE /circle/comments/{comment_id}

**路径参数：** `comment_id`

**Query参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_id | int | ✅ | 操作用户ID（必须是评论作者）|

**响应示例：**
```json
{"code": 200, "msg": "删除成功"}
{"code": 403, "msg": "无权限删除该评论"}
```

---

#### 5.4 我的评论列表 GET /circle/comments/user/{user_id}

> 按 user_id 反查该用户发过的评论（仅正常状态），按时间倒序，便于「我的评论」页展示。

**路径参数：** `user_id`（int）

**Query参数：** `page`（默认1）、`page_size`（默认20，最大100）

**响应示例：**
```json
{
  "code": 200, "msg": "ok",
  "data": [
    {
      "comment_id": 130, "post_id": 217, "content": "风景真不错！",
      "like_count": 0, "create_time": "2026-07-09 15:55:00.654321",
      "parent_id": null, "parent_content": null,
      "post": {"id": 217, "content": "今天去爬山了", "location_name": "灵山"}
    }
  ],
  "total": 1, "page": 1, "page_size": 20
}
```

---

## 数据库表结构

| 表名 | 说明 |
|------|------|
| users | 用户表 |
| posts | 帖子表 |
| post_images | 帖子图片表 |
| post_videos | 帖子视频表 |
| post_likes | 帖子点赞表 |
| post_comments | 帖子评论表 |
| digital_human_config | 数字人配置表（avatar_id/vcn/speed 等，驱动虚拟人，管理端写入） |
| avatar_catalog | 数字人形象目录（55 条种子：avatar_id/name/category/status），形象列表与名称解析的数据源（2026-07-07 新增） |
| avatar_chat_sessions | 数字人对话历史-会话主表（2026-07-08 新增；`/avatar/start` 写入，`/avatar/stop` 更新结束时间；管理端读取） |
| avatar_chat_messages | 数字人对话历史-消息明细表（2026-07-08 新增；`/avatar/drive/text` 写入 user+assistant 两轮；管理端读取） |
| user_suggestions | 用户建议信箱表（与管理端共享同一张表；游客端写入，管理端读取；2026-07-09 新增 user_id/contact 列） |
| province_codes | 标准省份编号表（GB/T 2260，34 条种子数据；2026-07-09 新增，供打卡校验与地图底图） |
| user_checkins | 用户省份打卡记录表（user_id + province_code 唯一，2026-07-09 新增） |
| realtime_explain_history | 实时讲解历史表（image + ai_response 原样存储，数字人对话历史分支，2026-07-10 新增） |

---

## 文件上传说明

- 图片存储路径：`uploads/images/`
- 视频存储路径：`uploads/videos/`
- 访问方式：`http://服务器IP:8000/uploads/images/文件名`
- 文件命名规则：`时间戳_UUID.扩展名`

---

## 项目结构

```
z:/SoftwareCupAI/
├── main.py                 # FastAPI主入口
├── config.py               # 配置文件
├── requirements.txt        # 依赖
├── README.md               # 本文档
├── database/
│   └── database.py         # 数据库连接和自动建表
├── models/
│   ├── user.py             # 用户模型
│   ├── post.py             # 帖子/图片/视频/点赞/评论模型
│   ├── digital_config.py   # 数字人配置模型（管理端共享表）
│   ├── avatar_chat.py      # 数字人对话历史模型（avatar_chat_sessions / avatar_chat_messages，2026-07-08 新增）
│   ├── suggestion.py       # 建议信箱模型（user_suggestions，与管理端共享表，2026-07-09 新增）
│   ├── checkin.py          # 地图打卡模型（province_codes / user_checkins，2026-07-09 新增）
│   └── realtime_explain.py # 实时讲解历史模型（realtime_explain_history，2026-07-10 新增）
├── schemas/
│   ├── user.py             # 用户请求/响应模型
│   ├── post.py             # 驴友圈请求/响应模型
│   ├── avatar.py           # 虚拟人请求/响应模型
│   ├── weather.py          # 天气请求/响应模型
│   ├── suggestion.py       # 建议信箱请求/响应模型（2026-07-09 新增）
│   ├── checkin.py          # 地图打卡请求/响应模型（2026-07-09 新增）
│   └── realtime_explain.py # 实时讲解历史请求/响应模型（2026-07-10 新增）
├── routers/
│   ├── user.py             # 用户接口
│   ├── post.py             # 帖子接口
│   ├── like.py             # 点赞接口
│   ├── comment.py          # 评论接口
│   ├── upload.py           # 文件上传接口
│   ├── avatar.py           # 讯飞虚拟人接口
│   ├── asr.py              # 语音转文字接口
│   ├── weather.py          # 高德天气查询接口
│   ├── suggestion.py       # 建议信箱接口（2026-07-09 新增）
│   ├── checkin.py          # 地图打卡接口（2026-07-09 新增）
│   └── realtime_explain.py # 实时讲解历史接口（2026-07-10 新增）
├── utils/
│   ├── crypto.py           # 密码工具
│   └── file_upload.py      # 文件上传工具
└── uploads/
    ├── images/             # 上传的图片
    └── videos/             # 上传的视频
```

---

## 六、虚拟人模块（讯飞在线虚拟人驱动）

> **服务授权能力**：在线虚拟人驱动  
> **官方文档**：https://www.xfyun.cn/doc/tts/virtual_human/API.html  
> **接口前缀**：`/avatar`  
> **架构**：本后端作为「安全代理层」，保护讯飞 APIKey/APISecret 不暴露给 uniapp 客户端

> **管理端联动**（2026-06-13 新增）：`/avatar/start` 和 `/avatar/drive/text` 的数字人形象ID（`avatar_id`）和发音人ID（`vcn`）不再使用 uniapp 传值，改为**每次请求实时从数据库 `digital_human_config` 表读取**。管理端修改配置后，用户端下次启动会话即可自动同步，无需 uniapp 前端改动。

### 前置配置

在 `config.py` 或项目根目录 `.env` 文件中配置讯飞凭证：

```python
# config.py 或 .env
XFYUN_APP_ID    = "你的讯飞APPID"
XFYUN_API_KEY   = "你的APIKey（32位）"
XFYUN_API_SECRET = "你的APISecret（32位）"
```

> 在讯飞控制台 https://console.xfyun.cn 创建应用，开通「在线虚拟人驱动」服务后可获取上述凭证。

---

### 完整调用流程

```
uniapp                         后端服务（本项目）              讯飞虚拟人服务
  │                                  │                             │
  │──① POST /avatar/start ──────────→│──→ POST vms2d_start ──────→│
  │←── {session, stream_url} ────────│←── {session, stream_url} ──│
  │                                  │                             │
  │  ② 用 stream_url 播放虚拟人画面   │                             │
  │                                  │                             │
  │──③ POST /avatar/drive/text ──────→│──→ POST vms2d_ctrl ───────→│
  │  （或 GET /avatar/drive/audio/url）│                            │
  │                                  │                             │
  │──④ POST /avatar/ping（每30s）─────→│──→ POST vms2d_ping ───────→│
  │                                  │                             │
  │──⑤ POST /avatar/stop ───────────→│──→ POST vms2d_stop ────────→│
```

---

### 6.1 启动虚拟人会话

**POST /avatar/start**

> **向后兼容**：`bg_transparent` 为新增可选参数，默认 `false`。原有 uniapp 请求不传该字段，行为与之前完全一致。

**请求体：**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| avatar_id | string | ❌ | 118801001 | 虚拟人形象ID（依丹默认）|
| width | int | ❌ | 1280 | 视频宽度 px |
| height | int | ❌ | 720 | 视频高度 px |
| protocol | string | ❌ | xrtc | 拉流协议：`xrtc`（低延迟）或 `rtmp` |
| bg_transparent | bool | ❌ | false | **（可选）** 开启透明背景。需配合 `xrtc` 协议，`rtmp` 下自动降级 |
| user_id | string / number | ❌ | — | **（可选，2026-07-08 新增）** 游客 ID，用于「数字人对话历史」归属。支持数字（Long）或字符串，后端归一为去空白的字符串；未登录态可留空（历史仍记录，仅 `user_id` 为空）。 |

**请求示例（原有 uniapp 无需修改）：**
```json
{
  "avatar_id": "118801001",
  "width": 1280,
  "height": 720,
  "protocol": "xrtc"
}
```

**请求示例（开启透明背景 + 携带游客 ID）：**
```json
{
  "avatar_id": "118801001",
  "width": 1280,
  "height": 720,
  "protocol": "xrtc",
  "bg_transparent": true,
  "user_id": "u_123456"
}
```
> 原 uniapp 请求不传 `user_id` 仍完全兼容，仅「数字人对话历史」中 `user_id` 为空。。

**响应示例（xrtc + 透明背景）：**
```json
{
  "code": 200,
  "msg": "启动成功",
  "data": {
    "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "stream_url": "xrtc://...",
    "xfyun_code": 0,
    "xfyun_message": "success",
    "warnings": []
  }
}
```

**响应示例（rtmp + 透明背景 → 自动降级）：**
```json
{
  "code": 200,
  "msg": "启动成功",
  "data": {
    "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "stream_url": "rtmp://...",
    "xfyun_code": 0,
    "xfyun_message": "success",
    "warnings": [
      "bg_transparent（透明背景）需要 xrtc 协议，rtmp 不支持 Alpha 通道，已自动关闭透明通道。"
    ]
  }
}
```

> `session` 是后续所有接口的必传字段。  
> `stream_url`：**xrtc** 需讯飞 Android/iOS SDK 播放（支持透明背景）；**rtmp** 可直接用 `<live-player>` 播放（不支持透明背景）。  
> `warnings`：协议不兼容时自动降级，前端应检查此字段并提示用户。

#### 播放方案对照

| 场景 | 协议 | 播放方式 | 透明背景 | 适用端 |
|------|------|---------|:---:|------|
| **需要透明背景** | xrtc | 讯飞 Android/iOS 原生 SDK（uniapp 原生插件封装） | ✅ | Android / iOS |
| **不需要透明背景** | rtmp | uniapp `<live-player>` 组件（开箱即用） | ❌ | H5 / 小程序 / App |

> **uniapp 播放 xrtc 流**：uniapp 内置 `<live-player>` 不支持 xrtc 协议。如需透明背景，须将讯飞 Android/iOS SDK 封装为 **uniapp 原生插件**，在原生 View 层渲染数字人。

---

### 6.2 文本驱动（TTS 播报）

**POST /avatar/drive/text**

> 让虚拟人朗读文本，同步做嘴型动画和表情

**请求体：**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| session | string | ✅ | — | 启动接口返回的 session |
| text | string | ✅ | — | 要朗读的文字（最大64KB）|
| vcn | string | ❌ | x4_lingxiaoyu_assist | 发音人，见 `/avatar/vcn/list` |
| speed | int | ❌ | 50 | 语速（0-100）|
| pitch | int | ❌ | 50 | 语调（0-100）|
| volume | int | ❌ | 50 | 音量（0-100）|
| actions | array | ❌ | null | 动作控制列表（见下方）|
| user_id | string / number | ❌ | — | **（可选，2026-07-08 新增）** 游客 ID，与 `/avatar/start` 携带的 `user_id` 保持一致，用于「数字人对话历史」归属（支持数字/字符串，后端归一化） |
| user_text | string | ❌ | — | **（可选，2026-07-08 新增）** 游客**语音转文字后的提问文本**。传入时，在历史中额外记录一条 `role=user` 的消息；不传则只记录数字人回答（`role=assistant`）。⚠️ 语义：`user_text`=游客说的话，`text`=数字人回答（丢给 TTS 的文本），两者都会落库。 |

**actions 字段说明（可选）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| type | string | 固定填 `action` |
| value | string | 动作名称，见 `/avatar/actions/{avatar_id}` |
| wb | int | 动作起始字偏移，从第几个字开始触发动作（0起始）|

**请求示例（含 2026-07-08 新增字段）：**
```json
{
  "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "text": "欢迎体验讯飞虚拟人服务！",
  "vcn": "x4_lingxiaoyu_assist",
  "speed": 50,
  "pitch": 50,
  "volume": 50,
  "user_id": "u_123456",
  "user_text": "你知道灵山有哪些必看景点吗？",
  "actions": [
    {"type": "action", "value": "A_LH_introduced_O", "wb": 0}
  ]
}
```
> `text` 含义不变（仍为数字人播报文本）；新增 `user_text` 为游客语音转文字提问，与 `text` 独立，均会写入「数字人对话历史」。

**响应示例：**
```json
{
  "code": 200,
  "msg": "文本驱动成功，虚拟人正在口播",
  "xfyun_code": 0,
  "xfyun_message": "success"
}
```

---

### 6.3 获取音频驱动 WSS 鉴权 URL

**POST /avatar/drive/audio/url**

> 【直连模式】uniapp 拿到此 URL 后，自己用 WebSocket 直连讯飞服务发送音频数据

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| session | string | ✅ | 会话 session |

**响应示例：**
```json
{
  "code": 200,
  "msg": "获取成功",
  "data": {
    "wss_url": "wss://vms.cn-huadong-1.xf-yun.com/v1/private/vms2d_audio_ctrl?host=...&date=...&authorization=...",
    "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "app_id": "你的APPID",
    "encoding": "raw",
    "sample_rate": 16000
  }
}
```

**uniapp 接收后，使用 `uni.connectSocket(wss_url)` 建立连接，并按以下格式逐帧发送音频数据：**

```json
{
  "header": {
    "app_id": "你的APPID",
    "session": "会话session",
    "status": 0
  },
  "payload": {
    "audio": {
      "encoding": "raw",
      "sample_rate": 16000,
      "status": 0,
      "seq": 1,
      "audio": "Base64编码的PCM音频数据"
    }
  }
}
```

**`status` 含义：**  
- `0`：开始帧（首帧）
- `1`：中间帧（持续发送）
- `2`：结束帧（最后一帧，附带一个空字节 `AA==` 确保信号被接收）

---

### 6.4 音频帧代理转发（服务端中转模式）

**POST /avatar/drive/audio/proxy**

> 【中转模式】uniapp 将音频 Base64 发给本后端，后端持有 WSS 长连接代为转发

**请求体：**

| 字段 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| session | string | ✅ | — | 会话 session |
| audio_base64 | string | ✅ | — | Base64 编码的 PCM 音频（16kHz，单声道）|
| status | int | ✅ | — | 帧状态：0=开始，1=继续，2=结束 |
| seq | int | ❌ | 1 | 帧序号，从1递增 |
| encoding | string | ❌ | raw | 音频编码：raw/lame/opus-wb/speex-wb |
| sample_rate | int | ❌ | 16000 | 采样率（固定16000）|
| actions | array | ❌ | null | 动作控制（音频驱动时 wb 字段无效）|

**请求示例（开始帧）：**
```json
{
  "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "audio_base64": "UklGRiQAAABXQVZF...",
  "status": 0,
  "seq": 1,
  "encoding": "raw",
  "sample_rate": 16000
}
```

**响应示例：**
```json
{
  "code": 200,
  "msg": "音频帧转发成功",
  "xfyun_code": 0,
  "xfyun_message": "success"
}
```

---

### 6.5 停止会话

**POST /avatar/stop**

> **使用完毕后务必调用**，否则讯飞计费资源不会及时释放

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| session | string | ✅ | 要停止的会话 session |

**响应示例：**
```json
{
  "code": 200,
  "msg": "会话已停止，资源已释放",
  "xfyun_code": 0,
  "xfyun_message": "success"
}
```

---

### 6.6 心跳保活

**POST /avatar/ping**

> 讯飞服务端 **60 秒无请求** 自动断开，建议 uniapp 每 **30 秒** 调用一次

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| session | string | ✅ | 会话 session |

**响应示例：**
```json
{
  "code": 200,
  "msg": "心跳成功",
  "xfyun_code": 0,
  "xfyun_message": "success"
}
```

---

### 6.7 查询会话状态

**GET /avatar/session/{session}**

**路径参数：** `session` - 会话 ID

**响应示例：**
```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "session": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "avatar_id": "118801001",
    "stream_url": "xrtc://...",
    "protocol": "xrtc",
    "width": 1280,
    "height": 720,
    "bg_transparent": true,
    "create_time": "2026-05-27 13:00:00",
    "last_ping_time": "2026-05-27 13:05:00",
    "is_active": true
  }
}
```

---

### 6.8 获取当前默认数字人配置（数据库驱动）

**GET /avatar/config**

返回 `digital_human_config` 表中当前景区（`lingshan`）的默认数字人配置。
**游客端（uniapp）应在启动时调用此接口获取默认形象/发音人**，而非在前端写死。
管理端在「数字人管理」中修改并保存后，本接口**立即**返回最新值，游客端随之同步。

**响应示例（管理端已配置，source=db）：**
```json
{
  "code": 200,
  "msg": "ok",
  "data": {
    "scenic_id": "lingshan",
    "avatar_id": "110021007",
    "avatar_name": "晓娴(黑裙)",
    "vcn": "x4_xiaoxuan",
    "speed": 50, "pitch": 50, "volume": 50,
    "service_id": "315837983779590144",
    "layout": "fullscreen", "display_mode": "full",
    "updated_at": "2026-06-15T00:38:31",
    "sync_status": "saved",
    "source": "db"
  }
}
```

**响应说明：**
- `source=db`：配置来自数据库（管理端已保存），为唯一可信默认值
- `source=builtin_default`：管理端尚未保存任何配置，返回 `config.py` 中的内置兜底默认值（仅降级用，不写库）
- 当 uniapp 调用 `/avatar/start`、`/avatar/drive/text` 时不传 `avatar_id`/`vcn`，服务端也会用本接口读取到的 DB 值覆盖
- `avatar_name` 由 `avatar_catalog` 表解析（形象ID→名称映射已从代码写死改为数据库驱动，新增 `avatar_catalog` 表，启动时自动建表+播种 55 条种子）

### 6.9 获取形象动作列表

**GET /avatar/actions/{avatar_id}**

**路径参数：** `avatar_id` - 虚拟人形象ID

**常用形象ID：**

| 形象ID | 名称 |
|--------|------|
| 118801001 | 依丹 |
| 110026010 | 伊凡 |

**响应示例：**
```json
{
  "code": 200,
  "msg": "ok",
  "avatar_id": "118801001",
  "actions": [
    {"value": "A_TALKING_LONG", "description": "说话段（长）"},
    {"value": "A_LISTENING", "description": "聆听"},
    {"value": "A_LH_introduced_O", "description": "左手向左上介绍"},
    {"value": "A_RH_bye_O", "description": "右手挥手再见"},
    {"value": "A_RH_good_O", "description": "右手点赞夸奖"}
  ]
}
```

---

### 6.9 获取发音人列表

**GET /avatar/vcn/list**

**响应示例：**
```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    {"vcn": "x4_lingxiaoyu_assist", "name": "聆小瑜-助手", "language": "中文普通话", "scene": "交互", "gender": "女"},
    {"vcn": "x4_lingxiaozhen_eclives", "name": "聆小臻-直播", "language": "中文普通话", "scene": "直播", "gender": "女"},
    {"vcn": "x4_lingyuyan", "name": "聆玉言", "language": "中文普通话", "scene": "通用", "gender": "女"},
    {"vcn": "x4_EnUs_Luna_assist", "name": "Luna-助手", "language": "美式英语", "scene": "交互", "gender": "女"},
    {"vcn": "x4_EnUs_Gavin_assist", "name": "Gavin-助手", "language": "美式英语", "scene": "交互", "gender": "男"}
  ]
}
```

---

### 6.10 数字人对话历史列表（个人中心用）GET /avatar/chat/history/user/{user_id}

> 前端 `DigitalHumanApi` 调用。数据来源：`avatar_chat_sessions`（会话主表） + `avatar_chat_messages`（消息明细），一次分页查询 + 一句 join 取每条会话最后一条消息。

**路径参数**：`user_id`（string/number，必填）— 需与 `/avatar/start` 携带的 `user_id` 完全一致。  
**Query 参数**：`page`（默认 1，≥1）、`page_size`（默认 20，1–100）

> `user_id` 前后端归一化规则：保存与查询两端均做「去首尾空白、数字(Long)转字符串、纯空白视为未登录」处理，避免「存得进、查不出」。

**响应示例**：
```json
{
  "code": 200, "msg": "ok",
  "data": {
    "total": 1, "page": 1, "page_size": 20,
    "items": [
      {
        "session_id": "xdF7sUElilEdfmAlfGFHu8h5EmHlgQAZgQAcfmIZhmRMg4W8hVvlfGF4tTIlilFkv7RbgVAcgmSkhWBHwQWLsmRcsVJohVA4gVI5gQgcglF9",
        "avatar_id": "118801001",
        "started_at": "2026-07-12T00:13:45",
        "ended_at": null,
        "message_count": 6,
        "last_message": "好的，帮您找到酒店推荐..."
      }
    ]
  }
}
```

> `last_message` = 该会话消息自增 id 最大者（即最新一条，user/assistant 均可）；`ended_at` 为 `null` 表示会话尚未调用 `/avatar/stop`。

---

### 错误码说明

| HTTP 状态码 | code | 说明 | 解决方法 |
|------------|------|------|---------|
| 503 | — | 讯飞服务未配置 | 在 config.py 配置 APPID/APIKey/APISecret |
| 401 | — | 讯飞鉴权失败 | 检查 APIKey/APISecret 是否正确 |
| 403 | — | 时钟偏移过大 | 确保服务器时间与标准时间差 <5 分钟 |
| 502 | — | 讯飞服务不可达 | 检查网络，域名不能用 IP 替代 |
| 200 | 400 | 讯飞业务层拒绝 | 查看 msg 中的 xfyun_code 排查 |
| 200 | 404 | 会话不存在或已过期 | 重新调用 /avatar/start 获取新 session |

---

### uniapp 接入示例（文本驱动完整流程）

```javascript
// 1. 启动虚拟人
const startRes = await uni.request({
  url: 'http://你的后端IP:8000/avatar/start',
  method: 'POST',
  data: { avatar_id: '118801001', width: 1280, height: 720, protocol: 'xrtc' }
})
const { session, stream_url } = startRes.data.data

// 2. 播放虚拟人视频流（伪代码，具体看 xrtc SDK 文档）
xrtcPlayer.play(stream_url)

// 3. 启动心跳（每30秒）
const heartbeat = setInterval(() => {
  uni.request({ url: 'http://你的后端IP:8000/avatar/ping', method: 'POST', data: { session } })
}, 30000)

// 4. 文本驱动（让虚拟人说话）
await uni.request({
  url: 'http://你的后端IP:8000/avatar/drive/text',
  method: 'POST',
  data: {
    session,
    text: '你好，欢迎使用虚拟人服务！',
    vcn: 'x4_lingxiaoyu_assist',
    speed: 50,
    actions: [{ type: 'action', value: 'A_LH_introduced_O', wb: 0 }]
  }
})

// 5. 结束时停止会话
clearInterval(heartbeat)
await uni.request({
  url: 'http://你的后端IP:8000/avatar/stop',
  method: 'POST',
  data: { session }
})
```

---

## 七、本地流式语音转文字（ASR / Vosk 真正流式）

> **引擎**：Vosk（CMU 开源离线 ASR 引擎，Kaldi 技术栈）  
> **接入方式**：WebSocket（真正逐帧流式）+ HTTP（查询/预热/模型列表）  
> **支持格式**：WAV · MP3 · M4A · OGG · WEBM · FLAC · AMR · PCM 原始流  
> **支持语言**：中文（zh）、英文（en）、日语（ja）、韩语（ko）等多语言  
> **真正流式**：Vosk 逐帧解码，每 10~20ms 音频即可输出文字，**端到端延迟 < 500ms**  
> **模型大小**：small 模型 40~50MB（手机可跑），large 模型 1~2GB（服务器高精度）

### 与 Whisper 的本质区别

| 特性 | Vosk（本次使用）| Whisper（旧方案）|
|--------|----------------|-------------------|
| 流式能力 | ✅ 真正逐帧流式，每帧输出 | ❌ 必须整段音频才能识别 |
| 延迟 | **< 500ms** | 2~3s（滑动窗口模拟）|
| 模型大小 | 40MB（small）~ 1.8GB（large）| 244MB（small）~ 1.5GB（large）|
| 离线运行 | ✅ 完全离线 | ✅ 完全离线 |
| 依赖 | 仅需 `pip install vosk` | 需 `torch` 500MB+ |
| 逐字输出 | ✅ `PartialResult()` 每帧调用 | ❌ 不支持 |

---

### 安装依赖

```bash
# 1. 安装 Vosk（PyPI 包名是 vosk，不是 vosk！）
pip install vosk>=0.3.45

# 2. 安装音频解码依赖（任意格式转 PCM）
pip install ffmpeg-python>=0.2.0

# 3. 安装系统 ffmpeg（必需）
# Windows:  scoop install ffmpeg  或  choco install ffmpeg
# Ubuntu:   sudo apt install ffmpeg
```

> **无需安装 torch！** Vosk 是 C++ 推理引擎，比 Whisper 轻量很多。

---

### 下载 Vosk 中文模型

```bash
# 创建模型目录
mkdir -p z:/SoftwareCupAI/models/vosk

# 下载中文轻量模型（42MB，推荐）
curl -L -o vosk-model-small-cn-0.22.zip ^
  https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip
unzip vosk-model-small-cn-0.22.zip -d z:/SoftwareCupAI/models/vosk/

# （可选）下载中文大模型（1.3GB，精度更高）
curl -L -o vosk-model-cn-0.22.zip ^
  https://alphacephei.com/vosk/models/vosk-model-cn-0.22.zip
```

| 模型 | 大小 | 精度（WER↓越好）| 适用场景 |
|------|------|----------------|---------|
| `vosk-model-small-cn-0.22` | 42MB | 23.54（SpeechIO-02）| **推荐**，CPU 快速推理 |
| `vosk-model-cn-0.22` | 1.3GB | 13.98（SpeechIO-02）| 服务器高精度场景 |
| `vosk-model-small-en-us-0.15` | 40MB | 9.85（Librispeech）| 英文轻量 |

> 模型存放路径可通过环境变量 `VOSK_MODEL_DIR` 覆盖，默认：`{项目}/models/vosk/`

---

### 7.1 真正流式语音转文字 WS /asr/stream

**连接地址**：`ws://你的后端IP:8000/asr/stream`

#### 通信协议（JSON 文本帧）

**客户端 → 服务端：**

| 步骤 | 类型 | 示例 |
|------|------|------|
| 1. 连接后先发配置帧 | `config` | `{"type":"config","language":"zh","format":"wav","sampleRate":16000}` |
| 2. 录音逐块发送 base64 音频 | `audio` | `{"type":"audio","data":"<base64>"}` |
| 3. 录音结束发送结束帧 | `end` | `{"type":"end"}` |
| *(可选)* 直接发二进制 PCM | *二进制帧* | `Uint8Array` 原始 int16 PCM 字节 |

> **低延迟技巧**：uniapp 录音用 `frameSize: 1`（每 256 字节推送一次），配合 `format: "pcm"`，可把延迟压到最低。

**服务端 → 客户端：**

| 类型 | 含义 | 示例 |
|------|------|------|
| `partial`（final=false） | 中间结果，**极高频推送**（每 ~100ms）| `{"type":"partial","text":"你好今天","final":false,"seq":1}` |
| `partial`（final=true） | 当前语音段结束，文字已稳定，可追加到 UI | `{"type":"partial","text":"你好今天天气","final":true,"seq":2}` |
| `final` | 会话结束，完整拼接结果 | `{"type":"final","text":"你好今天天气真不错。","seq":3}` |
| `error` | 出错 | `{"type":"error","msg":"..."}` |
| `ping` | 服务端心跳（30s 无数据触发）| `{"type":"ping"}` |

#### uniapp 完整接入示例（真正流式）

```javascript
// ── 真正流式 ASR 接入（Vosk 引擎，延迟 < 500ms）────────────────────
const BASE_WS = 'ws://你的后端IP:8000'

let socketTask = null
let recognizedText = ''
let recorderManager = null

// 1. 建立 WebSocket 连接
function startASR() {
  recognizedText = ''

  socketTask = uni.connectSocket({
    url: BASE_WS + '/asr/stream',
    success() { console.log('[ASR] WebSocket 连接成功') }
  })

  // 2. 连接成功后发送配置帧
  socketTask.onOpen(() => {
    socketTask.send({
      data: JSON.stringify({
        type: 'config',
        language: 'zh',        // zh | en | ja | ko
        format: 'wav',         // wav | mp3 | pcm（推荐 pcm 最低延迟）
        sampleRate: 16000      // 务必 16000（Vosk 要求）
      })
    })

    // 3. 开始录音（关键：frameSize 越小，流式延迟越低）
    recorderManager = uni.getRecorderManager()
    recorderManager.onFrameRecorded(({ frameBuffer }) => {
      // 每帧录音数据转 base64 立即发送（真正流式）
      const base64 = uni.arrayBufferToBase64(frameBuffer)
      if (socketTask) {
        socketTask.send({
          data: JSON.stringify({ type: 'audio', data: base64 })
        })
      }
    })

    recorderManager.start({
      duration: 60000,        // 最长 60s
      sampleRate: 16000,       // 必须 16000
      numberOfChannels: 1,       // 单声道
      format: 'wav',            // 推荐用 pcm（如果 uniapp 支持）
      frameSize: 1,             // 最小帧（256 字节 ≈ 8ms 音频），延迟最低
    })
  })

  // 4. 接收服务端推送的流式识别结果
  socketTask.onMessage(({ data }) => {
    const msg = JSON.parse(data)

    if (msg.type === 'partial') {
      // 中间结果（极高频，可实时展示在 UI 上）
      console.log('[ASR] 中间结果:', msg.text)
      recognizedText = msg.text

      if (msg.final) {
        // 该语音段已稳定，可追加到最终文本
        console.log('[ASR] 语音段结束，稳定文本:', msg.text)
      }
    } else if (msg.type === 'final') {
      // 整个录音会话的最终完整结果
      console.log('[ASR] 最终结果:', msg.text)
      recognizedText = msg.text
      // 此时可关闭连接或继续下一段
      // socketTask.close()
    } else if (msg.type === 'error') {
      console.error('[ASR] 错误:', msg.msg)
      stopASR()
    } else if (msg.type === 'ping') {
      // 心跳，忽略即可
    }
  })

  socketTask.onClose(() => {
    console.log('[ASR] 连接关闭')
    socketTask = null
  })
  socketTask.onError(e => console.error('[ASR] WebSocket 错误:', e))
}

// 5. 停止录音并发送结束帧
function stopASR() {
  if (recorderManager) {
    recorderManager.stop()
    recorderManager = null
  }
  // 发送结束帧
  if (socketTask) {
    socketTask.send({
      data: JSON.stringify({ type: 'end' })
    })
  }
}

// 6. 页面卸载时清理
function onUnload() {
  if (socketTask) {
    socketTask.close()
    socketTask = null
  }
}
```

#### 最低延迟方案（PCM 原始流，无 base64 开销）

```javascript
// 如果 uniapp 支持录制 PCM 原始格式（int16），可直接发二进制帧，零编码开销
// 注意：uni.connectSocket 的 binaryType 需支持 ArrayBuffer

recorderManager.onFrameRecorded(({ frameBuffer }) => {
  // 直接发送 ArrayBuffer，不经过 base64（最低延迟）
  if (socketTask) {
    socketTask.send({ data: frameBuffer })
  }
})
```

---

### 7.2 查询 ASR 模型状态 GET /asr/status

> 可在 APP 启动时轮询，确认模型加载完成后再显示录音按钮

**响应示例：**

```json
{
  "code": 200,
  "msg": "ok",
  "model_name": "vosk",
  "loaded": true,
  "backend": "vosk",
  "loaded_langs": ["zh"]
}
```

---

### 7.3 预加载模型 POST /asr/preload

> 触发后台加载，避免首次 WebSocket 连接时等待（Vosk 小模型加载约 1~3 秒）

**请求（query 参数）：** `?language=zh`（默认 zh）

**响应示例（模型未加载时）：**

```json
{"code": 200, "msg": "语言 zh 的模型预加载已在后台启动，请稍后通过 GET /asr/status 确认", "language": "zh"}
```

**响应示例（模型已加载时）：**

```json
{"code": 200, "msg": "语言 zh 的模型已加载，无需重复操作", "language": "zh"}
```

---

### 7.4 列出本地可用模型 GET /asr/models

> 检查哪些语言模型已下载解压到 `models/vosk/` 目录

**响应示例：**

```json
{
  "code": 200,
  "msg": "ok",
  "model_dir": "z:\SoftwareCupAI\models\vosk",
  "models": [
    {
      "lang": "zh",
      "dir_name": "vosk-model-small-cn-0.22",
      "path": "z:\SoftwareCupAI\models\vosk\vosk-model-small-cn-0.22",
      "ready": true
    }
  ]
}
```

---

### 延迟说明（Vosk 真正流式）

| 阶段 | 耗时 | 备注 |
|------|------|------|
| 音频帧到达后端 | < 50ms | 网络 + WebSocket 开销 |
| Vosk `AcceptWaveform()` 解码 | < 10ms | 纯 C++ 推理，极快 |
| `PartialResult()` 获取文字 | < 5ms | 每帧立即可读 |
| **端到端总延迟（CPU）** | **< 500ms** | 实际测试 ~200~400ms |
| 端到端延迟（GPU，无 GPU 加速）| 同 CPU | Vosk 主要用 CPU |

> **为什么比 Whisper 快这么多？**  
> Whisper 是端到端神经网络，必须看完一整段（≥2s）才能输出；  
> Vosk 是传统 Kaldi 混合系统 + 神经网络 LM，逐帧解码，每帧独立完成。

---

### 错误码说明

| error msg | 原因 | 解决方法 |
|-----------|------|---------|
| 等待配置帧超时 | 连接后 10s 内未发 config | 确保先发 config 帧再发音频 |
| Vosk 未安装 | `pip install vosk` 未执行 | `pip install vosk>=0.3.45`（注意包名是 vosk）|
| Vosk 模型未找到 | 模型未下载或路径错误 | 下载模型并解压到 `models/vosk/` 目录 |
| 系统未找到 ffmpeg | 格式转换工具缺失 | 安装 ffmpeg 到系统 PATH |
| base64 解码失败 | 客户端编码错误 | 检查 base64 编码逻辑 |


---

### 7.1 流式语音转文字 WS /asr/stream

**连接地址**：`ws://你的后端IP:8000/asr/stream`

#### 通信协议（全部使用 JSON 文本帧）

**客户端 → 服务端：**

| 步骤 | 消息类型 | 示例 |
|------|---------|------|
| 1. 连接后先发配置帧 | `config` | `{"type":"config","language":"zh","format":"wav"}` |
| 2. 录音过程中逐块发送 base64 音频 | `audio` | `{"type":"audio","data":"<base64>"}` |
| 3. 录音结束后发结束帧 | `end` | `{"type":"end"}` |

**服务端 → 客户端：**

| 消息类型 | 触发时机 | 示例 |
|---------|---------|------|
| `partial` | 每积累约 2s 音频后推送中间结果 | `{"type":"partial","text":"你好今天","seq":1}` |
| `final` | 收到 end 帧后推送完整最终结果 | `{"type":"final","text":"你好今天天气真不错。","seq":2}` |
| `error` | 发生错误时推送 | `{"type":"error","msg":"..."}` |

#### uniapp 接入示例

```javascript
// ── 完整的流式 ASR 接入示例 ──────────────────────────────────────
const BASE_WS = 'ws://你的后端IP:8000'

let socketTask = null
let recognizedText = ''

// 1. 建立 WebSocket 连接
function startASR() {
  recognizedText = ''
  
  socketTask = uni.connectSocket({
    url: BASE_WS + '/asr/stream',
    success() { console.log('[ASR] WebSocket 连接成功') }
  })

  // 2. 连接成功后发送配置帧
  socketTask.onOpen(() => {
    socketTask.send({
      data: JSON.stringify({
        type: 'config',
        language: 'zh',
        format: 'wav'
      })
    })

    // 3. 开始录音
    const recorder = uni.getRecorderManager()
    recorder.onFrameRecorded(({ frameBuffer, isLastFrame }) => {
      // 每帧录音数据转 base64 发送给服务端
      const base64 = uni.arrayBufferToBase64(frameBuffer)
      socketTask.send({
        data: JSON.stringify({ type: 'audio', data: base64 })
      })
      // 录音完毕时发送结束帧
      if (isLastFrame) {
        socketTask.send({ data: JSON.stringify({ type: 'end' }) })
      }
    })

    recorder.start({
      duration: 60000,        // 最长 60s
      sampleRate: 16000,
      numberOfChannels: 1,
      format: 'wav',
      frameSize: 4,           // 每 4KB 触发一次 onFrameRecorded（约 0.25s）
    })
  })

  // 4. 接收服务端推送的识别结果
  socketTask.onMessage(({ data }) => {
    const msg = JSON.parse(data)

    if (msg.type === 'partial') {
      // 中间结果（流式展示）
      console.log('[ASR] 中间结果:', msg.text)
      // 更新 UI（例如实时显示正在识别的内容）
      recognizedText = msg.text
    } else if (msg.type === 'final') {
      // 最终完整结果
      console.log('[ASR] 最终结果:', msg.text)
      recognizedText = msg.text
      socketTask.close()
    } else if (msg.type === 'error') {
      console.error('[ASR] 错误:', msg.msg)
      socketTask.close()
    }
  })

  socketTask.onClose(() => console.log('[ASR] 连接关闭'))
  socketTask.onError(e => console.error('[ASR] WebSocket 错误:', e))
}

// 5. 停止录音（手动停止时）
function stopASR() {
  uni.getRecorderManager().stop()
  // onFrameRecorded 的 isLastFrame=true 会自动触发发送 end 帧
}
```

---

### 7.2 查询 ASR 模型状态 GET /asr/status

> 可在 APP 启动时轮询，确认模型加载完成后再显示录音按钮

**响应示例：**
```json
{
  "code": 200,
  "msg": "ok",
  "model_name": "small",
  "loaded": true,
  "backend": "openai-whisper"
}
```

---

### 7.3 预加载模型 POST /asr/preload

> 触发后台加载，避免首次 WebSocket 连接时等待（加载约需 5~15 秒，视硬件而定）

**响应示例（模型未加载时）：**
```json
{"code": 200, "msg": "模型预加载已在后台启动，请稍后通过 GET /asr/status 确认"}
```

**响应示例（模型已加载时）：**
```json
{"code": 200, "msg": "模型已加载，无需重复操作"}
```

---

### 延迟说明（滑动窗口策略）

| 阶段 | 耗时 | 备注 |
|------|------|------|
| 音频积累触发阈值 | 2.0 s | 每 2s 音频触发一次 Whisper 识别 |
| Whisper small CPU 推理 | 约 0.5~1 s（GPU）/ 1~3 s（CPU）| 取决于服务器硬件 |
| 端到端延迟（CPU） | **≤ 3 s** | 2s 积累 + 最多 1s 推理 |
| 端到端延迟（GPU） | **< 1 s** | 推荐生产环境使用 |

> **提速建议**：服务器有 GPU 时安装 CUDA 版 torch，推理延迟可降至 0.5s 以内。

### 错误消息说明

| error msg | 原因 | 解决方法 |
|-----------|------|---------|
| 等待配置帧超时 | 连接后 10s 内未发 config | 确保先发 config 帧再发音频 |
| openai-whisper 未安装 | 服务端缺少依赖 | `pip install openai-whisper` |
| 系统未找到 ffmpeg | 格式转换工具缺失 | 安装 ffmpeg 到系统 PATH |
| base64 解码失败 | 客户端编码错误 | 检查 base64 编码逻辑 |

---

## 八、天气查询模块（高德地图 Web服务 API）

> **数据源**：高德地图 Web服务 API — 天气查询  
> **官方文档**：https://lbs.amap.com/api/webservice/guide/api/weatherinfo  
> **接口前缀**：`/weather`  
> **架构**：后端作为代理层，保护高德 API Key 不暴露给 uniapp 客户端

### 前置配置

在项目根目录 `.env` 文件中配置高德 API Key：

```ini
# 请到 https://console.amap.com/dev/key 创建「Web服务」类型应用后获取
AMAP_API_KEY=你的高德Web服务API_Key
```

> **免费配额**：5000次/天，并发 200 QPS，完全满足个人开发和小团队使用。

---

### 8.1 实况天气 GET /weather/current

> 查询指定城市的实时天气状况

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| city | string | ✅ | 城市名（如"北京"）或 adcode（如"110000"），多个用","分隔（最多10个）|

**请求示例：**

```
GET /weather/current?city=北京
GET /weather/current?city=110000
GET /weather/current?city=深圳,440300
```

**响应示例：**

```json
{
  "code": 200,
  "msg": "查询成功",
  "amap_status": "1",
  "amap_info": "OK",
  "data": [
    {
      "province": "北京",
      "city": "北京市",
      "adcode": "110000",
      "weather": "晴",
      "temperature": "28",
      "winddirection": "南风",
      "windpower": "≤3",
      "humidity": "45",
      "reporttime": "2026-06-04 14:00:00",
      "temperature_float": "28.0",
      "humidity_float": "45.0"
    }
  ]
}
```

**返回字段说明：**

| 字段 | 说明 |
|------|------|
| province | 省份名 |
| city | 城市名 |
| adcode | 区域编码 |
| weather | 天气现象（晴、多云、阴、小雨、中雨…） |
| temperature | 实时气温（摄氏度） |
| winddirection | 风向（东、南、西、北、东北…） |
| windpower | 风力等级 |
| humidity | 空气湿度（%） |
| reporttime | 数据发布时间 |

---

### 8.2 预报天气 GET /weather/forecast

> 查询指定城市未来 3~4 天天气预报

**Query 参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| city | string | ✅ | 城市名或 adcode |

**请求示例：**

```
GET /weather/forecast?city=深圳
GET /weather/forecast?city=440300
```

**响应示例：**

```json
{
  "code": 200,
  "msg": "查询成功",
  "amap_status": "1",
  "amap_info": "OK",
  "data": [
    {
      "city": "深圳市",
      "adcode": "440300",
      "province": "广东",
      "reporttime": "2026-06-04 11:00:00",
      "casts": [
        {
          "date": "2026-06-04",
          "week": "4",
          "dayweather": "多云",
          "nightweather": "阵雨",
          "daytemp": "32",
          "nighttemp": "26",
          "daywind": "东风",
          "nightwind": "东北风",
          "daypower": "≤3",
          "nightpower": "≤3"
        },
        {
          "date": "2026-06-05",
          "week": "5",
          "dayweather": "晴",
          "nightweather": "多云",
          "daytemp": "34",
          "nighttemp": "27",
          "daywind": "南风",
          "nightwind": "东南风",
          "daypower": "≤3",
          "nightpower": "≤3"
        }
      ]
    }
  ]
}
```

**casts 字段说明：**

| 字段 | 说明 |
|------|------|
| date | 日期（yyyy-MM-dd）|
| week | 星期（1~7）|
| dayweather | 白天天气现象 |
| nightweather | 夜间天气现象 |
| daytemp | 白天温度（℃）|
| nighttemp | 夜间温度（℃）|
| daywind | 白天风向 |
| nightwind | 夜间风向 |
| daypower | 白天风力 |
| nightpower | 夜间风力 |

> 预报每天更新 3 次：**8:00 / 11:00 / 18:00**

---

### 8.3 通用天气查询 GET /weather/query

> 通过 `extensions` 参数指定返回实况或预报天气

**Query 参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| city | string | ✅ | — | 城市名或 adcode |
| extensions | string | ❌ | base | base=实况天气，all=预报天气 |

**请求示例：**

```
GET /weather/query?city=北京&extensions=base
GET /weather/query?city=上海&extensions=all
```

**响应格式**：根据 `extensions` 参数分别返回实况或预报数据结构（同上 8.1 / 8.2）。

> **推荐**：直接使用 `/weather/current` 和 `/weather/forecast`，更语义化、更好维护。

---

### 8.4 uniapp 接入示例

```javascript
// ── 实况天气查询 ────────────────────────────────────
const res = await uni.request({
  url: 'http://你的后端IP:8000/weather/current',
  data: { city: '北京' },
})
if (res.data.code === 200) {
  const weather = res.data.data[0]
  console.log(`${weather.city}：${weather.weather}，${weather.temperature}℃`)
}

// ── 天气预报查询 ────────────────────────────────────
const forecastRes = await uni.request({
  url: 'http://你的后端IP:8000/weather/forecast',
  data: { city: '深圳' },
})
if (forecastRes.data.code === 200) {
  const { casts } = forecastRes.data.data[0]
  casts.forEach(day => {
    console.log(`${day.date}：${day.dayweather}，${day.daytemp}~${day.nighttemp}℃`)
  })
}
```

---

### 8.5 错误码说明

| HTTP 状态码 | code | 说明 | 解决方法 |
|------------|------|------|---------|
| 503 | — | 高德 API 未配置 | 在 .env 设置 AMAP_API_KEY |
| 502 | — | 高德业务层错误 | 查看 detail 中的 infocode |
| 200 | 400 | extensions 参数非法 | 只能为 base 或 all |

**常见高德 infocode：**

| infocode | 说明 | 解决方法 |
|----------|------|---------|
| 10000 | key 非法或过期 | 检查 AMAP_API_KEY |
| 10001 | 无查询结果 | 检查城市名/编码是否正确 |
| 10003 | 日调用量超限 | 等待第二天或升级配额 |
| 10012 | 权限不足 | 控制台开通「Web服务 API」|
| 20800 | 账号未开通配额 | 到控制台申请免费配额 |

---

## 九、地图打卡模块（地图点亮）

实现「地图打卡点亮」功能：后端以**标准省份编号（GB/T 2260，6 位行政区划代码）**记录用户点亮过的省份，前端按编号在地图上点亮。

> 数据表：`province_codes`（标准编号，34 条种子）、`user_checkins`（用户打卡，`user_id + province_code` 唯一约束）。后端启动时自动建表并灌入省份数据（幂等）。

### 9.1 标准省份编号列表 GET /checkin/provinces

**请求**：无参数

**响应示例**：
```json
{
  "code": 200,
  "msg": "ok",
  "data": [
    {"province_code": "110000", "province_name": "北京市"},
    {"province_code": "120000", "province_name": "天津市"},
    "...": "（共 34 条：23 省 + 5 自治区 + 4 直辖市 + 2 特别行政区）",
    {"province_code": "810000", "province_name": "香港特别行政区"},
    {"province_code": "820000", "province_name": "澳门特别行政区"}
  ]
}
```
> 前端渲染地图底图时获取「编号 ↔ 名称」映射，并用于校验提交的省份编号是否合法。

### 9.2 添加一次省份打卡 POST /checkin/

**请求体（JSON）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| user_id | int | ✅ | 用户ID（对应 `users.id`） |
| province_code | string | ✅ | 标准省份编号（须存在于 9.1 列表，如 `"110000"`） |

**请求示例**：
```json
{ "user_id": 51, "province_code": "110000" }
```

**响应示例（首次点亮）**：
```json
{
  "code": 200, "msg": "打卡成功",
  "data": { "province_code": "110000", "province_name": "北京市", "newly_added": true }
}
```

**响应示例（重复点亮同一省份，幂等不报错）**：
```json
{
  "code": 200, "msg": "该省份已点亮",
  "data": { "province_code": "110000", "province_name": "北京市", "newly_added": false }
}
```

**响应示例（省份编号无效）**：
```json
{ "code": 400, "msg": "无效的省份编号: 999999", "data": null }
```

### 9.3 获取用户已打卡省份编号列表 GET /checkin/user/{user_id}

**路径参数**：`user_id`（int，必填）

**响应示例**：
```json
{ "code": 200, "msg": "ok", "data": ["110000", "310000", "440000"] }
```
> `data` 为该用户已点亮的所有省份编号（字符串数组，按首次点亮时间升序）。前端直接遍历该数组在地图上点亮对应省份即可。

---

### 9.4 uniapp 接入示例

```javascript
// ── 地图打卡 ────────────────────────────────────────

// 1) 进入地图页：拉取省份底图 + 已点亮列表
const provinces = await uni.request({ url: '/checkin/provinces' })  // data: [{province_code, province_name}]
const mine = await uni.request({ url: `/checkin/user/${userId}` })  // data: ["110000", "310000"]

// 2) 用户点亮某省时
await uni.request({
  url: '/checkin/',
  method: 'POST',
  data: { user_id: userId, province_code: '110000' }
})
// 后端返回 data.newly_added=false 表示之前已点亮，前端可忽略
```

> 接口完整字段与示例见上文 §九 地图打卡模块。

---

## 十、实时讲解历史模块（数字人对话历史分支）

实现「实时讲解历史记录」：前端在实时讲解结束后，**主动上传图片**与 **AI 返回的含 JSON 文本**，后端仅做**原样存储、原样返回**，不做任何图像解码或 JSON 解析。适合「前端调用视觉模型得到讲解结果，再交后端存档」的场景。

> 数据表：`realtime_explain_history`（id / user_id / image / ai_response / scenic_id / created_at）。后端启动时自动建表。

### 10.1 添加一条实时讲解历史 POST /realtime-explain/history

**请求体（JSON）**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|:---:|------|
| user_id | string / number | ❌ | 用户 ID（前端传入，未登录可留空）。**支持数字（Long）或字符串**，后端统一归一为去首尾空白的字符串。 |
| image | string | ✅ | 前端上传的图片：base64 或 URL，原样存储，后端不分析（列类型 **LONGTEXT**，支持最大 ~4GB base64 图片） |
| ai_response | string | ✅ | AI 返回的含 JSON 文本，原样存储，后端不解析 |
| scenic_id | string | ❌ | 景区 ID（可选） |

**请求示例**：
```json
{
  "user_id": "u_123456",
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
  "ai_response": "{\"spots\":[{\"name\":\"灵山大佛\",\"duration\":30}],\"summary\":\"讲解1\"}",
  "scenic_id": "lingshan"
}
```

**响应示例**：
```json
{
  "code": 200, "msg": "ok",
  "data": {
    "id": 1, "user_id": "u_123456",
    "image": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQ...",
    "ai_response": "{\"spots\":[{\"name\":\"灵山大佛\",\"duration\":30}],\"summary\":\"讲解1\"}",
    "scenic_id": "lingshan", "created_at": "2026-07-10T19:20:00"
  }
}
```

### 10.2 获取用户实时讲解历史列表 GET /realtime-explain/history/user/{user_id}

**路径参数**：`user_id`（string，必填）  
**Query 参数**：`page`（默认 1）、`page_size`（默认 50，1–200）

**响应示例**（按时间倒序，原样返回）：
```json
{
  "code": 200, "msg": "ok",
  "data": {
    "total": 2, "page": 1, "page_size": 50,
    "items": [
      { "id": 2, "user_id": "u_123456", "image": "https://cdn.example.com/explain2.png",
        "ai_response": "{\"spots\":[{\"name\":\"九龙灌浴\"}]}", "scenic_id": "lingshan",
        "created_at": "2026-07-10T19:25:00" },
      { "id": 1, "user_id": "u_123456", "image": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
        "ai_response": "{\"spots\":[{\"name\":\"灵山大佛\"}]}", "scenic_id": "lingshan",
        "created_at": "2026-07-10T19:20:00" }
    ]
  }
}
```

> 前端渲染：`ai_response` 由前端自行 `JSON.parse` 取结构化字段（如 `spots` 数组）；`image` 直接作为 `<img>` 源或 base64 渲染。完整字段与示例见上文 §10.1 / §10.2。

> **排查：前端「实时讲解历史看不到」的常见原因**（后端已确认存储/查询逻辑正确，两端 user_id 均做归一化）：
> 1. **只有拍照识别流程才会写入**：纯文字对话（`sendText`）不会调用 `saveRealtimeExplain()`，只有拍照识别（`takePhoto` / `recognizeImageForOverlay`）才会写入 `realtime_explain_history` 表。若仅做了文字对话，列表自然为空——属预期行为。
> 2. **`user_id` 必须一致且非空**：保存时若未携带 `user_id`（或为空），记录落在 `user_id = NULL`，之后用 `user_id=4` 查询无法命中。请确保拍照识别时已登录并携带同一 `user_id`。
> 3. **数字/字符串兼容**：前端 `UserPrefs.userId()` 是 `Long`，现在后端 `user_id` 字段已支持数字（自动转字符串），保存与查询均归一化，不会因类型不一致而查不到。
> 验证方法：登录后在数字人页点「拍照识别」一张图，再进入「实时讲解历史」查看是否出现数据。
