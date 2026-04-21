package com.chatai.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ChatColors.Accent,
    onPrimary = ChatColors.TextOnAccent,
    primaryContainer = ChatColors.Accent,
    onPrimaryContainer = ChatColors.TextOnAccent,
    secondary = ChatColors.Surface,
    onSecondary = ChatColors.TextPrimary,
    secondaryContainer = ChatColors.SurfaceVariant,
    onSecondaryContainer = ChatColors.TextPrimary,
    tertiary = ChatColors.UserAvatarBg,
    onTertiary = ChatColors.TextOnAccent,
    background = ChatColors.Background,
    onBackground = ChatColors.TextPrimary,
    surface = ChatColors.Surface,
    onSurface = ChatColors.TextPrimary,
    surfaceVariant = ChatColors.SurfaceVariant,
    onSurfaceVariant = ChatColors.TextSecondary,
    error = ChatColors.Error,
    onError = ChatColors.TextOnAccent,
    outline = ChatColors.Border,
    outlineVariant = ChatColors.Divider,
)

@Composable
fun ChatAITheme(
    darkTheme: Boolean = true, // Force dark theme like ChatGPT
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ChatColors.Background.toArgb()
            window.navigationBarColor = ChatColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
