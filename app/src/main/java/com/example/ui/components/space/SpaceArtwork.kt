package com.example.ui.components.space

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.LocalSpaceUiPreferences
import com.example.ui.theme.SpaceColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * شعار ApkForge 3D المضيء (الهرم الثلاثي "A" بألوان النيون السيان والأرجواني)
 */
@Composable
fun SpaceApkForgeLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Ambient glow behind prism
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    SpaceColors.NeonCyan.copy(alpha = 0.38f),
                    SpaceColors.NeonPurple.copy(alpha = 0.22f),
                    Color.Transparent
                )
            ),
            radius = w * 0.72f,
            center = Offset(w * 0.5f, h * 0.55f)
        )

        // Outer neon A triangle frame
        val outerA = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.90f, h * 0.86f)
            lineTo(w * 0.68f, h * 0.86f)
            lineTo(w * 0.58f, h * 0.66f)
            lineTo(w * 0.40f, h * 0.66f)
            lineTo(w * 0.30f, h * 0.86f)
            lineTo(w * 0.10f, h * 0.86f)
            close()
        }

        drawPath(
            path = outerA,
            brush = Brush.linearGradient(
                colors = listOf(SpaceColors.NeonCyan, SpaceColors.ElectricBlue, SpaceColors.NeonPurple),
                start = Offset(w * 0.1f, h * 0.1f),
                end = Offset(w * 0.9f, h * 0.9f)
            )
        )

        // Bright beveled highlight stroke
        drawPath(
            path = outerA,
            brush = Brush.linearGradient(
                listOf(SpaceColors.White.copy(alpha = 0.9f), SpaceColors.NeonCyan, SpaceColors.NeonPurple)
            ),
            style = Stroke(width = w * 0.045f, join = StrokeJoin.Round)
        )

        // Inner dark cutout prism
        val innerCut = Path().apply {
            moveTo(w * 0.50f, h * 0.30f)
            lineTo(w * 0.60f, h * 0.53f)
            lineTo(w * 0.40f, h * 0.53f)
            close()
        }
        drawPath(path = innerCut, color = SpaceColors.DeepSpace)
        drawPath(
            path = innerCut,
            color = SpaceColors.NeonCyan.copy(alpha = 0.75f),
            style = Stroke(width = w * 0.025f, join = StrokeJoin.Round)
        )

        // Crossbar cyan energy bar
        drawLine(
            brush = Brush.horizontalGradient(listOf(SpaceColors.NeonCyan, SpaceColors.White, SpaceColors.NeonPurple)),
            start = Offset(w * 0.28f, h * 0.66f),
            end = Offset(w * 0.72f, h * 0.66f),
            strokeWidth = h * 0.055f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * أيقونة ذرة Quantum Engine ثلاثية الأبعاد
 */
@Composable
fun SpaceQuantumAtomIcon(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    tint: Color = SpaceColors.NeonCyan
) {
    val prefs = LocalSpaceUiPreferences.current
    val infinite = rememberInfiniteTransition(label = "atom_spin")
    val angle by if (!prefs.reducedMotion) {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
            label = "atom_angle"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier.size(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val rx = this.size.width * 0.44f
        val ry = this.size.height * 0.17f

        drawCircle(
            brush = Brush.radialGradient(listOf(tint.copy(alpha = 0.45f), Color.Transparent)),
            radius = rx * 1.15f,
            center = c
        )
        listOf(0f, 60f, -60f).forEach { baseAngle ->
            rotate(degrees = baseAngle + angle * 0.25f, pivot = c) {
                drawOval(
                    color = tint.copy(alpha = 0.88f),
                    topLeft = Offset(c.x - rx, c.y - ry),
                    size = Size(rx * 2f, ry * 2f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }
        drawCircle(
            color = SpaceColors.White,
            radius = this.size.minDimension * 0.12f,
            center = c
        )
    }
}

/**
 * الروبوت الفضائي ثلاثي الأبعاد مع الكرة الأرضية الهولوغرافية والسفينة الفضائية في خلفية بانر الرئيسية
 */
@Composable
fun SpaceHeroBannerIllustration(
    modifier: Modifier = Modifier
) {
    val prefs = LocalSpaceUiPreferences.current
    val infinite = rememberInfiniteTransition(label = "hero_robot")
    val orbitSpin by if (!prefs.reducedMotion) {
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
            label = "globe_orbit"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(24f) }
    }
    val floatOffset by if (!prefs.reducedMotion) {
        infinite.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "robot_float"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Planetary curved horizon at bottom
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SpaceColors.ElectricBlue.copy(alpha = 0.36f),
                    SpaceColors.MidnightBlue.copy(alpha = 0.85f),
                    SpaceColors.DeepSpace
                ),
                center = Offset(w * 0.5f, h * 2.18f),
                radius = w * 1.45f
            ),
            radius = w * 1.45f,
            center = Offset(w * 0.5f, h * 2.18f)
        )
        drawArc(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, SpaceColors.NeonCyan.copy(alpha = 0.72f), SpaceColors.NeonPurple.copy(alpha = 0.5f), Color.Transparent)
            ),
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(-w * 0.25f, h * 0.81f),
            size = Size(w * 1.5f, h * 0.9f),
            style = Stroke(width = 2.dp.toPx())
        )

        // Futuristic Spaceship top-right (in LTR) or top-end
        drawSpaceshipSilhouette(
            center = Offset(w * 0.82f, h * 0.20f + floatOffset * 0.4f),
            scale = w * 0.16f
        )

        // Space Robot positioned cleanly on left side so text sits unobstructed on right side
        val robotCx = w * 0.23f
        val robotCy = h * 0.52f + floatOffset.dp.toPx() * 0.6f
        val unit = minOf(w * 0.25f, h * 0.44f)

        drawSpaceRobotBody(center = Offset(robotCx, robotCy), unit = unit)

        // Holographic Glowing Earth Globe hovering above robot hand
        val globeCenter = Offset(w * 0.46f, h * 0.44f - floatOffset.dp.toPx() * 0.5f)
        drawHoloGlobe(center = globeCenter, radius = unit * 0.54f, orbitDeg = orbitSpin)
    }
}

