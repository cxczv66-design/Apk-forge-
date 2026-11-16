package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.StatFs
import com.example.domain.model.AppProject

enum class HybridBuildMode(val labelAr: String, val subtitleAr: String) {
    AUTO_SMART("تلقائي ذكي", "يختار المحلي أو السحابي تلقائياً"),
    FORCE_LOCAL("بناء محلي", "محرك Quantum على الجهاز"),
    FORCE_CLOUD("سحابي Gradle", "Android SDK عبر السحابة")
}

enum class SelectedBuildEngine(val labelAr: String) {
    LOCAL_QUANTUM("محرك Quantum المحلي (On-Device)"),
    CLOUD_GRADLE_SDK("محرك Gradle + Android SDK السحابي")
}

enum class BuildNetworkQuality(val labelAr: String) {
    OFFLINE("غير متصل"),
    METERED_CELLULAR("شبكة خلوية محدودة"),
    UNMETERED_WIFI("شبكة Wi-Fi / غير محدودة"),
    UNKNOWN("متصل بالشبكة")
}

data class DeviceCapabilitiesSnapshot(
    val abi: String,
    val freeStorageMb: Long,
    val totalRamMb: Long,
    val lowMemoryDevice: Boolean,
    val hasNativeAapt2Binary: Boolean,
    val hasNativeD8Binary: Boolean
) {
    val hasMinimumBuildStorage: Boolean get() = freeStorageMb >= 40L
}

data class NetworkConnectivitySnapshot(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val quality: BuildNetworkQuality,
    val labelAr: String
)

data class SmartBuildDecision(
    val selectedEngine: SelectedBuildEngine,
    val requestedMode: HybridBuildMode,
    val targetType: String,
    val reasonAr: String,
    val detailedFactors: List<String>,
    val cloudReasons: List<String>,
    val requiresCloudForFullFidelity: Boolean,
    val cloudCredentialsReady: Boolean,
    val cloudExecutionPossibleNow: Boolean,
    val networkSnapshot: NetworkConnectivitySnapshot,
    val deviceSnapshot: DeviceCapabilitiesSnapshot,
    val warnings: List<String>
)

/**
 * موجّه البناء الذكي (Smart Build Router):
 * يقيّم نوع المشروع ومتطلباته، قدرات الجهاز (التخزين والذاكرة والمعمارية والأدوات المتاحة)،
 * وحالة الاتصال بالشبكة لتحديد المسار الأمثل بين البناء المحلي (Quantum) والسحابي (Gradle + Android SDK).
 */
class SmartBuildRouter(private val context: Context) {

    fun captureDeviceSnapshot(): DeviceCapabilitiesSnapshot {
        val profile = BuildCapabilitiesInspector.inspect(context)
        val freeMb = runCatching {
            val stat = StatFs(context.filesDir.absolutePath)
            (stat.availableBlocksLong * stat.blockSizeLong) / (1024L * 1024L)
        }.getOrDefault(profile.freeStorageMb)

        val (ramMb, lowMem) = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            if (am != null) {
                am.getMemoryInfo(memInfo)
                (memInfo.totalMem / (1024L * 1024L)) to memInfo.lowMemory
            } else {
                2048L to false
            }
        }.getOrDefault(2048L to false)

