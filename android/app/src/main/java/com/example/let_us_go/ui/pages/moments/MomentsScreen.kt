package com.example.let_us_go.ui.pages.moments

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.BuildConfig
import com.example.let_us_go.R
import com.example.let_us_go.data.model.Comment
import com.example.let_us_go.data.model.Post
import com.example.let_us_go.data.prefs.UserPrefs
import com.example.let_us_go.ui.components.AvatarImage
import com.example.let_us_go.ui.components.RemoteImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandDanger
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary
import com.example.let_us_go.LocalNav
import com.example.let_us_go.ui.nav.Route
import com.example.let_us_go.ui.nav.navigateTo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

/**
 * 旅友圈（完整复刻 uni-app pages/moments/index.vue）
 *
 * UI 结构：
 *  1. 顶部状态栏 + 自定义头部（＋/标题/草稿箱/景区筛选）
 *  2. Hero 渐变 banner
 *  3. 动态流（头像 + 用户名/时间/位置/正文/图片九宫格/点赞评论/展开互动）
 *  4. 发布弹层（草稿 + 文字 + 选图 + 发布）
 *  5. 评论 dock（输入栏 + 发送）
 *  6. 草稿箱弹层
 */
@Composable
fun MomentsScreen() {
    val context = LocalContext.current
    val viewModel: MomentsViewModel = viewModel()
    val posts by viewModel.posts.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val commentsMap by viewModel.comments.collectAsState()
    val commentsLoading by viewModel.commentsLoading.collectAsState()
    val publishing by viewModel.publishing.collectAsState()

    val nav = LocalNav.current
    var showLoginTip by remember { mutableStateOf(false) }

    var showComposer by remember { mutableStateOf(false) }
    var showDraftBox by remember { mutableStateOf(false) }
    var showCommentBar by remember { mutableStateOf(false) }
    var activePostId by remember { mutableStateOf<Long?>(null) }
    var commentText by remember { mutableStateOf("") }
    var draftText by remember { mutableStateOf("") }
    var draftImages by remember { mutableStateOf<List<ImageSource>>(emptyList()) }
    var activeSpotOnly by remember { mutableStateOf(true) }
    var expandedComments by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var previewUrl by remember { mutableStateOf<String?>(null) }
    var publishRequested by remember { mutableStateOf(false) }

    val keyboard = LocalSoftwareKeyboardController.current
    val pickImages = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        draftImages = (draftImages + uris.map { ImageSource.Local(it) }).take(9)
    }

    LaunchedEffect(Unit) {
        viewModel.loadAll()
    }

    // 发布完成后自动关闭弹层（publishing 由 true 变 false 时）
    LaunchedEffect(publishing) {
        if (publishRequested && !publishing) {
            publishRequested = false
            showComposer = false
            draftText = ""
            draftImages = emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // 1. 自定义头部
            MomentsHeader(
                draftCount = drafts.size,
                activeSpotOnly = activeSpotOnly,
                onOpenComposer = {
                    showComposer = true
                    draftText = ""
                    draftImages = emptyList()
                },
                onOpenDraftBox = { showDraftBox = true },
                onToggleSpotFilter = { activeSpotOnly = !activeSpotOnly }
            )

            // 2. 动态流
            if (loading && posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "加载中...", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = if (showCommentBar) 80.dp else 16.dp)
                ) {
                    val displayedPosts = if (activeSpotOnly) {
                        posts.filter { it.locationName?.contains(CURRENT_SCENIC, ignoreCase = true) == true }
                    } else {
                        posts
                    }

                    item { HeroBanner(activeSpotOnly = activeSpotOnly) }

                    items(displayedPosts) { post ->
                        Spacer(modifier = Modifier.height(8.dp))
                        FeedCard(
                            post = post,
                            comments = commentsMap[post.id] ?: emptyList(),
                            commentsLoading = commentsLoading,
                            expanded = expandedComments.contains(post.id),
                            onLike = {
                                if (UserPrefs.isLoggedIn()) {
                                    viewModel.toggleLike(post)
                                } else {
                                    showLoginTip = true
                                }
                            },
                            onToggleComments = {
                                val willExpand = !expandedComments.contains(post.id)
                                expandedComments = if (willExpand) {
                                    expandedComments + post.id
                                } else {
                                    expandedComments - post.id
                                }
                                if (willExpand) viewModel.loadComments(post.id)
                            },
                            onDeleteComment = { cid -> viewModel.deleteComment(post.id, cid) },
                            onImageClick = { url -> previewUrl = toAbsoluteUrl(url) },
                            onComment = {
                                activePostId = post.id
                                expandedComments = expandedComments + post.id
                                viewModel.loadComments(post.id)
                                showCommentBar = true
                                commentText = ""
                            },
                            onDelete = { viewModel.deletePost(post) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (displayedPosts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无动态，发个朋友圈吧～",
                                    color = TextTertiary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. 发布弹层
        if (showComposer) {
            ComposerSheet(
                draftText = draftText,
                onDraftTextChange = { draftText = it },
                draftImages = draftImages,
                onRemoveImage = { idx -> draftImages = draftImages.toMutableList().apply { removeAt(idx) } },
                onPickImages = { pickImages.launch("image/*") },
                onClose = { showComposer = false },
                publishing = publishing,
                onSaveDraft = {
                    viewModel.saveDraft(draftText, draftImages, context)
                    showComposer = false
                },
                onPublish = {
                    if (!UserPrefs.isLoggedIn()) {
                        showLoginTip = true
                    } else if (draftText.isNotBlank()) {
                        publishRequested = true
                        viewModel.publishPost(
                            text = draftText,
                            imageSources = draftImages,
                            locationName = if (activeSpotOnly) CURRENT_SCENIC else null,
                            context = context
                        )
                    }
                }
            )
        }

        // 5. 评论 dock
        if (showCommentBar) {
            CommentBar(
                text = commentText,
                onTextChange = { commentText = it },
                onSend = {
                    val pid = activePostId
                    if (pid != null && commentText.isNotBlank()) {
                        expandedComments = expandedComments + pid
                        viewModel.submitComment(pid, commentText)
                    }
                    commentText = ""
                    showCommentBar = false
                },
                onClose = {
                    showCommentBar = false
                    commentText = ""
                }
            )
        }

        // 6. 草稿箱
        if (showDraftBox) {
            DraftBox(
                drafts = drafts,
                onLoad = { draft ->
                    draftText = draft.text
                    draftImages = draft.images.map { ImageSource.Remote(it) }
                    showDraftBox = false
                    showComposer = true
                },
                onDelete = { id -> viewModel.deleteDraft(id) },
                onClose = { showDraftBox = false }
            )
        }

        // 7. 全屏图片预览
        if (previewUrl != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { previewUrl = null },
                contentAlignment = Alignment.Center
            ) {
                RemoteImage(
                    url = previewUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    fallbackEmoji = "🖼️"
                )
            }
        }

        // 8. 未登录提示：点赞 / 评论 / 发布前需先登录
        if (showLoginTip) {
            AlertDialog(
                onDismissRequest = { showLoginTip = false },
                title = { Text("请先登录", color = TextPrimary) },
                text = { Text("登录后才能点赞、评论和发布旅友圈动态。", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        showLoginTip = false
                        nav.navigateTo(Route.LOGIN)
                    }) {
                        Text("去登录", color = BrandAccent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLoginTip = false }) {
                        Text("取消", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun MomentsHeader(
    draftCount: Int,
    activeSpotOnly: Boolean,
    onOpenComposer: () -> Unit,
    onOpenDraftBox: () -> Unit,
    onToggleSpotFilter: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(BgPage)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onOpenComposer() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "＋", fontSize = 22.sp, color = BrandAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "旅友圈",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(text = "当前景点所有游客都能看到", fontSize = 11.sp, color = TextTertiary)
        }
        Box {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onOpenDraftBox() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📁", fontSize = 16.sp)
            }
            if (draftCount > 0) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(BrandDanger)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (draftCount > 9) "9+" else draftCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (activeSpotOnly) BrandAccent else Color.White)
                .clickable { onToggleSpotFilter() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (activeSpotOnly) "当前景点" else "全部动态",
                fontSize = 12.sp,
                color = if (activeSpotOnly) Color.White else TextSecondary
            )
        }
    }
}

@Composable
private fun HeroBanner(activeSpotOnly: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // 背景图片
        Image(
            painter = painterResource(id = R.drawable.moments_banner),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // 半透明黑色渐变层（保证文字可读）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )
        // 文字
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = "景点旅友圈",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (activeSpotOnly) "灵山胜境 · 只看当前景点" else "查看景区所有旅友动态",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FeedCard(
    post: Post,
    comments: List<Comment>,
    commentsLoading: Boolean,
    expanded: Boolean,
    onLike: () -> Unit,
    onToggleComments: () -> Unit,
    onDeleteComment: (Long) -> Unit,
    onImageClick: (String) -> Unit,
    onComment: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row {
                // 头像
                AvatarImage(
                    url = post.avatar?.let { toAbsoluteUrl(it) },
                    nickname = post.nickname ?: post.username ?: "游",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.nickname ?: post.username ?: "游客",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = post.createTime ?: "", fontSize = 11.sp, color = TextTertiary)
                    }
                    if (!post.locationName.isNullOrBlank()) {
                        Text(
                            text = post.locationName,
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.content,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                    if (post.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ImageGrid(
                            images = post.images.map { toAbsoluteUrl(it.url) },
                            onImageClick = onImageClick
                        )
                    }
                }
            }

            // 点赞评论行
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionPill(
                    icon = {
                        Icon(
                            imageVector = if (post.isLiked) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (post.isLiked) BrandDanger else TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    text = "赞 ${post.likeCount}",
                    onTap = onLike
                )
                ActionPill(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    text = "评论 ${post.commentCount}",
                    onTap = onComment
                )
                if (post.isMine) {
                    ActionPill(
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = BrandDanger,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        text = "删除",
                        onTap = onDelete
                    )
                }
            }

            // 评论区（展开时显示）
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                CommentSection(
                    comments = comments,
                    loading = commentsLoading,
                    onDeleteComment = onDeleteComment,
                    onCollapse = onToggleComments
                )
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: @Composable () -> Unit,
    text: String,
    onTap: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onTap() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun ImageGrid(images: List<String>, onImageClick: (String) -> Unit) {
    val cols = when {
        images.size == 1 -> 1
        images.size <= 4 -> 2
        else -> 3
    }
    val cellSize = if (images.size == 1) 180.dp else 80.dp
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        images.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { url ->
                    RemoteImage(
                        url = url,
                        modifier = Modifier
                            .size(cellSize)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onImageClick(url) },
                        fallbackEmoji = "🖼️",
                        cornerRadius = 6.dp,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentSection(
    comments: List<Comment>,
    loading: Boolean,
    onDeleteComment: (Long) -> Unit,
    onCollapse: () -> Unit
) {
    val myId = UserPrefs.userIdSync() ?: 0L
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F7FA))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "评论区",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "收起",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.clickable { onCollapse() }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (loading) {
            Text(
                text = "加载评论中...",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        if (comments.isEmpty() && !loading) {
            Text(
                text = "还没有评论，快来抢沙发～",
                fontSize = 12.sp,
                color = TextTertiary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        comments.forEach { c ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = c.nickname ?: c.username ?: "游客",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = " · ${c.createTime ?: ""}",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = c.content,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
                if (c.userId == myId) {
                    Text(
                        text = "删除",
                        fontSize = 11.sp,
                        color = BrandDanger,
                        modifier = Modifier
                            .clickable { onDeleteComment(c.id) }
                            .padding(start = 8.dp, top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ComposerSheet(
    draftText: String,
    onDraftTextChange: (String) -> Unit,
    draftImages: List<ImageSource>,
    onRemoveImage: (Int) -> Unit,
    onPickImages: () -> Unit,
    onClose: () -> Unit,
    publishing: Boolean,
    onSaveDraft: () -> Unit,
    onPublish: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
                .background(BgCard)
                .clickable(enabled = false) { }
                .imePadding()
        ) {
            // 头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "取消",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.clickable { onClose() }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "发表动态",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (publishing) "发布中..." else "发布",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (draftText.isBlank() || publishing) TextTertiary else BrandAccent,
                    modifier = Modifier.clickable(enabled = draftText.isNotBlank() && !publishing) { onPublish() }
                )
            }

            // 文字输入
            OutlinedTextField(
                value = draftText,
                onValueChange = onDraftTextChange,
                placeholder = { Text("分享一下你今天在景点里的所见所感...", color = TextTertiary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 12.dp),
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            // 已选图片
            if (draftImages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    draftImages.forEachIndexed { idx, src ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            val model: Any? = when (src) {
                                is ImageSource.Local -> src.uri
                                is ImageSource.Remote -> src.url
                            }
                            AsyncImage(
                                model = model,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .clickable(enabled = !publishing) { onRemoveImage(idx) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "×", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 上传按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onPickImages() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "＋", fontSize = 22.sp, color = TextTertiary)
                        Text(text = "上传照片", fontSize = 9.sp, color = TextTertiary)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "最多选择 9 张", fontSize = 11.sp, color = TextTertiary)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "存草稿",
                    fontSize = 13.sp,
                    color = if (publishing) TextTertiary else TextSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable(enabled = !publishing) { onSaveDraft() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CommentBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(enabled = false) {}
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(BgCard)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("写评论...", color = TextTertiary) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (text.isBlank()) Color(0xFFE0E0E0) else BrandAccent)
                    .clickable(enabled = text.isNotBlank()) { onSend() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "发送",
                    color = if (text.isBlank()) TextTertiary else Color.White,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "关闭",
                tint = TextTertiary,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onClose() }
            )
        }
    }
}

@Composable
private fun DraftBox(
    drafts: List<DraftItem>,
    onLoad: (DraftItem) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onClose() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp)
                .background(BgCard)
                .clickable(enabled = false) {}
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "草稿箱",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "×",
                    fontSize = 22.sp,
                    color = TextTertiary,
                    modifier = Modifier.clickable { onClose() }
                )
            }
            if (drafts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "暂无草稿", color = TextTertiary)
                }
            } else {
                LazyColumn {
                    items(drafts) { draft ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier
                                .weight(1f)
                                .clickable { onLoad(draft) }) {
                                Text(
                                    text = draft.text.ifBlank { "（仅图片）" },
                                    fontSize = 14.sp,
                                    color = TextPrimary,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = draft.time,
                                    fontSize = 11.sp,
                                    color = TextTertiary
                                )
                            }
                            Text(
                                text = "删除",
                                color = BrandDanger,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable { onDelete(draft.id) }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 发布 / 草稿中的图片来源：本地新选（Uri）或草稿加载（远程 URL）
 * 用密封接口统一，便于发布时本地图上传、远程图直用，且草稿重启后仍可加载。
 */
sealed interface ImageSource {
    val key: String
    data class Local(val uri: Uri) : ImageSource {
        override val key get() = uri.toString()
    }
    data class Remote(val url: String) : ImageSource {
        override val key get() = url
    }
}

/** 当前景点（用于"只看当前景点"筛选与发布默认定位） */
private const val CURRENT_SCENIC = "灵山胜境"

/** 相对路径拼绝对 URL（与 MomentsViewModel.toAbsoluteUrl 保持一致） */
private fun toAbsoluteUrl(rel: String): String {
    if (rel.startsWith("http://", ignoreCase = true) ||
        rel.startsWith("https://", ignoreCase = true)
    ) return rel
    val base = BuildConfig.API_BASE_URL.trimEnd('/')
    return "$base$rel"
}
