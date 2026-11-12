package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.R
import com.example.domain.model.AppProject
import com.example.domain.model.SmartTemplatesCatalog
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.ImportApkForgeDialog
import com.example.ui.components.RealAppIconAvatar
import com.example.ui.components.SmartUrlScraperDialog
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.LocalAppLanguage

@Composable
fun DashboardScreen(
    projects: List<AppProject>,
    onOpenHtmlPicker: () -> Unit,
    onOpenZipPicker: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenProject: (AppProject) -> Unit,
    onOpenTermuxDiagnostic: () -> Unit,
    onUseTemplate: (AppProject) -> Unit = {},
    onImportProjectJson: (String) -> AppProject? = { null }
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val templates = remember { SmartTemplatesCatalog.getAll() }
    var showImportDialog by remember { mutableStateOf(false) }
    var showUrlScraperDialog by remember { mutableStateOf(false) }

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
                // Left Icon: Account Avatar Shortcut
                IconButton(onClick = onOpenAccount) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = AppStrings.account(lang),
                        tint = OrangePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Center Title
                Text(
                    text = AppStrings.appTitle(lang),
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextPrimary
                )

                // Right Menu Button: Termux Diagnostic
                IconButton(onClick = onOpenTermuxDiagnostic) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = AppStrings.termuxStatus(lang),
                        tint = TextPrimary
                    )
                }
            }
        },
        floatingActionButton = {
            // Direct Contact Developer Email Button (ziorio950@gmail.com)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape, spotColor = OrangePrimary.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(Color(0xFF1A202E))
                    .border(1.5.dp, OrangePrimary.copy(alpha = 0.65f), CircleShape)
                    .clickable {
                        val developerEmail = "ziorio950@gmail.com"
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$developerEmail")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
                            putExtra(Intent.EXTRA_SUBJECT, "تواصل مع مطور ApkForge")
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "مراسلة المطور عبر البريد"))
                        } catch (e: Exception) {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Developer Email", developerEmail))
                            Toast.makeText(context, "تم نسخ بريد المطور: $developerEmail", Toast.LENGTH_LONG).show()
                        }
                    }
                    .testTag("contact_developer_email_fab"),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_email_dev),
                    contentDescription = "تواصل مع المطور عبر البريد ziorio950@gmail.com",
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .testTag("dashboard_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // Two Big Cards Side-by-Side: ZIP Website & HTML Document
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Card: ZIP
                    DashboardActionCard(
                        title = AppStrings.zipWebsite(lang),
                        subtitle = AppStrings.zipWebsiteDesc(lang),
                        icon = Icons.Default.Archive,
                        modifier = Modifier.weight(1f),
                        testTag = "zip_card",
                        onClick = onOpenZipPicker
                    )

                    // Right Card: HTML
                    DashboardActionCard(
                        title = AppStrings.htmlFolder(lang),
                        subtitle = AppStrings.htmlFolderDesc(lang),
                        icon = Icons.Default.Code,
                        modifier = Modifier.weight(1f),
                        testTag = "html_card",
                        onClick = onOpenHtmlPicker
                    )
                }
            }

            // Smart URL / PWA Auto-Scraper Bar
            item {
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showUrlScraperDialog = true }
                        .testTag("smart_url_scraper_card")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFF0EA5E9).copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "تحويل رابط موقع ذكي (PWA Auto-Scraper) 🌐",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "ألصق رابط أي موقع لاستخراج الاسم والشعار والألوان وتعبئة المشروع تلقائياً",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Middle Navigation Row: المشاريع | التنزيلات | الحساب
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavTabItem(
                        title = AppStrings.myRecentProjects(lang),
                        icon = Icons.Default.Folder,
                        onClick = { /* already on projects */ }
                    )
                    NavTabItem(
                        title = AppStrings.downloads(lang),
                        icon = Icons.Default.Download,
                        onClick = onOpenDownloads
                    )
                    NavTabItem(
                        title = AppStrings.account(lang),
                        icon = Icons.Default.Person,
                        onClick = onOpenAccount
                    )
                }
            }

            // Smart Ready-Made App Templates Strip
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قوالب ذكية جاهزة ⚡",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Surface(
                            color = OrangePrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable { showImportDialog = true }
                                .testTag("import_apkforge_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد .apkforge", fontFamily = CairoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OrangePrimary)
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(templates, key = { it.id }) { tpl ->
                            val tplColor = parseColorSafe(tpl.accentHex, OrangePrimary)
                            Surface(
                                color = DarkSurfaceElevated,
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, tplColor.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .width(172.dp)
                                    .clickable { onUseTemplate(tpl.buildProject()) }
                                    .testTag("template_${tpl.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = tplColor.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = tpl.badgeAr,
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tplColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = tplColor, modifier = Modifier.size(15.dp))
                                    }
                                    Text(
                                        text = tpl.titleAr,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = tpl.subtitleAr,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Recent Projects Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.myRecentProjects(lang),
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                }
            }

            // Empty state when no projects exist (Removed fake Rio project!)
            if (projects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = OrangePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = AppStrings.noProjectsTitle(lang),
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = AppStrings.noProjectsDesc(lang),
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenZipPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = AppStrings.createNewProject(lang),
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // User's Real Created Projects
                items(projects.size) { index ->
                    val project = projects[index]
                    RecentProjectItem(
                        project = project,
                        onClick = { onOpenProject(project) }
                    )
                }
            }
        }
    }

    if (showImportDialog) {
        ImportApkForgeDialog(
            onDismiss = { showImportDialog = false },
            onImportJson = { raw ->
                val imported = onImportProjectJson(raw)
                if (imported != null) {
                    showImportDialog = false
                    Toast.makeText(context, "تم استيراد مشروع ${imported.name} بنجاح ✓", Toast.LENGTH_SHORT).show()
                    onOpenProject(imported)
                } else {
                    Toast.makeText(context, "صيغة ملف .apkforge غير صالحة", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showUrlScraperDialog) {
        SmartUrlScraperDialog(
            onDismiss = { showUrlScraperDialog = false },
            onCreateFromUrl = { urlProject ->
                showUrlScraperDialog = false
                onUseTemplate(urlProject)
            }
        )
    }
}

@Composable
private fun DashboardActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    GlassmorphismCard(
        modifier = modifier
            .height(154.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        backgroundColor = DarkSurfaceElevated,
        borderColor = GlassBorderSubtle,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF261A14))
                    .border(1.dp, OrangePrimary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontFamily = TajawalFontFamily,
                fontSize = 11.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontFamily = CairoFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun RecentProjectItem(
    project: AppProject,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RealAppIconAvatar(
                    project = project,
                    size = 44.dp,
                    cornerRadius = 12.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = project.name,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = project.packageConfig.packageName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
