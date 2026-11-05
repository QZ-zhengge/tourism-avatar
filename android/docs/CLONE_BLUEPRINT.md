# 「随行」 uni-app → Android 原生复刻蓝图

> **目标**：把 `Z:\uniapp_to_and\stuio_uniapp\stuio_uniapp\` 整个 uni-app 项目
> （应用名「随行 - 智慧旅游导览」，21 页面 + 3 组件）按"接口完全一致、效果一致、bug 少、功能全"的原则，
> 用 Kotlin + Jetpack Compose 重新实现到 `Z:\uniapp_to_and\let_us_go\`（Android Studio 工程）。
>
> **审计日期**：2026-06-21
> **后端 baseURL**：
> - 主后端 `http://192.0.2.1:50080`
> - 数字人导览 `http://192.0.2.1:50081`
> - 高德逆地理 `https://restapi.amap.com/v3/geocode/regeo`
> - 讯飞数字人 `wss://avatar.cn-huadong-1.xf-yun.com/v1/interact_lips_flow`
> - 后端 ASR `ws://{API_BASE}/asr/stream`
>
> **高德 Android Key**：`60c89976b2b93920896bf428bb25a88c`（manifest 注入）

---

## 一、复刻范围（21 页 + 3 组件 + 3 utils）

### 1.1 Tab 页（5 项，对应 `pages.json → tabBar`）

| # | 路径 | 核心 | 备注 |
|---|------|------|------|
| 1 | `pages/home/index.vue` | 轮播 + 8 宫格 + 快捷入口 + 旅友圈精选 + 景区切换 + 天气 | |
| 2 | `pages/map/index.vue` | 高德地图 + 12 个 POI + 搜索 + 路线规划 + 实时 GPS + TTS 播报 | **最复杂之一** |
| 3 | `pages/digital-human/index.vue` | RTMP 数字人 + 录音 + ASR WS + LLM + 预订卡片 + 行程同步 | **最复杂** |
| 4 | `pages/moments/index.vue` | 信息流 + 发帖弹窗 + 上传 9 图 + 草稿箱 + 评论 + 点赞 | |
| 5 | `pages/mine/index.vue` | 34 省份 SVG + 年度报告 + 9 宫格 + 打卡 | **次复杂** |

### 1.2 登录/老化/启动（4 项）

| # | 路径 | 核心 |
|---|------|------|
| 6 | `pages/login/index.vue` | 登录 + 老年版开关 + 三方登录占位 |
| 7 | `pages/register/index.vue` | 注册 + 表单校验 |
| 8 | `pages/aging/index.vue` | 适老化模式欢迎页 |
| 9 | `pages/index/index.vue` | 默认占位首页（带登出） |

### 1.3 工具页（12 项）

| # | 路径 | 核心 |
|---|------|------|
| 10 | `pages/ai-chat/index.vue` | 聊天 LLM + 历史 + 酒店卡片 + 行程解析 |
| 11 | `pages/digital-stream/index.vue` | 视频流播放器（HLS/FLV）+ 录音 ASR + TTS |
| 12 | `pages/scenic/index.vue` | 知识图谱（G6 in WebView）+ 实体查询 |
| 13 | `pages/events/index.vue` | 活动列表 + 分类 + 滚动加载 |
| 14 | `pages/food/index.vue` | 美食列表 |
| 15 | `pages/checkin/index.vue` | 拍照打卡 + 成就 + 100 条本地 |
| 16 | `pages/guide/index.vue` | 攻略 + 出行清单 + 外部链接 |
| 17 | `pages/nearby/index.vue` | 周边好店 |
| 18 | `pages/hotspots/index.vue` | 热门景点 + 小红书风格详情 + 分享 |
| 19 | `pages/search/index.vue` | 全局搜索 |
| 20 | `pages/itinerary/index.vue` | 我的行程 + 按日分组 + 长按菜单 |
| 21 | `pages/live-guide/index.vue` | 实时讲解：拍照 → 智能裁剪 → 识别 → TTS |

### 1.4 全局组件（3 项）

