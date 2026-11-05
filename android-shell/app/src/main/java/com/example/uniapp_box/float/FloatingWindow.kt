package com.example.uniapp_box.float

import android.net.Uri
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.uniapp_box.R
import kotlin.math.roundToInt

/**
 * 全局悬浮窗
 *
 * 特性：
 * - 圆形设计，60dp 大小
 * - 内嵌 MP4 视频循环播放
 * - 支持拖拽移动
 * - 点击开始语音识别
 * - 长按隐藏悬浮窗，显示小触发按钮
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
    var isHidden by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(with(density) { (-8).dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(with(density) { (-64).dp.toPx() }) }
    var isDragging by remember { mutableStateOf(false) }

    val chatState by FloatChatManager.chatState.collectAsState()
    val isListening = chatState == FloatChatManager.ChatState.LISTENING
    val isThinking = chatState == FloatChatManager.ChatState.THINKING
    val isSpeaking = chatState == FloatChatManager.ChatState.SPEAKING
    
    val isDialogShowing by FloatChatManager.isDialogShowing.collectAsState()
    val aiText by FloatChatManager.aiText.collectAsState()
    val userText by FloatChatManager.userText.collectAsState()

    // ==================== 隐藏状态：显示小触发按钮 ====================
    if (isHidden) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2DBA98).copy(alpha = 0.7f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { isHidden = false }
                        )
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

        // ---------- 波浪动画层 ----------
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
                        colors = listOf(Color(0xFF2DBA98), Color(0xFF2DBA98).copy(alpha = 0.8f))
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

        // ---------- 对话框：悬浮窗左边0.2cm，底端与悬浮窗底端持平 ----------
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
 * 点击悬浮窗的处理逻辑
 */
private fun handleFloatTap(
    chatState: FloatChatManager.ChatState,
    hasMicPermission: Boolean,
    context: android.content.Context,
    onRequestMicPermission: () -> Unit
) {
    when (chatState) {
        FloatChatManager.ChatState.IDLE,
        FloatChatManager.ChatState.ERROR -> {
            // 空闲/错误 → 开始录音，弹出对话框
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
        else -> { /* CONNECTING 等状态不响应 */ }
    }
}

/**
 * 对话框面板
 * 悬浮窗左边0.2cm，底端与悬浮窗底端持平
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
    // 悬浮窗使用BottomEnd对齐，大小60dp
    // 悬浮窗右下角在 (parentWidth + offsetX, parentHeight + offsetY)
    // 悬浮窗左边缘在 (parentWidth + offsetX - 60dp, ...)
    // 对话框右边缘应该在悬浮窗左边缘左边6dp
    // 对话框右边缘位置 = parentWidth + offsetX - 60dp - 6dp = parentWidth + offsetX - 66dp
    // 对话框也使用BottomEnd对齐，所以 dialogOffsetX = offsetX - 66dp
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
                .background(Color.White.copy(alpha = 0.8f)),
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
                        detectTapGestures(
                            onTap = { onDismiss() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "×",
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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
                // 状态指示器
                if (isListening || isThinking || isSpeaking) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val statusText = when {
                            isListening -> "正在聆听..."
                            isThinking -> "思考中..."
                            isSpeaking -> "回答中..."
                            else -> ""
                        }
                        Text(
                            text = statusText,
                            color = Color(0xFF2DBA98),
                            fontSize = 12.sp
                        )
                    }
                }

                // 用户文本
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

                // AI回答文本
                if (aiText.isNotEmpty()) {
                    Column {
                        Text(text = "AI：", color = Color(0xFF2DBA98), fontSize = 10.sp)
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
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave3"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing, delayMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha3"
    )

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale1)
                .clip(CircleShape)
                .background(Color(0xFF2DBA98).copy(alpha = alpha1))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale2)
                .clip(CircleShape)
                .background(Color(0xFF2DBA98).copy(alpha = alpha2))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale3)
                .clip(CircleShape)
                .background(Color(0xFF2DBA98).copy(alpha = alpha3))
        )
    }
}

/**
 * 视频播放器组件
 */
@Composable
private fun VideoPlayer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isReady by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val exoPlayer = remember {
        ExoPlayerManager.getOrCreate(context).apply {
            val mediaItem = MediaItem.fromUri(
                Uri.parse("android.resource://${context.packageName}/${R.raw.floating_video}")
            )
            setMediaItem(mediaItem)
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            volume = 0f
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> { isLoading = true }
                        Player.STATE_READY -> { isReady = true; isLoading = false }
                        Player.STATE_IDLE -> { isLoading = false }
                        Player.STATE_ENDED -> { isLoading = false }
                    }
                }
            })
            prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.playWhenReady = false }
    }

    Box(modifier = modifier) {
        if (isReady) {
            androidx.compose.ui.viewinterop.AndroidView(
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
                    .background(Color(0xFF2DBA98)),
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
