package com.example.let_us_go.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.let_us_go.core.bus.AppEventBus
import com.example.let_us_go.data.model.CardAction
import com.example.let_us_go.data.model.HotelCard
import com.example.let_us_go.ui.theme.BgCard
import com.example.let_us_go.ui.theme.BrandAccent
import com.example.let_us_go.ui.theme.TextPrimary
import com.example.let_us_go.ui.theme.TextSecondary
import com.example.let_us_go.util.dialPhone
import com.example.let_us_go.util.openExternalUrl

/**
 * 纵向堆叠的卡片列表（酒店 / 餐饮 / 景点等）。
 *
 * 用于 AI 问答页与数字人页文本记录中，渲染后端 guide/chat 返回的
 * card_data.items（或 cards）列表。卡片点击 / “预订”按钮 → 打开 booking_url
 * （设备上唤起美团 App 或浏览器）；“电话”按钮 → 拨号。
 */
@Composable
fun HotelCardList(
    cards: List<HotelCard>,
    modifier: Modifier = Modifier
) {
    if (cards.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cards.forEach { card ->
            HotelCardItem(card = card)
        }
    }
}

@Composable
private fun HotelCardItem(card: HotelCard) {
    val context = LocalContext.current

    // 卡片本体点击 → 打开预订链接（优先 bookingUrl，其次 book_now 动作的 url）
    val primaryUrl = card.bookingUrl ?: card.actions?.firstOrNull { it.type == "book_now" }?.url
    val onClickCard: () -> Unit = {
        if (!primaryUrl.isNullOrBlank()) openExternalUrl(context, primaryUrl)
    }

    // 动作按钮：优先用后端 actions；否则用 bookingUrl / phone 兜底生成
    val actions: List<CardAction> = card.actions ?: buildList {
        if (!card.bookingUrl.isNullOrBlank()) add(CardAction("book_now", "预订", url = card.bookingUrl))
        if (!card.phone.isNullOrBlank()) add(CardAction("call", "电话", phone = card.phone))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .clickable(enabled = !primaryUrl.isNullOrBlank(), onClick = onClickCard)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = card.title ?: card.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 摘要描述（清理后端 markdown 后展示）
            val summary = cleanMarkdown(card.summary)
            if (!summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 评分 + 距离
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!card.rating.isNullOrBlank()) {
                    Text(
                        text = "★ ${card.rating}",
                        color = BrandAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!card.distance.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📍 ${card.distance}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 价格（后端可能为数字字符串或文本，如"价格待确认"）
            if (!card.price.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                val priceText = if (card.price.matches(Regex("^\\d"))) "¥${card.price}" else card.price
                Text(
                    text = priceText,
                    color = Color(0xFFE53935),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 动作按钮
            if (actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEach { act ->
                        CardActionButton(act = act, card = card)
                    }
                    // "添加到行程" 按钮
                    AddToTripButton(card = card)
                }
            }
        }
    }
}

@Composable
private fun CardActionButton(act: CardAction, card: HotelCard) {
    val context = LocalContext.current
    val isCall = act.type == "call"
    val onClick = {
        when {
            isCall -> {
                val phone = act.phone ?: card.phone
                if (!phone.isNullOrBlank()) dialPhone(context, phone)
            }
            !act.url.isNullOrBlank() -> openExternalUrl(context, act.url)
            !card.bookingUrl.isNullOrBlank() -> openExternalUrl(context, card.bookingUrl)
        }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCall) BgCard else BrandAccent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCall) Icons.Outlined.Call else Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            tint = if (isCall) BrandAccent else Color.White,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = act.label,
            color = if (isCall) BrandAccent else Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AddToTripButton(card: HotelCard) {
    val context = LocalContext.current
    val title = card.title ?: card.name
    val summary = cleanMarkdown(card.summary) ?: ""
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .clickable {
                // 直接写入行程存储（不在数字人页时 ItineraryViewModel 未实例化）
                val item = com.example.let_us_go.data.model.ItineraryItem(
                    id = "booking_${System.currentTimeMillis()}",
                    type = "booking", kind = "hotel",
                    title = title, summary = summary,
                    date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(System.currentTimeMillis()),
                    note = "来自数字人"
                )
                com.example.let_us_go.data.prefs.ItineraryStore.addItem(context, item)
                android.widget.Toast.makeText(context, "已添加到我的行程", android.widget.Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.BookmarkAdd,
            contentDescription = null,
            tint = BrandAccent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "加行程",
            color = BrandAccent,
            fontSize = 12.sp
        )
    }
}

/**
 * 清理后端 summary 中的 markdown 标记，便于在卡片内纯文本展示：
 * - [**文本**](url) → 文本
 * - **文本** → 文本
 * - 合并多余空白
 */
private fun cleanMarkdown(src: String?): String? {
    if (src.isNullOrBlank()) return src
    var s = src.replace(Regex("\\[\\*\\*(.+?)\\*\\*\\]\\((?:.+?)\\)")) { mr -> mr.groupValues[1] }
    s = s.replace(Regex("\\*\\*(.+?)\\*\\*")) { mr -> mr.groupValues[1] }
    s = s.replace(Regex("\\n{2,}"), "\n").replace(Regex("[ \\t]{2,}"), " ").trim()
    return s
}
