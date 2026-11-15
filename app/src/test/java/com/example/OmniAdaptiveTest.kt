package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.*
import com.example.engine.AdaptiveSourceAnalyzer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OmniAdaptiveTest {

    @Test
    fun `complex html with 50 images and 20 buttons scales phases dynamically`() {
        val imgs = (1..50).joinToString("\n") { """<img src="img_$it.png" alt="صورة منتج $it">""" }
        val btns = (1..20).joinToString("\n") { """<button class="btn">زر رقم $it</button>""" }
        val headings = (1..16).joinToString("\n") { """<h2>قسم وميزة رقم $it</h2>""" }
        val html = "<html><body>$headings\n$imgs\n$btns</body></html>"

        val project = AppProject(
            id = "test_large_site",
            name = "LargeShop",
            sourceType = SourceType.HTML_FOLDER,
            sourceValue = "index.html"
        )

        val plan = AdaptiveSourceAnalyzer.buildAdaptivePlan(project, htmlOverride = html)

        assertEquals(50, plan.report.imageCount)
        assertEquals(20, plan.report.buttonCount)
        assertTrue(
            "Expected adaptive phases to scale to 40..100 for 50 imgs & 20 btns, got ${plan.phases.size}",
            plan.phases.size in 40..100
        )
        assertTrue(plan.phases.first().supportsLiveWebPreview)
    }

    @Test
    fun `simple html generates compact phase count`() {
        val simpleHtml = "<html><body><h1>Welcome</h1><img src='a.png'><button>Go</button></body></html>"
        val project = AppProject(
            id = "test_small_site",
            name = "SimplePage",
            sourceType = SourceType.HTML_FOLDER,
            sourceValue = "index.html"
        )

        val plan = AdaptiveSourceAnalyzer.buildAdaptivePlan(project, htmlOverride = simpleHtml)

        assertEquals(1, plan.report.imageCount)
        assertEquals(1, plan.report.buttonCount)
        assertTrue("Expected compact phase count (5..10), got ${plan.phases.size}", plan.phases.size in 5..10)
    }

    @Test
    fun `apk import generates 4 apk modification phases`() {
        val apkProject = AppProject(
            id = "test_apk",
            name = "ImportedTool",
            sourceType = SourceType.APK_PACKAGE,
            sourceValue = "/tmp/sample.apk",
            permissionsConfig = PermissionsConfig(camera = true, location = true)
        )

        val plan = AdaptiveSourceAnalyzer.buildAdaptivePlan(apkProject)

        assertEquals(AdaptiveSourceKind.APK_IMPORT, plan.sourceKind)
        assertEquals(4, plan.phases.size)
        assertFalse(plan.phases.first().supportsLiveWebPreview)
        assertEquals(2, plan.report.permissionCount)
    }

    @Test
    fun `applyFeatureEdit mutates project and writes css and js overrides to local workspace`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wsDir = File(context.filesDir, "test_ws_${System.currentTimeMillis()}").apply { mkdirs() }
        File(wsDir, "index.html").writeText(
            "<html><head></head><body><img src='hero.png'><button>Buy</button></body></html>",
            Charsets.UTF_8
        )

        val project = AppProject(
            id = "test_edit_ws",
            name = "MyShop",
            sourceType = SourceType.HTML_FOLDER,
            sourceValue = "index.html",
            localWorkspacePath = wsDir.absolutePath
        )

        val hideImgFeature = DynamicPhaseFeature(
            id = "img_show_0",
            title = "إظهار الصورة #1",
            subtitle = "",
            categoryTag = "صورة",
            type = DynamicFeatureType.SWITCH,
            bindingKey = "dom.img.visible.0",
            boolValue = true
        )
        val permFeature = DynamicPhaseFeature(
            id = "perm_cam",
            title = "الكاميرا",
            subtitle = "",
            categoryTag = "صلاحية",
            type = DynamicFeatureType.SWITCH,
            bindingKey = "perm.camera",
            boolValue = false
        )

        val afterImgEdit = AdaptiveSourceAnalyzer.applyFeatureEdit(
            context = context,
            project = project,
            feature = hideImgFeature,
            newBool = false
        )
        val afterPermEdit = AdaptiveSourceAnalyzer.applyFeatureEdit(
            context = context,
            project = afterImgEdit,
            feature = permFeature,
            newBool = true
        )

        assertTrue(afterPermEdit.permissionsConfig.camera)
        val cssFile = File(wsDir, "omni-adaptive-overrides.css")
        val jsFile = File(wsDir, "omni-adaptive-overrides.js")
        assertTrue(cssFile.exists())
        assertTrue(jsFile.exists())
        assertTrue(cssFile.readText().contains("display:none!important"))
        assertTrue(jsFile.readText().contains("imgs[0].style.display='none'"))

        wsDir.deleteRecursively()
    }

    @Test
    fun `smart templates catalog exposes 6 diverse 3d templates`() {
        val templates = SmartTemplatesCatalog.getAll()
        assertEquals(6, templates.size)
        val ids = templates.map { it.id }.toSet()
        assertEquals(setOf("tpl_store", "tpl_restaurant", "tpl_portfolio", "tpl_news", "tpl_academy", "tpl_booking"), ids)
    }

    @Test
    fun `ai roadmap generates matching adaptive phase count`() {
        val stages = (1..12).map { i ->
            AiRoadmapStage(
                stageNumber = i,
                titleAr = "مرحلة ذكية $i",
                descriptionAr = "وصف المرحلة $i",
                featureCountHint = 10,
                category = when (i % 6) {
                    1 -> "identity"
                    2 -> "auth"
                    3 -> "data"
                    4 -> "navigation"
                    5 -> "notifications"
                    else -> "packaging"
                }
            )
        }
        val aiPlan = AiArchitectPlan(
            ideaPrompt = "تطبيق تجارة ذكي",
            appNameAr = "سمارت ستور",
            packageSuffix = "smartstore",
            summaryAr = "خطة متكاملة",
            accentColorHex = "#A855F7",
            roadmap = stages,
            smartQuestions = emptyList()
        )
        val project = AppProject(
            id = "test_ai_proj",
            name = "سمارت ستور",
            sourceType = SourceType.AI_SCRATCH,
            sourceValue = "index.html"
        )

        val plan = AdaptiveSourceAnalyzer.buildAdaptivePlan(project, aiPlan = aiPlan)
        assertEquals(AdaptiveSourceKind.AI_SCRATCH, plan.sourceKind)
        assertEquals(12, plan.phases.size)
    }

    @Test
    fun `domEditsJson persists feature values across buildAdaptivePlan rebuilds and emits live js`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wsDir = File(context.filesDir, "test_dom_persist_${System.currentTimeMillis()}").apply { mkdirs() }
        val sampleHtml = "<html><body><h1>Title</h1><img src='a.png'><button>Buy</button></body></html>"
        File(wsDir, "index.html").writeText(sampleHtml, Charsets.UTF_8)

        val project = AppProject(
            id = "test_dom_persist",
            name = "PersistTest",
            sourceType = SourceType.HTML_FOLDER,
            sourceValue = "index.html",
            localWorkspacePath = wsDir.absolutePath
        )

        val btnEditFeature = DynamicPhaseFeature(
            id = "btn_txt_0",
            title = "تعديل نص الزر #1",
            subtitle = "",
            categoryTag = "زر",
            type = DynamicFeatureType.TEXT_INPUT,
            bindingKey = "dom.btn.text.0",
            textValue = "Buy"
        )
        val imgHideFeature = DynamicPhaseFeature(
            id = "img_show_0",
            title = "إظهار الصورة #1",
            subtitle = "",
            categoryTag = "صورة",
            type = DynamicFeatureType.SWITCH,
            bindingKey = "dom.img.visible.0",
            boolValue = true
        )

        val edited1 = AdaptiveSourceAnalyzer.applyFeatureEdit(
            context = context,
            project = project,
            feature = btnEditFeature,
            newText = "اطلب الآن"
        )
        val edited2 = AdaptiveSourceAnalyzer.applyFeatureEdit(
            context = context,
            project = edited1,
            feature = imgHideFeature,
            newBool = false
        )

        val rebuiltPlan = AdaptiveSourceAnalyzer.buildAdaptivePlan(edited2, htmlOverride = sampleHtml)
        val allFeatures = rebuiltPlan.phases.flatMap { it.features }

        val restoredBtn = allFeatures.find { it.bindingKey == "dom.btn.text.0" }
        val restoredImg = allFeatures.find { it.bindingKey == "dom.img.visible.0" }

        assertNotNull(restoredBtn)
        assertEquals("اطلب الآن", restoredBtn?.textValue)
        assertNotNull(restoredImg)
        assertEquals(false, restoredImg?.boolValue)

        val liveJs = AdaptiveSourceAnalyzer.buildLiveDomOverrideScript(edited2)
        assertTrue(liveJs.contains("اطلب الآن"))
        assertTrue(liveJs.contains("imgs[0].style.display='none'"))

        wsDir.deleteRecursively()
    }
}
