package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.repository.DownloadedApk
import com.example.data.repository.ProjectRepository
import com.example.domain.model.AppProject
import com.example.domain.usecase.*
import com.example.engine.TermuxEngine
import android.content.Context
import com.example.ui.util.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    data class Preview(val previousScreen: AppScreen = ZipPicker) : AppScreen()
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

    // Persistent Google user authentication
    private val _currentUser = MutableStateFlow<GoogleUser?>(
        if (prefs.getBoolean("user_logged_in", false)) {
            GoogleUser(
                name = prefs.getString("user_name", "Amina Urac") ?: "Amina Urac",
                email = prefs.getString("user_email", "aminauracaminaurac@gmail.com") ?: "aminauracaminaurac@gmail.com"
            )
        } else null
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

    private val _showDiagnosticDialog = MutableStateFlow(false)
    val showDiagnosticDialog: StateFlow<Boolean> = _showDiagnosticDialog.asStateFlow()

    init {
        checkTermuxStatus()
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
        _isTermuxInstalled.value = termuxEngine.isTermuxInstalled()
        _hasTermuxPermission.value = termuxEngine.hasRunCommandPermission()
    }

    fun saveProject(project: AppProject) {
        saveProjectUseCase(project)
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
