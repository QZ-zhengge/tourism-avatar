package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.RealtimeExplainHistory
import com.example.let_us_go.data.network.ApiResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 实时讲解历史 API —— 主后端 50080
 *
 * 对应 README「实时讲解历史」：
 *  - POST /realtime-explain/history          保存一次讲解记录
 *  - GET  /realtime-explain/history/user/{id} 获取我的讲解历史
 *
 * 响应结构（实测）：
 *  - 列表：`{code,msg,data:{total,page,page_size,items:[...]}}`
 *    → 用 [RealtimeExplainHistoryPage] 接 `data`，再取 [RealtimeExplainHistoryPage.items]
 *  - 单条：`{code,msg,data:{id,user_id,image,ai_response,scenic_id,created_at}}`
 */
interface RealtimeExplainApi {

    @POST("realtime-explain/history")
    suspend fun saveHistory(
        @Body body: RealtimeExplainHistoryBody
    ): ApiResult<RealtimeExplainHistory>

    @GET("realtime-explain/history/user/{userId}")
    suspend fun listByUser(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResult<RealtimeExplainHistoryPage>
}

/** 列表分页信封：后端 data 是 `{total,page,page_size,items:[...]}` */
@JsonClass(generateAdapter = true)
data class RealtimeExplainHistoryPage(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "page_size") val pageSize: Int = 20,
    @Json(name = "items") val items: List<RealtimeExplainHistory> = emptyList()
)

/**
 * 保存请求体（对齐 README §10.1）
 *
 * 字段：user_id(string,选填) / image(string,必填,base64或URL) /
 * ai_response(string,必填,AI 返回含 JSON 文本，原样存储) / scenic_id(string,选填)
 */
@JsonClass(generateAdapter = true)
data class RealtimeExplainHistoryBody(
    val user_id: String? = null,
    val image: String = "",
    val ai_response: String = "",
    val scenic_id: String? = null
)
