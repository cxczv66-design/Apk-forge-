package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.model.AppProject
import com.example.domain.model.ProStudioConfig
import com.example.engine.*
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
    var scraped by remember { mutableStateOf<ScrapedSiteIdentity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = OrangePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تحويل رابط موقع ذكي (PWA Scraper)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "أدخل رابط موقعك وسيقوم ApkForge باستخراج الاسم، اللون الأساسي، الأيقونة، واسم الحزمة تلقائياً.",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("رابط الموقع (URL)") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("smart_url_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isScanning = true
                                val res = scraper.scrapeSiteIdentity(urlInput)
                                isScanning = false
                                res.onSuccess { scraped = it }
                                    .onFailure {
                                        Toast.makeText(context, "يرجى إدخال رابط صحيح مثل https://example.com", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        enabled = !isScanning && urlInput.trim().length > 4,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ تحليل الموقع والـ Manifest...", fontFamily = CairoFontFamily, color = Color.White)
                        } else {
                            Icon(Icons.Default.TravelExplore, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحليل واستخراج الهوية تلقائياً", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    scraped?.let { id ->
                        val themeColor = parseColorSafe(id.themeColorHex, OrangePrimary)
                        Surface(
                            color = Color(0xFF0E131D),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeColor.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(themeColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!id.iconUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = id.iconUrl,
                                            contentDescription = id.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(id.title.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(id.title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Text(id.suggestedPackageName, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = themeColor)
                                    Text(
                                        text = if (id.hasPwaManifest) "PWA Manifest ✓ • ${id.themeColorHex}" else "Meta Auto-Detect • ${id.themeColorHex}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = StatusGreen
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val proj = scraper.toPreConfiguredProject(id)
                                onCreateFromUrl(proj)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تعبئة المعالج والبدء فوراً ⚡", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("إغلاق", fontFamily = CairoFontFamily, color = TextSecondary)
                    }
                }
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
        Surface(
            color = Color(0xFF121824),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "الاستوديو الاحترافي (Pro Studio Suite)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "حاقن CSS • مانع إعلانات • الأمان • البصمة/QR • الأيقونات • GitHub Cloud",
                                fontFamily = TajawalFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
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
                        accent = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f),
                        testTag = "pro_web_customizer_tile",
                        onClick = onOpenWebCustomizer
                    )
                    ProTile(
                        title = "الأمان والجسر v2",
                        subtitle = "بصمة=${pro.biometricAuthBridge} • SSL=${pro.sslPinningEnabled}",
                        icon = Icons.Default.Security,
                        accent = StatusGreen,
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
                        accent = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f),
                        testTag = "pro_icon_ui_tile",
                        onClick = onOpenIconAndUiStudio
                    )
                    ProTile(
                        title = "بناء GitHub فوري",
                        subtitle = "رفع + تشغيل Actions تلقائياً ☁️",
                        icon = Icons.Default.CloudSync,
                        accent = OrangePrimary,
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
    Surface(
        color = Color(0xFF0E131D),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                Text(subtitle, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextSecondary, maxLines = 1)
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("مخصص الويب وحاقن CSS/JS + مانع الإعلانات", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                    StudioSwitchTile("الوضع الليلي الإجباري (Force Dark CSS)", "يحول أي موقع فاتح إلى تصميم داكن مريح للعين", forceDark) { forceDark = it }
                    StudioSwitchTile("حاجب الإعلانات والنوافذ المنبثقة (Ad Blocker)", "يمنع النطاقات الإعلانية أثناء تحميل WebView", adBlocker) { adBlocker = it }

                    if (adBlocker) {
                        OutlinedTextField(
                            value = blockedHosts,
                            onValueChange = { blockedHosts = it },
                            label = { Text("النطاقات المحجوبة (مفصولة بفاصلة)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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

                    OutlinedTextField(
                        value = customCss,
                        onValueChange = { customCss = it },
                        label = { Text("CSS مخصص يُحقن داخل الصفحة") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = customJs,
                        onValueChange = { customJs = it },
                        label = { Text("JavaScript مخصص عند اكتمال التحميل") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("حارس الأمان والجسر البرمجي المتقدم (Bridge v2)", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                    StudioSwitchTile("توثيق البصمة والوجه (authenticateBiometric)", "تمكين تسجيل الدخول وتأكيد الدفع بالبصمة من JS", biometric) { biometric = it }
                    StudioSwitchTile("ماسح QR/Barcode المدمج (scanQrCode)", "فتح الماسح الضوئي وإرجاع القيمة لصفحة الويب", qrBridge) { qrBridge = it }
                    StudioSwitchTile("نافذة تقييم المتجر (requestStoreRating)", "استدعاء Google Play In-App Review من داخل التطبيق", reviewBridge) { reviewBridge = it }

                    HorizontalDivider(color = GlassBorderSubtle)

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
    val bgPalette = listOf("#1A1E29", "#FF7A2F", "#0EA5E9", "#10B981", "#7C3AED", "#E11D48")

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("استوديو الأيقونة المتكيّفة والقائمة الجانبية + FAB", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)

                    // Live Adaptive Icon & Play Store Listing Preview
                    val previewBg = parseColorSafe(iconBgHex, Color(0xFF1A1E29))
                    val previewRadius = when (iconShape) {
                        "circle" -> 50
                        "rounded" -> 18
                        "teardrop" -> 32
                        else -> 26
                    }
                    Surface(
                        color = Color(0xFF0E131D),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(RoundedCornerShape(previewRadius))
                                    .background(previewBg)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(previewRadius)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = project.name.take(2).uppercase().ifBlank { "AF" },
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.name, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                Text("معاينة حية لبطاقة Google Play وmipmap-xxxhdpi", fontFamily = TajawalFontFamily, fontSize = 10.sp, color = StatusGreen)
                                Text("★★★★★ 4.9 • مجاني • أندرويد ${project.packageConfig.minSdk}+", fontFamily = TajawalFontFamily, fontSize = 10.sp, color = GoldAccent)
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
                                color = if (selected) OrangePrimary.copy(alpha = 0.2f) else Color(0xFF0E131D),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) OrangePrimary else GlassBorderSubtle),
                                modifier = Modifier.clickable { iconShape = key }
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 11.sp,
                                    color = if (selected) OrangePrimary else TextSecondary,
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
                        Text("خلفية الأيقونة:", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = TextSecondary)
                        bgPalette.forEach { hex ->
                            val selected = iconBgHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseColorSafe(hex, OrangePrimary))
                                    .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else GlassBorderSubtle, CircleShape)
                                    .clickable { iconBgHex = hex }
                            )
                        }
                    }

                    HorizontalDivider(color = GlassBorderSubtle)

                    StudioSwitchTile("تفعيل القائمة الجانبية (Navigation Drawer)", "تضيف زر همبرجر مع قائمة أقسام منسدلة", drawerEnabled) { drawerEnabled = it }
                    if (drawerEnabled) {
                        OutlinedTextField(
                            value = drawerSubtitle,
                            onValueChange = { drawerSubtitle = it },
                            label = { Text("وصف رأس القائمة الجانبية") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    StudioSwitchTile("الزر العائم السريع (Floating Action Menu)", "زر عائم فوق WebView للتواصل عبر واتساب أو التحديث", fabEnabled) { fabEnabled = it }
                    if (fabEnabled) {
                        OutlinedTextField(
                            value = fabWhatsapp,
                            onValueChange = { fabWhatsapp = it },
                            label = { Text("رقم واتساب للدعم السريع (اختياري مثل 966500000000)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
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
}

@Composable
fun GitHubCloudBuildDialog(
    project: AppProject,
    exporter: ProjectExporter,
    cloudEngine: GitHubCloudBuildEngine,
    options: BuildOptimizationOptions,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(cloudEngine.getSavedToken()) }
    var repoName by remember {
        mutableStateOf(
            project.name.lowercase().replace(Regex("[^a-z0-9-]"), "-").ifBlank { cloudEngine.getSavedRepoName() }
        )
    }
    var isRunning by remember { mutableStateOf(false) }
    var progressLogs by remember { mutableStateOf<List<String>>(emptyList()) }
    var triggerResult by remember { mutableStateOf<GitHubBuildTriggerResult?>(null) }
    var runInfo by remember { mutableStateOf<GitHubWorkflowRunInfo?>(null) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                color = Color(0xFF141A26),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = OrangePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("البناء السحابي عبر GitHub API", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary) }
                    }

                    Text(
                        text = "اربط رمز GitHub Token ليقوم ApkForge برفع كود المشروع وتشغيل GitHub Actions لبناء APK و AAB في السحابة تلقائياً.",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        label = { Text("GitHub Personal Access Token (repo + workflow)") },
                        placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxx") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = repoName,
                        onValueChange = { repoName = it },
                        label = { Text("اسم مستودع GitHub المستهدف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isRunning = true
                                progressLogs = emptyList()
                                cloudEngine.saveCredentials(token, repoName)
                                val files = exporter.buildProjectTextEntries(project, options)
                                val res = cloudEngine.pushAndTriggerCloudBuild(token, repoName, files) { msg ->
                                    progressLogs = (listOf(msg) + progressLogs).take(12)
                                }
                                isRunning = false
                                res.onSuccess {
                                    triggerResult = it
                                    progressLogs = listOf("✅ ${it.statusMessage}") + progressLogs
                                }.onFailure { err ->
                                    progressLogs = listOf("❌ خطأ: ${err.message}") + progressLogs
                                    Toast.makeText(context, "فشل الرفع السحابي: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isRunning && token.isNotBlank() && repoName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ الرفع والتشغيل...", fontFamily = CairoFontFamily, color = Color.White)
                        } else {
                            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("رفع المشروع وتشغيل البناء السحابي ☁️", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (progressLogs.isNotEmpty()) {
                        Surface(
                            color = TerminalBackground,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232C3E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                progressLogs.forEach { line ->
                                    Text(line, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TerminalText)
                                }
                            }
                        }
                    }

                    triggerResult?.let { tr ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        cloudEngine.fetchLatestWorkflowRun(token, tr.owner, tr.repoName).onSuccess {
                                            runInfo = it
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تحديث حالة Workflow", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                            }
                            Button(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tr.actionsUrl)))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("فتح Actions ↗", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                            }
                        }
                    }

                    runInfo?.let { info ->
                        Text(
                            text = "Run #${info.runId}: ${info.status} (${info.conclusion ?: "جارٍ التنفيذ"}) • Artifacts: ${info.artifactCount}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = OrangePrimary
                        )
                    }
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0E131D))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
            Text(subtitle, fontFamily = TajawalFontFamily, fontSize = 10.sp, color = TextSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PresetCssChip(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF0E131D),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = "+ $label",
            fontFamily = CairoFontFamily,
            fontSize = 10.sp,
            color = OrangePrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DialogFooterButtons(onDismiss: () -> Unit, onSave: () -> Unit) {
    Spacer(modifier = Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
            Text("إلغاء", fontFamily = CairoFontFamily, color = TextSecondary)
        }
        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
            modifier = Modifier.weight(1.4f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("حفظ وتطبيق ✓", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
