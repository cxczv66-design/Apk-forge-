package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

/**
 * المرحلة 1 (المصدر): محرر المواقع و PWA محلياً داخل بطاقة المصدر
 */
@Composable
fun Step1PwaLocalEditorCard(
    omniConfig: OmniIdeConfig,
    onOpenPwaCustomizer: () -> Unit
) {
    val hasCustomEdits = omniConfig.customHeroTitle.isNotBlank() ||
        omniConfig.customPrimaryButtonText.isNotBlank() ||
        omniConfig.customHideSelectorsCsv.isNotBlank() ||
        !omniConfig.customBannerImageUri.isNullOrBlank()

    StageFeatureContainerCard(
        title = "تحويل وتخصيص الموقع / PWA محلياً",
        subtitle = if (hasCustomEdits) {
            "تعديلات نشطة: نصوص مخصصة / عناصر CSS مخفية"
        } else {
            "تعديل النصوص، الزر الرئيسي، صورة البانر، وإخفاء العناصر غير المرغوبة"
        },
        icon = Icons.Filled.WebAsset,
        accent = StatusGreen
    ) {
        Button(
            onClick = onOpenPwaCustomizer,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("step1_pwa_editor_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("فتح محرر وتخصيص الموقع المحلي", color = Color.White, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

/**
 * المرحلة 2 (الهوية والمظهر): التدرجات اللونية، حجم الخطوط، والوضع الليلي القسري + CSS مخصص
 */
@Composable
fun Step2DesignAndTypographyCard(
    omniConfig: OmniIdeConfig,
    proConfig: ProStudioConfig,
    onOmniChange: (OmniIdeConfig) -> Unit,
    onProChange: (ProStudioConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // التدرجات اللونية الذكية
        StageToggleFeatureCard(
            title = "التدرجات اللونية الذكية (Gradients)",
            subtitle = "${omniConfig.gradientStartHex} → ${omniConfig.gradientEndHex}",
            icon = Icons.Filled.Gradient,
            accent = Color(0xFFF472B6),
            checked = omniConfig.gradientEnabled,
            testTag = "step2_gradient_switch",
            onCheckedChange = { onOmniChange(omniConfig.copy(gradientEnabled = it)) }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "#FF7A2F" to "#0F1318",
                    "#0EA5E9" to "#6366F1",
                    "#10B981" to "#064E3B",
                    "#EC4899" to "#4C1D95"
                ).forEach { (st, en) ->
                    val isSelected = omniConfig.gradientStartHex.equals(st, true) && omniConfig.gradientEndHex.equals(en, true)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Brush.horizontalGradient(listOf(parseColorSafe(st), parseColorSafe(en))))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                onOmniChange(
                                    omniConfig.copy(
                                        gradientEnabled = true,
                                        gradientStartHex = st,
                                        gradientEndHex = en
                                    )
                                )
                            }
                    )
                }
            }
        }

        // مقياس الخطوط والنصوص
        StageFeatureContainerCard(
            title = "تناسب الخطوط والنصوص (${omniConfig.fontScalePercent}%)",
            subtitle = "التحكم في حجم الخطوط داخل كل صفحات التطبيق",
            icon = Icons.Filled.FormatSize,
            accent = OrangePrimary
        ) {
            Slider(
                value = omniConfig.fontScalePercent.toFloat(),
                onValueChange = { onOmniChange(omniConfig.copy(fontScalePercent = it.roundToInt())) },
                valueRange = 80f..140f,
                steps = 5,
                colors = SliderDefaults.colors(thumbColor = OrangePrimary, activeTrackColor = OrangePrimary)
            )
        }

        // الوضع الليلي القسري وحقن CSS
        StageToggleFeatureCard(
            title = "الوضع الليلي القسري وحقن CSS",
            subtitle = "يحوّل صفحات الويب الفاتحة تلقائياً إلى ثيم داكن متناسق",
            icon = Icons.Filled.DarkMode,
            accent = Color(0xFF818CF8),
            checked = proConfig.forceDarkCss,
            testTag = "step2_force_dark_switch",
            onCheckedChange = { onProChange(proConfig.copy(forceDarkCss = it)) }
        ) {
            OutlinedTextField(
                value = proConfig.customCss,
                onValueChange = { onProChange(proConfig.copy(customCss = it)) },
                label = { Text("قواعد CSS مخصصة (اختياري)", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                placeholder = { Text(".banner { border-radius: 14px; }", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                maxLines = 3,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * المرحلة 5 (تفعيل الميزات والمحرك): مسرّع التطبيق، قاعدة البيانات المحلية، REST API، تعدد اللغات، الجسر v2، ومانع الإعلانات
 */
@Composable
fun Step5EngineAndBackendCard(
    omniConfig: OmniIdeConfig,
    proConfig: ProStudioConfig,
    bridgeConfig: BridgeAndCloudConfig,
    onOmniChange: (OmniIdeConfig) -> Unit,
    onProChange: (ProStudioConfig) -> Unit,
    onBridgeChange: (BridgeAndCloudConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // مسرّع التطبيق App Accelerator
        StageToggleFeatureCard(
            title = "مسرّع التطبيق (App Accelerator ⚡)",
            subtitle = "تحميل مسبق للروابط + تخزين Cache-First + تحميل ذكي للصور",
            icon = Icons.Filled.Bolt,
            accent = GoldAccent,
            checked = omniConfig.appAcceleratorEnabled,
            testTag = "step5_accelerator_switch",
            onCheckedChange = { enabled ->
                onOmniChange(omniConfig.copy(appAcceleratorEnabled = enabled))
                onProChange(proConfig.copy(assetPreloaderEnabled = enabled))
            }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = omniConfig.acceleratorPrefetchLinks,
                    onClick = { onOmniChange(omniConfig.copy(acceleratorPrefetchLinks = !omniConfig.acceleratorPrefetchLinks)) },
                    label = { Text("جلب مسبق للروابط", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = omniConfig.acceleratorImageCompression,
                    onClick = { onOmniChange(omniConfig.copy(acceleratorImageCompression = !omniConfig.acceleratorImageCompression)) },
                    label = { Text("تحميل الصور الذكي", fontSize = 11.sp) }
                )
            }
        }

        // قاعدة البيانات المحلية
        StageToggleFeatureCard(
            title = "قاعدة البيانات المحلية (Local Key-Value DB)",
            subtitle = "يوفر دوال ApkForge.dbPut / dbGet لحفظ البيانات على الهاتف",
            icon = Icons.Filled.Storage,
            accent = StatusGreen,
            checked = omniConfig.localDbEnabled,
            testTag = "step5_local_db_switch",
            onCheckedChange = { onOmniChange(omniConfig.copy(localDbEnabled = it)) }
        ) {
            OutlinedTextField(
                value = omniConfig.localDbTableName,
                onValueChange = { onOmniChange(omniConfig.copy(localDbTableName = it)) },
                label = { Text("اسم جدول البيانات المحلية", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // مربط نقاط نهاية REST API
        StageToggleFeatureCard(
            title = "مربط نقاط نهاية REST API",
            subtitle = omniConfig.apiBaseUrl.ifBlank { "ربط واجهات JSON وخوادم البيانات" },
            icon = Icons.Filled.Api,
            accent = CyanAccent,
            checked = omniConfig.apiEndpointsEnabled,
            testTag = "step5_api_switch",
            onCheckedChange = { onOmniChange(omniConfig.copy(apiEndpointsEnabled = it)) }
        ) {
            OutlinedTextField(
                value = omniConfig.apiBaseUrl,
                onValueChange = { onOmniChange(omniConfig.copy(apiBaseUrl = it)) },
                label = { Text("رابط الخادم الأساسي Base URL", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                placeholder = { Text("https://api.example.com/v1", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // مدير اللغات والتوجيه RTL/LTR (i18n)
        StageToggleFeatureCard(
            title = "مدير اللغات والتوجيه RTL/LTR (i18n)",
            subtitle = "اللغات المدمجة: ${omniConfig.i18nSupportedCodesCsv}",
            icon = Icons.Filled.Translate,
            accent = Color(0xFF38BDF8),
            checked = omniConfig.i18nSupportedCodesCsv.contains(","),
            testTag = "step5_i18n_switch",
            onCheckedChange = { enabled ->
                val next = if (enabled) "ar,en,fr,es,tr" else omniConfig.i18nDefaultCode.ifBlank { "ar" }
                onOmniChange(omniConfig.copy(i18nSupportedCodesCsv = next))
            }
        ) {
            OutlinedTextField(
                value = omniConfig.i18nSupportedCodesCsv,
                onValueChange = { onOmniChange(omniConfig.copy(i18nSupportedCodesCsv = it)) },
                label = { Text("رموز اللغات (مفصولة بفاصلة)", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // الجسر البرمجي v2 والروابط العميقة
        StageToggleFeatureCard(
            title = "الجسر البرمجي v2 والروابط العميقة",
            subtitle = "البصمة، مسح QR، التقييم، وروابط App Links",
            icon = Icons.Filled.Hub,
            accent = OrangePrimary,
            checked = bridgeConfig.jsBridgeEnabled,
            testTag = "step5_bridge_switch",
            onCheckedChange = { onBridgeChange(bridgeConfig.copy(jsBridgeEnabled = it)) }
        ) {
            OutlinedTextField(
                value = bridgeConfig.deepLinkDomain,
                onValueChange = { onBridgeChange(bridgeConfig.copy(deepLinkDomain = it)) },
                label = { Text("نطاق الروابط العميقة (Deep Link Domain)", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                placeholder = { Text("app.mydomain.com", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // مانع الإعلانات والنوافذ المنبثقة
        StageToggleFeatureCard(
            title = "مانع الإعلانات والنوافذ المنبثقة",
            subtitle = "يحجب نطاقات التتبع والإعلانات المزعجة تلقائياً",
            icon = Icons.Filled.Block,
            accent = Color(0xFFF472B6),
            checked = proConfig.adAndPopupBlocker,
            testTag = "step5_adblock_switch",
            onCheckedChange = { onProChange(proConfig.copy(adAndPopupBlocker = it)) }
        )
    }
}

/**
 * المرحلة 6 (التنقل): القائمة الجانبية (Drawer) وزر التواصل العائم (FAB)
 */
@Composable
fun Step6DrawerAndFabCard(
    proConfig: ProStudioConfig,
    onProChange: (ProStudioConfig) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StageToggleFeatureCard(
            title = "القائمة الجانبية (Navigation Drawer)",
            subtitle = "قائمة سريعة متكاملة للوصول للأقسام والمشاركة",
            icon = Icons.Filled.ViewSidebar,
            accent = CyanAccent,
            checked = proConfig.drawerEnabled,
            testTag = "step6_drawer_switch",
            onCheckedChange = { onProChange(proConfig.copy(drawerEnabled = it)) }
        ) {
            OutlinedTextField(
                value = proConfig.drawerHeaderSubtitle,
                onValueChange = { onProChange(proConfig.copy(drawerHeaderSubtitle = it)) },
                label = { Text("عنوان القائمة الجانبية الفرعي", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        StageToggleFeatureCard(
            title = "زر التواصل والإجراء العائم (FAB)",
            subtitle = "زر تواصل فوري واتساب / دعم داخل واجهة التطبيق",
            icon = Icons.Filled.TouchApp,
            accent = WhatsAppGreen,
            checked = proConfig.fabMenuEnabled,
            testTag = "step6_fab_switch",
            onCheckedChange = { onProChange(proConfig.copy(fabMenuEnabled = it)) }
        ) {
            OutlinedTextField(
                value = proConfig.fabWhatsappNumber,
                onValueChange = { onProChange(proConfig.copy(fabWhatsappNumber = it)) },
                label = { Text("رقم واتساب أو بريد التواصل السريع", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                placeholder = { Text("966500000000", fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * المرحلة 7 (الحزمة والأمان والتشفير): منشئ المصادقة، FLAG_SECURE، SSL Pinning، ومفتاح KeyVault
 */
@Composable
fun Step7AuthAndSecurityCard(
    omniConfig: OmniIdeConfig,
    proConfig: ProStudioConfig,
    signingConfig: SigningKeyConfig,
    onOmniChange: (OmniIdeConfig) -> Unit,
    onProChange: (ProStudioConfig) -> Unit,
    onGenerateKeystore: (() -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // منشئ المصادقة Auth Builder
        StageToggleFeatureCard(
            title = "منشئ المصادقة (Auth Builder)",
            subtitle = "تسجيل دخول Google / Email / Phone مدمج بالحزمة",
            icon = Icons.Filled.VpnKey,
            accent = CyanAccent,
            checked = omniConfig.authBuilderEnabled,
            testTag = "step7_auth_builder_switch",
            onCheckedChange = { onOmniChange(omniConfig.copy(authBuilderEnabled = it)) }
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = omniConfig.authGoogleEnabled,
                    onClick = { onOmniChange(omniConfig.copy(authGoogleEnabled = !omniConfig.authGoogleEnabled)) },
                    label = { Text("Google", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = omniConfig.authEmailEnabled,
                    onClick = { onOmniChange(omniConfig.copy(authEmailEnabled = !omniConfig.authEmailEnabled)) },
                    label = { Text("Email", fontSize = 11.sp) }
                )
                FilterChip(
                    selected = omniConfig.authPhoneEnabled,
                    onClick = { onOmniChange(omniConfig.copy(authPhoneEnabled = !omniConfig.authPhoneEnabled)) },
                    label = { Text("Phone", fontSize = 11.sp) }
                )
            }
        }

        // منع لقطات الشاشة FLAG_SECURE
        StageToggleFeatureCard(
            title = "منع لقطات الشاشة (FLAG_SECURE)",
            subtitle = "حماية شاشات التطبيق من التصوير والتسجيل",
            icon = Icons.Filled.ScreenshotMonitor,
            accent = OrangePrimary,
            checked = proConfig.flagSecureEnabled,
            testTag = "step7_flag_secure_switch",
            onCheckedChange = { onProChange(proConfig.copy(flagSecureEnabled = it)) }
        )

        // تصلب شهادات الأمان SSL Pinning
        StageToggleFeatureCard(
            title = "تصلب شهادات الأمان (SSL Pinning Strict)",
            subtitle = "يرفض اتصالات HTTPS غير الموثوقة تلقائياً",
            icon = Icons.Filled.Https,
            accent = StatusGreen,
            checked = proConfig.sslPinningEnabled,
            testTag = "step7_ssl_pinning_switch",
            onCheckedChange = { onProChange(proConfig.copy(sslPinningEnabled = it)) }
        )

        // خزنة مفاتيح التوقيع المشفّرة AES-256-GCM
        StageFeatureContainerCard(
            title = "خزنة مفاتيح التوقيع المشفّرة (AES-256-GCM)",
            subtitle = if (signingConfig.keystorePath.isNotBlank()) {
                "شهادة نشطة: ${signingConfig.keyAlias} ✓"
            } else {
                "BouncyCastle RSA-2048 محمي داخل AndroidKeyStore"
            },
            icon = Icons.Filled.Key,
            accent = StatusGreen
        ) {
            if (onGenerateKeystore != null) {
                OutlinedButton(
                    onClick = onGenerateKeystore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("step7_generate_keystore_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.EnhancedEncryption, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("توليد / تحديث شهادة التوقيع المشفّرة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * المرحلة 8 (صلاحيات الجهاز): تفعيل شاشة توضيح الأذونات الذكية المنظمة قبل النظام
 */
@Composable
fun Step8SmartRationaleCard(
    smartRationaleEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    StageToggleFeatureCard(
        title = "واجهة توضيح الأذونات الذكية (Smart Rationale UI)",
        subtitle = "تشرح للمستخدم سبب كل إذن بوضوح قبل نظام أندرويد وتحقن المانيفست باللازم فقط",
        icon = Icons.Filled.AdminPanelSettings,
        accent = CyanAccent,
        checked = smartRationaleEnabled,
        testTag = "step8_smart_rationale_switch",
        onCheckedChange = onToggle
    )
}

@Composable
private fun StageToggleFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
    extraContent: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131622),
        border = BorderStroke(1.dp, if (checked) accent.copy(alpha = 0.5f) else GlassBorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text(subtitle, fontFamily = TajawalFontFamily, fontSize = 11.sp, color = TextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier.testTag(testTag),
                    colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
                )
            }
            AnimatedVisibility(visible = checked && extraContent != null) {
                extraContent?.invoke()
            }
        }
    }
}

@Composable
private fun StageFeatureContainerCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131622),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.38f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                    Text(subtitle, fontFamily = TajawalFontFamily, fontSize = 11.sp, color = TextSecondary)
                }
            }
            content()
        }
    }
}
