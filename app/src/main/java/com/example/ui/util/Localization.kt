package com.example.ui.util

import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", "Arabic (RTL)", true),
    ENGLISH("en", "English", "English (LTR)", false),
    FRENCH("fr", "Français", "French (LTR)", false),
    SPANISH("es", "Español", "Spanish (LTR)", false),
    TURKISH("tr", "Türkçe", "Turkish (LTR)", false),
    GERMAN("de", "Deutsch", "German (LTR)", false),
    PORTUGUESE("pt", "Português", "Portuguese (LTR)", false),
    INDONESIAN("id", "Bahasa Indonesia", "Indonesian (LTR)", false),
    HINDI("hi", "हिन्दी", "Hindi (LTR)", false),
    URDU("ur", "اردو", "Urdu (RTL)", true);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ARABIC
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }

object AppStrings {
    fun appTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "استوديو ApkForge"
        AppLanguage.URDU -> "ApkForge اسٹوڈیو"
        AppLanguage.FRENCH -> "Studio ApkForge"
        AppLanguage.SPANISH -> "Estudio ApkForge"
        AppLanguage.TURKISH -> "ApkForge Stüdyo"
        AppLanguage.GERMAN -> "ApkForge Studio"
        AppLanguage.PORTUGUESE -> "Estúdio ApkForge"
        AppLanguage.INDONESIAN -> "Studio ApkForge"
        AppLanguage.HINDI -> "ApkForge स्टूडियो"
        AppLanguage.ENGLISH -> "ApkForge Studio"
    }

    fun myRecentProjects(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مشاريعي الأخيرة"
        AppLanguage.URDU -> "حالیہ پروجیکٹس"
        AppLanguage.FRENCH -> "Projets Récents"
        AppLanguage.SPANISH -> "Proyectos Recientes"
        AppLanguage.TURKISH -> "Son Projeler"
        AppLanguage.GERMAN -> "Aktuelle Projekte"
        AppLanguage.PORTUGUESE -> "Projetos Recentes"
        AppLanguage.INDONESIAN -> "Proyek Terbaru"
        AppLanguage.HINDI -> "हाल के प्रोजेक्ट्स"
        AppLanguage.ENGLISH -> "Recent Projects"
    }

    fun noProjectsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "لا توجد مشاريع سابقة بعد"
        AppLanguage.URDU -> "ابھی کوئی پروجیکٹ موجود نہیں"
        AppLanguage.FRENCH -> "Aucun projet créé pour l'instant"
        AppLanguage.SPANISH -> "No hay proyectos creados aún"
        AppLanguage.TURKISH -> "Henüz oluşturulmuş proje yok"
        AppLanguage.GERMAN -> "Noch keine Projekte erstellt"
        AppLanguage.PORTUGUESE -> "Nenhum projeto criado ainda"
        AppLanguage.INDONESIAN -> "Belum ada proyek yang dibuat"
        AppLanguage.HINDI -> "अभी तक कोई प्रोजेक्ट नहीं है"
        AppLanguage.ENGLISH -> "No projects created yet"
    }

    fun noProjectsDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "ابدأ بإنشاء أول تطبيق أندرويد لك الآن باختيار أرشيف ZIP أو مجلد HTML أو تحويل رابط موقع."
        AppLanguage.URDU -> "ابھی ZIP آرکائیو، HTML یا ویب سائٹ لنک سے اپنی پہلی اینڈرائیڈ ایپ بنائیں۔"
        AppLanguage.FRENCH -> "Créez votre première application Android via ZIP, HTML ou URL."
        AppLanguage.SPANISH -> "Crea tu primera app Android desde ZIP, HTML o URL."
        AppLanguage.TURKISH -> "ZIP, HTML veya web sitesi bağlantısından ilk Android uygulamanızı oluşturun."
        AppLanguage.GERMAN -> "Erstelle deine erste Android-App per ZIP, HTML oder Web-URL."
        AppLanguage.PORTUGUESE -> "Crie seu primeiro app Android a partir de ZIP, HTML ou URL."
        AppLanguage.INDONESIAN -> "Buat aplikasi Android pertamamu dari ZIP, HTML, atau URL."
        AppLanguage.HINDI -> "ZIP, HTML या वेबसाइट लिंक से अपना पहला एंड्रॉइड ऐप बनाएं।"
        AppLanguage.ENGLISH -> "Create your first Android app now by picking a ZIP archive, HTML file, or Website URL."
    }

    fun createNewProject(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "+ بدء مشروع جديد"
        AppLanguage.URDU -> "+ نیا پروجیکٹ شروع کریں"
        AppLanguage.FRENCH -> "+ Nouveau Projet"
        AppLanguage.SPANISH -> "+ Nuevo Proyecto"
        AppLanguage.TURKISH -> "+ Yeni Proje"
        AppLanguage.GERMAN -> "+ Neues Projekt"
        AppLanguage.PORTUGUESE -> "+ Novo Projeto"
        AppLanguage.INDONESIAN -> "+ Proyek Baru"
        AppLanguage.HINDI -> "+ नया प्रोजेक्ट"
        AppLanguage.ENGLISH -> "+ Create New App"
    }

    fun zipWebsite(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "موقع مضغوط"
        AppLanguage.URDU -> "ZIP ویب سائٹ"
        AppLanguage.FRENCH -> "Site Web ZIP"
        AppLanguage.SPANISH -> "Sitio Web ZIP"
        AppLanguage.TURKISH -> "ZIP Web Sitesi"
        AppLanguage.GERMAN -> "ZIP-Website"
        AppLanguage.PORTUGUESE -> "Site ZIP"
        AppLanguage.INDONESIAN -> "Situs ZIP"
        AppLanguage.HINDI -> "ZIP वेबसाइट"
        AppLanguage.ENGLISH -> "ZIP Website"
    }

    fun zipWebsiteDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "ملف zip يحتوي على index.html ومجلدات الأصول"
        AppLanguage.URDU -> "index.html اور اثاثوں والی ZIP فائل"
        AppLanguage.FRENCH -> "Fichier ZIP contenant index.html et ressources"
        AppLanguage.SPANISH -> "Archivo ZIP con index.html y recursos"
        AppLanguage.TURKISH -> "index.html ve kaynakları içeren ZIP dosyası"
        AppLanguage.GERMAN -> "ZIP-Archiv mit index.html & Assets"
        AppLanguage.PORTUGUESE -> "Arquivo ZIP com index.html e ativos"
        AppLanguage.INDONESIAN -> "File ZIP berisi index.html & aset"
        AppLanguage.HINDI -> "index.html और एसेट्स वाली ZIP फ़ाइल"
        AppLanguage.ENGLISH -> "ZIP file containing index.html and assets"
    }

    fun htmlFolder(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "ملف HTML"
        AppLanguage.URDU -> "HTML فائل"
        AppLanguage.FRENCH -> "Document HTML"
        AppLanguage.SPANISH -> "Documento HTML"
        AppLanguage.TURKISH -> "HTML Belgesi"
        AppLanguage.GERMAN -> "HTML-Datei"
        AppLanguage.PORTUGUESE -> "Documento HTML"
        AppLanguage.INDONESIAN -> "Dokumen HTML"
        AppLanguage.HINDI -> "HTML दस्तावेज़"
        AppLanguage.ENGLISH -> "HTML Document"
    }

    fun htmlFolderDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مستند html واحد مدمج أو بسيط"
        AppLanguage.URDU -> "CSS/JS کے ساتھ سنگل HTML دستاویز"
        AppLanguage.FRENCH -> "Document HTML unique avec CSS/JS"
        AppLanguage.SPANISH -> "Documento HTML con CSS/JS integrados"
        AppLanguage.TURKISH -> "CSS/JS içeren tek HTML belgesi"
        AppLanguage.GERMAN -> "Einzelne HTML-Datei mit CSS/JS"
        AppLanguage.PORTUGUESE -> "Documento HTML único com CSS/JS"
        AppLanguage.INDONESIAN -> "Dokumen HTML tunggal dengan CSS/JS"
        AppLanguage.HINDI -> "CSS/JS के साथ एकल HTML फ़ाइल"
        AppLanguage.ENGLISH -> "Single HTML document with embedded CSS/JS"
    }

    fun downloads(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "التنزيلات"
        AppLanguage.URDU -> "ڈاؤن لوڈز"
        AppLanguage.FRENCH -> "Téléchargements"
        AppLanguage.SPANISH -> "Descargas"
        AppLanguage.TURKISH -> "İndirilenler"
        AppLanguage.GERMAN -> "Downloads"
        AppLanguage.PORTUGUESE -> "Downloads"
        AppLanguage.INDONESIAN -> "Unduhan"
        AppLanguage.HINDI -> "डाउनलोड्स"
        AppLanguage.ENGLISH -> "Downloads"
    }

    fun downloadsDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "ملفات APK و AAB و APKS المبنية"
        else -> "Built APK, AAB & APKS packages"
    }

    fun account(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "الحساب"
        AppLanguage.URDU -> "اکاؤنٹ"
        AppLanguage.FRENCH -> "Compte"
        AppLanguage.SPANISH -> "Cuenta"
        AppLanguage.TURKISH -> "Hesap"
        AppLanguage.GERMAN -> "Konto"
        AppLanguage.PORTUGUESE -> "Conta"
        AppLanguage.INDONESIAN -> "Akun"
        AppLanguage.HINDI -> "खाता"
        AppLanguage.ENGLISH -> "Account"
    }

    fun accountDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "الملف الشخصي وإدارة الإعدادات"
        else -> "Profile, Language & Data Lifecycle"
    }

    fun quantumEngineStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "محرك Quantum المدمج"
        AppLanguage.URDU -> "Quantum بلڈ انجن"
        else -> "Quantum Build Engine"
    }

    fun developerToolsHub(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مركز أدوات المطورين (24 أداة)"
        AppLanguage.URDU -> "ڈویلپر ٹولز حب (24 ٹولز)"
        AppLanguage.FRENCH -> "Outils Développeur (24)"
        AppLanguage.SPANISH -> "Herramientas Dev (24)"
        AppLanguage.TURKISH -> "Geliştirici Araçları (24)"
        else -> "Developer Tools Hub (24)"
    }

    fun livePreviewSandbox(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "المعاينة الحية التفاعلية (Sandbox)"
        AppLanguage.URDU -> "لائیو پری ویو سینڈ باکس"
        else -> "Live Preview Sandbox"
    }

    fun supportAndFeatureReq(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "دعم فني وطلب ميزة"
        AppLanguage.URDU -> "ٹیکنیکل سپورٹ اور فیچر"
        AppLanguage.FRENCH -> "Support & Idées"
        AppLanguage.SPANISH -> "Soporte y Sugerencias"
        AppLanguage.TURKISH -> "Destek ve Özellik"
        else -> "Support & Feature"
    }

    fun clearAllData(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مسح جميع البيانات والمشاريع"
        AppLanguage.URDU -> "تمام ڈیٹا صاف کریں"
        AppLanguage.FRENCH -> "Effacer toutes les données"
        AppLanguage.SPANISH -> "Borrar todos los datos"
        AppLanguage.TURKISH -> "Tüm Verileri Temizle"
        else -> "Clear All Data"
    }

    fun deleteAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "حذف الحساب نهائياً"
        AppLanguage.URDU -> "اکاؤنٹ مستقل حذف کریں"
        AppLanguage.FRENCH -> "Supprimer le compte"
        AppLanguage.SPANISH -> "Eliminar cuenta"
        AppLanguage.TURKISH -> "Hesabı Kalıcı Sil"
        else -> "Delete Account"
    }

    fun back(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "رجوع"
        AppLanguage.URDU -> "واپس"
        AppLanguage.FRENCH -> "Retour"
        AppLanguage.SPANISH -> "Volver"
        AppLanguage.TURKISH -> "Geri"
        AppLanguage.GERMAN -> "Zurück"
        AppLanguage.PORTUGUESE -> "Voltar"
        AppLanguage.INDONESIAN -> "Kembali"
        AppLanguage.HINDI -> "वापस"
        AppLanguage.ENGLISH -> "Back"
    }

    fun next(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "التالي"
        AppLanguage.URDU -> "اگلا"
        AppLanguage.FRENCH -> "Suivant"
        AppLanguage.SPANISH -> "Siguiente"
        AppLanguage.TURKISH -> "İleri"
        AppLanguage.GERMAN -> "Weiter"
        AppLanguage.PORTUGUESE -> "Avançar"
        AppLanguage.INDONESIAN -> "Lanjut"
        AppLanguage.HINDI -> "अगला"
        AppLanguage.ENGLISH -> "Next"
    }

    fun finishAndSave(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إنهاء وحفظ ✓"
        AppLanguage.URDU -> "مکمل اور محفوظ ✓"
        AppLanguage.FRENCH -> "Terminer & Enregistrer ✓"
        AppLanguage.SPANISH -> "Finalizar y Guardar ✓"
        AppLanguage.TURKISH -> "Bitir ve Kaydet ✓"
        else -> "Finish & Save ✓"
    }

    fun fullPhoneSimulator(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "📱 محاكي الهاتف التفاعلي"
        else -> "📱 Live Interactive Phone"
    }

    fun buildApk(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بناء APK"
        AppLanguage.URDU -> "APK بنائیں"
        AppLanguage.FRENCH -> "Générer APK"
        AppLanguage.SPANISH -> "Compilar APK"
        AppLanguage.TURKISH -> "APK Derle"
        else -> "Build APK"
    }

    fun buildAab(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بناء AAB"
        AppLanguage.URDU -> "AAB بنائیں"
        AppLanguage.FRENCH -> "Générer AAB"
        AppLanguage.SPANISH -> "Compilar AAB"
        AppLanguage.TURKISH -> "AAB Derle"
        else -> "Build AAB"
    }

    fun buildApks(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بناء APKS"
        AppLanguage.URDU -> "APKS بنائیں"
        else -> "Build APKS"
    }

    fun keystoreTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "مفتاح التوقيع"
        else -> "Signing Keystore"
    }

    fun googleSignIn(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "المتابعة باستخدام Google"
        AppLanguage.URDU -> "Google کے ساتھ جاری رکھیں"
        AppLanguage.FRENCH -> "Continuer avec Google"
        AppLanguage.SPANISH -> "Continuar con Google"
        AppLanguage.TURKISH -> "Google ile devam et"
        AppLanguage.GERMAN -> "Mit Google anmelden"
        AppLanguage.PORTUGUESE -> "Continuar com Google"
        AppLanguage.INDONESIAN -> "Lanjutkan dengan Google"
        AppLanguage.HINDI -> "Google के साथ जारी रखें"
        AppLanguage.ENGLISH -> "Continue with Google"
    }

    fun chooseGoogleAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "اختر حساباً للمتابعة إلى ApkForge"
        else -> "Choose an account for ApkForge"
    }

    fun termsAndPrivacy(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "شروط الخدمة وسياسة الخصوصية"
        else -> "Terms of Service & Privacy Policy"
    }

    fun iAgree(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "أوافق على شروط الخدمة وسياسة الخصوصية لـ ApkForge"
        else -> "I agree to the Terms of Service & Privacy Policy"
    }

    fun agreeAndContinue(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "موافقة ومتابعة"
        else -> "Agree & Continue"
    }

    fun cancel(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "إلغاء"
        AppLanguage.URDU -> "منسوخ"
        AppLanguage.FRENCH -> "Annuler"
        AppLanguage.SPANISH -> "Cancelar"
        AppLanguage.TURKISH -> "İptal"
        AppLanguage.GERMAN -> "Abbrechen"
        else -> "Cancel"
    }

    fun language(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "لغة التطبيق والاتجاه (RTL/LTR)"
        AppLanguage.URDU -> "ایپ کی زبان (RTL/LTR)"
        AppLanguage.FRENCH -> "Langue (RTL/LTR)"
        AppLanguage.SPANISH -> "Idioma (RTL/LTR)"
        AppLanguage.TURKISH -> "Uygulama Dili (RTL/LTR)"
        AppLanguage.GERMAN -> "Sprache (RTL/LTR)"
        else -> "App Language (RTL/LTR)"
    }

    fun signOut(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تسجيل الخروج"
        AppLanguage.URDU -> "سائن آؤٹ"
        AppLanguage.FRENCH -> "Se déconnecter"
        AppLanguage.SPANISH -> "Cerrar sesión"
        AppLanguage.TURKISH -> "Çıkış Yap"
        AppLanguage.GERMAN -> "Abmelden"
        else -> "Sign Out"
    }

    fun switchAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تبديل الحساب"
        else -> "Switch Account"
    }

    fun navHome(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "الرئيسية"
        AppLanguage.URDU -> "ہوم"
        AppLanguage.FRENCH -> "Accueil"
        AppLanguage.SPANISH -> "Inicio"
        AppLanguage.TURKISH -> "Ana Sayfa"
        AppLanguage.GERMAN -> "Startseite"
        else -> "Home"
    }

    fun webScraperAndPwa(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تحويل موقع / PWA"
        AppLanguage.URDU -> "ویب / PWA کنورٹر"
        else -> "Web / PWA"
    }

    fun designWizard(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "معالج التصميم"
        AppLanguage.URDU -> "ڈیزائن وزرڈ"
        else -> "Design Wizard"
    }

    fun buildStudio(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "استوديو البناء"
        AppLanguage.URDU -> "بلڈ اسٹوڈیو"
        else -> "Build Studio"
    }

    fun ideStudioSubtitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "بيئة تطوير تطبيقات متكاملة على الهاتف"
        else -> "Full-Stack On-Device Android IDE"
    }

    fun technicalSupport(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "دعم فني"
        AppLanguage.URDU -> "ٹیکنیکل سپورٹ"
        else -> "Support"
    }

    fun requestFeature(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "اطلب ميزة"
        AppLanguage.URDU -> "فیچر طلب کریں"
        else -> "Request Feature"
    }

    fun saveToDownloads(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "حفظ في التنزيلات"
        AppLanguage.URDU -> "ڈاؤن لوڈز میں محفوظ کریں"
        else -> "Save to Downloads"
    }

    fun installNow(lang: AppLanguage) = when (lang) {
        AppLanguage.ARABIC -> "تثبيت مباشر"
        AppLanguage.URDU -> "ابھی انسٹال کریں"
        else -> "Install Now"
    }
}

