package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import com.example.utils.Constants

import androidx.compose.ui.window.DialogProperties

@Composable
fun TermuxDiagnosticDialog(
    isInstalled: Boolean,
    hasPermission: Boolean,
    onDismiss: () -> Unit,
    onTestConnection: () -> Unit
) {
    val context = LocalContext.current
    val setupScript = "pkg update && pkg install -y openjdk-17 gradle aapt2\ntermux-setup-storage"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassmorphismCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("termux_diagnostic_dialog"),
            backgroundColor = DarkSurfaceElevated,
            borderColor = AmberPrimary.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isInstalled) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isInstalled) SuccessGreen else WarningYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مركز تشخيص محرك Termux",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiagnosticStatusBadge(
                        title = "تطبيق Termux",
                        isSuccess = isInstalled,
                        successText = "مثبت",
                        failText = "غير مثبت",
                        modifier = Modifier.weight(1f)
                    )
                    DiagnosticStatusBadge(
                        title = "إذن RUN_COMMAND",
                        isSuccess = hasPermission,
                        successText = "مفعل",
                        failText = "مطلوب",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "خطوات تهيئة Termux على الهاتف:",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GoldAccent
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "1. قم بتثبيت Termux من متجر F-Droid أو GitHub لضمان التوافق.\n" +
                            "2. قم بتثبيت إضافة Termux:Tasker أو منح إذن RUN_COMMAND عبر:\n" +
                            "   الإعدادات > التطبيقات > الأذونات الخاصة.\n" +
                            "3. افتح Termux وشغل أوامر البيئة الضرورية (Gradle + JDK):",
                    fontFamily = TajawalFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Code Snippet Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(TerminalBackground)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = setupScript,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = TerminalGreen,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Termux Setup", setupScript)
                                clipboard.setPrimaryClip(clip)
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

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
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
                                Toast.makeText(context, "تطبيق Termux غير مثبت على هذا الجهاز", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "فتح Termux",
                            fontFamily = CairoFontFamily,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }

                    GlowingButton(
                        text = "فحص الاتصال",
                        onClick = onTestConnection,
                        modifier = Modifier.weight(1f),
                        testTag = "test_termux_connection_button"
                    )
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
                fontSize = 11.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSuccess) "✓ $successText" else "✗ $failText",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSuccess) SuccessGreen else ErrorRed
            )
        }
    }
}
