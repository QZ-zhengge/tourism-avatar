# 🏔️ 景区数字人管理平台 — 后端 API 接口规范文档

> **项目名称：** 无锡灵山胜境 · 景区数字人管理平台  
> **前端技术栈：** Vue 3 + Vite 5 + ECharts  
> **目标版本：** v1.0  
> **编写日期：** 2026-06-13

---

## 目录

1. [通用约定](#1-通用约定)
2. [模块一：数字人管理](#2-模块一数字人管理)
3. [模块二：内容管理](#3-模块二内容管理)
4. [模块三：知识库管理](#4-模块三知识库管理)
5. [模块四：对话分析](#5-模块四对话分析)
6. [附录：通用数据类型](#6-附录通用数据类型)

---

## 1. 通用约定

### 1.1 接口基础

| 项目 | 值 |
|---|---|
| 基础路径 | `/api/v1/admin` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 时间格式 | ISO 8601 (`YYYY-MM-DDTHH:mm:ss.sssZ`) |
| 日期格式 | `YYYY-MM-DD` |
| 分页 | 传参 `page`(从1开始) + `page_size`(默认20) |
| 错误响应 | `{ "detail": "错误描述" }` |

### 1.2 通用响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

> 若前端当前使用原生 `fetch` 直接取响应体，也可直接返回 data 对象，无需包裹 code/message。

---

## 2. 模块一：数字人管理

### 2.1 需求说明

管理员在后台配置数字人形象、语音参数和场景设置，配置保存后游客端自动同步。

### 2.2 数据表：`digital_human_config`

后端需提供一张配置表，存储当前生效的配置（每条景区一条记录，支持覆盖保存）。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | INTEGER (PK, AUTO) | ✅ | — | 主键 |
| `scenic_id` | VARCHAR(50) | ✅ | `lingshan` | 景区ID |
| `avatar_id` | VARCHAR(50) | ✅ | `111260002` | 当前选中的数字人形象ID |
| `vcn` | VARCHAR(50) | ✅ | `x4_xiaoxuan` | 发音人ID |
| `speed` | INTEGER | ✅ | `50` | 语速，范围 0-100 |
| `pitch` | INTEGER | ✅ | `50` | 音调，范围 0-100 |
| `volume` | INTEGER | ✅ | `50` | 音量，范围 0-100 |
| `service_id` | VARCHAR(100) | ✅ | `315837983779590144` | 讯飞服务ID |
| `layout` | VARCHAR(20) | ✅ | `fullscreen` | 布局模式: `fullscreen` \| `left` \| `right` |
| `display_mode` | VARCHAR(20) | ✅ | `full` | 显示模式: `head`(仅头部) \| `upper`(上半身) \| `full`(全身) |
| `updated_at` | DATETIME | ✅ | — | 最后更新时间 |
| `sync_status` | VARCHAR(20) | — | `saved` | 同步状态: `saved`(已同步) \| `saving`(同步中) |

### 2.3 静态数据：数字人形象列表

以下 12 个形象为讯飞官方数字人，**由前端维护为静态常量**，后端无需提供增删改接口，但需要提供查询接口以便确认哪些形象可用。

| avatar_id | name | gender |
|---|---|---|
| `118801001` | 依丹 | 女 |
| `110026010` | 伊凡 | 男 |
| `110022010` | 晓云 | 女 |
| `110017006` | 马可 | 男 |
| `110005018` | 知新 | 男 |
| `110029003` | 晓姿(粉色衬衫) | 女 |
| `110029002` | 晓姿(白色西装) | 女 |
| `110021007` | 晓娴(黑裙) | 女 |
| `110021006` | 晓娴(灰色西装) | 女 |
| `110026013` | 伊凡(蓝色西装) | 男 |
| `110023004` | 小雅 | 女 |
| `111260002` | 雨菲 | 女 |

### 2.4 发音人列表（VCN）

以下发音人列表同样**由前端维护为静态常量**，共 4 组 22 个：

```javascript
// 分组示例
[
  { "label": "中文普通话 · 交互", "voices": [
    { "id": "x4_xiaoxuan", "name": "小璇", "gender": "女" },
    // ... 14个
  ]},
  { "label": "中文普通话 · 通用/阅读", "voices": [
    { "id": "x4_lingyuyan", "name": "聆玉言", "gender": "女" },
    // ... 5个
  ]},
  { "label": "美式英语", "voices": [
    { "id": "x4_EnUs_Luna_assist", "name": "Luna", "gender": "女" },
    // ... 7个
  ]},
  { "label": "英式英语", "voices": [
    { "id": "x4_EnUk_Amanda_education", "name": "Amanda", "gender": "女" },
    // ... 2个
  ]}
]
```

> 完整列表见附件或前端 `src/App.vue` 第 102-151 行。

### 2.5 接口定义

#### 2.5.1 获取数字人配置

```
GET /api/v1/admin/digital/config?scenic_id=lingshan
```

**响应示例：**
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
  "display_mode": "full",
  "sync_status": "saved",
  "updated_at": "2026-06-13T12:00:00.000Z"
}
```

#### 2.5.2 保存数字人配置

```
POST /api/v1/admin/digital/config
Content-Type: application/json
```

**请求体：**
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
|---|---|
| `speed` | 整数，0 ≤ value ≤ 100 |
| `pitch` | 整数，0 ≤ value ≤ 100 |
| `volume` | 整数，0 ≤ value ≤ 100 |
| `layout` | 枚举: `fullscreen` / `left` / `right` |
| `display_mode` | 枚举: `head` / `upper` / `full` |
| `avatar_id` | 必须在数字人形象列表中 |
| `vcn` | 必须在发音人列表中 |

**响应：**
```json
{
  "code": 200,
  "message": "配置保存成功",
  "data": {
    "saved_at": "2026-06-13T12:00:00.000Z",
    "sync_status": "saved"
  }
}
```

---

## 3. 模块二：内容管理

### 3.1 需求说明

管理员上传景点照片，关联景点名称。图片可预览、删除、复制引用路径。

### 3.2 数据表：`scenic_images`

| 字段名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | INTEGER (PK, AUTO) | ✅ | 图片ID |
| `scenic_id` | VARCHAR(50) | ✅ | 景区ID，默认 `lingshan` |
| `spot_name` | VARCHAR(200) | ✅ | 景点名称，如"灵山大佛" |
| `file_name` | VARCHAR(255) | ✅ | 原始文件名，如 `photo.jpg` |
| `file_path` | VARCHAR(500) | ✅ | 服务器存储路径，前端通过 `/api/v1/admin/uploads/{file_path}` 访问 |
| `file_size` | BIGINT | ✅ | 文件大小（字节） |
| `created_at` | DATETIME | ✅ | 上传时间 |

### 3.3 接口定义

#### 3.3.1 上传景点图片

```
POST /api/v1/admin/content/image/upload
Content-Type: multipart/form-data
```

**请求体：**
| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scenic_id` | string | 否 | 默认 `lingshan` |
| `spot_name` | string | **是** | 景点名称 |
| `file` | file | **是** | 图片文件，接受 `image/*` |

**响应：**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 1,
    "spot_name": "灵山大佛",
    "file_name": "photo_20260613.jpg",
    "file_url": "/api/v1/admin/uploads/photo_20260613.jpg",
    "file_size": 204800,
    "created_at": "2026-06-13T12:00:00.000Z"
  }
}
```

#### 3.3.2 获取图片列表

```
GET /api/v1/admin/content/images?scenic_id=lingshan
```

**响应：**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "spot_name": "灵山大佛",
      "file_name": "photo_20260613.jpg",
      "file_url": "/api/v1/admin/uploads/photo_20260613.jpg",
      "file_size": 204800,
      "created_at": "2026-06-13T12:00:00.000Z"
    }
  ]
}
```

#### 3.3.3 删除图片

```
DELETE /api/v1/admin/content/image/{id}
```

**响应：**
```json
{
  "code": 200,
  "message": "删除成功"
}
```

#### 3.3.4 提供图片静态资源访问

```
GET /api/v1/admin/uploads/{file_path}
```

直接返回图片文件内容，`Content-Type: image/jpeg` 等。

---

## 4. 模块三：知识库管理

### 4.1 需求说明

管理员上传景区知识文档（txt/md/docx），选择分类，后端对文档进行切块处理后存入向量数据库。支持预览切块效果、搜索筛选、按ID查看详情、导出 Markdown。

### 4.2 数据表：`knowledge_documents`

| 字段名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | INTEGER (PK, AUTO) | ✅ | 文档ID |
| `scenic_id` | VARCHAR(50) | ✅ | 景区ID，默认 `lingshan` |
| `name` | VARCHAR(255) | ✅ | 文件名，如 `景区介绍.txt` |
| `doc_type` | VARCHAR(50) | ✅ | 文档类型枚举: `guide`(导游指南) / `history`(历史文化) / `faq`(常见问答) / `notice`(景区公告) |
| `file_size` | BIGINT | ✅ | 文件大小（**字节**，前端格式化展示时将除以1024显示为KB/MB） |
| `file_ext` | VARCHAR(10) | ✅ | 文件扩展名: `txt` / `md` / `docx` |
| `chunk_count` | INTEGER | ✅ | 切块数量 |
| `stored` | BOOLEAN | ✅ | 是否已存入向量数据库，默认 `true` |
| `created_at` | DATETIME | ✅ | 上传时间 |
| `updated_at` | DATETIME | — | 最后更新时间 |

### 4.3 数据表：`knowledge_chunks`

| 字段名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | INTEGER (PK, AUTO) | ✅ | 切块ID |
| `doc_id` | INTEGER (FK) | ✅ | 关联 `knowledge_documents.id` |
| `seq_no` | INTEGER | ✅ | 切块序号，从1开始 |
| `content` | TEXT | ✅ | 切块文本内容 |
| `char_count` | INTEGER | ✅ | 字符数 |
| `embedding_id` | VARCHAR(100) | — | 向量数据库中对应的ID（如有） |

### 4.4 文档类型枚举

| doc_type | 中文标签 | 图标颜色 |
|---|---|---|
| `guide` | 导游指南 | 青色 `#06b6d4` |
| `history` | 历史文化 | 橙色 `#f59e0b` |
| `faq` | 常见问答 | 紫色 `#8b5cf6` |
| `notice` | 景区公告 | 红色 `#ef4444` |

### 4.5 接口定义

#### 4.5.1 获取文档列表

```
GET /api/v1/admin/knowledge/documents?scenic_id=lingshan&search=&doc_type=&file_ext=&page=1&page_size=50
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scenic_id` | string | 否 | 景区ID |
| `search` | string | 否 | 模糊搜索文件名 |
| `doc_type` | string | 否 | 文档类型筛选: `guide` / `history` / `faq` / `notice` |
| `file_ext` | string | 否 | 格式筛选: `txt` / `md` / `docx` |
| `page` | integer | 否 | 页码，默认1 |
| `page_size` | integer | 否 | 每页条数，默认50 |

**响应：**
```json
{
  "code": 200,
  "data": {
    "total": 10,
    "page": 1,
    "page_size": 50,
    "items": [
      {
        "id": 1,
        "name": "景区介绍.txt",
        "doc_type": "guide",
        "file_ext": "txt",
        "file_size": 2355,
        "chunk_count": 4,
        "stored": true,
        "created_at": "2026-06-07T00:00:00.000Z"
      }
    ]
  }
}
```

#### 4.5.2 获取文档详情（含切块）

```
GET /api/v1/admin/knowledge/documents/{id}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "name": "景区介绍.txt",
    "doc_type": "guide",
    "file_ext": "txt",
    "file_size": 2355,
    "chunk_count": 4,
    "stored": true,
    "created_at": "2026-06-07T00:00:00.000Z",
    "chunks": [
      {
        "seq_no": 1,
        "content": "灵山胜境位于无锡马山，是国家5A级旅游景区...",
        "char_count": 56
      },
      {
        "seq_no": 2,
        "content": "灵山大佛通高88米...",
        "char_count": 42
      }
    ]
  }
}
```

#### 4.5.3 上传并预览文档切块

```
POST /api/v1/admin/knowledge/upload/preview
Content-Type: multipart/form-data

已存在：POST /api/v1/guide/knowledge/upload?preview=true
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scenic_id` | string | 是 | `lingshan` |
| `file` | file | **是** | 文档文件，仅接受 `.txt` / `.md` / `.docx` |

**响应：**
```json
{
  "code": 200,
  "data": {
    "file_name": "景区介绍.txt",
    "file_size": 2355,
    "chunk_count": 4,
    "chunks": [
      {
        "seq_no": 1,
        "content": "灵山胜境位于无锡马山...",
        "char_count": 56
      }
    ]
  }
}
```

> ⚠️ **注意**：前端要求返回字段名为 `content` 和 `char_count`（而非 `text`），请保持字段名一致。

#### 4.5.4 确认存储文档

```
POST /api/v1/admin/knowledge/upload/store
Content-Type: multipart/form-data

已存在：POST /api/v1/guide/knowledge/upload?preview=false
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scenic_id` | string | 是 | `lingshan` |
| `scenic_name` | string | 是 | `灵山胜境` |
| `destination_id` | string | 是 | `wuxi` |
| `destination_name` | string | 是 | `无锡` |
| `doc_type` | string | **是** | `guide` / `history` / `faq` / `notice` |
| `file` | file | **是** | 文档文件 |
| `chunks` | string(JSON) | 是 | 预览时确认的chunk列表（JSON序列化） |

**响应：**
```json
{
  "code": 200,
  "message": "存储成功",
  "data": {
    "id": 5,
    "name": "景区介绍.txt",
    "doc_type": "guide",
    "file_size": 2355,
    "chunk_count": 4,
    "created_at": "2026-06-13T12:00:00.000Z"
  }
}
```

> ⚠️ **注意**：前端确认存储后需要后端返回的字段包括 `id`、`name`、`doc_type`、`file_size`（字节）、`chunk_count`、`created_at`，且 `created_at` 需为 ISO 8601 格式。

#### 4.5.5 删除文档

```
DELETE /api/v1/admin/knowledge/documents/{id}
```

**响应：**
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

## 5. 模块四：对话分析

### 5.1 需求说明

管理员查看游客与数字人的对话记录，每条记录包含用户会话ID、对话内容、AI自动分析摘要（含原因分析+改进建议）。前端通过 `session_id` 展示对话。

### 5.2 数据表：`dialogue_sessions`

| 字段名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | INTEGER (PK, AUTO) | ✅ | 主键 |
| `session_id` | VARCHAR(100) | ✅ | 唯一会话ID，如 `sess_abc123` |
| `scenic_id` | VARCHAR(50) | ✅ | 景区ID |
| `user_name` | VARCHAR(100) | — | 游客显示名，为空时前端默认显示 `游客{session_id前8位}` |
| `satisfaction_score` | DECIMAL(5,1) | ✅ | 满意度分数，范围 0-100 |
| `message_count` | INTEGER | ✅ | 消息总条数 |
| `categories` | JSON | — | 对话分类标签数组，如 `["门票咨询", "开放时间"]` |
| `reason` | TEXT | — | AI分析的**原因分析**文本，格式：`【标题1】内容1【标题2】内容2` |
| `suggestion` | TEXT | — | AI分析的**改进建议**文本，格式同上 |
| `last_updated` | DATETIME | ✅ | 最后一条消息时间 |

**关于 reason 和 suggestion 的格式要求（关键）：**
字段内容以 `【】` 包裹标题，后接对应内容文本，多个段落拼接：
```
【服务评价】游客对回答速度满意
【改进方向】建议增加票价详情
```
前端通过正则 `/【([^】]+)】/g` 解析为键值对，请严格遵守此格式。

### 5.3 数据表：`dialogue_messages`

| 字段名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | INTEGER (PK, AUTO) | ✅ | 主键 |
| `session_id` | VARCHAR(100) | ✅ | 关联会话ID |
| `seq_no` | INTEGER | ✅ | 消息序号 |
| `role` | VARCHAR(20) | ✅ | `user`(游客) / `assistant`(数字人) |
| `content` | TEXT | ✅ | 消息文本内容 |
| `sentiment` | VARCHAR(20) | — | 情感: `positive` / `neutral` / `negative` |
| `intent` | VARCHAR(50) | — | 意图类型（如 `knowledge_qa` / `route_plan` 等） |
| `created_at` | DATETIME | ✅ | 消息时间 |

### 5.4 对话分析结果表（可选）

如果后端需要缓存AI分析结果，可使用 `dialogue_analysis` 表：

| 字段名 | 类型 | 说明 |
|---|---|---|
| `session_id` | VARCHAR(100) (PK) | 会话ID |
| `reason` | TEXT | 分析原因（【】格式） |
| `suggestion` | TEXT | 改进建议（【】格式） |
| `analyzed_at` | DATETIME | 分析时间 |

### 5.5 接口定义

#### 5.5.1 获取所有会话列表（含分析摘要）

```
GET /api/v1/admin/dialogue/sessions?scenic_id=lingshan&search=&page=1&page_size=50
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `scenic_id` | string | 否 | 景区ID |
| `search` | string | 否 | 按 session_id 或 user_name 模糊搜索 |
| `page` | integer | 否 | 页码 |
| `page_size` | integer | 否 | 每页条数 |

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

#### 5.5.2 获取指定会话的对话详情

```
GET /api/v1/admin/dialogue/sessions/{session_id}/messages
```

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
        "role": "user",
        "content": "门票多少钱？",
        "created_at": "2026-06-13T10:31:00.000Z"
      },
      {
        "seq_no": 3,
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

## 6. 附录：通用数据类型

### 6.1 错误响应格式

所有接口错误时统一返回：
```json
{
  "code": 400,
  "message": "参数错误：spot_name 为必填项"
}
```

| HTTP 状态码 | code | 说明 |
|---|---|---|
| 200 | 200 | 成功 |
| 400 | 400 | 参数校验失败 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |

### 6.2 前后端字段名映射参考（重要）

以下是前端代码实际使用的字段名，**后端返回 JSON 字段名请严格对齐**：

| 模块 | 前端字段 | 后端对应 | 备注 |
|---|---|---|---|
| 数字人 | `selectedAvatarId` → `avatar_id` | `avatar_id` |  |
| 数字人 | `voiceConfig.vcn` | `vcn` | 发音人ID |
| 数字人 | `sceneConfig.layout` | `layout` | 枚举值 |
| 数字人 | `syncStatus` | `sync_status` | `saved` / `saving` |
| 内容管理 | `spot_name` | `spot_name` | 景点名称 |
| 内容管理 | `file_name` | `file_name` | 文件名 |
| 内容管理 | `file_size` | `file_size` | 字节数 |
| 内容管理 | `created_at` | `created_at` | ISO 8601 |
| 知识库 | `name` | `name` | 文件名 |
| 知识库 | `doc_type` | `doc_type` | 枚举值 |
| 知识库 | `file_ext` | `file_ext` | 扩展名 |
| 知识库 | `chunks[i].content` | `content` | 切块文本，**不要用 `text`** |
| 知识库 | `chunks[i].char_count` | `char_count` | 字符数，**不要用 `length`** |
| 知识库 | `created_at` | `created_at` | ISO 8601 日期 |
| 对话分析 | `session_id` | `session_id` | 会话ID |
| 对话分析 | `satisfaction_score` | `satisfaction_score` | 浮点数0-100 |
| 对话分析 | `last_updated` | `last_updated` | ISO 8601 时间 |
| 对话分析 | `reason` | `reason` | 含【】标签 |
| 对话分析 | `suggestion` | `suggestion` | 含【】标签 |
| 对话分析 | `role` | `role` | `user` / `assistant` |
| 对话分析 | `content` | `content` | 消息文本 |

### 6.3 接口汇总表

| 模块 | 方法 | 路径 | 说明 | 优先级 |
|---|---|---|---|---|
| 数字人 | GET | `/api/v1/admin/digital/config` | 获取数字人配置 | ⭐⭐⭐ |
| 数字人 | POST | `/api/v1/admin/digital/config` | 保存数字人配置 | ⭐⭐⭐ |
| 内容 | POST | `/api/v1/admin/content/image/upload` | 上传景点图片 | ⭐⭐⭐ |
| 内容 | GET | `/api/v1/admin/content/images` | 获取图片列表 | ⭐⭐⭐ |
| 内容 | DELETE | `/api/v1/admin/content/image/{id}` | 删除图片 | ⭐⭐⭐ |
| 内容 | GET | `/api/v1/admin/uploads/{path}` | 图片资源访问 | ⭐⭐⭐ |
| 知识库 | GET | `/api/v1/admin/knowledge/documents` | 文档列表(含搜索筛选) | ⭐⭐⭐ |
| 知识库 | GET | `/api/v1/admin/knowledge/documents/{id}` | 文档详情(含切块) | ⭐⭐⭐ |
| 知识库 | POST | `/api/v1/admin/knowledge/upload/preview` | 预览文档切块 | ⭐⭐⭐ |
| 知识库 | POST | `/api/v1/admin/knowledge/upload/store` | 确认存储文档 | ⭐⭐⭐ |
| 知识库 | DELETE | `/api/v1/admin/knowledge/documents/{id}` | 删除文档 | ⭐⭐ |
| 对话分析 | GET | `/api/v1/admin/dialogue/sessions` | 会话列表(含分析摘要) | ⭐⭐⭐ |
| 对话分析 | GET | `/api/v1/admin/dialogue/sessions/{id}/messages` | 会话消息详情 | ⭐⭐⭐ |
