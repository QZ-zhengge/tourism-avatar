package com.example.let_us_go.ui.nav

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * NavController 扩展（顶层函数，可直接 `nav.navigateTo(...)`）
 *
 * 完整映射 uni-app 5 种路由 API：
 * - `uni.navigateTo` → `navigateTo()`
 * - `uni.redirectTo` → `redirectTo()`
 * - `uni.navigateBack` → `navigateBack()`
 * - `uni.switchTab` → `switchTab()`
 * - `uni.reLaunch` → `reLaunch()`
 */

/** navigateTo：保留当前页，跳转到应用内的某个页面（对应 uni.navigateTo） */
fun NavController.navigateTo(
    route: String,
    args: Map<String, Any> = emptyMap(),
    builder: NavOptionsBuilder.() -> Unit = {}
) {
    val fullRoute = if (args.isEmpty()) route else {
        val encoded = args.entries.joinToString("&") { "${it.key}=${it.value}" }
        "$route?$encoded"
    }
    navigate(fullRoute) {
        builder()
    }
}

/** redirectTo：关闭当前页，跳转到应用内的某个页面（对应 uni.redirectTo） */
fun NavController.redirectTo(
    route: String,
    args: Map<String, Any> = emptyMap()
) {
    val fullRoute = if (args.isEmpty()) route else {
        val encoded = args.entries.joinToString("&") { "${it.key}=${it.value}" }
        "$route?$encoded"
    }
    navigate(fullRoute) {
        popUpTo(graph.startDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

/** navigateBack：返回上一页面或多级页面（对应 uni.navigateBack） */
fun NavController.navigateBack(delta: Int = 1) {
    if (delta <= 1) {
        popBackStack()
    } else {
        repeat(delta) { popBackStack() }
    }
}

/** switchTab：跳转到 tabBar 页面（对应 uni.switchTab） */
fun NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/** reLaunch：关闭所有页面，打开到应用内的某个页面（对应 uni.reLaunch） */
fun NavController.reLaunch(route: String) {
    navigate(route) {
        popUpTo(graph.id) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
