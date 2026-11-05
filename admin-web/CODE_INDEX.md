# 文旅数字人管理 Web · 代码索引

从 [项目 README](README.md) 了解主流程。下表把当前快照中的源码、页面、脚本与主要配置按目录列出。职责根据代码声明与目录用途概括。

| 文件 | 职责或阅读线索 |
| --- | --- |
| [build.mjs](build.mjs) | 配置构建、应用启动或环境参数 |
| [index.html](index.html) | 应用或子模块入口 |
| [package.json](package.json) | 提供工程配置、界面资源或结构化数据 |
| [public/china.json](public/china.json) | 提供工程配置、界面资源或结构化数据 |
| [server.js](server.js) | 应用或子模块入口。可从 `apiProxy` 、 `exactProxy` 开始 |
| [src/App.vue](src/App.vue) | 应用或子模块入口。可从 `fetchAPI` 、 `fetchAllDashboardData` 、 `handleLogout` 、 `_closeUserMenu` 开始 |
| [src/assets/global.css](src/assets/global.css) | 定义界面样式与布局 |
| [src/main.js](src/main.js) | Vue 入口 |
| [src/router/index.js](src/router/index.js) | 管理页面导航 |
| [src/utils/api.js](src/utils/api.js) | 请求封装。可从 `getToken` 、 `getAuthHeaders` 、 `apiFetch` 开始 |
| [src/views/content/ContentView.vue](src/views/content/ContentView.vue) | 实现页面、组件或用户交互 |
| [src/views/dashboard/DashboardView.vue](src/views/dashboard/DashboardView.vue) | 实现页面、组件或用户交互 |
| [src/views/data/DataView.vue](src/views/data/DataView.vue) | 实现页面、组件或用户交互 |
| [src/views/dialogue/DialogueView.vue](src/views/dialogue/DialogueView.vue) | 实现页面、组件或用户交互。可从 `emotionClass` 、 `parseSuggestions` 开始 |
| [src/views/digital/DigitalView.vue](src/views/digital/DigitalView.vue) | 数字人设置。可从 `selectAvatar` 、 `loadVcnVoices` 、 `loadConfig` 、 `saveConfig` 开始 |
| [src/views/kg3d/KG3DView.vue](src/views/kg3d/KG3DView.vue) | 实现页面、组件或用户交互。可从 `switchTab` 、 `updateTabIndicator` 、 `nodeColor` 、 `getNeighbor` 开始 |
| [src/views/knowledge/KnowledgeView.vue](src/views/knowledge/KnowledgeView.vue) | 实现页面、组件或用户交互。可从 `getExt` 、 `renderChunkMd` 开始 |
| [src/views/login/LoginView.vue](src/views/login/LoginView.vue) | 实现页面、组件或用户交互。可从 `showToast` 、 `switchMode` 、 `validate` 、 `handleSubmit` 开始 |
| [src/views/report/ReportView.vue](src/views/report/ReportView.vue) | 报表页面。可从 `satClass` 、 `fetchReport` 、 `exportCSV` 、 `getMockReport` 开始 |
| [src/views/settings/SettingsView.vue](src/views/settings/SettingsView.vue) | 实现页面、组件或用户交互。可从 `openIcp` 开始 |
| [vite.config.js](vite.config.js) | 配置构建、应用启动或环境参数 |

本页列出 21 个可阅读的项目文件，以及 0 个第三方或生成文件。
