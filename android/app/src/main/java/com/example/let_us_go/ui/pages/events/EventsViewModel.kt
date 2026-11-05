package com.example.let_us_go.ui.pages.events

import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.Event
import com.example.let_us_go.data.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventsViewModel : BaseViewModel() {

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    // 分页状态
    private var currentPage = 1
    private val pageSize = 10

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    companion object {
        const val SCENIC_ID = "lingshan"
    }

    /** 首次加载（重置分页） */
    fun load() {
        currentPage = 1
        launchIo(showLoading = true) {
            val res = NetworkModule.guideApi.events(
                scenicId = SCENIC_ID,
                page = currentPage,
                pageSize = pageSize
            )
            _events.value = res.items
            _hasMore.value = res.items.size >= pageSize
        }
    }

    /** 加载更多（分页追加） */
    fun loadMore() {
        if (_loadingMore.value || !_hasMore.value) return
        _loadingMore.value = true
        launchIo(showLoading = false) {
            try {
                currentPage++
                val res = NetworkModule.guideApi.events(
                    scenicId = SCENIC_ID,
                    page = currentPage,
                    pageSize = pageSize
                )
                if (res.items.isNotEmpty()) {
                    _events.value = _events.value + res.items
                    _hasMore.value = res.items.size >= pageSize
                } else {
                    currentPage-- // 回滚
                    _hasMore.value = false
                }
            } finally {
                _loadingMore.value = false
            }
        }
    }

    fun refresh() = load()
}
