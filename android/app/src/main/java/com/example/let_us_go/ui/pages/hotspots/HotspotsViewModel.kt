package com.example.let_us_go.ui.pages.hotspots

import android.util.Log
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.ScenicSpot
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.SpotImageCache
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 热门景点 ViewModel
 */
class HotspotsViewModel : BaseViewModel() {

    private val _spots = MutableStateFlow<List<ScenicSpot>>(emptyList())
    val spots: StateFlow<List<ScenicSpot>> = _spots.asStateFlow()

    private val _selectedSpot = MutableStateFlow<ScenicSpot?>(null)
    val selectedSpot: StateFlow<ScenicSpot?> = _selectedSpot.asStateFlow()

    private val _filter = MutableStateFlow("推荐")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private var currentPage = 1
    private val pageSize = 10

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    /** 景点名 → 图片 URL 列表（来自 SpotImageCache 共享缓存） */
    val spotImages: StateFlow<Map<String, List<String>>> = SpotImageCache.images

    companion object {
        const val SCENIC_ID = "lingshan"
    }

    fun setFilter(value: String) {
        _filter.value = value
        load()
    }

    fun load() {
        currentPage = 1
        // 兜底：如果缓存为空（可能没有经过首页），强制刷新
        if (SpotImageCache.images.value.isEmpty()) {
            SpotImageCache.refresh()
        }
        launchIo(showLoading = true) {
            val res = NetworkModule.guideApi.hotSpots(
                scenicId = SCENIC_ID,
                page = currentPage,
                pageSize = pageSize
            )
            _spots.value = res.items
            _hasMore.value = res.items.size >= pageSize
        }
    }

    /** 查找某个景点的图片 URL */
    fun getImageFor(spot: ScenicSpot): String? {
        return SpotImageCache.getImage(spot.name ?: spot.title)
    }

    fun loadMore() {
        if (_loadingMore.value || !_hasMore.value) return
        _loadingMore.value = true
        launchIo(showLoading = false) {
            try {
                currentPage++
                val res = NetworkModule.guideApi.hotSpots(
                    scenicId = SCENIC_ID,
                    page = currentPage,
                    pageSize = pageSize
                )
                if (res.items.isNotEmpty()) {
                    _spots.value = _spots.value + res.items
                    _hasMore.value = res.items.size >= pageSize
                } else {
                    currentPage--
                    _hasMore.value = false
                }
            } finally {
                _loadingMore.value = false
            }
        }
    }

    fun selectSpot(spot: ScenicSpot) { _selectedSpot.value = spot }
    fun clearSelectedSpot() { _selectedSpot.value = null }
    fun refresh() = load()
}