        return DeviceCapabilitiesSnapshot(
            abi = Build.SUPPORTED_ABIS.firstOrNull() ?: profile.deviceAbi,
            freeStorageMb = freeMb,
            totalRamMb = ramMb,
            lowMemoryDevice = lowMem,
            hasNativeAapt2Binary = profile.hasExecutableNativeAapt2,
            hasNativeD8Binary = profile.hasExecutableNativeD8
        )
    }

    fun captureNetworkSnapshot(): NetworkConnectivitySnapshot {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return@runCatching NetworkConnectivitySnapshot(
                    isConnected = false,
                    isValidated = false,
                    quality = BuildNetworkQuality.OFFLINE,
                    labelAr = "خدمة الشبكة غير متاحة"
                )
            val active = cm.activeNetwork
            val caps = active?.let { cm.getNetworkCapabilities(it) }
            if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                NetworkConnectivitySnapshot(
                    isConnected = false,
                    isValidated = false,
                    quality = BuildNetworkQuality.OFFLINE,
                    labelAr = "لا يوجد اتصال بالإنترنت"
                )
            } else {
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val unmetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                val quality = when {
                    unmetered -> BuildNetworkQuality.UNMETERED_WIFI
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> BuildNetworkQuality.METERED_CELLULAR
                    else -> BuildNetworkQuality.UNKNOWN
                }
                NetworkConnectivitySnapshot(
                    isConnected = true,
                    isValidated = validated,
                    quality = quality,
                    labelAr = quality.labelAr
                )
            }
        }.getOrDefault(
            NetworkConnectivitySnapshot(
                isConnected = false,
                isValidated = false,
                quality = BuildNetworkQuality.OFFLINE,
                labelAr = "تعذر قراءة حالة الشبكة"
            )
        )
    }

    fun decide(
        project: AppProject,
        targetType: String,
        requestedMode: HybridBuildMode,
        cloudTokenConfigured: Boolean,
        abiSplitRequested: Boolean = false,
        networkOverride: NetworkConnectivitySnapshot? = null,
        deviceOverride: DeviceCapabilitiesSnapshot? = null
    ): SmartBuildDecision {
        val network = networkOverride ?: captureNetworkSnapshot()
        val device = deviceOverride ?: captureDeviceSnapshot()
        val cloudReasons = BuildCapabilitiesInspector.projectRequiresCloudGradleForFullFidelity(project, targetType)
            .toMutableList()
        if (abiSplitRequested && !cloudReasons.any { it.contains("ABI", ignoreCase = true) }) {
            cloudReasons.add("تقسيم المعالجات (ABI Splits) الكامل للأكواد الأصلية يُدار بدقة عبر Gradle/AGP.")
        }

        val requiresCloud = cloudReasons.isNotEmpty()
        val warnings = mutableListOf<String>()
        val factors = mutableListOf<String>()

        factors.add("الهدف: ${targetType.uppercase()} • المصدر: ${project.sourceType.name}")
        factors.add("الجهاز: ${device.abi} • المساحة الحرة: ${device.freeStorageMb}MB • الذاكرة: ${device.totalRamMb}MB")
        factors.add("الاتصال: ${network.labelAr} • اعتماد GitHub: ${if (cloudTokenConfigured) "جاهز ومشفّر ✓" else "غير مربوط"}")

        if (!device.hasMinimumBuildStorage) {
            warnings.add("المساحة الحرة على الجهاز منخفضة (${device.freeStorageMb}MB). يُنصح بتفريغ مساحة أو استخدام البناء السحابي.")
        }
        if (device.lowMemoryDevice) {
            warnings.add("ذاكرة الجهاز منخفضة حالياً؛ يفضّل تجنب المعالجة الثقيلة على الجهاز.")
        }

        val selectedEngine: SelectedBuildEngine
        val reasonAr: String

        when (requestedMode) {
            HybridBuildMode.FORCE_LOCAL -> {
                selectedEngine = SelectedBuildEngine.LOCAL_QUANTUM
                reasonAr = "تم تحديد البناء المحلي يدوياً: تنفيذ التغليف المحلي والتوقيع الرسمي Google apksig v1-v4 على الجهاز."
                if (requiresCloud) {
                    warnings.addAll(cloudReasons)
                }
            }

            HybridBuildMode.FORCE_CLOUD -> {
                if (!network.isConnected) {
                    selectedEngine = SelectedBuildEngine.LOCAL_QUANTUM
                    reasonAr = "طلبت البناء السحابي لكن الجهاز غير متصل حالياً؛ تم التبديل الاحتياطي للمحلي حتى يعود الاتصال."
                    warnings.add("لا يوجد اتصال بالإنترنت لتشغيل Gradle السحابي. اتصل بالشبكة لإرسال المشروع إلى GitHub Actions.")
                    if (requiresCloud) warnings.addAll(cloudReasons)
                } else {
                    selectedEngine = SelectedBuildEngine.CLOUD_GRADLE_SDK
                    reasonAr = if (cloudTokenConfigured) {
                        "تم تحديد البناء السحابي يدوياً: سيُرفع المشروع لتجميعه بـ Gradle 8.7 و Android SDK وإرجاع الحزمة."
                    } else {
                        "تم تحديد البناء السحابي يدوياً — يلزم ربط رمز GitHub Token المشفّر للبدء."
                    }
                    if (!cloudTokenConfigured) {
                        warnings.add("رمز GitHub Token غير مضبوط بعد؛ سيُطلب منك ربطه قبل الإطلاق السحابي.")
                    }
                }
            }

            HybridBuildMode.AUTO_SMART -> {
                when {
                    !network.isConnected -> {
                        selectedEngine = SelectedBuildEngine.LOCAL_QUANTUM
                        reasonAr = "الموجّه الذكي: الجهاز غير متصل بالإنترنت، تم اختيار محرك Quantum المحلي للعمل دون اتصال."
                        if (requiresCloud) {
                            warnings.add("بعض الخصائص (${cloudReasons.firstOrNull().orEmpty()}) تحتاج الاتصال لتجميعها سحابياً عبر Gradle.")
                        }
                    }

                    requiresCloud && cloudTokenConfigured -> {
                        selectedEngine = SelectedBuildEngine.CLOUD_GRADLE_SDK
                        reasonAr = "الموجّه الذكي اختار السحابي (Gradle + Android SDK): المشروع يتطلب ترجمة أصلية أو حزمة Google Play، والاتصال والاعتماد جاهزان."
                        if (network.quality == BuildNetworkQuality.METERED_CELLULAR) {
                            warnings.add("أنت على بيانات الهاتف (Metered): رفع المشروع وتحميل الناتج سيستهلك حزمة بيانات.")
                        }
                    }

                    !device.hasMinimumBuildStorage && cloudTokenConfigured -> {
                        selectedEngine = SelectedBuildEngine.CLOUD_GRADLE_SDK
                        reasonAr = "الموجّه الذكي اختار السحابي نظراً لانخفاض مساحة التخزين المحلية (${device.freeStorageMb}MB)."
                    }

                    requiresCloud && !cloudTokenConfigured -> {
                        selectedEngine = SelectedBuildEngine.LOCAL_QUANTUM
                        reasonAr = "الموجّه الذكي: سيُنفّذ التغليف المحلي وتوقيع apksig v1-v4 الآن، مع التوصية بربط GitHub للبناء الكامل بـ Android SDK."
                        warnings.add("لتجميع الكود الأصلي أو Play AAB رسمياً، اربط GitHub Token المشفّر لتفعيل البناء السحابي التلقائي.")
                    }

                    else -> {
                        selectedEngine = SelectedBuildEngine.LOCAL_QUANTUM
                        reasonAr = "الموجّه الذكي اختار المحلي (Quantum): مشروع ويب/HTML خفيف جاهز للتغليف السريع والتوقيع v1-v4 مباشرة على الجهاز بدون انتظار السحابة."
                    }
                }
            }
        }

        val cloudPossible = network.isConnected && cloudTokenConfigured

        return SmartBuildDecision(
            selectedEngine = selectedEngine,
            requestedMode = requestedMode,
            targetType = targetType,
            reasonAr = reasonAr,
            detailedFactors = factors,
            cloudReasons = cloudReasons,
            requiresCloudForFullFidelity = requiresCloud,
            cloudCredentialsReady = cloudTokenConfigured,
            cloudExecutionPossibleNow = cloudPossible,
            networkSnapshot = network,
            deviceSnapshot = device,
            warnings = warnings
        )
    }
}
