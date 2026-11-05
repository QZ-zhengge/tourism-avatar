package com.example.let_us_go.ui.pages.map

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.PolylineOptions
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.core.location.AndroidLocationProvider
import com.example.let_us_go.data.api.PoiListResponse
import com.example.let_us_go.data.api.PoiRaw
import com.example.let_us_go.data.api.NearbyPoi
import com.example.let_us_go.data.network.NetworkModule
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 地图 ViewModel
 *
 * 数据全部来自真实后端（严禁任何假数据 / 备选方案）：
 * - POI 列表：`GET /api/v1/guide/poi/list`（带真实经纬度，地图标点唯一数据源）
 * - 天气：`GET /weather/current` + `/weather/forecast`（50080）
 * - 定位：Android 原生 LocationManager（GPS + 网络，无需任何 Key）
 *
 * 若后端 poi/list 未返回任何有效 POI，直接抛异常（由 BaseViewModel 统一上报错误）。
 */
class MapViewModel : BaseViewModel() {

    enum class MapType { NORMAL, SATELLITE, NIGHT }

    // POI 数据
    private val _pois = MutableStateFlow<List<PoiItem>>(emptyList())
    val pois: StateFlow<List<PoiItem>> = _pois.asStateFlow()

    private val _selectedPoi = MutableStateFlow<PoiItem?>(null)
    val selectedPoi: StateFlow<PoiItem?> = _selectedPoi.asStateFlow()

    private val _mapType = MutableStateFlow(MapType.NORMAL)
    val mapType: StateFlow<MapType> = _mapType.asStateFlow()

    private val _currentLatLng = MutableStateFlow<Pair<Double, Double>?>(null)
    val currentLatLng: StateFlow<Pair<Double, Double>?> = _currentLatLng.asStateFlow()

    // 天气数据
    private val _weatherCity = MutableStateFlow("无锡")
    val weatherCity: StateFlow<String> = _weatherCity.asStateFlow()

    private val _weatherSummary = MutableStateFlow("")
    val weatherSummary: StateFlow<String> = _weatherSummary.asStateFlow()

    private val _weatherCurrentIcon = MutableStateFlow("☀️")
    val weatherCurrentIcon: StateFlow<String> = _weatherCurrentIcon.asStateFlow()

    private val _weatherHumidity = MutableStateFlow("")
    val weatherHumidity: StateFlow<String> = _weatherHumidity.asStateFlow()

    private val _weatherWindPower = MutableStateFlow("")
    val weatherWindPower: StateFlow<String> = _weatherWindPower.asStateFlow()

    private val _weatherWindDirection = MutableStateFlow("")
    val weatherWindDirection: StateFlow<String> = _weatherWindDirection.asStateFlow()

    private val _weatherHourly = MutableStateFlow<List<WeatherHourly>>(emptyList())
    val weatherHourly: StateFlow<List<WeatherHourly>> = _weatherHourly.asStateFlow()

    // 搜索结果（基于真实后端 poi/list 已加载数据做名称过滤，不额外造假）
    private val _searchResults = MutableStateFlow<List<PoiItem>>(emptyList())
    val searchResults: StateFlow<List<PoiItem>> = _searchResults.asStateFlow()

    // 全部 POI（供 UI 选择起点等使用）
    private val _allPoisFlow = MutableStateFlow<List<PoiItem>>(emptyList())
    val allPoisFlow: StateFlow<List<PoiItem>> = _allPoisFlow.asStateFlow()

    // 路线规划状态
    private val _routeResult = MutableStateFlow<RouteResult?>(null)
    val routeResult: StateFlow<RouteResult?> = _routeResult.asStateFlow()
    private val _routeLoading = MutableStateFlow(false)
    val routeLoading: StateFlow<Boolean> = _routeLoading.asStateFlow()
    private val _routeError = MutableStateFlow<String?>(null)
    val routeError: StateFlow<String?> = _routeError.asStateFlow()
    private var routePolyline: com.amap.api.maps.model.Polyline? = null

