package com.example.let_us_go.core.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.let_us_go.data.network.NetworkModule

/**
 * 数字人播放器（Phase 2 重构）
 *
 * 基于 Media3 ExoPlayer 实现，替代 Phase 1 的 [android.widget.VideoView]。
 *
 * 支持协议（按优先级自动选择）：
 * 1. HLS (m3u8) — ExoPlayer 原生支持
 * 2. RTSP — ExoPlayer 原生支持
 * 3. HTTP/HTTPS FLV/MP4 — ExoPlayer 原生支持
 * 4. RTMP — ExoPlayer **不**原生支持，自动 fallback 到 play_url（HTTP 链路）
 *
 * RTMP 处理策略：
 * - 后端 `/avatar/start` 返回 `protocol=rtmp` + `stream_url=rtmp://...` 时，
 *   此播放器自动尝试用 `play_url`（HTTP/HTTPS FLV 或 HLS）播放。
 * - 完整的 RTMP→HTTP 转换需要在后端用 nginx-rtmp / SRS / mediasoup 代理。
 * - 客户端无法在 JVM 层做 RTMP 解封装（需 NDK + librtmp.so）。
 *
 * 暴露：
 * - [exoPlayer] — 真正的 Player 实例，可给 [androidx.media3.ui.PlayerView] 用
 * - [playbackState] / [isPlaying] — 状态流
 * - [errorMessage] — 错误流
 */
class DigitalHumanPlayer(private val context: Context) {

    private val okHttpFactory = OkHttpDataSource.Factory(NetworkModule.okHttp)
        .setUserAgent("SuiXing-DigitalHuman/1.0")

    private val dataSourceFactory = DefaultDataSource.Factory(context, okHttpFactory)

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = state
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlayerError(error: PlaybackException) {
            _errorMessage.value = "${error.errorCodeName}: ${error.message}"
        }
    }

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(buildMediaSourceFactory())
        .build()
        .apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1.0f
            addListener(playerListener)
        }

    private val _playbackState = kotlinx.coroutines.flow.MutableStateFlow(Player.STATE_IDLE)
    val playbackState: kotlinx.coroutines.flow.StateFlow<Int> = _playbackState

    private val _isPlaying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPlaying: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPlaying

    private val _errorMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val errorMessage: kotlinx.coroutines.flow.StateFlow<String?> = _errorMessage

    /**
     * 播放 URL（自动选择 HLS / HTTP / RTSP / RTMP fallback）
     *
     * @param url 主链路（通常是 stream_url）
     * @param fallbackUrl 备用链路（通常是 play_url，HTTP/HTTPS）
     */
    fun prepare(url: String, fallbackUrl: String? = null) {
        val mediaSource = buildMediaSource(url, fallbackUrl)
        exoPlayer.setMediaSource(mediaSource, true)
        exoPlayer.prepare()
        _errorMessage.value = null
    }

    private fun buildMediaSource(url: String, fallbackUrl: String?): MediaSource {
        val uri = Uri.parse(url)
        return when (uri.scheme?.lowercase()) {
            "rtmp" -> {
                // RTMP 不被原生支持，尝试 fallback
                fallbackUrl?.let { buildMediaSource(it, null) }
                    ?: throw IllegalArgumentException("RTMP requires fallbackUrl. Backend should proxy RTMP→HTTP.")
            }
            "rtsp" -> RtspMediaSource.Factory().createMediaSource(MediaItem.fromUri(uri))
            "http", "https" -> {
                if (url.endsWith(".m3u8", ignoreCase = true) || url.contains("/hls/")) {
                    HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
                } else {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri))
                }
            }
            else -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
        }
    }

    private fun buildMediaSourceFactory() = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    fun release() {
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}