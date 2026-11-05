package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.UploadedFile
import com.example.let_us_go.data.network.ApiResult
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * 文件上传 API
 *
 * 对应 uni 端 utils/api.js：
 *   uploadImage(filePath) → uni.uploadFile POST /upload/image
 *   uploadImages(filePaths) → 并行 POST /upload/images
 */
interface UploadApi {

    @Multipart
    @POST("upload/image")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): UploadImageResponse

    @Multipart
    @POST("upload/images")
    suspend fun uploadImages(
        @Part files: List<MultipartBody.Part>
    ): ApiResult<UploadImagesResult>

    @Multipart
    @POST("upload/video")
    suspend fun uploadVideo(
        @Part file: MultipartBody.Part
    ): UploadVideoResponse
}

data class UploadImagesResult(
    val files: List<UploadedFile> = emptyList()
)

/**
 * 单图上传响应（扁平结构，后端无 data 包裹）
 *
 * 实测后端返回：
 * ```json
 * { "code": 200, "msg": "上传成功", "url": "/uploads/images/2026xxxx.png", "filename": "t.png", "size": 69 }
 * ```
 * 注意 `url` 是**相对路径**，使用方需拼 `API_BASE_URL` 成绝对 URL 才能用于图片加载/绑定。
 */
data class UploadImageResponse(
    val code: Int = 0,
    val msg: String? = null,
    val url: String = "",
    val filename: String? = null,
    val size: Int = 0
)

/**
 * 视频上传响应（扁平结构，与单图上传一致）
 * 后端返回：{ "code":200, "msg":"上传成功", "url":"/uploads/videos/xxx.mp4", ... }
 * 注意 url 是相对路径，使用方需拼 API_BASE_URL。
 */
data class UploadVideoResponse(
    val code: Int = 0,
    val msg: String? = null,
    val url: String = "",
    val filename: String? = null,
    val size: Int = 0
)
