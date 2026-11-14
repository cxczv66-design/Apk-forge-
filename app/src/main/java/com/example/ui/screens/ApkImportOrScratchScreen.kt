package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.repository.DownloadedApk
import com.example.domain.model.ApkInspectCandidate
import com.example.domain.model.AppProject
import com.example.engine.AdaptiveSourceAnalyzer
import com.example.ui.components.HolographicAiOrb
import com.example.ui.components.ThreeDIconBadge
import com.example.ui.components.ThreeDSurfaceCard
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * مسار APK الذكي (Omni-Adaptive APK Choice):
 * يخيّر المستخدم بين:
 * 1) استيراد حزمة APK موجودة (من التنزيلات أو التطبيقات المثبتة أو اختيار ملف .apk) لتعديل الصلاحيات والأيقونة والاسم.
 * 2) البناء من الصفر عبر مساعد الذكاء الاصطناعي (AI Wizard).
 */
@Composable
fun ApkImportOrScratchScreen(
    downloads: List<DownloadedApk>,
    onBack: () -> Unit,
    onSelectImportedApkProject: (AppProject) -> Unit,
    onBuildFromScratchAi: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showScannerList by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var isPickingFile by remember { mutableStateOf(false) }
    var candidates by remember { mutableStateOf<List<ApkInspectCandidate>>(emptyList()) }

    val apkFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isPickingFile = true
            scope.launch {
                val inspected = AdaptiveSourceAnalyzer.inspectPickedApkUri(context, uri)
                isPickingFile = false
                if (inspected != null) {
                    val project = AdaptiveSourceAnalyzer.convertCandidateToProject(inspected)
                    Toast.makeText(context, "تم فحص ${inspected.appName} (${inspected.permissions.size} صلاحية) ✓", Toast.LENGTH_SHORT).show()
                    onSelectImportedApkProject(project)
                } else {
                    Toast.makeText(context, "تعذر قراءة ملف APK المختار — تأكد من سلامة الحزمة", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LaunchedEffect(showScannerList) {
        if (showScannerList && candidates.isEmpty()) {
            isScanning = true
            candidates = AdaptiveSourceAnalyzer.scanDeviceAndDownloadedApks(context, downloads)
            isScanning = false
        }
    }

    BackHandler {
        if (showScannerList) showScannerList = false else onBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (showScannerList) showScannerList = false else onBack() },
                    modifier = Modifier.testTag("apk_choice_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (showScannerList) "اختيار APK للاستيراد والتعديل" else "مسار APK الذكي",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "استيراد وتعديل حزمة موجودة أو البناء بالذكاء الاصطناعي",
                        fontFamily = TajawalFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .testTag("apk_import_or_scratch_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
        ) {
            if (!showScannerList) {
                item {
                    ThreeDSurfaceCard(
                        accentColor = StatusGreen,
                        onClick = { showScannerList = true },
                        testTag = "apk_option_import_existing"
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThreeDIconBadge(
                                    icon = Icons.Filled.Android,
                                    accentColor = StatusGreen,
                                    secondaryColor = CyanAccent,
                                    size = 56.dp
                                )
                                Surface(
                                    color = StatusGreen.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, StatusGreen.copy(alpha = 0.45f))
                                ) {
                                    Text(
                                        text = "4 مراحل تعديل",
                                        color = StatusGreen,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "استيراد APK موجود للتعديل",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "اختر ملف APK من التنزيلات أو التطبيقات المثبتة أو وحدة التخزين، لتعديل الاسم والأيقونة ومعرف الحزمة والصلاحيات والتحصين الأمني.",
                                fontFamily = TajawalFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "استعراض الحزم والملفات",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = StatusGreen
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                item {
                    ThreeDSurfaceCard(
                        accentColor = Color(0xFFA855F7),
                        onClick = onBuildFromScratchAi,
                        testTag = "apk_option_build_from_scratch_ai"
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HolographicAiOrb(
                                    icon = Icons.Filled.AutoAwesome,
                                    accentColor = Color(0xFFA855F7),
                                    secondaryColor = Color(0xFF38BDF8),
                                    size = 56.dp
                                )
                                Surface(
                                    color = Color(0xFFA855F7).copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "AI Wizard • +12 مرحلة",
                                        color = Color(0xFFD8B4FE),
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "البناء من الصفر بالذكاء الاصطناعي ✨",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "اكتب فكرة تطبيقك وسيتولى مساعد الذكاء الاصطناعي تصميم خارطة طريق من 12+ مرحلة وطرح أسئلة ذكية لتوليد تطبيقك الكامل تلقائياً.",
                                fontFamily = TajawalFontFamily,
                                fontSize = 13.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "تشغيل مساعد AI Wizard",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFD8B4FE)
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFFD8B4FE), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                item {
                    Button(
                        onClick = {
                            apkFilePicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
                        },
                        enabled = !isPickingFile,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("pick_external_apk_file_btn")
                    ) {
                        if (isPickingFile) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("جارٍ تحليل حزمة APK...", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("اختيار ملف .apk من الجهاز / التنزيلات", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                if (isScanning) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator(color = StatusGreen)
                                Text("جارٍ فحص التنزيلات والتطبيقات المتاحة...", fontFamily = TajawalFontFamily, color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else if (candidates.isEmpty()) {
                    item {
                        ThreeDSurfaceCard(accentColor = GlassBorderSubtle) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Android, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Text("لم يُعثر على ملفات APK محلية بعد", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("استخدم الزر أعلاه لاختيار أي ملف .apk من جهازك وتعديله فوراً.", fontFamily = TajawalFontFamily, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(candidates, key = { it.id }) { cand ->
                        ApkCandidateCard(
                            candidate = cand,
                            onClick = {
                                val project = AdaptiveSourceAnalyzer.convertCandidateToProject(cand)
                                onSelectImportedApkProject(project)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkCandidateCard(
    candidate: ApkInspectCandidate,
    onClick: () -> Unit
) {
    ThreeDSurfaceCard(
        accentColor = StatusGreen,
        elevation = 8.dp,
        cornerRadius = 16.dp,
        onClick = onClick,
        testTag = "apk_candidate_${candidate.id}"
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(StatusGreen.copy(alpha = 0.15f))
                    .border(1.dp, StatusGreen.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!candidate.iconImageUri.isNullOrBlank()) {
                    AsyncImage(
                        model = candidate.iconImageUri,
                        contentDescription = candidate.appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.Android, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(26.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = candidate.appName,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${candidate.packageName} • v${candidate.versionName}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = CyanAccent.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = candidate.sourceOriginLabel,
                            fontFamily = TajawalFontFamily,
                            fontSize = 10.sp,
                            color = CyanAccent,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${candidate.permissions.size} صلاحية",
                        fontFamily = TajawalFontFamily,
                        fontSize = 10.sp,
                        color = OrangePrimary
                    )
                }
            }

            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "تعديل APK", tint = StatusGreen, modifier = Modifier.size(18.dp))
        }
    }
}
