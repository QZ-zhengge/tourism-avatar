package com.example.let_us_go.core.float

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.let_us_go.data.model.ChatRequest
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.data.ws.AsrWebSocket
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
import java.util.Locale
import java.util.UUID

/**
 * 语音对话管理器（悬浮窗，使用 ASR WebSocket，与数字人页面一致）
 *
 * 流程：
 * 1. 连接 ASR WebSocket
 * 2. 发送 config 帧
 * 3. 开始录音
 * 4. 实时将录音数据发送到 WebSocket
 * 5. 实时接收识别结果
 * 6. 识别完成后发送到 LLM
 * 7. TTS 播放回答
 *
 * 相对旧版的优化（参考 uniapp_box）：
 * - 对话气泡状态 [isDialogShowing]，关闭时统一清理（停播+停录+回 IDLE）
 * - TTS 单例复用（[initTts]/[stopSpeaking]），不再每次新建实例
 * - 录音与「回答」可互相切换（[stopListeningAndStartAnswering]/[stopAnsweringAndStartListening]）
 */
object FloatChatManager {

    private const val TAG = "FloatChatManager"
    private const val ASR_URL = "http://192.0.2.1:50080"
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

    /** 对话框显示状态（参考 uniapp_box，新增） */
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

    // TTS 单例（参考 uniapp_box，新增复用）
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val sessionId: String = UUID.randomUUID().toString()

    /**
     * 开始语音对话
     */
    fun startChat(context: Context) {
        // 初始化 TTS（仅一次）
        if (!isTtsInitialized) {
            initTts(context)
        }
        // 显示对话框
        _isDialogShowing.value = true
        updateState(ChatState.CONNECTING)
        startListening(context)
    }

    /** 显示对话框 */
    fun showDialog() {
        _isDialogShowing.value = true
    }

    /** 关闭对话框：统一清理（停播 + 停录 + 回空闲） */
    fun dismissDialog() {
        _isDialogShowing.value = false
        stopSpeaking()
        stopListening()
        updateState(ChatState.IDLE)
    }

    /**
     * 停止录音，开始回答（录音中点悬浮窗触发）
     */
    fun stopListeningAndStartAnswering() {
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
        // 发送结束帧到 ASR WebSocket，finalText 到达后会自动走 sendToLLM
        asrWebSocket?.sendEnd()
    }

    /**
     * 停止回答，开始录音（思考/播报中点悬浮窗触发）
     */
    fun stopAnsweringAndStartListening() {
        stopSpeaking()
        updateState(ChatState.LISTENING)
        startRecording()
    }

    /**
     * 停止语音对话（整体重置）
     */
    fun stopChat() {
        stopListening()
        stopSpeaking()
        updateState(ChatState.IDLE)
    }

    /**
     * 初始化 TTS（仅一次）
     */
    private fun initTts(context: Context) {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINESE)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
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

    /** 停止 TTS 播放 */
    private fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    /**
     * 开始语音识别（连接 ASR + 初始化 AudioRecord + 录音发送）
     */
    private fun startListening(context: Context) {
        recordingJob = scope.launch {
            try {
                // 1. 连接 ASR WebSocket
                asrWebSocket?.close()
                asrWebSocket = AsrWebSocket()

                partialJob?.cancel()
                partialJob = scope.launch {
                    asrWebSocket?.partialText?.collect { text -> _userText.value = text }
                }
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

                asrWebSocket?.connectToAsr(ASR_URL)

                // 等待连接成功（最多 5 秒）
                var waitTime = 0L
                while (asrWebSocket?.state?.value !=
                    com.example.let_us_go.data.ws.BaseWebSocket.State.CONNECTED && waitTime < 5000
                ) {
                    delay(100)
                    waitTime += 100
                }

                if (asrWebSocket?.state?.value !=
                    com.example.let_us_go.data.ws.BaseWebSocket.State.CONNECTED
                ) {
                    Log.e(TAG, "ASR WebSocket connection timeout")
                    withContext(Dispatchers.Main) {
                        _userText.value = "ASR 连接超时"
                        updateState(ChatState.ERROR)
                    }
                    return@launch
                }

                Log.d(TAG, "ASR WebSocket connected, sending config...")
                updateState(ChatState.LISTENING)

                // 2. 发送 config 帧
                asrWebSocket?.sendConfig(format = "pcm", sampleRate = SAMPLE_RATE)

                // 3. 开始录音并实时发送（复用 startRecording）
                startRecordingInner()
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
     * 仅初始化并开始录音（不含 WS 连接），供「停止回答后重新听」复用
     */
    private fun startRecording() {
        recordingJob = scope.launch { startRecordingInner() }
    }

    private suspend fun startRecordingInner() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                withContext(Dispatchers.Main) {
                    _userText.value = "录音初始化失败"
                    updateState(ChatState.ERROR)
                }
                return
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
                return
            }

            isRecording = true
            audioRecord?.startRecording()
            Log.d(TAG, "AudioRecord started, streaming to ASR...")

            val readBuffer = ByteArray(4096)
            var seq = 1
            while (isRecording) {
                val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                if (bytesRead > 0) {
                    val base64 = Base64.encodeToString(readBuffer, 0, bytesRead, Base64.NO_WRAP)
                    asrWebSocket?.sendAudioChunk(base64, seq)
                    seq++
                }
            }

            asrWebSocket?.sendEnd()
            Log.d(TAG, "ASR streaming ended, sent end frame")
            delay(10000) // 等待 final 结果
        } catch (e: Throwable) {
            Log.e(TAG, "Start recording failed", e)
            withContext(Dispatchers.Main) {
                _userText.value = "录音失败: ${e.message}"
                updateState(ChatState.ERROR)
            }
        }
    }

    /**
     * 停止语音识别（仅停录音，不停 finalJob 以接收最终结果）
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
    }

    /**
     * 发送到 LLM（复用项目的 guideApi.chat）
     */
    private fun sendToLLM(text: String, context: Context) {
        // 登录闸门：未登录不发送，避免 /guide/chat 带上空 user_id
        if (!UserPrefs.isLoggedIn()) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            updateState(ChatState.IDLE)
            return
        }
        updateState(ChatState.THINKING)
        scope.launch {
            try {
                val res = NetworkModule.guideApi.chat(
                    ChatRequest(
                        query = text,
                        sessionId = sessionId,
                        userId = UserPrefs.userId(),
                        scenicId = "lingshan",
                        scenicName = "灵山胜境",
                        withMemory = true
                    )
                )
                val answer = res.answerText().ifBlank { "抱歉，暂时无法回答。" }
                _aiText.value = answer
                // TTS 优先用 speech_text，缺失时回退 answer
                val speak = res.speechText ?: answer
                if (speak.isNotBlank()) {
                    withContext(Dispatchers.Main) { speakText(speak) }
                } else {
                    updateState(ChatState.IDLE)
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
     * TTS 播报（复用单例 tts）
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
                scope.launch { withContext(Dispatchers.Main) { updateState(ChatState.IDLE) } }
            }

            override fun onError(utteranceId: String?) {
                scope.launch { withContext(Dispatchers.Main) { updateState(ChatState.IDLE) } }
            }
        })
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_${System.currentTimeMillis()}")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utterance_id")
    }

    private fun updateState(state: ChatState) {
        _chatState.value = state
    }

    /**
     * 释放资源（进程退出时）
     */
    fun release() {
        stopListening()
        stopSpeaking()
        tts?.shutdown()
        tts = null
        isTtsInitialized = false
        asrWebSocket?.close()
        asrWebSocket = null
        scope.cancel()
    }
}
