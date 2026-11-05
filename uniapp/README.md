# 随行 UniApp 客户端

跨端实现景区首页、地图、行程、动态和数字人助手。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [App.vue](App.vue) | 应用生命周期 |
| [pages.json](pages.json) | 页面注册 |
| [utils/api.js](utils/api.js) | 接口封装 |
| [pages/map](pages/map) | 地图页面 |
| [components/floating-assistant](components/floating-assistant) | 悬浮助手 |
| [utils/xunfei-ws.js](utils/xunfei-ws.js) | 数字人连接工具 |

## 一条业务链路

页面通过统一 API 加载内容，地图模块消费 POI 与定位信息。悬浮助手组件和连接工具配合处理数字人交互。

## 运行与验证

用 HBuilderX 导入并选择目标平台，检查 manifest.json 与 config/index.js 的平台配置。用自有地图、后端和语音服务做真机验证。

## 面试可能会问

**跨端开发节省了什么？**

页面、路由和接口层可以复用。权限、地图、WebSocket 和播放行为仍要按平台验证。

**为什么还保留原生客户端？**

两条实现记录了跨端复用和原生能力接入的不同侧重点。面试可用同一功能对照。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
- [原项目说明与操作细节](README-REFERENCE.md)
