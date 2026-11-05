package com.example.let_us_go.ui.pages.digital_human

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.let_us_go.SuiXingApp
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.core.playback.DigitalHumanPlayerHolder
import com.example.let_us_go.data.model.AvatarDriveAudioProxyRequest
import com.example.let_us_go.data.model.AvatarDriveAudioUrlRequest
import com.example.let_us_go.data.model.AvatarDriveTextRequest
import com.example.let_us_go.data.model.AvatarPingRequest
import com.example.let_us_go.data.model.AvatarSession
import com.example.let_us_go.data.model.AvatarStartRequest
import com.example.let_us_go.data.model.AvatarStopRequest
import com.example.let_us_go.data.model.ChatMessage
import com.example.let_us_go.data.model.ChatRequest
import com.example.let_us_go.data.api.RealtimeExplainHistoryBody
import com.example.let_us_go.data.model.ImageRecognitionResponse
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.DhConversationStore
import com.example.let_us_go.data.prefs.ConversationStore
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.data.ws.AsrWebSocket
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 数字人 ViewModel
 *
 * 必须与 uniapp pages/digital-human/index.js 完全一致
 */
class DigitalHumanViewModel : BaseViewModel() {

    enum class DhStatus { IDLE, STARTING, READY, FAILED, CLOSED }

    private val _status = MutableStateFlow(DhStatus.IDLE)
    val status: StateFlow<DhStatus> = _status.asStateFlow()

    private val _session = MutableStateFlow<AvatarSession?>(null)
    val session: StateFlow<AvatarSession?> = _session.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _retryAttempt = MutableStateFlow(0)
    val retryAttempt: StateFlow<Int> = _retryAttempt.asStateFlow()

    private val _asrPartialText = MutableStateFlow("")
    val asrPartialText: StateFlow<String> = _asrPartialText.asStateFlow()

    private val _recognizing = MutableStateFlow(false)
    val recognizing: StateFlow<Boolean> = _recognizing.asStateFlow()

    // ── 全屏模式状态 ──
    private val _isFullScreen = MutableStateFlow(true) // 默认全屏
    val isFullScreen: StateFlow<Boolean> = _isFullScreen.asStateFlow()

    // ── AI 思考状态（全屏模式下禁用按钮） ──
    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    // ── 心情 & 活力（假数据，用户说话时 0.5s 刷新） ──
    private val _mood = MutableStateFlow(90)
    val mood: StateFlow<Int> = _mood.asStateFlow()
    private val _energy = MutableStateFlow(85)
    val energy: StateFlow<Int> = _energy.asStateFlow()
    private val _showDetail = MutableStateFlow(false)
    val showDetail: StateFlow<Boolean> = _showDetail.asStateFlow()

    fun toggeDetail() { _showDetail.value = !_showDetail.value }

    private var moodJob: kotlinx.coroutines.Job? = null
    private val rng = kotlin.random.Random(System.currentTimeMillis())

