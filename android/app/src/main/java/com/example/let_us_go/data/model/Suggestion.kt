package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 建议（POST /suggestions/ 创建，GET /suggestions/user/{id} 列表）
 *
 * 后端返回字段：
 * id: String ("sg_xxx"), user_id: Int, content: String, contact: String?,
 * status: String ("unread"/"read"/"replied"), created_at: String (ISO datetime)
 */
@JsonClass(generateAdapter = true)
data class Suggestion(
    @Json(name = "id") val id: String = "",
    @Json(name = "user_id") val userId: Long = 0L,
    @Json(name = "content") val content: String = "",
    @Json(name = "contact") val contact: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "created_at") val createTime: String? = null
)
