package com.example.let_us_go.ui.pages.mine

import android.util.Log
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.AvatarChatHistoryItem
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 数字人对话历史 */
class AvatarChatHistoryViewModel : BaseViewModel() {
    private val _items = MutableStateFlow<List<AvatarChatHistoryItem>>(emptyList())
    val items: StateFlow<List<AvatarChatHistoryItem>> = _items.asStateFlow()

    fun load() {
        launchIo(showLoading = true) {
            val uid = UserPrefs.userId() ?: 0L
            Log.d("AvatarChatHistoryVM", "加载数字人对话历史: userId=$uid")
            runCatching {
                val res = NetworkModule.digitalHumanApi.listChatHistory(userId = uid)
                Log.d("AvatarChatHistoryVM", "响应: code=${res.code}, msg=${res.msg}, total=${res.data?.total}, items=${res.data?.items?.size}")
                if (res.isSuccess) res.data?.items ?: emptyList() else emptyList()
            }.onSuccess {
                Log.d("AvatarChatHistoryVM", "成功加载 ${it.size} 条对话历史")
                _items.value = it
            }.onFailure {
                Log.e("AvatarChatHistoryVM", "加载失败", it)
            }
        }
    }
}
