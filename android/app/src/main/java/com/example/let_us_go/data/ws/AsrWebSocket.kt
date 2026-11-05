package com.example.let_us_go.data.ws

import android.util.Log
import com.example.let_us_go.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * ASR WebSocket（后端 Vosk）
 *
 * 完全对齐 uniapp 的 ASR 协议：
 * 1. 连接 ws://{API_BASE}/asr/stream
 * 2. 发送 config 帧：{ type: 'config', language: 'zh', format: 'mp3', sampleRate: 16000 }
 * 3. 分块发送 audio 帧：{ type: 'audio', data: '<base64>' }
 * 4. 发送 end 帧：{ type: 'end' }
 * 5. 接收 partial/final/ping/error 响应
 * 6. 回复 pong 心跳
 *
 * ASR 是一次性连接，不需要自动重连
 */
class AsrWebSocket : BaseWebSocket(pingIntervalMs = 0, autoReconnect = false) {

    override val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)  // WebSocket 不设读超时
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(0, TimeUnit.MILLISECONDS)
        .build()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val finalText: SharedFlow<String> = _finalText.asSharedFlow()

    private val _asrError = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val asrError: SharedFlow<String> = _asrError.asSharedFlow()

    private var currentUrl: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 连接到 ASR WebSocket
     * @param baseUrl 后端地址，如 http://192.0.2.1:50080
     */
    fun connectToAsr(baseUrl: String) {
        val wsUrl = baseUrl.replace("https://", "wss://")
            .replace("http://", "ws://") + "/asr/stream"
        currentUrl = wsUrl
        Log.i(tag, "Connecting to ASR: $wsUrl")
        open(wsUrl)
    }

    /**
     * 发送配置帧
     */
    fun sendConfig(format: String = "mp3", sampleRate: Int = 16000) {
        val config = JSONObject().apply {
            put("type", "config")
            put("language", "zh")
            put("format", format)
            put("sampleRate", sampleRate)
        }
        send(config.toString())
        Log.d(tag, "ASR config sent: ${config}")
    }

    /**
     * 发送音频数据（base64 分块）
     * @param base64Data 完整的 base64 音频数据
     * @param chunkSize 每块大小（默认 8192，与 uniapp 一致）
     */
    fun sendAudio(base64Data: String, chunkSize: Int = 8192) {
        scope.launch {
            var offset = 0
            while (offset < base64Data.length) {
                val end = minOf(offset + chunkSize, base64Data.length)
                val chunk = base64Data.substring(offset, end)
                offset = end

                val audioFrame = JSONObject().apply {
                    put("type", "audio")
                    put("data", chunk)
                }
                send(audioFrame.toString())

                // 与 uniapp 一致的 30ms 间隔
                delay(30)
            }
            Log.d(tag, "ASR audio sent: ${base64Data.length} chars")
        }
    }

    /**
     * 发送单个音频块（流式模式）
     * 实时发送，无延迟
     * @param base64Chunk base64 编码的音频数据块
     * @param seq 帧序号
     */
    fun sendAudioChunk(base64Chunk: String, seq: Int) {
        val audioFrame = JSONObject().apply {
            put("type", "audio")
            put("data", base64Chunk)
        }
        send(audioFrame.toString())
        // 不打印日志，避免频繁输出
    }

    /**
     * 发送结束帧
     */
    fun sendEnd() {
        val endFrame = JSONObject().apply {
            put("type", "end")
        }
        send(endFrame.toString())
        Log.d(tag, "ASR end sent")
    }

    override fun onMessage(text: String) {
        try {
            val msg = JSONObject(text)
            val type = msg.optString("type", "")

            when (type) {
                "partial" -> {
                    val partialResult = msg.optString("text", "")
                    _partialText.value = partialResult
                    Log.d(tag, "ASR partial: $partialResult")
                }
                "final" -> {
                    val finalResult = msg.optString("text", "")
                    Log.i(tag, "ASR final: $finalResult")
                    _finalText.tryEmit(finalResult)
                    // 延迟关闭连接
                    scope.launch {
                        delay(300)
                        close()
                    }
                }
                "ping" -> {
                    // 回复 pong 心跳
                    val pong = JSONObject().apply {
                        put("type", "pong")
                    }
                    send(pong.toString())
                    Log.d(tag, "ASR pong sent")
                }
                "error" -> {
                    val errorMsg = msg.optString("msg", "ASR 错误")
                    Log.e(tag, "ASR error: $errorMsg")
                    _asrError.tryEmit(errorMsg)
                    close()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "ASR message parse error: ${e.message}")
        }
    }

    override fun onConnected() {
        Log.i(tag, "ASR WebSocket connected")
    }

    override fun onDisconnected(code: Int, reason: String) {
        Log.i(tag, "ASR WebSocket disconnected: $code $reason")
    }

    override fun onError(t: Throwable) {
        Log.e(tag, "ASR WebSocket error", t)
        _asrError.tryEmit(t.message ?: "连接失败")
    }

    /**
     * 重置状态
     */
    fun reset() {
        _partialText.value = ""
    }
}
