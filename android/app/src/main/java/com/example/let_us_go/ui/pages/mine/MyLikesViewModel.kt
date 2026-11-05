package com.example.let_us_go.ui.pages.mine

import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.api.LikedPostItem
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 我的点赞（我点赞过的帖子） */
class MyLikesViewModel : BaseViewModel() {
    private val _posts = MutableStateFlow<List<LikedPostItem>>(emptyList())
    val posts: StateFlow<List<LikedPostItem>> = _posts.asStateFlow()

    fun load() {
        launchIo(showLoading = true) {
            val uid = UserPrefs.userId() ?: 0L
            runCatching {
                val res = NetworkModule.circleApi.listUserLikes(userId = uid)
                if (res.isSuccess) res.data ?: emptyList() else emptyList()
            }.onSuccess { _posts.value = it }
        }
    }
}
