package com.example.let_us_go.ui.pages.register

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 注册页（与登录页样式统一）
 *
 * UI 结构：
 *  1. 顶部导航栏（返回箭头 + "注册"标题）
 *  2. Logo 区域（绿色 S 形叶子 + "随行" + 标语）
 *  3. 白色卡片表单（用户名/手机号/邮箱/密码/确认密码/注册按钮/链接）
 */
@Composable
fun RegisterScreen() {
    val nav = LocalNav.current
    val viewModel: RegisterViewModel = viewModel()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 2. Logo 区域
            LogoSection()
            Spacer(modifier = Modifier.height(24.dp))

            // 3. 白色卡片表单
            FormCard(
                username = username,
                onUsernameChange = { username = it },
                phone = phone,
                onPhoneChange = { phone = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                confirm = confirm,
                onConfirmChange = { confirm = it },
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword },
                showConfirmPassword = showConfirmPassword,
                onToggleConfirmPassword = { showConfirmPassword = !showConfirmPassword },
                loading = loading,
                error = error,
                onRegister = {
                    viewModel.register(username, phone, email, password, confirm) {
                        nav.navigateBack()
                    }
                },
                onGoLogin = { nav.navigateBack() }
            )

            Spacer(modifier = Modifier.height(24.dp))
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
            text = "注册",
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
        // 绿色 S 形叶子 Logo
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(80.dp)) {
                val cx = size.width / 2
                val cy = size.height / 2
                val gradient = Brush.linearGradient(
                    colors = listOf(Color(0xFF66BB6A), Color(0xFF43A047)),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                // 外圆环
                drawCircle(
                    brush = gradient,
                    radius = size.minDimension / 2 - 4f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f),
                    alpha = 0.3f
                )
                // S 形主体
                val path = Path().apply {
                    moveTo(cx - size.minDimension / 4, cy - size.minDimension / 3)
                    quadraticTo(
                        cx + size.minDimension / 5, cy - size.minDimension / 4,
                        cx + size.minDimension / 6, cy - size.minDimension / 12
                    )
                    quadraticTo(
                        cx + size.minDimension / 6, cy + size.minDimension / 12,
                        cx - size.minDimension / 12, cy + size.minDimension / 8
                    )
                    quadraticTo(
                        cx - size.minDimension / 5, cy + size.minDimension / 8,
                        cx - size.minDimension / 8, cy + size.minDimension / 4
                    )
                    quadraticTo(
                        cx, cy + size.minDimension / 2.5f,
                        cx + size.minDimension / 4, cy + size.minDimension / 3
                    )
                }
                drawPath(
                    path = path,
                    color = Color(0xFF43A047),
                    style = Stroke(width = 6f)
                )
            }
        }
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
    phone: String,
    onPhoneChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirm: String,
    onConfirmChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    showConfirmPassword: Boolean,
    onToggleConfirmPassword: () -> Unit,
    loading: Boolean,
    error: String?,
    onRegister: () -> Unit,
    onGoLogin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

            // 手机号
            OutlinedTextField(
                value = phone,
                onValueChange = onPhoneChange,
                placeholder = { Text("请输入手机号", color = TextTertiary) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 邮箱
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("请输入邮箱", color = TextTertiary) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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

            Spacer(modifier = Modifier.height(12.dp))

            // 确认密码
            OutlinedTextField(
                value = confirm,
                onValueChange = onConfirmChange,
                placeholder = { Text("请确认密码", color = TextTertiary) },
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
                        imageVector = if (showConfirmPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "切换密码可见",
                        tint = TextTertiary,
                        modifier = Modifier.clickable { onToggleConfirmPassword() }
                    )
                },
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
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

            // 注册按钮
            Button(
                onClick = onRegister,
                enabled = !loading && username.isNotBlank() && phone.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirm.isNotBlank(),
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
                    text = if (loading) "注册中..." else "注册",
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 链接行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "已有账号？",
                    fontSize = 12.sp,
                    color = TextTertiary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "返回登录",
                    fontSize = 12.sp,
                    color = BrandAccent,
                    modifier = Modifier.clickable { onGoLogin() }
                )
            }
        }
    }
}
