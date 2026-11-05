# 随行原生 Android 客户端

用 Kotlin 与 Compose 提供景区导览、地图、行程和数字人交互。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [app/src/main/java/com/example/let_us_go/MainActivity.kt](app/src/main/java/com/example/let_us_go/MainActivity.kt) | Activity 启动 |
| [app/src/main/java/com/example/let_us_go/ui/nav](app/src/main/java/com/example/let_us_go/ui/nav) | Compose 路由 |
| [app/src/main/java/com/example/let_us_go/data/network](app/src/main/java/com/example/let_us_go/data/network) | 网络配置和鉴权 |
| [app/src/main/java/com/example/let_us_go/data/ws](app/src/main/java/com/example/let_us_go/data/ws) | 语音及数字人连接 |
| [app/src/main/java/com/example/let_us_go/data/prefs](app/src/main/java/com/example/let_us_go/data/prefs) | 本地会话与偏好 |

## 一条业务链路

页面通过 API 读取景区数据，网络层统一解析与鉴权。本地偏好保存会话和行程。实时语音则走 WebSocket 通道。

## 运行与验证

Android Studio 导入并准备 SDK 与 Gradle。配置自有后端、地图与语音服务，先用测试设备联调。归档未重新打包。

## 面试可能会问

**普通查询与语音为何使用不同通信方式？**

景区列表可用请求响应。语音数据连续传输且需及时收到中间结果，适合持续连接。

**怎么处理用户离开页面？**

验证录音停止、连接关闭或复用，以及返回时状态恢复。先区分页面级状态与应用级会话。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
