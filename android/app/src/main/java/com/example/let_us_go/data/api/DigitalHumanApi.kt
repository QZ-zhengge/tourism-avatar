package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.AvatarAction
import com.example.let_us_go.data.model.AvatarChatHistoryPage
import com.example.let_us_go.data.model.AvatarConfig
import com.example.let_us_go.data.model.AvatarDriveAudioProxyRequest
import com.example.let_us_go.data.model.AvatarDriveAudioUrlRequest
import com.example.let_us_go.data.model.AvatarDriveAudioUrlResponse
import com.example.let_us_go.data.model.AvatarDriveTextRequest
import com.example.let_us_go.data.model.AvatarPingRequest
import com.example.let_us_go.data.model.AvatarSession
import com.example.let_us_go.data.model.AvatarStartRequest
import com.example.let_us_go.data.model.AvatarStopRequest
import com.example.let_us_go.data.model.AvatarVcn
import com.example.let_us_go.data.network.ApiResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 数字人 API（主后端代理讯飞）
 *
 * 必须与 uniapp pages/digital-human/index.js 完全一致
 */
interface DigitalHumanApi {

    @POST("avatar/start")
    suspend fun start(
        @Body req: AvatarStartRequest
    ): ApiResult<AvatarSession>

    @POST("avatar/drive/text")
    suspend fun driveText(
        @Body req: AvatarDriveTextRequest
    ): ApiResult<Any>

    @POST("avatar/drive/audio/url")
    suspend fun driveAudioUrl(
        @Body req: AvatarDriveAudioUrlRequest
    ): ApiResult<AvatarDriveAudioUrlResponse>

    @POST("avatar/drive/audio/proxy")
    suspend fun driveAudioProxy(
        @Body req: AvatarDriveAudioProxyRequest
    ): ApiResult<Any>

    @POST("avatar/ping")
    suspend fun ping(
        @Body req: AvatarPingRequest
    ): ApiResult<Any>

    @POST("avatar/stop")
    suspend fun stop(
        @Body req: AvatarStopRequest
    ): ApiResult<Any>

    @GET("avatar/session/{session}")
    suspend fun session(
        @Path("session") session: String
    ): ApiResult<AvatarSession>

    // ─── 配置 / 资源（启动期读取，去掉硬编码）───

    @GET("avatar/config")
    suspend fun getConfig(): ApiResult<AvatarConfig>

    @GET("avatar/actions/{avatarId}")
    suspend fun getActions(
        @Path("avatarId") avatarId: String
    ): ApiResult<List<AvatarAction>>

    @GET("avatar/vcn/list")
    suspend fun getVcnList(): ApiResult<List<AvatarVcn>>

    // ─── 6.10 数字人对话历史 ───

    @GET("avatar/chat/history/user/{userId}")
    suspend fun listChatHistory(
        @Path("userId") userId: Long,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ApiResult<AvatarChatHistoryPage>
}
