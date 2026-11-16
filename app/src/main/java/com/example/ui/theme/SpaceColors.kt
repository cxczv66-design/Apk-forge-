package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * نظام ألوان ApkForge الفضائي المستقبلي الموحد
 * Global Futuristic Space / Cyberpunk / Sci-Fi Palette
 */
object SpaceColors {
    val DeepSpace = Color(0xFF020611)
    val MidnightBlue = Color(0xFF061329)
    val SurfaceElevated = Color(0xFF0B1C3A)
    val SurfaceCard = Color(0xFF081630)
    val SurfaceDeep = Color(0xFF040B1B)

    val NeonCyan = Color(0xFF00E5FF)
    val ElectricBlue = Color(0xFF1677FF)
    val NeonPurple = Color(0xFFA020F0)
    val Violet = Color(0xFF7928CA)
    val Emerald = Color(0xFF00E6A0)
    val Gold = Color(0xFFFFB52E)
    val White = Color(0xFFF4F8FF)
    val MutedText = Color(0xFF91A8C9)
    val SubtleText = Color(0xFF637B9E)
    val ErrorRed = Color(0xFFFF365E)

    val GlassFill = Color(0xC8071630)
    val GlassFillBright = Color(0xDE0B1F42)
    val GlassBorderCyan = Color(0x6600E5FF)
    val GlassBorderPurple = Color(0x66A020F0)
    val GlassBorderSubtle = Color(0x2891A8C9)

    val TerminalBg = Color(0xFF030816)

    val HeroCtaGradient = Brush.horizontalGradient(
        listOf(NeonCyan, ElectricBlue, NeonPurple)
    )
    val CyanBlueGradient = Brush.horizontalGradient(
        listOf(NeonCyan, ElectricBlue)
    )
    val PurpleVioletGradient = Brush.horizontalGradient(
        listOf(NeonPurple, Violet)
    )
    val GoldAmberGradient = Brush.horizontalGradient(
        listOf(Gold, Color(0xFFFF8C00))
    )
    val EmeraldCyanGradient = Brush.horizontalGradient(
        listOf(Emerald, NeonCyan)
    )
    val NeonBorderGradient = Brush.linearGradient(
        listOf(
            NeonCyan.copy(alpha = 0.78f),
            ElectricBlue.copy(alpha = 0.45f),
            NeonPurple.copy(alpha = 0.72f)
        )
    )
}
