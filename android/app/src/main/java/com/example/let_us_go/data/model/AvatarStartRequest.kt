package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * POST /avatar/start 请求体
 *
 * avatar_id 和 vcn 后端会从 digital_human_config 表自动读取，前端可不传。
 */
@JsonClass(generateAdapter = true)
data class AvatarStartRequest(
    @Json(name = "avatar_id") val avatarId: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "width") val width: Int = 1280,
    @Json(name = "height") val height: Int = 720,
    @Json(name = "protocol") val protocol: String = "xrtc",
    @Json(name = "bg_transparent") val bgTransparent: Boolean = true
)
