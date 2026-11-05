# 文旅管理 Web 测试快照

保存管理前端在联调目录中的 Vue 页面和 API 层。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [src/main.js](src/main.js) | Vue 初始化 |
| [src/router/index.js](src/router/index.js) | 页面入口 |
| [src/utils/api.js](src/utils/api.js) | 接口封装 |
| [src/views/digital/DigitalView.vue](src/views/digital/DigitalView.vue) | 数字人配置页面 |

## 一条业务链路

将页面和 API 调用按业务对照当前管理 Web，区分 UI 差异和后端契约差异。

## 运行与验证

使用 npm ci 后按 package.json 启动，并指向独立测试 API。作为历史参考保存。

## 面试可能会问

**如何处理前端与后台发布不同步？**

先定义可兼容的字段变更和切换方式，再用契约测试覆盖新旧双方。

**怎么从前端找出接口差异？**

从请求封装追踪调用点，逐页比对 URL、参数与错误处理。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
