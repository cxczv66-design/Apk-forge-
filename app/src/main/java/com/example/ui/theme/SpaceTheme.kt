package com.example.ui.theme

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private val SpaceDarkColorScheme = darkColorScheme(
    primary = SpaceColors.NeonCyan,
    onPrimary = SpaceColors.DeepSpace,
    primaryContainer = Color(0xFF092A4C),
    onPrimaryContainer = SpaceColors.White,

    secondary = SpaceColors.NeonPurple,
    onSecondary = SpaceColors.White,
    secondaryContainer = Color(0xFF280D4F),
    onSecondaryContainer = SpaceColors.White,

    tertiary = SpaceColors.Gold,
    onTertiary = SpaceColors.DeepSpace,
    tertiaryContainer = Color(0xFF3A2400),
    onTertiaryContainer = SpaceColors.Gold,

    background = SpaceColors.DeepSpace,
    onBackground = SpaceColors.White,

    surface = SpaceColors.MidnightBlue,
    onSurface = SpaceColors.White,
    surfaceVariant = SpaceColors.SurfaceElevated,
    onSurfaceVariant = SpaceColors.MutedText,

    outline = SpaceColors.GlassBorderCyan,
    outlineVariant = SpaceColors.GlassBorderSubtle,
    error = SpaceColors.ErrorRed,
    onError = SpaceColors.White
)

@Stable
class SpaceUiPreferencesState(
    context: Context? = null
) {
    private val prefs = context?.getSharedPreferences("apkforge_space_ui_prefs", Context.MODE_PRIVATE)

    var soundEnabled by mutableStateOf(prefs?.getBoolean("touch_sound_enabled", false) ?: false)
        private set

    var reducedMotion by mutableStateOf(prefs?.getBoolean("reduced_motion_enabled", false) ?: false)
        private set

    fun updateSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
        runCatching { prefs?.edit()?.putBoolean("touch_sound_enabled", enabled)?.apply() }
    }

    fun updateReducedMotion(enabled: Boolean) {
        reducedMotion = enabled
        runCatching { prefs?.edit()?.putBoolean("reduced_motion_enabled", enabled)?.apply() }
    }

    fun triggerTouchFeedback(view: View?, isLongPress: Boolean = false) {
        if (view == null) return
        runCatching {
            view.performHapticFeedback(
                if (isLongPress) HapticFeedbackConstants.LONG_PRESS
                else HapticFeedbackConstants.KEYBOARD_TAP
            )
        }
        if (soundEnabled) {
            runCatching {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                val audioManager = view.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.35f)
            }
        }
    }
}

val LocalSpaceUiPreferences = compositionLocalOf { SpaceUiPreferencesState(null) }

@Composable
fun SpaceTheme(
    uiPreferences: SpaceUiPreferencesState = remember { SpaceUiPreferencesState(null) },
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalSpaceUiPreferences provides uiPreferences) {
        MaterialTheme(
            colorScheme = SpaceDarkColorScheme,
            typography = SpaceTypography,
            shapes = AppSpaceShapes,
            content = content
        )
    }
}
