package com.example.uniapp_box.float

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.example.uniapp_box.data.ws.AsrWebSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 语音对话管理器（使用 ASR WebSocket，与数字人页面一致）
 *
 * 流程：
 * 1. 连接 ASR WebSocket
 * 2. 发送 config 帧
 * 3. 开始录音
 * 4. 实时将录音数据发送到 WebSocket
 * 5. 实时接收识别结果
 * 6. 识别完成后发送到 LLM
 * 7. TTS 播放回答
 */
object FloatChatManager {

    private const val TAG = "FloatChatManager"
    private const val ASR_URL = "http://192.0.2.1:50080"
    private const val GUIDE_URL = "http://192.0.2.1:50081"
    private const val SAMPLE_RATE = 16000
    private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    /** 对话状态 */
    enum class ChatState {
        IDLE,           // 空闲
        CONNECTING,     // 正在连接 ASR
        LISTENING,      // 正在录音
        THINKING,       // 正在思考（调用LLM）
        SPEAKING,       // 正在播放语音
        ERROR           // 出错
    }

    private val _chatState = MutableStateFlow(ChatState.IDLE)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private val _userText = MutableStateFlow("")
    val userText: StateFlow<String> = _userText.asStateFlow()

    private val _aiText = MutableStateFlow("")
    val aiText: StateFlow<String> = _aiText.asStateFlow()
    
    // 对话框显示状态
    private val _isDialogShowing = MutableStateFlow(false)
    val isDialogShowing: StateFlow<Boolean> = _isDialogShowing.asStateFlow()

    private var asrWebSocket: AsrWebSocket? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordingJob: Job? = null
    private var partialJob: Job? = null
    private var finalJob: Job? = null
    private var errorJob: Job? = null
    
    // TTS
    private var tts: android.speech.tts.TextToSpeech? = null
    private var isTtsInitialized = false

    private val sessionId: String = UUID.randomUUID().toString()

    /**
     * 开始语音对话
     */
    fun startChat(context: Context) {
        // 初始化TTS（如果未初始化）
        if (!isTtsInitialized) {
            initTts(context)
        }
        
        // 显示对话框
        _isDialogShowing.value = true
        
        updateState(ChatState.CONNECTING)
        startListening(context)
    }

    /**
     * 显示对话框
     */
    fun showDialog() {
        _isDialogShowing.value = true
    }

    /**
     * 关闭对话框
     */
    fun dismissDialog() {
        _isDialogShowing.value = false
        // 停止语音播放
        stopSpeaking()
        // 停止语音识别
        stopListening()
        // 更新状态为空闲
        updateState(ChatState.IDLE)
    }
    
    /**
     * 停止录音，开始回答
     */
    fun stopListeningAndStartAnswering() {
        // 停止录音
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Throwable) {
            Log.e(TAG, "Stop audio record failed", e)
        }
        recordingJob?.cancel()
        recordingJob = null
        
        // 发送结束帧到ASR WebSocket
        asrWebSocket?.sendEnd()
        
