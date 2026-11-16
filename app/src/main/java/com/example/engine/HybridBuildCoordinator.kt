package com.example.engine

import android.content.Context
import com.example.data.repository.ProjectRepository
import com.example.domain.model.AppProject
import com.example.domain.model.BuildStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HybridStageStatus {
    PENDING,
    RUNNING,
    DONE,
    ERROR
}

data class HybridBuildStage(
    val id: String,
    val titleAr: String,
    val techLabel: String,
    val detailAr: String,
    val status: HybridStageStatus = HybridStageStatus.PENDING
)

data class HybridBuildState(
    val isRunning: Boolean = false,
    val activeEngine: SelectedBuildEngine = SelectedBuildEngine.LOCAL_QUANTUM,
    val targetType: String = "release",
    val projectName: String = "",
    val progress: Float = 0f,
    val statusMessage: String = "جاهز لاختيار محرك البناء الهجين",
    val stages: List<HybridBuildStage> = emptyList(),
    val logs: List<String> = emptyList(),
    val errorMessage: String? = null,
    val repoUrl: String? = null,
    val actionsUrl: String? = null,
    val runId: Long? = null,
    val artifactFilePath: String? = null,
    val artifactFileName: String? = null,
    val artifactSizeText: String? = null,
    val isApkVerified: Boolean = false,
    val sha256: String? = null,
    val keyProtectionSummary: String = "AndroidKeyStore AES-256-GCM",
    val signingSecretsSynced: Boolean = false,
    val signingCertSha256: String? = null,
    val failureLogExcerpt: String? = null,
    val artifactSignerSha256: String? = null,
    val artifactSignerSha1: String? = null,
    val artifactSignerSubject: String? = null,
    val artifactMatchesVaultKey: Boolean? = null,
    val artifactSigningLabelAr: String? = null
)

/**
 * المنسّق الهجين للبناء (Hybrid Build Coordinator):
 * يوحد تتبع المراحل والسجلات المنقّحة وأخطاء البناء السحابي (GitHub Actions Gradle + Android SDK)
 * ويربط المخرجات النهائية (.apk / .aab) مباشرة بسجل المشاريع والتنزيلات.
 */
