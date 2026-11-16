package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ImportedWorkspaceResult
import com.example.engine.LocalWorkspaceEngine
import com.example.ui.components.space.*
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.LocalAppLanguage
import kotlinx.coroutines.launch

/**
 * شاشة استيراد أرشيف ZIP الفضائية:
 * - ترويسة PremiumTopBar زجاجية
 * - بطاقة رفع ZIP ثلاثية الأبعاد
 * - شريط تقدم مضيء + قائمة محتويات المشروع المستخرج
 * - أزرار المعاينة والإنشاء مع الحفاظ على testTags (select_zip_button, preview_button, create_app_button)
 */
@Composable
fun ZipPickerScreen(
    hasPreviewed: Boolean,
    onBack: () -> Unit,
    onPreview: () -> Unit = {},
    onPreviewWorkspace: ((ImportedWorkspaceResult) -> Unit)? = null,
    onCreateApp: () -> Unit = {},
    onCreateAppWithFile: ((String) -> Unit)? = null,
    onCreateAppWithWorkspace: ((ImportedWorkspaceResult) -> Unit)? = null
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importedWorkspace by remember { mutableStateOf<ImportedWorkspaceResult?>(null) }
    var isExtracting by remember { mutableStateOf(false) }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            scope.launch {
                isExtracting = true
                val res = LocalWorkspaceEngine.importZipUri(context, pickedUri)
                isExtracting = false
                res.onSuccess { ws ->
                    importedWorkspace = ws
                    Toast.makeText(
                        context,
                        "✓ تم فك واستخراج ${ws.assets.size} عنصر من ${ws.displayFileName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    Toast.makeText(context, "خطأ في الأرشيف: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val openZipPicker: () -> Unit = {
        if (!isExtracting) {
            try {
                zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
            } catch (e: Exception) {
                Toast.makeText(context, "الرجاء اختيار ملف ZIP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = AppStrings.zipWebsite(lang),
                subtitle = "ZIP Project Extractor",
                onBack = onBack,
                accentColor = SpaceColors.NeonCyan,
                backTestTag = "zip_picker_back_btn"
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = SpaceDimensions.MaxContentWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("zip_picker_screen"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // بطاقة الرفع الكبيرة لأرشيف ZIP
                NeonGlassCard(
                    accentColor = SpaceColors.NeonCyan,
                    secondaryAccent = SpaceColors.NeonPurple,
                    elevation = 16.dp,
                    onClick = openZipPicker
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Space3DFeatureBadge(
                            icon = Icons.Default.FolderZip,
                            accentColor = SpaceColors.NeonCyan,
                            secondaryColor = SpaceColors.ElectricBlue,
                            size = 66.dp
                        )
                        Text(
                            text = AppStrings.zipWebsite(lang),
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SpaceColors.White
                        )
                        Text(
                            text = AppStrings.zipWebsiteDesc(lang),
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = SpaceColors.MutedText,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        NeonButton(
                            text = when {
                                isExtracting -> "جارٍ فك الضغط واستخراج الأصول..."
                                importedWorkspace == null -> "اختر ملف ZIP من ذاكرة الهاتف"
                                else -> "استيراد ملف ZIP آخر"
                            },
                            icon = Icons.Default.FileUpload,
                            style = NeonButtonStyle.PRIMARY_GLOW,
                            isLoading = isExtracting,
                            onClick = openZipPicker,
                            testTag = "select_zip_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isExtracting) {
                    FuturisticLoading(
                        title = "جارٍ فك ضغط الأرشيف...",
                        subtitle = "استخراج صفحات HTML وCSS والصور والسكربتات",
                        accentColor = SpaceColors.NeonCyan
                    )
                }

                val ws = importedWorkspace
                if (ws != null) {
                    val fileItems = remember(ws) { ws.assets.filter { !it.isDirectory } }
                    val kbText = (ws.totalSizeBytes / 1024.0).let { "%.1f KB".format(it) }

                    NeonGlassCard(
                        accentColor = SpaceColors.Emerald,
                        secondaryAccent = SpaceColors.NeonCyan,
                        elevation = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Space3DFeatureBadge(
                                        icon = Icons.Default.Archive,
                                        accentColor = SpaceColors.NeonCyan,
                                        secondaryColor = SpaceColors.Emerald,
                                        size = 42.dp
                                    )
                                    Column {
                                        Text(
                                            text = ws.displayFileName,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SpaceColors.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${fileItems.size} ملف مستخرج • $kbText • ${ws.detectedTitle}",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            color = SpaceColors.MutedText
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { importedWorkspace = null },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إزالة الملف",
                                        tint = SpaceColors.MutedText
                                    )
                                }
                            }

                            NeonProgressIndicator(
                                progress = 1f,
                                startColor = SpaceColors.NeonCyan,
                                endColor = SpaceColors.Emerald
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "مستخرج وجاهز للمعاينة وبناء التطبيق",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = SpaceColors.Emerald
                                )
                            }

                            if (fileItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "محتويات المشروع (${fileItems.size}):",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = SpaceColors.White
                                    )
                                    fileItems.take(5).forEach { asset ->
                                        Surface(
                                            color = SpaceColors.SurfaceDeep.copy(alpha = 0.72f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(0.8.dp, SpaceColors.NeonCyan.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = asset.relativePath,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = SpaceColors.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = "${(asset.sizeBytes / 1024).coerceAtLeast(1)} KB",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    color = SpaceColors.NeonCyan
                                                )
                                            }
                                        }
                                    }
                                    if (fileItems.size > 5) {
                                        Text(
                                            text = "+ ${fileItems.size - 5} ملفات أخرى داخل المساحة",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            color = SpaceColors.MutedText
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!isExtracting) {
                    NeonGlassCard(
                        accentColor = SpaceColors.NeonCyan.copy(alpha = 0.55f),
                        secondaryAccent = SpaceColors.NeonPurple.copy(alpha = 0.35f),
                        elevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = SpaceColors.MutedText,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "لم يتم اختيار أرشيف ZIP بعد. اضغط أعلاه لاختيار ملف مضغوط يحتوي على موقعك من ذاكرة الهاتف.",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = SpaceColors.MutedText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text = "عاين موقعك المستخرج بجودة كاملة قبل الإنشاء، أو ابدأ المعالج مباشرة.",
                    fontFamily = TajawalFontFamily,
                    fontSize = 13.sp,
                    color = SpaceColors.MutedText,
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeonButton(
                        text = "معاينة حية",
                        icon = Icons.Default.Visibility,
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        enabled = ws != null && !isExtracting,
                        testTag = "preview_button",
                        onClick = {
                            val currentWs = importedWorkspace ?: return@NeonButton
                            if (onPreviewWorkspace != null) {
                                onPreviewWorkspace(currentWs)
                            } else {
                                onPreview()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    NeonButton(
                        text = "إنشاء التطبيق",
                        icon = Icons.Default.Android,
                        style = NeonButtonStyle.EMERALD_SUCCESS,
                        enabled = ws != null && !isExtracting,
                        testTag = "create_app_button",
                        onClick = {
                            val currentWs = importedWorkspace ?: return@NeonButton
                            when {
                                onCreateAppWithWorkspace != null -> onCreateAppWithWorkspace(currentWs)
                                onCreateAppWithFile != null -> onCreateAppWithFile(currentWs.displayFileName)
                                else -> onCreateApp()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
