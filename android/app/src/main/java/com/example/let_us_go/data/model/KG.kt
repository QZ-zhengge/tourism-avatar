package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 知识图谱
 */
@JsonClass(generateAdapter = true)
data class KGGraphData(
    @Json(name = "nodes") val nodes: List<KGNode> = emptyList(),
    @Json(name = "edges") val edges: List<KGEdge> = emptyList(),
    @Json(name = "root") val root: String? = null
)

@JsonClass(generateAdapter = true)
data class KGNode(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "type") val type: String = "ScenicSpot",          // ScenicSpot / City / Person / Dynasty / Event / Show / Food / Hotel / Tag
    @Json(name = "properties") val properties: Map<String, Any> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class KGEdge(
    @Json(name = "id") val id: String? = null,
    @Json(name = "source") val source: String = "",
    @Json(name = "target") val target: String = "",
    @Json(name = "label") val label: String? = null,
    @Json(name = "type") val type: String? = null                 // LOCATED_IN / NEAR_WITH / HAS_FOOD / HAS_ACCOMMODATION / BELONG_TO_STYLE / RELATED_TO / BUILT_IN / HAS_EVENT / HAS_SHOW
)
