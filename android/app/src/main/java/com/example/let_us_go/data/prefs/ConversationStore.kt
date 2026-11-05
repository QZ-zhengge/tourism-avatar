package com.example.let_us_go.data.prefs

import android.content.Context
import android.util.Log
import com.example.let_us_go.data.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 共享对话历史存储
 *
 * 管理「一轮对话」的历史记录：
 * - 一轮 = 从 App 打开到退后台
 * - 数字人页和 AI 问答页共享同一个历史记录
 * - 实时读写，两个页面同步
 *
 * 存储路径：{filesDir}/conversations/{roundId}.json
 */
object ConversationStore {

    private const val TAG = "ConversationStore"

    /** 当前轮次 ID（格式：yyyyMMdd_HHmmss） */
    var currentRoundId: String = ""
        private set

    /** 当前轮次开始时间 */
    var roundStartTime: Long = 0L
        private set

    /**
     * 初始化新一轮对话（App 启动时调用）
     */
    fun startNewRound() {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        currentRoundId = sdf.format(Date())
        roundStartTime = System.currentTimeMillis()
        Log.d(TAG, "开始新一轮对话: roundId=$currentRoundId")
    }

    /**
     * 追加消息到当前轮次
     *
     * @param context Android Context
     * @param source 消息来源（"digital_human" 或 "ai_chat"）
     * @param message 消息内容
     */
    fun appendMessage(context: Context, source: String, message: ChatMessage) {
        if (currentRoundId.isBlank()) {
            Log.w(TAG, "currentRoundId 为空，跳过保存")
            return
        }
        try {
            val record = read(context, currentRoundId)
            val cardsJson = if (!message.cards.isNullOrEmpty()) {
                try {
                    val arr = org.json.JSONArray()
                    for (card in message.cards) {
                        val obj = org.json.JSONObject()
                        obj.put("type", card.type ?: "")
                        obj.put("kind", card.kind ?: "")
                        obj.put("id", card.id ?: "")
                        obj.put("name", card.name)
                        obj.put("title", card.title ?: "")
                        obj.put("image", card.image ?: "")
                        obj.put("rating", card.rating ?: "")
                        obj.put("distance", card.distance ?: "")
                        obj.put("price", card.price ?: "")
                        obj.put("phone", card.phone ?: "")
                        obj.put("booking_url", card.bookingUrl ?: "")
                        obj.put("summary", card.summary ?: "")
                        arr.put(obj)
                    }
                    Log.d(TAG, "保存卡片: ${message.cards.size} 张")
                    arr.toString()
                } catch (e: Exception) { 
                    Log.e(TAG, "卡片序列化失败: ${e.message}")
                    null 
                }
            } else {
                Log.d(TAG, "消息无卡片: role=${message.role}")
                null
            }
            val newMsg = MessageWithSource(
                role = message.role,
                type = message.type,
                content = message.content,
                time = message.time,
                source = source,
                cardsJson = cardsJson
            )
            val updated = record.copy(messages = record.messages + newMsg)
            write(context, updated)
            Log.d(TAG, "追加消息: source=$source, role=${message.role}, content=${message.content.take(30)}...")
        } catch (e: Exception) {
            Log.e(TAG, "appendMessage 失败: ${e.message}")
        }
    }

    /**
     * 获取当前轮次的所有消息（两个页面共享）
     */
    fun getCurrentMessages(context: Context): List<MessageWithSource> {
        if (currentRoundId.isBlank()) return emptyList()
        return read(context, currentRoundId).messages
    }

    /**
     * 获取指定来源的消息（用于各页面只显示自己的消息）
     */
    fun getMessagesBySource(context: Context, source: String): List<MessageWithSource> {
        return getCurrentMessages(context).filter { it.source == source }
    }

