package com.example.let_us_go.ui.pages.search

import androidx.lifecycle.viewModelScope
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.ScenicSpot
import com.example.let_us_go.data.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 搜索 ViewModel（Phase 2 重构）
 *
 * - 真实搜索：调 guideApi.hotSpots + 关键词过滤（替代 mock）
 * - 历史记录：内存保存（Phase 3 接入 DataStore）
 */
class SearchViewModel : BaseViewModel() {

    private val _history = MutableStateFlow(listOf("西湖", "黄山", "美食推荐"))
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _results = MutableStateFlow<List<ScenicSpot>>(emptyList())
    val results: StateFlow<List<ScenicSpot>> = _results.asStateFlow()

    /**
     * 搜索：调后端热门景点 + 本地关键词过滤
     */
    fun search(keyword: String) {
        if (keyword.isBlank()) return
        if (!_history.value.contains(keyword)) {
            _history.value = listOf(keyword) + _history.value.take(9)
        }
        viewModelScope.launch {
            val res = NetworkModule.guideApi.hotSpots(scenicId = "lingshan", page = 1, pageSize = 50)
            val filtered = res.items.filter { spot ->
                val name = spot.name ?: spot.title ?: ""
                val summary = spot.summary ?: ""
                name.contains(keyword, ignoreCase = true) ||
                    summary.contains(keyword, ignoreCase = true) ||
                    spot.tags.any { it.contains(keyword, ignoreCase = true) }
            }
            _results.value = if (filtered.isEmpty()) res.items.take(10) else filtered
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }
}