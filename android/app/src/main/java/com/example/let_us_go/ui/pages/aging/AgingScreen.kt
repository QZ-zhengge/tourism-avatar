package com.example.let_us_go.ui.pages.aging

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.let_us_go.LocalNav
import com.example.let_us_go.core.bus.AppEventBus
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.ui.nav.reLaunch
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import com.example.let_us_go.SuiXingApp

/**
 * 适老化模式欢迎页
 * 对应 uni-app `pages/aging/index.vue`
 */
@Composable
fun AgingScreen() {
    val nav = LocalNav.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) {
        UserPrefs.setAgingMode(true)
        UserPrefs.setFontScale(1.5f)
        AppEventBus.emit(AppEventBus.FontScaleChanged(1.5f))
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🌼 适老化模式",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = "已为您开启大字体、清晰界面",
            style = MaterialTheme.typography.titleMedium
        )
        Button(
            onClick = { nav.navigateTo(Route.HOME) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("开始使用")
        }
        TextButton(
            onClick = {
                scope.launch {
                    UserPrefs.logout()
                    AppEventBus.emit(AppEventBus.LoginStateChanged(null))
                    nav.reLaunch(Route.LOGIN)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("退出登录")
        }
    }
}
