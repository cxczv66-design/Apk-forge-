package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.repository.DownloadedApk
import com.example.data.repository.ProjectRepository
import com.example.domain.model.AiArchitectPlan
import com.example.domain.model.AiSmartQuestion
import com.example.domain.model.AppProject
import com.example.domain.usecase.*
import com.example.engine.AiAppArchitectRepository
import com.example.engine.BuildOptimizationOptions
import com.example.engine.QuantumBuildEngine
import com.example.engine.QuantumBuildWorker
import com.example.engine.QuantumDiagnosticReport
import com.example.engine.ToolchainManager
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class GoogleUser(
    val name: String,
    val email: String,
    val avatarLetter: String = name.take(1).uppercase(),
    val isGoogleAccount: Boolean = true
)

sealed class AiWizardUiState {
    data object Idle : AiWizardUiState()
    data object GeneratingPlan : AiWizardUiState()
    data class PlanReady(val plan: AiArchitectPlan) : AiWizardUiState()
    data object Materializing : AiWizardUiState()
    data class Error(val message: String) : AiWizardUiState()
}

sealed class AppScreen {
    data object Splash : AppScreen()
    data object Dashboard : AppScreen()
    data object HtmlPicker : AppScreen()
    data object ZipPicker : AppScreen()
    data object ApkImportChoice : AppScreen()
    data object AiWizard : AppScreen()
    data class AdaptiveStudio(val project: AppProject, val initialPhaseIndex: Int = 0) : AppScreen()
    data class Preview(
        val fileName: String = "index.html",
        val workspacePath: String? = null,
        val entryHtmlPath: String? = null,
        val previousScreen: AppScreen = ZipPicker
    ) : AppScreen()
    data class Wizard(val projectToEdit: AppProject? = null, val initialStep: Int = 2) : AppScreen()
    data class BuildCenter(val project: AppProject) : AppScreen()
    data class InteractivePhone(val project: AppProject, val fromScreen: AppScreen = AppScreen.Dashboard) : AppScreen()
    data object Downloads : AppScreen()
    data object Account : AppScreen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("apkforge_settings_prefs", Context.MODE_PRIVATE)

    private val repository = ProjectRepository(application)
    private val aiArchitectRepository = AiAppArchitectRepository()
    val quantumEngine: QuantumBuildEngine = QuantumBuildEngine.getInstance(application)
    val toolchainManager = ToolchainManager(application)
    private val workManager: WorkManager = WorkManager.getInstance(application)

    private val getProjectsUseCase = GetProjectsUseCase(repository)
    private val saveProjectUseCase = SaveProjectUseCase(repository)
    private val deleteProjectUseCase = DeleteProjectUseCase(repository)

