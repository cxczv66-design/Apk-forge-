package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppProject
import com.example.domain.model.SmartAppTemplate
import com.example.domain.model.SmartTemplatesCatalog
import com.example.ui.components.ImportApkForgeDialog
import com.example.ui.components.OmniSupport3dButton
import com.example.ui.components.OmniSupportDialog
import com.example.ui.components.RealAppIconAvatar
import com.example.ui.components.SmartUrlScraperDialog
import com.example.ui.components.space.*
import com.example.ui.theme.*
import com.example.ui.util.AppStrings
import com.example.ui.util.LocalAppLanguage

private data class HeroSlideSpec(
    val title: String,
    val subtitle: String,
    val ctaLabel: String,
    val badgeText: String
)

/**
 * الشاشة الرئيسية (ApkForge Futuristic Space Home) مطابقة للهوية المرجعية:
 * - ترويسة شعار 3D + زر القائمة الجانبية + بطاقة Quantum Engine النشطة
 * - بانر هيرو بالروبوت الفضائي ثلاثي الأبعاد والكرة الهولوغرافية والسفينة الفضائية + زر "ابدأ الآن"
 * - استيراد ملف أو مصدر (HTML / ZIP / APK)
 * - بطاقتا مساعد الذكاء الاصطناعي (أرجواني 3D) وبناء APK مباشر (ذهبي متوهج)
 * - شريط الوصول السريع + القوالب المميزة + المشاريع الأخيرة
 * - زر المساعد الفضائي العائم ("أحتاج إلى مساعدة؟") + شريط التنقل السفلي المستقبلي
 */
