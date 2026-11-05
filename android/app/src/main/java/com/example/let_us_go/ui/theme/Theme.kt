package com.example.let_us_go.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** 全局字号缩放（适老化模式 1.0~1.6） */
val LocalFontScale = staticCompositionLocalOf { 1.0f }

private val Color_LightErrorContainer = androidx.compose.ui.graphics.Color(0xFFFFE0E0)

/** 浅色配色（与 uni 端同色系） */
private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BgCard,
    primaryContainer = BrandPrimaryLight,
    onPrimaryContainer = BrandPrimaryDark,
    secondary = BrandAccent,
    onSecondary = BgCard,
    secondaryContainer = BrandAccentLight,
    onSecondaryContainer = BrandAccentDark,
    tertiary = BrandSecondary,
    onTertiary = BgCard,
    error = BrandDanger,
    onError = BgCard,
    errorContainer = Color_LightErrorContainer,
    background = BgPage,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgPage,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    outlineVariant = Divider,
)

/** 暗色配色（占位） */
private val DarkColors = darkColorScheme(
    primary = BrandAccent,
    onPrimary = DarkBgPage,
    primaryContainer = BrandAccentDark,
    onPrimaryContainer = BrandAccentLight,
    secondary = BrandSecondary,
    onSecondary = DarkBgPage,
    error = BrandDanger,
    onError = BgCard,
    background = DarkBgPage,
    onBackground = DarkTextPrimary,
    surface = DarkBgCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkBgCard,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider,
)

/**
 * 字号体系（适老化模式可放大 1.5x）
 * 对应 uni 端：font-size: 14px 16px 28rpx
 */
private fun scaledTypography(scale: Float): Typography {
    fun sp(s: Int) = (s * scale).sp
    val base = FontFamily.Default
    return Typography(
        // 标题
        displayLarge = TextStyle(fontFamily = base, fontSize = sp(36), fontWeight = FontWeight.Bold),
        displayMedium = TextStyle(fontFamily = base, fontSize = sp(32), fontWeight = FontWeight.Bold),
        displaySmall = TextStyle(fontFamily = base, fontSize = sp(28), fontWeight = FontWeight.Bold),
        headlineLarge = TextStyle(fontFamily = base, fontSize = sp(24), fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontFamily = base, fontSize = sp(20), fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(fontFamily = base, fontSize = sp(18), fontWeight = FontWeight.Bold),
        // 标题
        titleLarge = TextStyle(fontFamily = base, fontSize = sp(18), fontWeight = FontWeight.Medium),
        titleMedium = TextStyle(fontFamily = base, fontSize = sp(16), fontWeight = FontWeight.Medium),
        titleSmall = TextStyle(fontFamily = base, fontSize = sp(14), fontWeight = FontWeight.Medium),
        // 正文
        bodyLarge = TextStyle(fontFamily = base, fontSize = sp(16), fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = base, fontSize = sp(14), fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = base, fontSize = sp(12), fontWeight = FontWeight.Normal),
        // 标签
        labelLarge = TextStyle(fontFamily = base, fontSize = sp(14), fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontFamily = base, fontSize = sp(12), fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontFamily = base, fontSize = sp(11), fontWeight = FontWeight.Medium),
    )
}

/**
 * 「随行」主题
 * @param fontScale 全局字号缩放（适老化模式）
 * @param darkTheme 是否深色（暂不启用，业务未实现）
 * @param dynamicColor 关闭（uni 端风格需要稳定品牌色）
 */
@Composable
fun SuiXingTheme(
    fontScale: Float = 1.0f,
    darkTheme: Boolean = false,  // 强制浅色模式，暗色主题未适配
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) DarkColors else LightColors
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalFontScale provides fontScale,
        LocalDensity provides Density(
            density = density.density,
            fontScale = density.fontScale * fontScale
        )
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography(fontScale)
        ) {
            androidx.compose.material3.Surface(
                color = colorScheme.background,
                content = content
            )
        }
    }
}

/** 兼容旧名字（MainActivity 临时还在引用） */
@Composable
fun Let_us_goTheme(
    fontScale: Float = 1.0f,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) = SuiXingTheme(fontScale = fontScale, darkTheme = darkTheme, content = content)