        // 状态会自动更新为THINKING，当finalText到达时会调用sendToLLM
    }
    
    /**
     * 停止回答，开始录音
     */
    fun stopAnsweringAndStartListening() {
        // 停止TTS播放
        stopSpeaking()
        
        // 重新开始录音
        updateState(ChatState.LISTENING)
        startRecording()
    }
    
    /**
     * 开始录音（不包括WebSocket连接）
     */
    private fun startRecording() {
        recordingJob = scope.launch {
            try {
                // 重新初始化AudioRecord
                val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Invalid buffer size: $bufferSize")
                    withContext(Dispatchers.Main) {
                        _userText.value = "录音初始化失败"
                        updateState(ChatState.ERROR)
                    }
                    return@launch
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed")
                    withContext(Dispatchers.Main) {
                        _userText.value = "录音初始化失败"
                        updateState(ChatState.ERROR)
                    }
                    return@launch
                }

                isRecording = true
                audioRecord?.startRecording()
                Log.d(TAG, "AudioRecord started, streaming to ASR...")

                // 实时读取录音数据并发送到 WebSocket
                val readBuffer = ByteArray(4096)
                var seq = 1
                while (isRecording) {
                    val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (bytesRead > 0) {
                        // 将 PCM 数据转为 base64
                        val base64 = Base64.encodeToString(readBuffer, 0, bytesRead, Base64.NO_WRAP)
                        // 实时发送到 WebSocket
                        asrWebSocket?.sendAudioChunk(base64, seq)
                        seq++
                    }
                }

                // 录音结束，发送 end 帧
                asrWebSocket?.sendEnd()
                Log.d(TAG, "ASR streaming ended, sent end frame")

                // 等待 final 结果（最多 10 秒）
                delay(10000)

            } catch (e: Throwable) {
                Log.e(TAG, "Start recording failed", e)
                withContext(Dispatchers.Main) {
                    _userText.value = "录音失败: ${e.message}"
                    updateState(ChatState.ERROR)
                }
            }
        }
    }

    /**
     * 停止语音对话
     */
    fun stopChat() {
        stopListening()
        stopSpeaking()
        updateState(ChatState.IDLE)
    }
    
    /**
     * 初始化TTS
     */
    private fun initTts(context: Context) {
        tts = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(java.util.Locale.CHINESE)
                if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || 
                    result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS language not supported")
                } else {
                    isTtsInitialized = true
                    Log.d(TAG, "TTS initialized successfully")
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }
    
    /**
     * 停止语音播放
     */
    private fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    /**
     * 开始语音识别（使用 ASR WebSocket）
     */
    private fun startListening(context: Context) {
        recordingJob = scope.launch {
            try {
                // 1. 连接 ASR WebSocket
                asrWebSocket?.close()
                asrWebSocket = AsrWebSocket()

                // 监听 partial 结果（独立的 Job）
                partialJob?.cancel()
                partialJob = scope.launch {
                    asrWebSocket?.partialText?.collect { text ->
                        _userText.value = text
                    }
                }

                // 监听 final 结果（独立的 Job）
                finalJob?.cancel()
                finalJob = scope.launch {
                    asrWebSocket?.finalText?.collect { text ->
                        Log.d(TAG, "ASR final: $text")
                        _userText.value = text
                        if (text.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                sendToLLM(text, context)
                            }
                        }
                    }
                }

                // 监听错误（独立的 Job）
                errorJob?.cancel()
                errorJob = scope.launch {
                    asrWebSocket?.asrError?.collect { error ->
                        Log.e(TAG, "ASR error: $error")
                        withContext(Dispatchers.Main) {
                            _userText.value = "语音识别失败: $error"
                            updateState(ChatState.ERROR)
                        }
                    }
                }

                // 2. 连接 ASR WebSocket
                asrWebSocket?.connectToAsr(ASR_URL)

                // 等待 WebSocket 连接成功（最多 5 秒）
                var waitTime = 0L
                while (asrWebSocket?.state?.value != com.example.uniapp_box.data.ws.BaseWebSocket.State.CONNECTED && waitTime < 5000) {
                    delay(100)
                    waitTime += 100
                }

                if (asrWebSocket?.state?.value != com.example.uniapp_box.data.ws.BaseWebSocket.State.CONNECTED) {
                    Log.e(TAG, "ASR WebSocket connection timeout")
                    withContext(Dispatchers.Main) {
                        _userText.value = "ASR 连接超时"
                        updateState(ChatState.ERROR)
                    }
                    return@launch
                }

                Log.d(TAG, "ASR WebSocket connected, sending config...")
                updateState(ChatState.LISTENING)

                // 3. 发送 config 帧
                asrWebSocket?.sendConfig(format = "pcm", sampleRate = SAMPLE_RATE)

                // 4. 开始录音并实时发送
                val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(TAG, "Invalid buffer size: $bufferSize")
                    withContext(Dispatchers.Main) {
                        _userText.value = "录音初始化失败"
                        updateState(ChatState.ERROR)
                    }
                    return@launch
                }

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize * 2
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "AudioRecord initialization failed")
                    withContext(Dispatchers.Main) {
                        _userText.value = "录音初始化失败"
                        updateState(ChatState.ERROR)
                    }
                    return@launch
                }

                isRecording = true
                audioRecord?.startRecording()
                Log.d(TAG, "AudioRecord started, streaming to ASR...")

                // 5. 实时读取录音数据并发送到 WebSocket
                val readBuffer = ByteArray(4096)
                var seq = 1
                while (isRecording) {
                    val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (bytesRead > 0) {
                        // 将 PCM 数据转为 base64
                        val base64 = Base64.encodeToString(readBuffer, 0, bytesRead, Base64.NO_WRAP)
                        // 实时发送到 WebSocket
                        asrWebSocket?.sendAudioChunk(base64, seq)
                        seq++
                    }
                }

                // 6. 录音结束，发送 end 帧
                asrWebSocket?.sendEnd()
                Log.d(TAG, "ASR streaming ended, sent end frame")

                // 等待 final 结果（最多 10 秒）
                delay(10000)

            } catch (e: Throwable) {
                Log.e(TAG, "Start listening failed", e)
                withContext(Dispatchers.Main) {
                    _userText.value = "语音识别失败: ${e.message}"
                    updateState(ChatState.ERROR)
                }
            }
        }
    }

    /**
     * 停止语音识别
     */
    private fun stopListening() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Throwable) {
            Log.e(TAG, "Stop audio record failed", e)
        }
        recordingJob?.cancel()
        recordingJob = null
        // 注意：partialJob, finalJob, errorJob 不在这里取消，因为它们需要接收最终结果
    }

    /**
     * 发送到 LLM
     */
    private fun sendToLLM(text: String, context: Context) {
        updateState(ChatState.THINKING)
        scope.launch {
            try {
                // 构建请求
                val json = JSONObject().apply {
                    put("query", text)
                    put("session_id", sessionId)
                    put("scenic_id", "lingshan")
                    put("scenic_name", "灵山胜境")
                    put("with_memory", true)
                }
                
                val requestBody = json.toString().toRequestBody("application/json".toMediaType())
                
                val request = okhttp3.Request.Builder()
                    .url("$GUIDE_URL/api/v1/guide/chat")
                    .post(requestBody)
                    .build()
                
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val answer = jsonResponse.optString("answer", "")
                    val speechText = jsonResponse.optString("speech_text", answer)
                    
                    Log.d(TAG, "LLM response: $answer")
                    _aiText.value = answer
                    
                    // 使用TTS播放回答
                    if (speechText.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            speakText(speechText)
                        }
                    } else {
                        updateState(ChatState.IDLE)
                    }
                } else {
                    Log.e(TAG, "LLM API error: ${response.code}")
                    withContext(Dispatchers.Main) {
                        _aiText.value = "AI服务响应错误: ${response.code}"
                        updateState(ChatState.ERROR)
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "LLM call failed", e)
                withContext(Dispatchers.Main) {
                    _aiText.value = "网络错误，请重试"
                    updateState(ChatState.ERROR)
                }
            }
        }
    }

    /**
     * TTS 播放
     */
    private fun speakText(text: String) {
        if (!isTtsInitialized || text.isEmpty()) {
            updateState(ChatState.IDLE)
            return
        }
        
        updateState(ChatState.SPEAKING)
        
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        updateState(ChatState.IDLE)
                    }
                }
            }
            override fun onError(utteranceId: String?) {
                scope.launch {
                    withContext(Dispatchers.Main) {
                        updateState(ChatState.IDLE)
                    }
                }
            }
        })
        
        val params = android.os.Bundle().apply {
            putString(android.speech.tts.TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_${System.currentTimeMillis()}")
        }
        tts?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, params, "utterance_id")
    }

    /**
     * 更新状态
     */
    private fun updateState(state: ChatState) {
        _chatState.value = state
    }

    /**
     * 释放资源
     */
    fun release() {
        stopListening()
        stopSpeaking()
        tts?.shutdown()
        asrWebSocket?.close()
        asrWebSocket = null
        scope.cancel()
    }
}