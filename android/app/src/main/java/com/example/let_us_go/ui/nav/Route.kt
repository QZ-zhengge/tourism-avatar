package com.example.let_us_go.ui.nav

/**
 * 路由表（一处定义，多处复用）
 *
 * 与 uni-app `pages.json → pages` 一一对应：
 * 21 个页面路径
 */
object Route {

    // ── Tab 页（5）──
    const val HOME = "home"
    const val MAP = "map"
    const val DIGITAL_HUMAN = "digital_human"
    const val MOMENTS = "moments"
    const val MINE = "mine"

    // ── 登录/注册/老化（4）──
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val AGING = "aging"
    const val INDEX = "index"

    // ── 工具页（12）──
    const val AI_CHAT = "ai_chat"
    const val DIGITAL_STREAM = "digital_stream"
    const val SCENIC = "scenic"
    const val EVENTS = "events"
    const val FOOD = "food"
    const val CHECKIN = "checkin"
    const val GUIDE = "guide"
    const val GUIDE_DETAIL = "guide_detail"       // 游玩攻略详情
    const val NEARBY = "nearby"
    const val HOTSPOTS = "hotspots"
    const val SEARCH = "search"
    const val ITINERARY = "itinerary"
    const val LIVE_GUIDE = "live_guide"
    const val LIVE_GUIDE_FLOW = "live_guide_flow" // 实时讲解流程（选图+识别）

    // ── 我的页子页面 ──
    const val PROFILE_EDIT = "profile_edit"       // 个人资料编辑
    const val FAVORITES = "favorites"           // 我的收藏
    const val GUIDE_HISTORY = "guide_history"   // 导览历史
    const val DH_HISTORY = "dh_history"              // 实时讲解历史
    const val AVATAR_CHAT_HISTORY = "avatar_chat_history" // 数字人对话历史
    const val AVATAR_CHAT_DETAIL = "avatar_chat_detail"   // 数字人对话历史详情
    const val FAQ = "faq"                       // 常见问题
    const val HELP = "help"                     // 帮助中心
    const val FEEDBACK = "feedback"             // 意见反馈
    const val MY_POSTS = "my_posts"             // 我的帖子
    const val MY_LIKES = "my_likes"             // 我的点赞
    const val MY_COMMENTS = "my_comments"       // 我的评论
    const val NOTIFICATIONS = "notifications"   // 消息通知设置
    const val PRIVACY = "privacy"               // 隐私设置
    const val ABOUT = "about"                   // 关于我们

    // ── 路由表（用于统一注册）──
    val all = listOf(
        // Tab（注册到 navHost，composable 自身处理 bottom bar 隐藏）
        HOME, MAP, DIGITAL_HUMAN, MOMENTS, MINE,
        // 普通
        LOGIN, REGISTER, AGING, INDEX,
        AI_CHAT, DIGITAL_STREAM, SCENIC, EVENTS, FOOD, CHECKIN,
        GUIDE, GUIDE_DETAIL, NEARBY, HOTSPOTS, SEARCH, ITINERARY, LIVE_GUIDE, LIVE_GUIDE_FLOW,
        // 我的页子页面
        PROFILE_EDIT, FAVORITES, GUIDE_HISTORY, DH_HISTORY, AVATAR_CHAT_HISTORY, AVATAR_CHAT_DETAIL, FAQ, HELP, FEEDBACK,
        MY_POSTS, MY_LIKES, MY_COMMENTS, NOTIFICATIONS, PRIVACY, ABOUT,
    )

    /** 是否为 Tab 页（用于决定是否显示底部 TabBar） */
    val tabs = setOf(HOME, MAP, DIGITAL_HUMAN, MOMENTS, MINE)
}
