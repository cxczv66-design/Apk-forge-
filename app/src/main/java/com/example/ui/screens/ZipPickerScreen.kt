package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ImportedWorkspaceResult
import com.example.engine.LocalWorkspaceEngine
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.GlowingButton
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.LocalAppLanguage
import kotlinx.coroutines.launch

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
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = AppStrings.back(lang),
                        tint = TextPrimary
                    )
                }

                Text(
                    text = AppStrings.zipWebsite(lang),
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .testTag("zip_picker_screen"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top Hero Card
            GlassmorphismCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurfaceElevated,
                borderColor = GlassBorderSubtle,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = AppStrings.zipWebsite(lang),
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AppStrings.zipWebsiteDesc(lang),
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(OrangePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = OrangePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big Upload Button
            GlowingButton(
                text = when {
                    isExtracting -> "جارٍ فك الضغط واستخراج الأصول..."
                    importedWorkspace == null -> "اختر ملف ZIP من ذاكرة الهاتف"
                    else -> "استيراد ملف ZIP آخر"
                },
                icon = Icons.Default.FileUpload,
                onClick = {
                    if (!isExtracting) {
                        try {
                            zipPickerLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "الرجاء اختيار ملف ZIP", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "select_zip_button"
            )

            Spacer(modifier = Modifier.height(16.dp))

            val ws = importedWorkspace
            if (ws != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, StatusGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ws.displayFileName,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }

                            IconButton(
                                onClick = { importedWorkspace = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "حذف",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val fileCount = ws.assets.count { !it.isDirectory }
                            val kbText = (ws.totalSizeBytes / 1024.0).let { "%.1f KB".format(it) }
                            Text(
                                text = "$fileCount ملف مستخرج • $kbText • ${ws.detectedTitle}",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StatusGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "مستخرج وجاهز",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = StatusGreen
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم يتم اختيار أرشيف ZIP بعد. اضغط أعلاه لاختيار ملف مضغوط يحتوي على موقعك من ذاكرة الهاتف.",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "عاين موقعك المستخرج بجودة كاملة قبل الإنشاء، أو ابدأ المعالج مباشرة.",
                fontFamily = TajawalFontFamily,
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preview
                Button(
                    onClick = {
                        val currentWs = importedWorkspace ?: return@Button
                        if (onPreviewWorkspace != null) {
                            onPreviewWorkspace(currentWs)
                        } else {
                            onPreview()
                        }
                    },
                    enabled = ws != null && !isExtracting,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = if (ws != null) OrangePrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "معاينة حية",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (ws != null) TextPrimary else TextMuted
                    )
                }

                // Create App
                Button(
                    onClick = {
                        val currentWs = importedWorkspace ?: return@Button
                        when {
                            onCreateAppWithWorkspace != null -> onCreateAppWithWorkspace(currentWs)
                            onCreateAppWithFile != null -> onCreateAppWithFile(currentWs.displayFileName)
                            else -> onCreateApp()
                        }
                    },
                    enabled = ws != null && !isExtracting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangePrimary,
                        disabledContainerColor = DarkSurfaceElevated.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("create_app_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = if (ws != null) Color.White else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إنشاء التطبيق",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (ws != null) Color.White else TextMuted
                    )
                }
            }
        }
    }
}