private fun DrawScope.drawSpaceshipSilhouette(center: Offset, scale: Float) {
    // Engine trail
    drawLine(
        brush = Brush.horizontalGradient(listOf(Color.Transparent, SpaceColors.NeonCyan.copy(alpha = 0.65f))),
        start = Offset(center.x - scale * 1.3f, center.y + scale * 0.18f),
        end = Offset(center.x - scale * 0.35f, center.y + scale * 0.08f),
        strokeWidth = 2.dp.toPx()
    )
    val hull = Path().apply {
        moveTo(center.x - scale * 0.85f, center.y + scale * 0.10f)
        quadraticTo(center.x, center.y - scale * 0.42f, center.x + scale * 0.95f, center.y - scale * 0.05f)
        quadraticTo(center.x + scale * 0.15f, center.y + scale * 0.30f, center.x - scale * 0.85f, center.y + scale * 0.10f)
        close()
    }
    drawPath(
        path = hull,
        brush = Brush.linearGradient(listOf(Color(0xFF1E3A5F), Color(0xFF0A162E), SpaceColors.ElectricBlue.copy(alpha = 0.65f)))
    )
    drawPath(
        path = hull,
        color = SpaceColors.NeonCyan.copy(alpha = 0.55f),
        style = Stroke(width = 1.dp.toPx())
    )
    // Cyan cockpit glow windows
    for (i in 0..3) {
        drawCircle(
            color = SpaceColors.NeonCyan.copy(alpha = 0.85f),
            radius = scale * 0.04f,
            center = Offset(center.x - scale * 0.15f + i * scale * 0.18f, center.y - scale * 0.04f)
        )
    }
}

