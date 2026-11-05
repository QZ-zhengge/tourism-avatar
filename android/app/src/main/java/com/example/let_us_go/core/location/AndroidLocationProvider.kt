package com.example.let_us_go.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 设备原生定位（不依赖高德 SDK，无需任何 Key）。
 *
 * 使用 Android [LocationManager] 同时监听 GPS 与 网络 两种 Provider：
 * - 网络 Provider 出结果快（WiFi / 基站，精度数十米），先让镜头快速移到用户附近；
 * - GPS Provider 精度高（米级），拿到后更新为更精确的位置。
 *
 * 两种均为设备真实定位，不引入任何假数据。
 *
 * 与高德 [AmapLocationProvider] 接口保持一致（locationFlow / errors / start / stop / release），
 * 因此 [com.example.let_us_go.ui.pages.map.MapViewModel] 只需替换实现即可。
 */
data class ProviderLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float = 0f,
    val provider: String = "",
    val time: Long = 0L
)

class AndroidLocationProvider(private val context: Context) {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _location = MutableSharedFlow<ProviderLocation>(
        replay = 1,
        extraBufferCapacity = 8
    )
    val locationFlow: SharedFlow<ProviderLocation> = _location.asSharedFlow()

    private val _errors = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 4
    )
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var started = false
    private val listeners = mutableListOf<LocationListener>()

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return

        // 权限检查：缺失则给出明确错误而非崩溃
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            _errors.tryEmit("定位失败：未授予定位权限，请在系统设置中允许本应用使用定位")
            return
        }

        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val netEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!gpsEnabled && !netEnabled) {
            _errors.tryEmit("定位失败：设备未开启任何定位服务（GPS / 网络定位），请在系统设置中开启定位")
            return
        }

        // 先发一次最近已知位置，让镜头立即移动（无需等待新定位）
        val lastKnown = (if (gpsEnabled) locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null)
            ?: (if (netEnabled) locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null)
        lastKnown?.let { emit(it) }

        // 注册 Provider：网络优先（快），GPS 后（精）
        if (netEnabled) register(LocationManager.NETWORK_PROVIDER)
        if (gpsEnabled) register(LocationManager.GPS_PROVIDER)
        started = true
    }

    @SuppressLint("MissingPermission")
    private fun register(provider: String) {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                emit(location)
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onProviderEnabled(provider: String) {}

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onProviderDisabled(provider: String) {}
        }
        listeners.add(listener)
        // 最小间隔 1s / 最小位移 1m，主线程回调
        locationManager.requestLocationUpdates(
            provider,
            1000L,
            1f,
            listener,
            Looper.getMainLooper()
        )
    }

    private fun emit(loc: Location) {
        _location.tryEmit(
            ProviderLocation(
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) loc.accuracy else 0f,
                provider = loc.provider ?: "",
                time = loc.time
            )
        )
    }

    fun stop() {
        listeners.forEach { locationManager.removeUpdates(it) }
        listeners.clear()
        started = false
    }

    fun release() {
        stop()
    }
}
