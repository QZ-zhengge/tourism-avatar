package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.Suggestion
import com.example.let_us_go.data.network.ApiResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 建议信箱 API —— 主后端 50080
 *
 * 对应 README「建议信箱」：
 *  - POST /suggestions/           提交建议
 *  - GET  /suggestions/user/{id}  获取我的建议
 */
interface SuggestionApi {

    @POST("suggestions/")
    suspend fun create(
        @Body body: CreateSuggestionBody
    ): ApiResult<Suggestion>

    @GET("suggestions/user/{userId}")
    suspend fun listByUser(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResult<List<Suggestion>>
}

@JsonClass(generateAdapter = true)
data class CreateSuggestionBody(
    val user_id: Long,
    val content: String,
    val contact: String? = null,
    val user_name: String? = null
)
