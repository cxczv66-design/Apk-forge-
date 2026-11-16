package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AppProject
import com.example.engine.BuildStageInfo
import com.example.engine.BuildStageStatus
import com.example.engine.BuildTelemetry
import com.example.engine.QuantumBuildEngine
import com.example.ui.components.space.NeonButton
import com.example.ui.components.space.NeonButtonStyle
import com.example.ui.components.space.NeonGlassCard
import com.example.ui.components.space.NeonProgressIndicator
import com.example.ui.theme.*
import com.example.ui.util.strings

@Composable
fun OmniBuildTargetSuiteCard(
    isBuilding: Boolean,
    onBuildApk: () -> Unit,
    onBuildAab: () -> Unit,
    onBuildApks: () -> Unit,
    onEditAppStages: () -> Unit,
    onOpenLiveSandbox: () -> Unit
) {
    val s = strings()
    NeonGlassCard(
        accentColor = SpaceColors.NeonCyan,
        secondaryAccent = SpaceColors.NeonPurple,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = SpaceColors.Gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "مخرجات البناء النهائية (APK / AAB / APKS)",
                        fontFamily = CairoFontFamily,
                        color = SpaceColors.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OmniTargetTile(
                    title = "APK",
                    subtitle = "تثبيت مباشر",
                    icon = Icons.Filled.Android,
                    accent = SpaceColors.Gold,
                    enabled = !isBuilding,
                    tag = "omni_build_apk_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onBuildApk
                )
                OmniTargetTile(
                    title = "AAB",
                    subtitle = "متجر Play",
                    icon = Icons.Filled.CloudUpload,
                    accent = SpaceColors.Emerald,
                    enabled = !isBuilding,
                    tag = "omni_build_aab_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onBuildAab
                )
                OmniTargetTile(
                    title = "APKS",
                    subtitle = "حزمة متعددة",
                    icon = Icons.Filled.Layers,
                    accent = SpaceColors.NeonCyan,
                    enabled = !isBuilding,
                    tag = "omni_build_apks_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onBuildApks
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonButton(
                    text = s.livePreviewSandbox,
                    icon = Icons.Filled.Smartphone,
                    style = NeonButtonStyle.CYBER_OUTLINE,
                    onClick = onOpenLiveSandbox,
                    height = 48.dp,
                    modifier = Modifier.weight(1f)
                )
                NeonButton(
                    text = "مراحل الإعداد",
                    icon = Icons.Filled.Tune,
                    style = NeonButtonStyle.PURPLE_AI,
                    onClick = onEditAppStages,
                    height = 48.dp,
                    testTag = "edit_app_stages_btn",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OmniTargetTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    enabled: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NeonGlassCard(
        accentColor = accent,
        secondaryAccent = SpaceColors.ElectricBlue,
        shape = RoundedCornerShape(16.dp),
        elevation = 8.dp,
        enabled = enabled,
        onClick = onClick,
        testTag = tag,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f))
                    .border(1.dp, accent.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(title, fontFamily = CairoFontFamily, color = SpaceColors.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, fontFamily = TajawalFontFamily, color = SpaceColors.MutedText, fontSize = 10.sp)
        }
    }
}

@Composable
fun LiveBuildTerminalDialog(
    project: AppProject,
    engineState: QuantumBuildEngine.EngineStatus,
    stages: List<BuildStageInfo>,
    logs: List<String>,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.lastIndex)
        }
    }

    val progress = when (engineState) {
        is QuantumBuildEngine.EngineStatus.Running -> engineState.progress
        is QuantumBuildEngine.EngineStatus.Success -> 1f
        else -> 0.1f
    }
    val statusText = when (engineState) {
        is QuantumBuildEngine.EngineStatus.Running -> engineState.currentTask
        is QuantumBuildEngine.EngineStatus.Success -> "اكتمل البناء والتوقيع بنجاح ✓"
        is QuantumBuildEngine.EngineStatus.Failure -> "تعذر إكمال البناء: ${engineState.error}"
        else -> "جارٍ التحضير..."
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(min = 380.dp, max = 580.dp)
                .shadow(22.dp, RoundedCornerShape(24.dp), spotColor = SpaceColors.NeonCyan)
                .testTag("live_terminal_modal"),
            shape = RoundedCornerShape(24.dp),
            color = SpaceColors.TerminalBg,
            border = BorderStroke(1.3.dp, SpaceColors.NeonBorderGradient)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Terminal, contentDescription = null, tint = SpaceColors.Emerald)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Quantum Live Terminal • ${project.name}",
                                fontFamily = CairoFontFamily,
                                color = SpaceColors.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(statusText, fontFamily = TajawalFontFamily, color = SpaceColors.Gold, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إخفاء", tint = SpaceColors.MutedText)
                    }
                }

                Spacer(Modifier.height(8.dp))

                NeonProgressIndicator(
                    progress = progress,
                    startColor = if (engineState is QuantumBuildEngine.EngineStatus.Failure) SpaceColors.ErrorRed else SpaceColors.NeonCyan,
                    endColor = if (engineState is QuantumBuildEngine.EngineStatus.Failure) SpaceColors.ErrorRed else SpaceColors.Emerald
                )

                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    stages.forEach { st ->
                        val bg = when (st.status) {
                            BuildStageStatus.DONE -> SpaceColors.Emerald.copy(alpha = 0.2f)
                            BuildStageStatus.RUNNING -> SpaceColors.NeonCyan.copy(alpha = 0.25f)
                            BuildStageStatus.ERROR -> SpaceColors.ErrorRed.copy(alpha = 0.22f)
                            BuildStageStatus.PENDING -> SpaceColors.SurfaceElevated
                        }
                        val fg = when (st.status) {
                            BuildStageStatus.DONE -> SpaceColors.Emerald
                            BuildStageStatus.RUNNING -> SpaceColors.NeonCyan
                            BuildStageStatus.ERROR -> SpaceColors.ErrorRed
                            BuildStageStatus.PENDING -> SpaceColors.SubtleText
                        }
                        Surface(color = bg, shape = RoundedCornerShape(6.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                st.techLabel,
                                color = fg,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SpaceColors.DeepSpace)
                        .border(1.dp, SpaceColors.GlassBorderSubtle, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { line ->
                        val color = when {
                            line.contains("✓") || line.contains("🎉") -> SpaceColors.Emerald
                            line.contains("❌") -> SpaceColors.ErrorRed
                            line.contains("[AAPT2]") || line.contains("[D8]") || line.contains("[ZipAlign]") || line.contains("[apksig]") || line.contains("[APKS]") -> SpaceColors.Gold
                            else -> Color(0xFF7DD3FC)
                        }
                        Text(line, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildCompleteBottomSheet(
    project: AppProject,
    telemetry: BuildTelemetry,
    artifactPath: String,
    onInstallNow: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = strings()
    val fileName = telemetry.lastOutputFileName ?: artifactPath.substringAfterLast('/')

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpaceColors.MidnightBlue,
        contentColor = SpaceColors.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("build_complete_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RealAppIconAvatar(project = project, size = 56.dp, cornerRadius = 16.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("اكتمل بناء وتوقيع الحزمة بنجاح!", fontFamily = CairoFontFamily, color = SpaceColors.Emerald, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                    Text(project.name, fontFamily = CairoFontFamily, color = SpaceColors.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        "${project.packageConfig.packageName} • v${project.packageConfig.versionName} • ${telemetry.estimatedSizeMb}",
                        color = SpaceColors.MutedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            NeonGlassCard(
                accentColor = SpaceColors.Emerald,
                secondaryAccent = SpaceColors.NeonCyan,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الملف الناتج: $fileName", color = SpaceColors.White, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(
                        "التوقيع التشفيري: v1=${telemetry.v1Verified} | v2=${telemetry.v2Verified} | v3=${telemetry.v3Verified} | v4=${telemetry.v4Verified}",
                        color = SpaceColors.NeonCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeonButton(
                    text = s.saveToDownloads,
                    icon = Icons.Filled.Download,
                    style = NeonButtonStyle.CYBER_OUTLINE,
                    onClick = onSaveToDownloads,
                    testTag = "sheet_save_downloads_btn",
                    modifier = Modifier.weight(1f)
                )

                NeonButton(
                    text = s.installNow,
                    icon = Icons.Filled.InstallMobile,
                    style = NeonButtonStyle.EMERALD_SUCCESS,
                    onClick = onInstallNow,
                    testTag = "sheet_install_now_btn",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
