# 随行 Android H5 外壳

用 Android 承载 H5 页面，并在原生侧提供悬浮窗、视频和语音能力。

## 代码从哪里读

| 入口 | 负责什么 |
| --- | --- |
| [app/src/main/java/com/example/uniapp_box/MainActivity.kt](app/src/main/java/com/example/uniapp_box/MainActivity.kt) | H5 容器入口 |
| [app/src/main/java/com/example/uniapp_box/float/FloatingWindow.kt](app/src/main/java/com/example/uniapp_box/float/FloatingWindow.kt) | 悬浮窗 |
| [app/src/main/java/com/example/uniapp_box/float/FloatChatManager.kt](app/src/main/java/com/example/uniapp_box/float/FloatChatManager.kt) | 会话编排 |
| [app/src/main/java/com/example/uniapp_box/data/ws/AsrWebSocket.kt](app/src/main/java/com/example/uniapp_box/data/ws/AsrWebSocket.kt) | 语音识别连接 |

## 一条业务链路

应用加载 H5，悬浮交互由原生窗口处理。用户触发语音后，会话管理层调度识别连接和界面状态。

## 运行与验证

Android Studio 导入，准备 Gradle、Android SDK、自有 H5 和语音接口。真机验证麦克风、悬浮权限与播放器释放。

## 面试可能会问

**为什么使用 H5 加原生外壳？**

复用跨端业务页面，同时让录音、悬浮窗等设备相关能力走原生。

**拒绝悬浮权限时怎么办？**

展示可恢复的提示和 H5 主流程。验证权限撤销、退后台和音频资源释放。

## 继续阅读

- [逐文件代码索引](CODE_INDEX.md)
- [作品集展示与面试指南](https://github.com/QZ-zhengge/developer-portfolio/blob/main/docs/INTERVIEW.md)
