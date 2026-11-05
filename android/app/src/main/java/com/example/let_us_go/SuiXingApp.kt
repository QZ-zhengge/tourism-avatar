package com.example.let_us_go

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.example.let_us_go.core.bus.AppEventBus
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.ConversationStore
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 「随行」Application 单例
 *
 * - 初始化网络层（OkHttp + Retrofit）
 * - 初始化 DataStore
 * - 提供全局事件总线 [AppEventBus]
 * - 提供应用级 CoroutineScope
 * - 管理对话轮次（ConversationStore）
 *
 * 替代 uni-app：
 *   - Vue 根实例上的 `$emit` / `$on` / `$off`（→ AppEventBus）
 *   - 全局 storage（→ UserPrefs）
 *   - 全局模块（→ NetworkModule）
 *
 * 高德定位 SDK 初始化移至 Phase 2（map 页按需初始化）
 */
class SuiXingApp : Application(), Application.ActivityLifecycleCallbacks {

    val appScope: CoroutineScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    /** 活动计数器，用于检测前后台切换 */
    private var activityCount = 0

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "SuiXingApp onCreate")

        // 注册 Activity 生命周期回调
        registerActivityLifecycleCallbacks(this)

        // 捕获所有未捕获异常并记录（方便定位启动崩溃）
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            Log.e(TAG, "Uncaught exception on ${thread.name}", e)
            defaultHandler?.uncaughtException(thread, e)
        }

        try {
            // 1. DataStore 预热（先做，让 NetworkModule / AuthInterceptor 可用 userId）
            val t0 = System.currentTimeMillis()
            UserPrefs.init(this)
            Log.i(TAG, "UserPrefs initialized (${System.currentTimeMillis() - t0}ms)")
        } catch (e: Throwable) {
            Log.e(TAG, "UserPrefs.init failed", e)
        }

        try {
            // 2. 网络层（OkHttp + Retrofit 初始化）
            val t0 = System.currentTimeMillis()
            NetworkModule.init(this)
            Log.i(TAG, "NetworkModule initialized (${System.currentTimeMillis() - t0}ms)")
        } catch (e: Throwable) {
            Log.e(TAG, "NetworkModule.init failed", e)
        }

        // 3. 开始新一轮对话
        ConversationStore.startNewRound()
        Log.i(TAG, "ConversationStore: 新一轮对话开始, roundId=${ConversationStore.currentRoundId}")
    }

    override fun onTerminate() {
        super.onTerminate()
        AppEventBus.clear()
    }

    // ────── ActivityLifecycleCallbacks ──────

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Activity 创建
    }

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (activityCount == 1) {
            // App 从后台回到前台
            Log.i(TAG, "App 回到前台")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        // Activity 恢复
    }

    override fun onActivityPaused(activity: Activity) {
        // Activity 暂停
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            // App 进入后台
            Log.i(TAG, "App 进入后台, 当前轮次: ${ConversationStore.currentRoundId}")
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // 保存状态
    }

    override fun onActivityDestroyed(activity: Activity) {
        // Activity 销毁
    }

    companion object {
        private const val TAG = "SuiXingApp"
        @Volatile
        lateinit var instance: SuiXingApp
            private set

        @JvmStatic
        fun get(): SuiXingApp = instance
    }
}
