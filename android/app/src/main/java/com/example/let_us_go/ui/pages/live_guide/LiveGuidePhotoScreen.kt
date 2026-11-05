package com.example.let_us_go.ui.pages.live_guide

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.let_us_go.LocalNav
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import com.example.let_us_go.util.rememberCameraPermission
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import java.io.File

/**
 * 实时讲解 - 图片选择页
 *
 * 用户选择拍照或从相册选取图片，然后进入 AI 识别页。
 * UI 风格与整个应用保持一致。
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LiveGuidePhotoScreen(
    onPhotoSelected: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val nav = LocalNav.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCamera by remember { mutableStateOf(false) }

    // 相册选图
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = uriToJpegBytes(context, uri)
        if (bytes.isNotEmpty()) {
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            onPhotoSelected(bmp)
        }
    }

    // 拍照
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        val uri = cameraUri
        cameraUri = null
        if (ok && uri != null) {
            val bytes = uriToJpegBytes(context, uri)
            if (bytes.isNotEmpty()) {
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                onPhotoSelected(bmp)
            }
        }
    }

    val cameraPermission = rememberCameraPermission()
    val authority = "${context.packageName}.fileprovider"

    fun launchCameraCapture() {
        val file = File(context.cacheDir, "live_guide_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, authority, file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // 顶栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { nav.navigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = TextPrimary, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "实时讲解",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 顶部图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandAccent, BrandPrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📷", fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = "选择图片来源",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 说明文字
            Text(
                text = "拍摄或选择一张照片，AI 将为您识别并讲解",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 拍照按钮
            OptionCard(
                icon = Icons.Outlined.CameraAlt,
                title = "拍照",
                subtitle = "使用相机拍摄照片",
                gradient = listOf(
                    Color(0xFF4CAF50).copy(alpha = 0.15f),
                    Color(0xFF2DBA98).copy(alpha = 0.15f)
                ),
                iconColor = Color(0xFF4CAF50),
                onClick = { onTakePhoto() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 相册按钮
            OptionCard(
                icon = Icons.Outlined.PhotoLibrary,
                title = "从相册选取",
                subtitle = "选择已有的照片",
                gradient = listOf(
                    Color(0xFF2196F3).copy(alpha = 0.15f),
                    Color(0xFF1976D2).copy(alpha = 0.15f)
                ),
                iconColor = Color(0xFF2196F3),
                onClick = {
                    pickImage.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 提示文字
            Text(
                text = "支持 JPG、PNG 格式，建议清晰拍摄",
                fontSize = 12.sp,
                color = TextTertiary
            )
        }
    }
}

/**
 * 选项卡片
 */
@Composable
private fun OptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    gradient: List<Color>,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(colors = gradient)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文字
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // 箭头
            Text(
                text = "→",
                fontSize = 20.sp,
                color = TextTertiary
            )
        }
    }
}

/**
 * URI 转 JPEG 字节数组
 */
private fun uriToJpegBytes(context: android.content.Context, uri: Uri): ByteArray {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return byteArrayOf()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        byteArrayOf()
    }
}
