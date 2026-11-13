package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DownloadedApk
import com.example.data.repository.ProjectRepository
import com.example.domain.model.AppProject
import com.example.domain.usecase.*
import com.example.engine.BuildOptimizationOptions
import com.example.engine.TermuxEngine
import com.example.engine.ToolchainManager
import android.content.Context
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GoogleUser(
    val name: String,
    val email: String,
    val avatarLetter: String = name.take(1).uppercase(),
    val isGoogleAccount: Boolean = true
)

sealed class AppScreen {
    data object Splash : AppScreen()
    data object Dashboard : AppScreen()
    data object HtmlPicker : AppScreen()
    data object ZipPicker : AppScreen()
    data class Preview(
        val fileName: String = "index.html",
        val workspacePath: String? = null,
        val entryHtmlPath: String? = null,
        val previousScreen: AppScreen = ZipPicker
    ) : AppScreen()
    data class Wizard(val projectToEdit: AppProject? = null) : AppScreen()
    data class BuildCenter(val project: AppProject) : AppScreen()
    data class InteractivePhone(val project: AppProject, val fromScreen: AppScreen = AppScreen.Dashboard) : AppScreen()
    data object Downloads : AppScreen()
    data object Account : AppScreen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("apkforge_settings_prefs", Context.MODE_PRIVATE)

    private val repository = ProjectRepository(application)
    val termuxEngine = TermuxEngine(application)
    val toolchainManager = ToolchainManager(application)

    private val getProjectsUseCase = GetProjectsUseCase(repository)
    private val saveProjectUseCase = SaveProjectUseCase(repository)
    private val deleteProjectUseCase = DeleteProjectUseCase(repository)

    val projects: StateFlow<List<AppProject>> = getProjectsUseCase()
    val downloads: StateFlow<List<DownloadedApk>> = repository.downloads

    // App-wide language persistence
    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("app_language", "ar") ?: "ar")
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Persistent real Google user authentication (no hardcoded fake account)
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

    private val _isTermuxInstalled = MutableStateFlow(false)
    val isTermuxInstalled: StateFlow<Boolean> = _isTermuxInstalled.asStateFlow()

    private val _hasTermuxPermission = MutableStateFlow(false)
    val hasTermuxPermission: StateFlow<Boolean> = _hasTermuxPermission.asStateFlow()

    private val _termuxReport = MutableStateFlow(termuxEngine.runRealDiagnostic())
    val termuxReport: StateFlow<TermuxEngine.TermuxDiagnosticReport> = _termuxReport.asStateFlow()

    private val _showDiagnosticDialog = MutableStateFlow(false)
    val showDiagnosticDialog: StateFlow<Boolean> = _showDiagnosticDialog.asStateFlow()

    private val _uiToastEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val uiToastEvents: SharedFlow<String> = _uiToastEvents.asSharedFlow()

    init {
        checkTermuxStatus()
        viewModelScope.launch {
            termuxEngine.termuxReport.collect { report ->
                _termuxReport.value = report
                _isTermuxInstalled.value = report.isTermuxInstalled
                _hasTermuxPermission.value = report.hasRunCommandPermission
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

    fun onSplashFinished() {
        _currentScreen.value = AppScreen.Dashboard
    }

    fun markPreviewed(isZip: Boolean) {
        if (isZip) _hasPreviewedZip.value = true
        else _hasPreviewedHtml.value = true
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openDiagnosticDialog() {
        checkTermuxStatus()
        _showDiagnosticDialog.value = true
    }

    fun closeDiagnosticDialog() {
        _showDiagnosticDialog.value = false
    }

    fun checkTermuxStatus() {
        val report = termuxEngine.runRealDiagnostic()
        _termuxReport.value = report
        _isTermuxInstalled.value = report.isTermuxInstalled
        _hasTermuxPermission.value = report.hasRunCommandPermission
        toolchainManager.refreshInstalledToolsStatus()
    }

    fun saveProject(project: AppProject) {
        saveProjectUseCase(project)
    }

    fun startAutomatedBuild(
        project: AppProject,
        targetType: String,
        options: BuildOptimizationOptions,
        autoBumpVersion: Boolean
    ) {
        viewModelScope.launch {
            val ksInfo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
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

            val success = termuxEngine.executeBuildWithProgress(
                project = projToBuild,
                buildType = targetType,
                options = options
            ) { fileName, outputPath, sizeText ->
                recordCompletedBuild(projToBuild, fileName, outputPath, sizeText)
            }

            if (success) {
                _uiToastEvents.tryEmit("🎉 اكتمل البناء والتوقيع وتم حفظ الحزمة في التنزيلات!")
            } else {
                _uiToastEvents.tryEmit("⚠️ تعذر إكمال البناء — راجع سجل أوامر البناء أدناه")
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
                _uiToastEvents.tryEmit("🔐 تم توليد وحفظ مفتاح RSA-2048 في الجهاز (${info.alias}) ✓")
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
