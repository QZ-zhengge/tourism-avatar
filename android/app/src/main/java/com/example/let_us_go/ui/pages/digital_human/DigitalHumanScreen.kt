package com.example.let_us_go.ui.pages.digital_human

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.data.model.AvatarSession
import com.example.let_us_go.data.model.ChatMessage
import com.example.let_us_go.ui.components.HotelCardList
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.util.rememberCameraPermission
import com.example.let_us_go.util.rememberImagePicker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import java.io.ByteArrayOutputStream
import java.io.File
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.BrandSecondary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/** 主页「实时讲解」跳转数字人页后，自动弹出拍照识别选择器 */
object DigitalHumanLaunchIntent {
    var openPhotoSheetOnEnter: Boolean = false
}

/**
 * 检测软键盘是否真正弹出（adjustResize / adjustPan 下都可靠）。
 *
 * 旧版 Compose Foundation 的 WindowInsets.isImeVisible 在本项目不可用；
 * 若直接替换 ComposeView 的 OnApplyWindowInsetsListener 又会破坏 Compose 自身
 * 的 statusBars / imePadding 等 inset 处理。故改用 ViewTreeObserver 观察布局变化：
 * 比对「物理屏幕高度」与「窗口可见区域底部」，差值即键盘高度（adjustResize 下窗口会
 * 缩小，但物理屏幕高度恒定，因此差值仍能正确反映键盘显隐）。它驱动数字人形象区的
 * 收起/恢复，避免「收起键盘后数字人卡在消失态」。
 */
@Composable
private fun rememberIsKeyboardVisible(): Boolean {
    val view = LocalView.current
    var isVisible by remember { mutableStateOf(false) }
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)
            if (rect.bottom <= 0) return@OnGlobalLayoutListener
            val screenHeight = view.context.resources.displayMetrics.heightPixels
            val keyboardHeight = screenHeight - rect.bottom
            // 阈值取屏幕高度的 15%，避开导航栏等少量误差
            isVisible = keyboardHeight > screenHeight * 0.15
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose { view.viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
    return isVisible
}

