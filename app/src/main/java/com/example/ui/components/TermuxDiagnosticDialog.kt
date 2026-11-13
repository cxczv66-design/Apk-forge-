package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.TermuxConnectionState
import com.example.engine.TermuxEngine
import com.example.engine.ToolchainManager
import com.example.ui.theme.*
import com.example.utils.Constants

@Composable
fun TermuxDiagnosticDialog(
    report: TermuxEngine.TermuxDiagnosticReport,
    toolchainManager: ToolchainManager? = null,
    termuxEngine: TermuxEngine? = null,
    onDismiss: () -> Unit,
    onTestConnection: () -> Unit
) {
    val context = LocalContext.current
    val fixExternalAppsCmd = remember {
        termuxEngine?.getEnableExternalAppsCommand()
            ?: "mkdir -p ~/.termux && touch ~/.termux/termux.properties && (grep -q '^allow-external-apps' ~/.termux/termux.properties || echo 'allow-external-apps=true' >> ~/.termux/termux.properties) && termux-reload-settings && pkg install -y openjdk-17 aapt2 dx apksigner zip wget"
    }
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
                    .testTag("termux_diagnostic_dialog"),
                backgroundColor = DarkSurfaceElevated,
                borderColor = AmberPrimary.copy(alpha = 0.5f),
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
                                imageVector = if (report.isFullyConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (report.isFullyConnected) SuccessGreen else WarningYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "مركز تشخيص محرك Termux",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when (report.connectionState) {
                                        TermuxConnectionState.VERIFIED_READY -> "Termux v${report.termuxVersion ?: "installed"} متصل ومُتحقق ✓"
                                        TermuxConnectionState.NEEDS_ALLOW_EXTERNAL_APPS -> "خطأ (2): يلزم تفعيل allow-external-apps=true"
                                        TermuxConnectionState.INSTALLED_UNVERIFIED -> "Termux مثبت (v${report.termuxVersion ?: "?"}) • بانتظار التحقق"
                                        TermuxConnectionState.NEEDS_RUN_COMMAND_PERMISSION -> "Termux مثبت • يلزم إذن RUN_COMMAND"
                                        TermuxConnectionState.NOT_INSTALLED -> "Termux غير مثبت — الأدوات الداخلية متاحة"
                                    },
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = if (report.isFullyConnected) SuccessGreen else WarningYellow
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

                    // Primary Status Indicators (2x2 real system matrix)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticStatusBadge(
                            title = "تطبيق Termux",
                            isSuccess = report.isTermuxInstalled,
                            successText = "مثبت (${report.termuxVersion ?: "v0.118"})",
                            failText = "غير مثبت",
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticStatusBadge(
                            title = "إذن RUN_COMMAND",
                            isSuccess = report.hasRunCommandPermission,
                            successText = "ممنوح",
                            failText = if (report.isTermuxInstalled) "يحتاج تفعيل" else "غير متاح",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticStatusBadge(
                            title = "allow-external-apps",
                            isSuccess = report.isFullyConnected,
                            successText = "مُتحقق ويعمل ✓",
                            failText = if (report.connectionState == TermuxConnectionState.NEEDS_ALLOW_EXTERNAL_APPS) {
                                "غير مفعّل (Error 2)"
                            } else "غير مُختبر",
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticStatusBadge(
                            title = "خدمة RunCommandService",
                            isSuccess = report.isRunCommandServiceReachable,
                            successText = "الخدمة متوفرة",
                            failText = "غير متاحة",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Honest Live Connection Diagnostic Summary
                    Surface(
                        color = when {
                            report.isFullyConnected -> SuccessGreen.copy(alpha = 0.1f)
                            report.connectionState == TermuxConnectionState.NEEDS_ALLOW_EXTERNAL_APPS -> ErrorRed.copy(alpha = 0.12f)
                            else -> Color(0xFF141B29)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                report.isFullyConnected -> SuccessGreen.copy(alpha = 0.45f)
                                report.connectionState == TermuxConnectionState.NEEDS_ALLOW_EXTERNAL_APPS -> ErrorRed.copy(alpha = 0.5f)
                                else -> GlassBorderSubtle
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = report.diagnosticSummaryAr + if (checkCounter > 0) " (فحص #$checkCounter)" else "",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // 1-Tap Fix Box for Termux Error (2) allow-external-apps=true
                    Text(
                        text = "أمر تفعيل allow-external-apps وتثبيت أدوات البناء في Termux (الصقه داخل Termux واضغط Enter):",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = GoldAccent
                    )

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TerminalBackground)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fixExternalAppsCmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TerminalGreen,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Termux Fix Command", fixExternalAppsCmd))
                                        Toast.makeText(context, "تم نسخ أمر التفعيل — الصقه في Termux واضغط Enter ✓", Toast.LENGTH_LONG).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ الأوامر",
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Embedded In-App Toolchain Installer + Live Linear Terminal
                    if (toolchainManager != null) {
                        InAppToolchainTerminalCard(
                            toolchainManager = toolchainManager,
                            compactMode = true
                        )
                    }

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Termux Setup", fixExternalAppsCmd))
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(Constants.TERMUX_PACKAGE_NAME)
                                if (launchIntent != null) {
                                    Toast.makeText(context, "تم نسخ الأمر — الصقه داخل Termux ✓", Toast.LENGTH_LONG).show()
                                    context.startActivity(launchIntent)
                                } else {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "تطبيق Termux غير مثبت", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (report.isTermuxInstalled) "نسخ وفتح Termux" else "تحميل Termux",
                                fontFamily = CairoFontFamily,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }

                        GlowingButton(
                            text = "اختبار التنفيذ الفعلي",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                checkCounter++
                                onTestConnection()
                                termuxEngine?.pingTermuxConnection()
                            },
                            modifier = Modifier.weight(1.2f),
                            testTag = "test_termux_connection_button"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticStatusBadge(
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
                if (isSuccess) SuccessGreen.copy(alpha = 0.12f)
                else ErrorRed.copy(alpha = 0.12f)
            )
            .border(
                1.dp,
                if (isSuccess) SuccessGreen.copy(alpha = 0.4f)
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
                color = if (isSuccess) SuccessGreen else ErrorRed,
                maxLines = 1
            )
        }
    }
}
