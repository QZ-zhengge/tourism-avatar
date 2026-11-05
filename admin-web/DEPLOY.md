# 景区数字人管理平台 — 部署文档

## 项目概述

**项目名**: `admin-panel`（景区数字人管理平台）
**技术栈**: Vue 3.4 + Vite 5.4 + Vue Router 4.6 + ECharts 5.5  
**生产服务**: Express 5 + http-proxy-middleware  
**运行端口**: 80（开发/生产统一）

---

## 目录结构

```
vue-app/
├── src/                          # 源代码
│   ├── main.js                   # 应用入口
│   ├── App.vue                   # 根组件（布局、侧边栏、顶部导航、共享状态）
│   ├── assets/global.css         # 全局样式（暗色主题）
│   ├── router/index.js           # 路由配置 + 登录守卫
│   ├── utils/api.js              # API 请求封装（自动注入 Bearer Token）
│   └── views/
│       ├── login/LoginView.vue   # 登录/注册
│       ├── dashboard/            # 数据大屏（KPI + 图表 + 地图）
│       ├── digital/              # 数字人管理（形象 + 语音配置）
│       ├── data/                 # 数据分析（改进建议分类）
│       ├── content/              # 内容管理（景点照片）
│       ├── knowledge/            # 知识库（文档上传/预览/切块）
│       ├── kg3d/                 # 知识图谱（2D图谱 + 编辑 + 问答）
│       ├── dialogue/             # 对话分析（用户列表 + 情感分析）
│       ├── report/               # 分析报告（游客满意度+CSV导出）
│       └── settings/             # 系统设置
├── public/                       # 静态资源（地图GeoJSON、数字人头像、3D库）
├── dist/                         # 构建输出（生产部署用）
├── server.js                     # 生产环境 Express 服务器
├── vite.config.js                # 开发环境 Vite 配置
└── package.json
```

## 前置条件

- **Node.js** ≥ 18（推荐 22）
- **npm** ≥ 9
- 后端服务（可选，离线时自动使用 Mock 数据）：
  - **50085 端口**: 认证、数字人、内容、知识库、对话、建议 API
  - **50081 端口**: 仪表盘、知识图谱等模型服务

---

## 一、开发环境（Vite Dev Server）

```bash
cd vue-app
npm install
npm run dev
```

| 配置项 | 值 |
|--------|-----|
| 地址 | `http://localhost:80` |
| 端口 | 80 |
| 主机绑定 | `0.0.0.0`（局域网络可访问） |
| API 代理 | 后端 50085 / 模型服务 50081 |

开发模式下 Vite 提供热更新（HMR），修改代码后浏览器自动刷新。

### API 代理规则

| 路径 | 目标 | 说明 |
|------|------|------|
| `/api/v1/auth/` | → `47.236.85.42:50085` | 登录注册 |
| `/api/v1/admin/suggestions` | → `47.236.85.42:50085` | 用户建议信箱 |
| `/api/v1/admin/digital/` | → `47.236.85.42:50085` | 数字人管理 |
| `/api/v1/admin/vcn/` | → `47.236.85.42:50085` | 发音人管理 |
| `/api/v1/admin/content/` | → `47.236.85.42:50085` | 内容管理 |
| `/api/v1/admin/knowledge/` | → `47.236.85.42:50085` | 知识库 |
| `/api/v1/admin/dialogue/` | → `47.236.85.42:50085` | 对话分析 |
| `/api`（兜底） | → `47.236.85.42:50081` | 仪表盘/知识图谱等 |

---

## 二、生产部署

### 2.1 构建

```bash
cd vue-app
npm install
npm run build
```

构建产物输出到 `dist/` 目录。

### 2.2 启动生产服务器

```bash
npm start
# 或
node server.js
```

