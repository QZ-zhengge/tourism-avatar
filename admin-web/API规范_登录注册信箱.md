# 景区数字人管理平台 — 后端 API 对接规范

> **给后端开发的话**：下面是前端目前调用的接口和期望的数据格式。全部走 `/api` 代理到 `http://192.0.2.1:50081`。

---

## 一、登录/注册（目前纯前端 localStorage，需要后端实现）

### 现状
登录和注册 100% 在浏览器 localStorage 里模拟，路由守卫只检查 `localStorage.getItem('user_token')` 是否存在。

### 前端需要后端提供的接口

#### 1.1 登录

```
POST /api/v1/auth/login
Content-Type: application/json
```

**请求体：**
```json
{
  "username": "admin",
  "password": "CHANGE_ME",
  "remember": true
}
```

**期望响应（200 成功）：**
```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "CHANGE_ME",
    "user": {
      "id": 1,
      "username": "admin",
      "email": "user@example.com",
      "role": "admin",
      "avatar": null
    }
  }
}
```

**期望响应（401 失败）：**
```json
{
  "code": 401,
  "message": "用户名或密码错误"
}
```

**说明：**
- `username` 字段（之前用过邮箱登录，现已改为用户名登录）
- 前端收到 token 后写入 `localStorage.setItem('user_token', token)`
- 还会写入 `user_name` 和 `user_email` 用于顶部栏显示

---

#### 1.2 注册

```
POST /api/v1/auth/register
Content-Type: application/json
```

**请求体：**
```json
{
  "username": "新用户",
  "email": "user@example.com",
  "password": "CHANGE_ME"
}
```

**期望响应（200 成功）：**
```json
{
  "code": 0,
  "message": "注册成功"
}
```

**期望响应（409 冲突）：**
```json
{
  "code": 409,
  "message": "该邮箱已注册"
}
```

**前端校验规则（后端也应遵循）：**
| 字段 | 规则 |
|------|------|
| username | 必填，不能为空 |
| email | 必填，正则 `/^[^\s@]+@[^\s@]+\.[^\s@]+$/` |
| password | 必填，最少 6 位 |
| confirmPassword | 与 password 一致（前端校验，后端不需要此字段） |

---

#### 1.3 获取当前用户信息（可选但推荐）

```
GET /api/v1/auth/me
Authorization: Bearer {token}
```