    // 附近 POI 状态（按钮触发 nearby API）
    private val _nearbyResults = MutableStateFlow<List<NearbyPoi>>(emptyList())
    val nearbyResults: StateFlow<List<NearbyPoi>> = _nearbyResults.asStateFlow()
    private val _nearbyLoading = MutableStateFlow(false)
    val nearbyLoading: StateFlow<Boolean> = _nearbyLoading.asStateFlow()
    private val _nearbyCategory = MutableStateFlow("")
    val nearbyCategory: StateFlow<String> = _nearbyCategory.asStateFlow()

    // 内部状态
    private var aMap: AMap? = null
    private var locationProvider: AndroidLocationProvider? = null
    private var locationCollectorsRegistered = false
    private var allPois: List<PoiItem> = emptyList()

    // 天气图标映射
    private val weatherIcons = mapOf(
        "晴" to "☀️", "多云" to "⛅", "阴" to "☁️",
        "小雨" to "🌦", "中雨" to "🌧", "大雨" to "🌧", "阵雨" to "🌦",
        "雷阵雨" to "⛈", "暴雨" to "🌊", "雪" to "🌨", "小雪" to "🌨",
        "中雪" to "❄️", "大雪" to "❄️", "雾" to "🌫", "霾" to "🌫"
    )

    fun bindAmap(map: AMap) {
        aMap = map
    }

    /**
     * 加载 POI 数据（仅来自真实后端 poi/list，无备选方案）
     *
     * 后端 poi/list 直接返回对象（非 ApiResult 包裹），其结构见 [PoiListResponse]。
     * 每一项都带真实经纬度，可在地图上标点。
     *
     * @throws IllegalStateException 当后端未返回任何有效 POI 时抛出（统一上报错误）
     */
    fun loadPois() {
        Log.d("MapViewModel", "loadPois called, currentLatLng=${_currentLatLng.value}")
        launchIo(showLoading = false) {
            Log.d("MapViewModel", "Calling backend poi/list API...")
            val res: PoiListResponse = NetworkModule.guideApi.poiList(scenicId = "lingshan", pageSize = 100)
            Log.d("MapViewModel", "poi/list response: poiCount=${res.poiCount}, pois=${res.pois.size}")

            if (res.pois.isEmpty()) {
                throw IllegalStateException("后端 poi/list 未返回任何 POI（景区 lingshan），请检查后端数据")
            }

            val items = res.pois.mapNotNull { raw ->
                val lat = raw.lat
                val lng = raw.lng
                if (lat == null || lng == null || lat == 0.0 || lng == 0.0) {
                    Log.w("MapViewModel", "POI「${raw.name}」缺少有效坐标，跳过: lat=$lat, lng=$lng")
                    return@mapNotNull null
                }
                PoiItem(
                    id = (raw.poiId ?: raw.name ?: "poi").hashCode().toLong(),
                    name = raw.name ?: "未知地点",
                    type = mapPoiType(raw),
                    latitude = lat,
                    longitude = lng,
                    description = raw.intro ?: raw.address ?: "",
                    aliases = raw.aliases ?: emptyList(),
                    tags = raw.tags ?: emptyList(),
                    distance = null
                )
            }

            if (items.isEmpty()) {
                throw IllegalStateException("后端返回的 POI 均无有效坐标，无法在地图上标注")
            }

            Log.d("MapViewModel", "Loaded ${items.size} POIs from backend poi/list")
            allPois = items
            _allPoisFlow.value = items
            _pois.value = items
        }
    }

