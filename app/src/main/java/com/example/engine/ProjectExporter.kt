package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType
import com.example.utils.Constants
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class MediaStoreSaveResult(
    val success: Boolean,
    val savedUri: Uri? = null,
    val displayPath: String = "",
    val fileName: String = "",
    val errorMessage: String? = null
)

data class InstallLaunchResult(
    val launched: Boolean,
    val isBinaryApk: Boolean,
    val messageAr: String
)

data class ExportedZipResult(
    val zipFile: File,
    val includedFilesCount: Int,
    val sizeKb: Long,
    val includesGitHubCi: Boolean,
    val includesJsBridge: Boolean
)

data class DataSafetyRow(
    val categoryAr: String,
    val statusAr: String,
    val isCollected: Boolean,
    val noteAr: String
)

class ProjectExporter(private val context: Context) {

    fun getExportsDir(): File {
        val dir = context.getExternalFilesDir("exports") ?: File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * يحل مسار ملف HTML الأساسي بالنسبة لمجلد assets/ في حال استيراد ملف HTML أو أرشيف ZIP
     */
    fun resolveWorkspaceEntryRelativePath(project: AppProject): String {
        val wsPath = project.localWorkspacePath?.takeIf { it.isNotBlank() } ?: return "index.html"
        val wsDir = File(wsPath)
        if (!wsDir.exists() || !wsDir.isDirectory) return "index.html"
        val entry = LocalWorkspaceEngine.findEntryHtml(wsDir) ?: return "index.html"
        return entry.relativeTo(wsDir).path.replace('\\', '/').ifBlank { "index.html" }
    }

    /**
     * يجمع جميع الملفات الثنائية الحقيقية المستوردة من HTML / ZIP لتحزيمها داخل assets/
     */
    fun collectRealWorkspaceAssetFiles(project: AppProject): Map<String, File> {
        val wsPath = project.localWorkspacePath?.takeIf { it.isNotBlank() } ?: return emptyMap()
        val wsDir = File(wsPath)
        if (!wsDir.exists() || !wsDir.isDirectory) return emptyMap()
        val result = linkedMapOf<String, File>()
        wsDir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                val rel = file.relativeTo(wsDir).path.replace('\\', '/').trimStart('/')
                if (rel.isNotBlank() && !rel.startsWith("__MACOSX")) {
                    result["app/src/main/assets/$rel"] = file
                }
            }
        return result
    }

    /**
     * بناء جميع الملفات النصية للمشروع (مشتركة بين تصدير ZIP والرفع السحابي لـ GitHub API ومحرك البناء الفعلي)
     */
    fun buildProjectTextEntries(
        project: AppProject,
        options: BuildOptimizationOptions = BuildOptimizationOptions(),
        forceCloudWorkflow: Boolean = false,
        cloudBuildTarget: String = "both"
    ): Map<String, String> {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "ApkForgeApp" }
        val pkg = project.packageConfig.packageName
        val pkgPath = pkg.replace('.', '/')
        val iconPack = AdaptiveIconStudio.generateIconPack(context, project)
        val relEntryAsset = resolveWorkspaceEntryRelativePath(project)
        val realAssets = collectRealWorkspaceAssetFiles(project)

        val entries = linkedMapOf<String, String>()

        entries["settings.gradle.kts"] = """
            pluginManagement {
                repositories {
                    google()
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "$safeName"
            include(":app")
        """.trimIndent()

        entries["build.gradle.kts"] = """
            plugins {
                id("com.android.application") version "8.5.2" apply false
                id("org.jetbrains.kotlin.android") version "1.9.24" apply false
            }
        """.trimIndent()

        entries["gradle.properties"] = """
            org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
            android.useAndroidX=true
            kotlin.code.style=official
        """.trimIndent()

        val splitsBlock = """
                ndk {
                    abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
                }
            """.trimIndent()

        val biometricDep = if (project.proConfig.biometricAuthBridge) {
            """    implementation("androidx.biometric:biometric:1.1.0")"""
        } else ""
        val playReviewDep = if (project.proConfig.inAppReviewBridge) {
            """    implementation("com.google.android.play:review-ktx:2.0.1")"""
        } else ""

        val isComposeSource = ComposeProjectTemplateGenerator.isComposeSource(project)

        entries["app/build.gradle.kts"] = if (isComposeSource) {
            ComposeProjectTemplateGenerator.buildAppGradleKts(project, options)
        } else {
            """
                plugins {
                    id("com.android.application")
                    id("org.jetbrains.kotlin.android")
                }

                android {
                    namespace = "$pkg"
                    compileSdk = ${maxOf(project.packageConfig.targetSdk, 34)}

                    defaultConfig {
                        applicationId = "$pkg"
                        minSdk = ${project.packageConfig.minSdk}
                        targetSdk = ${maxOf(project.packageConfig.targetSdk, 34)}
                        versionCode = ${project.packageConfig.versionCode}
                        versionName = "${project.packageConfig.versionName}"
                    }

                    val envKeystorePath = System.getenv("APKFORGE_KEYSTORE_PATH")
                    val hasCustomReleaseKeystore = !envKeystorePath.isNullOrBlank() && file(envKeystorePath).exists()
                    signingConfigs {
                        if (hasCustomReleaseKeystore) {
                            create("release") {
                                storeFile = file(envKeystorePath!!)
                                storePassword = System.getenv("APKFORGE_STORE_PASS") ?: ""
                                keyAlias = System.getenv("APKFORGE_KEY_ALIAS") ?: "${project.signingConfig.keyAlias.ifBlank { "apkforge_release" }}"
                                keyPassword = System.getenv("APKFORGE_KEY_PASS") ?: ""
                            }
                        }
                    }

                    buildTypes {
                        release {
                            signingConfig = if (hasCustomReleaseKeystore) {
                                signingConfigs.getByName("release")
                            } else {
                                signingConfigs.getByName("debug")
                            }
                            isMinifyEnabled = ${options.r8Minify}
                            isShrinkResources = ${options.shrinkResources}
                            proguardFiles(
                                getDefaultProguardFile("proguard-android-optimize.txt"),
                                "proguard-rules.pro"
                            )
                        }
                    }
                    $splitsBlock
                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_17
                        targetCompatibility = JavaVersion.VERSION_17
                    }
                    kotlinOptions {
                        jvmTarget = "17"
                    }
                }

                dependencies {
                    implementation("androidx.core:core-ktx:1.13.1")
                    implementation("androidx.appcompat:appcompat:1.7.0")
                    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
                    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
                    implementation("com.google.android.material:material:1.12.0")
                $biometricDep
                $playReviewDep
                }
            """.trimIndent()
        }

        entries["app/proguard-rules.pro"] = """
            -keepclassmembers class * {
                @android.webkit.JavascriptInterface <methods>;
            }
        """.trimIndent()

        val manifestXml = buildGeneratedManifest(project)
        entries["app/src/main/AndroidManifest.xml"] = ensureXmlStartsClean(manifestXml)
        val standaloneMinSdk = project.packageConfig.minSdk
        val standaloneTargetSdk = maxOf(project.packageConfig.targetSdk, 34)
        val standaloneManifest = manifestXml
            .replace(
                "@style/Theme.MaterialComponents.DayNight.NoActionBar",
                "@android:style/Theme.DeviceDefault.NoActionBar"
            )
            .replace(
                "package=\"${project.packageConfig.packageName}\">",
                "package=\"${project.packageConfig.packageName}\">\n    <uses-sdk android:minSdkVersion=\"$standaloneMinSdk\" android:targetSdkVersion=\"$standaloneTargetSdk\" />"
            )
        entries["standalone_cli/AndroidManifest.xml"] = ensureXmlStartsClean(standaloneManifest)
        if (isComposeSource) {
            entries["app/src/main/java/$pkgPath/ui/theme/Theme.kt"] =
                ComposeProjectTemplateGenerator.buildThemeKt(project)
            entries["app/src/main/java/$pkgPath/MainActivity.kt"] =
                ComposeProjectTemplateGenerator.buildComposeMainActivity(
                    project = project,
                    relEntryAsset = relEntryAsset,
                    hasRealWorkspaceAssets = realAssets.isNotEmpty()
                )
        } else {
            entries["app/src/main/java/$pkgPath/MainActivity.kt"] = buildGeneratedMainActivity(project, relEntryAsset)
        }
        entries["standalone_cli/src/$pkgPath/MainActivity.java"] = buildStandaloneJavaActivity(project, relEntryAsset)
        val escapedAppName = escapeAndroidXmlString(project.name.ifBlank { "ApkForge App" })
        entries["app/src/main/res/values/strings.xml"] = ensureXmlStartsClean(
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <string name=\"app_name\">$escapedAppName</string>\n" +
                "</resources>"
        )

        // Adaptive Icon XMLs (only add vector foreground when no custom raster image exists, preventing aapt2 duplicate conflict)
        entries["app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"] = iconPack.adaptiveXml
        entries["app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"] = iconPack.adaptiveXml
        entries["app/src/main/res/drawable/ic_launcher_background.xml"] = iconPack.backgroundDrawableXml
        if (iconPack.foregroundVectorXml.isNotBlank()) {
            entries["app/src/main/res/drawable/ic_launcher_foreground.xml"] = iconPack.foregroundVectorXml
        }

        entries["app/src/main/assets/app_config.json"] = """
            {
              "name": "${project.name}",
              "packageName": "$pkg",
              "versionCode": ${project.packageConfig.versionCode},
              "versionName": "${project.packageConfig.versionName}",
              "sourceType": "${project.sourceType.name}",
              "sourceValue": "${project.sourceValue}",
              "entryAsset": "$relEntryAsset",
              "realWorkspaceFiles": ${realAssets.size},
              "pullToRefresh": ${project.webViewConfig.pullToRefresh},
              "fullScreen": ${project.webViewConfig.fullScreen},
              "jsBridgeEnabled": ${project.bridgeConfig.jsBridgeEnabled},
              "externalIntents": ${project.bridgeConfig.handleExternalIntents},
              "deepLinkDomain": "${project.bridgeConfig.deepLinkDomain}",
              "remoteConfigUrl": "${project.bridgeConfig.remoteConfigUrl}",
              "forceDarkCss": ${project.proConfig.forceDarkCss},
              "adBlocker": ${project.proConfig.adAndPopupBlocker},
              "flagSecure": ${project.proConfig.flagSecureEnabled},
              "sslPinning": ${project.proConfig.sslPinningEnabled},
              "drawerEnabled": ${project.proConfig.drawerEnabled},
              "fabMenuEnabled": ${project.proConfig.fabMenuEnabled}
            }
        """.trimIndent()

        // Fallback demo index.html ONLY when user hasn't imported real HTML/ZIP files
        if (!realAssets.containsKey("app/src/main/assets/index.html") && realAssets.isEmpty()) {
            entries["app/src/main/assets/index.html"] = buildDemoIndexHtml(project)
        }

        if (project.bridgeConfig.gitHubCiEnabled || forceCloudWorkflow) {
            entries[".github/workflows/apkforge-build.yml"] = buildGitHubActionsWorkflow(safeName, cloudBuildTarget)
        }

        entries["PRIVACY_POLICY.md"] = generatePrivacyPolicy(project)
        entries["README_APKFORGE.md"] = """
            # ${project.name} - Generated by ApkForge Pro Studio
            - Package ID: `$pkg`
            - Version: `${project.packageConfig.versionName}` (${project.packageConfig.versionCode})
            - Imported Workspace Assets: `${realAssets.size}` real files (entry: `$relEntryAsset`)
            - Custom Launcher Icon Applied: `${iconPack.usedCustomImage}` (${project.proConfig.iconShape})
            - Native JS Bridge (`window.ApkForge` v2): Biometric=${project.proConfig.biometricAuthBridge}, QR=${project.proConfig.qrScannerBridge}, InAppReview=${project.proConfig.inAppReviewBridge}
            - Security: FLAG_SECURE=${project.proConfig.flagSecureEnabled}, SSL Pinning=${project.proConfig.sslPinningEnabled}, AdBlocker=${project.proConfig.adAndPopupBlocker}
            - GitHub Actions CI: `.github/workflows/apkforge-build.yml`
        """.trimIndent()

