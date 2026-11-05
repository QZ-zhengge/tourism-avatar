package com.example.let_us_go.ui.pages.mine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.let_us_go.LocalNav
import com.example.let_us_go.core.location.AndroidLocationProvider
import com.example.let_us_go.data.model.User
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.ui.nav.reLaunch
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.Divider
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 我的页（按设计稿逐区还原，登录逻辑沿用原版）
 *
 * UI 结构（从上到下）：
 *  1. 纯白头部 + 用户白卡（灰色渐淡边框，真实昵称/等级/积分 + 点击登录）
 *  2. 点亮城市卡片（标题 + -/+ 按钮 + 已点亮X省 + 地图占位 + 图例）
 *  3. 常用服务（一行：意见反馈 / 帮助中心 / 分享应用）
 *  4. 导览服务列表（数字人对话历史 / 常见问题）
 *  5. 旅友圈列表（我的帖子 / 我的点赞 / 我的评论）
 *  6. 设置列表（适老化模式 / 消息通知 / 隐私设置 / 关于我们）
 *  7. 底部操作卡（客服热线 | 未登录:登录 / 已登录:退出登录）
 *  8. 页脚版本号
 */
@Composable
fun MineScreen() {
    val nav = LocalNav.current
    val viewModel: MineViewModel = viewModel()
    val checkinViewModel: com.example.let_us_go.ui.pages.checkin.CheckinViewModel = viewModel()
    val context = LocalContext.current
    val user by viewModel.user.collectAsState()
    val provinceCount by viewModel.provinceCount.collectAsState()
    val agingMode by viewModel.agingMode.collectAsState()
    val litProvinces by checkinViewModel.litCodes.collectAsState()
    val lastAutoCheckin by checkinViewModel.lastAutoCheckin.collectAsState()

    // 定位权限请求
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            // 权限授予，开始定位并自动打卡
            startLocationAndCheckin(context, checkinViewModel)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAll()
        checkinViewModel.load()

        // 检查定位权限并自动打卡
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFinePermission || hasCoarsePermission) {
            startLocationAndCheckin(context, checkinViewModel)
        } else {
            // 请求定位权限
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 显示自动打卡提示
    lastAutoCheckin?.let { name ->
        LaunchedEffect(name) {
            Toast.makeText(context, "已点亮 $name", Toast.LENGTH_SHORT).show()
            checkinViewModel.clearAutoCheckinHint()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── 1. 纯白头部 + 用户白卡（恢复原始登录注册逻辑）──
            item {
                UserHeaderCard(
                    user = user,
                    onLoginClick = { nav.navigateTo(Route.LOGIN) },
                    onEditClick = { nav.navigateTo(Route.PROFILE_EDIT) }
                )
            }

            // ── 2. 点亮城市 ──
            item { Spacer(Modifier.height(10.dp)) }
            item {
                ProvinceCityCard(
                    visitedCount = litProvinces.size,
                    litProvinces = litProvinces,
                    onProvinceClick = { code, name ->
                        checkinViewModel.checkIn(code, name)
                    }
                )
            }

            // ── 3. 常用服务 ──
            item { Spacer(Modifier.height(10.dp)) }
            item {
                QuickServicesGrid(
                    onItemTap = { label ->
                        when (label) {
                            "意见反馈" -> nav.navigateTo(Route.FEEDBACK)
                            "帮助中心" -> nav.navigateTo(Route.HELP)
                            "分享应用" -> shareApp(context)
                        }
                    }
                )
            }

            // ── 4. 导览服务 ──
            item { Spacer(Modifier.height(10.dp)) }
            item {
                GuideServiceList(onItemClick = { nav.navigateTo(it) })
            }

            // ── 5. 旅友圈 ──
            item { Spacer(Modifier.height(10.dp)) }
            item {
                MomentsList(onItemClick = { nav.navigateTo(it) })
            }

            // ── 6. 设置 ──
            item { Spacer(Modifier.height(10.dp)) }
            item {
                SettingsList(
                    agingMode = agingMode,
                    onAgingToggle = { viewModel.setAgingMode(!it) },
                    onItemClick = { route ->
                        when (route) {
                            "logout" -> {
                                viewModel.logout()
                            }
                            else -> nav.navigateTo(route)
                        }
                    },
                    onLoginClick = { nav.navigateTo(Route.LOGIN) }
                )
            }

            // ── 7. 底部操作卡 + 页脚 ──
            item { Spacer(Modifier.height(10.dp)) }
            item {
                BottomActionCard(
                    isLoggedIn = user != null,
                    onServiceClick = { },
                    onLoginClick = { nav.navigateTo(Route.LOGIN) },
                    onLogoutClick = {
                        viewModel.logout()
                    }
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "随行 v1.0.0 · 让旅行更简单",
                        fontSize = 11.sp,
                        color = Color(0xFFC0C0C0),
                        textAlign = TextAlign.Center
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ════════════════════════════════════════
//  1. 纯白头部 + 用户白卡（原始登录注册逻辑，仅改 UI）
// ════════════════════════════════════════

/**
 * 用户头部卡片 —— 逻辑与原版一致，仅 UI 调整为纯白头部 + 白卡灰渐淡边框：
 * - 未登录：点击整个头部卡片 → 跳登录页；显示"点击登录 / 登录后享受更多功能"
 * - 已登录：显示真实昵称（user.nickname ?: user.username）、头像取昵称首字、LV 等级 + 积分；
 *           右上角编辑图标点击跳转个人资料编辑页
 */
@Composable
private fun UserHeaderCard(user: User?, onLoginClick: () -> Unit, onEditClick: () -> Unit = {}) {
    val isLoggedIn = user != null
    // 纯白头部背景（替代原蓝绿渐变）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 16.dp)
    ) {
        // 白色用户卡片：灰色渐淡边框（上深下浅），未登录时整体可点跳登录
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFD6D6D6), Color(0xFFF2F2F2))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .then(if (!isLoggedIn) Modifier.clickable { onLoginClick() } else Modifier),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：头像圆形（登录后显示昵称首字，未登录显示占位图标）
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoggedIn) {
                        Text(
                            text = (user?.nickname ?: user?.username ?: "").take(1),
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "未登录",
                            tint = Color(0xFFBDBDBD),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                // 右侧：用户信息 / 登录提示
                Column(modifier = Modifier.weight(1f)) {
                    if (isLoggedIn) {
                        Text(
                            text = user?.nickname ?: user?.username ?: "游客用户",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFF0F0F0))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LV${user?.level ?: 1}",
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${user?.points ?: 0} 积分",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        Text(
                            text = "点击登录",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "登录后享受更多功能",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                // 已登录显示编辑图标（可点击跳转编辑页）；未登录显示 ">" 提示
                if (isLoggedIn) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "编辑资料",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onEditClick() }
                    )
                } else {
                    Text(
                        text = ">",
                        color = Color(0xFFBDBDBD),
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════
//  2. 点亮城市
// ════════════════════════════════════════

@Composable
private fun ProvinceCityCard(
    visitedCount: Int,
    litProvinces: List<String> = emptyList(),
    onProvinceClick: (String, String) -> Unit = { _, _ -> }
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 标题行：🌐 点亮城市
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Terrain,
                    contentDescription = null,
                    tint = BrandAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "点亮城市",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(6.dp))

            // 副标题：已点亮 X 省
            Text(
                text = "已点亮 ${visitedCount.coerceAtLeast(0)} 省",
                fontSize = 12.sp,
                color = BrandAccent
            )

            Spacer(Modifier.height(10.dp))

            // 地图区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                ChinaMapScreen(
                    litProvinces = litProvinces,
                    onProvinceClick = onProvinceClick
                )
            }

            Spacer(Modifier.height(10.dp))

            // 底部图例行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BrandAccent)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("已点亮", fontSize = 11.sp, color = TextTertiary)
                }
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E0E0))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("未探索", fontSize = 11.sp, color = TextTertiary)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "点击省份点亮",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  3. 常用服务 2×3 网格
// ════════════════════════════════════════