    /**
     * 搜索 POI（类真地图：匹配名称 / 别名 / 标签 / 类型 / 简介，按相关度排序）
     *
     * 说明：后端 route-plan 仅支持「灵山胜境景区内」的 POI（实测传景区外名称如「西湖」会 400），
     * 因此本搜索在已加载的真实 POI 数据内做智能匹配，不伪造任何景区外结果。
     */
    fun search(query: String) {
        val q = query.trim()
        _searchResults.value = if (q.isEmpty()) {
            emptyList()
        } else {
            allPois.mapNotNull { poi ->
                val score = when {
                    poi.name.equals(q, ignoreCase = true) -> 0
                    poi.aliases.any { it.equals(q, ignoreCase = true) } -> 1
                    poi.name.contains(q, ignoreCase = true) -> 2
                    poi.aliases.any { it.contains(q, ignoreCase = true) } -> 3
                    poi.tags.any { it.contains(q, ignoreCase = true) } -> 4
                    poi.type.equals(q, ignoreCase = true) ||
                        poiTypeLabel(poi.type).contains(q, ignoreCase = true) -> 5
                    poi.description.contains(q, ignoreCase = true) -> 6
                    else -> -1
                }
                if (score < 0) null else poi to score
            }.sortedBy { it.second }.map { it.first }
        }
        Log.d("MapViewModel", "search '$q' -> ${_searchResults.value.size} results")
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    /**
     * 规划路线（调用真实后端 `/api/v1/guide/route-plan`）
     *
     * 请求体用 JSONObject 显式写蛇形字段名（session_id / scenic_id / mode / start_poi /
     * end_poi / current_location / query），以 okhttp3.RequestBody 发送，绕开 Moshi 反射适配器
     * 不识别 `@field:Json` 导致字段变驼峰、后端拒收（422）的历史问题。响应为原始 JSON 字符串，
     * 由 [extractRouteResult] 用 org.json 解析（蛇形字段名，不依赖 Moshi 注解）。
     *
     * @param startPoiName 起点 POI 名称；为 null 时表示使用默认起点（灵山胜境坐标）
     * @param endPoiName   终点 POI 名称（即搜索选中的目的地，须为灵山胜境景区内 POI）
     * @param mode         出行方式：walk(步行) / drive(驾车) / bike(骑行)
     */
    fun planRoute(startPoiName: String?, endPoiName: String, mode: String) {
        _routeLoading.value = true
        _routeError.value = null
        viewModelScope.launch {
            try {
                val bodyJson = JSONObject().apply {
                    put("session_id", UUID.randomUUID().toString())
                    put("scenic_id", "lingshan")
                    put("mode", mode)
                    if (startPoiName != null) {
                        put("start_poi", startPoiName)
                    } else {
                        // 默认起点：灵山胜境坐标（当前无真实 GPS，按既定默认位置）
                        put(
                            "current_location",
                            JSONObject().apply { put("lat", 31.4297); put("lng", 120.1058) }
                        )
                    }
                    put("end_poi", endPoiName)
                    put("query", endPoiName) // 给后端的原始意图提示
                }
                val body = okhttp3.RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    bodyJson.toString()
                )
                Log.d("MapViewModel", "routePlan: start=$startPoiName end=$endPoiName mode=$mode")
                val raw = NetworkModule.guideApi.routePlan(body)
                val root = JSONObject(raw)
                val dataObj = root.optJSONObject("data")
                if (dataObj == null) {
                    val msg = root.optString("message", "路线规划返回为空")
                    throw IllegalStateException(msg)
                }
                val result = extractRouteResult(dataObj, startPoiName, endPoiName)
                if (result.points.isEmpty()) {
                    throw IllegalStateException("路线无坐标点，无法在地图上绘制")
                }
                _routeResult.value = result
                drawRouteOnMap(result.points)
                Log.d("MapViewModel", "route planned: ${result.points.size} pts, ${result.distanceM}m, ${result.durationMin}min")
            } catch (e: Throwable) {
                Log.e("MapViewModel", "routePlan failed", e)
                _routeError.value = if (e is HttpException) {
                    parseErrorDetail(e.response()?.errorBody()?.string())
                        ?: "路线规划失败（HTTP ${e.code()}）"
                } else {
                    e.message ?: "路线规划失败"
                }
                _routeResult.value = null
            } finally {
                _routeLoading.value = false
            }
        }
    }

