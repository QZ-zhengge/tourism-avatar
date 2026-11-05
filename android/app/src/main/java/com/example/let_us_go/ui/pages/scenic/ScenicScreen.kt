package com.example.let_us_go.ui.pages.scenic

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.BrandSecondary
import com.example.let_us_go.ui.theme.BrandWarning
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import kotlin.math.cos
import kotlin.math.sin

/**
 * 知识图谱页（复刻 uni-app pages/scenic/index.vue）
 * 简化版：用 Canvas 绘制节点 + 连线
 */
@Composable
fun ScenicScreen() {
    val nav = LocalNav.current
    val viewModel: ScenicViewModel = viewModel()
    val tags by viewModel.tags.collectAsState()
    val nodes by viewModel.nodes.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

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
                text = "知识图谱",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // 标签栏
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tags) { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrandAccent.copy(alpha = 0.1f))
                        .clickable { viewModel.filterByTag(tag) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = tag, fontSize = 12.sp, color = BrandAccent)
                }
            }
        }

        // 图谱区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard)
        ) {
            KnowledgeGraph(nodes = nodes)
        }

        // 节点列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(nodes) { n ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(when (n.type) {
                                    "scenic" -> BrandAccent
                                    "food" -> BrandWarning
                                    "event" -> BrandSecondary
                                    else -> BrandPrimary
                                })
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = n.label,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "类型：${n.type}",
                                fontSize = 11.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

data class KGNode(val id: String, val label: String, val type: String)

@Composable
private fun KnowledgeGraph(nodes: List<KGNode>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2
        val r = (minOf(w, h) / 2.5f).coerceAtMost(120f)

        // 画连线（中心 → 各节点）
        nodes.forEachIndexed { idx, _ ->
            val angle = (2 * Math.PI * idx / nodes.size).toFloat()
            val nx = cx + r * cos(angle)
            val ny = cy + r * sin(angle)
            drawLine(
                color = Color(0xFFB0BEC5),
                start = Offset(cx, cy),
                end = Offset(nx, ny),
                strokeWidth = 1.5f
            )
        }

        // 中心节点
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BrandPrimary, BrandAccent),
                center = Offset(cx, cy),
                radius = 40f
            ),
            radius = 32f,
            center = Offset(cx, cy)
        )

        // 各节点
        nodes.forEachIndexed { idx, _ ->
            val angle = (2 * Math.PI * idx / nodes.size).toFloat()
            val nx = cx + r * cos(angle)
            val ny = cy + r * sin(angle)
            drawCircle(
                color = BrandAccent,
                radius = 18f,
                center = Offset(nx, ny)
            )
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = Offset(nx, ny)
            )
        }
    }
}
