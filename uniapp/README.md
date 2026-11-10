# 随行 UniApp 客户端

跨端实现景区首页、地图、行程、动态和数字人助手。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [App.vue](App.vue) | 应用生命周期 |
| [pages.json](pages.json) | 页面注册 |
| [utils/api.js](utils/api.js) | 接口封装 |
| [pages/map](pages/map) | 地图页面 |
| [components/floating-assistant](components/floating-assistant) | 悬浮助手 |
| [utils/xunfei-ws.js](utils/xunfei-ws.js) | 数字人连接工具 |

## 运行与验证

用 HBuilderX 导入并选择目标平台，检查 manifest.json 与 config/index.js 的平台配置。用自有地图、后端和语音服务做真机验证。

## 继续阅读

- [原项目说明与操作细节](README-REFERENCE.md)