/**
 * 数字人页（Phase 2 重构）
 *
 * - 播放器：[androidx.media3.ui.PlayerView] 嵌入 ExoPlayer
 * - 状态机：IDLE → STARTING → READY → CONNECTING
 * - 回退：stream_url 失败 → 自动试 play_url
 *
 * UI 结构（与 uni 端一致）：
 *  1. 顶部导航（返回 + 标题 + 设置）
 *  2. 数字人形象区（ExoPlayer + 状态层 + 重试层）
 *  3. 快捷操作（拍照识别/麦克风/切换形象）
 *  4. 对话区（消息流）
 *  5. 输入栏
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DigitalHumanScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    val viewModel: DigitalHumanViewModel = viewModel()
    val status by viewModel.status.collectAsState()
    val session by viewModel.session.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val error by viewModel.error.collectAsState()
    val retryAttempt by viewModel.retryAttempt.collectAsState()
    val isFullScreen by viewModel.isFullScreen.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()

    // 持有 ExoPlayer 实例（整个屏幕生命周期）
    val playerHolder = remember {
        com.example.let_us_go.core.playback.DigitalHumanPlayerHolder(context)
    }

    // 把 player 暴露给 ViewModel
    LaunchedEffect(playerHolder) {
        viewModel.bindPlayer(playerHolder)
    }

    var inputText by remember { mutableStateOf("") }

    // 键盘是否弹出
    val imeVisible = rememberIsKeyboardVisible()

    // 录音权限请求
    var hasMicPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.toggleRecord()
        }
    }

    // ───── 拍照识别：选择器 + 相册/相机 ─────
    var showPhotoSheet by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCamera by remember { mutableStateOf(false) }
    val photoSheetState = rememberModalBottomSheetState()
    val recognizing by viewModel.recognizing.collectAsState()

    // 相册选图 → 直接识别并发送给数字人（不跳转页面）
    val pickImage = rememberImagePicker { uri ->
        uri ?: return@rememberImagePicker
        val bytes = uriToJpegBytes(context, uri)
        if (bytes.isNotEmpty()) {
            viewModel.recognizeAndDriveText(bytes)
            showPhotoSheet = false
        }
    }

    // 拍照 → 直接识别并发送给数字人（不跳转页面）
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = cameraUri
        cameraUri = null
        if (ok && uri != null) {
            val bytes = uriToJpegBytes(context, uri)
            if (bytes.isNotEmpty()) {
                viewModel.recognizeAndDriveText(bytes)
                showPhotoSheet = false
            }
        }
    }
    val cameraPermission = rememberCameraPermission()
    val authority = "${context.packageName}.fileprovider"
    fun launchCameraCapture() {
        val file = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, authority, file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }
    // 相机权限授予后自动拉起拍照
    LaunchedEffect(cameraPermission.status) {
        if (pendingCamera && cameraPermission.status is PermissionStatus.Granted) {
            pendingCamera = false
            launchCameraCapture()
        }
    }
    fun onTakePhoto() {
        if (cameraPermission.status is PermissionStatus.Granted) {
            launchCameraCapture()
        } else {
            pendingCamera = true
            cameraPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.start()
        if (DigitalHumanLaunchIntent.openPhotoSheetOnEnter) {
            DigitalHumanLaunchIntent.openPhotoSheetOnEnter = false
            showPhotoSheet = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stop()
            playerHolder.release()
        }
    }

    // 导航目标状态
    var navTarget by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        if (isFullScreen) {
            // ═══════════════ 全屏模式 ═══════════════
            // 数字人视频占满屏幕，下方只有3个按钮
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // 1. 顶部导航栏
                TopBar(
                    onBack = { nav.navigateBack() }
                )

                // 2. 数字人视频区域（占满剩余空间）+ 心情面板
                Box(modifier = Modifier.weight(1f)) {
                    HumanStageFullScreen(
                        session = session,
                        status = status,
                        playerHolder = playerHolder,
                        errorMsg = error,
                        retryAttempt = retryAttempt,
                        onRetry = { viewModel.start() },
                        modifier = Modifier.fillMaxSize()
                    )
                    // 心情/活力小面板（右上角，不遮挡数字人脸）
                    val mood by viewModel.mood.collectAsState()
                    val energy by viewModel.energy.collectAsState()
                    val showDetail by viewModel.showDetail.collectAsState()
                    MoodEnergyPanel(
                        mood = mood,
                        energy = energy,
                        showDetail = showDetail,
                        onToggle = { viewModel.toggeDetail() }
                    )
                }

                // 3. 底部3个按钮
               FullScreenBottomBar(
                    onPhoto = { showPhotoSheet = true },
                    onMic = {
                        if (hasMicPermission) {
                            viewModel.toggleRecord()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    isRecording = isRecording,
                    isThinking = isThinking,
                    onClose = { viewModel.exitFullScreen() }
                )
            }
        } else {
            // ═══════════════ 普通模式 ═══════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // 1. 顶部
                TopBar(
                    onBack = { nav.navigateBack() }
                )

                // 2. 导航目标提示条（只在有导航目标时显示）
                navTarget?.let { target ->
                    NavigationTargetBar(
                        targetName = target,
                        onCancel = { navTarget = null }
                    )
                }

                // 3. 数字人形象区 + 心情面板
                Box {
                    HumanStage(
                        session = session,
                        status = status,
                        playerHolder = playerHolder,
                        errorMsg = error,
                        retryAttempt = retryAttempt,
                        onCameraTap = { showPhotoSheet = true },
                        onRetry = { viewModel.start() },
                        collapsed = imeVisible
                    )
                    // 心情/活力小面板（右上角）
                    val mood2 by viewModel.mood.collectAsState()
                    val energy2 by viewModel.energy.collectAsState()
                    val showDetail2 by viewModel.showDetail.collectAsState()
                    MoodEnergyPanel(
                        mood = mood2,
                        energy = energy2,
                        showDetail = showDetail2,
                        onToggle = { viewModel.toggeDetail() }
                    )
                }

                // 4. 状态动画（录音/思考/播放时显示）
                if (isRecording) {
                    StatusAnimation()
                }

                // 5. 快捷操作
                QuickActionsRow(
                    onPhoto = { showPhotoSheet = true },
                    onMic = {
                        if (hasMicPermission) {
                            viewModel.toggleRecord()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    isRecording = isRecording,
                    onClose = { viewModel.enterFullScreen() }
                )

                // 5.1 拍照识别进行中提示（不影响数字人流）
                if (recognizing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusAnimation()
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "拍照识别中…",
                            fontSize = 12.sp,
                            color = BrandAccent
                        )
                    }
                }

                // 6. 对话区
                ChatPanel(
                    messages = messages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // 7. 输入栏
                ChatInputBar(
                    text = inputText,
                    onTextChange = { inputText = it },
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendText(inputText)
                            inputText = ""
                        }
                    },
                    onMic = { viewModel.toggleRecord() }
                )
            }
        }

        // 8. 拍照识别选择器（底部弹层，不中断数字人流）
        if (showPhotoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPhotoSheet = false },
                sheetState = photoSheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    Text(
                        text = "拍照识别",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    PhotoSheetItem(
                        icon = Icons.Outlined.Image,
                        label = "从相册选取",
                        onClick = {
                            showPhotoSheet = false
                            pickImage()
                        }
                    )
                    PhotoSheetItem(
                        icon = Icons.Outlined.Camera,
                        label = "拍摄",
                        onClick = {
                            showPhotoSheet = false
                            onTakePhoto()
                        }
                    )
                    PhotoSheetItem(
                        icon = Icons.Outlined.Close,
                        label = "取消",
                        onClick = { showPhotoSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(BgPage),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "数字人导游",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun HumanStage(
    session: AvatarSession?,
    status: DigitalHumanViewModel.DhStatus,
    playerHolder: com.example.let_us_go.core.playback.DigitalHumanPlayerHolder,
    errorMsg: String?,
    retryAttempt: Int,
    onCameraTap: () -> Unit,
    onRetry: () -> Unit,
    /** 键盘弹出（rememberIsKeyboardVisible）时收起形象区，给输入栏腾出空间，键盘收起即恢复 */
    collapsed: Boolean = false
) {
    val playbackState by playerHolder.playbackState.collectAsState()
    val playerError by playerHolder.errorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (collapsed) 0.dp else 260.dp)
            .background(Color.Black)
    ) {
        // ExoPlayer 视频播放器（永远渲染，IDLE 时不加载任何源）
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false  // 自定义控制条
                    player = playerHolder.player
                    // 只在非 READY 状态显示 buffering 指示器
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                }
            },
            update = { view ->
                view.player = playerHolder.player
            },
            modifier = Modifier.fillMaxSize()
        )

        // 未启动时显示渐变 + 光球
        if ((status != DigitalHumanViewModel.DhStatus.READY && status != DigitalHumanViewModel.DhStatus.CLOSED) || playbackState == Player.STATE_IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BrandAccent.copy(alpha = 0.4f), Color.Black)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(BrandAccent, BrandAccent.copy(alpha = 0.1f))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (status) {
                            DigitalHumanViewModel.DhStatus.IDLE -> "点击下方按钮启动"
                            DigitalHumanViewModel.DhStatus.STARTING ->
                                if (retryAttempt > 1) "正在重试 ($retryAttempt/3)…"
                                else "数字人启动中…"
                            DigitalHumanViewModel.DhStatus.READY -> "智能助手已就绪"
                            DigitalHumanViewModel.DhStatus.FAILED -> "启动失败，点击重试"
                            DigitalHumanViewModel.DhStatus.CLOSED -> ""
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 启动中遮罩（只在 STARTING 状态显示，READY 状态不显示加载遮罩）
        if (status == DigitalHumanViewModel.DhStatus.STARTING && status != DigitalHumanViewModel.DhStatus.CLOSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏳", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (status) {
                            DigitalHumanViewModel.DhStatus.STARTING ->
                                if (retryAttempt > 1) "正在重试 ($retryAttempt/3)…"
                                else "正在启动数字人…"
                            else -> "加载中…"
                        },
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // 失败层
        if ((status == DigitalHumanViewModel.DhStatus.FAILED || playerError != null) && status != DigitalHumanViewModel.DhStatus.CLOSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onRetry() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("!", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg ?: playerError ?: "数字人展示失败",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandAccent)
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                    Text("点击重试", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // 已关闭会话层：显示「数字人已关闭，是否重连」
        if (status == DigitalHumanViewModel.DhStatus.CLOSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "数字人已关闭",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "是否重新连接？",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BrandAccent)
                            .clickable { onRetry() }
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text("重新连接", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
}

@Composable
private fun QuickActionsRow(
    onPhoto: () -> Unit,
    onMic: () -> Unit,
    isRecording: Boolean,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPage)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickAction(
            icon = Icons.Outlined.Camera,
            label = "拍照识别",
            bgColor = BrandPrimary
        ) { onPhoto() }
        QuickAction(
            icon = if (isRecording) Icons.Outlined.Stop else Icons.Outlined.Mic,
            label = if (isRecording) "松开结束" else "按住说话",
            bgColor = if (isRecording) com.example.let_us_go.ui.theme.BrandDanger else BrandAccent
        ) { onMic() }
        QuickAction(
            icon = Icons.Outlined.Fullscreen,
            label = "进入全屏",
            bgColor = BrandAccent,
            bgAlpha = 0.15f,
            iconColor = BrandAccent
        ) { onClose() }
    }
}

/**
 * 全屏模式下的数字人视频区域（占满屏幕，无黑边）
 */
@Composable
private fun HumanStageFullScreen(
    session: AvatarSession?,
    status: DigitalHumanViewModel.DhStatus,
    playerHolder: com.example.let_us_go.core.playback.DigitalHumanPlayerHolder,
    errorMsg: String?,
    retryAttempt: Int,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackState by playerHolder.playbackState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        // ExoPlayer 视频播放器（全屏，使用 ZOOM 模式消除黑边）
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    player = playerHolder.player
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    // 设置缩放模式为 ZOOM，消除黑边
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { view ->
                view.player = playerHolder.player
            },
            modifier = Modifier.fillMaxSize()
        )

        // 未启动时显示渐变 + 光球
        if ((status != DigitalHumanViewModel.DhStatus.READY && status != DigitalHumanViewModel.DhStatus.CLOSED) || playbackState == Player.STATE_IDLE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BrandAccent.copy(alpha = 0.4f), Color.Black)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(BrandAccent, BrandAccent.copy(alpha = 0.1f))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (status) {
                            DigitalHumanViewModel.DhStatus.IDLE -> "点击下方按钮启动"
                            DigitalHumanViewModel.DhStatus.STARTING ->
                                if (retryAttempt > 1) "正在重试 ($retryAttempt/3)…"
                                else "数字人启动中…"
                            DigitalHumanViewModel.DhStatus.READY -> "智能助手已就绪"
                            DigitalHumanViewModel.DhStatus.FAILED -> "启动失败，点击重试"
                            DigitalHumanViewModel.DhStatus.CLOSED -> ""
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // 启动中遮罩
        if (status == DigitalHumanViewModel.DhStatus.STARTING && status != DigitalHumanViewModel.DhStatus.CLOSED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        // 失败时显示重试按钮
        if (status == DigitalHumanViewModel.DhStatus.FAILED) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onRetry() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "重试",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击重试",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * 全屏模式底部按钮栏
 */
@Composable
private fun FullScreenBottomBar(
    onPhoto: () -> Unit,
    onMic: () -> Unit,
    isRecording: Boolean,
    isThinking: Boolean,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // AI思考中提示（仅在思考状态下显示）
        if (isThinking) {
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF4CAF50),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI正在思考...",
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 拍照识别按钮（思考时禁用）
            FullScreenButton(
                icon = Icons.Outlined.CameraAlt,
                onClick = { if (!isThinking) onPhoto() },
                bgColor = if (isThinking) Color.Gray.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f),
                enabled = !isThinking
            )

            // 按住说话按钮（思考时禁用）
            FullScreenButton(
                icon = if (isRecording) Icons.Outlined.Stop else Icons.Outlined.Mic,
                onClick = { if (!isThinking) onMic() },
                bgColor = when {
                    isRecording -> Color(0xFFE53935).copy(alpha = 0.8f)
                    isThinking -> Color.Gray.copy(alpha = 0.3f)
                    else -> Color.White.copy(alpha = 0.2f)
                },
                size = 64,
                enabled = !isThinking
            )

            // 关闭全屏按钮（始终可用）
            FullScreenButton(
                icon = Icons.Outlined.Close,
                onClick = onClose,
                bgColor = Color(0xFFE53935).copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 全屏模式圆形按钮
 */
@Composable
private fun FullScreenButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    bgColor: Color,
    size: Int = 52,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

@Composable
private fun QuickAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    iconColor: Color? = null,
    bgAlpha: Float = 0.15f,
    onTap: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onTap() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor.copy(alpha = bgAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor ?: bgColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun ChatPanel(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    if (messages.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("💬", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "试试说：带我游览一下这里",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg = msg)
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    // 用 Box + contentAlignment 保证：用户消息靠右、AI 消息靠左（不依赖 Row arrangement 歧义）
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.widthIn(max = 320.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                // 显示图片（如果有）
                if (!msg.imagePath.isNullOrBlank()) {
                    val imageFile = java.io.File(msg.imagePath)
                    if (imageFile.exists()) {
                        Card(
                            modifier = Modifier.width(160.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = android.graphics.ImageDecoder.decodeBitmap(
                                    android.graphics.ImageDecoder.createSource(imageFile)
                                ).asImageBitmap(),
                                contentDescription = "拍摄的图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 100.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // 文本内容
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) BrandPrimary else BgCard
                    ),
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                ) {
                    Text(
                        text = msg.content,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp
                    )
                }
                if (msg.cards?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HotelCardList(cards = msg.cards!!)
                }
            }
            if (isUser) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("我", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onMic: () -> Unit
) {
    // 外层 Row：背景延伸到键盘 / 系统导航条区，消除「输入框与键盘之间的浅色缝隙」
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
    ) {
        // 内层 Row：内容避开系统导航条与软键盘
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("和数字人聊点什么…") },
                modifier = Modifier
                    .weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandAccent)
                    .clickable { onMic() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "语音",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary)
                    .clickable { onSend() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "发送",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NavigationTargetBar(
    targetName: String,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2196F3).copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⊙",
            fontSize = 16.sp,
            color = Color(0xFF2196F3)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "正在导航至：$targetName",
            fontSize = 14.sp,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "×", fontSize = 16.sp, color = Color(0xFF2196F3))
        }
    }
}

@Composable
private fun StatusAnimation() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BrandAccent)
            )
        }
    }
}

