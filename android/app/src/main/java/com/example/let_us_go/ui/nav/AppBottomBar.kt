package com.example.let_us_go.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TabActive
import com.example.let_us_go.ui.theme.TabNormal

/**
 * 底部 TabBar（5 项 + 中间数字人凸起）
 *
 * 对应 uni-app `pages.json → tabBar`：
 * - color: #999999
 * - selectedColor: #2dba98
 * - backgroundColor: #ffffff
 * - 中间"数字人"项是突出圆形按钮（参考 pages/digital-human 是核心功能）
 */
@Composable
fun AppBottomBar(
    currentRoute: String?,
    onSelect: (route: String) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
        // 底层白条
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TabItem(
                label = "首页",
                normalIcon = Icons.Outlined.Home,
                activeIcon = Icons.Filled.Home,
                selected = currentRoute == Route.HOME,
                onClick = { onSelect(Route.HOME) },
                isCenter = false
            )
            TabItem(
                label = "地图",
                normalIcon = Icons.Outlined.Map,
                activeIcon = Icons.Filled.Map,
                selected = currentRoute == Route.MAP,
                onClick = { onSelect(Route.MAP) },
                isCenter = false
            )
            // 中间占位（数字人凸起按钮叠在上面）
            Spacer(modifier = Modifier.width(72.dp))
            TabItem(
                label = "旅友圈",
                normalIcon = Icons.Outlined.Forum,
                activeIcon = Icons.Filled.Forum,
                selected = currentRoute == Route.MOMENTS,
                onClick = { onSelect(Route.MOMENTS) },
                isCenter = false
            )
            TabItem(
                label = "我的",
                normalIcon = Icons.Outlined.Person,
                activeIcon = Icons.Filled.Person,
                selected = currentRoute == Route.MINE,
                onClick = { onSelect(Route.MINE) },
                isCenter = false
            )
        }

        // 中间凸起圆形按钮（数字人）
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp)
                .size(60.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(TabActive)
                .clickable { onSelect(Route.DIGITAL_HUMAN) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = "数字人",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    normalIcon: ImageVector,
    activeIcon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    isCenter: Boolean
) {
    val color = if (selected) TabActive else TabNormal
    val icon = if (selected) activeIcon else normalIcon
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = color,
            textAlign = TextAlign.Center
        )
    }
}
