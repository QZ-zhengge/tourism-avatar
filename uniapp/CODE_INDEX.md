# 随行 UniApp 客户端 · 代码索引

从 [项目 README](README.md) 了解主流程。下表把当前快照中的源码、页面、脚本与主要配置按目录列出。职责根据代码声明与目录用途概括。

| 文件 | 职责或阅读线索 |
| --- | --- |
| [App.vue](App.vue) | 应用生命周期 |
| [common/uni.css](common/uni.css) | 定义界面样式与布局 |
| [components/bottom-nav/index.css](components/bottom-nav/index.css) | 应用或子模块入口 |
| [components/bottom-nav/index.js](components/bottom-nav/index.js) | 应用或子模块入口 |
| [components/bottom-nav/index.vue](components/bottom-nav/index.vue) | 应用或子模块入口 |
| [components/floating-assistant/index.css](components/floating-assistant/index.css) | 应用或子模块入口 |
| [components/floating-assistant/index.js](components/floating-assistant/index.js) | 应用或子模块入口。可从 `log` 、 `logError` 、 `arrayBufferToBase64` 、 `base64ToArrayBuffer` 开始 |
| [components/floating-assistant/index.vue](components/floating-assistant/index.vue) | 应用或子模块入口 |
| [components/global-dh/index.css](components/global-dh/index.css) | 应用或子模块入口 |
| [components/global-dh/index.vue](components/global-dh/index.vue) | 应用或子模块入口。可从 `send` 、 `arrayBufferToBase64` 开始 |
| [config/index.js](config/index.js) | 应用或子模块入口 |
| [index.html](index.html) | 应用或子模块入口 |
| [main.js](main.js) | 应用或子模块入口 |
| [manifest.json](manifest.json) | 提供工程配置、界面资源或结构化数据 |
| [package.json](package.json) | 提供工程配置、界面资源或结构化数据 |
| [pages.json](pages.json) | 页面注册 |
| [pages/aging/index.css](pages/aging/index.css) | 应用或子模块入口 |
| [pages/aging/index.js](pages/aging/index.js) | 应用或子模块入口 |
| [pages/aging/index.vue](pages/aging/index.vue) | 应用或子模块入口 |
| [pages/ai-chat/index.css](pages/ai-chat/index.css) | 应用或子模块入口 |
| [pages/ai-chat/index.js](pages/ai-chat/index.js) | 应用或子模块入口 |
| [pages/ai-chat/index.vue](pages/ai-chat/index.vue) | 应用或子模块入口 |
| [pages/checkin/index.css](pages/checkin/index.css) | 应用或子模块入口 |
| [pages/checkin/index.js](pages/checkin/index.js) | 应用或子模块入口。可从 `getColor` 、 `getLetter` 、 `saveCheckin` 、 `getCheckins` 开始 |
| [pages/checkin/index.vue](pages/checkin/index.vue) | 应用或子模块入口 |
| [pages/digital-human/index.css](pages/digital-human/index.css) | 应用或子模块入口 |
| [pages/digital-human/index.js](pages/digital-human/index.js) | 应用或子模块入口。可从 `log` 、 `logError` 、 `logWarn` 、 `requestBackend` 开始 |
| [pages/digital-human/index.vue](pages/digital-human/index.vue) | 应用或子模块入口 |
| [pages/digital-stream/index.css](pages/digital-stream/index.css) | 应用或子模块入口 |
| [pages/digital-stream/index.js](pages/digital-stream/index.js) | 应用或子模块入口 |
| [pages/digital-stream/index.vue](pages/digital-stream/index.vue) | 应用或子模块入口 |
| [pages/events/index.css](pages/events/index.css) | 应用或子模块入口 |
| [pages/events/index.js](pages/events/index.js) | 应用或子模块入口 |
| [pages/events/index.vue](pages/events/index.vue) | 应用或子模块入口 |
| [pages/food/index.css](pages/food/index.css) | 应用或子模块入口 |
| [pages/food/index.js](pages/food/index.js) | 应用或子模块入口 |
| [pages/food/index.vue](pages/food/index.vue) | 应用或子模块入口 |
| [pages/guide/index.css](pages/guide/index.css) | 应用或子模块入口 |
| [pages/guide/index.js](pages/guide/index.js) | 应用或子模块入口 |
| [pages/guide/index.vue](pages/guide/index.vue) | 应用或子模块入口 |
| [pages/home/index.css](pages/home/index.css) | 应用或子模块入口 |
| [pages/home/index.js](pages/home/index.js) | 应用或子模块入口 |
| [pages/home/index.vue](pages/home/index.vue) | 应用或子模块入口 |
| [pages/hotspots/index.css](pages/hotspots/index.css) | 应用或子模块入口 |
| [pages/hotspots/index.js](pages/hotspots/index.js) | 应用或子模块入口。可从 `getColor` 、 `getLetter` 、 `trimTag` 、 `parseSections` 开始 |
| [pages/hotspots/index.vue](pages/hotspots/index.vue) | 应用或子模块入口 |
| [pages/index/index.css](pages/index/index.css) | 应用或子模块入口 |
| [pages/index/index.js](pages/index/index.js) | 应用或子模块入口 |
| [pages/index/index.vue](pages/index/index.vue) | 应用或子模块入口 |
| [pages/itinerary/index.css](pages/itinerary/index.css) | 应用或子模块入口 |
| [pages/itinerary/index.js](pages/itinerary/index.js) | 应用或子模块入口 |
| [pages/itinerary/index.vue](pages/itinerary/index.vue) | 应用或子模块入口 |
| [pages/live-guide/index.css](pages/live-guide/index.css) | 应用或子模块入口 |
| [pages/live-guide/index.js](pages/live-guide/index.js) | 应用或子模块入口 |
| [pages/live-guide/index.vue](pages/live-guide/index.vue) | 应用或子模块入口 |
| [pages/login/index.css](pages/login/index.css) | 应用或子模块入口 |
| [pages/login/index.js](pages/login/index.js) | 应用或子模块入口 |
| [pages/login/index.vue](pages/login/index.vue) | 应用或子模块入口 |
| [pages/map/index.css](pages/map/index.css) | 应用或子模块入口 |
| [pages/map/index.js](pages/map/index.js) | 应用或子模块入口。可从 `getMarkerIcon` 、 `getDistance` 、 `formatDistance` 、 `formatDuration` 开始 |
| [pages/map/index.vue](pages/map/index.vue) | 应用或子模块入口 |
| [pages/mine/index.css](pages/mine/index.css) | 应用或子模块入口 |
| [pages/mine/index.js](pages/mine/index.js) | 应用或子模块入口 |
| [pages/mine/index.vue](pages/mine/index.vue) | 应用或子模块入口 |
| [pages/moments/index.css](pages/moments/index.css) | 应用或子模块入口 |
| [pages/moments/index.js](pages/moments/index.js) | 应用或子模块入口。可从 `randomImageStyle` 、 `randomScenicName` 、 `getCommentAvatarColor` 、 `getCommentAvatarText` 开始 |
| [pages/moments/index.vue](pages/moments/index.vue) | 应用或子模块入口 |
| [pages/nearby/index.css](pages/nearby/index.css) | 应用或子模块入口 |
| [pages/nearby/index.js](pages/nearby/index.js) | 应用或子模块入口 |
| [pages/nearby/index.vue](pages/nearby/index.vue) | 应用或子模块入口 |
| [pages/register/index.css](pages/register/index.css) | 应用或子模块入口 |
| [pages/register/index.js](pages/register/index.js) | 应用或子模块入口 |
| [pages/register/index.vue](pages/register/index.vue) | 应用或子模块入口 |
| [pages/scenic/index.css](pages/scenic/index.css) | 应用或子模块入口 |
| [pages/scenic/index.js](pages/scenic/index.js) | 应用或子模块入口 |
| [pages/scenic/index.vue](pages/scenic/index.vue) | 应用或子模块入口。可从 `loadG6` 、 `getNodeColor` 开始 |
| [pages/search/index.css](pages/search/index.css) | 应用或子模块入口 |
| [pages/search/index.js](pages/search/index.js) | 应用或子模块入口 |
| [pages/search/index.vue](pages/search/index.vue) | 应用或子模块入口 |
| [uni.scss](uni.scss) | 定义界面样式与布局 |
| [utils/api.js](utils/api.js) | 接口封装。可从 `request` 开始 |
| [utils/location.js](utils/location.js) | 该目录下的业务实现或工具代码。可从 `getCurrentDistrict` 开始 |
| [utils/xunfei-ws.js](utils/xunfei-ws.js) | 数字人连接工具。可从 `getFractionalBits` 、 `convertEndian` 、 `rightRotate` 、 `sha256` 开始 |

本页列出 83 个可阅读的项目文件，以及 0 个第三方或生成文件。
