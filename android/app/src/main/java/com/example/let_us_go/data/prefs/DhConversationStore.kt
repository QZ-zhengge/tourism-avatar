package com.example.let_us_go.data.prefs

import android.content.Context
import android.util.Log
import com.example.let_us_go.data.model.ChatMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 数字人对话本地持久化
 *
 * 将每次会话的完整消息列表存为 JSON 文件，以便在「我的 → 数字人对话历史 → 详情」中回看。
 * 文件路径：{filesDir}/dh_conversations/{session_id}.json
 */
object DhConversationStore {

    private const val TAG = "DhConvStore"

    /** 一条对话记录（一个会话的所有消息） */
    data class ConversationRecord(
        val sessionId: String,
        val avatarId: String? = null,
        val startedAt: String? = null,
        val messages: List<ChatMessage> = emptyList()
    )

    // ────── 写 ──────

    /** 保存/追加一条消息到指定会话（不存在则创建） */
    fun appendMessage(context: Context, sessionId: String, message: ChatMessage) {
        runCatching {
            val record = read(context, sessionId)
            val updated = record.copy(messages = record.messages + message)
            write(context, updated)
        }.onFailure {
            Log.w(TAG, "appendMessage 失败: ${it.message}")
        }
    }

    /** 保存/追加多条消息到指定会话 */
    fun appendMessages(context: Context, sessionId: String, msgs: List<ChatMessage>) {
        if (msgs.isEmpty()) return
        runCatching {
            val record = read(context, sessionId)
            val updated = record.copy(messages = record.messages + msgs)
            write(context, updated)
        }.onFailure {
            Log.w(TAG, "appendMessages 失败: ${it.message}")
        }
    }

    /** 完整写入会话记录（覆盖） */
    fun write(context: Context, record: ConversationRecord) {
        runCatching {
            val dir = File(context.filesDir, "dh_conversations")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "${sanitize(record.sessionId)}.json")
            val json = JSONObject().apply {
                put("session_id", record.sessionId)
                put("avatar_id", record.avatarId ?: "")
                put("started_at", record.startedAt ?: "")
                val arr = JSONArray()
                record.messages.forEach { msg ->
                    arr.put(JSONObject().apply {
                        put("role", msg.role)
                        put("type", msg.type)
                        put("content", msg.content)
                        put("time", msg.time)
                    })
                }
                put("messages", arr)
            }
            file.writeText(json.toString(), Charsets.UTF_8)
            Log.d(TAG, "写入会话: session=${record.sessionId.take(16)}…, msgs=${record.messages.size}")
        }.onFailure {
            Log.w(TAG, "write 失败: ${it.message}")
        }
    }

    // ────── 读 ──────

    /** 读取指定会话（不存在返回空记录） */
    fun read(context: Context, sessionId: String): ConversationRecord {
        return try {
            val file = File(context.filesDir, "dh_conversations/${sanitize(sessionId)}.json")
            if (!file.exists()) return ConversationRecord(sessionId = sessionId)
            val json = JSONObject(file.readText(Charsets.UTF_8))
            val msgs = mutableListOf<ChatMessage>()
            val arr = json.optJSONArray("messages") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                msgs.add(
                    ChatMessage(
                        role = obj.optString("role", "user"),
                        type = obj.optString("type", "text"),
                        content = obj.optString("content", ""),
                        time = obj.optLong("time", 0L)
                    )
                )
            }
            ConversationRecord(
                sessionId = json.optString("session_id", sessionId),
                avatarId = json.optString("avatar_id", "").takeIf { it.isNotBlank() },
                startedAt = json.optString("started_at", "").takeIf { it.isNotBlank() },
                messages = msgs
            ).also {
                Log.d(TAG, "读取会话: session=${sessionId.take(16)}…, msgs=${msgs.size}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "read 失败: ${e.message}")
            ConversationRecord(sessionId = sessionId)
        }
    }

    /** 列出所有已存储的会话 ID */
    fun listSessionIds(context: Context): List<String> {
        return try {
            val dir = File(context.filesDir, "dh_conversations")
            if (!dir.exists()) return emptyList()
            dir.listFiles()
                ?.filter { it.extension == "json" }
                ?.map { it.nameWithoutExtension }
                ?.sorted()
                ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "listSessionIds 失败: ${e.message}")
            emptyList()
        }
    }

    /** 检查本地是否有指定会话的消息 */
    fun hasMessages(context: Context, sessionId: String): Boolean {
        return read(context, sessionId).messages.isNotEmpty()
    }

    /** 删除指定会话的本地文件 */
    fun delete(context: Context, sessionId: String) {
        try {
            val file = File(context.filesDir, "dh_conversations/${sanitize(sessionId)}.json")
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    /** 清理 session_id 中不适合做文件名的字符 */
    private fun sanitize(sessionId: String): String =
        sessionId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
}