@Composable
private fun QuickServicesGrid(onItemTap: (String) -> Unit) {
    val items = remember {
        listOf(
            ServiceGridItem("意见反馈", Icons.Outlined.Lightbulb, Color(0xFFFFFDE7), Color(0xFFFF8F00), null),
            ServiceGridItem("帮助中心", Icons.AutoMirrored.Outlined.HelpOutline, Color(0xFFFFF3E0), Color(0xFFF4511E), null),
            ServiceGridItem("分享应用", Icons.Outlined.Share, Color(0xFFE0F2F1), Color(0xFF00897B), null),
        )
    }

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        // 区块标题
        Text(
            text = "常用服务",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 10.dp, start = 4.dp)
        )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            items.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { item ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onItemTap(item.label) }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box {
                                // 图标圆底
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(item.bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = item.iconColor,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                                // 角标
                                if (item.badge != null) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF5350))
                                            .padding(horizontal = 5.dp, vertical = 1.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.badge,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                    // 补齐不足 3 个的空位
                    repeat(3 - row.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
    }
}

private data class ServiceGridItem(
    val label: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color,
    val badge: String?
)

// ════════════════════════════════════════
//  通用：带图标背景的列表项组件
// ════════════════════════════════════════

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(start = 18.dp, bottom = 8.dp, top = 4.dp)
    )
}

