package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.LocalSpaceUiPreferences
import com.example.ui.theme.SpaceColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlowingButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    testTag: String = "glowing_button",
    brush: Brush = SpaceColors.CyanBlueGradient,
    textColor: Color = SpaceColors.White,
    height: Dp = 52.dp
) {
    val view = LocalView.current
    val prefs = LocalSpaceUiPreferences.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var sweepTrigger by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "glow_btn_scale"
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled && !isLoading) 2.5.dp else 0.dp,
        animationSpec = tween(90),
        label = "glow_btn_offset"
    )
    val sweepFraction by animateFloatAsState(
        targetValue = if (sweepTrigger) 1.4f else -0.4f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "glow_btn_sweep"
    )

    LaunchedEffect(sweepTrigger) {
        if (sweepTrigger) {
            delay(360)
            sweepTrigger = false
        }
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(height)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = pressOffset.toPx()
            }
            .shadow(
                elevation = if (enabled) (if (isPressed) 6.dp else 14.dp) else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = SpaceColors.NeonCyan.copy(alpha = 0.55f),
                spotColor = SpaceColors.NeonPurple.copy(alpha = 0.72f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) brush
                else Brush.horizontalGradient(listOf(SpaceColors.SurfaceCard, SpaceColors.SurfaceDeep))
            )
            .drawBehind {
                if (enabled && sweepFraction > -0.35f && sweepFraction < 1.35f) {
                    val sx = size.width * sweepFraction
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent)),
                        start = Offset(sx - 26f, 0f),
                        end = Offset(sx + 26f, size.height),
                        strokeWidth = 34f
                    )
                }
            }
            .border(
                width = 1.1.dp,
                brush = Brush.horizontalGradient(
                    if (enabled) listOf(SpaceColors.NeonCyan.copy(alpha = 0.85f), SpaceColors.NeonPurple.copy(alpha = 0.7f))
                    else listOf(SpaceColors.GlassBorderSubtle, SpaceColors.GlassBorderSubtle)
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = SpaceColors.NeonCyan.copy(alpha = 0.5f)),
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
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = textColor,
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
                        tint = if (enabled) textColor else SpaceColors.MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (enabled) textColor else SpaceColors.MutedText
                )
            }
        }
    }
}
