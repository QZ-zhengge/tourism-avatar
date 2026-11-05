package com.example.let_us_go.ui.pages.mine

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 个人资料编辑页
 *
 * 包含：
 * 1. 修改个人信息（昵称/邮箱/手机号）
 * 2. 修改密码
 */
@Composable
fun ProfileEditScreen() {
    val nav = LocalNav.current
    val viewModel: ProfileEditViewModel = viewModel()
    val profile by viewModel.profile.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // 加载用户资料
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // 显示成功消息
    successMessage?.let { msg ->
        LaunchedEffect(msg) {
            viewModel.clearSuccess()
        }
    }

    // 显示错误对话框
    errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearUserError() },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearUserError() }) {
                    Text("确定", color = BrandAccent)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // 顶部导航栏
        TopNavBar(onBack = { nav.navigateBack() })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 头像区域
            ProfileAvatar(
                nickname = profile?.nickname ?: profile?.username ?: "",
                avatar = profile?.avatar
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 修改个人信息卡片
            ProfileInfoCard(
                profile = profile,
                loading = loading,
                onSave = { nickname, email, phone ->
                    viewModel.updateProfile(
                        nickname = nickname,
                        email = email,
                        phoneNumber = phone
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 修改密码卡片
            ChangePasswordCard(
                loading = loading,
                onChangePassword = { old, new, confirm ->
                    viewModel.changePassword(old, new, confirm)
                }
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
            text = "个人资料",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun ProfileAvatar(nickname: String, avatar: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(BrandAccent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nickname.firstOrNull()?.toString() ?: "我",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nickname,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
    }
}

@Composable
private fun ProfileInfoCard(
    profile: com.example.let_us_go.data.api.UserProfileData?,
    loading: Boolean,
    onSave: (nickname: String, email: String, phone: String) -> Unit
) {
    var nickname by remember(profile) { mutableStateOf(profile?.nickname ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.email ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.phone_number ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "修改个人信息",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 昵称
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                placeholder = { Text("请输入昵称", color = TextTertiary) },
                label = { Text("昵称") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Person,
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

            // 邮箱
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("请输入邮箱", color = TextTertiary) },
                label = { Text("邮箱") },
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

            // 手机号
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                placeholder = { Text("请输入手机号", color = TextTertiary) },
                label = { Text("手机号") },
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

            Spacer(modifier = Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = { onSave(nickname, email, phone) },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandAccent,
                    disabledContainerColor = Color(0xFFB2DFDB)
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "保存修改",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangePasswordCard(
    loading: Boolean,
    onChangePassword: (old: String, new: String, confirm: String) -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "修改密码",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 旧密码
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                placeholder = { Text("请输入旧密码", color = TextTertiary) },
                label = { Text("旧密码") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                },
                visualTransformation = if (showOldPassword) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 新密码
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                placeholder = { Text("请输入新密码（至少6位）", color = TextTertiary) },
                label = { Text("新密码") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                },
                visualTransformation = if (showNewPassword) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 确认新密码
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                placeholder = { Text("请再次输入新密码", color = TextTertiary) },
                label = { Text("确认新密码") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = TextTertiary
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 修改密码按钮
            Button(
                onClick = { onChangePassword(oldPassword, newPassword, confirmPassword) },
                enabled = !loading && oldPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2),
                    disabledContainerColor = Color(0xFFBBDEFB)
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "修改密码",
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