@Composable
private fun PhotoSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 15.sp, color = TextPrimary)
    }
}

/**
 * 把 content:// 或 file:// 图片 URI 读取为压缩后的 JPEG 字节，
 * 用于拍照识别（控制体积，避免长时间请求与超大 base64）。
 *
 * - 先按原始尺寸计算 inSampleSize 降采样
 * - 若最长边仍超过 [maxDim] 再缩放一次
 * - 以 [quality] 压缩为 JPEG
 */
private fun uriToJpegBytes(
    context: android.content.Context,
    uri: Uri,
    maxDim: Int = 1280,
    quality: Int = 82
): ByteArray {
    val resolver = context.contentResolver

    // 1. 读取原始尺寸
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val (w, h) = bounds.outWidth to bounds.outHeight
    val scale = if (w > 0 && h > 0) {
        (maxOf(w, h).toFloat() / maxDim).coerceAtLeast(1f)
    } else 1f

    // 2. 降采样加载
    val opts = BitmapFactory.Options().apply { inSampleSize = scale.toInt().coerceAtLeast(1) }
    val src = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: return ByteArray(0)

    // 3. 二次缩放，确保最长边 ≤ maxDim
    val dst = if (maxOf(src.width, src.height) > maxDim) {
        val ratio = maxDim.toFloat() / maxOf(src.width, src.height)
        val tw = (src.width * ratio).toInt().coerceAtLeast(1)
        val th = (src.height * ratio).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(src, tw, th, true)
    } else {
        src
    }

    val out = ByteArrayOutputStream()
    dst.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (dst != src) dst.recycle()
    src.recycle()
    return out.toByteArray()
}

