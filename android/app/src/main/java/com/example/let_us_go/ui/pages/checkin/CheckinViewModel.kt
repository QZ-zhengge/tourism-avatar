package com.example.let_us_go.ui.pages.checkin

import android.util.Log
import com.example.let_us_go.core.base.BaseViewModel
import com.example.let_us_go.data.model.CheckinRecord
import com.example.let_us_go.data.model.ProvinceCheckin
import com.example.let_us_go.data.network.NetworkModule
import com.example.let_us_go.data.prefs.UserPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 地图打卡（点亮城市）ViewModel
 *
 * 对齐 50080 后端「打卡模块」：
 *  - GET  /checkin/provinces      标准省份列表（用于省份选择 chips）
 *  - GET  /checkin/user/{id}      我的已点亮省份编号（字符串数组）
 *  - POST /checkin/               创建打卡（点亮），请求体仅 user_id + province_code
 */
class CheckinViewModel : BaseViewModel() {

    companion object {
        private const val TAG = "CheckinVM"
    }

    private val _provinces = MutableStateFlow<List<ProvinceCheckin>>(emptyList())
    val provinces: StateFlow<List<ProvinceCheckin>> = _provinces.asStateFlow()

    /** 我已点亮的省份编号（["110000", ...]） */
    private val _litCodes = MutableStateFlow<List<String>>(emptyList())
    val litCodes: StateFlow<List<String>> = _litCodes.asStateFlow()

    /** 上次自动打卡的省份 */
    private val _lastAutoCheckin = MutableStateFlow<String?>(null)
    val lastAutoCheckin: StateFlow<String?> = _lastAutoCheckin.asStateFlow()

    fun load() {
        launchIo(showLoading = true) {
            val uid = UserPrefs.userIdSync() ?: 0L
            runCatching {
                val p = NetworkModule.checkinApi.getProvinces()
                val list = if (p.isSuccess) p.data ?: emptyList() else emptyList()
                if (uid != 0L) {
                    val r = NetworkModule.checkinApi.getUserCheckins(userId = uid)
                    if (r.isSuccess) _litCodes.value = r.data ?: emptyList()
                }
                list
            }.onSuccess { _provinces.value = it }
        }
    }

    /**
     * 点亮一个省份
     * @param provinceCode 标准省份编号，如 "110000"
     */
    fun checkIn(provinceCode: String, provinceName: String) {
        val code = provinceCode.trim()
        if (code.isBlank()) {
            setError("请选择省份")
            return
        }
        val uid = UserPrefs.userIdSync() ?: run {
            setError("请先登录后再点亮城市")
            return
        }
        launchIo(showLoading = false) {
            runCatching {
                val res = NetworkModule.checkinApi.createCheckin(
                    com.example.let_us_go.data.api.CreateCheckinBody(
                        user_id = uid,
                        province_code = code
                    )
                )
                if (res.isSuccess) {
                    load()
                } else {
                    setError(res.msg ?: "点亮失败")
                }
            }.onFailure { setError(it.message ?: "点亮异常") }
        }
    }

    /**
     * 根据经纬度自动打卡
     *
     * @param latitude 纬度
     * @param longitude 经度
     */
    fun autoCheckinByLocation(latitude: Double, longitude: Double) {
        val provinceCode = getProvinceCodeByLocation(latitude, longitude)
        if (provinceCode != null) {
            val provinceName = getProvinceNameByCode(provinceCode)
            Log.d(TAG, "自动打卡: $provinceName ($provinceCode)")
            checkIn(provinceCode, provinceName)
            _lastAutoCheckin.value = provinceName
        }
    }

    /**
     * 清除自动打卡提示
     */
    fun clearAutoCheckinHint() {
        _lastAutoCheckin.value = null
    }

    /**
     * 根据经纬度获取省份编号（简化版，基于省份中心点距离判断）
     */
    private fun getProvinceCodeByLocation(lat: Double, lng: Double): String? {
        // 省份中心点和编号（GB/T 2260）
        val provinceCenters = listOf(
            Triple("110000", "北京市", Pair(39.9042, 116.4074)),
            Triple("120000", "天津市", Pair(39.3434, 117.3616)),
            Triple("130000", "河北省", Pair(38.0428, 114.5149)),
            Triple("140000", "山西省", Pair(37.8706, 112.5489)),
            Triple("150000", "内蒙古自治区", Pair(40.8183, 111.7652)),
            Triple("210000", "辽宁省", Pair(41.8057, 123.4315)),
            Triple("220000", "吉林省", Pair(43.8868, 125.3245)),
            Triple("230000", "黑龙江省", Pair(45.7420, 126.6610)),
            Triple("310000", "上海市", Pair(31.2304, 121.4737)),
            Triple("320000", "江苏省", Pair(32.0617, 118.7778)),
            Triple("330000", "浙江省", Pair(30.2741, 120.1551)),
            Triple("340000", "安徽省", Pair(31.8612, 117.2830)),
            Triple("350000", "福建省", Pair(26.0745, 119.2965)),
            Triple("360000", "江西省", Pair(27.6140, 115.8579)),
            Triple("370000", "山东省", Pair(36.6683, 116.9972)),
            Triple("410000", "河南省", Pair(34.7657, 113.7536)),
            Triple("420000", "湖北省", Pair(30.5928, 114.3055)),
            Triple("430000", "湖南省", Pair(28.2282, 112.9388)),
            Triple("440000", "广东省", Pair(23.1321, 113.2664)),
            Triple("450000", "广西壮族自治区", Pair(22.8170, 108.3665)),
            Triple("460000", "海南省", Pair(19.2000, 109.9900)),
            Triple("500000", "重庆市", Pair(29.5630, 106.5516)),
            Triple("510000", "四川省", Pair(30.5723, 104.0665)),
            Triple("520000", "贵州省", Pair(26.6470, 106.6302)),
            Triple("530000", "云南省", Pair(25.0389, 102.7183)),
            Triple("540000", "西藏自治区", Pair(29.6500, 91.1000)),
            Triple("610000", "陕西省", Pair(34.2658, 108.9541)),
            Triple("620000", "甘肃省", Pair(36.0594, 103.8343)),
            Triple("630000", "青海省", Pair(36.6171, 101.7782)),
            Triple("640000", "宁夏回族自治区", Pair(38.4872, 106.2309)),
            Triple("650000", "新疆维吾尔自治区", Pair(43.7930, 87.6271)),
            Triple("710000", "台湾省", Pair(23.6978, 120.9605)),
            Triple("810000", "香港特别行政区", Pair(22.3193, 114.1694)),
            Triple("820000", "澳门特别行政区", Pair(22.1987, 113.5439))
        )

        // 找到最近的省份中心点
        var minDist = Double.MAX_VALUE
        var nearestCode: String? = null

        for ((code, _, center) in provinceCenters) {
            val dist = haversineDistance(lat, lng, center.first, center.second)
            if (dist < minDist) {
                minDist = dist
                nearestCode = code
            }
        }

        // 如果距离太远（超过500km），可能是海外，不打卡
        return if (minDist < 500) nearestCode else null
    }

    /**
     * Haversine 公式计算两点距离（km）
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // 地球半径（km）
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    /**
     * 根据省份编号获取省份名称
     */
    private fun getProvinceNameByCode(code: String): String {
        return _provinces.value.find { it.provinceCode == code }?.provinceName ?: code
    }
}
