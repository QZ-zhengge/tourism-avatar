package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.KGGraphData
import com.example.let_us_go.data.network.ApiResult
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 知识图谱 API
 *
 * 对应 uni 端 `pages/scenic/index.vue` 调用的 7 个 KG 接口
 */
interface KnowledgeGraphApi {

    @GET("knowledge-graph/overview")
    suspend fun overview(
        @Query("scenic_id") scenicId: String
    ): ApiResult<KGGraphData>

    @GET("knowledge-graph/knowledge-graph")
    suspend fun knowledgeGraph(
        @Query("entity") entity: String,
        @Query("scenic_id") scenicId: String,
        @Query("depth") depth: Int = 2
    ): ApiResult<KGGraphData>

    @GET("knowledge-graph/expand")
    suspend fun expand(
        @Query("entity") entity: String,
        @Query("scenic_id") scenicId: String
    ): ApiResult<KGGraphData>

    @GET("knowledge-graph/explore")
    suspend fun explore(
        @Query("scenic_id") scenicId: String
    ): ApiResult<KGGraphData>

    @GET("knowledge-graph/route")
    suspend fun route(
        @Query("scenic_id") scenicId: String
    ): ApiResult<KGGraphData>

    @GET("knowledge-graph/filter")
    suspend fun filter(
        @Query("scenic_id") scenicId: String,
        @Query("tag") tag: String
    ): ApiResult<KGGraphData>

    @GET("knowledge-graph/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("scenic_id") scenicId: String
    ): ApiResult<KGGraphData>

    @GET("knowledge-graph/tags")
    suspend fun tags(
        @Query("scenic_id") scenicId: String
    ): ApiResult<List<String>>
}
