package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.DownloadedApk
import com.example.ui.components.ApkAnalyzerDialog
import com.example.ui.components.QrShareApkDialog
import com.example.ui.components.space.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(
    downloads: List<DownloadedApk>,
    projects: List<com.example.domain.model.AppProject> = emptyList(),
    onBack: () -> Unit,
    onDeleteDownload: (String) -> Unit = {},
    onExportCiZipForDownload: (DownloadedApk) -> Unit = {}
) {
    val context = LocalContext.current
    val exporter = remember(context) { com.example.engine.ProjectExporter(context) }
    var selectedForAnalyzer by remember { mutableStateOf<DownloadedApk?>(null) }
    var selectedForQrShare by remember { mutableStateOf<DownloadedApk?>(null) }
    var savedBannerMessage by remember { mutableStateOf<String?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    val apkCount = downloads.count { it.buildType.equals("APK", ignoreCase = true) }
    val aabCount = downloads.count { it.buildType.equals("AAB", ignoreCase = true) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = "مركز الحزم والتنزيلات",
                subtitle = "تثبيت مباشر أو حفظ في تنزيلات الهاتف عبر MediaStore",
                onBack = onBack,
                accentColor = SpaceColors.NeonCyan
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .testTag("downloads_screen"),
            contentAlignment = Alignment.Center
        ) {
            if (downloads.isEmpty()) {
                NeonGlassCard(
                    accentColor = SpaceColors.NeonCyan,
                    secondaryAccent = SpaceColors.NeonPurple,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        Space3DFeatureBadge(
                            icon = Icons.Default.AllInbox,
                            accentColor = SpaceColors.NeonCyan,
                            secondaryColor = SpaceColors.NeonPurple,
                            size = 76.dp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "لم تبنِ أو تنزّل أي حزمة بعد",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = SpaceColors.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "تصل جميع التطبيقات المبنية هنا مع أيقونتها الحقيقية للتثبيت المباشر أو الحفظ في تنزيلات الهاتف.",
                            fontFamily = TajawalFontFamily,
                            fontSize = 13.sp,
                            color = SpaceColors.MutedText,
                            textAlign = TextAlign.Center,
                            lineHeight = 21.sp
                        )
                    }
                }
            } else {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            // Summary Stats Strip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SummaryBadgeCard("الإجمالي", "${downloads.size}", SpaceColors.NeonCyan, Modifier.weight(1f))
                                SummaryBadgeCard("APK للتثبيت", "$apkCount", SpaceColors.Emerald, Modifier.weight(1f))
                                SummaryBadgeCard("AAB للمتجر", "$aabCount", SpaceColors.Gold, Modifier.weight(1f))
                            }
                        }

                        savedBannerMessage?.let { bannerMsg ->
                            item {
                                NeonGlassCard(
                                    accentColor = SpaceColors.Emerald,
                                    secondaryAccent = SpaceColors.NeonCyan,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bannerMsg,
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp,
                                                color = SpaceColors.White
                                            )
                                        }
                                        IconButton(onClick = { savedBannerMessage = null }) {
                                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = SpaceColors.MutedText, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }

                        items(downloads, key = { it.id }) { item ->
                            val fileExistsOnDisk = remember(item.filePath) {
                                java.io.File(item.filePath).let { it.exists() && it.isFile }
                            }
                            val linkedProject = remember(item, projects) {
                                projects.find { it.id == item.projectId || it.name == item.projectName }
                                    ?: com.example.domain.model.AppProject(
                                        id = item.projectId.ifBlank { item.id },
                                        name = item.projectName,
                                        iconImageUri = item.iconImageUri,
                                        accentColorHex = item.accentColorHex,
                                        packageConfig = com.example.domain.model.PackageConfig(
                                            packageName = item.packageName,
                                            versionName = item.versionName,
                                            versionCode = item.versionCode
                                        )
                                    )
                            }.let { base ->
                                if (base.iconImageUri.isNullOrBlank() && !item.iconImageUri.isNullOrBlank()) {
                                    base.copy(iconImageUri = item.iconImageUri, accentColorHex = item.accentColorHex)
                                } else {
                                    base
                                }
                            }

                            NeonGlassCard(
                                accentColor = if (fileExistsOnDisk) SpaceColors.NeonCyan else SpaceColors.Gold,
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            com.example.ui.components.RealAppIconAvatar(
                                                project = linkedProject,
                                                size = 52.dp,
                                                cornerRadius = 14.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = item.projectName,
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp,
                                                    color = SpaceColors.White
                                                )
                                                Text(
                                                    text = "${item.fileName} • ${item.sizeText}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = SpaceColors.MutedText,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                                )
                                                Text(
                                                    text = "${item.packageName} • v${item.versionName} • ${dateFormat.format(Date(item.downloadedAt))}" +
                                                        if (fileExistsOnDisk) " • جاهز ✓" else " • محفوظ بالسجل",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = if (fileExistsOnDisk) SpaceColors.Emerald else SpaceColors.Gold,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                                )
                                            }
                                        }

                                        NeonIconButton(
                                            icon = Icons.Default.DeleteOutline,
                                            contentDescription = "حذف",
                                            onClick = { onDeleteDownload(item.id) },
                                            tint = SpaceColors.ErrorRed,
                                            borderAccent = SpaceColors.ErrorRed.copy(alpha = 0.5f),
                                            size = 44.dp
                                        )
                                    }

                                    // Primary Install & MediaStore Save Actions (2 Clear Options)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        NeonButton(
                                            text = "تثبيت مباشر",
                                            icon = Icons.Default.InstallMobile,
                                            style = NeonButtonStyle.GOLD_ENGINE,
                                            height = 48.dp,
                                            testTag = "install_now_btn_${item.id}",
                                            onClick = {
                                                val res = exporter.installApkDirectly(
                                                    filePath = item.filePath,
                                                    buildType = item.buildType,
                                                    projectName = item.projectName
                                                )
                                                Toast.makeText(context, res.messageAr, Toast.LENGTH_LONG).show()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        NeonButton(
                                            text = "حفظ في التنزيلات",
                                            icon = Icons.Default.DownloadDone,
                                            style = NeonButtonStyle.EMERALD_SUCCESS,
                                            height = 48.dp,
                                            testTag = "save_downloads_btn_${item.id}",
                                            onClick = {
                                                val saveRes = exporter.savePackageToPublicDownloadsViaMediaStore(
                                                    sourceFilePath = item.filePath,
                                                    preferredFileName = item.fileName,
                                                    projectName = item.projectName
                                                )
                                                if (saveRes.success) {
                                                    val msg = "✅ تم الحفظ في التنزيلات (${saveRes.displayPath}) بنجاح — جاهز للتثبيت من مدير الملفات"
                                                    savedBannerMessage = msg
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        saveRes.errorMessage ?: "تعذر الحفظ في مجلد التنزيلات",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Secondary Tools Row (Analyze / QR / Android Studio CI ZIP)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SpaceToolMiniChip(
                                            label = "تحليل",
                                            icon = Icons.Default.Analytics,
                                            accent = SpaceColors.NeonCyan,
                                            onClick = { selectedForAnalyzer = item },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SpaceToolMiniChip(
                                            label = "QR",
                                            icon = Icons.Default.QrCode2,
                                            accent = SpaceColors.Emerald,
                                            onClick = { selectedForQrShare = item },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SpaceToolMiniChip(
                                            label = "ZIP",
                                            icon = Icons.Default.FolderZip,
                                            accent = SpaceColors.Gold,
                                            onClick = { onExportCiZipForDownload(item) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedForAnalyzer?.let { apk ->
        ApkAnalyzerDialog(
            apk = apk,
            onDismiss = { selectedForAnalyzer = null }
        )
    }

    selectedForQrShare?.let { apk ->
        QrShareApkDialog(
            apk = apk,
            onDismiss = { selectedForQrShare = null }
        )
    }
}

@Composable
private fun SpaceToolMiniChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SpaceColors.SurfaceDeep.copy(alpha = 0.85f))
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = SpaceColors.White
        )
    }
}

@Composable
private fun SummaryBadgeCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    NeonGlassCard(
        accentColor = color,
        secondaryAccent = SpaceColors.ElectricBlue,
        shape = RoundedCornerShape(16.dp),
        elevation = 10.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 19.sp,
                color = color
            )
            Text(
                text = label,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = SpaceColors.MutedText
            )
        }
    }
}