// ────────── 心情/活力面板 ──────────

/**
 * 心情 & 活力迷你面板
 *
 * - 默认折叠：右上角两个小圆角矩形条，「心情：93」「活力：97」
 * - 点击展开：弹出窗口显示柱状图
 * - 半透明绿色，尽量不遮挡数字人
 */
@Composable
private fun BoxScope.MoodEnergyPanel(
    mood: Int,
    energy: Int,
    showDetail: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // 心情小标签
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x6600C853))
                .clickable { onToggle() }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("😊", fontSize = 10.sp)
            Text(" $mood", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // 活力小标签
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x6600C853))
                .clickable { onToggle() }
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚡", fontSize = 10.sp)
            Text(" $energy", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    // 展开详情弹窗
    if (showDetail) {
        DetailPopup(mood = mood, energy = energy, onDismiss = onToggle)
    }
}

@Composable
private fun DetailPopup(mood: Int, energy: Int, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1B2E1B),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("实时数据", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "✕",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 心情柱状图
                Text("😊 心情指数", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                BarChart(value = mood, max = 100, color = Color(0xFF4CAF50))

                Spacer(modifier = Modifier.height(4.dp))

                // 活力柱状图
                Text("⚡ 对话活力", color = Color(0xFF8BC34A), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                BarChart(value = energy, max = 100, color = Color(0xFF8BC34A))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Color(0xFF4CAF50))
            }
        },
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun BarChart(value: Int, max: Int, color: Color) {
    val fraction = (value.coerceIn(0, max)).toFloat() / max
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color, color.copy(alpha = 0.6f))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "$value",
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