@Composable
fun DashboardScreen(
    projects: List<AppProject>,
    onOpenHtmlPicker: () -> Unit,
    onOpenZipPicker: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenProject: (AppProject) -> Unit,
    onDeleteProject: (String) -> Unit = {},
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
    var showAllTemplatesDialog by remember { mutableStateOf(false) }
    var showAllProjectsDialog by remember { mutableStateOf(false) }
    var heroSlideIndex by remember { mutableIntStateOf(0) }

    val heroSlides = remember {
        listOf(
            HeroSlideSpec(
                title = "استيراد موقع ذكي",
                subtitle = "قم بتحليل أي موقع وإنشاء تطبيق احترافي مع معاينة مباشرة",
                ctaLabel = "ابدأ الآن",
                badgeText = "PWA Auto-Scraper"
            ),
            HeroSlideSpec(
                title = "مساعد البناء الذكي",
                subtitle = "ولّد تطبيق أندرويد متكامل بمراحله التكيفية عبر الذكاء الاصطناعي",
                ctaLabel = "ابدأ بالذكاء",
                badgeText = "AI Architect"
            ),
            HeroSlideSpec(
                title = "بناء وتوقيع APK",
                subtitle = "حزم AAB وAPK موقعة بشهادة رقمية وجاهزة للنشر الفوري",
                ctaLabel = "معاينة وبناء",
                badgeText = "Quantum Build"
            ),
            HeroSlideSpec(
                title = "استوديو المراحل 3D",
                subtitle = "خصّص الأيقونة وشاشات الترحيب والقوائم مع محاكي هاتف حي",
                ctaLabel = "افتح الاستوديو",
                badgeText = "Omni Studio"
            ),
            HeroSlideSpec(
                title = "استيراد ومزج المشاريع",
                subtitle = "حوّل ملفات HTML وZIP وAPK إلى تطبيقات عصرية متكاملة",
                ctaLabel = "استيراد الآن",
                badgeText = "Multi-Source"
            )
        )
    }

    val navItems = remember(onOpenAiWizard, onOpenAppSetup, onOpenDownloads, onOpenAccount) {
        defaultSpaceBottomNavItems(
            onHome = {},
            onAiWizard = onOpenAiWizard,
            onBuildStudio = onOpenAppSetup,
            onDownloads = onOpenDownloads,
            onAccount = onOpenAccount
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            DashboardSpaceTopBar(
                onOpenSidebar = onOpenSidebar,
                onOpenAccount = onOpenAccount,
                onOpenQuantumDiagnostic = onOpenQuantumDiagnostic,
                lang = lang
            )
        },
        bottomBar = {
            FuturisticBottomBar(
                items = navItems,
                selectedId = "dashboard",
                tagline = "مع ApkForge .. أطلق إبداعك بلا حدود"
            )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = SpaceDimensions.MaxContentWidth)
                    .padding(horizontal = 16.dp)
                    .testTag("dashboard_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 92.dp)
            ) {
                // 1) بانر الهيرو الفضائي مع الروبوت ثلاثي الأبعاد والكرة الهولوغرافية
                item {
                    val activeSlide = heroSlides[heroSlideIndex.coerceIn(0, heroSlides.lastIndex)]
                    val triggerSlideAction: () -> Unit = {
                        when (heroSlideIndex) {
                            1 -> onOpenAiWizard()
                            2 -> onDirectApkBuild()
                            3 -> onOpenAppSetup()
                            4 -> onOpenHtmlPicker()
                            else -> showUrlScraperDialog = true
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonGlassCard(
                            accentColor = SpaceColors.NeonCyan,
                            secondaryAccent = SpaceColors.NeonPurple,
                            shape = RoundedCornerShape(24.dp),
                            elevation = 18.dp,
                            glassAlpha = 0.78f,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 196.dp)
                            ) {
                                // الخلفية المرسومة للروبوت الفضائي والكرة الهولوغرافية والسفينة
                                SpaceHeroBannerIllustration(
                                    modifier = Modifier.matchParentSize()
                                )

                                // تدرج شفاف ناعم لضمان وضوح النص فوق الرسم
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    SpaceColors.DeepSpace.copy(alpha = 0.84f),
                                                    SpaceColors.MidnightBlue.copy(alpha = 0.45f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // مساحة النصوص وزر البداية
                                    Column(
                                        modifier = Modifier.fillMaxWidth(0.62f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = SpaceColors.NeonCyan.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(50),
                                            border = BorderStroke(1.dp, SpaceColors.NeonCyan.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = activeSlide.badgeText,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = SpaceColors.NeonCyan,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                            )
                                        }

                                        AnimatedContent(
                                            targetState = activeSlide,
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                            label = "hero_text_slide"
                                        ) { slide ->
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = slide.title,
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 20.sp,
                                                    color = SpaceColors.NeonCyan
                                                )
                                                Text(
                                                    text = slide.subtitle,
                                                    fontFamily = TajawalFontFamily,
                                                    fontSize = 12.sp,
                                                    lineHeight = 18.sp,
                                                    color = SpaceColors.White.copy(alpha = 0.88f),
                                                    maxLines = 3
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(2.dp))

                                        NeonButton(
                                            text = activeSlide.ctaLabel,
                                            icon = Icons.Default.PlayArrow,
                                            style = NeonButtonStyle.PRIMARY_GLOW,
                                            height = 48.dp,
                                            shape = RoundedCornerShape(50),
                                            testTag = "smart_url_scraper_card",
                                            onClick = triggerSlideAction
                                        )
                                    }

                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        // مؤشر الشرائح التفاعلي أسفل الهيرو
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            heroSlides.indices.forEach { idx ->
                                val isCurrent = idx == heroSlideIndex
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .height(6.dp)
                                        .width(if (isCurrent) 24.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) SpaceColors.NeonCyan
                                            else SpaceColors.MutedText.copy(alpha = 0.35f)
                                        )
                                        .clickable { heroSlideIndex = idx }
                                )
                            }
                        }
                    }
                }

                // 2) لوحة استيراد ملف أو مصدر (HTML / ZIP / APK)
                item {
                    NeonGlassCard(
                        accentColor = SpaceColors.NeonCyan,
                        secondaryAccent = SpaceColors.NeonPurple,
                        shape = RoundedCornerShape(22.dp),
                        elevation = 14.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(2.dp)
                                        .background(SpaceColors.NeonCyan.copy(alpha = 0.7f), CircleShape)
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = SpaceColors.NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "استيراد ملف أو مصدر",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = SpaceColors.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(2.dp)
                                        .background(SpaceColors.NeonPurple.copy(alpha = 0.7f), CircleShape)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SpaceCommandSourceCard(
                                    title = "HTML",
                                    subtitle = "استيراد موقع ويب",
                                    icon = Icons.Default.Code,
                                    accentColor = SpaceColors.ElectricBlue,
                                    secondaryAccent = SpaceColors.NeonPurple,
                                    tag = "html_card",
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenHtmlPicker
                                )
                                SpaceCommandSourceCard(
                                    title = "ملف مضغوط",
                                    subtitle = "فتح مشروع كامل",
                                    icon = Icons.Default.FolderZip,
                                    accentColor = SpaceColors.NeonCyan,
                                    secondaryAccent = SpaceColors.ElectricBlue,
                                    tag = "zip_card",
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenZipPicker
                                )
                                SpaceCommandSourceCard(
                                    title = "APK",
                                    subtitle = "تحليل وتعديل تطبيق",
                                    icon = Icons.Default.Android,
                                    accentColor = SpaceColors.Emerald,
                                    secondaryAccent = SpaceColors.NeonCyan,
                                    tag = "apk_card",
                                    modifier = Modifier.weight(1f),
                                    onClick = onOpenApkChoice
                                )
                            }
                        }
                    }
                }

                // 3) البطاقتان المضيئتان: مساعد الذكاء الاصطناعي (أرجواني) + بناء APK مباشر (ذهبي)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonGlassCard(
                            accentColor = SpaceColors.NeonPurple,
                            secondaryAccent = SpaceColors.Violet,
                            shape = RoundedCornerShape(22.dp),
                            elevation = 16.dp,
                            onClick = onOpenAiWizard,
                            testTag = "ai_wizard_entry_card",
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SpaceFloatingRobotAvatar(
                                        size = 48.dp,
                                        accentColor = SpaceColors.NeonCyan,
                                        showOnlineDot = false
                                    )
                                    Surface(
                                        color = SpaceColors.NeonPurple.copy(alpha = 0.28f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SpaceColors.NeonPurple)
                                    ) {
                                        Text(
                                            text = "AI",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = SpaceColors.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "مساعد الذكاء الاصطناعي",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = SpaceColors.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "صمم تطبيقك بسهولة بالذكاء",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 10.sp,
                                            color = SpaceColors.MutedText,
                                            maxLines = 2,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    SpaceCircleArrowPill(accentColor = SpaceColors.NeonPurple)
                                }
                            }
                        }

                        NeonGlassCard(
                            accentColor = SpaceColors.Gold,
                            secondaryAccent = Color(0xFFFF8C00),
                            shape = RoundedCornerShape(22.dp),
                            elevation = 16.dp,
                            onClick = onDirectApkBuild,
                            testTag = "direct_apk_build_btn",
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Space3DFeatureBadge(
                                        icon = Icons.Default.RocketLaunch,
                                        accentColor = SpaceColors.Gold,
                                        secondaryColor = Color(0xFFFF8C00),
                                        badgeLabel = "APK",
                                        size = 48.dp
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = SpaceColors.Gold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "بناء APK مباشر",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            color = SpaceColors.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "إنشاء وتوقيع تطبيقك الآن",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 10.sp,
                                            color = SpaceColors.MutedText,
                                            maxLines = 2,
                                            lineHeight = 14.sp
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    SpaceCircleArrowPill(accentColor = SpaceColors.Gold)
                                }
                            }
                        }
                    }
                }

                // 4) شريط الوصول السريع (التنزيلات / الاستوديو / المعاينة 3D)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpaceQuickTile(
                            label = AppStrings.downloads(lang),
                            icon = Icons.Default.Download,
                            accent = SpaceColors.Emerald,
                            tag = "dashboard_downloads_btn",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenDownloads
                        )
                        SpaceQuickTile(
                            label = "الاستوديو",
                            icon = Icons.Default.Tune,
                            accent = SpaceColors.ElectricBlue,
                            tag = "dashboard_app_setup_btn",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenAppSetup
                        )
                        SpaceQuickTile(
                            label = "المعاينة 3D",
                            icon = Icons.Default.Smartphone,
                            accent = SpaceColors.NeonCyan,
                            tag = "dashboard_live_sandbox_btn",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenLiveSandbox
                        )
                    }
                }

                // 5) قسم القوالب المميزة (3D Featured Templates) + استيراد .apkforge
                item {
                    NeonGlassCard(
                        accentColor = SpaceColors.ElectricBlue,
                        secondaryAccent = SpaceColors.Gold,
                        shape = RoundedCornerShape(22.dp),
                        elevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumSectionHeader(
                                title = "قوالب مميزة",
                                icon = Icons.Default.Star,
                                accentColor = SpaceColors.Gold,
                                actionLabel = "عرض الكل",
                                actionTestTag = "view_all_templates_btn",
                                onActionClick = { showAllTemplatesDialog = true }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    color = SpaceColors.NeonCyan.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SpaceColors.NeonCyan.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showImportDialog = true }
                                        .testTag("import_apkforge_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = "استيراد .apkforge",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SpaceColors.NeonCyan
                                        )
                                    }
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(templates, key = { it.id }) { tpl ->
                                    SpaceTemplateTile(
                                        template = tpl,
                                        onClick = { onUseTemplate(tpl.buildProject()) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6) قسم المشاريع الأخيرة
                item {
                    PremiumSectionHeader(
                        title = AppStrings.myRecentProjects(lang),
                        icon = Icons.Default.History,
                        accentColor = SpaceColors.NeonCyan,
                        actionLabel = if (projects.isNotEmpty()) "عرض الكل (${projects.size})" else null,
                        actionTestTag = "view_all_projects_btn",
                        onActionClick = if (projects.isNotEmpty()) ({ showAllProjectsDialog = true }) else null
                    )
                }

                if (projects.isEmpty()) {
                    item {
                        NeonGlassCard(
                            accentColor = SpaceColors.NeonPurple,
                            secondaryAccent = SpaceColors.NeonCyan,
                            elevation = 10.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Space3DFeatureBadge(
                                    icon = Icons.Default.FolderOpen,
                                    accentColor = SpaceColors.NeonCyan,
                                    secondaryColor = SpaceColors.NeonPurple,
                                    size = 54.dp
                                )
                                Text(
                                    text = AppStrings.noProjectsTitle(lang),
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = SpaceColors.White
                                )
                                Text(
                                    text = AppStrings.noProjectsDesc(lang),
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = SpaceColors.MutedText,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                NeonButton(
                                    text = "ابدأ بمساعد الذكاء الاصطناعي ✨",
                                    icon = Icons.Default.AutoAwesome,
                                    style = NeonButtonStyle.PURPLE_AI,
                                    onClick = onOpenAiWizard
                                )
                            }
                        }
                    }
                } else {
                    items(projects, key = { it.id }) { project ->
                        SpaceRecentProjectCard(
                            project = project,
                            onClick = { onOpenProject(project) },
                            onDelete = { onDeleteProject(project.id) }
                        )
                    }
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

    if (showAllTemplatesDialog) {
        SpaceDialog(
            title = "كل القوالب الذكية (${templates.size})",
            accentColor = SpaceColors.Gold,
            onDismissRequest = { showAllTemplatesDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                templates.forEach { tpl ->
                    val tplColor = parseColorSafe(tpl.accentHex, SpaceColors.NeonCyan)
                    NeonGlassCard(
                        accentColor = tplColor,
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            showAllTemplatesDialog = false
                            onUseTemplate(tpl.buildProject())
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Space3DFeatureBadge(
                                icon = resolveTemplateIcon(tpl.iconKey),
                                accentColor = tplColor,
                                size = 42.dp
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tpl.titleAr,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = SpaceColors.White
                                )
                                Text(
                                    text = tpl.subtitleAr,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = SpaceColors.MutedText,
                                    maxLines = 2
                                )
                            }
                            SpaceCircleArrowPill(accentColor = tplColor)
                        }
                    }
                }
            }
        }
    }

    if (showAllProjectsDialog) {
        SpaceDialog(
            title = "كل المشاريع (${projects.size})",
            accentColor = SpaceColors.NeonCyan,
            onDismissRequest = { showAllProjectsDialog = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                projects.forEach { proj ->
                    SpaceRecentProjectCard(
                        project = proj,
                        onClick = {
                            showAllProjectsDialog = false
                            onOpenProject(proj)
                        },
                        onDelete = { onDeleteProject(proj.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardSpaceTopBar(
    onOpenSidebar: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenQuantumDiagnostic: () -> Unit,
    lang: com.example.ui.util.AppLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NeonIconButton(
                icon = Icons.Default.Menu,
                contentDescription = "قائمة الاستوديو",
                onClick = onOpenSidebar,
                tint = SpaceColors.NeonCyan,
                testTag = "open_studio_sidebar_btn"
            )
            NeonIconButton(
                icon = Icons.Default.AccountCircle,
                contentDescription = AppStrings.account(lang),
                onClick = onOpenAccount,
                tint = SpaceColors.NeonPurple,
                testTag = "dashboard_account_btn"
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpaceApkForgeLogoMark(size = 38.dp)
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = AppStrings.appTitle(lang),
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            listOf(SpaceColors.White, SpaceColors.NeonCyan, SpaceColors.NeonPurple)
                        ),
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = 19.sp
                    )
                )
                Text(
                    text = "Build • Edit • Create",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = SpaceColors.MutedText
                )
            }
        }

        QuantumStatusCard(
            compact = true,
            isActive = true,
            onClick = onOpenQuantumDiagnostic,
            testTag = "quantum_status_pill"
        )
    }
}

@Composable
private fun SpaceCommandSourceCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    secondaryAccent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NeonGlassCard(
        accentColor = accentColor,
        secondaryAccent = secondaryAccent,
        shape = RoundedCornerShape(18.dp),
        elevation = 12.dp,
        onClick = onClick,
        testTag = tag,
        modifier = modifier.heightIn(min = 132.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Space3DFeatureBadge(
                icon = icon,
                accentColor = accentColor,
                secondaryColor = secondaryAccent,
                size = 44.dp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = SpaceColors.White,
                    maxLines = 1
                )
                SpaceCircleArrowPill(accentColor = accentColor, size = 18.dp)
            }
            Text(
                text = subtitle,
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = SpaceColors.MutedText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpaceQuickTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    NeonGlassCard(
        accentColor = accent,
        secondaryAccent = SpaceColors.ElectricBlue,
        shape = RoundedCornerShape(15.dp),
        elevation = 8.dp,
        onClick = onClick,
        testTag = tag,
        modifier = modifier.heightIn(min = 50.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
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
                color = SpaceColors.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpaceTemplateTile(
    template: SmartAppTemplate,
    onClick: () -> Unit
) {
    val tplColor = parseColorSafe(template.accentHex, SpaceColors.NeonCyan)
    val tplSecondary = parseColorSafe(template.secondaryHex, SpaceColors.NeonPurple)
    val tplIcon = resolveTemplateIcon(template.iconKey)

    NeonGlassCard(
        accentColor = tplColor,
        secondaryAccent = tplSecondary,
        shape = RoundedCornerShape(18.dp),
        elevation = 12.dp,
        onClick = onClick,
        testTag = "template_${template.id}",
        modifier = Modifier.width(148.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Space3DFeatureBadge(
                    icon = tplIcon,
                    accentColor = tplColor,
                    secondaryColor = tplSecondary,
                    size = 38.dp
                )
                Surface(
                    color = tplColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, tplColor.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = template.badgeAr,
                        fontFamily = CairoFontFamily,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = tplColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = template.titleAr,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                color = SpaceColors.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${template.featureCountLabel} • تكيفي",
                fontFamily = TajawalFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                color = SpaceColors.MutedText,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpaceRecentProjectCard(
    project: AppProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = parseColorSafe(project.accentColorHex, SpaceColors.NeonCyan)
    NeonGlassCard(
        accentColor = accent,
        secondaryAccent = SpaceColors.NeonPurple,
        shape = RoundedCornerShape(20.dp),
        elevation = 12.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RealAppIconAvatar(
                project = project,
                size = 50.dp,
                cornerRadius = 15.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = SpaceColors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = project.packageConfig.packageName,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = SpaceColors.MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "حذف المشروع",
                    tint = SpaceColors.MutedText.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }

            NeonButton(
                text = "فتح 🚀",
                onClick = onClick,
                style = NeonButtonStyle.PURPLE_AI,
                height = 40.dp,
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
private fun SpaceCircleArrowPill(
    accentColor: Color,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, CircleShape, spotColor = accentColor)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.24f))
            .border(1.dp, accentColor.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = SpaceColors.White,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

private fun resolveTemplateIcon(iconKey: String): ImageVector = when (iconKey) {
    "store" -> Icons.Default.ShoppingCart
    "restaurant" -> Icons.Default.Restaurant
    "portfolio" -> Icons.Default.Workspaces
    "blog" -> Icons.Default.Article
    "academy" -> Icons.Default.School
    "game" -> Icons.Default.SportsEsports
    else -> Icons.Default.EventAvailable
}
