package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.repository.DownloadedApk
import com.example.ui.components.ApkAnalyzerDialog
import com.example.ui.components.QrShareApkDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DownloadsScreen(
    downloads: List<DownloadedApk>,
    onBack: () -> Unit,
    onDeleteDownload: (String) -> Unit = {},
    onExportCiZipForDownload: (DownloadedApk) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedForAnalyzer by remember { mutableStateOf<DownloadedApk?>(null) }
    var selectedForQrShare by remember { mutableStateOf<DownloadedApk?>(null) }
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    val apkCount = downloads.count { it.buildType.equals("APK", ignoreCase = true) }
    val aabCount = downloads.count { it.buildType.equals("AAB", ignoreCase = true) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مركز الحزم والتنزيلات",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "حلّل محتوى الـ APK/AAB، شارك عبر رمز QR، أو صدّر مشروع Android Studio السحابي.",
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                )
            }
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AllInbox,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "لم تبنِ أو تنزّل أي حزمة بعد",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "تصل جميع ملفات APK و AAB المبنية هنا لتحليلها ومشاركتها وتصديرها.",
                        fontFamily = TajawalFontFamily,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp
                    )
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
                                SummaryBadgeCard("الإجمالي", "${downloads.size}", OrangePrimary, Modifier.weight(1f))
                                SummaryBadgeCard("APK للتثبيت", "$apkCount", StatusGreen, Modifier.weight(1f))
                                SummaryBadgeCard("AAB للمتجر", "$aabCount", Color(0xFF38BDF8), Modifier.weight(1f))
                            }
                        }

                        items(downloads, key = { it.id }) { item ->
                            val fileExistsOnDisk = remember(item.filePath) {
                                java.io.File(item.filePath).let { it.exists() && it.isFile }
                            }
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(OrangePrimary.copy(alpha = 0.16f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (item.buildType.equals("AAB", ignoreCase = true)) {
                                                        Icons.Default.CloudUpload
                                                    } else {
                                                        Icons.Default.Android
                                                    },
                                                    contentDescription = null,
                                                    tint = OrangePrimary,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = item.fileName,
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextPrimary,
                                                    style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr)
                                                )
                                                Text(
                                                    text = "${item.projectName} • v${item.versionName} (${item.versionCode}) • ${item.sizeText}",
                                                    fontFamily = TajawalFontFamily,
                                                    fontSize = 12.sp,
                                                    color = TextSecondary,
                                                    style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.ContentOrLtr)
                                                )
                                                Text(
                                                    text = "${item.packageName} • ${dateFormat.format(Date(item.downloadedAt))}" +
                                                        if (fileExistsOnDisk) " • جاهز ✓" else " • غير موجود بالقرص",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = if (fileExistsOnDisk) StatusGreen else WarningYellow,
                                                    style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr)
                                                )
                                            }
                                        }

                                        IconButton(onClick = { onDeleteDownload(item.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(20.dp))
                                        }
                                    }

                                    // Action Workbench Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val opened = com.example.engine.ProjectExporter(context)
                                                    .openOrInstallFile(item.filePath, item.buildType)
                                                if (!opened) {
                                                    Toast.makeText(
                                                        context,
                                                        "الملف غير موجود على القرص — أعد البناء لتوليده فعلياً",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF140C00))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (item.fileName.endsWith(".apk", true)) "تثبيت" else "فتح",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF140C00)
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { selectedForAnalyzer = item },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.Analytics, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تحليل", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                        }

                                        OutlinedButton(
                                            onClick = { selectedForQrShare = item },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("QR", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextPrimary)
                                        }

                                        OutlinedButton(
                                            onClick = { onExportCiZipForDownload(item) },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(0.95f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.FolderZip, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("ZIP", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextPrimary)
                                        }
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
private fun SummaryBadgeCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = label,
                fontFamily = CairoFontFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
