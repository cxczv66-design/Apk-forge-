package com.example.engine

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import com.example.data.repository.DownloadedApk
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * محرك الذكاء التكيفي (Omni-Adaptive Source Analyzer):
 * يحلل تعقيد المصادر الحقيقية (HTML / ZIP / URL / APK / AI Roadmap)
 * ويولد مراحل تفاعلية وميزات ديناميكية تتكيف مع كل مصدر وحجمه.
 */
object AdaptiveSourceAnalyzer {

    private val imgRegex = Regex("""<img\b([^>]*)>""", RegexOption.IGNORE_CASE)
    private val srcRegex = Regex("""src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val altRegex = Regex("""alt\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val btnRegex = Regex("""<(button|a)\b([^>]*)>(.*?)</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val headingRegex = Regex("""<(h[1-4]|p)\b[^>]*>(.*?)</\1>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val inputRegex = Regex("""<(input|textarea|select|form)\b""", RegexOption.IGNORE_CASE)
    private val sectionRegex = Regex("""<(section|nav|header|footer|article)\b""", RegexOption.IGNORE_CASE)
    private val scriptRegex = Regex("""<script\b""", RegexOption.IGNORE_CASE)
    private val tagStripRegex = Regex("""<[^>]+>""")

    fun analyzeHtmlComplexity(
        sourceKind: AdaptiveSourceKind,
        rawHtml: String
    ): SourceComplexityReport {
        val imgs = imgRegex.findAll(rawHtml).toList()
        val imageLabels = imgs.mapIndexed { idx, match ->
            val attrs = match.groupValues[1]
            val alt = altRegex.find(attrs)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val src = srcRegex.find(attrs)?.groupValues?.getOrNull(1)?.substringAfterLast('/')?.take(28).orEmpty()
            when {
                alt.isNotBlank() -> alt.take(28)
                src.isNotBlank() -> src
                else -> "صورة رقم ${idx + 1}"
            }
        }

        val btnMatches = btnRegex.findAll(rawHtml).filter { m ->
            val tag = m.groupValues[1].lowercase()
            val attrs = m.groupValues[2].lowercase()
            tag == "button" || attrs.contains("btn") || attrs.contains("button") || attrs.contains("cta")
        }.toList()

        val buttonLabels = btnMatches.mapIndexed { idx, m ->
            val txt = tagStripRegex.replace(m.groupValues[3], "").replace(Regex("\\s+"), " ").trim()
            txt.take(26).ifBlank { "زر تفاعل ${idx + 1}" }
        }

        val headings = headingRegex.findAll(rawHtml).mapNotNull { m ->
            val txt = tagStripRegex.replace(m.groupValues[2], "").replace(Regex("\\s+"), " ").trim()
            txt.take(36).takeIf { it.isNotBlank() }
        }.toList()

        val inputCount = inputRegex.findAll(rawHtml).count()
        val sectionCount = sectionRegex.findAll(rawHtml).count()
        val scriptCount = scriptRegex.findAll(rawHtml).count()

        val imgCount = imgs.size
        val btnCount = btnMatches.size
        val textCount = headings.size

        val totalElements = imgCount + btnCount + textCount + inputCount + sectionCount
        val estimatedPhases = calculateAdaptivePhaseTarget(imgCount, btnCount, textCount, inputCount, sectionCount)

        val tier = when {
            totalElements >= 60 -> "موقع ضخم عالي الكثافة (${estimatedPhases} مرحلة)"
            totalElements >= 22 -> "موقع متوسط متشعب (${estimatedPhases} مرحلة)"
            else -> "موقع خفيف وسريع (${estimatedPhases} مرحلة)"
        }

        return SourceComplexityReport(
            sourceKind = sourceKind,
            imageCount = imgCount,
            buttonCount = btnCount,
            formInputCount = inputCount,
            textBlockCount = textCount,
            sectionCount = sectionCount,
            scriptCount = scriptCount,
            permissionCount = 0,
            extractedImageLabels = imageLabels,
            extractedButtonLabels = buttonLabels,
            extractedHeadings = headings,
            recommendedPhaseCount = estimatedPhases,
            complexityTierAr = tier
        )
    }

    private fun calculateAdaptivePhaseTarget(
        imgCount: Int,
        btnCount: Int,
        textCount: Int,
        inputCount: Int,
        sectionCount: Int
    ): Int {
        val corePhases = 6
        val denseSite = imgCount >= 20 || btnCount >= 12 || (imgCount + btnCount + textCount) >= 40
        val extraImagePhases = if (imgCount == 0) 0 else if (denseSite) ((imgCount + 1) / 2) else ((imgCount + 4) / 5)
        val extraBtnPhases = if (btnCount == 0) 0 else if (denseSite) ((btnCount + 1) / 2) else ((btnCount + 4) / 5)
        val extraTextPhases = if (textCount <= 3) 0 else if (denseSite) ((textCount + 3) / 4) else ((textCount + 7) / 8)
        val extraFormPhases = if (inputCount >= 3) 2 else if (inputCount > 0) 1 else 0
        val extraSectionPhases = if (sectionCount >= 6) 2 else 0
        return (corePhases + extraImagePhases + extraBtnPhases + extraTextPhases + extraFormPhases + extraSectionPhases)
            .coerceIn(5, 100)
    }

    fun buildAdaptivePlan(
        project: AppProject,
        htmlOverride: String? = null,
        aiPlan: AiArchitectPlan? = null
    ): AdaptiveWorkspacePlan {
        // 1) مسار تعديل APK
        if (project.sourceType == SourceType.APK_PACKAGE) {
            return buildApkModificationPlan(project)
        }

        // 2) مسار خطة الذكاء الاصطناعي من الصفر (AI Wizard)
        if (project.sourceType == SourceType.AI_SCRATCH && aiPlan != null) {
            return buildAiRoadmapAdaptivePlan(project, aiPlan)
        }

        // 3) مسار HTML / ZIP / URL
        val sourceKind = if (project.sourceType == SourceType.WEB_URL) {
            AdaptiveSourceKind.WEB_SITE
        } else {
            AdaptiveSourceKind.HTML_ZIP
        }

        val resolvedHtml = resolveSourceHtml(project, htmlOverride)
        val report = analyzeHtmlComplexity(sourceKind, resolvedHtml)
        val phases = mutableListOf<AdaptivePhase>()
        var phaseNum = 1

        // المرحلة 1: تحليل المصدر ومحرر الموقع / PWA
        val phase1Features = buildList {
            add(
                DynamicPhaseFeature(
                    id = "src_open_pwa_customizer",
                    title = "فتح محرر وتخصيص الموقع المحلي (PWA Editor)",
                    subtitle = "تعديل النصوص، الزر الرئيسي، البانر، وإخفاء العناصر مباشرة",
                    categoryTag = "محرر مباشر",
                    type = DynamicFeatureType.ACTION_TRIGGER,
                    bindingKey = "action.open_pwa_editor"
                )
            )
            add(
                DynamicPhaseFeature(
                    id = "src_hero_title",
                    title = "تخصيص العنوان الرئيسي للموقع (Hero)",
                    subtitle = "يُحقن فوراً في أول عنوان داخل الصفحة",
                    categoryTag = "نص",
                    type = DynamicFeatureType.TEXT_INPUT,
                    bindingKey = "omni.customHeroTitle",
                    textValue = project.omniConfig.customHeroTitle
                )
            )
            add(
                DynamicPhaseFeature(
                    id = "src_primary_btn",
                    title = "تخصيص نص الزر الرئيسي (CTA)",
                    subtitle = "تغيير نص الزر الأساسي في الواجهة",
                    categoryTag = "زر",
                    type = DynamicFeatureType.TEXT_INPUT,
                    bindingKey = "omni.customPrimaryButtonText",
                    textValue = project.omniConfig.customPrimaryButtonText
                )
            )
            add(
                DynamicPhaseFeature(
                    id = "src_hide_selectors",
                    title = "إخفاء عناصر CSS غير المرغوبة",
                    subtitle = "مثال: .ads-banner, footer, .popup",
                    categoryTag = "DOM",
                    type = DynamicFeatureType.TEXT_INPUT,
                    bindingKey = "omni.customHideSelectorsCsv",
                    textValue = project.omniConfig.customHideSelectorsCsv
                )
            )
            add(
                DynamicPhaseFeature(
                    id = "src_accelerator",
                    title = "مسرّع التطبيق (App Accelerator ⚡)",
                    subtitle = "تحميل مسبق + ضغط وتخزين Cache-First",
                    categoryTag = "الأداء",
                    type = DynamicFeatureType.SWITCH,
                    bindingKey = "omni.appAcceleratorEnabled",
                    boolValue = project.omniConfig.appAcceleratorEnabled
                )
            )
        }
        phases.add(
            AdaptivePhase(
                index = phaseNum++,
                title = "تحليل المصدر والتخصيص الفوري",
                subtitle = "اكتشفنا ${report.imageCount} صورة • ${report.buttonCount} زر • ${report.textBlockCount} نص (${report.complexityTierAr})",
                badge = "المصدر",
                accentHex = sourceKind.badgeColorHex,
                supportsLiveWebPreview = true,
                features = phase1Features
            )
        )

        // المرحلة 2: الهوية، الألوان، التدرجات والخطوط
        phases.add(
            AdaptivePhase(
                index = phaseNum++,
                title = "الهوية، التدرجات والخطوط",
                subtitle = "تحكم كامل في الاسم والألوان والوضع الليلي ومقياس الخط",
                badge = "التصميم",
                accentHex = "#FF7A2F",
                supportsLiveWebPreview = true,
                features = listOf(
                    DynamicPhaseFeature(
                        id = "id_app_name",
                        title = "اسم التطبيق الظاهر",
                        subtitle = "يظهر تحت الأيقونة وفي شريط العنوان",
                        categoryTag = "الهوية",
                        type = DynamicFeatureType.TEXT_INPUT,
                        bindingKey = "app.name",
                        textValue = project.name
                    ),
                    DynamicPhaseFeature(
                        id = "id_accent",
                        title = "اللون الأساسي (Accent Color)",
                        subtitle = "يلون الأزرار، التدرجات، والمؤشرات",
                        categoryTag = "ألوان",
                        type = DynamicFeatureType.COLOR_PICKER,
                        bindingKey = "app.accentColorHex",
                        textValue = project.accentColorHex,
                        options = listOf("#FF7A2F", "#0EA5E9", "#10B981", "#A855F7", "#EC4899", "#F59E0B")
                    ),
                    DynamicPhaseFeature(
                        id = "id_gradient",
                        title = "التدرجات اللونية الذكية (Gradients)",
                        subtitle = "${project.omniConfig.gradientStartHex} → ${project.omniConfig.gradientEndHex}",
                        categoryTag = "مظهر",
                        type = DynamicFeatureType.SWITCH,
                        bindingKey = "omni.gradientEnabled",
                        boolValue = project.omniConfig.gradientEnabled
                    ),
                    DynamicPhaseFeature(
                        id = "id_font_scale",
                        title = "مقياس حجم الخطوط (${project.omniConfig.fontScalePercent}%)",
                        subtitle = "تحكم دقيق في نسبة تكبير النصوص بالصفحات",
                        categoryTag = "خطوط",
                        type = DynamicFeatureType.SLIDER,
                        bindingKey = "omni.fontScalePercent",
                        intValue = project.omniConfig.fontScalePercent,
                        minInt = 80,
                        maxInt = 140
                    ),
                    DynamicPhaseFeature(
                        id = "id_force_dark",
                        title = "الوضع الليلي القسري (Force Dark CSS)",
                        subtitle = "يحوّل صفحات الويب الفاتحة إلى ثيم داكن متناسق",
                        categoryTag = "ثيم",
                        type = DynamicFeatureType.SWITCH,
                        bindingKey = "pro.forceDarkCss",
                        boolValue = project.proConfig.forceDarkCss
                    ),
                    DynamicPhaseFeature(
                        id = "id_custom_css",
                        title = "قواعد CSS مخصصة إضافية",
                        subtitle = "تُحقن مباشرة فوق عناصر الموقع",
                        categoryTag = "CSS",
                        type = DynamicFeatureType.TEXT_INPUT,
                        bindingKey = "pro.customCss",
                        textValue = project.proConfig.customCss
                    ),
                    DynamicPhaseFeature(
                        id = "id_pick_icon",
                        title = "تخصيص الأيقونة والهوية المتكيّفة 3D",
                        subtitle = "اختيار صورة أيقونة حقيقية من المعرض",
                        categoryTag = "أيقونة",
                        type = DynamicFeatureType.ACTION_TRIGGER,
                        bindingKey = "action.pick_icon"
                    )
                )
            )
        )

        // مراحل ديناميكية متكيفة للصور المكتشفة (تتوسع تلقائياً مع زيادة الصور!)
        val denseSite = report.imageCount >= 20 || report.buttonCount >= 12 ||
            (report.imageCount + report.buttonCount + report.textBlockCount) >= 40
        val imgsPerPhase = if (denseSite) 2 else 5
        if (report.extractedImageLabels.isNotEmpty()) {
            val imageBatches = report.extractedImageLabels.chunked(imgsPerPhase)
            imageBatches.forEachIndexed { batchIdx, batch ->
                if (phases.size < 94) {
                    val startNum = batchIdx * imgsPerPhase + 1
                    val endNum = startNum + batch.size - 1
                    val features = mutableListOf<DynamicPhaseFeature>()
                    batch.forEachIndexed { innerIdx, label ->
                        val imgIdx = batchIdx * imgsPerPhase + innerIdx
                        features.add(
                            DynamicPhaseFeature(
                                id = "img_show_$imgIdx",
                                title = "إظهار الصورة #${imgIdx + 1}: $label",
                                subtitle = "تشغيل أو إخفاء هذه الصورة من واجهة التطبيق",
                                categoryTag = "صورة",
                                type = DynamicFeatureType.SWITCH,
                                bindingKey = "dom.img.visible.$imgIdx",
                                boolValue = true
                            )
                        )
                        features.add(
                            DynamicPhaseFeature(
                                id = "img_round_$imgIdx",
                                title = "تدوير حواف الصورة #${imgIdx + 1} (3D Card)",
                                subtitle = "إضافة ظل وتنعيم الحواف للصورة داخل العرض الحي",
                                categoryTag = "تأثير 3D",
                                type = DynamicFeatureType.SWITCH,
                                bindingKey = "dom.img.rounded.$imgIdx",
                                boolValue = false
                            )
                        )
                    }
                    phases.add(
                        AdaptivePhase(
                            index = phaseNum++,
                            title = "تخصيص الصور ($startNum - $endNum)",
                            subtitle = "تحكم مخصص بالصور المكتشفة من المصدر مع معاينة فورية",
                            badge = "صور",
                            accentHex = "#38BDF8",
                            supportsLiveWebPreview = true,
                            features = features
                        )
                    )
                }
            }
        }

        // مراحل ديناميكية متكيفة للأزرار المكتشفة
        val btnsPerPhase = if (denseSite) 2 else 5
        if (report.extractedButtonLabels.isNotEmpty()) {
            val btnBatches = report.extractedButtonLabels.chunked(btnsPerPhase)
            btnBatches.forEachIndexed { batchIdx, batch ->
                if (phases.size < 96) {
                    val startNum = batchIdx * btnsPerPhase + 1
                    val endNum = startNum + batch.size - 1
                    val features = mutableListOf<DynamicPhaseFeature>()
                    batch.forEachIndexed { innerIdx, label ->
                        val btnIdx = batchIdx * btnsPerPhase + innerIdx
                        features.add(
                            DynamicPhaseFeature(
                                id = "btn_txt_$btnIdx",
                                title = "نص الزر #${btnIdx + 1}",
                                subtitle = "تعديل تسمية الزر في التطبيق",
                                categoryTag = "زر",
                                type = DynamicFeatureType.TEXT_INPUT,
                                bindingKey = "dom.btn.text.$btnIdx",
                                textValue = label
                            )
                        )
                        features.add(
                            DynamicPhaseFeature(
                                id = "btn_vis_$btnIdx",
                                title = "تفعيل الزر #${btnIdx + 1} ($label)",
                                subtitle = "إظهار أو إخفاء هذا الزر في التطبيق",
                                categoryTag = "تفاعل",
                                type = DynamicFeatureType.SWITCH,
                                bindingKey = "dom.btn.visible.$btnIdx",
                                boolValue = true
                            )
                        )
                    }
                    phases.add(
                        AdaptivePhase(
                            index = phaseNum++,
                            title = "الأزرار والتفاعل ($startNum - $endNum)",
                            subtitle = "تعديل نصوص وظهور أزرار الموقع المكتشفة",
                            badge = "أزرار",
                            accentHex = "#F59E0B",
                            supportsLiveWebPreview = true,
                            features = features
                        )
                    )
                }
            }
        }

        // مراحل النصوص والعناوين المكتشفة للمواقع الكبيرة
        val textsPerPhase = if (denseSite) 4 else 8
        if (report.extractedHeadings.size > 3) {
            val headingBatches = report.extractedHeadings.take(40).chunked(textsPerPhase)
            headingBatches.forEachIndexed { batchIdx, batch ->
                if (phases.size < 97) {
                    val features = batch.mapIndexed { innerIdx, headingText ->
                        val hIdx = batchIdx * textsPerPhase + innerIdx
                        DynamicPhaseFeature(
                            id = "txt_edit_$hIdx",
                            title = "تعديل النص #${hIdx + 1}",
                            subtitle = "العنصر المكتشف: ${headingText.take(24)}",
                            categoryTag = "نصوص",
                            type = DynamicFeatureType.TEXT_INPUT,
                            bindingKey = "dom.text.replace.$hIdx",
                            textValue = headingText
                        )
                    }
                    phases.add(
                        AdaptivePhase(
                            index = phaseNum++,
                            title = "النصوص والمحتوى (${batchIdx + 1})",
                            subtitle = "تحرير العناوين والفقرات المكتشفة من الصفحة",
                            badge = "نصوص",
                            accentHex = "#A855F7",
                            supportsLiveWebPreview = true,
                            features = features
                        )
                    )
                }
            }
        }

        // مرحلة المحرك والتجربة التفاعلية (WebView + Nav + Drawer + FAB + DB + API)
        phases.add(
            AdaptivePhase(
                index = phaseNum++,
                title = "المحرك، التنقل والبيانات",
                subtitle = "ملء الشاشة، شريط التنقل، القائمة الجانبية، DB، و API",
                badge = "المحرك",
                accentHex = "#06B6D4",
                supportsLiveWebPreview = true,
                features = listOf(
                    DynamicPhaseFeature("eng_fullscreen", "وضع ملء الشاشة (Full Screen)", "إخفاء الأشرطة لإعطاء مساحة كاملة", "واجهة", DynamicFeatureType.SWITCH, "web.fullScreen", boolValue = project.webViewConfig.fullScreen),
                    DynamicPhaseFeature("eng_ptr", "السحب للتحديث (Pull to Refresh)", "تحديث الصفحة بالسحب للأسفل", "تفاعل", DynamicFeatureType.SWITCH, "web.pullToRefresh", boolValue = project.webViewConfig.pullToRefresh),
                    DynamicPhaseFeature("eng_bottom_nav", "شريط التنقل السفلي (Bottom Nav)", "أيقونات تنقل أصلية سريعة", "تنقل", DynamicFeatureType.SWITCH, "bottomNav.enabled", boolValue = project.bottomNavConfig.enabled),
                    DynamicPhaseFeature("eng_drawer", "القائمة الجانبية (Navigation Drawer)", "قائمة سحب جانبية للأقسام", "تنقل", DynamicFeatureType.SWITCH, "pro.drawerEnabled", boolValue = project.proConfig.drawerEnabled),
                    DynamicPhaseFeature("eng_fab", "زر التواصل العائم (FAB)", "زر تواصل فوري عائم فوق المحتوى", "تفاعل", DynamicFeatureType.SWITCH, "pro.fabMenuEnabled", boolValue = project.proConfig.fabMenuEnabled),
                    DynamicPhaseFeature("eng_adblock", "مانع الإعلانات والنوافذ المنبثقة", "حجب نطاقات الإعلانات والتتبع", "أداء", DynamicFeatureType.SWITCH, "pro.adAndPopupBlocker", boolValue = project.proConfig.adAndPopupBlocker),
                    DynamicPhaseFeature("eng_localdb", "قاعدة البيانات المحلية (Key-Value DB)", "تخزين بيانات المستخدم أوفلاين", "بيانات", DynamicFeatureType.SWITCH, "omni.localDbEnabled", boolValue = project.omniConfig.localDbEnabled),
                    DynamicPhaseFeature("eng_bridge", "الجسر البرمجي الأصلي (JS Bridge v2)", "ربط الكاميرا والبصمة ومشاركة النظام", "جسر", DynamicFeatureType.SWITCH, "bridge.jsBridgeEnabled", boolValue = project.bridgeConfig.jsBridgeEnabled),
                    DynamicPhaseFeature("eng_api_url", "رابط خادم REST API (اختياري)", "ربط واجهات JSON الخارجية", "API", DynamicFeatureType.TEXT_INPUT, "omni.apiBaseUrl", textValue = project.omniConfig.apiBaseUrl)
                )
            )
        )

        // مرحلة الحزمة، الأمان والصلاحيات والإشعارات
        phases.add(
            AdaptivePhase(
                index = phaseNum++,
                title = "الحزمة، الأمان والصلاحيات",
                subtitle = "معرّف الحزمة، التوقيع AES-256، الصلاحيات الذكية، والإشعارات",
                badge = "الأمان",
                accentHex = "#10B981",
                supportsLiveWebPreview = false,
                features = listOf(
                    DynamicPhaseFeature("pkg_id", "معرّف الحزمة (Package ID)", "مثال: com.company.myapp", "الحزمة", DynamicFeatureType.TEXT_INPUT, "pkg.packageName", textValue = project.packageConfig.packageName),
                    DynamicPhaseFeature("pkg_ver", "اسم الإصدار (Version Name)", "مثال: 1.0.0", "الإصدار", DynamicFeatureType.TEXT_INPUT, "pkg.versionName", textValue = project.packageConfig.versionName),
                    DynamicPhaseFeature("sec_auth", "منشئ المصادقة (Google / Email)", "شاشة تسجيل دخول مدمجة", "مصادقة", DynamicFeatureType.SWITCH, "omni.authBuilderEnabled", boolValue = project.omniConfig.authBuilderEnabled),
                    DynamicPhaseFeature("sec_flag_secure", "حماية الشاشة (FLAG_SECURE)", "منع التقاط أو تسجيل الشاشة", "حماية", DynamicFeatureType.SWITCH, "pro.flagSecureEnabled", boolValue = project.proConfig.flagSecureEnabled),
                    DynamicPhaseFeature("sec_ssl", "تصلب SSL Pinning Strict", "رفض الاتصالات غير الموثوقة", "شبكة", DynamicFeatureType.SWITCH, "pro.sslPinningEnabled", boolValue = project.proConfig.sslPinningEnabled),
                    DynamicPhaseFeature("sec_rationale", "توضيح الأذونات الذكية (Rationale UI)", "شرح سبب كل إذن قبل النظام", "أذونات", DynamicFeatureType.SWITCH, "omni.smartPermissionsRationaleEnabled", boolValue = project.omniConfig.smartPermissionsRationaleEnabled),
                    DynamicPhaseFeature("perm_camera", "إذن الكاميرا (Camera)", "لمسح QR ورفع الصور", "صلاحية", DynamicFeatureType.SWITCH, "perm.camera", boolValue = project.permissionsConfig.camera),
                    DynamicPhaseFeature("perm_location", "إذن الموقع (GPS Location)", "لخدمات الخرائط والتوصيل", "صلاحية", DynamicFeatureType.SWITCH, "perm.location", boolValue = project.permissionsConfig.location),
                    DynamicPhaseFeature("perm_mic", "إذن الميكروفون (Microphone)", "للتسجيل الصوتي والمكالمات", "صلاحية", DynamicFeatureType.SWITCH, "perm.microphone", boolValue = project.permissionsConfig.microphone),
                    DynamicPhaseFeature("perm_notif", "إذن الإشعارات الفورية (Push)", "تفعيل تنبيهات التطبيق", "إشعارات", DynamicFeatureType.SWITCH, "push.enabled", boolValue = project.pushConfig.enabled),
                    DynamicPhaseFeature("sec_keystore", "توليد شهادة توقيع AES-256-GCM", "مطلوبة لتوقيع APK/AAB/APKS نهائياً", "تشفير", DynamicFeatureType.ACTION_TRIGGER, "action.generate_keystore")
                )
            )
        )

        return AdaptiveWorkspacePlan(
            sourceKind = sourceKind,
            report = report.copy(recommendedPhaseCount = phases.size),
            phases = phases
        )
    }

    private fun buildApkModificationPlan(project: AppProject): AdaptiveWorkspacePlan {
        val perms = project.permissionsConfig
        val activePermCount = listOf(
            perms.camera, perms.location, perms.microphone, perms.notifications,
            perms.storage, perms.vibration, perms.bluetooth, perms.networkState
        ).count { it }

        val report = SourceComplexityReport(
            sourceKind = AdaptiveSourceKind.APK_IMPORT,
            imageCount = 1,
            buttonCount = 0,
            formInputCount = 0,
            textBlockCount = 2,
            sectionCount = 4,
            scriptCount = 0,
            permissionCount = activePermCount,
            recommendedPhaseCount = 4,
            complexityTierAr = "وضع تعديل APK (${activePermCount} صلاحية نشطة)"
        )

        val phases = listOf(
            AdaptivePhase(
                index = 1,
                title = "تحليل حزمة APK المستوردة",
                subtitle = "فحص الهوية الحالية، الأيقونة، ومكونات الحزمة المستخرجة",
                badge = "APK",
                accentHex = "#10B981",
                supportsLiveWebPreview = false,
                features = listOf(
                    DynamicPhaseFeature("apk_app_name", "تعديل اسم التطبيق المثبّت", "الاسم الذي يظهر في شاشة الهاتف", "الهوية", DynamicFeatureType.TEXT_INPUT, "app.name", textValue = project.name),
                    DynamicPhaseFeature("apk_accent", "اللون الرئيسي لشاشة البداية والأزرار", "تغيير طابع الألوان", "مظهر", DynamicFeatureType.COLOR_PICKER, "app.accentColorHex", textValue = project.accentColorHex, options = listOf("#10B981", "#FF7A2F", "#38BDF8", "#A855F7", "#EC4899")),
                    DynamicPhaseFeature("apk_icon_pick", "تغيير أيقونة التطبيق الحقيقية", "اختيار صورة أيقونة جديدة من المعرض", "أيقونة", DynamicFeatureType.ACTION_TRIGGER, "action.pick_icon"),
                    DynamicPhaseFeature("apk_splash_enable", "شاشة البداية المتحركة (Splash)", "إضافة أو تخصيص شاشة إقلاع احترافية", "إقلاع", DynamicFeatureType.SWITCH, "splash.enabled", boolValue = project.splashConfig.enabled)
                )
            ),
            AdaptivePhase(
                index = 2,
                title = "تعديل الحزمة والإصدار والتوقيع",
                subtitle = "إعادة تعيين Package Name ورقم الإصدار وتوقيع v1-v4",
                badge = "الحزمة",
                accentHex = "#FF7A2F",
                supportsLiveWebPreview = false,
                features = listOf(
                    DynamicPhaseFeature("apk_pkg", "معرّف الحزمة الجديد (Package Name)", "غيّره لتثبيت نسخة معدّلة بجانب الأصلية", "حزمة", DynamicFeatureType.TEXT_INPUT, "pkg.packageName", textValue = project.packageConfig.packageName),
                    DynamicPhaseFeature("apk_ver_name", "إصدار التطبيق (Version Name)", "مثال: 2.0.1", "إصدار", DynamicFeatureType.TEXT_INPUT, "pkg.versionName", textValue = project.packageConfig.versionName),
                    DynamicPhaseFeature("apk_ver_code", "رقم البناء (Version Code: ${project.packageConfig.versionCode})", "رفع رقم الإصدار لتحديث التطبيقات", "إصدار", DynamicFeatureType.SLIDER, "pkg.versionCode", intValue = project.packageConfig.versionCode.coerceIn(1, 50), minInt = 1, maxInt = 50),
                    DynamicPhaseFeature("apk_keystore", "توليد شهادة توقيع جديدة AES-256-GCM", "ضروري بعد أي تعديل على ملف APK", "توقيع", DynamicFeatureType.ACTION_TRIGGER, "action.generate_keystore")
                )
            ),
            AdaptivePhase(
                index = 3,
                title = "إدارة وتعديل الصلاحيات",
                subtitle = "تشغيل أو إزالة أذونات النظام داخل Manifest قبل إعادة التجميع",
                badge = "الصلاحيات",
                accentHex = "#38BDF8",
                supportsLiveWebPreview = false,
                features = listOf(
                    DynamicPhaseFeature("apk_perm_cam", "صلاحية الكاميرا (CAMERA)", "سماح أو منع الوصول للكاميرا", "أذونات", DynamicFeatureType.SWITCH, "perm.camera", boolValue = perms.camera),
                    DynamicPhaseFeature("apk_perm_loc", "صلاحية الموقع الجغرافي (LOCATION)", "سماح أو منع تتبع الموقع", "أذونات", DynamicFeatureType.SWITCH, "perm.location", boolValue = perms.location),
                    DynamicPhaseFeature("apk_perm_mic", "صلاحية الميكروفون (RECORD_AUDIO)", "سماح أو منع تسجيل الصوت", "أذونات", DynamicFeatureType.SWITCH, "perm.microphone", boolValue = perms.microphone),
                    DynamicPhaseFeature("apk_perm_notif", "صلاحية الإشعارات (POST_NOTIFICATIONS)", "التحكم في إشعارات النظام", "أذونات", DynamicFeatureType.SWITCH, "perm.notifications", boolValue = perms.notifications),
                    DynamicPhaseFeature("apk_perm_bt", "صلاحية البلوتوث (BLUETOOTH)", "الاتصال بالأجهزة القريبة", "أذونات", DynamicFeatureType.SWITCH, "perm.bluetooth", boolValue = perms.bluetooth),
                    DynamicPhaseFeature("apk_perm_net", "حالة الشبكة (ACCESS_NETWORK_STATE)", "مراقبة الاتصال بالإنترنت", "أذونات", DynamicFeatureType.SWITCH, "perm.networkState", boolValue = perms.networkState)
                )
            ),
            AdaptivePhase(
                index = 4,
                title = "التحصين الأمني والتوافق",
                subtitle = "تفعيل حماية الشاشة، تشفير الاتصال، وواجهة شرح الأذونات",
                badge = "تحصين",
                accentHex = "#A855F7",
                supportsLiveWebPreview = false,
                features = listOf(
                    DynamicPhaseFeature("apk_flag_secure", "منع لقطات الشاشة (FLAG_SECURE)", "حماية شاشات الـ APK من التصوير", "حماية", DynamicFeatureType.SWITCH, "pro.flagSecureEnabled", boolValue = project.proConfig.flagSecureEnabled),
                    DynamicPhaseFeature("apk_ssl", "تفعيل SSL Pinning الصارم", "حماية حركة الشبكة داخل التطبيق", "شبكة", DynamicFeatureType.SWITCH, "pro.sslPinningEnabled", boolValue = project.proConfig.sslPinningEnabled),
                    DynamicPhaseFeature("apk_adblock", "حقن مانع الإعلانات الداخلي", "حجب نطاقات التتبع والإعلانات", "تحسين", DynamicFeatureType.SWITCH, "pro.adAndPopupBlocker", boolValue = project.proConfig.adAndPopupBlocker),
                    DynamicPhaseFeature("apk_rationale", "تفعيل شاشة الأذونات الذكية", "عرض سبب الصلاحية للمستخدم", "خصوصية", DynamicFeatureType.SWITCH, "omni.smartPermissionsRationaleEnabled", boolValue = project.omniConfig.smartPermissionsRationaleEnabled)
                )
            )
        )

        return AdaptiveWorkspacePlan(
            sourceKind = AdaptiveSourceKind.APK_IMPORT,
            report = report,
            phases = phases
        )
    }

    private fun buildAiRoadmapAdaptivePlan(
        project: AppProject,
        aiPlan: AiArchitectPlan
    ): AdaptiveWorkspacePlan {
        val phases = aiPlan.roadmap.mapIndexed { idx, stage ->
            val stageFeatures = when (stage.category.lowercase()) {
                "identity", "design", "ui" -> listOf(
                    DynamicPhaseFeature("ai_name_$idx", "اسم التطبيق", "تأكيد الاسم المقترح من AI", "هوية", DynamicFeatureType.TEXT_INPUT, "app.name", textValue = project.name),
                    DynamicPhaseFeature("ai_color_$idx", "اللون الأساسي المقترح", "لوحة ألوان 3D متناسقة", "ألوان", DynamicFeatureType.COLOR_PICKER, "app.accentColorHex", textValue = project.accentColorHex, options = listOf(aiPlan.accentColorHex, "#FF7A2F", "#38BDF8", "#10B981", "#A855F7", "#EC4899")),
                    DynamicPhaseFeature("ai_grad_$idx", "تدرجات الخلفية الذكية", "إضاءة وعمق 3D للواجهات", "مظهر", DynamicFeatureType.SWITCH, "omni.gradientEnabled", boolValue = project.omniConfig.gradientEnabled),
                    DynamicPhaseFeature("ai_font_$idx", "تناسب الخطوط (${project.omniConfig.fontScalePercent}%)", "ضبط مقاس النصوص العربية والإنجليزية", "خطوط", DynamicFeatureType.SLIDER, "omni.fontScalePercent", intValue = project.omniConfig.fontScalePercent, minInt = 80, maxInt = 140),
                    DynamicPhaseFeature("ai_icon_$idx", "اختيار أيقونة مخصصة", "أيقونة حقيقية تظهر على الهاتف", "أيقونة", DynamicFeatureType.ACTION_TRIGGER, "action.pick_icon")
                )
                "auth", "security" -> listOf(
                    DynamicPhaseFeature("ai_auth_$idx", "تسجيل الدخول (Auth Builder)", "Google / Email / Phone", "مصادقة", DynamicFeatureType.SWITCH, "omni.authBuilderEnabled", boolValue = project.omniConfig.authBuilderEnabled),
                    DynamicPhaseFeature("ai_secure_$idx", "حماية الشاشة FLAG_SECURE", "منع التصوير والتسجيل", "أمان", DynamicFeatureType.SWITCH, "pro.flagSecureEnabled", boolValue = project.proConfig.flagSecureEnabled),
                    DynamicPhaseFeature("ai_ssl_$idx", "تصلب SSL Pinning", "اتصال HTTPS مشدد", "أمان", DynamicFeatureType.SWITCH, "pro.sslPinningEnabled", boolValue = project.proConfig.sslPinningEnabled),
                    DynamicPhaseFeature("ai_key_$idx", "توليد شهادة توقيع AES-256", "توقيع التطبيق المشفّر", "تشفير", DynamicFeatureType.ACTION_TRIGGER, "action.generate_keystore")
                )
                "data", "api", "backend" -> listOf(
                    DynamicPhaseFeature("ai_db_$idx", "قاعدة البيانات المحلية (Key-Value)", "حفظ السجلات والمفضلة أوفلاين", "بيانات", DynamicFeatureType.SWITCH, "omni.localDbEnabled", boolValue = project.omniConfig.localDbEnabled),
                    DynamicPhaseFeature("ai_api_$idx", "ربط خادم REST API", "اتصال مباشر بواجهات JSON", "API", DynamicFeatureType.TEXT_INPUT, "omni.apiBaseUrl", textValue = project.omniConfig.apiBaseUrl),
                    DynamicPhaseFeature("ai_acc_$idx", "مسرّع الأداء Cache-First", "تحميل فوري وضغط صور", "سرعة", DynamicFeatureType.SWITCH, "omni.appAcceleratorEnabled", boolValue = project.omniConfig.appAcceleratorEnabled)
                )
                "navigation", "ux" -> listOf(
                    DynamicPhaseFeature("ai_nav_$idx", "شريط التنقل السفلي", "تبويبات سريعة بين الأقسام", "تنقل", DynamicFeatureType.SWITCH, "bottomNav.enabled", boolValue = project.bottomNavConfig.enabled),
                    DynamicPhaseFeature("ai_drawer_$idx", "القائمة الجانبية (Drawer)", "وصول سريع للأقسام والإعدادات", "تنقل", DynamicFeatureType.SWITCH, "pro.drawerEnabled", boolValue = project.proConfig.drawerEnabled),
                    DynamicPhaseFeature("ai_fab_$idx", "زر الإجراء العائم (FAB)", "تواصل سريع ودعم مباشر", "تفاعل", DynamicFeatureType.SWITCH, "pro.fabMenuEnabled", boolValue = project.proConfig.fabMenuEnabled),
                    DynamicPhaseFeature("ai_fs_$idx", "وضع ملء الشاشة (Full Screen)", "تجربة غامرة بدون أشرطة", "عرض", DynamicFeatureType.SWITCH, "web.fullScreen", boolValue = project.webViewConfig.fullScreen)
                )
                "notifications", "permissions" -> listOf(
                    DynamicPhaseFeature("ai_push_$idx", "الإشعارات الفورية (Push)", "تفعيل قنوات التنبيه المباشر", "إشعارات", DynamicFeatureType.SWITCH, "push.enabled", boolValue = project.pushConfig.enabled),
                    DynamicPhaseFeature("ai_cam_$idx", "صلاحية الكاميرا", "لالتقاط الصور ومسح QR", "أذونات", DynamicFeatureType.SWITCH, "perm.camera", boolValue = project.permissionsConfig.camera),
                    DynamicPhaseFeature("ai_loc_$idx", "صلاحية الموقع (GPS)", "لتحديد الموقع والخدمات القريبة", "أذونات", DynamicFeatureType.SWITCH, "perm.location", boolValue = project.permissionsConfig.location),
                    DynamicPhaseFeature("ai_rat_$idx", "واجهة توضيح الأذونات الذكية", "شرح كل إذن قبل الطلب", "أذونات", DynamicFeatureType.SWITCH, "omni.smartPermissionsRationaleEnabled", boolValue = project.omniConfig.smartPermissionsRationaleEnabled)
                )
                else -> listOf(
                    DynamicPhaseFeature("ai_pkg_$idx", "معرّف الحزمة (Package ID)", "الهوية البرمجية على أندرويد", "حزمة", DynamicFeatureType.TEXT_INPUT, "pkg.packageName", textValue = project.packageConfig.packageName),
                    DynamicPhaseFeature("ai_ver_$idx", "إصدار البناء (Version Name)", "مثال: 1.0.0", "إصدار", DynamicFeatureType.TEXT_INPUT, "pkg.versionName", textValue = project.packageConfig.versionName),
                    DynamicPhaseFeature("ai_pwa_$idx", "فتح محرر تخصيص الواجهة الحية", "تعديل النصوص والأزرار والعناصر مباشرة", "محرر", DynamicFeatureType.ACTION_TRIGGER, "action.open_pwa_editor"),
                    DynamicPhaseFeature("ai_bridge_$idx", "الجسر البرمجي v2 (Hardware APIs)", "البصمة، الاهتزاز، مشاركة النظام", "جسر", DynamicFeatureType.SWITCH, "bridge.jsBridgeEnabled", boolValue = project.bridgeConfig.jsBridgeEnabled)
                )
            }

            AdaptivePhase(
                index = idx + 1,
                title = stage.titleAr,
                subtitle = stage.descriptionAr,
                badge = "AI #${idx + 1}",
                accentHex = aiPlan.accentColorHex,
                supportsLiveWebPreview = true,
                features = stageFeatures
            )
        }

        val report = SourceComplexityReport(
            sourceKind = AdaptiveSourceKind.AI_SCRATCH,
            imageCount = 4,
            buttonCount = 6,
            formInputCount = 2,
            textBlockCount = 8,
            sectionCount = phases.size,
            scriptCount = 2,
            permissionCount = 3,
            recommendedPhaseCount = phases.size,
            complexityTierAr = "خطة ذكاء اصطناعي متكاملة (${phases.size} مرحلة)"
        )

        return AdaptiveWorkspacePlan(
            sourceKind = AdaptiveSourceKind.AI_SCRATCH,
            report = report,
            phases = phases
        )
    }

    fun applyFeatureEdit(
        context: Context,
        project: AppProject,
        feature: DynamicPhaseFeature,
        newBool: Boolean = feature.boolValue,
        newInt: Int = feature.intValue,
        newText: String = feature.textValue
    ): AppProject {
        var updated = when (feature.bindingKey) {
            "app.name" -> project.copy(name = newText.ifBlank { project.name })
            "app.accentColorHex" -> project.copy(accentColorHex = newText, primaryColorHex = newText)
            "splash.enabled" -> project.copy(splashConfig = project.splashConfig.copy(enabled = newBool))
            "omni.gradientEnabled" -> project.copy(omniConfig = project.omniConfig.copy(gradientEnabled = newBool))
            "omni.fontScalePercent" -> project.copy(omniConfig = project.omniConfig.copy(fontScalePercent = newInt.coerceIn(75, 145)))
            "omni.customHeroTitle" -> project.copy(omniConfig = project.omniConfig.copy(customHeroTitle = newText))
            "omni.customPrimaryButtonText" -> project.copy(omniConfig = project.omniConfig.copy(customPrimaryButtonText = newText))
            "omni.customHideSelectorsCsv" -> project.copy(omniConfig = project.omniConfig.copy(customHideSelectorsCsv = newText))
            "omni.appAcceleratorEnabled" -> project.copy(
                omniConfig = project.omniConfig.copy(appAcceleratorEnabled = newBool),
                proConfig = project.proConfig.copy(assetPreloaderEnabled = newBool)
            )
            "omni.localDbEnabled" -> project.copy(omniConfig = project.omniConfig.copy(localDbEnabled = newBool))
            "omni.apiBaseUrl" -> project.copy(
                omniConfig = project.omniConfig.copy(apiEndpointsEnabled = newText.isNotBlank(), apiBaseUrl = newText)
            )
            "omni.authBuilderEnabled" -> project.copy(omniConfig = project.omniConfig.copy(authBuilderEnabled = newBool))
            "omni.smartPermissionsRationaleEnabled" -> project.copy(omniConfig = project.omniConfig.copy(smartPermissionsRationaleEnabled = newBool))
            "pro.forceDarkCss" -> project.copy(proConfig = project.proConfig.copy(forceDarkCss = newBool))
            "pro.customCss" -> project.copy(proConfig = project.proConfig.copy(customCss = newText))
            "pro.drawerEnabled" -> project.copy(proConfig = project.proConfig.copy(drawerEnabled = newBool))
            "pro.fabMenuEnabled" -> project.copy(proConfig = project.proConfig.copy(fabMenuEnabled = newBool))
            "pro.adAndPopupBlocker" -> project.copy(proConfig = project.proConfig.copy(adAndPopupBlocker = newBool))
            "pro.flagSecureEnabled" -> project.copy(proConfig = project.proConfig.copy(flagSecureEnabled = newBool))
            "pro.sslPinningEnabled" -> project.copy(proConfig = project.proConfig.copy(sslPinningEnabled = newBool))
            "web.fullScreen" -> project.copy(webViewConfig = project.webViewConfig.copy(fullScreen = newBool))
            "web.pullToRefresh" -> project.copy(webViewConfig = project.webViewConfig.copy(pullToRefresh = newBool))
            "bottomNav.enabled" -> project.copy(bottomNavConfig = project.bottomNavConfig.copy(enabled = newBool))
            "bridge.jsBridgeEnabled" -> project.copy(bridgeConfig = project.bridgeConfig.copy(jsBridgeEnabled = newBool))
            "pkg.packageName" -> project.copy(packageConfig = project.packageConfig.copy(packageName = newText))
            "pkg.versionName" -> project.copy(packageConfig = project.packageConfig.copy(versionName = newText))
            "pkg.versionCode" -> project.copy(packageConfig = project.packageConfig.copy(versionCode = newInt.coerceAtLeast(1)))
            "perm.camera" -> project.copy(permissionsConfig = project.permissionsConfig.copy(camera = newBool))
            "perm.location" -> project.copy(permissionsConfig = project.permissionsConfig.copy(location = newBool))
            "perm.microphone" -> project.copy(permissionsConfig = project.permissionsConfig.copy(microphone = newBool))
            "perm.notifications" -> project.copy(permissionsConfig = project.permissionsConfig.copy(notifications = newBool))
            "perm.bluetooth" -> project.copy(permissionsConfig = project.permissionsConfig.copy(bluetooth = newBool))
            "perm.networkState" -> project.copy(permissionsConfig = project.permissionsConfig.copy(networkState = newBool))
            "push.enabled" -> project.copy(
                pushConfig = project.pushConfig.copy(enabled = newBool),
                permissionsConfig = project.permissionsConfig.copy(notifications = if (newBool) true else project.permissionsConfig.notifications)
            )
            else -> applyDomOverrideEdit(project, feature.bindingKey, newBool, newText)
        }

        syncDomOverridesIntoWorkspace(context, updated)
        return updated
    }

    private fun applyDomOverrideEdit(
        project: AppProject,
        bindingKey: String,
        newBool: Boolean,
        newText: String
    ): AppProject {
        val currentMap = runCatching {
            val obj = JSONObject(project.omniConfig.domEditsJson.takeIf { it.startsWith("{") } ?: "{}")
            obj
        }.getOrElse { JSONObject() }

        if (bindingKey.startsWith("dom.img.") || bindingKey.startsWith("dom.btn.visible.")) {
            currentMap.put(bindingKey, newBool)
        } else if (bindingKey.startsWith("dom.")) {
            currentMap.put(bindingKey, newText)
        }
        return project.copy(
            omniConfig = project.omniConfig.copy(domEditsJson = currentMap.toString())
        )
    }

    private fun syncDomOverridesIntoWorkspace(context: Context, project: AppProject) {
        val domObj = runCatching {
            val raw = project.omniConfig.domEditsJson
            if (raw.startsWith("{")) JSONObject(raw) else JSONObject()
        }.getOrNull() ?: JSONObject()

        val wsDirPath = project.localWorkspacePath ?: return
        val wsDir = File(wsDirPath)
        if (!wsDir.exists()) return

        val dynamicCss = buildString {
            domObj.keys().forEach { key ->
                when {
                    key.startsWith("dom.img.visible.") && !domObj.optBoolean(key, true) -> {
                        val idx = key.substringAfterLast(".").toIntOrNull() ?: return@forEach
                        append("img:nth-of-type(${idx + 1}){display:none!important;}\n")
                    }
                    key.startsWith("dom.img.rounded.") && domObj.optBoolean(key, false) -> {
                        val idx = key.substringAfterLast(".").toIntOrNull() ?: return@forEach
                        append("img:nth-of-type(${idx + 1}){border-radius:18px!important;box-shadow:0 10px 24px rgba(0,0,0,0.35)!important;}\n")
                    }
                }
            }
        }

        val dynamicJs = buildString {
            append("(function(){")
            append("var imgs=document.querySelectorAll('img');")
            append("var btns=document.querySelectorAll('button,a.btn,.btn,[role=\"button\"]');")
            append("var texts=document.querySelectorAll('h1,h2,h3,h4,p');")
            domObj.keys().forEach { key ->
                when {
                    key.startsWith("dom.img.visible.") && !domObj.optBoolean(key, true) -> {
                        val idx = key.substringAfterLast(".").toIntOrNull() ?: return@forEach
                        append("if(imgs[$idx])imgs[$idx].style.display='none';")
                    }
                    key.startsWith("dom.img.rounded.") && domObj.optBoolean(key, false) -> {
                        val idx = key.substringAfterLast(".").toIntOrNull() ?: return@forEach
                        append("if(imgs[$idx]){imgs[$idx].style.borderRadius='18px';imgs[$idx].style.boxShadow='0 10px 24px rgba(0,0,0,0.35)';}")
                    }
                    key.startsWith("dom.btn.visible.") && !domObj.optBoolean(key, true) -> {
                        val idx = key.substringAfterLast(".").toIntOrNull() ?: return@forEach
                        append("if(btns[$idx])btns[$idx].style.display='none';")
                    }
                    key.startsWith("dom.btn.text.") -> {
                        val idx = key.substringAfterLast(".").toIntOrNull() ?: return@forEach
                        val safe = domObj.optString(key).replace("'", "\\'")
                        if (safe.isNotBlank()) append("if(btns[$idx])btns[$idx].textContent='$safe';")
                    }
                    key.startsWith("dom.text.replace.") -> {
                        val idx = key.substringAfterLast(".").toIntOrNull() ?: return@forEach
                        val safe = domObj.optString(key).replace("'", "\\'")
                        if (safe.isNotBlank()) append("if(texts[$idx])texts[$idx].textContent='$safe';")
                    }
                }
            }
            append("})();")
        }

        runCatching {
            File(wsDir, "omni-adaptive-overrides.css").writeText(dynamicCss, Charsets.UTF_8)
            File(wsDir, "omni-adaptive-overrides.js").writeText(dynamicJs, Charsets.UTF_8)
            val entry = LocalWorkspaceEngine.findEntryHtml(wsDir)
            if (entry != null && entry.exists()) {
                val html = entry.readText(Charsets.UTF_8)
                var patched = html
                if (!patched.contains("omni-adaptive-overrides.css") && patched.contains("</head>", true)) {
                    patched = patched.replace("</head>", "<link rel=\"stylesheet\" href=\"omni-adaptive-overrides.css\"></head>", true)
                }
                if (!patched.contains("omni-adaptive-overrides.js") && patched.contains("</body>", true)) {
                    patched = patched.replace("</body>", "<script src=\"omni-adaptive-overrides.js\"></script></body>", true)
                }
                if (patched != html) entry.writeText(patched, Charsets.UTF_8)
            }
        }
    }

    private fun resolveSourceHtml(project: AppProject, htmlOverride: String?): String {
        if (!htmlOverride.isNullOrBlank()) return htmlOverride
        val wsPath = project.localWorkspacePath
        if (!wsPath.isNullOrBlank()) {
            val entry = LocalWorkspaceEngine.findEntryHtml(File(wsPath))
            if (entry != null && entry.exists()) {
                return runCatching { entry.readText(Charsets.UTF_8) }.getOrDefault("")
            }
        }
        return buildDefaultTemplateHtml(project)
    }

    private fun buildDefaultTemplateHtml(project: AppProject): String = """
        <!DOCTYPE html><html lang="ar" dir="rtl"><body>
        <header><h1>${project.name}</h1><p>${project.description}</p></header>
        <section>
          <img src="hero.png" alt="صورة العرض الرئيسية"/>
          <img src="card1.png" alt="صورة القسم الأول"/>
          <img src="card2.png" alt="صورة القسم الثاني"/>
          <button class="btn-primary">ابدأ الآن</button>
          <button class="btn-secondary">اكتشف المزيد</button>
        </section>
        </body></html>
    """.trimIndent()

    /**
     * فحص ملفات APK الموجودة في التنزيلات والتطبيقات المثبتة عبر Launcher Intent (متوافق مع Play Policy)
     */
    suspend fun scanDeviceAndDownloadedApks(
        context: Context,
        downloads: List<DownloadedApk>
    ): List<ApkInspectCandidate> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ApkInspectCandidate>()
        val pm = context.packageManager

        // 1) من تنزيلات ApkForge الداخلية
        downloads.filter { it.filePath.endsWith(".apk", ignoreCase = true) }.forEach { dl ->
            val file = File(dl.filePath)
            val perms = if (file.exists()) {
                extractArchivePermissions(pm, file.absolutePath)
            } else {
                emptyList()
            }
            results.add(
                ApkInspectCandidate(
                    id = "dl_${dl.id}",
                    appName = dl.projectName,
                    packageName = dl.packageName,
                    versionName = dl.versionName,
                    versionCode = dl.versionCode,
                    sourcePath = dl.filePath,
                    permissions = perms,
                    sourceOriginLabel = "التنزيلات (${dl.sizeText})",
                    iconImageUri = dl.iconImageUri
                )
            )
        }

        // 2) من التطبيقات المثبتة التي تدعم Launcher (دون استخدام QUERY_ALL_PACKAGES المخالفة)
        runCatching {
            val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }
            val seenPkgs = HashSet<String>()
            resolved.forEach { info ->
                val pkgName = info.activityInfo?.packageName ?: return@forEach
                if (pkgName == context.packageName || !seenPkgs.add(pkgName)) return@forEach
                val appInfo = info.activityInfo?.applicationInfo ?: return@forEach
                // ركز على تطبيقات المستخدم أو أبرز التطبيقات المتاحة
                val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                if (!isUserApp && results.size >= 14) return@forEach

                val pkgInfo = getPackageInfoCompat(pm, pkgName)
                val label = info.loadLabel(pm)?.toString().orEmpty().ifBlank { pkgName }
                val iconPath = persistDrawableToCache(context, info.loadIcon(pm), pkgName)
                val perms = pkgInfo?.requestedPermissions?.toList().orEmpty()
                val vName = pkgInfo?.versionName ?: "1.0"
                val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    (pkgInfo?.longVersionCode ?: 1L).toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo?.versionCode ?: 1
                }

                results.add(
                    ApkInspectCandidate(
                        id = "app_$pkgName",
                        appName = label,
                        packageName = pkgName,
                        versionName = vName,
                        versionCode = vCode,
                        sourcePath = appInfo.sourceDir.orEmpty(),
                        permissions = perms,
                        sourceOriginLabel = if (isUserApp) "تطبيق مثبّت" else "تطبيق نظام",
                        iconImageUri = iconPath
                    )
                )
            }
        }

