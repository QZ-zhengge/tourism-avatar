package com.example.let_us_go.data.prefs

import android.content.Context
import com.example.let_us_go.data.model.ItineraryItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * 行程本地存储（按用户隔离）
 *
 * 存储路径：{filesDir}/itinerary/{userId}.json
 * 不同登录用户在手机上各自独立的行程数据池。
 */
object ItineraryStore {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, ItineraryItem::class.java)
    private val adapter = moshi.adapter<List<ItineraryItem>>(listType)

    /**
     * 加载当前用户的行程列表
     */
    fun load(context: Context): List<ItineraryItem> {
        val uid = UserPrefs.userIdSync() ?: 0L
        if (uid == 0L) return emptyList()

        val file = getFile(context, uid)
        if (!file.exists()) return emptyList()

        return try {
            val json = file.readText()
            if (json.isBlank()) emptyList()
            else adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存当前用户的行程列表
     */
    fun save(context: Context, items: List<ItineraryItem>) {
        val uid = UserPrefs.userIdSync() ?: return
        if (uid == 0L) return

        val file = getFile(context, uid)
        try {
            file.parentFile?.mkdirs()
            file.writeText(adapter.toJson(items))
        } catch (e: Exception) {
            android.util.Log.e("ItineraryStore", "Save failed: ${e.message}")
        }
    }

    /**
     * 添加单个行程项
     */
    fun addItem(context: Context, item: ItineraryItem) {
        val items = load(context).toMutableList()
        items.add(item)
        save(context, items)
    }

    /**
     * 删除行程项
     */
    fun removeItem(context: Context, itemId: String) {
        val items = load(context).toMutableList()
        items.removeAll { it.id == itemId }
        save(context, items)
    }

    private fun getFile(context: Context, userId: Long): File {
        return File(context.filesDir, "itinerary/$userId.json")
    }
}