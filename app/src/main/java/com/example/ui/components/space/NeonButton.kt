package com.example.ui.components.space

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.LocalSpaceUiPreferences
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.SpaceDimensions
import kotlinx.coroutines.delay

enum class NeonButtonStyle {
    PRIMARY_GLOW,
    CYBER_OUTLINE,
    PURPLE_AI,
    GOLD_ENGINE,
    EMERALD_SUCCESS,
    DANGER_RED
}

/**
 * زر مستقبلي موحد مع نظام التفاعل الفضائي الكامل:
 * - انخفاض بصري بسيط + Spring Scale
 * - توهج داخلي متزايد + شريط إضاءة متحرك عند الضغط
 * - Ripple مخصص + اهتزاز وصوت لمس اختياري
 * - دعم الضغط المطول مع الحفاظ الكامل على onClick الأصلي
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    style: NeonButtonStyle = NeonButtonStyle.PRIMARY_GLOW,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = SpaceDimensions.ButtonHeight,
    shape: Shape = RoundedCornerShape(16.dp),
    testTag: String = ""
) {
    val view = LocalView.current
    val prefs = LocalSpaceUiPreferences.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var sweepTrigger by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )
    val pressTranslateY by animateDpAsState(
        targetValue = if (isPressed && enabled && !isLoading) 2.5.dp else 0.dp,
        animationSpec = tween(90),
        label = "btn_translate"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.55f else 0.25f,
        animationSpec = tween(150),
        label = "btn_glow"
    )
    val sweepFraction by animateFloatAsState(
        targetValue = if (sweepTrigger) 1.4f else -0.4f,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "btn_light_sweep"
    )

    LaunchedEffect(sweepTrigger) {
        if (sweepTrigger) {
            delay(380)
            sweepTrigger = false
        }
    }

    val (accent, secondary, bgBrush, labelColor) = when (style) {
        NeonButtonStyle.PRIMARY_GLOW -> Quad(
            SpaceColors.NeonCyan,
            SpaceColors.NeonPurple,
            Brush.horizontalGradient(listOf(SpaceColors.ElectricBlue, SpaceColors.NeonCyan.copy(alpha = 0.88f))),
            SpaceColors.White
        )
        NeonButtonStyle.PURPLE_AI -> Quad(
            SpaceColors.NeonPurple,
            SpaceColors.NeonCyan,
            Brush.horizontalGradient(listOf(SpaceColors.Violet, SpaceColors.NeonPurple)),
            SpaceColors.White
        )
        NeonButtonStyle.GOLD_ENGINE -> Quad(
            SpaceColors.Gold,
            Color(0xFFFF8C00),
            Brush.horizontalGradient(listOf(SpaceColors.Gold, Color(0xFFFF8C00))),
            SpaceColors.DeepSpace
        )
        NeonButtonStyle.EMERALD_SUCCESS -> Quad(
            SpaceColors.Emerald,
            SpaceColors.NeonCyan,
            Brush.horizontalGradient(listOf(SpaceColors.Emerald, Color(0xFF00C896))),
            SpaceColors.DeepSpace
        )
        NeonButtonStyle.DANGER_RED -> Quad(
            SpaceColors.ErrorRed,
            Color(0xFFB91C1C),
            Brush.horizontalGradient(listOf(SpaceColors.ErrorRed, Color(0xFFD91E48))),
            SpaceColors.White
        )
        NeonButtonStyle.CYBER_OUTLINE -> Quad(
            SpaceColors.NeonCyan,
            SpaceColors.ElectricBlue,
            Brush.horizontalGradient(listOf(SpaceColors.MidnightBlue, SpaceColors.SurfaceElevated)),
            SpaceColors.White
        )
    }

    val finalBrush = if (enabled) bgBrush else Brush.horizontalGradient(
        listOf(SpaceColors.SurfaceCard, SpaceColors.SurfaceDeep)
    )

    Box(
        modifier = modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .heightIn(min = height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = pressTranslateY.toPx()
            }
            .shadow(
                elevation = if (enabled) (if (isPressed) 6.dp else 14.dp) else 0.dp,
                shape = shape,
                spotColor = accent.copy(alpha = 0.68f),
                ambientColor = secondary.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(finalBrush)
            .drawBehind {
                if (enabled) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = glowAlpha * 0.45f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.2f),
                            radius = size.width * 0.65f
                        )
                    )
                    if (sweepFraction > -0.35f && sweepFraction < 1.35f) {
                        val sx = size.width * sweepFraction
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.42f), Color.Transparent)
                            ),
                            start = Offset(sx - 28f, 0f),
                            end = Offset(sx + 28f, size.height),
                            strokeWidth = 36f
                        )
                    }
                }
            }
            .border(
                width = if (style == NeonButtonStyle.CYBER_OUTLINE) 1.3.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    if (enabled) listOf(accent.copy(alpha = 0.9f), secondary.copy(alpha = 0.65f))
                    else listOf(SpaceColors.GlassBorderSubtle, SpaceColors.GlassBorderSubtle)
                ),
                shape = shape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = accent.copy(alpha = 0.5f)),
                enabled = enabled && !isLoading,
                onClick = {
                    sweepTrigger = true
                    prefs.triggerTouchFeedback(view, isLongPress = false)
                    onClick()
                },
                onLongClick = onLongClick?.let { longCb ->
                    {
                        sweepTrigger = true
                        prefs.triggerTouchFeedback(view, isLongPress = true)
                        longCb()
                    }
                }
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = labelColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) labelColor else SpaceColors.MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = if (enabled) labelColor else SpaceColors.MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
