package com.example.let_us_go.ui.pages.mine

import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.RealtimeExplainHistory
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 数字人对话历史（实时讲解历史） */
class DhHistoryViewModel : BaseViewModel() {
    private val _history = MutableStateFlow<List<RealtimeExplainHistory>>(emptyList())
    val history: StateFlow<List<RealtimeExplainHistory>> = _history.asStateFlow()

    fun load() {
        launchIo(showLoading = true) {
            val uid = UserPrefs.userId() ?: 0L
            runCatching {
                val res = NetworkModule.realtimeExplainApi.listByUser(userId = uid)
                if (res.isSuccess) res.data?.items ?: emptyList() else emptyList()
            }.onSuccess { _history.value = it }
        }
    }
}
