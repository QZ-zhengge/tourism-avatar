package com.example.let_us_go.ui.pages.hotspots

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.model.ScenicSpot
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.BrandWarning
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 热门景点页（完整复刻 uni-app pages/hotspots/index.vue）
 *
 * 核心功能：
 * - 分页列表加载（滚动到底部自动 loadMore）
 * - 点击条目弹出详情面板（selectedSpot，对应 uni 端内嵌弹窗）
 * - 筛选 Tab：推荐 / 热度 / 距离
 */
@Composable
fun HotspotsScreen() {
    val nav = LocalNav.current
    val viewModel: HotspotsViewModel = viewModel()
    val spots by viewModel.spots.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selectedSpot by viewModel.selectedSpot.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val spotImages by viewModel.spotImages.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.load() }

    // 滚动到底部时触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearBottom ->
            if (nearBottom && hasMore && !loadingMore) {
                viewModel.loadMore()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgPage)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(filter, viewModel::setFilter, onBack = { nav.navigateBack() })

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(spots) { spot ->
                    SpotRow(
                        spot = spot,
                        imageUrl = spotImages[spot.name ?: spot.title],
                        onClick = { viewModel.selectSpot(spot) }
                    )
                }
                if (loadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("加载更多...", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                }
                if (!hasMore && spots.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("— 已显示全部景点 —", fontSize = 12.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }

        // 景点详情面板（对应 uni 端 selectedSpot 弹窗）
        if (selectedSpot != null) {
            SpotDetailPanel(
                spot = selectedSpot!!,
                imageUrl = spotImages[selectedSpot!!.name ?: selectedSpot!!.title],
                onClose = { viewModel.clearSelectedSpot() }
            )
        }
    }
}

@Composable
private fun TopBar(
    filter: String,
    onFilterChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(BgPage)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier.clickable { onBack() }.padding(8.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "热门景点",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("推荐", "热度", "距离").forEach { f ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (filter == f) BrandAccent else BgCard)
                        .clickable { onFilterChange(f) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = f,
                        fontSize = 12.sp,
                        color = if (filter == f) Color.White else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotRow(spot: ScenicSpot, imageUrl: List<String>?, onClick: () -> Unit) {
    val hasImage = !imageUrl.isNullOrEmpty()
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column {
            // 图片区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .then(
                        if (hasImage) Modifier else Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BrandAccent.copy(alpha = 0.6f),
                                    BrandPrimary.copy(alpha = 0.6f)
                                )
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (hasImage) {
                    AsyncImage(
                        model = imageUrl!![0],
                        contentDescription = spot.name ?: spot.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = (spot.title ?: "景").first().toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // 文字区域
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = spot.name ?: spot.title ?: "未命名",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = spot.summary ?: spot.description ?: "",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = BrandWarning,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${spot.rating ?: 4.5}",
                        fontSize = 11.sp,
                        color = BrandWarning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (!spot.location.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(text = spot.location, fontSize = 11.sp, color = TextTertiary, maxLines = 1)
                    }
                }
                if (spot.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        spot.tags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BrandAccent.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(text = tag, fontSize = 10.sp, color = BrandAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 景点详情面板（对应 uni 端 selectedSpot 内嵌弹窗）
 */
@Composable
private fun SpotDetailPanel(spot: ScenicSpot, imageUrl: List<String>?, onClose: () -> Unit) {
    val hasImage = !imageUrl.isNullOrEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { onClose() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = spot.name ?: spot.title ?: "景点详情",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "关闭",
                        tint = TextTertiary,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onClose() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 封面图
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (hasImage) Modifier else Modifier.background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        BrandAccent.copy(alpha = 0.6f),
                                        BrandPrimary.copy(alpha = 0.6f)
                                    )
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasImage) {
                        AsyncImage(
                            model = imageUrl!![0],
                            contentDescription = spot.name ?: spot.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("🏞️", fontSize = 60.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 评分 + 位置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = BrandWarning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${spot.rating ?: 4.5}",
                        fontSize = 13.sp,
                        color = BrandWarning,
                        fontWeight = FontWeight.Medium
                    )
                    if (!spot.location.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = spot.location, fontSize = 12.sp, color = TextTertiary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(modifier = Modifier.height(8.dp))

                // 简介
                val intro = spot.description ?: spot.summary
                if (!intro.isNullOrBlank()) {
                    Text(
                        text = intro,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 标签
                if (spot.tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        spot.tags.take(5).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BrandAccent.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(text = tag, fontSize = 11.sp, color = BrandAccent)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 来源链接提示
                if (!spot.sourceLink.isNullOrBlank()) {
                    Text(
                        text = "来源：${spot.source ?: spot.sourceLink}",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
