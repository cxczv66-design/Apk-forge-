package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.ui.theme.*
import com.example.ui.util.strings

/**
 * شريط أزرار البناء النهائي الثلاثية الواضحة (APK / AAB / APKS) مع أيقونات Material حقيقية
 * + زر فتح الأدوات الاحترافية (24 أداة) قبل البناء.
 */
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("مخرجات البناء النهائية (APK / AAB / APKS)", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
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
                    accent = OrangePrimary,
                    enabled = !isBuilding,
                    tag = "omni_build_apk_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onBuildApk
                )
                OmniTargetTile(
                    title = "AAB",
                    subtitle = "متجر Play",
                    icon = Icons.Filled.CloudUpload,
                    accent = StatusGreen,
                    enabled = !isBuilding,
                    tag = "omni_build_aab_btn",
                    modifier = Modifier.weight(1f),
                    onClick = onBuildAab
                )
                OmniTargetTile(
                    title = "APKS",
                    subtitle = "حزمة متعددة",
                    icon = Icons.Filled.Layers,
                    accent = CyanAccent,
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
                OutlinedButton(
                    onClick = onOpenLiveSandbox,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Smartphone, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.livePreviewSandbox, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyanAccent, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onEditAppStages,
                    modifier = Modifier.weight(1f).testTag("edit_app_stages_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("مراحل الإعداد", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary, maxLines = 1)
                }
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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = if (enabled) 0.65f else 0.2f)),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            }
            Text(title, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

/**
 * نافذة الترمينال الحية (Live Streaming Terminal Modal):
 * تفتح فور بدء البناء وتعرض تدفق سجلات [AAPT2], [D8], [ZipAlign], [apksig] بالوقت الفعلي.
 */
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
                .heightIn(min = 380.dp, max = 560.dp)
                .testTag("live_terminal_modal"),
            shape = RoundedCornerShape(22.dp),
            color = TerminalBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Terminal, contentDescription = null, tint = TerminalGreen)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Quantum Live Terminal • ${project.name}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(statusText, color = TerminalYellow, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إخفاء", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (engineState is QuantumBuildEngine.EngineStatus.Failure) ErrorRed else TerminalGreen,
                    trackColor = DarkSurfaceElevated
                )

                Spacer(Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    stages.forEach { st ->
                        val bg = when (st.status) {
                            BuildStageStatus.DONE -> StatusGreen.copy(alpha = 0.22f)
                            BuildStageStatus.RUNNING -> OrangePrimary.copy(alpha = 0.28f)
                            BuildStageStatus.ERROR -> ErrorRed.copy(alpha = 0.22f)
                            BuildStageStatus.PENDING -> DarkSurfaceElevated
                        }
                        val fg = when (st.status) {
                            BuildStageStatus.DONE -> StatusGreen
                            BuildStageStatus.RUNNING -> OrangePrimary
                            BuildStageStatus.ERROR -> ErrorRed
                            BuildStageStatus.PENDING -> TextMuted
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF05070B))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { line ->
                        val color = when {
                            line.contains("✓") || line.contains("🎉") -> TerminalGreen
                            line.contains("❌") -> ErrorRed
                            line.contains("[AAPT2]") || line.contains("[D8]") || line.contains("[ZipAlign]") || line.contains("[apksig]") || line.contains("[APKS]") -> TerminalYellow
                            else -> TerminalText
                        }
                        Text(line, color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

/**
 * واجهة إتمام البناء السفلية (Completion Bottom Sheet):
 * تعرض معاينة مصغرة للتطبيق مع أيقونته الحقيقية وخيارين: "تثبيت مباشر" و"حفظ في التنزيلات".
 */
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
        containerColor = DarkSurface,
        contentColor = TextPrimary,
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
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("اكتمل بناء وتوقيع الحزمة بنجاح!", color = StatusGreen, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                    Text(project.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        "${project.packageConfig.packageName} • v${project.packageConfig.versionName} • ${telemetry.estimatedSizeMb}",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("الملف الناتج: $fileName", color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(
                        "التوقيع التشفيري: v1=${telemetry.v1Verified} | v2=${telemetry.v2Verified} | v3=${telemetry.v3Verified} | v4=${telemetry.v4Verified}",
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveToDownloads,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("sheet_save_downloads_btn"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(s.saveToDownloads, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onInstallNow,
                    modifier = Modifier.weight(1f).height(52.dp).testTag("sheet_install_now_btn"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                ) {
                    Icon(Icons.Filled.InstallMobile, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(s.installNow, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}
