package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.CheckinRecord
import com.example.let_us_go.data.model.ProvinceCheckin
import com.example.let_us_go.data.network.ApiResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 地图打卡 API — 主后端 50080
 *
 * 对应 README「地图打卡模块」：
 *  - POST /checkin/                   添加一次省份打卡（幂等，地图点亮）
 *  - GET  /checkin/user/{user_id}     获取用户已打卡省份编号列表
 *  - GET  /checkin/provinces          获取标准省份编号列表（地图底图）
 */
interface CheckinApi {

    /**
     * 添加一次省份打卡
     *
     * @param body 打卡请求体
     */
    @POST("checkin/")
    suspend fun checkin(
        @Body body: CheckinRequest
    ): ApiResult<CheckinRecord>

    /**
     * 添加一次省份打卡（别名，兼容旧代码）
     *
     * @param body 打卡请求体
     */
    @POST("checkin/")
    suspend fun createCheckin(
        @Body body: CreateCheckinBody
    ): ApiResult<CheckinRecord>

    /**
     * 获取用户已打卡省份编号列表
     *
     * @param userId 用户ID
     */
    @GET("checkin/user/{userId}")
    suspend fun getUserCheckins(
        @Path("userId") userId: Long
    ): ApiResult<List<String>>

    /**
     * 获取标准省份编号列表（地图底图）
     */
    @GET("checkin/provinces")
    suspend fun getProvinces(): ApiResult<List<ProvinceCheckin>>
}

/**
 * 打卡请求体
 */
data class CheckinRequest(
    val user_id: Long,
    val province_code: String
)

/**
 * 打卡请求体（别名，兼容旧代码）
 */
data class CreateCheckinBody(
    val user_id: Long,
    val province_code: String
)
