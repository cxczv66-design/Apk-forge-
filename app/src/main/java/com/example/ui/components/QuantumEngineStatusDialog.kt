package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.BuildCapabilitiesInspector
import com.example.engine.LocalCapabilitySupport
import com.example.engine.QuantumDiagnosticReport
import com.example.engine.ToolchainManager
import com.example.ui.components.space.NeonButton
import com.example.ui.components.space.NeonButtonStyle
import com.example.ui.components.space.SpaceDialog
import com.example.ui.theme.*

@Composable
fun QuantumEngineStatusDialog(
    report: QuantumDiagnosticReport,
    toolchainManager: ToolchainManager? = null,
    onDismiss: () -> Unit,
    onRefreshDiagnostic: () -> Unit
) {
    val context = LocalContext.current
    var checkCounter by remember { mutableIntStateOf(0) }
    val statusAccent = if (report.isFullyOperational) SpaceColors.Emerald else SpaceColors.Gold
    val capsProfile = remember(checkCounter, report.checkTimestamp) {
        BuildCapabilitiesInspector.inspect(context, report.freeStorageMb)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SpaceDialog(
            onDismissRequest = onDismiss,
            title = "قدرات محرك Quantum المحلي والسحابي",
            subtitle = "${report.deviceAbi} • ${report.freeStorageMb}MB متاحة • شفافية كاملة للقدرات",
            icon = if (report.isFullyOperational) Icons.Default.Verified else Icons.Default.Shield,
            accentColor = statusAccent,
            secondaryAccent = SpaceColors.NeonCyan,
            widthFraction = 0.95f,
            testTag = "quantum_engine_status_dialog"
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuantumStatusBadge(
                        title = if (capsProfile.hasExecutableNativeAapt2 && capsProfile.hasExecutableNativeD8) "AAPT2 + D8 (ELF)" else "مُغلف AXML + DEX035",
                        isSuccess = report.isAapt2AndD8PipelineReady,
                        successText = if (capsProfile.hasExecutableNativeAapt2) "ثنائي أصلي جاهز ✓" else "تغليف محلي مفعّل ✓",
                        failText = "غير جاهز",
                        modifier = Modifier.weight(1f)
                    )
                    QuantumStatusBadge(
                        title = "Google apksig (v1-v4)",
                        isSuccess = report.isApkSigReady,
                        successText = "v1/v2/v3/v4 جاهز",
                        failText = "مفقود",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuantumStatusBadge(
                        title = "BouncyCastle + KeyVault",
                        isSuccess = report.isBouncyCastleLoaded,
                        successText = if (report.isAndroidKeyStoreAvailable) "Hardware AES-GCM" else "AES-256-GCM",
                        failText = "غير محمّل",
                        modifier = Modifier.weight(1f)
                    )
                    QuantumStatusBadge(
                        title = "WorkManager الخلفي",
                        isSuccess = report.isWorkManagerReady,
                        successText = "نشط ومستقر ✓",
                        failText = "متوقف",
                        modifier = Modifier.weight(1f)
                    )
                }

                Surface(
                    color = statusAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, statusAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = statusAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = report.diagnosticSummaryAr + if (checkCounter > 0) " (فحص #$checkCounter)" else "",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = SpaceColors.White,
                            lineHeight = 18.sp
                        )
                    }
                }

                // مصفوفة الشفافية: ماذا يعمل محلياً vs ما يحتاج Gradle + Android SDK سحابياً
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "حدود البناء المحلي vs السحابي (${capsProfile.nativeCount} حقيقي محلياً • ${capsProfile.cloudRequiredCount} سحابي):",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SpaceColors.NeonCyan
                    )
                    capsProfile.capabilities.forEach { cap ->
                        val capColor = when (cap.support) {
                            LocalCapabilitySupport.NATIVE_VERIFIED -> SpaceColors.Emerald
                            LocalCapabilitySupport.ON_DEVICE_PACKAGER -> SpaceColors.NeonCyan
                            LocalCapabilitySupport.REQUIRES_CLOUD_SDK -> SpaceColors.Gold
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpaceColors.SurfaceDeep.copy(alpha = 0.8f))
                                .border(0.8.dp, capColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cap.titleAr,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = SpaceColors.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cap.support.labelAr,
                                        fontFamily = TajawalFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = capColor
                                    )
                                }
                                Text(
                                    text = cap.detailAr,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 10.sp,
                                    color = SpaceColors.MutedText,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                if (toolchainManager != null) {
                    InAppToolchainTerminalCard(
                        toolchainManager = toolchainManager,
                        compactMode = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = "إغلاق",
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    NeonButton(
                        text = "فحص وتحديث الحالة",
                        icon = Icons.Default.Refresh,
                        style = NeonButtonStyle.PRIMARY_GLOW,
                        onClick = {
                            checkCounter++
                            onRefreshDiagnostic()
                            toolchainManager?.refreshInstalledToolsStatus()
                        },
                        modifier = Modifier.weight(1.35f),
                        testTag = "refresh_quantum_engine_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantumStatusBadge(
    title: String,
    isSuccess: Boolean,
    successText: String,
    failText: String,
    modifier: Modifier = Modifier
) {
    val accent = if (isSuccess) SpaceColors.Emerald else SpaceColors.ErrorRed
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.48f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = SpaceColors.MutedText,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSuccess) "✓ $successText" else "✗ $failText",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accent,
                maxLines = 1
            )
        }
    }
}
