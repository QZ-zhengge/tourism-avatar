package com.example.let_us_go.ui.pages.live_guide

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.ui.pages.digital_human.ImageRecognitionOverlay

/**
 * 实时讲解流程页
 *
 * 管理两个阶段：
 * 1. 图片选择（拍照/相册）
 * 2. AI 识别结果展示
 */
@Composable
fun LiveGuideFlowScreen() {
    val viewModel: LiveGuideViewModel = viewModel()
    val description by viewModel.description.collectAsState()
    val speechText by viewModel.speechText.collectAsState()
    val currentBitmap by viewModel.currentBitmap.collectAsState()
    val isRecognizing by viewModel.isRecognizing.collectAsState()

    // 当前阶段：SELECT = 选择图片，RECOGNIZING = 识别中/展示结果
    var stage by remember { mutableStateOf("SELECT") }

    when (stage) {
        "SELECT" -> {
            LiveGuidePhotoScreen(
                onPhotoSelected = { bitmap ->
                    viewModel.recognizeImage(bitmap)
                    stage = "RECOGNIZING"
                }
            )
        }
        "RECOGNIZING" -> {
            val bmp = currentBitmap
            if (bmp != null) {
                ImageRecognitionOverlay(
                    photoBitmap = bmp,
                    description = description,
                    speechText = speechText,
                    onBack = {
                        viewModel.clearResult()
                        stage = "SELECT"
                    },
                    onRetakeCamera = {
                        // 重新选择图片（清空当前结果，回到选择页）
                        viewModel.clearResult()
                        stage = "SELECT"
                    },
                    onPickGallery = {
                        // 重新选择图片（清空当前结果，回到选择页）
                        viewModel.clearResult()
                        stage = "SELECT"
                    }
                )
            } else {
                // 异常情况：没有图片，回到选择页
                stage = "SELECT"
            }
        }
    }
}
