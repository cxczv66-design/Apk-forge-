package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.AppProject
import com.example.domain.model.ProStudioConfig
import com.example.engine.*
import com.example.ui.components.space.NeonButton
import com.example.ui.components.space.NeonButtonStyle
import com.example.ui.components.space.NeonGlassCard
import com.example.ui.components.space.NeonProgressIndicator
import com.example.ui.components.space.NeonTextField
import com.example.ui.components.space.Space3DFeatureBadge
import com.example.ui.components.space.SpaceDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * نافذة جلب هوية أي موقع تلقائياً (Smart URL / PWA Auto-Scraper)
 */
@Composable
fun SmartUrlScraperDialog(
    onDismiss: () -> Unit,
    onCreateFromUrl: (AppProject) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scraper = remember { UrlScraperEngine() }

    var urlInput by remember { mutableStateOf("https://") }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var scraped by remember { mutableStateOf<ScrapedSiteIdentity?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SpaceDialog(
            onDismissRequest = onDismiss,
            title = "تحليل واستخراج هوية الموقع الحقيقية 🌐",
            subtitle = "يجلب الاسم الفعلي، الشعار، الألوان، الأزرار، وأذونات الأجهزة مباشرة من خادم الموقع.",
            icon = Icons.Default.AutoFixHigh,
            accentColor = SpaceColors.NeonCyan,
            secondaryAccent = SpaceColors.NeonPurple,
            widthFraction = 0.94f
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeonTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        scanError = null
                    },
                    label = "رابط الموقع (URL)",
                    placeholder = "https://example.com",
                    leadingIcon = Icons.Default.Language,
                    accentColor = SpaceColors.NeonCyan,
                    singleLine = true,
                    testTag = "smart_url_input"
                )

                NeonButton(
                    text = if (isScanning) "جارٍ الجلب الفعلي للبيانات والأيقونة..." else "تحليل واستخراج حقيقي للموقع ⚡",
                    onClick = {
                        scope.launch {
                            isScanning = true
                            scanError = null
                            val res = scraper.scrapeSiteIdentity(urlInput, context)
                            isScanning = false
                            res.onSuccess { scraped = it }
                                .onFailure { err ->
                                    scraped = null
                                    scanError = err.message ?: "تعذر الاتصال بالموقع أو تحليله"
                                    Toast.makeText(context, "فشل التحليل: $scanError", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    enabled = !isScanning && urlInput.trim().length > 4,
                    isLoading = isScanning,
                    icon = Icons.Default.TravelExplore,
                    style = NeonButtonStyle.PRIMARY_GLOW,
                    modifier = Modifier.fillMaxWidth()
                )

                if (scanError != null) {
                    NeonGlassCard(
                        accentColor = SpaceColors.ErrorRed,
                        secondaryAccent = SpaceColors.NeonPurple,
                        shape = RoundedCornerShape(14.dp),
                        elevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✗ تعذر استخراج بيانات الموقع: $scanError\nتأكد من صحة الرابط وتوفر الاتصال بالإنترنت.",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = SpaceColors.ErrorRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                scraped?.let { id ->
                    val themeColor = parseColorSafe(id.themeColorHex, SpaceColors.NeonCyan)
                    val effectiveIcon = id.localCachedIconUri ?: id.iconUrl
                    NeonGlassCard(
                        accentColor = themeColor,
                        secondaryAccent = SpaceColors.NeonPurple,
                        shape = RoundedCornerShape(16.dp),
                        elevation = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(themeColor.copy(alpha = 0.25f))
                                        .border(1.2.dp, themeColor.copy(alpha = 0.7f), RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!effectiveIcon.isNullOrBlank()) {
                                        AsyncImage(
                                            model = effectiveIcon,
                                            contentDescription = id.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(id.title.take(2).uppercase(), color = SpaceColors.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(id.title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SpaceColors.White)
                                    Text(id.suggestedPackageName, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = themeColor)
                                    Text(
                                        text = "HTTP ${id.httpStatusCode} • ${if (id.hasPwaManifest) "PWA Manifest ✓" else "HTML Meta ✓"} • ${id.themeColorHex}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = SpaceColors.Emerald
                                    )
                                }
                            }

                            Text(
                                text = id.description,
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = SpaceColors.MutedText,
                                maxLines = 2
                            )

                            Text(
                                text = "العناصر المستخرجة: ${id.detectedLinks.size} روابط تنقل • ${id.detectedButtons.size} أزرار • ${id.formsCount} نماذج • ${id.scriptsCount} سكربت",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = SpaceColors.Gold
                            )

                            if (id.detectedLinks.isNotEmpty()) {
                                Text("روابط القوائم المكتشفة:", fontFamily = CairoFontFamily, fontSize = 11.sp, color = SpaceColors.White)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    id.detectedLinks.forEach { link ->
                                        Surface(
                                            color = SpaceColors.SurfaceDeep.copy(alpha = 0.85f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "${link.title} (${link.relativePath})",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 10.sp,
                                                color = SpaceColors.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (id.detectedButtons.isNotEmpty()) {
                                Text("الأزرار التفاعلية المكتشفة:", fontFamily = CairoFontFamily, fontSize = 11.sp, color = SpaceColors.White)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    id.detectedButtons.forEach { btnLabel ->
                                        Surface(
                                            color = themeColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = btnLabel,
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.sp,
                                                color = themeColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (id.detectedPermissions.isNotEmpty() || id.detectedExternalSchemes.isNotEmpty()) {
                                val permsSummary = (id.detectedPermissions + id.detectedExternalSchemes).joinToString(" • ")
                                Text(
                                    text = "الأذونات والروابط الخارجية المطلوبة: $permsSummary",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = SpaceColors.Emerald
                                )
                            }
                        }
                    }

                    var showPwaEditor by remember { mutableStateOf(false) }

                    NeonButton(
                        text = "تعبئة جميع البيانات الحقيقية والبدء ⚡",
                        onClick = {
                            val proj = scraper.toPreConfiguredProject(id, context)
                            onCreateFromUrl(proj)
                        },
                        icon = Icons.Default.Bolt,
                        style = NeonButtonStyle.GOLD_ENGINE,
                        modifier = Modifier.fillMaxWidth()
                    )

                    NeonButton(
                        text = "تحويل لمشروع محلي وتعديل العناصر (PWA) ✏️",
                        onClick = { showPwaEditor = true },
                        icon = Icons.Default.Edit,
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showPwaEditor) {
                        val preProj = remember(id.normalizedUrl) { scraper.toPreConfiguredProject(id, context) }
                        PwaSiteCustomizerDialog(
                            initialProject = preProj,
                            scrapedIdentity = id,
                            onDismiss = { showPwaEditor = false },
                            onApplyAndSave = { edited ->
                                showPwaEditor = false
                                onCreateFromUrl(edited)
                            },
                            onApplyAndPreview = { edited ->
                                showPwaEditor = false
                                onCreateFromUrl(edited)
                            }
                        )
                    }
                }

                NeonButton(
                    text = "إغلاق",
                    onClick = onDismiss,
                    style = NeonButtonStyle.CYBER_OUTLINE,
                    height = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * بطاقة موحدة وأنيقة لأدوات الاستوديو الاحترافي الـ 7 داخل مركز البناء
 */
@Composable
fun ProStudioHubCard(
    project: AppProject,
    onOpenWebCustomizer: () -> Unit,
    onOpenSecurityAndBridgeV2: () -> Unit,
    onOpenIconAndUiStudio: () -> Unit,
    onOpenGitHubCloudTrigger: () -> Unit
) {
    val pro = project.proConfig
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NeonGlassCard(
            accentColor = SpaceColors.Gold,
            secondaryAccent = SpaceColors.NeonPurple,
            shape = RoundedCornerShape(20.dp),
            elevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Space3DFeatureBadge(
                            icon = Icons.Default.Diamond,
                            accentColor = SpaceColors.Gold,
                            secondaryColor = SpaceColors.NeonPurple,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الاستوديو الاحترافي (Pro Studio Suite)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = SpaceColors.White
                            )
                            Text(
                                text = "حاقن CSS • مانع إعلانات • الأمان • البصمة/QR • الأيقونات • GitHub Cloud",
                                fontFamily = TajawalFontFamily,
                                fontSize = 10.sp,
                                color = SpaceColors.MutedText
                            )
                        }
                    }
                }

                // 2x2 Clean Grid of Smart Action Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProTile(
                        title = "تخصيص الويب",
                        subtitle = buildString {
                            append(if (pro.forceDarkCss) "Dark ✓" else "Light")
                            append(" • ")
                            append(if (pro.adAndPopupBlocker) "AdBlock ✓" else "Ads On")
                        },
                        icon = Icons.Default.Style,
                        accent = SpaceColors.NeonCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "pro_web_customizer_tile",
                        onClick = onOpenWebCustomizer
                    )
                    ProTile(
                        title = "الأمان والجسر v2",
                        subtitle = "بصمة=${pro.biometricAuthBridge} • SSL=${pro.sslPinningEnabled}",
                        icon = Icons.Default.Security,
                        accent = SpaceColors.Emerald,
                        modifier = Modifier.weight(1f),
                        testTag = "pro_security_tile",
                        onClick = onOpenSecurityAndBridgeV2
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProTile(
                        title = "الأيقونة وDrawer/FAB",
                        subtitle = "${pro.iconShape} • Drawer=${pro.drawerEnabled} • FAB=${pro.fabMenuEnabled}",
                        icon = Icons.Default.Palette,
                        accent = SpaceColors.NeonPurple,
                        modifier = Modifier.weight(1f),
                        testTag = "pro_icon_ui_tile",
                        onClick = onOpenIconAndUiStudio
                    )
                    ProTile(
                        title = "بناء GitHub فوري",
                        subtitle = "رفع + تشغيل Actions تلقائياً ☁️",
                        icon = Icons.Default.CloudSync,
                        accent = SpaceColors.Gold,
                        modifier = Modifier.weight(1f),
                        testTag = "pro_github_cloud_tile",
                        onClick = onOpenGitHubCloudTrigger
                    )
                }
            }
        }
    }
}

@Composable
private fun ProTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    NeonGlassCard(
        accentColor = accent,
        secondaryAccent = SpaceColors.ElectricBlue,
        shape = RoundedCornerShape(14.dp),
        elevation = 8.dp,
        glassAlpha = 0.78f,
        onClick = onClick,
        testTag = testTag,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Space3DFeatureBadge(
                icon = icon,
                accentColor = accent,
                secondaryColor = SpaceColors.MidnightBlue,
                size = 34.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SpaceColors.White, maxLines = 1)
                Text(subtitle, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = SpaceColors.MutedText, maxLines = 1)
            }
        }
    }
}

@Composable
fun WebCustomizerDialog(
    initial: ProStudioConfig,
    onDismiss: () -> Unit,
    onSave: (ProStudioConfig) -> Unit
) {
    var forceDark by remember { mutableStateOf(initial.forceDarkCss) }
    var adBlocker by remember { mutableStateOf(initial.adAndPopupBlocker) }
    var blockedHosts by remember { mutableStateOf(initial.blockedHostsCsv) }
    var customCss by remember { mutableStateOf(initial.customCss) }
    var customJs by remember { mutableStateOf(initial.customJs) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SpaceDialog(
            onDismissRequest = onDismiss,
            title = "مخصص الويب وحاقن CSS/JS",
            subtitle = "الوضع الليلي + مانع الإعلانات وحقن الأكواد",
            icon = Icons.Default.Style,
            accentColor = SpaceColors.NeonCyan,
            secondaryAccent = SpaceColors.NeonPurple,
            widthFraction = 0.92f
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudioSwitchTile("الوضع الليلي الإجباري (Force Dark CSS)", "يحول أي موقع فاتح إلى تصميم داكن مريح للعين", forceDark) { forceDark = it }
                StudioSwitchTile("حاجب الإعلانات والنوافذ المنبثقة (Ad Blocker)", "يمنع النطاقات الإعلانية أثناء تحميل WebView", adBlocker) { adBlocker = it }

                if (adBlocker) {
                    NeonTextField(
                        value = blockedHosts,
                        onValueChange = { blockedHosts = it },
                        label = "النطاقات المحجوبة (مفصولة بفاصلة)",
                        leadingIcon = Icons.Default.Block,
                        accentColor = SpaceColors.NeonCyan,
                        singleLine = true
                    )
                }

                // Quick CSS Presets
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PresetCssChip("إخفاء Header/Footer") {
                        customCss = "header, footer, .navbar, .cookie-banner { display: none !important; }"
                    }
                    PresetCssChip("خط عربي مريح") {
                        customCss = "body { line-height: 1.8 !important; font-family: sans-serif !important; }"
                    }
                }

                NeonTextField(
                    value = customCss,
                    onValueChange = { customCss = it },
                    label = "CSS مخصص يُحقن داخل الصفحة",
                    accentColor = SpaceColors.NeonPurple,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4
                )

                NeonTextField(
                    value = customJs,
                    onValueChange = { customJs = it },
                    label = "JavaScript مخصص عند اكتمال التحميل",
                    accentColor = SpaceColors.NeonCyan,
                    singleLine = false,
                    minLines = 2,
                    maxLines = 3
                )

                DialogFooterButtons(onDismiss = onDismiss) {
                    onSave(
                        initial.copy(
                            forceDarkCss = forceDark,
                            adAndPopupBlocker = adBlocker,
                            blockedHostsCsv = blockedHosts.trim(),
                            customCss = customCss.trim(),
                            customJs = customJs.trim()
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityAndBridgeV2Dialog(
    initial: ProStudioConfig,
    onDismiss: () -> Unit,
    onSave: (ProStudioConfig) -> Unit
) {
    var biometric by remember { mutableStateOf(initial.biometricAuthBridge) }
    var qrBridge by remember { mutableStateOf(initial.qrScannerBridge) }
    var reviewBridge by remember { mutableStateOf(initial.inAppReviewBridge) }
    var flagSecure by remember { mutableStateOf(initial.flagSecureEnabled) }
    var sslPinning by remember { mutableStateOf(initial.sslPinningEnabled) }
    var preloader by remember { mutableStateOf(initial.assetPreloaderEnabled) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SpaceDialog(
            onDismissRequest = onDismiss,
            title = "حارس الأمان والجسر البرمجي (Bridge v2)",
            subtitle = "توثيق البصمة، ماسح QR، وتشديد الحماية",
            icon = Icons.Default.Security,
            accentColor = SpaceColors.Emerald,
            secondaryAccent = SpaceColors.NeonCyan,
            widthFraction = 0.92f
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudioSwitchTile("توثيق البصمة والوجه (authenticateBiometric)", "تمكين تسجيل الدخول وتأكيد الدفع بالبصمة من JS", biometric) { biometric = it }
                StudioSwitchTile("ماسح QR/Barcode المدمج (scanQrCode)", "فتح الماسح الضوئي وإرجاع القيمة لصفحة الويب", qrBridge) { qrBridge = it }
                StudioSwitchTile("نافذة تقييم المتجر (requestStoreRating)", "استدعاء Google Play In-App Review من داخل التطبيق", reviewBridge) { reviewBridge = it }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(SpaceColors.Emerald.copy(alpha = 0.5f), SpaceColors.NeonCyan.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )

                StudioSwitchTile("منع تصوير الشاشة (FLAG_SECURE)", "يحظر لقطات الشاشة والتسجيل للصفحات الحساسة", flagSecure) { flagSecure = it }
                StudioSwitchTile("تشديد فحص SSL (SSL Pinning Strict)", "يرفض أي شهادة غير موثوقة لحماية الاتصال", sslPinning) { sslPinning = it }
                StudioSwitchTile("التخزين المسبق الذكي (Asset Pre-Loader)", "تحميل الأصول من الذاكرة المؤقتة لتسريع الفتح 3×", preloader) { preloader = it }

                DialogFooterButtons(onDismiss = onDismiss) {
                    onSave(
                        initial.copy(
                            biometricAuthBridge = biometric,
                            qrScannerBridge = qrBridge,
                            inAppReviewBridge = reviewBridge,
                            flagSecureEnabled = flagSecure,
                            sslPinningEnabled = sslPinning,
                            assetPreloaderEnabled = preloader
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AdaptiveIconAndUiStudioDialog(
    project: AppProject,
    onDismiss: () -> Unit,
    onSave: (ProStudioConfig) -> Unit
) {
    val initial = project.proConfig
    var iconShape by remember { mutableStateOf(initial.iconShape) }
    var iconBgHex by remember { mutableStateOf(initial.iconBgColorHex) }
    var drawerEnabled by remember { mutableStateOf(initial.drawerEnabled) }
    var drawerSubtitle by remember { mutableStateOf(initial.drawerHeaderSubtitle) }
    var fabEnabled by remember { mutableStateOf(initial.fabMenuEnabled) }
    var fabWhatsapp by remember { mutableStateOf(initial.fabWhatsappNumber) }

    val shapeOptions = listOf("squircle" to "Squircle", "circle" to "دائرة", "rounded" to "مربع منحني", "teardrop" to "قطرة")
    val bgPalette = listOf("#1A1E29", "#00E5FF", "#1677FF", "#00E6A0", "#A020F0", "#FFB52E")

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SpaceDialog(
            onDismissRequest = onDismiss,
            title = "استوديو الأيقونة المتكيّفة + UI",
            subtitle = "شكل الأيقونة، القائمة الجانبية، والزر العائم",
            icon = Icons.Default.Palette,
            accentColor = SpaceColors.NeonPurple,
            secondaryAccent = SpaceColors.NeonCyan,
            widthFraction = 0.92f
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 530.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val previewRadius = when (iconShape) {
                    "circle" -> 50
                    "rounded" -> 18
                    "teardrop" -> 32
                    else -> 26
                }
                NeonGlassCard(
                    accentColor = SpaceColors.NeonPurple,
                    secondaryAccent = SpaceColors.NeonCyan,
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RealAppIconAvatar(
                            project = project,
                            size = 62.dp,
                            cornerRadius = previewRadius.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(project.name, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SpaceColors.White)
                            Text("معاينة حية لبطاقة Google Play وmipmap-xxxhdpi", fontFamily = TajawalFontFamily, fontSize = 10.sp, color = SpaceColors.Emerald)
                            Text("★★★★★ 4.9 • مجاني • أندرويد ${project.packageConfig.minSdk}+", fontFamily = TajawalFontFamily, fontSize = 10.sp, color = SpaceColors.Gold)
                        }
                    }
                }

                // Shape Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shapeOptions.forEach { (key, label) ->
                        val selected = iconShape == key
                        Surface(
                            color = if (selected) SpaceColors.NeonPurple.copy(alpha = 0.22f) else SpaceColors.SurfaceDeep.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (selected) SpaceColors.NeonPurple else SpaceColors.GlassBorderSubtle),
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { iconShape = key }
                        ) {
                            Text(
                                text = label,
                                fontFamily = CairoFontFamily,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (selected) SpaceColors.NeonCyan else SpaceColors.MutedText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Background Palette Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("خلفية الأيقونة:", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = SpaceColors.MutedText)
                    bgPalette.forEach { hex ->
                        val selected = iconBgHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parseColorSafe(hex, SpaceColors.NeonCyan))
                                .border(if (selected) 2.5.dp else 1.dp, if (selected) SpaceColors.White else SpaceColors.GlassBorderSubtle, CircleShape)
                                .clickable { iconBgHex = hex }
                        )
                    }
                }

                HorizontalDivider(color = SpaceColors.GlassBorderSubtle)

                StudioSwitchTile("تفعيل القائمة الجانبية (Navigation Drawer)", "تضيف زر همبرجر مع قائمة أقسام منسدلة", drawerEnabled) { drawerEnabled = it }
                if (drawerEnabled) {
                    NeonTextField(
                        value = drawerSubtitle,
                        onValueChange = { drawerSubtitle = it },
                        label = "وصف رأس القائمة الجانبية",
                        accentColor = SpaceColors.NeonPurple,
                        singleLine = true
                    )
                }

                StudioSwitchTile("الزر العائم السريع (Floating Action Menu)", "زر عائم فوق WebView للتحديث السريع أو مراسلة البريد المحدد", fabEnabled) { fabEnabled = it }
                if (fabEnabled) {
                    NeonTextField(
                        value = fabWhatsapp,
                        onValueChange = { fabWhatsapp = it },
                        label = "بريد الدعم أو رابط تواصل اختياري (مثال: ziorio950@gmail.com)",
                        accentColor = SpaceColors.NeonCyan,
                        singleLine = true
                    )
                }

                DialogFooterButtons(onDismiss = onDismiss) {
                    onSave(
                        initial.copy(
                            iconShape = iconShape,
                            iconBgColorHex = iconBgHex,
                            drawerEnabled = drawerEnabled,
                            drawerHeaderSubtitle = drawerSubtitle.trim().ifBlank { "القائمة السريعة" },
                            fabMenuEnabled = fabEnabled,
                            fabWhatsappNumber = fabWhatsapp.trim()
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun GitHubCloudBuildDialog(
    project: AppProject,
    exporter: ProjectExporter,
    cloudEngine: GitHubCloudBuildEngine,
    options: BuildOptimizationOptions,
    hybridCoordinator: HybridBuildCoordinator? = null,
    onArtifactDownloaded: ((fileName: String, path: String, sizeText: String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(cloudEngine.getSavedToken()) }
    var hasStoredToken by remember { mutableStateOf(cloudEngine.hasSavedToken()) }
    var repoName by remember {
        mutableStateOf(
            project.name.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-').ifBlank { cloudEngine.getSavedRepoName() }
        )
    }
    var selectedTarget by remember { mutableStateOf("apk") } // "apk", "aab", "both"
    var autoSyncSecrets by remember { mutableStateOf(true) }
    var isSyncingSecrets by remember { mutableStateOf(false) }
    var isCheckingPreflight by remember { mutableStateOf(false) }
    var preflightReport by remember { mutableStateOf<GitHubCloudPreflightReport?>(null) }
    var secretsSynced by remember(repoName) { mutableStateOf(cloudEngine.isSigningSecretsSynced(repoName)) }
    var syncedCertFingerprint by remember(repoName) { mutableStateOf(cloudEngine.getSyncedCertFingerprint(repoName)) }
    var isRunning by remember { mutableStateOf(false) }
    var progressLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var triggerResult by remember { mutableStateOf<GitHubBuildTriggerResult?>(null) }
    var runInfo by remember { mutableStateOf<GitHubWorkflowRunInfo?>(null) }
    var downloadedArtifact by remember { mutableStateOf<DownloadedCloudArtifact?>(null) }
    var cloudError by remember { mutableStateOf<String?>(null) }
    var localFailureExcerpt by remember { mutableStateOf<String?>(null) }

    val coordinatorState by (hybridCoordinator?.state ?: remember {
        kotlinx.coroutines.flow.MutableStateFlow(HybridBuildState())
    }).collectAsState()

    val effectiveIsRunning = isRunning || isSyncingSecrets || isCheckingPreflight || (hybridCoordinator != null && coordinatorState.isRunning)
    val protectionMode = remember(hasStoredToken) { cloudEngine.getTokenProtectionMode() }
    val effectiveSynced = secretsSynced || (hybridCoordinator != null && coordinatorState.signingSecretsSynced)
    val effectiveFingerprint = coordinatorState.signingCertSha256?.takeIf { it.isNotBlank() } ?: syncedCertFingerprint

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SpaceDialog(
            onDismissRequest = onDismiss,
            title = "البناء السحابي الكامل (Gradle + Android SDK)",
            subtitle = "تجميع Kotlin/Compose وإنتاج APK/AAB عبر GitHub Actions مع جلب الناتج وتشفير المفاتيح.",
            icon = Icons.Default.CloudUpload,
            accentColor = SpaceColors.Gold,
            secondaryAccent = SpaceColors.NeonCyan,
            widthFraction = 0.94f
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // شارة تشفير الرمز ومفاتيح التوقيع
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpaceColors.Emerald.copy(alpha = 0.1f))
                        .border(1.dp, SpaceColors.Emerald.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "حماية الرمز: $protectionMode • مفاتيح Keystore لا تُرفع داخل Git",
                            fontFamily = TajawalFontFamily,
                            fontSize = 10.sp,
                            color = SpaceColors.White
                        )
                    }
                    if (hasStoredToken) {
                        Text(
                            text = "مسح الرمز",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = SpaceColors.ErrorRed,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    cloudEngine.clearCredentials()
                                    token = ""
                                    hasStoredToken = false
                                    Toast.makeText(context, "تم مسح رمز GitHub المشفّر من الجهاز", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                NeonTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = "GitHub Personal Access Token (repo + workflow)",
                    placeholder = "ghp_xxxxxxxxxxxxxxxxxxxx",
                    leadingIcon = Icons.Default.Key,
                    accentColor = SpaceColors.Gold,
                    singleLine = true,
                    testTag = "github_token_input"
                )

                NeonTextField(
                    value = repoName,
                    onValueChange = { repoName = it },
                    label = "اسم مستودع GitHub المستهدف",
                    leadingIcon = Icons.Default.Folder,
                    accentColor = SpaceColors.NeonCyan,
                    singleLine = true,
                    testTag = "github_repo_input"
                )

                // اختيار هدف البناء السحابي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "apk" to "APK موقّع",
                        "aab" to "Play AAB",
                        "both" to "APK + AAB"
                    ).forEach { (key, label) ->
                        val active = selectedTarget == key
                        Surface(
                            color = if (active) SpaceColors.Gold.copy(alpha = 0.22f) else SpaceColors.SurfaceDeep.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (active) SpaceColors.Gold else SpaceColors.GlassBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !effectiveIsRunning) { selectedTarget = key }
                        ) {
                            Text(
                                text = label,
                                fontFamily = CairoFontFamily,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                color = if (active) SpaceColors.Gold else SpaceColors.MutedText,
                                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // خيار تشفير ومزامنة مفتاح التوقيع عبر Libsodium
                StudioSwitchTile(
                    title = "مزامنة أسرار التوقيع مشفرة تلقائياً (Libsodium)",
                    subtitle = if (effectiveSynced && effectiveFingerprint.isNotBlank()) {
                        "Secrets مشفّرة متزامنة ✓ • SHA-256: ${effectiveFingerprint.take(12)}…"
                    } else {
                        "تشفّر مفتاح KeyVault (PKCS#12) وترفع SIGNING_KEYSTORE_BASE64 لـ GitHub Secrets"
                    },
                    checked = autoSyncSecrets,
                    onCheckedChange = { if (!effectiveIsRunning) autoSyncSecrets = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonButton(
                        text = if (isSyncingSecrets) "جارٍ المزامنة..." else "مزامنة الأسرار 🔐",
                        onClick = {
                            cloudError = null
                            cloudEngine.saveCredentials(token, repoName)
                            hasStoredToken = cloudEngine.hasSavedToken()
                            scope.launch {
                                isSyncingSecrets = true
                                val res = cloudEngine.syncProjectSigningSecrets(
                                    token = token,
                                    repoName = repoName,
                                    keyAlias = project.signingConfig.keyAlias.ifBlank { "apkforge_release" },
                                    organization = project.name
                                ) { msg ->
                                    progressLogs = (progressLogs + BuildLogSanitizer.redactSensitive(msg)).takeLast(20)
                                }
                                isSyncingSecrets = false
                                res.onSuccess { syncRes ->
                                    secretsSynced = true
                                    syncedCertFingerprint = syncRes.certSha256
                                    Toast.makeText(
                                        context,
                                        "✓ تمت مزامنة ${syncRes.syncedSecrets.size} أسرار توقيع مشفّرة إلى ${syncRes.owner}/${syncRes.repoName}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }.onFailure { err ->
                                    cloudError = BuildLogSanitizer.redactSensitive(err.message ?: "تعذر مزامنة أسرار التوقيع")
                                }
                            }
                        },
                        enabled = !effectiveIsRunning && token.isNotBlank() && repoName.isNotBlank(),
                        isLoading = isSyncingSecrets,
                        icon = Icons.Default.EnhancedEncryption,
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        height = 44.dp,
                        modifier = Modifier.weight(1.1f),
                        testTag = "github_sync_secrets_btn"
                    )

                    NeonButton(
                        text = if (isCheckingPreflight) "فحص..." else "فحص الجاهزية 🔍",
                        onClick = {
                            cloudError = null
                            cloudEngine.saveCredentials(token, repoName)
                            hasStoredToken = cloudEngine.hasSavedToken()
                            scope.launch {
                                isCheckingPreflight = true
                                val res = cloudEngine.verifyCloudBuildSetup(
                                    token = token,
                                    repoName = repoName,
                                    keyAlias = project.signingConfig.keyAlias.ifBlank { "apkforge_release" },
                                    organization = project.name
                                )
                                isCheckingPreflight = false
                                res.onSuccess { rep ->
                                    preflightReport = rep
                                    if (rep.allSigningSecretsReadyOnGitHub) {
                                        secretsSynced = true
                                        syncedCertFingerprint = rep.localVaultSha256
                                    }
                                }.onFailure { err ->
                                    cloudError = BuildLogSanitizer.redactSensitive(err.message ?: "تعذر فحص الحساب السحابي")
                                }
                            }
                        },
                        enabled = !effectiveIsRunning && token.isNotBlank() && repoName.isNotBlank(),
                        isLoading = isCheckingPreflight,
                        icon = Icons.Default.Verified,
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        height = 44.dp,
                        modifier = Modifier.weight(0.9f),
                        testTag = "github_preflight_check_btn"
                    )
                }

                preflightReport?.let { rep ->
                    val repAccent = if (rep.allSigningSecretsReadyOnGitHub && !rep.hasWorkflowScopeWarning) {
                        SpaceColors.Emerald
                    } else {
                        SpaceColors.Gold
                    }
                    NeonGlassCard(
                        accentColor = repAccent,
                        secondaryAccent = SpaceColors.NeonCyan,
                        shape = RoundedCornerShape(12.dp),
                        elevation = 6.dp,
                        glassAlpha = 0.88f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("github_preflight_report_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = rep.summaryAr,
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = SpaceColors.White,
                                lineHeight = 16.sp
                            )
                            val sha256Colon = QuantumKeyVault.formatFingerprintColon(rep.localVaultSha256)
                            val sha1Colon = QuantumKeyVault.formatFingerprintColon(rep.localVaultSha1)
                            if (sha256Colon.isNotBlank()) {
                                Text(
                                    text = "KeyVault SHA-256:\n$sha256Colon",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SpaceColors.NeonCyan,
                                    lineHeight = 13.sp
                                )
                            }
                            if (sha1Colon.isNotBlank()) {
                                Text(
                                    text = "SHA-1 (Firebase/Play): $sha1Colon",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SpaceColors.Gold,
                                    lineHeight = 13.sp
                                )
                            }
                            Text(
                                text = "📋 نسخ البصمات (SHA-1 / SHA-256)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = SpaceColors.Emerald,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        cm.setPrimaryClip(
                                            android.content.ClipData.newPlainText(
                                                "ApkForge Signing Fingerprints",
                                                "SHA-256: $sha256Colon\nSHA-1: $sha1Colon"
                                            )
                                        )
                                        Toast.makeText(context, "تم نسخ بصمات الشهادة (SHA-1 & SHA-256) ✓", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 3.dp)
                            )
                        }
                    }
                }

                NeonButton(
                    text = if (effectiveIsRunning) "جارٍ البناء السحابي ومتابعة Gradle..." else "بناء سحابي كامل وجلب الحزمة تلقائياً ☁️",
                    onClick = {
                        cloudError = null
                        localFailureExcerpt = null
                        downloadedArtifact = null
                        cloudEngine.saveCredentials(token, repoName)
                        hasStoredToken = cloudEngine.hasSavedToken()

                        if (hybridCoordinator != null) {
                            hybridCoordinator.startCloudBuild(
                                project = project,
                                targetType = selectedTarget,
                                options = options,
                                repoNameOverride = repoName,
                                cloudEngineOverride = cloudEngine,
                                syncSigningSecrets = autoSyncSecrets,
                                onCompleted = { fileName, path, sizeText ->
                                    onArtifactDownloaded?.invoke(fileName, path, sizeText)
                                },
                                onFailed = { err -> cloudError = err }
                            )
                        } else {
                            scope.launch {
                                isRunning = true
                                progressLogs = emptyList()
                                if (autoSyncSecrets) {
                                    cloudEngine.syncProjectSigningSecrets(
                                        token = token,
                                        repoName = repoName,
                                        keyAlias = project.signingConfig.keyAlias.ifBlank { "apkforge_release" },
                                        organization = project.name
                                    ) { msg ->
                                        progressLogs = (progressLogs + BuildLogSanitizer.redactSensitive(msg)).takeLast(20)
                                    }.onSuccess { sRes ->
                                        secretsSynced = true
                                        syncedCertFingerprint = sRes.certSha256
                                    }.onFailure { err ->
                                        val safeWarn = BuildLogSanitizer.redactSensitive(err.message ?: "تعذر مزامنة الأسرار")
                                        progressLogs = (progressLogs + "⚠️ $safeWarn").takeLast(20)
                                    }
                                }
                                val files = exporter.buildProjectTextEntries(
                                    project = project,
                                    options = options,
                                    forceCloudWorkflow = true,
                                    cloudBuildTarget = selectedTarget
                                )
                                val binaries = exporter.buildProjectBinaryEntries(project)
                                val res = cloudEngine.pushAndTriggerCloudBuild(
                                    token = token,
                                    repoName = repoName,
                                    filesToPush = files,
                                    binaryFiles = binaries
                                ) { msg ->
                                    progressLogs = (progressLogs + BuildLogSanitizer.redactSensitive(msg)).takeLast(20)
                                }
                                res.onSuccess { tr ->
                                    triggerResult = tr
                                    progressLogs = (progressLogs + "✅ ${tr.statusMessage}").takeLast(20)
                                    val finalRun = cloudEngine.pollWorkflowUntilComplete(
                                        token = token,
                                        owner = tr.owner,
                                        repoName = tr.repoName
                                    ) { rInfo, jobs ->
                                        runInfo = rInfo
                                        val activeStep = jobs.flatMap { it.steps }.lastOrNull { it.status != "queued" }
                                        if (activeStep != null) {
                                            progressLogs = (progressLogs + "☁️ #${activeStep.number}: ${activeStep.name}").takeLast(20)
                                        }
                                    }.getOrNull()

                                    if (finalRun != null && finalRun.conclusion == "success") {
                                        val arts = cloudEngine.listRunArtifacts(token, tr.owner, tr.repoName, finalRun.runId).getOrDefault(emptyList())
                                        val preferExt = if (selectedTarget == "aab") "aab" else "apk"
                                        val art = arts.firstOrNull { !it.expired && it.name.contains(preferExt, ignoreCase = true) }
                                            ?: arts.firstOrNull { !it.expired }
                                        if (art != null) {
                                            val assembler = BuildPackageAssembler(context, exporter)
                                            cloudEngine.downloadAndExtractArtifact(
                                                token = token,
                                                artifact = art,
                                                outputDir = assembler.getBuildsDir(),
                                                preferExtension = preferExt,
                                                expectedVaultCertSha256 = syncedCertFingerprint.takeIf { it.isNotBlank() }
                                            ) { msg ->
                                                progressLogs = (progressLogs + msg).takeLast(20)
                                            }.onSuccess { dl ->
                                                downloadedArtifact = dl
                                                val mime = if (dl.fileName.endsWith(".aab", true)) "application/octet-stream" else "application/vnd.android.package-archive"
                                                assembler.mirrorFileToPublicDownloads(dl.file, dl.fileName, mime)
                                                val sizeText = "${maxOf(1L, dl.sizeBytes / 1024L)} KB"
                                                onArtifactDownloaded?.invoke(dl.fileName, dl.file.absolutePath, sizeText)
                                                progressLogs = (progressLogs + "🎉 تم جلب ${dl.fileName} بنجاح ✓").takeLast(20)
                                            }.onFailure { err ->
                                                cloudError = err.message
                                            }
                                        } else {
                                            cloudError = "انتهى البناء لكن لم يُعثر على مخرج APK/AAB في GitHub Actions."
                                        }
                                    } else if (finalRun != null) {
                                        val jobs = cloudEngine.fetchWorkflowJobs(token, tr.owner, tr.repoName, finalRun.runId).getOrDefault(emptyList())
                                        cloudError = jobs.firstNotNullOfOrNull { it.failedStepSummary }
                                            ?: "انتهى التشغيل بالحالة: ${finalRun.conclusion ?: finalRun.status}"
                                        val failedJob = jobs.firstOrNull { it.conclusion == "failure" } ?: jobs.firstOrNull()
                                        if (failedJob != null && failedJob.jobId > 0L) {
                                            localFailureExcerpt = cloudEngine.fetchJobFailureLogExcerpt(
                                                token = token,
                                                owner = tr.owner,
                                                repoName = tr.repoName,
                                                jobId = failedJob.jobId
                                            ).getOrNull()
                                        }
                                    }
                                }.onFailure { err ->
                                    cloudError = BuildLogSanitizer.redactSensitive(err.message ?: "فشل الرفع السحابي")
                                    progressLogs = (progressLogs + "❌ $cloudError").takeLast(20)
                                }
                                isRunning = false
                            }
                        }
                    },
                    enabled = !effectiveIsRunning && token.isNotBlank() && repoName.isNotBlank(),
                    isLoading = effectiveIsRunning,
                    icon = Icons.Default.RocketLaunch,
                    style = NeonButtonStyle.GOLD_ENGINE,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "github_cloud_launch_btn"
                )

                // عرض التقدم المنسق من HybridBuildCoordinator إن كان متصلاً
                if (hybridCoordinator != null && (coordinatorState.activeEngine == SelectedBuildEngine.CLOUD_GRADLE_SDK) &&
                    (coordinatorState.isRunning || coordinatorState.progress > 0f || coordinatorState.errorMessage != null)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = coordinatorState.statusMessage,
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = if (coordinatorState.errorMessage != null) SpaceColors.ErrorRed else SpaceColors.NeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(coordinatorState.progress * 100).toInt()}%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = SpaceColors.Gold
                            )
                        }
                        NeonProgressIndicator(
                            progress = coordinatorState.progress,
                            height = 6.dp,
                            startColor = SpaceColors.NeonCyan,
                            endColor = SpaceColors.Gold
                        )
                    }
                }

                val mergedLogs = if (hybridCoordinator != null && coordinatorState.logs.isNotEmpty()) {
                    coordinatorState.logs.takeLast(14)
                } else {
                    progressLogs
                }
                if (mergedLogs.isNotEmpty()) {
                    NeonGlassCard(
                        accentColor = SpaceColors.NeonCyan,
                        secondaryAccent = SpaceColors.ElectricBlue,
                        shape = RoundedCornerShape(12.dp),
                        elevation = 6.dp,
                        glassAlpha = 0.92f,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            mergedLogs.forEach { line ->
                                Text(line, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = SpaceColors.Emerald)
                            }
                        }
                    }
                }

                val displayError = cloudError ?: coordinatorState.errorMessage
                val displayExcerpt = localFailureExcerpt ?: coordinatorState.failureLogExcerpt
                if (!displayError.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SpaceColors.ErrorRed.copy(alpha = 0.14f))
                            .border(1.dp, SpaceColors.ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "❌ $displayError",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = SpaceColors.ErrorRed
                        )
                        if (!displayExcerpt.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SpaceColors.TerminalBg)
                                    .border(0.8.dp, SpaceColors.ErrorRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = displayExcerpt,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = SpaceColors.White,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                val readyArtifactPath = downloadedArtifact?.file?.absolutePath ?: coordinatorState.artifactFilePath
                val readyArtifactName = downloadedArtifact?.fileName ?: coordinatorState.artifactFileName
                val sigVerdictLabel = downloadedArtifact?.signingModeLabelAr?.takeIf { it.isNotBlank() }
                    ?: coordinatorState.artifactSigningLabelAr
                val sigCertSha256 = downloadedArtifact?.signerCertSha256?.takeIf { it.isNotBlank() }
                    ?: coordinatorState.artifactSignerSha256.orEmpty()
                val sigCertSha1 = downloadedArtifact?.signerCertSha1?.takeIf { it.isNotBlank() }
                    ?: coordinatorState.artifactSignerSha1.orEmpty()
                val sigSubject = downloadedArtifact?.signerSubject?.takeIf { it.isNotBlank() }
                    ?: coordinatorState.artifactSignerSubject.orEmpty()
                val sigMatchesVault = downloadedArtifact?.matchesVaultCert ?: coordinatorState.artifactMatchesVaultKey

                if (!sigVerdictLabel.isNullOrBlank()) {
                    val verdictColor = when (sigMatchesVault) {
                        true -> SpaceColors.Emerald
                        false -> SpaceColors.Gold
                        null -> SpaceColors.NeonCyan
                    }
                    NeonGlassCard(
                        accentColor = verdictColor,
                        secondaryAccent = SpaceColors.NeonPurple,
                        shape = RoundedCornerShape(12.dp),
                        elevation = 6.dp,
                        glassAlpha = 0.88f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cloud_artifact_cert_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "🔏 فحص شهادة المخرج السحابي: $sigVerdictLabel",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = verdictColor
                            )
                            if (sigSubject.isNotBlank()) {
                                Text(
                                    text = "Subject: $sigSubject",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SpaceColors.MutedText
                                )
                            }
                            if (sigCertSha256.isNotBlank()) {
                                val sha256Formatted = QuantumKeyVault.formatFingerprintColon(sigCertSha256)
                                Text(
                                    text = "SHA-256: $sha256Formatted",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SpaceColors.White,
                                    lineHeight = 13.sp
                                )
                            }
                            if (sigCertSha1.isNotBlank()) {
                                Text(
                                    text = "SHA-1: ${QuantumKeyVault.formatFingerprintColon(sigCertSha1)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SpaceColors.Gold
                                )
                            }
                        }
                    }
                }

                if (!readyArtifactPath.isNullOrBlank() && !readyArtifactName.isNullOrBlank() && readyArtifactName.endsWith(".apk", true)) {
                    NeonButton(
                        text = "تثبيت $readyArtifactName الآن 📲",
                        icon = Icons.Default.InstallMobile,
                        style = NeonButtonStyle.EMERALD_SUCCESS,
                        height = 44.dp,
                        onClick = {
                            val res = exporter.installApkDirectly(readyArtifactPath, "cloud_release", project.name)
                            Toast.makeText(context, res.messageAr, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val actionsLink = triggerResult?.actionsUrl ?: coordinatorState.actionsUrl
                if (!actionsLink.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (triggerResult != null) {
                            NeonButton(
                                text = "تحديث Workflow",
                                onClick = {
                                    scope.launch {
                                        cloudEngine.fetchLatestWorkflowRun(token, triggerResult!!.owner, triggerResult!!.repoName).onSuccess {
                                            runInfo = it
                                        }
                                    }
                                },
                                style = NeonButtonStyle.CYBER_OUTLINE,
                                height = 42.dp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        NeonButton(
                            text = "فتح Actions ↗",
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(actionsLink)))
                            },
                            style = NeonButtonStyle.CYBER_OUTLINE,
                            height = 42.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                runInfo?.let { info ->
                    Text(
                        text = "Run #${info.runId}: ${info.status} (${info.conclusion ?: "جارٍ التنفيذ"}) • Artifacts: ${info.artifactCount}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = SpaceColors.Gold
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioSwitchTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    NeonGlassCard(
        accentColor = if (checked) SpaceColors.NeonCyan else SpaceColors.GlassBorderSubtle,
        secondaryAccent = SpaceColors.NeonPurple,
        shape = RoundedCornerShape(14.dp),
        elevation = if (checked) 8.dp else 4.dp,
        glassAlpha = 0.78f,
        onClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Text(title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SpaceColors.White)
                Text(subtitle, fontFamily = TajawalFontFamily, fontSize = 10.sp, color = SpaceColors.MutedText)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SpaceColors.DeepSpace,
                    checkedTrackColor = SpaceColors.NeonCyan,
                    uncheckedThumbColor = SpaceColors.MutedText,
                    uncheckedTrackColor = SpaceColors.SurfaceDeep
                )
            )
        }
    }
}

@Composable
private fun PresetCssChip(label: String, onClick: () -> Unit) {
    Surface(
        color = SpaceColors.SurfaceDeep.copy(alpha = 0.85f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SpaceColors.NeonCyan.copy(alpha = 0.45f)),
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }
    ) {
        Text(
            text = "+ $label",
            fontFamily = CairoFontFamily,
            fontSize = 10.sp,
            color = SpaceColors.NeonCyan,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DialogFooterButtons(onDismiss: () -> Unit, onSave: () -> Unit) {
    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NeonButton(
            text = "إلغاء",
            onClick = onDismiss,
            style = NeonButtonStyle.CYBER_OUTLINE,
            height = 46.dp,
            modifier = Modifier.weight(1f)
        )
        NeonButton(
            text = "حفظ وتطبيق ✓",
            onClick = onSave,
            style = NeonButtonStyle.PRIMARY_GLOW,
            height = 46.dp,
            modifier = Modifier.weight(1.4f)
        )
    }
}
