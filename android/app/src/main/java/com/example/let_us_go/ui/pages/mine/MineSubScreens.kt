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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.BuildConfig
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.model.Post
import com.example.let_us_go.ui.components.AvatarImage
import com.example.let_us_go.ui.components.RemoteImage
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.nav.navigateTo
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import kotlinx.coroutines.withContext

private fun toAbs(rel: String): String {
    if (rel.startsWith("http://", ignoreCase = true) || rel.startsWith("https://", ignoreCase = true)) return rel
    return "${BuildConfig.API_BASE_URL.trimEnd('/')}$rel"
}

@Composable
private fun SubPageScaffold(title: String, content: @Composable () -> Unit) {
    val nav = LocalNav.current
    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.navigateBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = TextPrimary)
            }
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        content()
    }
}

@Composable
private fun LoadingOrEmpty(loading: Boolean, empty: Boolean, emptyText: String) {
    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("加载中...", color = TextSecondary)
        }
    } else if (empty) {
        Box(Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.TopCenter) {
            Text(emptyText, color = TextTertiary, fontSize = 14.sp)
        }
    }
}

// ───────────────────────── 我的帖子 / 我的点赞 ─────────────────────────

@Composable
fun MyPostsScreen() {
    val vm: MyPostsViewModel = viewModel()
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    var deleting by remember { mutableStateOf<Post?>(null) }
    LaunchedEffect(Unit) { vm.load() }
    SubPageScaffold("我的帖子") {
        LoadingOrEmpty(loading, posts.isEmpty(), "还没有发布动态")
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(posts) { post ->
                PostRow(post, onDelete = { deleting = post })
            }
        }
    }
    // 删除确认弹窗
    deleting?.let { post ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除帖子") },
            text = { Text("确定要删除这条帖子吗？") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        vm.deletePost(post)
                        deleting = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = Color.Red
                    )
                ) { Text("删除") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { deleting = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun MyLikesScreen() {
    val vm: MyLikesViewModel = viewModel()
    val posts by vm.posts.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    SubPageScaffold("我的点赞") {
        LoadingOrEmpty(loading, posts.isEmpty(), "还没有点赞过动态")
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(posts) { item ->
                item.post?.let { PostRow(it) }
            }
        }
    }
}

@Composable
private fun PostRow(post: Post, onDelete: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(url = post.avatar?.let { toAbs(it) }, nickname = post.nickname ?: post.username ?: "游", modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.nickname ?: post.username ?: "游客", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    if (!post.createTime.isNullOrBlank()) {
                        Text(post.createTime, fontSize = 11.sp, color = TextTertiary)
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "删除",
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(post.content, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
            if (post.images.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                RemoteImage(
                    url = toAbs(post.images.first().url),
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)),
                    fallbackEmoji = "🖼️"
                )
            }
        }
    }
}

// ───────────────────────── 我的评论 ─────────────────────────

