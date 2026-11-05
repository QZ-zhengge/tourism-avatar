package com.example.let_us_go.ui.pages.live_guide

import android.graphics.Bitmap
import android.util.Log
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 实时讲解 ViewModel
 *
 * 处理图片识别逻辑，独立于数字人页。
 */
class LiveGuideViewModel : BaseViewModel() {

    /** 识别结果 */
    private val _description = MutableStateFlow<String?>(null)
    val description: StateFlow<String?> = _description.asStateFlow()

    private val _speechText = MutableStateFlow<String?>(null)
    val speechText: StateFlow<String?> = _speechText.asStateFlow()

    /** 识别中的状态 */
    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    /** 当前识别的图片 */
    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    /**
     * 开始识别图片
     *
     * @param bitmap 选中/拍摄的图片
     */
    fun recognizeImage(bitmap: Bitmap) {
        _currentBitmap.value = bitmap
        _description.value = null
        _speechText.value = null
        _isRecognizing.value = true

        launchIo(showLoading = false) {
            try {
                // 将 bitmap 转为 JPEG 字节数组
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val imageBytes = outputStream.toByteArray()

                // 构建 multipart 请求
                val filePart = MultipartBody.Part.createFormData(
                    "file", "live_guide.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                val sessionId = "live_guide_${System.currentTimeMillis()}"
                val sessionPart = sessionId.toRequestBody("text/plain".toMediaType())
                val scenicPart = "lingshan".toRequestBody("text/plain".toMediaType())

                Log.d("LiveGuideVM", "Calling image-recognition API, imageBytes=${imageBytes.size}")
                val res = NetworkModule.guideApi.imageRecognize(
                    file = filePart,
                    sessionId = sessionPart,
                    scenicId = scenicPart
                )

                val answer = res.answerText()
                val speech = res.speechText()

                if (answer.isNotBlank()) {
                    _description.value = answer
                    _speechText.value = speech
                    Log.d("LiveGuideVM", "Recognition success: ${answer.take(50)}...")
                } else {
                    _description.value = "抱歉，未能识别出图片内容。"
                    Log.w("LiveGuideVM", "Recognition returned empty")
                }
            } catch (e: Throwable) {
                Log.e("LiveGuideVM", "Recognition failed", e)
                _description.value = "识别失败：${e.message ?: "未知错误"}"
            } finally {
                _isRecognizing.value = false
            }
        }
    }

    /**
     * 清除识别结果（重新选择图片时调用）
     */
    fun clearResult() {
        _description.value = null
        _speechText.value = null
        _currentBitmap.value = null
        _isRecognizing.value = false
    }
}
