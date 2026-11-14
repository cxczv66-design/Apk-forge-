package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.QuantumDiagnosticReport
import com.example.engine.ToolchainManager
import com.example.ui.theme.*

@Composable
fun QuantumEngineStatusDialog(
    report: QuantumDiagnosticReport,
    toolchainManager: ToolchainManager? = null,
    onDismiss: () -> Unit,
    onRefreshDiagnostic: () -> Unit
) {
    var checkCounter by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            GlassmorphismCard(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.88f)
                    .padding(4.dp)
                    .testTag("quantum_engine_status_dialog"),
                backgroundColor = DarkSurfaceElevated,
                borderColor = if (report.isFullyOperational) StatusGreen.copy(alpha = 0.5f) else AmberPrimary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (report.isFullyOperational) Icons.Default.Verified else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (report.isFullyOperational) StatusGreen else WarningYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "محرك Quantum المدمج (On-Device)",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${report.deviceAbi} • ${report.freeStorageMb}MB متاحة • صفر اعتماد خارجي",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = if (report.isFullyOperational) StatusGreen else WarningYellow
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = TextSecondary
                            )
                        }
                    }

                    // 2x2 Real On-Device Pipeline Matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuantumStatusBadge(
                            title = "AAPT2 + D8 + ZipAlign",
                            isSuccess = report.isAapt2AndD8PipelineReady,
                            successText = "مدمج ومفعّل ✓",
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

                    // Live Diagnostic Summary
                    Surface(
                        color = if (report.isFullyOperational) StatusGreen.copy(alpha = 0.1f) else Color(0xFF141B29),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (report.isFullyOperational) StatusGreen.copy(alpha = 0.45f) else GlassBorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = StatusGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = report.diagnosticSummaryAr + if (checkCounter > 0) " (فحص #$checkCounter)" else "",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Embedded Toolchain & Interactive Terminal
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
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text(
                                text = "إغلاق",
                                fontFamily = CairoFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        GlowingButton(
                            text = "فحص وتحديث الحالة",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                checkCounter++
                                onRefreshDiagnostic()
                                toolchainManager?.refreshInstalledToolsStatus()
                            },
                            modifier = Modifier.weight(1.3f),
                            testTag = "refresh_quantum_engine_button"
                        )
                    }
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSuccess) StatusGreen.copy(alpha = 0.12f)
                else ErrorRed.copy(alpha = 0.12f)
            )
            .border(
                1.dp,
                if (isSuccess) StatusGreen.copy(alpha = 0.4f)
                else ErrorRed.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSuccess) "✓ $successText" else "✗ $failText",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isSuccess) StatusGreen else ErrorRed,
                maxLines = 1
            )
        }
    }
}