**期望响应：**
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "username": "admin",
    "email": "user@example.com",
    "role": "admin"
  }
}
```

**说明：** 前端目前从 localStorage 读取 `user_name` 和 `user_email`，如果后端提供此接口，可以改为每次刷新时校验 token 有效性。

---

#### 1.4 退出登录（可选）

```
POST /api/v1/auth/logout
Authorization: Bearer {token}
```

**期望响应：**
```json
{
  "code": 0,
  "message": "已退出"
}
```

---

### 前端已完成的改动（2026-06-14）

**LoginView.vue** — `handleSubmit()`：
1. ✅ 登录：优先调 `POST /api/v1/auth/login`，API 不可用时 fallback 到硬编码 admin + localStorage
2. ✅ 注册：调 `POST /api/v1/auth/register`，不再写 localStorage
3. ✅ 登录成功后将 `data.token`、`data.user.username`、`data.user.email` 写入 localStorage

**App.vue** — 全局 API 调用：
4. ✅ 新增 `src/utils/api.js` 共享模块，`getAuthHeaders()` 自动注入 `Authorization: Bearer {token}`
5. ✅ `fetchAPI()` 仪表盘数据请求已加 auth 头
6. ✅ `loadSuggestions()` / `markAsRead()` 信箱接口已加 auth 头
7. ✅ 知识图谱、对话分析、改进建议、知识库上传等全部 fetch 已加 auth 头

**router/index.js** 路由守卫：
1. 维持 `localStorage.getItem('user_token')` 检查，token 值改为后端返回的 JWT

---

## 二、信箱（用户建议）— 已有 API，需确认格式

### 前端调用现状

两个 API 已在工作，前端正在调用但后端可能还没实现（目前降级为空数组）。

#### 2.1 获取建议列表

```
GET /api/v1/admin/suggestions
```

**前端期望的响应体：**
```json
{
  "items": [
    {
      "id": "sg_001",
      "user_name": "张三",
      "content": "建议增加夜间游览路线",
      "status": "unread",
      "created_at": "2026-06-14T10:30:00"
    },
    {
      "id": "sg_002",
      "user_name": "李四",
      "content": "梵宫的导览不够详细",
      "status": "read",
      "created_at": "2026-06-13T15:22:00"
    }
  ]
}
```

**字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | string | 唯一标识 |
| `user_name` | string | 提交建议的用户名，没有则为"匿名用户" |
| `content` | string | 建议内容文本 |
| `status` | `"unread"` 或 `"read"` | 是否已读 |
| `created_at` | ISO 8601 字符串 | 建议提交时间 |

**前端容错：** 也兼容 `data.suggestions` 数组格式。

**前端显示逻辑：**
- `status === 'unread'` → 左侧显示蓝色圆点 + 未读样式
- 点击建议条目 → 自动调用标记已读接口
- 时间调用 `mailboxFormatTime()` 格式化（显示"今天 10:30"、"昨天 15:22"等）

---

#### 2.2 标记已读

```
PUT /api/v1/admin/suggestions/{id}/read
```

**示例：** `PUT /api/v1/admin/suggestions/sg_001/read`

**无需请求体。**

**期望响应：**
```json
{
  "code": 0,
  "message": "ok"
}
```

---

#### 2.3 右上角未读角标

打开信箱面板时调用 `loadSuggestions()`，计算 `suggestions.filter(s => s.status === 'unread').length` 作为未读数，显示在右侧顶部信箱图标右上角。

---

## 三、全局约定

### 3.1 通用响应格式（建议）
```json
{
  "code": 0,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 0 | 成功 |
| 401 | 未登录 / token 过期 |
| 403 | 无权限 |
| 409 | 资源冲突（如邮箱已注册） |
| 500 | 服务器错误 |

### 3.2 认证方式
前端在所有 API 请求中携带 `Authorization: Bearer {token}` 请求头（通过 `src/utils/api.js` 的 `getAuthHeaders()` 统一注入）。

**实现文件：** `src/utils/api.js`
```javascript
export function getAuthHeaders(extraHeaders = {}) {
  const token = localStorage.getItem('user_token') || ''
  const headers = { 'Content-Type': 'application/json', ...extraHeaders }
  if (token) headers['Authorization'] = `Bearer ${token}`
  return headers
}
```

> 注意：文件上传（FormData）请求仅携带 `Authorization` 头，不设 `Content-Type`（由浏览器自动处理 multipart boundary）。

### 3.3 代理
前端 Vite 开发服务器将 `/api` 代理到 `http://192.0.2.1:50081`，后端只需监听 50081 端口，路由前缀为 `/api/v1/`。

---

## 四、实施优先级建议

| 优先级 | 接口 | 理由 |
|--------|------|------|
| 🔴 P0 | `POST /api/v1/auth/login` | 核心功能，目前纯前端模拟不安全 |
| 🔴 P0 | `POST /api/v1/auth/register` | 核心功能，数据应持久化到数据库 |
| 🟡 P1 | `GET /api/v1/admin/suggestions` | 已有调用，数据源准备好即可工作 |
| 🟡 P1 | `PUT /api/v1/admin/suggestions/{id}/read` | 同上 |
| 🟢 P2 | `GET /api/v1/auth/me` | 提升用户体验（刷新不丢登录态） |
| 🟢 P2 | `POST /api/v1/auth/logout` | 可选，目前前端直接清 localStorage |