/**
 * 统一的列表行项：左侧彩色图标背景圆 + 标题 + 右侧可选角标/文字 + 箭头
 */
@Composable
private fun IconListItem(
    title: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color = TextPrimary,
    trailingText: String? = null,
    trailingBadge: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            // 标题
            Text(
                text = title,
                fontSize = 14.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            // 右侧角标或文字
            if (trailingBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0F0F0))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = trailingBadge, fontSize = 12.sp, color = TextTertiary)
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 13.sp,
                    color = TextTertiary
                )
            }
            // 右箭头
            if (showChevron) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = ">",
                    fontSize = 14.sp,
                    color = Color(0xFFCCCCCC)
                )
            }
        }
        // 分隔线（缩进到图标右侧）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Divider)
                .padding(horizontal = 16.dp)
        )
    }
}

// ════════════════════════════════════════
//  4. 导览服务列表
// ════════════════════════════════════════

@Composable
private fun GuideServiceList(onItemClick: (String) -> Unit) {
    Column {
        SectionHeader("导览服务")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
        Column {
            IconListItem(
                title = "实时讲解历史",
                icon = Icons.Outlined.CameraAlt,
                iconBgColor = Color(0xFFE8F5E9),
                iconTint = Color(0xFF4CAF50),
                onClick = { onItemClick(Route.DH_HISTORY) }
            )
            IconListItem(
                title = "数字人对话历史",
                icon = Icons.Outlined.AccessTime,
                iconBgColor = Color(0xFFECEFF1),
                iconTint = Color(0xFF607D8B),
                onClick = { onItemClick(Route.AVATAR_CHAT_HISTORY) }
            )
            // 最后一条不需要分隔线（视觉上由卡片边界收尾即可）
            IconListItem(
                title = "常见问题",
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                iconBgColor = Color(0xFFFFF3E0),
                iconTint = Color(0xFFFF9800),
                showChevron = true,
                onClick = { onItemClick(Route.FAQ) }
            )
        }
        }
    }
}

// ════════════════════════════════════════
//  5. 旅友圈列表
// ════════════════════════════════════════

