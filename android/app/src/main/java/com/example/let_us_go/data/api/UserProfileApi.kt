package com.example.let_us_go.data.api

import com.example.let_us_go.data.network.ApiResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * 用户个人资料 API — 主后端 50080
 *
 * 对应 README「用户模块」：
 *  - GET  /user/profile/         获取用户个人资料
 *  - PUT  /user/profile/         修改个人信息（昵称/头像/邮箱/手机号）
 *  - PUT  /user/password/        修改密码（验证旧密码）
 */
interface UserProfileApi {

    /**
     * 获取用户个人资料
     *
     * @param userId 用户ID
     */
    @GET("user/profile/")
    suspend fun getProfile(
        @Query("user_id") userId: Long
    ): ApiResult<UserProfileData>

    /**
     * 修改个人信息
     *
     * @param body 修改请求体
     */
    @PUT("user/profile/")
    suspend fun updateProfile(
        @Body body: UpdateProfileRequest
    ): ApiResult<UserProfileData>

    /**
     * 修改密码
     *
     * @param body 修改密码请求体
     */
    @PUT("user/password/")
    suspend fun changePassword(
        @Body body: ChangePasswordRequest
    ): ApiResult<Any>
}

/**
 * 用户个人资料数据
 */
data class UserProfileData(
    val id: Long = 0,
    val username: String = "",
    val nickname: String = "",
    val email: String = "",
    val phone_number: String = "",
    val avatar: String? = null,
    val status: Int = 1,
    val create_time: String = ""
)

/**
 * 修改个人信息请求体
 */
data class UpdateProfileRequest(
    val user_id: Long,
    val nickname: String? = null,
    val avatar: String? = null,
    val email: String? = null,
    val phone_number: String? = null
)

/**
 * 修改密码请求体
 */
data class ChangePasswordRequest(
    val user_id: Long,
    val old_password: String,
    val new_password: String,
    val confirm_password: String
)
