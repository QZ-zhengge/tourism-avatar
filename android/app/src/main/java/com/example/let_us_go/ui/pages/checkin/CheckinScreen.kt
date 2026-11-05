package com.example.let_us_go.ui.pages.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.let_us_go.BuildConfig
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.model.PhotoSpot
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CheckinScreen() {
    val context = LocalContext.current
    val nav = LocalNav.current
    val vm: PhotoSpotsViewModel = viewModel()
    val spots by vm.spots.collectAsState()
    val loading by vm.loading.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val loadingMore by vm.loadingMore.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    // 滚到底部自动加载更多
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (lastVisible >= spots.size - 3 && hasMore && !loadingMore) {
                    vm.loadMore()
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        // 顶栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.navigateBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text("拍照打卡", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        if (loading && spots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandAccent)
            }
        } else if (spots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Text("暂无拍照打卡点", color = TextTertiary, fontSize = 14.sp, modifier = Modifier.padding(top = 60.dp))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(spots) { spot ->
                    PhotoSpotCard(spot, onClick = {
                        val url = spot.link ?: spot.sourceLink
                        if (!url.isNullOrBlank()) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        }
                    })
                }
                if (loadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandAccent, strokeWidth = 2.dp)
                        }
                    }
                }
                if (!hasMore && spots.isNotEmpty()) {
                    item {
                        Text("— 没有更多了 —", color = TextTertiary, fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSpotCard(spot: PhotoSpot, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column {
            // 图片区域：有图显示图片，无图显示首字+渐变背景
            val imageUrl = spot.image?.let {
                if (it.startsWith("http")) it else "${BuildConfig.API_BASE_URL.trimEnd('/')}$it"
            }
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = spot.title,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 无图：首字 + 渐变背景
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BrandAccent.copy(alpha = 0.6f),
                                    BrandPrimary.copy(alpha = 0.6f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = spot.title.firstOrNull()?.toString() ?: "拍",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = BrandAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(spot.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }

                if (!spot.summary.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(spot.summary, fontSize = 13.sp, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                }

                // 底部信息
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!spot.location.isNullOrBlank()) {
                        Text(spot.location, fontSize = 11.sp, color = TextTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                    if (spot.count > 0) {
                        Text("📸 ${spot.count}", fontSize = 11.sp, color = TextTertiary)
                    }
                    if (spot.rating != null) {
                        Spacer(Modifier.width(8.dp))
                        Text("⭐ ${spot.rating}", fontSize = 11.sp, color = TextTertiary)
                    }
                }
            }
        }
    }
}
