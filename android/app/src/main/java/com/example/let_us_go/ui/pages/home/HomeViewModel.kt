package com.example.let_us_go.ui.pages.home

import androidx.lifecycle.viewModelScope
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.Post
import com.example.let_us_go.data.model.ScenicSpot
import com.example.let_us_go.data.model.WeatherCurrent
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.SpotImageCache
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.StateFlow
import com.example.let_us_go.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 * 完整复刻 uni-app pages/home 加载逻辑：
 *  - 天气（/weather/current）
 *  - 旅友圈精选（/circle/posts/）
 *  - 热门景点（/api/v1/guide/hot-spots）→ 横幅图
 */
class HomeViewModel : BaseViewModel() {

    private val _weather = MutableStateFlow<WeatherCurrent?>(null)
    val weather: StateFlow<WeatherCurrent?> = _weather.asStateFlow()

    private val _banners = MutableStateFlow<List<String>>(emptyList())
    val banners: StateFlow<List<String>> = _banners.asStateFlow()

    /**
     * 首页轮播图（本地资源，按 docs/1.jpg → 2.jpg → 3.jpg 顺序）。
     * 图片已按轮播框比例居中裁剪（上下等量、保留中间）。
     */
    val bannerImages: List<Int> = listOf(
        R.drawable.banner_3,
        R.drawable.banner_1,
        R.drawable.banner_2
    )

    /** 景点图片缓存（来自管理端），供 HomeScreen 横向卡片使用 */
    val spotImages: StateFlow<Map<String, List<String>>> = SpotImageCache.images

    private val _posts = MutableStateFlow<List<HomePost>>(emptyList())
    val posts: StateFlow<List<HomePost>> = _posts.asStateFlow()

    private val _hotSpots = MutableStateFlow<List<ScenicSpot>>(emptyList())
    val hotSpots: StateFlow<List<ScenicSpot>> = _hotSpots.asStateFlow()

    init {
        // 默认兜底数据（uni 端也有，渐隐体验与 uni 端一致）
        _posts.value = listOf(
            HomePost("p1", "朝阳公园的日落真的好看", "湖边风很舒服，傍晚拍照特别出片，适合和朋友慢慢逛。", ""),
            HomePost("p2", "景区夜游氛围感拉满", "灯光亮起来以后特别梦幻，推荐穿浅色衣服拍照。", ""),
            HomePost("p3", "周末适合带家人来走走", "路线不累，附近吃饭也方便，整体体验很轻松。", "")
        )
    }

    fun loadAll() {
        loadWeather("无锡")
        loadHotSpots()
        loadPosts()
        // 每次进入首页时强制刷新景点图片缓存
        SpotImageCache.forceRefresh()
    }

    fun loadWeather(city: String) {
        launchIo(showLoading = false, onError = {}) {
            val res = NetworkModule.weatherApi.current(city = city)
            if (res.isSuccess && res.data != null && res.data.isNotEmpty()) {
                _weather.value = res.data.first()
            }
        }
    }

    fun loadHotSpots() {
        launchIo(showLoading = false, onError = {}) {
            val res = NetworkModule.guideApi.hotSpots(scenicId = "lingshan", page = 1, pageSize = 3)
            _hotSpots.value = res.items
            _banners.value = res.items.mapNotNull { it.image ?: it.coverImage }.take(3)
        }
    }

    fun loadPosts() {
        launchIo(showLoading = false, onError = {}) {
            val userId = UserPrefs.userId() ?: 0L
            val res = NetworkModule.circleApi.listPosts(page = 1, pageSize = 3, currentUserId = userId)
            if (res.isSuccess && res.data != null) {
                _posts.value = res.data.map { p ->
                    HomePost(
                        id = p.id.toString(),
                        title = p.locationName ?: p.nickname ?: "旅友分享",
                        excerpt = p.content.take(50),
                        image = p.images.firstOrNull()?.url ?: ""
                    )
                }
            }
        }
    }
}
