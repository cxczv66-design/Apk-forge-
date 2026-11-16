package com.example.engine

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.android.apksig.ApkVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class GitHubBuildTriggerResult(
    val owner: String,
    val repoName: String,
    val repoHtmlUrl: String,
    val actionsUrl: String,
    val uploadedFilesCount: Int,
    val statusMessage: String,
    val reusedUnchangedFilesCount: Int = 0
)

data class GitHubWorkflowRunInfo(
    val runId: Long,
    val status: String, // queued, in_progress, completed
    val conclusion: String?, // success, failure, cancelled, null
    val htmlUrl: String,
    val artifactCount: Int
)

data class GitHubWorkflowStepInfo(
    val name: String,
    val status: String,
    val conclusion: String?,
    val number: Int
)

data class GitHubWorkflowJobInfo(
    val jobId: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val steps: List<GitHubWorkflowStepInfo>,
    val failedStepSummary: String?
)

data class GitHubArtifactItem(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val archiveDownloadUrl: String,
    val expired: Boolean
)

data class DownloadedCloudArtifact(
    val file: File,
    val fileName: String,
    val sizeBytes: Long,
    val isApkVerified: Boolean,
    val v1Verified: Boolean,
    val v2Verified: Boolean,
    val v3Verified: Boolean,
    val sha256: String,
    val signerCertSha256: String = "",
    val signerCertSha1: String = "",
    val signerSubject: String = "",
    val matchesVaultCert: Boolean? = null,
    val signingModeLabelAr: String = ""
)

data class ArtifactSignatureReport(
    val isVerified: Boolean,
    val v1Verified: Boolean = false,
    val v2Verified: Boolean = false,
    val v3Verified: Boolean = false,
    val signerCertSha256: String = "",
    val signerCertSha1: String = "",
    val signerSubject: String = "",
    val matchesExpectedCert: Boolean? = null,
    val verdictAr: String = ""
)

data class GitHubCloudPreflightReport(
    val owner: String,
    val repoName: String,
    val repoExists: Boolean,
    val tokenScopes: List<String>,
    val hasWorkflowScopeWarning: Boolean,
    val remoteSecretNames: List<String>,
    val missingSigningSecrets: List<String>,
    val allSigningSecretsReadyOnGitHub: Boolean,
    val localVaultSha256: String,
    val localVaultSha1: String,
    val summaryAr: String
)

data class GitHubActionsPublicKey(
    val keyId: String,
    val keyBase64: String
)

data class GitHubSecretsSyncResult(
    val owner: String,
    val repoName: String,
    val syncedSecrets: List<String>,
    val certSha256: String,
    val keyId: String,
    val certSha1: String = ""
)

