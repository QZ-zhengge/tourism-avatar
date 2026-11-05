package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * POST /api/v1/guide/route-plan 请求体
 *
 * 字段严格对齐后端 OpenAPI（`/openapi.json` 中 `RoutePlanRequest`）：
 * - `session_id` / `scenic_id` 必填
 * - `mode`：出行方式枚举 `walk`(步行) / `drive`(驾车) / `bike`(骑行)，默认 `walk`
 * - `start_poi` / `end_poi`：起点/终点 POI 名称或 ID（来自真实 poi/list 数据集）
 * - `current_location`：当未提供 start_poi 时作为起点（如灵山胜境默认坐标）
 *
 * 注意：旧文档示例的 `from_name` / `to_name` 字段已废弃，实测会导致
 * "There was an error parsing the body"（FastAPI 无法解析请求体）。
 */
@JsonClass(generateAdapter = true)
data class RoutePlanRequest(
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "scenic_id") val scenicId: String,
    @Json(name = "query") val query: String? = null,
    @Json(name = "mode") val mode: String = "walk",
    @Json(name = "start_poi") val startPoi: String? = null,
    @Json(name = "end_poi") val endPoi: String? = null,
    @Json(name = "current_location") val currentLocation: LocationPoint? = null,
    @Json(name = "waypoints") val waypoints: List<String>? = null,
    @Json(name = "interests") val interests: List<String>? = null,
    @Json(name = "max_walk_minutes") val maxWalkMinutes: Int? = null,
    @Json(name = "avoid_stairs") val avoidStairs: Boolean = false,
    @Json(name = "family_friendly") val familyFriendly: Boolean = false
)

/**
 * 经纬度坐标点（请求/响应通用）
 */
@JsonClass(generateAdapter = true)
data class LocationPoint(
    @Json(name = "lat") val lat: Double,
    @Json(name = "lng") val lng: Double
)

/**
 * POST /api/v1/guide/route-plan 响应（【不】包裹 ApiResult，直接返回）
 *
 * 结构见 OpenAPI `RoutePlanResponse`：
 * { intent, action, message, data: RoutePlanData, ui_command }
 */
@JsonClass(generateAdapter = true)
data class RoutePlanResponse(
    @Json(name = "intent") val intent: String? = null,
    @Json(name = "action") val action: String? = null,
    @Json(name = "message") val message: String = "",
    @Json(name = "data") val data: RoutePlanData? = null,
    @Json(name = "ui_command") val uiCommand: Map<String, Any?>? = null
)

/**
 * 路线规划核心数据
 */
@JsonClass(generateAdapter = true)
data class RoutePlanData(
    @Json(name = "scenic_id") val scenicId: String? = null,
    @Json(name = "start") val start: RouteNode? = null,
    @Json(name = "end") val end: RouteNode? = null,
    @Json(name = "waypoints") val waypoints: List<RouteNode> = emptyList(),
    @Json(name = "route_nodes") val routeNodes: List<RouteNode> = emptyList(),
    @Json(name = "polyline") val polyline: List<LocationPoint> = emptyList(),
    @Json(name = "steps") val steps: List<RoutePlanStep> = emptyList(),
    @Json(name = "summary") val summary: RoutePlanSummary? = null,
    @Json(name = "narration") val narration: String = "",
    @Json(name = "guide_answer") val guideAnswer: String = "",
    @Json(name = "arrival_tip") val arrivalTip: String = "",
    @Json(name = "map_pois") val mapPois: List<RouteNode> = emptyList()
)

/**
 * 路线节点（起点/终点/途经点）
 */
@JsonClass(generateAdapter = true)
data class RouteNode(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "lat") val lat: Double = 0.0,
    @Json(name = "lng") val lng: Double = 0.0,
    @Json(name = "point_type") val pointType: String? = null,
    @Json(name = "stay_minutes") val stayMinutes: Int? = null,
    @Json(name = "intro") val intro: String? = null
)

/**
 * 路线步骤（文字指引）
 */
@JsonClass(generateAdapter = true)
data class RoutePlanStep(
    @Json(name = "seq_no") val seqNo: Int = 0,
    @Json(name = "instruction") val instruction: String = "",
    @Json(name = "distance_m") val distanceM: Int = 0,
    @Json(name = "duration_min") val durationMin: Int = 0
)

/**
 * 路线概要
 */
@JsonClass(generateAdapter = true)
data class RoutePlanSummary(
    @Json(name = "total_distance_m") val totalDistanceM: Int = 0,
    @Json(name = "total_duration_min") val totalDurationMin: Int = 0,
    @Json(name = "mode") val mode: String = "walk"
)
