package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.QuantumEngineStatusDialog
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.LocalAppLanguage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val hasPreviewedZip by viewModel.hasPreviewedZip.collectAsState()
    val hasPreviewedHtml by viewModel.hasPreviewedHtml.collectAsState()
    val quantumReport by viewModel.quantumReport.collectAsState()
    val showDiagnostic by viewModel.showDiagnosticDialog.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val context = LocalContext.current

    // عرض إشعارات نتائج البناء الخلفي الصادرة من ViewModel / WorkManager
    LaunchedEffect(Unit) {
        viewModel.uiToastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                contentKey = { s ->
                    when (s) {
                        is AppScreen.BuildCenter -> "build_center_${s.project.id}"
                        is AppScreen.Wizard -> "wizard_${s.projectToEdit?.id ?: "new"}"
                        is AppScreen.InteractivePhone -> "phone_${s.project.id}"
                        is AppScreen.Preview -> "preview_${s.fileName}"
                        else -> s::class.simpleName ?: "screen"
                    }
                },
                transitionSpec = {
                    if (initialState is AppScreen.Splash && targetState is AppScreen.Dashboard) {
                        (fadeIn(tween(420)) + scaleIn(initialScale = 0.96f, animationSpec = tween(420))) togetherWith
                            fadeOut(tween(300))
                    } else {
                        fadeIn() togetherWith fadeOut()
                    }
                },
                label = "screen_routing"
            ) { screen ->
                when (screen) {
                    is AppScreen.Splash -> {
                        SplashScreen(
                            onSplashFinished = { viewModel.onSplashFinished() }
                        )
                    }

                    is AppScreen.Dashboard -> {
                        DashboardScreen(
                            projects = projects,
                            onOpenHtmlPicker = { viewModel.navigateTo(AppScreen.HtmlPicker) },
                            onOpenZipPicker = { viewModel.navigateTo(AppScreen.ZipPicker) },
                            onOpenDownloads = { viewModel.navigateTo(AppScreen.Downloads) },
                            onOpenAccount = { viewModel.navigateTo(AppScreen.Account) },
                            onOpenProject = { project ->
                                viewModel.navigateTo(AppScreen.BuildCenter(project))
                            },
                            onOpenQuantumDiagnostic = { viewModel.openDiagnosticDialog() },
                            onUseTemplate = { templateProject ->
                                viewModel.navigateTo(AppScreen.Wizard(templateProject))
                            },
                            onImportProjectJson = { rawJson ->
                                viewModel.importProjectJson(rawJson)
                            }
                        )
                    }

                    is AppScreen.HtmlPicker -> {
                        HtmlPickerScreen(
                            hasPreviewed = hasPreviewedHtml,
                            onBack = { viewModel.navigateTo(AppScreen.Dashboard) },
                            onPreview = {
                                viewModel.markPreviewed(isZip = false)
                                viewModel.navigateTo(AppScreen.Preview(previousScreen = AppScreen.HtmlPicker))
                            },
                            onPreviewWorkspace = { ws ->
                                viewModel.markPreviewed(isZip = false)
                                viewModel.navigateTo(
                                    AppScreen.Preview(
                                        fileName = ws.displayFileName,
                                        workspacePath = ws.workspaceDirPath,
                                        entryHtmlPath = ws.entryHtmlPath,
                                        previousScreen = AppScreen.HtmlPicker
                                    )
                                )
                            },
                            onCreateApp = {
                                viewModel.navigateTo(
                                    AppScreen.Wizard(
                                        AppProject(
                                            id = "proj_${System.currentTimeMillis()}",
                                            name = "MyWebApp",
                                            sourceType = SourceType.HTML_FOLDER,
                                            sourceValue = "index.html"
                                        )
                                    )
                                )
                            },
                            onCreateAppWithFile = { fileName ->
                                val cleanName = fileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "WebPage" }
                                viewModel.navigateTo(
                                    AppScreen.Wizard(
                                        AppProject(
                                            id = "proj_${System.currentTimeMillis()}",
                                            name = cleanName,
                                            sourceType = SourceType.HTML_FOLDER,
                                            sourceValue = fileName
                                        )
                                    )
                                )
                            },
                            onCreateAppWithWorkspace = { ws ->
                                val cleanName = ws.detectedTitle.take(28).ifBlank {
                                    ws.displayFileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "WebPage" }
                                }
                                viewModel.navigateTo(
                                    AppScreen.Wizard(
                                        AppProject(
                                            id = "proj_${System.currentTimeMillis()}",
                                            name = cleanName,
                                            sourceType = SourceType.HTML_FOLDER,
                                            sourceValue = ws.displayFileName,
                                            localWorkspacePath = ws.workspaceDirPath
                                        )
                                    )
                                )
                            }
                        )
                    }

                    is AppScreen.ZipPicker -> {
                        ZipPickerScreen(
                            hasPreviewed = hasPreviewedZip,
                            onBack = { viewModel.navigateTo(AppScreen.Dashboard) },
                            onPreview = {
                                viewModel.markPreviewed(isZip = true)
                                viewModel.navigateTo(AppScreen.Preview(previousScreen = AppScreen.ZipPicker))
                            },
                            onPreviewWorkspace = { ws ->
                                viewModel.markPreviewed(isZip = true)
                                viewModel.navigateTo(
                                    AppScreen.Preview(
                                        fileName = ws.displayFileName,
                                        workspacePath = ws.workspaceDirPath,
                                        entryHtmlPath = ws.entryHtmlPath,
                                        previousScreen = AppScreen.ZipPicker
                                    )
                                )
                            },
                            onCreateApp = {
                                viewModel.navigateTo(
                                    AppScreen.Wizard(
                                        AppProject(
                                            id = "proj_${System.currentTimeMillis()}",
                                            name = "ArchiveApp",
                                            sourceType = SourceType.ZIP_ARCHIVE,
                                            sourceValue = "archive.zip"
                                        )
                                    )
                                )
                            },
                            onCreateAppWithFile = { fileName ->
                                val cleanName = fileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "ArchiveApp" }
                                viewModel.navigateTo(
                                    AppScreen.Wizard(
                                        AppProject(
                                            id = "proj_${System.currentTimeMillis()}",
                                            name = cleanName,
                                            sourceType = SourceType.ZIP_ARCHIVE,
                                            sourceValue = fileName
                                        )
                                    )
                                )
                            },
                            onCreateAppWithWorkspace = { ws ->
                                val cleanName = ws.detectedTitle.take(28).ifBlank {
                                    ws.displayFileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "ArchiveApp" }
                                }
                                viewModel.navigateTo(
                                    AppScreen.Wizard(
                                        AppProject(
                                            id = "proj_${System.currentTimeMillis()}",
                                            name = cleanName,
                                            sourceType = SourceType.ZIP_ARCHIVE,
                                            sourceValue = ws.displayFileName,
                                            localWorkspacePath = ws.workspaceDirPath
                                        )
                                    )
                                )
                            }
                        )
                    }

                    is AppScreen.Preview -> {
                        PreviewScreen(
                            fileName = screen.fileName,
                            workspacePath = screen.workspacePath,
                            entryHtmlPath = screen.entryHtmlPath,
                            onBack = {
                                viewModel.navigateTo(screen.previousScreen)
                            }
                        )
                    }

                    is AppScreen.Wizard -> {
                        WizardScreen(
                            initialProject = screen.projectToEdit,
                            onFinish = { project ->
                                viewModel.saveProject(project)
                                viewModel.navigateTo(AppScreen.BuildCenter(project))
                            },
                            onCancel = {
                                viewModel.navigateTo(AppScreen.Dashboard)
                            },
                            onOpenInteractivePhone = { project ->
                                viewModel.navigateTo(AppScreen.InteractivePhone(project, fromScreen = AppScreen.Wizard(project)))
                            }
                        )
                    }

                    is AppScreen.BuildCenter -> {
                        val liveProject = projects.find { it.id == screen.project.id } ?: screen.project
                        BuildCenterScreen(
                            project = liveProject,
                            quantumEngine = viewModel.quantumEngine,
                            toolchainManager = viewModel.toolchainManager,
                            onOpenDiagnostic = { viewModel.openDiagnosticDialog() },
                            onEditProject = {
                                viewModel.navigateTo(AppScreen.Wizard(liveProject))
                            },
                            onOpenInteractivePhone = {
                                viewModel.navigateTo(AppScreen.InteractivePhone(liveProject, fromScreen = AppScreen.BuildCenter(liveProject)))
                            },
                            onOpenDownloads = {
                                viewModel.navigateTo(AppScreen.Downloads)
                            },
                            onUpdateProject = { updatedProject ->
                                viewModel.saveProject(updatedProject)
                            },
                            onExportProjectJson = { proj ->
                                viewModel.exportProjectJson(proj)
                            },
                            onStartAutomatedBuild = { proj, targetType, options, autoBump ->
                                viewModel.startAutomatedBuild(proj, targetType, options, autoBump)
                            },
                            onGenerateKeystore = { proj, alias, pass, org, onDone ->
                                viewModel.generateProjectKeystore(proj, alias, pass, org, onDone)
                            },
                            onInspectPackage = { path ->
                                viewModel.inspectBuiltPackage(path)
                            },
                            onBuildCompleted = { updatedProject, fileName, outputPath, sizeText ->
                                viewModel.recordCompletedBuild(updatedProject, fileName, outputPath, sizeText)
                            },
                            onBack = {
                                viewModel.navigateTo(AppScreen.Dashboard)
                            }
                        )
                    }

                    is AppScreen.InteractivePhone -> {
                        FullInteractivePhoneScreen(
                            project = screen.project,
                            onBack = { viewModel.navigateTo(screen.fromScreen) },
                            onBuild = {
                                viewModel.navigateTo(AppScreen.BuildCenter(screen.project))
                            },
                            onEdit = {
                                viewModel.navigateTo(AppScreen.Wizard(screen.project))
                            }
                        )
                    }

                    is AppScreen.Downloads -> {
                        val ctx = androidx.compose.ui.platform.LocalContext.current
                        DownloadsScreen(
                            downloads = downloads,
                            projects = projects,
                            onBack = { viewModel.navigateTo(AppScreen.Dashboard) },
                            onDeleteDownload = { viewModel.removeDownload(it) },
                            onExportCiZipForDownload = { dl ->
                                val linkedProj = projects.find { it.id == dl.projectId || it.name == dl.projectName }
                                    ?: AppProject(
                                        id = dl.projectId.ifBlank { "proj_dl_${dl.id}" },
                                        name = dl.projectName,
                                        packageConfig = com.example.domain.model.PackageConfig(
                                            packageName = dl.packageName,
                                            versionName = dl.versionName,
                                            versionCode = dl.versionCode
                                        )
                                    )
                                val exporter = com.example.engine.ProjectExporter(ctx)
                                val res = exporter.exportAndroidStudioProjectZip(linkedProj)
                                exporter.shareFileWithChooser(
                                    res.zipFile,
                                    "application/zip",
                                    "تصدير مشروع ${dl.projectName} لـ GitHub Actions"
                                )
                            }
                        )
                    }

                    is AppScreen.Account -> {
                        AccountScreen(
                            currentUser = currentUser,
                            onSignInGoogle = { viewModel.signInWithGoogle(it) },
                            onSignOut = { viewModel.signOut() },
                            onSelectLanguage = { viewModel.setLanguage(it) },
                            onBack = { viewModel.navigateTo(AppScreen.Dashboard) }
                        )
                    }
                }
            }

            if (showDiagnostic) {
                QuantumEngineStatusDialog(
                    report = quantumReport,
                    toolchainManager = viewModel.toolchainManager,
                    onDismiss = { viewModel.closeDiagnosticDialog() },
                    onRefreshDiagnostic = { viewModel.refreshQuantumStatus() }
                )
            }
        }
    }
}
