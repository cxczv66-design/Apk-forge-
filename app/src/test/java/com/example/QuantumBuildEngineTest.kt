package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.android.apksig.ApkVerifier
import com.example.data.repository.ProjectRepository
import com.example.domain.model.*
import com.example.engine.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuantumBuildEngineTest {

    private fun buildSampleProject(wsDir: File? = null): AppProject {
        return AppProject(
            id = "proj_quantum_${System.currentTimeMillis()}",
            name = "متجر النخبة",
            sourceType = if (wsDir != null) SourceType.ZIP_ARCHIVE else SourceType.WEB_URL,
            sourceValue = if (wsDir != null) "elite_site.zip" else "https://elite.example.com",
            localWorkspacePath = wsDir?.absolutePath,
            primaryColorHex = "#0EA5E9",
            accentColorHex = "#F59E0B",
            splashConfig = SplashConfig(
                enabled = true,
                backgroundColorHex = "#0B1120",
                indicatorStyle = "نبض"
            ),
            webViewConfig = WebViewConfig(
                fullScreen = true,
                pullToRefresh = true,
                cacheEnabled = true
            ),
            bottomNavConfig = BottomNavConfig(
                enabled = true,
                barColorHex = "#111827",
                destinations = listOf(
                    BottomNavDestination("home", "الرئيسية", "/"),
                    BottomNavDestination("shop", "المتجر", "/shop")
                )
            ),
            packageConfig = PackageConfig(
                packageName = "com.elite.quantumstore",
                versionName = "3.4.1",
                versionCode = 42,
                minSdk = 24,
                targetSdk = 35
            ),
            permissionsConfig = PermissionsConfig(
                camera = true,
                location = true,
                notifications = true,
                vibration = true
            ),
            signingConfig = SigningKeyConfig(
                useDebugKey = false,
                keyAlias = "quantum_test_key",
                keystorePath = ""
            )
        )
    }

    @Test
    fun `QuantumBuildCommandSpec translates all user project settings directly into aapt2, d8, zipalign, and apksig commands`() {
        val project = buildSampleProject()
        val vaultFile = File("/tmp/quantum_test_key.qvault")
        val options = BuildOptimizationOptions(
            r8Minify = true,
            shrinkResources = true,
            abiSplit = true,
            zipAlign = true
        )

        val spec = QuantumBuildCommandSpec.fromProject(project, "release", options, vaultFile)

        assertTrue(spec.aapt2CompileCmd.contains("aapt2 compile"))
        assertTrue(spec.aapt2LinkCmd.contains("--custom-package com.elite.quantumstore"))
        assertTrue(spec.aapt2LinkCmd.contains("--version-code 42"))
        assertTrue(spec.aapt2LinkCmd.contains("--version-name \"3.4.1\""))
        assertTrue(spec.aapt2LinkCmd.contains("--min-sdk-version 24"))
        assertTrue(spec.aapt2LinkCmd.contains("--target-sdk-version 35"))
        assertTrue(spec.aapt2LinkCmd.contains("--shrink-resources"))
        assertTrue(spec.aapt2LinkCmd.contains("--split-abi"))

        assertTrue(spec.d8CompileCmd.contains("d8 --release"))
        assertTrue(spec.d8CompileCmd.contains("--min-api 24"))
        assertTrue(spec.d8CompileCmd.contains("--pg-conf"))

        assertTrue(spec.zipAlignCmd.contains("zipalign -p -f -v 4"))

        assertTrue(spec.apkSigCmd.contains("apksigner sign"))
        assertTrue(spec.apkSigCmd.contains("--v1-signing-enabled true"))
        assertTrue(spec.apkSigCmd.contains("--v2-signing-enabled true"))
        assertTrue(spec.apkSigCmd.contains("--v3-signing-enabled true"))
        assertTrue(spec.apkSigCmd.contains("--v4-signing-enabled true"))
        assertTrue(spec.apkSigCmd.contains("--v4-signature-file"))
        assertTrue(spec.translatedFlagsSummary.any { it.contains("#0EA5E9") && it.contains("#F59E0B") })
    }

    @Test
    fun `QuantumKeyVault seals RSA-2048 X509 keys using AES-256-GCM without writing plaintext private keys on disk`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = QuantumKeyVault(context)

        val created = vault.generateEncryptedVault(
            alias = "unit_vault_key",
            password = "strong_vault_pass",
            organization = "Quantum Unit"
        )

        assertTrue(created.vaultFile.exists())
        assertTrue(created.sha256Fingerprint.length == 64)
        assertEquals("RSA", created.privateKey.algorithm)

        val vaultText = created.vaultFile.readText(Charsets.UTF_8)
        assertFalse(vaultText.contains("BEGIN PRIVATE KEY"))
        assertFalse(vaultText.contains("RSA PRIVATE KEY"))
        assertTrue(vaultText.contains("QuantumKeyVault"))
        assertTrue(vaultText.contains("cipherB64"))

        val status = vault.getVaultStatus("unit_vault_key")
        assertTrue(status.vaultExists)
        assertTrue(status.noPlaintextPrivateKeyOnDisk)

        val unlocked = vault.ensureAndUnlockKeyMaterial("unit_vault_key", "strong_vault_pass", "Quantum Unit")
        assertEquals(created.sha256Fingerprint, unlocked.sha256Fingerprint)
        assertArrayEquals(created.privateKey.encoded, unlocked.privateKey.encoded)
    }

    @Test
    fun `QuantumBuildEngine builds aligned APK with binary AXML, resources arsc, Dalvik dex 035, and verified Google apksig v1 v2 v3 v4`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wsDir = File(context.cacheDir, "q_ws_${System.currentTimeMillis()}").apply { mkdirs() }
        File(wsDir, "index.html").writeText("<html><head><title>Quantum Shop</title></head><body><h1>Ready</h1></body></html>")
        File(wsDir, "bundle.js").writeText("window.appReady = true;")

        val project = buildSampleProject(wsDir)
        val engine = QuantumBuildEngine(context)

        var outputFile: String? = null
        var outputPath: String? = null
        var outputSize: String? = null

        val success = engine.executeBuildWithProgress(
            project = project,
            buildType = "release",
            options = BuildOptimizationOptions(r8Minify = true, shrinkResources = true, zipAlign = true)
        ) { fileName, path, size ->
            outputFile = fileName
            outputPath = path
            outputSize = size
        }

        assertTrue("Expected Quantum build to succeed", success)
        assertNotNull(outputFile)
        assertNotNull(outputPath)
        assertNotNull(outputSize)

        val apkFile = File(outputPath!!)
        assertTrue(apkFile.exists() && apkFile.length() > 0L)
        val v4Idsig = File(apkFile.parentFile, "${apkFile.name}.idsig")
        assertTrue("Expected v4 .idsig signature file", v4Idsig.exists() && v4Idsig.length() > 0L)

        // Check telemetry verification flags
        val telemetry = engine.telemetry.value
        assertTrue(telemetry.alignmentVerified)
        assertTrue(telemetry.v1Verified)
        assertTrue(telemetry.v2Verified)
        assertTrue(telemetry.v3Verified)
        assertTrue(telemetry.v4Verified)
        assertEquals(6, telemetry.assembledLayers)
        assertTrue(engine.buildStages.value.all { it.status == BuildStageStatus.DONE })

        // Verify strict 4-byte ZipAlign on disk
        assertTrue(QuantumZipAligner().verifyZipAlignment(apkFile))

        // Verify cryptographic signatures independently via Google ApkVerifier
        val verified = ApkVerifier.Builder(apkFile)
            .setMinCheckedPlatformVersion(21)
            .build()
            .verify()
        assertTrue("Google ApkVerifier failed: ${verified.errors}", verified.isVerified)
        assertTrue(verified.isVerifiedUsingV1Scheme)
        assertTrue(verified.isVerifiedUsingV2Scheme)
        assertTrue(verified.isVerifiedUsingV3Scheme)

        // Verify real APK binary payloads (Binary AXML Manifest, resources.arsc, Dalvik 035 classes.dex, res, assets)
        ZipFile(apkFile).use { zip ->
            val manifestEntry = zip.getEntry("AndroidManifest.xml")
            assertNotNull(manifestEntry)
            val manifestHeader = zip.getInputStream(manifestEntry).readNBytes(4)
            // Binary Android XML chunk header: 0x0003 (RES_XML_TYPE), 0x0008 (header size)
            assertEquals(0x03.toByte(), manifestHeader[0])
            assertEquals(0x00.toByte(), manifestHeader[1])

            val arscEntry = zip.getEntry("resources.arsc")
            assertNotNull(arscEntry)
            val arscHeader = zip.getInputStream(arscEntry).readNBytes(4)
            // Binary resource table header: 0x0002 (RES_TABLE_TYPE)
            assertEquals(0x02.toByte(), arscHeader[0])
            assertEquals(0x00.toByte(), arscHeader[1])

            val dexEntry = zip.getEntry("classes.dex")
            assertNotNull(dexEntry)
            val dexMagic = zip.getInputStream(dexEntry).readNBytes(8)
            assertEquals("dex\n035\u0000", String(dexMagic, Charsets.US_ASCII))

            assertNotNull(zip.getEntry("assets/quantum_app_config.json"))
            assertNotNull(zip.getEntry("assets/index.html"))
            assertNotNull(zip.getEntry("assets/bundle.js"))
            assertNotNull(zip.getEntry("res/mipmap-xxxhdpi/ic_launcher.png"))
        }
    }

    @Test
    fun `QuantumBuildWorker executes full pipeline in background and persists success into ProjectRepository`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = ProjectRepository(context)
        val project = buildSampleProject()
        repo.saveProject(project)

        val jobFile = QuantumBuildWorker.stageBuildJobFile(
            context = context,
            projectJson = repo.serializeProjectToJson(project),
            buildType = "debug",
            options = BuildOptimizationOptions()
        )
        assertTrue(jobFile.exists())

        val worker = TestListenableWorkerBuilder<QuantumBuildWorker>(context)
            .setInputData(workDataOf(QuantumBuildWorker.KEY_JOB_FILE_PATH to jobFile.absolutePath))
            .build()

        val result = worker.doWork()
        assertTrue("Worker expected Success but got $result", result is ListenableWorker.Result.Success)
        val successOut = (result as ListenableWorker.Result.Success).outputData
        val apkPath = successOut.getString(QuantumBuildWorker.KEY_OUTPUT_APK_PATH).orEmpty()
        assertTrue(File(apkPath).exists())

        val reloadedRepo = ProjectRepository(context)
        val updated = reloadedRepo.getProjectById(project.id)
        assertNotNull(updated)
        assertEquals(BuildStatus.SUCCESS, updated!!.lastBuildStatus)
        assertEquals(apkPath, updated.lastOutputApk)
    }

    @Test
    fun `preBuildAudit, autoFixProject, and ProjectExporter standalone_cli XMLs stay valid without Termux artifacts`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = QuantumBuildEngine(context)

        val broken = AppProject(
            id = "broken_q_1",
            name = "Broken App",
            sourceType = SourceType.WEB_URL,
            sourceValue = "http://insecure.example.com",
            packageConfig = PackageConfig(packageName = "invalidpkg", targetSdk = 30)
        )
        val auditBefore = engine.runPreBuildAudit(broken, hasKeystore = false)
        assertTrue(auditBefore.any { !it.passed })

        val fixed = engine.autoFixProject(broken)
        val auditAfter = engine.runPreBuildAudit(fixed, hasKeystore = true)
        assertTrue(auditAfter.all { it.passed })
        assertTrue(fixed.signingConfig.keystorePath.endsWith(".qvault"))
        assertFalse(fixed.signingConfig.useDebugKey)

        val exporter = ProjectExporter(context)
        val entries = exporter.buildProjectTextEntries(fixed)
        assertFalse(entries.keys.any { it.contains("termux", ignoreCase = true) })
        val cliManifest = entries["standalone_cli/AndroidManifest.xml"]!!
        assertTrue(cliManifest.startsWith("<"))
        assertTrue(entries.containsKey("standalone_cli/src/com/apkforge/brokenapp/MainActivity.java"))
    }
}
