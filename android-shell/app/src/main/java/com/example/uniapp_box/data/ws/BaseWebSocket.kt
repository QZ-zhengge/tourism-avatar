package com.example.uniapp_box.data.ws

import android.util.Log
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
import okio.ByteString

/**
 * WebSocket 抽象基类
 *
 * 替代 uni-app `uni.connectSocket` + `uni.onSocketMessage` / `uni.onSocketOpen` /
 * `uni.onSocketClose` / `uni.onSocketError` / `uni.sendSocketMessage` / `uni.closeSocket`。
 *
 * 用法：
 * ```
 * class AsrSocket : BaseWebSocket() {
 *     suspend fun connect(url: String) = open(url)
 *
 *     override fun onMessage(text: String) {
 *         // 处理 ASR 识别结果
 *     }
 * }
 *
 * val socket = AsrSocket().also { it.connect("ws://server/asr") }
 * socket.send("hello")
 * ```
 *
 * 设计要点：
 * - 自动重连（指数退避，最大 30s）
 * - 可选心跳（默认 12s，与 xunfei-ws.js 行为一致）
 * - 状态通过 StateFlow 暴露（连接/断连/重连/错误）
 * - 文本消息通过 SharedFlow 暴露（业务订阅）
 */
abstract class BaseWebSocket(
    private val pingIntervalMs: Long = 12_000L,
    private val autoReconnect: Boolean = true,
    private val maxReconnectAttempts: Int = 5
) {
    protected val tag: String = this::class.java.simpleName

    enum class State { IDLE, CONNECTING, CONNECTED, RECONNECTING, CLOSED, ERROR }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _errors = MutableSharedFlow<Throwable>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val errors: SharedFlow<Throwable> = _errors.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var pingJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var currentUrl: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 打开连接
     */
    protected fun open(url: String) {
        currentUrl = url
        connect()
    }

    private fun connect() {
        val url = currentUrl ?: return
        if (_state.value == State.CONNECTED || _state.value == State.CONNECTING) return
        _state.value = if (reconnectAttempts > 0) State.RECONNECTING else State.CONNECTING

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(tag, "WebSocket opened: $currentUrl")
            _state.value = State.CONNECTED
            reconnectAttempts = 0
            startPing()
            onConnected()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            _messages.tryEmit(text)
            onMessage(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            val text = bytes.utf8()
            _messages.tryEmit(text)
            onMessage(text)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.w(tag, "WebSocket closing: code=$code reason=$reason")
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(tag, "WebSocket closed: code=$code reason=$reason")
            _state.value = State.CLOSED
            stopPing()
            onDisconnected(code, reason)
            if (autoReconnect && code != 1000) {
                scheduleReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(tag, "WebSocket failure", t)
            _state.value = State.ERROR
            _errors.tryEmit(t)
            stopPing()
            onError(t)
            if (autoReconnect) {
                scheduleReconnect()
            }
        }
    }

    /**
     * 发送文本消息
     */
    fun send(text: String): Boolean {
        val ws = webSocket ?: run {
            Log.w(tag, "send ignored, socket null")
            return false
        }
        return ws.send(text)
    }

    /**
     * 发送二进制
     */
    fun send(bytes: ByteString): Boolean {
        val ws = webSocket ?: return false
        return ws.send(bytes)
    }

    /**
     * 关闭连接（code 1000 不会触发重连）
     */
    fun close(code: Int = 1000, reason: String? = null) {
        autoReconnect  // suppress warning
        webSocket?.close(code, reason ?: "client close")
        _state.value = State.CLOSED
        stopPing()
        reconnectJob?.cancel()
    }

    /**
     * 释放资源
     */
    fun release() {
        close()
        scope.coroutineContext[Job]?.cancel()
    }

    // ─────────────── 心跳 ───────────────

    private fun startPing() {
        stopPing()
        if (pingIntervalMs <= 0) return
        pingJob = scope.launch {
            while (_state.value == State.CONNECTED) {
                delay(pingIntervalMs)
                if (_state.value == State.CONNECTED) {
                    onPing()
                }
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        pingJob = null
    }

    // ─────────────── 重连 ───────────────

    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            Log.w(tag, "Reconnect max attempts reached ($maxReconnectAttempts), giving up")
            return
        }
        reconnectAttempts++
        val delayMs = (1000L * (1 shl reconnectAttempts.coerceAtMost(5))).coerceAtMost(30_000L)
        Log.i(tag, "Reconnecting in ${delayMs}ms (attempt $reconnectAttempts/$maxReconnectAttempts)")
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delayMs)
            connect()
        }
    }

    // ─────────────── 子类回调 ───────────────

    protected open fun onConnected() {}
    protected open fun onDisconnected(code: Int, reason: String) {}
    protected open fun onMessage(text: String) {}
    protected open fun onError(t: Throwable) {}
    protected open fun onPing() {
        // 默认发送 ping 帧
        webSocket?.send("{\"header\":{\"ctrl\":\"ping\"}}")
    }

    /**
     * 子类需提供 OkHttpClient（复用全局，连接池/拦截器共享）
     */
    protected abstract val okHttpClient: OkHttpClient
}