    private fun startMoodSimulation() {
        if (moodJob?.isActive == true) return
        moodJob = viewModelScope.launch {
            while (moodJob?.isActive == true) {
                _mood.value = (_mood.value + rng.nextInt(-2, 3)).coerceIn(85, 99)
                _energy.value = (_energy.value + rng.nextInt(-2, 3)).coerceIn(75, 95)
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun stopMoodSimulation() {
        moodJob?.cancel()
        moodJob = null
    }

    // 加载共享对话历史（来自 AI 问答页的消息）并去重合并
    init {
        observeRecordingForMood()
        val context = SuiXingApp.instance
        val shared = ConversationStore.getCurrentMessages(context)
        if (shared.isNotEmpty()) {
            val existing = _messages.value
            val existingContents = existing.map { it.content }.toSet()
            val newMsgs = shared
                .filter { it.content !in existingContents }
                .sortedBy { it.time }
                .map { msg ->
                    val cards = if (!msg.cardsJson.isNullOrBlank()) {
                        try {
                            val arr = org.json.JSONArray(msg.cardsJson)
                            (0 until arr.length()).map { i ->
                                val o = arr.getJSONObject(i)
                                com.example.let_us_go.data.model.HotelCard(
                                    type = o.optString("type", null),
                                    kind = o.optString("kind", null),
                                    id = o.optString("id", null),
                                    name = o.optString("name", ""),
                                    title = o.optString("title", null),
                                    image = o.optString("image", null),
                                    rating = o.optString("rating", null),
                                    distance = o.optString("distance", null),
                                    price = o.optString("price", null),
                                    phone = o.optString("phone", null),
                                    bookingUrl = o.optString("booking_url", null),
                                    summary = o.optString("summary", null)
                                )
                            }
                        } catch (e: Exception) { null }
                    } else null
                    ChatMessage(role = msg.role, content = msg.content, time = msg.time, cards = cards)
                }
            if (newMsgs.isNotEmpty()) {
                _messages.value = existing + newMsgs
            }
        }
    }

    // 心情/活力仿真：录制中时每 0.5s 刷新
    private fun observeRecordingForMood() {
        viewModelScope.launch {
            _isRecording.collect { recording ->
                if (recording) startMoodSimulation() else stopMoodSimulation()
            }
        }
    }

    fun setThinking(thinking: Boolean) {
        _isThinking.value = thinking
    }

    fun toggleFullScreen() {
        _isFullScreen.value = !_isFullScreen.value
        // 退出全屏时清除思考状态
        if (!_isFullScreen.value) {
            _isThinking.value = false
        }
    }

    fun exitFullScreen() {
        _isFullScreen.value = false
        _isThinking.value = false
    }

    fun enterFullScreen() {
        _isFullScreen.value = true
    }

    // ── 覆盖页（实时讲解）专用状态 ──
    /** 拍照识别结果（覆盖页读取，非 null 表示有结果可展示） */
    private val _overlayResult = MutableStateFlow<OverlayRecognitionData?>(null)
    val overlayResult: StateFlow<OverlayRecognitionData?> = _overlayResult.asStateFlow()

    data class OverlayRecognitionData(
        val description: String,
        val speechText: String,
        val audioUrl: String?
    )

    // ── 覆盖页音频控制（复用数字人 ExoPlayer 流播放 50080 TTS）──
    /**
     * 静音/恢复数字人 ExoPlayer。
     * 覆盖页用此实现：打开→恢复音量（听得到 50080 TTS）；退出/暂停→静音（立即停）。
     */
    fun muteAvatarAudio(muted: Boolean) {
        playerHolder?.setMuted(muted)
    }

    /**
     * 估算 TTS 播报时长（用于覆盖页假进度条），约 4 字/秒，下限 2 秒。
     */
    fun estimateSpeechMs(text: String): Long {
        val chars = text.length.coerceAtLeast(1)
        return (chars / 4.0 * 1000).toLong().coerceAtLeast(2000L)
    }

    private var sessionId: String = ""
    private var playerHolder: DigitalHumanPlayerHolder? = null
    private var currentAvatarId: String = "118801001"  // 与 uniapp 默认一致
    private var currentVcn: String = "x4_lingxiaoyu_assist"

    // 心跳定时器
    private var pingJob: Job? = null

    // ASR WebSocket
    private var asrWebSocket: AsrWebSocket? = null
    private var asrJob: Job? = null

    // ASR 录音相关
    private var audioRecord: AudioRecord? = null
    private var isAudioRecording = false
    private var recordingStartTime = 0L
    private val pcmBuffer = ByteArrayOutputStream()

    // 录音限制常量
    companion object {
        private const val MIN_RECORD_DURATION_MS = 2000L  // 最低 2 秒
        private const val MAX_RECORD_DURATION_MS = 60000L // 最高 60 秒
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun bindPlayer(holder: DigitalHumanPlayerHolder) {
        this.playerHolder = holder
        _session.value?.let { preparePlayerForSession(it) }
    }

    /**
     * 启动数字人会话：最多重试 3 次
     *
     * 与 uniapp startAvatarSession() 完全一致
     * 参数：avatar_id, width, height, protocol, bg_custom
     */
    fun start() {
        if (_status.value == DhStatus.STARTING || _status.value == DhStatus.READY) return
        _status.value = DhStatus.STARTING
        launchIo(showLoading = false) {
            // 启动前读取默认数字人配置（去硬编码，失败不影响启动）
            try {
                val cfg = NetworkModule.digitalHumanApi.getConfig()
                if (cfg.isSuccess && cfg.data != null) {
                    cfg.data.avatarId?.let { currentAvatarId = it }
                    cfg.data.vcn?.let { currentVcn = it }
                    Log.d("DigitalHumanVM", "avatar/config: avatarId=$currentAvatarId, vcn=$currentVcn")
                }
            } catch (e: Throwable) {
                Log.w("DigitalHumanVM", "读取 avatar/config 失败，使用默认值: ${e.message}")
            }

            val maxRetries = 3
            var lastError: String? = null
            for (attempt in 1..maxRetries) {
                _retryAttempt.value = attempt
                if (attempt > 1) {
                    delay(2000L)  // 与 uniapp RETRY_DELAY_MS = 2000 一致
                }
                try {
                    val res = NetworkModule.digitalHumanApi.start(
                        AvatarStartRequest(
                            // avatar_id 不传，后端从 DB digital_human_config 表自动读取
                            userId = UserPrefs.userId()?.toString(),
                            width = 1280,
                            height = 720,
                            protocol = "rtmp",
                            bgTransparent = false
                        )
                    )
                    // 必须检查 session 非空（xfyun_code=11203 时 session 为空）
                    if (res.isSuccess && !res.data?.session.isNullOrBlank()) {
                        _session.value = res.data
                        sessionId = res.data!!.session
                        res.data!!.avatarId?.let { currentAvatarId = it }
                        res.data!!.vcn?.let { currentVcn = it }
                        _status.value = DhStatus.READY
                        preparePlayerForSession(res.data!!)
                        startPingTimer()
                        // 持久化会话元数据（空消息列表，后续 sendText/takePhoto 会追加）
                        DhConversationStore.write(
                            SuiXingApp.instance,
                            DhConversationStore.ConversationRecord(
                                sessionId = sessionId,
                                avatarId = res.data!!.avatarId
                            )
                        )
                        Log.d("DigitalHumanVM", "start 成功: session=$sessionId, streamUrl=${res.data!!.streamUrl}")
                        return@launchIo
                    } else {
                        lastError = "xfyun_code=${res.data?.xfyunCode ?: "?"}, ${res.data?.xfyunMessage ?: res.msg ?: "未知错误"}"
                        Log.w("DigitalHumanVM", "start 第${attempt}次返回但无 session: $lastError")
                    }
                } catch (e: Throwable) {
                    lastError = e.message ?: "启动异常"
                    Log.e("DigitalHumanVM", "start 第${attempt}次异常: ${e.message}")
                }
            }
            _status.value = DhStatus.FAILED
            setError(lastError ?: "启动失败（已重试 $maxRetries 次）")
        }
    }

    /** 在主线程初始化播放器（ExoPlayer 必须在主线程操作） */
    private fun preparePlayerForSession(session: AvatarSession) {
        val holder = playerHolder ?: return
        val streamUrl = session.streamUrl
        val playUrl = session.playUrl
        Log.d("DigitalHumanVM", "preparePlayer: streamUrl=$streamUrl, playUrl=$playUrl")
        // ExoPlayer 所有操作必须在主线程，用 post 保证线程安全
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            holder.prepare(streamUrl, playUrl)
        }
    }

    /**
     * 发送文本 → 调 LLM 获取回答 → 驱动数字人播报
     *
     * 与 uniapp handleAsrResult() 逻辑一致
     */
    fun sendText(text: String) {
        val userMsg = ChatMessage(role = "user", content = text)
        _messages.value = _messages.value + userMsg
        if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, userMsg)
        // 同步到共享对话存储
        ConversationStore.appendMessage(SuiXingApp.instance, "digital_human", userMsg)
        // 设置思考状态
        _isThinking.value = true
        launchIo(showLoading = false) {
            try {
                // 1. 调 guideApi.chat 获取 LLM 回答
                Log.d("DigitalHumanVM", "发送消息到 50081 后端: $text")
                Log.d("DigitalHumanVM", "请求参数: sessionId=$sessionId, scenicId=lingshan, scenicName=灵山胜境")

                val chatRes = NetworkModule.guideApi.chat(
                    ChatRequest(
                        query = text,
                        sessionId = sessionId,
                        userId = UserPrefs.userId(),
                        scenicId = "lingshan",
                        scenicName = "灵山胜境",
                        withMemory = true
                    )
                )
                Log.d("DigitalHumanVM", "50081 后端响应: intent=${chatRes.intent}, action=${chatRes.action}")
                Log.d("DigitalHumanVM", "响应 answer: ${chatRes.answer}")
                Log.d("DigitalHumanVM", "响应 speech_text: ${chatRes.speechText}")

                // 后端直接返回 ChatResponse，使用 answer 或 answerText() 获取回答
                val answer = chatRes.answerText()
                if (answer.isNotBlank()) {
                    Log.d("DigitalHumanVM", "最终回答: $answer")
                } else {
                    Log.w("DigitalHumanVM", "回答为空")
                }

                val aiMsg = ChatMessage(
                    role = "ai",
                    content = answer.ifBlank { "抱歉，暂时无法回答。" },
                    // 结构化预订卡片：取 data.card_data.items（真实数组），兜底 data.cards / 顶层 cards
                    cards = chatRes.bookingCards()
                )
                _messages.value = _messages.value + aiMsg
                if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, aiMsg)
                // 同步到共享对话存储
                ConversationStore.appendMessage(SuiXingApp.instance, "digital_human", aiMsg)

                // 2. 用 speech_text 驱动数字人说话（与 uniapp 一致）
                if (sessionId.isNotBlank()) {
                    try {
                        NetworkModule.digitalHumanApi.driveText(
                            AvatarDriveTextRequest(
                                session = sessionId,
                                userId = UserPrefs.userId()?.toString(),
                                userText = chatRes.speechText ?: answer,
                                text = chatRes.speechText ?: answer,
                                vcn = currentVcn,
                                speed = 50,
                                pitch = 50,
                                volume = 50
                            )
                        )
                        Log.d("DigitalHumanVM", "driveText 成功: text=${answer.take(30)}...")
                        // 清除思考状态（数字人开始播报）
                        _isThinking.value = false
                    } catch (e: Throwable) {
                        Log.w("DigitalHumanVM", "driveText 失败: ${e.message}")
                        _isThinking.value = false
                    }
                }
            } catch (e: Throwable) {
                Log.e("DigitalHumanVM", "sendText 异常: ${e.message}", e)
                val errMsg = ChatMessage(role = "ai", content = "抱歉，网络异常。错误: ${e.message}")
                _messages.value = _messages.value + errMsg
                if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, errMsg)
                _isThinking.value = false
            }
        }
    }

