package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 实时讲解历史条目
 *
 * 对应 README §10.2 列表项真实字段：
 * `id` / `user_id`(string) / `image`(base64 或 URL) /
 * `ai_response`(AI 返回的含 JSON 文本，原样存储) / `scenic_id` / `created_at`
 *
 * 注意：与最初草稿（question/answer/session_id）不同，后端实际以
 * `image` + `ai_response` 存储，前端自行解析 `ai_response` 取结构化字段。
 */
@JsonClass(generateAdapter = true)
data class RealtimeExplainHistory(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "ai_response") val aiResponse: String? = null,
    @Json(name = "scenic_id") val scenicId: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)
