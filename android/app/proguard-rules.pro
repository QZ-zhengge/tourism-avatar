# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\<USER>\AppData\Local\Android\Sdk\tools\proguard\proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ─── Moshi（反射使用，需要保留） ───
-keepclassmembers class kotlin.Metadata { *; }
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}
-keep class kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoaderImpl

# ─── OkHttp / Retrofit ───
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keep class kotlin.coroutines.Continuation

# ─── 高德 SDK ───
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-dontwarn com.amap.api.**

# ─── IJKPlayer ───
-keep class tv.danmaku.ijk.media.** { *; }
-dontwarn tv.danmaku.ijk.media.**

# ─── Media3 / ExoPlayer ───
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ─── ML Kit ───
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**

# ─── Kotlinx Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.android.AndroidExceptionPreHandler {
    public <init>(...);
}
-keepclassmembers class kotlinx.coroutines.android.AndroidDispatcherFactory {
    public <init>(...);
}

# ─── Compose ───
-keep class androidx.compose.** { *; }

# ─── Application 类与 ViewModel ───
-keep public class * extends android.app.Application
-keep public class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# ─── 自有数据模型（被 Moshi 反射） ───
-keep class com.example.let_us_go.data.model.** { *; }

# ─── 资源文件 ───
-keep class **.R$* { *; }