    /**
     * 通过代理转发音频帧驱动数字人对口型
     *
     * 与 uniapp driveAudioProxy() 逻辑一致
     * @param audioBase64 Base64 编码的 PCM 音频（16kHz，单声道）
     * @param status 帧状态：0=开始，1=继续，2=结束
     * @param seq 帧序号，从1递增
     */
    fun driveAudioProxy(audioBase64: String, status: Int, seq: Int = 1) {
        if (sessionId.isBlank()) {
            Log.w("DigitalHumanVM", "driveAudioProxy: 没有活跃 session")
            return
        }
        launchIo(showLoading = false) {
            try {
                NetworkModule.digitalHumanApi.driveAudioProxy(
                    AvatarDriveAudioProxyRequest(
                        session = sessionId,
                        audioBase64 = audioBase64,
                        status = status,
                        seq = seq,
                        encoding = "raw",
                        sampleRate = 16000
                    )
                )
                Log.d("DigitalHumanVM", "driveAudioProxy 成功: status=$status, seq=$seq")
            } catch (e: Throwable) {
                Log.w("DigitalHumanVM", "driveAudioProxy 失败: ${e.message}")
            }
        }
    }

    /**
     * 获取音频驱动 WSS 鉴权 URL
     *
     * 与 uniapp driveAudioUrl() 逻辑一致
     * @return WSS URL，用于直连讯飞服务
     */
    suspend fun getDriveAudioUrl(): String? {
        if (sessionId.isBlank()) {
            Log.w("DigitalHumanVM", "getDriveAudioUrl: 没有活跃 session")
            return null
        }
        return try {
            val res = NetworkModule.digitalHumanApi.driveAudioUrl(
                AvatarDriveAudioUrlRequest(session = sessionId)
            )
            if (res.isSuccess && res.data != null) {
                Log.d("DigitalHumanVM", "driveAudioUrl 成功: ${res.data.wssUrl.take(50)}...")
                res.data.wssUrl
            } else {
                Log.w("DigitalHumanVM", "driveAudioUrl 失败: ${res.msg}")
                null
            }
        } catch (e: Throwable) {
            Log.w("DigitalHumanVM", "driveAudioUrl 异常: ${e.message}")
            null
        }
    }

