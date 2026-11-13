package com.example.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
        options: BuildOptimizationOptions = BuildOptimizationOptions()
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

        val splitsBlock = if (options.abiSplit) {
            """
                splits {
                    abi {
                        isEnable = true
                        reset()
                        include("armeabi-v7a", "arm64-v8a")
                        isUniversalApk = true
                    }
                }
            """.trimIndent()
        } else ""

        val biometricDep = if (project.proConfig.biometricAuthBridge) {
            """    implementation("androidx.biometric:biometric:1.1.0")"""
        } else ""
        val playReviewDep = if (project.proConfig.inAppReviewBridge) {
            """    implementation("com.google.android.play:review-ktx:2.0.1")"""
        } else ""

        entries["app/build.gradle.kts"] = """
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

                buildTypes {
                    release {
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

        entries["app/proguard-rules.pro"] = """
            -keepclassmembers class * {
                @android.webkit.JavascriptInterface <methods>;
            }
        """.trimIndent()

        val manifestXml = buildGeneratedManifest(project)
        entries["app/src/main/AndroidManifest.xml"] = manifestXml
        entries["termux_standalone/AndroidManifest.xml"] = manifestXml.replace(
            "@style/Theme.MaterialComponents.DayNight.NoActionBar",
            "@android:style/Theme.DeviceDefault.NoActionBar"
        )
        entries["app/src/main/java/$pkgPath/MainActivity.kt"] = buildGeneratedMainActivity(project, relEntryAsset)
        entries["termux_standalone/src/$pkgPath/MainActivity.java"] = buildStandaloneTermuxJavaActivity(project, relEntryAsset)
        entries["app/src/main/res/values/strings.xml"] = """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <string name="app_name">${project.name.replace("&", "&amp;")}</string>
            </resources>
        """.trimIndent()

        // Adaptive Icon XMLs
        entries["app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"] = iconPack.adaptiveXml
        entries["app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"] = iconPack.adaptiveXml
        entries["app/src/main/res/drawable/ic_launcher_background.xml"] = iconPack.backgroundDrawableXml
        entries["app/src/main/res/drawable/ic_launcher_foreground.xml"] = iconPack.foregroundVectorXml

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

        if (project.bridgeConfig.gitHubCiEnabled) {
            entries[".github/workflows/apkforge-build.yml"] = buildGitHubActionsWorkflow(safeName)
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

    fun openOrInstallFile(filePath: String, buildType: String): Boolean {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return false
        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val isApk = file.extension.equals("apk", ignoreCase = true) ||
                (buildType.equals("APK", ignoreCase = true) && file.name.endsWith(".apk", ignoreCase = true))
            val mime = when {
                isApk -> "application/vnd.android.package-archive"
                file.extension.equals("zip", ignoreCase = true) -> "application/zip"
                else -> "application/octet-stream"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, if (isApk) "تثبيت الحزمة APK" else "فتح الحزمة: ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (_: Exception) {
            shareFileWithChooser(file, "*/*", "مشاركة / فتح ${file.name}")
            true
        }
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
        }

        val deepLinkFilter = if (project.bridgeConfig.deepLinkDomain.isNotBlank()) {
            val host = project.bridgeConfig.deepLinkDomain.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
            """
            <intent-filter android:autoVerify="true">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" android:host="$host" />
            </intent-filter>
            """.trimIndent()
        } else ""

        val adMobMeta = if (project.bridgeConfig.adMobAppId.isNotBlank()) {
            """        <meta-data android:name="com.google.android.gms.ads.APPLICATION_ID" android:value="${project.bridgeConfig.adMobAppId}" />"""
        } else ""

        val pushMeta = if (project.pushConfig.enabled && project.pushConfig.appId.isNotBlank()) {
            """        <meta-data android:name="onesignal_app_id" android:value="${project.pushConfig.appId}" />"""
        } else ""

        return """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="${project.packageConfig.packageName}">
            $perms
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:roundIcon="@mipmap/ic_launcher_round"
                    android:label="@string/app_name"
                    android:usesCleartextTraffic="true"
                    android:supportsRtl="true"
                    android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar">
            $pushMeta
            $adMobMeta
                    <activity
                        android:name=".MainActivity"
                        android:exported="true"
                        android:configChanges="orientation|screenSize|keyboardHidden">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                        $deepLinkFilter
                    </activity>
                </application>
            </manifest>
        """.trimIndent()
    }

    /**
     * Activity نقية بـ Android SDK فقط (بدون androidx) ليتمكن Termux من ترجمتها مباشرة بـ aapt2 + javac + d8 + apksigner
     */
    fun buildStandaloneTermuxJavaActivity(project: AppProject, relEntryAsset: String = "index.html"): String {
        val pkg = project.packageConfig.packageName
        val pro = project.proConfig
        val startUrl = if (project.sourceType == SourceType.WEB_URL && project.sourceValue.startsWith("http")) {
            project.sourceValue
        } else {
            "file:///android_asset/$relEntryAsset"
        }
        val escapedCss = pro.customCss.replace("\"", "\\\"").replace("\n", " ")
        val darkCss = if (pro.forceDarkCss) {
            "html { filter: invert(0.92) hue-rotate(180deg) !important; background: #0d1117 !important; } img, video { filter: invert(1) hue-rotate(180deg) !important; }"
        } else ""
        val combinedCss = "$darkCss $escapedCss".trim()
        val escapedJs = pro.customJs.replace("\"", "\\\"").replace("\n", " ")
        val blockedHostsArray = pro.blockedHostsCsv
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .joinToString(",") { "\"$it\"" }

        val contact = pro.fabWhatsappNumber.ifBlank { "ziorio950@gmail.com" }

        return """
            package $pkg;

            import android.annotation.SuppressLint;
            import android.app.Activity;
            import android.content.Context;
            import android.content.Intent;
            import android.graphics.Color;
            import android.net.ConnectivityManager;
            import android.net.Uri;
            import android.net.http.SslError;
            import android.os.Build;
            import android.os.Bundle;
            import android.os.VibrationEffect;
            import android.os.Vibrator;
            import android.view.Gravity;
            import android.view.View;
            import android.view.Window;
            import android.view.WindowManager;
            import android.webkit.JavascriptInterface;
            import android.webkit.SslErrorHandler;
            import android.webkit.WebResourceRequest;
            import android.webkit.WebResourceResponse;
            import android.webkit.WebSettings;
            import android.webkit.WebView;
            import android.webkit.WebViewClient;
            import android.widget.Button;
            import android.widget.FrameLayout;
            import android.widget.LinearLayout;
            import android.widget.Toast;
            import java.io.ByteArrayInputStream;

            public class MainActivity extends Activity {
                private WebView webView;
                private final String[] blockedHosts = new String[]{$blockedHostsArray};

                @SuppressLint("SetJavaScriptEnabled")
                @Override
                protected void onCreate(Bundle savedInstanceState) {
                    super.onCreate(savedInstanceState);
                    requestWindowFeature(Window.FEATURE_NO_TITLE);
                    ${if (pro.flagSecureEnabled) "getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);" else ""}

                    FrameLayout root = new FrameLayout(this);
                    LinearLayout column = new LinearLayout(this);
                    column.setOrientation(LinearLayout.VERTICAL);

                    webView = new WebView(this);
                    WebSettings ws = webView.getSettings();
                    ws.setJavaScriptEnabled(true);
                    ws.setDomStorageEnabled(true);
                    ws.setAllowFileAccess(true);
                    ws.setAllowFileAccessFromFileURLs(true);
                    ws.setAllowUniversalAccessFromFileURLs(true);
                    ws.setSupportZoom(${project.webViewConfig.supportZoom});
                    ws.setCacheMode(${if (pro.assetPreloaderEnabled) "WebSettings.LOAD_CACHE_ELSE_NETWORK" else "WebSettings.LOAD_DEFAULT"});
                    ${if (project.bridgeConfig.jsBridgeEnabled) "webView.addJavascriptInterface(new ApkForgeBridge(this), \"ApkForge\");" else ""}

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
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            String url = request != null && request.getUrl() != null ? request.getUrl().toString() : "";
                            if (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:")) {
                                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); } catch (Exception ignored) {}
                                return true;
                            }
                            return false;
                        }

                        @Override
                        public void onPageFinished(WebView view, String url) {
                            ${if (combinedCss.isNotBlank()) "view.evaluateJavascript(\"(function(){var s=document.createElement('style');s.innerHTML='$combinedCss';document.head.appendChild(s);})();\", null);" else ""}
                            ${if (escapedJs.isNotBlank()) "view.evaluateJavascript(\"$escapedJs\", null);" else ""}
                        }
                    });

                    LinearLayout.LayoutParams wvLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
                    );
                    column.addView(webView, wvLp);

                    ${if (project.bottomNavConfig.enabled && project.bottomNavConfig.destinations.isNotEmpty()) {
                        val buttonsCode = project.bottomNavConfig.destinations.take(4).joinToString("\n") { dest ->
                            val target = if (dest.targetUrl.startsWith("http")) dest.targetUrl else startUrl
                            """
                            Button btn_${dest.id.filter { it.isLetterOrDigit() }} = new Button(this);
                            btn_${dest.id.filter { it.isLetterOrDigit() }}.setText("${dest.title}");
                            btn_${dest.id.filter { it.isLetterOrDigit() }}.setTextColor(Color.WHITE);
                            btn_${dest.id.filter { it.isLetterOrDigit() }}.setBackgroundColor(Color.parseColor("${project.bottomNavConfig.barColorHex}"));
                            btn_${dest.id.filter { it.isLetterOrDigit() }}.setOnClickListener(v -> webView.loadUrl("$target"));
                            navBar.addView(btn_${dest.id.filter { it.isLetterOrDigit() }}, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                            """.trimIndent()
                        }
                        """
                        LinearLayout navBar = new LinearLayout(this);
                        navBar.setOrientation(LinearLayout.HORIZONTAL);
                        $buttonsCode
                        column.addView(navBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                        """.trimIndent()
                    } else ""}

                    root.addView(column, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                    ));

                    ${if (pro.fabMenuEnabled) """
                    Button fab = new Button(this);
                    fab.setText("✉");
                    fab.setTextColor(Color.WHITE);
                    fab.setBackgroundColor(Color.parseColor("${project.accentColorHex}"));
                    FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(132, 132);
                    flp.gravity = Gravity.BOTTOM | Gravity.END;
                    flp.setMargins(24, 24, 24, 48);
                    fab.setOnClickListener(v -> {
                        try { startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$contact"))); } catch (Exception ignored) {}
                    });
                    root.addView(fab, flp);
                    """.trimIndent() else ""}

                    ${if (project.splashConfig.enabled) """
                    final FrameLayout splash = new FrameLayout(this);
                    splash.setBackgroundColor(Color.parseColor("${project.splashConfig.backgroundColorHex}"));
                    root.addView(splash, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    splash.postDelayed(() -> {
                        ${if (project.splashConfig.fadeBackground) """
                        splash.animate().alpha(0f).setDuration(600L).withEndAction(() -> root.removeView(splash)).start();
                        """.trimIndent() else "root.removeView(splash);"}
                    }, 900L);
                    """.trimIndent() else ""}

                    setContentView(root);
                    String deepLink = getIntent() != null && getIntent().getData() != null ? getIntent().getData().toString() : null;
                    webView.loadUrl(deepLink != null ? deepLink : "$startUrl");
                }

                @Override
                public void onBackPressed() {
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

        val escapedCss = pro.customCss.replace("\"", "\\\"").replace("\n", " ")
        val darkCss = if (pro.forceDarkCss) {
            "html { filter: invert(0.92) hue-rotate(180deg) !important; background: #0d1117 !important; } img, video { filter: invert(1) hue-rotate(180deg) !important; }"
        } else ""
        val combinedCss = "$darkCss $escapedCss".trim()
        val escapedJs = pro.customJs.replace("\n", " ")

        val blockedHostsArray = pro.blockedHostsCsv
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .joinToString(",") { "\"$it\"" }

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
            }
            """.trimIndent()
        } else ""

        return """
            package $pkg

            import android.annotation.SuppressLint
            import android.content.Context
            import android.content.Intent
            import android.net.ConnectivityManager
            import android.net.Uri
            import android.net.http.SslError
            import android.os.Build
            import android.os.Bundle
            import android.os.VibrationEffect
            import android.os.Vibrator
            import android.view.Gravity
            import android.view.WindowManager
            import android.webkit.JavascriptInterface
            import android.webkit.SslErrorHandler
            import android.webkit.WebResourceRequest
            import android.webkit.WebResourceResponse
            import android.webkit.WebSettings
            import android.webkit.WebView
            import android.webkit.WebViewClient
            import android.widget.FrameLayout
            import android.widget.Toast
            import androidx.appcompat.app.AppCompatActivity
            import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
            import com.google.android.material.floatingactionbutton.FloatingActionButton
            import java.io.ByteArrayInputStream

            class MainActivity : AppCompatActivity() {

                private lateinit var webView: WebView
                private val blockedAdHosts = setOf($blockedHostsArray)

                @SuppressLint("SetJavaScriptEnabled")
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)

                    ${if (pro.flagSecureEnabled) "window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)" else ""}

                    val rootFrame = FrameLayout(this)
                    val swipe = SwipeRefreshLayout(this)
                    webView = WebView(this).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.allowFileAccessFromFileURLs = true
                        settings.setSupportZoom(${project.webViewConfig.supportZoom})
                        settings.cacheMode = ${if (pro.assetPreloaderEnabled) "WebSettings.LOAD_CACHE_ELSE_NETWORK" else "WebSettings.LOAD_DEFAULT"}
                        ${if (project.bridgeConfig.jsBridgeEnabled) "addJavascriptInterface(ApkForgeBridge(this@MainActivity), \"ApkForge\")" else ""}
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
                                ${if (combinedCss.isNotBlank()) """
                                val css = "$combinedCss"
                                view?.evaluateJavascript("(function(){var s=document.createElement('style');s.innerHTML='" + css + "';document.head.appendChild(s);})();", null)
                                """.trimIndent() else ""}
                                ${if (escapedJs.isNotBlank()) """
                                view?.evaluateJavascript("$escapedJs", null)
                                """.trimIndent() else ""}
                            }
                        }
                    }

                    swipe.isEnabled = ${project.webViewConfig.pullToRefresh}
                    swipe.setOnRefreshListener { webView.reload() }
                    swipe.addView(webView)
                    rootFrame.addView(swipe)

                    ${if (pro.fabMenuEnabled) """
                    val fab = FloatingActionButton(this).apply {
                        setImageResource(android.R.drawable.ic_dialog_email)
                        setOnClickListener {
                            val contact = "${pro.fabWhatsappNumber.ifBlank { "ziorio950@gmail.com" }}"
                            if (contact.contains("@")) {
                                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + contact)))
                            } else {
                                webView.reload()
                            }
                        }
                    }
                    val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.BOTTOM or Gravity.END
                        setMargins(36, 36, 36, 48)
                    }
                    rootFrame.addView(fab, lp)
                    """.trimIndent() else ""}

                    ${if (project.splashConfig.enabled) """
                    val splashOverlay = FrameLayout(this).apply {
                        setBackgroundColor(android.graphics.Color.parseColor("${project.splashConfig.backgroundColorHex}"))
                    }
                    rootFrame.addView(splashOverlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    splashOverlay.postDelayed({
                        ${if (project.splashConfig.fadeBackground) """
                        splashOverlay.animate().alpha(0f).setDuration(600L).withEndAction {
                            rootFrame.removeView(splashOverlay)
                        }.start()
                        """.trimIndent() else """
                        rootFrame.removeView(splashOverlay)
                        """.trimIndent()}
                    }, 900L)
                    """.trimIndent() else ""}

                    setContentView(rootFrame)

                    val deepLinkUrl = intent?.data?.toString()
                    webView.loadUrl(deepLinkUrl ?: "$startUrl")
                }

                override fun onBackPressed() {
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

    private fun buildGitHubActionsWorkflow(appName: String): String {
        return """
            name: ApkForge Cloud Build ($appName)

            on:
              push:
                branches: [ "main", "master" ]
              workflow_dispatch:

            jobs:
              build-android:
                runs-on: ubuntu-latest
                steps:
                  - name: Checkout repository
                    uses: actions/checkout@v4

                  - name: Set up JDK 17
                    uses: actions/setup-java@v4
                    with:
                      distribution: 'temurin'
                      java-version: '17'

                  - name: Setup Gradle
                    uses: gradle/actions/setup-gradle@v4

                  - name: Build Release APK & AAB
                    run: gradle assembleRelease bundleRelease --no-daemon

                  - name: Upload APK Artifact
                    uses: actions/upload-artifact@v4
                    with:
                      name: $appName-release-apk
                      path: app/build/outputs/apk/release/*.apk

                  - name: Upload AAB Artifact
                    uses: actions/upload-artifact@v4
                    with:
                      name: $appName-play-bundle
                      path: app/build/outputs/bundle/release/*.aab
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
