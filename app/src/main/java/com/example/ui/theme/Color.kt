package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Unified Futuristic Space Palette with backward-compatible tokens across legacy modules
val DarkBackground = SpaceColors.DeepSpace
val DarkSurface = SpaceColors.MidnightBlue
val DarkSurfaceElevated = SpaceColors.SurfaceElevated
val DarkSurfaceCard = SpaceColors.SurfaceCard
val DarkSurfaceGlass = SpaceColors.GlassFill
val DarkSurfaceGlassHover = SpaceColors.GlassFillBright

// Primary futuristic accents replacing legacy amber/orange tokens cleanly
val OrangePrimary = SpaceColors.ElectricBlue
val OrangeNeon = SpaceColors.NeonPurple
val AmberPrimary = SpaceColors.ElectricBlue
val AmberVariant = SpaceColors.Gold
val GoldAccent = SpaceColors.Gold

val StatusGreen = SpaceColors.Emerald
val WhatsAppGreen = Color(0xFF00E6A0)
val SuccessGreen = SpaceColors.Emerald
val CyanAccent = SpaceColors.NeonCyan
val ErrorRed = SpaceColors.ErrorRed
val WarningYellow = SpaceColors.Gold

val TextPrimary = SpaceColors.White
val TextSecondary = SpaceColors.MutedText
val TextMuted = SpaceColors.SubtleText

val GlassBorder = SpaceColors.GlassBorderCyan
val GlassBorderSubtle = SpaceColors.GlassBorderSubtle

val TerminalBackground = SpaceColors.TerminalBg
val TerminalText = Color(0xFF5EEAD4)
val TerminalGreen = SpaceColors.Emerald
val TerminalYellow = SpaceColors.Gold

/**
 * دالة مساعدة لتحويل كود الألوان Hex إلى كائن Color بأمان لمنع أي أخطاء وقت التشغيل
 */
fun parseColorSafe(hex: String?, fallback: Color = SpaceColors.NeonCyan): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
}
