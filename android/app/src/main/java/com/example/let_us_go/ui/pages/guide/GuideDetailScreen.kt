package com.example.let_us_go.ui.pages.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.let_us_go.LocalNav
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 游玩攻略详情页
 *
 * 从 GuideTipHolder 中获取数据，展示完整的攻略内容。
 * 样式与主页面统一。
 */
@Composable
fun GuideDetailScreen() {
    val nav = LocalNav.current
    val tip = GuideTipHolder.currentTip

    if (tip == null) {
        // 数据丢失（如页面被系统回收），返回
        nav.navigateBack()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "返回",
                tint = TextPrimary,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { nav.navigateBack() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "游玩攻略",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }

        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // 标题卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 标题图标
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BrandAccent.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tip.title.firstOrNull()?.toString() ?: "攻",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 标题
                    Text(
                        text = tip.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )

                    // 来源信息
                    if (!tip.source.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "来源",
                                fontSize = 12.sp,
                                color = TextTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tip.source,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // 标签
                    if (tip.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                            tip.tags.take(5).forEach { tag ->
                                if (tag.length <= 20) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BrandAccent.copy(alpha = 0.1f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            fontSize = 11.sp,
                                            color = BrandAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 正文内容卡片 - 展示完整的 summary 内容
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "攻略详情",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 完整内容 - 直接显示 summary（API 返回的完整文本）
                    val content = tip.summary ?: ""
                    if (content.isNotBlank()) {
                        Text(
                            text = content,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )
                    } else {
                        Text(
                            text = "暂无详细内容",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}