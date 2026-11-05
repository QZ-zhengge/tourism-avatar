package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.Comment
import com.example.let_us_go.data.model.Post
import com.example.let_us_go.data.network.ApiResult
import com.example.let_us_go.data.network.MoshiLenientAdapters.LenientInt
import com.example.let_us_go.data.network.MoshiLenientAdapters.LenientString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 旅友圈（圈子）API
 *
 * 对应 uni 端 utils/api.js 的 9 个圈子接口
 */
interface CircleApi {

    // ─── 帖子 ───

    @GET("circle/posts/")
    suspend fun listPosts(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
        @Query("current_user_id") currentUserId: Long = 0
    ): ApiResult<List<Post>>

    @GET("circle/posts/user/{userId}")
    suspend fun listUserPosts(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10,
        @Query("current_user_id") currentUserId: Long = 0
    ): ApiResult<List<Post>>

    @POST("circle/posts/")
    suspend fun createPost(
        @Body body: CreatePostBody
    ): ApiResult<CreatePostResult>

    // ─── 我的圈子（按用户聚合）───

    @GET("circle/likes/user/{userId}")
    suspend fun listUserLikes(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): ApiResult<List<LikedPostItem>>

    @GET("circle/comments/user/{userId}")
    suspend fun listUserComments(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ): ApiResult<List<MyCommentItem>>

    @POST("circle/posts/{postId}/media")
    suspend fun bindPostMedia(
        @Path("postId") postId: Long,
        @Body body: BindMediaBody
    ): ApiResult<Any>

    @PUT("circle/posts/{postId}")
    suspend fun updatePost(
        @Path("postId") postId: Long,
        @Query("user_id") userId: Long,
        @Body body: UpdatePostBody
    ): ApiResult<Any>

    @DELETE("circle/posts/{postId}")
    suspend fun deletePost(
        @Path("postId") postId: Long,
        @Query("user_id") userId: Long
    ): ApiResult<Any>

    @GET("circle/posts/{postId}")
    suspend fun getPostDetail(
        @Path("postId") postId: Long,
        @Query("current_user_id") currentUserId: Long = 0
    ): ApiResult<Post>

    // ─── 点赞 ───

    @POST("circle/likes/post")
    suspend fun toggleLike(
        @Body body: ToggleLikeBody
    ): ToggleLikeResponse

    @GET("circle/likes/check")
    suspend fun checkLike(
        @Query("post_id") postId: Long,
        @Query("user_id") userId: Long
    ): CheckLikeResponse

    @GET("circle/likes/post/{postId}")
    suspend fun listPostLikes(
        @Path("postId") postId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResult<List<LikeUser>>

    // ─── 评论 ───

    @POST("circle/comments/")
    suspend fun createComment(
        @Body body: CreateCommentBody
    ): ApiResult<Comment>

    @POST("circle/likes/comment")
    suspend fun likeComment(
        @Query("comment_id") commentId: Long,
        @Query("user_id") userId: Long
    ): ToggleLikeResponse

    @GET("circle/comments/{postId}")
    suspend fun listComments(
        @Path("postId") postId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResult<List<Comment>>

    @DELETE("circle/comments/{commentId}")
    suspend fun deleteComment(
        @Path("commentId") commentId: Long,
        @Query("user_id") userId: Long
    ): ApiResult<Any>
}

// ─── 响应结构 ───

/**
 * 创建帖子响应：后端返回 {post_id: N}
 */
data class CreatePostResult(
    @Json(name = "post_id") val postId: Long = 0L
)

data class CommentListData(
    val items: List<Comment> = emptyList(),
    val total: Int = 0
)

// ─── 请求体 ───

data class CreatePostBody(
    val user_id: Long,
    val content: String,
    val location_name: String? = null,
    @LenientString val latitude: String? = null,
    @LenientString val longitude: String? = null,
    val visibility: Int = 0  // 0=public, 1=private, 2=friends
)

data class BindMediaBody(
    val images: List<MediaItem> = emptyList(),
    val videos: List<MediaItem> = emptyList()
)

data class MediaItem(
    val url: String,
    val sort_order: Int = 0,
    val cover_url: String? = null,
    val duration: Double? = null
)

data class UpdatePostBody(
    val content: String,
    val location_name: String? = null,
    @LenientInt val visibility: Int = 0
)

data class ToggleLikeBody(
    val post_id: Long,
    val user_id: Long
)

/**
 * 点赞/取消点赞响应（后端扁平结构，无 data 包裹）
 *
 * 实测后端返回：
 * ```json
 * { "code": 200, "msg": "点赞成功", "is_liked": true, "like_count": 1 }
 * ```
 * 注意字段名是 `is_liked` / `like_count` 且直接位于顶层，需与 ApiResult 的 data 包裹区分。
 */
data class ToggleLikeResponse(
    val code: Int = 0,
    val msg: String? = null,
    @Json(name = "is_liked") val liked: Boolean = false,
    @Json(name = "like_count") val likeCount: Int = 0
)

/**
 * 检查点赞状态响应（后端扁平结构，无 data 包裹）
 * 实测后端返回：{ "code":200, "msg":"...", "is_liked":true, "like_count":1 }
 */
data class CheckLikeResponse(
    val code: Int = 0,
    val msg: String? = null,
    @Json(name = "is_liked") val isLiked: Boolean = false,
    @Json(name = "like_count") val likeCount: Int = 0
)

/**
 * 评论点赞请求体
 */
data class LikeCommentBody(
    val comment_id: Long,
    val user_id: Long
)

/**
 * 点赞用户（GET /circle/likes/post/{id}）
 */
data class LikeUser(
    @Json(name = "user_id") val userId: Long = 0L,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "avatar") val avatar: String? = null
)

data class CreateCommentBody(
    val post_id: Long,
    val user_id: Long,
    val content: String,
    val parent_id: Long? = null
)

/**
 * 「我的点赞」列表项（GET /circle/likes/user/{id}）
 * 后端返回 `{ post_id, like_time, post: {...} }`，Post 嵌套在 post 字段内。
 */
data class LikedPostItem(
    @Json(name = "post_id") val postId: Long? = null,
    @Json(name = "like_time") val likeTime: String? = null,
    @Json(name = "post") val post: Post? = null
)

/**
 * 「我的评论」列表项（GET /circle/comments/user/{id}）
 * 注意主键字段名是 `comment_id`（不是 id），并带 `post` 被评帖子包裹。
 */
data class MyCommentItem(
    @Json(name = "comment_id") val id: Long = 0L,
    @Json(name = "post_id") val postId: Long = 0L,
    @Json(name = "content") val content: String = "",
    @Json(name = "like_count") val likeCount: Int = 0,
    @Json(name = "create_time") val createTime: String? = null,
    @Json(name = "parent_id") val parentId: Long? = null,
    @Json(name = "parent_content") val parentContent: String? = null,
    @Json(name = "post") val post: Post? = null
)