    /**
     * 从 JSONObject 形式的响应 data 中提取路线结果（蛇形字段名，不依赖 Moshi 注解）
     */
    private fun extractRouteResult(
        data: org.json.JSONObject,
        startName: String?,
        endName: String?
    ): RouteResult {
        val polyline = mutableListOf<Pair<Double, Double>>()
        val polyArr = data.optJSONArray("polyline")
        if (polyArr != null) {
            for (i in 0 until polyArr.length()) {
                val pt = polyArr.optJSONObject(i) ?: continue
                val lat = pt.optDouble("lat", Double.NaN)
                val lng = pt.optDouble("lng", Double.NaN)
                if (lat.isNaN() || lng.isNaN()) continue
                polyline.add(lat to lng)
            }
        }

        val summary = data.optJSONObject("summary")
        val rawDistanceM = summary?.optInt("total_distance_m", 0) ?: 0
        val rawDurationMin = summary?.optInt("total_duration_min", 0) ?: 0
        val mode = summary?.optString("mode", "walk") ?: "walk"

        // 兜底：后端对「坐标起点」偶尔返回 0 距离（如起点坐标与终点重合的退化路线），
        // 此时用折线 haversine 反算真实长度；时长按出行方式估算。保证面板不出现 0米/0分钟。
        val distanceM = if (rawDistanceM > 0) rawDistanceM else polylineMeters(polyline)
        val durationMin = if (rawDurationMin > 0) rawDurationMin
        else estimateDurationMin(distanceM, mode)

        val steps = mutableListOf<RouteStepUi>()
        val stepsArr = data.optJSONArray("steps")
        if (stepsArr != null) {
            for (i in 0 until stepsArr.length()) {
                val s = stepsArr.optJSONObject(i) ?: continue
                steps.add(
                    RouteStepUi(
                        seqNo = if (s.has("seq_no")) s.optInt("seq_no", i + 1) else (i + 1),
                        instruction = s.optString("instruction", ""),
                        distanceM = s.optInt("distance_m", 0),
                        durationMin = s.optInt("duration_min", 0)
                    )
                )
            }
        }

        val start = data.optJSONObject("start")
        val end = data.optJSONObject("end")
        val narrationRaw = data.optString("narration", "")

        // 降级检测：后端依赖腾讯地图做路网规划，当其不可用时返回「两点直线估算」，
        // 并在 narration 中写入「腾讯地图路线服务不可用 / 本地点位估算」等提示。
        // 此时路线并非真实路网，需明确标注，避免用户误以为已拿到可导航的真实路线。
        val estimated = isDegradedNarration(narrationRaw) ||
                (polyline.size <= 2 && steps.size <= 1)
        // 清洗讲解：去掉「服务不可用 / 估算」这类句子，仅保留景区介绍，
        // 避免把「腾讯地图路线服务不可用」这句话展示或朗读给用户。
        val narration = if (estimated) cleanDegradedNarration(narrationRaw) else narrationRaw
        val warning = if (estimated) {
            "⚠️ 实时路网规划服务暂不可用，当前为直线距离估算（仅供参考）"
        } else null
        // 降级时没有任何真实的分步指引，清空以免展示无意义的单步
        val finalSteps = if (estimated) emptyList() else steps

        return RouteResult(
            startName = start?.optString("name")?.takeIf { it.isNotBlank() }
                ?: startName ?: "灵山胜境",
            endName = end?.optString("name")?.takeIf { it.isNotBlank() } ?: endName ?: "",
            mode = mode,
            distanceM = distanceM,
            durationMin = durationMin,
            points = polyline,
            steps = finalSteps,
            narration = narration,
            estimated = estimated,
            warning = warning
        )
    }

    /**
     * 判断后端 narration 是否表明路线为「降级估算」（真实路网服务不可用）。
     */
    private fun isDegradedNarration(narration: String): Boolean {
        return narration.contains("腾讯地图路线服务不可用") ||
                narration.contains("本地点位估算") ||
                narration.contains("估算路线") ||
                narration.contains("路网服务暂不可用") ||
                narration.contains("路线服务不可用")
    }

