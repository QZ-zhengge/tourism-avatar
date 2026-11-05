@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.let_us_go.ui.pages.map

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.api.NearbyPoi
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.switchTab
import com.example.let_us_go.R

/**
 * POI 类型 → 中文标签
 */
private val POI_TYPE_LABELS = mapOf(
    "attraction" to "景点", "food" to "餐饮", "toilet" to "卫生间",
    "shop" to "商店", "service" to "服务", "digital" to "数字人",
    "rest" to "休息", "medical" to "急救", "entrance" to "入口", "exit" to "出口"
)

fun poiTypeLabel(type: String): String = POI_TYPE_LABELS[type] ?: "地点"

/**
 * 出行方式 → 中文标签
 */
private val MODE_LABELS = mapOf("walk" to "步行", "drive" to "驾车", "bike" to "骑行")

/**
 * 路线语音讲解播放器（封装 Android 原生 TextToSpeech，无需任何第三方 Key）
 *
 * 用于把后端返回的 `narration` 讲解文本朗读出来，满足「语音讲解」需求。
 */
class TtsController(context: Context) {
    var isSpeaking by mutableStateOf(false)
        private set

    private val tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.CHINESE
                tts.setSpeechRate(1.0f)
            }
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "route_narration")
        isSpeaking = true
    }

    fun stop() {
        tts.stop()
        isSpeaking = false
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

/**
 * 生成路线语音讲解文本：
 * - 降级估算路线：朗读规范的估算播报（不念「服务不可用」原话），并附上清洗后的景区介绍；
 * - 正常路线：朗读后端 narration。
 * 无内容时返回 null（UI 据此隐藏语音按钮）。
 */
private fun routeTtsText(r: RouteResult): String? {
    return if (r.estimated) {
        "已为您估算从${r.startName}到${r.endName}的路线，约${r.distanceM}米，需要${r.durationMin}分钟。" +
                "温馨提示：实时路网服务暂不可用，当前为直线距离估算，仅供参考。" +
                if (r.narration.isNotBlank()) r.narration else ""
    } else {
        r.narration.takeIf { it.isNotBlank() }
    }
}

/**
 * 地图页（严格对齐 UniApp UI 结构）
 *
 * 布局（从上到下）：
 * 1. 地图区域（约 40%）
 * 2. 搜索栏（可展开 → 实时结果列表）
 * 3. 10 个功能按钮（2 行 × 5 列）
 * 4. 天气卡片
 *
 * 搜索到目的地后，弹出路线规划面板，可选出行方式（步行/驾车/骑行），
 * 调用真实后端 `/api/v1/guide/route-plan` 并在地图上绘制路线折线。
 */
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val nav = LocalNav.current
    val mapType by viewModel.mapType.collectAsState()
    val currentLatLng by viewModel.currentLatLng.collectAsState()
    val pois by viewModel.pois.collectAsState()
    val error by viewModel.error.collectAsState()

    // 语音讲解播放器（生命周期随页面销毁而关闭）
    val tts = remember { TtsController(context) }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }

    // 天气数据
    val weatherCity by viewModel.weatherCity.collectAsState()
    val weatherSummary by viewModel.weatherSummary.collectAsState()
    val weatherCurrentIcon by viewModel.weatherCurrentIcon.collectAsState()
    val weatherHumidity by viewModel.weatherHumidity.collectAsState()
    val weatherWindPower by viewModel.weatherWindPower.collectAsState()
    val weatherWindDirection by viewModel.weatherWindDirection.collectAsState()
    val weatherHourly by viewModel.weatherHourly.collectAsState()

    // 搜索 / 路线状态
    val searchResults by viewModel.searchResults.collectAsState()
    val allPois by viewModel.allPoisFlow.collectAsState()
    val routeResult by viewModel.routeResult.collectAsState()
    val routeLoading by viewModel.routeLoading.collectAsState()
    val routeError by viewModel.routeError.collectAsState()

    // 附近 POI 状态
    val nearbyResults by viewModel.nearbyResults.collectAsState()
    val nearbyLoading by viewModel.nearbyLoading.collectAsState()
    val nearbyCategory by viewModel.nearbyCategory.collectAsState()

    // 路线规划成功：自动语音讲解一次（「语音讲解」需求）。
    // 若为降级估算路线，则朗读规范的估算播报，避免把「腾讯地图服务不可用」念出来。
    LaunchedEffect(routeResult) {
        routeResult?.let { r ->
            val text = routeTtsText(r)
            if (text != null) tts.speak(text)
        }
    }

    // 本地 UI 状态
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var routeSheetOpen by remember { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<PoiItem?>(null) }
    var startSelection by remember { mutableStateOf<String?>(null) } // null = 灵山胜境默认起点
    var selectedMode by remember { mutableStateOf("walk") }

    // 初始化
    LaunchedEffect(Unit) {
        try {
            MapsInitializer.updatePrivacyShow(context, true, true)
            MapsInitializer.updatePrivacyAgree(context, true)
        } catch (_: Throwable) { /* ignore */ }
        // 默认位置即灵山胜境（AmapMapView factory 中已 moveCamera 到该坐标）
        viewModel.loadPois()
        viewModel.loadWeather()
    }

    // 10 个功能按钮配置
    val quickTools = listOf(
        QuickTool("attraction", "景点", "#FF6B6B"),
        QuickTool("food", "餐饮", "#FF9F43"),
        QuickTool("toilet", "卫生间", "#54A0FF"),
        QuickTool("shop", "商店", "#A855F7"),
        QuickTool("service", "服务", "#0ABDE3"),
        QuickTool("digital", "数字人", "#2ECC71"),
        QuickTool("rest", "休息", "#64748B"),
        QuickTool("medical", "急救", "#EF4444"),
        QuickTool("entrance", "入口", "#10AC84"),
        QuickTool("exit", "出口", "#EE5A24"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // 错误提示横幅（后端无数据等，由 ViewModel 抛出的异常统一上报）
        if (error != null) {
            ErrorBanner(message = error!!, onDismiss = { viewModel.clearErrorState() })
        }

        // 1. 地图区域（约 40%）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        ) {
            AmapMapView(
                mapType = mapType,
                currentLatLng = currentLatLng,
                pois = pois,
                onMapReady = { aMap -> viewModel.bindAmap(aMap) }
            )
        }

        // 2. 下方内容区域（搜索栏 + 按钮 + 天气）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f)
                .verticalScroll(rememberScrollState())
                .background(BgPage)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 搜索栏（可展开）
            if (isSearching) {
                SearchInput(
                    text = searchText,
                    onTextChange = { value ->
                        searchText = value
                        viewModel.search(value)
                    },
                    onClose = {
                        isSearching = false
                        searchText = ""
                        viewModel.clearSearch()
                    }
                )
                // 搜索结果列表（真实后端数据按名称过滤）
                SearchResultsList(
                    results = searchResults,
                    onItemClick = { poi ->
                        selectedDestination = poi
                        startSelection = null
                        selectedMode = "walk"
                        isSearching = false
                        searchText = ""
                        viewModel.clearSearch()
                        routeSheetOpen = true
                        Log.d("MapScreen", "Search result selected -> route: ${poi.name}")
                    }
                )
            } else {
                SearchBar(onClick = {
                    Log.d("MapScreen", "SearchBar clicked -> expand")
                    isSearching = true
                })
            }

            // 10 个功能按钮
            QuickToolGrid(
                tools = quickTools,
                onToolClick = { key ->
                    if (key == "digital") {
                        // 数字人按钮 → 跳转数字人页（与底部栏中间凸起按钮一致）
                        nav.switchTab(Route.DIGITAL_HUMAN)
                    } else {
                        Log.d("MapScreen", "QuickTool clicked: $key -> loadNearby")
                        viewModel.loadNearby(key)
                    }
                }
            )

            // 分类筛选后无数据提示（真实数据为空时，不做任何伪造）
            if (error == null && pois.isEmpty()) {
                EmptyHint(text = "该分类暂无数据")
            }

            // 天气卡片
            WeatherCard(
                city = weatherCity,
                summary = weatherSummary,
                currentIcon = weatherCurrentIcon,
                humidity = weatherHumidity,
                windPower = weatherWindPower,
                windDirection = weatherWindDirection,
                hourly = weatherHourly
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 路线规划底部面板
    if (routeSheetOpen && selectedDestination != null) {
        RoutePlanningSheet(
            destination = selectedDestination!!,
            allPois = allPois,
            startSelection = startSelection,
            onStartChange = { startSelection = it },
            mode = selectedMode,
            onModeChange = { selectedMode = it },
            routeResult = routeResult,
            routeLoading = routeLoading,
            routeError = routeError,
            onPlan = {
                viewModel.planRoute(startSelection, selectedDestination!!.name, selectedMode)
            },
            onClear = {
                viewModel.clearRoute()
                routeSheetOpen = false
            },
            onDismiss = { routeSheetOpen = false },
            tts = tts
        )
    }

    // 附近 POI 列表底部面板
    if (nearbyResults.isNotEmpty() || nearbyLoading) {
        NearbyPoiSheet(
            category = nearbyCategory,
            results = nearbyResults,
            loading = nearbyLoading,
            onPoiClick = { poi ->
                // 点击 POI → 打开路线规划面板
                viewModel.clearNearby()
                selectedDestination = PoiItem(
                    id = poi.poi_id.hashCode().toLong(),
                    name = poi.name,
                    type = poi.category,
                    latitude = poi.lat,
                    longitude = poi.lng,
                    description = poi.address
                )
                routeSheetOpen = true
            },
            onDismiss = { viewModel.clearNearby() }
        )
    }
}

/**
 * 搜索栏（折叠态）
 */
@Composable
private fun SearchBar(
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "搜索地点、景点、餐饮",
                color = TextTertiary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 搜索输入框（展开态）
 */
@Composable
private fun SearchInput(
    text: String,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "搜索",
                tint = TextTertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("输入景点 / 设施名称", color = TextTertiary, fontSize = 14.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "关闭",
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 搜索结果列表
 */
@Composable
private fun SearchResultsList(
    results: List<PoiItem>,
    onItemClick: (PoiItem) -> Unit
) {
    if (results.isEmpty()) {
        EmptyHint(text = "未找到相关地点（当前仅支持灵山胜境景区内搜索）")
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        results.forEach { poi ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(poi) }
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "📍", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = poi.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(
                        text = poiTypeLabel(poi.type) + if (poi.description.isNotBlank()) " · ${poi.description.take(20)}" else "",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
                Text(
                    text = "导航 ›",
                    fontSize = 13.sp,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            HorizontalDivider(color = Color(0xFFEEEEEE))
        }
    }
}

/**
 * 路线规划底部面板
 */
@Composable
private fun RoutePlanningSheet(
    destination: PoiItem,
    allPois: List<PoiItem>,
    startSelection: String?,
    onStartChange: (String?) -> Unit,
    mode: String,
    onModeChange: (String) -> Unit,
    routeResult: RouteResult?,
    routeLoading: Boolean,
    routeError: String?,
    onPlan: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    tts: TtsController
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var startMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = 24.dp)
        ) {
            // 标题
            Text(
                text = "路线规划",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 终点
            RouteField(label = "终点", value = destination.name, valueColor = TextPrimary)

            // 起点（下拉选择）
            Box {
                RouteField(
                    label = "起点",
                    value = startSelection ?: "灵山胜境（默认）",
                    valueColor = if (startSelection == null) TextTertiary else TextPrimary,
                    onClick = { startMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = startMenuExpanded,
                    onDismissRequest = { startMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("灵山胜境（默认起点）", color = TextPrimary) },
                        onClick = { onStartChange(null); startMenuExpanded = false }
                    )
                    allPois.forEach { poi ->
                        DropdownMenuItem(
                            text = { Text(poi.name, color = TextPrimary) },
                            onClick = { onStartChange(poi.name); startMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 出行方式
            Text(text = "出行方式", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODE_LABELS.forEach { (key, label) ->
                    FilterChip(
                        selected = mode == key,
                        onClick = { onModeChange(key) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 规划按钮
            androidx.compose.material3.Button(
                onClick = onPlan,
                enabled = !routeLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                if (routeLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = if (routeLoading) "规划中…" else "规划路线", color = Color.White)
            }

            // 错误
            routeError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "⚠️ $it", fontSize = 13.sp, color = Color(0xFFD32F2F))
            }

            // 结果展示
            routeResult?.let { r ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(12.dp))

                // 降级提示（真实路网服务不可用时）
                if (r.estimated && r.warning != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                    ) {
                        Text(
                            text = r.warning,
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 概要
                val modeLabel = MODE_LABELS[r.mode] ?: r.mode
                Text(
                    text = "${modeLabel} · ${if (r.distanceM >= 1000) "%.1f公里".format(r.distanceM / 1000.0) else "${r.distanceM}米"} · 约${r.durationMin}分钟",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // 步骤
                r.steps.forEach { step ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = "${step.seqNo}.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            modifier = Modifier.width(20.dp)
                        )
                        Text(
                            text = step.instruction,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 讲解 + 语音讲解按钮
                val ttsText = routeTtsText(r)
                if (r.narration.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = r.narration, fontSize = 13.sp, color = TextTertiary, lineHeight = 20.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (ttsText != null) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            if (tts.isSpeaking) tts.stop() else tts.speak(ttsText)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandPrimary
                        )
                    ) {
                        Text(
                            text = if (tts.isSpeaking) "⏹ 停止讲解" else "🔊 语音讲解",
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                androidx.compose.material3.TextButton(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("清除路线", color = Color(0xFFD32F2F))
                }
            }
        }
    }
}

/**
 * 路线字段行（标签 + 值，可点击）
 */
@Composable
private fun RouteField(
    label: String,
    value: String,
    valueColor: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextTertiary,
            modifier = Modifier.width(48.dp)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            modifier = Modifier.weight(1f)
        )
        if (onClick != null) {
            Text(text = "›", fontSize = 18.sp, color = TextTertiary)
        }
    }
}

/**
 * 10 个功能按钮（2 行 × 5 列）
 */
@Composable
private fun QuickToolGrid(
    tools: List<QuickTool>,
    onToolClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 第一行（5 个）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tools.take(5).forEach { tool ->
                QuickToolItem(tool = tool, onClick = { onToolClick(tool.key) })
            }
        }
        // 第二行（5 个）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tools.drop(5).forEach { tool ->
                QuickToolItem(tool = tool, onClick = { onToolClick(tool.key) })
            }
        }
    }
}

/**
 * 单个功能按钮
 */
@Composable
private fun QuickToolItem(
    tool: QuickTool,
    onClick: () -> Unit
) {
    var lastClickTime by remember { mutableStateOf(0L) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            val now = System.currentTimeMillis()
            if (now - lastClickTime > 500) { // 防抖 500ms
                lastClickTime = now
                Log.d("MapScreen", "QuickTool clicked: ${tool.key}")
                onClick()
            }
        }
        .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(tool.color))),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = tool.iconRes),
                contentDescription = tool.name,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tool.name,
            fontSize = 12.sp,
            color = TextPrimary
        )
    }
}

/**
 * 天气卡片
 */
@Composable
private fun WeatherCard(
    city: String,
    summary: String,
    currentIcon: String,
    humidity: String,
    windPower: String,
    windDirection: String,
    hourly: List<WeatherHourly>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "☁ 今日${city}天气",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (windDirection.isNotBlank()) {
                    Text(
                        text = windDirection,
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 天气概况
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentIcon,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = summary,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (humidity.isNotBlank()) {
                            Text(
                                text = "💧 $humidity",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        if (windPower.isNotBlank()) {
                            Text(
                                text = "🌬 $windPower",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 逐小时预报 —— 均分整张卡片宽度，节点间留白均匀铺满
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                hourly.forEach { hour ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = hour.time,
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hour.icon,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = hour.temp,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * 高德 MapView
 */
@Composable
private fun AmapMapView(
    mapType: MapViewModel.MapType,
    currentLatLng: Pair<Double, Double>?,
    pois: List<PoiItem>,
    onMapReady: (AMap) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            onResume()
        }
    }
    val aMapRef = remember { mutableStateOf<AMap?>(null) }

    // 切换地图类型
    LaunchedEffect(mapType) {
        val aMap = aMapRef.value ?: return@LaunchedEffect
        aMap.mapType = when (mapType) {
            MapViewModel.MapType.NORMAL -> AMap.MAP_TYPE_NORMAL
            MapViewModel.MapType.SATELLITE -> AMap.MAP_TYPE_SATELLITE
            MapViewModel.MapType.NIGHT -> AMap.MAP_TYPE_NIGHT
        }
    }

    // 平滑定位
    LaunchedEffect(currentLatLng) {
        val (lat, lng) = currentLatLng ?: return@LaunchedEffect
        val aMap = aMapRef.value ?: return@LaunchedEffect
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16f))
    }

    AndroidView(
        factory = { ctx ->
            val aMap = mapView.map
            aMapRef.value = aMap
            aMap.uiSettings.isZoomControlsEnabled = false
            aMap.uiSettings.isCompassEnabled = true
            aMap.uiSettings.isScaleControlsEnabled = true

            // 默认位置
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(31.4297, 120.1058), 14f))

            onMapReady(aMap)
            mapView
        },
        update = { },
        modifier = Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}

/**
 * 功能按钮数据类
 */
data class QuickTool(
    val key: String,
    val name: String,
    val color: String,
    val iconRes: Int = when (key) {
        "attraction" -> R.drawable.ic_tool_attraction
        "food" -> R.drawable.ic_tool_food
        "toilet" -> R.drawable.ic_tool_toilet
        "shop" -> R.drawable.ic_tool_shop
        "service" -> R.drawable.ic_tool_service
        "digital" -> R.drawable.ic_tool_digital
        "rest" -> R.drawable.ic_tool_rest
        "medical" -> R.drawable.ic_tool_medical
        "entrance" -> R.drawable.ic_tool_entrance
        "exit" -> R.drawable.ic_tool_exit
        else -> R.drawable.ic_tool_attraction
    }
)

/**
 * 错误提示横幅
 */
@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFDECEA))
            .clickable { onDismiss() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚠️ $message（点击关闭）",
            fontSize = 13.sp,
            color = Color(0xFFD32F2F)
        )
    }
}

/**
 * 空数据提示
 */
@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextTertiary
        )
    }
}

