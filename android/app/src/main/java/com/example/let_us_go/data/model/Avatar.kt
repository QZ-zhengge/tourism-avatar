package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 数字人会话
 *
 * 后端 `POST /avatar/start` 返回结构
 */
@JsonClass(generateAdapter = true)
data class AvatarSession(
    @Json(name = "session") val session: String = "",
    @Json(name = "client_id") val clientId: String? = null,
    @Json(name = "stream_url") val streamUrl: String? = null,
    @Json(name = "play_url") val playUrl: String? = null,
    @Json(name = "protocol") val protocol: String = "rtmp",
    @Json(name = "xfyun_code") val xfyunCode: Int = 0,
    @Json(name = "xfyun_message") val xfyunMessage: String? = null,
    @Json(name = "avatar_id") val avatarId: String? = null,
    @Json(name = "vcn") val vcn: String? = null
)

// ─── 6.10 数字人对话历史列表 ───

/** 对话历史列表分页信封（GET /avatar/chat/history/user/{user_id}） */
@JsonClass(generateAdapter = true)
data class AvatarChatHistoryPage(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "page_size") val pageSize: Int = 20,
    @Json(name = "items") val items: List<AvatarChatHistoryItem> = emptyList()
)

/** 单条对话历史：一个会话 + 最后一条消息 */
@JsonClass(generateAdapter = true)
data class AvatarChatHistoryItem(
    @Json(name = "session_id") val sessionId: String = "",
    @Json(name = "avatar_id") val avatarId: String? = null,
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "ended_at") val endedAt: String? = null,
    @Json(name = "message_count") val messageCount: Int = 0,
    @Json(name = "last_message") val lastMessage: String? = null
)
