package com.example.let_us_go.ui.pages.mine

import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.Suggestion
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 意见反馈（建议信箱） */
class FeedbackViewModel : BaseViewModel() {

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()

    fun load() {
        launchIo(showLoading = true) {
            val uid = UserPrefs.userId() ?: 0L
            runCatching {
                val res = NetworkModule.suggestionApi.listByUser(userId = uid)
                if (res.isSuccess) res.data ?: emptyList() else emptyList()
            }.onSuccess { _suggestions.value = it }
        }
    }

    fun submit(content: String, contact: String? = null) {
        val text = content.trim()
        if (text.isBlank()) {
            setError("请输入反馈内容")
            return
        }
        val uid = UserPrefs.userIdSync() ?: run {
            setError("请先登录后再提交反馈")
            return
        }
        _submitting.value = true
        launchIo(showLoading = false) {
            try {
                val res = NetworkModule.suggestionApi.create(
                    com.example.let_us_go.data.api.CreateSuggestionBody(
                        user_id = uid,
                        content = text,
                        contact = contact?.takeIf { it.isNotBlank() }
                    )
                )
                if (res.isSuccess) {
                    load()
                } else {
                    setError(res.msg ?: "提交失败")
                }
            } catch (e: Throwable) {
                setError(e.message ?: "提交异常")
            } finally {
                _submitting.value = false
            }
        }
    }
}
