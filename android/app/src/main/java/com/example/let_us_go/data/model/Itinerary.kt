package com.example.let_us_go.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * 行程
 */
@JsonClass(generateAdapter = true)
data class Itinerary(
    @Json(name = "date") val date: String = "",                    // YYYY-MM-DD
    @Json(name = "items") val items: List<ItineraryItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItineraryItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "type") val type: String = "note",                // route / booking / note
    @Json(name = "kind") val kind: String = "",                    // hotel / restaurant / spot / transport
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "time") val time: String? = null,                 // HH:mm
    @Json(name = "date") val date: String? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "checkIn") val checkIn: String? = null,
    @Json(name = "checkOut") val checkOut: String? = null,
    @Json(name = "roomType") val roomType: String? = null,
    @Json(name = "note") val note: String? = null,
    @Json(name = "image") val image: String? = null
)

@JsonClass(generateAdapter = true)
data class ItineraryParseResult(
    @Json(name = "items") val items: List<ItineraryItem> = emptyList(),
    @Json(name = "summary") val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class HotelListData(
    @Json(name = "items") val items: List<HotelCard> = emptyList(),
    @Json(name = "title") val title: String? = null
)
