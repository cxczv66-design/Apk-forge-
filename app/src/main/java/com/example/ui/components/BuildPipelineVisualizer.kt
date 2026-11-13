package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppProject
import com.example.engine.BuildStageInfo
import com.example.engine.BuildStageStatus
import com.example.engine.BuildTelemetry
import com.example.engine.TermuxEngine
import com.example.ui.theme.*
import java.util.Locale

/**
 * لوحة البناء التفاعلية الحية المتكيفة مع حجم الشاشة:
 * تتيح للمستخدم مشاهدة مراحل تجميع التطبيق وتغليفه على القرص لحظة بلحظة بدون قص أو انقلاب للأرقام والوحدات.
 */
@Composable
fun AdaptiveBuildPipelineBoard(
    project: AppProject,
    engineState: TermuxEngine.EngineStatus,
    stages: List<BuildStageInfo>,
    telemetry: BuildTelemetry,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = engineState is TermuxEngine.EngineStatus.Running
    val isSuccess = engineState is TermuxEngine.EngineStatus.Success
    val isError = engineState is TermuxEngine.EngineStatus.Failure
    val accent = parseColorSafe(project.accentColorHex, OrangePrimary)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .testTag("adaptive_build_pipeline_board")
        ) {
            val isExpandedLayout = maxWidth >= 560.dp

            Surface(
                color = Color(0xFF121722),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = when {
                        isError -> ErrorRed.copy(alpha = 0.7f)
                        isRunning -> OrangePrimary.copy(alpha = 0.65f)
                        isSuccess -> StatusGreen.copy(alpha = 0.6f)
                        else -> Color(0xFF222C3D)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header + Live Status Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(accent.copy(alpha = 0.25f), OrangePrimary.copy(alpha = 0.15f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PrecisionManufacturing,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "خط البناء التفاعلي الحي",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = telemetry.activeTarget,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        StatusLiveBadge(isRunning = isRunning, isSuccess = isSuccess, isError = isError)
                    }

                    // Adaptive Layout: 2 columns on tablet/wide screens, stacked on phones
                    if (isExpandedLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            LiveAppAssemblyBlueprint(
                                project = project,
                                telemetry = telemetry,
                                isRunning = isRunning,
                                isSuccess = isSuccess,
                                modifier = Modifier.weight(1f)
                            )
                            LiveStagesTimeline(
                                stages = stages,
                                modifier = Modifier.weight(1.15f)
                            )
                        }
                    } else {
                        LiveAppAssemblyBlueprint(
                            project = project,
                            telemetry = telemetry,
                            isRunning = isRunning,
                            isSuccess = isSuccess,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LiveStagesTimeline(
                            stages = stages,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Celebratory Green Success Output Card with Direct Actions
                    AnimatedVisibility(visible = isSuccess && telemetry.lastOutputFileName != null) {
                        Surface(
                            color = StatusGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, StatusGreen.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDownloads() }
                                .testTag("build_success_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = StatusGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "اكتمل البناء والتوقيع بنجاح 100% ✓",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = StatusGreen
                                            )
                                            Text(
                                                text = telemetry.lastOutputFileName.orEmpty(),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = TextPrimary,
                                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = telemetry.estimatedSizeMb,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = StatusGreen,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                                )
                                                Text(
                                                    text = "• ${String.format(Locale.US, "%.1fs", telemetry.elapsedSec)} • محفوظ في Download/",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = GoldAccent,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = StatusGreen,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "تثبيت وحفظ 📲",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFF062114),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLiveBadge(isRunning: Boolean, isSuccess: Boolean, isError: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val (bgColor, textColor, label) = when {
        isError -> Triple(ErrorRed.copy(alpha = 0.16f), ErrorRed, "يتطلب معالجة ⚠")
        isRunning -> Triple(OrangePrimary.copy(alpha = 0.18f), OrangePrimary, "جارٍ البناء حيّاً")
        isSuccess -> Triple(StatusGreen.copy(alpha = 0.16f), StatusGreen, "تم الحفظ على القرص ✓")
        else -> Triple(Color(0xFF1D2432), TextSecondary, "في وضع الاستعداد")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) textColor.copy(alpha = dotAlpha) else textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = textColor
            )
        }
    }
}

@Composable
private fun LiveAppAssemblyBlueprint(
    project: AppProject,
    telemetry: BuildTelemetry,
    isRunning: Boolean,
    isSuccess: Boolean,
    modifier: Modifier = Modifier
) {
    val layerLabels = listOf(
        "Gradle" to Icons.Default.Settings,
        "Manifest" to Icons.Default.Security,
        "Assets" to Icons.Default.Language,
        "Icons/Res" to Icons.Default.Layers,
        "RSA Sign" to Icons.Default.VerifiedUser,
        "Package" to Icons.Default.Inventory2
    )

    Surface(
        color = Color(0xFF0C1018),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D2638)),
        modifier = modifier
    ) {
        BoxWithConstraints {
            val useSingleRow = maxWidth >= 460.dp

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live App Card Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        RealAppIconAvatar(
                            project = project,
                            size = 42.dp,
                            cornerRadius = 12.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = project.name.ifBlank { "تطبيق جديد" },
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = project.packageConfig.packageName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Telemetry Mini Metrics with clean LTR number/unit formatting
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${telemetry.processedFilesCount} ملف",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSuccess) StatusGreen else OrangePrimary
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = telemetry.estimatedSizeMb,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                }

                // Live Layer Assembly Visualizer (3x2 grid on phones so Manifest/Icons never clip!)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val rows = if (useSingleRow) listOf(layerLabels) else layerLabels.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEachIndexed { rIdx, rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val rowOffset = if (useSingleRow) 0 else rIdx * 3
                                rowItems.forEachIndexed { cIdx, (label, icon) ->
                                    val layerNumber = rowOffset + cIdx + 1
                                    val isBuilt = telemetry.assembledLayers >= layerNumber
                                    val isCurrent = isRunning && telemetry.assembledLayers == layerNumber

                                    val boxBg by animateColorAsState(
                                        targetValue = when {
                                            isCurrent -> OrangePrimary.copy(alpha = 0.22f)
                                            isBuilt -> StatusGreen.copy(alpha = 0.16f)
                                            else -> Color(0xFF151C28)
                                        },
                                        label = "layerBg"
                                    )
                                    val borderClr by animateColorAsState(
                                        targetValue = when {
                                            isCurrent -> OrangePrimary
                                            isBuilt -> StatusGreen.copy(alpha = 0.7f)
                                            else -> Color(0xFF242F42)
                                        },
                                        label = "layerBorder"
                                    )
                                    val tintClr = when {
                                        isCurrent -> OrangePrimary
                                        isBuilt -> StatusGreen
                                        else -> Color(0xFF607087)
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(boxBg)
                                            .border(1.dp, borderClr, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 4.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = tintClr,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            fontWeight = if (isBuilt) FontWeight.Bold else FontWeight.Normal,
                                            color = tintClr,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active File / Artifact Monitor Strip (Strictly LTR so paths & seconds stay aligned)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF141B27))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = telemetry.currentArtifact,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            style = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrLtr),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (telemetry.elapsedSec > 0f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", telemetry.elapsedSec)}s",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = GoldAccent,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveStagesTimeline(
    stages: List<BuildStageInfo>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stages.forEach { stage ->
            val isRunning = stage.status == BuildStageStatus.RUNNING
            val isDone = stage.status == BuildStageStatus.DONE
            val isErr = stage.status == BuildStageStatus.ERROR

            val pulseTransition = rememberInfiniteTransition(label = "stagePulse")
            val scale by pulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (isRunning) 1.12f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(550),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "nodeScale"
            )

            val rowBg by animateColorAsState(
                targetValue = when {
                    isErr -> Color(0xFF261215)
                    isRunning -> Color(0xFF1F1A18)
                    isDone -> Color(0xFF111D19)
                    else -> Color(0xFF0E131C)
                },
                label = "stageBg"
            )
            val rowBorder by animateColorAsState(
                targetValue = when {
                    isErr -> ErrorRed.copy(alpha = 0.7f)
                    isRunning -> OrangePrimary.copy(alpha = 0.7f)
                    isDone -> StatusGreen.copy(alpha = 0.45f)
                    else -> Color(0xFF1D2534)
                },
                label = "stageBorder"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(rowBg)
                    .border(1.dp, rowBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .scale(if (isRunning) scale else 1f)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isErr -> ErrorRed
                                    isRunning -> OrangePrimary
                                    isDone -> StatusGreen
                                    else -> Color(0xFF1E2636)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isErr -> Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            isRunning -> CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            isDone -> Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF062114),
                                modifier = Modifier.size(15.dp)
                            )
                            else -> Text(
                                text = "${stage.index}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stage.titleAr,
                            fontFamily = CairoFontFamily,
                            fontWeight = if (isRunning || isDone) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (stage.status == BuildStageStatus.PENDING) TextSecondary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stage.detailAr,
                            fontFamily = TajawalFontFamily,
                            fontSize = 10.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = when {
                        isErr -> ErrorRed.copy(alpha = 0.2f)
                        isRunning -> OrangePrimary.copy(alpha = 0.18f)
                        isDone -> StatusGreen.copy(alpha = 0.14f)
                        else -> Color(0xFF18202E)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isDone && stage.durationMs > 0) "${stage.durationMs}ms" else stage.techLabel,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                        color = when {
                            isErr -> ErrorRed
                            isRunning -> OrangePrimary
                            isDone -> StatusGreen
                            else -> TextMuted
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
