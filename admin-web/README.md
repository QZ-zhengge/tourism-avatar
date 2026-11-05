# 文旅数字人管理 Web

用 Vue 管理景区内容、知识库、数字人配置和对话报表。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [src/main.js](src/main.js) | Vue 入口 |
| [src/router/index.js](src/router/index.js) | 管理页面导航 |
| [src/utils/api.js](src/utils/api.js) | 请求封装 |
| [src/views/digital/DigitalView.vue](src/views/digital/DigitalView.vue) | 数字人设置 |
| [src/views/report/ReportView.vue](src/views/report/ReportView.vue) | 报表页面 |

## 一条业务链路

路由装配后台视图，视图经 API 模块调用管理服务，渲染内容、对话和报表。可从数字人配置页跟一次读取和保存。

## 运行与验证

在目录运行 npm ci，再按 package.json 执行开发或构建脚本。按 Vite 与 API 配置连到测试管理服务。未重新构建发布。

## 面试可能会问

**为什么统一封装 API？**

统一基址、鉴权和错误处理。页面聚焦表单、列表及状态反馈。

**如何展示没有真实用户数据的报表？**

用合成的请求响应重现正常、空数据和错误状态，说明指标字段与后端接口的对应关系。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
