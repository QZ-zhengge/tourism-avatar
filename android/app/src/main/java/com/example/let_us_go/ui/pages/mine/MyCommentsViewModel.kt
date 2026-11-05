package com.example.let_us_go.ui.pages.mine

import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.api.MyCommentItem
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 我的评论 */
class MyCommentsViewModel : BaseViewModel() {
    private val _comments = MutableStateFlow<List<MyCommentItem>>(emptyList())
    val comments: StateFlow<List<MyCommentItem>> = _comments.asStateFlow()

    fun load() {
        launchIo(showLoading = true) {
            val uid = UserPrefs.userId() ?: 0L
            runCatching {
                val res = NetworkModule.circleApi.listUserComments(userId = uid)
                if (res.isSuccess) res.data ?: emptyList() else emptyList()
            }.onSuccess { _comments.value = it }
        }
    }
}
