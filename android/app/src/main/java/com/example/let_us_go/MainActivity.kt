package com.example.let_us_go

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.let_us_go.core.bus.AppEventBus
import com.example.let_us_go.core.float.FloatingWindow
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.ui.nav.AppBottomBar
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.switchTab
import com.example.let_us_go.ui.pages.digital_human.DigitalHumanScreen
import com.example.let_us_go.ui.pages.home.HomeScreen
import com.example.let_us_go.ui.pages.login.LoginScreen
import com.example.let_us_go.ui.pages.map.MapScreen
import com.example.let_us_go.ui.pages.mine.AvatarChatDetailScreen
import com.example.let_us_go.ui.pages.mine.AvatarChatHistoryScreen
import com.example.let_us_go.ui.pages.mine.DhHistoryScreen
import com.example.let_us_go.ui.pages.mine.FeedbackScreen
import com.example.let_us_go.ui.pages.mine.MineScreen
import com.example.let_us_go.ui.pages.mine.MyCommentsScreen
import com.example.let_us_go.ui.pages.mine.MyLikesScreen
import com.example.let_us_go.ui.pages.mine.MyPostsScreen
import com.example.let_us_go.ui.pages.mine.SimplePageScreen
import com.example.let_us_go.ui.pages.moments.MomentsScreen
import com.example.let_us_go.ui.theme.SuiXingTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import android.Manifest

/** 暴露 NavController 给 Composable 子树 */
val LocalNav = staticCompositionLocalOf<NavHostController> { error("NavController not provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            val fontScale by UserPrefs.fontScaleFlow().collectAsState(initial = 1.0f)
            SuiXingTheme(fontScale = fontScale) {
                if (showSplash) {
                    com.example.let_us_go.ui.pages.splash.SplashScreen(
                        onFinish = { showSplash = false }
                    )
                } else {
                    AppRoot()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in Route.tabs
    // 悬浮窗：全页面显示（除登录/注册/数字人页）
    // 登录/注册页不需要浮窗；数字人页自带对话能力，避免浮窗遮挡
    val hideFloatingRoutes = setOf(Route.LOGIN, Route.REGISTER, Route.DIGITAL_HUMAN)
    val showFloating = currentRoute != null && currentRoute !in hideFloatingRoutes

    // 悬浮窗麦克风权限（无权限时点击先请求，参考 uniapp_box）
    val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val hasMicPermission = recordAudioPermission.status ==
        com.google.accompanist.permissions.PermissionStatus.Granted

    // 监听 GotoDigitalHuman（来自浮窗点击 / 唤醒命中）→ 切到数字人 tab
    androidx.compose.runtime.LaunchedEffect(Unit) {
        AppEventBus.events.collect { ev ->
            if (ev is AppEventBus.GotoDigitalHuman) {
                navController.switchTab(Route.DIGITAL_HUMAN)
            }
        }
    }

    CompositionLocalProvider(LocalNav provides navController) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AppBottomBar(
                            currentRoute = currentRoute,
                            onSelect = { route -> navController.switchTab(route) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AppNavHost(navController = navController)
                // 全局浮窗（仅在非数字人的 Tab 页显示，避免遮挡登录/注册表单及数字人页）
                if (showFloating) {
                    FloatingWindow(
                        hasMicPermission = hasMicPermission,
                        onRequestMicPermission = { recordAudioPermission.launchPermissionRequest() }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Route.HOME
    ) {
        // ── Tab 页（5）──
        composable(Route.HOME) { HomeScreen() }
        composable(Route.MAP) { MapScreen() }
        composable(Route.DIGITAL_HUMAN) { DigitalHumanScreen() }
        composable(Route.MOMENTS) { MomentsScreen() }
        composable(Route.MINE) { MineScreen() }

        // ── 我的页子页面（新接入 50080） ──
        composable(Route.MY_POSTS) { MyPostsScreen() }
        composable(Route.GUIDE_DETAIL) { com.example.let_us_go.ui.pages.guide.GuideDetailScreen() }
        composable(Route.MY_LIKES) { MyLikesScreen() }
        composable(Route.MY_COMMENTS) { MyCommentsScreen() }
        composable(Route.DH_HISTORY) { DhHistoryScreen() }
        composable(Route.PROFILE_EDIT) { com.example.let_us_go.ui.pages.mine.ProfileEditScreen() }
        composable(Route.AVATAR_CHAT_HISTORY) { AvatarChatHistoryScreen() }
        composable("${Route.AVATAR_CHAT_DETAIL}?sessionId={sessionId}") { backStack ->
            val sid = backStack.arguments?.getString("sessionId") ?: ""
            AvatarChatDetailScreen(sessionId = sid)
        }
        composable(Route.FEEDBACK) { FeedbackScreen() }
        composable(Route.FAQ) { SimplePageScreen("常见问题") }
        composable(Route.HELP) { com.example.let_us_go.ui.pages.help.HelpCenterScreen() }
        composable(Route.NOTIFICATIONS) { SimplePageScreen("消息通知") }
        composable(Route.PRIVACY) { SimplePageScreen("隐私设置") }
        composable(Route.ABOUT) { SimplePageScreen("关于我们") }

        // ── 登录 ──
        composable(Route.LOGIN) { LoginScreen() }
        composable(Route.REGISTER) { com.example.let_us_go.ui.pages.register.RegisterScreen() }
        composable(Route.AGING) { com.example.let_us_go.ui.pages.aging.AgingScreen() }
        composable(Route.INDEX) { com.example.let_us_go.ui.pages.index.IndexScreen() }

        // ── 工具页（已实装） ──
        composable(Route.AI_CHAT) { com.example.let_us_go.ui.pages.ai_chat.AiChatScreen() }
        composable(Route.DIGITAL_STREAM) { com.example.let_us_go.ui.pages.digital_stream.DigitalStreamScreen() }
        composable(Route.SCENIC) { com.example.let_us_go.ui.pages.scenic.ScenicScreen() }
        composable(Route.EVENTS) { com.example.let_us_go.ui.pages.events.EventsScreen() }
        composable(Route.FOOD) { com.example.let_us_go.ui.pages.food.FoodScreen() }
        composable(Route.CHECKIN) { com.example.let_us_go.ui.pages.checkin.CheckinScreen() }
        composable(Route.GUIDE) { com.example.let_us_go.ui.pages.guide.GuideScreen() }
        composable(Route.NEARBY) { com.example.let_us_go.ui.pages.nearby.NearbyScreen() }
        composable(Route.HOTSPOTS) { com.example.let_us_go.ui.pages.hotspots.HotspotsScreen() }
        composable(Route.SEARCH) { com.example.let_us_go.ui.pages.search.SearchScreen() }
        composable(Route.ITINERARY) { com.example.let_us_go.ui.pages.itinerary.ItineraryScreen() }
        composable(Route.LIVE_GUIDE) { com.example.let_us_go.ui.pages.live_guide.LiveGuideScreen() }
        composable(Route.LIVE_GUIDE_FLOW) { com.example.let_us_go.ui.pages.live_guide.LiveGuideFlowScreen() }
    }
}