/**
 * 附近 POI 列表底部面板
 *
 * 按距离由近到远排列，用户点击后触发路线规划。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NearbyPoiSheet(
    category: String,
    results: List<NearbyPoi>,
    loading: Boolean,
    onPoiClick: (NearbyPoi) -> Unit,
    onDismiss: () -> Unit
) {
    val categoryName = when (category) {
        "attraction" -> "景点"
        "food" -> "餐饮"
        "toilet" -> "卫生间"
        "shop" -> "商店"
        "service" -> "服务"
        "rest" -> "休息"
        "medical" -> "急救"
        "entrance" -> "入口"
        "exit" -> "出口"
        else -> category
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "附近${categoryName}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!loading) {
                    Text(
                        text = "${results.size}个结果",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                // 加载中
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandAccent, modifier = Modifier.size(32.dp))
                }
            } else if (results.isEmpty()) {
                // 无结果
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("附近暂无${categoryName}", color = TextTertiary, fontSize = 14.sp)
                }
            } else {
                // POI 列表（由近到远）
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { poi ->
                        NearbyPoiRow(poi = poi, onClick = { onPoiClick(poi) })
                    }
                }
            }
        }
    }
}

/**
 * 附近 POI 单行
 */
@Composable
private fun NearbyPoiRow(
    poi: NearbyPoi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 首字图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = poi.name.firstOrNull()?.toString() ?: "?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandAccent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = poi.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
                if (poi.address.isNotBlank()) {
                    Text(
                        text = poi.address,
                        fontSize = 12.sp,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 距离
            Text(
                text = formatDistance(poi.distance_m),
                fontSize = 12.sp,
                color = BrandAccent
            )
        }
    }
}

/**
 * 格式化距离（米 → 友好文本）
 */
private fun formatDistance(meters: Int): String {
    return if (meters >= 1000) {
        String.format("%.1fkm", meters / 1000.0)
    } else {
        "${meters}m"
    }
}
