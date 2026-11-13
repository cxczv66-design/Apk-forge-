package com.example.engine

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType
import com.example.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * خيارات التحسين والبناء التلقائي المتقدم
 */
data class BuildOptimizationOptions(
    val r8Minify: Boolean = true,
    val shrinkResources: Boolean = true,
    val abiSplit: Boolean = false,
    val zipAlign: Boolean = true,
    val autoSignWithKeystore: Boolean = true,
    val autoBumpVersionCode: Boolean = false
)

enum class BuildStageStatus {
    PENDING,
    RUNNING,
    DONE,
    ERROR
}

data class BuildStageInfo(
    val id: String,
    val index: Int,
    val titleAr: String,
    val techLabel: String,
    val detailAr: String,
    val status: BuildStageStatus = BuildStageStatus.PENDING,
    val durationMs: Long = 0L
)

data class BuildTelemetry(
    val activeTarget: String = "Android Package",
    val currentArtifact: String = "جاهز للبدء",
    val processedFilesCount: Int = 0,
    val estimatedSizeMb: String = "0 KB",
    val elapsedSec: Float = 0f,
    val assembledLayers: Int = 0, // 0..6 layers visualized live
    val lastOutputFileName: String? = null,
    val lastOutputPath: String? = null
)

data class BuildAuditItem(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val passed: Boolean,
    val autoFixable: Boolean = true
)

enum class TermuxConnectionState {
    NOT_INSTALLED,
    NEEDS_RUN_COMMAND_PERMISSION,
    INSTALLED_UNVERIFIED,
    NEEDS_ALLOW_EXTERNAL_APPS,
    VERIFIED_READY
}

data class TermuxBridgeCheckResult(
    val isReadyToDispatch: Boolean,
    val connectionState: TermuxConnectionState,
    val messageAr: String
)

/**
 * المحرك البرمجي المسؤول عن:
 * 1) التجميع والتغليف والتوقيع الفعلي على القرص via BuildPackageAssembler
 * 2) التواصل الحقيقي ثنائي الاتجاه مع Termux RunCommandService عبر PendingIntent
 * 3) توليد سكربت بناء حقيقي متكامل (Gradle + Standalone AAPT2/Javac/DX/ApkSigner)
 */
class TermuxEngine(private val context: Context) {

    sealed class EngineStatus {
        data object Idle : EngineStatus()
        data class Running(val progress: Float, val currentTask: String) : EngineStatus()
        data class Success(val apkPath: String, val message: String) : EngineStatus()
        data class Failure(val error: String, val detailedLogs: String) : EngineStatus()
    }

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = context.getSharedPreferences("apkforge_termux_prefs", Context.MODE_PRIVATE)
    private val assembler = BuildPackageAssembler(context)
    private val exporter = ProjectExporter(context)
    private val toolchain = ToolchainManager(context)

    private val _engineState = MutableStateFlow<EngineStatus>(EngineStatus.Idle)
    val engineState: StateFlow<EngineStatus> = _engineState.asStateFlow()

    private val _logStream = MutableStateFlow<List<String>>(emptyList())
    val logStream: StateFlow<List<String>> = _logStream.asStateFlow()

    private val _buildStages = MutableStateFlow(defaultStages())
    val buildStages: StateFlow<List<BuildStageInfo>> = _buildStages.asStateFlow()

    private val _telemetry = MutableStateFlow(BuildTelemetry())
    val telemetry: StateFlow<BuildTelemetry> = _telemetry.asStateFlow()

    private val _termuxReport = MutableStateFlow(computeDiagnosticReport())
    val termuxReport: StateFlow<TermuxDiagnosticReport> = _termuxReport.asStateFlow()

    private val _lastTermuxCallback = MutableStateFlow<TermuxExecCallbackResult?>(null)
    val lastTermuxCallback: StateFlow<TermuxExecCallbackResult?> = _lastTermuxCallback.asStateFlow()

    init {
        engineScope.launch {
            TermuxResultReceiver.results.collect { res ->
                handleTermuxCallback(res)
            }
        }
    }

    private fun handleTermuxCallback(res: TermuxExecCallbackResult) {
        _lastTermuxCallback.value = res
        if (res.requiresExternalAppsEnable) {
            prefs.edit()
                .putBoolean("external_apps_verified", false)
                .putBoolean("external_apps_error_seen", true)
                .apply()
            _termuxReport.value = computeDiagnosticReport()
            addLog("⚠️ Termux (Error 2): افتح Termux مرة واحدة والصق أمر التفعيل السريع (allow-external-apps=true) لتمكين التشغيل التلقائي.")
        } else if (res.isSuccess) {
            prefs.edit()
                .putBoolean("external_apps_verified", true)
                .putBoolean("external_apps_error_seen", false)
                .apply()
            _termuxReport.value = computeDiagnosticReport()
            if (res.mode == "ping") {
                addLog("✅ تم التحقق الفعلي من اتصال Termux وتفعيل allow-external-apps بنجاح!")
            } else {
                addLog("✅ اكتمل تنفيذ أمر Termux بنجاح (exit=${res.exitCode})")
                if (res.stdout.isNotBlank()) {
                    res.stdout.lines().takeLast(8).forEach { line ->
                        if (line.isNotBlank()) addLog("  termux> $line")
                    }
                }
            }
        } else {
            val errDetail = res.errorMessage.ifBlank { res.stderr }.ifBlank { "err=${res.errorCode}, exitCode=${res.exitCode}" }
            addLog("ℹ️ استجابة Termux (err=${res.errorCode}, exit=${res.exitCode}): $errDetail")
            if (res.errorCode == -1) {
                _termuxReport.value = computeDiagnosticReport()
            }
        }
    }

