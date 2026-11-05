package com.example.let_us_go.ui.pages.mine

import com.example.let_us_go.SuiXingApp
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.ChatMessage
import com.example.let_us_go.data.prefs.DhConversationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 数字人对话历史详情 ViewModel */
class AvatarChatDetailViewModel : BaseViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _record = MutableStateFlow<DhConversationStore.ConversationRecord?>(null)
    val record: StateFlow<DhConversationStore.ConversationRecord?> = _record.asStateFlow()

    /** 从本地存储加载指定会话的消息 */
    fun load(sessionId: String) {
        launchIo(showLoading = false) {
            val r = DhConversationStore.read(SuiXingApp.instance, sessionId)
            _record.value = r
            _messages.value = r.messages
        }
    }
}
