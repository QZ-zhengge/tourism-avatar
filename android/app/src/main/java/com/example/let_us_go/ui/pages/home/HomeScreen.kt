package com.example.let_us_go.ui.pages.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.R
import com.example.let_us_go.data.model.ScenicSpot
import com.example.let_us_go.ui.components.RemoteImage
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.ui.pages.digital_human.DigitalHumanLaunchIntent
import com.example.let_us_go.ui.nav.switchTab
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.BrandSecondary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 首页（完整复刻 uni-app pages/home/index.vue）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val nav = LocalNav.current
    val viewModel: HomeViewModel = viewModel()
    val weather by viewModel.weather.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val hotSpots by viewModel.hotSpots.collectAsState()
    val spotImages by viewModel.spotImages.collectAsState()

    var selectedSpotIndex by remember { mutableStateOf(0) }
    val scenicSpots = remember {
        listOf(
            HomeSpot("灵山胜境", "无锡", 31.42, 120.09),
            HomeSpot("西湖", "杭州", 30.25, 120.15),
            HomeSpot("故宫", "北京", 39.92, 116.40),
            HomeSpot("黄山", "黄山", 30.14, 118.18),
            HomeSpot("鼓浪屿", "厦门", 24.45, 118.07)
        )
    }

    LaunchedEffect(Unit) { viewModel.loadAll() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                HeroBanner(
                    bannerImages = viewModel.bannerImages,
                    weatherText = weather?.let { "${it.weather} · ${it.temperature}°C" }
                        ?: "晴朗 · 24°C",
                    weatherIcon = weather?.let { mapWeatherIcon(it.weather) } ?: "☀️",
                    selectedSpotIndex = selectedSpotIndex,
                    scenicSpots = scenicSpots,
                    onSpotChange = { idx ->
                        selectedSpotIndex = idx
                        viewModel.loadWeather(scenicSpots[idx].city)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                FunctionGrid { item ->
                    if (item.autoOpenPhoto) DigitalHumanLaunchIntent.openPhotoSheetOnEnter = true
                    if (Route.tabs.contains(item.route)) nav.switchTab(item.route)
                    else nav.navigateTo(item.route)
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                ActionRow(
                    onDigitalHuman = { nav.switchTab(Route.DIGITAL_HUMAN) },
                    onItinerary = { nav.navigateTo(Route.ITINERARY) }
                )
            }

            if (hotSpots.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    HotSpotsSection(hotSpots = hotSpots, spotImages = spotImages) { _ -> }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                PostsSection(posts = posts) { nav.switchTab(Route.MOMENTS) }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

private fun mapWeatherIcon(weather: String): String = when (weather) {
    "晴" -> "☀️"
    "多云" -> "⛅"
    "阴" -> "☁️"
    "小雨", "阵雨" -> "🌦"
    "中雨", "大雨" -> "🌧"
    "雷阵雨" -> "⛈"
    "暴雨" -> "🌊"
    "雪", "小雪", "中雪", "大雪" -> "❄️"
    "雾", "霾", "浮尘", "扬沙" -> "🌫"
    "大风", "风" -> "💨"
    else -> "☀️"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroBanner(
    bannerImages: List<Int>,
    weatherText: String,
    weatherIcon: String,
    selectedSpotIndex: Int,
    scenicSpots: List<HomeSpot>,
    onSpotChange: (Int) -> Unit
) {
    val displayBanners = bannerImages
    val pagerState = rememberPagerState(pageCount = { displayBanners.size })
    var showSpotDropdown by remember { mutableStateOf(false) }

    // 自动播放轮播图（3.2秒间隔，与uniapp一致）
    LaunchedEffect(displayBanners.size) {
        while (true) {
            kotlinx.coroutines.delay(3200)
            val nextPage = (pagerState.currentPage + 1) % displayBanners.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 渲染本地轮播图（已按轮播比例居中裁剪，顺序 banner_1 → banner_2 → banner_3）
                Image(
                    painter = painterResource(id = displayBanners[page]),
                    contentDescription = "轮播图 ${page + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 天气和景区选择 - 左上角（可点击展开选择）
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 12.dp)
        ) {
            Card(
                modifier = Modifier.clickable { showSpotDropdown = true },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = scenicSpots[selectedSpotIndex].name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(" ▼", fontSize = 10.sp, color = TextTertiary)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = weatherIcon, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = weatherText, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }

            // 景区选择下拉菜单
            DropdownMenu(
                expanded = showSpotDropdown,
                onDismissRequest = { showSpotDropdown = false }
            ) {
                scenicSpots.forEachIndexed { index, spot ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = spot.name,
                                    fontSize = 14.sp,
                                    color = if (index == selectedSpotIndex) BrandAccent else TextPrimary
                                )
                                if (index == selectedSpotIndex) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("✓", fontSize = 14.sp, color = BrandAccent)
                                }
                            }
                        },
                        onClick = {
                            onSpotChange(index)
                            showSpotDropdown = false
                        }
                    )
                }
            }
        }

        // 页面指示器 - 底部居中（基于轮播图数量）
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            val coroutineScope = rememberCoroutineScope()
            displayBanners.forEachIndexed { idx, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (idx == pagerState.currentPage) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (idx == pagerState.currentPage) Color(0xFF333333) else Color(0xFFBDBDBD))
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(idx)
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun FunctionGrid(onFunctionTap: (FunctionItem) -> Unit) {
    val functions = remember {
        listOf(
            FunctionItem("热门景点", Icons.Outlined.Star, Color(0xFF2BB57B), Color(0xFFE8F5F0), Route.HOTSPOTS),
            FunctionItem("实时讲解", Icons.Outlined.Map, Color(0xFF00BCD4), Color(0xFFE0F7FA), Route.LIVE_GUIDE_FLOW),
            FunctionItem("AI问答", Icons.AutoMirrored.Outlined.Chat, Color(0xFF8E5FD4), Color(0xFFF2EAF8), Route.AI_CHAT),
            FunctionItem("活动演出", Icons.Outlined.PlayArrow, Color(0xFFF09030), Color(0xFFFFF3E0), Route.EVENTS),
            FunctionItem("美食推荐", Icons.Outlined.Restaurant, Color(0xFFE85050), Color(0xFFFDECEA), Route.FOOD),
            FunctionItem("拍照打卡", Icons.Outlined.Camera, Color(0xFF2196C8), Color(0xFFE8F4FD), Route.CHECKIN),
            FunctionItem("游玩攻略", Icons.Outlined.Bookmark, Color(0xFF3AAA5C), Color(0xFFEDF7EE), Route.GUIDE),
            FunctionItem("周边好店", Icons.Outlined.ShoppingCart, Color(0xFFE07820), Color(0xFFFEF3E2), Route.NEARBY)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "探索服务",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    functions.subList(0, 4).forEach { item ->
                        FunctionItemView(item = item, onTap = { onFunctionTap(item) }, modifier = Modifier.weight(1f))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    functions.subList(4, 8).forEach { item ->
                        FunctionItemView(item = item, onTap = { onFunctionTap(item) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FunctionItemView(
    item: FunctionItem,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onTap() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(item.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = item.iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = item.title, fontSize = 12.sp, color = TextPrimary)
    }
}

data class FunctionItem(
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val bgColor: Color,
    val route: String,
    val autoOpenPhoto: Boolean = false
)

@Composable
private fun ActionRow(
    onDigitalHuman: () -> Unit,
    onItinerary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionCard(
            title = "数字人",
            sub = "智慧陪伴导览",
            bgColor = BrandAccent,
            modifier = Modifier.weight(1f),
            onTap = onDigitalHuman
        )
        ActionCard(
            title = "我的行程",
            sub = "路线及预订记录",
            bgColor = BrandSecondary,
            modifier = Modifier.weight(1f),
            onTap = onItinerary
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    sub: String,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(bgColor, bgColor.copy(alpha = 0.7f))
                )
            )
            .clickable { onTap() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.Center) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun HotSpotsSection(
    hotSpots: List<ScenicSpot>,
    spotImages: Map<String, List<String>>,
    onTap: (ScenicSpot) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "热门景点",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(text = "查看更多 >", fontSize = 12.sp, color = TextTertiary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(hotSpots) { spot ->
                    val imgUrl = spotImages[spot.name ?: spot.title]
                    HotSpotCard(spot = spot, imageUrl = imgUrl?.firstOrNull(), onTap = { onTap(spot) })
                }
            }
        }
    }
}

@Composable
private fun HotSpotCard(spot: ScenicSpot, imageUrl: String? = null, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onTap() }
    ) {
        // 优先用管理端图片，其次用景点自带的 image/coverImage
        val url = imageUrl ?: spot.image ?: spot.coverImage
        RemoteImage(
            url = url,
            fallbackText = spot.name ?: spot.title ?: "?",
            fallbackEmoji = "🗺",
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = spot.name ?: spot.title ?: "未命名",
            fontSize = 13.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PostsSection(
    posts: List<HomePost>,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "旅友圈精选",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "查看更多 >",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    modifier = Modifier.clickable { onTap() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            posts.forEach { post ->
                PostItemRow(post = post, onTap = onTap)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PostItemRow(post: HomePost, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .height(72.dp)
    ) {
        RemoteImage(
            url = post.image,
            fallbackText = post.title.take(1),
            fallbackEmoji = "📝",
            modifier = Modifier.size(72.dp),
            cornerRadius = 8.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = post.title,
                fontSize = 14.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = post.excerpt,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class HomePost(
    val id: String,
    val title: String,
    val excerpt: String,
    val image: String
)

data class HomeSpot(
    val name: String,
    val city: String,
    val latitude: Double,
    val longitude: Double
)
