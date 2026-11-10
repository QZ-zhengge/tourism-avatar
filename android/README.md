# 随行原生 Android 客户端

用 Kotlin 与 Compose 提供景区导览、地图、行程和数字人交互。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [app/src/main/java/com/example/let_us_go/MainActivity.kt](app/src/main/java/com/example/let_us_go/MainActivity.kt) | Activity 启动 |
| [app/src/main/java/com/example/let_us_go/ui/nav](app/src/main/java/com/example/let_us_go/ui/nav) | Compose 路由 |
| [app/src/main/java/com/example/let_us_go/data/network](app/src/main/java/com/example/let_us_go/data/network) | 网络配置和鉴权 |
| [app/src/main/java/com/example/let_us_go/data/ws](app/src/main/java/com/example/let_us_go/data/ws) | 语音及数字人连接 |
| [app/src/main/java/com/example/let_us_go/data/prefs](app/src/main/java/com/example/let_us_go/data/prefs) | 本地会话与偏好 |

## 运行与验证

Android Studio 导入并准备 SDK 与 Gradle。配置自有后端、地图与语音服务，先用测试设备联调。归档未重新打包。
