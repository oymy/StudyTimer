package com.oymyisme.studytimer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

/**
 * 暗色主题配色方案
 * 使用深色背景和绿蓝橙的色调组合，对眼睛更友好
 */
private val DarkColorScheme = darkColorScheme(
    // 主色调
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    
    // 次要色调
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    
    // 第三色调
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    
    // 背景和表面
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    
    // 错误色和轮廓
    error = DarkError,
    onError = DarkOnError,
    outline = DarkOutline
)

/**
 * 亮色主题配色方案
 * 使用浅色背景和相同的色调组合，适合白天使用
 */
private val LightColorScheme = lightColorScheme(
    // 主色调
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    
    // 次要色调
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    
    // 第三色调
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    
    // 背景和表面
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    
    // 错误色和轮廓
    error = LightError,
    onError = LightOnError,
    outline = LightOutline
)

/**
 * 学习计时器应用的主题组件
 * 
 * @param darkTheme 是否使用暗色主题，默认根据系统设置决定
 * @param dynamicColor 是否使用动态颜色（Android 12+），默认为 true
 * @param amoledBlack 是否使用纯黑色背景（适合 AMOLED 屏幕），默认为 false
 * @param content 内容组件
 */
@Composable
fun StudyTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    // Pure black mode for AMOLED screens
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    // 基于系统设置和用户选项决定使用的颜色方案
    val colorScheme = when {
        // 如果支持动态颜色且用户开启了动态颜色功能，使用系统生成的动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        
        // 如果是暗色主题且用户选择了纯黑模式，使用纯黑色背景
        darkTheme && amoledBlack -> DarkColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212)
        )
        
        // 正常的暗色主题
        darkTheme -> DarkColorScheme
        
        // 亮色主题
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}