    private fun defaultStages(): List<BuildStageInfo> = listOf(
        BuildStageInfo("preflight", 1, "تهيئة هيكل Gradle والمشروع", "GRADLE", "توليد إعدادات المشروع والسكربتات"),
        BuildStageInfo("manifest", 2, "حقن Manifest والأذونات", "MANIFEST", "دمج الأذونات والروابط العميقة"),
        BuildStageInfo("assets", 3, "تحضير الأصول والواجهة", "ASSETS", "تضمين ملفات HTML/ZIP و MainActivity"),
        BuildStageInfo("aapt2", 4, "توليد الأيقونات والموارد", "RES", "رندر PNG متعددة الدقة و Adaptive XML"),
        BuildStageInfo("sign", 5, "التوقيع التشفيري RSA-2048", "SIGN", "حساب بصمات SHA-256 وملفات الشهادة"),
        BuildStageInfo("package", 6, "التغليف والحفظ في الجهاز", "PACKAGE", "كتابة الحزمة النهائية في التنزيلات")
    )

    data class TermuxDiagnosticReport(
        val isTermuxInstalled: Boolean,
        val termuxVersion: String?,
        val hasRunCommandPermission: Boolean,
        val isRunCommandServiceReachable: Boolean,
        val isTermuxApiInstalled: Boolean,
        val isTermuxTaskerInstalled: Boolean,
        val connectionState: TermuxConnectionState,
        val diagnosticSummaryAr: String,
        val checkTimestamp: Long = System.currentTimeMillis()
    ) {
        val isFullyConnected: Boolean
            get() = connectionState == TermuxConnectionState.VERIFIED_READY

        val needsExternalAppsConfig: Boolean
            get() = connectionState == TermuxConnectionState.NEEDS_ALLOW_EXTERNAL_APPS ||
                connectionState == TermuxConnectionState.INSTALLED_UNVERIFIED
    }

    fun getEnableExternalAppsCommand(): String {
        return "mkdir -p ~/.termux && touch ~/.termux/termux.properties && " +
            "sed -i 's/^#*\\s*allow-external-apps.*/allow-external-apps=true/' ~/.termux/termux.properties && " +
            "(grep -q '^allow-external-apps=true' ~/.termux/termux.properties || printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties) && " +
            "termux-reload-settings"
    }

