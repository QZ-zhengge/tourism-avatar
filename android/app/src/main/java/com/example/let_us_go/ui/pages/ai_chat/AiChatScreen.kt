package com.example.let_us_go.ui.pages.ai_chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.data.model.ChatMessage
import com.example.let_us_go.ui.components.HotelCardList
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * AI 问答页（完整复刻 uni-app pages/ai-chat/index.vue）
 */
@Composable
fun AiChatScreen() {
    val nav = LocalNav.current
    val viewModel: AiChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()

    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.ensureWelcome()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // 顶部
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPage)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier
                    .clickable { nav.navigateBack() }
                    .padding(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AI 问答",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            // 清除记忆按钮（对应 uni 端 clearMemory）
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "清除记忆",
                tint = TextTertiary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { viewModel.clearMemory() }
            )
        }

        // 建议
        if (messages.size <= 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listOf("推荐一日游路线", "附近美食有哪些", "景区天气怎么样", "带老人游玩建议")) { sugg ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(BgCard)
                            .clickable { inputText = sugg }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(text = sugg, fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }

        // 消息流
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg = msg)
            }
            if (loading) {
                item {
                    Row {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BrandAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AI", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BgCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "正在思考…",
                                color = TextTertiary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // 输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .imePadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("有问题尽管问我…") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandAccent)
                    .clickable {
                        // 登录闸门：未登录禁发，跳登录页（保证 user_id 真实）
                        if (!UserPrefs.isLoggedIn()) {
                            nav.navigateTo(Route.LOGIN)
                        } else if (inputText.isNotBlank()) {
                            viewModel.send(inputText)
                            inputText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "发送",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    // 用 Box + contentAlignment 保证：用户消息靠右、AI 消息靠左（不依赖 Row arrangement 歧义）
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.widthIn(max = 320.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(BrandAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("AI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                // 显示图片（如果有）
                if (!msg.imagePath.isNullOrBlank()) {
                    val imageFile = java.io.File(msg.imagePath)
                    if (imageFile.exists()) {
                        Card(
                            modifier = Modifier.width(160.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = android.graphics.ImageDecoder.decodeBitmap(
                                    android.graphics.ImageDecoder.createSource(imageFile)
                                ).asImageBitmap(),
                                contentDescription = "拍摄的图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 100.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // 文本内容
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUser) BrandPrimary else BgCard
                    ),
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                ) {
                    Text(
                        text = msg.content,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp
                    )
                }
                if (msg.cards?.isNotEmpty() == true) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HotelCardList(cards = msg.cards!!)
                }
            }
        }
    }
}
