package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.components.space.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * مسار APK الذكي (Space / Sci-Fi APK Choice):
 * - بطاقات استيراد APK ثلاثية الأبعاد + تحذير أمان وتوقيع واضح قبل العمليات الحساسة
 * - قائمة الحزم المكتشفة أو استيراد ملف .apk خارجي
 * - خيار البناء من الصفر بالذكاء الاصطناعي (AI Wizard)
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
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = if (showScannerList) "اختيار APK للاستيراد والتعديل" else "مسار APK الذكي",
                subtitle = "استيراد وتعديل حزمة موجودة أو البناء بالذكاء الاصطناعي",
                onBack = { if (showScannerList) showScannerList = false else onBack() },
                backTestTag = "apk_choice_back_btn",
                accentColor = SpaceColors.Emerald
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = SpaceDimensions.MaxContentWidth)
                    .padding(horizontal = 16.dp)
                    .testTag("apk_import_or_scratch_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
            ) {
                // تنبيه أمني واضح قبل العمليات الحساسة (تعديل حزم APK وإعادة التوقيع)
                item {
                    NeonGlassCard(
                        accentColor = SpaceColors.Gold,
                        secondaryAccent = SpaceColors.NeonCyan,
                        elevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Space3DFeatureBadge(
                                icon = Icons.Default.Security,
                                accentColor = SpaceColors.Gold,
                                secondaryColor = Color(0xFFFF8C00),
                                size = 40.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تنبيه التعديل والتوقيع الرقمي",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SpaceColors.Gold
                                )
                                Text(
                                    text = "تعديل حزمة APK أو تغيير اسم الحزمة والصلاحيات يتطلب إعادة التوقيع بمفتاح Keystore جديد عند التصدير.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = SpaceColors.MutedText,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                if (!showScannerList) {
                    item {
                        NeonGlassCard(
                            accentColor = SpaceColors.Emerald,
                            secondaryAccent = SpaceColors.NeonCyan,
                            elevation = 16.dp,
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
                                    Space3DFeatureBadge(
                                        icon = Icons.Filled.Android,
                                        accentColor = SpaceColors.Emerald,
                                        secondaryColor = SpaceColors.NeonCyan,
                                        size = 58.dp
                                    )
                                    Surface(
                                        color = SpaceColors.Emerald.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SpaceColors.Emerald.copy(alpha = 0.55f))
                                    ) {
                                        Text(
                                            text = "4 مراحل تعديل",
                                            color = SpaceColors.Emerald,
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
                                    color = SpaceColors.White
                                )
                                Text(
                                    text = "اختر ملف APK من التنزيلات أو التطبيقات المثبتة أو وحدة التخزين، لتعديل الاسم والأيقونة ومعرف الحزمة والصلاحيات والتحصين الأمني.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp,
                                    color = SpaceColors.MutedText,
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
                                        color = SpaceColors.Emerald
                                    )
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    item {
                        NeonGlassCard(
                            accentColor = SpaceColors.NeonPurple,
                            secondaryAccent = SpaceColors.NeonCyan,
                            elevation = 16.dp,
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
                                    SpaceFloatingRobotAvatar(
                                        size = 58.dp,
                                        accentColor = SpaceColors.NeonPurple,
                                        showOnlineDot = true
                                    )
                                    Surface(
                                        color = SpaceColors.NeonPurple.copy(alpha = 0.20f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SpaceColors.NeonPurple.copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "AI Wizard • +12 مرحلة",
                                            color = Color(0xFFE9D5FF),
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
                                    color = SpaceColors.White
                                )
                                Text(
                                    text = "اكتب فكرة تطبيقك وسيتولى مساعد الذكاء الاصطناعي تصميم خارطة طريق من 12+ مرحلة وطرح أسئلة ذكية لتوليد تطبيقك الكامل تلقائياً.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp,
                                    color = SpaceColors.MutedText,
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
                                        color = SpaceColors.NeonCyan
                                    )
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                } else {
                    item {
                        NeonButton(
                            text = if (isPickingFile) "جارٍ تحليل حزمة APK..." else "اختيار ملف .apk من الجهاز / التنزيلات",
                            icon = Icons.Filled.UploadFile,
                            style = NeonButtonStyle.EMERALD_SUCCESS,
                            isLoading = isPickingFile,
                            testTag = "pick_external_apk_file_btn",
                            onClick = {
                                apkFilePicker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (isScanning) {
                        item {
                            FuturisticLoading(
                                title = "جارٍ فحص التنزيلات والتطبيقات المتاحة...",
                                subtitle = "استخراج الأذونات وبيانات الحزم",
                                accentColor = SpaceColors.Emerald
                            )
                        }
                    } else if (candidates.isEmpty()) {
                        item {
                            NeonGlassCard(accentColor = SpaceColors.ElectricBlue) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Space3DFeatureBadge(
                                        icon = Icons.Filled.Android,
                                        accentColor = SpaceColors.Emerald,
                                        size = 52.dp
                                    )
                                    Text(
                                        text = "لم يُعثر على ملفات APK محلية بعد",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = SpaceColors.White
                                    )
                                    Text(
                                        text = "استخدم الزر أعلاه لاختيار أي ملف .apk من جهازك وتحليله فوراً.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        color = SpaceColors.MutedText
                                    )
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
}

@Composable
private fun ApkCandidateCard(
    candidate: ApkInspectCandidate,
    onClick: () -> Unit
) {
    NeonGlassCard(
        accentColor = SpaceColors.Emerald,
        secondaryAccent = SpaceColors.NeonCyan,
        elevation = 10.dp,
        shape = RoundedCornerShape(18.dp),
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
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SpaceColors.Emerald.copy(alpha = 0.14f))
                    .border(1.2.dp, SpaceColors.Emerald.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
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
                    Icon(Icons.Filled.Android, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = candidate.appName,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = SpaceColors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${candidate.packageName} • v${candidate.versionName}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SpaceColors.MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = SpaceColors.NeonCyan.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, SpaceColors.NeonCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = candidate.sourceOriginLabel,
                            fontFamily = TajawalFontFamily,
                            fontSize = 10.sp,
                            color = SpaceColors.NeonCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${candidate.permissions.size} صلاحية",
                        fontFamily = TajawalFontFamily,
                        fontSize = 10.sp,
                        color = SpaceColors.Gold
                    )
                }
            }

            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "تعديل APK", tint = SpaceColors.Emerald, modifier = Modifier.size(18.dp))
        }
    }
}
