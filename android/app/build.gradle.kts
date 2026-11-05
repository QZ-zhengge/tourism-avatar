import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    // kotlin-compose 必须显式 apply，负责 Compose 编译器版本匹配
    // kotlin-android 由 AGP 9.x 内置，不需要额外 apply（避免版本冲突）
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(FileInputStream(f))
}

// 从 local.properties 读取敏感凭据（不入 Git）
val amapAndroidKey: String = localProps.getProperty("AMAP_ANDROID_KEY", "CHANGE_ME")
val apiBaseUrl: String = localProps.getProperty("API_BASE_URL", "http://192.0.2.1:50080")
val guideBaseUrl: String = localProps.getProperty("GUIDE_BASE_URL", "http://192.0.2.1:50081")
val amapWebKey: String = localProps.getProperty("AMAP_WEB_KEY", "CHANGE_ME")
val xunfeiAppId: String = localProps.getProperty("XUNFEI_APP_ID", "CHANGE_ME")
val xunfeiApiKey: String = localProps.getProperty("XUNFEI_API_KEY", "CHANGE_ME")
val xunfeiApiSecret: String = localProps.getProperty("XUNFEI_API_SECRET", "CHANGE_ME")
val xunfeiSceneId: String = localProps.getProperty("XUNFEI_SCENE_ID", "315837983779590144")

android {
    namespace = "com.example.let_us_go"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.let_us_go"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "betav1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // BuildConfig 注入所有凭据
        buildConfigField("String", "AMAP_ANDROID_KEY", "\"$amapAndroidKey\"")
        buildConfigField("String", "AMAP_WEB_KEY", "\"$amapWebKey\"")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "GUIDE_BASE_URL", "\"$guideBaseUrl\"")
        buildConfigField("String", "XUNFEI_APP_ID", "\"$xunfeiAppId\"")
        buildConfigField("String", "XUNFEI_API_KEY", "\"$xunfeiApiKey\"")
        buildConfigField("String", "XUNFEI_API_SECRET", "\"$xunfeiApiSecret\"")
        buildConfigField("String", "XUNFEI_SCENE_ID", "\"$xunfeiSceneId\"")
        buildConfigField("String", "XUNFEI_WS_HOST", "\"avatar.cn-huadong-1.xf-yun.com\"")
        buildConfigField("String", "XUNFEI_WS_PATH", "\"/v1/interact_lips_flow\"")

        // 资源去重（AGP 9.x 新 API 替代 resourceConfigurations）
        @Suppress("UnstableApiUsage")
        androidResources {
            localeFilters += listOf("zh", "en")
        }

        // Manifest 占位符
        manifestPlaceholders["AMAP_ANDROID_KEY"] = amapAndroidKey
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    // AGP 9.x 内置 Kotlin 编译支持（android.builtInKotlin 默认 true）
    // 不再需要 kotlin{} 块或 java { toolchain{} } 块
    // JVM target 由 compileOptions 统一控制（VERSION_21）
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 16 KB 页面大小兼容性：提取 so 文件避免对齐警告
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // AndroidX 核心
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.core.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)

    // 协程
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // 网络
    implementation(platform(libs.okhttp.bom))
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.adapters)
    // Moshi Kotlin reflect 运行时依赖（moshi-kotlin 不传递）
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.21")

    // 图片 (Coil + SVG + Video Frame)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.video)

    // 权限
    implementation(libs.accompanist.permissions)

    // ─── Phase 2 ───
    // Media3 ExoPlayer (数字人 RTMP/HLS/FLV 播放)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.datasource.rtmp)  // RTMP 流支持（讯飞数字人）

    // 高德地图 (完整地图/POI/定位/导航) — 本地 aar/jar，详见 README_AMAP.md
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar", "*.jar"))))

    // 核心库 desugaring（让 minSdk 24 也能用 java.time 等）
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
