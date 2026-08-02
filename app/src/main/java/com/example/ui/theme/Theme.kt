package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode {
    LIGHT, DARK, AMOLED, SYSTEM
}

private val DarkColorScheme = darkColorScheme(
    primary = ChatGptGreen,
    onPrimary = Color.White,
    primaryContainer = ChatGptSurfaceVariant,
    onPrimaryContainer = ChatGptTextPrimary,
    secondary = ChatGptGreen,
    onSecondary = Color.White,
    tertiary = ChatGptGreen,
    background = ChatGptBackground,
    onBackground = ChatGptTextPrimary,
    surface = ChatGptSurface,
    onSurface = ChatGptTextPrimary,
    surfaceVariant = ChatGptSurfaceVariant,
    onSurfaceVariant = ChatGptTextSecondary,
    outlineVariant = ChatGptBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = CyanVariant,
    onPrimaryContainer = Color.White,
    secondary = PurpleAccent,
    onSecondary = Color.White,
    tertiary = ElectricBlue,
    background = AmoledBackground,
    onBackground = Color.White,
    surface = AmoledSurface,
    onSurface = Color.White,
    surfaceVariant = AmoledCard,
    onSurfaceVariant = Color(0xFFC7C5D0)
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE3FF),
    onPrimaryContainer = Color(0xFF001946),
    secondary = PurpleAccent,
    onSecondary = Color.White,
    tertiary = CyanVariant,
    background = LightBackground,
    onBackground = Color(0xFF1B1B1F),
    surface = LightSurface,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF46464F)
)

@Composable
fun FaizulAiTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.AMOLED -> AmoledColorScheme
        AppThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (isSystemDark) {
                DarkColorScheme
            } else {
                LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    FaizulAiTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        dynamicColor = dynamicColor,
        content = content
    )
}

