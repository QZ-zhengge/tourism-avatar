package com.example.let_us_go.data.model

import com.example.let_us_go.data.network.MoshiLenientAdapters.LenientInt
import com.example.let_us_go.data.network.MoshiLenientAdapters.LenientString
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 旅友圈帖子
 */
@JsonClass(generateAdapter = true)
data class Post(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "user_id") val userId: Long = 0L,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "avatarText") val avatarText: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "location_name") val locationName: String? = null,
    @Json(name = "latitude") @LenientString val latitude: String? = null,
    @Json(name = "longitude") @LenientString val longitude: String? = null,
    @Json(name = "visibility") @LenientInt val visibility: Int = 0,
    @Json(name = "images") val images: List<PostImage> = emptyList(),
    @Json(name = "videos") val videos: List<PostVideo> = emptyList(),
    @Json(name = "like_count") val likeCount: Int = 0,
    @Json(name = "comment_count") val commentCount: Int = 0,
    @Json(name = "is_liked") val isLiked: Boolean = false,
    @Json(name = "create_time") val createTime: String? = null,
    @Json(name = "isMine") val isMine: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PostImage(
    @Json(name = "url") val url: String,
    @Json(name = "sort_order") val sortOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class PostVideo(
    @Json(name = "url") val url: String,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "duration") val duration: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class Comment(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "post_id") val postId: Long = 0L,
    @Json(name = "user_id") val userId: Long = 0L,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "avatar") val avatar: String? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "parent_id") val parentId: Long? = null,
    @Json(name = "reply_to") val replyTo: Long? = null,
    @Json(name = "like_count") val likeCount: Int = 0,
    @Json(name = "replies") val replies: List<Comment> = emptyList(),
    @Json(name = "create_time") val createTime: String? = null
)