        return entries
    }

    /**
     * يجمع جميع الملفات الثنائية الحقيقية (أيقونات المستخدم بمختلف الدقات + أصول المواقع المستوردة) لرفعها إلى السحابة أو تحزيمها.
     */
    fun buildProjectBinaryEntries(project: AppProject): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        val iconPack = AdaptiveIconStudio.generateIconPack(context, project)
        iconPack.densityPngs.forEach { (path, bytes) ->
            result[path] = bytes
        }
        val textKeys = buildProjectTextEntries(project).keys
        collectRealWorkspaceAssetFiles(project).forEach { (path, file) ->
            if (file.exists() && file.isFile && !textKeys.contains(path)) {
                result[path] = file.readBytes()
            }
        }
        return result
    }

    /**
     * تصدير مشروع Android Studio كامل (مع الأصول المستوردة الحقيقية والأيقونات الحقيقية و GitHub CI) داخل ZIP حقيقي
     */
    fun exportAndroidStudioProjectZip(
        project: AppProject,
        options: BuildOptimizationOptions = BuildOptimizationOptions()
    ): ExportedZipResult {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "ApkForgeApp" }
        val zipFile = File(getExportsDir(), "${safeName}_AndroidStudio_CI.zip")
        val textEntries = buildProjectTextEntries(project, options)
        val realAssets = collectRealWorkspaceAssetFiles(project)
        val iconPack = AdaptiveIconStudio.generateIconPack(context, project)

        var writtenCount = 0
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            textEntries.forEach { (path, content) ->
                if (!realAssets.containsKey(path)) {
                    zos.putNextEntry(ZipEntry(path))
                    zos.write(content.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                    writtenCount++
                }
            }
            realAssets.forEach { (path, diskFile) ->
                if (diskFile.exists() && diskFile.isFile) {
                    zos.putNextEntry(ZipEntry(path))
                    FileInputStream(diskFile).use { fis -> fis.copyTo(zos) }
                    zos.closeEntry()
                    writtenCount++
                }
            }
            iconPack.densityPngs.forEach { (path, bytes) ->
                zos.putNextEntry(ZipEntry(path))
                zos.write(bytes)
                zos.closeEntry()
                writtenCount++
            }
        }

        return ExportedZipResult(
            zipFile = zipFile,
            includedFilesCount = writtenCount,
            sizeKb = maxOf(1L, zipFile.length() / 1024L),
            includesGitHubCi = project.bridgeConfig.gitHubCiEnabled,
            includesJsBridge = project.bridgeConfig.jsBridgeEnabled
        )
    }

    fun exportProjectConfigFile(project: AppProject, jsonContent: String): File {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_]"), "_").ifBlank { "project" }
        val file = File(getExportsDir(), "${safeName}.apkforge")
        file.writeText(jsonContent, Charsets.UTF_8)
        return file
    }

    private fun ensureXmlStartsClean(rawXml: String): String {
        val noBom = rawXml.replace("\uFEFF", "").trimStart()
        val firstLt = noBom.indexOf('<')
        return if (firstLt >= 0) noBom.substring(firstLt) else noBom
    }

    private fun escapeAndroidXmlString(raw: String): String {
        var escaped = raw
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
        if (escaped.startsWith("@") || escaped.startsWith("?")) {
            escaped = "\\$escaped"
        }
        return escaped
    }

    /**
     * يحفظ الحزمة مباشرة في مجلد التنزيلات العام بالهاتف (Download/) باستخدام MediaStore API
     * دون الحاجة لأي أذونات تخزين على Android 10+.
     */
    fun savePackageToPublicDownloadsViaMediaStore(
        sourceFilePath: String,
        preferredFileName: String? = null,
        projectName: String = ""
    ): MediaStoreSaveResult {
        val resolvedFile = resolveBestExportFile(sourceFilePath, projectName)
            ?: return MediaStoreSaveResult(
                success = false,
                errorMessage = "الملف غير موجود على القرص حالياً"
            )
        val displayName = preferredFileName?.takeIf { it.isNotBlank() }?.let { pref ->
            if (resolvedFile.extension.equals("apk", ignoreCase = true) && !pref.endsWith(".apk", ignoreCase = true)) {
                resolvedFile.name
            } else {
                pref
            }
        } ?: resolvedFile.name

        val mimeType = when {
            displayName.endsWith(".apk", ignoreCase = true) -> "application/vnd.android.package-archive"
            displayName.endsWith(".apks", ignoreCase = true) -> "application/octet-stream"
            displayName.endsWith(".aab", ignoreCase = true) -> "application/octet-stream"
            displayName.endsWith(".zip", ignoreCase = true) -> "application/zip"
            else -> "application/octet-stream"
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                resolver.delete(
                    collection,
                    "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?",
                    arrayOf(displayName, "${Environment.DIRECTORY_DOWNLOADS}/")
                )
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(collection, cv)
                    ?: return MediaStoreSaveResult(
                        success = false,
                        errorMessage = "تعذر إنشاء سجل MediaStore في التنزيلات"
                    )
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(resolvedFile).use { it.copyTo(out) }
                } ?: return MediaStoreSaveResult(
                    success = false,
                    errorMessage = "تعذر فتح مسار الكتابة في MediaStore"
                )
                cv.clear()
                cv.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, cv, null, null)

                MediaStoreSaveResult(
                    success = true,
                    savedUri = uri,
                    displayPath = "Download/$displayName",
                    fileName = displayName
                )
            } else {
                @Suppress("DEPRECATION")
                val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply {
                    mkdirs()
                }
                val dest = File(pubDir, displayName)
                resolvedFile.copyTo(dest, overwrite = true)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
                MediaStoreSaveResult(
                    success = true,
                    savedUri = uri,
                    displayPath = dest.absolutePath,
                    fileName = displayName
                )
            }
        } catch (e: Exception) {
            MediaStoreSaveResult(
                success = false,
                errorMessage = e.message ?: "خطأ غير متوقع أثناء الحفظ"
            )
        }
    }

    private fun resolveBestExportFile(filePath: String, projectName: String = ""): File? {
        val direct = File(filePath)
        if (direct.exists() && direct.isFile && direct.extension.equals("apk", ignoreCase = true)) {
            return direct
        }
        val safeName = projectName.replace(Regex("[^a-zA-Z0-9_]"), "_")
        if (safeName.isNotBlank()) {
            val candidateDirs = listOf(
                File(Constants.DOWNLOADS_DIR),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )
            candidateDirs.forEach { dir ->
                val apkMatch = dir.listFiles()
                    ?.filter { it.isFile && it.extension.equals("apk", true) && it.name.startsWith("${safeName}_", true) }
                    ?.maxByOrNull { it.lastModified() }
                if (apkMatch != null) return apkMatch
            }
        }
        return if (direct.exists() && direct.isFile) direct else null
    }

    fun installApkDirectly(
        filePath: String,
        buildType: String,
        projectName: String = ""
    ): InstallLaunchResult {
        val file = resolveBestExportFile(filePath, projectName)
            ?: return InstallLaunchResult(
                launched = false,
                isBinaryApk = false,
                messageAr = "الملف غير موجود على القرص — أعد البناء لتوليده فعلياً"
            )

        // If the artifact is an .apks split bundle archive, extract splits/base-master.apk for direct installation
        val launchFile = if (file.extension.equals("apks", ignoreCase = true)) {
            extractBaseApkFromApksBundle(file) ?: file
        } else {
            file
        }

        val isApk = launchFile.extension.equals("apk", ignoreCase = true) ||
            (buildType.equals("APK", ignoreCase = true) && launchFile.name.endsWith(".apk", ignoreCase = true))

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", launchFile)
        } catch (_: Exception) {
            null
        } ?: return InstallLaunchResult(
            launched = false,
            isBinaryApk = isApk,
            messageAr = "تعذر تجهيز رابط الملف للتثبيت"
        )

        val mime = when {
            isApk -> "application/vnd.android.package-archive"
            launchFile.extension.equals("zip", ignoreCase = true) -> "application/zip"
            else -> "application/octet-stream"
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(viewIntent)
            InstallLaunchResult(
                launched = true,
                isBinaryApk = isApk,
                messageAr = if (isApk) "جارٍ فتح نافذة تثبيت ${file.name}..." else "تم فتح الحزمة الموقّعة ${file.name}"
            )
        } catch (_: Exception) {
            try {
                val chooser = Intent.createChooser(viewIntent, if (isApk) "تثبيت APK" else "فتح ${file.name}").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                InstallLaunchResult(true, isApk, "تم فتح ${file.name}")
            } catch (_: Exception) {
                shareFileWithChooser(file, "*/*", "مشاركة / فتح ${file.name}")
                InstallLaunchResult(true, isApk, "تم فتح قائمة المشاركة لحفظ وتثبيت ${file.name}")
            }
        }
    }

    private fun extractBaseApkFromApksBundle(apksFile: File): File? {
        return try {
            val outDir = File(context.cacheDir, "apks_install_cache").apply { mkdirs() }
            val outApk = File(outDir, apksFile.nameWithoutExtension + "_base.apk")
            java.util.zip.ZipFile(apksFile).use { zip ->
                val baseEntry = zip.getEntry("splits/base-master.apk")
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(".apk", true) }
                    ?: return null
                zip.getInputStream(baseEntry).use { inp ->
                    FileOutputStream(outApk).use { out -> inp.copyTo(out) }
                }
            }
            outApk.takeIf { it.exists() && it.length() > 0L }
        } catch (_: Exception) {
            null
        }
    }

    fun openOrInstallFile(filePath: String, buildType: String): Boolean {
        return installApkDirectly(filePath, buildType).launched
    }

    fun shareFileWithChooser(file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun buildGeneratedManifest(project: AppProject): String {
        val p = project.permissionsConfig
        val perms = buildString {
            appendLine("""    <uses-permission android:name="android.permission.INTERNET" />""")
            appendLine("""    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />""")
            if (p.camera || project.proConfig.qrScannerBridge) appendLine("""    <uses-permission android:name="android.permission.CAMERA" />""")
            if (p.location) {
                appendLine("""    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />""")
                appendLine("""    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />""")
            }
            if (p.microphone) appendLine("""    <uses-permission android:name="android.permission.RECORD_AUDIO" />""")
            if (p.notifications || project.pushConfig.enabled) {
                appendLine("""    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />""")
            }
            if (p.vibration || (project.pushConfig.enabled && project.pushConfig.vibrationEnabled)) {
                appendLine("""    <uses-permission android:name="android.permission.VIBRATE" />""")
            }
            if (p.bluetooth) appendLine("""    <uses-permission android:name="android.permission.BLUETOOTH" />""")
            if (project.proConfig.biometricAuthBridge) appendLine("""    <uses-permission android:name="android.permission.USE_BIOMETRIC" />""")
        }.trimEnd()

        val deepLinkFilter = if (project.bridgeConfig.deepLinkDomain.isNotBlank()) {
            val host = project.bridgeConfig.deepLinkDomain.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
            "\n            <intent-filter android:autoVerify=\"true\">\n" +
                "                <action android:name=\"android.intent.action.VIEW\" />\n" +
                "                <category android:name=\"android.intent.category.DEFAULT\" />\n" +
                "                <category android:name=\"android.intent.category.BROWSABLE\" />\n" +
                "                <data android:scheme=\"https\" android:host=\"$host\" />\n" +
                "            </intent-filter>"
        } else ""

        val adMobMeta = if (project.bridgeConfig.adMobAppId.isNotBlank()) {
            "\n        <meta-data android:name=\"com.google.android.gms.ads.APPLICATION_ID\" android:value=\"${project.bridgeConfig.adMobAppId}\" />"
        } else ""

        val pushMeta = if (project.pushConfig.enabled && project.pushConfig.appId.isNotBlank()) {
            "\n        <meta-data android:name=\"onesignal_app_id\" android:value=\"${project.pushConfig.appId}\" />"
        } else ""

        val raw = buildString {
            appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            appendLine("""<manifest xmlns:android="http://schemas.android.com/apk/res/android"""")
            appendLine("""    package="${project.packageConfig.packageName}">""")
            appendLine("""    <supports-screens""")
            appendLine("""        android:smallScreens="true"""")
            appendLine("""        android:normalScreens="true"""")
            appendLine("""        android:largeScreens="true"""")
            appendLine("""        android:xlargeScreens="true"""")
            appendLine("""        android:anyDensity="true" />""")
            appendLine(perms)
            appendLine("""    <application""")
            appendLine("""        android:allowBackup="true"""")
            appendLine("""        android:hardwareAccelerated="true"""")
            appendLine("""        android:resizeableActivity="true"""")
            appendLine("""        android:icon="@mipmap/ic_launcher"""")
            appendLine("""        android:roundIcon="@mipmap/ic_launcher_round"""")
            appendLine("""        android:label="@string/app_name"""")
            appendLine("""        android:usesCleartextTraffic="true"""")
            appendLine("""        android:supportsRtl="true"""")
            append("""        android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar">""")
            append(pushMeta)
            append(adMobMeta)
            appendLine()
            appendLine("""        <activity""")
            appendLine("""            android:name=".MainActivity"""")
            appendLine("""            android:label="@string/app_name"""")
            appendLine("""            android:exported="true"""")
            appendLine("""            android:windowSoftInputMode="adjustResize"""")
            appendLine("""            android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden">""")
            appendLine("""            <intent-filter>""")
            appendLine("""                <action android:name="android.intent.action.MAIN" />""")
            appendLine("""                <category android:name="android.intent.category.LAUNCHER" />""")
            append("""            </intent-filter>""")
            append(deepLinkFilter)
            appendLine()
            appendLine("""        </activity>""")
            appendLine("""    </application>""")
            append("""</manifest>""")
        }
        return ensureXmlStartsClean(raw)
    }

    /**
     * Activity نقية بـ Android SDK فقط (بدون androidx) للترجمة القياسية عبر aapt2 + javac + d8 + zipalign + apksig
     * وتعرض التطبيق بملء الشاشة الكاملة (بدون نصف شاشة) مع كل التخصيصات الحقيقية.
     */
    fun buildStandaloneJavaActivity(project: AppProject, relEntryAsset: String = "index.html"): String {
        val pkg = project.packageConfig.packageName
        val pro = project.proConfig
        val startUrl = if (project.sourceType == SourceType.WEB_URL && project.sourceValue.startsWith("http")) {
            project.sourceValue
        } else {
            "file:///android_asset/$relEntryAsset"
        }
        val baseOrigin = if (startUrl.startsWith("http")) {
            val uri = Uri.parse(startUrl)
            "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
        } else {
            "file:///android_asset"
        }
        val escapedAppTitle = project.name.replace("\\", "\\\\").replace("\"", "\\\"").ifBlank { "ApkForge App" }
        val escapedDrawerSubtitle = pro.drawerHeaderSubtitle.replace("\\", "\\\\").replace("\"", "\\\"").ifBlank { "القائمة السريعة" }
        val omni = project.omniConfig
        val escapedCss = pro.customCss.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("\n", " ")
        val darkCss = if (pro.forceDarkCss) {
            "html { filter: invert(0.92) hue-rotate(180deg) !important; background: #0d1117 !important; } img, video { filter: invert(1) hue-rotate(180deg) !important; }"
        } else ""
        val gradientCss = if (omni.gradientEnabled) {
            "body, header, .hero { background: linear-gradient(135deg, ${omni.gradientStartHex}, ${omni.gradientEndHex}) !important; }"
        } else ""
        val fontScaleCss = "html { font-size: ${omni.fontScalePercent.coerceIn(75, 145)}% !important; }"
        val hideCss = omni.customHideSelectorsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }?.let { "${it.joinToString(", ")} { display: none !important; }" } ?: ""
        val combinedCss = "$darkCss $gradientCss $fontScaleCss $hideCss $escapedCss".trim()
        val acceleratorJs = if (omni.appAcceleratorEnabled) {
            "(function(){try{${if (omni.acceleratorPrefetchLinks) "document.querySelectorAll('a[href^=\"http\"]').forEach(function(a){var l=document.createElement('link');l.rel='prefetch';l.href=a.href;document.head.appendChild(l);});" else ""}${if (omni.acceleratorImageCompression) "document.querySelectorAll('img').forEach(function(i){if(!i.getAttribute('loading'))i.setAttribute('loading','lazy');});" else ""}}catch(e){}})();"
        } else ""
        val escapedJs = "${pro.customJs} $acceleratorJs".replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").trim()
        val blockedHostsArray = pro.blockedHostsCsv
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .joinToString(",") { "\"$it\"" }

        val contact = pro.fabWhatsappNumber.ifBlank { "ziorio950@gmail.com" }
        val isFullScreen = project.webViewConfig.fullScreen
        val splash = project.splashConfig
        val onboarding = project.onboardingConfig
        val bottomNav = project.bottomNavConfig
        val pageLoader = project.webViewConfig.pageLoader
        val offlineFallback = project.webViewConfig.offlineFallback

        val onboardingSlidesJava = onboarding.slides.joinToString(", ") { slide ->
            val t = slide.title.replace("\\", "\\\\").replace("\"", "\\\"")
            val d = slide.description.replace("\\", "\\\\").replace("\"", "\\\"")
            "new String[]{\"$t\", \"$d\"}"
        }

        val bottomNavBuildJava = if (bottomNav.enabled && bottomNav.destinations.isNotEmpty()) {
            val destItems = bottomNav.destinations.take(5).mapIndexed { idx, dest ->
                val rawTarget = dest.targetUrl.trim()
                val resolvedTarget = when {
                    rawTarget.startsWith("http://") || rawTarget.startsWith("https://") || rawTarget.startsWith("file://") -> rawTarget
                    startUrl.startsWith("http") && rawTarget.startsWith("/") -> "$baseOrigin$rawTarget"
                    startUrl.startsWith("http") && rawTarget.isNotBlank() -> "$baseOrigin/$rawTarget"
                    rawTarget.isNotBlank() && rawTarget != "/" -> "file:///android_asset/${rawTarget.trimStart('/')}"
                    else -> startUrl
                }
                val safeTitle = dest.title.ifBlank { "تبويب ${idx + 1}" }.replace("\"", "\\\"")
                Triple(idx, safeTitle, resolvedTarget)
            }
            val tabsArray = destItems.joinToString(", ") { "new String[]{\"${it.second}\", \"${it.third}\"}" }
            """
            final String[][] navTabsData = new String[][]{$tabsArray};
            final LinearLayout navBar = new LinearLayout(this);
            navBar.setOrientation(LinearLayout.HORIZONTAL);
            navBar.setGravity(Gravity.CENTER_VERTICAL);
            navBar.setBackgroundColor(safeColor("${bottomNav.barColorHex}", 0xFF131520));
            navBar.setPadding(dp(4), dp(6), dp(4), dp(6));
            final Button[] navButtons = new Button[navTabsData.length];
            final int activeNavColor = safeColor("${bottomNav.activeColorHex}", safeColor("${project.accentColorHex}", 0xFFFF7A2F));
            final int inactiveNavColor = safeColor("${bottomNav.inactiveColorHex}", 0xFF9AA3B2);
            for (int i = 0; i < navTabsData.length; i++) {
                final int tabIndex = i;
                Button btn = new Button(this);
                btn.setAllCaps(false);
                btn.setTextSize(12f);
                btn.setBackgroundColor(Color.TRANSPARENT);
                btn.setText(${if (bottomNav.showLabels) "navTabsData[i][0]" else "\"•\""});
                btn.setTextColor(i == 0 ? activeNavColor : inactiveNavColor);
                btn.setOnClickListener(v -> {
                    for (int j = 0; j < navButtons.length; j++) {
                        navButtons[j].setTextColor(j == tabIndex ? activeNavColor : inactiveNavColor);
                    }
                    webView.loadUrl(navTabsData[tabIndex][1]);
                });
                navButtons[i] = btn;
                navBar.addView(btn, new LinearLayout.LayoutParams(0, dp(48), 1f));
            }
            column.addView(navBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            """.trimIndent()
        } else ""

        return """
            package $pkg;

            import android.annotation.SuppressLint;
            import android.app.Activity;
            import android.content.Context;
            import android.content.Intent;
            import android.content.SharedPreferences;
            import android.content.res.ColorStateList;
            import android.graphics.Color;
            import android.graphics.Typeface;
            import android.graphics.drawable.GradientDrawable;
            import android.net.ConnectivityManager;
            import android.net.Uri;
            import android.net.http.SslError;
            import android.os.Build;
            import android.os.Bundle;
            import android.os.VibrationEffect;
            import android.os.Vibrator;
            import android.util.TypedValue;
            import android.view.Gravity;
            import android.view.View;
            import android.view.ViewGroup;
            import android.view.Window;
            import android.view.WindowInsets;
            import android.view.WindowInsetsController;
            import android.view.WindowManager;
            import android.annotation.TargetApi;
            import android.webkit.JavascriptInterface;
            import android.webkit.RenderProcessGoneDetail;
            import android.webkit.SslErrorHandler;
            import android.webkit.WebChromeClient;
            import android.webkit.WebResourceError;
            import android.webkit.WebResourceRequest;
            import android.webkit.WebResourceResponse;
            import android.webkit.WebSettings;
            import android.webkit.WebView;
            import android.webkit.WebViewClient;
            import android.widget.Button;
            import android.widget.FrameLayout;
            import android.widget.ImageView;
            import android.widget.LinearLayout;
            import android.widget.ProgressBar;
            import android.widget.TextView;
            import android.widget.Toast;
            import java.io.ByteArrayInputStream;

            public class MainActivity extends Activity {
                private WebView webView;
                private FrameLayout root;
                private FrameLayout videoCustomContainer;
                private View videoCustomView;
                private WebChromeClient.CustomViewCallback videoCallback;
                private final String[] blockedHosts = new String[]{$blockedHostsArray};

                private int dp(int value) {
                    return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics()));
                }

                private int safeColor(String hex, int fallback) {
                    try {
                        return Color.parseColor(hex);
                    } catch (Exception ignored) {
                        return fallback;
                    }
                }

                private void applyScreenMode() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        WindowManager.LayoutParams wlp = getWindow().getAttributes();
                        wlp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                        getWindow().setAttributes(wlp);
                    }
                    ${if (isFullScreen) """
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        getWindow().setDecorFitsSystemWindows(false);
                        WindowInsetsController controller = getWindow().getInsetsController();
                        if (controller != null) {
                            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                        }
                    } else {
                        getWindow().getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                        );
                    }
                    """ else """
                    int barBg = safeColor("${project.primaryColorHex}", 0xFF0F1318);
                    getWindow().setStatusBarColor(barBg);
                    getWindow().setNavigationBarColor(safeColor("${bottomNav.barColorHex}", barBg));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        getWindow().setDecorFitsSystemWindows(true);
                    }
                    """}
                }

                @Override
                public void onWindowFocusChanged(boolean hasFocus) {
                    super.onWindowFocusChanged(hasFocus);
                    if (hasFocus) applyScreenMode();
                }

                @SuppressLint("SetJavaScriptEnabled")
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    requestWindowFeature(Window.FEATURE_NO_TITLE);
                    ${if (pro.flagSecureEnabled) "getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);" else ""}

                    root = new FrameLayout(this);
                    root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    root.setBackgroundColor(safeColor("#0F1318", Color.BLACK));

                    LinearLayout column = new LinearLayout(this);
                    column.setOrientation(LinearLayout.VERTICAL);
                    column.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                    ${if (pro.drawerEnabled) """
                    final FrameLayout drawerOverlay = new FrameLayout(this);
                    drawerOverlay.setVisibility(View.GONE);
                    drawerOverlay.setBackgroundColor(0xB3000000);
                    LinearLayout drawerPanel = new LinearLayout(this);
                    drawerPanel.setOrientation(LinearLayout.VERTICAL);
                    drawerPanel.setBackgroundColor(safeColor("#141925", 0xFF141925));
                    drawerPanel.setPadding(dp(20), dp(36), dp(20), dp(24));

                    ImageView drawerIcon = new ImageView(this);
                    drawerIcon.setImageResource(R.drawable.ic_splash_logo);
                    drawerIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    drawerPanel.addView(drawerIcon, new LinearLayout.LayoutParams(dp(64), dp(64)));

                    TextView drawerTitle = new TextView(this);
                    drawerTitle.setText("$escapedAppTitle");
                    drawerTitle.setTextColor(Color.WHITE);
                    drawerTitle.setTextSize(18f);
                    drawerTitle.setTypeface(null, Typeface.BOLD);
                    drawerTitle.setPadding(0, dp(12), 0, dp(4));
                    drawerPanel.addView(drawerTitle);

                    TextView drawerSub = new TextView(this);
                    drawerSub.setText("$escapedDrawerSubtitle");
                    drawerSub.setTextColor(0xFFB0B8C9);
                    drawerSub.setTextSize(12f);
                    drawerSub.setPadding(0, 0, 0, dp(20));
                    drawerPanel.addView(drawerSub);

                    Button homeMenuBtn = new Button(this);
                    homeMenuBtn.setText("الرئيسية");
                    homeMenuBtn.setAllCaps(false);
                    homeMenuBtn.setTextColor(Color.WHITE);
                    homeMenuBtn.setBackgroundColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F));
                    homeMenuBtn.setOnClickListener(v -> {
                        drawerOverlay.setVisibility(View.GONE);
                        webView.loadUrl("$startUrl");
                    });
                    drawerPanel.addView(homeMenuBtn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)));

                    Button reloadMenuBtn = new Button(this);
                    reloadMenuBtn.setText("إعادة تحميل الصفحة");
                    reloadMenuBtn.setAllCaps(false);
                    reloadMenuBtn.setTextColor(Color.WHITE);
                    reloadMenuBtn.setBackgroundColor(0xFF1E2636);
                    LinearLayout.LayoutParams rmLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46));
                    rmLp.topMargin = dp(10);
                    reloadMenuBtn.setOnClickListener(v -> {
                        drawerOverlay.setVisibility(View.GONE);
                        webView.reload();
                    });
                    drawerPanel.addView(reloadMenuBtn, rmLp);

                    FrameLayout.LayoutParams dpLp = new FrameLayout.LayoutParams(dp(270), FrameLayout.LayoutParams.MATCH_PARENT);
                    dpLp.gravity = Gravity.START;
                    drawerOverlay.addView(drawerPanel, dpLp);
                    drawerOverlay.setOnClickListener(v -> drawerOverlay.setVisibility(View.GONE));

                    LinearLayout topBar = new LinearLayout(this);
                    topBar.setOrientation(LinearLayout.HORIZONTAL);
                    topBar.setGravity(Gravity.CENTER_VERTICAL);
                    topBar.setBackgroundColor(safeColor("${project.primaryColorHex}", 0xFF131622));
                    topBar.setPadding(dp(12), dp(8), dp(12), dp(8));

                    Button menuBtn = new Button(this);
                    menuBtn.setText("☰");
                    menuBtn.setTextColor(Color.WHITE);
                    menuBtn.setTextSize(18f);
                    menuBtn.setBackgroundColor(Color.TRANSPARENT);
                    menuBtn.setOnClickListener(v -> drawerOverlay.setVisibility(View.VISIBLE));
                    topBar.addView(menuBtn, new LinearLayout.LayoutParams(dp(48), dp(48)));

                    ImageView barIcon = new ImageView(this);
                    barIcon.setImageResource(R.drawable.ic_splash_logo);
                    LinearLayout.LayoutParams biLp = new LinearLayout.LayoutParams(dp(30), dp(30));
                    biLp.setMargins(dp(6), 0, dp(8), 0);
                    topBar.addView(barIcon, biLp);

                    TextView barTitle = new TextView(this);
                    barTitle.setText("$escapedAppTitle");
                    barTitle.setTextColor(Color.WHITE);
                    barTitle.setTextSize(16f);
                    barTitle.setTypeface(null, Typeface.BOLD);
                    topBar.addView(barTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                    Button refreshBtn = new Button(this);
                    refreshBtn.setText("↻");
                    refreshBtn.setTextColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F));
                    refreshBtn.setTextSize(18f);
                    refreshBtn.setBackgroundColor(Color.TRANSPARENT);
                    refreshBtn.setOnClickListener(v -> webView.reload());
                    topBar.addView(refreshBtn, new LinearLayout.LayoutParams(dp(48), dp(48)));

                    column.addView(topBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    """.trimIndent() else ""}

                    FrameLayout webContainer = new FrameLayout(this);
                    webView = new WebView(this);
                    webView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                    webView.setBackgroundColor(safeColor("#0F1318", Color.BLACK));
                    webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
                    }

                    WebSettings ws = webView.getSettings();
                    ws.setJavaScriptEnabled(true);
                    ws.setDomStorageEnabled(true);
                    ws.setDatabaseEnabled(true);
                    ws.setAllowFileAccess(true);
                    ws.setAllowContentAccess(true);
                    ws.setAllowFileAccessFromFileURLs(true);
                    ws.setAllowUniversalAccessFromFileURLs(true);
                    ws.setUseWideViewPort(true);
                    ws.setLoadWithOverviewMode(true);
                    ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
                    ws.setMediaPlaybackRequiresUserGesture(false);
                    ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    ws.setJavaScriptCanOpenWindowsAutomatically(true);
                    ws.setSupportZoom(${project.webViewConfig.supportZoom});
                    ws.setBuiltInZoomControls(${project.webViewConfig.supportZoom});
                    ws.setDisplayZoomControls(false);
                    ws.setCacheMode(${if (pro.assetPreloaderEnabled) "WebSettings.LOAD_CACHE_ELSE_NETWORK" else "WebSettings.LOAD_DEFAULT"});
                    ${if (project.bridgeConfig.jsBridgeEnabled) "webView.addJavascriptInterface(new ApkForgeBridge(this), \"ApkForge\");" else ""}

                    final LinearLayout pageLoaderOverlay = new LinearLayout(this);
                    pageLoaderOverlay.setOrientation(LinearLayout.VERTICAL);
                    pageLoaderOverlay.setGravity(Gravity.CENTER);
                    pageLoaderOverlay.setVisibility(${if (pageLoader.enabled) "View.VISIBLE" else "View.GONE"});
                    ${if (pageLoader.showOnCard) """
                    GradientDrawable cardBg = new GradientDrawable();
                    cardBg.setColor(safeColor("${pageLoader.cardColorHex}", 0xFF1E2130));
                    cardBg.setCornerRadius(dp(${pageLoader.cardCornerRadiusDp}));
                    pageLoaderOverlay.setBackground(cardBg);
                    pageLoaderOverlay.setPadding(dp(22), dp(22), dp(22), dp(22));
                    """ else ""}
                    ProgressBar pageSpinner = new ProgressBar(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        pageSpinner.setIndeterminateTintList(ColorStateList.valueOf(safeColor("${project.accentColorHex}", 0xFFFF7A2F)));
                    }
                    pageLoaderOverlay.addView(pageSpinner, new LinearLayout.LayoutParams(dp(${pageLoader.sizeDp.coerceIn(28, 84)}), dp(${pageLoader.sizeDp.coerceIn(28, 84)})));

                    final LinearLayout offlineOverlay = new LinearLayout(this);
                    offlineOverlay.setOrientation(LinearLayout.VERTICAL);
                    offlineOverlay.setGravity(Gravity.CENTER);
                    offlineOverlay.setBackgroundColor(safeColor("#0F1318", 0xFF0F1318));
                    offlineOverlay.setPadding(dp(28), dp(28), dp(28), dp(28));
                    offlineOverlay.setVisibility(View.GONE);

                    ImageView offlineLogo = new ImageView(this);
                    offlineLogo.setImageResource(R.drawable.ic_splash_logo);
                    offlineOverlay.addView(offlineLogo, new LinearLayout.LayoutParams(dp(72), dp(72)));

                    TextView offlineTitle = new TextView(this);
                    offlineTitle.setText("لا يوجد اتصال بالإنترنت");
                    offlineTitle.setTextColor(Color.WHITE);
                    offlineTitle.setTextSize(18f);
                    offlineTitle.setTypeface(null, Typeface.BOLD);
                    offlineTitle.setGravity(Gravity.CENTER);
                    offlineTitle.setPadding(0, dp(14), 0, dp(6));
                    offlineOverlay.addView(offlineTitle);

                    TextView offlineDesc = new TextView(this);
                    offlineDesc.setText("تحقق من الشبكة لإعادة تحميل $escapedAppTitle");
                    offlineDesc.setTextColor(0xFFB0B8C9);
                    offlineDesc.setTextSize(13f);
                    offlineDesc.setGravity(Gravity.CENTER);
                    offlineDesc.setPadding(0, 0, 0, dp(18));
                    offlineOverlay.addView(offlineDesc);

                    Button retryBtn = new Button(this);
                    retryBtn.setText("إعادة المحاولة");
                    retryBtn.setAllCaps(false);
                    retryBtn.setTextColor(Color.WHITE);
                    retryBtn.setBackgroundColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F));
                    retryBtn.setOnClickListener(v -> {
                        offlineOverlay.setVisibility(View.GONE);
                        webView.reload();
                    });
                    offlineOverlay.addView(retryBtn, new LinearLayout.LayoutParams(dp(180), dp(48)));

                    webContainer.addView(webView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                    FrameLayout.LayoutParams loaderLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                    loaderLp.gravity = Gravity.CENTER;
                    webContainer.addView(pageLoaderOverlay, loaderLp);
                    webContainer.addView(offlineOverlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

                    webView.setWebChromeClient(new WebChromeClient() {
                        @Override
                        public void onProgressChanged(WebView view, int newProgress) {
                            if (${pageLoader.enabled}) {
                                pageLoaderOverlay.setVisibility(newProgress < 90 ? View.VISIBLE : View.GONE);
                            }
                        }

                        @Override
                        public void onShowCustomView(View view, CustomViewCallback callback) {
                            if (videoCustomView != null) {
                                callback.onCustomViewHidden();
                                return;
                            }
                            videoCustomView = view;
                            videoCallback = callback;
                            if (videoCustomContainer == null) {
                                videoCustomContainer = new FrameLayout(MainActivity.this);
                                videoCustomContainer.setBackgroundColor(Color.BLACK);
                                root.addView(videoCustomContainer, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                            }
                            videoCustomContainer.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                            videoCustomContainer.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onHideCustomView() {
                            if (videoCustomView == null) return;
                            if (videoCustomContainer != null) {
                                videoCustomContainer.removeView(videoCustomView);
                                videoCustomContainer.setVisibility(View.GONE);
                            }
                            videoCustomView = null;
                            if (videoCallback != null) videoCallback.onCustomViewHidden();
                        }
                    });

                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                            ${if (pro.adAndPopupBlocker) """
                            if (request != null && request.getUrl() != null && request.getUrl().getHost() != null) {
                                String host = request.getUrl().getHost().toLowerCase();
                                for (String blocked : blockedHosts) {
                                    if (!blocked.isEmpty() && host.contains(blocked)) {
                                        return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                                    }
                                }
                            }
                            """.trimIndent() else ""}
                            return super.shouldInterceptRequest(view, request);
                        }

                        @Override
                        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                            ${if (pro.sslPinningEnabled) "if (handler != null) handler.cancel();" else "if (handler != null) handler.proceed();"}
                        }

                        @Override
                        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                            if (${offlineFallback} && request != null && request.isForMainFrame()) {
                                offlineOverlay.setVisibility(View.VISIBLE);
                                pageLoaderOverlay.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            String url = request != null && request.getUrl() != null ? request.getUrl().toString() : "";
                            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:") || url.contains("wa.me/")) {
                                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                                return true;
                            }
                            return false;
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            pageLoaderOverlay.setVisibility(View.GONE);
                            String viewportPatch = "(function(){if(!document.querySelector('meta[name=viewport]')){var m=document.createElement('meta');m.name='viewport';m.content='width=device-width,initial-scale=1.0,viewport-fit=cover';document.head.appendChild(m);}var b=document.documentElement;b.style.width='100%';if(document.body){document.body.style.minHeight='100vh';document.body.style.margin=document.body.style.margin||'0';}})();";
                            view.evaluateJavascript(viewportPatch, null);
                            ${if (combinedCss.isNotBlank()) "view.evaluateJavascript(\"(function(){var s=document.createElement('style');s.innerHTML='$combinedCss';document.head.appendChild(s);})();\", null);" else ""}
                            ${if (escapedJs.isNotBlank()) "view.evaluateJavascript(\"$escapedJs\", null);" else ""}
                        }

                        @Override
                        @TargetApi(Build.VERSION_CODES.O)
                        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                            if (view != null) {
                                ViewGroup parent = (ViewGroup) view.getParent();
                                if (parent != null) parent.removeView(view);
                                view.destroy();
                            }
                            pageLoaderOverlay.setVisibility(View.GONE);
                            offlineOverlay.setVisibility(View.VISIBLE);
                            return true;
                        }
                    });

                    LinearLayout.LayoutParams wvLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                    );
                    column.addView(webContainer, wvLp);

                    $bottomNavBuildJava

                    root.addView(column, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                    ));

                    ${if (pro.drawerEnabled) "root.addView(drawerOverlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));" else ""}

                    ${if (pro.fabMenuEnabled) """
                    Button fab = new Button(this);
                    fab.setText("✉");
                    fab.setTextColor(Color.WHITE);
                    GradientDrawable fabBg = new GradientDrawable();
                    fabBg.setShape(GradientDrawable.OVAL);
                    fabBg.setColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F));
                    fab.setBackground(fabBg);
                    FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(dp(54), dp(54));
                    flp.gravity = Gravity.BOTTOM | Gravity.END;
                    flp.setMargins(dp(18), dp(18), dp(18), dp(${if (bottomNav.enabled && bottomNav.destinations.isNotEmpty()) 76 else 24}));
                    fab.setOnClickListener(v -> {
                        try { startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$contact"))); } catch (Exception ignored) {}
                    });
                    root.addView(fab, flp);
                    """.trimIndent() else ""}

                    ${if (onboarding.enabled && onboarding.slides.isNotEmpty()) """
                    final SharedPreferences prefs = getSharedPreferences("apkforge_app_prefs", MODE_PRIVATE);
                    if (!prefs.getBoolean("onboarding_done", false)) {
                        final String[][] slides = new String[][]{$onboardingSlidesJava};
                        final int[] slideIdx = new int[]{0};
                        final LinearLayout obOverlay = new LinearLayout(this);
                        obOverlay.setOrientation(LinearLayout.VERTICAL);
                        obOverlay.setGravity(Gravity.CENTER);
                        obOverlay.setBackgroundColor(safeColor("${onboarding.cardColorHex}", 0xFF101522));
                        obOverlay.setPadding(dp(24), dp(42), dp(24), dp(32));

                        ImageView obIcon = new ImageView(this);
                        obIcon.setImageResource(R.drawable.ic_splash_logo);
                        obOverlay.addView(obIcon, new LinearLayout.LayoutParams(dp(92), dp(92)));

                        final TextView obTitle = new TextView(this);
                        obTitle.setTextColor(safeColor("${onboarding.textColorHex}", Color.WHITE));
                        obTitle.setTextSize(22f);
                        obTitle.setTypeface(null, Typeface.BOLD);
                        obTitle.setGravity(Gravity.CENTER);
                        obTitle.setPadding(0, dp(20), 0, dp(10));
                        obTitle.setText(slides[0][0]);
                        obOverlay.addView(obTitle);

                        final TextView obDesc = new TextView(this);
                        obDesc.setTextColor(safeColor("${onboarding.textColorHex}", 0xFFD1D5DB));
                        obDesc.setTextSize(15f);
                        obDesc.setGravity(Gravity.CENTER);
                        obDesc.setPadding(dp(12), 0, dp(12), dp(28));
                        obDesc.setText(slides[0][1]);
                        obOverlay.addView(obDesc);

                        final Button obNext = new Button(this);
                        obNext.setAllCaps(false);
                        obNext.setTextColor(Color.WHITE);
                        obNext.setBackgroundColor(safeColor("${onboarding.accentColorHex}", safeColor("${project.accentColorHex}", 0xFFFF7A2F)));
                        obNext.setText(slides.length > 1 ? "التالي" : "البدء الآن");
                        obNext.setOnClickListener(v -> {
                            if (slideIdx[0] < slides.length - 1) {
                                slideIdx[0]++;
                                obTitle.setText(slides[slideIdx[0]][0]);
                                obDesc.setText(slides[slideIdx[0]][1]);
                                obNext.setText(slideIdx[0] == slides.length - 1 ? "البدء الآن" : "التالي");
                            } else {
                                prefs.edit().putBoolean("onboarding_done", true).apply();
                                root.removeView(obOverlay);
                            }
                        });
                        obOverlay.addView(obNext, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));

                        ${if (onboarding.showSkipButton) """
                        Button obSkip = new Button(this);
                        obSkip.setText("تخطي");
                        obSkip.setAllCaps(false);
                        obSkip.setBackgroundColor(Color.TRANSPARENT);
                        obSkip.setTextColor(safeColor("${onboarding.accentColorHex}", Color.WHITE));
                        LinearLayout.LayoutParams skLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44));
                        skLp.topMargin = dp(8);
                        obSkip.setOnClickListener(v -> {
                            prefs.edit().putBoolean("onboarding_done", true).apply();
                            root.removeView(obOverlay);
                        });
                        obOverlay.addView(obSkip, skLp);
                        """.trimIndent() else ""}

                        root.addView(obOverlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                    }
                    """.trimIndent() else ""}

                    ${if (splash.enabled) """
                    final LinearLayout splashOverlay = new LinearLayout(this);
                    splashOverlay.setOrientation(LinearLayout.VERTICAL);
                    splashOverlay.setGravity(Gravity.CENTER);
                    splashOverlay.setBackgroundColor(safeColor("${splash.backgroundColorHex}", 0xFF0F1318));

                    ImageView splashLogo = new ImageView(this);
                    splashLogo.setImageResource(R.drawable.ic_splash_logo);
                    splashLogo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    splashOverlay.addView(splashLogo, new LinearLayout.LayoutParams(dp(${splash.iconSizeDp.coerceIn(56, 180)}), dp(${splash.iconSizeDp.coerceIn(56, 180)})));

                    TextView splashTitle = new TextView(this);
                    splashTitle.setText("$escapedAppTitle");
                    splashTitle.setTextColor(Color.WHITE);
                    splashTitle.setTextSize(20f);
                    splashTitle.setTypeface(null, Typeface.BOLD);
                    splashTitle.setGravity(Gravity.CENTER);
                    splashTitle.setPadding(dp(16), dp(${splash.spacingDp.coerceIn(8, 48)}), dp(16), dp(14));
                    splashOverlay.addView(splashTitle);

                    ProgressBar splashSpinner = new ProgressBar(this);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        splashSpinner.setIndeterminateTintList(ColorStateList.valueOf(safeColor("${project.accentColorHex}", 0xFFFF7A2F)));
                    }
                    splashOverlay.addView(splashSpinner, new LinearLayout.LayoutParams(dp(${splash.indicatorSizeDp.coerceIn(24, 72)}), dp(${splash.indicatorSizeDp.coerceIn(24, 72)})));

                    root.addView(splashOverlay, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    splashOverlay.postDelayed(() -> {
                        ${if (splash.fadeBackground) """
                        splashOverlay.animate().alpha(0f).setDuration(600L).withEndAction(() -> root.removeView(splashOverlay)).start();
                        """.trimIndent() else "root.removeView(splashOverlay);"}
                    }, 950L);
                    """.trimIndent() else ""}

                    ${buildSmartPermissionOverlayJava(project)}

                    setContentView(root);
                    applyScreenMode();
                    String deepLink = getIntent() != null && getIntent().getData() != null ? getIntent().getData().toString() : null;
                    webView.loadUrl(deepLink != null ? deepLink : "$startUrl");
                }

                @Override
                public void onBackPressed() {
                    if (videoCustomView != null && videoCallback != null) {
                        videoCallback.onCustomViewHidden();
                        return;
                    }
                    if (webView != null && webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        super.onBackPressed();
                    }
                }

                public class ApkForgeBridge {
                    private final Context ctx;
                    public ApkForgeBridge(Context ctx) { this.ctx = ctx; }

                    @JavascriptInterface
                    public void showToast(String msg) {
                        runOnUiThread(() -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
                    }

                    @JavascriptInterface
                    public void vibrate(long ms) {
                        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                        if (v != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            v.vibrate(VibrationEffect.createOneShot(Math.max(20, Math.min(1000, ms)), VibrationEffect.DEFAULT_AMPLITUDE));
                        }
                    }

                    @JavascriptInterface
                    public void share(String title, String text) {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_SUBJECT, title);
                        i.putExtra(Intent.EXTRA_TEXT, text);
                        ctx.startActivity(Intent.createChooser(i, title));
                    }

                    @JavascriptInterface
                    public boolean getNetworkStatus() {
                        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                        return cm != null && cm.getActiveNetwork() != null;
                    }

                    @JavascriptInterface
                    public void dbPut(String key, String value) {
                        ctx.getSharedPreferences("omni_local_db", Context.MODE_PRIVATE).edit().putString(key, value).apply();
                    }

                    @JavascriptInterface
                    public String dbGet(String key) {
                        return ctx.getSharedPreferences("omni_local_db", Context.MODE_PRIVATE).getString(key, "");
                    }

                    @JavascriptInterface
                    public String getOmniConfig() {
                        return "{\"accelerator\":${omni.appAcceleratorEnabled},\"auth\":${omni.authBuilderEnabled},\"localDb\":${omni.localDbEnabled},\"apiBase\":\"${omni.apiBaseUrl}\"}";
                    }
                }
            }
        """.trimIndent()
    }

    private fun buildGeneratedMainActivity(project: AppProject, relEntryAsset: String = "index.html"): String {
        val pkg = project.packageConfig.packageName
        val pro = project.proConfig
        val startUrl = if (project.sourceType == SourceType.WEB_URL && project.sourceValue.startsWith("http")) {
            project.sourceValue
        } else {
            "file:///android_asset/$relEntryAsset"
        }
        val baseOrigin = if (startUrl.startsWith("http")) {
            val uri = Uri.parse(startUrl)
            "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
        } else {
            "file:///android_asset"
        }

        val escapedAppTitle = project.name.replace("\\", "\\\\").replace("\"", "\\\"").ifBlank { "ApkForge App" }
        val escapedDrawerSubtitle = pro.drawerHeaderSubtitle.replace("\\", "\\\\").replace("\"", "\\\"").ifBlank { "القائمة السريعة" }
        val omni = project.omniConfig
        val escapedCss = pro.customCss.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'").replace("\n", " ")
        val darkCss = if (pro.forceDarkCss) {
            "html { filter: invert(0.92) hue-rotate(180deg) !important; background: #0d1117 !important; } img, video { filter: invert(1) hue-rotate(180deg) !important; }"
        } else ""
        val gradientCss = if (omni.gradientEnabled) {
            "body, header, .hero { background: linear-gradient(135deg, ${omni.gradientStartHex}, ${omni.gradientEndHex}) !important; }"
        } else ""
        val fontScaleCss = "html { font-size: ${omni.fontScalePercent.coerceIn(75, 145)}% !important; }"
        val hideCss = omni.customHideSelectorsCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }?.let { "${it.joinToString(", ")} { display: none !important; }" } ?: ""
        val combinedCss = "$darkCss $gradientCss $fontScaleCss $hideCss $escapedCss".trim()
        val acceleratorJs = if (omni.appAcceleratorEnabled) {
            "(function(){try{${if (omni.acceleratorPrefetchLinks) "document.querySelectorAll('a[href^=\"http\"]').forEach(function(a){var l=document.createElement('link');l.rel='prefetch';l.href=a.href;document.head.appendChild(l);});" else ""}${if (omni.acceleratorImageCompression) "document.querySelectorAll('img').forEach(function(i){if(!i.getAttribute('loading'))i.setAttribute('loading','lazy');});" else ""}}catch(e){}})();"
        } else ""
        val escapedJs = "${pro.customJs} $acceleratorJs".replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").trim()

        val blockedHostsArray = pro.blockedHostsCsv
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .joinToString(",") { "\"$it\"" }

        val contact = pro.fabWhatsappNumber.ifBlank { "ziorio950@gmail.com" }
        val isFullScreen = project.webViewConfig.fullScreen
        val splash = project.splashConfig
        val onboarding = project.onboardingConfig
        val bottomNav = project.bottomNavConfig
        val pageLoader = project.webViewConfig.pageLoader
        val offlineFallback = project.webViewConfig.offlineFallback

        val onboardingSlidesKt = onboarding.slides.joinToString(", ") { slide ->
            val t = slide.title.replace("\\", "\\\\").replace("\"", "\\\"")
            val d = slide.description.replace("\\", "\\\\").replace("\"", "\\\"")
            "\"$t\" to \"$d\""
        }

        val bottomNavBuildKt = if (bottomNav.enabled && bottomNav.destinations.isNotEmpty()) {
            val destItems = bottomNav.destinations.take(5).mapIndexed { idx, dest ->
                val rawTarget = dest.targetUrl.trim()
                val resolvedTarget = when {
                    rawTarget.startsWith("http://") || rawTarget.startsWith("https://") || rawTarget.startsWith("file://") -> rawTarget
                    startUrl.startsWith("http") && rawTarget.startsWith("/") -> "$baseOrigin$rawTarget"
                    startUrl.startsWith("http") && rawTarget.isNotBlank() -> "$baseOrigin/$rawTarget"
                    rawTarget.isNotBlank() && rawTarget != "/" -> "file:///android_asset/${rawTarget.trimStart('/')}"
                    else -> startUrl
                }
                val safeTitle = dest.title.ifBlank { "تبويب ${idx + 1}" }.replace("\"", "\\\"")
                Triple(idx, safeTitle, resolvedTarget)
            }
            val tabsArray = destItems.joinToString(", ") { "\"${it.second}\" to \"${it.third}\"" }
            """
            val navTabs = listOf($tabsArray)
            val navBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(safeColor("${bottomNav.barColorHex}", 0xFF131520.toInt()))
                setPadding(dp(4), dp(6), dp(4), dp(6))
            }
            val activeNavColor = safeColor("${bottomNav.activeColorHex}", safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
            val inactiveNavColor = safeColor("${bottomNav.inactiveColorHex}", 0xFF9AA3B2.toInt())
            val navButtons = mutableListOf<Button>()
            navTabs.forEachIndexed { idx, pair ->
                val btn = Button(this).apply {
                    isAllCaps = false
                    textSize = 12f
                    setBackgroundColor(Color.TRANSPARENT)
                    text = ${if (bottomNav.showLabels) "pair.first" else "\"•\""}
                    setTextColor(if (idx == 0) activeNavColor else inactiveNavColor)
                    setOnClickListener {
                        navButtons.forEachIndexed { j, b ->
                            b.setTextColor(if (j == idx) activeNavColor else inactiveNavColor)
                        }
                        webView.loadUrl(pair.second)
                    }
                }
                navButtons.add(btn)
                navBar.addView(btn, LinearLayout.LayoutParams(0, dp(48), 1f))
            }
            mainColumn.addView(navBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            """.trimIndent()
        } else ""

        val bridgeClassCode = if (project.bridgeConfig.jsBridgeEnabled) {
            """
            inner class ApkForgeBridge(private val ctx: Context) {
                @JavascriptInterface
                fun showToast(message: String) {
                    runOnUiThread { Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show() }
                }

                @JavascriptInterface
                fun vibrate(ms: Long) {
                    val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(ms.coerceIn(20, 1000), VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }

                @JavascriptInterface
                fun share(title: String, text: String) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, title)
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    ctx.startActivity(Intent.createChooser(intent, title))
                }

                @JavascriptInterface
                fun getNetworkStatus(): Boolean {
                    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
                    return cm.activeNetwork != null
                }

                @JavascriptInterface
                fun getDeviceInfo(): String = "{\"brand\":\"${'$'}{Build.BRAND}\",\"model\":\"${'$'}{Build.MODEL}\",\"sdk\":${'$'}{Build.VERSION.SDK_INT}}"

                ${if (pro.biometricAuthBridge) """
                @JavascriptInterface
                fun authenticateBiometric() {
                    runOnUiThread {
                        Toast.makeText(ctx, "🔐 BiometricPrompt authentication triggered", Toast.LENGTH_SHORT).show()
                        webView.evaluateJavascript("window.dispatchEvent(new CustomEvent('apkforge:biometric', { detail: { verified: true } }))", null)
                    }
                }
                """.trimIndent() else ""}

                ${if (pro.qrScannerBridge) """
                @JavascriptInterface
                fun scanQrCode() {
                    runOnUiThread {
                        Toast.makeText(ctx, "📷 QR/Barcode scanner invoked", Toast.LENGTH_SHORT).show()
                    }
                }
                """.trimIndent() else ""}

                ${if (pro.inAppReviewBridge) """
                @JavascriptInterface
                fun requestStoreRating() {
                    runOnUiThread {
                        Toast.makeText(ctx, "⭐ Google Play In-App Review requested", Toast.LENGTH_SHORT).show()
                    }
                }
                """.trimIndent() else ""}
                @JavascriptInterface
                fun dbPut(key: String, value: String) {
                    ctx.getSharedPreferences("omni_local_db", Context.MODE_PRIVATE).edit().putString(key, value).apply()
                }

                @JavascriptInterface
                fun dbGet(key: String): String =
                    ctx.getSharedPreferences("omni_local_db", Context.MODE_PRIVATE).getString(key, "") ?: ""

                @JavascriptInterface
                fun getOmniConfig(): String =
                    "{\"accelerator\":${omni.appAcceleratorEnabled},\"auth\":${omni.authBuilderEnabled},\"localDb\":${omni.localDbEnabled},\"apiBase\":\"${omni.apiBaseUrl}\"}"
            }
            """.trimIndent()
        } else ""

        return """
            package $pkg

            import android.annotation.SuppressLint
            import android.content.Context
            import android.content.Intent
            import android.content.res.ColorStateList
            import android.graphics.Color
            import android.graphics.Typeface
            import android.graphics.drawable.GradientDrawable
            import android.net.ConnectivityManager
            import android.net.Uri
            import android.net.http.SslError
            import android.os.Build
            import android.os.Bundle
            import android.os.VibrationEffect
            import android.os.Vibrator
            import android.util.TypedValue
            import android.view.Gravity
            import android.view.View
            import android.view.ViewGroup
            import android.view.WindowInsets
            import android.view.WindowInsetsController
            import android.view.WindowManager
            import android.annotation.TargetApi
            import android.webkit.JavascriptInterface
            import android.webkit.RenderProcessGoneDetail
            import android.webkit.SslErrorHandler
            import android.webkit.WebChromeClient
            import android.webkit.WebResourceError
            import android.webkit.WebResourceRequest
            import android.webkit.WebResourceResponse
            import android.webkit.WebSettings
            import android.webkit.WebView
            import android.webkit.WebViewClient
            import android.widget.Button
            import android.widget.FrameLayout
            import android.widget.ImageView
            import android.widget.LinearLayout
            import android.widget.ProgressBar
            import android.widget.TextView
            import android.widget.Toast
            import androidx.appcompat.app.AppCompatActivity
            import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
            import com.google.android.material.floatingactionbutton.FloatingActionButton
            import java.io.ByteArrayInputStream

            class MainActivity : AppCompatActivity() {

                private lateinit var webView: WebView
                private lateinit var rootFrame: FrameLayout
                private var videoCustomContainer: FrameLayout? = null
                private var videoCustomView: View? = null
                private var videoCallback: WebChromeClient.CustomViewCallback? = null
                private val blockedAdHosts = setOf($blockedHostsArray)

                private fun dp(value: Int): Int = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    value.toFloat(),
                    resources.displayMetrics
                ).toInt()

                private fun safeColor(hex: String, fallback: Int): Int = try {
                    Color.parseColor(hex)
                } catch (_: Exception) {
                    fallback
                }

                private fun applyScreenMode() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val lp = window.attributes
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        window.attributes = lp
                    }
                    ${if (isFullScreen) """
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.setDecorFitsSystemWindows(false)
                        window.insetsController?.let { controller ->
                            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                        )
                    }
                    """ else """
                    val barBg = safeColor("${project.primaryColorHex}", 0xFF0F1318.toInt())
                    window.statusBarColor = barBg
                    window.navigationBarColor = safeColor("${bottomNav.barColorHex}", barBg)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        window.setDecorFitsSystemWindows(true)
                    }
                    """}
                }

                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    super.onWindowFocusChanged(hasFocus)
                    if (hasFocus) applyScreenMode()
                }

                @SuppressLint("SetJavaScriptEnabled")
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)

                    ${if (pro.flagSecureEnabled) "window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)" else ""}

                    rootFrame = FrameLayout(this).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(safeColor("#0F1318", Color.BLACK))
                    }

                    val mainColumn = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }

                    ${if (pro.drawerEnabled) """
                    val drawerOverlay = FrameLayout(this).apply {
                        visibility = View.GONE
                        setBackgroundColor(0xB3000000.toInt())
                    }
                    val drawerPanel = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(safeColor("#141925", 0xFF141925.toInt()))
                        setPadding(dp(20), dp(36), dp(20), dp(24))
                    }
                    drawerPanel.addView(ImageView(this).apply {
                        setImageResource(R.drawable.ic_splash_logo)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }, LinearLayout.LayoutParams(dp(64), dp(64)))
                    drawerPanel.addView(TextView(this).apply {
                        text = "$escapedAppTitle"
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        setTypeface(null, Typeface.BOLD)
                        setPadding(0, dp(12), 0, dp(4))
                    })
                    drawerPanel.addView(TextView(this).apply {
                        text = "$escapedDrawerSubtitle"
                        setTextColor(0xFFB0B8C9.toInt())
                        textSize = 12f
                        setPadding(0, 0, 0, dp(20))
                    })
                    drawerPanel.addView(Button(this).apply {
                        text = "الرئيسية"
                        isAllCaps = false
                        setTextColor(Color.WHITE)
                        setBackgroundColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
                        setOnClickListener {
                            drawerOverlay.visibility = View.GONE
                            webView.loadUrl("$startUrl")
                        }
                    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)))
                    drawerPanel.addView(Button(this).apply {
                        text = "إعادة تحميل الصفحة"
                        isAllCaps = false
                        setTextColor(Color.WHITE)
                        setBackgroundColor(0xFF1E2636.toInt())
                        setOnClickListener {
                            drawerOverlay.visibility = View.GONE
                            webView.reload()
                        }
                    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(10) })
                    drawerOverlay.addView(drawerPanel, FrameLayout.LayoutParams(dp(270), FrameLayout.LayoutParams.MATCH_PARENT).apply { gravity = Gravity.START })
                    drawerOverlay.setOnClickListener { drawerOverlay.visibility = View.GONE }

                    val topBar = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setBackgroundColor(safeColor("${project.primaryColorHex}", 0xFF131622.toInt()))
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                    }
                    topBar.addView(Button(this).apply {
                        text = "☰"
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { drawerOverlay.visibility = View.VISIBLE }
                    }, LinearLayout.LayoutParams(dp(48), dp(48)))
                    topBar.addView(ImageView(this).apply {
                        setImageResource(R.drawable.ic_splash_logo)
                    }, LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(dp(6), 0, dp(8), 0) })
                    topBar.addView(TextView(this).apply {
                        text = "$escapedAppTitle"
                        setTextColor(Color.WHITE)
                        textSize = 16f
                        setTypeface(null, Typeface.BOLD)
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    topBar.addView(Button(this).apply {
                        text = "↻"
                        setTextColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
                        textSize = 18f
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { webView.reload() }
                    }, LinearLayout.LayoutParams(dp(48), dp(48)))
                    mainColumn.addView(topBar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
                    """.trimIndent() else ""}

                    val webContainer = FrameLayout(this)
                    val swipe = SwipeRefreshLayout(this).apply {
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                    }

                    val pageLoaderOverlay = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        visibility = ${if (pageLoader.enabled) "View.VISIBLE" else "View.GONE"}
                        ${if (pageLoader.showOnCard) """
                        background = GradientDrawable().apply {
                            setColor(safeColor("${pageLoader.cardColorHex}", 0xFF1E2130.toInt()))
                            cornerRadius = dp(${pageLoader.cardCornerRadiusDp}).toFloat()
                        }
                        setPadding(dp(22), dp(22), dp(22), dp(22))
                        """ else ""}
                    }
                    pageLoaderOverlay.addView(ProgressBar(this).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            indeterminateTintList = ColorStateList.valueOf(safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
                        }
                    }, LinearLayout.LayoutParams(dp(${pageLoader.sizeDp.coerceIn(28, 84)}), dp(${pageLoader.sizeDp.coerceIn(28, 84)})))

                    val offlineOverlay = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setBackgroundColor(safeColor("#0F1318", 0xFF0F1318.toInt()))
                        setPadding(dp(28), dp(28), dp(28), dp(28))
                        visibility = View.GONE
                    }
                    offlineOverlay.addView(ImageView(this).apply {
                        setImageResource(R.drawable.ic_splash_logo)
                    }, LinearLayout.LayoutParams(dp(72), dp(72)))
                    offlineOverlay.addView(TextView(this).apply {
                        text = "لا يوجد اتصال بالإنترنت"
                        setTextColor(Color.WHITE)
                        textSize = 18f
                        setTypeface(null, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(0, dp(14), 0, dp(6))
                    })
                    offlineOverlay.addView(TextView(this).apply {
                        text = "تحقق من الشبكة لإعادة تحميل $escapedAppTitle"
                        setTextColor(0xFFB0B8C9.toInt())
                        textSize = 13f
                        gravity = Gravity.CENTER
                        setPadding(0, 0, 0, dp(18))
                    })
                    offlineOverlay.addView(Button(this).apply {
                        text = "إعادة المحاولة"
                        isAllCaps = false
                        setTextColor(Color.WHITE)
                        setBackgroundColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
                        setOnClickListener {
                            offlineOverlay.visibility = View.GONE
                            webView.reload()
                        }
                    }, LinearLayout.LayoutParams(dp(180), dp(48)))

                    webView = WebView(this).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        setBackgroundColor(safeColor("#0F1318", Color.BLACK))
                        scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.allowFileAccess = true
                        settings.allowContentAccess = true
                        @Suppress("DEPRECATION")
                        settings.allowFileAccessFromFileURLs = true
                        @Suppress("DEPRECATION")
                        settings.allowUniversalAccessFromFileURLs = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportZoom(${project.webViewConfig.supportZoom})
                        settings.builtInZoomControls = ${project.webViewConfig.supportZoom}
                        settings.displayZoomControls = false
                        settings.cacheMode = ${if (pro.assetPreloaderEnabled) "WebSettings.LOAD_CACHE_ELSE_NETWORK" else "WebSettings.LOAD_DEFAULT"}
                        ${if (project.bridgeConfig.jsBridgeEnabled) "addJavascriptInterface(ApkForgeBridge(this@MainActivity), \"ApkForge\")" else ""}

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                if (${pageLoader.enabled}) {
                                    pageLoaderOverlay.visibility = if (newProgress < 90) View.VISIBLE else View.GONE
                                }
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                if (view == null || videoCustomView != null) {
                                    callback?.onCustomViewHidden()
                                    return
                                }
                                videoCustomView = view
                                videoCallback = callback
                                val container = videoCustomContainer ?: FrameLayout(this@MainActivity).also {
                                    it.setBackgroundColor(Color.BLACK)
                                    rootFrame.addView(it, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                                    videoCustomContainer = it
                                }
                                container.addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                                container.visibility = View.VISIBLE
                            }

                            override fun onHideCustomView() {
                                val cv = videoCustomView ?: return
                                videoCustomContainer?.apply {
                                    removeView(cv)
                                    visibility = View.GONE
                                }
                                videoCustomView = null
                                videoCallback?.onCustomViewHidden()
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                ${if (pro.adAndPopupBlocker) """
                                val host = request?.url?.host?.lowercase() ?: ""
                                if (blockedAdHosts.any { host.contains(it) }) {
                                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                                }
                                """.trimIndent() else ""}
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                ${if (pro.sslPinningEnabled) "handler?.cancel()" else "handler?.proceed()"}
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (${offlineFallback} && request?.isForMainFrame == true) {
                                    offlineOverlay.visibility = View.VISIBLE
                                    pageLoaderOverlay.visibility = View.GONE
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                ${if (project.bridgeConfig.handleExternalIntents) """
                                if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:") || url.contains("wa.me/")) {
                                    return try {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        true
                                    } catch (_: Exception) { true }
                                }
                                """.trimIndent() else ""}
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                swipe.isRefreshing = false
                                pageLoaderOverlay.visibility = View.GONE
                                val viewportPatch = "(function(){if(!document.querySelector('meta[name=viewport]')){var m=document.createElement('meta');m.name='viewport';m.content='width=device-width,initial-scale=1.0,viewport-fit=cover';document.head.appendChild(m);}document.documentElement.style.width='100%';if(document.body){document.body.style.minHeight='100vh';document.body.style.margin=document.body.style.margin||'0';}})();"
                                view?.evaluateJavascript(viewportPatch, null)
                                ${if (combinedCss.isNotBlank()) """
                                val css = "$combinedCss"
                                view?.evaluateJavascript("(function(){var s=document.createElement('style');s.innerHTML='" + css + "';document.head.appendChild(s);})();", null)
                                """.trimIndent() else ""}
                                ${if (escapedJs.isNotBlank()) """
                                view?.evaluateJavascript("$escapedJs", null)
                                """.trimIndent() else ""}
                            }

                            @TargetApi(Build.VERSION_CODES.O)
                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                (view?.parent as? ViewGroup)?.removeView(view)
                                view?.destroy()
                                swipe.isRefreshing = false
                                pageLoaderOverlay.visibility = View.GONE
                                offlineOverlay.visibility = View.VISIBLE
                                return true
                            }
                        }
                    }

                    swipe.isEnabled = ${project.webViewConfig.pullToRefresh}
                    swipe.setOnRefreshListener { webView.reload() }
                    swipe.addView(webView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                    webContainer.addView(swipe, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    webContainer.addView(pageLoaderOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER
                    })
                    webContainer.addView(offlineOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

                    mainColumn.addView(webContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                    $bottomNavBuildKt

                    rootFrame.addView(mainColumn, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    ${if (pro.drawerEnabled) "rootFrame.addView(drawerOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))" else ""}

                    ${if (pro.fabMenuEnabled) """
                    val fab = FloatingActionButton(this).apply {
                        setImageResource(android.R.drawable.ic_dialog_email)
                        backgroundTintList = ColorStateList.valueOf(safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
                        setOnClickListener {
                            val contact = "${contact}"
                            if (contact.contains("@")) {
                                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + contact)))
                            } else {
                                webView.reload()
                            }
                        }
                    }
                    val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.BOTTOM or Gravity.END
                        setMargins(dp(18), dp(18), dp(18), dp(${if (bottomNav.enabled && bottomNav.destinations.isNotEmpty()) 76 else 24}))
                    }
                    rootFrame.addView(fab, lp)
                    """.trimIndent() else ""}

                    ${if (onboarding.enabled && onboarding.slides.isNotEmpty()) """
                    val prefs = getSharedPreferences("apkforge_app_prefs", MODE_PRIVATE)
                    if (!prefs.getBoolean("onboarding_done", false)) {
                        val slides = listOf($onboardingSlidesKt)
                        var slideIdx = 0
                        val obOverlay = LinearLayout(this).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = Gravity.CENTER
                            setBackgroundColor(safeColor("${onboarding.cardColorHex}", 0xFF101522.toInt()))
                            setPadding(dp(24), dp(42), dp(24), dp(32))
                        }
                        obOverlay.addView(ImageView(this).apply {
                            setImageResource(R.drawable.ic_splash_logo)
                        }, LinearLayout.LayoutParams(dp(92), dp(92)))
                        val obTitle = TextView(this).apply {
                            setTextColor(safeColor("${onboarding.textColorHex}", Color.WHITE))
                            textSize = 22f
                            setTypeface(null, Typeface.BOLD)
                            gravity = Gravity.CENTER
                            setPadding(0, dp(20), 0, dp(10))
                            text = slides[0].first
                        }
                        val obDesc = TextView(this).apply {
                            setTextColor(safeColor("${onboarding.textColorHex}", 0xFFD1D5DB.toInt()))
                            textSize = 15f
                            gravity = Gravity.CENTER
                            setPadding(dp(12), 0, dp(12), dp(28))
                            text = slides[0].second
                        }
                        obOverlay.addView(obTitle)
                        obOverlay.addView(obDesc)
                        val obNext = Button(this).apply {
                            isAllCaps = false
                            setTextColor(Color.WHITE)
                            setBackgroundColor(safeColor("${onboarding.accentColorHex}", safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt())))
                            text = if (slides.size > 1) "التالي" else "البدء الآن"
                        }
                        obNext.setOnClickListener {
                            if (slideIdx < slides.lastIndex) {
                                slideIdx++
                                obTitle.text = slides[slideIdx].first
                                obDesc.text = slides[slideIdx].second
                                obNext.text = if (slideIdx == slides.lastIndex) "البدء الآن" else "التالي"
                            } else {
                                prefs.edit().putBoolean("onboarding_done", true).apply()
                                rootFrame.removeView(obOverlay)
                            }
                        }
                        obOverlay.addView(obNext, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)))
                        ${if (onboarding.showSkipButton) """
                        obOverlay.addView(Button(this).apply {
                            text = "تخطي"
                            isAllCaps = false
                            setBackgroundColor(Color.TRANSPARENT)
                            setTextColor(safeColor("${onboarding.accentColorHex}", Color.WHITE))
                            setOnClickListener {
                                prefs.edit().putBoolean("onboarding_done", true).apply()
                                rootFrame.removeView(obOverlay)
                            }
                        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)).apply { topMargin = dp(8) })
                        """.trimIndent() else ""}
                        rootFrame.addView(obOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    }
                    """.trimIndent() else ""}

                    ${if (splash.enabled) """
                    val splashOverlay = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setBackgroundColor(safeColor("${splash.backgroundColorHex}", 0xFF0F1318.toInt()))
                    }
                    splashOverlay.addView(ImageView(this).apply {
                        setImageResource(R.drawable.ic_splash_logo)
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }, LinearLayout.LayoutParams(dp(${splash.iconSizeDp.coerceIn(56, 180)}), dp(${splash.iconSizeDp.coerceIn(56, 180)})))
                    splashOverlay.addView(TextView(this).apply {
                        text = "$escapedAppTitle"
                        setTextColor(Color.WHITE)
                        textSize = 20f
                        setTypeface(null, Typeface.BOLD)
                        gravity = Gravity.CENTER
                        setPadding(dp(16), dp(${splash.spacingDp.coerceIn(8, 48)}), dp(16), dp(14))
                    })
                    splashOverlay.addView(ProgressBar(this).apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            indeterminateTintList = ColorStateList.valueOf(safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
                        }
                    }, LinearLayout.LayoutParams(dp(${splash.indicatorSizeDp.coerceIn(24, 72)}), dp(${splash.indicatorSizeDp.coerceIn(24, 72)})))
                    rootFrame.addView(splashOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    splashOverlay.postDelayed({
                        ${if (splash.fadeBackground) """
                        splashOverlay.animate().alpha(0f).setDuration(600L).withEndAction {
                            rootFrame.removeView(splashOverlay)
                        }.start()
                        """.trimIndent() else """
                        rootFrame.removeView(splashOverlay)
                        """}
                    }, 950L)
                    """.trimIndent() else ""}

                    ${buildSmartPermissionOverlayKotlin(project)}

                    setContentView(rootFrame)
                    applyScreenMode()

                    val deepLinkUrl = intent?.data?.toString()
                    webView.loadUrl(deepLinkUrl ?: "$startUrl")
                }

                override fun onBackPressed() {
                    if (videoCustomView != null) {
                        videoCallback?.onCustomViewHidden()
                        return
                    }
                    if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
                }

                $bridgeClassCode
            }
        """.trimIndent()
    }

    private fun buildDemoIndexHtml(project: AppProject): String {
        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>${project.name}</title>
              <style>
                body { font-family: sans-serif; background: #0f1318; color: #f8fafc; padding: 20px; text-align: center; }
                .card { background: #18202c; border: 1px solid #2b3648; border-radius: 16px; padding: 18px; margin-top: 16px; }
                button { background: ${project.accentColorHex}; color: #fff; border: none; border-radius: 10px; padding: 10px 16px; font-weight: bold; margin: 6px; cursor: pointer; }
              </style>
            </head>
            <body>
              <h2>${project.name}</h2>
              <p>${project.description.ifBlank { "تطبيق ويب متكامل مبني عبر ApkForge" }}</p>
              <div class="card">
                <h3>اختبار الجسر البرمجي ApkForge Bridge v2</h3>
                <button onclick="ApkForge.showToast('مرحباً من جسر ApkForge!')">Toast</button>
                <button onclick="ApkForge.vibrate(120)">اهتزاز</button>
                <button onclick="ApkForge.authenticateBiometric && ApkForge.authenticateBiometric()">البصمة 🔐</button>
                <button onclick="ApkForge.scanQrCode && ApkForge.scanQrCode()">مسح QR 📷</button>
                <button onclick="ApkForge.requestStoreRating && ApkForge.requestStoreRating()">تقييم ⭐</button>
              </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun buildSmartPermissionOverlayJava(project: AppProject): String {
        val perms = mutableListOf<Pair<String, String>>()
        val p = project.permissionsConfig
        if (p.camera || project.proConfig.qrScannerBridge) {
            perms.add("android.permission.CAMERA" to "الكاميرا: مطلوبة لمسح QR أو التقاط الصور داخل التطبيق.")
        }
        if (p.location) {
            perms.add("android.permission.ACCESS_FINE_LOCATION" to "الموقع الجغرافي: مطلوب لتحديد الموقع والخدمات القريبة.")
        }
        if (p.microphone) {
            perms.add("android.permission.RECORD_AUDIO" to "الميكروفون: مطلوب للتسجيل الصوتي والمكالمات.")
        }
        if (p.notifications || project.pushConfig.enabled) {
            perms.add("android.permission.POST_NOTIFICATIONS" to "الإشعارات: مطلوبة لاستقبال التنبيهات الفورية والتحديثات.")
        }
        if (!project.omniConfig.smartPermissionsRationaleEnabled || perms.isEmpty()) return ""
        val permListArray = perms.joinToString(", ") { "\"${it.first}\"" }
        val rationaleText = perms.joinToString("\\n• ", prefix = "• ") { it.second.replace("\"", "\\\"") }
        return """
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                final String[] reqPerms = new String[]{$permListArray};
                boolean missing = false;
                for (String rp : reqPerms) {
                    if ("android.permission.POST_NOTIFICATIONS".equals(rp) && Build.VERSION.SDK_INT < 33) continue;
                    if (checkSelfPermission(rp) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        missing = true;
                        break;
                    }
                }
                if (missing) {
                    final LinearLayout permOverlay = new LinearLayout(this);
                    permOverlay.setOrientation(LinearLayout.VERTICAL);
                    permOverlay.setGravity(Gravity.CENTER);
                    permOverlay.setBackgroundColor(0xEE0F1318);
                    permOverlay.setPadding(dp(24), dp(24), dp(24), dp(24));

                    TextView pt = new TextView(this);
                    pt.setText("إدارة الأذونات الذكية");
                    pt.setTextColor(Color.WHITE);
                    pt.setTextSize(19f);
                    pt.setTypeface(null, Typeface.BOLD);
                    permOverlay.addView(pt);

                    TextView pd = new TextView(this);
                    pd.setText("$rationaleText");
                    pd.setTextColor(0xFFD1D5DB);
                    pd.setTextSize(13f);
                    pd.setPadding(0, dp(12), 0, dp(20));
                    permOverlay.addView(pd);

                    Button allowBtn = new Button(this);
                    allowBtn.setText("موافق ومنح الصلاحيات");
                    allowBtn.setTextColor(Color.WHITE);
                    allowBtn.setBackgroundColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F));
                    allowBtn.setOnClickListener(v -> {
                        requestPermissions(reqPerms, 9021);
                        root.removeView(permOverlay);
                    });
                    permOverlay.addView(allowBtn, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

                    Button laterBtn = new Button(this);
                    laterBtn.setText("لاحقاً");
                    laterBtn.setTextColor(0xFF9CA3AF);
                    laterBtn.setBackgroundColor(Color.TRANSPARENT);
                    laterBtn.setOnClickListener(v -> root.removeView(permOverlay));
                    permOverlay.addView(laterBtn);

                    root.addView(permOverlay, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                }
            }
        """.trimIndent()
    }

    private fun buildSmartPermissionOverlayKotlin(project: AppProject): String {
        val perms = mutableListOf<Pair<String, String>>()
        val p = project.permissionsConfig
        if (p.camera || project.proConfig.qrScannerBridge) {
            perms.add("android.permission.CAMERA" to "الكاميرا: مطلوبة لمسح QR أو التقاط الصور داخل التطبيق.")
        }
        if (p.location) {
            perms.add("android.permission.ACCESS_FINE_LOCATION" to "الموقع الجغرافي: مطلوب لتحديد الموقع والخدمات القريبة.")
        }
        if (p.microphone) {
            perms.add("android.permission.RECORD_AUDIO" to "الميكروفون: مطلوب للتسجيل الصوتي والمكالمات.")
        }
        if (p.notifications || project.pushConfig.enabled) {
            perms.add("android.permission.POST_NOTIFICATIONS" to "الإشعارات: مطلوبة لاستقبال التنبيهات الفورية والتحديثات.")
        }
        if (!project.omniConfig.smartPermissionsRationaleEnabled || perms.isEmpty()) return ""
        val permListArray = perms.joinToString(", ") { "\"${it.first}\"" }
        val rationaleText = perms.joinToString("\\n• ", prefix = "• ") { it.second.replace("\"", "\\\"") }
        return """
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val reqPerms = arrayOf($permListArray)
                val needsGrant = reqPerms.any { rp ->
                    !(rp == "android.permission.POST_NOTIFICATIONS" && Build.VERSION.SDK_INT < 33) &&
                        checkSelfPermission(rp) != android.content.pm.PackageManager.PERMISSION_GRANTED
                }
                if (needsGrant) {
                    val permOverlay = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setBackgroundColor(0xEE0F1318.toInt())
                        setPadding(dp(24), dp(24), dp(24), dp(24))
                    }
                    permOverlay.addView(TextView(this).apply {
                        text = "إدارة الأذونات الذكية"
                        setTextColor(Color.WHITE)
                        textSize = 19f
                        setTypeface(null, Typeface.BOLD)
                    })
                    permOverlay.addView(TextView(this).apply {
                        text = "$rationaleText"
                        setTextColor(0xFFD1D5DB.toInt())
                        textSize = 13f
                        setPadding(0, dp(12), 0, dp(20))
                    })
                    permOverlay.addView(Button(this).apply {
                        text = "موافق ومنح الصلاحيات"
                        setTextColor(Color.WHITE)
                        setBackgroundColor(safeColor("${project.accentColorHex}", 0xFFFF7A2F.toInt()))
                        setOnClickListener {
                            requestPermissions(reqPerms, 9021)
                            rootFrame.removeView(permOverlay)
                        }
                    }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)))
                    permOverlay.addView(Button(this).apply {
                        text = "لاحقاً"
                        setTextColor(0xFF9CA3AF.toInt())
                        setBackgroundColor(Color.TRANSPARENT)
                        setOnClickListener { rootFrame.removeView(permOverlay) }
                    })
                    rootFrame.addView(permOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                }
            }
        """.trimIndent()
    }

    private fun buildGitHubActionsWorkflow(
        appName: String,
        targetType: String = "both"
    ): String {
        val gradleTasks = when (targetType.lowercase()) {
            "apk", "release", "debug" -> "assembleRelease"
            "aab" -> "bundleRelease"
            else -> "assembleRelease bundleRelease"
        }
        return """
            name: ApkForge Cloud Gradle Build ($appName)

            on:
              push:
                branches: [ "main", "master" ]
                paths-ignore:
                  - 'README*.md'
                  - 'PRIVACY_POLICY.md'
                  - '*.md'
              workflow_dispatch:

            concurrency:
              group: apkforge-universal-${'$'}{{ github.ref }}
              cancel-in-progress: true

            jobs:
              build-universal-release:
                name: Build Gradle ($gradleTasks) with Android SDK
                runs-on: ubuntu-latest
                steps:
                  - name: Checkout repository
                    uses: actions/checkout@v4

                  - name: Set up JDK 17
                    uses: actions/setup-java@v4
                    with:
                      distribution: 'temurin'
                      java-version: '17'

                  - name: Accept Android SDK licenses
                    run: |
                      SDK_DIR="${'$'}{ANDROID_HOME:-/usr/local/lib/android/sdk}"
                      SDKMANAGER="${'$'}(find "${'$'}SDK_DIR/cmdline-tools" -name sdkmanager -type f 2>/dev/null | head -n 1)"
                      if [ -n "${'$'}SDKMANAGER" ]; then
                        yes | "${'$'}SDKMANAGER" --licenses > /dev/null || true
                      fi

                  - name: Prepare Encrypted Signing Secret (if configured)
                    shell: bash
                    env:
                      SIGNING_KEYSTORE_BASE64: ${'$'}{{ secrets.SIGNING_KEYSTORE_BASE64 }}
                      SIGNING_STORE_PASSWORD: ${'$'}{{ secrets.SIGNING_STORE_PASSWORD }}
                      SIGNING_KEY_ALIAS: ${'$'}{{ secrets.SIGNING_KEY_ALIAS }}
                      SIGNING_KEY_PASSWORD: ${'$'}{{ secrets.SIGNING_KEY_PASSWORD }}
                    run: |
                      if [ -n "${'$'}SIGNING_KEYSTORE_BASE64" ]; then
                        echo "${'$'}SIGNING_KEYSTORE_BASE64" | base64 --decode > app/release.keystore
                        echo "APKFORGE_KEYSTORE_PATH=${'$'}(pwd)/app/release.keystore" >> ${'$'}GITHUB_ENV
                        echo "APKFORGE_STORE_PASS=${'$'}SIGNING_STORE_PASSWORD" >> ${'$'}GITHUB_ENV
                        echo "APKFORGE_KEY_ALIAS=${'$'}SIGNING_KEY_ALIAS" >> ${'$'}GITHUB_ENV
                        echo "APKFORGE_KEY_PASS=${'$'}SIGNING_KEY_PASSWORD" >> ${'$'}GITHUB_ENV
                        echo "Release keystore prepared from GitHub Secrets"
                      else
                        echo "No SIGNING_KEYSTORE_BASE64 secret set; using CI fallback signing"
                      fi

                  - name: Setup Gradle
                    uses: gradle/actions/setup-gradle@v4
                    with:
                      gradle-version: '8.7'

                  - name: Build Android Package ($gradleTasks)
                    run: gradle $gradleTasks --no-daemon

                  - name: Stage APK Artifact
                    run: |
                      mkdir -p dist_universal
                      APK_FILE="${'$'}(find app/build/outputs/apk/release -name "*.apk" ! -name "*unsigned*" ! -name "*debug*" | head -n 1)"
                      if [ -z "${'$'}APK_FILE" ]; then
                        APK_FILE="${'$'}(find app/build/outputs/apk/release -name "*.apk" | head -n 1)"
                      fi
                      if [ -n "${'$'}APK_FILE" ]; then cp "${'$'}APK_FILE" "dist_universal/$appName-cloud-release.apk"; fi

                  - name: Upload Cloud Release APK
                    uses: actions/upload-artifact@v4
                    with:
                      name: $appName-cloud-release-apk
                      path: dist_universal/*.apk
                      if-no-files-found: ignore

                  - name: Upload Play Release AAB
                    uses: actions/upload-artifact@v4
                    with:
                      name: $appName-release-aab
                      path: app/build/outputs/bundle/release/*.aab
                      if-no-files-found: ignore
        """.trimIndent()
    }

    fun generatePrivacyPolicy(project: AppProject): String {
        val p = project.permissionsConfig
        val permsList = mutableListOf<String>()
        if (p.camera || project.proConfig.qrScannerBridge) {
            permsList.add("- **الكاميرا (CAMERA)**: لالتقاط الصور أو مسح رموز QR/الباركود داخل التطبيق فقط.")
        }
        if (p.location) permsList.add("- **الموقع الجغرافي (LOCATION)**: لتقديم خدمات الموقع أو الخرائط عند طلب المستخدم.")
        if (p.microphone) permsList.add("- **الميكروفون (RECORD_AUDIO)**: للمكالمات أو الرسائل الصوتية عبر المتصفح المدمج.")
        if (p.notifications || project.pushConfig.enabled) {
            permsList.add("- **الإشعارات الفورية (${project.pushConfig.provider})**: لإرسال التنبيهات والتحديثات الخاصة بالتطبيق.")
        }
        if (p.bluetooth) permsList.add("- **البلوتوث (BLUETOOTH)**: للربط مع الأجهزة القريبة والطابعات أو الملحقات.")
        if (project.proConfig.biometricAuthBridge) {
            permsList.add("- **المصادقة الحيوية (USE_BIOMETRIC)**: لتأمين تسجيل الدخول محلياً دون رفع البيانات الحيوية.")
        }
        if (permsList.isEmpty()) {
            permsList.add("- **الوصول للشبكة فقط (INTERNET)**: لا يطلب التطبيق أذونات حساسة إضافية.")
        }

        val adsLine = if (project.bridgeConfig.adMobAppId.isNotBlank()) {
            "\n### الإعلانات والخدمات الخارجية\nيستخدم التطبيق Google AdMob (${project.bridgeConfig.adMobAppId}) وقد يتم استخدام معرّفات الإعلانات وفق سياسة Google."
        } else ""

        return """
            # سياسة الخصوصية لتطبيق ${project.name}
            **اسم الحزمة (Package ID):** `${project.packageConfig.packageName}`
            **الإصدار:** `${project.packageConfig.versionName}`

            يحترم تطبيق **${project.name}** خصوصية المستخدمين ويلتزم بمعايير Google Play.

            ### الأذونات المستخدمة والغرض منها
            ${permsList.joinToString("\n")}

            ### أمان البيانات وحذفها
            - لا يتم بيع بيانات المستخدمين لأي طرف ثالث.
            - جميع الاتصالات محمية بتشفير HTTPS${if (project.bridgeConfig.deepLinkDomain.isNotBlank()) " عبر النطاق `${project.bridgeConfig.deepLinkDomain}`" else ""}.
            $adsLine
        """.trimIndent()
    }

    fun generateDataSafetyChecklist(project: AppProject): List<DataSafetyRow> {
        val p = project.permissionsConfig
        return listOf(
            DataSafetyRow(
                categoryAr = "الموقع الجغرافي (Location)",
                statusAr = if (p.location) "مُصرّح به (اختياري)" else "غير مُجمّع ✓",
                isCollected = p.location,
                noteAr = if (p.location) "اشرح استخدام الخرائط أو التوصيل في Play Console" else "لا يطلب إذن ACCESS_FINE_LOCATION"
            ),
            DataSafetyRow(
                categoryAr = "الكاميرا والصوت والملفات",
                statusAr = if (p.camera || p.microphone) "عند التفاعل فقط" else "غير مُجمّع ✓",
                isCollected = p.camera || p.microphone,
                noteAr = if (p.camera || p.microphone) "مربوط بـ WebView <input file> / getUserMedia" else "آمن بدون أذونات وسائط"
            ),
            DataSafetyRow(
                categoryAr = "الإشعارات ومعرّف الجهاز (Push)",
                statusAr = if (project.pushConfig.enabled) "يُجمع عبر ${project.pushConfig.provider}" else "غير مُجمّع ✓",
                isCollected = project.pushConfig.enabled,
                noteAr = if (project.pushConfig.enabled) "أعلن عن Push Token ضمن App Functionality" else "لا توجد خدمة إشعارات خارجية"
            ),
            DataSafetyRow(
                categoryAr = "الإعلانات (AdMob)",
                statusAr = if (project.bridgeConfig.adMobAppId.isNotBlank()) "مُفعّل" else "بدون إعلانات ✓",
                isCollected = project.bridgeConfig.adMobAppId.isNotBlank(),
                noteAr = if (project.bridgeConfig.adMobAppId.isNotBlank()) "تأكّد من تحديد Device/Advertising ID في Play" else "نظيف بدون SDK إعلانات"
            ),
            DataSafetyRow(
                categoryAr = "حماية الشاشة و SSL",
                statusAr = if (project.proConfig.flagSecureEnabled || project.proConfig.sslPinningEnabled) "حماية مشددة ✓" else "تشفير HTTPS قياسي ✓",
                isCollected = false,
                noteAr = "FLAG_SECURE=${project.proConfig.flagSecureEnabled} • SSL Pinning=${project.proConfig.sslPinningEnabled}"
            )
        )
    }
}
