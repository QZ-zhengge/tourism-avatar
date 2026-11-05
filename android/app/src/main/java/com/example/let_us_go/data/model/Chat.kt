package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * LLM Chat 请求
 *
 * 对应后端 POST /api/v1/guide/chat 请求体（字段与后端 1:1 对齐，可选字段留空即可）：
 * {
 *   "session_id": "string",
 *   "query": "帮我订酒店",
 *   "scenic_id": "lingshan",
 *   "scenic_name": "灵山胜境",
 *   "destination_id": "string",
 *   "destination_name": "string",
 *   "scope_mode": "current_only",
 *   "current_location": { "lat": 0, "lng": 0 },
 *   "page_id": "string",
 *   "user_profile": {},
 *   "with_memory": true
 * }
 */
@JsonClass(generateAdapter = true)
data class ChatRequest(
    /** 对话文本 */
    @Json(name = "query") val query: String,
    @Json(name = "session_id") val sessionId: String? = null,
    /** 当前登录用户 ID（可选，未登录传 null） */
    @Json(name = "user_id") val userId: Long? = null,
    @Json(name = "scenic_id") val scenicId: String? = "lingshan",
    @Json(name = "scenic_name") val scenicName: String? = "灵山胜境",
    @Json(name = "destination_id") val destinationId: String? = null,
    @Json(name = "destination_name") val destinationName: String? = null,
    @Json(name = "scope_mode") val scopeMode: String? = null,
    /** 当前定位坐标（可选） */
    @Json(name = "current_location") val currentLocation: GeoPoint? = null,
    @Json(name = "page_id") val pageId: String? = null,
    /** 用户画像（可选，留空对象） */
    @Json(name = "user_profile") val userProfile: Map<String, Any>? = null,
    @Json(name = "with_memory") val withMemory: Boolean = true
)

/** current_location 坐标 */
@JsonClass(generateAdapter = true)
data class GeoPoint(
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lng") val lng: Double = 0.0
)

/**
 * LLM Chat 消息（UI 本地存储，也用于发送历史 history 字段）
 */
@JsonClass(generateAdapter = true)
data class ChatMessage(
    @Json(name = "role") val role: String,                        // user / ai
    @Json(name = "type") val type: String = "text",               // text / card / hotel-list / rich
    @Json(name = "content") val content: String = "",
    @Json(name = "time") val time: Long = System.currentTimeMillis(),
    @Json(name = "data") val data: Map<String, Any>? = null,
    @Json(name = "cards") val cards: List<HotelCard>? = null,
    @Json(name = "image_path") val imagePath: String? = null      // 拍照识别时保存的图片路径
)