    val projects: StateFlow<List<AppProject>> = getProjectsUseCase()
    val downloads: StateFlow<List<DownloadedApk>> = repository.downloads

    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("app_language", "ar") ?: "ar")
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _currentUser = MutableStateFlow<GoogleUser?>(
        run {
            val logged = prefs.getBoolean("user_logged_in", false)
            val savedEmail = prefs.getString("user_email", "")?.trim().orEmpty()
            val savedName = prefs.getString("user_name", "")?.trim().orEmpty()
            if (logged && savedEmail.isNotBlank()) {
                val displayName = savedName.ifBlank { savedEmail.substringBefore("@") }
                GoogleUser(
                    name = displayName,
                    email = savedEmail,
                    avatarLetter = displayName.take(1).uppercase().ifBlank { "G" }
                )
            } else null
        }
    )
    val currentUser: StateFlow<GoogleUser?> = _currentUser.asStateFlow()

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Splash)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _hasPreviewedZip = MutableStateFlow(false)
    val hasPreviewedZip: StateFlow<Boolean> = _hasPreviewedZip.asStateFlow()

    private val _hasPreviewedHtml = MutableStateFlow(false)
    val hasPreviewedHtml: StateFlow<Boolean> = _hasPreviewedHtml.asStateFlow()

    private val _quantumReport = MutableStateFlow(quantumEngine.runRealDiagnostic())
    val quantumReport: StateFlow<QuantumDiagnosticReport> = _quantumReport.asStateFlow()

    private val _activeWorkState = MutableStateFlow<WorkInfo.State?>(null)
    val activeWorkState: StateFlow<WorkInfo.State?> = _activeWorkState.asStateFlow()

    private val _showDiagnosticDialog = MutableStateFlow(false)
    val showDiagnosticDialog: StateFlow<Boolean> = _showDiagnosticDialog.asStateFlow()

    private val _uiToastEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val uiToastEvents: SharedFlow<String> = _uiToastEvents.asSharedFlow()

    private val _aiWizardState = MutableStateFlow<AiWizardUiState>(AiWizardUiState.Idle)
    val aiWizardState: StateFlow<AiWizardUiState> = _aiWizardState.asStateFlow()

    private val _activeAdaptiveAiPlan = MutableStateFlow<AiArchitectPlan?>(null)
    val activeAdaptiveAiPlan: StateFlow<AiArchitectPlan?> = _activeAdaptiveAiPlan.asStateFlow()

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private var workObserverJob: Job? = null
    private var aiPlanJob: Job? = null

    init {
        refreshQuantumStatus()
        viewModelScope.launch {
            quantumEngine.quantumReport.collect { report ->
                _quantumReport.value = report
            }
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
        prefs.edit().putString("app_language", lang.code).apply()
    }

    fun signInWithGoogle(user: GoogleUser) {
        _currentUser.value = user
        prefs.edit()
            .putBoolean("user_logged_in", true)
            .putString("user_name", user.name)
            .putString("user_email", user.email)
            .apply()
    }

    fun signOut() {
        _currentUser.value = null
        prefs.edit()
            .putBoolean("user_logged_in", false)
            .remove("user_name")
            .remove("user_email")
            .apply()
    }

    fun clearAllData() {
        repository.clearAllProjectsAndDownloads()
        quantumEngine.resetState()
        _activeProjectId.value = null
        _hasPreviewedHtml.value = false
        _hasPreviewedZip.value = false
        _uiToastEvents.tryEmit("تم مسح جميع البيانات والمشاريع والتنزيلات بنجاح ✓")
    }

    fun deleteAccountAndData() {
        signOut()
        repository.clearAllProjectsAndDownloads()
        quantumEngine.resetState()
        _activeProjectId.value = null
        prefs.edit().clear().apply()
        _hasPreviewedHtml.value = false
        _hasPreviewedZip.value = false
        _uiToastEvents.tryEmit("تم حذف الحساب ومسح كافة البيانات المحلية نهائياً")
    }

    fun onSplashFinished() {
        _currentScreen.value = AppScreen.Dashboard
    }

    fun markPreviewed(isZip: Boolean) {
        if (isZip) _hasPreviewedZip.value = true
        else _hasPreviewedHtml.value = true
    }

    fun navigateTo(screen: AppScreen) {
        when (screen) {
            is AppScreen.AdaptiveStudio -> _activeProjectId.value = screen.project.id
            is AppScreen.InteractivePhone -> _activeProjectId.value = screen.project.id
            is AppScreen.BuildCenter -> _activeProjectId.value = screen.project.id
            is AppScreen.Wizard -> screen.projectToEdit?.let { _activeProjectId.value = it.id }
            else -> Unit
        }
        _currentScreen.value = screen
    }

    fun openDiagnosticDialog() {
        refreshQuantumStatus()
        _showDiagnosticDialog.value = true
    }

    fun closeDiagnosticDialog() {
        _showDiagnosticDialog.value = false
    }

    fun refreshQuantumStatus() {
        _quantumReport.value = quantumEngine.runRealDiagnostic()
        toolchainManager.refreshInstalledToolsStatus()
    }

    fun saveProject(project: AppProject) {
        _activeProjectId.value = project.id
        saveProjectUseCase(project)
    }

    fun generateAiArchitectPlan(ideaPrompt: String, presetCategory: String?) {
        aiPlanJob?.cancel()
        _aiWizardState.value = AiWizardUiState.GeneratingPlan
        aiPlanJob = viewModelScope.launch {
            val result = aiArchitectRepository.generateArchitectPlan(ideaPrompt, presetCategory)
            result.onSuccess { plan ->
                _activeAdaptiveAiPlan.value = plan
                _aiWizardState.value = AiWizardUiState.PlanReady(plan)
            }.onFailure { err ->
                _aiWizardState.value = AiWizardUiState.Error(
                    err.localizedMessage ?: "تعذر الاتصال بمحرك الذكاء الاصطناعي حالياً."
                )
            }
        }
    }

    fun materializeAiArchitectPlan(
        plan: AiArchitectPlan,
        answers: List<AiSmartQuestion>,
        onProjectReady: (AppProject) -> Unit
    ) {
        aiPlanJob?.cancel()
        _aiWizardState.value = AiWizardUiState.Materializing
        aiPlanJob = viewModelScope.launch {
            runCatching {
                aiArchitectRepository.materializeProjectFromPlan(
                    context = getApplication(),
                    plan = plan,
                    answeredQuestions = answers
                )
            }.onSuccess { builtProject ->
                _activeAdaptiveAiPlan.value = plan
                _activeProjectId.value = builtProject.id
                saveProjectUseCase(builtProject)
                _aiWizardState.value = AiWizardUiState.Idle
                onProjectReady(builtProject)
            }.onFailure { err ->
                _uiToastEvents.tryEmit(err.localizedMessage ?: "تعذر إنشاء ملفات المشروع التفاعلي.")
                _aiWizardState.value = AiWizardUiState.PlanReady(plan)
            }
        }
    }

    fun resetAiWizardPrompt() {
        aiPlanJob?.cancel()
        _aiWizardState.value = AiWizardUiState.Idle
    }

    fun clearActiveAiPlan() {
        _activeAdaptiveAiPlan.value = null
    }

    /**
     * تشغيل خط بناء Quantum في الخلفية عبر WorkManager (CoroutineWorker)
     * لضمان عدم تأثر عملية التجميع والتوقيع بتدوير الشاشة أو مغادرة الواجهة.
     */
    fun startAutomatedBuild(
        project: AppProject,
        targetType: String,
        options: BuildOptimizationOptions,
        autoBumpVersion: Boolean
    ) {
        viewModelScope.launch {
            val ksInfo = withContext(Dispatchers.IO) {
                toolchainManager.ensureProjectKeystoreExists(project)
            }
            val baseProject = if (project.signingConfig.keystorePath.isBlank()) {
                project.copy(
                    signingConfig = project.signingConfig.copy(
                        useDebugKey = false,
                        keyAlias = ksInfo.alias,
                        keystorePath = ksInfo.filePath
                    )
                )
            } else {
                project
            }
            val projToBuild = if (autoBumpVersion) {
                baseProject.copy(
                    packageConfig = baseProject.packageConfig.copy(
                        versionCode = baseProject.packageConfig.versionCode + 1
                    )
                )
            } else {
                baseProject
            }
            saveProjectUseCase(projToBuild)

            val jobFile = withContext(Dispatchers.IO) {
                val serialized = repository.serializeProjectToJson(projToBuild)
                QuantumBuildWorker.stageBuildJobFile(
                    context = getApplication(),
                    projectJson = serialized,
                    buildType = targetType,
                    options = options
                )
            }

            val request = OneTimeWorkRequestBuilder<QuantumBuildWorker>()
                .setInputData(workDataOf(QuantumBuildWorker.KEY_JOB_FILE_PATH to jobFile.absolutePath))
                .addTag("quantum_build_${projToBuild.id}")
                .build()

            workManager.enqueueUniqueWork(
                QuantumBuildWorker.UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            observeQuantumWork(request.id)
        }
    }

    private fun observeQuantumWork(workId: UUID) {
        workObserverJob?.cancel()
        workObserverJob = viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { info ->
                if (info == null) return@collect
                _activeWorkState.value = info.state
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        repository.refreshFromPrefs()
                        refreshQuantumStatus()
                        val outFile = info.outputData.getString(QuantumBuildWorker.KEY_OUTPUT_FILE_NAME).orEmpty()
                        _uiToastEvents.tryEmit(
                            if (outFile.isNotBlank()) "🎉 اكتمل بناء وتوقيع $outFile (v1-v4) وحفظه في التنزيلات!"
                            else "🎉 اكتمل البناء والتوقيع وتم حفظ الحزمة في التنزيلات!"
                        )
                    }
                    WorkInfo.State.FAILED -> {
                        repository.refreshFromPrefs()
                        val err = info.outputData.getString(QuantumBuildWorker.KEY_ERROR_MESSAGE).orEmpty()
                        _uiToastEvents.tryEmit(
                            if (err.isNotBlank()) "⚠️ تعذر إكمال البناء: $err"
                            else "⚠️ تعذر إكمال البناء — راجع سجل أوامر البناء أدناه"
                        )
                    }
                    else -> Unit
                }
            }
        }
    }

    fun generateProjectKeystore(
        project: AppProject,
        alias: String,
        password: String,
        org: String,
        onDone: (Boolean, AppProject?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val res = toolchainManager.generateRealKeystore(alias, password, org)
            res.onSuccess { info ->
                val updated = project.copy(
                    signingConfig = project.signingConfig.copy(
                        useDebugKey = false,
                        keyAlias = info.alias,
                        keystorePath = info.filePath
                    )
                )
                saveProjectUseCase(updated)
                refreshQuantumStatus()
                _uiToastEvents.tryEmit("🔐 تم توليد وتشفير خزنة Quantum (${info.alias}) ✓")
                onDone(true, updated)
            }.onFailure { err ->
                _uiToastEvents.tryEmit("تعذر توليد المفتاح: ${err.message}")
                onDone(false, null)
            }
        }
    }

    fun inspectBuiltPackage(outputPath: String) {
        viewModelScope.launch {
            toolchainManager.inspectApkOrZipFile(outputPath)
            _uiToastEvents.tryEmit("تم فحص الحزمة وعرض التقرير في الترمينال ✓")
        }
    }

    fun recordCompletedBuild(
        project: AppProject,
        fileName: String,
        outputPath: String,
        sizeText: String
    ) {
        saveProjectUseCase(project)
        repository.updateBuildStatus(
            projectId = project.id,
            status = com.example.domain.model.BuildStatus.SUCCESS,
            apkPath = outputPath,
            customFileName = fileName,
            sizeText = sizeText
        )
    }

    fun deleteProject(projectId: String) {
        deleteProjectUseCase(projectId)
    }

    fun removeDownload(downloadId: String) {
        repository.removeDownload(downloadId)
    }

    fun exportProjectJson(project: AppProject): String {
        return repository.exportProjectToJson(project)
    }

    fun importProjectJson(rawJson: String): AppProject? {
        return repository.importProjectFromJson(rawJson)
    }
}
