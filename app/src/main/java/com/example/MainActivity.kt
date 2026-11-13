package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.TermuxDiagnosticDialog
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.util.LocalAppLanguage
import com.example.utils.Constants

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
    val isTermuxInstalled by viewModel.isTermuxInstalled.collectAsState()
    val hasTermuxPermission by viewModel.hasTermuxPermission.collectAsState()
    val termuxReport by viewModel.termuxReport.collectAsState()
    val showDiagnostic by viewModel.showDiagnosticDialog.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isTermuxReady = isTermuxInstalled && hasTermuxPermission

    val runCommandPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.checkTermuxStatus()
        if (isGranted) {
            Toast.makeText(context, "✅ تم تفعيل صلاحية Termux RUN_COMMAND بنجاح", Toast.LENGTH_SHORT).show()
        } else if (viewModel.termuxEngine.isTermuxInstalled()) {
            Toast.makeText(
                context,
                "⚠️ يرجى منح إذن RUN_COMMAND من إعدادات التطبيق للاتصال بـ Termux",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val requestRunCommandPermission: () -> Unit = {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Constants.TERMUX_PERMISSION_RUN_COMMAND
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.checkTermuxStatus()
        } else if (viewModel.termuxEngine.isTermuxInstalled()) {
            val prefs = context.getSharedPreferences("apkforge_perm_prefs", Context.MODE_PRIVATE)
            val askedBefore = prefs.getBoolean("asked_run_command_once", false)
            val activity = context as? ComponentActivity
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                Constants.TERMUX_PERMISSION_RUN_COMMAND
            ) ?: false
            if (!askedBefore || shouldShowRationale) {
                prefs.edit().putBoolean("asked_run_command_once", true).apply()
                runCommandPermissionLauncher.launch(Constants.TERMUX_PERMISSION_RUN_COMMAND)
            } else {
                // إذا تم الرفض نهائياً مسبقاً، نفتح شاشة أذونات التطبيق أو نعيد طلب الإذن
                try {
                    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                } catch (_: Exception) {
                    runCommandPermissionLauncher.launch(Constants.TERMUX_PERMISSION_RUN_COMMAND)
                }
            }
        } else {
            runCommandPermissionLauncher.launch(Constants.TERMUX_PERMISSION_RUN_COMMAND)
        }
    }

    // طلب صلاحية Termux RUN_COMMAND تلقائياً عند فتح التطبيق لأول مرة
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("apkforge_perm_prefs", Context.MODE_PRIVATE)
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Constants.TERMUX_PERMISSION_RUN_COMMAND
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPerm && !prefs.getBoolean("first_launch_run_cmd_prompted", false)) {
            prefs.edit().putBoolean("first_launch_run_cmd_prompted", true).apply()
            runCommandPermissionLauncher.launch(Constants.TERMUX_PERMISSION_RUN_COMMAND)
        }
    }

    // تحديث حالة Termux والأذونات تلقائياً عند العودة إلى التطبيق
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkTermuxStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
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
                            onOpenTermuxDiagnostic = { viewModel.openDiagnosticDialog() },
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
                        BuildCenterScreen(
                            project = screen.project,
                            termuxEngine = viewModel.termuxEngine,
                            toolchainManager = viewModel.toolchainManager,
                            isTermuxReady = isTermuxReady,
                            onOpenDiagnostic = { viewModel.openDiagnosticDialog() },
                            onRequestRunCommandPermission = requestRunCommandPermission,
                            onEditProject = {
                                viewModel.navigateTo(AppScreen.Wizard(screen.project))
                            },
                            onOpenInteractivePhone = {
                                viewModel.navigateTo(AppScreen.InteractivePhone(screen.project, fromScreen = AppScreen.BuildCenter(screen.project)))
                            },
                            onOpenDownloads = {
                                viewModel.navigateTo(AppScreen.Downloads)
                            },
                            onUpdateProject = { updatedProject ->
                                viewModel.saveProject(updatedProject)
                                viewModel.navigateTo(AppScreen.BuildCenter(updatedProject))
                            },
                            onExportProjectJson = { proj ->
                                viewModel.exportProjectJson(proj)
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
                TermuxDiagnosticDialog(
                    report = termuxReport,
                    toolchainManager = viewModel.toolchainManager,
                    termuxEngine = viewModel.termuxEngine,
                    onRequestRunCommandPermission = requestRunCommandPermission,
                    onDismiss = { viewModel.closeDiagnosticDialog() },
                    onTestConnection = {
                        viewModel.checkTermuxStatus()
                    }
                )
            }
        }
    }
}
