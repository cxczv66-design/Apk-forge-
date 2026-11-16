package com.example.ui.components.space

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalSpaceUiPreferences
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.SpaceDimensions
import com.example.ui.theme.SpaceShapes

/**
 * بطاقة زجاجية مستقبلية بإطار نيون وظلال 3D متعددة الطبقات واستجابة ضغط موحدة
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeonGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = SpaceColors.NeonCyan,
    secondaryAccent: Color = SpaceColors.NeonPurple,
    shape: Shape = SpaceShapes.CardLarge,
    elevation: Dp = 14.dp,
    borderWidth: Dp = SpaceDimensions.CardBorderWidth,
    glassAlpha: Float = 0.86f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    val view = LocalView.current
    val prefs = LocalSpaceUiPreferences.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var longPressFlash by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null && enabled) 0.972f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "card_press_scale"
    )
    val pressOffsetDp by animateDpAsState(
        targetValue = if (isPressed && onClick != null && enabled) 2.dp else 0.dp,
        animationSpec = tween(110),
        label = "card_press_offset"
    )
    val activeAccent by animateColorAsState(
        targetValue = if (longPressFlash) SpaceColors.Gold else accentColor,
        animationSpec = tween(220),
        label = "card_accent"
    )

    val cardModifier = modifier
        .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            translationY = pressOffsetDp.toPx()
        }
        .shadow(
            elevation = if (isPressed) (elevation * 0.65f) else elevation,
            shape = shape,
            spotColor = activeAccent.copy(alpha = 0.55f),
            ambientColor = Color.Black.copy(alpha = 0.9f)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                listOf(
                    SpaceColors.SurfaceElevated.copy(alpha = glassAlpha),
                    SpaceColors.MidnightBlue.copy(alpha = glassAlpha),
                    SpaceColors.SurfaceDeep.copy(alpha = (glassAlpha + 0.08f).coerceAtMost(0.98f))
                )
            )
        )
        .drawBehind {
            // Soft inner top light reflection + bottom rim glow
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeAccent.copy(alpha = if (isPressed) 0.24f else 0.11f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.maxDimension * 0.85f
                )
            )
        }
        .border(
            border = BorderStroke(
                width = if (isPressed) borderWidth + 0.5.dp else borderWidth,
                brush = Brush.linearGradient(
                    listOf(
                        activeAccent.copy(alpha = if (isPressed) 0.95f else 0.72f),
                        SpaceColors.White.copy(alpha = 0.14f),
                        secondaryAccent.copy(alpha = if (isPressed) 0.75f else 0.42f)
                    )
                )
            ),
            shape = shape
        )
        .then(
            if (onClick != null || onLongClick != null) {
                Modifier.combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = activeAccent.copy(alpha = 0.45f)),
                    enabled = enabled,
                    onClick = {
                        prefs.triggerTouchFeedback(view, isLongPress = false)
                        onClick?.invoke()
                    },
                    onLongClick = onLongClick?.let { longCb ->
                        {
                            longPressFlash = true
                            prefs.triggerTouchFeedback(view, isLongPress = true)
                            longCb()
                        }
                    }
                )
            } else Modifier
        )

    LaunchedEffect(longPressFlash) {
        if (longPressFlash) {
            kotlinx.coroutines.delay(260)
            longPressFlash = false
        }
    }

    Column(modifier = cardModifier, content = content)
}
