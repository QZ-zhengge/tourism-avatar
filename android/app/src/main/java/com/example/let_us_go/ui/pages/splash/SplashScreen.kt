package com.example.let_us_go.ui.pages.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.let_us_go.R
import com.example.let_us_go.ui.theme.BrandPrimary
import kotlinx.coroutines.delay

/**
 * 应用启动动画（约 1 秒，极简）
 *
 * 0~450ms: logo 微放大淡入
 * 450~800ms: 静止
 * 800~1000ms: 整体淡出
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var phase by remember { mutableStateOf(0) } // 0=show, 1=hide

    val contentAlpha by animateFloatAsState(
        targetValue = if (phase == 0) 1f else 0f,
        animationSpec = tween(250)
    )
    val logoScale by animateFloatAsState(
        targetValue = if (phase == 0) 1f else 0.9f,
        animationSpec = tween(250)
    )

    LaunchedEffect(Unit) {
        delay(800)  // 展示 800ms
        phase = 1   // 开始淡出
        delay(250)  // 等淡出动画完成
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(contentAlpha)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "随行",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .scale(logoScale)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "随行",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "智能景区虚拟向导",
                fontSize = 13.sp,
                color = Color(0xFF888888)
            )
        }
    }
}