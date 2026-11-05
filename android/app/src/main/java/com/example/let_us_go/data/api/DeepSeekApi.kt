package com.example.let_us_go.data.api

import android.util.Base64
import android.util.Log
import com.example.let_us_go.BuildConfig
import com.example.let_us_go.data.model.ItineraryItem
import com.example.let_us_go.data.model.ItineraryParseResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * DeepSeek AI API 客户端
 *
 * API Key 使用 XOR + SHA-256 混淆存储，反编译无法直接看到明文。
 */
object DeepSeekApi {

    private const val TAG = "DeepSeekApi"
    private const val BASE_URL = "https://api.deepseek.com/v1/chat/completions"
    private const val MODEL = "deepseek-chat"

    // XOR 混淆后的 API Key（Base64 编码），用 encryptKey() 生成
    private const val ENCRYPTED_KEY = "qwHHgJgUIVivqTDjZBzM8OIk9O2ObKa0c9UEFllg0HO6XdM="

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // XOR 密钥（基于 applicationId 的 SHA-256，32 字节）
    private val xorKey: ByteArray by lazy {
        MessageDigest.getInstance("SHA-256")
            .digest(BuildConfig.APPLICATION_ID.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decryptKey(): String {
        return try {
            val enc = Base64.decode(ENCRYPTED_KEY, Base64.NO_WRAP)
            val dec = ByteArray(enc.size)
            for (i in enc.indices) {
                dec[i] = (enc[i].toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
            }
            String(dec, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Key decryption failed: ${e.message}")
            ""
        }
    }

    /** 加密 + 生成 ENCRYPTED_KEY 值（App 内调用一次，复制输出） */
    fun encryptKey(plainKey: String): String {
        val plain = plainKey.toByteArray(StandardCharsets.UTF_8)
        val enc = ByteArray(plain.size)
        for (i in plain.indices) {
            enc[i] = (plain[i].toInt() xor xorKey[i % xorKey.size].toInt()).toByte()
        }
        return Base64.encodeToString(enc, Base64.NO_WRAP)
    }

    /**
     * AI 行程规划
     */
    suspend fun planItinerary(query: String): ItineraryParseResult {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = decryptKey()
                if (apiKey.isBlank()) {
                    return@withContext ItineraryParseResult(emptyList(), "API Key 未配置")
                }

                val body = JSONObject().apply {
                    put("model", MODEL)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "你是专业旅行规划助手。根据用户描述生成行程 JSON。\n格式：{\"items\":[{\"title\":\"项名\",\"summary\":\"描述\",\"time\":\"09:00\",\"date\":\"2026-07-15\",\"kind\":\"spot\"}],\"summary\":\"概述\"}\nkind取hotel/restaurant/transport/spot/note。只返回JSON，不要其他文字。")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", "帮我规划：$query")
                        })
                    })
                    put("temperature", 0.7)
                    put("max_tokens", 2000)
                }

                val request = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val respBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(respBody)
                    val choices = json.getJSONArray("choices")
                    val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                    Log.d(TAG, "DeepSeek OK, raw content: ${content.take(200)}...")
                    parseResponse(content)
                } else {
                    Log.e(TAG, "DeepSeek ${response.code}: $respBody")
                    ItineraryParseResult(emptyList(), "AI 服务错误（${response.code}）")
                }
            } catch (e: Exception) {
                Log.e(TAG, "DeepSeek failed: ${e.message}", e)
                ItineraryParseResult(emptyList(), "网络错误：${e.message}")
            }
        }
    }

    private fun parseResponse(raw: String): ItineraryParseResult {
        return try {
            var json = raw.trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val start = json.indexOf('{')
            if (start >= 0) json = json.substring(start)

            Log.d(TAG, "Parsing JSON: ${json.take(300)}...")  // 看待解析的 JSON

            val obj = JSONObject(json)
            val arr = obj.optJSONArray("items") ?: JSONArray()
            Log.d(TAG, "Parsed ${arr.length()} items")
            val items = (0 until arr.length()).map { i ->
                val item = arr.getJSONObject(i)
                ItineraryItem(
                    id = "ds_${System.currentTimeMillis()}_$i",
                    title = item.optString("title", "未命名"),
                    summary = item.optString("summary", ""),
                    time = item.optString("time", null),
                    date = item.optString("date", null),
                    type = "note",
                    kind = item.optString("kind", "note"),
                    price = if (item.has("price") && !item.isNull("price")) item.getDouble("price") else null,
                    checkIn = item.optString("checkIn", null),
                    checkOut = item.optString("checkOut", null),
                    roomType = item.optString("roomType", null)
                )
            }
            ItineraryParseResult(items = items, summary = obj.optString("summary", "AI 规划行程"))
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed: ${e.message}")
            ItineraryParseResult(
                items = listOf(ItineraryItem(
                    id = "ds_${System.currentTimeMillis()}",
                    title = "AI 规划结果",
                    summary = raw.take(500),
                    type = "note", kind = "note"
                )),
                summary = "AI 行程建议"
            )
        }
    }
}