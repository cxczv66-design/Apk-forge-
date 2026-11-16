package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import com.example.engine.QuantumBuildEngine
import com.example.ui.components.space.NeonGlassCard
import com.example.ui.components.space.Space3DFeatureBadge
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun AdaptiveBuildPipelineBoard(
    project: AppProject,
    engineState: QuantumBuildEngine.EngineStatus,
    stages: List<BuildStageInfo>,
    telemetry: BuildTelemetry,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = engineState is QuantumBuildEngine.EngineStatus.Running
    val isSuccess = engineState is QuantumBuildEngine.EngineStatus.Success
    val isError = engineState is QuantumBuildEngine.EngineStatus.Failure

    val borderAccent = when {
        isError -> SpaceColors.ErrorRed
        isRunning -> SpaceColors.NeonCyan
        isSuccess -> SpaceColors.Emerald
        else -> SpaceColors.ElectricBlue
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .testTag("adaptive_build_pipeline_board")
        ) {
            val isExpandedLayout = maxWidth >= 560.dp

            NeonGlassCard(
                accentColor = borderAccent,
                secondaryAccent = SpaceColors.NeonPurple,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Space3DFeatureBadge(
                                icon = Icons.Default.PrecisionManufacturing,
                                accentColor = borderAccent,
                                secondaryColor = SpaceColors.NeonPurple,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "خط بناء Quantum الحي (On-Device)",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SpaceColors.White
                                )
                                Text(
                                    text = telemetry.activeTarget,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = SpaceColors.MutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        StatusLiveBadge(isRunning = isRunning, isSuccess = isSuccess, isError = isError)
                    }

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

                    AnimatedVisibility(visible = isSuccess && telemetry.lastOutputFileName != null) {
                        Surface(
                            color = SpaceColors.Emerald.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.2.dp, SpaceColors.Emerald.copy(alpha = 0.7f)),
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
                                            tint = SpaceColors.Emerald,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "اكتمل البناء والتوقيع بنجاح 100% ✓",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = SpaceColors.Emerald
                                            )
                                            Text(
                                                text = telemetry.lastOutputFileName.orEmpty(),
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = SpaceColors.White,
                                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = telemetry.estimatedSizeMb,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = SpaceColors.Emerald,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                                )
                                                Text(
                                                    text = "• ${String.format(Locale.US, "%.1fs", telemetry.elapsedSec)} • Download/",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = SpaceColors.Gold,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = SpaceColors.Emerald,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "تثبيت وحفظ 📲",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = SpaceColors.DeepSpace,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                        )
                                    }
                                }

                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        VerificationSchemeChip("v1", telemetry.v1Verified)
                                        VerificationSchemeChip("v2", telemetry.v2Verified)
                                        VerificationSchemeChip("v3", telemetry.v3Verified)
                                        VerificationSchemeChip("v4 .idsig", telemetry.v4Verified)
                                        VerificationSchemeChip("ZipAlign 4B", telemetry.alignmentVerified)
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
        animationSpec = infiniteRepeatable(tween(650), repeatMode = RepeatMode.Reverse),
        label = "dotAlpha"
    )

    val (bgColor, textColor, label) = when {
        isError -> Triple(SpaceColors.ErrorRed.copy(alpha = 0.16f), SpaceColors.ErrorRed, "يتطلب معالجة ⚠")
        isRunning -> Triple(SpaceColors.NeonCyan.copy(alpha = 0.18f), SpaceColors.NeonCyan, "جارٍ البناء حيّاً")
        isSuccess -> Triple(SpaceColors.Emerald.copy(alpha = 0.16f), SpaceColors.Emerald, "تم الحفظ على القرص ✓")
        else -> Triple(SpaceColors.SurfaceDeep, SpaceColors.MutedText, "في وضع الاستعداد")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
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
        "KeyVault" to Icons.Default.Lock,
        "AAPT2" to Icons.Default.Layers,
        "D8 DEX" to Icons.Default.Memory,
        "ZipAlign" to Icons.Default.ViewAgenda,
        "apksig" to Icons.Default.VerifiedUser,
        "Package" to Icons.Default.Inventory2
    )

    Surface(
        color = SpaceColors.SurfaceDeep,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SpaceColors.GlassBorderCyan),
        modifier = modifier
    ) {
        BoxWithConstraints {
            val useSingleRow = maxWidth >= 460.dp

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                color = SpaceColors.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = project.packageConfig.packageName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SpaceColors.MutedText,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${telemetry.processedFilesCount} ملف",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSuccess) SpaceColors.Emerald else SpaceColors.NeonCyan
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = telemetry.estimatedSizeMb,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SpaceColors.MutedText,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                }

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
                                            isCurrent -> SpaceColors.NeonCyan.copy(alpha = 0.22f)
                                            isBuilt -> SpaceColors.Emerald.copy(alpha = 0.16f)
                                            else -> SpaceColors.MidnightBlue
                                        },
                                        label = "layerBg"
                                    )
                                    val borderClr by animateColorAsState(
                                        targetValue = when {
                                            isCurrent -> SpaceColors.NeonCyan
                                            isBuilt -> SpaceColors.Emerald.copy(alpha = 0.7f)
                                            else -> SpaceColors.GlassBorderSubtle
                                        },
                                        label = "layerBorder"
                                    )
                                    val tintClr = when {
                                        isCurrent -> SpaceColors.NeonCyan
                                        isBuilt -> SpaceColors.Emerald
                                        else -> SpaceColors.MutedText
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

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpaceColors.MidnightBlue)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = SpaceColors.NeonCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = telemetry.currentArtifact,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = SpaceColors.White,
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
                                color = SpaceColors.Gold,
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
                animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
                label = "nodeScale"
            )

            val rowBg by animateColorAsState(
                targetValue = when {
                    isErr -> SpaceColors.ErrorRed.copy(alpha = 0.14f)
                    isRunning -> SpaceColors.NeonCyan.copy(alpha = 0.14f)
                    isDone -> SpaceColors.Emerald.copy(alpha = 0.11f)
                    else -> SpaceColors.SurfaceDeep
                },
                label = "stageBg"
            )
            val rowBorder by animateColorAsState(
                targetValue = when {
                    isErr -> SpaceColors.ErrorRed.copy(alpha = 0.7f)
                    isRunning -> SpaceColors.NeonCyan.copy(alpha = 0.75f)
                    isDone -> SpaceColors.Emerald.copy(alpha = 0.45f)
                    else -> SpaceColors.GlassBorderSubtle
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
                                    isErr -> SpaceColors.ErrorRed
                                    isRunning -> SpaceColors.NeonCyan
                                    isDone -> SpaceColors.Emerald
                                    else -> SpaceColors.SurfaceElevated
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isErr -> Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            isRunning -> CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SpaceColors.DeepSpace, strokeWidth = 2.dp)
                            isDone -> Icon(Icons.Default.Check, contentDescription = null, tint = SpaceColors.DeepSpace, modifier = Modifier.size(15.dp))
                            else -> Text("${stage.index}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SpaceColors.MutedText)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stage.titleAr,
                            fontFamily = CairoFontFamily,
                            fontWeight = if (isRunning || isDone) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (stage.status == BuildStageStatus.PENDING) SpaceColors.MutedText else SpaceColors.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stage.detailAr,
                            fontFamily = TajawalFontFamily,
                            fontSize = 10.sp,
                            color = SpaceColors.SubtleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = when {
                        isErr -> SpaceColors.ErrorRed.copy(alpha = 0.2f)
                        isRunning -> SpaceColors.NeonCyan.copy(alpha = 0.18f)
                        isDone -> SpaceColors.Emerald.copy(alpha = 0.14f)
                        else -> SpaceColors.MidnightBlue
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
                            isErr -> SpaceColors.ErrorRed
                            isRunning -> SpaceColors.NeonCyan
                            isDone -> SpaceColors.Emerald
                            else -> SpaceColors.MutedText
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationSchemeChip(label: String, verified: Boolean) {
    val bgColor = if (verified) SpaceColors.Emerald.copy(alpha = 0.16f) else SpaceColors.MidnightBlue
    val borderColor = if (verified) SpaceColors.Emerald.copy(alpha = 0.65f) else SpaceColors.GlassBorderSubtle
    val contentColor = if (verified) SpaceColors.Emerald else SpaceColors.SubtleText
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = if (verified) "$label ✓" else label,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}