| 配置项 | 值 |
|--------|-----|
| 地址 | `http://localhost:80` |
| 端口 | 80（可通过 `PORT` 环境变量修改） |
| 静态文件 | `dist/` + `public/` |
| SPA 回退 | 所有未匹配路径 → `dist/index.html` |

### 2.3 部署架构

```
用户浏览器
     │
     ▼
Express 服务器 (端口 80) ─── 静态文件: dist/ + public/
     │
     ├── /api/v1/auth/*          ──→ 50085 后端
     ├── /api/v1/admin/digital/*  ──→ 50085 后端
     ├── /api/v1/admin/vcn/*      ──→ 50085 后端
     ├── /api/v1/admin/content/*  ──→ 50085 后端
     ├── /api/v1/admin/knowledge/* ──→ 50085 后端
     ├── /api/v1/admin/dialogue/*  ──→ 50085 后端
     ├── /api/v1/admin/suggestions ──→ 50085 后端
     └── /api/* (兜底)            ──→ 50081 模型服务
```

### 2.4 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `PORT` | `80` | 服务端口号 |

---

## 三、路由清单

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | 登录/注册 | 无需认证 |
| `/dashboard` | 数据大屏 | KPI 指标、图表、地图 |
| `/digital` | 数字人管理 | 形象选择、语音配置 |
| `/data` | 数据分析 | 改进建议四象限 |
| `/content` | 内容管理 | 景点照片管理 |
| `/knowledge` | 知识库 | 文档上传/预览/切块 |
| `/kg3d` | 知识图谱 | 2D图谱、编辑、问答 |
| `/dialogue` | 对话分析 | 用户列表、情感分析 |
| `/report` | 分析报告 | 游客满意度+CSV导出 |
| `/settings` | 系统设置 | 服务配置、通知 |

所有页面（除 `/login` 外）需携带 `user_token` 登录凭证，未登录自动跳转 `/login`。

---

## 四、离线模式

后端服务不可用时，以下页面自动使用 Mock 数据：

| 页面 | Mock 数据 |
|------|-----------|
| 数据大屏 | 所有 10 个 API 均有兜底 Mock |
| 分析报告 | 8 名游客、7天/30天趋势、4 条分类建议 |
| 用户信箱 | 12 条伪造建议（6 条未读、6 条已读） |
| 对话分析 | 内置测试数据 |
| 数字人管理 | 12 个讯飞数字人 + 24 个音色（完全本地） |

所有页面不会因为后端不可用而白屏或崩溃。

---

## 五、Nginx 反向代理（可选）

如果不使用 Express 服务器，可直接用 Nginx 托管构建产物：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 静态文件
    root /path/to/vue-app/dist;
    index index.html;

    # SPA 回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理 — 50085 后端
    location /api/v1/auth/ {
        proxy_pass http://192.0.2.1:50085;
    }
    location /api/v1/admin/ {
        proxy_pass http://192.0.2.1:50085;
    }

    # API 代理 — 50081 模型服务（兜底）
    location /api/ {
        proxy_pass http://192.0.2.1:50081;
    }
}
```

---

## 六、常见问题

### Q: 部署后页面白屏/空白
检查 `dist/` 目录是否存在且非空。运行 `npm run build` 重新构建。

### Q: API 请求一直超时
后端服务 `47.236.85.42:50085/50081` 可能未启动。前端已内置 Mock 数据回退，不影响页面展示。

### Q: 登录后跳转到登录页循环
清除浏览器 `localStorage` 中的 `user_token`，重新登录。

### Q: 如何修改端口
```bash
PORT=3000 node server.js     # 生产环境运行在 3000 端口
```

### Q: `favicon.ico` 404 报错
在 `public/` 目录放置 `favicon.ico` 文件，或忽略（不影响功能）。

---

## 七、维护命令

```bash
npm install        # 安装依赖
npm run dev        # 开发模式（热更新）
npm run build      # 生产构建
npm start          # 生产启动
npm run preview    # Vite 预览构建产物
```
