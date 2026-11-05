package com.example.let_us_go.ui.pages.ai_chat

import com.example.let_us_go.SuiXingApp
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.ChatMessage
import com.example.let_us_go.data.model.ChatRequest
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.ConversationStore
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * AI 问答 ViewModel
 *
 * 完整复刻 uni 端 pages/ai-chat/index.js：
 * - POST /api/v1/guide/chat   { session_id, query, scenic_id, scenic_name, with_memory:true }
 * - 响应字段优先级：answer > reply > text
 * - DELETE /api/v1/guide/memory/{sessionId}  清除记忆
 *
 * 与数字人页共享对话历史（通过 ConversationStore）
 */
class AiChatViewModel : BaseViewModel() {

    companion object {
        const val SCENIC_ID = "lingshan"
        const val SCENIC_NAME = "灵山胜境"
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /** 当前会话 ID，随 ViewModel 存活 */
    private val sessionId: String = UUID.randomUUID().toString()

    init {
        // 从共享存储加载当前轮次的历史消息
        loadHistory()
    }

    /**
     * 从共享对话存储加载历史消息
     */
    private fun loadHistory() {
        val context = SuiXingApp.instance
        val allMessages = ConversationStore.getCurrentMessages(context)

        val chatMessages = allMessages
            .sortedBy { it.time }
            .map { msg ->
                val cards = if (!msg.cardsJson.isNullOrBlank()) {
                    try {
                        val arr = org.json.JSONArray(msg.cardsJson)
                        val list = mutableListOf<com.example.let_us_go.data.model.HotelCard>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(com.example.let_us_go.data.model.HotelCard(
                                type = obj.optString("type", null),
                                kind = obj.optString("kind", null),
                                id = obj.optString("id", null),
                                name = obj.optString("name", ""),
                                title = obj.optString("title", null),
                                image = obj.optString("image", null),
                                rating = obj.optString("rating", null),
                                distance = obj.optString("distance", null),
                                price = obj.optString("price", null),
                                phone = obj.optString("phone", null),
                                bookingUrl = obj.optString("booking_url", null),
                                summary = obj.optString("summary", null)
                            ))
                        }
                        android.util.Log.d("AiChatVM", "恢复卡片: ${list.size} 张")
                        list
                    } catch (e: Exception) { 
                        android.util.Log.e("AiChatVM", "卡片反序列化失败: ${e.message}")
                        null 
                    }
                } else {
                    android.util.Log.d("AiChatVM", "消息无卡片JSON: role=${msg.role}")
                    null
                }
                ChatMessage(
                    role = msg.role,
                    type = msg.type,
                    content = msg.content,
                    time = msg.time,
                    cards = cards
                )
            }

        if (chatMessages.isNotEmpty()) {
            _messages.value = chatMessages
        } else {
            // 没有历史消息时显示欢迎语
            ensureWelcome()
        }
    }

    fun ensureWelcome() {
        if (_messages.value.isEmpty()) {
            _messages.value = listOf(
                ChatMessage(
                    role = "ai",
                    content = "你好，我是随行 AI 导游 ✨\n\n我可以帮你：\n• 规划游览路线\n• 推荐美食 / 酒店\n• 解答景区问题\n• 拍照识别景点\n\n请告诉我你想了解什么？"
                )
            )
        }
    }

    /**
     * 发送消息
     * 对应 uni 端 sendMessage()：POST /api/v1/guide/chat
     * 请求体：{ session_id, query, scenic_id, scenic_name, with_memory:true }
     */
    fun send(text: String) {
        val userMsg = ChatMessage(role = "user", content = text)
        _messages.value = _messages.value + userMsg
        // 同步到共享对话存储
        ConversationStore.appendMessage(SuiXingApp.instance, "ai_chat", userMsg)

        launchIo(showLoading = true) {
            val res = NetworkModule.guideApi.chat(
                ChatRequest(
                    query = text,
                    sessionId = sessionId,
                    userId = UserPrefs.userId(),
                    scenicId = SCENIC_ID,
                    scenicName = SCENIC_NAME,
                    withMemory = true
                )
            )
            val content = res.answerText().ifBlank { "（暂无回复）" }
            val aiMsg = ChatMessage(
                role = "ai",
                content = content,
                // 结构化预订卡片：取 data.card_data.items（真实数组），兜底 data.cards / 顶层 cards
                cards = res.bookingCards()
            )
            _messages.value = _messages.value + aiMsg
            // 同步到共享对话存储
            ConversationStore.appendMessage(SuiXingApp.instance, "ai_chat", aiMsg)
        }
    }

    /**
     * 清除 AI 记忆
     * 对应 uni 端 DELETE /api/v1/guide/memory/{sessionId}
     */
    fun clearMemory() {
        launchIo(showLoading = false) {
            runCatching {
                NetworkModule.guideApi.clearMemory(sessionId)
            }
            _messages.value = emptyList()
            ensureWelcome()
        }
    }

    /** 获取当前 sessionId（供 DigitalHuman 等页面复用） */
    fun getSessionId(): String = sessionId
}
