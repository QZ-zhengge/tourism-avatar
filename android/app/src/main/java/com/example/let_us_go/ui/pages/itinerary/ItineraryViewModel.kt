package com.example.let_us_go.ui.pages.itinerary

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.let_us_go.core.bus.AppEventBus
import com.example.let_us_go.data.api.DeepSeekApi
import com.example.let_us_go.data.model.ItineraryItem
import com.example.let_us_go.data.prefs.ItineraryStore
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 我的行程 ViewModel
 *
 * 功能：
 * - 加载/保存本地行程（按用户隔离）
 * - AI 规划（调用 DeepSeek API）
 * - 监听数字人页的酒店预订事件
 * - 删除行程项
 */
class ItineraryViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ItineraryVM"
    }

    private val context = application.applicationContext

    private val _items = MutableStateFlow<List<ItineraryItem>>(emptyList())
    val items: StateFlow<List<ItineraryItem>> = _items.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadFromStore()

        // 监听酒店预订事件（来自数字人页）
        viewModelScope.launch {
            AppEventBus.events.collect { event ->
                if (event is AppEventBus.DhBookingCard) {
                    addFromBooking(event.title, event.summary)
                } else if (event is AppEventBus.ItineraryChanged) {
                    loadFromStore()
                } else if (event is AppEventBus.LoginStateChanged) {
                    // 切换用户后重新加载
                    loadFromStore()
                }
            }
        }
    }

    private fun loadFromStore() {
        _items.value = ItineraryStore.load(context)
    }

    /**
     * 从数字人酒店预订卡片添加行程项
     */
    private fun addFromBooking(title: String, summary: String) {
        val item = ItineraryItem(
            id = "booking_${System.currentTimeMillis()}",
            type = "booking",
            kind = "hotel",
            title = title,
            summary = summary,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(System.currentTimeMillis()),
            note = "来自数字人预订"
        )
        ItineraryStore.addItem(context, item)
        loadFromStore()
        _aiSummary.value = "已添加酒店：$title"
    }

    /**
     * AI 规划行程（调用 DeepSeek）
     */
    fun planByAi(query: String) {
        if (query.isBlank()) return

        val userId = UserPrefs.userIdSync() ?: 0L
        if (userId == 0L) {
            _error.value = "请先登录后使用 AI 规划"
            return
        }

        _loading.value = true
        _error.value = null
        _aiSummary.value = null

        viewModelScope.launch {
            try {
                Log.d(TAG, "planByAi: calling DeepSeek with query=$query")
                val result = DeepSeekApi.planItinerary(query)
                Log.d(TAG, "planByAi: got ${result.items.size} items, summary=${result.summary}")

                if (result.items.isNotEmpty()) {
                    // 合并到现有行程
                    val existing = ItineraryStore.load(context).toMutableList()
                    existing.addAll(result.items)
                    ItineraryStore.save(context, existing)
                    _items.value = existing
                    Log.d(TAG, "planByAi: saved ${result.items.size} items, total now ${existing.size}")
                } else {
                    Log.w(TAG, "planByAi: DeepSeek returned empty items")
                }

                _aiSummary.value = result.summary ?: "行程已生成"
                _loading.value = false

                // 通知其他页面行程已更新
                AppEventBus.emit(AppEventBus.ItineraryChanged)
            } catch (e: Exception) {
                Log.e(TAG, "AI plan failed: ${e.message}", e)
                _error.value = "AI 规划失败：${e.message}"
                _loading.value = false
            }
        }
    }

    /**
     * 手动添加行程项
     */
    fun addItem(item: ItineraryItem) {
        ItineraryStore.addItem(context, item)
        loadFromStore()
    }

    /**
     * 删除行程项
     */
    fun removeItem(itemId: String) {
        ItineraryStore.removeItem(context, itemId)
        loadFromStore()
    }

    fun clearError() {
        _error.value = null
    }

    fun clearAiSummary() {
        _aiSummary.value = null
    }
}