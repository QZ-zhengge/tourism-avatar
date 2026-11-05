package com.example.let_us_go.core.base

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel 基类
 *
 * 提供：
 * - 通用 loading 状态
 * - 通用错误状态（与 Toast 解耦）
 * - 协程快捷方法
 *
 * 子类示例：
 * ```
 * class HomeViewModel : BaseViewModel() {
 *     private val _pois = MutableStateFlow<List<Poi>>(emptyList())
 *     val pois: StateFlow<List<Poi>> = _pois
 *
 *     fun load() = launchIo({
 *         val list = api.getPois()
 *         _pois.value = list
 *     }, onError = { e -> setError(e.message ?: "加载失败") })
 * }
 * ```
 */
abstract class BaseViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    protected fun setLoading(value: Boolean) {
        _loading.value = value
    }

    protected fun setError(msg: String?) {
        _error.value = msg
    }

    protected fun clearError() {
        _error.value = null
    }

    /**
     * 在 IO 线程上启动协程，自动管理 loading 和 error。
     * 返回 Job，调用方可以 cancel()。
     */
    protected fun launchIo(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        showLoading: Boolean = true,
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                if (showLoading) setLoading(true)
                withContext(dispatcher) { block() }
            } catch (e: Throwable) {
                val msg = e.message ?: "未知错误"
                Log.e("BaseViewModel", "launchIo error", e)
                setError(msg)
                onError?.invoke(e)
            } finally {
                if (showLoading) setLoading(false)
            }
        }
    }

    /**
     * 通用协程启动
     */
    protected fun launchSafe(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Throwable) {
                val msg = e.message ?: "未知错误"
                Log.e("BaseViewModel", "launchIo error", e)
                setError(msg)
                onError?.invoke(e)
            }
        }
    }
}
