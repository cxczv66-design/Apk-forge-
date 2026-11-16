package com.example.engine

import android.content.Context
import android.os.Build
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType
import java.io.File

/**
 * تنقيح السجلات وحماية الرموز السرية ومفاتيح التوقيع من التسرب في السجلات أو التقارير.
 */
object BuildLogSanitizer {

    private val githubTokenRegex = Regex("""\b(ghp|github_pat|gho|ghu|ghs)_[A-Za-z0-9_]{8,}\b""")
    private val bearerRegex = Regex("""(?i)\bBearer\s+[A-Za-z0-9._\-+/=]{8,}\b""")
    private val secretKeyValRegex = Regex(
        """(?i)(token|password|storePassword|keyPassword|ks-pass|key-pass|SIGNING_KEYSTORE_BASE64|secret)\s*([=:])\s*([^\s,;]+)""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val pemPrivateKeyRegex = Regex(
        """-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----"""
    )

    fun redactSensitive(raw: String): String {
        if (raw.isBlank()) return raw
        var out = raw
        out = pemPrivateKeyRegex.replace(out, "[REDACTED_PRIVATE_KEY]")
        out = githubTokenRegex.replace(out, "[REDACTED_GITHUB_TOKEN]")
        out = bearerRegex.replace(out, "Bearer [REDACTED]")
        out = secretKeyValRegex.replace(out) { m ->
            "${m.groupValues[1]}${m.groupValues[2]}[REDACTED]"
        }
        return out
    }
}

enum class LocalCapabilitySupport(val labelAr: String) {
    NATIVE_VERIFIED("حقيقي على الجهاز"),
    ON_DEVICE_PACKAGER("تغليف محلي متوافق"),
    REQUIRES_CLOUD_SDK("يتطلب Gradle + Android SDK سحابياً")
}

data class BuildCapabilityDescriptor(
    val id: String,
    val titleAr: String,
    val detailAr: String,
    val support: LocalCapabilitySupport
)

data class LocalCapabilitiesProfile(
    val deviceAbi: String,
    val freeStorageMb: Long,
    val hasExecutableNativeAapt2: Boolean,
    val hasExecutableNativeD8: Boolean,
    val capabilities: List<BuildCapabilityDescriptor>
) {
    val nativeCount: Int get() = capabilities.count { it.support == LocalCapabilitySupport.NATIVE_VERIFIED }
    val cloudRequiredCount: Int get() = capabilities.count { it.support == LocalCapabilitySupport.REQUIRES_CLOUD_SDK }
}

object BuildCapabilitiesInspector {

    fun inspect(context: Context, freeStorageMb: Long = -1L): LocalCapabilitiesProfile {
        val binDir = File(context.filesDir, "apkforge_toolchain/bin")
        val aapt2Bin = File(binDir, "aapt2")
        val d8Bin = File(binDir, "d8")
        val hasNativeAapt2 = aapt2Bin.exists() && aapt2Bin.canExecute() && isElfExecutable(aapt2Bin)
        val hasNativeD8 = d8Bin.exists() && d8Bin.canExecute() && isElfExecutable(d8Bin)

        val storage = if (freeStorageMb >= 0L) freeStorageMb else runCatching {
            val stat = android.os.StatFs(context.filesDir.absolutePath)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024L)
        }.getOrDefault(0L)

        val caps = listOf(
            BuildCapabilityDescriptor(
                id = "keyvault",
                titleAr = "خزنة مفاتيح التوقيع (AES-256-GCM + RSA-2048)",
                detailAr = "توليد شهادات X.509 وتشفير المفتاح الخاص داخل AndroidKeyStore/PBKDF2 دون أي نص مكشوف على القرص.",
                support = LocalCapabilitySupport.NATIVE_VERIFIED
            ),
            BuildCapabilityDescriptor(
                id = "apksig",
                titleAr = "توقيع Google apksig الرسمي (v1 / v2 / v3 / v4) + ApkVerifier",
                detailAr = "توقيع وفحص تشفيري حقيقي عبر مكتبة com.android.tools.build:apksig داخل التطبيق.",
                support = LocalCapabilitySupport.NATIVE_VERIFIED
            ),
            BuildCapabilityDescriptor(
                id = "zipalign",
                titleAr = "محاذاة ZipAlign (4-Byte / 4096-Byte) + CRC-32",
                detailAr = "محاذاة دقيقة لملفات STORED والموارد للتحميل المباشر على أندرويد.",
                support = LocalCapabilitySupport.NATIVE_VERIFIED
            ),
            BuildCapabilityDescriptor(
                id = "aapt2_res",
                titleAr = if (hasNativeAapt2) "AAPT2 الأصلي لترجمة الموارد (Native ELF)" else "مُرمّز الموارد المحلي (AXML + ARSC + Adaptive PNG)",
                detailAr = if (hasNativeAapt2) {
                    "ثنائي aapt2 تنفيذي متاح على الجهاز لترجمة الموارد."
                } else {
                    "يُنشئ Binary AndroidManifest.xml و resources.arsc والأيقونات محلياً؛ دمج مكتبات AAR الكامل يتم سحابياً."
                },
                support = if (hasNativeAapt2) LocalCapabilitySupport.NATIVE_VERIFIED else LocalCapabilitySupport.ON_DEVICE_PACKAGER
            ),
            BuildCapabilityDescriptor(
                id = "d8_assets",
                titleAr = if (hasNativeD8) "مترجم Dalvik D8 الأصلي" else "مُجمّع أصول الويب + حاوية DEX 035",
                detailAr = "يحزّم ملفات HTML/JS/CSS و quantum_app_config.json مع DEX 035 قياسي؛ ترجمة كود Kotlin/Compose الكاملة تحتاج Gradle السحابي.",
                support = if (hasNativeD8) LocalCapabilitySupport.NATIVE_VERIFIED else LocalCapabilitySupport.ON_DEVICE_PACKAGER
            ),
            BuildCapabilityDescriptor(
                id = "gradle_kotlin",
                titleAr = "ترجمة Kotlin / Jetpack Compose / AndroidX عبر Gradle",
                detailAr = "مشاريع Jetpack Compose و Material 3 و Kotlin الكاملة (AI / قوالب / Biometric / Room) تُبنى سحابياً عبر Android SDK & Gradle 8.7.",
                support = LocalCapabilitySupport.REQUIRES_CLOUD_SDK
            ),
            BuildCapabilityDescriptor(
                id = "play_aab",
                titleAr = "بناء حزمة Google Play الإنتاجية (bundleRelease AAB)",
                detailAr = "توليد App Bundle الرسمي المعتمد في Google Play (bundletool / AGP) يتم عبر محرك البناء السحابي.",
                support = LocalCapabilitySupport.REQUIRES_CLOUD_SDK
            )
        )

        return LocalCapabilitiesProfile(
            deviceAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a",
            freeStorageMb = storage,
            hasExecutableNativeAapt2 = hasNativeAapt2,
            hasExecutableNativeD8 = hasNativeD8,
            capabilities = caps
        )
    }

    fun projectRequiresCloudGradleForFullFidelity(project: AppProject, targetType: String): List<String> {
        val reasons = mutableListOf<String>()
        if (targetType.equals("aab", ignoreCase = true) || targetType.equals("both", ignoreCase = true)) {
            reasons.add("الهدف ${targetType.uppercase()} يتطلب Android Gradle Plugin (bundleRelease) لإنتاج App Bundle معتمد لـ Google Play.")
        }
        val pro = project.proConfig
        val omni = project.omniConfig
        val nativeFeatures = buildList {
            if (pro.drawerEnabled) add("NativeDrawerLayout")
            if (pro.fabMenuEnabled) add("NativeFloatingMenu")
            if (pro.sslPinningEnabled) add("StrictSSLPinning")
            if (pro.flagSecureEnabled) add("FlagSecureGuard")
            if (omni.localDbEnabled) add("LocalRoom/SQLite")
            if (omni.authBuilderEnabled) add("NativeAuthBuilder")
            if (project.pushConfig.enabled && project.pushConfig.appId.isNotBlank()) add("PushSDK(${project.pushConfig.provider})")
            if (project.bridgeConfig.adMobAppId.isNotBlank()) add("AdMobSDK")
            if (project.sourceType == SourceType.AI_SCRATCH) add("JetpackComposeAiApp")
            if (project.sourceType == SourceType.KOTLIN_TEMPLATE) add("JetpackComposeMaterial3")
        }
        if (nativeFeatures.isNotEmpty()) {
            reasons.add("الميزات الأصلية المفعّلة (${nativeFeatures.joinToString("، ")}) تحتاج مترجم Kotlin و AndroidX عبر Gradle.")
        }
        return reasons
    }

    private fun isElfExecutable(file: File): Boolean {
        return runCatching {
            if (file.length() < 4) return@runCatching false
            val magic = file.inputStream().use { it.readNBytes(4) }
            magic.size == 4 &&
                magic[0] == 0x7F.toByte() &&
                magic[1] == 'E'.code.toByte() &&
                magic[2] == 'L'.code.toByte() &&
                magic[3] == 'F'.code.toByte()
        }.getOrDefault(false)
    }
}
