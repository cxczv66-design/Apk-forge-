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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.space.NeonProgressIndicator
import com.example.ui.components.space.SpaceApkForgeLogoMark
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.util.AppLanguage
import com.example.ui.util.LocalAppLanguage
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class CosmicSphereNode(
    val x: Float,
    val y: Float,
    val z: Float,
    val isPurple: Boolean
)

/**
 * شاشة الترحيب السينمائية المستقبلية (Space Portal + 3D ApkForge Emblem)
 * تدوم 2.8 ثانية وتكشف الشعار والعنوان وشريط التقدم مبكراً لضمان استجابة فورية وسلسة.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val lang = LocalAppLanguage.current
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

    val infiniteTransition = rememberInfiniteTransition(label = "cosmicOrbit")
    val orbitPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitPhase"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )

    val sphereNodes = remember {
        buildList {
            val latBands = 12
            val lonSteps = 22
            for (latIdx in 1 until latBands) {
                val theta = (latIdx.toFloat() / latBands) * PI.toFloat()
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)
                for (lonIdx in 0 until lonSteps) {
                    val phi = (lonIdx.toFloat() / lonSteps) * (2f * PI.toFloat())
                    add(
                        CosmicSphereNode(
                            x = sinTheta * cos(phi),
                            y = cosTheta,
                            z = sinTheta * sin(phi),
                            isPurple = (latIdx + lonIdx) % 3 == 0
                        )
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        timeline.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2600, easing = LinearEasing)
        )
        isFadingOut = true
        delay(300)
        triggerFinish()
    }

    val progress = timeline.value
    // ظهور سريع ومتدرج للشعار والعنوان بحيث يكون مكتملاً قبل 900ms (ومتوافقاً مع الاختبارات عند 1600ms)
    val logoAppear = ((progress - 0.08f) / 0.24f).coerceIn(0f, 1f)
    val easedLogoAppear = FastOutSlowInEasing.transform(logoAppear)
    val titleAppear = ((progress - 0.14f) / 0.26f).coerceIn(0f, 1f)
    val easedTitleAppear = FastOutSlowInEasing.transform(titleAppear)
    val tunnelZoom = ((progress - 0.72f) / 0.28f).coerceIn(0f, 1f)

    val logoFloatY = cos(orbitPhase * 1.1f) * 6f
    val logoTiltY = sin(orbitPhase) * 6f * easedLogoAppear
    val logoPulseScale = 1f + 0.024f * sin(orbitPhase * 2f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .graphicsLayer { alpha = screenAlpha }
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // طبقة النقر لتجاوز الشاشة فوراً
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = triggerFinish
                )
        )

        // بوابة المجرة ثلاثية الأبعاد + الموجات الضوئية السيان والأرجوانية
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.5f, h * 0.45f)

            // توهج كوني مركزي عميق
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpaceColors.NeonCyan.copy(alpha = 0.20f + tunnelZoom * 0.14f),
                        SpaceColors.NeonPurple.copy(alpha = 0.16f),
                        SpaceColors.ElectricBlue.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * (0.58f + tunnelZoom * 0.50f)
                ),
                radius = w * (0.58f + tunnelZoom * 0.50f),
                center = center
            )

            // موجة بوابة مضيئة في البداية
            val burstProgress = (progress / 0.55f).coerceIn(0f, 1f)
            if (burstProgress > 0f && burstProgress < 1f) {
                for (ring in 0..2) {
                    val localR = (burstProgress - ring * 0.15f).coerceIn(0f, 1f)
                    if (localR > 0f) {
                        val alpha = (1f - localR) * (0.65f - ring * 0.16f)
                        drawCircle(
                            color = if (ring % 2 == 0) SpaceColors.NeonCyan.copy(alpha = alpha) else SpaceColors.NeonPurple.copy(alpha = alpha),
                            radius = w * (0.08f + localR * (0.38f + ring * 0.12f)),
                            center = center,
                            style = Stroke(width = (2.2f - ring * 0.4f).dp.toPx())
                        )
                    }
                }
            }

            // شبكة الكرة الكونية المضيئة 3D
            val sphereRadius = w * (0.34f + easedLogoAppear * 0.09f + tunnelZoom * 0.42f)
            val rotY = progress * 3.2f
            val rotX = 0.24f - tunnelZoom * 0.2f
            val cosY = cos(rotY)
            val sinY = sin(rotY)
            val cosX = cos(rotX)
            val sinX = sin(rotX)

            for (node in sphereNodes) {
                val rx = node.x * cosY - node.z * sinY
                val rz1 = node.x * sinY + node.z * cosY
                val ry = node.y * cosX - rz1 * sinX
                val rz = node.y * sinX + rz1 * cosX

                val perspective = 2.4f / (2.4f - rz * (0.85f + tunnelZoom * 0.5f))
                val px = center.x + rx * sphereRadius * perspective
                val py = center.y + ry * sphereRadius * perspective

                val depthNorm = ((rz + 1f) * 0.5f).coerceIn(0f, 1f)
                val dotAlpha = (0.12f + depthNorm * 0.72f) * (1f - tunnelZoom * 0.35f)
                val dotRadius = (1.1f + depthNorm * 2.1f).dp.toPx()

                if (dotAlpha > 0.03f) {
                    drawCircle(
                        color = if (node.isPurple) SpaceColors.NeonPurple.copy(alpha = dotAlpha) else SpaceColors.NeonCyan.copy(alpha = dotAlpha),
                        radius = dotRadius,
                        center = Offset(px, py)
                    )
                }
            }
        }

        // محور الشعار والاسم الثلاثي الأبعاد
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .statusBarsPadding()
                .padding(bottom = 44.dp)
                .graphicsLayer {
                    alpha = easedLogoAppear.coerceAtLeast(0.02f)
                    translationY = logoFloatY.dp.toPx()
                    rotationY = logoTiltY
                    scaleX = (0.76f + 0.24f * easedLogoAppear) * logoPulseScale
                    scaleY = (0.76f + 0.24f * easedLogoAppear) * logoPulseScale
                    cameraDistance = 14f * density
                }
        ) {
            Box(
                modifier = Modifier.size(164.dp),
                contentAlignment = Alignment.Center
            ) {
                // حلقة الطاقة الخارجية الدوارة
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
                                SpaceColors.NeonCyan.copy(alpha = 0.90f),
                                SpaceColors.White.copy(alpha = 0.90f),
                                Color.Transparent,
                                SpaceColors.NeonPurple.copy(alpha = 0.75f),
                                Color.Transparent
                            )
                        ),
                        radius = r - 4.dp.toPx(),
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // حلقة النقاط الفضائية الداخلية المعاكسة
                Canvas(
                    modifier = Modifier
                        .size(142.dp)
                        .graphicsLayer { rotationZ = -ringRotation * 0.75f }
                ) {
                    val dots = 18
                    val rad = size.minDimension / 2f - 2.dp.toPx()
                    for (i in 0 until dots) {
                        val angle = (i.toFloat() / dots) * (2f * PI.toFloat())
                        drawCircle(
                            color = when {
                                i % 3 == 0 -> SpaceColors.NeonCyan
                                i % 3 == 1 -> SpaceColors.NeonPurple
                                else -> SpaceColors.White.copy(alpha = 0.65f)
                            },
                            radius = if (i % 3 == 0) 2.2.dp.toPx() else 1.3.dp.toPx(),
                            center = Offset(
                                x = center.x + cos(angle) * rad,
                                y = center.y + sin(angle) * rad
                            )
                        )
                    }
                }

                // ميدالية الشعار الزجاجي ثلاثي الأبعاد
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .testTag("splash_logo")
                        .shadow(26.dp, RoundedCornerShape(30.dp), spotColor = SpaceColors.NeonCyan)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFF132C5C),
                                    SpaceColors.MidnightBlue,
                                    SpaceColors.DeepSpace
                                )
                            )
                        )
                        .border(
                            width = 1.6.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    SpaceColors.NeonCyan,
                                    SpaceColors.ElectricBlue,
                                    SpaceColors.NeonPurple
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SpaceApkForgeLogoMark(size = 80.dp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = easedTitleAppear.coerceAtLeast(0.02f) }
            ) {
                Text(
                    text = "ApkForge",
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(SpaceColors.White, SpaceColors.NeonCyan, SpaceColors.NeonPurple)
                        ),
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = 32.sp,
                        letterSpacing = 1.4.sp
                    ),
                    modifier = Modifier.testTag("splash_title")
                )

                Text(
                    text = "Build • Edit • Create",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = SpaceColors.NeonCyan,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                val subtitleText = when (lang) {
                    AppLanguage.ENGLISH -> "Futuristic Android App Studio"
                    AppLanguage.FRENCH -> "Studio Android Futuriste"
                    AppLanguage.SPANISH -> "Estudio Android Futurista"
                    AppLanguage.TURKISH -> "Fütüristik Android Uygulama Stüdyosu"
                    else -> "مصنع تطبيقات أندرويد الفضائي"
                }
                Text(
                    text = subtitleText,
                    fontFamily = TajawalFontFamily,
                    fontSize = 13.sp,
                    color = SpaceColors.MutedText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // شريط التقدم الفضائي المضيء في الأسفل
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 44.dp)
                .graphicsLayer { alpha = easedLogoAppear.coerceAtLeast(0.02f) }
                .testTag("splash_progress"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NeonProgressIndicator(
                progress = progress.coerceIn(0.10f, 1f),
                modifier = Modifier.width(196.dp),
                height = 5.dp,
                startColor = SpaceColors.NeonCyan,
                endColor = SpaceColors.NeonPurple
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(SpaceColors.Emerald)
                )
                val statusText = when (lang) {
                    AppLanguage.ENGLISH -> "Initializing Quantum Engine..."
                    AppLanguage.FRENCH -> "Initialisation du moteur Quantum..."
                    AppLanguage.SPANISH -> "Iniciando Quantum Engine..."
                    AppLanguage.TURKISH -> "Quantum Motoru başlatılıyor..."
                    else -> "جار تهيئة محرك Quantum الفضائي..."
                }
                Text(
                    text = statusText,
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = SpaceColors.MutedText
                )
            }
        }
    }
}
