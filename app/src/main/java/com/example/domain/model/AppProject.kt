package com.example.domain.model

/**
 * كيانات المشروع لنظام ApkForge
 */

enum class SourceType(val titleAr: String, val descAr: String) {
    HTML_FOLDER("ملف HTML", "مستند html واحد"),
    ZIP_ARCHIVE("موقع مضغوط", "ملف zip يحتوي على index.html"),
    WEB_URL("رابط ويب (URL)", "تحويل موقع ويب مباشر إلى تطبيق"),
    APK_PACKAGE("ملف / حزمة APK", "استيراد وتعديل APK موجود"),
    AI_SCRATCH("بناء بالذكاء الاصطناعي", "توليد المراحل والميزات من الصفر عبر AI"),
    KOTLIN_TEMPLATE("قالب Kotlin", "تطبيق أندرويد متقدم")
}

data class SplashConfig(
    val enabled: Boolean = true,
    val backgroundColorHex: String = "#0F1318",
    val fadeBackground: Boolean = false,
    val indicatorStyle: String = "حلقة", // بدون، حلقة، شريط، حلقة - L، حلقة - S، نبض
    val iconSizeDp: Int = 120,
    val iconCornerRadiusDp: Int = 16,
    val indicatorSizeDp: Int = 48,
    val spacingDp: Int = 28
)

data class OnboardingSlide(
    val title: String,
    val description: String,
    val iconName: String = "bolt",
    val imageUri: String? = null
)

data class OnboardingConfig(
    val enabled: Boolean = false,
    val slides: List<OnboardingSlide> = listOf(
        OnboardingSlide("العنوان", "الوصف", "bolt")
    ),
    val layoutType: String = "بطاقة سفلية",
    val imageShape: String = "دائرة",
    val gradientBackground: Boolean = false,
    val accentColorHex: String = "#FFE236",
    val textColorHex: String = "#111827",
    val cardColorHex: String = "#FFFFFF",
    val showSkipButton: Boolean = true
)

data class PageLoaderConfig(
    val enabled: Boolean = false,
    val style: String = "حلقة", // حلقة، شريط، حلقة بالشعار، شريط بالشعار
    val sizeDp: Int = 78,
    val thicknessDp: Int = 7,
    val showOnCard: Boolean = true,
    val cardColorHex: String = "#FFFFFF",
    val cardOpacityPercent: Int = 100,
    val cardCornerRadiusDp: Int = 12
)

data class WebViewConfig(
    val fullScreen: Boolean = false,
    val pullToRefresh: Boolean = false,
    val pageLoader: PageLoaderConfig = PageLoaderConfig(),
    val supportZoom: Boolean = false,
    val cacheEnabled: Boolean = true,
    val fileDownload: Boolean = true,
    val offlineFallback: Boolean = true
)

data class BottomNavDestination(
    val id: String,
    val title: String,
    val targetUrl: String,
    val iconName: String = "home"
)

data class BottomNavConfig(
    val enabled: Boolean = false,
    val destinations: List<BottomNavDestination> = listOf(
        BottomNavDestination("1", "Home", "/")
    ),
    val barStyle: String = "مسطح",
    val showLabels: Boolean = true,
    val activeColorHex: String = "#FF7A2F",
    val inactiveColorHex: String = "#9CA3AF",
    val barColorHex: String = "#FFFFFF"
)

data class PackageConfig(
    val packageName: String = "com.zk.app",
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val minSdk: Int = 24,
    val targetSdk: Int = 34
)

data class PermissionsConfig(
    val camera: Boolean = false,
    val location: Boolean = false,
    val microphone: Boolean = false,
    val notifications: Boolean = false,
    val storage: Boolean = false,
    val vibration: Boolean = false,
    val bluetooth: Boolean = false,
    val networkState: Boolean = false,
    val autoDetected: Boolean = false,
    val storeJustificationNotes: String = ""
)

data class PushNotificationConfig(
    val enabled: Boolean = false,
    val provider: String = "OneSignal",
    val appId: String = "",
    val channelName: String = "التنبيهات العامة",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val badgeEnabled: Boolean = true,
    val openUrlOnTap: Boolean = true,
    val autoPromptOnFirstLaunch: Boolean = true,
    val testTitle: String = "مرحباً بك في التطبيق!",
    val testBody: String = "تم تفعيل الإشعارات الفورية بنجاح."
)

data class SigningKeyConfig(
    val useDebugKey: Boolean = true,
    val keyAlias: String = "apkforge_key",
    val keystorePath: String = ""
)

/**
 * إعدادات الجسر البرمجي الأصلي (JS Bridge)، الروابط العميقة، والبناء السحابي CI/CD
 */
data class BridgeAndCloudConfig(
    val jsBridgeEnabled: Boolean = true,
    val handleExternalIntents: Boolean = true, // wa.me, tel:, mailto:, payment schemes
    val deepLinkDomain: String = "", // مثال: app.mydomain.com
    val adMobAppId: String = "", // معرّف AdMob الاختياري
    val remoteConfigUrl: String = "", // رابط التحديثات والإعدادات عن بُعد
    val gitHubCiEnabled: Boolean = true
)

