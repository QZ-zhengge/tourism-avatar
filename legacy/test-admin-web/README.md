# 文旅管理 Web 测试快照

保存管理前端在联调目录中的 Vue 页面和 API 层。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [src/main.js](src/main.js) | Vue 初始化 |
| [src/router/index.js](src/router/index.js) | 页面入口 |
| [src/utils/api.js](src/utils/api.js) | 接口封装 |
| [src/views/digital/DigitalView.vue](src/views/digital/DigitalView.vue) | 数字人配置页面 |

## 运行与验证

使用 npm ci 后按 package.json 启动，并指向独立测试 API。作为历史参考保存。