@Composable
fun MyCommentsScreen() {
    val vm: MyCommentsViewModel = viewModel()
    val comments by vm.comments.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    SubPageScaffold("我的评论") {
        LoadingOrEmpty(loading, comments.isEmpty(), "还没有评论过")
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(comments) { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(c.content, fontSize = 14.sp, color = TextPrimary, lineHeight = 18.sp)
                        if (!c.post?.content.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("评论于：${c.post?.content}", fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
                        }
                        if (!c.createTime.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(c.createTime, fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 实时讲解历史（数字人对话历史）─────────────────────────

@Composable
fun DhHistoryScreen() {
    val vm: DhHistoryViewModel = viewModel()
    val history by vm.history.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    SubPageScaffold("实时讲解历史") {
        LoadingOrEmpty(loading, history.isEmpty(), "还没有讲解记录")
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { h ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // 讲解配图（异步解码，先显示占位符）
                        if (!h.image.isNullOrBlank()) {
                            AsyncDecodedImage(
                                imageData = h.image,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                fallbackEmoji = "🏞️"
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        // ai_response 是 JSON 文本，原样解析；解析失败则整体作为文本展示
                        val parsed = remember(h.aiResponse) { parseAiResponse(h.aiResponse) }
                        if (parsed.isNotBlank()) {
                            Text(parsed, fontSize = 14.sp, color = TextPrimary, lineHeight = 20.sp)
                        }
                        if (!h.createdAt.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(h.createdAt, fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

/** 解析实时讲解历史的 ai_response（后端原样存储的 JSON，含 summary/spots） */
private fun parseAiResponse(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val json = org.json.JSONObject(raw)
        val summary = json.optString("summary", "")
        val sb = StringBuilder()
        if (summary.isNotBlank()) sb.append(summary)
        val spots = json.optJSONArray("spots")
        if (spots != null && spots.length() > 0) {
            val names = (0 until spots.length()).mapNotNull { i ->
                (spots.optJSONObject(i)?.optString("name"))?.takeIf { it.isNotBlank() }
            }
            if (names.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.append("\n")
                sb.append("看点：").append(names.joinToString("、"))
            }
        }
        sb.toString().ifBlank { raw }
    } catch (_: Exception) {
        raw
    }
}

/**
 * 异步解码图片组件
 *
 * 支持两种格式：
 * - base64 Data URI：后台线程解码，先显示占位符
 * - URL：使用 Coil 加载
 *
 * 优化策略：文本先渲染，图片异步加载，提升用户体验
 */
@Composable
private fun AsyncDecodedImage(
    imageData: String,
    modifier: Modifier = Modifier,
    fallbackEmoji: String = "🏞️"
) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val isLoading = remember { mutableStateOf(true) }

    // 后台解码 base64 图片
    LaunchedEffect(imageData) {
        if (imageData.startsWith("data:") || !imageData.startsWith("http")) {
            // base64 Data URI，后台解码
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val base64Data = if (imageData.startsWith("data:")) {
                        imageData.substringAfter("base64,")
                    } else {
                        imageData
                    }
                    val bytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    bitmap = decoded
                } catch (e: Exception) {
                    android.util.Log.e("AsyncDecodedImage", "解码失败: ${e.message}")
                } finally {
                    isLoading.value = false
                }
            }
        } else {
            // URL，交给 Coil 处理
            isLoading.value = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            // base64 图片已解码
            bitmap != null -> {
                androidx.compose.foundation.Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "讲解配图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            // URL 图片，使用 Coil 加载
            !imageData.startsWith("data:") && imageData.startsWith("http") -> {
                RemoteImage(
                    url = imageData,
                    modifier = Modifier.fillMaxSize(),
                    fallbackEmoji = fallbackEmoji
                )
            }
            // 加载中，显示占位符
            isLoading.value -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = BrandAccent,
                        strokeWidth = 2.dp
                    )
                }
            }
            // 解码失败，显示占位符
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(fallbackEmoji, fontSize = 32.sp)
                }
            }
        }
    }
}

// ───────────────────────── 数字人对话历史 ─────────────────────────

@Composable
fun AvatarChatHistoryScreen() {
    val vm: AvatarChatHistoryViewModel = viewModel()
    val items by vm.items.collectAsState()
    val loading by vm.loading.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    SubPageScaffold("数字人对话历史") {
        val nav = LocalNav.current
        LoadingOrEmpty(loading, items.isEmpty(), "还没有对话记录")
        LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        nav.navigateTo(Route.AVATAR_CHAT_DETAIL, mapOf("sessionId" to item.sessionId))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BgCard)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "💬",
                                fontSize = 20.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.lastMessage?.take(40)?.plus(if ((item.lastMessage?.length ?: 0) > 40) "..." else "") ?: "",
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    maxLines = 2
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${item.messageCount} 条消息  ·  ${item.startedAt?.take(10) ?: ""}",
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 数字人对话历史详情 ─────────────────────────

@Composable
fun AvatarChatDetailScreen(sessionId: String) {
    val vm: AvatarChatDetailViewModel = viewModel()
    val messages by vm.messages.collectAsState()
    val record by vm.record.collectAsState()
    LaunchedEffect(sessionId) { vm.load(sessionId) }
    SubPageScaffold("对话详情") {
        Column(modifier = Modifier.fillMaxSize()) {
            // 会话元数据
            record?.let { r ->
                if (!r.avatarId.isNullOrBlank()) {
                    Text(
                        "会话 ID：${r.sessionId.take(16)}…",
                        fontSize = 11.sp,
                        color = TextTertiary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            if (messages.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("本地暂无消息记录（该会话在本地存储之前创建）", color = TextTertiary, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(msg: com.example.let_us_go.data.model.ChatMessage) {
    val isUser = msg.role == "user"
    val bubbleColor = if (isUser) BrandAccent else BgCard
    val textColor = if (isUser) Color.White else TextPrimary
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // 角色标签
                Text(
                    if (isUser) "我" else "数字人",
                    fontSize = 11.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else TextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    msg.content,
                    fontSize = 14.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// ── data: URI 内联图片渲染（绕过 Coil 网络层，避免大 base64 超时）──

@Composable
private fun InlineDataImage(
    dataUri: String,
    modifier: Modifier = Modifier,
    fallbackEmoji: String = "🏞️"
) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(dataUri) {
        try {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val base64 = dataUri.substringAfter("base64,")
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (_: Throwable) {
            error = true
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (error) {
        Box(modifier.background(Color(0xFFF0F4F8)), contentAlignment = Alignment.Center) {
            Text(fallbackEmoji, fontSize = 28.sp)
        }
    } else {
        Box(modifier.background(Color(0xFFF0F4F8)), contentAlignment = Alignment.Center) {
            Text("⏳", fontSize = 28.sp)
        }
    }
}

// ───────────────────────── 意见反馈 ─────────────────────────

@Composable
fun FeedbackScreen() {
    val context = LocalContext.current
    val vm: FeedbackViewModel = viewModel()
    val suggestions by vm.suggestions.collectAsState()
    val loading by vm.loading.collectAsState()
    val submitting by vm.submitting.collectAsState()
    val error by vm.error.collectAsState()
    var text by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.load() }

    SubPageScaffold("意见反馈") {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("请描述您遇到的问题或建议...", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent, unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = contact,
                onValueChange = { contact = it },
                placeholder = { Text("联系方式（选填，方便我们回访）", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent, unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (text.isBlank() || submitting) Color(0xFFE0E0E0) else BrandAccent)
                    .clickable(enabled = text.isNotBlank() && !submitting) { vm.submit(text, contact) }
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(if (submitting) "提交中..." else "提交反馈", color = if (text.isBlank() || submitting) TextTertiary else Color.White, fontSize = 14.sp)
            }
            if (error != null) {
                Spacer(Modifier.height(6.dp))
                Text(error ?: "", fontSize = 12.sp, color = Color(0xFFE53935))
            }
            Spacer(Modifier.height(12.dp))
            Text("我的反馈记录", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            LoadingOrEmpty(loading, suggestions.isEmpty(), "暂无反馈记录")
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { s ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(s.content, fontSize = 14.sp, color = TextPrimary, lineHeight = 18.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("状态：${s.status ?: "已提交"}  ·  ${s.createTime ?: ""}", fontSize = 11.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── 通用简单页（常见问题/帮助/通知/隐私/关于）─────────────────────────

@Composable
fun SimplePageScreen(title: String, body: String = "该页面正在完善中。") {
    SubPageScaffold(title) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopStart) {
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(body, fontSize = 14.sp, color = TextSecondary, lineHeight = 22.sp)
            }
        }
    }
}
