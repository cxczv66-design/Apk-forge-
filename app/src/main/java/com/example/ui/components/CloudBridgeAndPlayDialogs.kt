package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.domain.model.AppProject
import com.example.domain.model.BridgeAndCloudConfig
import com.example.domain.model.BuildHistoryEntry
import com.example.engine.DataSafetyRow
import com.example.engine.ProjectExporter
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudAndBridgeFactoryCard(
    project: AppProject,
    onExportAndroidStudioZip: () -> Unit,
    onOpenBridgeConfig: () -> Unit,
    onOpenPlayStorePrep: () -> Unit,
    onExportApkForgeJson: () -> Unit
) {
    val bridge = project.bridgeConfig
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            color = Color(0xFF131A26),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28354D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "المصنع السحابي والجسر البرمجي (CI/CD + Bridge)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "GitHub Actions • JS Bridge • App Links • Google Play Bundle",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Active Status Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusMiniTag(
                        text = if (bridge.jsBridgeEnabled) "JS Bridge ✓" else "Bridge Off",
                        active = bridge.jsBridgeEnabled
                    )
                    StatusMiniTag(
                        text = if (bridge.gitHubCiEnabled) "GitHub CI ✓" else "CI Off",
                        active = bridge.gitHubCiEnabled
                    )
                    StatusMiniTag(
                        text = if (bridge.deepLinkDomain.isNotBlank()) "Deep Link ✓" else "No Deep Link",
                        active = bridge.deepLinkDomain.isNotBlank()
                    )
                }

                // Primary Cloud ZIP Export CTA
                Button(
                    onClick = onExportAndroidStudioZip,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("export_studio_zip_btn")
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تصدير مشروع Android Studio + GitHub Actions (.zip)",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Secondary Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBridgeConfig,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cable, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعدادات Bridge", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = onOpenPlayStorePrep,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ملف Google Play", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = onExportApkForgeJson,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(".apkforge", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMiniTag(text: String, active: Boolean) {
    Surface(
        color = if (active) StatusGreen.copy(alpha = 0.14f) else Color(0xFF0E131D),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) StatusGreen.copy(alpha = 0.45f) else Color(0xFF263042))
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = if (active) StatusGreen else TextMuted,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun BridgeAndCloudConfigDialog(
    initialConfig: BridgeAndCloudConfig,
    onDismiss: () -> Unit,
    onSave: (BridgeAndCloudConfig) -> Unit
) {
    var jsBridge by remember { mutableStateOf(initialConfig.jsBridgeEnabled) }
    var extIntents by remember { mutableStateOf(initialConfig.handleExternalIntents) }
    var githubCi by remember { mutableStateOf(initialConfig.gitHubCiEnabled) }
    var deepLinkDomain by remember { mutableStateOf(initialConfig.deepLinkDomain) }
    var adMobAppId by remember { mutableStateOf(initialConfig.adMobAppId) }
    var remoteConfigUrl by remember { mutableStateOf(initialConfig.remoteConfigUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إعدادات الجسر البرمجي والسحابة",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "اربط موقعك بميزات النظام الأصلية والروابط العميقة وتجهيز GitHub Actions",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    BridgeToggleRow(
                        title = "تفعيل الجسر البرمجي (window.ApkForge)",
                        subtitle = "يدعم showToast و vibrate و share و getDeviceInfo من داخل HTML/JS",
                        checked = jsBridge,
                        onCheckedChange = { jsBridge = it }
                    )

                    BridgeToggleRow(
                        title = "التوجيه الذكي للروابط الخارجية",
                        subtitle = "فتح wa.me و tel: و mailto: وروابط الدفع بالتطبيقات الأصلية",
                        checked = extIntents,
                        onCheckedChange = { extIntents = it }
                    )

                    BridgeToggleRow(
                        title = "توليد GitHub Actions CI (.yml)",
                        subtitle = "يبني APK و AAB تلقائياً على خوادم GitHub عند رفع المشروع",
                        checked = githubCi,
                        onCheckedChange = { githubCi = it }
                    )

                    OutlinedTextField(
                        value = deepLinkDomain,
                        onValueChange = { deepLinkDomain = it },
                        label = { Text("نطاق الروابط العميقة (App Links Host)") },
                        placeholder = { Text("app.yourdomain.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = adMobAppId,
                        onValueChange = { adMobAppId = it },
                        label = { Text("معرّف Google AdMob App ID (اختياري)") },
                        placeholder = { Text("ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = remoteConfigUrl,
                        onValueChange = { remoteConfigUrl = it },
                        label = { Text("رابط Remote Config / التحديث عن بُعد (اختياري)") },
                        placeholder = { Text("https://yourdomain.com/app-config.json") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                onSave(
                                    BridgeAndCloudConfig(
                                        jsBridgeEnabled = jsBridge,
                                        handleExternalIntents = extIntents,
                                        deepLinkDomain = deepLinkDomain.trim(),
                                        adMobAppId = adMobAppId.trim(),
                                        remoteConfigUrl = remoteConfigUrl.trim(),
                                        gitHubCiEnabled = githubCi
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            modifier = Modifier.weight(1.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("حفظ وتطبيق ✓", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BridgeToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E131C))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
            Text(subtitle, fontFamily = TajawalFontFamily, fontSize = 10.sp, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun PlayStorePrepDialog(
    project: AppProject,
    exporter: ProjectExporter,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val policyText = remember(project) { exporter.generatePrivacyPolicy(project) }
    val safetyRows: List<DataSafetyRow> = remember(project) { exporter.generateDataSafetyChecklist(project) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.82f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = StatusGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حزمة تجهيز متجر Google Play",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "نموذج أمان البيانات (Data Safety Checklist)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OrangePrimary
                        )

                        safetyRows.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0E131D))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (row.isCollected) Icons.Default.Info else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (row.isCollected) OrangePrimary else StatusGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${row.categoryAr} — ${row.statusAr}",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = row.noteAr,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Text(
                            text = "سياسة الخصوصية المولّدة تلقائياً (Markdown)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = OrangePrimary
                        )

                        Surface(
                            color = TerminalBackground,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232C3E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = policyText,
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Privacy Policy", policyText))
                            Toast.makeText(context, "تم نسخ سياسة الخصوصية لمتجر Google Play ✓", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("نسخ سياسة الخصوصية للمتجر", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun BuildHistoryCard(history: List<BuildHistoryEntry>) {
    if (history.isEmpty()) return
    val formatter = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            color = Color(0xFF131924),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سجل الإصدارات السابقة (${history.size})",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                history.take(5).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0E131C))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.fileName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "v${entry.versionName} (${entry.versionCode}) • ${entry.targetType} • ${entry.sizeText}",
                                fontFamily = TajawalFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = formatter.format(Date(entry.timestamp)),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImportApkForgeDialog(
    onDismiss: () -> Unit,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    var rawJson by remember { mutableStateOf("") }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val content = reader.readText()
                    onImportJson(content)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "تعذر قراءة الملف المحدد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "استيراد مشروع (.apkforge / JSON)",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )

                    Button(
                        onClick = { fileLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار ملف .apkforge من الجهاز", fontFamily = CairoFontFamily, color = TextPrimary)
                    }

                    OutlinedTextField(
                        value = rawJson,
                        onValueChange = { rawJson = it },
                        label = { Text("أو الصق محتوى JSON المشروع هنا") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondary)
                        }
                        Button(
                            onClick = { onImportJson(rawJson) },
                            enabled = rawJson.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("استيراد المشروع", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
