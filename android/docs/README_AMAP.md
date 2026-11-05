# 高德地图 AAR 手动接入指南

> 因为高德地图 Android SDK 不在 Maven Central / Google / Jitpack 公共仓库，需要手动下载 aar 后通过 `fileTree` 接入。

---

## 步骤 1：注册高德开发者账号并创建应用

1. 访问 https://lbs.amap.com/dev/id/appkey
2. 用手机号注册（已有账号可跳过）
3. 「应用管理」→「我的应用」→「创建新应用」
4. 应用名随便填，例如 `随行智慧旅游`
5. 给 **Android 平台** 添加 Key：
   - PackageName：`com.example.let_us_go`
   - SHA1：见下文「查看 SHA1」一节
6. 复制生成的 **Android Key** 填到 `local.properties` 的 `AMAP_ANDROID_KEY`（已经有占位 `60c89976b2b93920896bf428bb25a88c`，直接用即可）

---

## 步骤 2：查看 SHA1（重要，否则 Key 不生效）

调试版 SHA1（debug keystore）：

```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android 2>$null | Select-String "SHA1"
```

把输出的 `SHA1: XX:XX:XX:...` 复制到高德后台。

---

## 步骤 3：下载 4 个 SDK 的 AAR

访问下面 4 个页面，点「下载 AAR」（不要下载「示例工程」/「一键接入」），会得到 4 个 ZIP。

| 模块 | 下载页 | 解压后要的 aar 文件 |
|------|--------|---------------------|
| **3D 地图** | https://lbs.amap.com/api/android-sdk/download | `AMap3DMap_xxx.aar` |
| **定位** | https://lbs.amap.com/api/android-location-sdk/download | `AMapLocation_xxx.aar` |
| **搜索/POI** | https://lbs.amap.com/api/webservice/download | `AMapSearch_xxx.aar` |
| **导航** | https://lbs.amap.com/api/android-navi-sdk/download | `AMapNavigation_xxx.aar` |

---

## 步骤 4：把 aar 放进项目

把 4 个 aar 文件**直接复制到**：

```
Z:\uniapp_to_and\let_us_go\app\libs\
```

不要建子目录，直接平铺。`app/build.gradle.kts` 已用：

```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))
```

会自动包含 `libs/` 下所有 `*.aar` 和 `*.jar`。

---

## 步骤 5：处理 aar 的传递依赖（重要）

高德 aar 内部依赖一些老 jar，必须**手动**放进 `libs/`：

打开任一高德 aar（它本身是个 ZIP），看 `libs/` 目录里的 jar 列表。一般来说：

- `AMap3DMap` aar 内自带依赖，不需要额外
- `AMapLocation` aar 内自带
- **AMapSearch** aar 通常依赖一个外部 `AMapSearchSDK.jar`（在 SDK 包里的 `AMapSearchSDK.jar` 单独存在）
- AMapNavigation aar 内部依赖 GLEngine 等 jar

**操作：**
1. 把每个 aar 当 ZIP 解压到临时目录，例如 `C:\tmp\amap\`
2. 进入解压后的 `libs/` 子目录，把里面所有 `.jar` 都复制到 `Z:\uniapp_to_and\let_us_go\app\libs\`
3. 删掉 `C:\tmp\amap\`（可选）

---

## 步骤 6：本地离线 repository（断网/防失败）

如果你机器访问 maven 失败，可额外在 `settings.gradle.kts` 顶部加阿里云镜像（已经加过了）：

```kotlin
maven { url = uri("https://maven.aliyun.com/repository/public") }
```

---

## 步骤 7：第一次编译

在 Android Studio 中：

1. **File → Sync Project with Gradle Files**
2. **Build → Rebuild Project**（首次会慢，因为解压 aar）
3. **Run → Run 'app'**（安装到模拟器/真机）

启动 App → 进入「地图」Tab → 应看到完整高德地图。

---

## 步骤 8：常见报错速查

| 报错 | 原因 | 修复 |
|------|------|------|
| `Could not find com.amap.api:3dmap:10.1.200` | aar 没复制 | 重复步骤 4 |
| `Manifest merger failed ... element meta-data#AMap_KEY` | Key 没注入 Manifest | 检查 `local.properties` 的 `AMAP_ANDROID_KEY` |
| 高德 SDK 隐私弹窗一闪而过 | AndroidManifest 漏 meta-data | 检查 `<meta-data android:name="com.amap.api.v2.apikey" .../>` 已在 |
| `UnsatisfiedLinkError: couldn't find libgdinitsync.so` | aar 自带 so 没被解压 | Build → Clean Project → Rebuild |
| 地图显示灰色底图 | 网络/Key 无效 | 检查 Android Key 的 PackageName 和 SHA1 是否匹配 |

---

## 已完成的本地配置

| 文件 | 内容 |
|------|------|
| `app/build.gradle.kts` | `implementation(fileTree("dir" to "libs", "include" to listOf("*.aar", "*.jar")))` |
| `gradle.properties` | `org.gradle.configuration-cache=false`（绕过 AGP 9.x 序列化 bug） |
| `app/src/main/AndroidManifest.xml` | 3 个高德隐私合规 `<meta-data>` |
| `app/src/main/res/xml/network_security_config.xml` | 高德域名放行 |

---

## 下一步

**Phase 2 编译通过后**告诉我，我会继续推进：
- Phase 3：全局浮窗 (GlobalDhFloat) + 唤醒服务 (Foreground Service + AudioRecord) + 富媒体上传 (UploadApi)