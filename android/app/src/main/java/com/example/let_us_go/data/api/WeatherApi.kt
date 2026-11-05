package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.WeatherCurrent
import com.example.let_us_go.data.model.WeatherForecastDaily
import com.example.let_us_go.data.network.ApiResult
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 天气 API（主后端）
 *
 * 对应 uni 端 `pages/home` / `pages/map` 调用
 */
interface WeatherApi {

    @GET("weather/current")
    suspend fun current(
        @Query("city") city: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): ApiResult<List<WeatherCurrent>>

    @GET("weather/forecast")
    suspend fun forecast(
        @Query("city") city: String
    ): ApiResult<List<WeatherForecastDaily>>
}
