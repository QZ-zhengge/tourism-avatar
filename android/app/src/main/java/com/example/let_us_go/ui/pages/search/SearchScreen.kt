package com.example.let_us_go.ui.pages.search

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.let_us_go.LocalNav
import com.example.let_us_go.data.model.ScenicSpot
import com.example.let_us_go.ui.components.RemoteImage
import com.example.let_us_go.ui.nav.navigateBack
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BgPage
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.BrandPrimary
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.ui.theme.TextTertiary

/**
 * 搜索页（复刻 uni-app pages/search/index.vue）
 */
@Composable
fun SearchScreen() {
    val nav = LocalNav.current
    val viewModel: SearchViewModel = viewModel()
    val history by viewModel.history.collectAsState()
    val results by viewModel.results.collectAsState()

    var keyword by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                fontSize = 22.sp,
                color = TextPrimary,
                modifier = Modifier.clickable { nav.navigateBack() }.padding(8.dp)
            )
            OutlinedTextField(
                value = keyword,
                onValueChange = { keyword = it },
                placeholder = { Text("搜索景点、攻略、美食…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandAccent,
                    unfocusedBorderColor = Color(0xFFEEEEEE)
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "搜索",
                        tint = BrandAccent,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { viewModel.search(keyword) }
                    )
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }

        if (keyword.isBlank()) {
            // 搜索建议页
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "搜索历史",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "清空",
                        fontSize = 12.sp,
                        color = TextTertiary,
                        modifier = Modifier.clickable { viewModel.clearHistory() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history) { h ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(BgCard)
                                .clickable {
                                    keyword = h
                                    viewModel.search(h)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = h, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "热门搜索",
                    fontSize = 14.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf("灵山胜境", "西湖", "故宫", "黄山", "鼓浪屿").forEach { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                keyword = tag
                                viewModel.search(tag)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${history.size + listOf("灵山胜境", "西湖", "故宫", "黄山", "鼓浪屿").indexOf(tag) + 1}",
                            color = BrandDanger,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(text = tag, fontSize = 14.sp, color = TextPrimary)
                    }
                }
            }
        } else {
            // 搜索结果
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BgCard)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RemoteImage(
                                url = result.image ?: result.coverImage,
                                fallbackText = (result.name ?: result.title ?: "?").take(1),
                                fallbackEmoji = "🔎",
                                modifier = Modifier.size(56.dp),
                                cornerRadius = 8.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.name ?: result.title ?: "未命名",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = result.summary ?: result.description ?: "",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 2,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 给 Color(0xFF...) 引用
private val BrandDanger = Color(0xFFF44336)