    /**
     * 开始/停止录音（ASR）- 流式模式
     *
     * 交互方式：点击开始录音，再点击停止录音
     * 录音限制：最低 2 秒，最高 60 秒
     * 流式处理：录音过程中实时发送音频帧到后端，实时接收识别结果
     */
    fun toggleRecord() {
        if (_isRecording.value) {
            // 停止录音
            val elapsed = System.currentTimeMillis() - recordingStartTime
            if (elapsed < MIN_RECORD_DURATION_MS) {
                setError("录音时间太短，请至少说 2 秒")
                stopStreamingAsr()
                _isRecording.value = false
                return
            }
            _isRecording.value = false
            stopStreamingAsr()
        } else {
            // 开始录音
            if (_status.value != DhStatus.READY) {
                setError("数字人未就绪")
                return
            }
            _isRecording.value = true
            _asrPartialText.value = "正在录音..."
            recordingStartTime = System.currentTimeMillis()
            startStreamingAsr()

            // 启动最大时长自动停止
            viewModelScope.launch {
                delay(MAX_RECORD_DURATION_MS)
                if (_isRecording.value) {
                    Log.w("DigitalHumanVM", "录音达到最大时长 ${MAX_RECORD_DURATION_MS}ms，自动停止")
                    _isRecording.value = false
                    stopStreamingAsr()
                }
            }
        }
    }

