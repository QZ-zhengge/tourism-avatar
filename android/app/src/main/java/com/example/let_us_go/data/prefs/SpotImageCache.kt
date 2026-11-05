package com.example.let_us_go.data.prefs

import android.util.Log
import com.example.let_us_go.data.api.AdminApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 景点图片缓存（全局单例）
 *
 * 首页进入时由 HomeViewModel 触发刷新，
 * 热门景点页直接读取，避免重复请求。
 */
object SpotImageCache {

    private const val TAG = "SpotImageCache"

    private val _images = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val images: StateFlow<Map<String, List<String>>> = _images.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** 刷新缓存（首页 onEnter 调用） */
    fun refresh(scenicId: String = "lingshan") {
        if (_loading.value) return
        _loading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val map = AdminApi.fetchSpotImages(scenicId)
                withContext(Dispatchers.Main) {
                    _images.value = map
                    _loading.value = false
                    Log.d(TAG, "Cache refreshed: ${map.size} spots")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Refresh failed: ${e.message}")
                _loading.value = false
            }
        }
    }

    /** 强制重新拉取（绕过 loading 锁） */
    fun forceRefresh(scenicId: String = "lingshan") {
        _loading.value = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val map = AdminApi.fetchSpotImages(scenicId)
                withContext(Dispatchers.Main) {
                    _images.value = map
                    _loading.value = false
                    Log.d(TAG, "Force refresh: ${map.size} spots")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Force refresh failed: ${e.message}")
                _loading.value = false
            }
        }
    }

    /** 查找某景点的图片 */
    fun getImage(spotName: String?): String? {
        if (spotName.isNullOrBlank()) return null
        val imgs = _images.value[spotName]
        return imgs?.firstOrNull()
    }
}