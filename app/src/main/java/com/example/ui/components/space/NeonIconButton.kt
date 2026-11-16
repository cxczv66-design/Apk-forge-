package com.example.ui.components.space

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalSpaceUiPreferences
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.SpaceDimensions

/**
 * زر أيقونة نيون دائري/مربع دائري بمساحة لمس لا تقل عن 48dp
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeonIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    tint: Color = SpaceColors.NeonCyan,
    borderAccent: Color = tint,
    size: Dp = SpaceDimensions.IconButtonSize,
    shape: Shape = RoundedCornerShape(15.dp),
    enabled: Boolean = true,
    testTag: String = ""
) {
    val view = LocalView.current
    val prefs = LocalSpaceUiPreferences.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.91f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "icon_btn_scale"
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) 2.dp else 0.dp,
        animationSpec = tween(90),
        label = "icon_btn_offset"
    )

    Box(
        modifier = modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .size(size.coerceAtLeast(SpaceDimensions.TouchTargetMin))
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = pressOffset.toPx()
            }
            .shadow(
                elevation = if (enabled) (if (isPressed) 4.dp else 10.dp) else 0.dp,
                shape = shape,
                spotColor = borderAccent.copy(alpha = 0.55f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        SpaceColors.SurfaceElevated.copy(alpha = if (isPressed) 0.96f else 0.85f),
                        SpaceColors.MidnightBlue
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        borderAccent.copy(alpha = if (isPressed) 0.95f else 0.62f),
                        SpaceColors.ElectricBlue.copy(alpha = 0.35f)
                    )
                ),
                shape = shape
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(color = borderAccent.copy(alpha = 0.5f)),
                enabled = enabled,
                onClick = {
                    prefs.triggerTouchFeedback(view, isLongPress = false)
                    onClick()
                },
                onLongClick = onLongClick?.let { longCb ->
                    {
                        prefs.triggerTouchFeedback(view, isLongPress = true)
                        longCb()
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else SpaceColors.MutedText,
            modifier = Modifier.size(22.dp)
        )
    }
}
