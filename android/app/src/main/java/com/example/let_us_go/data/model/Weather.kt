package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherCurrent(
    @Json(name = "weather") val weather: String = "",
    @Json(name = "temperature") val temperature: String = "",
    @Json(name = "city") val city: String = "",
    @Json(name = "winddirection") val windDirection: String? = null,
    @Json(name = "windpower") val windPower: String? = null,
    @Json(name = "humidity") val humidity: String? = null,
    @Json(name = "reporttime") val reportTime: String? = null
)

/**
 * 天气预报（按天）——对应 `GET /weather/forecast` 返回的 `data[]` 元素。
 *
 * 真实响应结构：
 * ```json
 * { "code":200, "data":[ { "city":"无锡市", "casts":[ { "date","week","dayweather","nightweather","daytemp","nighttemp","daywind","nightwind","daypower","nightpower" } ] } ] }
 * ```
 * 注意：casts 是数组，今日信息在 casts[0]，而非扁平的 dayweather/daytemp 字段。
 */
@JsonClass(generateAdapter = true)
data class WeatherForecastDaily(
    @Json(name = "city") val city: String? = null,
    @Json(name = "adcode") val adcode: String? = null,
    @Json(name = "province") val province: String? = null,
    @Json(name = "reporttime") val reportTime: String? = null,
    @Json(name = "casts") val casts: List<WeatherCast> = emptyList()
)

@JsonClass(generateAdapter = true)
data class WeatherCast(
    @Json(name = "date") val date: String = "",
    @Json(name = "week") val week: String = "",
    @Json(name = "dayweather") val dayWeather: String = "",
    @Json(name = "nightweather") val nightWeather: String = "",
    @Json(name = "daytemp") val dayTemp: String = "",
    @Json(name = "nighttemp") val nightTemp: String = "",
    @Json(name = "daywind") val dayWind: String = "",
    @Json(name = "nightwind") val nightWind: String = "",
    @Json(name = "daypower") val dayPower: String = "",
    @Json(name = "nightpower") val nightPower: String = ""
)
