package com.example.let_us_go.core.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.datasource.rtmp.RtmpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 数字人播放器 Holder（Phase 2 新增）
 *
 * 解决「Compose 重组时如何保留 ExoPlayer 实例」问题：
 * - [remember] 持有一个 Holder 实例，Holder 内部持有一个 ExoPlayer
 * - Holder 暴露 [player] / [playbackState] / [errorMessage] / [isPlaying] 等 StateFlow
 * - 屏幕销毁时（DisposableEffect onDispose）调用 [release] 释放资源
 *
 * 支持协议优先级：HLS/HTTP > RTSP > RTMP（通过 media3-datasource-rtmp）
 */
class DigitalHumanPlayerHolder(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val _player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        playWhenReady = true
        volume = 1.0f
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _playbackState.value = state
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    requestAudioFocus()
                } else {
                    abandonAudioFocus()
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _errorMessage.value = "${error.errorCodeName}: ${error.message}"
                Log.e("PlayerHolder", "Player error: ${error.errorCodeName}: ${error.message}")
            }
        })
    }

    val player: Player get() = _player

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus() {
        if (audioFocusRequest != null) return

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        // 永久失去焦点，暂停播放
                        _player.pause()
                        Log.d("PlayerHolder", "Audio focus lost, pausing")
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        // 短暂失去焦点（如电话），暂停播放
                        _player.pause()
                        Log.d("PlayerHolder", "Audio focus lost transient, pausing")
                    }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        // 短暂失去焦点但可以降低音量
                        _player.volume = 0.3f
                        Log.d("PlayerHolder", "Audio focus lost transient can duck, volume reduced")
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        // 重新获得焦点，恢复播放
                        _player.volume = 1.0f
                        _player.play()
                        Log.d("PlayerHolder", "Audio focus gained, resuming")
                    }
                }
            }
            .build()

        val result = audioManager.requestAudioFocus(audioFocusRequest!!)
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("PlayerHolder", "Audio focus granted")
        } else {
            Log.w("PlayerHolder", "Audio focus request denied")
        }
    }

    /**
     * 释放音频焦点
     */
    private fun abandonAudioFocus() {
        audioFocusRequest?.let {
            audioManager.abandonAudioFocusRequest(it)
            audioFocusRequest = null
            Log.d("PlayerHolder", "Audio focus abandoned")
        }
    }

    /**
     * 准备播放（自动选 HLS/HTTP > RTSP > RTMP）
     * @param primary 主链路（streamUrl，可能是 rtmp:// 或 https://）
     * @param fallback 备链路（playUrl，通常是 HTTP 格式）
     */
    fun prepare(primary: String?, fallback: String? = null) {
        _errorMessage.value = null
        try {
            // 优先级：fallback(HTTP) > primary(HTTP/HLS) > primary(RTSP) > primary(RTMP)
            val url = fallback?.takeIf { it.isNotBlank() && isHttp(it) }
                ?: primary?.takeIf { it.isNotBlank() && isHttp(it) }
                ?: primary?.takeIf { it.startsWith("rtsp://") }
                ?: primary?.takeIf { it.startsWith("rtmp://") }

            if (url == null) {
                _errorMessage.value = "无可用播放地址"
                return
            }

            when {
                url.startsWith("rtmp://") -> playRtmp(url)
                else -> playDefault(url)
            }
        } catch (e: Throwable) {
            _errorMessage.value = "播放初始化失败：${e.message}"
        }
    }

    /** HTTP/HTTPS/RTSP → 默认 MediaSource（支持 HLS/DASH/MP4/RTSP） */
    private fun playDefault(url: String) {
        val mediaItem = androidx.media3.common.MediaItem.fromUri(url)
        _player.setMediaItem(mediaItem)
        _player.prepare()
        _player.play()
    }

    /** RTMP → 使用 RtmpDataSource（需要 media3-datasource-rtmp 依赖） */
    private fun playRtmp(url: String) {
        val dataSourceFactory = RtmpDataSource.Factory()
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(androidx.media3.common.MediaItem.fromUri(url))
        _player.setMediaSource(mediaSource)
        _player.prepare()
        _player.play()
    }

    private fun isHttp(url: String): Boolean =
        url.startsWith("http://") || url.startsWith("https://")

    fun play() = _player.play()

    fun pause() = _player.pause()

    /**
     * 覆盖页（实时讲解）音频控制：
     * 覆盖页复用数字人 ExoPlayer 流播放 50080 TTS，
     * 通过静音该播放器实现「退出即停」与「播放/暂停」。
     *
     * @param muted true=静音（立即听不到），false=恢复音量
     */
    fun setMuted(muted: Boolean) {
        // ExoPlayer 必须在创建线程（主线程）访问；调用方可能在协程(IO)里调用，统一切回主线程
        Handler(Looper.getMainLooper()).post {
            _player.volume = if (muted) 0f else 1.0f
        }
    }

    fun stop() {
        _player.stop()
        _player.clearMediaItems()
    }

    fun release() {
        abandonAudioFocus()
        _player.release()
    }
}
