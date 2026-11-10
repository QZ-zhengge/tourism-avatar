# 文旅数字人管理 Web

用 Vue 管理景区内容、知识库、数字人配置和对话报表。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [src/main.js](src/main.js) | Vue 入口 |
| [src/router/index.js](src/router/index.js) | 管理页面导航 |
| [src/utils/api.js](src/utils/api.js) | 请求封装 |
| [src/views/digital/DigitalView.vue](src/views/digital/DigitalView.vue) | 数字人设置 |
| [src/views/report/ReportView.vue](src/views/report/ReportView.vue) | 报表页面 |

## 运行与验证

在目录运行 npm ci，再按 package.json 执行开发或构建脚本。按 Vite 与 API 配置连到测试管理服务。未重新构建发布。
