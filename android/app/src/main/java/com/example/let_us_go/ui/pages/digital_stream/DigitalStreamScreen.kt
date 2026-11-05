package com.example.let_us_go.ui.pages.digital_stream

import android.view.ViewGroup
import android.widget.VideoView
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 数字人直播页（复刻 uni-app pages/digital-stream/index.vue）
 */
@Composable
fun DigitalStreamScreen() {
    val nav = LocalNav.current
    val viewModel: DigitalStreamViewModel = viewModel()
    val streams by viewModel.streams.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPage)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier
                    .clickable { nav.navigateBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "数字人直播",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        // 主播放区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Color.Black)
        ) {
            // 简化：用渐变背景 + 浮动"播放"按钮
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BrandAccent.copy(alpha = 0.3f), Color.Black)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { viewModel.startStream() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            // 直播标签
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 直播列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            items(streams) { stream ->
                StreamRow(stream = stream, onClick = { viewModel.selectStream(stream.id) })
            }
        }
    }
}

@Composable
private fun StreamRow(stream: StreamInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(BrandPrimary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(stream.avatar, fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.size(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stream.title, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text("${stream.viewers} 人正在观看", fontSize = 11.sp, color = TextTertiary)
        }
    }
}

data class StreamInfo(
    val id: String,
    val title: String,
    val avatar: String,
    val viewers: Int
)
