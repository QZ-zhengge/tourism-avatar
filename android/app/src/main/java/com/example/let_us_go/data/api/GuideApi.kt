package com.example.let_us_go.data.api

import com.example.let_us_go.data.model.ChatRequest
import com.example.let_us_go.data.model.ChatResponse
import com.example.let_us_go.data.model.Event
import com.example.let_us_go.data.model.Food
import com.example.let_us_go.data.model.GuideTip
import com.example.let_us_go.data.model.HotelListData
import com.example.let_us_go.data.model.ImageRecognitionResponse
import com.example.let_us_go.data.model.ItineraryParseResult
import com.example.let_us_go.data.model.NearbyShop
import com.example.let_us_go.data.model.PhotoSpot
import com.example.let_us_go.data.model.ScenicSpot
import com.example.let_us_go.data.network.ApiResult
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 数字人导览 API（GUIDE_BASE_URL：http://192.0.2.1:50081）
 *
 * 对应 uni 端 `pages/home` / `pages/ai-chat` / `pages/itinerary` / `pages/scenic` 等多个页面调用的后端
 */
interface GuideApi {

    // ─── Chat LLM ───
    // 后端直接返回 GuideChatResponse，不包裹 ApiResult
    @POST("api/v1/guide/chat")
    suspend fun chat(
        @Body req: ChatRequest
    ): ChatResponse

    @DELETE("api/v1/guide/memory/{sessionId}")
    suspend fun clearMemory(
        @Path("sessionId") sessionId: String
    ): ApiResult<Any>

    @POST("api/v1/guide/elderly-mode")
    suspend fun elderlyMode(
        @Body body: Map<String, Boolean>
    ): ApiResult<Any>

    // ─── POI 列表（真实坐标 + 类型，地图页核心数据源）───
    //
    // 注意：该接口【不】包裹 ApiResult，直接返回
    // { "scenic_id", "scenic_name", "poi_count", "pois": [ {poi_id,name,lat,lng,point_type,tags,...} ] }
    // 与 hot-spots 不同，poi/list 的每一项都带有真实经纬度，可在地图上标点。

    @GET("api/v1/guide/poi/list")
    suspend fun poiList(
        @Query("scenic_id") scenicId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 100
    ): PoiListResponse

    // ─── 热门景点 / 列表 ───

    // 注意：以下 6 个列表端点【不】包裹 ApiResult，直接返回 ListData
    // { "scenic_id", "scenic_name", "total", "page", "page_size", "items": [...] }

    @GET("api/v1/guide/hot-spots")
    suspend fun hotSpots(
        @Query("scenic_id") scenicId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ListData<ScenicSpot>

    @GET("api/v1/guide/events")
    suspend fun events(
        @Query("scenic_id") scenicId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ListData<Event>

    @GET("api/v1/guide/food")
    suspend fun food(
        @Query("scenic_id") scenicId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ListData<Food>

    @GET("api/v1/guide/nearby-shops")
    suspend fun nearbyShops(
        @Query("scenic_id") scenicId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ListData<NearbyShop>

    @GET("api/v1/guide/guide-tips")
    suspend fun guideTips(
        @Query("scenic_id") scenicId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ListData<GuideTip>

    @GET("api/v1/guide/photo-spots")
    suspend fun photoSpots(
        @Query("scenic_id") scenicId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): ListData<PhotoSpot>

    // ─── 路线规划 ───

    // 注意：route-plan 响应【不】包裹 ApiResult，直接返回
    // { intent, action, message, data:{polyline,steps,summary,narration,start,end}, ui_command }
    //
    // 关键修复：此前用 RoutePlanRequest 数据类 + @field:Json，但本项目的 Moshi 仅配了
    // KotlinJsonAdapterFactory（反射），不会识别 @field:Json 的蛇形命名，导致请求体被序列化成
    // 驼峰（sessionId/startPoi...），后端报 422「Field required: session_id」。
    // 故请求体改为 okhttp3.RequestBody（调用方用 JSONObject 显式写蛇形字段），
    // 响应直接返回 String 由业务层用 org.json 解析 —— 完全不依赖 Moshi 注解/泛型。
    @POST("api/v1/guide/route-plan")
    suspend fun routePlan(
        @Body body: okhttp3.RequestBody
    ): String

    // ─── 行程解析 / 酒店 ───

    @POST("api/v1/guide/parse-itinerary")
    suspend fun parseItinerary(
        @Body body: Map<String, String>
    ): ApiResult<ItineraryParseResult>

    @GET("api/v1/guide/hotel-list")
    suspend fun hotelList(
        @Query("scenic_id") scenicId: String,
        @Query("check_in") checkIn: String,
        @Query("check_out") checkOut: String
    ): ApiResult<HotelListData>

    // ─── 拍照识别（multipart/form-data，图片作为文件上传）───
    //
    // 注意：该端点【不】包裹 ApiResult，直接返回 ImageRecognitionResponse。
    // 请求体为 multipart/form-data：
    //   file        : 图片二进制（image/*）
    //   session_id  : 字符串（与 guide/chat 同一会话，复用数字人 avatar session）
    //   scenic_id   : 字符串（如 lingshan）

    @Multipart
    @POST("api/v1/guide/image-recognition")
    suspend fun imageRecognize(
        @Part file: MultipartBody.Part,
        @Part("session_id") sessionId: RequestBody,
        @Part("scenic_id") scenicId: RequestBody
    ): ImageRecognitionResponse

    // ─── 附近 ───

    @POST("api/v1/guide/nearby")
    suspend fun nearby(
        @Body body: okhttp3.RequestBody
    ): String
}

data class ListData<T>(
    @Json(name = "items") val items: List<T> = emptyList(),
    @Json(name = "total") val total: Int = 0,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "page_size") val pageSize: Int = 20,
    @Json(name = "has_more") val hasMore: Boolean = false
)

/**
 * POI 列表响应（非 ApiResult 包裹，直接返回）
 *
 * 对应 `GET /api/v1/guide/poi/list`
 */
@JsonClass(generateAdapter = true)
data class PoiListResponse(
    @Json(name = "scenic_id") val scenicId: String? = null,
    @Json(name = "scenic_name") val scenicName: String? = null,
    @Json(name = "poi_count") val poiCount: Int = 0,
    @Json(name = "pois") val pois: List<PoiRaw> = emptyList()
)

/**
 * 单个 POI（带真实经纬度）
 *
 * 字段严格对齐后端 `poi/list` 返回结构。
 */
@JsonClass(generateAdapter = true)
data class PoiRaw(
    @Json(name = "poi_id") val poiId: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "aliases") val aliases: List<String>? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "point_type") val pointType: String? = null,
    @Json(name = "stay_minutes") val stayMinutes: Int? = null,
    @Json(name = "intro") val intro: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "tags") val tags: List<String>? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "geocode_status") val geocodeStatus: String? = null,
    @Json(name = "confidence") val confidence: Double? = null
)

/**
 * 附近 POI 单项（POST /api/v1/guide/nearby 返回）
 */
data class NearbyPoi(
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val distance_m: Int = 0,
    val category: String = "",
    val source: String = "",
    val poi_id: String = "",
    val point_type: String = ""
)
