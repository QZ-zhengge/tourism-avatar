package com.example.let_us_go.ui.pages.help

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 帮助中心（展示《随行_管理员使用手册》内容）
 */
@Composable
fun HelpCenterScreen() {
    val nav = LocalNav.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // 顶部导航
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                text = "帮助中心",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // 文档内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 文档封面（说明卡片）
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = BrandAccent.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "随行·管理员使用手册",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = HelpData.intro,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 各章节
            items(HelpData.sections) { section ->
                HelpSectionCard(section)
            }
        }
    }
}

@Composable
private fun HelpSectionCard(section: HelpSection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = section.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.content,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 22.sp
            )
        }
    }
}
