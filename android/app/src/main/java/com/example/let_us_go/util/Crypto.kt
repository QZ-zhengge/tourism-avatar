package com.example.let_us_go.util

import android.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 讯飞数字人鉴权加密工具
 *
 * 替代 uni-app `utils/xunfei-ws.js` 中 120+ 行的自实现 HMAC-SHA256 + Base64。
 *
 * 讯飞 WebSocket 鉴权流程：
 * 1. 构造签名原文（RFC 3339 格式）
 *    `host: <host>\ndate: <date>\nGET <path> HTTP/1.1`
 * 2. 用 API_SECRET 对签名原文做 HMAC-SHA256，得到字节数组
 * 3. 对字节数组做 Base64 编码 → signature
 * 4. 构造 authorization 原文
 *    `api_key="<key>", algorithm="hmac-sha256", headers="host date request-line", signature="<sig>"`
 * 5. 对 authorization 原文做 Base64 编码 → authorization
 *
 * 与 JS 版行为完全一致（已通过 uni 端 test_sha256_v2.js 验证过 RFC 向量）。
 */
object Crypto {

    private const val HMAC_ALG = "HmacSHA256"

    /**
     * HMAC-SHA256
     * @param key 密钥（API_SECRET）
     * @param data 明文字符串
     * @return 32 字节摘要
     */
    fun hmacSha256(key: String, data: String): ByteArray {
        val mac = Mac.getInstance(HMAC_ALG)
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), HMAC_ALG)
        mac.init(secretKey)
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    /**
     * Base64 编码（标准）
     */
    fun base64Encode(input: ByteArray): String =
        Base64.encodeToString(input, Base64.NO_WRAP)

    fun base64Encode(input: String): String =
        Base64.encodeToString(input.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    /**
     * Base64 解码
     */
    fun base64Decode(input: String): ByteArray =
        Base64.decode(input, Base64.DEFAULT)

    /**
     * 生成讯飞 WebSocket 鉴权参数
     *
     * @param apiKey 讯飞 API_KEY
     * @param apiSecret 讯飞 API_SECRET
     * @param host 主机（avatar.cn-huadong-1.xf-yun.com）
     * @param path 路径（/v1/interact_lips_flow）
     * @return Pair(date, authorization)
     */
    fun generateXunfeiAuth(
        apiKey: String,
        apiSecret: String,
        host: String,
        path: String
    ): Pair<String, String> {
        val now = java.util.Date()
        val dateStr = formatRfcDate(now)

        val signatureOrigin = "host: $host\ndate: $dateStr\nGET $path HTTP/1.1"

        // 1. HMAC-SHA256
        val signatureBytes = hmacSha256(apiSecret, signatureOrigin)
        // 2. signature 字节数组 Base64
        val signature = base64Encode(signatureBytes)

        // 3. 构造 authorization 原文
        val authorizationOrigin =
            "api_key=\"$apiKey\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"$signature\""

        // 4. authorization 字符串整体 Base64
        val authorization = base64Encode(authorizationOrigin)

        return dateStr to authorization
    }

    /**
     * 格式化 GMT 字符串（RFC 7231 / RFC 3339 风格）
     * 例：`Sun, 21 Jun 2026 12:00:00 GMT`
     */
    fun formatRfcDate(date: java.util.Date = java.util.Date()): String {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT"), java.util.Locale.US)
        cal.time = date
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val months = arrayOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        val dayOfWeek = days[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val month = months[cal.get(java.util.Calendar.MONTH)]
        val year = cal.get(java.util.Calendar.YEAR)
        val hh = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val mm = cal.get(java.util.Calendar.MINUTE)
        val ss = cal.get(java.util.Calendar.SECOND)
        return String.format(
            "%s, %02d %s %d %02d:%02d:%02d GMT",
            dayOfWeek, day, month, year, hh, mm, ss
        )
    }

    /**
     * SHA-256 摘要（hex 字符串）
     */
    fun sha256Hex(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * MD5（hex 字符串）
     */
    fun md5Hex(input: String): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
