package com.example.let_us_go.core.float

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.let_us_go.R
import com.example.let_us_go.ui.theme.BrandAccent
import kotlin.math.roundToInt

/**
 * 全局悬浮窗（应用内 Compose 实现，参考 uniapp_box 优化版）
 *
 * 特性：
 * - 圆形设计，60dp 大小，内嵌 MP4 视频循环播放（ExoPlayer 单例复用）
 * - 支持拖拽移动
 * - 点击开始语音识别；录音中再点=结束并回答；思考/播报中再点=停止并重新听
 * - 长按隐藏悬浮窗，显示小触发按钮（点小按钮恢复）
 * - 麦克风权限门控：无权限时先请求
 * - 对话气泡面板：实时显示「我 / AI」文本与状态（聆听/思考/回答）
 *
 * @param hasMicPermission 是否有麦克风权限
 * @param onRequestMicPermission 请求麦克风权限的回调
 */
@Composable
fun FloatingWindow(
    hasMicPermission: Boolean = false,
    onRequestMicPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var isVisible by remember { mutableStateOf(true) }
    // 长按隐藏后，显示小触发按钮；点小按钮恢复
    var isHidden by remember { mutableStateOf(false) }
    // 默认位置：右边 8dp，底部导航栏上方 8dp（offset 为负值，因对齐 BottomEnd）
    var offsetX by remember { mutableFloatStateOf(with(density) { (-8).dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(with(density) { (-64).dp.toPx() }) } // 56dp导航栏 + 8dp间距
    var isDragging by remember { mutableStateOf(false) }

    val chatState by FloatChatManager.chatState.collectAsState()
    val isListening = chatState == FloatChatManager.ChatState.LISTENING
    val isThinking = chatState == FloatChatManager.ChatState.THINKING
    val isSpeaking = chatState == FloatChatManager.ChatState.SPEAKING

    val isDialogShowing by FloatChatManager.isDialogShowing.collectAsState()
    val aiText by FloatChatManager.aiText.collectAsState()
    val userText by FloatChatManager.userText.collectAsState()

    // ==================== 隐藏态：小触发按钮 ====================
    if (isHidden) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(BrandAccent.copy(alpha = 0.7f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { isHidden = false })
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        return
    }

    if (!isVisible) return

    // ==================== 主界面 ====================
    Box(modifier = Modifier.fillMaxSize()) {

        // ---------- 波浪动画层（监听态显示，向右下偏移 10dp 居中） ----------
        if (isListening) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            offsetX.roundToInt() + with(density) { 10.dp.roundToPx() },
                            offsetY.roundToInt() + with(density) { 10.dp.roundToPx() }
                        )
                    }
                    .align(Alignment.BottomEnd)
                    .size(80.dp)
            ) {
                WaveAnimation(modifier = Modifier.fillMaxSize())
            }
        }

        // ---------- 悬浮窗按钮 ----------
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .align(Alignment.BottomEnd)
                .size(60.dp)
                .clip(CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandAccent, BrandAccent.copy(alpha = 0.8f))
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { isHidden = true },
                        onTap = {
                            if (!isDragging) {
                                handleFloatTap(
                                    chatState = chatState,
                                    hasMicPermission = hasMicPermission,
                                    context = context,
                                    onRequestMicPermission = onRequestMicPermission
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )
        }

        // ---------- 对话气泡面板（悬浮窗左侧，底端对齐） ----------
        if (isDialogShowing) {
            DialogPanel(
                userText = userText,
                aiText = aiText,
                isListening = isListening,
                isThinking = isThinking,
                isSpeaking = isSpeaking,
                offsetX = offsetX,
                offsetY = offsetY,
                onDismiss = { FloatChatManager.dismissDialog() }
            )
        }
    }
}

/**
 * 点击悬浮窗的处理逻辑（参考 uniapp_box）
 */
