package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 上传成功的文件
 */
@JsonClass(generateAdapter = true)
data class UploadedFile(
    @Json(name = "url") val url: String,
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "sort_order") val sortOrder: Int = 0
)
