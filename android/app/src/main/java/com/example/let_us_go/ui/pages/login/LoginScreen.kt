package com.example.let_us_go.ui.pages.login

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.let_us_go.R
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import androidx.compose.ui.platform.LocalContext

/**
 * 登录页（基于 uni-app pages/login/index.vue 简化）
 *
 * UI 结构：
 *  1. 顶部导航栏（返回箭头 + "登录"标题）
 *  2. Logo 区域（绿色 S 形叶子 SVG + "随行" + 标语）
 *  3. 白色卡片表单（用户名/密码/登录/链接行/老年版开关）
 *  4. 其他登录方式（微信 / QQ / 手机 三方按钮）
 */
@Composable
fun LoginScreen() {
    val nav = LocalNav.current
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val viewModel: LoginViewModel = viewModel()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var agingMode by remember { mutableStateOf(UserPrefs.agingMode()) }

    LaunchedEffect(agingMode) {
        UserPrefs.setAgingMode(agingMode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // 1. 顶部导航
        TopNavBar(onBack = { nav.navigateBack() })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // 2. Logo 区域
            LogoSection()
            Spacer(modifier = Modifier.height(32.dp))

            // 3. 白色卡片表单
            FormCard(
                username = username,
                onUsernameChange = { username = it },
                password = password,
                onPasswordChange = { password = it },
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword },
                agingMode = agingMode,
                onAgingChange = { agingMode = it },
                loading = loading,
                error = error,
                onLogin = {
                    viewModel.login(username, password) {
                        nav.navigateTo(Route.HOME) {
                            popUpTo(Route.LOGIN) { inclusive = true }
                        }
                    }
                },
                onForgotPassword = {
                    android.widget.Toast.makeText(context, "请联系客服重置密码", android.widget.Toast.LENGTH_SHORT).show()
                },
                onGoRegister = {
                    nav.navigateTo(Route.REGISTER)
                }
            )
        }
    }
}

@Composable
private fun TopNavBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "返回",
            tint = TextPrimary,
            modifier = Modifier
                .size(24.dp)
                .clickable { onBack() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "登录",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 圆形 Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "随行",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "— 您的智能景区虚拟向导 —",
            fontSize = 12.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun FormCard(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    agingMode: Boolean,
    onAgingChange: (Boolean) -> Unit,
    loading: Boolean,
    error: String?,
    onLogin: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoRegister: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = BgCard),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 用户名
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = { Text("请输入用户名", color = TextTertiary) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 密码
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { Text("请输入密码", color = TextTertiary) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "切换密码可见",
                        tint = TextTertiary,
                        modifier = Modifier.clickable { onTogglePassword() }
                    )
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )

            // 错误提示
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 登录按钮
            Button(
                onClick = onLogin,
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    disabledContainerColor = Color(0xFFB2DFDB)
                )
            ) {
                Text(
                    text = if (loading) "登录中..." else "登录",
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 链接行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "忘记密码？",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    modifier = Modifier.clickable { onForgotPassword() }
                )
                Row {
                    Text(
                        text = "还没有账号？",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "立即注册",
                        fontSize = 12.sp,
                        color = BrandAccent,
                        modifier = Modifier.clickable { onGoRegister() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 老年版开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "老年版",
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "字体更大，使用更简单哦",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = agingMode,
                    onCheckedChange = onAgingChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF66BB6A)
                    )
                )
            }
        }
    }
}

@Composable
private fun SocialLoginSection(
    onWechat: () -> Unit,
    onQQ: () -> Unit,
    onPhone: () -> Unit
) {
    Column {
        // 分隔线
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFEEEEEE))
            )
            Text(
                text = "其他登录方式",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(Color(0xFFEEEEEE))
            )
        }
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SocialItem(
                label = "微信",
                bgColor = Color(0xFF07C160),
                icon = { Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = null, tint = Color.White) },
                onTap = onWechat
            )
            SocialItem(
                label = "QQ",
                bgColor = Color(0xFF12B7F5),
                icon = { Text("Q", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                onTap = onQQ
            )
            SocialItem(
                label = "手机",
                bgColor = Color(0xFF2DBA98),
                icon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color.White) },
                onTap = onPhone
            )
        }
    }
}

@Composable
private fun SocialItem(
    label: String,
    bgColor: Color,
    icon: @Composable () -> Unit,
    onTap: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onTap() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
    }
}