data class LocalizedStringsSnapshot(
    val navHome: String,
    val htmlImport: String,
    val zipImport: String,
    val webScraperAndPwa: String,
    val designWizard: String,
    val developerToolsHub: String,
    val livePreviewSandbox: String,
    val buildStudio: String,
    val navDownloads: String,
    val supportAndFeatureReq: String,
    val navAccount: String,
    val ideStudioSubtitle: String,
    val technicalSupport: String,
    val requestFeature: String,
    val saveToDownloads: String,
    val installNow: String,
    val clearAllData: String,
    val deleteAccount: String
)

@androidx.compose.runtime.Composable
fun strings(): LocalizedStringsSnapshot {
    val lang = LocalAppLanguage.current
    return LocalizedStringsSnapshot(
        navHome = AppStrings.navHome(lang),
        htmlImport = AppStrings.htmlFolder(lang),
        zipImport = AppStrings.zipWebsite(lang),
        webScraperAndPwa = AppStrings.webScraperAndPwa(lang),
        designWizard = AppStrings.designWizard(lang),
        developerToolsHub = AppStrings.developerToolsHub(lang),
        livePreviewSandbox = AppStrings.livePreviewSandbox(lang),
        buildStudio = AppStrings.buildStudio(lang),
        navDownloads = AppStrings.downloads(lang),
        supportAndFeatureReq = AppStrings.supportAndFeatureReq(lang),
        navAccount = AppStrings.account(lang),
        ideStudioSubtitle = AppStrings.ideStudioSubtitle(lang),
        technicalSupport = AppStrings.technicalSupport(lang),
        requestFeature = AppStrings.requestFeature(lang),
        saveToDownloads = AppStrings.saveToDownloads(lang),
        installNow = AppStrings.installNow(lang),
        clearAllData = AppStrings.clearAllData(lang),
        deleteAccount = AppStrings.deleteAccount(lang)
    )
}

