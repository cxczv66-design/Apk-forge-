package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.util.AppLanguage
import com.example.ui.util.LocalAppLanguage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class SphereNode(
    val x: Float,
    val y: Float,
    val z: Float,
    val isAccent: Boolean
)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val lang = LocalAppLanguage.current
    // Master cinematic timeline: 0f -> 1f matching kinetic touch-to-sphere-portal entrance
    val timeline = remember { Animatable(0f) }
    var isFadingOut by remember { mutableStateOf(false) }
    var hasFinished by remember { mutableStateOf(false) }

    val triggerFinish: () -> Unit = {
        if (!hasFinished) {
            hasFinished = true
            onSplashFinished()
        }
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (isFadingOut) 0f else 1f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "splashFadeOut"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "orbitMotion")
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitPhase"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    // Precompute deterministic 3D spherical dot lattice (latitude x longitude rings)
    val sphereNodes = remember {
        buildList {
            val latBands = 14
            val lonSteps = 26
            for (latIdx in 1 until latBands) {
                val theta = (latIdx.toFloat() / latBands) * PI.toFloat()
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)
                for (lonIdx in 0 until lonSteps) {
                    val phi = (lonIdx.toFloat() / lonSteps) * (2f * PI.toFloat())
                    val x = sinTheta * cos(phi)
                    val y = cosTheta
                    val z = sinTheta * sin(phi)
                    add(
                        SphereNode(
                            x = x,
                            y = y,
                            z = z,
                            isAccent = (latIdx + lonIdx) % 7 == 0
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        timeline.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2800, easing = LinearEasing)
        )
        isFadingOut = true
        delay(320)
        triggerFinish()
    }

    val progress = timeline.value

    // Phase weights derived cleanly from master progress (0f..1f)
    // 0.00..0.16: Kinetic typography ("CREATE" / "APK FORGE") flash
    // 0.14..0.44: Luminous white-outline pointing hand glides up from bottom-left & taps touch node at 0.28
    // 0.28..0.58: Concentric ripple rings burst out from touch point
    // 0.30..1.00: 3D dotted luminous sphere rotates to center and expands into dot portal tunnel
    // 0.32..1.00: Real ApkForge app icon image emerges & glides smoothly in its dedicated showcase slot

    val kineticTextAlpha = when {
        progress < 0.04f -> progress / 0.04f
        progress < 0.14f -> 1f
        progress < 0.20f -> 1f - ((progress - 0.14f) / 0.06f)
        else -> 0f
    }.coerceIn(0f, 1f)

    val kineticTextScale = when {
        progress < 0.14f -> 0.90f + 0.12f * (progress / 0.14f)
        else -> 1.02f - 0.55f * ((progress - 0.14f) / 0.06f).coerceIn(0f, 1f)
    }

    val handProgress = ((progress - 0.14f) / 0.16f).coerceIn(0f, 1f)
    val handExit = ((progress - 0.34f) / 0.14f).coerceIn(0f, 1f)
    val handAlpha = if (progress < 0.14f || progress > 0.48f) 0f
    else if (progress < 0.20f) (progress - 0.14f) / 0.06f
    else (1f - handExit).coerceIn(0f, 1f)

    val touchRipple = ((progress - 0.27f) / 0.30f).coerceIn(0f, 1f)
    val sphereAppear = ((progress - 0.29f) / 0.14f).coerceIn(0f, 1f)
    val tunnelZoom = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f)

    // Dedicated moving app image animation (appears right after touch spark & stays smoothly animated)
    val logoAppear = ((progress - 0.30f) / 0.18f).coerceIn(0f, 1f)
    val easedLogoAppear = FastOutSlowInEasing.transform(logoAppear)
    val logoExitScale = 1f + tunnelZoom * 0.12f

    // Purposeful orbital floating motion for the ApkForge app image inside its spotlight frame
    val logoDriftX = sin(orbitPhase) * 8f * easedLogoAppear
    val logoDriftY = (-14f * (1f - easedLogoAppear) + cos(orbitPhase * 1.2f) * 7f)
    val logoTiltY = sin(orbitPhase) * 6f * easedLogoAppear
    val logoPulseScale = 1f + 0.025f * sin(orbitPhase * 2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer { alpha = screenAlpha }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Full-screen tap-to-skip layer kept as a sibling so logo/title semantics stay accessible individually
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = triggerFinish
                )
        )

        // Layer 1: Video-inspired Monochrome/Cyber-Orange Touch + 3D Dotted Sphere Portal Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.5f)
            val touchPoint = Offset(w * 0.46f, h * 0.52f)

            // 1) Subtle radial darkness depth glow once sphere appears
            if (sphereAppear > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x28FF7A2F),
                            Color(0x14FFFFFF),
                            Color.Transparent
                        ),
                        center = center,
                        radius = w * (0.45f + tunnelZoom * 0.65f)
                    ),
                    radius = w * (0.45f + tunnelZoom * 0.65f),
                    center = center,
                    alpha = sphereAppear
                )
            }

            // 2) Concentric luminous touch ripple rings (00:03 -> 00:04)
            if (touchRipple > 0f && touchRipple < 1f) {
                val baseRadius = w * 0.04f
                for (ring in 0..2) {
                    val localR = (touchRipple - ring * 0.12f).coerceIn(0f, 1f)
                    if (localR > 0f) {
                        val radius = baseRadius + localR * w * (0.36f + ring * 0.11f)
                        val alpha = (1f - localR) * (0.78f - ring * 0.18f)
                        drawCircle(
                            color = if (ring == 1) OrangePrimary.copy(alpha = alpha) else Color.White.copy(alpha = alpha),
                            radius = radius,
                            center = touchPoint,
                            style = Stroke(width = (2.4f - ring * 0.5f).dp.toPx())
                        )
                    }
                }
                // Spark burst dots on the primary ripple ring
                val dotRingRadius = baseRadius + touchRipple * w * 0.34f
                val dotAlpha = (1f - touchRipple) * 0.9f
                val ringDots = 24
                for (i in 0 until ringDots) {
                    val angle = (i.toFloat() / ringDots) * (2f * PI.toFloat()) + touchRipple * 0.4f
                    val pos = Offset(
                        x = touchPoint.x + cos(angle) * dotRingRadius,
                        y = touchPoint.y + sin(angle) * dotRingRadius
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = dotAlpha),
                        radius = (2.2f * (1f - touchRipple * 0.5f)).dp.toPx(),
                        center = pos
                    )
                }
            }

            // 3) 3D Dotted Luminous Sphere -> Tunnel Portal (00:04 -> 00:09)
            if (sphereAppear > 0f) {
                val sphereCenterX = (w * 0.68f) + (center.x - w * 0.68f) * ((progress - 0.30f) / 0.36f).coerceIn(0f, 1f)
                val sphereCenterY = (h * 0.50f) + (center.y - h * 0.50f) * ((progress - 0.30f) / 0.36f).coerceIn(0f, 1f)
                val sphereRadius = w * (0.34f + sphereAppear * 0.12f + tunnelZoom * 0.52f)
                val rotY = (progress - 0.28f) * 3.8f
                val rotX = 0.28f - tunnelZoom * 0.28f
                val cosRotY = cos(rotY)
                val sinRotY = sin(rotY)
                val cosRotX = cos(rotX)
                val sinRotX = sin(rotX)

                for (node in sphereNodes) {
                    // Rotate around Y then tilt around X
                    val rx = node.x * cosRotY - node.z * sinRotY
                    val rz1 = node.x * sinRotY + node.z * cosRotY
                    val ry = node.y * cosRotX - rz1 * sinRotX
                    val rz = node.y * sinRotX + rz1 * cosRotX

                    // Perspective projection with tunnel warp near the end
                    val perspective = 2.4f / (2.4f - rz * (0.85f + tunnelZoom * 0.65f))
                    val px = sphereCenterX + rx * sphereRadius * perspective
                    val py = sphereCenterY + ry * sphereRadius * perspective

                    val depthNorm = ((rz + 1f) * 0.5f).coerceIn(0f, 1f)
                    val dotAlpha = (0.10f + depthNorm * 0.85f) * sphereAppear * (1f - tunnelZoom * 0.35f)
                    val dotSize = (1.1f + depthNorm * 2.5f + tunnelZoom * 1.1f).dp.toPx()

                    if (dotAlpha > 0.02f) {
                        drawCircle(
                            color = if (node.isAccent) {
                                OrangePrimary.copy(alpha = (dotAlpha * 0.95f).coerceIn(0f, 1f))
                            } else {
                                Color.White.copy(alpha = dotAlpha.coerceIn(0f, 1f))
                            },
                            radius = dotSize,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // 4) Stylized Luminous White-Outline Pointing Hand gliding in from bottom-left (00:02 -> 00:04)
            if (handAlpha > 0.01f) {
                val easedApproach = FastOutSlowInEasing.transform(handProgress)
                val tipX = (w * 0.14f) + (touchPoint.x - w * 0.14f) * easedApproach - handExit * w * 0.12f
                val tipY = (h * 0.82f) + (touchPoint.y - h * 0.82f) * easedApproach + handExit * h * 0.18f

                // Scale vector silhouette anchored cleanly at the fingertip
                val s = w * 0.34f
                val handPath = Path().apply {
                    // Pointing index finger -> curved knuckles -> wrist & forearm going bottom-left
                    moveTo(tipX, tipY)
                    cubicTo(
                        tipX - 0.08f * s, tipY + 0.02f * s,
                        tipX - 0.25f * s, tipY + 0.08f * s,
                        tipX - 0.42f * s, tipY + 0.17f * s
                    )
                    // Back of hand / forearm outer edge down left
                    cubicTo(
                        tipX - 0.52f * s, tipY + 0.36f * s,
                        tipX - 0.72f * s, tipY + 0.75f * s,
                        tipX - 0.92f * s, tipY + 1.15f * s
                    )
                    // Forearm bottom width
                    lineTo(tipX - 0.62f * s, tipY + 1.24f * s)
                    // Palm underside rising to curled fingers
                    cubicTo(
                        tipX - 0.45f * s, tipY + 0.84f * s,
                        tipX - 0.32f * s, tipY + 0.56f * s,
                        tipX - 0.18f * s, tipY + 0.42f * s
                    )
                    // Curled middle/ring fingers
                    cubicTo(
                        tipX - 0.08f * s, tipY + 0.38f * s,
                        tipX - 0.05f * s, tipY + 0.27f * s,
                        tipX - 0.17f * s, tipY + 0.25f * s
                    )
                    // Index finger underside back toward fingertip
                    cubicTo(
                        tipX - 0.08f * s, tipY + 0.17f * s,
                        tipX + 0.03f * s, tipY + 0.07f * s,
                        tipX, tipY
                    )
                }

                // Soft glowing outer stroke + razor crisp inner white silhouette
                drawPath(
                    path = handPath,
                    color = Color.White.copy(alpha = handAlpha * 0.28f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = handPath,
                    color = Color.White.copy(alpha = handAlpha * 0.94f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Fingertip luminous spark at moment of touch
                if (handProgress > 0.75f) {
                    val sparkAlpha = ((handProgress - 0.75f) / 0.25f) * handAlpha
                    drawCircle(
                        color = Color.White.copy(alpha = sparkAlpha),
                        radius = 5.dp.toPx(),
                        center = Offset(tipX, tipY)
                    )
                }
            }
        }

        // Layer 2: Opening Kinetic Stacked Typography ("APK / FORGE") matching frame 00:01
        if (kineticTextAlpha > 0.01f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = kineticTextAlpha
                    scaleX = kineticTextScale
                    scaleY = kineticTextScale
                }
            ) {
                Text(
                    text = "AP\nKF\nOR\nGE",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 44.sp,
                    lineHeight = 40.sp,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )
            }
        }

        // Layer 3: Dedicated Moving App Image Showcase ("مع صورة تطبيقنا تضهر في مكان معين تتحرك")
        if (easedLogoAppear > 0.01f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(bottom = 36.dp)
                    .graphicsLayer {
                        alpha = easedLogoAppear
                        translationX = logoDriftX.dp.toPx()
                        translationY = logoDriftY.dp.toPx()
                        rotationY = logoTiltY
                        scaleX = (0.72f + 0.28f * easedLogoAppear) * logoPulseScale * logoExitScale
                        scaleY = (0.72f + 0.28f * easedLogoAppear) * logoPulseScale * logoExitScale
                        cameraDistance = 14f * density
                    }
            ) {
                Box(
                    modifier = Modifier.size(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Rotating Outer Orbital Halo Ring around our dedicated app logo position
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationZ = ringRotation }
                    ) {
                        val r = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    OrangePrimary.copy(alpha = 0.85f),
                                    Color.White.copy(alpha = 0.90f),
                                    Color.Transparent,
                                    OrangePrimary.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            ),
                            radius = r - 4.dp.toPx(),
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Counter-rotating dashed precision inner ring
                    Canvas(
                        modifier = Modifier
                            .size(148.dp)
                            .graphicsLayer { rotationZ = -ringRotation * 0.7f }
                    ) {
                        val dots = 16
                        val rad = size.minDimension / 2f - 2.dp.toPx()
                        for (i in 0 until dots) {
                            val angle = (i.toFloat() / dots) * (2f * PI.toFloat())
                            drawCircle(
                                color = if (i % 4 == 0) OrangePrimary else Color.White.copy(alpha = 0.55f),
                                radius = if (i % 4 == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
                                center = Offset(
                                    x = center.x + cos(angle) * rad,
                                    y = center.y + sin(angle) * rad
                                )
                            )
                        }
                    }

                    // Real ApkForge Black App Icon Image moving smoothly inside its spotlight frame
                    Image(
                        painter = painterResource(id = R.drawable.apkforge_brand_logo_1794592633199),
                        contentDescription = "ApkForge App Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(126.dp)
                            .testTag("splash_logo")
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(30.dp),
                                ambientColor = OrangePrimary,
                                spotColor = OrangePrimary
                            )
                            .clip(RoundedCornerShape(30.dp))
                            .border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.65f),
                                        OrangePrimary.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.25f)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "ApkForge",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    letterSpacing = 1.2.sp,
                    color = Color.White,
                    modifier = Modifier.testTag("splash_title")
                )

                Spacer(modifier = Modifier.height(4.dp))

                val subtitleText = when (lang) {
                    AppLanguage.ENGLISH -> "Smart Android App Forge"
                    AppLanguage.FRENCH -> "Studio Android Intelligent"
                    AppLanguage.SPANISH -> "Fábrica Inteligente de Apps Android"
                    AppLanguage.TURKISH -> "Akıllı Android Uygulama Stüdyosu"
                    else -> "مصنع تطبيقات أندرويد الاحترافي"
                }
                Text(
                    text = subtitleText,
                    fontFamily = TajawalFontFamily,
                    fontSize = 13.sp,
                    color = Color(0xFFB8C0CC)
                )
            }
        }

        // Layer 4: Minimal Sleek Progress Track at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 42.dp)
                .graphicsLayer { alpha = easedLogoAppear }
                .testTag("splash_progress"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(156.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F2430))
            ) {
                Box(
                    modifier = Modifier
                        .width(156.dp * progress.coerceIn(0.08f, 1f))
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(OrangePrimary, Color.White)
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(OrangePrimary)
                )
                val statusText = when (lang) {
                    AppLanguage.ENGLISH -> "Preparing build studio..."
                    AppLanguage.FRENCH -> "Initialisation du studio..."
                    AppLanguage.SPANISH -> "Preparando el estudio..."
                    AppLanguage.TURKISH -> "Derleme stüdyosu hazırlanıyor..."
                    else -> "جار تهيئة بيئة البناء الذكية..."
                }
                Text(
                    text = statusText,
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = Color(0xFF9AA3B2)
                )
            }
        }
    }
}