private fun DrawScope.drawSpaceRobotBody(center: Offset, unit: Float) {
    // Aura glow behind robot
    drawCircle(
        brush = Brush.radialGradient(
            listOf(SpaceColors.ElectricBlue.copy(alpha = 0.34f), SpaceColors.NeonCyan.copy(alpha = 0.14f), Color.Transparent)
        ),
        radius = unit * 1.55f,
        center = Offset(center.x, center.y - unit * 0.12f)
    )

    // Robotic torso / armored chest
    val torsoTop = center.y + unit * 0.16f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF284366), Color(0xFF0B1830))),
        topLeft = Offset(center.x - unit * 0.54f, torsoTop),
        size = Size(unit * 1.08f, unit * 0.95f),
        cornerRadius = CornerRadius(unit * 0.28f, unit * 0.28f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(listOf(SpaceColors.NeonCyan.copy(alpha = 0.65f), SpaceColors.NeonPurple.copy(alpha = 0.45f))),
        topLeft = Offset(center.x - unit * 0.54f, torsoTop),
        size = Size(unit * 1.08f, unit * 0.95f),
        cornerRadius = CornerRadius(unit * 0.28f, unit * 0.28f),
        style = Stroke(width = 1.4.dp.toPx())
    )

    // Chest glowing A emblem
    val chestA = Path().apply {
        val cy = torsoTop + unit * 0.44f
        moveTo(center.x, cy - unit * 0.18f)
        lineTo(center.x + unit * 0.18f, cy + unit * 0.16f)
        lineTo(center.x + unit * 0.08f, cy + unit * 0.16f)
        lineTo(center.x, cy + unit * 0.01f)
        lineTo(center.x - unit * 0.08f, cy + unit * 0.16f)
        lineTo(center.x - unit * 0.18f, cy + unit * 0.16f)
        close()
    }
    drawPath(chestA, brush = Brush.linearGradient(listOf(SpaceColors.NeonCyan, SpaceColors.NeonPurple)))

    // Left shoulder & raised holographic palm arm
    drawCircle(
        color = Color(0xFF1E3252),
        radius = unit * 0.22f,
        center = Offset(center.x + unit * 0.62f, torsoTop + unit * 0.22f)
    )
    drawLine(
        brush = Brush.linearGradient(listOf(Color(0xFF264066), SpaceColors.NeonCyan.copy(alpha = 0.8f))),
        start = Offset(center.x + unit * 0.65f, torsoTop + unit * 0.24f),
        end = Offset(center.x + unit * 1.18f, torsoTop + unit * 0.04f),
        strokeWidth = unit * 0.19f,
        cap = StrokeCap.Round
    )
    // Open palm glow
    drawOval(
        brush = Brush.radialGradient(listOf(SpaceColors.NeonCyan.copy(alpha = 0.7f), Color.Transparent)),
        topLeft = Offset(center.x + unit * 1.02f, torsoTop - unit * 0.08f),
        size = Size(unit * 0.42f, unit * 0.22f)
    )

    // Helmet head (white/silver metallic dome + dark glossy visor)
    val headCy = center.y - unit * 0.40f
    // Headphones left & right
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(SpaceColors.NeonCyan, SpaceColors.ElectricBlue)),
        topLeft = Offset(center.x - unit * 0.62f, headCy - unit * 0.18f),
        size = Size(unit * 0.16f, unit * 0.40f),
        cornerRadius = CornerRadius(unit * 0.08f, unit * 0.08f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(SpaceColors.NeonCyan, SpaceColors.ElectricBlue)),
        topLeft = Offset(center.x + unit * 0.46f, headCy - unit * 0.18f),
        size = Size(unit * 0.16f, unit * 0.40f),
        cornerRadius = CornerRadius(unit * 0.08f, unit * 0.08f)
    )

    // Head outer shell
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFE6F0FF), Color(0xFF9DB4D4), Color(0xFF39557D))),
        topLeft = Offset(center.x - unit * 0.50f, headCy - unit * 0.44f),
        size = Size(unit * 1.00f, unit * 0.84f),
        cornerRadius = CornerRadius(unit * 0.36f, unit * 0.36f)
    )
    // Visor screen
    drawRoundRect(
        color = Color(0xFF040B1A),
        topLeft = Offset(center.x - unit * 0.38f, headCy - unit * 0.22f),
        size = Size(unit * 0.76f, unit * 0.46f),
        cornerRadius = CornerRadius(unit * 0.20f, unit * 0.20f)
    )
    drawRoundRect(
        color = SpaceColors.NeonCyan.copy(alpha = 0.7f),
        topLeft = Offset(center.x - unit * 0.38f, headCy - unit * 0.22f),
        size = Size(unit * 0.76f, unit * 0.46f),
        cornerRadius = CornerRadius(unit * 0.20f, unit * 0.20f),
        style = Stroke(width = 1.2.dp.toPx())
    )
    // Glowing cyan robot eyes
    drawOval(
        color = SpaceColors.NeonCyan,
        topLeft = Offset(center.x - unit * 0.24f, headCy - unit * 0.08f),
        size = Size(unit * 0.18f, unit * 0.13f)
    )
    drawOval(
        color = SpaceColors.NeonCyan,
        topLeft = Offset(center.x + unit * 0.06f, headCy - unit * 0.08f),
        size = Size(unit * 0.18f, unit * 0.13f)
    )
    // Headband antenna arc
    drawArc(
        color = SpaceColors.NeonCyan,
        startAngle = 195f,
        sweepAngle = 150f,
        useCenter = false,
        topLeft = Offset(center.x - unit * 0.54f, headCy - unit * 0.54f),
        size = Size(unit * 1.08f, unit * 0.68f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawHoloGlobe(center: Offset, radius: Float, orbitDeg: Float) {
    // Globe radial aura
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                SpaceColors.NeonCyan.copy(alpha = 0.55f),
                SpaceColors.ElectricBlue.copy(alpha = 0.25f),
                Color.Transparent
            )
        ),
        radius = radius * 1.8f,
        center = center
    )

    // Hologram core sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF67E8F9), SpaceColors.ElectricBlue, Color(0xFF0A245E)),
            center = Offset(center.x - radius * 0.25f, center.y - radius * 0.25f),
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )

    // Latitude & Longitude hologram grid lines
    drawCircle(
        color = SpaceColors.NeonCyan.copy(alpha = 0.85f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )
    drawOval(
        color = SpaceColors.White.copy(alpha = 0.45f),
        topLeft = Offset(center.x - radius * 0.48f, center.y - radius),
        size = Size(radius * 0.96f, radius * 2f),
        style = Stroke(width = 0.9.dp.toPx())
    )
    drawLine(
        color = SpaceColors.White.copy(alpha = 0.45f),
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = 0.9.dp.toPx()
    )

    // Orbiting cyan/purple hologram rings
    listOf(28f, -32f).forEachIndexed { idx, tilt ->
        rotate(degrees = tilt + orbitDeg * if (idx == 0) 0.15f else -0.15f, pivot = center) {
            drawOval(
                brush = Brush.horizontalGradient(
                    listOf(SpaceColors.NeonCyan.copy(alpha = 0.2f), SpaceColors.NeonCyan, SpaceColors.NeonPurple.copy(alpha = 0.5f))
                ),
                topLeft = Offset(center.x - radius * 1.55f, center.y - radius * 0.38f),
                size = Size(radius * 3.1f, radius * 0.76f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

/**
 * أيقونة روبوت AI الدائري العائم (للزر العائم وAI Wizard)
 */
@Composable
fun SpaceFloatingRobotAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    accentColor: Color = SpaceColors.NeonCyan,
    showOnlineDot: Boolean = true
) {
    Box(modifier = modifier.size(size)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .shadow(16.dp, CircleShape, spotColor = accentColor)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF103366), SpaceColors.MidnightBlue, SpaceColors.DeepSpace)
                    )
                )
                .border(1.8.dp, Brush.linearGradient(listOf(accentColor, SpaceColors.NeonPurple)), CircleShape)
        ) {
            val cx = this.size.width / 2f
            val cy = this.size.height * 0.54f
            val r = this.size.minDimension * 0.28f

            // Mini headphones
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(cx - r * 1.28f, cy - r * 0.45f),
                size = Size(r * 0.34f, r * 0.85f),
                cornerRadius = CornerRadius(r * 0.17f, r * 0.17f)
            )
            drawRoundRect(
                color = accentColor,
                topLeft = Offset(cx + r * 0.94f, cy - r * 0.45f),
                size = Size(r * 0.34f, r * 0.85f),
                cornerRadius = CornerRadius(r * 0.17f, r * 0.17f)
            )
            // Robot helmet
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFF0F7FF), Color(0xFF93B3DA))),
                topLeft = Offset(cx - r, cy - r * 0.92f),
                size = Size(r * 2f, r * 1.75f),
                cornerRadius = CornerRadius(r * 0.78f, r * 0.78f)
            )
            // Dark visor
            drawRoundRect(
                color = SpaceColors.DeepSpace,
                topLeft = Offset(cx - r * 0.76f, cy - r * 0.48f),
                size = Size(r * 1.52f, r * 0.92f),
                cornerRadius = CornerRadius(r * 0.42f, r * 0.42f)
            )
            // Glowing eyes
            drawOval(
                color = accentColor,
                topLeft = Offset(cx - r * 0.46f, cy - r * 0.18f),
                size = Size(r * 0.36f, r * 0.25f)
            )
            drawOval(
                color = accentColor,
                topLeft = Offset(cx + r * 0.10f, cy - r * 0.18f),
                size = Size(r * 0.36f, r * 0.25f)
            )
        }

        if (showOnlineDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(SpaceColors.Emerald)
                    .border(2.dp, SpaceColors.DeepSpace, CircleShape)
            )
        }
    }
}

/**
 * شارة 3D مضيئة للأيقونات الرئيسية (HTML / ZIP / APK / Gear)
 */
@Composable
fun Space3DFeatureBadge(
    icon: ImageVector,
    accentColor: Color,
    secondaryColor: Color = SpaceColors.ElectricBlue,
    badgeLabel: String? = null,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = modifier
            .size(size)
            .shadow(14.dp, shape, spotColor = accentColor.copy(alpha = 0.65f))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.30f),
                        secondaryColor.copy(alpha = 0.18f),
                        SpaceColors.MidnightBlue
                    )
                )
            )
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(listOf(accentColor.copy(alpha = 0.85f), secondaryColor.copy(alpha = 0.45f))),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (badgeLabel != null) {
            Text(
                text = badgeLabel,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = SpaceColors.White
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(size * 0.56f)
            )
        }
    }
}
