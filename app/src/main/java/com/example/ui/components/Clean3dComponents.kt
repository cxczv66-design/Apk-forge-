package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Clean 3D UI Surface Card:
 * بطاقة نظيفة بعمق 3D حقيقي، حواف مضيئة، وظلال لونية متدرجة وفق إرشادات Material 3.
 */
@Composable
fun ThreeDSurfaceCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevation: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val baseModifier = modifier
        .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
        .shadow(
            elevation = elevation,
            shape = shape,
            spotColor = accentColor.copy(alpha = 0.45f),
            ambientColor = Color.Black.copy(alpha = 0.85f)
        )
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    DarkSurfaceElevated,
                    Color(0xFF141724),
                    Color(0xFF10121C)
                )
            )
        )
        .border(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                listOf(
                    accentColor.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.08f),
                    accentColor.copy(alpha = 0.25f)
                )
            ),
            shape = shape
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column(modifier = baseModifier, content = content)
}

@Composable
fun ThreeDIconBadge(
    icon: ImageVector,
    accentColor: Color,
    secondaryColor: Color = accentColor.copy(alpha = 0.65f),
    size: Dp = 50.dp,
    cornerRadius: Dp = 16.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = Modifier
            .size(size)
            .shadow(10.dp, shape, spotColor = accentColor.copy(alpha = 0.55f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.28f),
                        secondaryColor.copy(alpha = 0.16f)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(size * 0.52f)
        )
    }
}

@Composable
fun ThreeDStatPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
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
                color = TextSecondary,
                maxLines = 1
            )
        }
    }
}

/**
 * نواة الذكاء الاصطناعي الهولوغرافية المتحركة (Holographic 3D AI Orb)
 * لشاشة AI Wizard والمؤشرات الذكية.
 */
@Composable
fun HolographicAiOrb(
    icon: ImageVector,
    accentColor: Color = Color(0xFFA855F7),
    secondaryColor: Color = Color(0xFF38BDF8),
    size: Dp = 76.dp,
    isPulsing: Boolean = true
) {
    val infinite = rememberInfiniteTransition(label = "ai_orb")
    val pulse by infinite.animateFloat(
        initialValue = if (isPulsing) 0.94f else 1f,
        targetValue = if (isPulsing) 1.07f else 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orb_scale"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "orb_glow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size + 16.dp)
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
                .shadow(16.dp, CircleShape, spotColor = accentColor)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(accentColor, secondaryColor)
                    )
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
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
