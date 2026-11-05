package com.example.let_us_go.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

/**
 * 权限 / 系统选择器封装
 *
 * 替代 uni-app：
 * - `uni.chooseImage` → PickVisualMedia
 * - `uni.chooseMedia`（视频） → PickVisualMedia + video MIME
 * - `uni.makePhoneCall` → Intent.ACTION_DIAL
 * - `uni.openLocation` → 高德地图 Intent
 * - `uni.setClipboardData` → ClipboardManager
 * - 各种权限 → Accompanist Permissions
 */

/** ── 相册选图（替 uni.chooseImage） ── */
@Composable
fun rememberImagePicker(
    onResult: (uri: Uri?) -> Unit
): () -> Unit {
    val ctx = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> onResult(uri) }
    return remember {
        {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

/** ── 拍照（替 uni.chooseImage sourceType: camera） ── */
@Composable
fun rememberCameraLauncher(
    onResult: (uri: Uri?) -> Unit
): () -> Unit {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> onResult(if (success) null else null) }
    return remember { { launcher.launch(Uri.EMPTY) } }
}

/** ── 录音权限 ── */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberRecordAudioPermission(): PermissionState {
    return rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)
}

/** ── 相机权限 ── */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberCameraPermission(): PermissionState {
    return rememberPermissionState(android.Manifest.permission.CAMERA)
}

/** ── 位置权限 ── */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberLocationPermission(): com.google.accompanist.permissions.MultiplePermissionsState {
    return rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
}

/** ── 通知权限（Android 13+） ── */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberNotificationPermission(): PermissionState {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
    } else {
        // 低版本始终视为已授权
        object : PermissionState {
            override val permission: String = android.Manifest.permission.POST_NOTIFICATIONS
            override val status: com.google.accompanist.permissions.PermissionStatus =
                com.google.accompanist.permissions.PermissionStatus.Granted
            override fun launchPermissionRequest() = Unit
        }
    }
}

/** ── 拨号（替 uni.makePhoneCall） ── */
fun dialPhone(context: Context, phone: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

/** ── 复制到剪贴板（替 uni.setClipboardData） ── */
fun copyToClipboard(context: Context, text: String, label: String = "text") {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

/** ── 打开外部浏览器（替 plus.runtime.openURL） ── */
fun openExternalUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

/** ── 震动反馈（替 uni.vibrateShort） ── */
@Suppress("DEPRECATION")
fun vibrateShort(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(android.os.VibrationEffect.createOneShot(30, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator?.vibrate(30)
    }
}
