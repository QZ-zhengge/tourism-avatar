package com.example.let_us_go.ui.pages.nearby

import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.NearbyShop
import com.example.let_us_go.data.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyViewModel : BaseViewModel() {

    private val _shops = MutableStateFlow<List<NearbyShop>>(emptyList())
    val shops: StateFlow<List<NearbyShop>> = _shops.asStateFlow()

    private var currentPage = 1
    private val pageSize = 10

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    companion object {
        const val SCENIC_ID = "lingshan"
    }

    fun load() {
        currentPage = 1
        launchIo(showLoading = true) {
            val res = NetworkModule.guideApi.nearbyShops(
                scenicId = SCENIC_ID,
                page = currentPage,
                pageSize = pageSize
            )
            _shops.value = res.items
            _hasMore.value = res.items.size >= pageSize
        }
    }

    fun loadMore() {
        if (_loadingMore.value || !_hasMore.value) return
        _loadingMore.value = true
        launchIo(showLoading = false) {
            try {
                currentPage++
                val res = NetworkModule.guideApi.nearbyShops(
                    scenicId = SCENIC_ID,
                    page = currentPage,
                    pageSize = pageSize
                )
                if (res.items.isNotEmpty()) {
                    _shops.value = _shops.value + res.items
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

    fun refresh() = load()
}
