package com.example.let_us_go.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.let_us_go.data.model.User
import com.example.let_us_go.data.model.UserAdapterFactory
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * 「随行」持久化层
 *
 * 替代 uni-app 的 `uni.getStorageSync` / `uni.setStorageSync` / `uni.removeStorageSync`。
 *
 * 10 个 key 一一对应：
 * 1. token
 * 2. userInfo
 * 3. suixing_itinerary
 * 4. suixing_checkins
 * 5. moments_drafts
 * 6. hotspot_likes
 * 7. hotspot_favs
 * 8. province_visited
 * 9. agingMode
 * 10. dh_chat_history
 *
 * 序列化策略：复杂对象用 JSON（Moshi），简单标量用 Preferences 原生类型。
 *
 * 提供 runBlocking 同步访问入口（供 OkHttp 拦截器等必须在主线程返回的场景），
 * 但推荐在协程中通过 [tokenFlow] 等 Flow API 订阅。
 */
object UserPrefs {

    private const val DS_NAME = "suixing_prefs"

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = DS_NAME)

    private lateinit var appContext: Context
    private val moshi: Moshi = Moshi.Builder()
        .add(UserAdapterFactory)
        .add(KotlinJsonAdapterFactory())
        .build()
    private val userAdapter: JsonAdapter<User> = moshi.adapter(User::class.java)

    /** 初始化（Application.onCreate 中调用，仅存上下文引用，耗时操作延迟） */
    fun init(context: Context) {
        try {
            appContext = context.applicationContext
        } catch (e: Throwable) {
            android.util.Log.e("UserPrefs", "init failed", e)
        }
    }

    // ──────────────────────────────────────────────
    //  Keys
    // ──────────────────────────────────────────────

    private val K_TOKEN = stringPreferencesKey("token")                // "user-{userId}"
    private val K_USER_ID = longPreferencesKey("user_id")
    private val K_USERNAME = stringPreferencesKey("username")
    private val K_USER_JSON = stringPreferencesKey("user_info_json")
    private val K_AGING_MODE = booleanPreferencesKey("aging_mode")
    private val K_FONT_SCALE = floatPreferencesKey("font_scale")
    private val K_ITINERARY = stringPreferencesKey("suixing_itinerary")  // JSON
    private val K_CHECKINS = stringPreferencesKey("suixing_checkins")      // JSON
    private val K_DRAFTS = stringPreferencesKey("moments_drafts")         // JSON
    private val K_HOTSPOT_LIKES = stringPreferencesKey("hotspot_likes")    // JSON
    private val K_HOTSPOT_FAVS = stringPreferencesKey("hotspot_favs")      // JSON
    private val K_PROVINCE_VISITED = stringPreferencesKey("province_visited") // JSON
    private val K_DH_CHAT_HISTORY = stringPreferencesKey("dh_chat_history")  // JSON
    private val K_FOLLOWED_SPOTS = stringPreferencesKey("followed_spots")  // JSON

    // ──────────────────────────────────────────────
    //  Token / User
    // ──────────────────────────────────────────────

    /** 同步获取 token（OkHttp 拦截器等场景用） */
    fun tokenSync(): String? {
        if (!::appContext.isInitialized) return null
        return runCatching {
            runBlocking { appContext.dataStore.data.first()[K_TOKEN] }
        }.getOrNull()
    }
    suspend fun token(): String? = appContext.dataStore.data.first()[K_TOKEN]

    fun tokenFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_TOKEN] }

    suspend fun setToken(token: String) {
        appContext.dataStore.edit { it[K_TOKEN] = token }
    }

    /** 是否已登录：直接读文件 */
    fun isLoggedIn(): Boolean = LoginStateFile.readUserId(appContextOrNull()) > 0L

    /** 同步获取 userId（拦截器、ViewModel 等全部统一走这里）——直接读文件 */
    fun userIdSync(): Long = LoginStateFile.readUserId(appContextOrNull())

    /** 挂起获取 userId（Composable 里用，和 userIdSync 一样直接读文件） */
    suspend fun userId(): Long = userIdSync()

    /** userId Flow（Compose collectAsState 用） */
    fun userIdFlow(): kotlinx.coroutines.flow.Flow<Long?> =
        kotlinx.coroutines.flow.flow { emit(userIdSync().takeIf { it > 0L }) }

    /** 冷启动恢复（兼容旧版本，已无实际作用，保留不报错） */
    fun restoreUserId(): Long = userIdSync()

    /** 内部使用：给同包内访问 appContext 提供一个不导出给外部的安全入口 */
    internal fun appContextOrNull(): Context? = if (::appContext.isInitialized) appContext else null

    suspend fun saveLogin(user: User) {
        if (user.userId <= 0L) {
            Log.w("UserPrefs", "saveLogin: userId=${user.userId} <= 0，跳过写入登录文件")
            return
        }
        // ★ 唯一写入点：JSON 文件
        LoginStateFile.write(appContext, user.userId, user.username)
        // DataStore 保留（其他功能可能依赖 K_USER_JSON 等 key，如适老化、行程单）
        appContext.dataStore.edit { prefs ->
            prefs[K_TOKEN] = "user-${user.userId}"
            prefs[K_USER_ID] = user.userId
            prefs[K_USERNAME] = user.username
            prefs[K_USER_JSON] = userAdapter.toJson(user)
        }
    }

    suspend fun currentUser(): User? {
        val json = appContext.dataStore.data.first()[K_USER_JSON] ?: return null
        return runCatching { userAdapter.fromJson(json) }.getOrNull()
    }

    fun currentUserFlow(): Flow<User?> = appContext.dataStore.data.map { prefs ->
        prefs[K_USER_JSON]?.let { runCatching { userAdapter.fromJson(it) }.getOrNull() }
    }

    // ──────────────────────────────────────────────
    //  适老化模式
    // ──────────────────────────────────────────────

    suspend fun setAgingMode(enabled: Boolean) {
        appContext.dataStore.edit { it[K_AGING_MODE] = enabled }
    }

    fun agingMode(): Boolean {
        if (!::appContext.isInitialized) return false
        return runCatching {
            runBlocking { appContext.dataStore.data.first()[K_AGING_MODE] ?: false }
        }.getOrDefault(false)
    }
    fun agingModeFlow(): Flow<Boolean> = appContext.dataStore.data.map { it[K_AGING_MODE] ?: false }

    suspend fun setFontScale(scale: Float) {
        appContext.dataStore.edit { it[K_FONT_SCALE] = scale.coerceIn(1.0f, 1.6f) }
    }

    fun fontScale(): Float {
        if (!::appContext.isInitialized) return 1.0f
        return runCatching {
            runBlocking { appContext.dataStore.data.first()[K_FONT_SCALE] ?: 1.0f }
        }.getOrDefault(1.0f)
    }
    fun fontScaleFlow(): Flow<Float> = appContext.dataStore.data.map { it[K_FONT_SCALE] ?: 1.0f }

    // ──────────────────────────────────────────────
    //  通用 JSON 字段（按需解析为 String，业务层解析）
    // ──────────────────────────────────────────────

    suspend fun setItineraryJson(json: String) = appContext.dataStore.edit { it[K_ITINERARY] = json }
    fun itineraryJsonFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_ITINERARY] }
    suspend fun itineraryJson(): String? = appContext.dataStore.data.first()[K_ITINERARY]

    suspend fun setCheckinsJson(json: String) = appContext.dataStore.edit { it[K_CHECKINS] = json }
    fun checkinsJsonFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_CHECKINS] }
    suspend fun checkinsJson(): String? = appContext.dataStore.data.first()[K_CHECKINS]

    suspend fun setDraftsJson(json: String) = appContext.dataStore.edit { it[K_DRAFTS] = json }
    fun draftsJsonFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_DRAFTS] }
    suspend fun draftsJson(): String? = appContext.dataStore.data.first()[K_DRAFTS]

    suspend fun setHotspotLikesJson(json: String) = appContext.dataStore.edit { it[K_HOTSPOT_LIKES] = json }
    fun hotspotLikesFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_HOTSPOT_LIKES] }

    suspend fun setHotspotFavsJson(json: String) = appContext.dataStore.edit { it[K_HOTSPOT_FAVS] = json }
    fun hotspotFavsFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_HOTSPOT_FAVS] }

    suspend fun setProvinceVisitedJson(json: String) = appContext.dataStore.edit { it[K_PROVINCE_VISITED] = json }
    fun provinceVisitedFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_PROVINCE_VISITED] }

    suspend fun setDhChatHistoryJson(json: String) = appContext.dataStore.edit { it[K_DH_CHAT_HISTORY] = json }
    fun dhChatHistoryFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_DH_CHAT_HISTORY] }

    suspend fun setFollowedSpotsJson(json: String) = appContext.dataStore.edit { it[K_FOLLOWED_SPOTS] = json }
    fun followedSpotsFlow(): Flow<String?> = appContext.dataStore.data.map { it[K_FOLLOWED_SPOTS] }

    // ──────────────────────────────────────────────
    //  登出（清空 6 个 key，与 uni 端一致）
    // ──────────────────────────────────────────────

    suspend fun logout() {
        Log.w("UserPrefs", "logout() 被调用！调用栈:", Throwable("LOGOUT_TRACE"))
        // ★ 唯一清除点：删除 JSON 文件
        LoginStateFile.clear(appContext)
        // DataStore 同步清除
        appContext.dataStore.edit { prefs ->
            prefs.remove(K_TOKEN)
            prefs.remove(K_USER_ID)
            prefs.remove(K_USERNAME)
            prefs.remove(K_USER_JSON)
            prefs.remove(K_AGING_MODE)
            prefs.remove(K_FONT_SCALE)
        }
    }

    suspend fun clearAll() {
        appContext.dataStore.edit { it.clear() }
    }
}
