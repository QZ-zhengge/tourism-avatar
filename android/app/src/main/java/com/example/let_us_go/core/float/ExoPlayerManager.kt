package com.example.let_us_go.core.float

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.let_us_go.R

/**
 * ExoPlayer 单例管理器（悬浮窗视频复用）
 *
 * 移植自 uniapp_box，避免 FloatingWindow 每次重组都重建/释放播放器，
 * 让循环播放的悬浮窗视频更平滑、更省资源。
 *
 * - 全局共享 ExoPlayer 实例（引用计数）
 * - 媒体源固定为 res/raw/floating_video
 * - 悬浮窗销毁时仅暂停（playWhenReady=false），不释放，下次复用
 */
object ExoPlayerManager {
    private var exoPlayer: ExoPlayer? = null
    private var referenceCount = 0

    /**
     * 获取或创建 ExoPlayer 实例（自动加载悬浮窗视频）
     */
    @Synchronized
    fun getOrCreate(context: Context): ExoPlayer {
        referenceCount++
        return exoPlayer ?: ExoPlayer.Builder(context.applicationContext).build().also {
            val mediaItem = MediaItem.fromUri(
                Uri.parse("android.resource://${context.packageName}/${R.raw.floating_video}")
            )
            it.setMediaItem(mediaItem)
            it.repeatMode = Player.REPEAT_MODE_ALL
            it.playWhenReady = true
            it.volume = 0f
            it.prepare()
            exoPlayer = it
        }
    }

    /**
     * 引用计数减一；归零时才真正释放
     */
    @Synchronized
    fun release() {
        referenceCount--
        if (referenceCount <= 0) {
            exoPlayer?.release()
            exoPlayer = null
            referenceCount = 0
        }
    }

    /** 悬浮窗退出时仅暂停，保留实例以便快速复用 */
    fun pause() {
        exoPlayer?.playWhenReady = false
    }

    /** 强制释放（进程退出时调用） */
    @Synchronized
    fun forceRelease() {
        exoPlayer?.release()
        exoPlayer = null
        referenceCount = 0
    }
}
