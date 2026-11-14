package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.ProjectRepository
import com.example.domain.model.*
import com.example.engine.BuildOptimizationOptions
import com.example.engine.ProjectExporter
import com.example.engine.QuantumBuildEngine
import com.example.engine.UrlScraperEngine
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
class OmniIdeFeaturesTest {

    @Test
    fun `global languages expose proper RTL and LTR layout directions`() {
        assertEquals(10, AppLanguage.entries.size)
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertTrue(AppLanguage.URDU.isRtl)
        assertFalse(AppLanguage.ENGLISH.isRtl)
        assertFalse(AppLanguage.FRENCH.isRtl)
        assertFalse(AppLanguage.SPANISH.isRtl)
        assertFalse(AppLanguage.TURKISH.isRtl)
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromCode("fr"))
    }

    @Test
    fun `repository persists OmniIdeConfig roundtrip and clears all data cleanly`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = ProjectRepository(context)
        repo.clearAllProjectsAndDownloads()

        val project = AppProject(
            id = "omni_test_1",
            name = "OmniShop",
            omniConfig = OmniIdeConfig(
                gradientEnabled = true,
                gradientStartHex = "#0EA5E9",
                gradientEndHex = "#6366F1",
                fontScalePercent = 115,
                authBuilderEnabled = true,
                localDbEnabled = true,
                localDbTableName = "shop_orders",
                smartPermissionsRationaleEnabled = true,
                appAcceleratorEnabled = true,
                customHeroTitle = "أومني شوب",
                customPrimaryButtonText = "تسوق الآن"
            )
        )
        repo.saveProject(project)

        val reloadedRepo = ProjectRepository(context)
        val restored = reloadedRepo.projects.value.find { it.id == "omni_test_1" }
        assertNotNull(restored)
        assertTrue(restored!!.omniConfig.gradientEnabled)
        assertEquals("#0EA5E9", restored.omniConfig.gradientStartHex)
        assertEquals(115, restored.omniConfig.fontScalePercent)
        assertEquals("shop_orders", restored.omniConfig.localDbTableName)
        assertEquals("أومني شوب", restored.omniConfig.customHeroTitle)

        reloadedRepo.updateBuildStatus(
            projectId = "omni_test_1",
            status = BuildStatus.SUCCESS,
            apkPath = "/tmp/OmniShop_v1.0_quantum.apks",
            customFileName = "OmniShop_v1.0_quantum.apks",
            sizeText = "240 KB"
        )
        assertEquals("APKS", reloadedRepo.downloads.value.first().buildType)

        reloadedRepo.clearAllProjectsAndDownloads()
        assertTrue(reloadedRepo.projects.value.isEmpty())
        assertTrue(reloadedRepo.downloads.value.isEmpty())
    }

    @Test
    fun `exporter generates universal single APK workflow, Smart Permissions Rationale, and App Accelerator`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val exporter = ProjectExporter(context)
        val project = AppProject(
            id = "proj_omni_exp",
            name = "SmartOmni",
            packageConfig = PackageConfig("com.omni.smart"),
            permissionsConfig = PermissionsConfig(camera = true, location = true, notifications = true),
            omniConfig = OmniIdeConfig(
                smartPermissionsRationaleEnabled = true,
                appAcceleratorEnabled = true,
                gradientEnabled = true
            )
        )

        val files = exporter.buildProjectTextEntries(project, BuildOptimizationOptions())
        val workflow = files[".github/workflows/apkforge-build.yml"]!!
        assertTrue(workflow.contains("assembleRelease bundleRelease"))
        assertFalse(workflow.contains("assembleDebug"))

        val appGradle = files["app/build.gradle.kts"]!!
        assertTrue(appGradle.contains("arm64-v8a"))
        assertTrue(appGradle.contains("armeabi-v7a"))
        assertTrue(appGradle.contains("x86_64"))
        assertFalse(appGradle.contains("splits {"))

        val mainKt = files["app/src/main/java/com/omni/smart/MainActivity.kt"]!!
        assertTrue(mainKt.contains("إدارة الأذونات الذكية"))
        assertTrue(mainKt.contains("requestPermissions"))
        assertTrue(mainKt.contains("dbPut"))
        assertTrue(mainKt.contains("getOmniConfig"))

        val javaActivity = exporter.buildStandaloneJavaActivity(project)
        assertTrue(javaActivity.contains("إدارة الأذونات الذكية"))
        assertTrue(javaActivity.contains("dbGet"))
    }

    @Test
    fun `url scraper materializes website locally and writes visual CSS JS customization files`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val scraper = UrlScraperEngine()
        val project = AppProject(
            id = "pwa_local_test",
            name = "MySite",
            sourceType = SourceType.WEB_URL,
            sourceValue = "https://example.org",
            omniConfig = OmniIdeConfig(
                gradientEnabled = true,
                gradientStartHex = "#FF7A2F",
                gradientEndHex = "#0F1318",
                customHeroTitle = "العنوان الجديد",
                customPrimaryButtonText = "دخول سريع",
                customHideSelectorsCsv = ".ads-banner, footer"
            )
        )

        val materialized = scraper.materializeWebsiteAsLocalProject(
            context = context,
            project = project,
            rawHtmlOverride = "<html><head><title>Old</title></head><body><h1>Old Hero</h1><button class='btn-primary'>Old</button></body></html>"
        )
        assertEquals(SourceType.HTML_FOLDER, materialized.sourceType)
        assertNotNull(materialized.localWorkspacePath)

        val wsDir = File(materialized.localWorkspacePath!!)
        assertTrue(File(wsDir, "index.html").exists())
        val cssText = File(wsDir, "omni-overrides.css").readText()
        assertTrue(cssText.contains(".ads-banner, footer { display: none !important; }"))
        val jsText = File(wsDir, "omni-overrides.js").readText()
        assertTrue(jsText.contains("العنوان الجديد"))
        assertTrue(jsText.contains("دخول سريع"))
    }

    @Test
    fun `quantum build engine packages valid APKS split archive with base master signed APK`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val engine = QuantumBuildEngine.getInstance(context)
        val project = AppProject(
            id = "apks_test_proj",
            name = "OmniApks",
            packageConfig = PackageConfig("com.omni.apks")
        )

        var producedPath = ""
        val success = engine.executeBuildWithProgress(
            project = project,
            buildType = "apks",
            options = BuildOptimizationOptions()
        ) { _, path, _ ->
            producedPath = path
        }

        assertTrue(success)
        val outFile = File(producedPath)
        assertTrue(outFile.exists())
        assertTrue(outFile.name.endsWith(".apks"))

        ZipFile(outFile).use { zip ->
            assertNotNull(zip.getEntry("splits/base-master.apk"))
            assertNotNull(zip.getEntry("meta.json"))
            assertNotNull(zip.getEntry("toc.pb"))
        }
    }
}
