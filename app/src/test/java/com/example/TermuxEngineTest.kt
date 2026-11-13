package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AppProject
import com.example.domain.model.PackageConfig
import com.example.domain.model.PermissionsConfig
import com.example.domain.model.PushNotificationConfig
import com.example.domain.model.SourceType
import com.example.engine.BuildOptimizationOptions
import com.example.engine.BuildStageStatus
import com.example.engine.TermuxEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TermuxEngineTest {

    @Test
    fun `generateBashBuildScript contains project name and gradle command`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TermuxEngine(context)

        val project = AppProject(
            id = "test_01",
            name = "Rio",
            sourceType = SourceType.WEB_URL,
            sourceValue = "https://rio-store.example.com",
            packageConfig = PackageConfig(
                packageName = "com.rio.app",
                versionName = "1.0.0"
            )
        )

        val script = engine.generateBashBuildScript(project, "debug")
        assertTrue(script.contains("Rio"))
        assertTrue(script.contains("com.rio.app"))
        assertTrue(script.contains("aapt2 compile"))
        assertTrue(script.contains("APKSIGNER_BIN"))
        assertTrue(script.contains("zipalign"))
        assertTrue(script.contains("--ks-key-alias"))
        assertTrue(script.contains("xargs -0 d8"))
        assertTrue(script.contains("keytool -genkeypair"))
        assertTrue(script.contains("apkforge_workspace"))
    }

    @Test
    fun `generateBashBuildScript includes enabled new permissions in manifest`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TermuxEngine(context)

        val project = AppProject(
            id = "test_02",
            name = "PermsApp",
            sourceType = SourceType.ZIP_ARCHIVE,
            sourceValue = "archive.zip",
            permissionsConfig = PermissionsConfig(
                notifications = true,
                storage = true,
                vibration = true,
                bluetooth = true,
                networkState = true
            )
        )

        val script = engine.generateBashBuildScript(project, "debug")
        assertTrue(script.contains("android.permission.POST_NOTIFICATIONS"))
        assertFalse(script.contains("android.permission.READ_EXTERNAL_STORAGE"))
        assertTrue(script.contains("android.permission.VIBRATE"))
        assertTrue(script.contains("android.permission.BLUETOOTH"))
        assertTrue(script.contains("android.permission.ACCESS_NETWORK_STATE"))
    }

    @Test
    fun `generateBashBuildScript includes OneSignal push metadata when pushConfig is enabled`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TermuxEngine(context)

        val project = AppProject(
            id = "test_03",
            name = "PushApp",
            sourceType = SourceType.WEB_URL,
            sourceValue = "https://example.com",
            pushConfig = PushNotificationConfig(
                enabled = true,
                provider = "OneSignal",
                appId = "a4f8c92e-7b13-4d9a-9e21-8c3f5b10294a"
            )
        )

        val script = engine.generateBashBuildScript(project, "debug")
        assertTrue(script.contains("android.permission.POST_NOTIFICATIONS"))
        assertTrue(script.contains("onesignal_app_id"))
        assertTrue(script.contains("a4f8c92e-7b13-4d9a-9e21-8c3f5b10294a"))
    }

    @Test
    fun `runPreBuildAudit and autoFixProject repair invalid fields cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TermuxEngine(context)

        val brokenProject = AppProject(
            id = "test_04",
            name = "MyShop",
            sourceType = SourceType.WEB_URL,
            sourceValue = "http://myshop.example.com",
            packageConfig = PackageConfig(
                packageName = "invalidpkg",
                targetSdk = 30
            ),
            pushConfig = PushNotificationConfig(enabled = true, appId = "")
        )

        val beforeAudit = engine.runPreBuildAudit(brokenProject, hasKeystore = false)
        assertTrue(beforeAudit.any { !it.passed })

        val fixedProject = engine.autoFixProject(brokenProject)
        assertTrue(fixedProject.packageConfig.packageName.contains("."))
        assertTrue(fixedProject.packageConfig.targetSdk >= 34)
        assertTrue(fixedProject.sourceValue.startsWith("https://"))
        assertTrue(fixedProject.pushConfig.appId.isNotBlank())
    }

    @Test
    fun `executeBuildWithProgress completes all 6 pipeline stages and produces real signed artifact on disk`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TermuxEngine(context)
        val project = AppProject(
            id = "test_05",
            name = "LivePipelineApp",
            sourceType = SourceType.ZIP_ARCHIVE,
            sourceValue = "site.zip"
        )

        val producedFiles = mutableListOf<String>()
        val producedPaths = mutableListOf<String>()
        val success = engine.executeBuildWithProgress(
            project = project,
            buildType = "both",
            options = BuildOptimizationOptions(r8Minify = true, shrinkResources = true, abiSplit = true)
        ) { fileName, outputPath, _ ->
            producedFiles.add(fileName)
            producedPaths.add(outputPath)
        }

        assertTrue(success)
        assertEquals(1, producedFiles.size)
        assertTrue(producedFiles.first().endsWith("_signed_bundle.zip"))
        assertTrue(java.io.File(producedPaths.first()).exists())
        assertTrue(engine.buildStages.value.all { it.status == BuildStageStatus.DONE })
        assertEquals(6, engine.telemetry.value.assembledLayers)
    }

    @Test
    fun `verifyBridgePrerequisites blocks dispatch when Termux or RUN_COMMAND permission is missing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = TermuxEngine(context)

        val check = engine.verifyBridgePrerequisites()
        assertFalse(check.isReadyToDispatch)
        assertEquals(com.example.engine.TermuxConnectionState.NOT_INSTALLED, check.connectionState)

        val pingCheck = engine.pingTermuxConnection()
        assertFalse(pingCheck.isReadyToDispatch)
    }

    @Test
    fun `ensureProjectKeystoreExists auto-generates valid keystore file before build`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toolchain = com.example.engine.ToolchainManager(context)
        val project = AppProject(
            id = "test_ks_01",
            name = "AutoKeyApp",
            sourceType = SourceType.WEB_URL,
            sourceValue = "https://example.com"
        )

        val info = toolchain.ensureProjectKeystoreExists(project)
        val ksFile = java.io.File(info.filePath)
        assertTrue(ksFile.exists())
        assertTrue(ksFile.length() > 0L)
        assertTrue(info.sha256Fingerprint.isNotBlank())
    }

    @Test
    fun `TermuxResultReceiver handles err 2, err -1, and exitCode 0 cleanly`() {
        val err2 = com.example.engine.TermuxExecCallbackResult(
            mode = "ping",
            stdout = "",
            stderr = "RunCommandService requires allow-external-apps",
            exitCode = -1,
            errorCode = 2,
            errorMessage = "Error (2): allow-external-apps required"
        )
        assertTrue(err2.requiresExternalAppsEnable)
        assertFalse(err2.isSuccess)

        val errNeg1 = com.example.engine.TermuxExecCallbackResult(
            mode = "build",
            stdout = "",
            stderr = "Permission denied",
            exitCode = -1,
            errorCode = -1,
            errorMessage = ""
        )
        assertFalse(errNeg1.isSuccess)

        val success = com.example.engine.TermuxExecCallbackResult(
            mode = "ping",
            stdout = "APKFORGE_PING_OK",
            stderr = "",
            exitCode = 0,
            errorCode = 0,
            errorMessage = ""
        )
        assertTrue(success.isSuccess)
        assertFalse(success.requiresExternalAppsEnable)
    }
}
