package com.example.let_us_go.ui.pages.index

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.example.let_us_go.ui.nav.switchTab
import com.example.let_us_go.ui.nav.reLaunch
import kotlinx.coroutines.launch

/**
 * 默认占位首页（带登出）
 * 对应 uni-app `pages/index/index.vue`
 */
@Composable
fun IndexScreen() {
    val nav = LocalNav.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "随行",
            style = MaterialTheme.typography.displayLarge
        )
        Text(
            text = "欢迎使用",
            style = MaterialTheme.typography.titleMedium
        )
        Button(
            onClick = { nav.switchTab(Route.HOME) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("进入首页")
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
