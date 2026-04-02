package com.example.aichallenge.ui.theme

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

private val DarkGameScheme = darkColorScheme(
    primary = ArcadeNeonMint,
    onPrimary = Color(0xFF00322C),
    primaryContainer = Color(0xFF004D45),
    onPrimaryContainer = Color(0xFF8FF7EA),
    secondary = ArcadeNeonPink,
    onSecondary = Color(0xFF4A001F),
    secondaryContainer = Color(0xFF6B1A3D),
    onSecondaryContainer = Color(0xFFFFD8E6),
    tertiary = ArcadeViolet,
    onTertiary = Color(0xFF1C1040),
    tertiaryContainer = Color(0xFF352A6B),
    onTertiaryContainer = Color(0xFFE4DEFF),
    background = ArcadeDeepSpace,
    onBackground = ArcadeOnSurface,
    surface = ArcadeSurface,
    onSurface = ArcadeOnSurface,
    surfaceContainerLow = Color(0xFF12131C),
    surfaceContainer = ArcadeSurface,
    surfaceContainerHigh = ArcadeSurfaceHigh,
    surfaceContainerHighest = Color(0xFF25273A),
    surfaceVariant = Color(0xFF2A2D42),
    onSurfaceVariant = ArcadeMuted,
    outline = Color(0xFF5C6078),
    outlineVariant = Color(0xFF3D4055)
)

private val LightGameScheme = lightColorScheme(
    primary = ArcadeLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8FF7EA),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF9C3A63),
    onSecondary = Color.White,
    tertiary = Color(0xFF5E4BB8),
    onTertiary = Color.White,
    background = ArcadeDawnBottom,
    onBackground = ArcadeLightOnSurface,
    surface = Color(0xFFFFFBFF),
    onSurface = ArcadeLightOnSurface,
    surfaceContainerLow = ArcadeDawnTop,
    surfaceContainer = Color(0xFFF3EEFF),
    surfaceContainerHigh = Color(0xFFE8E0FF),
    surfaceVariant = Color(0xFFE4DFF5),
    onSurfaceVariant = Color(0xFF4A4A5C),
    outline = Color(0xFF7A7890)
)

@Composable
fun AIChallengeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkGameScheme
        else -> LightGameScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
