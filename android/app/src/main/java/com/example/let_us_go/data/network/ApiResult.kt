package com.example.let_us_go.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 统一后端响应包装（与 uni 端 utils/api.js 行为一致）
 *
 * 后端返回：
 * ```json
 * { "code": 200, "msg": "ok", "data": { ... } }
 * ```
 *
 * code: 200 或 0 = 成功，其他 = 业务错误（uni 端弹 toast）
 */
@JsonClass(generateAdapter = true)
data class ApiResult<T>(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: T? = null
) {
    val isSuccess: Boolean get() = code == 200 || code == 0
}

/** 后端无 data 字段时的回退 */
@JsonClass(generateAdapter = true)
data class EmptyData(
    @Json(name = "ok") val ok: Boolean = true
)