| # | 组件 | 核心 | 备注 |
|---|------|------|------|
| B1 | `floating-assistant` | 圆形悬浮 + 录音 + 说话波形 + 对话气泡 | uni 端仅 UI，需**完整重写** |
| B2 | `global-dh` | 后台轮询录音 → ASR → 唤醒词 → 跳转数字人 | 需 ForegroundService |
| B3 | `bottom-nav` | 自定义 TabBar | 几乎未被使用，可丢 |

### 1.5 工具/配置

- `utils/api.js` → 14 个 API（用户、文件、帖子、点赞、评论）
- `utils/location.js` → 高德逆地理
- `utils/xunfei-ws.js` → 讯飞数字人 WS（HMAC-SHA256 + Base64 + 6 协议方法）
- `config/index.js` → 全部 key/secret/baseURL

---

## 二、技术选型（Android 端）

| 层 | 选型 | 替代方案 |
|----|------|---------|
| UI | **Jetpack Compose + Material3** | （项目已用 Compose） |
| 路由 | **Navigation Compose** + 自定义 BottomBar | – |
| 网络 | **Retrofit 2 + OkHttp 4 + Moshi** | – |
| WS | **OkHttp WebSocket** | – |
| 鉴权 | **OkHttp Interceptor**（注入 `X-User-Id`） | – |
| 持久化 | **Jetpack DataStore Preferences** | 替代 `uni.getStorageSync` |
| 状态 | **ViewModel + StateFlow** | – |
| 图片 | **Coil** | Glide |
| 视频 | **Media3 ExoPlayer + IJKPlayer**（RTMP/HLS/FLV） | 阿里云播放器 |
| 地图/定位 | **高德 Android SDK**（3D/2D/Search/Location） | – |
| 相机 | **CameraX** + ActivityResult `PickVisualMedia` | – |
| 权限 | **ActivityResultContracts.RequestPermission** | Accompanist |
| SVG | **AndroidSVG** | WebView |
| 知识图谱 | **WebView + G6** | Compose 自绘 |
| 震动/TTS/拨号/剪贴板 | 系统 API | – |
| 事件总线 | **Application 单例 SharedFlow** | – |

---

## 三、待办（编号驱动，按依赖顺序）

### A. 框架与基础设施（无业务依赖）
- [ ] **A1** Gradle/Kotlin/AGP 脚手架 + 版本目录（catalog）+ Compose BOM
- [ ] **A2** AndroidManifest 权限（INTERNET/RECORD_AUDIO/CAMERA/位置/震动）+ 高德 Key `<meta-data>`
- [ ] **A3** 主题/颜色系统（#1976d2 #2dba98 #f5f5f5 → Material3 ColorScheme）
- [ ] **A4** 底部 TabBar Composable（5 项 + 数字人中间凸起）
- [ ] **A5** 全局导航封装（navigateTo / switchTab / reLaunch / navigateBack）
- [ ] **A6** 网络层（Retrofit + AuthInterceptor + ApiResult<T>）
- [ ] **A7** HMAC-SHA256 + Base64 工具（替 120 行 JS）
- [ ] **A8** WebSocket 封装（OkHttp + 12s 心跳）
- [ ] **A9** DataStore（10 个 key：token / userInfo / itinerary / checkins / drafts / hotspot_likes / hotspot_favs / province_visited / agingMode / dh_chat_history）
- [ ] **A10** ViewModel + StateFlow 基类 + Application EventBus
- [ ] **A11** ActivityResult API 封装（PickVisualMedia）

### B. 全局组件与浮窗
- [ ] **B1** FloatingAssistant 浮窗（Canvas + 拖拽 + 录音波纹 + 说话波形 + 气泡）
- [ ] **B2** GlobalDh 唤醒后台服务（ForegroundService + AudioRecord + ASR WS + 唤醒词）
- [ ] **B3** AvatarPlayer 数字人播放器（IJKPlayer RTMP + 全屏 + 状态机）
- [ ] **B4** 麦克风长按录音组件

