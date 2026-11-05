# 随行 Android H5 外壳 · 代码索引

从 [项目 README](README.md) 了解主流程。下表把当前快照中的源码、页面、脚本与主要配置按目录列出。职责根据代码声明与目录用途概括。

| 文件 | 职责或阅读线索 |
| --- | --- |
| [app/build.gradle.kts](app/build.gradle.kts) | 配置构建、应用启动或环境参数 |
| [app/src/androidTest/java/com/example/uniapp_box/ExampleInstrumentedTest.kt](app/src/androidTest/java/com/example/uniapp_box/ExampleInstrumentedTest.kt) | 验证功能或回归场景。可从 `ExampleInstrumentedTest` 、 `useAppContext` 开始 |
| [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/java/com/example/uniapp_box/MainActivity.kt](app/src/main/java/com/example/uniapp_box/MainActivity.kt) | H5 容器入口。可从 `MainActivity` 、 `onCreate` 、 `onNewIntent` 、 `onDestroy` 开始 |
| [app/src/main/java/com/example/uniapp_box/data/ws/AsrWebSocket.kt](app/src/main/java/com/example/uniapp_box/data/ws/AsrWebSocket.kt) | 语音识别连接。可从 `AsrWebSocket` 、 `connectToAsr` 、 `sendConfig` 、 `sendAudio` 开始 |
| [app/src/main/java/com/example/uniapp_box/data/ws/BaseWebSocket.kt](app/src/main/java/com/example/uniapp_box/data/ws/BaseWebSocket.kt) | 封装接口调用、连接或数据解析。可从 `BaseWebSocket` 、 `class` 、 `open` 、 `connect` 开始 |
| [app/src/main/java/com/example/uniapp_box/float/ExoPlayerManager.kt](app/src/main/java/com/example/uniapp_box/float/ExoPlayerManager.kt) | 该目录下的业务实现或工具代码。可从 `getOrCreate` 、 `release` 、 `forceRelease` 、 `preload` 开始 |
| [app/src/main/java/com/example/uniapp_box/float/FloatChatManager.kt](app/src/main/java/com/example/uniapp_box/float/FloatChatManager.kt) | 会话编排。可从 `class` 、 `startChat` 、 `showDialog` 、 `dismissDialog` 开始 |
| [app/src/main/java/com/example/uniapp_box/float/FloatingWindow.kt](app/src/main/java/com/example/uniapp_box/float/FloatingWindow.kt) | 悬浮窗。可从 `FloatingWindow` 、 `handleFloatTap` 、 `DialogPanel` 、 `WaveAnimation` 开始 |
| [app/src/main/java/com/example/uniapp_box/ui/theme/Color.kt](app/src/main/java/com/example/uniapp_box/ui/theme/Color.kt) | 该目录下的业务实现或工具代码 |
| [app/src/main/java/com/example/uniapp_box/ui/theme/Theme.kt](app/src/main/java/com/example/uniapp_box/ui/theme/Theme.kt) | 该目录下的业务实现或工具代码。可从 `UniappBoxTheme` 开始 |
| [app/src/main/res/drawable/ic_launcher_background.xml](app/src/main/res/drawable/ic_launcher_background.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/drawable/ic_launcher_foreground.xml](app/src/main/res/drawable/ic_launcher_foreground.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml](app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml](app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/values-night/themes.xml](app/src/main/res/values-night/themes.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/values/colors.xml](app/src/main/res/values/colors.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/values/themes.xml](app/src/main/res/values/themes.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/xml/backup_rules.xml](app/src/main/res/xml/backup_rules.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/main/res/xml/data_extraction_rules.xml](app/src/main/res/xml/data_extraction_rules.xml) | 提供工程配置、界面资源或结构化数据 |
| [app/src/test/java/com/example/uniapp_box/ExampleUnitTest.kt](app/src/test/java/com/example/uniapp_box/ExampleUnitTest.kt) | 验证功能或回归场景。可从 `ExampleUnitTest` 、 `addition_isCorrect` 开始 |
| [build.gradle.kts](build.gradle.kts) | 配置构建、应用启动或环境参数 |
| [gradlew.bat](gradlew.bat) | 提供运行、构建或部署辅助 |
| [settings.gradle.kts](settings.gradle.kts) | 配置构建、应用启动或环境参数 |

本页列出 25 个可阅读的项目文件，以及 0 个第三方或生成文件。