@Composable
private fun MomentsList(onItemClick: (String) -> Unit) {
    Column {
        SectionHeader("旅友圈")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column {
                IconListItem(
                    title = "我的帖子",
                    icon = Icons.Outlined.MusicNote,
                    iconBgColor = Color(0xFFFFF8E1),
                    iconTint = Color(0xFFFFB300),
                    onClick = { onItemClick(Route.MY_POSTS) }
                )
                IconListItem(
                    title = "我的点赞",
                    icon = Icons.Outlined.FavoriteBorder,
                    iconBgColor = Color(0xFFFCE4EC),
                    iconTint = Color(0xFFE91E63),
                    onClick = { onItemClick(Route.MY_LIKES) }
                )
                IconListItem(
                    title = "我的评论",
                    icon = Icons.AutoMirrored.Outlined.Comment,
                    iconBgColor = Color(0xFFE0F2F1),
                    iconTint = Color(0xFF00897B),
                    onClick = { onItemClick(Route.MY_COMMENTS) }
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  6. 设置列表
// ════════════════════════════════════════

@Composable
private fun SettingsList(
    agingMode: Boolean,
    onAgingToggle: (Boolean) -> Unit,
    onItemClick: (String) -> Unit,
    onLoginClick: () -> Unit
) {
    Column {
        SectionHeader("设置")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard)
        ) {
            Column {
                IconListItem(
                    title = "适老化模式",
                    icon = Icons.Outlined.Person,
                    iconBgColor = Color(0xFFE3F2FD),
                    iconTint = Color(0xFF1976D2),
                    trailingText = if (agingMode) "已开启" else "未开启",
                    showChevron = true,
                    onClick = { onAgingToggle(agingMode) }
                )
                IconListItem(
                    title = "消息通知",
                    icon = Icons.Outlined.Notifications,
                    iconBgColor = Color(0xFFFFFDE7),
                    iconTint = Color(0xFFFF8F00),
                    onClick = { onItemClick(Route.NOTIFICATIONS) }
                )
                IconListItem(
                    title = "隐私设置",
                    icon = Icons.Outlined.Lock,
                    iconBgColor = Color(0xFFE0F2F1),
                    iconTint = Color(0xFF00897B),
                    onClick = { onItemClick(Route.PRIVACY) }
                )
                IconListItem(
                    title = "关于我们",
                    icon = Icons.Outlined.Info,
                    iconBgColor = Color(0xFFF5F5F5),
                    iconTint = Color(0xFF757575),
                    onClick = { onItemClick(Route.ABOUT) }
                )
            }
        }
    }
}

// ════════════════════════════════════════
//  7. 底部操作卡（客服热线 | 登录）
// ════════════════════════════════════════

@Composable
private fun BottomActionCard(
    isLoggedIn: Boolean,
    onServiceClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val rightColor = if (isLoggedIn) Color(0xFFE53935) else TextPrimary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clickable(enabled = false) { },  // 整体不可点，内部子项可点
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // 客服热线
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onServiceClick() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "客服热线",
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
            // 分割线
            Box(
                modifier = Modifier
                    .width(0.5.dp)
                    .height(24.dp)
                    .align(Alignment.CenterVertically)
                    .background(Divider)
            )
            // 右：未登录=登录（跳登录页）；已登录=退出登录（清登录态并回登录页）
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { if (isLoggedIn) onLogoutClick() else onLoginClick() }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "→",
                    fontSize = 15.sp,
                    color = rightColor
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isLoggedIn) "退出登录" else "登录",
                    fontSize = 14.sp,
                    color = rightColor
                )
            }
        }
    }
}

/**
 * 分享应用：调用系统分享面板
 */
private fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TITLE, "随行")
        putExtra(Intent.EXTRA_TEXT, "我在用「随行」规划旅行，推荐你也试试！")
    }
    context.startActivity(Intent.createChooser(intent, "分享到"))
}

/**
 * 启动定位并自动打卡
 */
private fun startLocationAndCheckin(
    context: Context,
    checkinViewModel: com.example.let_us_go.ui.pages.checkin.CheckinViewModel
) {
    val locationProvider = AndroidLocationProvider(context)
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

    scope.launch {
        locationProvider.locationFlow.collect { location ->
            checkinViewModel.autoCheckinByLocation(location.latitude, location.longitude)
            locationProvider.stop()
        }
    }

    locationProvider.start()
}
