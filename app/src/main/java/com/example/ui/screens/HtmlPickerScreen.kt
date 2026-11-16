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
 * شاشة استيراد ملفات HTML الفضائية:
 * - ترويسة PremiumTopBar زجاجية
 * - بطاقة رفع مضيئة بأيقونة HTML ثلاثية الأبعاد
 * - حالة استخراج متوهجة ومعلومات الملف المستورد
 * - أزرار المعاينة والإنشاء مع الحفاظ الكامل على كل الوظائف و testTags
 */
@Composable
fun HtmlPickerScreen(
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
    var isImporting by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }

    val htmlPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            scope.launch {
                isImporting = true
                importError = null
                val res = LocalWorkspaceEngine.importHtmlUri(context, pickedUri)
                isImporting = false
                res.onSuccess { ws ->
                    importedWorkspace = ws
                    Toast.makeText(
                        context,
                        "✓ تم فحص واستيراد ${ws.displayFileName} (${(ws.totalSizeBytes / 1024).coerceAtLeast(1)} KB)",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure { err ->
                    importError = err.message ?: "تعذر قراءة ملف HTML"
                    Toast.makeText(context, "تعذر قراءة ملف HTML: ${err.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val openPicker: () -> Unit = {
        if (!isImporting) {
            try {
                htmlPickerLauncher.launch(arrayOf("text/html", "text/plain", "*/*"))
            } catch (e: Exception) {
                Toast.makeText(context, "الرجاء اختيار ملف HTML", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = AppStrings.htmlFolder(lang),
                subtitle = "HTML Source Importer",
                onBack = onBack,
                accentColor = SpaceColors.NeonCyan,
                backTestTag = "html_picker_back_btn"
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
                    .testTag("html_picker_screen"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // بطاقة إضافة واستيراد ملف HTML الكبيرة بأيقونة مضيئة
                NeonGlassCard(
                    accentColor = SpaceColors.ElectricBlue,
                    secondaryAccent = SpaceColors.NeonCyan,
                    elevation = 16.dp,
                    onClick = openPicker
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Space3DFeatureBadge(
                            icon = Icons.Default.Code,
                            accentColor = SpaceColors.NeonCyan,
                            secondaryColor = SpaceColors.ElectricBlue,
                            size = 66.dp
                        )
                        Text(
                            text = AppStrings.htmlFolder(lang),
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SpaceColors.White
                        )
                        Text(
                            text = AppStrings.htmlFolderDesc(lang),
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = SpaceColors.MutedText,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        NeonButton(
                            text = when {
                                isImporting -> "جارٍ الاستيراد والتحليل..."
                                importedWorkspace == null -> "اختر ملف HTML من ذاكرة الهاتف"
                                else -> "استيراد ملف HTML آخر"
                            },
                            icon = Icons.Default.FileUpload,
                            style = NeonButtonStyle.PRIMARY_GLOW,
                            isLoading = isImporting,
                            onClick = openPicker,
                            testTag = "select_html_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (isImporting) {
                    FuturisticLoading(
                        title = "جارٍ تحليل صفحة HTML واستخراج الأصول...",
                        subtitle = "فحص الوسوم والألوان والسكربتات",
                        accentColor = SpaceColors.NeonCyan
                    )
                }

                if (importError != null) {
                    NeonGlassCard(
                        accentColor = SpaceColors.ErrorRed,
                        secondaryAccent = SpaceColors.ErrorRed
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SpaceColors.ErrorRed)
                            Text(
                                text = importError ?: "",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = SpaceColors.ErrorRed,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                val ws = importedWorkspace
                if (ws != null) {
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
                                        icon = Icons.Default.Code,
                                        accentColor = SpaceColors.Emerald,
                                        secondaryColor = SpaceColors.NeonCyan,
                                        size = 42.dp
                                    )
                                    Column {
                                        Text(
                                            text = ws.displayFileName,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SpaceColors.White
                                        )
                                        Text(
                                            text = ws.detectedTitle,
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            color = SpaceColors.MutedText
                                        )
                                    }
                                }

                                Surface(
                                    color = SpaceColors.Emerald.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SpaceColors.Emerald.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "✓ مستورد فعلياً",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = SpaceColors.Emerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            NeonProgressIndicator(
                                progress = 1f,
                                startColor = SpaceColors.Emerald,
                                endColor = SpaceColors.NeonCyan
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الحجم: ${(ws.totalSizeBytes / 1024.0).let { "%.1f KB".format(it) }}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = SpaceColors.NeonCyan
                                )
                                Text(
                                    text = "${ws.assets.size} ملف/مورد • جاهز للبناء",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = SpaceColors.MutedText
                                )
                            }
                        }
                    }
                } else if (!isImporting) {
                    NeonGlassCard(
                        accentColor = SpaceColors.ElectricBlue.copy(alpha = 0.55f),
                        secondaryAccent = SpaceColors.NeonPurple.copy(alpha = 0.4f),
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
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = SpaceColors.MutedText,
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "لم يتم اختيار ملف HTML بعد. اضغط أعلاه لاختيار ملف من جهازك.",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = SpaceColors.MutedText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Text(
                    text = "عاين صفحة الويب الحقيقية قبل الإنشاء أو انتقل مباشرة لمعالج التخصيص.",
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
                        enabled = ws != null && !isImporting,
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
                        enabled = ws != null && !isImporting,
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
