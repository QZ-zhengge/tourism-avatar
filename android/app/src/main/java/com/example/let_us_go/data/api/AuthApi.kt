package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.User
import com.example.let_us_go.data.network.ApiResult
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 鉴权 API
 *
 * 对应 uni 端 utils/api.js：
 *   userLogin(username, password) → POST /user/login/
 *   userRegister(params) → POST /user/register/
 */
interface AuthApi {

    @POST("user/login/")
    suspend fun login(
        @Body body: Map<String, String>
    ): ApiResult<User>

    @POST("user/register/")
    suspend fun register(
        @Body body: Map<String, String>
    ): ApiResult<User>

    @GET("user/profile/")
    suspend fun profile(
        @Query("user_id") userId: Long
    ): ApiResult<User>
}
