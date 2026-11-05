package com.example.uniapp_box.float

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

/**
 * ExoPlayer 单例管理器
 *
 * 功能：
 * - 全局共享 ExoPlayer 实例，避免重复创建
 * - 管理播放器生命周期
 * - 提供预加载支持
 */
object ExoPlayerManager {
    private var exoPlayer: ExoPlayer? = null
    private var referenceCount = 0
    
    /**
     * 获取或创建 ExoPlayer 实例
     */
    @Synchronized
    fun getOrCreate(context: Context): ExoPlayer {
        referenceCount++
        
        return exoPlayer ?: ExoPlayer.Builder(context.applicationContext).build().also {
            exoPlayer = it
        }
    }
    
    /**
     * 释放 ExoPlayer 实例（当引用计数为0时）
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
    
    /**
     * 强制释放 ExoPlayer 实例
     */
    @Synchronized
    fun forceRelease() {
        exoPlayer?.release()
        exoPlayer = null
        referenceCount = 0
    }
    
    /**
     * 预加载视频资源
     */
    fun preload(context: Context) {
        // 预加载逻辑可以在需要时实现
        // 例如：预先创建 ExoPlayer 并加载媒体项
    }
}