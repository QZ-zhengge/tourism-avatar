package com.example.let_us_go.data.api

import com.example.let_us_go.data.network.ApiResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 语音识别（ASR）HTTP 配套接口 —— 主后端 50080
 *
 * 实时识别走 WS /asr/stream（见 AsrWebSocket），这里是对应的 HTTP 管理接口：
 *  - GET  /asr/status   查询 Vosk 模型加载状态
 *  - POST /asr/preload  预热/加载模型（启动期调用可加速首次识别）
 *  - GET  /asr/models   查询可用模型列表
 */
interface AsrApi {

    @GET("asr/status")
    suspend fun status(): ApiResult<AsrStatus>

    @POST("asr/preload")
    suspend fun preload(
        @Body body: AsrPreloadBody = AsrPreloadBody()
    ): ApiResult<Any>

    @GET("asr/models")
    suspend fun models(): ApiResult<List<AsrModel>>
}

@JsonClass(generateAdapter = true)
data class AsrStatus(
    @Json(name = "loaded") val loaded: Boolean = false,
    @Json(name = "model") val model: String? = null,
    @Json(name = "ready") val ready: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AsrPreloadBody(
    val model: String = "vosk"
)

@JsonClass(generateAdapter = true)
data class AsrModel(
    @Json(name = "name") val name: String? = null,
    @Json(name = "desc") val desc: String? = null
)
