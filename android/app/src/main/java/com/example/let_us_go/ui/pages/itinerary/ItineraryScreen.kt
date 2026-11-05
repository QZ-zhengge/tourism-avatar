package com.example.let_us_go.ui.pages.itinerary

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.model.ItineraryItem
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 我的行程页
 *
 * 功能：
 * - 展示按日期分组的行程列表
 * - AI 规划入口（调用 DeepSeek）
 * - 自动接收数字人页的酒店预订
 * - 删除行程项
 */
@Composable
fun ItineraryScreen() {
    val nav = LocalNav.current
    val viewModel: ItineraryViewModel = viewModel()
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val aiSummary by viewModel.aiSummary.collectAsState()
    val error by viewModel.error.collectAsState()

    var showAiDialog by remember { mutableStateOf(false) }
    var aiQuery by remember { mutableStateOf("") }

    // 清空 AI 摘要提示
    LaunchedEffect(aiSummary) {
        if (aiSummary != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearAiSummary()
        }
    }

    // AI 规划对话框
    if (showAiDialog) {
        AiPlanDialog(
            query = aiQuery,
            loading = loading,
            onQueryChange = { aiQuery = it },
            onConfirm = {
                if (UserPrefs.isLoggedIn()) {
                    viewModel.planByAi(aiQuery)
                } else {
                    nav.navigateTo(Route.LOGIN)
                }
                showAiDialog = false
                aiQuery = ""
            },
            onDismiss = {
                showAiDialog = false
                aiQuery = ""
            }
        )
    }

    // 错误对话框
    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("确定", color = BrandAccent)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(BgPage)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部导航
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
                    text = "我的行程",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                if (items.isNotEmpty()) {
                    Text(
                        text = "${items.size} 项",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }

            // AI 摘要提示
            aiSummary?.let { summary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BrandAccent.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("✨", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = summary,
                            fontSize = 13.sp,
                            color = BrandAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 加载指示器
            if (loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = BrandAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "AI 正在规划行程...",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 行程列表
            if (items.isEmpty() && !loading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无行程",
                            color = TextTertiary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击下方「AI 规划」，让 AI 帮你规划行程\n或与数字人对话自动添加酒店信息",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                // 按日期分组
                val grouped = items.groupBy { it.date ?: "待定" }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (date, dayItems) ->
                        item {
                            // 日期标题
                            Row(
                                modifier = Modifier.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📅",
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = date,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "${dayItems.size} 项",
                                    fontSize = 12.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                        items(dayItems) { item ->
                            ItineraryRow(
                                item = item,
                                onDelete = { viewModel.removeItem(item.id) }
                            )
                        }
                    }
                }
            }
        }

        // AI 规划按钮
        ExtendedFloatingActionButton(
            onClick = { showAiDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = BrandAccent
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "AI 规划", color = Color.White)
        }
    }
}

/**
 * AI 规划对话框
 */
@Composable
private fun AiPlanDialog(
    query: String,
    loading: Boolean,
    onQueryChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 行程规划") },
        text = {
            Column {
                Text(
                    text = "描述你的旅行计划，AI 会自动生成详细行程：",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            "例如：明天去杭州西湖两日游，帮我订一家靠近景区的酒店",
                            color = TextTertiary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandAccent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = query.isNotBlank() && !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = BrandAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("生成", color = BrandAccent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = TextTertiary)
            }
        }
    )
}

/**
 * 行程项卡片
 */
@Composable
private fun ItineraryRow(
    item: ItineraryItem,
    onDelete: () -> Unit
) {
    val icon: ImageVector = when (item.kind) {
        "hotel" -> Icons.Outlined.Hotel
        "restaurant" -> Icons.Outlined.Restaurant
        "transport" -> Icons.Outlined.DirectionsBus
        "spot" -> Icons.Outlined.Place
        else -> Icons.Outlined.Notes
    }

    val iconColor: Color = when (item.kind) {
        "hotel" -> Color(0xFFE65100)
        "restaurant" -> Color(0xFFD32F2F)
        "transport" -> Color(0xFF1565C0)
        "spot" -> Color(0xFF2E7D32)
        else -> BrandAccent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 类型图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 标题
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )

                // 时间 + 类型标签
                if (item.time != null || item.kind.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.time != null) {
                            Text(
                                text = item.time,
                                fontSize = 12.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = when (item.kind) {
                                "hotel" -> "住宿"
                                "restaurant" -> "餐饮"
                                "transport" -> "交通"
                                "spot" -> "景点"
                                else -> "备注"
                            },
                            fontSize = 10.sp,
                            color = iconColor.copy(alpha = 0.8f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(iconColor.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }

                // 描述
                if (!item.summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.summary,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2
                    )
                }

                // 酒店额外信息
                if (item.kind == "hotel" && item.price != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row {
                        Text(
                            text = "¥${item.price}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                        if (item.roomType != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item.roomType,
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            // 删除按钮
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}