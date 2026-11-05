package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 景区/景点通用模型
 */
@JsonClass(generateAdapter = true)
data class ScenicSpot(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "type") val type: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "source_link") val sourceLink: String? = null,
    @Json(name = "cover_image") val coverImage: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "images") val images: List<String> = emptyList(),
    @Json(name = "location") val location: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "hot_score") val hotScore: Int? = null
)

@JsonClass(generateAdapter = true)
data class Event(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "source_link") val sourceLink: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "category") val category: String? = null
)

@JsonClass(generateAdapter = true)
data class Food(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "source_link") val sourceLink: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "price") val price: String? = null
)

@JsonClass(generateAdapter = true)
data class NearbyShop(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "category") val category: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "distance") val distance: String? = null,
    @Json(name = "source_link") val sourceLink: String? = null
)

@JsonClass(generateAdapter = true)
data class GuideTip(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "source") val source: String? = null,
    @Json(name = "source_link") val sourceLink: String? = null
) {
    /** 获取完整内容（优先 description，其次 content，兜底 summary） */
    val fullContent: String
        get() = description ?: content ?: summary ?: ""
}

@JsonClass(generateAdapter = true)
data class PhotoSpot(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "count") val count: Int = 0,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "source_link") val sourceLink: String? = null
)
