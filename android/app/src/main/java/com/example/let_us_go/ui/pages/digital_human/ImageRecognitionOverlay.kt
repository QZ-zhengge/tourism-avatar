package com.example.let_us_go.ui.pages.digital_human

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import kotlinx.coroutines.delay
import kotlin.random.Random
import java.util.Locale

/**
 * 拍照识别结果覆盖页（实时讲解）
 *
 * 音频方案：【照搬悬浮窗 FloatChatManager 的 TTS 逻辑】
 * - 整段朗读，不做分句；onDone → 停止；onError → 停止
 * - 进度条由「已过时间 / 估算总时长」驱动
 * - 暂停 → tts.stop()；恢复 → 从当前字符位置重新朗读剩余文本
 * - 退出 → tts.stop() + tts.shutdown()
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageRecognitionOverlay(
    photoBitmap: Bitmap,
    description: String?,
    speechText: String?,
    onBack: () -> Unit,
    onRetakeCamera: () -> Unit,
    onPickGallery: () -> Unit
) {
    val context = LocalContext.current

    // 加载态：后端数据尚未返回（description 为 null 或空）→ 文本区显示「AI 正在思考中」
    val isLoading = description.isNullOrBlank()

    // 实际朗读的文本：优先 speechText，空则回退 description
    val speakText = (speechText ?: "").ifBlank { description ?: "" }
    val totalChars = speakText.length.coerceAtLeast(1)
    val durationMs = remember(speakText) { estimateDurationMs(speakText).coerceAtLeast(1L) }

    // ── 播放状态 ──
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    // 播放起始时间（用于进度估算）
    var playStartMs by remember { mutableLongStateOf(0L) }
    // 暂停时已播放的字符数（用于恢复朗读剩余文本）
    var pausedCharPos by remember { mutableLongStateOf(0L) }

    // 波形动画数据（20 根竖条）
    var waveHeights by remember { mutableStateOf(List(20) { 3 }) }

    // ── TTS 引擎（照搬 FloatChatManager 的初始化方式）──
    var ttsReady by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
                tts?.setSpeechRate(1.0f)
                ttsReady = true
                Log.d("ImageRecOverlay", "TTS initialized successfully")
            } else {
                Log.e("ImageRecOverlay", "TTS initialization failed")
            }
        }
    }

    // ── 照搬 FloatChatManager 的 speakAll：整段朗读，不做分句 ──
    fun speakAll(text: String) {
        if (!ttsReady || text.isBlank()) return
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "overlay_tts_${System.currentTimeMillis()}")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "overlay_tts")
    }

    // ── 播放控制 ──
    fun startPlayback() {
        if (speakText.isBlank()) return
        isPlaying = true
        hasStarted = true
        currentPositionMs = 0L
        sliderPosition = 0f
        pausedCharPos = 0
        playStartMs = System.currentTimeMillis()
        speakAll(speakText)
    }

    fun pausePlayback() {
        if (!isPlaying) return
        isPlaying = false
        tts?.stop()
        // 记录暂停时的字符位置（用于恢复）
        pausedCharPos = ((currentPositionMs.toFloat() / durationMs) * totalChars).toLong().coerceIn(0L, totalChars.toLong())
    }

    fun resumePlayback() {
        if (isPlaying || speakText.isBlank()) return
        isPlaying = true
        // 从暂停位置朗读剩余文本
        val startPos = pausedCharPos.toInt().coerceIn(0, speakText.length)
        val remaining = speakText.substring(startPos)
        if (remaining.isBlank()) {
            isPlaying = false
            return
        }
        playStartMs = System.currentTimeMillis() - pausedCharPos * durationMs / totalChars
        speakAll(remaining)
    }

    fun togglePlayback() {
        if (isPlaying) pausePlayback()
        else if (!hasStarted) startPlayback()
        else resumePlayback()
    }

    /** 拖动 seek：停止当前朗读，从目标位置重新朗读剩余文本 */
    fun seekToFraction(frac: Float) {
        val f = frac.coerceIn(0f, 1f)
        val targetChars = (f * totalChars).toLong().coerceIn(0L, totalChars.toLong())
        currentPositionMs = (f * durationMs).toLong()
        sliderPosition = f
        pausedCharPos = targetChars
        if (isPlaying) {
            tts?.stop()
            val startPos = targetChars.toInt().coerceIn(0, speakText.length)
            val remaining = speakText.substring(startPos)
            if (remaining.isBlank()) {
                isPlaying = false
                return
            }
            playStartMs = System.currentTimeMillis() - targetChars * durationMs / totalChars
            speakAll(remaining)
        }
    }

    // ── TTS 回调（照搬 FloatChatManager 的 onDone / onError 模式）──
    DisposableEffect(Unit) {
        val listener = object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                Log.d("ImageRecOverlay", "onDone: utteranceId=$utteranceId, isPlaying=$isPlaying")
                isPlaying = false
                currentPositionMs = durationMs
                sliderPosition = 1f
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.w("ImageRecOverlay", "onError: utteranceId=$utteranceId")
                isPlaying = false
            }
        }
        tts?.setOnUtteranceProgressListener(listener)

        onDispose {
            try { tts?.stop() } catch (_: Throwable) {}
            try { tts?.shutdown() } catch (_: Throwable) {}
            Log.d("ImageRecOverlay", "本地 TTS 已真实停止并释放")
        }
    }

    // ── 初始化：TTS 就绪且数据到达后自动开始朗读 ──
    LaunchedEffect(ttsReady, speakText) {
        if (speakText.isBlank()) {
            if (isPlaying) { try { tts?.stop() } catch (_: Throwable) {} }
            isPlaying = false
            hasStarted = false
            currentPositionMs = 0L
            sliderPosition = 0f
            return@LaunchedEffect
        }
        if (ttsReady && !hasStarted) {
            startPlayback()
        }
    }

    // ── 进度推进：按已过时间 / 估算总时长 驱动进度条 ──
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            delay(100L)
            val elapsed = System.currentTimeMillis() - playStartMs
            val estimatedTotal = durationMs
            currentPositionMs = elapsed.coerceAtMost(estimatedTotal)
            sliderPosition = if (estimatedTotal > 0)
                (elapsed.toFloat() / estimatedTotal).coerceIn(0f, 1f) else 0f
            // 波形动画：播放时随机跳动
            waveHeights = List(20) { Random.nextInt(4, 22) }
        }
        // 暂停/结束时波形静止
        waveHeights = List(20) { 3 }
    }

    // ═══════════════════ UI 部分（不变）═══════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 1. 顶栏
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", color = TextPrimary, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("实时讲解", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF4CAF50)).padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("90", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 2. 照片
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context).data(photoBitmap).build(),
                contentDescription = "拍摄的照片",
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )

            // 3. 标签行
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("拍摄的照片", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(BrandAccent.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("AI 识别", color = BrandAccent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            // 4. 识别文本
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    if (isLoading) {
                        ThinkingIndicator()
                    } else {
                        Text(text = description ?: "未能识别出图片内容。", color = TextPrimary, fontSize = 14.sp, lineHeight = 22.sp)
                    }
                }
            }

            // 5. 音频播放器
            AudioPlayerBar(
                isPlaying = isPlaying,
                isLoading = isLoading,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                sliderPosition = sliderPosition,
                waveHeights = waveHeights,
                isDragging = isDragging,
                onTogglePlay = { if (!isLoading) togglePlayback() },
                onSeekStart = { isDragging = true },
                onSeekChange = { fraction ->
                    sliderPosition = fraction
                    currentPositionMs = (fraction * durationMs).toLong()
                },
                onSeekEnd = { fraction ->
                    isDragging = false
                    seekToFraction(fraction)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. 底部三按钮
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverlayActionButton(icon = Icons.Outlined.CameraAlt, label = "重新拍摄") { onRetakeCamera() }
                OverlayActionButton(icon = Icons.Outlined.ContentCopy, label = "复制文本") {
                    if (isLoading) return@OverlayActionButton
                    val text = description ?: ""
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("识别结果", text))
                    Log.d("ImageRecOverlay", "已复制文本 (${text.length} 字)")
                }
                OverlayActionButton(icon = Icons.Outlined.Image, label = "相册选图") { onPickGallery() }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ═══════════════════ 以下 UI 组件不变 ═══════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AudioPlayerBar(
    isPlaying: Boolean,
    isLoading: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    sliderPosition: Float,
    waveHeights: List<Int>,
    isDragging: Boolean,
    onTogglePlay: () -> Unit,
    onSeekStart: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekEnd: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp)
            .clip(RoundedCornerShape(26.dp)).background(BgCard).padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape)
                .background(if (isLoading) BrandAccent.copy(alpha = 0.35f) else BrandAccent)
                .clickable(enabled = !isLoading) { onTogglePlay() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                repeat(waveHeights.size) { idx ->
                    Box(
                        modifier = Modifier.width(3.dp).height((waveHeights.getOrNull(idx) ?: 3).dp)
                            .clip(RoundedCornerShape(1.5.dp)).background(TextTertiary.copy(alpha = 0.3f))
                    )
                }
            }
            Slider(
                value = sliderPosition,
                onValueChange = onSeekChange,
                onValueChangeFinished = { onSeekEnd(sliderPosition) },
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }.also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect { interaction ->
                            when (interaction) {
                                is androidx.compose.foundation.interaction.PressInteraction.Press -> onSeekStart()
                                is androidx.compose.foundation.interaction.PressInteraction.Release -> onSeekEnd(sliderPosition)
                            }
                        }
                    }
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent, activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent, disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent, disabledInactiveTrackColor = Color.Transparent
                ),
                thumb = {},
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = formatAudioTime(if (isDragging) (sliderPosition * durationMs).toLong() else currentPositionMs),
            color = TextSecondary, fontSize = 12.sp
        )
    }
}

@Composable
private fun ThinkingIndicator() {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(400); tick = (tick + 1) % 3 } }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("AI 正在思考中", color = TextSecondary, fontSize = 14.sp, lineHeight = 22.sp)
        Spacer(modifier = Modifier.width(4.dp))
        repeat(3) { i ->
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(TextSecondary.copy(alpha = if (i.toLong() == tick) 1f else 0.3f)))
            if (i < 2) Spacer(modifier = Modifier.width(3.dp))
        }
    }
}

@Composable
private fun OverlayActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(imageVector = icon, contentDescription = label, tint = TextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}

private fun formatAudioTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "$m:${if (s < 10) "0" else ""}$s"
}

/** 估算朗读时长（ms）：约 4 字/秒，下限 800ms */
private fun estimateDurationMs(text: String): Long {
    val chars = text.length.coerceAtLeast(1)
    return ((chars / 4.0) * 1000).toLong().coerceAtLeast(800L)
}
