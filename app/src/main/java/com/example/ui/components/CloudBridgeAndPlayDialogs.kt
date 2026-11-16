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
        com.example.ui.components.space.NeonGlassCard(
            accentColor = SpaceColors.NeonCyan,
            secondaryAccent = SpaceColors.NeonPurple,
            shape = RoundedCornerShape(20.dp),
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
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "المصنع السحابي والجسر البرمجي (CI/CD + Bridge)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SpaceColors.White
                            )
                            Text(
                                text = "GitHub Actions • JS Bridge • App Links • Google Play Bundle",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SpaceColors.MutedText
                            )
                        }
                    }
                }

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

                com.example.ui.components.space.NeonButton(
                    text = "تصدير مشروع Android Studio + GitHub Actions (.zip)",
                    icon = Icons.Default.FolderZip,
                    style = com.example.ui.components.space.NeonButtonStyle.PRIMARY_GLOW,
                    height = 48.dp,
                    testTag = "export_studio_zip_btn",
                    onClick = onExportAndroidStudioZip,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenBridgeConfig,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceColors.GlassBorderCyan),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Cable, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعدادات Bridge", fontFamily = CairoFontFamily, fontSize = 11.sp, color = SpaceColors.White)
                    }

                    OutlinedButton(
                        onClick = onOpenPlayStorePrep,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceColors.Emerald.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Policy, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ملف Google Play", fontFamily = CairoFontFamily, fontSize = 11.sp, color = SpaceColors.White)
                    }

                    OutlinedButton(
                        onClick = onExportApkForgeJson,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceColors.NeonPurple.copy(alpha = 0.55f)),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, tint = SpaceColors.NeonPurple, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(".apkforge", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = SpaceColors.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusMiniTag(text: String, active: Boolean) {
    Surface(
        color = if (active) SpaceColors.Emerald.copy(alpha = 0.14f) else SpaceColors.SurfaceDeep,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) SpaceColors.Emerald.copy(alpha = 0.45f) else SpaceColors.GlassBorderSubtle)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = if (active) SpaceColors.Emerald else SpaceColors.SubtleText,
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

    com.example.ui.components.space.SpaceDialog(
        onDismissRequest = onDismiss,
        title = "إعدادات الجسر البرمجي والسحابة",
        subtitle = "ربط الموقع بميزات النظام والروابط العميقة وGitHub CI",
        icon = Icons.Default.Hub,
        accentColor = SpaceColors.NeonCyan,
        secondaryAccent = SpaceColors.NeonPurple,
        widthFraction = 0.93f
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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

                com.example.ui.components.space.NeonTextField(
                    value = deepLinkDomain,
                    onValueChange = { deepLinkDomain = it },
                    label = "نطاق الروابط العميقة (App Links Host)",
                    placeholder = "app.yourdomain.com",
                    accentColor = SpaceColors.NeonCyan
                )

                com.example.ui.components.space.NeonTextField(
                    value = adMobAppId,
                    onValueChange = { adMobAppId = it },
                    label = "معرّف Google AdMob App ID (اختياري)",
                    placeholder = "ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy",
                    accentColor = SpaceColors.NeonPurple
                )

                com.example.ui.components.space.NeonTextField(
                    value = remoteConfigUrl,
                    onValueChange = { remoteConfigUrl = it },
                    label = "رابط Remote Config / التحديث عن بُعد (اختياري)",
                    placeholder = "https://yourdomain.com/app-config.json",
                    accentColor = SpaceColors.Gold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.example.ui.components.space.NeonButton(
                        text = "إلغاء",
                        style = com.example.ui.components.space.NeonButtonStyle.CYBER_OUTLINE,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    com.example.ui.components.space.NeonButton(
                        text = "حفظ وتطبيق ✓",
                        style = com.example.ui.components.space.NeonButtonStyle.PRIMARY_GLOW,
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
                        modifier = Modifier.weight(1.4f)
                    )
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

    com.example.ui.components.space.SpaceDialog(
        onDismissRequest = onDismiss,
        title = "حزمة تجهيز متجر Google Play",
        subtitle = "Data Safety Checklist وسياسة الخصوصية",
        icon = Icons.Default.VerifiedUser,
        accentColor = SpaceColors.Emerald,
        secondaryAccent = SpaceColors.NeonCyan,
        widthFraction = 0.93f
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
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
                        color = SpaceColors.NeonCyan
                    )

                    safetyRows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpaceColors.SurfaceDeep)
                                .border(0.8.dp, SpaceColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (row.isCollected) Icons.Default.Info else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (row.isCollected) SpaceColors.Gold else SpaceColors.Emerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${row.categoryAr} — ${row.statusAr}",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = SpaceColors.White
                                )
                                Text(
                                    text = row.noteAr,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 10.sp,
                                    color = SpaceColors.MutedText
                                )
                            }
                        }
                    }

                    Text(
                        text = "سياسة الخصوصية المولّدة تلقائياً (Markdown)",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SpaceColors.NeonCyan
                    )

                    Surface(
                        color = SpaceColors.TerminalBg,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpaceColors.GlassBorderCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = policyText,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = SpaceColors.White,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                com.example.ui.components.space.NeonButton(
                    text = "نسخ سياسة الخصوصية للمتجر",
                    icon = Icons.Default.ContentCopy,
                    style = com.example.ui.components.space.NeonButtonStyle.EMERALD_SUCCESS,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Privacy Policy", policyText))
                        Toast.makeText(context, "تم نسخ سياسة الخصوصية لمتجر Google Play ✓", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun BuildHistoryCard(history: List<BuildHistoryEntry>) {
    if (history.isEmpty()) return
    val formatter = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        com.example.ui.components.space.NeonGlassCard(
            accentColor = SpaceColors.ElectricBlue,
            secondaryAccent = SpaceColors.NeonCyan,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.History, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "سجل الإصدارات السابقة (${history.size})",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SpaceColors.White
                    )
                }

                history.take(5).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpaceColors.SurfaceDeep)
                            .border(0.8.dp, SpaceColors.GlassBorderSubtle, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.fileName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SpaceColors.White
                            )
                            Text(
                                text = "v${entry.versionName} (${entry.versionCode}) • ${entry.targetType} • ${entry.sizeText}",
                                fontFamily = TajawalFontFamily,
                                fontSize = 10.sp,
                                color = SpaceColors.MutedText
                            )
                        }
                        Text(
                            text = formatter.format(Date(entry.timestamp)),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SpaceColors.SubtleText
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

    com.example.ui.components.space.SpaceDialog(
        onDismissRequest = onDismiss,
        title = "استيراد مشروع (.apkforge / JSON)",
        icon = Icons.Default.FileOpen,
        accentColor = SpaceColors.NeonCyan,
        secondaryAccent = SpaceColors.NeonPurple
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.ui.components.space.NeonButton(
                    text = "اختيار ملف .apkforge من الجهاز",
                    icon = Icons.Default.FileOpen,
                    style = com.example.ui.components.space.NeonButtonStyle.CYBER_OUTLINE,
                    onClick = { fileLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                )

                com.example.ui.components.space.NeonTextField(
                    value = rawJson,
                    onValueChange = { rawJson = it },
                    label = "أو الصق محتوى JSON المشروع هنا",
                    singleLine = false,
                    minLines = 4,
                    maxLines = 6,
                    accentColor = SpaceColors.NeonCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.example.ui.components.space.NeonButton(
                        text = "إلغاء",
                        style = com.example.ui.components.space.NeonButtonStyle.CYBER_OUTLINE,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    com.example.ui.components.space.NeonButton(
                        text = "استيراد المشروع",
                        style = com.example.ui.components.space.NeonButtonStyle.PRIMARY_GLOW,
                        enabled = rawJson.isNotBlank(),
                        onClick = { onImportJson(rawJson) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}
