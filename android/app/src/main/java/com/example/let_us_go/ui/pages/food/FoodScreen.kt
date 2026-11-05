package com.example.let_us_go.ui.pages.food

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
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.model.Food
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandDanger
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.BrandWarning
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 美食推荐页（复刻 uni-app pages/food/index.vue）
 */
@Composable
fun FoodScreen() {
    val context = LocalContext.current
    val nav = LocalNav.current
    val viewModel: FoodViewModel = viewModel()
    val foods by viewModel.foods.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.load() }

    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 3
        }.collect { nearBottom ->
            if (nearBottom && hasMore && !loadingMore) viewModel.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier.clickable { nav.navigateBack() }.padding(8.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "美食推荐",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(foods) { food ->
                FoodRow(food = food, onClick = {
                    if (!food.sourceLink.isNullOrBlank()) {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(food.sourceLink))
                            context.startActivity(intent)
                        } catch (_: Exception) { }
                    }
                })
            }
            if (loadingMore) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Text("加载更多...", fontSize = 12.sp, color = TextTertiary)
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                BrandDanger.copy(alpha = 0.5f),
                                BrandWarning.copy(alpha = 0.5f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = food.title.firstOrNull()?.toString() ?: "美",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = food.summary ?: "",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!food.price.isNullOrBlank()) {
                        Text(text = food.price, fontSize = 12.sp, color = BrandDanger)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        imageVector = Icons.Outlined.Star,
                        contentDescription = null,
                        tint = BrandWarning,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = "4.5", fontSize = 11.sp, color = BrandWarning)
                }
            }
        }
    }
}
