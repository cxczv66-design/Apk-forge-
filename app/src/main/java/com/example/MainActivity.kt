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
import androidx.compose.ui.graphics.Color
import com.example.ui.components.space.SpaceBackground
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.LocalAppLanguage

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.engine.configureAppCheck
import com.example.ui.components.OmniStudioSidebarScaffold
import com.example.ui.components.OmniSupportDialog
import com.example.ui.components.SmartUrlScraperDialog
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // تهيئة Firebase App Check قبل أي استدعاء لـ Firebase AI Logic
        configureAppCheck(this, intent)
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
    val aiWizardState by viewModel.aiWizardState.collectAsState()
    val activeAdaptiveAiPlan by viewModel.activeAdaptiveAiPlan.collectAsState()
    val activeProjectId by viewModel.activeProjectId.collectAsState()
    val selectedBuildMode by viewModel.selectedBuildMode.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showSidebarScraper by remember { mutableStateOf(false) }
    var showSidebarSupport by remember { mutableStateOf(false) }

    val layoutDirection = if (currentLanguage.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val currentActiveProject = remember(projects, activeProjectId, currentScreen) {
        val screenProj = when (val s = currentScreen) {
            is AppScreen.AdaptiveStudio -> projects.find { it.id == s.project.id } ?: s.project
            is AppScreen.InteractivePhone -> projects.find { it.id == s.project.id } ?: s.project
            is AppScreen.BuildCenter -> projects.find { it.id == s.project.id } ?: s.project
            is AppScreen.Wizard -> s.projectToEdit?.let { p -> projects.find { it.id == p.id } ?: p }
            else -> null
        }
        screenProj
            ?: activeProjectId?.let { id -> projects.find { it.id == id } }
            ?: projects.firstOrNull()
    }
    val fallbackActiveProject = currentActiveProject ?: AppProject(
        id = "omni_default_proj",
        name = "Omni Studio App",
        sourceValue = "https://example.com"
    )

    // عرض إشعارات نتائج البناء الخلفي الصادرة من ViewModel / WorkManager
    LaunchedEffect(Unit) {
        viewModel.uiToastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides currentLanguage,
        LocalLayoutDirection provides layoutDirection
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceColors.DeepSpace)
        ) {
            // خلفية فضائية ثلاثية الأبعاد موحدة وعالية الأداء خلف جميع الشاشات
            SpaceBackground(
                modifier = Modifier.fillMaxSize(),
                intensity = 1f
            )

            val screenContent: @Composable () -> Unit = {
                AnimatedContent(
                    targetState = currentScreen,
                    contentKey = { s ->
                        when (s) {
                            is AppScreen.BuildCenter -> "build_center_${s.project.id}"
                            is AppScreen.AdaptiveStudio -> "adaptive_${s.project.id}_${s.initialPhaseIndex}"
                            is AppScreen.Wizard -> "wizard_${s.projectToEdit?.id ?: "new"}_${s.initialStep}"
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
                                    viewModel.clearActiveAiPlan()
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(project, 0))
                                },
                                onDeleteProject = { projectId ->
                                    viewModel.deleteProject(projectId)
                                },
                                onOpenQuantumDiagnostic = { viewModel.openDiagnosticDialog() },
                                onUseTemplate = { templateProject ->
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(templateProject)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(templateProject, 0))
                                },
                                onImportProjectJson = { rawJson ->
                                    viewModel.importProjectJson(rawJson)
                                },
                                onOpenSidebar = {
                                    scope.launch { drawerState.open() }
                                },
                                onOpenAppSetup = {
                                    viewModel.clearActiveAiPlan()
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(fallbackActiveProject, 0))
                                },
                                onOpenLiveSandbox = {
                                    viewModel.navigateTo(AppScreen.InteractivePhone(fallbackActiveProject, fromScreen = AppScreen.Dashboard))
                                },
                                onOpenApkChoice = {
                                    viewModel.navigateTo(AppScreen.ApkImportChoice)
                                },
                                onOpenAiWizard = {
                                    viewModel.navigateTo(AppScreen.AiWizard)
                                },
                                onDirectApkBuild = {
                                    viewModel.saveProject(fallbackActiveProject)
                                    viewModel.navigateTo(AppScreen.InteractivePhone(fallbackActiveProject, fromScreen = AppScreen.Dashboard))
                                }
                            )
                        }

                        is AppScreen.ApkImportChoice -> {
                            ApkImportOrScratchScreen(
                                downloads = downloads,
                                onBack = { viewModel.navigateTo(AppScreen.Dashboard) },
                                onSelectImportedApkProject = { apkProj ->
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(apkProj)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(apkProj, 0))
                                },
                                onBuildFromScratchAi = {
                                    viewModel.navigateTo(AppScreen.AiWizard)
                                }
                            )
                        }

                        is AppScreen.AiWizard -> {
                            AiWizardScreen(
                                uiState = aiWizardState,
                                onGeneratePlan = { prompt, preset ->
                                    viewModel.generateAiArchitectPlan(prompt, preset)
                                },
                                onConfirmPlanAndMaterialize = { plan, answers ->
                                    viewModel.materializeAiArchitectPlan(plan, answers) { builtProject ->
                                        viewModel.navigateTo(AppScreen.AdaptiveStudio(builtProject, 0))
                                    }
                                },
                                onResetToPrompt = { viewModel.resetAiWizardPrompt() },
                                onBack = { viewModel.navigateTo(AppScreen.Dashboard) }
                            )
                        }

                        is AppScreen.AdaptiveStudio -> {
                            val liveProject = projects.find { it.id == screen.project.id } ?: screen.project
                            AdaptivePhaseStudioScreen(
                                initialProject = liveProject,
                                activeAiPlan = if (liveProject.sourceType == SourceType.AI_SCRATCH) activeAdaptiveAiPlan else null,
                                initialPhaseIndex = screen.initialPhaseIndex,
                                onSaveProject = { viewModel.saveProject(it) },
                                onGenerateKeystore = { proj, onSigned ->
                                    viewModel.generateProjectKeystore(
                                        project = proj,
                                        alias = proj.signingConfig.keyAlias.ifBlank { "apkforge_release" },
                                        password = "appforge2026",
                                        org = proj.name
                                    ) { ok, updatedProject ->
                                        if (ok && updatedProject != null) onSigned(updatedProject)
                                    }
                                },
                                onProceedToLiveSandbox = { finalProject, phaseIdx ->
                                    viewModel.saveProject(finalProject)
                                    viewModel.navigateTo(
                                        AppScreen.InteractivePhone(
                                            project = finalProject,
                                            fromScreen = AppScreen.AdaptiveStudio(finalProject, phaseIdx)
                                        )
                                    )
                                },
                                onBack = { viewModel.navigateTo(AppScreen.Dashboard) }
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
                                    val proj = AppProject(
                                        id = "proj_${System.currentTimeMillis()}",
                                        name = "MyWebApp",
                                        sourceType = SourceType.HTML_FOLDER,
                                        sourceValue = "index.html"
                                    )
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(proj)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(proj, 0))
                                },
                                onCreateAppWithFile = { fileName ->
                                    val cleanName = fileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "WebPage" }
                                    val proj = AppProject(
                                        id = "proj_${System.currentTimeMillis()}",
                                        name = cleanName,
                                        sourceType = SourceType.HTML_FOLDER,
                                        sourceValue = fileName
                                    )
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(proj)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(proj, 0))
                                },
                                onCreateAppWithWorkspace = { ws ->
                                    val cleanName = ws.detectedTitle.take(28).ifBlank {
                                        ws.displayFileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "WebPage" }
                                    }
                                    val proj = AppProject(
                                        id = "proj_${System.currentTimeMillis()}",
                                        name = cleanName,
                                        sourceType = SourceType.HTML_FOLDER,
                                        sourceValue = ws.displayFileName,
                                        localWorkspacePath = ws.workspaceDirPath
                                    )
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(proj)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(proj, 0))
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
                                    val proj = AppProject(
                                        id = "proj_${System.currentTimeMillis()}",
                                        name = "ArchiveApp",
                                        sourceType = SourceType.ZIP_ARCHIVE,
                                        sourceValue = "archive.zip"
                                    )
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(proj)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(proj, 0))
                                },
                                onCreateAppWithFile = { fileName ->
                                    val cleanName = fileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "ArchiveApp" }
                                    val proj = AppProject(
                                        id = "proj_${System.currentTimeMillis()}",
                                        name = cleanName,
                                        sourceType = SourceType.ZIP_ARCHIVE,
                                        sourceValue = fileName
                                    )
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(proj)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(proj, 0))
                                },
                                onCreateAppWithWorkspace = { ws ->
                                    val cleanName = ws.detectedTitle.take(28).ifBlank {
                                        ws.displayFileName.substringBeforeLast(".").filter { it.isLetterOrDigit() }.ifEmpty { "ArchiveApp" }
                                    }
                                    val proj = AppProject(
                                        id = "proj_${System.currentTimeMillis()}",
                                        name = cleanName,
                                        sourceType = SourceType.ZIP_ARCHIVE,
                                        sourceValue = ws.displayFileName,
                                        localWorkspacePath = ws.workspaceDirPath
                                    )
                                    viewModel.clearActiveAiPlan()
                                    viewModel.saveProject(proj)
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(proj, 0))
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
                                initialStep = screen.initialStep,
                                onFinish = { project ->
                                    viewModel.saveProject(project)
                                    viewModel.navigateTo(AppScreen.InteractivePhone(project, fromScreen = AppScreen.AdaptiveStudio(project, 0)))
                                },
                                onCancel = {
                                    viewModel.navigateTo(AppScreen.Dashboard)
                                },
                                onOpenInteractivePhone = { project ->
                                    viewModel.navigateTo(AppScreen.InteractivePhone(project, fromScreen = AppScreen.Wizard(project, screen.initialStep)))
                                },
                                onGenerateKeystore = { updated, onDone ->
                                    viewModel.generateProjectKeystore(
                                        project = updated,
                                        alias = updated.signingConfig.keyAlias.ifBlank { "apkforge_release" },
                                        password = "appforge2026",
                                        org = updated.name
                                    ) { ok, updatedProject ->
                                        if (ok && updatedProject != null) {
                                            onDone(updatedProject)
                                        }
                                    }
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
                                hybridCoordinator = viewModel.hybridCoordinator,
                                smartBuildRouter = viewModel.smartBuildRouter,
                                selectedBuildMode = selectedBuildMode,
                                onSelectBuildMode = { mode ->
                                    viewModel.setHybridBuildMode(mode)
                                },
                                onStartHybridBuild = { proj, targetType, options, autoBump ->
                                    viewModel.startHybridBuild(proj, targetType, options, autoBump)
                                },
                                onCancelCloudBuild = {
                                    viewModel.cancelCloudBuild()
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
                            val liveProject = projects.find { it.id == screen.project.id } ?: screen.project
                            val originPhaseIndex = (screen.fromScreen as? AppScreen.AdaptiveStudio)?.initialPhaseIndex ?: 0
                            FullInteractivePhoneScreen(
                                project = liveProject,
                                quantumEngine = viewModel.quantumEngine,
                                onStartAutomatedBuild = { proj, targetType, opts, autoBump ->
                                    viewModel.startAutomatedBuild(proj, targetType, opts, autoBump)
                                },
                                onBack = { viewModel.navigateTo(screen.fromScreen) },
                                onBuild = {
                                    viewModel.navigateTo(AppScreen.BuildCenter(liveProject))
                                },
                                onEdit = {
                                    viewModel.navigateTo(AppScreen.AdaptiveStudio(liveProject, originPhaseIndex))
                                }
                            )
                        }

                        is AppScreen.Downloads -> {
                            val ctx = LocalContext.current
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
                                onClearAllData = { viewModel.clearAllData() },
                                onDeleteAccount = { viewModel.deleteAccountAndData() },
                                onOpenSupport = { showSidebarSupport = true },
                                onBack = { viewModel.navigateTo(AppScreen.Dashboard) }
                            )
                        }
                    }
                }
            }

            if (currentScreen is AppScreen.Splash) {
                screenContent()
            } else {
                OmniStudioSidebarScaffold(
                    drawerState = drawerState,
                    currentScreen = currentScreen,
                    activeProject = currentActiveProject,
                    projectsCount = projects.size,
                    downloadsCount = downloads.size,
                    onNavigate = { viewModel.navigateTo(it) },
                    onOpenUrlScraper = { showSidebarScraper = true },
                    onOpenSupport = { showSidebarSupport = true },
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                ) {
                    screenContent()
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

            if (showSidebarScraper) {
                SmartUrlScraperDialog(
                    onDismiss = { showSidebarScraper = false },
                    onCreateFromUrl = { urlProject ->
                        showSidebarScraper = false
                        viewModel.clearActiveAiPlan()
                        viewModel.saveProject(urlProject)
                        viewModel.navigateTo(AppScreen.AdaptiveStudio(urlProject, 0))
                    }
                )
            }

            if (showSidebarSupport) {
                OmniSupportDialog(
                    activeProjectName = currentActiveProject?.name ?: "ApkForge Omni IDE",
                    onDismiss = { showSidebarSupport = false }
                )
            }
        }
    }
}
