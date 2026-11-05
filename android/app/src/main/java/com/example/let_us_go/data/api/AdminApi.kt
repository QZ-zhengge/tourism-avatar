package com.example.let_us_go.data.api

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 管理端 API 客户端（47.236.85.42:50085）
 *
 * 仅供热门景点页调用，获取管理端上传的景点图片。
 * 不要私自调用本客户端的其他接口。
 */
object AdminApi {

    private const val TAG = "AdminApi"
    const val BASE_URL = "http://192.0.2.1:50085"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 获取景点图片列表
     * GET /api/v1/admin/content/images?scenic_id=lingshan
     *
     * @return Map<spot_name, List<完整图片URL>>
     */
    suspend fun fetchSpotImages(scenicId: String = "lingshan"): Map<String, List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/api/v1/admin/content/images?scenic_id=$scenicId"
                Log.d(TAG, "fetchSpotImages: $url")

                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "Admin API error: ${response.code}")
                    return@withContext emptyMap()
                }

                val json = JSONObject(body)
                val code = json.optInt("code", -1)
                if (code != 200) {
                    Log.e(TAG, "Admin API code: $code, body: ${body.take(100)}")
                    return@withContext emptyMap()
                }

                val data = json.optJSONArray("data") ?: JSONArray()
                val map = mutableMapOf<String, List<String>>()

                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    val spotName = item.optString("spot_name", "").trim()
                    val fileName = item.optString("file_name", "").trim()
                        .removeSuffix(".png").removeSuffix(".jpg").removeSuffix(".jpeg").trim()
                    val fileUrl = item.optString("file_url", "").trim()

                    if (fileUrl.isBlank()) continue

                    val fullUrl = if (fileUrl.startsWith("http")) fileUrl else "$BASE_URL$fileUrl"

                    // 优先用 spot_name 作为 key；同时用 file_name（去掉扩展名）作为兜底
                    val keys = mutableSetOf<String>()
                    if (spotName.isNotBlank()) keys.add(spotName)
                    if (fileName.isNotBlank() && fileName.length > 1) keys.add(fileName)

                    for (key in keys) {
                        if (!map.containsKey(key)) {
                            map[key] = listOf(fullUrl)
                        }
                    }
                }

                Log.d(TAG, "Loaded ${map.size} spot images")
                map
            } catch (e: Exception) {
                Log.e(TAG, "fetchSpotImages failed: ${e.message}", e)
                emptyMap()
            }
        }
    }
}