package com.example.let_us_go.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * 远程图片组件（Phase 2 新增）
 *
 * 替代 emoji 占位符，加载真实 URL 图片。
 *
 * 用法：
 * ```
 * RemoteImage(
 *     url = "https://...",
 *     fallbackText = "故宫",
 *     modifier = Modifier.size(120.dp)
 * )
 * ```
 *
 * 特性：
 * - 自动 SVG/视频帧解码
 * - Loading 状态显示渐变占位
 * - Error 状态显示 emoji fallback + 文字
 * - Crop 自动按 ContentScale.Crop
 *
 * @param url 远程图片 URL（可为 null）
 * @param fallbackText 加载失败时显示的文字
 * @param fallbackEmoji 加载失败时显示的 emoji（默认 📍）
 */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    fallbackText: String? = null,
    fallbackEmoji: String = "📍",
    cornerRadius: androidx.compose.ui.unit.Dp = 0.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else RoundedCornerShape(0.dp)

    if (url.isNullOrBlank()) {
        // 无 URL：直接显示 emoji fallback
        PlaceholderBox(
            modifier = modifier.clip(shape),
            text = fallbackText,
            emoji = fallbackEmoji
        )
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = fallbackText,
        modifier = modifier.clip(shape),
        contentScale = contentScale
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Empty -> {
                PlaceholderBox(
                    modifier = Modifier.fillMaxSize(),
                    text = null,
                    emoji = null,
                    showShimmer = true
                )
            }
            is AsyncImagePainter.State.Error -> {
                PlaceholderBox(
                    modifier = Modifier.fillMaxSize(),
                    text = fallbackText,
                    emoji = fallbackEmoji
                )
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}

@Composable
private fun PlaceholderBox(
    modifier: Modifier,
    text: String?,
    emoji: String?,
    showShimmer: Boolean = false
) {
    val bg = if (showShimmer) {
        Modifier.background(
            androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE3F2FD),
                    Color(0xFFBBDEFB),
                    Color(0xFFE3F2FD)
                )
            )
        )
    } else {
        Modifier.background(Color(0xFFF0F4F8))
    }

    Box(
        modifier = modifier.then(bg),
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Text(text = emoji, fontSize = 28.sp)
        } else if (text != null) {
            Text(
                text = text.take(1),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
    }
}

/**
 * 头像组件（圆形，带 fallback 文字）
 */
@Composable
fun AvatarImage(
    url: String?,
    nickname: String?,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFF4A7EE0)
) {
    val context = LocalContext.current
    val initial = (nickname?.firstOrNull() ?: '?').toString().uppercase()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (!url.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(url).crossfade(true).build(),
                contentDescription = nickname,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Error,
                    is AsyncImagePainter.State.Empty -> Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    else -> SubcomposeAsyncImageContent()
                }
            }
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}