        results
    }

    suspend fun inspectPickedApkUri(context: Context, uri: Uri): ApkInspectCandidate? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "imported_apks").apply { mkdirs() }
        val target = File(dir, "picked_${System.currentTimeMillis()}.apk")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { out -> input.copyTo(out) }
            }
            if (!target.exists() || target.length() == 0L) return@withContext null

            val pm = context.packageManager
            val flags = PackageManager.GET_PERMISSIONS
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(target.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(target.absolutePath, flags)
            }
            pkgInfo?.applicationInfo?.sourceDir = target.absolutePath
            pkgInfo?.applicationInfo?.publicSourceDir = target.absolutePath

            val appName = pkgInfo?.applicationInfo?.loadLabel(pm)?.toString().orEmpty()
                .ifBlank { LocalWorkspaceEngine.getDisplayName(context, uri, "ImportedApp").substringBeforeLast(".") }
            val pkgName = pkgInfo?.packageName ?: "com.apkforge.imported"
            val iconUri = pkgInfo?.applicationInfo?.loadIcon(pm)?.let { persistDrawableToCache(context, it, pkgName) }
            val vName = pkgInfo?.versionName ?: "1.0"
            val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                (pkgInfo?.longVersionCode ?: 1L).toInt()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo?.versionCode ?: 1
            }

            ApkInspectCandidate(
                id = "file_${System.currentTimeMillis()}",
                appName = appName,
                packageName = pkgName,
                versionName = vName,
                versionCode = vCode,
                sourcePath = target.absolutePath,
                permissions = pkgInfo?.requestedPermissions?.toList().orEmpty(),
                sourceOriginLabel = "ملف APK من الجهاز",
                iconImageUri = iconUri
            )
        }.getOrNull()
    }

    fun convertCandidateToProject(candidate: ApkInspectCandidate): AppProject {
        val perms = candidate.permissions.joinToString(",")
        return AppProject(
            id = "proj_apk_${System.currentTimeMillis()}",
            name = candidate.appName.take(28),
            description = "مشروع تعديل APK مستورد من ${candidate.packageName}",
            accentColorHex = "#10B981",
            primaryColorHex = "#10B981",
            iconImageUri = candidate.iconImageUri,
            sourceType = SourceType.APK_PACKAGE,
            sourceValue = candidate.sourcePath.ifBlank { candidate.packageName },
            packageConfig = PackageConfig(
                packageName = candidate.packageName,
                versionName = candidate.versionName,
                versionCode = candidate.versionCode.coerceAtLeast(1)
            ),
            permissionsConfig = PermissionsConfig(
                camera = perms.contains("CAMERA", ignoreCase = true),
                location = perms.contains("LOCATION", ignoreCase = true),
                microphone = perms.contains("RECORD_AUDIO", ignoreCase = true),
                notifications = perms.contains("POST_NOTIFICATIONS", ignoreCase = true),
                storage = perms.contains("STORAGE", ignoreCase = true),
                vibration = perms.contains("VIBRATE", ignoreCase = true),
                bluetooth = perms.contains("BLUETOOTH", ignoreCase = true),
                networkState = perms.contains("ACCESS_NETWORK_STATE", ignoreCase = true)
            )
        )
    }

    private fun extractArchivePermissions(pm: PackageManager, apkPath: String): List<String> = runCatching {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apkPath, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkPath, PackageManager.GET_PERMISSIONS)
        }
        info?.requestedPermissions?.toList().orEmpty()
    }.getOrDefault(emptyList())

    private fun getPackageInfoCompat(pm: PackageManager, pkg: String): PackageInfo? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS)
        }
    }.getOrNull()

    private fun persistDrawableToCache(context: Context, drawable: Drawable?, key: String): String? {
        if (drawable == null) return null
        return runCatching {
            val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
                drawable.bitmap
            } else {
                val w = drawable.intrinsicWidth.coerceIn(64, 192)
                val h = drawable.intrinsicHeight.coerceIn(64, 192)
                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }
            val dir = File(context.filesDir, "apk_icons").apply { mkdirs() }
            val outFile = File(dir, "${key.replace(Regex("[^a-zA-Z0-9_]"), "_")}.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, fos)
            }
            Uri.fromFile(outFile).toString()
        }.getOrNull()
    }
}
