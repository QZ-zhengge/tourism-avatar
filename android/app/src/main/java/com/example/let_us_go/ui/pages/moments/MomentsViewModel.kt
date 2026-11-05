package com.example.let_us_go.ui.pages.moments

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.let_us_go.BuildConfig
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.api.BindMediaBody
import com.example.let_us_go.data.api.CreateCommentBody
import com.example.let_us_go.data.api.CreatePostBody
import com.example.let_us_go.data.api.MediaItem
import com.example.let_us_go.data.api.ToggleLikeBody
import com.example.let_us_go.data.model.Comment
import com.example.let_us_go.data.model.Post
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 旅友圈 ViewModel
 *
 * 复刻 uni 端 utils/api.js 旅友圈接口 + pages/moments/index.js 逻辑：
 * - 帖子列表（分页）
 * - 发帖：先上传图片（POST /upload/image），再创建帖子，再绑定媒体
 * - 点赞（乐观更新 + 后端确认）
 * - 评论
 * - 删帖
 * - 草稿箱（本地 DataStore 持久化）
 */
class MomentsViewModel : BaseViewModel() {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val draftsAdapter: JsonAdapter<List<DraftItem>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, DraftItem::class.java)
    )

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _drafts = MutableStateFlow<List<DraftItem>>(emptyList())
    val drafts: StateFlow<List<DraftItem>> = _drafts.asStateFlow()

    /** 发布中状态（含图片上传） */
    private val _publishing = MutableStateFlow(false)
    val publishing: StateFlow<Boolean> = _publishing.asStateFlow()

    private var currentPage = 1
    private val pageSize = 20

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    init {
        // 恢复草稿
        launchIo(showLoading = false) {
            val json = UserPrefs.draftsJson()
            if (!json.isNullOrBlank()) {
                _drafts.value = runCatching {
                    draftsAdapter.fromJson(json)
                }.getOrNull() ?: emptyList()
            }
        }
    }

    fun loadAll() {
        loadPosts()
    }

    fun loadPosts() {
        currentPage = 1
        launchIo(showLoading = false) {
            val userId = UserPrefs.userId() ?: 0L
            val res = NetworkModule.circleApi.listPosts(
                page = currentPage,
                pageSize = pageSize,
                currentUserId = userId
            )
            if (res.isSuccess && res.data != null) {
                _posts.value = res.data
                _hasMore.value = res.data.size >= pageSize
            } else {
                Log.w("MomentsVM", "loadPosts failed: ${res.msg}")
            }
        }
    }

    fun loadMore() {
        if (!_hasMore.value) return
        launchIo(showLoading = false) {
            currentPage++
            val userId = UserPrefs.userId() ?: 0L
            val res = NetworkModule.circleApi.listPosts(
                page = currentPage,
                pageSize = pageSize,
                currentUserId = userId
            )
            if (res.isSuccess && res.data != null) {
                _posts.value = _posts.value + res.data
                _hasMore.value = res.data.size >= pageSize
            } else {
                currentPage--
            }
        }
    }

    fun toggleLike(post: Post) {
        // 乐观更新（调用方已做登录校验，未登录不会走到这里）
        val original = post
        _posts.value = _posts.value.map {
            if (it.id == post.id) {
                val newLiked = !it.isLiked
                it.copy(
                    isLiked = newLiked,
                    likeCount = it.likeCount + (if (newLiked) 1 else -1)
                )
            } else it
        }
        launchIo(showLoading = false) {
            val userId = UserPrefs.userId() ?: return@launchIo
            val res = NetworkModule.circleApi.toggleLike(
                ToggleLikeBody(post_id = post.id, user_id = userId)
            )
            // 后端返回扁平结构 {code,msg,is_liked,like_count}，无 data 包裹
            if (res.code == 200) {
                _posts.value = _posts.value.map {
                    if (it.id == post.id) {
                        it.copy(isLiked = res.liked, likeCount = res.likeCount)
                    } else it
                }
            } else {
                // 失败：回滚到服务端权威状态（原始值）
                _posts.value = _posts.value.map {
                    if (it.id == original.id) {
                        it.copy(isLiked = original.isLiked, likeCount = original.likeCount)
                    } else it
                }
            }
        }
    }

    /**
     * 发布帖子（含图片上传）
     *
     * 流程：
     * 1. 将 Uri 列表依次上传 POST /upload/image，拿到服务器 URL
     * 2. POST /circle/posts/ 创建帖子
     * 3. POST /circle/posts/{id}/media 绑定图片
     *
     * @param text        正文
     * @param imageUris   本地 content:// URI 列表（可为空）
     * @param locationName 位置名称（可选）
     * @param context     用于读取 ContentResolver（上传图片需要）
     */
    fun publishPost(
        text: String,
        imageSources: List<ImageSource>,
        locationName: String?,
        context: Context
    ) {
        if (_publishing.value) return
        _publishing.value = true
        launchIo(showLoading = false) {
            try {
                val userId = UserPrefs.userId() ?: return@launchIo

                // Step 1：远程图直用、本地图上传，合并绝对 URL 列表
                val remoteUrls = imageSources.filterIsInstance<ImageSource.Remote>().map { it.url }
                val localUris = imageSources.filterIsInstance<ImageSource.Local>().map { it.uri }
                val uploadedUrls = remoteUrls + uploadUris(localUris, context)

                // Step 2：创建帖子
                val createRes = NetworkModule.circleApi.createPost(
                    CreatePostBody(
                        user_id = userId,
                        content = text,
                        location_name = locationName,
                        visibility = 0
                    )
                )
                if (!createRes.isSuccess || createRes.data == null) {
                    Log.w("MomentsVM", "createPost failed: ${createRes.msg}")
                    return@launchIo
                }

                // Step 3：绑定图片到帖子
                if (uploadedUrls.isNotEmpty()) {
                    val bindRes = NetworkModule.circleApi.bindPostMedia(
                        postId = createRes.data.postId,
                        body = BindMediaBody(
                            images = uploadedUrls.mapIndexed { idx, url ->
                                MediaItem(url = url, sort_order = idx)
                            }
                        )
                    )
                    if (!bindRes.isSuccess) {
                        Log.w("MomentsVM", "bindMedia failed: ${bindRes.msg}")
                    }
                }

                // 刷新列表
                loadPosts()
            } finally {
                _publishing.value = false
            }
        }
    }

    /**
     * 将本地图片 Uri 批量上传，返回绝对 URL 列表（跳过失败项）
     */
    private suspend fun uploadUris(uris: List<Uri>, context: Context): List<String> {
        val out = mutableListOf<String>()
        for (uri in uris) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: continue
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = when {
                    mimeType.contains("png") -> "png"
                    mimeType.contains("webp") -> "webp"
                    else -> "jpg"
                }
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData(
                    "file", "photo_${System.currentTimeMillis()}.$ext", requestBody
                )
                val uploadRes = NetworkModule.uploadApi.uploadImage(part)
                // 后端返回扁平结构 {code,msg,url,...}，url 为相对路径
                if (uploadRes.code == 200 && uploadRes.url.isNotBlank()) {
                    out.add(toAbsoluteUrl(uploadRes.url))
                } else {
                    Log.w("MomentsVM", "upload failed: ${uploadRes.msg}")
                }
            } catch (e: Throwable) {
                Log.w("MomentsVM", "upload error for $uri", e)
            }
        }
        return out
    }

    /**
     * 将后端返回的相对路径拼成绝对 URL
     * （上传接口返回的 url 形如 /uploads/images/xxx.png，需拼 API_BASE_URL）
     */
    private fun toAbsoluteUrl(relative: String): String {
        if (relative.startsWith("http://", ignoreCase = true) ||
            relative.startsWith("https://", ignoreCase = true)
        ) return relative
        val base = BuildConfig.API_BASE_URL.trimEnd('/')
        return "$base$relative"
    }

    fun deletePost(post: Post) {
        launchIo(showLoading = false) {
            val userId = UserPrefs.userId() ?: return@launchIo
            val res = NetworkModule.circleApi.deletePost(postId = post.id, userId = userId)
            if (res.isSuccess) {
                _posts.value = _posts.value.filter { it.id != post.id }
            }
        }
    }

    fun submitComment(postId: Long, content: String) {
        launchIo(showLoading = false) {
            val userId = UserPrefs.userId() ?: return@launchIo
            val res = NetworkModule.circleApi.createComment(
                CreateCommentBody(post_id = postId, user_id = userId, content = content)
            )
            if (res.isSuccess) {
                // 重新拉取列表，更新 comment_count
                loadPosts()
                // 若评论区已展开，刷新评论列表
                if (_comments.value.containsKey(postId)) {
                    loadComments(postId)
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    //  评论列表 / 删除
    // ──────────────────────────────────────────────

    /** 各帖子已加载的评论：postId -> 评论列表 */
    private val _comments = MutableStateFlow<Map<Long, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<Long, List<Comment>>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    fun loadComments(postId: Long) {
        launchIo(showLoading = false) {
            _commentsLoading.value = true
            try {
                val res = NetworkModule.circleApi.listComments(postId)
                if (res.isSuccess && res.data != null) {
                    _comments.value = _comments.value.toMutableMap().apply {
                        put(postId, res.data)
                    }
                }
            } finally {
                _commentsLoading.value = false
            }
        }
    }

    fun deleteComment(postId: Long, commentId: Long) {
        launchIo(showLoading = false) {
            val userId = UserPrefs.userId() ?: return@launchIo
            val res = NetworkModule.circleApi.deleteComment(commentId, userId)
            if (res.isSuccess) {
                _comments.value = _comments.value.toMutableMap().apply {
                    put(postId, (this[postId] ?: emptyList()).filter { it.id != commentId })
                }
                loadPosts()
            }
        }
    }

    // ──────────────────────────────────────────────
    //  草稿箱
    // ──────────────────────────────────────────────

    fun saveDraft(text: String, sources: List<ImageSource>, context: Context) {
        launchIo(showLoading = false) {
            // 远程图直用、本地图上传，统一存绝对 URL（草稿重启后仍可加载）
            val localUris = sources.filterIsInstance<ImageSource.Local>().map { it.uri }
            val remoteUrls = sources.filterIsInstance<ImageSource.Remote>().map { it.url }
            val urls = remoteUrls + uploadUris(localUris, context)
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            val newDraft = DraftItem(
                id = UUID.randomUUID().toString(),
                text = text,
                images = urls,
                time = now
            )
            _drafts.value = listOf(newDraft) + _drafts.value
            persistDrafts()
        }
    }

    fun deleteDraft(id: String) {
        _drafts.value = _drafts.value.filter { it.id != id }
        persistDrafts()
    }

    private fun persistDrafts() {
        launchIo(showLoading = false) {
            val json = draftsAdapter.toJson(_drafts.value)
            UserPrefs.setDraftsJson(json)
        }
    }
}

/**
 * 草稿条目（本地 DataStore 持久化，不上传后端）
 * 使用 Moshi KotlinJsonAdapterFactory 反射序列化，无需 KSP codegen。
 */
data class DraftItem(
    val id: String = "",
    val text: String = "",
    val images: List<String> = emptyList(),
    val time: String = ""
)
