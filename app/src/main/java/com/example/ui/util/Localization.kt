package com.example.ui.util

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.example.ui.theme.CairoFontFamily

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String, val isRtl: Boolean) {
    ARABIC("ar", "العربية", "العربية", true),
    ENGLISH("en", "English", "English", false),
    FRENCH("fr", "Français", "Français", false),
    SPANISH("es", "Español", "Español", false),
    TURKISH("tr", "Türkçe", "Türkçe", false);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: ARABIC
        }
    }
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.ARABIC }

object AppStrings {
    fun appTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ApkForge Studio"
        AppLanguage.FRENCH -> "Studio ApkForge"
        AppLanguage.SPANISH -> "Estudio ApkForge"
        AppLanguage.TURKISH -> "ApkForge Stüdyo"
        else -> "استوديو ApkForge"
    }

    fun myRecentProjects(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Recent Projects"
        AppLanguage.FRENCH -> "Projets Récents"
        AppLanguage.SPANISH -> "Proyectos Recientes"
        AppLanguage.TURKISH -> "Son Projeler"
        else -> "مشاريعي الأخيرة"
    }

    fun noProjectsTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "No projects created yet"
        AppLanguage.FRENCH -> "Aucun projet créé pour l'instant"
        AppLanguage.SPANISH -> "No hay proyectos creados aún"
        AppLanguage.TURKISH -> "Henüz oluşturulmuş proje yok"
        else -> "لا توجد مشاريع سابقة بعد"
    }

    fun noProjectsDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Start creating your first Android app now by picking a ZIP archive or HTML folder."
        AppLanguage.FRENCH -> "Commencez à créer votre application en choisissant une archive ZIP ou un dossier HTML."
        AppLanguage.SPANISH -> "Comience a crear su primera aplicación eligiendo un archivo ZIP o una carpeta HTML."
        AppLanguage.TURKISH -> "Bir ZIP arşivi veya HTML klasörü seçerek ilk uygulamanızı oluşturmaya başlayın."
        else -> "ابدأ بإنشاء أول تطبيق أندرويد لك الآن باختيار أرشيف ZIP أو مجلد HTML."
    }

    fun createNewProject(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "+ Create New App"
        AppLanguage.FRENCH -> "+ Créer Nouvelle Application"
        AppLanguage.SPANISH -> "+ Crear Nueva Aplicación"
        AppLanguage.TURKISH -> "+ Yeni Uygulama Oluştur"
        else -> "+ بدء مشروع جديد"
    }

    fun zipWebsite(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ZIP Website"
        AppLanguage.FRENCH -> "Site Web ZIP"
        AppLanguage.SPANISH -> "Sitio Web ZIP"
        AppLanguage.TURKISH -> "ZIP Web Sitesi"
        else -> "موقع مضغوط"
    }

    fun zipWebsiteDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "ZIP file containing index.html and assets"
        AppLanguage.FRENCH -> "Fichier ZIP contenant index.html et ressources"
        AppLanguage.SPANISH -> "Archivo ZIP que contiene index.html y recursos"
        AppLanguage.TURKISH -> "index.html ve kaynakları içeren ZIP dosyası"
        else -> "ملف zip يحتوي على index.html ومجلدات الأصول"
    }

    fun htmlFolder(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "HTML Document"
        AppLanguage.FRENCH -> "Document HTML"
        AppLanguage.SPANISH -> "Documento HTML"
        AppLanguage.TURKISH -> "HTML Belgesi"
        else -> "ملف HTML"
    }

    fun htmlFolderDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Single HTML document with embedded CSS/JS"
        AppLanguage.FRENCH -> "Document HTML unique avec CSS/JS intégrés"
        AppLanguage.SPANISH -> "Documento HTML individual con CSS/JS integrados"
        AppLanguage.TURKISH -> "Gömülü CSS/JS içeren tek HTML belgesi"
        else -> "مستند html واحد مدمج أو بسيط"
    }

    fun downloads(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Downloads"
        AppLanguage.FRENCH -> "Téléchargements"
        AppLanguage.SPANISH -> "Descargas"
        AppLanguage.TURKISH -> "İndirilenler"
        else -> "التنزيلات"
    }

    fun downloadsDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Built APK & AAB files"
        AppLanguage.FRENCH -> "Fichiers APK & AAB générés"
        AppLanguage.SPANISH -> "Archivos APK y AAB generados"
        AppLanguage.TURKISH -> "Oluşturulan APK ve AAB dosyaları"
        else -> "ملفات APK و AAB المبنية"
    }

    fun account(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Account"
        AppLanguage.FRENCH -> "Compte"
        AppLanguage.SPANISH -> "Cuenta"
        AppLanguage.TURKISH -> "Hesap"
        else -> "الحساب"
    }

    fun accountDesc(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Google Profile & Settings"
        AppLanguage.FRENCH -> "Profil Google et Paramètres"
        AppLanguage.SPANISH -> "Perfil de Google y Ajustes"
        AppLanguage.TURKISH -> "Google Profili ve Ayarlar"
        else -> "الملف الشخصي وإدارة الإعدادات"
    }

    fun quantumEngineStatus(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Quantum Build Engine"
        AppLanguage.FRENCH -> "Moteur Quantum Local"
        AppLanguage.SPANISH -> "Motor Quantum Local"
        AppLanguage.TURKISH -> "Quantum Derleme Motoru"
        else -> "محرك Quantum المدمج"
    }

    fun back(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Back"
        AppLanguage.FRENCH -> "Retour"
        AppLanguage.SPANISH -> "Volver"
        AppLanguage.TURKISH -> "Geri"
        else -> "رجوع"
    }

    fun next(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Next"
        AppLanguage.FRENCH -> "Suivant"
        AppLanguage.SPANISH -> "Siguiente"
        AppLanguage.TURKISH -> "İleri"
        else -> "التالي"
    }

    fun finishAndSave(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Finish & Save ✓"
        AppLanguage.FRENCH -> "Terminer & Enregistrer ✓"
        AppLanguage.SPANISH -> "Finalizar y Guardar ✓"
        AppLanguage.TURKISH -> "Bitir ve Kaydet ✓"
        else -> "إنهاء وحفظ ✓"
    }

    fun fullPhoneSimulator(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "📱 Live Interactive Phone"
        AppLanguage.FRENCH -> "📱 Téléphone Interactif"
        AppLanguage.SPANISH -> "📱 Teléfono Interactivo"
        AppLanguage.TURKISH -> "📱 Canlı Etkileşimli Telefon"
        else -> "📱 محاكي الهاتف التفاعلي"
    }

    fun buildApk(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Build APK"
        AppLanguage.FRENCH -> "Générer APK"
        AppLanguage.SPANISH -> "Compilar APK"
        AppLanguage.TURKISH -> "APK Derle"
        else -> "بناء APK"
    }

    fun buildAab(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Build AAB"
        AppLanguage.FRENCH -> "Générer AAB"
        AppLanguage.SPANISH -> "Compilar AAB"
        AppLanguage.TURKISH -> "AAB Derle"
        else -> "بناء AAB"
    }

    fun keystoreTitle(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Signing Keystore"
        AppLanguage.FRENCH -> "Clé de Signature"
        AppLanguage.SPANISH -> "Clave de Firma"
        AppLanguage.TURKISH -> "İmzalama Anahtarı"
        else -> "مفتاح التوقيع"
    }

    fun googleSignIn(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Continue with Google"
        AppLanguage.FRENCH -> "Continuer avec Google"
        AppLanguage.SPANISH -> "Continuar con Google"
        AppLanguage.TURKISH -> "Google ile devam et"
        else -> "المتابعة باستخدام Google"
    }

    fun chooseGoogleAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Choose an account for ApkForge"
        AppLanguage.FRENCH -> "Choisissez un compte pour ApkForge"
        AppLanguage.SPANISH -> "Elige una cuenta para ApkForge"
        AppLanguage.TURKISH -> "ApkForge için bir hesap seçin"
        else -> "اختر حساباً للمتابعة إلى ApkForge"
    }

    fun termsAndPrivacy(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Terms of Service & Privacy Policy"
        AppLanguage.FRENCH -> "Conditions d'utilisation et Confidentialité"
        AppLanguage.SPANISH -> "Términos de servicio y Privacidad"
        AppLanguage.TURKISH -> "Hizmet Şartları ve Gizlilik Politikası"
        else -> "شروط الخدمة وسياسة الخصوصية"
    }

    fun iAgree(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "I agree to the Terms of Service & Privacy Policy"
        AppLanguage.FRENCH -> "J'accepte les conditions d'utilisation et la confidentialité"
        AppLanguage.SPANISH -> "Acepto los términos de servicio y la política de privacidad"
        AppLanguage.TURKISH -> "Hizmet şartlarını ve gizlilik politikasını kabul ediyorum"
        else -> "أوافق على شروط الخدمة وسياسة الخصوصية لـ ApkForge"
    }

    fun agreeAndContinue(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Agree & Continue"
        AppLanguage.FRENCH -> "Accepter et Continuer"
        AppLanguage.SPANISH -> "Aceptar y Continuar"
        AppLanguage.TURKISH -> "Kabul Et ve Devam Et"
        else -> "موافقة ومتابعة"
    }

    fun cancel(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Cancel"
        AppLanguage.FRENCH -> "Annuler"
        AppLanguage.SPANISH -> "Cancelar"
        AppLanguage.TURKISH -> "İptal"
        else -> "إلغاء"
    }

    fun language(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "App Language"
        AppLanguage.FRENCH -> "Langue de l'application"
        AppLanguage.SPANISH -> "Idioma de la aplicación"
        AppLanguage.TURKISH -> "Uygulama Dili"
        else -> "لغة التطبيق"
    }

    fun signOut(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Sign Out"
        AppLanguage.FRENCH -> "Se déconnecter"
        AppLanguage.SPANISH -> "Cerrar sesión"
        AppLanguage.TURKISH -> "Çıkış Yap"
        else -> "تسجيل الخروج"
    }

    fun switchAccount(lang: AppLanguage) = when (lang) {
        AppLanguage.ENGLISH -> "Switch Account"
        AppLanguage.FRENCH -> "Changer de compte"
        AppLanguage.SPANISH -> "Cambiar de cuenta"
        AppLanguage.TURKISH -> "Hesap Değiştir"
        else -> "تبديل الحساب"
    }
}