class GitHubCloudBuildEngine(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    private val prefs = context.getSharedPreferences("apkforge_github_ci_prefs", Context.MODE_PRIVATE)
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val patKeyAlias = "apkforge_github_pat_aes_v1"

    fun hasSavedToken(): Boolean = getSavedToken().isNotBlank()

    fun getTokenProtectionMode(): String {
        val storedMode = prefs.getString("gh_token_mode", null)
        if (!storedMode.isNullOrBlank()) return storedMode
        return if (getOrCreateKeystoreSecretKey() != null) {
            "ANDROID_KEYSTORE_AES256_GCM"
        } else {
            "PBKDF2_AES256_GCM"
        }
    }

    fun getSavedToken(): String {
        // Auto-migrate any legacy plaintext token into encrypted AES-256-GCM immediately
        val legacyPlain = prefs.getString("gh_token", null)
        if (!legacyPlain.isNullOrBlank()) {
            saveCredentials(legacyPlain, getSavedRepoName())
            prefs.edit().remove("gh_token").apply()
            return legacyPlain.trim()
        }

        val cipherB64 = prefs.getString("gh_token_cipher_b64", null) ?: return ""
        val ivB64 = prefs.getString("gh_token_iv_b64", null) ?: return ""
        val mode = prefs.getString("gh_token_mode", "PBKDF2_AES256_GCM") ?: "PBKDF2_AES256_GCM"
        return runCatching {
            val cipherBytes = Base64.decode(cipherB64, Base64.DEFAULT)
            val iv = Base64.decode(ivB64, Base64.DEFAULT)
            val key = if (mode == "ANDROID_KEYSTORE_AES256_GCM") {
                getOrCreateKeystoreSecretKey() ?: throw IllegalStateException("KeyStore key unavailable")
            } else {
                val saltB64 = prefs.getString("gh_token_salt_b64", null)
                    ?: throw IllegalStateException("Missing salt")
                derivePbkdf2Key(Base64.decode(saltB64, Base64.DEFAULT))
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun getSavedRepoName(): String = prefs.getString("gh_repo", "apkforge-cloud-build") ?: "apkforge-cloud-build"

    fun saveCredentials(token: String, repoName: String) {
        val cleanToken = token.trim()
        val cleanRepo = repoName.trim().ifBlank { "apkforge-cloud-build" }
        val editor = prefs.edit()
            .remove("gh_token") // Strictly never persist plaintext token
            .putString("gh_repo", cleanRepo)

        if (cleanToken.isBlank()) {
            editor
                .remove("gh_token_cipher_b64")
                .remove("gh_token_iv_b64")
                .remove("gh_token_salt_b64")
                .remove("gh_token_mode")
                .apply()
            return
        }

        val plainBytes = cleanToken.toByteArray(Charsets.UTF_8)
        val hwKey = getOrCreateKeystoreSecretKey()
        if (hwKey != null) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, hwKey)
            val encrypted = cipher.doFinal(plainBytes)
            editor
                .putString("gh_token_cipher_b64", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString("gh_token_iv_b64", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .remove("gh_token_salt_b64")
                .putString("gh_token_mode", "ANDROID_KEYSTORE_AES256_GCM")
                .apply()
        } else {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
            val key = derivePbkdf2Key(salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val encrypted = cipher.doFinal(plainBytes)
            editor
                .putString("gh_token_cipher_b64", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString("gh_token_iv_b64", Base64.encodeToString(iv, Base64.NO_WRAP))
                .putString("gh_token_salt_b64", Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString("gh_token_mode", "PBKDF2_AES256_GCM")
                .apply()
        }
        plainBytes.fill(0)
    }

    fun clearCredentials() {
        prefs.edit()
            .remove("gh_token")
            .remove("gh_token_cipher_b64")
            .remove("gh_token_iv_b64")
            .remove("gh_token_salt_b64")
            .remove("gh_token_mode")
            .apply()
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            val existing = ks.getKey(patKeyAlias, null) as? SecretKey
            if (existing != null) return existing
            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                patKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    private fun derivePbkdf2Key(salt: ByteArray): SecretKey {
        val secretInput = "${context.packageName}|ApkForge_GitHub_Vault_v1".toCharArray()
        val spec = PBEKeySpec(secretInput, salt, 65_536, 256)
        val skf = runCatching {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        }.getOrElse {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        }
        val keyBytes = skf.generateSecret(spec).encoded
        secretInput.fill('\u0000')
        return SecretKeySpec(keyBytes, "AES")
    }

    fun isSigningSecretsSynced(repoName: String): Boolean {
        val clean = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
        return prefs.getBoolean("secrets_synced_$clean", false)
    }

    fun getSyncedCertFingerprint(repoName: String): String {
        val clean = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
        return prefs.getString("secrets_cert_$clean", "").orEmpty()
    }

    suspend fun ensureCloudRepository(
        token: String,
        repoName: String,
        onProgress: (String) -> Unit = {}
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            val cleanRepo = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
            if (cleanToken.isBlank() || cleanRepo.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال رمز GitHub Token واسم المستودع"))
            }

            fun safeProgress(msg: String) {
                onProgress(BuildLogSanitizer.redactSensitive(msg))
            }

            safeProgress("🔐 التحقق من هوية حساب GitHub...")
            val userReq = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .build()

            val ownerLogin = client.newCall(userReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("فشل التحقق من Token (رمز HTTP ${resp.code})"))
                }
                JSONObject(resp.body?.string().orEmpty()).getString("login")
            }

            safeProgress("📦 تجهيز المستودع السحابي: $ownerLogin/$cleanRepo ...")
            val checkRepoReq = Request.Builder()
                .url("https://api.github.com/repos/$ownerLogin/$cleanRepo")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .build()

            var defaultBranch = "main"
            val repoExists = client.newCall(checkRepoReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    defaultBranch = JSONObject(resp.body?.string().orEmpty())
                        .optString("default_branch", "main")
                        .ifBlank { "main" }
                    true
                } else {
                    false
                }
            }
            if (!repoExists) {
                val createBody = JSONObject().apply {
                    put("name", cleanRepo)
                    put("private", true)
                    put("auto_init", true)
                    put("description", "Automated Android Gradle + SDK build generated by ApkForge")
                }.toString().toRequestBody(jsonMedia)

                val createReq = Request.Builder()
                    .url("https://api.github.com/user/repos")
                    .post(createBody)
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                client.newCall(createReq).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("تعذر إنشاء المستودع (${resp.code})"))
                    }
                    val createdJson = JSONObject(resp.body?.string().orEmpty())
                    defaultBranch = createdJson.optString("default_branch", "main").ifBlank { "main" }
                }
            }

            Result.success(ownerLogin to defaultBranch)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Error preparing repository"), e))
        }
    }

    suspend fun fetchRepoPublicKey(
        token: String,
        owner: String,
        repoName: String
    ): Result<GitHubActionsPublicKey> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repoName/actions/secrets/public-key")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("تعذر جلب مفتاح Libsodium العام للمستودع (HTTP ${resp.code})"))
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val keyId = json.getString("key_id")
                val keyB64 = json.getString("key")
                Result.success(GitHubActionsPublicKey(keyId = keyId, keyBase64 = keyB64))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Public key fetch failed"), e))
        }
    }

    suspend fun putEncryptedRepoSecret(
        token: String,
        owner: String,
        repoName: String,
        secretName: String,
        secretPlaintext: String,
        publicKey: GitHubActionsPublicKey
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val recipientPubBytes = Base64.decode(publicKey.keyBase64, Base64.DEFAULT)
            val plainBytes = secretPlaintext.toByteArray(Charsets.UTF_8)
            val sealed = try {
                LibsodiumSealedBox.seal(plainBytes, recipientPubBytes)
            } finally {
                plainBytes.fill(0)
            }
            val encryptedB64 = Base64.encodeToString(sealed, Base64.NO_WRAP)

            val bodyJson = JSONObject().apply {
                put("encrypted_value", encryptedB64)
                put("key_id", publicKey.keyId)
            }.toString().toRequestBody(jsonMedia)

            val req = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repoName/actions/secrets/$secretName")
                .put(bodyJson)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("تعذر رفع السر المشفر $secretName (HTTP ${resp.code})"))
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Secret upload error"), e))
        }
    }

    /**
     * يصدّر مفتاح التوقيع من خزنة Quantum المشفّرة (PKCS#12 Base64 في الذاكرة فقط)
     * ويشفره بمفتاح Libsodium العام للمستودع (crypto_box_seal) لرفعه كـ GitHub Actions Secrets دون كشف أي نص.
     */
    suspend fun syncProjectSigningSecrets(
        token: String,
        repoName: String,
        keyAlias: String,
        organization: String,
        vaultPassword: String = "quantum_apkforge_2026",
        exportPassword: String = "quantum_apkforge_2026",
        onProgress: (String) -> Unit = {}
    ): Result<GitHubSecretsSyncResult> = withContext(Dispatchers.IO) {
        try {
            val cleanRepo = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
            val (owner, _) = ensureCloudRepository(token, cleanRepo, onProgress).getOrElse {
                return@withContext Result.failure(it)
            }
            onProgress("🔐 تجهيز أرشيف التوقيع PKCS#12 من خزنة Quantum في الذاكرة...")
            val vault = QuantumKeyVault(context)
            val bundle = vault.exportPkcs12Base64(
                alias = keyAlias.ifBlank { "apkforge_release" },
                vaultPassword = vaultPassword,
                exportPassword = exportPassword,
                organization = organization
            )

            onProgress("🔑 جلب مفتاح التشفير العام للمستودع (Libsodium SealedBox)...")
            val pubKey = fetchRepoPublicKey(token, owner, cleanRepo).getOrElse {
                return@withContext Result.failure(it)
            }

            val secretPairs = listOf(
                "SIGNING_KEYSTORE_BASE64" to bundle.pkcs12Base64,
                "SIGNING_STORE_PASSWORD" to bundle.storePassword,
                "SIGNING_KEY_ALIAS" to bundle.keyAlias,
                "SIGNING_KEY_PASSWORD" to bundle.keyPassword
            )

            val syncedNames = mutableListOf<String>()
            for ((name, value) in secretPairs) {
                onProgress("🛡️ تشفير ورفع $name ...")
                putEncryptedRepoSecret(token, owner, cleanRepo, name, value, pubKey).getOrElse {
                    return@withContext Result.failure(it)
                }
                syncedNames.add(name)
            }

            prefs.edit()
                .putBoolean("secrets_synced_$cleanRepo", true)
                .putString("secrets_cert_$cleanRepo", bundle.certSha256)
                .putString("secrets_cert_sha1_$cleanRepo", bundle.certSha1)
                .apply()

            onProgress("✅ تمت مزامنة ${syncedNames.size} أسرار توقيع مشفرة إلى $owner/$cleanRepo")
            Result.success(
                GitHubSecretsSyncResult(
                    owner = owner,
                    repoName = cleanRepo,
                    syncedSecrets = syncedNames,
                    certSha256 = bundle.certSha256,
                    keyId = pubKey.keyId,
                    certSha1 = bundle.certSha1
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Secrets sync error"), e))
        }
    }

    /**
     * فحص جاهزية الاتصال السحابي: التحقق من هوية GitHub وصلاحيات PAT (`X-OAuth-Scopes`)
     * ووجود أسرار التوقيع الأربعة فعلياً داخل GitHub Actions Secrets قبل بدء البناء.
     */
    suspend fun verifyCloudBuildSetup(
        token: String,
        repoName: String,
        keyAlias: String = "apkforge_release",
        organization: String = "ApkForge Studio"
    ): Result<GitHubCloudPreflightReport> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            val cleanRepo = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
            if (cleanToken.isBlank() || cleanRepo.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال رمز GitHub Token واسم المستودع أولاً"))
            }

            val userReq = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .build()

            var tokenScopes = emptyList<String>()
            var hasScopesHeader = false
            val ownerLogin = client.newCall(userReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("فشل التحقق من Token (HTTP ${resp.code})"))
                }
                val scopesHeader = resp.header("X-OAuth-Scopes").orEmpty()
                if (scopesHeader.isNotBlank()) {
                    hasScopesHeader = true
                    tokenScopes = scopesHeader.split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                JSONObject(resp.body?.string().orEmpty()).getString("login")
            }

            val workflowWarn = hasScopesHeader && tokenScopes.none { it.equals("workflow", ignoreCase = true) }

            val repoReq = Request.Builder()
                .url("https://api.github.com/repos/$ownerLogin/$cleanRepo")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .build()
            val repoExists = client.newCall(repoReq).execute().use { it.isSuccessful }

            val remoteSecrets = mutableListOf<String>()
            if (repoExists) {
                val secretsReq = Request.Builder()
                    .url("https://api.github.com/repos/$ownerLogin/$cleanRepo/actions/secrets")
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github+json")
                    .build()
                client.newCall(secretsReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val arr = JSONObject(resp.body?.string().orEmpty()).optJSONArray("secrets") ?: JSONArray()
                        for (i in 0 until arr.length()) {
                            val name = arr.optJSONObject(i)?.optString("name").orEmpty()
                            if (name.isNotBlank()) remoteSecrets.add(name)
                        }
                    }
                }
            }

            val requiredSecrets = listOf(
                "SIGNING_KEYSTORE_BASE64",
                "SIGNING_STORE_PASSWORD",
                "SIGNING_KEY_ALIAS",
                "SIGNING_KEY_PASSWORD"
            )
            val missingSecrets = requiredSecrets.filterNot { reqName ->
                remoteSecrets.any { it.equals(reqName, ignoreCase = false) }
            }
            val allReady = repoExists && missingSecrets.isEmpty()

            val material = QuantumKeyVault(context).ensureAndUnlockKeyMaterial(
                alias = keyAlias.ifBlank { "apkforge_release" },
                organization = organization.ifBlank { "ApkForge Studio" }
            )

            if (allReady) {
                prefs.edit()
                    .putBoolean("secrets_synced_$cleanRepo", true)
                    .putString("secrets_cert_$cleanRepo", material.sha256Fingerprint)
                    .putString("secrets_cert_sha1_$cleanRepo", material.sha1Fingerprint)
                    .apply()
            }

            val summary = buildString {
                append("الحساب: $ownerLogin")
                if (tokenScopes.isNotEmpty()) append(" • الصلاحيات: ${tokenScopes.joinToString(", ")}")
                if (workflowWarn) append("\n⚠️ تنبيه: الرمز قد يفتقد صلاحية `workflow` لرفع ملفات GitHub Actions.")
                append(
                    if (!repoExists) "\n📦 المستودع $ownerLogin/$cleanRepo سيُنشأ تلقائياً عند أول بناء أو مزامنة."
                    else if (allReady) "\n✅ جميع أسرار التوقيع الأربعة جاهزة داخل GitHub Secrets."
                    else "\n⚠️ ينقص في GitHub Secrets: ${missingSecrets.joinToString(", ")}"
                )
            }

            Result.success(
                GitHubCloudPreflightReport(
                    owner = ownerLogin,
                    repoName = cleanRepo,
                    repoExists = repoExists,
                    tokenScopes = tokenScopes,
                    hasWorkflowScopeWarning = workflowWarn,
                    remoteSecretNames = remoteSecrets,
                    missingSigningSecrets = missingSecrets,
                    allSigningSecretsReadyOnGitHub = allReady,
                    localVaultSha256 = material.sha256Fingerprint,
                    localVaultSha1 = material.sha1Fingerprint,
                    summaryAr = summary
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Preflight error"), e))
        }
    }

    /**
     * رفع ملفات المشروع النصية والثنائية (أيقونات PNG + أصول ZIP + Gradle + GitHub Actions) في عملية دفع واحدة.
     */
    suspend fun pushAndTriggerCloudBuild(
        token: String,
        repoName: String,
        filesToPush: Map<String, String>,
        binaryFiles: Map<String, ByteArray> = emptyMap(),
        onProgress: (String) -> Unit
    ): Result<GitHubBuildTriggerResult> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = token.trim()
            val cleanRepo = repoName.trim().replace(Regex("[^a-zA-Z0-9._-]"), "-")
            if (cleanToken.isBlank() || cleanRepo.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("يرجى إدخال رمز GitHub Token واسم المستودع"))
            }

            fun safeProgress(msg: String) {
                onProgress(BuildLogSanitizer.redactSensitive(msg))
            }

            safeProgress("🔐 التحقق من هوية حساب GitHub...")
            val userReq = Request.Builder()
                .url("https://api.github.com/user")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .build()

            val ownerLogin = client.newCall(userReq).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("فشل التحقق من Token (رمز HTTP ${resp.code})"))
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                json.getString("login")
            }

            safeProgress("📦 تجهيز المستودع السحابي: $ownerLogin/$cleanRepo ...")
            val checkRepoReq = Request.Builder()
                .url("https://api.github.com/repos/$ownerLogin/$cleanRepo")
                .header("Authorization", "Bearer $cleanToken")
                .header("Accept", "application/vnd.github+json")
                .build()

            var defaultBranch = "main"
            val repoExists = client.newCall(checkRepoReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    defaultBranch = JSONObject(resp.body?.string().orEmpty())
                        .optString("default_branch", "main")
                        .ifBlank { "main" }
                    true
                } else {
                    false
                }
            }
            if (!repoExists) {
                val createBody = JSONObject().apply {
                    put("name", cleanRepo)
                    put("private", true)
                    put("auto_init", true)
                    put("description", "Automated Android Gradle + SDK build generated by ApkForge")
                }.toString().toRequestBody(jsonMedia)

                val createReq = Request.Builder()
                    .url("https://api.github.com/user/repos")
                    .post(createBody)
                    .header("Authorization", "Bearer $cleanToken")
                    .header("Accept", "application/vnd.github+json")
                    .build()

                client.newCall(createReq).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext Result.failure(IllegalStateException("تعذر إنشاء المستودع (${resp.code})"))
                    }
                    val createdJson = JSONObject(resp.body?.string().orEmpty())
                    defaultBranch = createdJson.optString("default_branch", "main").ifBlank { "main" }
                }
            }

            val totalFileCount = filesToPush.size + binaryFiles.size
            safeProgress("🧩 تحليل وتجهيز $totalFileCount ملف (Gradle + Kotlin/Compose + أيقونات + Actions)...")
            val atomicSync = pushFilesInSingleCommit(
                token = cleanToken,
                owner = ownerLogin,
                repo = cleanRepo,
                branch = defaultBranch,
                filesToPush = filesToPush,
                binaryFiles = binaryFiles,
                onProgress = ::safeProgress
            )

            var reusedCount = 0
            val uploaded = if (atomicSync != null && (atomicSync.uploadedCount + atomicSync.reusedCount) > 0) {
                reusedCount = atomicSync.reusedCount
                atomicSync.uploadedCount
            } else {
                var count = 0
                binaryFiles.forEach { (path, bytes) ->
                    safeProgress("🖼️ رفع أصل/أيقونة: $path")
                    val existingSha = fetchFileSha(cleanToken, ownerLogin, cleanRepo, path)
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val putBody = JSONObject().apply {
                        put("message", "ApkForge binary asset: $path")
                        put("content", b64)
                        if (!existingSha.isNullOrBlank()) put("sha", existingSha)
                    }.toString().toRequestBody(jsonMedia)
                    val putReq = Request.Builder()
                        .url("https://api.github.com/repos/$ownerLogin/$cleanRepo/contents/$path")
                        .put(putBody)
                        .header("Authorization", "Bearer $cleanToken")
                        .header("Accept", "application/vnd.github+json")
                        .build()
                    client.newCall(putReq).execute().use { resp ->
                        if (resp.isSuccessful) count++
                    }
                }
                val orderedEntries = filesToPush.entries.sortedBy {
                    if (it.key.startsWith(".github/workflows/")) 1 else 0
                }
                orderedEntries.forEach { (path, content) ->
                    safeProgress("⬆️ رفع: $path")
                    val existingSha = fetchFileSha(cleanToken, ownerLogin, cleanRepo, path)
                    val b64 = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val putBody = JSONObject().apply {
                        put("message", "ApkForge sync: $path")
                        put("content", b64)
                        if (!existingSha.isNullOrBlank()) put("sha", existingSha)
                    }.toString().toRequestBody(jsonMedia)

                    val putReq = Request.Builder()
                        .url("https://api.github.com/repos/$ownerLogin/$cleanRepo/contents/$path")
                        .put(putBody)
                        .header("Authorization", "Bearer $cleanToken")
                        .header("Accept", "application/vnd.github+json")
                        .build()

                    client.newCall(putReq).execute().use { resp ->
                        if (resp.isSuccessful) count++
                    }
                }
                count
            }

            if (uploaded + reusedCount <= 0) {
                return@withContext Result.failure(IllegalStateException("تعذر رفع ملفات المشروع إلى GitHub."))
            }

            val summaryMsg = if (reusedCount > 0) {
                "تمت المزامنة التفاضلية: رفع $uploaded ملف وتخطي $reusedCount ملف متطابق ثم تشغيل Gradle CI!"
            } else {
                "تم رفع $uploaded ملف وتشغيل Gradle + Android SDK في GitHub Actions!"
            }

            Result.success(
                GitHubBuildTriggerResult(
                    owner = ownerLogin,
                    repoName = cleanRepo,
                    repoHtmlUrl = "https://github.com/$ownerLogin/$cleanRepo",
                    actionsUrl = "https://github.com/$ownerLogin/$cleanRepo/actions",
                    uploadedFilesCount = uploaded,
                    statusMessage = summaryMsg,
                    reusedUnchangedFilesCount = reusedCount
                )
            )
        } catch (e: Exception) {
            val safeMsg = BuildLogSanitizer.redactSensitive(e.message ?: "خطأ في الاتصال بـ GitHub API")
            Result.failure(IllegalStateException(safeMsg, e))
        }
    }

    private data class CommitSyncResult(
        val uploadedCount: Int,
        val reusedCount: Int
    )

    internal fun computeGitBlobSha1(bytes: ByteArray): String {
        val header = "blob ${bytes.size}\u0000".toByteArray(Charsets.US_ASCII)
        val md = MessageDigest.getInstance("SHA-1")
        md.update(header)
        md.update(bytes)
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun fetchExistingTreeBlobs(
        token: String,
        owner: String,
        repo: String,
        treeSha: String
    ): Map<String, String> {
        return runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/git/trees/$treeSha?recursive=1")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching emptyMap()
                val root = JSONObject(resp.body?.string().orEmpty())
                if (root.optBoolean("truncated", false)) return@runCatching emptyMap()
                val arr = root.optJSONArray("tree") ?: return@runCatching emptyMap()
                buildMap {
                    for (i in 0 until arr.length()) {
                        val item = arr.getJSONObject(i)
                        if (item.optString("type") == "blob") {
                            val p = item.optString("path")
                            val s = item.optString("sha")
                            if (p.isNotBlank() && s.isNotBlank()) put(p, s)
                        }
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun pushFilesInSingleCommit(
        token: String,
        owner: String,
        repo: String,
        branch: String,
        filesToPush: Map<String, String>,
        binaryFiles: Map<String, ByteArray>,
        onProgress: (String) -> Unit
    ): CommitSyncResult? {
        return try {
            var headCommitSha: String? = null
            for (attempt in 0 until 4) {
                val refReq = Request.Builder()
                    .url("https://api.github.com/repos/$owner/$repo/git/ref/heads/$branch")
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .build()
                client.newCall(refReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string().orEmpty())
                        headCommitSha = json.optJSONObject("object")?.optString("sha")?.takeIf { it.isNotBlank() }
                    }
                }
                if (!headCommitSha.isNullOrBlank()) break
                Thread.sleep(450L)
            }
            val parentCommitSha = headCommitSha ?: return null

            val commitReq = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/git/commits/$parentCommitSha")
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
            val baseTreeSha = client.newCall(commitReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body?.string().orEmpty())
                    .optJSONObject("tree")
                    ?.optString("sha")
                    ?.takeIf { it.isNotBlank() }
            } ?: return null

            val remoteBlobs = fetchExistingTreeBlobs(token, owner, repo, baseTreeSha)
            val treeArray = JSONArray()
            var uploadedCount = 0
            var reusedCount = 0

            if (binaryFiles.isNotEmpty()) {
                onProgress("🖼️ فحص ${binaryFiles.size} أصل/أيقونة ثنائية ومقارنة بصمات Git...")
                binaryFiles.forEach { (path, bytes) ->
                    val localSha = computeGitBlobSha1(bytes)
                    if (remoteBlobs[path] == localSha) {
                        reusedCount++
                    } else {
                        val blobSha = createBinaryGitBlob(token, owner, repo, bytes) ?: return null
                        treeArray.put(
                            JSONObject().apply {
                                put("path", path)
                                put("mode", "100644")
                                put("type", "blob")
                                put("sha", blobSha)
                            }
                        )
                        uploadedCount++
                    }
                }
            }

            filesToPush.forEach { (path, content) ->
                if (!binaryFiles.containsKey(path)) {
                    val localSha = computeGitBlobSha1(content.toByteArray(Charsets.UTF_8))
                    if (remoteBlobs[path] == localSha) {
                        reusedCount++
                    } else {
                        treeArray.put(
                            JSONObject().apply {
                                put("path", path)
                                put("mode", "100644")
                                put("type", "blob")
                                put("content", content)
                            }
                        )
                        uploadedCount++
                    }
                }
            }

            if (reusedCount > 0) {
                onProgress("⚡ المزامنة التفاضلية: إعادة استخدام $reusedCount ملف متطابق دون رفع، وتحديث $uploadedCount ملف...")
            } else {
                onProgress("🌳 إنشاء شجرة المشروع الكاملة ($uploadedCount ملف)...")
            }

            val newTreeSha = if (treeArray.length() == 0) {
                baseTreeSha
            } else {
                val treeBody = JSONObject().apply {
                    put("base_tree", baseTreeSha)
                    put("tree", treeArray)
                }.toString().toRequestBody(jsonMedia)
                val treeReq = Request.Builder()
                    .url("https://api.github.com/repos/$owner/$repo/git/trees")
                    .post(treeBody)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/vnd.github+json")
                    .build()
                client.newCall(treeReq).execute().use { resp ->
                    if (!resp.isSuccessful) return null
                    JSONObject(resp.body?.string().orEmpty()).optString("sha").takeIf { it.isNotBlank() }
                } ?: return null
            }

            onProgress("📦 إنشاء التزام واحد شامل (Single Commit)...")
            val newCommitBody = JSONObject().apply {
                put("message", "ApkForge: Incremental Gradle + Android SDK sync ($uploadedCount updated, $reusedCount unchanged)")
                put("tree", newTreeSha)
                put("parents", JSONArray().put(parentCommitSha))
            }.toString().toRequestBody(jsonMedia)
            val newCommitReq = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/git/commits")
                .post(newCommitBody)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
            val newCommitSha = client.newCall(newCommitReq).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body?.string().orEmpty()).optString("sha").takeIf { it.isNotBlank() }
            } ?: return null

            onProgress("🚀 تشغيل بناء Gradle & Android SDK في GitHub Actions...")
            val patchRefBody = JSONObject().apply {
                put("sha", newCommitSha)
                put("force", false)
            }.toString().toRequestBody(jsonMedia)
            val patchRefReq = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repo/git/refs/heads/$branch")
                .patch(patchRefBody)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(patchRefReq).execute().use { resp ->
                if (resp.isSuccessful) CommitSyncResult(uploadedCount = uploadedCount, reusedCount = reusedCount) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun createBinaryGitBlob(
        token: String,
        owner: String,
        repo: String,
        bytes: ByteArray
    ): String? {
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val body = JSONObject().apply {
            put("content", b64)
            put("encoding", "base64")
        }.toString().toRequestBody(jsonMedia)
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/git/blobs")
            .post(body)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .build()
        return client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) JSONObject(resp.body?.string().orEmpty()).optString("sha").takeIf { it.isNotBlank() } else null
        }
    }

    suspend fun fetchLatestWorkflowRun(
        token: String,
        owner: String,
        repoName: String
    ): Result<GitHubWorkflowRunInfo?> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repoName/actions/runs?per_page=1")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IllegalStateException("HTTP ${resp.code}"))
                val root = JSONObject(resp.body?.string().orEmpty())
                val runs: JSONArray = root.optJSONArray("workflow_runs") ?: JSONArray()
                if (runs.length() == 0) return@withContext Result.success(null)
                val latest = runs.getJSONObject(0)
                val runId = latest.getLong("id")

                val artReq = Request.Builder()
                    .url("https://api.github.com/repos/$owner/$repoName/actions/runs/$runId/artifacts")
                    .header("Authorization", "Bearer ${token.trim()}")
                    .header("Accept", "application/vnd.github+json")
                    .build()
                val artCount = client.newCall(artReq).execute().use { artResp ->
                    if (artResp.isSuccessful) {
                        JSONObject(artResp.body?.string().orEmpty()).optInt("total_count", 0)
                    } else 0
                }

                Result.success(
                    GitHubWorkflowRunInfo(
                        runId = runId,
                        status = latest.optString("status", "queued"),
                        conclusion = latest.optString("conclusion").takeIf { it.isNotBlank() && it != "null" },
                        htmlUrl = latest.optString("html_url"),
                        artifactCount = artCount
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Error"), e))
        }
    }

    suspend fun fetchWorkflowJobs(
        token: String,
        owner: String,
        repoName: String,
        runId: Long
    ): Result<List<GitHubWorkflowJobInfo>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repoName/actions/runs/$runId/jobs")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(IllegalStateException("Jobs HTTP ${resp.code}"))
                val root = JSONObject(resp.body?.string().orEmpty())
                val jobsArr = root.optJSONArray("jobs") ?: JSONArray()
                val jobs = buildList {
                    for (i in 0 until jobsArr.length()) {
                        val j = jobsArr.getJSONObject(i)
                        val stepsArr = j.optJSONArray("steps") ?: JSONArray()
                        val steps = buildList {
                            for (sIdx in 0 until stepsArr.length()) {
                                val st = stepsArr.getJSONObject(sIdx)
                                add(
                                    GitHubWorkflowStepInfo(
                                        name = st.optString("name", "Step ${sIdx + 1}"),
                                        status = st.optString("status", "queued"),
                                        conclusion = st.optString("conclusion").takeIf { it.isNotBlank() && it != "null" },
                                        number = st.optInt("number", sIdx + 1)
                                    )
                                )
                            }
                        }
                        val failedStep = steps.firstOrNull { it.conclusion == "failure" }
                        add(
                            GitHubWorkflowJobInfo(
                                jobId = j.optLong("id", 0L),
                                name = j.optString("name", "Gradle Job"),
                                status = j.optString("status", "queued"),
                                conclusion = j.optString("conclusion").takeIf { it.isNotBlank() && it != "null" },
                                steps = steps,
                                failedStepSummary = failedStep?.let { "تعثر عند الخطوة #${it.number}: ${it.name}" }
                            )
                        )
                    }
                }
                Result.success(jobs)
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Error"), e))
        }
    }

    suspend fun listRunArtifacts(
        token: String,
        owner: String,
        repoName: String,
        runId: Long
    ): Result<List<GitHubArtifactItem>> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repoName/actions/runs/$runId/artifacts")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Artifacts HTTP ${resp.code}"))
                }
                val root = JSONObject(resp.body?.string().orEmpty())
                val arr = root.optJSONArray("artifacts") ?: JSONArray()
                val items = buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        add(
                            GitHubArtifactItem(
                                id = o.optLong("id", 0L),
                                name = o.optString("name", "artifact_$i"),
                                sizeInBytes = o.optLong("size_in_bytes", 0L),
                                archiveDownloadUrl = o.optString("archive_download_url"),
                                expired = o.optBoolean("expired", false)
                            )
                        )
                    }
                }
                Result.success(items)
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Error"), e))
        }
    }

    /**
     * يحمل ملف Artifact ZIP من GitHub Actions بدون تمرير ترويسة Bearer عند التحويل لروابط Azure CDN،
     * ويفك الضغط بأمان (Zip-Slip Guard) لاستخراج `.apk` أو `.aab` والتحقق من APK عبر Google ApkVerifier.
     */
    suspend fun downloadAndExtractArtifact(
        token: String,
        artifact: GitHubArtifactItem,
        outputDir: File,
        preferExtension: String = "apk",
        expectedVaultCertSha256: String? = null,
        onProgress: (String) -> Unit = {}
    ): Result<DownloadedCloudArtifact> = withContext(Dispatchers.IO) {
        try {
            if (artifact.expired || artifact.archiveDownloadUrl.isBlank()) {
                return@withContext Result.failure(IllegalStateException("المخرج السحابي منتهي الصلاحية أو غير صالح"))
            }
            outputDir.mkdirs()
            val canonicalOutDir = outputDir.canonicalPath

            onProgress("⬇️ جارٍ تحميل المخرج السحابي: ${artifact.name}...")

            // Use followRedirects=false so we never forward GitHub PAT to external Azure Blob redirect URL
            val noRedirectClient = client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

            val initialReq = Request.Builder()
                .url(artifact.archiveDownloadUrl)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            val zipTempFile = File.createTempFile("cloud_art_", ".zip", outputDir)
            try {
                noRedirectClient.newCall(initialReq).execute().use { resp ->
                    when {
                        resp.isRedirect -> {
                            val redirectUrl = resp.header("Location")
                                ?: return@withContext Result.failure(IllegalStateException("رابط تحميل المخرج السحابي غير مكتمل"))
                            val cdnReq = Request.Builder().url(redirectUrl).build()
                            client.newCall(cdnReq).execute().use { cdnResp ->
                                if (!cdnResp.isSuccessful) {
                                    return@withContext Result.failure(IllegalStateException("فشل تحميل المخرج السحابي (HTTP ${cdnResp.code})"))
                                }
                                cdnResp.body?.byteStream()?.use { input ->
                                    FileOutputStream(zipTempFile).use { out -> input.copyTo(out) }
                                } ?: return@withContext Result.failure(IllegalStateException("محتوى المخرج السحابي فارغ"))
                            }
                        }

                        resp.isSuccessful -> {
                            resp.body?.byteStream()?.use { input ->
                                FileOutputStream(zipTempFile).use { out -> input.copyTo(out) }
                            } ?: return@withContext Result.failure(IllegalStateException("محتوى المخرج السحابي فارغ"))
                        }

                        else -> {
                            return@withContext Result.failure(IllegalStateException("فشل طلب المخرج السحابي (HTTP ${resp.code})"))
                        }
                    }
                }

                onProgress("📂 فك ضغط وفحص المخرج السحابي (${artifact.name})...")
                val extractedFiles = mutableListOf<File>()
                ZipInputStream(zipTempFile.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val safeName = File(entry.name).name.ifBlank { "cloud_output.apk" }
                            val destFile = File(outputDir, safeName)
                            val canonicalDest = destFile.canonicalPath
                            if (!canonicalDest.startsWith(canonicalOutDir + File.separator) && canonicalDest != canonicalOutDir) {
                                return@withContext Result.failure(SecurityException("تم رفض مسار غير آمن داخل الأرشيف: ${entry.name}"))
                            }
                            if (safeName.endsWith(".apk", ignoreCase = true) || safeName.endsWith(".aab", ignoreCase = true)) {
                                FileOutputStream(destFile).use { out -> zis.copyTo(out) }
                                extractedFiles.add(destFile)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val targetFile = extractedFiles.firstOrNull {
                    it.extension.equals(preferExtension, ignoreCase = true)
                } ?: extractedFiles.firstOrNull()
                    ?: return@withContext Result.failure(IllegalStateException("لم يُعثر على ملف APK أو AAB داخل أرشيف السحابة"))

                val sigReport = inspectDownloadedArtifactSignature(targetFile, expectedVaultCertSha256)
                val sha256 = sha256Hex(targetFile)
                Result.success(
                    DownloadedCloudArtifact(
                        file = targetFile,
                        fileName = targetFile.name,
                        sizeBytes = targetFile.length(),
                        isApkVerified = sigReport.isVerified,
                        v1Verified = sigReport.v1Verified,
                        v2Verified = sigReport.v2Verified,
                        v3Verified = sigReport.v3Verified,
                        sha256 = sha256,
                        signerCertSha256 = sigReport.signerCertSha256,
                        signerCertSha1 = sigReport.signerCertSha1,
                        signerSubject = sigReport.signerSubject,
                        matchesVaultCert = sigReport.matchesExpectedCert,
                        signingModeLabelAr = sigReport.verdictAr
                    )
                )
            } finally {
                if (zipTempFile.exists()) zipTempFile.delete()
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Error downloading artifact"), e))
        }
    }

    /**
     * يفحص شهادة التوقيع الرقمية الفعلية داخل ملف `.apk` (عبر Google ApkVerifier) أو `.aab` (عبر Jar/PKCS7)
     * ويستخرج بصمات SHA-256 وSHA-1 ومقارنتها بشهادة خزنة Quantum المحفوظة للمطور.
     */
    fun inspectDownloadedArtifactSignature(
        file: File,
        expectedVaultCertSha256: String? = null
    ): ArtifactSignatureReport {
        if (!file.exists() || file.length() == 0L) {
            return ArtifactSignatureReport(isVerified = false, verdictAr = "الملف غير موجود أو فارغ")
        }

        var isVerified = false
        var v1Ok = false
        var v2Ok = false
        var v3Ok = false
        var signerCert: X509Certificate? = null

        if (file.extension.equals("apk", ignoreCase = true)) {
            runCatching {
                val vr = ApkVerifier.Builder(file)
                    .setMinCheckedPlatformVersion(21)
                    .build()
                    .verify()
                isVerified = vr.isVerified
                v1Ok = vr.isVerifiedUsingV1Scheme
                v2Ok = vr.isVerifiedUsingV2Scheme
                v3Ok = vr.isVerifiedUsingV3Scheme
                signerCert = vr.signerCertificates.firstOrNull()
            }
        }

        if (signerCert == null) {
            // Works for AAB (JAR-signed by jarsigner/AGP) or fallback v1 APK certificates
            signerCert = extractFirstSignerCertFromJarOrZip(file)
            if (signerCert != null && file.extension.equals("aab", ignoreCase = true)) {
                isVerified = true
                v1Ok = true
            }
        }

        val certSha256 = signerCert?.let { sha256HexBytes(it.encoded) }.orEmpty()
        val certSha1 = signerCert?.let { sha1HexBytes(it.encoded) }.orEmpty()
        val subject = signerCert?.subjectX500Principal?.name.orEmpty()
        val cleanExpected = expectedVaultCertSha256?.trim()?.lowercase()?.takeIf { it.length == 64 }
        val matchesExpected = if (cleanExpected != null && certSha256.isNotBlank()) {
            certSha256.equals(cleanExpected, ignoreCase = true)
        } else null

        val verdict = when {
            matchesExpected == true -> "موقّع بمفتاح خزنة Quantum الخاص بك ✓"
            matchesExpected == false -> "موقّع بشهادة مختلفة (غالباً توقيع CI الاحتياطي Debug)"
            isVerified && certSha256.isNotBlank() -> "حزمة موقّعة رقمياً بشهادة X.509 صالحة ✓"
            else -> "الحزمة غير موقّعة أو لم يتم العثور على شهادة"
        }

        return ArtifactSignatureReport(
            isVerified = isVerified,
            v1Verified = v1Ok,
            v2Verified = v2Ok,
            v3Verified = v3Ok,
            signerCertSha256 = certSha256,
            signerCertSha1 = certSha1,
            signerSubject = subject,
            matchesExpectedCert = matchesExpected,
            verdictAr = verdict
        )
    }

    private fun extractFirstSignerCertFromJarOrZip(file: File): X509Certificate? {
        runCatching {
            JarFile(file, true).use { jf ->
                val entries = jf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                        jf.getInputStream(entry).use { stream ->
                            val buf = ByteArray(4096)
                            while (stream.read(buf) != -1) {
                                // Reading entry completely populates entry.certificates
                            }
                        }
                        val cert = entry.certificates?.firstOrNull() as? X509Certificate
                        if (cert != null) return cert
                    }
                }
            }
        }
        return runCatching {
            val cf = CertificateFactory.getInstance("X.509")
            ZipFile(file).use { zf ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val upper = entry.name.uppercase()
                    if (!entry.isDirectory && upper.startsWith("META-INF/") &&
                        (upper.endsWith(".RSA") || upper.endsWith(".EC") || upper.endsWith(".DSA"))
                    ) {
                        zf.getInputStream(entry).use { input ->
                            val certs = cf.generateCertificates(input)
                            val x509 = certs.firstOrNull() as? X509Certificate
                            if (x509 != null) return x509
                        }
                    }
                }
            }
            null
        }.getOrNull()
    }

    suspend fun pollWorkflowUntilComplete(
        token: String,
        owner: String,
        repoName: String,
        timeoutMs: Long = 10 * 60_000L,
        pollIntervalMs: Long = 5_000L,
        onUpdate: (GitHubWorkflowRunInfo, List<GitHubWorkflowJobInfo>) -> Unit
    ): Result<GitHubWorkflowRunInfo> {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastRun: GitHubWorkflowRunInfo? = null
        while (System.currentTimeMillis() < deadline) {
            val runRes = fetchLatestWorkflowRun(token, owner, repoName)
            val runInfo = runRes.getOrNull()
            if (runInfo != null) {
                lastRun = runInfo
                val jobs = fetchWorkflowJobs(token, owner, repoName, runInfo.runId).getOrDefault(emptyList())
                onUpdate(runInfo, jobs)
                if (runInfo.status.equals("completed", ignoreCase = true)) {
                    return Result.success(runInfo)
                }
            }
            delay(pollIntervalMs)
        }
        return lastRun?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("انتهت مهلة مراقبة البناء السحابي قبل ظهور حالة Workflow"))
    }

    /**
     * يجلب مقتطفاً من سجل أخطاء Gradle للخطوة الفاشلة عبر GitHub Jobs Logs API
     * مع منع تمرير ترويسة Bearer عند التحويل لرابط CDN، وتنقيح أي أسرار محتملة.
     */
    suspend fun fetchJobFailureLogExcerpt(
        token: String,
        owner: String,
        repoName: String,
        jobId: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val noRedirectClient = client.newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()

            val initialReq = Request.Builder()
                .url("https://api.github.com/repos/$owner/$repoName/actions/jobs/$jobId/logs")
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github+json")
                .build()

            val rawLogText = noRedirectClient.newCall(initialReq).execute().use { resp ->
                when {
                    resp.isRedirect -> {
                        val redirectUrl = resp.header("Location")
                            ?: return@withContext Result.failure(IllegalStateException("رابط سجل البناء غير مكتمل"))
                        val cdnReq = Request.Builder().url(redirectUrl).build()
                        client.newCall(cdnReq).execute().use { cdnResp ->
                            if (!cdnResp.isSuccessful) {
                                return@withContext Result.failure(IllegalStateException("تعذر قراءة سجل الخطوة (HTTP ${cdnResp.code})"))
                            }
                            cdnResp.body?.string().orEmpty()
                        }
                    }

                    resp.isSuccessful -> resp.body?.string().orEmpty()
                    else -> return@withContext Result.failure(IllegalStateException("تعذر جلب سجل الوظيفة (HTTP ${resp.code})"))
                }
            }

            Result.success(extractGradleFailureExcerpt(rawLogText))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(BuildLogSanitizer.redactSensitive(e.message ?: "Error fetching job logs"), e))
        }
    }

    internal fun extractGradleFailureExcerpt(rawLogs: String): String {
        val redacted = BuildLogSanitizer.redactSensitive(rawLogs)
        val lines = redacted.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return "لا تتوفر تفاصيل نصية في سجل Gradle."

        val errorKeywords = listOf(
            "FAILURE:",
            "Execution failed for task",
            "e: ",
            "ERROR:",
            "AAPT: error",
            "Compilation error",
            "Caused by:",
            "> Task :"
        )
        val matched = lines.filter { line ->
            errorKeywords.any { kw -> line.contains(kw, ignoreCase = false) } ||
                line.trimStart().startsWith("> ")
        }
        val selected = if (matched.isNotEmpty()) matched.takeLast(8) else lines.takeLast(8)
        return selected.joinToString("\n")
    }

    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun sha256HexBytes(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun sha1HexBytes(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun fetchFileSha(token: String, owner: String, repo: String, path: String): String? {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/contents/$path")
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/vnd.github+json")
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) JSONObject(resp.body?.string().orEmpty()).optString("sha").takeIf { it.isNotBlank() } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