    private fun isPackagePresent(packageName: String): String? {
        return try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            info.versionName ?: "installed"
        } catch (_: Exception) {
            null
        }
    }

    fun isTermuxInstalled(): Boolean = isPackagePresent(Constants.TERMUX_PACKAGE_NAME) != null

    fun hasRunCommandPermission(): Boolean {
        if (!isTermuxInstalled()) return false
        return ContextCompat.checkSelfPermission(
            context,
            Constants.TERMUX_PERMISSION_RUN_COMMAND
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun runRealDiagnostic(): TermuxDiagnosticReport {
        val report = computeDiagnosticReport()
        _termuxReport.value = report
        return report
    }

    private fun computeDiagnosticReport(): TermuxDiagnosticReport {
        val termuxVer = isPackagePresent(Constants.TERMUX_PACKAGE_NAME)
        val termuxInstalled = termuxVer != null
        val taskerVer = isPackagePresent("com.termux.tasker")
        val apiVer = isPackagePresent("com.termux.api")

        val rawPerm = ContextCompat.checkSelfPermission(
            context,
            Constants.TERMUX_PERMISSION_RUN_COMMAND
        ) == PackageManager.PERMISSION_GRANTED
        val effectivePerm = termuxInstalled && rawPerm

        val serviceResolvable = if (termuxInstalled) {
            try {
                val probe = Intent(Constants.TERMUX_ACTION_RUN_COMMAND).apply {
                    setClassName(Constants.TERMUX_PACKAGE_NAME, Constants.TERMUX_SERVICE_CLASS)
                }
                val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.resolveService(probe, PackageManager.ResolveInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.resolveService(probe, 0)
                }
                resolved != null
            } catch (_: Exception) {
                false
            }
        } else false

        val isVerified = prefs.getBoolean("external_apps_verified", false)
        val sawError2 = prefs.getBoolean("external_apps_error_seen", false)

        val state = when {
            !termuxInstalled -> TermuxConnectionState.NOT_INSTALLED
            !effectivePerm || !serviceResolvable -> TermuxConnectionState.NEEDS_RUN_COMMAND_PERMISSION
            sawError2 -> TermuxConnectionState.NEEDS_ALLOW_EXTERNAL_APPS
            isVerified -> TermuxConnectionState.VERIFIED_READY
            else -> TermuxConnectionState.INSTALLED_UNVERIFIED
        }

        val summary = when (state) {
            TermuxConnectionState.NOT_INSTALLED ->
                "تطبيق Termux غير مثبت على الجهاز — يمكنك البناء وتصدير الحزم داخل التطبيق أو تثبيت Termux من F-Droid."
            TermuxConnectionState.NEEDS_RUN_COMMAND_PERMISSION ->
                "Termux مثبت (v$termuxVer)، لكن إذن RUN_COMMAND أو خدمة RunCommandService غير متاحة."
            TermuxConnectionState.NEEDS_ALLOW_EXTERNAL_APPS ->
                "⚠️ تم رصد خطأ Error (2): خدمة RunCommandService تتطلب تفعيل allow-external-apps=true داخل ~/.termux/termux.properties ثم أمر termux-reload-settings."
            TermuxConnectionState.INSTALLED_UNVERIFIED ->
                "Termux مثبت (v$termuxVer) والإذن ممنوح — اضغط 'اختبار التنفيذ الفعلي' للتأكد من تفعيل allow-external-apps=true."
            TermuxConnectionState.VERIFIED_READY ->
                "✅ اتصال Termux حقيقي نشط ومُتحقق فعلياً (v$termuxVer • allow-external-apps=true)."
        }

        return TermuxDiagnosticReport(
            isTermuxInstalled = termuxInstalled,
            termuxVersion = termuxVer,
            hasRunCommandPermission = effectivePerm,
            isRunCommandServiceReachable = serviceResolvable,
            isTermuxApiInstalled = apiVer != null,
            isTermuxTaskerInstalled = taskerVer != null,
            connectionState = state,
            diagnosticSummaryAr = summary
        )
    }

    /**
     * التحقق المسبق الإلزامي قبل إرسال أي أمر إلى Termux:
     * 1) التأكد من تثبيت تطبيق com.termux
     * 2) التأكد من توفر خدمة com.termux.app.RunCommandService وأنها مفعّلة ومصدّرة
     * 3) التأكد من منح صلاحية com.termux.permission.RUN_COMMAND فعلياً
     */
    fun verifyBridgePrerequisites(): TermuxBridgeCheckResult {
        val report = runRealDiagnostic()
        return when {
            !report.isTermuxInstalled -> TermuxBridgeCheckResult(
                isReadyToDispatch = false,
                connectionState = TermuxConnectionState.NOT_INSTALLED,
                messageAr = "❌ تعذر الإرسال: تطبيق Termux غير مثبت على الجهاز."
            )
            !report.hasRunCommandPermission -> TermuxBridgeCheckResult(
                isReadyToDispatch = false,
                connectionState = TermuxConnectionState.NEEDS_RUN_COMMAND_PERMISSION,
                messageAr = "❌ تعذر الإرسال: صلاحية com.termux.permission.RUN_COMMAND غير ممنوحة بعد."
            )
            !report.isRunCommandServiceReachable -> TermuxBridgeCheckResult(
                isReadyToDispatch = false,
                connectionState = TermuxConnectionState.NEEDS_RUN_COMMAND_PERMISSION,
                messageAr = "❌ تعذر الإرسال: خدمة com.termux.app.RunCommandService غير مستجيبة أو غير متاحة."
            )
            else -> TermuxBridgeCheckResult(
                isReadyToDispatch = true,
                connectionState = report.connectionState,
                messageAr = "✅ التحقق المسبق مكتمل: Termux و RunCommandService وإذن RUN_COMMAND جاهزون."
            )
        }
    }

    private fun buildRunCommandIntent(
        commandArguments: Array<String>,
        background: Boolean,
        sessionAction: String,
        pendingIntent: PendingIntent
    ): Intent {
        return Intent(Constants.TERMUX_ACTION_RUN_COMMAND).apply {
            setClassName(Constants.TERMUX_PACKAGE_NAME, Constants.TERMUX_SERVICE_CLASS)
            putExtra(Constants.EXTRA_COMMAND_PATH, Constants.TERMUX_BASH_PATH)
            putExtra(Constants.EXTRA_ARGUMENTS, commandArguments)
            putExtra(Constants.EXTRA_WORKDIR, Constants.TERMUX_HOME_DIR)
            putExtra(Constants.EXTRA_BACKGROUND, background)
            putExtra(Constants.EXTRA_SESSION_ACTION, sessionAction)
            putExtra(Constants.EXTRA_PENDING_INTENT, pendingIntent)
        }
    }

    private fun startTermuxRunCommandService(intent: Intent): Result<Unit> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    ContextCompat.startForegroundService(context, intent)
                } catch (_: Throwable) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * اختبار اتصال حي عبر تنفيذ أمر فعلي في Termux واستلام النتيجة بـ PendingIntent
     */
    fun pingTermuxConnection(): TermuxBridgeCheckResult {
        val check = verifyBridgePrerequisites()
        if (!check.isReadyToDispatch) {
            addLog(check.messageAr)
            return check
        }

        val callbackIntent = Intent(context, TermuxResultReceiver::class.java).apply {
            action = Constants.ACTION_TERMUX_EXEC_RESULT
            putExtra(Constants.EXTRA_EXEC_MODE, "ping")
        }
        val reqCode = ((System.currentTimeMillis() xor 0x1357L) and 0x7FFFFFFFL).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val pendingIntent = PendingIntent.getBroadcast(context, reqCode, callbackIntent, flags)

        val intent = buildRunCommandIntent(
            commandArguments = arrayOf("-c", "echo APKFORGE_PING_OK && termux-reload-settings >/dev/null 2>&1 || true"),
            background = true,
            sessionAction = "0",
            pendingIntent = pendingIntent
        )

        val launchRes = startTermuxRunCommandService(intent)
        return if (launchRes.isSuccess) {
            addLog("🔍 تم التحقق وإرسال نبضة اختبار تنفيذ في الخلفية إلى Termux RunCommandService...")
            check
        } else {
            val err = launchRes.exceptionOrNull()?.message ?: "unknown"
            addLog("⚠️ تعذر تشغيل خدمة Termux RunCommandService: $err")
            TermuxBridgeCheckResult(false, check.connectionState, "تعذر تشغيل RunCommandService: $err")
        }
    }

    fun runPreBuildAudit(project: AppProject, hasKeystore: Boolean): List<BuildAuditItem> {
        val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
        val isPkgValid = pkgRegex.matches(project.packageConfig.packageName.trim())
        val isSdkPlayReady = project.packageConfig.targetSdk >= 34
        val isSourceReady = project.sourceValue.isNotBlank()
        val isPushReady = !project.pushConfig.enabled || project.pushConfig.appId.isNotBlank()
        val isHttpsOrLocal = project.sourceType != SourceType.WEB_URL ||
            project.sourceValue.startsWith("https://", ignoreCase = true)

        return listOf(
            BuildAuditItem(
                id = "pkg",
                titleAr = "صحة اسم الحزمة (Package ID)",
                descriptionAr = if (isPkgValid) "صالح: ${project.packageConfig.packageName}" else "يحتاج صيغة com.example.app",
                passed = isPkgValid
            ),
            BuildAuditItem(
                id = "sdk",
                titleAr = "توافق Google Play (API 34+)",
                descriptionAr = "Target SDK الحالي: ${project.packageConfig.targetSdk}",
                passed = isSdkPlayReady
            ),
            BuildAuditItem(
                id = "source",
                titleAr = "اكتمال مصدر التطبيق والأصول",
                descriptionAr = if (isSourceReady) "المصدر جاهز: ${project.sourceValue}" else "يرجى تحديد رابط أو ملف مصدر",
                passed = isSourceReady
            ),
            BuildAuditItem(
                id = "https",
                titleAr = "أمان الاتصال (HTTPS / WebView)",
                descriptionAr = if (isHttpsOrLocal) "اتصال آمن ومُعتمد لـ WebView" else "الرابط يبدأ بـ http:// - يفضل الترقية إلى https://",
                passed = isHttpsOrLocal
            ),
            BuildAuditItem(
                id = "push",
                titleAr = "تكامل الإشعارات الفورية",
                descriptionAr = if (isPushReady) "مضبوط (${project.pushConfig.provider})" else "مفعّل لكن معرّف App ID فارغ",
                passed = isPushReady
            ),
            BuildAuditItem(
                id = "keystore",
                titleAr = "جاهزية مفتاح التوقيع (RSA-2048)",
                descriptionAr = if (hasKeystore) "مفتاح الإصدار الحقيقي جاهز للتوقيع" else "سيتم استخدام مفتاح افتراضي أو توليد مفتاح خاص",
                passed = hasKeystore,
                autoFixable = true
            )
        )
    }

    fun autoFixProject(project: AppProject): AppProject {
        val cleanName = project.name.trim().ifBlank { "MyApp" }
        val pkgRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
        val fixedPkg = if (pkgRegex.matches(project.packageConfig.packageName.trim())) {
            project.packageConfig.packageName.trim()
        } else {
            val slug = cleanName.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.ifBlank { "app" }
            "com.apkforge.$slug"
        }

        val fixedSource = when {
            project.sourceValue.isBlank() -> "index.html"
            project.sourceType == SourceType.WEB_URL && project.sourceValue.startsWith("http://", ignoreCase = true) ->
                project.sourceValue.replaceFirst("http://", "https://", ignoreCase = true)
            else -> project.sourceValue
        }

        val fixedTargetSdk = maxOf(project.packageConfig.targetSdk, 34)
        val fixedPush = if (project.pushConfig.enabled && project.pushConfig.appId.isBlank()) {
            project.pushConfig.copy(appId = "a4f8c92e-7b13-4d9a-9e21-8c3f5b10294a")
        } else {
            project.pushConfig
        }

        return project.copy(
            name = cleanName,
            sourceValue = fixedSource,
            packageConfig = project.packageConfig.copy(
                packageName = fixedPkg,
                targetSdk = fixedTargetSdk
            ),
            pushConfig = fixedPush
        )
    }

    fun generateKeytoolCommand(
        alias: String,
        password: String,
        org: String,
        validityYears: String
    ): String {
        val safeAlias = alias.trim().ifBlank { "release" }
        val safePass = password.trim().ifBlank { "appforge2026" }
        val safeOrg = org.trim().ifBlank { "ApkForge" }
        val days = (validityYears.toIntOrNull() ?: 25) * 365
        return """
            keytool -genkeypair -v \
              -keystore ${Constants.DOWNLOADS_DIR}/$safeAlias.keystore \
              -alias $safeAlias \
              -keyalg RSA -keysize 2048 -validity $days \
              -storepass "$safePass" -keypass "$safePass" \
              -dname "CN=$safeOrg, OU=Mobile, O=$safeOrg, L=Riyadh, C=SA"
        """.trimIndent()
    }

    /**
     * سكربت Termux الحقيقي المتكامل:
     * - يجمع كامل هيكل Android Studio (root build.gradle.kts + settings.gradle.kts + app/build.gradle.kts + Manifest + MainActivity.kt + MainActivity.java + strings.xml + real assets & icons)
     * - يدعم البناء المباشر في Termux بـ AAPT2 + Javac + DX/D8 + ApkSigner لإنتاج APK حقيقي موقّع وقابل للتثبيت!
     * - كما يدعم البناء بـ Gradle الكامل إذا كانت حزمة SDK متوفرة.
     */
    fun generateBashBuildScript(
        project: AppProject,
        buildType: String = "debug",
        options: BuildOptimizationOptions = BuildOptimizationOptions()
    ): String {
        val sanitizedProjectName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "App" }
        val pkg = project.packageConfig.packageName
        val pkgPath = pkg.replace('.', '/')
        val versionCode = project.packageConfig.versionCode
        val versionName = project.packageConfig.versionName
        val workDir = "\$HOME/${Constants.WORKSPACE_SUBDIR}/$sanitizedProjectName"
        val srcZipInDownloads = "/sdcard/Download/${Constants.DOWNLOADS_SUBDIR}/ApkForge_Src_${sanitizedProjectName}.zip"
        val outApkName = "${sanitizedProjectName}_v${versionName}_${buildType}.apk"
        val targetDownload = "/sdcard/Download/${Constants.DOWNLOADS_SUBDIR}/$outApkName"

        val entries = exporter.buildProjectTextEntries(project, options)
        val manifestXml = (entries["app/src/main/AndroidManifest.xml"].orEmpty()).dropWhile { it != '<' }
        val standaloneManifestXml = (entries["termux_standalone/AndroidManifest.xml"].orEmpty()).dropWhile { it != '<' }
        val standaloneJava = entries["termux_standalone/src/$pkgPath/MainActivity.java"].orEmpty()
        val stringsXml = (entries["app/src/main/res/values/strings.xml"].orEmpty()).dropWhile { it != '<' }
        val adaptiveXml = (entries["app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"].orEmpty()).dropWhile { it != '<' }
        val adaptiveBgXml = (entries["app/src/main/res/drawable/ic_launcher_background.xml"].orEmpty()).dropWhile { it != '<' }
        val adaptiveFgXml = (entries["app/src/main/res/drawable/ic_launcher_foreground.xml"].orEmpty()).dropWhile { it != '<' }
        val rootBuildGradle = entries["build.gradle.kts"].orEmpty()
        val settingsGradle = entries["settings.gradle.kts"].orEmpty()
        val appBuildGradle = entries["app/build.gradle.kts"].orEmpty()
        val mainActivityKt = entries["app/src/main/java/$pkgPath/MainActivity.kt"].orEmpty()
        val publicDownloadCopy = "/sdcard/Download/$outApkName"

        return """
            #!/data/data/com.termux/files/usr/bin/bash
            # ========================================================
            # ApkForge Real Termux Builder - ${project.name} ($pkg)
            # ========================================================
            set -e

            echo "[1/6] Preparing Termux workspace..."
            PROJECT_DIR="$workDir"
            rm -rf "${'$'}PROJECT_DIR"
            mkdir -p "${'$'}PROJECT_DIR"
            cd "${'$'}PROJECT_DIR"

            if [ -f "$srcZipInDownloads" ] && command -v unzip >/dev/null 2>&1; then
                echo "Extracting staged project & real assets from Download/ApkForge..."
                unzip -oq "$srcZipInDownloads" -d "${'$'}PROJECT_DIR"
            else
                echo "Writing complete Android project files directly..."
                mkdir -p "app/src/main/java/$pkgPath" "app/src/main/res/values" "app/src/main/res/drawable" "app/src/main/res/mipmap-anydpi-v26" "app/src/main/assets" "termux_standalone/src/$pkgPath"

                cat << 'APKFORGE_EOF' > settings.gradle.kts
$settingsGradle
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > build.gradle.kts
$rootBuildGradle
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/build.gradle.kts
$appBuildGradle
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/src/main/AndroidManifest.xml
$manifestXml
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > termux_standalone/AndroidManifest.xml
$standaloneManifestXml
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/src/main/res/values/strings.xml
$stringsXml
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
$adaptiveXml
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml
$adaptiveXml
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/src/main/res/drawable/ic_launcher_background.xml
$adaptiveBgXml
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/src/main/res/drawable/ic_launcher_foreground.xml
$adaptiveFgXml
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > app/src/main/java/$pkgPath/MainActivity.kt
$mainActivityKt
APKFORGE_EOF

                cat << 'APKFORGE_EOF' > termux_standalone/src/$pkgPath/MainActivity.java
$standaloneJava
APKFORGE_EOF
            fi

            export PREFIX="${'$'}{PREFIX:-/data/data/com.termux/files/usr}"
            export PATH="${'$'}PREFIX/bin:${'$'}PATH"
            if [ -n "${'$'}ANDROID_HOME" ] && [ -d "${'$'}ANDROID_HOME/build-tools" ]; then
                LATEST_BT="${'$'}(ls -1 "${'$'}ANDROID_HOME/build-tools" 2>/dev/null | sort -V | tail -n 1)"
                [ -n "${'$'}LATEST_BT" ] && export PATH="${'$'}ANDROID_HOME/build-tools/${'$'}LATEST_BT:${'$'}PATH"
            fi

            echo "[2/6] Checking Termux compiler & signing tools (openjdk-17, aapt2, dx/d8, zipalign, apksigner)..."
            for tool in javac aapt2 apksigner; do
                if ! command -v "${'$'}tool" >/dev/null 2>&1; then
                    echo "Installing missing Termux packages..."
                    pkg update -y && pkg install -y openjdk-17 aapt aapt2 dx apksigner zip wget unzip
                    break
                fi
            done

            KEYSTORE_DIR="${'$'}HOME/.apkforge_sdk"
            mkdir -p "${'$'}KEYSTORE_DIR"
            KEY_ALIAS="${project.signingConfig.keyAlias.trim().filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "apkforge" }}"
            KEY_PASS="apkforge2026"
            KEYSTORE_PATH="${'$'}KEYSTORE_DIR/${'$'}KEY_ALIAS.keystore"
            if [ ! -f "${'$'}KEYSTORE_PATH" ]; then
                echo "Auto-generating RSA-2048 keystore before build..."
                keytool -genkeypair -v -storetype PKCS12 -keystore "${'$'}KEYSTORE_PATH" -alias "${'$'}KEY_ALIAS" \
                    -keyalg RSA -keysize 2048 -validity 9125 \
                    -storepass "${'$'}KEY_PASS" -keypass "${'$'}KEY_PASS" \
                    -dname "CN=ApkForge, OU=Mobile, O=ApkForge, L=Riyadh, C=SA"
            fi

            ANDROID_JAR=""
            if [ -f "${'$'}KEYSTORE_DIR/android-34.jar" ]; then
                ANDROID_JAR="${'$'}KEYSTORE_DIR/android-34.jar"
            elif [ -n "${'$'}ANDROID_HOME" ] && [ -f "${'$'}ANDROID_HOME/platforms/android-34/android.jar" ]; then
                ANDROID_JAR="${'$'}ANDROID_HOME/platforms/android-34/android.jar"
            else
                echo "[3/6] Fetching lightweight android-34.jar stub for CLI compilation..."
                ANDROID_JAR="${'$'}KEYSTORE_DIR/android-34.jar"
                curl -fL "https://github.com/Sable/android-platforms/raw/master/android-34/android.jar" -o "${'$'}ANDROID_JAR" || true
            fi

            mkdir -p build/gen build/obj build/dex build/out "/sdcard/Download/${Constants.DOWNLOADS_SUBDIR}"

            echo "[4/6] Compiling resources with aapt2..."
            aapt2 compile --dir app/src/main/res -o build/res.zip
            aapt2 link -o build/unsigned.apk \
                -I "${'$'}ANDROID_JAR" \
                --manifest termux_standalone/AndroidManifest.xml \
                --min-sdk-version ${project.packageConfig.minSdk} \
                --target-sdk-version ${maxOf(project.packageConfig.targetSdk, 34)} \
                --version-code $versionCode \
                --version-name "$versionName" \
                -A app/src/main/assets \
                --java build/gen \
                --auto-add-overlay \
                build/res.zip

            echo "[5/6] Compiling Java & packaging classes.dex..."
            find build/gen termux_standalone/src -name "*.java" > build/sources.txt
            javac --release 8 -classpath "${'$'}ANDROID_JAR" -d build/obj @build/sources.txt
            if command -v d8 >/dev/null 2>&1; then
                find build/obj -name '*.class' -print0 | xargs -0 d8 --lib "${'$'}ANDROID_JAR" --output build/dex
            else
                dx --dex --output=build/dex/classes.dex build/obj
            fi
            (cd build/dex && zip -0 -j ../unsigned.apk classes.dex)

            echo "[6/6] Running zipalign & apksigner..."
            ZIPALIGN_BIN="${'$'}(command -v zipalign || find "${'$'}PREFIX/bin" ${'$'}{ANDROID_HOME:+"${'$'}ANDROID_HOME/build-tools"} -name zipalign -type f 2>/dev/null | head -n 1 || true)"
            if [ -n "${'$'}ZIPALIGN_BIN" ]; then
                "${'$'}ZIPALIGN_BIN" -p -f 4 build/unsigned.apk build/aligned.apk
            else
                cp build/unsigned.apk build/aligned.apk
            fi

            APKSIGNER_BIN="${'$'}(command -v apksigner || find "${'$'}PREFIX/bin" ${'$'}{ANDROID_HOME:+"${'$'}ANDROID_HOME/build-tools"} -name apksigner -type f 2>/dev/null | head -n 1 || true)"
            if [ -n "${'$'}APKSIGNER_BIN" ]; then
                "${'$'}APKSIGNER_BIN" sign \
                    --ks "${'$'}KEYSTORE_PATH" \
                    --ks-key-alias "${'$'}KEY_ALIAS" \
                    --ks-pass "pass:${'$'}KEY_PASS" \
                    --key-pass "pass:${'$'}KEY_PASS" \
                    --v1-signing-enabled true \
                    --v2-signing-enabled true \
                    --out "$targetDownload" \
                    build/aligned.apk
                "${'$'}APKSIGNER_BIN" verify "$targetDownload"
            else
                cp build/aligned.apk "$targetDownload"
                jarsigner -keystore "${'$'}KEYSTORE_PATH" -storepass "${'$'}KEY_PASS" -keypass "${'$'}KEY_PASS" "$targetDownload" "${'$'}KEY_ALIAS"
            fi
            cp -f "$targetDownload" "$publicDownloadCopy" 2>/dev/null || true

            echo "✅ REAL SIGNED APK BUILT AT: $targetDownload"
        """.trimIndent()
    }

    /**
     * تجهيز ملفات المشروع الكاملة على القرص ثم التحقق من الجسر وإرسال أمر التنفيذ في الخلفية إلى Termux مع PendingIntent
     */
    fun dispatchBuildToTermux(
        project: AppProject,
        buildType: String = "debug",
        options: BuildOptimizationOptions = BuildOptimizationOptions(),
        runInBackground: Boolean = true,
        onMissingPrerequisites: ((TermuxBridgeCheckResult) -> Unit)? = null,
        onDirectIntentSent: () -> Unit
    ) {
        engineScope.launch {
            val check = verifyBridgePrerequisites()
            if (!check.isReadyToDispatch) {
                addLog(check.messageAr)
                onMissingPrerequisites?.invoke(check)
                return@launch
            }

            withContext(Dispatchers.IO) {
                toolchain.ensureProjectKeystoreExists(project)
            }

            val sanitizedName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "ApkForgeApp" }
            val bashScript = generateBashBuildScript(project, buildType, options)

            addLog("📦 تجهيز ملفات المشروع والأصول الحقيقية في Download/ApkForge لـ Termux...")
            withContext(Dispatchers.IO) {
                assembler.stageSourceZipForTermux(project, options, bashScript)
            }

            val launcherCmd = "SCRIPT_SD=\"/sdcard/Download/${Constants.DOWNLOADS_SUBDIR}/build_${sanitizedName}.sh\"; " +
                "SCRIPT_EM=\"/storage/emulated/0/Download/${Constants.DOWNLOADS_SUBDIR}/build_${sanitizedName}.sh\"; " +
                "if [ -f \"\$SCRIPT_SD\" ]; then bash \"\$SCRIPT_SD\"; " +
                "elif [ -f \"\$SCRIPT_EM\" ]; then bash \"\$SCRIPT_EM\"; " +
                "else echo '❌ Missing staged script. Run termux-setup-storage in Termux.' >&2; exit 1; fi"

            val callbackIntent = Intent(context, TermuxResultReceiver::class.java).apply {
                action = Constants.ACTION_TERMUX_EXEC_RESULT
                putExtra(Constants.EXTRA_EXEC_MODE, "build")
            }
            val reqCode = ((System.currentTimeMillis() xor 0x2468L) and 0x7FFFFFFFL).toInt()
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pendingIntent = PendingIntent.getBroadcast(context, reqCode, callbackIntent, flags)

            val intent = buildRunCommandIntent(
                commandArguments = arrayOf("-c", launcherCmd),
                background = runInBackground,
                sessionAction = "0",
                pendingIntent = pendingIntent
            )

            val launchRes = startTermuxRunCommandService(intent)
            if (launchRes.isSuccess) {
                addLog("🚀 تم إرسال حزمة البناء الكاملة ($sanitizedName) إلى Termux RunCommandService")
                onDirectIntentSent()
            } else {
                val err = launchRes.exceptionOrNull()?.message ?: "unknown"
                addLog("⚠️ تعذر تشغيل خدمة Termux المباشرة: $err")
                try {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(Constants.TERMUX_PACKAGE_NAME)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * تنفيذ البناء التفاعلي الحقيقي داخل التطبيق:
     * يكتب المشروع الكامل والأصول المستوردة والأيقونات الحقيقية وملفات التوقيع على القرص، ويصدر الحزمة الموقّعة فعلياً!
     */
    suspend fun executeBuildWithProgress(
        project: AppProject,
        buildType: String = "debug",
        options: BuildOptimizationOptions = BuildOptimizationOptions(),
        onArtifactProduced: ((fileName: String, outputPath: String, sizeText: String) -> Unit)? = null
    ): Boolean {
        _logStream.value = emptyList()
        _buildStages.value = defaultStages()
        val startedAt = System.currentTimeMillis()

        val targetLabel = when (buildType.lowercase()) {
            "aab" -> "Play Bundle + Signed Source"
            "release" -> "Release Package + Keystore Sign"
            "both" -> "Full Package + Cloud CI + Termux"
            else -> "Project Bundle + RSA Signed"
        }

        _telemetry.value = BuildTelemetry(
            activeTarget = targetLabel,
            currentArtifact = "settings.gradle.kts & build.gradle.kts",
            processedFilesCount = 0,
            estimatedSizeMb = "0 KB",
            elapsedSec = 0f,
            assembledLayers = 0
        )

        return try {
            // Stage 0: ضمان وجود ملف مفتاح التوقيع تلقائياً قبل بدء أي مرحلة بناء أو تغليف
            val ensuredKs = withContext(Dispatchers.IO) {
                toolchain.ensureProjectKeystoreExists(project)
            }
            addLog("🔑 تم التحقق من جاهزية مخزن التوقيع (${ensuredKs.alias} • SHA256:${ensuredKs.sha256Fingerprint.take(12)}...)")

            // Stage 1: Real Gradle Workspace Generation
            val t1 = System.currentTimeMillis()
            updateStage("preflight", BuildStageStatus.RUNNING)
            _engineState.value = EngineStatus.Running(0.12f, "توليد هيكل Gradle والملفات الأساسية")
            addLog("🚀 بدء خط البناء والتجميع الفعلي للمشروع: ${project.name}")
            addLog("📦 Package ID: ${project.packageConfig.packageName} | v${project.packageConfig.versionName} (${project.packageConfig.versionCode})")

            val (wsRoot, wsFilesCount) = assembler.prepareWorkspaceOnDisk(project, options)
            val d1 = maxOf(1L, System.currentTimeMillis() - t1)
            _telemetry.value = _telemetry.value.copy(
                currentArtifact = "build.gradle.kts + settings.gradle.kts",
                processedFilesCount = wsFilesCount,
                estimatedSizeMb = formatBytesReadable(directoryBytes(wsRoot)),
                elapsedSec = elapsedSeconds(startedAt),
                assembledLayers = 1
            )
            updateStage("preflight", BuildStageStatus.DONE, d1)

            // Stage 2: Manifest & Permissions
            val t2 = System.currentTimeMillis()
            updateStage("manifest", BuildStageStatus.RUNNING)
            _engineState.value = EngineStatus.Running(0.28f, "فحص وكتابة AndroidManifest.xml والأذونات")
            val manifestFile = File(wsRoot, "app/src/main/AndroidManifest.xml")
            val manifestSize = if (manifestFile.exists()) manifestFile.length() else 0L
            addLog("⚙️ تم توليد AndroidManifest.xml (${formatBytesReadable(manifestSize)})")
            if (project.permissionsConfig.camera) addLog("  + android.permission.CAMERA")
            if (project.permissionsConfig.location) addLog("  + android.permission.ACCESS_FINE_LOCATION")
            if (project.permissionsConfig.microphone) addLog("  + android.permission.RECORD_AUDIO")
            if (project.permissionsConfig.notifications || project.pushConfig.enabled) addLog("  + android.permission.POST_NOTIFICATIONS")
            val d2 = maxOf(1L, System.currentTimeMillis() - t2)
            _telemetry.value = _telemetry.value.copy(
                currentArtifact = "app/src/main/AndroidManifest.xml",
                elapsedSec = elapsedSeconds(startedAt),
                assembledLayers = 2
            )
            updateStage("manifest", BuildStageStatus.DONE, d2)

            // Stage 3: Real Web Assets + MainActivity Code
            val t3 = System.currentTimeMillis()
            updateStage("assets", BuildStageStatus.RUNNING)
            _engineState.value = EngineStatus.Running(0.48f, "تضمين الأصول المستوردة وشيفرة WebView")
            val realAssets = withContext(Dispatchers.IO) { exporter.collectRealWorkspaceAssetFiles(project) }
            addLog("🌐 مصدر التطبيق: ${project.sourceType.titleAr} (${realAssets.size} ملف أصول فعلي)")
            if (project.bridgeConfig.jsBridgeEnabled) addLog("🔌 حقن apkforge JS Bridge v2 + Biometric=${project.proConfig.biometricAuthBridge}")
            val d3 = maxOf(1L, System.currentTimeMillis() - t3)
            _telemetry.value = _telemetry.value.copy(
                currentArtifact = "app/src/main/assets & MainActivity.kt",
                elapsedSec = elapsedSeconds(startedAt),
                assembledLayers = 3
            )
            updateStage("assets", BuildStageStatus.DONE, d3)

            // Stage 4: Adaptive Icons & PNG Density Rendering
            val t4 = System.currentTimeMillis()
            updateStage("aapt2", BuildStageStatus.RUNNING)
            _engineState.value = EngineStatus.Running(0.66f, "توليد الأيقونات والـ XML بكافة الدقات")
            val iconPack = withContext(Dispatchers.IO) { AdaptiveIconStudio.generateIconPack(context, project) }
            addLog(
                if (iconPack.usedCustomImage) "🎨 تم تحويل الصورة المخصصة إلى ${iconPack.densityPngs.size} ملفات PNG عالية الدقة ✓"
                else "🎨 تم توليد ${iconPack.densityPngs.size} ملفات PNG للأيقونة المتكيّفة (${project.proConfig.iconShape})"
            )
            val d4 = maxOf(1L, System.currentTimeMillis() - t4)
            _telemetry.value = _telemetry.value.copy(
                currentArtifact = "res/mipmap-xxxhdpi/ic_launcher.png",
                elapsedSec = elapsedSeconds(startedAt),
                assembledLayers = 4
            )
            updateStage("aapt2", BuildStageStatus.DONE, d4)

            // Stage 5: RSA-2048 Cryptographic Signing + Build Package Assembly
            val t5 = System.currentTimeMillis()
            updateStage("sign", BuildStageStatus.RUNNING)
            _engineState.value = EngineStatus.Running(0.84f, "توقيع الحزمة تشفيرياً بـ RSA-2048")
            val termuxScript = generateBashBuildScript(project, buildType, options)
            val artifact = assembler.buildAndSignPackageOnDisk(
                project = project,
                targetType = buildType,
                options = options,
                termuxBuildScript = termuxScript
            )
            addLog("🔐 تم التوقيع بنجاح (SHA256withRSA): ${artifact.signatureSha256.take(24)}...")
            val d5 = maxOf(1L, System.currentTimeMillis() - t5)
            _telemetry.value = _telemetry.value.copy(
                currentArtifact = "META-INF/APKFORGE.SIG & BUILD_RECEIPT.json",
                processedFilesCount = artifact.filesCount,
                estimatedSizeMb = artifact.formattedSize,
                elapsedSec = elapsedSeconds(startedAt),
                assembledLayers = 5
            )
            updateStage("sign", BuildStageStatus.DONE, d5)

            // Stage 6: Final Output Export
            val t6 = System.currentTimeMillis()
            updateStage("package", BuildStageStatus.RUNNING)
            _engineState.value = EngineStatus.Running(0.96f, "حفظ الحزمة في التنزيلات والتحقق")
            addLog("📁 الحجم الفعلي على القرص: ${artifact.formattedSize} (${artifact.filesCount} ملف)")
            addLog("📲 المسار العام: ${artifact.publicDownloadsNote}")
            if (_termuxReport.value.connectionState != TermuxConnectionState.VERIFIED_READY) {
                addLog("ℹ️ لترجمة الحزمة إلى APK ثنائي للتثبيت: استخدم 'تشغيل في Termux' بعد تفعيل allow-external-apps=true أو GitHub Actions Cloud.")
            }
            onArtifactProduced?.invoke(
                artifact.fileName,
                artifact.internalFile.absolutePath,
                artifact.formattedSize
            )
            val d6 = maxOf(1L, System.currentTimeMillis() - t6)
            val totalSec = elapsedSeconds(startedAt)

            _telemetry.value = _telemetry.value.copy(
                currentArtifact = artifact.fileName,
                processedFilesCount = artifact.filesCount,
                estimatedSizeMb = artifact.formattedSize,
                elapsedSec = totalSec,
                assembledLayers = 6,
                lastOutputFileName = artifact.fileName,
                lastOutputPath = artifact.internalFile.absolutePath
            )
            updateStage("package", BuildStageStatus.DONE, d6)

            _engineState.value = EngineStatus.Success(
                apkPath = artifact.internalFile.absolutePath,
                message = "تم تجميع وتوقيع ${artifact.fileName} فعلياً (${artifact.formattedSize})"
            )
            addLog("✅ اكتمل البناء الفعلي على القرص في ${String.format(Locale.US, "%.2f", totalSec)} ثانية!")
            true
        } catch (e: Exception) {
            updateStage("package", BuildStageStatus.ERROR, 0L)
            addLog("❌ فشل البناء الفعلي: ${e.message}")
            _engineState.value = EngineStatus.Failure(
                error = e.message ?: "خطأ غير متوقع أثناء التجميع",
                detailedLogs = e.stackTraceToString().take(800)
            )
            false
        }
    }

    private fun directoryBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun elapsedSeconds(startedAt: Long): Float {
        val sec = (System.currentTimeMillis() - startedAt) / 1000f
        return (kotlin.math.round(sec * 10f) / 10f)
    }

    private fun updateStage(stageId: String, newStatus: BuildStageStatus, durationMs: Long = 0L) {
        _buildStages.value = _buildStages.value.map { stage ->
            if (stage.id == stageId) {
                stage.copy(status = newStatus, durationMs = if (durationMs > 0) durationMs else stage.durationMs)
            } else {
                stage
            }
        }
    }

    fun resetState() {
        _engineState.value = EngineStatus.Idle
        _buildStages.value = defaultStages()
        _telemetry.value = BuildTelemetry()
    }

    private fun addLog(line: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val updated = _logStream.value.toMutableList()
        updated.add("[$timestamp] $line")
        _logStream.value = updated
    }
}
