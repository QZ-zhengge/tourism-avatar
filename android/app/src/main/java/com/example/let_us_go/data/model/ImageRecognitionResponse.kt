package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 拍照识别响应（POST /api/v1/guide/image-recognition）
 *
 * 后端【不】包裹 ApiResult，直接返回该对象（与 guide/chat 一致）：
 * ```json
 * {
 *   "session_id": "xxx",
 *   "scenic_id": "lingshan",
 *   "speech_text": "这是灵山大佛……",
 *   "description": "这是灵山大佛……"
 * }
 * ```
 *
 * - [description]：讲解正文（聊天区展示）
 * - [speechText]：TTS 播报文本（送数字人对口型，缺省时回退到 description）
 */
@JsonClass(generateAdapter = true)
data class ImageRecognitionResponse(
    @Json(name = "session_id") val sessionId: String? = null,
    @Json(name = "scenic_id") val scenicId: String? = null,
    /** TTS 语音播报文本 */
    @Json(name = "speech_text") val speechText: String? = null,
    /** 讲解正文 */
    @Json(name = "description") val description: String? = null
) {
    /** 聊天区展示文本（description 优先） */
    fun answerText(): String = (description ?: speechText)?.takeIf { it.isNotBlank() } ?: ""

    /** 送数字人播报的文本（speech_text 优先，回退 description） */
    fun speechText(): String = (speechText ?: description)?.takeIf { it.isNotBlank() } ?: ""
}
