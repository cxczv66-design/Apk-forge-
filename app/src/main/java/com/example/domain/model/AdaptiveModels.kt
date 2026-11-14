package com.example.domain.model

/**
 * نماذج محرك الذكاء التكيفي (Omni-Adaptive Models):
 * يدير تحليل تعقيد المصادر، توليد المراحل التكيفية، الميزات الديناميكية،
 * وخطط مساعد الذكاء الاصطناعي للبناء من الصفر.
 */

enum class AdaptiveSourceKind(val labelAr: String, val badgeColorHex: String) {
    WEB_SITE("موقع ويب / PWA", "#38BDF8"),
    HTML_ZIP("ملفات HTML / ZIP", "#FF7A2F"),
    APK_IMPORT("حزمة APK مستوردة", "#10B981"),
    AI_SCRATCH("مولّد بالذكاء الاصطناعي", "#A855F7")
}

data class SourceComplexityReport(
    val sourceKind: AdaptiveSourceKind,
    val imageCount: Int,
    val buttonCount: Int,
    val formInputCount: Int,
    val textBlockCount: Int,
    val sectionCount: Int,
    val scriptCount: Int,
    val permissionCount: Int,
    val extractedImageLabels: List<String> = emptyList(),
    val extractedButtonLabels: List<String> = emptyList(),
    val extractedHeadings: List<String> = emptyList(),
    val extractedPermissions: List<String> = emptyList(),
    val recommendedPhaseCount: Int,
    val complexityTierAr: String
)

enum class DynamicFeatureType {
    SWITCH,
    SLIDER,
    TEXT_INPUT,
    COLOR_PICKER,
    CHIP_GROUP,
    ACTION_TRIGGER
}

data class DynamicPhaseFeature(
    val id: String,
    val title: String,
    val subtitle: String,
    val categoryTag: String,
    val type: DynamicFeatureType,
    val bindingKey: String,
    val boolValue: Boolean = false,
    val intValue: Int = 100,
    val minInt: Int = 0,
    val maxInt: Int = 100,
    val textValue: String = "",
    val options: List<String> = emptyList()
)

data class AdaptivePhase(
    val index: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val accentHex: String,
    val supportsLiveWebPreview: Boolean,
    val features: List<DynamicPhaseFeature>
)

data class AdaptiveWorkspacePlan(
    val sourceKind: AdaptiveSourceKind,
    val report: SourceComplexityReport,
    val phases: List<AdaptivePhase>
)

data class ApkInspectCandidate(
    val id: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val sourcePath: String,
    val permissions: List<String>,
    val sourceOriginLabel: String,
    val iconImageUri: String? = null
)

data class AiSmartQuestion(
    val id: String,
    val questionAr: String,
    val kind: String, // "boolean" or "choice"
    val options: List<String> = emptyList(),
    val boolAnswer: Boolean = true,
    val selectedOption: String = ""
)

data class AiRoadmapStage(
    val stageNumber: Int,
    val titleAr: String,
    val descriptionAr: String,
    val featureCountHint: Int,
    val category: String
)

data class AiArchitectPlan(
    val ideaPrompt: String,
    val appNameAr: String,
    val packageSuffix: String,
    val summaryAr: String,
    val accentColorHex: String,
    val roadmap: List<AiRoadmapStage>,
    val smartQuestions: List<AiSmartQuestion>
)
