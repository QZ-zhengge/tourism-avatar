package com.example.let_us_go.data.ws

import com.example.let_us_go.BuildConfig
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.util.Crypto
import okhttp3.OkHttpClient

/**
 * 讯飞数字人 WebSocket 客户端
 *
 * 替代 uni 端 `utils/xunfei-ws.js` 的全部功能：
 * - HMAC-SHA256 鉴权
 * - start / text_interact / text_driver / ping / stop 协议
 * - 12 秒心跳（与 JS 端 pingInterval 12s 一致）
 * - 自动重连
 *
 * 实际场景下，Android 端走**后端代理**（POST /avatar/start 等），
 * 直接调讯飞 WS 的场景主要在调试模式或后端代理不可用时。
 */
class XunfeiAvatarWebSocket : BaseWebSocket(pingIntervalMs = 12_000L) {

    override val okHttpClient: OkHttpClient = NetworkModule.okHttp

    private var appId: String = BuildConfig.XUNFEI_APP_ID
    private var apiKey: String = BuildConfig.XUNFEI_API_KEY
    private var apiSecret: String = BuildConfig.XUNFEI_API_SECRET
    private var sceneId: String = BuildConfig.XUNFEI_SCENE_ID
    private var avatarId: String = "111260002"     // 雨菲（与 JS 端默认值一致）
    private var vcn: String = "xiaoyan"

    /**
     * 打开连接（带鉴权）
     */
    fun connect(
        avatarId: String = this.avatarId,
        vcn: String = this.vcn,
        sceneId: String? = this.sceneId
    ) {
        this.avatarId = avatarId
        this.vcn = vcn
        sceneId?.let { this.sceneId = it }
        val url = buildAuthorizedUrl()
        open(url)
    }

    /**
     * 构造带鉴权参数的 WebSocket URL
     */
    private fun buildAuthorizedUrl(): String {
        val host = BuildConfig.XUNFEI_WS_HOST
        val path = BuildConfig.XUNFEI_WS_PATH
        val (date, authorization) = Crypto.generateXunfeiAuth(apiKey, apiSecret, host, path)
        return "wss://$host$path" +
            "?authorization=${java.net.URLEncoder.encode(authorization, "UTF-8")}" +
            "&date=${java.net.URLEncoder.encode(date, "UTF-8")}" +
            "&host=${java.net.URLEncoder.encode(host, "UTF-8")}" +
            "&appid=$appId"
    }

    /**
     * 发送 start 协议
     */
    fun sendStart() {
        val protocol = """
            {
                "header": {
                    "app_id": "CHANGE_ME",
                    "request_id": "dh_${System.currentTimeMillis()}",
                    "ctrl": "start",
                    "scene_id": "$sceneId",
                    "uid": ""
                },
                "parameter": {
                    "avatar": {
                        "stream": {"protocol": "xrtc", "fps": 25, "bitrate": 2000, "alpha": 1},
                        "avatar_id": "$avatarId",
                        "width": 960,
                        "height": 720,
                        "format": "rgb24",
                        "scale": 2
                    },
                    "tts": {"vcn": "$vcn", "speed": 50, "volume": 50, "pitch": 50},
                    "vc": {"vc": 1, "vcn": "$vcn"},
                    "asr": {},
                    "dispatch": {"interactive_mode": 1, "content_analysis": 1}
                },
                "payload": {}
            }
        """.trimIndent()
        send(protocol)
    }

    /**
     * 发送文本交互（语义理解）
     */
    fun sendTextInteract(text: String) {
        if (text.isBlank()) return
        val escaped = text.replace("\"", "\\\"").replace("\n", "\\n")
        val protocol = """
            {
                "header": {"app_id": "CHANGE_ME", "request_id": "txt_${System.currentTimeMillis()}", "ctrl": "text_interact"},
                "parameter": {
                    "tts": {"vcn": "$vcn", "speed": 50, "volume": 50},
                    "air": {"air": 1, "add_nonsemantic": 1}
                },
                "payload": {"text": {"content": "$escaped"}}
            }
        """.trimIndent()
        send(protocol)
    }

    /**
     * 发送文本驱动（不走语义）
     */
    fun sendTextDriver(text: String) {
        if (text.isBlank()) return
        val escaped = text.replace("\"", "\\\"").replace("\n", "\\n")
        val protocol = """
            {
                "header": {"app_id": "CHANGE_ME", "request_id": "drv_${System.currentTimeMillis()}", "ctrl": "text_driver"},
                "parameter": {
                    "avatar_dispatch": {"interactive_mode": 1},
                    "tts": {"vcn": "$vcn", "speed": 50, "volume": 50},
                    "air": {"air": 1, "add_nonsemantic": 1}
                },
                "payload": {"text": {"content": "$escaped"}}
            }
        """.trimIndent()
        send(protocol)
    }

    override fun onPing() {
        // 讯飞要求的 ping 协议
        val protocol = """{"header":{"app_id":"CHANGE_ME","request_id":"ping_${System.currentTimeMillis()}","ctrl":"ping"}}"""
        send(protocol)
    }

    /**
     * 解析收到的消息，提取关键信息
     */
    override fun onMessage(text: String) {
        // 业务可重写此方法，或订阅 messages SharedFlow
    }
}