private fun handleFloatTap(
    chatState: FloatChatManager.ChatState,
    hasMicPermission: Boolean,
    context: Context,
    onRequestMicPermission: () -> Unit
) {
    when (chatState) {
        FloatChatManager.ChatState.IDLE,
        FloatChatManager.ChatState.ERROR -> {
            if (hasMicPermission) {
                FloatChatManager.startChat(context)
            } else {
                onRequestMicPermission()
            }
        }
        FloatChatManager.ChatState.LISTENING -> {
            // 录音中 → 停止录音，开始回答
            FloatChatManager.stopListeningAndStartAnswering()
        }
        FloatChatManager.ChatState.THINKING,
        FloatChatManager.ChatState.SPEAKING -> {
            // 思考/回答中 → 停止回答，开始录音
            FloatChatManager.stopAnsweringAndStartListening()
        }
        else -> { /* CONNECTING 不响应 */ }
    }
}

/**
 * 对话气泡面板：悬浮窗左侧，底端与其持平
 */
@Composable
private fun DialogPanel(
    userText: String,
    aiText: String,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    offsetX: Float,
    offsetY: Float,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    // 悬浮窗 60dp，气泡右边缘距其左缘 6dp → 偏移 = offsetX - 66dp
    val dialogOffsetX = offsetX - with(density) { 66.dp.roundToPx() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(dialogOffsetX.roundToInt(), offsetY.roundToInt()) }
                .width(250.dp)
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.85f)),
            contentAlignment = Alignment.TopCenter
        ) {
            // 关闭按钮
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismiss() })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "×", color = Color.DarkGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // 对话内容（可滚动）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .padding(top = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isListening || isThinking || isSpeaking) {
                    val statusText = when {
                        isListening -> "正在聆听..."
                        isThinking -> "思考中..."
                        isSpeaking -> "回答中..."
                        else -> ""
                    }
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = statusText, color = BrandAccent, fontSize = 12.sp)
                    }
                }

                if (userText.isNotEmpty()) {
                    Column {
                        Text(text = "我：", color = Color.Gray, fontSize = 10.sp)
                        Text(
                            text = userText,
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (aiText.isNotEmpty()) {
                    Column {
                        Text(text = "AI：", color = BrandAccent, fontSize = 10.sp)
                        Text(
                            text = aiText,
                            color = Color.Black,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 波浪动画
 */
@Composable
private fun WaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "wave1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ), label = "wave2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ), label = "wave3"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha3"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize().scale(scale1).clip(CircleShape)
                .background(BrandAccent.copy(alpha = alpha1))
        )
        Box(
            modifier = Modifier
                .fillMaxSize().scale(scale2).clip(CircleShape)
                .background(BrandAccent.copy(alpha = alpha2))
        )
        Box(
            modifier = Modifier
                .fillMaxSize().scale(scale3).clip(CircleShape)
                .background(BrandAccent.copy(alpha = alpha3))
        )
    }
}

/**
 * 视频播放器组件（复用 ExoPlayerManager 单例，避免每次重建）
 */
@Composable
private fun VideoPlayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val exoPlayer = remember { ExoPlayerManager.getOrCreate(context) }

    // 监听器在 DisposableEffect 内增/删，避免切 Tab 时重复累积
    DisposableEffect(Unit) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    androidx.media3.common.Player.STATE_BUFFERING -> isLoading = true
                    androidx.media3.common.Player.STATE_READY -> { isReady = true; isLoading = false }
                    androidx.media3.common.Player.STATE_IDLE -> isLoading = false
                    androidx.media3.common.Player.STATE_ENDED -> isLoading = false
                }
            }
        }
        exoPlayer.addListener(listener)
        // 补一次初始状态同步（可能 prepare 已完成）
        if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_READY) {
            isReady = true
            isLoading = false
        }
        onDispose {
            exoPlayer.removeListener(listener)
            // 悬浮窗仅在 Tab 页显示：离开时暂停，不释放（下次复用）
            ExoPlayerManager.pause()
        }
    }

    Box(modifier = modifier) {
        if (isReady) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BrandAccent),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.7f))
                    )
                }
            }
        }
    }
}
