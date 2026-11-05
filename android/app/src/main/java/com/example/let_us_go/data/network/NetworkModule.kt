package com.example.let_us_go.data.network

import android.content.Context
import com.example.let_us_go.BuildConfig
import com.example.let_us_go.data.api.AuthApi
import com.example.let_us_go.data.api.AsrApi
import com.example.let_us_go.data.api.CircleApi
import com.example.let_us_go.data.api.CheckinApi
import com.example.let_us_go.data.api.DigitalHumanApi
import com.example.let_us_go.data.api.GuideApi
import com.example.let_us_go.data.api.KnowledgeGraphApi
import com.example.let_us_go.data.api.RealtimeExplainApi
import com.example.let_us_go.data.api.SuggestionApi
import com.example.let_us_go.data.api.UploadApi
import com.example.let_us_go.data.api.UserProfileApi
import com.example.let_us_go.data.api.WeatherApi
import com.example.let_us_go.data.model.UserAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * 网络层
 *
 * 替代 uni-app `utils/api.js` 的 `request()` 封装。
 *
 * 设计：
 * - 单例 OkHttpClient（带连接池、CookieJar、Auth 拦截器、Logging 拦截器）
 * - 两套 Retrofit（主后端 API_BASE_URL + 数字人导览 GUIDE_BASE_URL）
 * - Moshi 处理 JSON（kotlin reflect + rfc3339）
 *
 * 调用：
 * ```
 * val response = NetworkModule.authApi.userLogin(username, password)
 * if (response.code == 200) ...
 * ```
 */
object NetworkModule {

    private const val TIMEOUT_CONNECT = 15L
    private const val TIMEOUT_READ = 30L
    private const val TIMEOUT_WRITE = 30L

    // 导览后端（50081 视觉识别等大模型接口）响应较慢，单独放宽超时
    private const val TIMEOUT_GUIDE_CONNECT = 20L
    private const val TIMEOUT_GUIDE_READ = 120L
    private const val TIMEOUT_GUIDE_WRITE = 60L

    @Volatile
    private var initialized = false

    private lateinit var moshi: Moshi
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var guideHttpClient: OkHttpClient
    private lateinit var mainRetrofit: Retrofit
    private lateinit var guideRetrofit: Retrofit

    // ─────────────── API 实例 ───────────────

    lateinit var authApi: AuthApi
        private set
    lateinit var circleApi: CircleApi
        private set
    lateinit var uploadApi: UploadApi
        private set
    lateinit var weatherApi: WeatherApi
        private set
    lateinit var guideApi: GuideApi
        private set
    lateinit var knowledgeGraphApi: KnowledgeGraphApi
        private set
    lateinit var digitalHumanApi: DigitalHumanApi
        private set
    lateinit var checkinApi: CheckinApi
        private set
    lateinit var suggestionApi: SuggestionApi
        private set
    lateinit var realtimeExplainApi: RealtimeExplainApi
        private set
    lateinit var asrApi: AsrApi
        private set
    lateinit var userProfileApi: UserProfileApi
        private set

    /** OkHttpClient 暴露给 WebSocket 等复用 */
    val okHttp: OkHttpClient get() = okHttpClient

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            moshi = Moshi.Builder()
                .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
                .add(MoshiLenientAdapters.LenientStringAdapter())
                .add(MoshiLenientAdapters.LenientIntAdapter())
                .add(UserAdapterFactory)
                .add(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.HEADERS
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            okHttpClient = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_CONNECT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_READ, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_WRITE, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(AuthInterceptor())
                .addInterceptor(logging)
                .build()

            // 导览后端专用客户端：放宽超时以适配慢速大模型接口（如拍照识别）
            guideHttpClient = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_GUIDE_CONNECT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_GUIDE_READ, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_GUIDE_WRITE, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(AuthInterceptor())
                .addInterceptor(logging)
                .build()

            mainRetrofit = Retrofit.Builder()
                .baseUrl(ensureSlash(BuildConfig.API_BASE_URL))
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            guideRetrofit = Retrofit.Builder()
                .baseUrl(ensureSlash(BuildConfig.GUIDE_BASE_URL))
                .client(guideHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            // 初始化所有 API
            authApi = mainRetrofit.create(AuthApi::class.java)
            circleApi = mainRetrofit.create(CircleApi::class.java)
            uploadApi = mainRetrofit.create(UploadApi::class.java)
            weatherApi = mainRetrofit.create(WeatherApi::class.java)
            guideApi = guideRetrofit.create(GuideApi::class.java)
            knowledgeGraphApi = guideRetrofit.create(KnowledgeGraphApi::class.java)
            digitalHumanApi = mainRetrofit.create(DigitalHumanApi::class.java)
            checkinApi = mainRetrofit.create(CheckinApi::class.java)
            suggestionApi = mainRetrofit.create(SuggestionApi::class.java)
            realtimeExplainApi = mainRetrofit.create(RealtimeExplainApi::class.java)
            asrApi = mainRetrofit.create(AsrApi::class.java)
            userProfileApi = mainRetrofit.create(UserProfileApi::class.java)

            initialized = true
        }
    }

    private fun ensureSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"

    /** 提供给业务层直接调用的 Moshi（解析复杂 JSON 字段） */
    fun moshi(): Moshi = moshi
}
