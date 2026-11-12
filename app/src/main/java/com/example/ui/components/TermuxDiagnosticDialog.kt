package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.engine.TermuxEngine
import com.example.engine.ToolchainManager
import com.example.ui.theme.*
import com.example.utils.Constants

@Composable
fun TermuxDiagnosticDialog(
    report: TermuxEngine.TermuxDiagnosticReport,
    toolchainManager: ToolchainManager? = null,
    onDismiss: () -> Unit,
    onTestConnection: () -> Unit
) {
    val context = LocalContext.current
    val setupScript = "pkg update && pkg install -y openjdk-21 gradle aapt2 apksigner\ntermux-setup-storage"
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
                                    text = if (report.isTermuxInstalled) {
                                        "Termux v${report.termuxVersion ?: "installed"} مكتشف على الجهاز"
                                    } else {
                                        "Termux غير مكتشف — تتوفر أدوات بناء مدمجة داخل التطبيق"
                                    },
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = if (report.isTermuxInstalled) SuccessGreen else WarningYellow
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
                            successText = "ممنوح ومفعّل",
                            failText = if (report.isTermuxInstalled) "يحتاج تفعيل" else "غير متاح",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticStatusBadge(
                            title = "خدمة RunCommandService",
                            isSuccess = report.isRunCommandServiceReachable,
                            successText = "متصلة وجاهزة",
                            failText = "غير متاحة",
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticStatusBadge(
                            title = "إضافات API / Tasker",
                            isSuccess = report.isTermuxApiInstalled || report.isTermuxTaskerInstalled,
                            successText = listOfNotNull(
                                if (report.isTermuxApiInstalled) "API" else null,
                                if (report.isTermuxTaskerInstalled) "Tasker" else null
                            ).joinToString("+"),
                            failText = "غير مثبتة (اختياري)",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Honest Live Connection Diagnostic Summary
                    Surface(
                        color = if (report.isFullyConnected) SuccessGreen.copy(alpha = 0.1f) else Color(0xFF141B29),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (report.isFullyConnected) SuccessGreen.copy(alpha = 0.4f) else GlassBorderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = report.diagnosticSummaryAr + if (checkCounter > 0) " (فحص #$checkCounter ✓)" else "",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    // Embedded In-App Toolchain Installer + Live Linear Terminal (Replaces passive-only Termux instructions!)
                    if (toolchainManager != null) {
                        InAppToolchainTerminalCard(
                            toolchainManager = toolchainManager,
                            compactMode = true
                        )
                    }

                    // Termux Manual Script Helper (Collapsible / Secondary Reference)
                    Text(
                        text = "تهيئة Termux الخارجي (اختياري):",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GoldAccent
                    )

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
                                text = setupScript,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TerminalGreen,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Termux Setup", setupScript))
                                    Toast.makeText(context, "تم نسخ الأوامر بنجاح!", Toast.LENGTH_SHORT).show()
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

                    // Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(Constants.TERMUX_PACKAGE_NAME)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                } else {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux/"))
                                        )
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "تطبيق Termux غير مثبت — استخدم الأدوات المدمجة أعلاه", Toast.LENGTH_SHORT).show()
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
                                text = if (report.isTermuxInstalled) "فتح Termux" else "تحميل Termux",
                                fontFamily = CairoFontFamily,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }

                        GlowingButton(
                            text = "فحص الاتصال الحقيقي",
                            icon = Icons.Default.Refresh,
                            onClick = {
                                checkCounter++
                                onTestConnection()
                                val statusMsg = if (report.isFullyConnected) {
                                    "✅ Termux متصل بالكامل (v${report.termuxVersion})"
                                } else if (report.isTermuxInstalled) {
                                    "⚠️ Termux مثبت (v${report.termuxVersion}) — تحقق من RUN_COMMAND"
                                } else {
                                    "ℹ️ Termux غير مثبت — جميع أدوات البناء الداخلية متاحة"
                                }
                                Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
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
                fontSize = 12.sp,
                color = if (isSuccess) SuccessGreen else ErrorRed,
                maxLines = 1
            )
        }
    }
}
