package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 打卡结果（POST /checkin/ 响应 data）
 *
 * 后端实测返回：
 * `{ "province_code": "110000", "province_name": "北京市", "newly_added": true }`
 */
@JsonClass(generateAdapter = true)
data class CheckinRecord(
    @Json(name = "province_code") val provinceCode: String? = null,
    @Json(name = "province_name") val provinceName: String? = null,
    @Json(name = "newly_added") val newlyAdded: Boolean = false
)

/**
 * 标准省份（GET /checkin/provinces，地图底图）
 *
 * 后端实测返回：`{ "province_code": "110000", "province_name": "北京市" }`
 */
@JsonClass(generateAdapter = true)
data class ProvinceCheckin(
    @Json(name = "province_code") val provinceCode: String = "",
    @Json(name = "province_name") val provinceName: String = ""
)