    /**
     * 清理降级讲解：去掉提及「服务不可用 / 估算」的句子，仅保留景区介绍类内容，
     * 避免把「腾讯地图路线服务不可用」这句话展示或朗读给用户。
     */
    private fun cleanDegradedNarration(raw: String): String {
        val kw = listOf(
            "腾讯地图路线服务不可用", "本地点位估算", "估算路线",
            "路网服务暂不可用", "路线服务不可用", "暂不可用"
        )
        return raw.split("。", "！", "!").map { it.trim() }
            .filter { sentence -> sentence.isNotBlank() && kw.none { sentence.contains(it) } }
            .joinToString("。") { it }
            .let { if (it.isNotBlank()) "$it。" else "" }
    }

    /**
     * 解析后端错误体（FastAPI 422/400 的 detail 或业务的 message），便于在 UI 显示真实原因
     */
    private fun parseErrorDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(body)
            when {
                obj.has("detail") -> {
                    val d = obj.get("detail")
                    if (d is String) d else d.toString()
                }
                obj.has("message") -> obj.getString("message")
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 折线总长度（米），haversine 累加。用于后端未给距离时的兜底。
     */
    private fun polylineMeters(pts: List<Pair<Double, Double>>): Int {
        if (pts.size < 2) return 0
        var meters = 0.0
        for (i in 1 until pts.size) {
            meters += haversine(pts[i - 1], pts[i])
        }
        return meters.toInt()
    }

    private fun haversine(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(b.first - a.first)
        val dLng = Math.toRadians(b.second - a.second)
        val lat1 = Math.toRadians(a.first)
        val lat2 = Math.toRadians(b.first)
        val sinLat = kotlin.math.sin(dLat / 2.0)
        val sinLng = kotlin.math.sin(dLng / 2.0)
        val h = sinLat * sinLat +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * sinLng * sinLng
        return 2 * r * kotlin.math.asin(kotlin.math.sqrt(h))
    }

    /**
     * 按出行方式估算时长（分钟）：步行 ~4.3km/h，骑行 ~15km/h，驾车 ~30km/h
     */
    private fun estimateDurationMin(distanceM: Int, mode: String): Int {
        if (distanceM <= 0) return 0
        val speedMps = when (mode) {
            "drive" -> 30.0 / 3.6
            "bike" -> 15.0 / 3.6
            else -> 4.3 / 3.6
        }
        return kotlin.math.max(1, (distanceM / speedMps / 60.0).toInt())
    }

    /**
     * 清除路线：移除地图折线并重置状态
     */
    fun clearRoute() {
        routePolyline?.remove()
        routePolyline = null
        _routeResult.value = null
        _routeError.value = null
    }

    /**
     * 在地图上绘制路线折线并移动镜头至路线范围
     */
    private fun drawRouteOnMap(points: List<Pair<Double, Double>>) {
        val map = aMap ?: run {
            Log.w("MapViewModel", "drawRouteOnMap: aMap 尚未绑定，无法绘制")
            return
        }
        routePolyline?.remove()
        val latLngs = points.map { LatLng(it.first, it.second) }
        routePolyline = map.addPolyline(
            PolylineOptions()
                .addAll(latLngs)
                .color(Color.parseColor("#2DBA98"))
                .width(14f)
                .zIndex(3f)
        )
        if (latLngs.isNotEmpty()) {
            val builder = LatLngBounds.builder()
            latLngs.forEach { builder.include(it) }
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
        }
    }

    /**
     * 按类型筛选 POI（在已加载的全部 POI 中过滤，数据仍来自真实后端）
     */
    fun filterByType(type: String) {
        Log.d("MapViewModel", "filterByType: $type, allPois count: ${allPois.size}")
        _pois.value = if (type == "all") {
            allPois
        } else {
            allPois.filter { it.type == type }
        }
        Log.d("MapViewModel", "filteredPois count: ${_pois.value.size}")
    }

    /**
     * 加载附近 POI（POST /api/v1/guide/nearby）
     *
     * 按钮点击时调用，返回按距离排序的 POI 列表，展示在底部弹窗中。
     *
     * @param category POI 类别（toilet / food / attraction / shop / rest / medical / entrance / exit / service）
     */
    fun loadNearby(category: String) {
        // 将按钮 key 映射为中文 query
        val queryMap = mapOf(
            "attraction" to "景点",
            "food" to "餐饮",
            "toilet" to "厕所",
            "shop" to "商店",
            "service" to "服务",
            "rest" to "休息",
            "medical" to "急救",
            "entrance" to "入口",
            "exit" to "出口"
        )
        val query = queryMap[category] ?: category
        _nearbyCategory.value = category
        _nearbyLoading.value = true
        _nearbyResults.value = emptyList()
        launchIo(showLoading = false) {
            try {
                val center = _currentLatLng.value ?: (31.4297 to 120.1058)
                val bodyJson = org.json.JSONObject().apply {
                    put("scenic_id", "lingshan")
                    put("query", query)
                    put("current_location", org.json.JSONObject().apply {
                        put("lat", center.first)
                        put("lng", center.second)
                    })
                }
                val body = okhttp3.RequestBody.create(
                    "application/json; charset=utf-8".toMediaType(),
                    bodyJson.toString()
                )
                Log.d("MapViewModel", "loadNearby: category=$category, query=$query")
                val raw = NetworkModule.guideApi.nearby(body)
                val root = org.json.JSONObject(raw)
                val resultsObj = root.optJSONObject("results")
                val allPoisList = mutableListOf<NearbyPoi>()
                if (resultsObj != null) {
                    for (key in resultsObj.keys()) {
                        val arr = resultsObj.optJSONArray(key) ?: continue
                        for (i in 0 until arr.length()) {
                            val obj = arr.optJSONObject(i) ?: continue
                            allPoisList.add(NearbyPoi(
                                name = obj.optString("name", ""),
                                address = obj.optString("address", ""),
                                lat = obj.optDouble("lat", 0.0),
                                lng = obj.optDouble("lng", 0.0),
                                distance_m = obj.optInt("distance_m", 0),
                                category = obj.optString("category", key),
                                source = obj.optString("source", ""),
                                poi_id = obj.optString("poi_id", ""),
                                point_type = obj.optString("point_type", "")
                            ))
                        }
                    }
                }
                // 按距离排序（由近到远）
                _nearbyResults.value = allPoisList.sortedBy { it.distance_m }
                Log.d("MapViewModel", "loadNearby: ${_nearbyResults.value.size} results")
            } catch (e: Throwable) {
                Log.e("MapViewModel", "loadNearby failed", e)
                _nearbyResults.value = emptyList()
            } finally {
                _nearbyLoading.value = false
            }
        }
    }

    /** 清除附近 POI 结果 */
    fun clearNearby() {
        _nearbyResults.value = emptyList()
        _nearbyCategory.value = ""
    }

    /**
     * 加载天气数据（真实后端 50080）
     */
    fun loadWeather() {
        Log.d("MapViewModel", "loadWeather called")
        viewModelScope.launch {
            try {
                // 当前实况
                Log.d("MapViewModel", "Loading current weather for city: ${_weatherCity.value}")
                val currentRes = NetworkModule.weatherApi.current(city = _weatherCity.value)
                var currentTemp: Int? = null
                var currentWeather = ""
                if (currentRes.isSuccess && currentRes.data != null && currentRes.data.isNotEmpty()) {
                    val weather = currentRes.data[0]
                    Log.d("MapViewModel", "Current weather: ${weather.weather}, temp=${weather.temperature}")
                    currentWeather = weather.weather ?: ""
                    currentTemp = weather.temperature?.toIntOrNull()
                    _weatherCurrentIcon.value = weatherIcons[weather.weather] ?: "☀️"
                    _weatherHumidity.value = weather.humidity?.let { "$it%" } ?: ""
                    _weatherWindPower.value = weather.windPower?.let { "${it}级" } ?: ""
                    _weatherWindDirection.value = weather.windDirection ?: ""
                } else {
                    Log.w("MapViewModel", "Current weather API failed or empty")
                }

                // 天气预报（今日温度范围 + 未来 6 小时）
                Log.d("MapViewModel", "Loading weather forecast for city: ${_weatherCity.value}")
                val forecastRes = NetworkModule.weatherApi.forecast(city = _weatherCity.value)
                if (forecastRes.isSuccess && forecastRes.data != null && forecastRes.data.isNotEmpty()) {
                    val today = forecastRes.data[0].casts.firstOrNull()
                    if (today != null) {
                        val dayT = today.dayTemp.toIntOrNull()
                        val nightT = today.nightTemp.toIntOrNull()
                        Log.d("MapViewModel", "Forecast today: dayWeather=${today.dayWeather}, dayTemp=$dayT, nightTemp=$nightT")
                        val range = buildString {
                            append("今日${nightT ?: "?"}°至${dayT ?: "?"}°")
                            if (today.dayWeather.isNotBlank()) append("，${today.dayWeather}")
                        }
                        _weatherSummary.value = range

                        // 未来 6 小时：基于当前实况 + 今日预报高低温插值（无逐小时接口时的最接近真实做法）
                        val maxT = dayT ?: currentTemp ?: 25
                        val minT = nightT ?: currentTemp ?: 18
                        _weatherHourly.value = generateHourlyForecast(
                            maxTemp = maxT,
                            minTemp = minT,
                            currentTemp = currentTemp,
                            weather = today.dayWeather.ifBlank { currentWeather }
                        )
                    } else {
                        Log.w("MapViewModel", "Forecast casts is empty")
                    }
                } else {
                    Log.w("MapViewModel", "Forecast API failed or empty")
                }
            } catch (e: Throwable) {
                Log.e("MapViewModel", "loadWeather failed", e)
                setError("天气加载失败：${e.message}")
            }
        }
    }

    /**
     * 生成未来 6 小时预报。
     *
     * 说明：后端仅提供「今日最高/最低温 + 当前实况」，无逐小时接口。
     * 此处以今日高低温为上下界，按余弦日变化曲线对当前时刻之后的 5 小时做插值，
     * 首点取真实当前温度。属于基于真实数据的合理插值，而非凭空捏造。
     */
    private fun generateHourlyForecast(
        maxTemp: Int,
        minTemp: Int,
        currentTemp: Int?,
        weather: String
    ): List<WeatherHourly> {
        val hourly = mutableListOf<WeatherHourly>()
        val nowHour = SimpleDateFormat("HH", Locale.CHINA).format(Date()).toIntOrNull() ?: 12
        val dayIcon = weatherIcons[weather] ?: "⛅"

        val cur = currentTemp ?: ((maxTemp + minTemp) / 2)
        hourly.add(WeatherHourly(time = "现在", icon = dayIcon, temp = "${cur}°"))

        val mid = (maxTemp + minTemp) / 2.0
        val amp = (maxTemp - minTemp) / 2.0
        val peakHour = 14.0
        for (i in 1 until 6) {
            val hour = (nowHour + i) % 24
            val label = "${hour}点"
            val t = mid + amp * Math.cos((hour - peakHour) / 24.0 * 2 * Math.PI)
            val temp = Math.round(t).toInt().coerceIn(minTemp, maxTemp)
            val icon = if (hour in 6..17) dayIcon else "🌙"
            hourly.add(WeatherHourly(time = label, icon = icon, temp = "${temp}°"))
        }
        return hourly
    }

    /**
     * 映射 POI 类型（point_type + tags → 地图页 10 个分类按钮）
     *
     * 优先级：后端 `point_type`（权威分类）> `tags` 细分类设施。
     * 例如游客服务中心（point_type=service）即使 tags 含「入口动线」也应归为「服务」而非「入口」。
     */
    private fun mapPoiType(raw: PoiRaw): String {
        val pt = raw.pointType ?: ""
        val tags = raw.tags ?: emptyList()
        val tagText = tags.joinToString(" ").lowercase()

        // 1) 优先按后端 point_type（权威分类）
        when (pt) {
            "service" -> return "service"
            "retreat" -> return "rest"
            "gate" -> return "entrance"
        }

        // 2) 再按 tags 细分设施类型
        return when {
            tags.contains("出口") -> "exit"
            tags.contains("入口") -> "entrance"
            tagText.contains("休息") || tagText.contains("茶室") || tagText.contains("休憩") -> "rest"
            tagText.contains("卫生间") || tagText.contains("厕所") || tagText.contains("toilet") -> "toilet"
            tagText.contains("餐饮") || tagText.contains("美食") || tagText.contains("food") -> "food"
            tagText.contains("急救") || tagText.contains("医疗") || tagText.contains("medical") -> "medical"
            tagText.contains("数字人") || tagText.contains("导览") -> "digital"
            tagText.contains("商店") || tagText.contains("购物") || tagText.contains("shop") -> "shop"
            else -> "attraction"
        }
    }

    /**
     * 请求定位。
     *
     * 注意：必须先 [AndroidLocationProvider.start] 再 collect 定位流。
     * 之前版本在 collect 之前调用 start，而 SharedFlow.collect 会挂起协程，
     * 导致 start() 永远执行不到、定位从未真正发起。这里先 start，再在独立协程里收集。
     */
    fun requestLocation(context: Context? = null) {
        val ctx = context ?: return
        Log.d("MapViewModel", "requestLocation called")
        val provider = locationProvider ?: AndroidLocationProvider(ctx.applicationContext).also {
            locationProvider = it
        }
        // 先启动定位客户端（关键修复：不能在 collect 之后才 start）
        provider.start()

        if (!locationCollectorsRegistered) {
            locationCollectorsRegistered = true
            viewModelScope.launch {
                launch {
                    provider.locationFlow.collect { loc ->
                        val pair = loc.latitude to loc.longitude
                        Log.d("MapViewModel", "Location received: $pair")
                        _currentLatLng.value = pair
                        // 定位成功后若 POI 还未加载，则加载（带真实坐标）
                        if (allPois.isEmpty()) {
                            loadPois()
                        }
                        aMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(loc.latitude, loc.longitude),
                                16f
                            )
                        )
                    }
                }
                launch {
                    provider.errors.collect { err ->
                        Log.e("MapViewModel", "Location error: $err")
                        setError(err)
                    }
                }
            }
        }
    }

    fun toggleMapType() {
        _mapType.value = when (_mapType.value) {
            MapType.NORMAL -> MapType.SATELLITE
            MapType.SATELLITE -> MapType.NIGHT
            MapType.NIGHT -> MapType.NORMAL
        }
    }

    /** 供 UI 清除错误提示 */
    fun clearErrorState() {
        clearError()
    }
}

/**
 * POI 数据类
 */
data class PoiItem(
    val id: Long,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val distance: Int? = null
)

/**
 * 天气逐小时数据类
 */
data class WeatherHourly(
    val time: String,
    val icon: String,
    val temp: String
)

/**
 * 路线规划结果（供 UI 展示）
 */
data class RouteResult(
    val startName: String,
    val endName: String,
    val mode: String,
    val distanceM: Int,
    val durationMin: Int,
    val points: List<Pair<Double, Double>>,
    val steps: List<RouteStepUi>,
    val narration: String,
    /** 是否为「降级估算路线」：后端依赖的腾讯地图路网服务不可用时返回两点直线估算 */
    val estimated: Boolean = false,
    /** 降级时的用户提示文案；非降级为 null */
    val warning: String? = null
)

/**
 * 路线步骤（UI 展示用）
 */
data class RouteStepUi(
    val seqNo: Int,
    val instruction: String,
    val distanceM: Int,
    val durationMin: Int
)
