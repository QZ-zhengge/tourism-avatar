package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * POST /avatar/drive/text 请求体
 *
 * 必须与 uniapp index.js driveText() 完全一致
 */
@JsonClass(generateAdapter = true)
data class AvatarDriveTextRequest(
    @Json(name = "session") val session: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "user_text") val userText: String? = null,
    @Json(name = "text") val text: String,
    @Json(name = "vcn") val vcn: String = "x4_lingxiaoyu_assist",
    @Json(name = "speed") val speed: Int = 50,
    @Json(name = "pitch") val pitch: Int = 50,
    @Json(name = "volume") val volume: Int = 50
)

/**
 * POST /avatar/ping 请求体
 */
@JsonClass(generateAdapter = true)
data class AvatarPingRequest(
    @Json(name = "session") val session: String
)

/**
 * POST /avatar/stop 请求体
 */
@JsonClass(generateAdapter = true)
data class AvatarStopRequest(
    @Json(name = "session") val session: String
)

/**
 * GET /avatar/config 响应（数字人默认形象/发音人配置）
 */
@JsonClass(generateAdapter = true)
data class AvatarConfig(
    @Json(name = "avatar_id") val avatarId: String? = null,
    @Json(name = "vcn") val vcn: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "desc") val desc: String? = null
)

/**
 * GET /avatar/actions/{avatar_id} 响应元素（动作列表）
 */
@JsonClass(generateAdapter = true)
data class AvatarAction(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "desc") val desc: String? = null
)

/**
 * GET /avatar/vcn/list 响应元素（可选发音人列表）
 */
@JsonClass(generateAdapter = true)
data class AvatarVcn(
    @Json(name = "vcn") val vcn: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "desc") val desc: String? = null
)
