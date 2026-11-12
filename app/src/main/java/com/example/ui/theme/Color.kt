package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dark luxury palette with warm amber / neon orange highlights matching the video
val DarkBackground = Color(0xFF0C0D12)
val DarkSurface = Color(0xFF13141C)
val DarkSurfaceElevated = Color(0xFF1B1D28)
val DarkSurfaceCard = Color(0xFF181A24)
val DarkSurfaceGlass = Color(0xB81A1D28)
val DarkSurfaceGlassHover = Color(0xD9222636)

// The signature ApkForge bright orange seen in video (#FF7A2F)
val OrangePrimary = Color(0xFFFF7A2F)
val OrangeNeon = Color(0xFFFF6B00)
val AmberPrimary = Color(0xFFFF7A2F)
val AmberVariant = Color(0xFFFF9F1C)
val GoldAccent = Color(0xFFFFD166)

// Upgraded status green and WhatsApp green
val StatusGreen = Color(0xFF10B981)
val WhatsAppGreen = Color(0xFF25D366)
val SuccessGreen = Color(0xFF10B981)
val CyanAccent = Color(0xFF2EC4B6)
val ErrorRed = Color(0xFFEF4444)
val WarningYellow = Color(0xFFF59E0B)

val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)

val GlassBorder = Color(0x33FF7A2F)
val GlassBorderSubtle = Color(0x1FFFFFFF)

val TerminalBackground = Color(0xFF090A0F)
val TerminalText = Color(0xFF60A5FA)
val TerminalGreen = Color(0xFF34D399)
val TerminalYellow = Color(0xFFFBBF24)

/**
 * دالة مساعدة لتحويل كود الألوان Hex إلى كائن Color بأمان لمنع أي أخطاء وقت التشغيل
 */
fun parseColorSafe(hex: String?, fallback: Color = OrangePrimary): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(clean))
    } catch (e: Exception) {
        fallback
    }
}