    /**
     * 获取所有历史轮次列表（用于历史记录页面）
     */
    fun getAllRounds(context: Context): List<ConversationRecord> {
        return try {
            val dir = File(context.filesDir, "conversations")
            if (!dir.exists()) return emptyList()

            dir.listFiles()
                ?.filter { it.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        val json = JSONObject(file.readText(Charsets.UTF_8))
                        val msgs = mutableListOf<MessageWithSource>()
                        val arr = json.optJSONArray("messages") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            msgs.add(
                                MessageWithSource(
                                    role = obj.optString("role", "user"),
                                    type = obj.optString("type", "text"),
                                    content = obj.optString("content", ""),
                                    time = obj.optLong("time", 0L),
                                    source = obj.optString("source", "unknown")
                                )
                            )
                        }
                        ConversationRecord(
                            roundId = json.optString("roundId", ""),
                            startTime = json.optLong("startTime", 0L),
                            messages = msgs
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.startTime }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getAllRounds 失败: ${e.message}")
            emptyList()
        }
    }

    // ────── 内部读写 ──────

    private fun read(context: Context, roundId: String): ConversationRecord {
        return try {
            val file = File(context.filesDir, "conversations/$roundId.json")
            if (!file.exists()) return ConversationRecord(roundId = roundId, startTime = roundStartTime)

            val json = JSONObject(file.readText(Charsets.UTF_8))
            val msgs = mutableListOf<MessageWithSource>()
            val arr = json.optJSONArray("messages") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                msgs.add(
                    MessageWithSource(
                        role = obj.optString("role", "user"),
                        type = obj.optString("type", "text"),
                        content = obj.optString("content", ""),
                        time = obj.optLong("time", 0L),
                        source = obj.optString("source", "unknown"),
                        cardsJson = obj.optString("cardsJson", null)
                    )
                )
            }
            ConversationRecord(
                roundId = json.optString("roundId", roundId),
                startTime = json.optLong("startTime", 0L),
                messages = msgs
            )
        } catch (e: Exception) {
            Log.e(TAG, "read 失败: ${e.message}")
            ConversationRecord(roundId = roundId, startTime = roundStartTime)
        }
    }

    private fun write(context: Context, record: ConversationRecord) {
        try {
            val dir = File(context.filesDir, "conversations")
            if (!dir.exists()) dir.mkdirs()

            val file = File(dir, "${record.roundId}.json")
            val json = JSONObject().apply {
                put("roundId", record.roundId)
                put("startTime", record.startTime)
                val arr = JSONArray()
                record.messages.forEach { msg ->
                    arr.put(JSONObject().apply {
                        put("role", msg.role)
                        put("type", msg.type)
                        put("content", msg.content)
                        put("time", msg.time)
                        put("source", msg.source)
                        if (!msg.cardsJson.isNullOrBlank()) {
                            put("cardsJson", msg.cardsJson)
                        }
                    })
                }
                put("messages", arr)
            }
            file.writeText(json.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "write 失败: ${e.message}")
        }
    }

    /**
     * 清除当前轮次（用于测试或用户主动清除）
     */
    fun clearCurrentRound(context: Context) {
        if (currentRoundId.isBlank()) return
        try {
            val file = File(context.filesDir, "conversations/$currentRoundId.json")
            if (file.exists()) file.delete()
            Log.d(TAG, "已清除当前轮次: $currentRoundId")
        } catch (e: Exception) {
            Log.e(TAG, "clearCurrentRound 失败: ${e.message}")
        }
    }
}

/**
 * 一条带来源的消息
 */
data class MessageWithSource(
    val role: String,
    val type: String = "text",
    val content: String = "",
    val time: Long = System.currentTimeMillis(),
    val source: String, // "digital_human" 或 "ai_chat"
    /** 结构化卡片 JSON（HotelCard 列表序列化） */
    val cardsJson: String? = null
)

/**
 * 一轮对话的完整记录
 */
data class ConversationRecord(
    val roundId: String,
    val startTime: Long = 0L,
    val messages: List<MessageWithSource> = emptyList()
)
