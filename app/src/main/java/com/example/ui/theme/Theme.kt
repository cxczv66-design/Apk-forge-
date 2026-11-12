package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppDarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = Color(0xFF1A1000),
    primaryContainer = Color(0xFF472700),
    onPrimaryContainer = GoldAccent,
    
    secondary = OrangeNeon,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1E00),
    onSecondaryContainer = Color(0xFFFFDBC8),
    
    tertiary = CyanAccent,
    onTertiary = Color(0xFF003732),
    tertiaryContainer = Color(0xFF004F48),
    onTertiaryContainer = Color(0xFF70F8E7),
    
    background = DarkBackground,
    onBackground = TextPrimary,
    
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    
    outline = GlassBorder,
    outlineVariant = GlassBorderSubtle,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to stunning dark mode as requested
    dynamicColor: Boolean = false, // Keep intentional golden/amber brand theme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}
