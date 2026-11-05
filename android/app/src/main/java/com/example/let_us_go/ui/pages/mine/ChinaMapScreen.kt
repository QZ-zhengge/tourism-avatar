package com.example.let_us_go.ui.pages.mine

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.LatLngBounds
import com.amap.api.maps.model.PolygonOptions
import com.example.let_us_go.BuildConfig
import com.example.let_us_go.ui.theme.BrandAccent
import org.json.JSONObject

/**
 * 中国省份点亮地图（使用高德 AMap SDK）
 *
 * 显示中国全图，已点亮的省份用绿色多边形高亮显示（真实省份边界）。
 * 默认状态：中国地图刚好填充整个区域。
 *
 * @param litProvinces 已点亮省份编号列表
 * @param onProvinceClick 省份点击回调（provinceCode, provinceName）
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ChinaMapScreen(
    litProvinces: List<String>,
    onProvinceClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val litCodes = remember(litProvinces) { litProvinces.toSet() }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            onResume()
        }
    }

    var aMapRef = remember { mutableStateOf<AMap?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFFF5F5F5))
    ) {
        AndroidView(
            factory = { ctx ->
                MapsInitializer.setApiKey(BuildConfig.AMAP_ANDROID_KEY)

                val aMap = mapView.map
                aMapRef.value = aMap

                // 地图基本设置
                aMap.uiSettings.isZoomControlsEnabled = false
                aMap.uiSettings.isCompassEnabled = false
                aMap.uiSettings.isScaleControlsEnabled = false
                aMap.uiSettings.isMyLocationButtonEnabled = false
                aMap.uiSettings.isScrollGesturesEnabled = false
                aMap.uiSettings.isZoomGesturesEnabled = false
                aMap.mapType = AMap.MAP_TYPE_NORMAL

                // 默认显示中国全图（刚好填充整个区域，无需滚动）
                val chinaCenter = LatLng(35.0, 104.0)
                aMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                    CameraPosition(chinaCenter, 2.8f, 0f, 0f)
                ))

                // 限制地图范围为中国
                val chinaBounds = LatLngBounds(
                    LatLng(15.0, 70.0),
                    LatLng(56.0, 138.0)
                )
                aMap.setMapStatusLimits(chinaBounds)
                aMap.setMinZoomLevel(2.5f)
                aMap.setMaxZoomLevel(10.0f)

                // 加载省份边界数据并绘制
                drawLitProvinces(aMap, litCodes, ctx)

                // 点击省份事件
                aMap.setOnMapClickListener { latLng ->
                    // 这里可以添加点击检测逻辑
                }

                mapView
            },
            update = { },
            modifier = Modifier.fillMaxSize()
        )
    }

    // 监听点亮省份变化
    LaunchedEffect(litCodes) {
        val aMap = aMapRef.value ?: return@LaunchedEffect
        drawLitProvinces(aMap, litCodes, context)
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
}

/**
 * 绘制已点亮的省份（使用真实省份边界数据）
 */
private fun drawLitProvinces(
    aMap: AMap,
    litCodes: Set<String>,
    context: android.content.Context
) {
    try {
        // 清除旧的多边形
        aMap.clear()

        // 读取省份边界数据
        val jsonStr = context.assets.open("provinces.json")
            .bufferedReader()
            .use { it.readText() }

        val json = JSONObject(jsonStr)
        val features = json.optJSONArray("features") ?: return

        val litColor = 0x552DBA98.toInt()  // 半透明绿色填充
        val strokeColor = BrandAccent.value.toLong().toInt()

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val props = feature.optJSONObject("properties")
            val name = props?.optString("name") ?: continue

            // 查找省份编号
            val code = PROVINCE_NAME_TO_CODE[name] ?: continue

            if (code in litCodes) {
                val geometry = feature.optJSONObject("geometry")
                val coordinates = geometry?.optJSONArray("coordinates") ?: continue
                val geoType = geometry.optString("type")

                // 处理 Polygon 和 MultiPolygon
                val allRings = mutableListOf<org.json.JSONArray>()

                if (geoType == "Polygon") {
                    // Polygon: 每个元素是一个环（外环或内环）
                    for (k in 0 until coordinates.length()) {
                        coordinates.optJSONArray(k)?.let { allRings.add(it) }
                    }
                } else if (geoType == "MultiPolygon") {
                    // MultiPolygon: 每个元素是一个 Polygon
                    for (p in 0 until coordinates.length()) {
                        val polygon = coordinates.optJSONArray(p)
                        if (polygon != null) {
                            for (k in 0 until polygon.length()) {
                                polygon.optJSONArray(k)?.let { allRings.add(it) }
                            }
                        }
                    }
                }

                // 为每个环绘制多边形
                for (ring in allRings) {
                    val polygonPoints = mutableListOf<LatLng>()
                    for (j in 0 until ring.length()) {
                        val point = ring.optJSONArray(j)
                        if (point != null && point.length() >= 2) {
                            polygonPoints.add(
                                LatLng(point.getDouble(1), point.getDouble(0))
                            )
                        }
                    }
                    if (polygonPoints.size >= 3) {
                        aMap.addPolygon(
                            PolygonOptions()
                                .addAll(polygonPoints)
                                .fillColor(litColor)
                                .strokeColor(strokeColor)
                                .strokeWidth(3f)
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        Log.e("ChinaMap", "绘制点亮省份失败: ${e.message}", e)
    }
}

/**
 * 省份名称到编号的映射
 */
private val PROVINCE_NAME_TO_CODE = mapOf(
    "北京市" to "110000",
    "天津市" to "120000",
    "河北省" to "130000",
    "山西省" to "140000",
    "内蒙古自治区" to "150000",
    "辽宁省" to "210000",
    "吉林省" to "220000",
    "黑龙江省" to "230000",
    "上海市" to "310000",
    "江苏省" to "320000",
    "浙江省" to "330000",
    "安徽省" to "340000",
    "福建省" to "350000",
    "江西省" to "360000",
    "山东省" to "370000",
    "河南省" to "410000",
    "湖北省" to "420000",
    "湖南省" to "430000",
    "广东省" to "440000",
    "广西壮族自治区" to "450000",
    "海南省" to "460000",
    "重庆市" to "500000",
    "四川省" to "510000",
    "贵州省" to "520000",
    "云南省" to "530000",
    "西藏自治区" to "540000",
    "陕西省" to "610000",
    "甘肃省" to "620000",
    "青海省" to "630000",
    "宁夏回族自治区" to "640000",
    "新疆维吾尔自治区" to "650000",
    "台湾省" to "710000",
    "香港特别行政区" to "810000",
    "澳门特别行政区" to "820000"
)