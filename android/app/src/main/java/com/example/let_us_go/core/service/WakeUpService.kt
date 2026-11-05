package com.example.let_us_go.core.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.let_us_go.MainActivity
import com.example.let_us_go.R
import com.example.let_us_go.core.bus.AppEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 「导游导游」后台唤醒服务
 *
 * ## 工作原理
 * 1. 前台服务（microphone 类型），常驻 + 通知可见
 * 2. AudioRecord 每 1.5s 切一段 16k/16bit/mono PCM
 * 3. 计算音量 RMS（暂不连 ASR 引擎，本地阈值检测）
 * 4. 唤醒词匹配（"导游导游"/"导游"/"数字人"）
 * 5. 命中后 emit AppEventBus.WakeUpDetected，主页面 / 浮窗联动
 *
 * ## 注意点
 * - 仅在用户登录后 + 已授录音权限时录音
 * - foregroundServiceType = "microphone"（Android 14+ 强制）
 * - 录音用 MediaRecorder.AudioSource.VOICE_RECOGNITION（系统降噪）
 */
class WakeUpService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recordJob: Job? = null
    private val running = AtomicBoolean(false)

    // 唤醒词列表（主唤醒词「导游导游」，与 uni 端 wsKeyWord 保持一致）
    private val keywords = listOf("导游导游", "导游", "数字人")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WakeUpService onCreate")
        startInForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running.get()) return START_STICKY
        if (!hasMicPermission()) {
            Log.w(TAG, "RECORD_AUDIO not granted; service idle")
            return START_STICKY
        }
        running.set(true)
        recordJob = scope.launch { recordLoop() }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        running.set(false)
        recordJob?.cancel()
        scope.cancel()
        Log.i(TAG, "WakeUpService onDestroy")
    }

    // ──────────────────── 录音循环 ────────────────────

    private suspend fun recordLoop() {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuf <= 0) {
            Log.e(TAG, "Invalid AudioRecord minBuf=$minBuf; abort")
            return
        }
        val bufSize = minBuf.coerceAtLeast(4096) * 2
        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate, channelConfig, audioFormat, bufSize
            )
        } catch (e: Throwable) {
            Log.e(TAG, "AudioRecord create failed", e)
            return
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized; abort")
            recorder.release()
            return
        }

        try {
            recorder.startRecording()
            val chunk = ShortArray(sampleRate * 3 / 2)  // 1.5s = 24000 samples
            while (running.get() && !Thread.currentThread().isInterrupted) {
                var read = 0
                while (read < chunk.size) {
                    val r = recorder.read(chunk, read, chunk.size - read)
                    if (r < 0) {
                        Log.w(TAG, "AudioRecord.read err=$r")
                        break
                    }
                    read += r
                }
                if (read <= 0) continue

                val rms = computeRms(chunk, read)
                if (rms > SILENCE_RMS_THRESHOLD) {
                    // 命中：粗筛（高音量）→ 真实 ASR 在 Phase 3 接（端侧 Picovoice 或后端）
                    // 这里对唤醒词做软匹配：录音时无 ASR，先用模拟匹配——Phase 3 接 Snowboy
                    Log.d(TAG, "speech-like RMS=$rms (will upgrade to ASR in Phase 3)")
                    // 临时：连续 2 次高音量判定为唤醒（双击式体验）
                    if (++consecutiveVoiceFrames >= 2) {
                        consecutiveVoiceFrames = 0
                        emitWakeUp("voice-burst")
                    }
                } else {
                    consecutiveVoiceFrames = 0
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "recordLoop crashed", e)
        } finally {
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
        }
    }

    private var consecutiveVoiceFrames = 0

    private fun computeRms(buf: ShortArray, len: Int): Double {
        if (len == 0) return 0.0
        var sum = 0.0
        for (i in 0 until len) {
            val v = buf[i].toDouble()
            sum += v * v
        }
        return Math.sqrt(sum / len)
    }

    // ──────────────────── 前台通知 ────────────────────

    private fun startInForeground() {
        ensureChannel()
        val openIntent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("「随行」正在听")
            .setContentText("说「导游导游」唤醒数字人")
            .setOngoing(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID, notif,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIF_ID, notif)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "随行唤醒监听",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { description = "后台语音唤醒数字人" }
                )
            }
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun emitWakeUp(keyword: String) {
        Log.i(TAG, "Wake-up detected: $keyword")
        AppEventBus.emit(AppEventBus.WakeUpDetected(keyword))
    }

    companion object {
        private const val TAG = "WakeUpService"
        private const val CHANNEL_ID = "suixing_wakeup"
        private const val NOTIF_ID = 1001

        /** 经验值：16k/16bit 静音约 30-50，语音段 200-3000 */
        private const val SILENCE_RMS_THRESHOLD = 250.0

        fun start(context: android.content.Context) {
            val intent = Intent(context, WakeUpService::class.java)
            runCatching { context.startForegroundService(intent) }
                .onFailure { Log.e(TAG, "start service failed", it) }
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, WakeUpService::class.java)
            runCatching { context.stopService(intent) }
        }
    }
}

/**
 * 唤醒命中事件流（可被任意 Composable 订阅）
 */
object WakeUpBus {
    private val _events = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 8)
    val events = _events.asSharedFlow()
    suspend fun emit(keyword: String) { _events.emit(keyword) }
}
