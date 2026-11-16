package com.example.ui.components.space

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalSpaceUiPreferences
import com.example.ui.theme.SpaceColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class SpaceStar(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val phase: Float,
    val speed: Float,
    val tint: Color
)

private data class SpaceParticle(
    val xFraction: Float,
    val yFraction: Float,
    val radiusDp: Float,
    val driftSpeed: Float,
    val parallaxLayer: Float,
    val tint: Color
)

/**
 * خلفية فضائية موحدة وعالية الأداء مع مجرات وسدم وكواكب 3D ونجوم متلألئة وخطوط طاقة.
 * يُنصح بوضعها مرة واحدة في جذر التطبيق أو الشاشات الأساسية دون تكرار مفرط.
 */
@Composable
fun SpaceBackground(
    modifier: Modifier = Modifier,
    parallaxProgress: Float = 0f,
    intensity: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val prefs = LocalSpaceUiPreferences.current
    val animate = !prefs.reducedMotion

    val stars = remember {
        val rng = Random(42091)
        List(58) { idx ->
            val tint = when (idx % 5) {
                0 -> SpaceColors.NeonCyan
                1 -> SpaceColors.White
                2 -> SpaceColors.NeonPurple.copy(alpha = 0.85f)
                3 -> SpaceColors.Gold.copy(alpha = 0.9f)
                else -> Color(0xFFBAE6FD)
            }
            SpaceStar(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat(),
                radiusDp = 0.7f + rng.nextFloat() * 1.6f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                speed = 0.6f + rng.nextFloat() * 1.6f,
                tint = tint
            )
        }
    }

    val particles = remember {
        val rng = Random(88317)
        List(18) { idx ->
            SpaceParticle(
                xFraction = rng.nextFloat(),
                yFraction = rng.nextFloat(),
                radiusDp = 1.6f + rng.nextFloat() * 2.8f,
                driftSpeed = 0.15f + rng.nextFloat() * 0.4f,
                parallaxLayer = 0.35f + (idx % 3) * 0.32f,
                tint = if (idx % 2 == 0) SpaceColors.NeonCyan else SpaceColors.NeonPurple
            )
        }
    }

    val infinite = rememberInfiniteTransition(label = "space_cosmos")
    val phaseShift by if (animate) {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 16000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cosmos_phase"
        )
    } else {
        remember { mutableFloatStateOf(0.8f) }
    }

    val nebulaPulse by if (animate) {
        infinite.animateFloat(
            initialValue = 0.82f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "nebula_pulse"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SpaceColors.DeepSpace,
                        SpaceColors.MidnightBlue,
                        Color(0xFF040B1E),
                        SpaceColors.DeepSpace
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val pxOffset = (parallaxProgress * 18f).dp.toPx()

            // Top-right Cyan/Electric nebula
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpaceColors.ElectricBlue.copy(alpha = 0.20f * intensity * nebulaPulse),
                        SpaceColors.NeonCyan.copy(alpha = 0.08f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.84f - pxOffset * 0.4f, h * 0.14f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.84f - pxOffset * 0.4f, h * 0.14f)
            )

            // Mid-left Neon Purple / Violet nebula
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpaceColors.NeonPurple.copy(alpha = 0.17f * intensity),
                        SpaceColors.Violet.copy(alpha = 0.09f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.12f + pxOffset * 0.5f, h * 0.42f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.12f + pxOffset * 0.5f, h * 0.42f)
            )

            // Bottom horizon cosmic glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpaceColors.NeonCyan.copy(alpha = 0.14f * intensity),
                        SpaceColors.ElectricBlue.copy(alpha = 0.09f * intensity),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.52f, h * 0.94f),
                    radius = w * 0.85f
                ),
                radius = w * 0.85f,
                center = Offset(w * 0.52f, h * 0.94f)
            )

            // Subtle 3D ringed distant planet top-left
            drawDistantPlanet(
                center = Offset(w * 0.10f - pxOffset * 0.3f, h * 0.16f),
                radius = w * 0.13f,
                ringRotation = -22f
            )

            // Small secondary cyan planet bottom-right
            drawDistantPlanet(
                center = Offset(w * 0.92f + pxOffset * 0.25f, h * 0.72f),
                radius = w * 0.09f,
                ringRotation = 18f,
                primary = SpaceColors.NeonCyan,
                secondary = SpaceColors.MidnightBlue
            )

            // Flowing cyan and purple energy wave arcs
            val waveShift = sin(phaseShift.toDouble()).toFloat() * 14.dp.toPx()
            val cyanWave = Path().apply {
                moveTo(-w * 0.1f, h * 0.28f + waveShift)
                cubicTo(
                    w * 0.30f, h * 0.21f - waveShift,
                    w * 0.68f, h * 0.34f + waveShift,
                    w * 1.1f, h * 0.25f - waveShift * 0.5f
                )
            }
            drawPath(
                path = cyanWave,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        SpaceColors.NeonCyan.copy(alpha = 0.16f * intensity),
                        SpaceColors.NeonPurple.copy(alpha = 0.14f * intensity),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )

            val purpleWave = Path().apply {
                moveTo(-w * 0.1f, h * 0.68f - waveShift * 0.6f)
                cubicTo(
                    w * 0.25f, h * 0.74f + waveShift,
                    w * 0.72f, h * 0.62f - waveShift,
                    w * 1.1f, h * 0.70f + waveShift * 0.5f
                )
            }
            drawPath(
                path = purpleWave,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        SpaceColors.NeonPurple.copy(alpha = 0.14f * intensity),
                        SpaceColors.ElectricBlue.copy(alpha = 0.15f * intensity),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Twinkling stars
            stars.forEach { star ->
                val twinkle = 0.28f + 0.68f * ((sin((phaseShift * star.speed + star.phase).toDouble()).toFloat() + 1f) * 0.5f)
                val cx = (star.xFraction * w + pxOffset * 0.2f).coerceIn(0f, w)
                val cy = star.yFraction * h
                val r = star.radiusDp.dp.toPx()
                drawCircle(
                    color = star.tint.copy(alpha = twinkle * intensity),
                    radius = r,
                    center = Offset(cx, cy)
                )
                if (star.radiusDp > 1.75f) {
                    drawLine(
                        color = star.tint.copy(alpha = twinkle * 0.42f * intensity),
                        start = Offset(cx - r * 2.3f, cy),
                        end = Offset(cx + r * 2.3f, cy),
                        strokeWidth = 0.7.dp.toPx()
                    )
                    drawLine(
                        color = star.tint.copy(alpha = twinkle * 0.42f * intensity),
                        start = Offset(cx, cy - r * 2.3f),
                        end = Offset(cx, cy + r * 2.3f),
                        strokeWidth = 0.7.dp.toPx()
                    )
                }
            }

            // Slow drifting glowing particles
            particles.forEach { p ->
                val yProgress = (p.yFraction - (phaseShift / (2f * PI.toFloat())) * p.driftSpeed + 1f) % 1f
                val cx = p.xFraction * w + sin((phaseShift + p.xFraction * 6f).toDouble()).toFloat() * 10.dp.toPx() + pxOffset * p.parallaxLayer
                val cy = yProgress * h
                val radius = p.radiusDp.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            p.tint.copy(alpha = 0.45f * intensity),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = radius * 3.2f
                    ),
                    radius = radius * 3.2f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = SpaceColors.White.copy(alpha = 0.65f * intensity),
                    radius = radius * 0.45f,
                    center = Offset(cx, cy)
                )
            }
        }

        content()
    }
}

