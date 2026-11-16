package com.example.ui.components.space

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.TajawalFontFamily

/**
 * شريط تقدم نيون متدرج متوهج
 */
@Composable
fun NeonProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
    startColor: Color = SpaceColors.NeonCyan,
    endColor: Color = SpaceColors.NeonPurple,
    trackColor: Color = SpaceColors.SurfaceDeep
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .border(0.8.dp, startColor.copy(alpha = 0.30f), RoundedCornerShape(50))
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clamped)
                    .shadow(8.dp, RoundedCornerShape(50), spotColor = startColor)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(startColor, endColor)))
            )
        }
    }
}

/**
 * بطاقة حالة Quantum Engine المدمجة أو الكاملة (تستخدم في Header الرئيسية ومركز البناء)
 */
@Composable
fun QuantumStatusCard(
    title: String = "Quantum Engine",
    subtitle: String? = null,
    isActive: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    testTag: String = "quantum_status_pill"
) {
    val dotColor = if (isActive) SpaceColors.Emerald else SpaceColors.Gold

    val infinite = rememberInfiniteTransition(label = "quantum_dot")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "quantum_pulse"
    )

    if (compact) {
        Box(
            modifier = modifier
                .testTag(testTag)
                .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = SpaceColors.NeonCyan.copy(alpha = 0.55f))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(SpaceColors.SurfaceElevated, SpaceColors.MidnightBlue)
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(SpaceColors.NeonCyan.copy(alpha = 0.85f), SpaceColors.ElectricBlue.copy(alpha = 0.45f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            // Online green pulse dot at top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = pulseAlpha))
                    .border(1.2.dp, SpaceColors.DeepSpace, CircleShape)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpaceQuantumAtomIcon(size = 24.dp, tint = SpaceColors.NeonCyan)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Quantum\nEngine",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    color = SpaceColors.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        NeonGlassCard(
            accentColor = SpaceColors.NeonCyan,
            secondaryAccent = dotColor,
            onClick = onClick,
            testTag = testTag,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SpaceQuantumAtomIcon(size = 34.dp, tint = SpaceColors.NeonCyan)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = title,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = SpaceColors.White
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor.copy(alpha = pulseAlpha))
                        )
                    }
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = SpaceColors.MutedText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * مؤشر تحميل فضائي متوهج بهالة نيون دوارة
 */
@Composable
fun FuturisticLoading(
    title: String,
    subtitle: String? = null,
    accentColor: Color = SpaceColors.NeonCyan,
    modifier: Modifier = Modifier
) {
    NeonGlassCard(
        accentColor = accentColor,
        secondaryAccent = SpaceColors.NeonPurple,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(58.dp),
                    color = accentColor,
                    trackColor = SpaceColors.SurfaceDeep,
                    strokeWidth = 3.dp
                )
                SpaceQuantumAtomIcon(size = 32.dp, tint = SpaceColors.NeonPurple)
            }
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = SpaceColors.White,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = SpaceColors.MutedText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * ترويسة قسم مميزة مع أيقونة نيون وزر "عرض الكل" اختياري
 */
@Composable
fun PremiumSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accentColor: Color = SpaceColors.NeonCyan,
    actionLabel: String? = null,
    actionTestTag: String = "",
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = SpaceColors.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!actionLabel.isNullOrBlank() && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .then(if (actionTestTag.isNotEmpty()) Modifier.testTag(actionTestTag) else Modifier)
            ) {
                Text(
                    text = actionLabel,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SpaceColors.NeonCyan
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = actionLabel,
                    tint = SpaceColors.NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
