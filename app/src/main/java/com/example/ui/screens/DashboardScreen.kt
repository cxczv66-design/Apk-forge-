package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.AppProject
import com.example.domain.model.SmartTemplatesCatalog
import com.example.ui.components.ImportApkForgeDialog
import com.example.ui.components.OmniSupport3dButton
import com.example.ui.components.OmniSupportDialog
import com.example.ui.components.RealAppIconAvatar
import com.example.ui.components.SmartUrlScraperDialog
import com.example.ui.components.ThreeDIconBadge
import com.example.ui.components.ThreeDSurfaceCard
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.LocalAppLanguage

/**
 * الشاشة الرئيسية (The Command Center) بنظام Clean 3D UI والذكاء التكيفي:
 * - استيراد موقع (PWA Auto-Scraper)
 * - استيراد ملف (HTML / ZIP / APK)
 * - قوالب جاهزة 3D متنوعة
 * - زر بناء APK مباشر + بوابة مساعد الذكاء الاصطناعي
 * - التنزيلات والمشاريع الأخيرة + الدعم الفني العائم 3D
 */
@Composable
fun DashboardScreen(
    projects: List<AppProject>,
    onOpenHtmlPicker: () -> Unit,
    onOpenZipPicker: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenProject: (AppProject) -> Unit,
    onOpenQuantumDiagnostic: () -> Unit,
    onUseTemplate: (AppProject) -> Unit = {},
    onImportProjectJson: (String) -> AppProject? = { null },
    onOpenSidebar: () -> Unit = {},
    onOpenAppSetup: () -> Unit = {},
    onOpenLiveSandbox: () -> Unit = {},
    onOpenApkChoice: () -> Unit = {},
    onOpenAiWizard: () -> Unit = {},
    onDirectApkBuild: () -> Unit = {},
    onOpenSupport: () -> Unit = {}
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val templates = remember { SmartTemplatesCatalog.getAll() }
    var showImportDialog by remember { mutableStateOf(false) }
    var showUrlScraperDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenSidebar, modifier = Modifier.testTag("open_studio_sidebar_btn")) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "قائمة الاستوديو",
                            tint = TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = onOpenAccount) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = AppStrings.account(lang),
                            tint = OrangePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.apkforge_brand_logo_1794592633199),
                        contentDescription = "ApkForge Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.2.dp, OrangePrimary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    )
                    Column {
                        Text(
                            text = AppStrings.appTitle(lang),
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Omni-Adaptive Studio",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = CyanAccent
                        )
                    }
                }

                IconButton(onClick = onOpenQuantumDiagnostic) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = AppStrings.quantumEngineStatus(lang),
                        tint = OrangePrimary
                    )
                }
            }
        },
        floatingActionButton = {
            OmniSupport3dButton(
                onClick = {
                    showSupportDialog = true
                    onOpenSupport()
                }
            )
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 92.dp)
        ) {
            // 1) بطاقة استيراد موقع (PWA Auto-Scraper) 3D
            item {
                ThreeDSurfaceCard(
                    accentColor = Color(0xFF38BDF8),
                    onClick = { showUrlScraperDialog = true },
                    testTag = "smart_url_scraper_card"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ThreeDIconBadge(
                            icon = Icons.Default.Language,
                            accentColor = Color(0xFF38BDF8),
                            secondaryColor = Color(0xFF818CF8),
                            size = 52.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "استيراد موقع ذكي (PWA Auto-Scraper) 🌐",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "تحليل تلقائي للصور والأزرار وتوليد مراحل تكيفية مع المعاينة الحية",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 2) بطاقات استيراد ملف: HTML / ZIP / APK (3D Interactive Cards)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "استيراد ملف أو مصدر",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CommandSource3dCard(
                            title = "HTML",
                            subtitle = "صفحة أو مجلد",
                            icon = Icons.Default.Code,
                            accentColor = OrangePrimary,
                            tag = "html_card",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenHtmlPicker
                        )
                        CommandSource3dCard(
                            title = "ZIP",
                            subtitle = "مشروع مضغوط",
                            icon = Icons.Default.FolderZip,
                            accentColor = Color(0xFF818CF8),
                            tag = "zip_card",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenZipPicker
                        )
                        CommandSource3dCard(
                            title = "APK",
                            subtitle = "تعديل أو AI",
                            icon = Icons.Default.Android,
                            accentColor = StatusGreen,
                            tag = "apk_card",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenApkChoice
                        )
                    }
                }
            }

            // 3) زر بناء APK مباشر + بوابة مساعد الذكاء الاصطناعي من الصفر
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThreeDSurfaceCard(
                        accentColor = OrangePrimary,
                        cornerRadius = 18.dp,
                        onClick = onDirectApkBuild,
                        testTag = "direct_apk_build_btn",
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThreeDIconBadge(
                                icon = Icons.Default.RocketLaunch,
                                accentColor = OrangePrimary,
                                secondaryColor = OrangeNeon,
                                size = 42.dp,
                                cornerRadius = 12.dp
                            )
                            Text(
                                text = "بناء APK مباشر ⚡",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "الانتقال السريع للمعاينة 3D والبناء الفوري",
                                fontFamily = TajawalFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                    }

                    ThreeDSurfaceCard(
                        accentColor = Color(0xFFA855F7),
                        cornerRadius = 18.dp,
                        onClick = onOpenAiWizard,
                        testTag = "ai_wizard_entry_card",
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThreeDIconBadge(
                                icon = Icons.Default.AutoAwesome,
                                accentColor = Color(0xFFA855F7),
                                secondaryColor = Color(0xFF38BDF8),
                                size = 42.dp,
                                cornerRadius = 12.dp
                            )
                            Text(
                                text = "مساعد AI Wizard ✨",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "اصنع تطبيقاً من الصفر بخطة +12 مرحلة",
                                fontFamily = TajawalFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // 4) شريط التنزيلات والإعدادات والمعاينة التفاعلية
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CommandQuickTile(
                        label = AppStrings.downloads(lang),
                        icon = Icons.Default.Download,
                        accent = StatusGreen,
                        modifier = Modifier.weight(1f),
                        tag = "dashboard_downloads_btn",
                        onClick = onOpenDownloads
                    )
                    CommandQuickTile(
                        label = "الاستوديو",
                        icon = Icons.Default.Tune,
                        accent = OrangePrimary,
                        modifier = Modifier.weight(1f),
                        tag = "dashboard_app_setup_btn",
                        onClick = onOpenAppSetup
                    )
                    CommandQuickTile(
                        label = "المعاينة 3D",
                        icon = Icons.Default.Smartphone,
                        accent = CyanAccent,
                        modifier = Modifier.weight(1f),
                        tag = "dashboard_live_sandbox_btn",
                        onClick = onOpenLiveSandbox
                    )
                }
            }

            // 5) قوالب جاهزة بتصميم 3D
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قوالب جاهزة 3D ⚡",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Surface(
                            color = OrangePrimary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
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

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(templates, key = { it.id }) { tpl ->
                            val tplColor = parseColorSafe(tpl.accentHex, OrangePrimary)
                            val tplSecondary = parseColorSafe(tpl.secondaryHex, CyanAccent)
                            val tplIcon = when (tpl.iconKey) {
                                "store" -> Icons.Default.ShoppingBag
                                "restaurant" -> Icons.Default.Restaurant
                                "portfolio" -> Icons.Default.Workspaces
                                "blog" -> Icons.Default.Article
                                "academy" -> Icons.Default.School
                                else -> Icons.Default.EventAvailable
                            }
                            ThreeDSurfaceCard(
                                accentColor = tplColor,
                                cornerRadius = 20.dp,
                                elevation = 12.dp,
                                onClick = { onUseTemplate(tpl.buildProject()) },
                                testTag = "template_${tpl.id}",
                                modifier = Modifier.width(182.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ThreeDIconBadge(
                                            icon = tplIcon,
                                            accentColor = tplColor,
                                            secondaryColor = tplSecondary,
                                            size = 38.dp,
                                            cornerRadius = 11.dp
                                        )
                                        Surface(
                                            color = tplColor.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = tpl.badgeAr,
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tplColor,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = tpl.titleAr,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tpl.subtitleAr,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 2,
                                        lineHeight = 14.sp
                                    )
                                    Text(
                                        text = "${tpl.featureCountLabel} • تكيفي",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = tplColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6) المشاريع الأخيرة
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.myRecentProjects(lang),
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    if (projects.isNotEmpty()) {
                        Text(
                            text = "${projects.size} مشاريع",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (projects.isEmpty()) {
                item {
                    ThreeDSurfaceCard(accentColor = GlassBorderSubtle, elevation = 6.dp) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThreeDIconBadge(
                                icon = Icons.Default.FolderOpen,
                                accentColor = OrangePrimary,
                                size = 54.dp,
                                cornerRadius = 18.dp
                            )
                            Text(
                                text = AppStrings.noProjectsTitle(lang),
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = AppStrings.noProjectsDesc(lang),
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onOpenAiWizard,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "ابدأ بمساعد الذكاء الاصطناعي ✨",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
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

    if (showSupportDialog) {
        OmniSupportDialog(
            activeProjectName = projects.firstOrNull()?.name ?: "ApkForge Omni IDE",
            onDismiss = { showSupportDialog = false }
        )
    }
}

@Composable
private fun CommandSource3dCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ThreeDSurfaceCard(
        accentColor = accentColor,
        cornerRadius = 18.dp,
        elevation = 10.dp,
        onClick = onClick,
        testTag = tag,
        modifier = modifier.height(124.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ThreeDIconBadge(
                icon = icon,
                accentColor = accentColor,
                size = 44.dp,
                cornerRadius = 14.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CommandQuickTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentProjectItem(
    project: AppProject,
    onClick: () -> Unit
) {
    val accent = parseColorSafe(project.accentColorHex, OrangePrimary)
    ThreeDSurfaceCard(
        accentColor = accent,
        cornerRadius = 16.dp,
        elevation = 8.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                RealAppIconAvatar(
                    project = project,
                    size = 46.dp,
                    cornerRadius = 13.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = project.name,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = project.packageConfig.packageName,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
