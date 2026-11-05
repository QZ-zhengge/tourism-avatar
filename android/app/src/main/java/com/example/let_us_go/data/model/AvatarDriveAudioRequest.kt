package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * POST /avatar/drive/audio/url 请求体
 * 获取音频驱动 WSS 鉴权 URL
 */
@JsonClass(generateAdapter = true)
data class AvatarDriveAudioUrlRequest(
    @Json(name = "session") val session: String
)

/**
 * POST /avatar/drive/audio/url 响应数据
 */
@JsonClass(generateAdapter = true)
data class AvatarDriveAudioUrlResponse(
    @Json(name = "wss_url") val wssUrl: String = "",
    @Json(name = "session") val session: String = "",
    @Json(name = "app_id") val appId: String = "",
    @Json(name = "encoding") val encoding: String = "raw",
    @Json(name = "sample_rate") val sampleRate: Int = 16000
)

/**
 * POST /avatar/drive/audio/proxy 请求体
 * 音频帧代理转发
 */
@JsonClass(generateAdapter = true)
data class AvatarDriveAudioProxyRequest(
    @Json(name = "session") val session: String,
    @Json(name = "audio_base64") val audioBase64: String,
    @Json(name = "status") val status: Int,  // 0=开始, 1=继续, 2=结束
    @Json(name = "seq") val seq: Int = 1,
    @Json(name = "encoding") val encoding: String = "raw",
    @Json(name = "sample_rate") val sampleRate: Int = 16000
)
