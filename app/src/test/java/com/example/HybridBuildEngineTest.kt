package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AppProject
import com.example.domain.model.PackageConfig
import com.example.domain.model.ProStudioConfig
import com.example.domain.model.SourceType
import com.example.engine.BuildCapabilitiesInspector
import com.example.engine.BuildLogSanitizer
import com.example.engine.GitHubArtifactItem
import com.example.engine.GitHubCloudBuildEngine
import com.example.engine.HybridBuildMode
import com.example.engine.LocalCapabilitySupport
import com.example.engine.NetworkConnectivitySnapshot
import com.example.engine.BuildNetworkQuality
import com.example.engine.DeviceCapabilitiesSnapshot
import com.example.engine.LibsodiumSealedBox
import com.android.apksig.ApkSigner
import com.example.engine.ProjectExporter
import com.example.engine.QuantumKeyVault
import com.example.engine.SelectedBuildEngine
import com.example.engine.SmartBuildRouter
import com.example.engine.ToolchainManager
import com.example.ui.components.WebViewSafety
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.bouncycastle.crypto.engines.XSalsa20Engine
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import org.bouncycastle.math.ec.rfc7748.X25519
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HybridBuildEngineTest {

    private fun simpleWebProject(): AppProject = AppProject(
        id = "proj_hybrid_web",
        name = "HybridShop",
        sourceType = SourceType.HTML_FOLDER,
        sourceValue = "index.html",
        packageConfig = PackageConfig(packageName = "com.hybrid.shop", targetSdk = 35)
    )

    private fun nativeFeatureProject(): AppProject = AppProject(
        id = "proj_hybrid_native",
        name = "BiometricBank",
        sourceType = SourceType.WEB_URL,
        sourceValue = "https://bank.example.com",
        proConfig = ProStudioConfig(
            biometricAuthBridge = true,
            qrScannerBridge = true,
            drawerEnabled = true
        ),
        packageConfig = PackageConfig(packageName = "com.hybrid.bank", targetSdk = 35)
    )

    private val wifiOnline = NetworkConnectivitySnapshot(
        isConnected = true,
        isValidated = true,
        quality = BuildNetworkQuality.UNMETERED_WIFI,
        labelAr = "شبكة Wi-Fi / غير محدودة"
    )

    private val offlineNet = NetworkConnectivitySnapshot(
        isConnected = false,
        isValidated = false,
        quality = BuildNetworkQuality.OFFLINE,
        labelAr = "لا يوجد اتصال بالإنترنت"
    )

    private val healthyDevice = DeviceCapabilitiesSnapshot(
        abi = "arm64-v8a",
        freeStorageMb = 1024L,
        totalRamMb = 6144L,
        lowMemoryDevice = false,
        hasNativeAapt2Binary = false,
        hasNativeD8Binary = false
    )

    private val lowStorageDevice = healthyDevice.copy(freeStorageMb = 18L)

    @Test
    fun `SmartBuildRouter picks Local for lightweight HTML APK and Cloud for AAB or native features when online`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = SmartBuildRouter(context)

        val localDecision = router.decide(
            project = simpleWebProject(),
            targetType = "release",
            requestedMode = HybridBuildMode.AUTO_SMART,
            cloudTokenConfigured = true,
            networkOverride = wifiOnline,
            deviceOverride = healthyDevice
        )
        assertEquals(SelectedBuildEngine.LOCAL_QUANTUM, localDecision.selectedEngine)
        assertFalse(localDecision.requiresCloudForFullFidelity)

        val aabDecision = router.decide(
            project = simpleWebProject(),
            targetType = "aab",
            requestedMode = HybridBuildMode.AUTO_SMART,
            cloudTokenConfigured = true,
            networkOverride = wifiOnline,
            deviceOverride = healthyDevice
        )
        assertEquals(SelectedBuildEngine.CLOUD_GRADLE_SDK, aabDecision.selectedEngine)
        assertTrue(aabDecision.requiresCloudForFullFidelity)

        val nativeCloudDecision = router.decide(
            project = nativeFeatureProject(),
            targetType = "release",
            requestedMode = HybridBuildMode.AUTO_SMART,
            cloudTokenConfigured = true,
            networkOverride = wifiOnline,
            deviceOverride = healthyDevice
        )
        assertEquals(SelectedBuildEngine.CLOUD_GRADLE_SDK, nativeCloudDecision.selectedEngine)
        assertTrue(nativeCloudDecision.cloudReasons.any { it.contains("NativeDrawerLayout") })

        val lowDiskDecision = router.decide(
            project = simpleWebProject(),
            targetType = "release",
            requestedMode = HybridBuildMode.AUTO_SMART,
            cloudTokenConfigured = true,
            networkOverride = wifiOnline,
            deviceOverride = lowStorageDevice
        )
        assertEquals(SelectedBuildEngine.CLOUD_GRADLE_SDK, lowDiskDecision.selectedEngine)
        assertTrue(lowDiskDecision.warnings.any { it.contains("18MB") })
    }

    @Test
    fun `SmartBuildRouter falls back safely to Local when offline even under FORCE_CLOUD`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val router = SmartBuildRouter(context)

        val offlineForcedCloud = router.decide(
            project = nativeFeatureProject(),
            targetType = "aab",
            requestedMode = HybridBuildMode.FORCE_CLOUD,
            cloudTokenConfigured = true,
            networkOverride = offlineNet,
            deviceOverride = healthyDevice
        )
        assertEquals(SelectedBuildEngine.LOCAL_QUANTUM, offlineForcedCloud.selectedEngine)
        assertFalse(offlineForcedCloud.cloudExecutionPossibleNow)
        assertTrue(offlineForcedCloud.warnings.any { it.contains("اتصال") })
    }

    @Test
    fun `BuildCapabilitiesInspector reports honest local vs cloud capabilities without false native claims`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val profile = BuildCapabilitiesInspector.inspect(context, freeStorageMb = 512L)

        assertFalse(profile.hasExecutableNativeAapt2)
        assertFalse(profile.hasExecutableNativeD8)
        assertTrue(profile.capabilities.any { it.id == "apksig" && it.support == LocalCapabilitySupport.NATIVE_VERIFIED })
        assertTrue(profile.capabilities.any { it.id == "keyvault" && it.support == LocalCapabilitySupport.NATIVE_VERIFIED })
        assertTrue(profile.capabilities.any { it.id == "gradle_kotlin" && it.support == LocalCapabilitySupport.REQUIRES_CLOUD_SDK })
        assertTrue(profile.capabilities.any { it.id == "play_aab" && it.support == LocalCapabilitySupport.REQUIRES_CLOUD_SDK })
    }

    @Test
    fun `BuildLogSanitizer redacts GitHub tokens, Bearer headers, passwords, and PEM keys`() {
        val raw = """
            Authorization: Bearer ghp_1234567890abcdef123456
            token=github_pat_AbCdEf1234567890
            storePassword=superSecretStorePass
            SIGNING_KEYSTORE_BASE64:MIIKeQIBAzCC
            -----BEGIN PRIVATE KEY-----
            MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSj
            -----END PRIVATE KEY-----
        """.trimIndent()

        val redacted = BuildLogSanitizer.redactSensitive(raw)
        assertFalse(redacted.contains("ghp_1234567890"))
        assertFalse(redacted.contains("github_pat_AbCdEf"))
        assertFalse(redacted.contains("superSecretStorePass"))
        assertFalse(redacted.contains("MIIKeQIBAzCC"))
        assertFalse(redacted.contains("MIIEvQIBADAN"))
        assertTrue(redacted.contains("[REDACTED]"))
        assertTrue(redacted.contains("[REDACTED_PRIVATE_KEY]"))
    }

    @Test
    fun `GitHubCloudBuildEngine encrypts PAT and safely runs push, job failure parsing, and 302 CDN artifact download without auth leak`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("apkforge_github_ci_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("gh_token", "ghp_legacyPlainToken123456").commit()

        val jsonType = "application/json; charset=utf-8".toMediaType()
        val zipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(ZipEntry("app-release.aab"))
                zos.write("PK_DUMMY_AAB_BUNDLE".toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            baos.toByteArray()
        }

        var cdnRequestSawAuthHeader = false

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                val jsonResp: (Int, String) -> Response = { code, body ->
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(code)
                        .message("OK")
                        .body(body.toResponseBody(jsonType))
                        .build()
                }
                when {
                    url == "https://api.github.com/user" ->
                        jsonResp(200, """{"login":"dev-tester"}""")
                    url == "https://api.github.com/repos/dev-tester/my-app" ->
                        jsonResp(200, """{"default_branch":"main"}""")
                    url == "https://api.github.com/repos/dev-tester/my-app/git/ref/heads/main" ->
                        jsonResp(200, """{"object":{"sha":"commit_base_1"}}""")
                    url == "https://api.github.com/repos/dev-tester/my-app/git/commits/commit_base_1" ->
                        jsonResp(200, """{"tree":{"sha":"tree_base_1"}}""")
                    url == "https://api.github.com/repos/dev-tester/my-app/git/blobs" ->
                        jsonResp(201, """{"sha":"blob_1"}""")
                    url == "https://api.github.com/repos/dev-tester/my-app/git/trees" ->
                        jsonResp(201, """{"sha":"tree_new_1"}""")
                    url == "https://api.github.com/repos/dev-tester/my-app/git/commits" ->
                        jsonResp(201, """{"sha":"commit_new_1"}""")
                    url == "https://api.github.com/repos/dev-tester/my-app/git/refs/heads/main" ->
                        jsonResp(200, """{"ref":"refs/heads/main"}""")
                    url.endsWith("/actions/runs?per_page=1") ->
                        jsonResp(200, """{"workflow_runs":[{"id":991,"status":"completed","conclusion":"success","html_url":"https://github.com/dev-tester/my-app/actions/runs/991"}]}""")
                    url.endsWith("/actions/runs/991/jobs") ->
                        jsonResp(
                            200,
                            """{"jobs":[{"id":55,"name":"build-apk-and-aab","status":"completed","conclusion":"failure","steps":[{"name":"Checkout","status":"completed","conclusion":"success","number":1},{"name":"Build Signed Play AAB","status":"completed","conclusion":"failure","number":5}]}]}"""
                        )
                    url.endsWith("/actions/runs/991/artifacts") ->
                        jsonResp(
                            200,
                            """{"total_count":1,"artifacts":[{"id":700,"name":"my-app-AAB","size_in_bytes":128,"archive_download_url":"https://api.github.com/repos/dev-tester/my-app/actions/artifacts/700/zip","expired":false}]}"""
                        )
                    url.endsWith("/actions/artifacts/700/zip") ->
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(302)
                            .message("Found")
                            .header("Location", "https://pipelines.actions.githubusercontent.com/artifact.zip?sig=secret")
                            .body(ByteArray(0).toResponseBody(null))
                            .build()
                    url.startsWith("https://pipelines.actions.githubusercontent.com/artifact.zip") -> {
                        cdnRequestSawAuthHeader = req.header("Authorization") != null
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(zipBytes.toResponseBody("application/zip".toMediaType()))
                            .build()
                    }
                    else -> jsonResp(404, """{"message":"not found: $url"}""")
                }
            })
            .build()

        val engine = GitHubCloudBuildEngine(context, mockClient)
        // Migrates legacy plaintext token cleanly into AES-256-GCM ciphertext
        val migrated = engine.getSavedToken()
        assertEquals("ghp_legacyPlainToken123456", migrated)
        assertNull("Plaintext token must be purged from SharedPreferences", prefs.getString("gh_token", null))
        assertNotNull(prefs.getString("gh_token_cipher_b64", null))

        val exporter = ProjectExporter(context)
        val textEntries = exporter.buildProjectTextEntries(
            project = nativeFeatureProject(),
            forceCloudWorkflow = true,
            cloudBuildTarget = "aab"
        )
        val workflowYml = textEntries[".github/workflows/apkforge-build.yml"]!!
        assertTrue(workflowYml.contains("bundleRelease"))
        assertTrue(workflowYml.contains("SIGNING_KEYSTORE_BASE64"))
        assertFalse(textEntries.keys.any { it.endsWith(".jks") || it.endsWith(".keystore") })

        val triggerRes = engine.pushAndTriggerCloudBuild(
            token = migrated,
            repoName = "my-app",
            filesToPush = textEntries,
            binaryFiles = exporter.buildProjectBinaryEntries(nativeFeatureProject())
        ) {}
        assertTrue(triggerRes.isSuccess)
        val tr = triggerRes.getOrThrow()

        val jobs = engine.fetchWorkflowJobs(migrated, tr.owner, tr.repoName, 991L).getOrThrow()
        assertEquals(1, jobs.size)
        assertTrue(jobs.first().failedStepSummary!!.contains("Build Signed Play AAB"))

        val artifacts = engine.listRunArtifacts(migrated, tr.owner, tr.repoName, 991L).getOrThrow()
        assertEquals(1, artifacts.size)

        val outDir = File(context.cacheDir, "cloud_art_test_${System.currentTimeMillis()}")
        val dl = engine.downloadAndExtractArtifact(
            token = migrated,
            artifact = artifacts.first(),
            outputDir = outDir,
            preferExtension = "aab"
        ).getOrThrow()

        assertFalse("GitHub Authorization header MUST NOT be leaked to Azure CDN redirect URL", cdnRequestSawAuthHeader)
        assertEquals("app-release.aab", dl.fileName)
        assertTrue(dl.file.exists() && dl.file.length() > 0L)

        engine.clearCredentials()
        assertFalse(engine.hasSavedToken())
    }

    @Test
    fun `WebViewSafety enables software rendering layer on virtual GPU emulators and keeps hardware acceleration on real devices`() {
        assertTrue(
            WebViewSafety.isVirtualGpuEnvironment(
                fingerprint = "google/sdk_gphone64_x86_64/emu64x:14/UPB5/123:userdebug/dev-keys",
                model = "sdk_gphone64_x86_64",
                manufacturer = "Google",
                brand = "google",
                device = "emu64x",
                product = "sdk_gphone64_x86_64",
                hardware = "ranchu"
            )
        )
        assertTrue(
            WebViewSafety.isVirtualGpuEnvironment(
                fingerprint = "google/aosp_cf_x86_64_phone/vsoc_x86_64:14/AP1A/99:user/release-keys",
                model = "Cuttlefish x86_64 phone",
                manufacturer = "Google",
                brand = "google",
                device = "vsoc_x86_64",
                product = "aosp_cf_x86_64_phone",
                hardware = "cutf_cvm",
                board = "cutf",
                supportedAbis = listOf("x86_64", "arm64-v8a")
            )
        )
        assertFalse(
            WebViewSafety.isVirtualGpuEnvironment(
                fingerprint = "google/cheetah/cheetah:14/AP2A.240805.005/12025142:user/release-keys",
                model = "Pixel 7 Pro",
                manufacturer = "Google",
                brand = "google",
                device = "cheetah",
                product = "cheetah",
                hardware = "cheetah",
                board = "cheetah",
                supportedAbis = listOf("arm64-v8a", "armeabi-v7a")
            )
        )

        WebViewSafety.resetRendererSession()
        assertFalse(WebViewSafety.isSessionRendererBlocked())
        WebViewSafety.recordRendererCrash()
        assertFalse(WebViewSafety.isSessionRendererBlocked())
        WebViewSafety.recordRendererCrash()
        assertTrue(WebViewSafety.isSessionRendererBlocked())
        WebViewSafety.resetRendererSession()
        assertFalse(WebViewSafety.isSessionRendererBlocked())
    }

    @Test
    fun `LibsodiumSealedBox secretBoxEasy matches BouncyCastle XSalsa20 + Poly1305 and seal-open round-trips cleanly`() {
        val key = ByteArray(32) { (it * 7 + 3).toByte() }
        val nonce = ByteArray(24) { (it * 11 + 5).toByte() }
        val message = "ApkForge_Signing_Secret_Payload_2026".toByteArray(Charsets.UTF_8)

        // Independent BouncyCastle XSalsa20 + Poly1305 computation (NaCl secretbox specification)
        val engine = XSalsa20Engine()
        engine.init(true, ParametersWithIV(KeyParameter(key), nonce))
        val zerosAndMsg = ByteArray(32 + message.size)
        System.arraycopy(message, 0, zerosAndMsg, 32, message.size)
        val streamOut = ByteArray(zerosAndMsg.size)
        engine.processBytes(zerosAndMsg, 0, zerosAndMsg.size, streamOut, 0)
        val bcPolyKey = streamOut.copyOfRange(0, 32)
        val bcCiphertext = streamOut.copyOfRange(32, streamOut.size)
        val poly = Poly1305().apply { init(KeyParameter(bcPolyKey)) }
        poly.update(bcCiphertext, 0, bcCiphertext.size)
        val bcMac = ByteArray(16)
        poly.doFinal(bcMac, 0)
        val expectedBoxed = bcMac + bcCiphertext

        val actualBoxed = LibsodiumSealedBox.secretBoxEasy(message, nonce, key)
        assertArrayEquals("secretBoxEasy output must match XSalsa20-Poly1305 byte-for-byte", expectedBoxed, actualBoxed)

        val openedBox = LibsodiumSealedBox.secretBoxOpenEasy(actualBoxed, nonce, key)
        assertArrayEquals(message, openedBox)

        // Full X25519 crypto_box_seal / crypto_box_seal_open round-trip
        val priv = ByteArray(32)
        val pub = ByteArray(32)
        X25519.generatePrivateKey(SecureRandom(), priv)
        X25519.generatePublicKey(priv, 0, pub, 0)

        val sealed = LibsodiumSealedBox.seal(message, pub)
        assertEquals(48 + message.size, sealed.size)
        val unsealed = LibsodiumSealedBox.open(sealed, pub, priv)
        assertArrayEquals(message, unsealed)

        // Tampering any byte must fail Poly1305 MAC verification
        val tampered = sealed.copyOf()
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 0xFF).toByte()
        var failedMac = false
        try {
            LibsodiumSealedBox.open(tampered, pub, priv)
        } catch (_: SecurityException) {
            failedMac = true
        }
        assertTrue("Tampered sealed box must reject with SecurityException", failedMac)
    }

    @Test
    fun `QuantumKeyVault exportPkcs12Base64 produces valid PKCS12 keystore in memory without plaintext disk leaks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = QuantumKeyVault(context)
        val bundle = vault.exportPkcs12Base64(
            alias = "hybrid_release",
            vaultPassword = "vault_pass_2026",
            exportPassword = "export_pass_2026",
            organization = "ApkForge Test Org"
        )

        assertEquals("hybrid_release", bundle.keyAlias)
        assertEquals("export_pass_2026", bundle.storePassword)
        assertTrue(bundle.pkcs12Base64.isNotBlank())

        val rawP12 = Base64.getDecoder().decode(bundle.pkcs12Base64)
        val ks = KeyStore.getInstance("PKCS12")
        ks.load(ByteArrayInputStream(rawP12), bundle.storePassword.toCharArray())

        assertTrue(ks.containsAlias(bundle.keyAlias))
        val privKey = ks.getKey(bundle.keyAlias, bundle.keyPassword.toCharArray())
        assertNotNull(privKey)
        val cert = ks.getCertificate(bundle.keyAlias)
        assertNotNull(cert)
        val expectedSha256 = MessageDigest.getInstance("SHA-256").digest(cert.encoded).joinToString("") { "%02x".format(it) }
        assertEquals(expectedSha256, bundle.certSha256)

        val signingDir = File(context.filesDir, "apkforge_toolchain/signing")
        val unencryptedFiles = signingDir.listFiles()?.filter {
            it.extension.equals("p12", true) || it.extension.equals("jks", true) || it.name.endsWith(".pem", true)
        }.orEmpty()
        assertTrue("No unencrypted keystore/PEM files should be written to disk", unencryptedFiles.isEmpty())
    }

    @Test
    fun `GitHubCloudBuildEngine encrypts and uploads all 4 signing secrets via Libsodium and extracts redacted Gradle failure logs without auth leak`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val recipientPriv = ByteArray(32)
        val recipientPub = ByteArray(32)
        X25519.generatePrivateKey(SecureRandom(), recipientPriv)
        X25519.generatePublicKey(recipientPriv, 0, recipientPub, 0)
        val pubB64 = Base64.getEncoder().encodeToString(recipientPub)

        val uploadedSecrets = linkedMapOf<String, JSONObject>()
        var logCdnSawAuthHeader = false
        val jsonType = "application/json; charset=utf-8".toMediaType()

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                val jsonResp: (Int, String) -> Response = { code, body ->
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(code)
                        .message("OK")
                        .body(body.toResponseBody(jsonType))
                        .build()
                }
                when {
                    url == "https://api.github.com/user" ->
                        jsonResp(200, """{"login":"dev-tester"}""")
                    url == "https://api.github.com/repos/dev-tester/my-app" ->
                        jsonResp(200, """{"default_branch":"main"}""")
                    url == "https://api.github.com/repos/dev-tester/my-app/actions/secrets/public-key" ->
                        jsonResp(200, """{"key_id":"key_987654","key":"$pubB64"}""")
                    url.startsWith("https://api.github.com/repos/dev-tester/my-app/actions/secrets/") && req.method == "PUT" -> {
                        val name = url.substringAfterLast("/")
                        val buf = Buffer()
                        req.body?.writeTo(buf)
                        uploadedSecrets[name] = JSONObject(buf.readUtf8())
                        jsonResp(204, "")
                    }
                    url == "https://api.github.com/repos/dev-tester/my-app/actions/jobs/55/logs" ->
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(302)
                            .message("Found")
                            .header("Location", "https://productionresultssa0.blob.core.windows.net/logs/job55.txt?sig=abc")
                            .body(ByteArray(0).toResponseBody(null))
                            .build()
                    url.startsWith("https://productionresultssa0.blob.core.windows.net/logs/job55.txt") -> {
                        logCdnSawAuthHeader = req.header("Authorization") != null
                        val fakeLog = """
                            2026-11-16T05:00:01Z Authorization: Bearer ghp_leakTokenMustBeRedacted99
                            2026-11-16T05:00:02Z > Task :app:compileReleaseKotlin
                            2026-11-16T05:00:03Z e: file:///app/src/main/java/MainActivity.kt:42:5 Unresolved reference: brokenSymbol
                            2026-11-16T05:00:04Z FAILURE: Build failed with an exception.
                            2026-11-16T05:00:04Z * What went wrong:
                            2026-11-16T05:00:04Z Execution failed for task ':app:compileReleaseKotlin'.
                            2026-11-16T05:00:04Z > Compilation error. See log for more details
                        """.trimIndent()
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(fakeLog.toResponseBody("text/plain".toMediaType()))
                            .build()
                    }
                    else -> jsonResp(404, """{"message":"not found: $url"}""")
                }
            })
            .build()

        val engine = GitHubCloudBuildEngine(context, mockClient)
        val syncResult = engine.syncProjectSigningSecrets(
            token = "ghp_testSecretSyncToken001",
            repoName = "my-app",
            keyAlias = "cloud_key",
            organization = "HybridShop Studio",
            vaultPassword = "vault_test_pw",
            exportPassword = "ci_store_pw"
        ).getOrThrow()

        assertEquals(
            listOf("SIGNING_KEYSTORE_BASE64", "SIGNING_STORE_PASSWORD", "SIGNING_KEY_ALIAS", "SIGNING_KEY_PASSWORD"),
            syncResult.syncedSecrets
        )
        assertTrue(engine.isSigningSecretsSynced("my-app"))
        assertEquals(syncResult.certSha256, engine.getSyncedCertFingerprint("my-app"))

        // Decrypt all 4 secrets with recipient private key to verify end-to-end Libsodium wire format
        fun decryptSecret(name: String): String {
            val obj = uploadedSecrets[name]!!
            assertEquals("key_987654", obj.getString("key_id"))
            val sealed = Base64.getDecoder().decode(obj.getString("encrypted_value"))
            return String(LibsodiumSealedBox.open(sealed, recipientPub, recipientPriv), Charsets.UTF_8)
        }

        val decryptedB64 = decryptSecret("SIGNING_KEYSTORE_BASE64")
        val decryptedStorePw = decryptSecret("SIGNING_STORE_PASSWORD")
        val decryptedAlias = decryptSecret("SIGNING_KEY_ALIAS")
        val decryptedKeyPw = decryptSecret("SIGNING_KEY_PASSWORD")

        assertEquals("ci_store_pw", decryptedStorePw)
        assertEquals("cloud_key", decryptedAlias)
        assertEquals("ci_store_pw", decryptedKeyPw)

        val ks = KeyStore.getInstance("PKCS12")
        ks.load(ByteArrayInputStream(Base64.getDecoder().decode(decryptedB64)), decryptedStorePw.toCharArray())
        assertTrue(ks.containsAlias(decryptedAlias))

        // Verify failure log excerpt fetching
        val excerpt = engine.fetchJobFailureLogExcerpt(
            token = "ghp_testSecretSyncToken001",
            owner = "dev-tester",
            repoName = "my-app",
            jobId = 55L
        ).getOrThrow()

        assertFalse("Job logs redirect must NEVER leak GitHub Bearer header", logCdnSawAuthHeader)
        assertFalse("Tokens inside CI logs must be redacted", excerpt.contains("ghp_leakTokenMustBeRedacted99"))
        assertTrue(excerpt.contains("FAILURE: Build failed with an exception."))
        assertTrue(excerpt.contains("Execution failed for task ':app:compileReleaseKotlin'."))
        assertTrue(excerpt.contains("Unresolved reference: brokenSymbol"))
    }

    @Test
    fun `GitHubCloudBuildEngine verifyCloudBuildSetup detects missing workflow scope and missing remote GitHub secrets`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jsonType = "application/json; charset=utf-8".toMediaType()
        var oauthScopesHeader = "repo"
        var remoteSecretsJson = """{"total_count":2,"secrets":[{"name":"SIGNING_KEYSTORE_BASE64"},{"name":"SIGNING_KEY_ALIAS"}]}"""

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when (url) {
                    "https://api.github.com/user" ->
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("X-OAuth-Scopes", oauthScopesHeader)
                            .body("""{"login":"cloud-dev"}""".toResponseBody(jsonType))
                            .build()
                    "https://api.github.com/repos/cloud-dev/preflight-app" ->
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("""{"default_branch":"main"}""".toResponseBody(jsonType))
                            .build()
                    "https://api.github.com/repos/cloud-dev/preflight-app/actions/secrets" ->
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body(remoteSecretsJson.toResponseBody(jsonType))
                            .build()
                    else ->
                        Response.Builder()
                            .request(req)
                            .protocol(Protocol.HTTP_1_1)
                            .code(404)
                            .message("Not Found")
                            .body("{}".toResponseBody(jsonType))
                            .build()
                }
            })
            .build()

        val engine = GitHubCloudBuildEngine(context, mockClient)
        val warnReport = engine.verifyCloudBuildSetup(
            token = "ghp_preflightToken1234",
            repoName = "preflight-app",
            keyAlias = "preflight_key",
            organization = "ApkForge QA"
        ).getOrThrow()

        assertEquals("cloud-dev", warnReport.owner)
        assertTrue(warnReport.repoExists)
        assertTrue(warnReport.hasWorkflowScopeWarning)
        assertFalse(warnReport.allSigningSecretsReadyOnGitHub)
        assertEquals(listOf("SIGNING_STORE_PASSWORD", "SIGNING_KEY_PASSWORD"), warnReport.missingSigningSecrets)
        assertEquals(64, warnReport.localVaultSha256.length)
        assertEquals(40, warnReport.localVaultSha1.length)

        // Now simulate full scopes and all 4 secrets present
        oauthScopesHeader = "repo, workflow"
        remoteSecretsJson = """{"total_count":4,"secrets":[{"name":"SIGNING_KEYSTORE_BASE64"},{"name":"SIGNING_STORE_PASSWORD"},{"name":"SIGNING_KEY_ALIAS"},{"name":"SIGNING_KEY_PASSWORD"}]}"""

        val readyReport = engine.verifyCloudBuildSetup(
            token = "ghp_preflightToken1234",
            repoName = "preflight-app",
            keyAlias = "preflight_key",
            organization = "ApkForge QA"
        ).getOrThrow()

        assertFalse(readyReport.hasWorkflowScopeWarning)
        assertTrue(readyReport.allSigningSecretsReadyOnGitHub)
        assertTrue(readyReport.missingSigningSecrets.isEmpty())
        assertTrue(engine.isSigningSecretsSynced("preflight-app"))
    }

    @Test
    fun `inspectDownloadedArtifactSignature extracts X509 SHA256 and SHA1 fingerprints and matches QuantumKeyVault certificate`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = QuantumKeyVault(context)
        val material = vault.ensureAndUnlockKeyMaterial(
            alias = "artifact_verify_key",
            organization = "ApkForge Sign Verify"
        )

        val tempDir = File(context.cacheDir, "apk_sig_verify_${System.currentTimeMillis()}").apply { mkdirs() }
        val unsignedApk = File(tempDir, "unsigned.apk")
        val signedApk = File(tempDir, "cloud-release-signed.apk")

        ZipOutputStream(FileOutputStream(unsignedApk)).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write("BIN_MANIFEST_TEST".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write("dex\n035\u0000DUMMY_DEX_BYTES".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }

        val signerConfig = ApkSigner.SignerConfig.Builder(
            material.alias,
            material.privateKey,
            listOf(material.certificate)
        ).build()

        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsignedApk)
            .setOutputApk(signedApk)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setMinSdkVersion(21)
            .build()
            .sign()

        val engine = GitHubCloudBuildEngine(context)
        val matched = engine.inspectDownloadedArtifactSignature(
            file = signedApk,
            expectedVaultCertSha256 = material.sha256Fingerprint
        )

        assertTrue(matched.isVerified)
        assertTrue(matched.v2Verified)
        assertEquals(material.sha256Fingerprint, matched.signerCertSha256)
        assertEquals(material.sha1Fingerprint, matched.signerCertSha1)
        assertTrue(matched.signerSubject.contains("ApkForge Sign Verify"))
        assertEquals(true, matched.matchesExpectedCert)
        assertTrue(matched.verdictAr.contains("Quantum"))

        val mismatched = engine.inspectDownloadedArtifactSignature(
            file = signedApk,
            expectedVaultCertSha256 = "0".repeat(64)
        )
        assertEquals(false, mismatched.matchesExpectedCert)
        assertTrue(mismatched.verdictAr.contains("مختلفة"))

        val toolchainReport = ToolchainManager(context).inspectApkOrZipFile(signedApk.absolutePath)
        val expectedColonSha1 = QuantumKeyVault.formatFingerprintColon(material.sha1Fingerprint)
        assertTrue(toolchainReport.contains(expectedColonSha1))
    }

    @Test
    fun `ProjectExporter emits Jetpack Compose Material 3 Gradle build and Theme for KOTLIN_TEMPLATE and AI_SCRATCH`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val exporter = ProjectExporter(context)
        val router = SmartBuildRouter(context)

        val composeProject = AppProject(
            id = "proj_compose_1",
            name = "SmartNotes",
            description = "تطبيق ملاحظات ذكي",
            primaryColorHex = "#00E5FF",
            accentColorHex = "#7C3AED",
            sourceType = SourceType.KOTLIN_TEMPLATE,
            sourceValue = "compose_template",
            proConfig = ProStudioConfig(drawerEnabled = true, fabMenuEnabled = true),
            omniConfig = com.example.domain.model.OmniIdeConfig(
                authBuilderEnabled = true,
                localDbEnabled = true,
                localDbTableName = "smart_notes"
            ),
            packageConfig = PackageConfig(packageName = "com.apkforge.smartnotes", targetSdk = 35)
        )

        val entries = exporter.buildProjectTextEntries(composeProject)
        val gradleKts = entries["app/build.gradle.kts"].orEmpty()
        val themeKt = entries["app/src/main/java/com/apkforge/smartnotes/ui/theme/Theme.kt"].orEmpty()
        val mainActivityKt = entries["app/src/main/java/com/apkforge/smartnotes/MainActivity.kt"].orEmpty()

        assertTrue(gradleKts.contains("compose = true"))
        assertTrue(gradleKts.contains("kotlinCompilerExtensionVersion = \"1.5.14\""))
        assertTrue(gradleKts.contains("androidx.compose:compose-bom:2024.06.00"))
        assertTrue(gradleKts.contains("androidx.compose.material3:material3"))

        assertTrue(themeKt.contains("fun ApkForgeTheme"))
        assertTrue(themeKt.contains("#7C3AED"))

        assertTrue(mainActivityKt.contains("class MainActivity : ComponentActivity()"))
        assertTrue(mainActivityKt.contains("setContent"))
        assertTrue(mainActivityKt.contains("ModalNavigationDrawer"))
        assertTrue(mainActivityKt.contains("smart_notes_rows"))

        val aiScratchProject = composeProject.copy(
            id = "proj_ai_1",
            sourceType = SourceType.AI_SCRATCH,
            sourceValue = "index.html"
        )
        val aiEntries = exporter.buildProjectTextEntries(aiScratchProject)
        assertTrue(aiEntries["app/build.gradle.kts"]!!.contains("compose = true"))
        assertTrue(aiEntries.containsKey("app/src/main/java/com/apkforge/smartnotes/ui/theme/Theme.kt"))

        val aiDecision = router.decide(
            project = aiScratchProject,
            targetType = "release",
            requestedMode = HybridBuildMode.AUTO_SMART,
            cloudTokenConfigured = true,
            networkOverride = wifiOnline,
            deviceOverride = healthyDevice
        )
        assertEquals(SelectedBuildEngine.CLOUD_GRADLE_SDK, aiDecision.selectedEngine)
        assertTrue(aiDecision.cloudReasons.any { it.contains("JetpackComposeAiApp") })
    }

    @Test
    fun `GitHubCloudBuildEngine incremental Git tree sync reuses unchanged blobs and skips redundant POST blobs`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jsonType = "application/json; charset=utf-8".toMediaType()

        val unchangedBinary = "PNG_UNCHANGED_ICON_DATA".toByteArray(Charsets.UTF_8)
        val changedBinary = "PNG_MODIFIED_ICON_DATA_V2".toByteArray(Charsets.UTF_8)
        val unchangedText = "rootProject.name = \"App\""
        val changedText = "println(\"updated v2\")"

        val helperEngine = GitHubCloudBuildEngine(context)
        val unchangedBinarySha = helperEngine.computeGitBlobSha1(unchangedBinary)
        val unchangedTextSha = helperEngine.computeGitBlobSha1(unchangedText.toByteArray(Charsets.UTF_8))

        val remoteTreeJson = """
            {
              "sha": "tree_prev",
              "truncated": false,
              "tree": [
                {"path": "res/icon_same.png", "type": "blob", "sha": "$unchangedBinarySha"},
                {"path": "res/icon_changed.png", "type": "blob", "sha": "0000000000000000000000000000000000000000"},
                {"path": "settings.gradle.kts", "type": "blob", "sha": "$unchangedTextSha"}
              ]
            }
        """.trimIndent()

        var blobUploadCalls = 0
        var postedTreePaths = emptyList<String>()
        var treePostCalls = 0

        val mockClient = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                val jsonResp: (Int, String) -> Response = { code, body ->
                    Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(code)
                        .message("OK")
                        .body(body.toResponseBody(jsonType))
                        .build()
                }
                when {
                    url == "https://api.github.com/user" -> jsonResp(200, """{"login":"diff-dev"}""")
                    url == "https://api.github.com/repos/diff-dev/diff-repo" -> jsonResp(200, """{"default_branch":"main"}""")
                    url == "https://api.github.com/repos/diff-dev/diff-repo/git/ref/heads/main" ->
                        jsonResp(200, """{"object":{"sha":"commit_prev"}}""")
                    url == "https://api.github.com/repos/diff-dev/diff-repo/git/commits/commit_prev" ->
                        jsonResp(200, """{"tree":{"sha":"tree_prev"}}""")
                    url == "https://api.github.com/repos/diff-dev/diff-repo/git/trees/tree_prev?recursive=1" ->
                        jsonResp(200, remoteTreeJson)
                    url == "https://api.github.com/repos/diff-dev/diff-repo/git/blobs" -> {
                        blobUploadCalls++
                        jsonResp(201, """{"sha":"blob_new_$blobUploadCalls"}""")
                    }
                    url == "https://api.github.com/repos/diff-dev/diff-repo/git/trees" -> {
                        treePostCalls++
                        val buf = Buffer()
                        req.body?.writeTo(buf)
                        val bodyJson = JSONObject(buf.readUtf8())
                        val arr = bodyJson.getJSONArray("tree")
                        postedTreePaths = (0 until arr.length()).map { idx -> arr.getJSONObject(idx).getString("path") }
                        jsonResp(201, """{"sha":"tree_next"}""")
                    }
                    url == "https://api.github.com/repos/diff-dev/diff-repo/git/commits" ->
                        jsonResp(201, """{"sha":"commit_next"}""")
                    url == "https://api.github.com/repos/diff-dev/diff-repo/git/refs/heads/main" ->
                        jsonResp(200, """{"ref":"refs/heads/main"}""")
                    else -> jsonResp(404, "{}")
                }
            })
            .build()

        val engine = GitHubCloudBuildEngine(context, mockClient)
        val diffRes = engine.pushAndTriggerCloudBuild(
            token = "ghp_diffSyncToken12345",
            repoName = "diff-repo",
            filesToPush = mapOf(
                "settings.gradle.kts" to unchangedText,
                "MainActivity.kt" to changedText
            ),
            binaryFiles = mapOf(
                "res/icon_same.png" to unchangedBinary,
                "res/icon_changed.png" to changedBinary
            )
        ) {}.getOrThrow()

        assertEquals(1, blobUploadCalls) // Only icon_changed.png uploaded!
        assertEquals(2, diffRes.uploadedFilesCount) // icon_changed.png + MainActivity.kt
        assertEquals(2, diffRes.reusedUnchangedFilesCount) // icon_same.png + settings.gradle.kts
        assertEquals(1, treePostCalls)
        assertEquals(setOf("res/icon_changed.png", "MainActivity.kt"), postedTreePaths.toSet())

        // Now verify an all-unchanged rebuild triggers CI cleanly with 0 blob uploads and 0 POST /git/trees calls
        blobUploadCalls = 0
        treePostCalls = 0
        val allSameRes = engine.pushAndTriggerCloudBuild(
            token = "ghp_diffSyncToken12345",
            repoName = "diff-repo",
            filesToPush = mapOf("settings.gradle.kts" to unchangedText),
            binaryFiles = mapOf("res/icon_same.png" to unchangedBinary)
        ) {}.getOrThrow()

        assertEquals(0, blobUploadCalls)
        assertEquals(0, treePostCalls)
        assertEquals(0, allSameRes.uploadedFilesCount)
        assertEquals(2, allSameRes.reusedUnchangedFilesCount)
    }
}
