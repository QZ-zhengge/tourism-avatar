package com.example.let_us_go.ui.pages.mine

import com.example.let_us_go.core.base.BaseViewModel
import android.util.Log
import com.example.let_us_go.data.model.Post
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 我的帖子 */
class MyPostsViewModel : BaseViewModel() {
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    fun load() {
        launchIo(showLoading = true) {
            val uid = UserPrefs.userId() ?: 0L
            runCatching {
                val res = NetworkModule.circleApi.listUserPosts(userId = uid)
                if (res.isSuccess) res.data ?: emptyList() else emptyList()
            }.onSuccess { _posts.value = it }
        }
    }

    /** 删除帖子（乐观更新 + 后端确认） */
    fun deletePost(post: Post) {
        val original = _posts.value
        _posts.value = _posts.value.filter { it.id != post.id }
        launchIo(showLoading = false) {
            val userId = UserPrefs.userId() ?: 0L
            runCatching {
                val res = NetworkModule.circleApi.deletePost(postId = post.id, userId = userId)
                if (res.isSuccess) {
                    Log.d("MyPostsVM", "删帖成功: postId=${post.id}")
                } else {
                    _posts.value = original // 失败回滚
                    Log.w("MyPostsVM", "删帖失败: ${res.msg}")
                }
            }.onFailure {
                _posts.value = original
                Log.e("MyPostsVM", "删帖异常", it)
            }
        }
    }
}