/**
 * LLM Chat 响应
 *
 * 后端直接返回 GuideChatResponse，不包裹 ApiResult。真实结构（实测）：
 * ```json
 * {
 *   "session_id": "xxx",
 *   "intent": "agent_task",
 *   "action": "show_agent_result",
 *   "answer": "好的，帮您找到酒店推荐。点击下方卡片即可继续查看和预订。",
 *   "speech_text": "好的，帮您找到酒店推荐。",
 *   "ui_command": { "type": "show_agent_result" },
 *   "data": {
 *     "task_type": "hotel_query",
 *     "used_meituan": true,
 *     "card_data": {                            // ← 预订卡片【容器】
 *       "type": "booking_cards",
 *       "title": "酒店推荐",
 *       "items": [                              // ← 真正的卡片数组
 *         {
 *           "type": "booking_card",
 *           "kind": "hotel",
 *           "title": "如家酒店(无锡南禅寺三阳广场店)",
 *           "image": "http://...jpg",
 *           "price": "￥141起/晚",             // 注意：后端返回的是字符串
 *           "rating": "4.7分",                  // 注意：后端返回的是字符串
 *           "distance": "",
 *           "booking_url": "http://dpurl.cn/xxx",
 *           "phone": "",
 *           "summary": "如家酒店位于...",
 *           "actions": [ { "type": "book_now", "label": "立即预订", "url": "..." },
 *                        { "type": "call", "label": "致电咨询", "phone": "" } ]
 *         }
 *       ]
 *     },
 *     "cards": [ /* 美团 + 知识库混合结果，可作兜底 */ ]
 *   }
 * }
 * ```
 *
 * 取卡优先级：data.card_data.items > data.cards > 顶层 cards
 */
@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "intent") val intent: String? = null,
    @Json(name = "action") val action: String? = null,
    /** 主回答文本 */
    @Json(name = "answer") val answer: String? = null,
    /** TTS 语音播报文本 */
    @Json(name = "speech_text") val speechText: String? = null,
    /** 回复文本（兼容旧格式） */
    @Json(name = "reply") val reply: String = "",
    @Json(name = "text") val text: String? = null,
    @Json(name = "ui_command") val uiCommand: Map<String, Any>? = null,
    /** 后端结构化结果（含预订卡片 card_data） */
    @Json(name = "data") val data: AgentResultData? = null,
    @Json(name = "memory") val memory: Map<String, Any>? = null,
    @Json(name = "sentiment") val sentiment: Map<String, Any>? = null,
    @Json(name = "debug") val debug: Map<String, Any>? = null,
    /** 顶层卡片列表（兼容旧格式，真实响应一般在 data 内） */
    @Json(name = "cards") val cards: List<HotelCard>? = null,
    @Json(name = "suggestions") val suggestions: List<String>? = null
) {
    /** 统一取回复文本（与后端 answer > reply > text 顺序一致） */
    fun answerText(): String = when {
        !answer.isNullOrBlank() -> answer
        reply.isNotBlank()      -> reply
        !text.isNullOrBlank()   -> text
        else                    -> ""
    }

    /**
     * 提取可展示的结构化卡片列表。
     * 优先级：data.card_data.items（真实预订卡片）> data.cards（混合兜底）> 顶层 cards。
     */
    fun bookingCards(): List<HotelCard> {
        val fromContainer = data?.cardData?.items
        if (!fromContainer.isNullOrEmpty()) return fromContainer
        val fromDataCards = data?.cards
        if (!fromDataCards.isNullOrEmpty()) return fromDataCards
        return cards ?: emptyList()
    }
}

@JsonClass(generateAdapter = true)
data class AgentResultData(
    /** 结构化预订 / 推荐卡片容器：{ type, title, items:[HotelCard] } */
    @Json(name = "card_data") val cardData: BookingCardData? = null,
    /** 知识库 + 美团混合结果列表（可选，可作为兜底） */
    @Json(name = "cards") val cards: List<HotelCard>? = null
)

/** data.card_data 容器：type + title + items[] */
@JsonClass(generateAdapter = true)
data class BookingCardData(
    @Json(name = "type") val type: String? = null,
    @Json(name = "title") val title: String? = null,
    /** 真正的预订 / 推荐卡片数组 */
    @Json(name = "items") val items: List<HotelCard>? = null
)

@JsonClass(generateAdapter = true)
data class HotelCard(
    @Json(name = "type") val type: String? = null,
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null,
    /** 评分：后端返回字符串（如"4.7分" / "评分待确认"） */
    @Json(name = "rating") val rating: String? = null,
    @Json(name = "distance") val distance: String? = null,
    /** 价格：后端返回字符串（如"￥141起/晚" / "价格待确认"） */
    @Json(name = "price") val price: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "booking_url") val bookingUrl: String? = null,
    /** 卡片摘要描述（后端 card_data.items[].summary，含 markdown） */
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "actions") val actions: List<CardAction>? = null
)

@JsonClass(generateAdapter = true)
data class CardAction(
    @Json(name = "type") val type: String,                        // book_now / call
    @Json(name = "label") val label: String,
    @Json(name = "url") val url: String? = null,
    @Json(name = "phone") val phone: String? = null
)