class HybridBuildCoordinator private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(HybridBuildState(stages = defaultCloudStages()))
    val state: StateFlow<HybridBuildState> = _state.asStateFlow()

    private var activeCloudJob: Job? = null

    fun defaultCloudStages(): List<HybridBuildStage> = listOf(
        HybridBuildStage("push", "رفع كود Gradle & SDK إلى GitHub", "GIT SYNC", "مزامنة الكود والموارد والأيقونات"),
        HybridBuildStage("queue", "تهيئة خادم البناء و Android SDK", "RUNNER", "Java 17 + Android SDK 34/35 + Gradle 8.7"),
        HybridBuildStage("gradle", "ترجمة Kotlin/Java والموارد بـ Gradle", "GRADLE CI", "assembleRelease / bundleRelease"),
        HybridBuildStage("sign", "التوقيع السحابي وفحص الحزمة", "SIGN & VERIFY", "استخدام مفاتيح Secrets الآمنة إن وجدت"),
        HybridBuildStage("download", "تحميل واستخراج APK/AAB للجهاز", "ARTIFACT", "نقل المخرج إلى التنزيلات والتحقق التشفيري")
    )

    private fun appendLog(msg: String) {
        val safe = BuildLogSanitizer.redactSensitive(msg)
        _state.update { cur ->
            cur.copy(logs = (cur.logs + safe).takeLast(90))
        }
    }

    fun startCloudBuild(
        project: AppProject,
        targetType: String,
        options: BuildOptimizationOptions,
        repoNameOverride: String? = null,
        cloudEngineOverride: GitHubCloudBuildEngine? = null,
        syncSigningSecrets: Boolean = true,
        onCompleted: (fileName: String, outputPath: String, sizeText: String) -> Unit = { _, _, _ -> },
        onFailed: (String) -> Unit = {}
    ) {
        activeCloudJob?.cancel()
        val cloudEngine = cloudEngineOverride ?: GitHubCloudBuildEngine(appContext)
        val router = SmartBuildRouter(appContext)
        val exporter = ProjectExporter(appContext)
        val assembler = BuildPackageAssembler(appContext, exporter)
        val repository = ProjectRepository(appContext)

        activeCloudJob = scope.launch {
            val token = cloudEngine.getSavedToken()
            val repoName = (repoNameOverride?.takeIf { it.isNotBlank() }
                ?: project.name.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-').ifBlank { cloudEngine.getSavedRepoName() })
            val keyMode = cloudEngine.getTokenProtectionMode()
            val cachedSynced = cloudEngine.isSigningSecretsSynced(repoName)
            val cachedCert = cloudEngine.getSyncedCertFingerprint(repoName).takeIf { it.isNotBlank() }

            _state.value = HybridBuildState(
                isRunning = true,
                activeEngine = SelectedBuildEngine.CLOUD_GRADLE_SDK,
                targetType = targetType,
                projectName = project.name,
                progress = 0.06f,
                statusMessage = "جارٍ التحقق من الاتصال وخزنة الرمز المشفّرة...",
                stages = updateStage(defaultCloudStages(), "push", HybridStageStatus.RUNNING),
                logs = emptyList(),
                keyProtectionSummary = if (cachedSynced && cachedCert != null) {
                    "Token: $keyMode • Signing: Secrets مشفّرة (${cachedCert.take(10)}…)"
                } else {
                    "Token: $keyMode • Signing: لا تُرفع المفاتيح داخل Git"
                },
                signingSecretsSynced = cachedSynced,
                signingCertSha256 = cachedCert,
                failureLogExcerpt = null
            )

            try {
                val net = router.captureNetworkSnapshot()
                if (!net.isConnected) {
                    val err = "لا يوجد اتصال بالإنترنت لتنفيذ البناء السحابي عبر Gradle."
                    failCloudBuild(err, "push", onFailed)
                    return@launch
                }
                if (token.isBlank()) {
                    val err = "لم يتم العثور على رمز GitHub Token مشفّر؛ أضفه في إعدادات البناء السحابي."
                    failCloudBuild(err, "push", onFailed)
                    return@launch
                }

                appendLog("🔒 حماية الرمز السحابي: $keyMode")
                appendLog("🌐 حالة الشبكة: ${net.labelAr}")

                if (syncSigningSecrets) {
                    appendLog("🔑 مزامنة أسرار التوقيع المشفّرة (Libsodium SealedBox) قبل الدفع...")
                    val syncRes = cloudEngine.syncProjectSigningSecrets(
                        token = token,
                        repoName = repoName,
                        keyAlias = project.signingConfig.keyAlias.ifBlank { "apkforge_release" },
                        organization = project.name
                    ) { msg -> appendLog(msg) }

                    syncRes.onSuccess { res ->
                        _state.update { cur ->
                            cur.copy(
                                signingSecretsSynced = true,
                                signingCertSha256 = res.certSha256,
                                keyProtectionSummary = "Token: $keyMode • Signing: Secrets مشفّرة (${res.certSha256.take(10)}…)"
                            )
                        }
                    }.onFailure { err ->
                        val safeWarn = BuildLogSanitizer.redactSensitive(err.message ?: "تعذر مزامنة أسرار التوقيع")
                        appendLog("⚠️ تحذير التوقيع السحابي: $safeWarn — سيتم متابعة البناء مع فحص Secrets الحالية")
                    }
                }

                val normalizedTarget = when {
                    targetType.equals("aab", ignoreCase = true) -> "aab"
                    targetType.equals("both", ignoreCase = true) -> "both"
                    else -> "apk"
                }

                appendLog("🧩 تجهيز مشروع Gradle الكامل للهدف ($normalizedTarget)...")
                val textFiles = exporter.buildProjectTextEntries(
                    project = project,
                    options = options,
                    forceCloudWorkflow = true,
                    cloudBuildTarget = normalizedTarget
                )
                val binaryFiles = exporter.buildProjectBinaryEntries(project)

                val triggerRes = cloudEngine.pushAndTriggerCloudBuild(
                    token = token,
                    repoName = repoName,
                    filesToPush = textFiles,
                    binaryFiles = binaryFiles
                ) { msg ->
                    appendLog(msg)
                }

                val trigger = triggerRes.getOrElse { err ->
                    failCloudBuild(err.message ?: "تعذر رفع المشروع إلى GitHub", "push", onFailed)
                    return@launch
                }

                _state.update { cur ->
                    cur.copy(
                        progress = 0.28f,
                        statusMessage = if (trigger.reusedUnchangedFilesCount > 0) {
                            "مزامنة تفاضلية: رفع ${trigger.uploadedFilesCount} وتخطي ${trigger.reusedUnchangedFilesCount} ملف متطابق — انتظار GitHub Actions..."
                        } else {
                            "تم رفع ${trigger.uploadedFilesCount} ملف — انتظار بدء GitHub Actions..."
                        },
                        repoUrl = trigger.repoHtmlUrl,
                        actionsUrl = trigger.actionsUrl,
                        stages = updateStage(
                            updateStage(cur.stages, "push", HybridStageStatus.DONE),
                            "queue",
                            HybridStageStatus.RUNNING
                        )
                    )
                }
                appendLog("✅ ${trigger.statusMessage}")

                val finalRun = cloudEngine.pollWorkflowUntilComplete(
                    token = token,
                    owner = trigger.owner,
                    repoName = trigger.repoName,
                    timeoutMs = 12 * 60_000L,
                    pollIntervalMs = 5_000L
                ) { runInfo, jobs ->
                    mapJobsToStages(runInfo, jobs)
                }.getOrElse { err ->
                    failCloudBuild(err.message ?: "انقطع تتبع Workflow السحابي", "gradle", onFailed)
                    return@launch
                }

                val jobs = cloudEngine.fetchWorkflowJobs(
                    token = token,
                    owner = trigger.owner,
                    repoName = trigger.repoName,
                    runId = finalRun.runId
                ).getOrDefault(emptyList())

                if (!finalRun.conclusion.equals("success", ignoreCase = true)) {
                    val failDetail = jobs.firstNotNullOfOrNull { it.failedStepSummary }
                        ?: "انتهى التشغيل السحابي بالحالة: ${finalRun.conclusion ?: finalRun.status}"
                    val failedJob = jobs.firstOrNull {
                        it.conclusion.equals("failure", ignoreCase = true)
                    } ?: jobs.firstOrNull()
                    val excerpt = if (failedJob != null && failedJob.jobId > 0L) {
                        cloudEngine.fetchJobFailureLogExcerpt(
                            token = token,
                            owner = trigger.owner,
                            repoName = trigger.repoName,
                            jobId = failedJob.jobId
                        ).getOrNull()
                    } else null

                    if (!excerpt.isNullOrBlank()) {
                        excerpt.lineSequence().forEach { line ->
                            appendLog("📄 $line")
                        }
                    }

                    _state.update { cur ->
                        cur.copy(
                            actionsUrl = finalRun.htmlUrl,
                            runId = finalRun.runId,
                            failureLogExcerpt = excerpt
                        )
                    }
                    failCloudBuild(failDetail, "gradle", onFailed)
                    return@launch
                }

                _state.update { cur ->
                    cur.copy(
                        progress = 0.82f,
                        statusMessage = "نجح Gradle السحابي! جارٍ البحث عن حزمة APK/AAB...",
                        runId = finalRun.runId,
                        actionsUrl = finalRun.htmlUrl,
                        stages = updateStage(
                            updateStage(
                                updateStage(cur.stages, "queue", HybridStageStatus.DONE),
                                "gradle",
                                HybridStageStatus.DONE
                            ),
                            "sign",
                            HybridStageStatus.DONE
                        ).let { updateStage(it, "download", HybridStageStatus.RUNNING) }
                    )
                }

                val artifacts = cloudEngine.listRunArtifacts(
                    token = token,
                    owner = trigger.owner,
                    repoName = trigger.repoName,
                    runId = finalRun.runId
                ).getOrElse { err ->
                    failCloudBuild(err.message ?: "تعذر جلب قائمة مخرجات البناء", "download", onFailed)
                    return@launch
                }

                val preferExt = if (normalizedTarget == "aab") "aab" else "apk"
                val chosenArtifact = artifacts.firstOrNull {
                    !it.expired && it.name.contains(preferExt, ignoreCase = true)
                } ?: artifacts.firstOrNull { !it.expired }

                if (chosenArtifact == null) {
                    failCloudBuild("لم ينتج Workflow السحابي أي ملف APK أو AAB صالح للتحميل.", "download", onFailed)
                    return@launch
                }

                val expectedVaultCert = _state.value.signingCertSha256
                    ?: cloudEngine.getSyncedCertFingerprint(repoName).takeIf { it.isNotBlank() }

                val downloaded = cloudEngine.downloadAndExtractArtifact(
                    token = token,
                    artifact = chosenArtifact,
                    outputDir = assembler.getBuildsDir(),
                    preferExtension = preferExt,
                    expectedVaultCertSha256 = expectedVaultCert
                ) { msg -> appendLog(msg) }.getOrElse { err ->
                    failCloudBuild(err.message ?: "تعذر تحميل أو فك الحزمة السحابية", "download", onFailed)
                    return@launch
                }

                val mime = if (downloaded.fileName.endsWith(".aab", ignoreCase = true)) {
                    "application/octet-stream"
                } else {
                    "application/vnd.android.package-archive"
                }
                val downloadsNote = assembler.mirrorFileToPublicDownloads(
                    sourceFile = downloaded.file,
                    displayName = downloaded.fileName,
                    mimeType = mime
                )
                val sizeMb = downloaded.sizeBytes / (1024.0 * 1024.0)
                val sizeText = if (sizeMb >= 0.1) String.format(java.util.Locale.US, "%.2f MB", sizeMb)
                else "${maxOf(1L, downloaded.sizeBytes / 1024L)} KB"

                appendLog("🎉 تم استخراج ${downloaded.fileName} ($sizeText) وحفظه في $downloadsNote")
                if (downloaded.fileName.endsWith(".apk", ignoreCase = true)) {
                    appendLog(
                        if (downloaded.isApkVerified) {
                            "🔐 Google ApkVerifier: صالح ✓ (v1=${downloaded.v1Verified}, v2=${downloaded.v2Verified}, v3=${downloaded.v3Verified})"
                        } else {
                            "ℹ️ الحزمة السحابية محفوظة (موقّعة بمفاتيح CI أو غير موقّعة بـ Release Keystore)"
                        }
                    )
                }
                if (downloaded.signingModeLabelAr.isNotBlank()) {
                    appendLog("🔏 فحص شهادة المخرج: ${downloaded.signingModeLabelAr}")
                }
                if (downloaded.signerCertSha256.isNotBlank()) {
                    appendLog("🧬 SHA-256 للشهادة: ${QuantumKeyVault.formatFingerprintColon(downloaded.signerCertSha256)}")
                }
                if (downloaded.signerCertSha1.isNotBlank()) {
                    appendLog("🧬 SHA-1 للشهادة: ${QuantumKeyVault.formatFingerprintColon(downloaded.signerCertSha1)}")
                }

                repository.refreshFromPrefs()
                if (repository.getProjectById(project.id) == null) {
                    repository.saveProject(project)
                }
                repository.updateBuildStatus(
                    projectId = project.id,
                    status = BuildStatus.SUCCESS,
                    apkPath = downloaded.file.absolutePath,
                    customFileName = downloaded.fileName,
                    sizeText = sizeText
                )

                _state.update { cur ->
                    cur.copy(
                        isRunning = false,
                        progress = 1f,
                        statusMessage = "اكتمل البناء السحابي وتم حفظ ${downloaded.fileName} ($sizeText) ✓",
                        stages = updateStage(cur.stages, "download", HybridStageStatus.DONE),
                        errorMessage = null,
                        artifactFilePath = downloaded.file.absolutePath,
                        artifactFileName = downloaded.fileName,
                        artifactSizeText = sizeText,
                        isApkVerified = downloaded.isApkVerified,
                        sha256 = downloaded.sha256,
                        artifactSignerSha256 = downloaded.signerCertSha256.takeIf { it.isNotBlank() },
                        artifactSignerSha1 = downloaded.signerCertSha1.takeIf { it.isNotBlank() },
                        artifactSignerSubject = downloaded.signerSubject.takeIf { it.isNotBlank() },
                        artifactMatchesVaultKey = downloaded.matchesVaultCert,
                        artifactSigningLabelAr = downloaded.signingModeLabelAr.takeIf { it.isNotBlank() }
                    )
                }
                onCompleted(downloaded.fileName, downloaded.file.absolutePath, sizeText)
            } catch (ce: CancellationException) {
                appendLog("⏹️ تم إيقاف مراقبة البناء السحابي.")
                _state.update { cur ->
                    cur.copy(
                        isRunning = false,
                        statusMessage = "تم إلغاء البناء السحابي من المستخدم."
                    )
                }
                throw ce
            } catch (e: Exception) {
                failCloudBuild(e.message ?: "خطأ غير متوقع أثناء البناء السحابي", "gradle", onFailed)
            }
        }
    }

    private fun mapJobsToStages(runInfo: GitHubWorkflowRunInfo, jobs: List<GitHubWorkflowJobInfo>) {
        val currentStep = jobs.flatMap { it.steps }
            .lastOrNull { it.status.equals("in_progress", ignoreCase = true) || it.status.equals("completed", ignoreCase = true) }
        val stepText = currentStep?.let { "الخطوة #${it.number}: ${it.name}" } ?: runInfo.status

        val isQueued = runInfo.status.equals("queued", ignoreCase = true)
        val isInProgress = runInfo.status.equals("in_progress", ignoreCase = true)
        val isGradleStep = currentStep?.name?.contains("Build", ignoreCase = true) == true ||
            currentStep?.name?.contains("Gradle", ignoreCase = true) == true ||
            currentStep?.name?.contains("Assemble", ignoreCase = true) == true ||
            currentStep?.name?.contains("Bundle", ignoreCase = true) == true

        val updatedStages = _state.value.stages.map { st ->
            when (st.id) {
                "push" -> st.copy(status = HybridStageStatus.DONE)
                "queue" -> st.copy(
                    status = when {
                        isQueued -> HybridStageStatus.RUNNING
                        isInProgress || runInfo.status == "completed" -> HybridStageStatus.DONE
                        else -> st.status
                    }
                )
                "gradle" -> st.copy(
                    status = when {
                        runInfo.status == "completed" && runInfo.conclusion == "success" -> HybridStageStatus.DONE
                        runInfo.status == "completed" -> HybridStageStatus.ERROR
                        isInProgress -> HybridStageStatus.RUNNING
                        else -> HybridStageStatus.PENDING
                    },
                    detailAr = if (isInProgress) stepText else st.detailAr
                )
                "sign" -> st.copy(
                    status = when {
                        runInfo.status == "completed" && runInfo.conclusion == "success" -> HybridStageStatus.DONE
                        isInProgress && isGradleStep -> HybridStageStatus.RUNNING
                        else -> HybridStageStatus.PENDING
                    }
                )
                else -> st
            }
        }

        val targetProgress = when {
            isQueued -> 0.36f
            isInProgress -> (0.45f + (currentStep?.number ?: 1).coerceAtMost(8) * 0.04f).coerceAtMost(0.78f)
            runInfo.status == "completed" -> 0.80f
            else -> _state.value.progress
        }

        appendLog("☁️ GitHub Run #${runInfo.runId} [${runInfo.status}] • $stepText")
        _state.update { cur ->
            cur.copy(
                runId = runInfo.runId,
                actionsUrl = runInfo.htmlUrl.ifBlank { cur.actionsUrl },
                progress = targetProgress,
                statusMessage = "سحابي (Run #${runInfo.runId}): $stepText",
                stages = updatedStages
            )
        }
    }

    private fun failCloudBuild(
        rawMessage: String,
        failedStageId: String,
        onFailed: (String) -> Unit
    ) {
        val safeError = BuildLogSanitizer.redactSensitive(rawMessage)
        appendLog("❌ $safeError")
        _state.update { cur ->
            cur.copy(
                isRunning = false,
                statusMessage = "فشل البناء السحابي",
                errorMessage = safeError,
                stages = updateStage(cur.stages, failedStageId, HybridStageStatus.ERROR)
            )
        }
        onFailed(safeError)
    }

    private fun updateStage(
        stages: List<HybridBuildStage>,
        stageId: String,
        newStatus: HybridStageStatus
    ): List<HybridBuildStage> {
        return stages.map { if (it.id == stageId) it.copy(status = newStatus) else it }
    }

    fun cancelCloudBuild() {
        activeCloudJob?.cancel()
        activeCloudJob = null
        _state.update { cur ->
            cur.copy(
                isRunning = false,
                statusMessage = "تم إيقاف البناء السحابي."
            )
        }
    }

    fun notifySelectedLocal(project: AppProject, targetType: String, decision: SmartBuildDecision) {
        _state.value = HybridBuildState(
            isRunning = false,
            activeEngine = SelectedBuildEngine.LOCAL_QUANTUM,
            targetType = targetType,
            projectName = project.name,
            progress = 0f,
            statusMessage = decision.reasonAr,
            stages = emptyList(),
            logs = decision.detailedFactors.map { BuildLogSanitizer.redactSensitive(it) },
            keyProtectionSummary = "KeyVault AES-256-GCM • محلي على الجهاز"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: HybridBuildCoordinator? = null

        fun getInstance(context: Context): HybridBuildCoordinator {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HybridBuildCoordinator(context).also { INSTANCE = it }
            }
        }
    }
}
