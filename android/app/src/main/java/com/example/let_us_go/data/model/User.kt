package com.example.let_us_go.data.model

import com.squareup.moshi.Json

/**
 * 用户模型
 *
 * 注意：不使用 @JsonClass(generateAdapter = true)，因为项目 KSP 未生成适配器。
 * 改为由 KotlinJsonAdapterFactory 反射解析（正确读取 @Json(name)）。
 */
data class User(
    @Json(name = "userId") val userId: Long = 0L,
    @Json(name = "username") val username: String = "",
    @Json(name = "password") val password: String? = null,
    @Json(name = "phoneNumber") val phoneNumber: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "level") val level: Int = 1,
    @Json(name = "points") val points: Int = 0,
    @Json(name = "scenicName") val scenicName: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "avatarText") val avatarText: String? = null,
)