    /**
     * 启动流式 ASR
     * 1. 连接 WebSocket 并发送 config
     * 2. 开始录音
     * 3. 实时将录音数据发送到 WebSocket
     * 4. 实时接收识别结果
     */
    private fun startStreamingAsr() {
        asrJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. 连接 ASR WebSocket
                asrWebSocket?.close()
                asrWebSocket = AsrWebSocket()

                // 监听 partial 结果（实时显示）
                val partialJob = launch {
                    asrWebSocket?.partialText?.collect { text ->
                        _asrPartialText.value = text
                    }
                }
                // 监听 final 结果
                val finalJob = launch {
                    asrWebSocket?.finalText?.collect { text ->
                        Log.i("DigitalHumanVM", "ASR final: $text")
                        _asrPartialText.value = text
                        if (text.isNotBlank()) {
                            withContext(Dispatchers.Main) {
                                delay(300)
                                sendText(text)
                            }
                        }
                    }
                }
                // 监听错误
                val errorJob = launch {
                    asrWebSocket?.asrError?.collect { error ->
                        Log.e("DigitalHumanVM", "ASR error: $error")
                        withContext(Dispatchers.Main) {
                            setError("语音识别失败: $error")
                            _isRecording.value = false
                        }
                    }
                }

                // 连接 ASR WebSocket
                asrWebSocket?.connectToAsr("http://192.0.2.1:50080")

                // 等待 WebSocket 连接成功（最多 5 秒）
                var waitTime = 0L
                while (asrWebSocket?.state?.value != com.example.let_us_go.data.ws.BaseWebSocket.State.CONNECTED && waitTime < 5000) {
                    delay(100)
                    waitTime += 100
                }

                if (asrWebSocket?.state?.value != com.example.let_us_go.data.ws.BaseWebSocket.State.CONNECTED) {
                    Log.e("DigitalHumanVM", "ASR WebSocket connection timeout")
                    withContext(Dispatchers.Main) {
                        setError("ASR 连接超时")
                        _isRecording.value = false
                    }
                    return@launch
                }

                Log.d("DigitalHumanVM", "ASR WebSocket connected, sending config...")

                // 2. 发送 config 帧（PCM 格式）- 确保在连接成功后发送
                asrWebSocket?.sendConfig(format = "pcm", sampleRate = SAMPLE_RATE)
                Log.d("DigitalHumanVM", "ASR config sent, starting recording...")

                // 3. 开始录音并实时发送
                val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
                Log.d("DigitalHumanVM", "AudioRecord config: sampleRate=$SAMPLE_RATE, channelConfig=$CHANNEL_CONFIG, audioFormat=$AUDIO_FORMAT, bufferSize=$bufferSize")

                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e("DigitalHumanVM", "Invalid buffer size: $bufferSize")
                    withContext(Dispatchers.Main) {
                        setError("无法初始化录音")
                        _isRecording.value = false
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

                Log.d("DigitalHumanVM", "AudioRecord created, state=${audioRecord?.state}")

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e("DigitalHumanVM", "AudioRecord initialization failed, state=${audioRecord?.state}")
                    withContext(Dispatchers.Main) {
                        setError("录音初始化失败")
                        _isRecording.value = false
                    }
                    return@launch
                }

                isAudioRecording = true
                audioRecord?.startRecording()
                Log.d("DigitalHumanVM", "AudioRecord started, streaming to ASR...")

                // 4. 实时读取录音数据并发送到 WebSocket
                val readBuffer = ByteArray(4096)
                var seq = 1
                var totalBytesRead = 0L
                var nonZeroBytes = 0L
                while (isAudioRecording) {
                    val bytesRead = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
                    if (bytesRead > 0) {
                        totalBytesRead += bytesRead
                        // 统计非零字节数（检测是否有实际音频）
                        for (i in 0 until bytesRead) {
                            if (readBuffer[i].toInt() != 0) {
                                nonZeroBytes++
                            }
                        }
                        // 将 PCM 数据转为 base64
                        val base64 = Base64.encodeToString(readBuffer, 0, bytesRead, Base64.NO_WRAP)
                        // 实时发送到 WebSocket
                        asrWebSocket?.sendAudioChunk(base64, seq)
                        seq++

                        // 每 100 帧打印一次统计
                        if (seq % 100 == 0) {
                            val ratio = if (totalBytesRead > 0) nonZeroBytes * 100 / totalBytesRead else 0
                            Log.d("DigitalHumanVM", "Audio stats: totalBytes=$totalBytesRead, nonZeroRatio=${ratio}%, seq=$seq")
                        }
                    }
                }

                Log.d("DigitalHumanVM", "Recording ended: totalBytes=$totalBytesRead, nonZeroBytes=$nonZeroBytes")

                // 5. 录音结束，发送 end 帧
                asrWebSocket?.sendEnd()
                Log.d("DigitalHumanVM", "ASR streaming ended, sent end frame")

                // 等待 final 结果
                delay(5000)

            } catch (e: Throwable) {
                Log.e("DigitalHumanVM", "Streaming ASR error", e)
                withContext(Dispatchers.Main) {
                    setError("语音识别失败: ${e.message}")
                    _isRecording.value = false
                }
            }
        }
    }

    /**
     * 停止流式 ASR
     */
    private fun stopStreamingAsr() {
        isAudioRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d("DigitalHumanVM", "AudioRecord stopped")
        } catch (e: Throwable) {
            Log.e("DigitalHumanVM", "stopAudioRecording failed", e)
            audioRecord?.release()
            audioRecord = null
        }
    }


    /**
     * 停止 ASR 录音
     */
    private fun stopAsrRecording() {
        stopStreamingAsr()
        asrJob?.cancel()
        asrJob = null
        asrWebSocket?.close()
        asrWebSocket = null
    }

    /**
     * 拍照识别（真实管线）
     *
     * 与 uniapp pages/live-guide 的 拍照→识别→TTS 一致：
     * 1. 把图片（JPEG 字节）以 multipart/form-data 发给 50081 的 image-recognition 接口
     * 2. 解析返回 description / speech_text
     * 3. 用 speech_text 调 50080 的 driveText 让数字人对口型播报
     *    （与语音对话 sendText → driveText 的回答逻辑完全一致）
     *
     * 注意：本方法【不会】重启或中断数字人流，仅追加聊天消息并注入播报，
     * 因此点击拍照识别不会造成数字人画面截断刷新。
     *
     * @param imageBytes 已压缩的 JPEG 图片字节（建议 ≤ 1.5MB）
     * @param fileName   multipart 文件名（用于 Content-Disposition）
     */
    fun takePhoto(imageBytes: ByteArray, fileName: String = "photo.jpg") {
        val photoUserMsg = ChatMessage(role = "user", content = "[拍照识别] 📷")
        _messages.value = _messages.value + photoUserMsg
        if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, photoUserMsg)
        if (imageBytes.isEmpty()) {
            val readFailMsg = ChatMessage(role = "ai", content = "图片读取失败，请重试。")
            _messages.value = _messages.value + readFailMsg
            if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, readFailMsg)
            return
        }
        _recognizing.value = true
        launchIo(showLoading = false) {
            try {
                // 1. 组装 multipart 请求（file + session_id + scenic_id）
                val filePart = MultipartBody.Part.createFormData(
                    "file", fileName,
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                val sessionPart = sessionId.toRequestBody("text/plain".toMediaType())
                val scenicPart = "lingshan".toRequestBody("text/plain".toMediaType())

                Log.d("DigitalHumanVM", "调用 image-recognition 接口，图片字节=${imageBytes.size}")
                val res = NetworkModule.guideApi.imageRecognize(
                    file = filePart,
                    sessionId = sessionPart,
                    scenicId = scenicPart
                )

                // 2. 解析返回
                val answer = res.answerText()
                val speech = res.speechText()
                if (answer.isNotBlank()) {
                    val photoAiMsg = ChatMessage(role = "ai", content = answer)
                    _messages.value = _messages.value + photoAiMsg
                    if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, photoAiMsg)

                    // 3. 持久化实时讲解历史（火焰即弃，失败不影响主流程）
                    saveRealtimeExplain(imageBytes, answer, speech)

                    // 4. 用 speech_text 驱动数字人对口型播报（与语音对话一致）
                    if (sessionId.isNotBlank()) {
                            try {
                            NetworkModule.digitalHumanApi.driveText(
                                AvatarDriveTextRequest(
                                    session = sessionId,
                                    userId = UserPrefs.userId()?.toString(),
                                    userText = res.speechText(),
                                    text = res.speechText(),
                                    vcn = currentVcn,
                                    speed = 50,
                                    pitch = 50,
                                    volume = 50
                                )
                            )
                            Log.d("DigitalHumanVM", "拍照识别 driveText 成功")
                        } catch (e: Throwable) {
                            Log.w("DigitalHumanVM", "拍照识别 driveText 失败: ${e.message}")
                        }
                    }
                } else {
                    Log.w("DigitalHumanVM", "拍照识别返回为空")
                    val emptyMsg = ChatMessage(role = "ai", content = "抱歉，未能识别出图片内容。")
                    _messages.value = _messages.value + emptyMsg
                    if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, emptyMsg)
                }
            } catch (e: Throwable) {
                Log.e("DigitalHumanVM", "拍照识别失败", e)
                val failMsg = ChatMessage(role = "ai", content = "抱歉，拍照识别失败：${e.message ?: "未知错误"}")
                _messages.value = _messages.value + failMsg
                if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, failMsg)
            } finally {
                _recognizing.value = false
            }
        }
    }

    /**
     * 覆盖页专用：拍照识别（不追加聊天消息，结果存入 [overlayResult]）
     *
     * 与 [takePhoto] 的区别：
     * - 不往 [_messages] 追加消息
     * - 不调用 driveText 驱动数字人（覆盖页改用本地 Android TTS 朗读，互不影响）
     * - 结果存到 [_overlayResult]，供 ImageRecognitionOverlay 读取
     *
     * @param imageBytes 已压缩的 JPEG 图片字节
     */
    fun recognizeImageForOverlay(imageBytes: ByteArray) {
        _recognizing.value = true
        _overlayResult.value = null

        launchIo(showLoading = false) {
            try {
                // 1. 调用 50081 image-recognition 获取识别结果
                val filePart = MultipartBody.Part.createFormData(
                    "file", "overlay_photo.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                val sessionPart = sessionId.toRequestBody("text/plain".toMediaType())
                val scenicPart = "lingshan".toRequestBody("text/plain".toMediaType())

                val res = NetworkModule.guideApi.imageRecognize(
                    file = filePart,
                    sessionId = sessionPart,
                    scenicId = scenicPart
                )

                val description = res.answerText()
                val speechText = res.speechText()

                // 持久化实时讲解历史（火焰即弃，失败不影响覆盖页展示）
                saveRealtimeExplain(imageBytes, description, speechText)

                if (description.isBlank()) {
                    Log.w("DigitalHumanVM", "覆盖页识别返回为空")
                    _overlayResult.value = OverlayRecognitionData(
                        description = "抱歉，未能识别出图片内容。",
                        speechText = "",
                        audioUrl = null
                    )
                    return@launchIo
                }

                // 2. 直接存结果供覆盖页展示。
                //    注意：本方法【不】调用 50080 driveText，覆盖页改用本地 Android TTS 朗读，
                //    完全不干扰主数字人流（视频/音频均不受影响）。
                _overlayResult.value = OverlayRecognitionData(
                    description = description,
                    speechText = speechText,
                    audioUrl = null
                )

            } catch (e: Throwable) {
                Log.e("DigitalHumanVM", "覆盖页识别失败", e)
                _overlayResult.value = OverlayRecognitionData(
                    description = "拍照识别失败：${e.message ?: "未知错误"}",
                    speechText = "",
                    audioUrl = null
                )
            } finally {
                _recognizing.value = false
            }
        }
    }

    /** 清除覆盖页结果（用户退出覆盖页或重新识别前调用） */
    fun clearOverlayResult() {
        _overlayResult.value = null
    }

    /**
     * 拍照识别 → 数字人播报（不跳转页面）
     *
     * 流程：
     * 1. 发送图片到 50081 image-recognition 接口
     * 2. 获取识别文本
     * 3. 将文本发给数字人做 TTS 播放（driveText）
     * 4. 结果显示在对话历史中（数字人页 + AI 问答页都能看到）
     *
     * @param imageBytes 已压缩的 JPEG 图片字节
     */
    fun recognizeAndDriveText(imageBytes: ByteArray) {
        // 保存图片到文件
        val imageFileName = "photo_${System.currentTimeMillis()}.jpg"
        val imageFile = java.io.File(SuiXingApp.instance.filesDir, imageFileName)
        try {
            imageFile.writeBytes(imageBytes)
        } catch (e: Exception) {
            Log.e("DigitalHumanVM", "保存图片失败: ${e.message}")
        }

        // 添加用户消息（标记为图片识别，附带图片路径）
        val userMsg = ChatMessage(
            role = "user",
            content = "拍照识别",
            type = "text",
            imagePath = imageFile.absolutePath
        )
        _messages.value = _messages.value + userMsg
        if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, userMsg)
        ConversationStore.appendMessage(SuiXingApp.instance, "digital_human", userMsg)
        // 设置思考状态
        _isThinking.value = true

        launchIo(showLoading = false) {
            try {
                // 1. 调用 50081 image-recognition 获取识别结果
                Log.d("DigitalHumanVM", "拍照识别: 发送图片到后端, imageBytes=${imageBytes.size}")
                val filePart = MultipartBody.Part.createFormData(
                    "file", "photo_${System.currentTimeMillis()}.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                val sessionPart = sessionId.toRequestBody("text/plain".toMediaType())
                val scenicPart = "lingshan".toRequestBody("text/plain".toMediaType())

                val res = NetworkModule.guideApi.imageRecognize(
                    file = filePart,
                    sessionId = sessionPart,
                    scenicId = scenicPart
                )

                val description = res.answerText()
                val speechText = res.speechText()

                if (description.isBlank()) {
                    Log.w("DigitalHumanVM", "拍照识别返回为空")
                    val emptyMsg = ChatMessage(role = "ai", content = "抱歉，未能识别出图片内容。")
                    _messages.value = _messages.value + emptyMsg
                    if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, emptyMsg)
                    ConversationStore.appendMessage(SuiXingApp.instance, "digital_human", emptyMsg)
                    _isThinking.value = false
                    return@launchIo
                }

                Log.d("DigitalHumanVM", "拍照识别成功: ${description.take(50)}...")

                // 2. 添加 AI 回复消息到对话历史
                val aiMsg = ChatMessage(
                    role = "ai",
                    content = description,
                    type = "text"
                )
                _messages.value = _messages.value + aiMsg
                if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, aiMsg)
                ConversationStore.appendMessage(SuiXingApp.instance, "digital_human", aiMsg)

                // 3. 发送给数字人做 TTS 播放
                if (sessionId.isNotBlank()) {
                    try {
                        NetworkModule.digitalHumanApi.driveText(
                            AvatarDriveTextRequest(
                                session = sessionId,
                                text = speechText.ifBlank { description },
                                volume = 50
                            )
                        )
                        Log.d("DigitalHumanVM", "拍照识别 driveText 成功")
                        _isThinking.value = false
                    } catch (e: Throwable) {
                        Log.w("DigitalHumanVM", "拍照识别 driveText 失败: ${e.message}")
                        _isThinking.value = false
                    }
                }
            } catch (e: Throwable) {
                Log.e("DigitalHumanVM", "拍照识别异常: ${e.message}", e)
                val errMsg = ChatMessage(role = "ai", content = "拍照识别失败：${e.message ?: "未知错误"}")
                _messages.value = _messages.value + errMsg
                if (sessionId.isNotBlank()) DhConversationStore.appendMessage(SuiXingApp.instance, sessionId, errMsg)
                ConversationStore.appendMessage(SuiXingApp.instance, "digital_human", errMsg)
            }
        }
    }

    /**
     * 持久化一条「实时讲解历史」（数字人对话历史分支）
     *
     * 对齐 README §10.1：POST /realtime-explain/history
     * 请求体 user_id(string) / image(base64) / ai_response(JSON 文本，原样存) / scenic_id
     * 火焰即弃（fire-and-forget），任何失败只记日志，绝不影响主识别/播报流程。
     *
     * @param imageBytes 拍照原图字节
     * @param description 讲解正文（作为 ai_response.summary）
     * @param speechText  播报文本（作为 ai_response.speech）
     */
    private fun saveRealtimeExplain(imageBytes: ByteArray, description: String, speechText: String) {
        if (description.isBlank()) return
        launchIo(showLoading = false) {
            try {
                val uid = UserPrefs.userIdSync()?.toString()
                val imageB64 = "data:image/jpeg;base64," +
                        android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)
                // 构造后端原样存储的 ai_response JSON（前端列表页自行 JSON.parse 取结构化字段）
                val safeDesc = description.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                val safeSpeech = speechText.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                val aiResponse = "{\"summary\":\"$safeDesc\",\"speech\":\"$safeSpeech\"}"
                val res = NetworkModule.realtimeExplainApi.saveHistory(
                    RealtimeExplainHistoryBody(
                        user_id = uid,
                        image = imageB64,
                        ai_response = aiResponse,
                        scenic_id = "lingshan"
                    )
                )
                Log.d("DigitalHumanVM", "realtime-explain 历史保存: code=${res.code}, msg=${res.msg}")
            } catch (e: Throwable) {
                Log.w("DigitalHumanVM", "realtime-explain 历史保存失败（已忽略）: ${e.message}")
            }
        }
    }

    fun switchAvatar() {
        val avatarIds = listOf("111260002", "111260007", "111260008", "111260009", "111260010", "111260011")
        val vcns = listOf("xiaoyan", "aisxping", "aisjinger", "aisxrong", "yaya")
        val idx = avatarIds.indexOf(currentAvatarId)
        currentAvatarId = avatarIds[(idx + 1) % avatarIds.size]
        currentVcn = vcns[(idx + 1) % vcns.size]
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            playerHolder?.stop()
        }
        _session.value = null
        _status.value = DhStatus.IDLE
        start()
    }

    // ─────────────── 心跳保活（与 uniapp 一致，每 30 秒）───────────────

    private fun startPingTimer() {
        stopPingTimer()
        pingJob = launchIo(showLoading = false) {
            while (true) {
                delay(30000L)
                if (sessionId.isNotBlank()) {
                    try {
                        NetworkModule.digitalHumanApi.ping(AvatarPingRequest(session = sessionId))
                        Log.d("DigitalHumanVM", "ping 成功")
                    } catch (e: Throwable) {
                        Log.w("DigitalHumanVM", "ping 失败: ${e.message}")
                    }
                }
            }
        }
    }

    private fun stopPingTimer() {
        pingJob?.cancel()
        pingJob = null
    }

    /**
     * 关闭数字人会话（前端关闭显示，不真正断开后端会话）。
     * 停播放器、停心跳、停 ASR，状态置 [DhStatus.CLOSED]；
     * 用户在页面点「重连」时由 [start] 重新拉起会话。
     */
    fun closeSession() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            playerHolder?.stop()
        }
        stopPingTimer()
        stopAsrRecording()
        _status.value = DhStatus.CLOSED
    }

    fun stop() {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            playerHolder?.stop()
        }
        stopPingTimer()
        stopAsrRecording()

        // 同步发送停止信号，确保在页面关闭前完成
        if (sessionId.isNotBlank()) {
            try {
                // 使用 runBlocking 确保在主线程同步执行
                kotlinx.coroutines.runBlocking {
                    NetworkModule.digitalHumanApi.stop(AvatarStopRequest(session = sessionId))
                    Log.d("DigitalHumanVM", "stop 成功: session=$sessionId")
                }
            } catch (e: Throwable) {
                Log.w("DigitalHumanVM", "stop 失败: ${e.message}")
            }
        }
        sessionId = ""
        _status.value = DhStatus.IDLE
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