### C. 登录注册 / 老化
- [ ] **C1** 登录页 + UserViewModel + AuthInterceptor
- [ ] **C2** 注册页 + 校验
- [ ] **C3** 适老化模式页 + 全局 FontScale
- [ ] **C4** 占位 index 页 + 登出
- [ ] **C5** Mine 页（34 省 SVG + 缩放/拖拽 + 9 宫格 + 打卡）

### D. 5 个 Tab 页
- [ ] **D1** Home
- [ ] **D2** Map（最复杂之一）
- [ ] **D3** DigitalHuman（最复杂）
- [ ] **D4** Moments
- [ ] **D5** TabBar 5 项导航

### E. 16 个其他页面
- [ ] **E1** AI 聊天
- [ ] **E2** DigitalStream
- [ ] **E3** Scenic 知识图谱
- [ ] **E4** Events
- [ ] **E5** Food
- [ ] **E6** Checkin
- [ ] **E7** Guide
- [ ] **E8** Nearby
- [ ] **E9** Hotspots
- [ ] **E10** Search
- [ ] **E11** Itinerary
- [ ] **E12** LiveGuide

### F. 资源
- [ ] **F1** TabBar 5 对 PNG（10 张）
- [ ] **F2** 头像/导航/轮播等占位图
- [ ] **F3** 旅友圈帖子图

### G. 权限与端能力封装
- [ ] **G1** 动态权限申请（录音/相机/位置/相册）
- [ ] **G2** 高德地图 + 定位 SDK 集成
- [ ] **G3** Media3 ExoPlayer + IJKPlayer
- [ ] **G4** CameraX + ML Kit
- [ ] **G5** 通知渠道（ForegroundService）

---

## 四、关键决策

1. **鉴权**：uni 端实际**没有真正的 token**，仅伪造 `user-{userId}`，所有受保护接口靠业务 `user_id` 参数。Android 端**保持一致**，用 DataStore 存 `userId`，Retrofit 拦截器加 `X-User-Id` 头，**不引入 JWT**。
2. **数字人视频流**：后端 `/avatar/start` 返回 `protocol=rtmp` + `stream_url`，uni 端用 `<live-player>` 播。Android 端用 **IJKPlayer** 播 RTMP。
3. **xrtc 协议**：uni 端**未实际使用**（虽然在 `xunfei-ws.js` 写了 `protocol:'xrtc'`），主路径是 RTMP。Android 端**不实现 xrtc**。
4. **Base 坐标系**：所有定位 `type: 'gcj02'`，高德 SDK 默认就是 gcj02，**保持一致**。
5. **HLS/FLV**：攻略页 HLS 用 ExoPlayer；FLV 用 IJKPlayer。
6. **WebRTC 数字人**：不实现（uni 端未实现），用 RTMP 兜底。
7. **34 省份 SVG**：mine 页用 AndroidSVG 渲染，支持双指缩放/拖拽。
8. **G6 知识图谱**：用 WebView + G6（最简）。

---

## 五、风险与注意点

- **IJKPlayer 体积大**（约 4MB so），可改 Media3 播 RTMP（实验性）
- **高德 SDK 需要企业资质 Key 验证**；本项目有现成 Key 但应保留在 `local.properties`（不入 Git）
- **WebRTC 后续如需可单独加**（数字人 xrtc 流）
- **global-dh 后台录音耗电**，需 ForegroundService + 通知 + 用户手动开启
- **10 个 storage key** 在 DataStore 中要建结构化类型而非纯字符串

---

## 六、MVP 一期建议（可先跑起来）

如果不能一次交付全部，建议优先级：

**Phase 1（必须先跑）**：A1-A11 全部 + C1-C4（登录注册） + D1-D5（5 个 tab 基础版） + F1（tabbar 图标）

**Phase 2（核心业务）**：D3（数字人）+ D2（地图）+ D4（旅友圈） + B1（浮窗） + B2（唤醒）

**Phase 3（工具页）**：E1-E12 全部

**Phase 4（完善）**：C5（mine 页 34 省份）+ F2/F3 资源 + G 系列 SDK 集成

---

完成审计 ✓