/**
 * حزمة المزايا الاحترافية المتقدمة:
 * 1) حاقن CSS/JS + الوضع الليلي + مانع الإعلانات
 * 2) الجسر البرمجي v2 (البصمة، QR، التقييم داخل التطبيق)
 * 3) الأيقونة المتكيّفة (Adaptive Icon Studio)
 * 4) الأمان والتخزين المسبق (FLAG_SECURE, SSL Pinning, Preloader)
 * 5) القائمة الجانبية (Drawer) والزر العائم (FAB)
 */
data class ProStudioConfig(
    // Web Customizer & Ad-Blocker
    val customCss: String = "",
    val customJs: String = "",
    val forceDarkCss: Boolean = false,
    val adAndPopupBlocker: Boolean = true,
    val blockedHostsCsv: String = "doubleclick.net,googlesyndication.com,adservice.google.com",

    // Bridge v2 Hardware & Store APIs
    val biometricAuthBridge: Boolean = true,
    val qrScannerBridge: Boolean = true,
    val inAppReviewBridge: Boolean = true,

    // Adaptive Icon Studio Spec
    val iconShape: String = "squircle", // squircle, circle, rounded, teardrop
    val iconBgColorHex: String = "#1A1E29",

    // Security Guard & Asset Preloader
    val flagSecureEnabled: Boolean = false,
    val sslPinningEnabled: Boolean = false,
    val assetPreloaderEnabled: Boolean = true,

    // Navigation Drawer & Floating Action Button Menu
    val drawerEnabled: Boolean = false,
    val drawerHeaderSubtitle: String = "القائمة السريعة",
    val fabMenuEnabled: Boolean = false,
    val fabWhatsappNumber: String = ""
)

/**
 * إعدادات بيئة التطوير المتكاملة (Omni Full-Stack IDE Config):
 * تشمل محرر التدرجات والخطوط، بناء المصادقة وقاعدة البيانات المحلية وواجهات API،
 * تعدد اللغات، مسرّع التطبيق، الأذونات الذكية، وتخصيص عناصر مواقع الويب المحوّلة (PWA-to-Native).
 */
data class OmniIdeConfig(
    // Design & Typography
    val gradientEnabled: Boolean = false,
    val gradientStartHex: String = "#FF7A2F",
    val gradientEndHex: String = "#7C3AED",
    val gradientAngleDeg: Int = 135,
    val fontScalePercent: Int = 100,

    // Functionality: Auth, Local DB, REST API, i18n
    val authBuilderEnabled: Boolean = false,
    val authGoogleEnabled: Boolean = true,
    val authEmailEnabled: Boolean = true,
    val authPhoneEnabled: Boolean = false,
    val localDbEnabled: Boolean = false,
    val localDbTableName: String = "app_records",
    val apiEndpointsEnabled: Boolean = false,
    val apiBaseUrl: String = "",
    val apiEndpointsJson: String = "[]",
    val i18nSupportedCodesCsv: String = "ar,en",
    val i18nDefaultCode: String = "ar",

    // Smart Permissions Rationale & App Accelerator
    val smartPermissionsRationaleEnabled: Boolean = true,
    val appAcceleratorEnabled: Boolean = true,
    val acceleratorPrefetchLinks: Boolean = true,
    val acceleratorImageCompression: Boolean = true,

    // PWA-to-Native Live Site Customization Overrides
    val customHeroTitle: String = "",
    val customPrimaryButtonText: String = "",
    val customHideSelectorsCsv: String = "",
    val customBannerImageUri: String? = null,
    val domEditsJson: String = "[]"
)

/**
 * سجل تاريخ الإصدارات المبنية لكل مشروع
 */
data class BuildHistoryEntry(
    val id: String,
    val versionCode: Int,
    val versionName: String,
    val targetType: String,
    val fileName: String,
    val sizeText: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BuildStatus {
    IDLE,
    PREPARING,
    BUILDING,
    SUCCESS,
    FAILED
}

data class AppProject(
    val id: String,
    val name: String,
    val description: String = "",
    val iconName: String = "default_app",
    val iconImageUri: String? = null,
    val primaryColorHex: String = "#FF7A2F",
    val accentColorHex: String = "#FF7A2F",
    val sourceType: SourceType = SourceType.ZIP_ARCHIVE,
    val sourceValue: String = "MegaEditor_New.zip",
    val localWorkspacePath: String? = null,
    val splashConfig: SplashConfig = SplashConfig(),
    val onboardingConfig: OnboardingConfig = OnboardingConfig(),
    val webViewConfig: WebViewConfig = WebViewConfig(),
    val bottomNavConfig: BottomNavConfig = BottomNavConfig(),
    val packageConfig: PackageConfig = PackageConfig(),
    val permissionsConfig: PermissionsConfig = PermissionsConfig(),
    val pushConfig: PushNotificationConfig = PushNotificationConfig(),
    val signingConfig: SigningKeyConfig = SigningKeyConfig(),
    val bridgeConfig: BridgeAndCloudConfig = BridgeAndCloudConfig(),
    val proConfig: ProStudioConfig = ProStudioConfig(),
    val omniConfig: OmniIdeConfig = OmniIdeConfig(),
    val buildHistory: List<BuildHistoryEntry> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastBuiltAt: Long? = null,
    val lastBuildStatus: BuildStatus = BuildStatus.IDLE,
    val lastOutputApk: String? = null
)
