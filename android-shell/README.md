# 随行 Android H5 外壳

用 Android 承载 H5 页面，并在原生侧提供悬浮窗、视频和语音能力。

## 项目结构

| 入口 | 负责什么 |
| --- | --- |
| [app/src/main/java/com/example/uniapp_box/MainActivity.kt](app/src/main/java/com/example/uniapp_box/MainActivity.kt) | H5 容器入口 |
| [app/src/main/java/com/example/uniapp_box/float/FloatingWindow.kt](app/src/main/java/com/example/uniapp_box/float/FloatingWindow.kt) | 悬浮窗 |
| [app/src/main/java/com/example/uniapp_box/float/FloatChatManager.kt](app/src/main/java/com/example/uniapp_box/float/FloatChatManager.kt) | 会话编排 |
| [app/src/main/java/com/example/uniapp_box/data/ws/AsrWebSocket.kt](app/src/main/java/com/example/uniapp_box/data/ws/AsrWebSocket.kt) | 语音识别连接 |

## 运行与验证

Android Studio 导入，准备 Gradle、Android SDK、自有 H5 和语音接口。真机验证麦克风、悬浮权限与播放器释放。