private fun DrawScope.drawDistantPlanet(
    center: Offset,
    radius: Float,
    ringRotation: Float,
    primary: Color = SpaceColors.NeonPurple,
    secondary: Color = Color(0xFF0A1736)
) {
    // Soft atmosphere halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
            center = center,
            radius = radius * 1.9f
        ),
        radius = radius * 1.9f,
        center = center
    )

    // Planet sphere with 3D directional lighting
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = 0.55f),
                SpaceColors.ElectricBlue.copy(alpha = 0.35f),
                secondary.copy(alpha = 0.85f)
            ),
            center = Offset(center.x + radius * 0.32f, center.y - radius * 0.32f),
            radius = radius * 1.3f
        ),
        radius = radius,
        center = center
    )

    // Atmospheric rim arc
    drawCircle(
        color = SpaceColors.NeonCyan.copy(alpha = 0.28f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // Tilted planetary ring
    rotate(degrees = ringRotation, pivot = center) {
        drawOval(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    SpaceColors.NeonCyan.copy(alpha = 0.32f),
                    SpaceColors.NeonPurple.copy(alpha = 0.28f),
                    Color.Transparent
                )
            ),
            topLeft = Offset(center.x - radius * 1.85f, center.y - radius * 0.34f),
            size = androidx.compose.ui.geometry.Size(radius * 3.7f, radius * 0.68f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
