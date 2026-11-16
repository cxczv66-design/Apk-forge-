package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.space.NeonGlassCard
import com.example.ui.components.space.Space3DFeatureBadge
import com.example.ui.theme.*

/**
 * Clean 3D UI Surface Card — مدمج مع نظام Space NeonGlassCard الموحد
 */
@Composable
fun ThreeDSurfaceCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    NeonGlassCard(
        accentColor = accentColor,
        secondaryAccent = SpaceColors.NeonPurple,
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        elevation = elevation,
        onClick = onClick,
        onLongClick = onLongClick,
        testTag = testTag,
        content = content
    )
}

@Composable
fun ThreeDIconBadge(
    icon: ImageVector,
    accentColor: Color,
    secondaryColor: Color = SpaceColors.ElectricBlue,
    size: Dp = 50.dp,
    cornerRadius: Dp = 16.dp
) {
    Space3DFeatureBadge(
        icon = icon,
        accentColor = accentColor,
        secondaryColor = secondaryColor,
        size = size,
        modifier = Modifier.clip(RoundedCornerShape(cornerRadius))
    )
}

@Composable
fun ThreeDStatPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SpaceColors.SurfaceDeep.copy(alpha = 0.82f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(accentColor.copy(alpha = 0.65f), SpaceColors.NeonPurple.copy(alpha = 0.25f))
            )
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = accentColor
            )
            Text(
                text = label,
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = SpaceColors.MutedText,
                maxLines = 1
            )
        }
    }
}

/**
 * نواة الذكاء الاصطناعي الهولوغرافية المتحركة (Holographic 3D AI Orb)
 */
@Composable
fun HolographicAiOrb(
    icon: ImageVector,
    accentColor: Color = SpaceColors.NeonPurple,
    secondaryColor: Color = SpaceColors.NeonCyan,
    size: Dp = 76.dp,
    isPulsing: Boolean = true
) {
    val prefs = LocalSpaceUiPreferences.current
    val infinite = rememberInfiniteTransition(label = "ai_orb")
    val pulse by if (!prefs.reducedMotion && isPulsing) {
        infinite.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "orb_scale"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }
    val glowAlpha by if (!prefs.reducedMotion) {
        infinite.animateFloat(
            initialValue = 0.34f,
            targetValue = 0.78f,
            animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
            label = "orb_glow"
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0.55f) }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 18.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = glowAlpha),
                            secondaryColor.copy(alpha = glowAlpha * 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(size)
                .shadow(18.dp, CircleShape, spotColor = secondaryColor)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accentColor, SpaceColors.ElectricBlue, secondaryColor)
                    )
                )
                .border(1.6.dp, SpaceColors.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SpaceColors.White,
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

fun Modifier.threeDPerspectiveTilt(enabled: Boolean): Modifier {
    return if (!enabled) this else this.graphicsLayer {
        cameraDistance = 14f * density
        rotationX = 5.5f
        rotationY = -4.5f
        scaleX = 0.96f
        scaleY = 0.96f
    }
}
