package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.ProjectRepository
import com.example.domain.model.*
import com.example.engine.AdaptiveIconStudio
import com.example.engine.ProjectExporter
import com.example.engine.UrlScraperEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ApkForgePowerFeaturesTest {

    @Test
    fun `full 9 step project with pro studio config persists and exports to apkforge json`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = ProjectRepository(context)

        val richProject = AppProject(
            id = "proj_test_full_9",
            name = "متجر النخبة",
            description = "وصف كامل للمشروع",
            sourceType = SourceType.WEB_URL,
            sourceValue = "https://elite.example.com",
            splashConfig = SplashConfig(enabled = true, backgroundColorHex = "#111827", indicatorStyle = "نبض"),
            onboardingConfig = OnboardingConfig(
                enabled = true,
                slides = listOf(OnboardingSlide("مرحباً", "ابدأ التسوق", "bolt"))
            ),
            webViewConfig = WebViewConfig(fullScreen = true, pullToRefresh = true, offlineFallback = true),
            bottomNavConfig = BottomNavConfig(
                enabled = true,
                destinations = listOf(
                    BottomNavDestination("h", "الرئيسية", "/"),
                    BottomNavDestination("c", "السلة", "/cart")
                )
            ),
            packageConfig = PackageConfig(
                packageName = "com.elite.shop",
                versionName = "2.1.0",
                versionCode = 14,
                minSdk = 24,
                targetSdk = 35
            ),
            permissionsConfig = PermissionsConfig(
                camera = true,
                location = true,
                notifications = true,
                vibration = true,
                networkState = true
            ),
            pushConfig = PushNotificationConfig(
                enabled = true,
                provider = "OneSignal",
                appId = "12345678-1234-1234-1234-1234567890ab"
            ),
            bridgeConfig = BridgeAndCloudConfig(
                jsBridgeEnabled = true,
                handleExternalIntents = true,
                deepLinkDomain = "elite.example.com",
                adMobAppId = "ca-app-pub-123456789~987654321",
                gitHubCiEnabled = true
            ),
            proConfig = ProStudioConfig(
                forceDarkCss = true,
                customCss = "footer { display: none; }",
                adAndPopupBlocker = true,
                biometricAuthBridge = true,
                qrScannerBridge = true,
                inAppReviewBridge = true,
                iconShape = "teardrop",
                iconBgColorHex = "#0EA5E9",
                flagSecureEnabled = true,
                sslPinningEnabled = true,
                drawerEnabled = true,
                fabMenuEnabled = true,
                fabWhatsappNumber = "966500000000"
            )
        )

        repo1.saveProject(richProject)
        repo1.updateBuildStatus(
            projectId = richProject.id,
            status = BuildStatus.SUCCESS,
            apkPath = "/storage/emulated/0/Download/Elite_v2.1.0.apk",
            customFileName = "Elite_v2.1.0.apk",
            sizeText = "7.6 MB"
        )

        val repo2 = ProjectRepository(context)
        val restored = repo2.getProjectById("proj_test_full_9")
        assertNotNull(restored)
        assertTrue(restored!!.proConfig.forceDarkCss)
        assertTrue(restored.proConfig.flagSecureEnabled)
        assertTrue(restored.proConfig.sslPinningEnabled)
        assertTrue(restored.proConfig.drawerEnabled)
        assertTrue(restored.proConfig.fabMenuEnabled)
        assertEquals("teardrop", restored.proConfig.iconShape)
        assertEquals("966500000000", restored.proConfig.fabWhatsappNumber)
        assertEquals(1, restored.buildHistory.size)

        val exportedJson = repo2.exportProjectToJson(restored)
        val imported = repo2.importProjectFromJson(exportedJson)
        assertNotNull(imported)
        assertEquals("teardrop", imported!!.proConfig.iconShape)
        assertTrue(imported.proConfig.flagSecureEnabled)
        assertNotEquals(restored.id, imported.id)
    }

    @Test
    fun `project exporter includes adaptive icon pngs security guard css injector adblocker and bridge v2`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exporter = ProjectExporter(context)
        val template = SmartTemplatesCatalog.getAll().first().buildProject().let {
            it.copy(
                proConfig = it.proConfig.copy(
                    forceDarkCss = true,
                    adAndPopupBlocker = true,
                    flagSecureEnabled = true,
                    sslPinningEnabled = true,
                    fabMenuEnabled = true,
                    fabWhatsappNumber = "966555555555",
                    biometricAuthBridge = true,
                    qrScannerBridge = true,
                    inAppReviewBridge = true
                )
            )
        }

        val iconPack = AdaptiveIconStudio.generateIconPack(template)
        assertTrue(iconPack.densityPngs.containsKey("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"))
        assertTrue(iconPack.adaptiveXml.contains("<adaptive-icon"))

        val result = exporter.exportAndroidStudioProjectZip(template)
        assertTrue(result.zipFile.exists())

        val entries = mutableMapOf<String, String>()
        ZipFile(result.zipFile).use { zip ->
            val enumEntries = zip.entries()
            while (enumEntries.hasMoreElements()) {
                val entry = enumEntries.nextElement()
                if (!entry.name.endsWith(".png")) {
                    entries[entry.name] = zip.getInputStream(entry).bufferedReader().readText()
                } else {
                    entries[entry.name] = "PNG_BYTES"
                }
            }
        }

        assertTrue(entries.containsKey(".github/workflows/apkforge-build.yml"))
        assertTrue(entries.containsKey("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"))
        assertTrue(entries.containsKey("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"))

        val mainKt = entries.entries.first { it.key.endsWith("MainActivity.kt") }.value
        assertTrue(mainKt.contains("class ApkForgeBridge"))
        assertTrue(mainKt.contains("authenticateBiometric"))
        assertTrue(mainKt.contains("scanQrCode"))
        assertTrue(mainKt.contains("requestStoreRating"))
        assertTrue(mainKt.contains("FLAG_SECURE"))
        assertTrue(mainKt.contains("shouldInterceptRequest"))
        assertTrue(mainKt.contains("FloatingActionButton"))
    }

    @Test
    fun `url scraper extracts metadata links buttons permissions and package id from html`() {
        val scraper = UrlScraperEngine()
        val html = """
            <html lang="ar">
              <head>
                <title>متجر المستقبل | Future Shop</title>
                <meta name="description" content="أفضل تطبيق للتسوق السريع" />
                <meta name="theme-color" content="#10B981" />
                <link rel="manifest" href="/manifest.webmanifest" />
                <link rel="icon" href="/assets/logo.png" />
              </head>
              <body>
                <nav>
                  <a href="/products">المنتجات</a>
                  <a href="/cart">السلة</a>
                </nav>
                <button>اشترك الآن</button>
                <a href="mailto:ziorio950@gmail.com">راسلنا</a>
                <script>navigator.geolocation.getCurrentPosition(() => {});</script>
              </body>
            </html>
        """.trimIndent()

        val identity = scraper.parseHtmlMetadata("https://shop.futuristic.sa", html)
        assertEquals("متجر المستقبل | Future Shop", identity.title)
        assertEquals("#10B981", identity.themeColorHex)
        assertEquals("https://shop.futuristic.sa/assets/logo.png", identity.iconUrl)
        assertEquals("sa.futuristic.shop", identity.suggestedPackageName)
        assertTrue(identity.hasPwaManifest)
        assertTrue(identity.detectedLinks.any { it.relativePath == "/products" })
        assertTrue(identity.detectedButtons.contains("اشترك الآن"))
        assertTrue(identity.detectedPermissions.contains("location"))
        assertTrue(identity.detectedExternalSchemes.contains("mailto:"))

        val proj = scraper.toPreConfiguredProject(identity)
        assertEquals(SourceType.WEB_URL, proj.sourceType)
        assertEquals("#10B981", proj.accentColorHex)
        assertEquals("shop.futuristic.sa", proj.bridgeConfig.deepLinkDomain)
        assertTrue(proj.permissionsConfig.location)
    }

    @Test
    fun `toolchain manager installs verifies tools generates RSA keystore and Termux diagnostic reports honest state`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolchain = com.example.engine.ToolchainManager(context)
        val installed = toolchain.installAndVerifyAllTools()
        assertTrue(installed)
        assertEquals(8, toolchain.tools.value.size)
        assertTrue(toolchain.tools.value.all { it.isInstalled && it.verifiedSha256.isNotBlank() && it.sizeBytes > 0L })

        val ksRes = toolchain.generateRealKeystore("release_test", "pass1234", "ApkForge")
        assertTrue(ksRes.isSuccess)
        val ksInfo = ksRes.getOrThrow()
        assertTrue(java.io.File(ksInfo.filePath).exists())
        assertTrue(ksInfo.sha256Fingerprint.isNotBlank())

        val termux = com.example.engine.TermuxEngine(context)
        val report = termux.runRealDiagnostic()
        assertFalse(report.isTermuxInstalled)
        assertFalse(report.hasRunCommandPermission)
    }

    @Test
    fun `local workspace engine locates nested entry html and lists assets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tmpDir = java.io.File(context.cacheDir, "ws_test_${System.currentTimeMillis()}").apply { mkdirs() }
        val nestedDir = java.io.File(tmpDir, "dist").apply { mkdirs() }
        val htmlFile = java.io.File(nestedDir, "index.html").apply {
            writeText("<html><head><title>Nested Site</title></head><body>Hi</body></html>")
        }
        val cssFile = java.io.File(nestedDir, "style.css").apply { writeText("body { color: #fff; }") }

        val found = com.example.engine.LocalWorkspaceEngine.findEntryHtml(tmpDir)
        assertNotNull(found)
        assertEquals(htmlFile.absolutePath, found!!.absolutePath)

        val assets = com.example.engine.LocalWorkspaceEngine.listWorkspaceAssets(tmpDir.absolutePath)
        assertTrue(assets.any { it.relativePath.endsWith("style.css") })
        assertTrue(assets.any { it.relativePath.endsWith("index.html") })
    }

    @Test
    fun `termux engine executes real signed build on disk with workspace assets and archive analyzer reads real entries`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wsDir = java.io.File(context.cacheDir, "real_build_ws_${System.currentTimeMillis()}").apply { mkdirs() }
        java.io.File(wsDir, "index.html").writeText("<html><body><h1>Real Site</h1></body></html>")
        java.io.File(wsDir, "app.js").writeText("console.log('ready');")

        val project = AppProject(
            id = "proj_real_build",
            name = "Room bk",
            sourceType = SourceType.ZIP_ARCHIVE,
            sourceValue = "roombk.zip",
            localWorkspacePath = wsDir.absolutePath,
            packageConfig = PackageConfig(
                packageName = "com.roombk.app",
                versionName = "1.0.0",
                versionCode = 1
            )
        )

        val engine = com.example.engine.TermuxEngine(context)
        var producedFile: String? = null
        var producedPath: String? = null
        var producedSize: String? = null

        val ok = engine.executeBuildWithProgress(project, "debug") { fileName, outputPath, sizeText ->
            producedFile = fileName
            producedPath = outputPath
            producedSize = sizeText
        }

        assertTrue(ok)
        assertNotNull(producedFile)
        assertNotNull(producedPath)
        assertNotNull(producedSize)

        val artifact = java.io.File(producedPath!!)
        assertTrue(artifact.exists() && artifact.length() > 0L)

        val zipNames = mutableSetOf<String>()
        ZipFile(artifact).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                zipNames.add(entries.nextElement().name)
            }
        }

        assertTrue(zipNames.contains("META-INF/MANIFEST.MF"))
        assertTrue(zipNames.contains("META-INF/APKFORGE.SF"))
        assertTrue(zipNames.contains("META-INF/APKFORGE.SIG"))
        assertTrue(zipNames.contains("META-INF/BUILD_RECEIPT.json"))
        assertTrue(zipNames.contains("app/src/main/assets/index.html"))
        assertTrue(zipNames.contains("app/src/main/assets/app.js"))
        assertTrue(zipNames.contains("termux_standalone/AndroidManifest.xml"))
        assertTrue(zipNames.contains("termux_standalone/src/com/roombk/app/MainActivity.java"))
        assertTrue(zipNames.contains("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"))

        val toolchain = com.example.engine.ToolchainManager(context)
        val breakdown = toolchain.analyzeArchiveBreakdown(artifact.absolutePath)
        assertNotNull(breakdown)
        assertTrue(breakdown!!.existsOnDisk)
        assertTrue(breakdown.entryCount > 15)
        assertEquals(artifact.length(), breakdown.totalBytes)
        assertTrue(breakdown.isSigned)
        assertTrue(breakdown.hasManifest)
        assertTrue(breakdown.certificateEntriesCount >= 3)

        val bash = engine.generateBashBuildScript(project, "release")
        assertTrue(bash.contains("--manifest termux_standalone/AndroidManifest.xml"))
        assertTrue(bash.contains("Theme.DeviceDefault.NoActionBar"))
    }
}
