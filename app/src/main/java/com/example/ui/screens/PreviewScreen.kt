package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PreviewScreen(
    fileName: String = "MegaEditor_New.zip",
    onBack: () -> Unit
) {
    var isReloading by remember { mutableStateOf(false) }
    var selectedFileToView by remember { mutableStateOf<Pair<String, String>?>(null) }

    val folders = listOf(
        Pair("Android", "مجلد • 4 عناصر"),
        Pair("AIDEProjects", "مجلد • 2 مشروع"),
        Pair("ApkEditor", "مجلد • 3 ملفات"),
        Pair("Alarms", "مجلد • فارغ"),
        Pair("Audiobooks", "مجلد • فارغ"),
        Pair("backups", "مجلد • 1 ملف"),
        Pair("Download", "مجلد • 12 عنصر"),
        Pair("Documents", "مجلد • 5 مستندات"),
        Pair("DCIM", "مجلد • 84 صورة"),
        Pair("welcome.txt", "ملف • 1.2 KB"),
        Pair("index.html", "ملف • 8.4 KB"),
        Pair("notes.md", "ملف • 3.4 KB")
    )

    Scaffold(
        containerColor = Color(0xFF0F111A),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F111A))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "معاينة",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextPrimary
                )

                IconButton(
                    onClick = {
                        isReloading = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة تحميل",
                        tint = TextPrimary
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF090A0E))
                    .navigationBarsPadding()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "هذا بالضبط ما سيعرضه تطبيقك...",
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
                .testTag("preview_screen")
        ) {
            // Mega Editor Header (matching video at 00:48)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1B1834))
                    .border(1.dp, Color(0xFF4A3E80), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Mega Editor",
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Advanced File Manager",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 11.sp,
                                color = Color(0xFFB3A5E6)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Path box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262145))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "/storage/emulated/0/",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                            Text(
                                text = "9 مجلد • 2 ملف",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFB3A5E6)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dual Panels Row: اللوحة اليمنى & اللوحة اليسرى
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1B38))
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "★ اللوحة اليمنى",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFFFB74D)
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E1B38))
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♥ اللوحة اليسرى",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFFF79B0)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // File items list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(folders) { (name, info) ->
                    val isFile = name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".html")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF18162B))
                            .border(1.dp, Color(0x334A3E80), RoundedCornerShape(12.dp))
                            .clickable {
                                selectedFileToView = Pair(name, info)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isFile) Icons.Default.Description else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isFile) Color(0xFF64B5F6) else Color(0xFFFFB74D),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = info,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 10.sp,
                                    color = Color(0xFF9E92C8)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF6A6093),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // File Inspection Dialog
            if (selectedFileToView != null) {
                val (name, info) = selectedFileToView!!
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { selectedFileToView = null },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .clickable { selectedFileToView = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = Color(0xFF1A1730),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5A4499)),
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .clickable(enabled = false) {}
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = OrangePrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = name, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { selectedFileToView = null }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text(text = "المسار: /storage/emulated/0/$name ($info)", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = TextSecondary)
                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    color = Color(0xFF100E20),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 100.dp, max = 220.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = when {
                                                name.endsWith(".html") -> "<!DOCTYPE html>\n<html>\n  <head>\n    <title>ApkForge App</title>\n  </head>\n  <body>\n    <h1>Hello from ApkForge</h1>\n  </body>\n</html>"
                                                name.endsWith(".md") -> "# ApkForge Notes\n- Source successfully imported.\n- Ready for packaging into APK."
                                                else -> "محتوى الملف محفوظ بتنسيق UTF-8.\nتم فحصه والتحقق من سلامة البنية للتضمين داخل أرشيف APK."
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = StatusGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = { selectedFileToView = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("إغلاق المعاينة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // In-app toolbar (matching video bottom of preview: إنشاء + | مزامنة | قدم | رجوع | الأعلى)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141224))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "الأعلى ^", fontSize = 11.sp, color = Color(0xFFB3A5E6), fontFamily = CairoFontFamily)
                Text(text = "رجوع <", fontSize = 11.sp, color = Color(0xFFB3A5E6), fontFamily = CairoFontFamily)
                Text(text = "قدم >", fontSize = 11.sp, color = Color(0xFFB3A5E6), fontFamily = CairoFontFamily)
                Text(text = "مزامنة", fontSize = 11.sp, color = Color(0xFFB3A5E6), fontFamily = CairoFontFamily)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF5A4499))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "إنشاء +", fontSize = 11.sp, color = Color.White, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
