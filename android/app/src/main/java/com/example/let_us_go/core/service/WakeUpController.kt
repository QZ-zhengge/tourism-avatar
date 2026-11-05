package com.example.let_us_go.core.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.let_us_go.core.bus.AppEventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 唤醒服务状态（全局单例 UI 状态）
 */
object WakeUpState {
    private val _enabled = MutableStateFlow(false)
    val enabled = _enabled.asStateFlow()
    private val _lastKeyword = MutableStateFlow<String?>(null)
    val lastKeyword = _lastKeyword.asStateFlow()

    fun setEnabled(v: Boolean) { _enabled.value = v }
    fun setLastKeyword(k: String?) { _lastKeyword.value = k }
}

/**
 * 唤醒开关的 Composable 入口。
 * 第一次点开 → 请求 RECORD_AUDIO + POST_NOTIFICATIONS → 启动 WakeUpService
 * 关闭 → 停服务
 */
@Composable
fun WakeUpController(
    modifier: Modifier = Modifier,
    onWakeUp: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val enabled by WakeUpState.enabled.collectAsState()

    // 订阅唤醒事件
    LaunchedEffect(Unit) {
        AppEventBus.events.collect { ev ->
            if (ev is AppEventBus.WakeUpDetected) {
                WakeUpState.setLastKeyword(ev.keyword)
                onWakeUp(ev.keyword)
            }
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = enabled,
            onCheckedChange = { wantOn ->
                if (wantOn) {
                    if (ensurePermissions(context)) {
                        WakeUpService.start(context)
                        WakeUpState.setEnabled(true)
                    }
                } else {
                    WakeUpService.stop(context)
                    WakeUpState.setEnabled(false)
                }
            }
        )
    }
}

private fun ensurePermissions(context: Context): Boolean {
    val need = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED
    ) need += Manifest.permission.RECORD_AUDIO

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) need += Manifest.permission.POST_NOTIFICATIONS
    }

    if (need.isEmpty()) return true
    val activity = (context as? Activity) ?: return false
    ActivityCompat.requestPermissions(activity, need.toTypedArray(), REQ_WAKEUP)
    return false
}

private const val REQ_WAKEUP = 0x7100
