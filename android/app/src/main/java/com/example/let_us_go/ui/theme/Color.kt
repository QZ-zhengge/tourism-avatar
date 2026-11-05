package com.example.let_us_go.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════
//  「随行」 品牌色（与 uni-app 端 uni.scss + manifest 一致）
// ════════════════════════════════════════════════════════════════

/** 主色：#1976d2（uni btn-primary） */
val BrandPrimary = Color(0xFF1976D2)
val BrandPrimaryDark = Color(0xFF0D47A1)
val BrandPrimaryLight = Color(0xFFE3F2FD)

/** 强调色：#2dba98（uni tabBar selectedColor，数字人主色） */
val BrandAccent = Color(0xFF2DBA98)
val BrandAccentDark = Color(0xFF1E8A70)
val BrandAccentLight = Color(0xFFA7E8D5)

/** 副色：#2e9eca（home 卡片背景） */
val BrandSecondary = Color(0xFF2E9ECA)

/** 危险：#f44336（btn-danger） */
val BrandDanger = Color(0xFFF44336)
val BrandDangerDark = Color(0xFFC62828)

/** 警告：#ff9800 */
val BrandWarning = Color(0xFFFF9800)

/** 页面背景：#f5f5f5（globalStyle.backgroundColor） */
val BgPage = Color(0xFFF5F5F5)

/** 卡片背景：#ffffff */
val BgCard = Color(0xFFFFFFFF)

/** 文字色阶：#333 / #666 / #999 */
val TextPrimary = Color(0xFF333333)
val TextSecondary = Color(0xFF666666)
val TextTertiary = Color(0xFF999999)

/** 分割线：#eeeeee */
val Divider = Color(0xFFEEEEEE)

/** TabBar 颜色 */
val TabNormal = Color(0xFF999999)
val TabActive = BrandAccent
val TabBg = Color(0xFFFFFFFF)

// ════════════════════════════════════════════════════════════════
//  暗色模式（占位，业务暂不启用深色）
// ════════════════════════════════════════════════════════════════

val DarkBgPage = Color(0xFF121212)
val DarkBgCard = Color(0xFF1E1E1E)
val DarkTextPrimary = Color(0xFFE6E6E6)
val DarkTextSecondary = Color(0xFFB3B3B3)
val DarkDivider = Color(0xFF2C2C2C)
