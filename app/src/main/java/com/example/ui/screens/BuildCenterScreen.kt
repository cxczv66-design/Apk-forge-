package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppProject
import com.example.engine.BuildAuditItem
import com.example.engine.BuildOptimizationOptions
import com.example.engine.GitHubCloudBuildEngine
import com.example.engine.HybridBuildCoordinator
import com.example.engine.HybridBuildMode
import com.example.engine.HybridBuildState
import com.example.engine.HybridStageStatus
import com.example.engine.ProjectExporter
import com.example.engine.QuantumBuildEngine
import com.example.engine.QuantumDiagnosticReport
import com.example.engine.SelectedBuildEngine
import com.example.engine.SmartBuildDecision
import com.example.engine.SmartBuildRouter
import com.example.engine.ToolchainManager
import com.example.ui.components.AdaptiveBuildPipelineBoard
import com.example.ui.components.AdaptiveIconAndUiStudioDialog
import com.example.ui.components.BridgeAndCloudConfigDialog
import com.example.ui.components.BuildCompleteBottomSheet
import com.example.ui.components.BuildHistoryCard
import com.example.ui.components.CloudAndBridgeFactoryCard
import com.example.ui.components.GitHubCloudBuildDialog
import com.example.ui.components.InAppToolchainTerminalCard
import com.example.ui.components.LiveBuildTerminalDialog
import com.example.ui.components.PlayStorePrepDialog
import com.example.ui.components.ProStudioHubCard
import com.example.ui.components.SecurityAndBridgeV2Dialog
import com.example.ui.components.TerminalLogView
import com.example.ui.components.WebCustomizerDialog
import com.example.ui.components.space.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun BuildCenterScreen(
    project: AppProject,
    quantumEngine: QuantumBuildEngine,
    toolchainManager: ToolchainManager? = null,
    onOpenDiagnostic: () -> Unit,
    onEditProject: () -> Unit,
    onOpenInteractivePhone: () -> Unit = {},
    onOpenDownloads: () -> Unit,
    onUpdateProject: (AppProject) -> Unit = {},
    onExportProjectJson: (AppProject) -> String = { "{}" },
    onStartAutomatedBuild: ((AppProject, String, BuildOptimizationOptions, Boolean) -> Unit)? = null,
    onGenerateKeystore: ((AppProject, String, String, String, (Boolean, AppProject?) -> Unit) -> Unit)? = null,
    onInspectPackage: ((String) -> Unit)? = null,
    hybridCoordinator: HybridBuildCoordinator? = null,
    smartBuildRouter: SmartBuildRouter? = null,
    selectedBuildMode: HybridBuildMode? = null,
    onSelectBuildMode: ((HybridBuildMode) -> Unit)? = null,
    onStartHybridBuild: ((AppProject, String, BuildOptimizationOptions, Boolean) -> Unit)? = null,
    onCancelCloudBuild: (() -> Unit)? = null,
    onBuildCompleted: (updatedProject: AppProject, fileName: String, outputPath: String, sizeText: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val effectiveToolchain = toolchainManager ?: remember(context) { ToolchainManager(context) }
    val projectExporter = remember(context) { ProjectExporter(context) }
    val githubCloudEngine = remember(context) { GitHubCloudBuildEngine(context) }
    val effectiveRouter = smartBuildRouter ?: remember(context) { SmartBuildRouter(context) }
    val effectiveCoordinator = hybridCoordinator ?: remember(context) { HybridBuildCoordinator.getInstance(context) }
    var internalBuildMode by remember { mutableStateOf(HybridBuildMode.AUTO_SMART) }
    val currentBuildMode = selectedBuildMode ?: internalBuildMode
    val hybridState by effectiveCoordinator.state.collectAsState()
    val engineState by quantumEngine.engineState.collectAsState()
    val logs by quantumEngine.logStream.collectAsState()
    val buildStages by quantumEngine.buildStages.collectAsState()
    val telemetry by quantumEngine.telemetry.collectAsState()
    val quantumReport by quantumEngine.quantumReport.collectAsState()

    var activeProject by remember(project) { mutableStateOf(project) }
    var showScriptDialog by remember { mutableStateOf(false) }
    var showKeystoreDialog by remember { mutableStateOf(false) }
    var showBridgeDialog by remember { mutableStateOf(false) }
    var showPlayPrepDialog by remember { mutableStateOf(false) }
    var showWebCustomizerDialog by remember { mutableStateOf(false) }
    var showSecurityV2Dialog by remember { mutableStateOf(false) }
    var showIconUiStudioDialog by remember { mutableStateOf(false) }
    var showGitHubCloudDialog by remember { mutableStateOf(false) }
    var showAuditDetails by remember { mutableStateOf(true) }
    var showLiveTerminalModal by remember { mutableStateOf(false) }
    var buildSessionStarted by remember { mutableStateOf(false) }
    var showCompletionBottomSheet by remember { mutableStateOf(false) }
    var completedArtifactPath by remember { mutableStateOf("") }
    var keystoreGenerated by remember(project) {
        mutableStateOf(!project.signingConfig.useDebugKey && project.signingConfig.keystorePath.isNotBlank())
    }

    LaunchedEffect(engineState) {
        if (buildSessionStarted && engineState is QuantumBuildEngine.EngineStatus.Success) {
            completedArtifactPath = (engineState as QuantumBuildEngine.EngineStatus.Success).apkPath
            showLiveTerminalModal = false
            showCompletionBottomSheet = true
            buildSessionStarted = false
        } else if (buildSessionStarted && engineState is QuantumBuildEngine.EngineStatus.Failure) {
            buildSessionStarted = false
        }
    }

    // Automated Build Optimization Options
    var r8Minify by remember { mutableStateOf(true) }
    var shrinkResources by remember { mutableStateOf(true) }
    var abiSplit by remember { mutableStateOf(false) }
    var zipAlign by remember { mutableStateOf(true) }
    var autoBumpVersion by remember { mutableStateOf(true) }

    // Custom Keystore Generator Fields
    var keyAlias by remember(project) { mutableStateOf(project.signingConfig.keyAlias.ifBlank { "release" }) }
    var keyPassword by remember { mutableStateOf("appforge2026") }
    var keyOrg by remember { mutableStateOf("ApkForge Developer") }
    var keyValidityYears by remember { mutableStateOf("25") }

    val buildOptions = remember(r8Minify, shrinkResources, abiSplit, zipAlign, keystoreGenerated, autoBumpVersion) {
        BuildOptimizationOptions(
            r8Minify = r8Minify,
            shrinkResources = shrinkResources,
            abiSplit = abiSplit,
            zipAlign = zipAlign,
            autoSignWithKeystore = keystoreGenerated,
            autoBumpVersionCode = autoBumpVersion
        )
    }

    val auditItems: List<BuildAuditItem> = remember(activeProject, keystoreGenerated) {
        quantumEngine.runPreBuildAudit(activeProject, keystoreGenerated)
    }
    val passedAuditCount = auditItems.count { it.passed }

    val isLocalBuilding = engineState is QuantumBuildEngine.EngineStatus.Running
    val isCloudBuilding = hybridState.isRunning
    val isBuilding = isLocalBuilding || isCloudBuilding

    var previewTargetForRouter by remember { mutableStateOf("release") }
    var cloudTokenConfigured by remember(showGitHubCloudDialog) {
        mutableStateOf(githubCloudEngine.hasSavedToken())
    }
    val derivedRepoSlug = remember(activeProject.name) {
        activeProject.name.lowercase().replace(Regex("[^a-z0-9-]"), "-").trim('-')
            .ifBlank { githubCloudEngine.getSavedRepoName() }
    }
    val signingSecretsSyncedForCard = remember(derivedRepoSlug, showGitHubCloudDialog, hybridState.signingSecretsSynced) {
        hybridState.signingSecretsSynced || githubCloudEngine.isSigningSecretsSynced(derivedRepoSlug)
    }
    val smartDecision = remember(
        activeProject,
        previewTargetForRouter,
        currentBuildMode,
        cloudTokenConfigured,
        abiSplit,
        quantumReport.checkTimestamp
    ) {
        effectiveRouter.decide(
            project = activeProject,
            targetType = previewTargetForRouter,
            requestedMode = currentBuildMode,
            cloudTokenConfigured = cloudTokenConfigured,
            abiSplitRequested = abiSplit
        )
    }

    fun launchAutomatedBuild(targetType: String) {
        buildSessionStarted = true
        showLiveTerminalModal = true
        if (onStartAutomatedBuild != null) {
            onStartAutomatedBuild.invoke(activeProject, targetType, buildOptions, autoBumpVersion)
        } else {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate).launch {
                val projToBuild = if (autoBumpVersion) {
                    activeProject.copy(
                        packageConfig = activeProject.packageConfig.copy(
                            versionCode = activeProject.packageConfig.versionCode + 1
                        )
                    ).also {
                        activeProject = it
                        onUpdateProject(it)
                    }
                } else {
                    activeProject
                }

                val success = quantumEngine.executeBuildWithProgress(
                    project = projToBuild,
                    buildType = targetType,
                    options = buildOptions
                ) { fileName, outputPath, sizeText ->
                    onBuildCompleted(projToBuild, fileName, outputPath, sizeText)
                }
                if (success) {
                    Toast.makeText(context, "🎉 اكتمل البناء والتوقيع وتم حفظ الحزمة في التنزيلات!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "⚠️ تعذر إكمال البناء — راجع سجل أوامر البناء أدناه", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun launchHybridBuild(targetType: String) {
        previewTargetForRouter = targetType
        val decisionNow = effectiveRouter.decide(
            project = activeProject,
            targetType = targetType,
            requestedMode = currentBuildMode,
            cloudTokenConfigured = githubCloudEngine.hasSavedToken(),
            abiSplitRequested = abiSplit
        )
        if (decisionNow.selectedEngine == SelectedBuildEngine.CLOUD_GRADLE_SDK) {
            if (!decisionNow.cloudCredentialsReady) {
                Toast.makeText(context, "☁️ أدخل رمز GitHub Token المشفّر لإطلاق البناء السحابي", Toast.LENGTH_LONG).show()
                showGitHubCloudDialog = true
                return
            }
            if (onStartHybridBuild != null) {
                onStartHybridBuild.invoke(activeProject, targetType, buildOptions, autoBumpVersion)
            } else {
                effectiveCoordinator.startCloudBuild(
                    project = activeProject,
                    targetType = targetType,
                    options = buildOptions,
                    cloudEngineOverride = githubCloudEngine,
                    onCompleted = { fileName, path, sizeText ->
                        onBuildCompleted(activeProject, fileName, path, sizeText)
                    }
                )
            }
        } else {
            effectiveCoordinator.notifySelectedLocal(activeProject, targetType, decisionNow)
            launchAutomatedBuild(targetType)
        }
    }

    fun exportStudioZipAndShare() {
        try {
            val res = projectExporter.exportAndroidStudioProjectZip(activeProject, buildOptions)
            Toast.makeText(
                context,
                "✅ تم تصدير ${res.zipFile.name} (${res.includedFilesCount} ملف • ${res.sizeKb} KB)",
                Toast.LENGTH_LONG
            ).show()
            projectExporter.shareFileWithChooser(
                file = res.zipFile,
                mimeType = "application/zip",
                title = "مشاركة مشروع Android Studio + GitHub Actions"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تصدير المشروع: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun exportApkForgeProjectFile() {
        try {
            val json = onExportProjectJson(activeProject)
            val file = projectExporter.exportProjectConfigFile(activeProject, json)
            Toast.makeText(context, "✅ تم حفظ نسخة ${file.name}", Toast.LENGTH_SHORT).show()
            projectExporter.shareFileWithChooser(
                file = file,
                mimeType = "application/json",
                title = "مشاركة نسخة .apkforge"
            )
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر تصدير الملف", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = !isBuilding) {
        onBack()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = activeProject.name,
                subtitle = "مركز التحكم الفضائي لبناء APK و AAB",
                onBack = { if (!isBuilding) onBack() },
                accentColor = SpaceColors.Gold,
                trailingContent = {
                    NeonIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        enabled = !isBuilding,
                        onClick = onEditProject,
                        tint = SpaceColors.NeonCyan,
                        size = 44.dp
                    )
                    NeonIconButton(
                        icon = Icons.Default.Download,
                        contentDescription = "التنزيلات",
                        onClick = onOpenDownloads,
                        tint = SpaceColors.Gold,
                        size = 44.dp
                    )
                }
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("build_center_screen")
        ) {
            val isWideWorkbench = maxWidth >= 720.dp

            if (isWideWorkbench) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp)
                        ) {
                            item {
                                SmartHybridRouterCard(
                                    project = activeProject,
                                    selectedMode = currentBuildMode,
                                    decision = smartDecision,
                                    hybridState = hybridState,
                                    isBuilding = isBuilding,
                                    signingSecretsSynced = signingSecretsSyncedForCard,
                                    onSelectMode = { mode ->
                                        internalBuildMode = mode
                                        onSelectBuildMode?.invoke(mode)
                                    },
                                    onLaunchApk = { launchHybridBuild("release") },
                                    onLaunchAab = { launchHybridBuild("aab") },
                                    onCancelCloud = {
                                        if (onCancelCloudBuild != null) onCancelCloudBuild.invoke()
                                        else effectiveCoordinator.cancelCloudBuild()
                                    },
                                    onOpenCloudCredentials = { showGitHubCloudDialog = true },
                                    onOpenDiagnostic = onOpenDiagnostic,
                                    onInstallCloudArtifact = { path ->
                                        val res = projectExporter.installApkDirectly(path, "cloud_release", activeProject.name)
                                        Toast.makeText(context, res.messageAr, Toast.LENGTH_LONG).show()
                                    },
                                    onOpenActions = { url ->
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                    }
                                )
                            }
                            item {
                                InteractivePhonePreviewCard(activeProject, onOpenInteractivePhone)
                            }
                            item {
                                AutoBuildSuiteCard(
                                    activeProject = activeProject,
                                    isBuilding = isBuilding,
                                    r8Minify = r8Minify,
                                    onR8Change = { r8Minify = it },
                                    shrinkResources = shrinkResources,
                                    onShrinkChange = { shrinkResources = it },
                                    abiSplit = abiSplit,
                                    onAbiSplitChange = { abiSplit = it },
                                    zipAlign = zipAlign,
                                    onZipAlignChange = { zipAlign = it },
                                    autoBumpVersion = autoBumpVersion,
                                    onAutoBumpChange = { autoBumpVersion = it },
                                    onDualBuild = { onOpenInteractivePhone() },
                                    onShowScript = { showScriptDialog = true }
                                )
                            }
                            item {
                                PreBuildDoctorCard(
                                    auditItems = auditItems,
                                    passedCount = passedAuditCount,
                                    expanded = showAuditDetails,
                                    onToggleExpand = { showAuditDetails = !showAuditDetails },
                                    onAutoFixAll = {
                                        val fixed = quantumEngine.autoFixProject(activeProject)
                                        activeProject = fixed
                                        keystoreGenerated = true
                                        onUpdateProject(fixed)
                                        Toast.makeText(context, "تم إصلاح وتجهيز الخزنة المشفّرة وكافة المتطلبات تلقائياً ⚡", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            item {
                                BuildCenterCard(
                                    title = "مفتاح التوقيع المشفّر",
                                    subtitle = if (keystoreGenerated) "خزنة Quantum محفوظة ($keyAlias.qvault) ✓" else "خزنة AES-256-GCM نحتفظ بها للأبد",
                                    icon = Icons.Default.VpnKey,
                                    buttonText = if (keystoreGenerated) "إعادة التوليد" else "بناء",
                                    isLoading = false,
                                    testTag = "build_keystore_btn",
                                    onBuildClick = { showKeystoreDialog = true }
                                )
                            }
                            item {
                                CloudAndBridgeFactoryCard(
                                    project = activeProject,
                                    onExportAndroidStudioZip = { exportStudioZipAndShare() },
                                    onOpenBridgeConfig = { showBridgeDialog = true },
                                    onOpenPlayStorePrep = { showPlayPrepDialog = true },
                                    onExportApkForgeJson = { exportApkForgeProjectFile() }
                                )
                            }
                            item {
                                ProStudioHubCard(
                                    project = activeProject,
                                    onOpenWebCustomizer = { showWebCustomizerDialog = true },
                                    onOpenSecurityAndBridgeV2 = { showSecurityV2Dialog = true },
                                    onOpenIconAndUiStudio = { showIconUiStudioDialog = true },
                                    onOpenGitHubCloudTrigger = { showGitHubCloudDialog = true }
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1.15f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp)
                        ) {
                            item {
                                AdaptiveBuildPipelineBoard(
                                    project = activeProject,
                                    engineState = engineState,
                                    stages = buildStages,
                                    telemetry = telemetry,
                                    onOpenDownloads = onOpenDownloads
                                )
                            }
                            if (engineState is QuantumBuildEngine.EngineStatus.Running) {
                                val running = engineState as QuantumBuildEngine.EngineStatus.Running
                                item { BuildProgressCard(running) }
                            }
                            item {
                                InAppToolchainTerminalCard(
                                    toolchainManager = effectiveToolchain,
                                    activeProject = activeProject,
                                    compactMode = false
                                )
                            }
                            if (telemetry.lastOutputPath != null) {
                                item {
                                    InspectOutputPackageCard(
                                        fileName = telemetry.lastOutputFileName ?: "output.apk",
                                        onInspect = {
                                            val path = telemetry.lastOutputPath ?: return@InspectOutputPackageCard
                                            if (onInspectPackage != null) {
                                                onInspectPackage.invoke(path)
                                            } else {
                                                effectiveToolchain.launchInteractiveCommand("verify", activeProject)
                                            }
                                        }
                                    )
                                }
                            }
                            item {
                                QuantumEngineDirectCard(
                                    quantumReport = quantumReport,
                                    isBuilding = isBuilding,
                                    onRunQuantumBuild = { launchAutomatedBuild("release") },
                                    onOpenDiagnostic = onOpenDiagnostic
                                )
                            }
                            if (activeProject.buildHistory.isNotEmpty()) {
                                item {
                                    BuildHistoryCard(history = activeProject.buildHistory)
                                }
                            }
                            item {
                                TerminalLogView(logs = logs)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp)
                ) {
                    item {
                        SmartHybridRouterCard(
                            project = activeProject,
                            selectedMode = currentBuildMode,
                            decision = smartDecision,
                            hybridState = hybridState,
                            isBuilding = isBuilding,
                            signingSecretsSynced = signingSecretsSyncedForCard,
                            onSelectMode = { mode ->
                                internalBuildMode = mode
                                onSelectBuildMode?.invoke(mode)
                            },
                            onLaunchApk = { launchHybridBuild("release") },
                            onLaunchAab = { launchHybridBuild("aab") },
                            onCancelCloud = {
                                if (onCancelCloudBuild != null) onCancelCloudBuild.invoke()
                                else effectiveCoordinator.cancelCloudBuild()
                            },
                            onOpenCloudCredentials = { showGitHubCloudDialog = true },
                            onOpenDiagnostic = onOpenDiagnostic,
                            onInstallCloudArtifact = { path ->
                                val res = projectExporter.installApkDirectly(path, "cloud_release", activeProject.name)
                                Toast.makeText(context, res.messageAr, Toast.LENGTH_LONG).show()
                            },
                            onOpenActions = { url ->
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                            }
                        )
                    }

                    item {
                        InteractivePhonePreviewCard(activeProject, onOpenInteractivePhone)
                    }

                    item {
                        AdaptiveBuildPipelineBoard(
                            project = activeProject,
                            engineState = engineState,
                            stages = buildStages,
                            telemetry = telemetry,
                            onOpenDownloads = onOpenDownloads
                        )
                    }

                    if (engineState is QuantumBuildEngine.EngineStatus.Running) {
                        val running = engineState as QuantumBuildEngine.EngineStatus.Running
                        item { BuildProgressCard(running) }
                    }

                    item {
                        AutoBuildSuiteCard(
                            activeProject = activeProject,
                            isBuilding = isBuilding,
                            r8Minify = r8Minify,
                            onR8Change = { r8Minify = it },
                            shrinkResources = shrinkResources,
                            onShrinkChange = { shrinkResources = it },
                            abiSplit = abiSplit,
                            onAbiSplitChange = { abiSplit = it },
                            zipAlign = zipAlign,
                            onZipAlignChange = { zipAlign = it },
                            autoBumpVersion = autoBumpVersion,
                            onAutoBumpChange = { autoBumpVersion = it },
                            onDualBuild = { onOpenInteractivePhone() },
                            onShowScript = { showScriptDialog = true }
                        )
                    }

                    item {
                        PreBuildDoctorCard(
                            auditItems = auditItems,
                            passedCount = passedAuditCount,
                            expanded = showAuditDetails,
                            onToggleExpand = { showAuditDetails = !showAuditDetails },
                            onAutoFixAll = {
                                val fixed = quantumEngine.autoFixProject(activeProject)
                                activeProject = fixed
                                keystoreGenerated = true
                                onUpdateProject(fixed)
                                Toast.makeText(context, "تم إصلاح وتجهيز الخزنة المشفّرة وكافة المتطلبات تلقائياً ⚡", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (!keystoreGenerated) {
                        item {
                            NeonGlassCard(
                                accentColor = SpaceColors.Gold,
                                secondaryAccent = SpaceColors.ErrorRed,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = SpaceColors.Gold,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "لا يوجد مفتاح توقيع في التنزيلات بعد، أنشئ واحداً أدناه وسيوثق به كل APK و AAB تبنيه - احتفظ به بأمان، فقدانه يوقف تحديثات كل التطبيقات المبنية به.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        color = SpaceColors.MutedText,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        BuildCenterCard(
                            title = "مفتاح التوقيع المشفّر",
                            subtitle = if (keystoreGenerated) "خزنة Quantum محفوظة ($keyAlias.qvault) ✓" else "خزنة AES-256-GCM نحتفظ بها للأبد",
                            icon = Icons.Default.VpnKey,
                            buttonText = if (keystoreGenerated) "إعادة التوليد" else "بناء",
                            isLoading = false,
                            testTag = "build_keystore_btn",
                            onBuildClick = { showKeystoreDialog = true }
                        )
                    }

                    item {
                        CloudAndBridgeFactoryCard(
                            project = activeProject,
                            onExportAndroidStudioZip = { exportStudioZipAndShare() },
                            onOpenBridgeConfig = { showBridgeDialog = true },
                            onOpenPlayStorePrep = { showPlayPrepDialog = true },
                            onExportApkForgeJson = { exportApkForgeProjectFile() }
                        )
                    }

                    item {
                        ProStudioHubCard(
                            project = activeProject,
                            onOpenWebCustomizer = { showWebCustomizerDialog = true },
                            onOpenSecurityAndBridgeV2 = { showSecurityV2Dialog = true },
                            onOpenIconAndUiStudio = { showIconUiStudioDialog = true },
                            onOpenGitHubCloudTrigger = { showGitHubCloudDialog = true }
                        )
                    }

                    if (activeProject.buildHistory.isNotEmpty()) {
                        item {
                            BuildHistoryCard(history = activeProject.buildHistory)
                        }
                    }

                    item {
                        InAppToolchainTerminalCard(
                            toolchainManager = effectiveToolchain,
                            activeProject = activeProject,
                            compactMode = false
                        )
                    }

                    if (telemetry.lastOutputPath != null) {
                        item {
                            InspectOutputPackageCard(
                                fileName = telemetry.lastOutputFileName ?: "output.apk",
                                onInspect = {
                                    val path = telemetry.lastOutputPath ?: return@InspectOutputPackageCard
                                    if (onInspectPackage != null) {
                                        onInspectPackage.invoke(path)
                                    } else {
                                        effectiveToolchain.launchInteractiveCommand("verify", activeProject)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        QuantumEngineDirectCard(
                            quantumReport = quantumReport,
                            isBuilding = isBuilding,
                            onRunQuantumBuild = { launchAutomatedBuild("release") },
                            onOpenDiagnostic = onOpenDiagnostic
                        )
                    }

                    item {
                        PremiumSectionHeader(
                            title = "سجل أوامر البناء (Build Engine Log)",
                            icon = Icons.Default.Terminal,
                            accentColor = SpaceColors.NeonCyan
                        )
                    }

                    item {
                        TerminalLogView(logs = logs)
                    }
                }
            }
        }

        // Quantum Build Spec Inspector Dialog
        if (showScriptDialog) {
            val fullScript = remember(activeProject, buildOptions) {
                quantumEngine.generateQuantumBuildSpecText(activeProject, "release", buildOptions)
            }
            SpaceDialog(
                onDismissRequest = { showScriptDialog = false },
                title = "مواصفات أوامر Quantum المدمجة",
                subtitle = "aapt2 + d8 + zipalign + apksig",
                icon = Icons.Default.Code,
                accentColor = SpaceColors.NeonCyan,
                secondaryAccent = SpaceColors.NeonPurple,
                widthFraction = 0.94f
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 240.dp, max = 390.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceColors.TerminalBg)
                            .border(1.dp, SpaceColors.GlassBorderCyan, RoundedCornerShape(14.dp))
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = fullScript,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = TerminalText,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    NeonButton(
                        text = "نسخ السكربت الكامل",
                        icon = Icons.Default.ContentCopy,
                        style = NeonButtonStyle.PRIMARY_GLOW,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ApkForge Build Script", fullScript))
                            Toast.makeText(context, "تم نسخ السكربت الكامل للحافظة ✓", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Quantum KeyVault Encrypted Keystore Generator Dialog
        if (showKeystoreDialog) {
            val keytoolCmd = remember(keyAlias, keyPassword, keyOrg, keyValidityYears) {
                val safeAlias = keyAlias.trim().ifBlank { "apkforge_release" }
                "quantum-keyvault seal --alias=$safeAlias --cipher=AndroidKeyStore-AES256-GCM --cert=BouncyCastle-X509-RSA2048 --org=\"$keyOrg\" --validity=${keyValidityYears}y"
            }
            SpaceDialog(
                onDismissRequest = { showKeystoreDialog = false },
                title = "خزنة التوقيع المشفّرة (KeyVault)",
                subtitle = "توقيع APK و AAB للنشر الرسمي",
                icon = Icons.Default.VpnKey,
                accentColor = SpaceColors.Gold,
                secondaryAccent = SpaceColors.NeonCyan,
                widthFraction = 0.94f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonTextField(
                        value = keyAlias,
                        onValueChange = { keyAlias = it },
                        label = "اسم المفتاح (Key Alias)",
                        leadingIcon = Icons.Default.Badge,
                        accentColor = SpaceColors.Gold
                    )
                    NeonTextField(
                        value = keyPassword,
                        onValueChange = { keyPassword = it },
                        label = "كلمة المرور (Key Password)",
                        leadingIcon = Icons.Default.Password,
                        accentColor = SpaceColors.Gold
                    )
                    NeonTextField(
                        value = keyOrg,
                        onValueChange = { keyOrg = it },
                        label = "اسم المطور / المؤسسة (Organization)",
                        leadingIcon = Icons.Default.Business,
                        accentColor = SpaceColors.NeonCyan
                    )
                    NeonTextField(
                        value = keyValidityYears,
                        onValueChange = { keyValidityYears = it },
                        label = "مدة الصلاحية بالسنوات (Validity)",
                        leadingIcon = Icons.Default.Schedule,
                        accentColor = SpaceColors.NeonCyan
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceColors.TerminalBg)
                            .border(1.dp, SpaceColors.Emerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = keytoolCmd,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = SpaceColors.Emerald
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonButton(
                            text = "إلغاء",
                            style = NeonButtonStyle.CYBER_OUTLINE,
                            onClick = { showKeystoreDialog = false },
                            modifier = Modifier.weight(1f)
                        )

                        NeonButton(
                            text = "توليد وحفظ ✓",
                            style = NeonButtonStyle.GOLD_ENGINE,
                            onClick = {
                                val cleanAlias = keyAlias.trim().ifBlank { "release" }
                                if (onGenerateKeystore != null) {
                                    onGenerateKeystore.invoke(activeProject, cleanAlias, keyPassword, keyOrg) { ok, updated ->
                                        if (ok && updated != null) {
                                            activeProject = updated
                                            keystoreGenerated = true
                                            showKeystoreDialog = false
                                        }
                                    }
                                } else {
                                    effectiveToolchain.launchInteractiveCommand("gen-key", activeProject)
                                    keystoreGenerated = true
                                    showKeystoreDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }
            }
        }

        if (showBridgeDialog) {
            BridgeAndCloudConfigDialog(
                initialConfig = activeProject.bridgeConfig,
                onDismiss = { showBridgeDialog = false },
                onSave = { updatedBridge ->
                    val updatedProj = activeProject.copy(bridgeConfig = updatedBridge)
                    activeProject = updatedProj
                    onUpdateProject(updatedProj)
                    showBridgeDialog = false
                    Toast.makeText(context, "تم حفظ إعدادات JS Bridge والسحابة ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showPlayPrepDialog) {
            PlayStorePrepDialog(
                project = activeProject,
                exporter = projectExporter,
                onDismiss = { showPlayPrepDialog = false }
            )
        }

        if (showWebCustomizerDialog) {
            WebCustomizerDialog(
                initial = activeProject.proConfig,
                onDismiss = { showWebCustomizerDialog = false },
                onSave = { updatedPro ->
                    val updatedProj = activeProject.copy(proConfig = updatedPro)
                    activeProject = updatedProj
                    onUpdateProject(updatedProj)
                    showWebCustomizerDialog = false
                    Toast.makeText(context, "تم حفظ تخصيص الويب ومانع الإعلانات ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showSecurityV2Dialog) {
            SecurityAndBridgeV2Dialog(
                initial = activeProject.proConfig,
                onDismiss = { showSecurityV2Dialog = false },
                onSave = { updatedPro ->
                    val updatedProj = activeProject.copy(proConfig = updatedPro)
                    activeProject = updatedProj
                    onUpdateProject(updatedProj)
                    showSecurityV2Dialog = false
                    Toast.makeText(context, "تم حفظ إعدادات الأمان والجسر v2 ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showIconUiStudioDialog) {
            AdaptiveIconAndUiStudioDialog(
                project = activeProject,
                onDismiss = { showIconUiStudioDialog = false },
                onSave = { updatedPro ->
                    val updatedProj = activeProject.copy(proConfig = updatedPro)
                    activeProject = updatedProj
                    onUpdateProject(updatedProj)
                    showIconUiStudioDialog = false
                    Toast.makeText(context, "تم حفظ استوديو الأيقونة والقائمة/FAB ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showGitHubCloudDialog) {
            GitHubCloudBuildDialog(
                project = activeProject,
                exporter = projectExporter,
                cloudEngine = githubCloudEngine,
                options = buildOptions,
                hybridCoordinator = effectiveCoordinator,
                onArtifactDownloaded = { fileName, path, sizeText ->
                    onBuildCompleted(activeProject, fileName, path, sizeText)
                },
                onDismiss = {
                    cloudTokenConfigured = githubCloudEngine.hasSavedToken()
                    showGitHubCloudDialog = false
                }
            )
        }

        if (showLiveTerminalModal) {
            LiveBuildTerminalDialog(
                project = activeProject,
                engineState = engineState,
                stages = buildStages,
                logs = logs,
                onDismiss = { showLiveTerminalModal = false }
            )
        }

        if (showCompletionBottomSheet && completedArtifactPath.isNotBlank()) {
            BuildCompleteBottomSheet(
                project = activeProject,
                telemetry = telemetry,
                artifactPath = completedArtifactPath,
                onInstallNow = {
                    val res = projectExporter.installApkDirectly(
                        filePath = completedArtifactPath,
                        buildType = telemetry.activeTarget,
                        projectName = activeProject.name
                    )
                    Toast.makeText(context, res.messageAr, Toast.LENGTH_LONG).show()
                },
                onSaveToDownloads = {
                    val saveRes = projectExporter.savePackageToPublicDownloadsViaMediaStore(
                        sourceFilePath = completedArtifactPath,
                        preferredFileName = telemetry.lastOutputFileName,
                        projectName = activeProject.name
                    )
                    Toast.makeText(
                        context,
                        if (saveRes.success) "✓ تم الحفظ في التنزيلات: ${saveRes.displayPath}" else (saveRes.errorMessage ?: "تعذر الحفظ"),
                        Toast.LENGTH_LONG
                    ).show()
                },
                onDismiss = { showCompletionBottomSheet = false }
            )
        }
    }
}

@Composable
private fun InteractivePhonePreviewCard(
    project: AppProject,
    onOpenInteractivePhone: () -> Unit
) {
    val accent = parseColorSafe(project.accentColorHex, SpaceColors.NeonCyan)
    NeonGlassCard(
        accentColor = accent,
        secondaryAccent = SpaceColors.NeonPurple,
        shape = RoundedCornerShape(20.dp),
        onClick = onOpenInteractivePhone,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Space3DFeatureBadge(
                    icon = Icons.Default.PhoneAndroid,
                    accentColor = accent,
                    secondaryColor = SpaceColors.NeonPurple,
                    size = 48.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "محاكي الهاتف التفاعلي المتطور",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = SpaceColors.White
                    )
                    Text(
                        text = "المس وجرب التطبيق والتنقل حياً قبل البناء",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = SpaceColors.MutedText
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            NeonButton(
                text = "معاينة 📱",
                style = NeonButtonStyle.PURPLE_AI,
                height = 44.dp,
                onClick = onOpenInteractivePhone
            )
        }
    }
}

@Composable
private fun AutoBuildSuiteCard(
    activeProject: AppProject,
    isBuilding: Boolean,
    r8Minify: Boolean,
    onR8Change: (Boolean) -> Unit,
    shrinkResources: Boolean,
    onShrinkChange: (Boolean) -> Unit,
    abiSplit: Boolean,
    onAbiSplitChange: (Boolean) -> Unit,
    zipAlign: Boolean,
    onZipAlignChange: (Boolean) -> Unit,
    autoBumpVersion: Boolean,
    onAutoBumpChange: (Boolean) -> Unit,
    onDualBuild: () -> Unit,
    onShowScript: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NeonGlassCard(
            accentColor = SpaceColors.Gold,
            secondaryAccent = SpaceColors.NeonCyan,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Space3DFeatureBadge(
                            icon = Icons.Default.AutoAwesome,
                            accentColor = SpaceColors.Gold,
                            secondaryColor = SpaceColors.NeonPurple,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "أدوات البناء التلقائية المتقدمة",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = SpaceColors.White
                            )
                            Text(
                                text = "تحسين الحجم، تقسيم المعالجات، ورفع الإصدار v${activeProject.packageConfig.versionCode}",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = SpaceColors.MutedText
                            )
                        }
                    }

                    NeonIconButton(
                        icon = Icons.Default.Code,
                        contentDescription = "عرض السكربت",
                        onClick = onShowScript,
                        tint = SpaceColors.NeonCyan,
                        size = 44.dp
                    )
                }

                // Optimization Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptChip("R8 Minify", r8Minify) { onR8Change(!r8Minify) }
                    OptChip("Shrink Res", shrinkResources) { onShrinkChange(!shrinkResources) }
                    OptChip("ABI Split", abiSplit) { onAbiSplitChange(!abiSplit) }
                    OptChip("ZipAlign", zipAlign) { onZipAlignChange(!zipAlign) }
                    OptChip("Auto Version+", autoBumpVersion) { onAutoBumpChange(!autoBumpVersion) }
                }

                NeonButton(
                    text = "بناء تلقائي شامل (APK + AAB معاً) ⚡",
                    icon = Icons.Default.RocketLaunch,
                    style = NeonButtonStyle.GOLD_ENGINE,
                    enabled = !isBuilding,
                    onClick = onDualBuild,
                    testTag = "build_dual_auto_btn",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun OptChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) SpaceColors.NeonCyan.copy(alpha = 0.18f)
                else SpaceColors.SurfaceDeep.copy(alpha = 0.8f)
            )
            .border(
                1.dp,
                if (selected) SpaceColors.NeonCyan else SpaceColors.GlassBorderSubtle,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) SpaceColors.NeonCyan else SpaceColors.SubtleText,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            color = if (selected) SpaceColors.White else SpaceColors.MutedText
        )
    }
}

@Composable
private fun PreBuildDoctorCard(
    auditItems: List<BuildAuditItem>,
    passedCount: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onAutoFixAll: () -> Unit
) {
    val allPassed = passedCount == auditItems.size
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NeonGlassCard(
            accentColor = if (allPassed) SpaceColors.Emerald else SpaceColors.Gold,
            secondaryAccent = SpaceColors.NeonCyan,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpand() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (allPassed) Icons.Default.Verified else Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = if (allPassed) SpaceColors.Emerald else SpaceColors.Gold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "فاحص الجاهزية التلقائي ($passedCount/${auditItems.size})",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SpaceColors.White
                            )
                            Text(
                                text = if (allPassed) "المشروع جاهز 100% للنشر والبناء" else "توجد تحسينات مقترحة يمكن إصلاحها بضغطة",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = SpaceColors.MutedText
                            )
                        }
                    }

                    if (!allPassed) {
                        NeonButton(
                            text = "إصلاح ⚡",
                            style = NeonButtonStyle.PRIMARY_GLOW,
                            height = 38.dp,
                            onClick = onAutoFixAll
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        auditItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(SpaceColors.SurfaceDeep.copy(alpha = 0.78f))
                                    .border(
                                        0.8.dp,
                                        if (item.passed) SpaceColors.Emerald.copy(alpha = 0.3f) else SpaceColors.Gold.copy(alpha = 0.3f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.passed) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (item.passed) SpaceColors.Emerald else SpaceColors.Gold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.titleAr,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = SpaceColors.White
                                    )
                                    Text(
                                        text = item.descriptionAr,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 10.sp,
                                        color = SpaceColors.MutedText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BuildProgressCard(running: QuantumBuildEngine.EngineStatus.Running) {
    NeonGlassCard(
        accentColor = SpaceColors.NeonCyan,
        secondaryAccent = SpaceColors.Gold,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = running.currentTask,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SpaceColors.Gold
                )
                Text(
                    text = "${(running.progress * 100).toInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SpaceColors.NeonCyan
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            NeonProgressIndicator(
                progress = running.progress,
                height = 8.dp,
                startColor = SpaceColors.NeonCyan,
                endColor = SpaceColors.Gold
            )
        }
    }
}

@Composable
private fun QuantumEngineDirectCard(
    quantumReport: QuantumDiagnosticReport,
    isBuilding: Boolean,
    onRunQuantumBuild: () -> Unit,
    onOpenDiagnostic: () -> Unit
) {
    val ready = quantumReport.isFullyOperational
    val statusColor = if (ready) SpaceColors.Emerald else SpaceColors.Gold
    val statusText = if (ready) {
        "جاهز للبناء الذاتي داخل التطبيق (v1-v4 • ${quantumReport.vaultProtectionMode})"
    } else {
        quantumReport.diagnosticSummaryAr
    }

    NeonGlassCard(
        accentColor = statusColor,
        secondaryAccent = SpaceColors.NeonCyan,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpaceQuantumAtomIcon(size = 28.dp, tint = statusColor)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "نظام البناء والأمان المدمج (Quantum Engine)",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = SpaceColors.White
                    )
                    Text(
                        text = statusText,
                        fontFamily = TajawalFontFamily,
                        fontSize = 11.sp,
                        color = statusColor
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "aapt2 + d8 + zipalign" to quantumReport.isAapt2AndD8PipelineReady,
                    "Google apksig v1-v4" to quantumReport.isApkSigReady,
                    "AndroidKeyStore AES-256-GCM" to quantumReport.isBouncyCastleLoaded,
                    "WorkManager Background" to quantumReport.isWorkManagerReady
                ).forEach { (label, active) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) SpaceColors.Emerald.copy(alpha = 0.14f)
                                else SpaceColors.SurfaceDeep
                            )
                            .border(
                                1.dp,
                                if (active) SpaceColors.Emerald.copy(alpha = 0.45f) else SpaceColors.GlassBorderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (active) "✓ $label" else label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) SpaceColors.Emerald else SpaceColors.MutedText
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NeonButton(
                    text = "فحص المحرك ✓",
                    style = NeonButtonStyle.CYBER_OUTLINE,
                    height = 46.dp,
                    onClick = onOpenDiagnostic,
                    modifier = Modifier.weight(1f)
                )

                NeonButton(
                    text = if (isBuilding) "جارٍ البناء..." else "بناء وتوقيع Release",
                    style = NeonButtonStyle.EMERALD_SUCCESS,
                    enabled = !isBuilding,
                    isLoading = isBuilding,
                    height = 46.dp,
                    onClick = onRunQuantumBuild,
                    modifier = Modifier.weight(1.25f)
                )
            }
        }
    }
}

@Composable
private fun BuildCenterCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    buttonText: String,
    isLoading: Boolean,
    testTag: String,
    onBuildClick: () -> Unit
) {
    NeonGlassCard(
        accentColor = SpaceColors.Gold,
        secondaryAccent = SpaceColors.NeonCyan,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Space3DFeatureBadge(
                    icon = icon,
                    accentColor = SpaceColors.Gold,
                    secondaryColor = SpaceColors.NeonCyan,
                    size = 46.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = SpaceColors.White
                    )
                    Text(
                        text = subtitle,
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = SpaceColors.MutedText
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            NeonButton(
                text = buttonText,
                style = NeonButtonStyle.GOLD_ENGINE,
                isLoading = isLoading,
                enabled = !isLoading,
                height = 44.dp,
                testTag = testTag,
                onClick = onBuildClick
            )
        }
    }
}

@Composable
private fun InspectOutputPackageCard(
    fileName: String,
    onInspect: () -> Unit
) {
    NeonGlassCard(
        accentColor = SpaceColors.NeonCyan,
        secondaryAccent = SpaceColors.ElectricBlue,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Space3DFeatureBadge(
                    icon = Icons.Default.ManageSearch,
                    accentColor = SpaceColors.NeonCyan,
                    secondaryColor = SpaceColors.ElectricBlue,
                    size = 42.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "فحص الحزمة النهائية (APK Inspector)",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SpaceColors.White
                    )
                    Text(
                        text = "تحليل classes.dex و AndroidManifest لـ $fileName",
                        fontFamily = TajawalFontFamily,
                        fontSize = 11.sp,
                        color = SpaceColors.MutedText
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            NeonButton(
                text = "فحص ✓",
                style = NeonButtonStyle.PRIMARY_GLOW,
                height = 42.dp,
                onClick = onInspect
            )
        }
    }
}

@Composable
private fun SmartHybridRouterCard(
    project: AppProject,
    selectedMode: HybridBuildMode,
    decision: SmartBuildDecision,
    hybridState: HybridBuildState,
    isBuilding: Boolean,
    signingSecretsSynced: Boolean = false,
    onSelectMode: (HybridBuildMode) -> Unit,
    onLaunchApk: () -> Unit,
    onLaunchAab: () -> Unit,
    onCancelCloud: () -> Unit,
    onOpenCloudCredentials: () -> Unit,
    onOpenDiagnostic: () -> Unit,
    onInstallCloudArtifact: (String) -> Unit,
    onOpenActions: (String) -> Unit
) {
    val isCloudChosen = decision.selectedEngine == SelectedBuildEngine.CLOUD_GRADLE_SDK
    val cardAccent = if (isCloudChosen) SpaceColors.Gold else SpaceColors.NeonCyan

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NeonGlassCard(
            accentColor = cardAccent,
            secondaryAccent = SpaceColors.NeonPurple,
            shape = RoundedCornerShape(20.dp),
            elevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("smart_hybrid_router_card")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Space3DFeatureBadge(
                            icon = if (isCloudChosen) Icons.Default.CloudDone else Icons.Default.Memory,
                            accentColor = cardAccent,
                            secondaryColor = SpaceColors.NeonPurple,
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "الموجّه الهجين الذكي (محلي + سحابي SDK)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = SpaceColors.White
                            )
                            Text(
                                text = decision.selectedEngine.labelAr,
                                fontFamily = TajawalFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = cardAccent
                            )
                        }
                    }

                    NeonIconButton(
                        icon = Icons.Default.VerifiedUser,
                        contentDescription = "قدرات المحرك",
                        onClick = onOpenDiagnostic,
                        tint = SpaceColors.Emerald,
                        size = 40.dp
                    )
                }

                // أزرار اختيار وضع التوجيه
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HybridBuildMode.entries.forEach { mode ->
                        val active = mode == selectedMode
                        Surface(
                            color = if (active) cardAccent.copy(alpha = 0.22f) else SpaceColors.SurfaceDeep.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (active) cardAccent else SpaceColors.GlassBorderSubtle
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(enabled = !isBuilding) { onSelectMode(mode) }
                                .testTag("hybrid_mode_${mode.name.lowercase()}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = mode.labelAr,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (active) SpaceColors.White else SpaceColors.MutedText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // شارات حالة الجهاز والشبكة والاعتماد السحابي
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HybridTelemetryChip(
                        label = decision.networkSnapshot.labelAr,
                        color = if (decision.networkSnapshot.isConnected) SpaceColors.Emerald else SpaceColors.ErrorRed
                    )
                    HybridTelemetryChip(
                        label = "${decision.deviceSnapshot.abi} • ${decision.deviceSnapshot.freeStorageMb}MB",
                        color = if (decision.deviceSnapshot.hasMinimumBuildStorage) SpaceColors.NeonCyan else SpaceColors.Gold
                    )
                    HybridTelemetryChip(
                        label = if (decision.cloudCredentialsReady) "GitHub Token مشفّر ✓" else "ربط GitHub ☁️",
                        color = if (decision.cloudCredentialsReady) SpaceColors.Emerald else SpaceColors.Gold,
                        onClick = onOpenCloudCredentials
                    )
                    HybridTelemetryChip(
                        label = if (signingSecretsSynced) "توقيع سحابي: Secrets مشفّرة ✓" else "مزامنة التوقيع السحابي 🔐",
                        color = if (signingSecretsSynced) SpaceColors.Emerald else SpaceColors.NeonPurple,
                        onClick = onOpenCloudCredentials
                    )
                }

                // شرح قرار الموجّه بكل شفافية
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpaceColors.SurfaceDeep.copy(alpha = 0.82f))
                        .border(0.8.dp, cardAccent.copy(alpha = 0.38f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = decision.reasonAr,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = SpaceColors.White,
                            lineHeight = 16.sp
                        )
                        decision.warnings.take(2).forEach { warn ->
                            Text(
                                text = "• $warn",
                                fontFamily = TajawalFontFamily,
                                fontSize = 10.sp,
                                color = SpaceColors.Gold,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // أزرار إطلاق البناء الهجين (APK أو Play AAB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonButton(
                        text = if (isBuilding) "جارٍ البناء..." else "بناء APK هجين ⚡",
                        style = if (isCloudChosen) NeonButtonStyle.GOLD_ENGINE else NeonButtonStyle.PRIMARY_GLOW,
                        enabled = !isBuilding,
                        isLoading = isBuilding && hybridState.targetType != "aab",
                        height = 44.dp,
                        testTag = "hybrid_build_apk_btn",
                        onClick = onLaunchApk,
                        modifier = Modifier.weight(1.1f)
                    )
                    NeonButton(
                        text = "Play AAB ☁️",
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        enabled = !isBuilding,
                        isLoading = isBuilding && hybridState.targetType == "aab",
                        height = 44.dp,
                        testTag = "hybrid_build_aab_btn",
                        onClick = onLaunchAab,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                // متابعة السحابة الحية (Progress + Stages + Logs + Errors + Artifact)
                if (hybridState.activeEngine == SelectedBuildEngine.CLOUD_GRADLE_SDK &&
                    (hybridState.isRunning || hybridState.progress > 0f || hybridState.errorMessage != null)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceColors.TerminalBg.copy(alpha = 0.9f))
                            .border(1.dp, SpaceColors.GlassBorderCyan, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = hybridState.statusMessage,
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = if (hybridState.errorMessage != null) SpaceColors.ErrorRed else SpaceColors.NeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${(hybridState.progress * 100).toInt()}%",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SpaceColors.Gold
                            )
                        }
                        NeonProgressIndicator(
                            progress = hybridState.progress,
                            height = 6.dp,
                            startColor = SpaceColors.NeonCyan,
                            endColor = SpaceColors.Gold
                        )

                        if (hybridState.stages.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                hybridState.stages.forEach { st ->
                                    val stColor = when (st.status) {
                                        HybridStageStatus.DONE -> SpaceColors.Emerald
                                        HybridStageStatus.RUNNING -> SpaceColors.Gold
                                        HybridStageStatus.ERROR -> SpaceColors.ErrorRed
                                        HybridStageStatus.PENDING -> SpaceColors.SubtleText
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(stColor.copy(alpha = 0.14f))
                                            .border(0.8.dp, stColor.copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = st.techLabel,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            color = stColor
                                        )
                                    }
                                }
                            }
                        }

                        hybridState.errorMessage?.let { err ->
                            Text(
                                text = "❌ $err",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = SpaceColors.ErrorRed
                            )
                        }

                        hybridState.failureLogExcerpt?.takeIf { it.isNotBlank() }?.let { excerpt ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SpaceColors.SurfaceDeep.copy(alpha = 0.92f))
                                    .border(0.8.dp, SpaceColors.ErrorRed.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                    .horizontalScroll(rememberScrollState())
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = excerpt,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = SpaceColors.White,
                                    lineHeight = 13.sp
                                )
                            }
                        }

                        hybridState.logs.takeLast(4).forEach { logLine ->
                            Text(
                                text = logLine,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = SpaceColors.Emerald,
                                maxLines = 2
                            )
                        }

                        hybridState.artifactSigningLabelAr?.takeIf { it.isNotBlank() }?.let { sigLabel ->
                            val sigColor = when (hybridState.artifactMatchesVaultKey) {
                                true -> SpaceColors.Emerald
                                false -> SpaceColors.Gold
                                null -> SpaceColors.NeonCyan
                            }
                            val fpShort = hybridState.artifactSignerSha256
                                ?.takeIf { it.isNotBlank() }
                                ?.let { com.example.engine.QuantumKeyVault.formatFingerprintColon(it).take(23) + "…" }
                                ?: ""
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(sigColor.copy(alpha = 0.12f))
                                    .border(0.8.dp, sigColor.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (fpShort.isNotBlank()) "🔏 $sigLabel ($fpShort)" else "🔏 $sigLabel",
                                    fontFamily = TajawalFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    color = sigColor
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hybridState.isRunning) {
                                NeonButton(
                                    text = "إلغاء",
                                    style = NeonButtonStyle.DANGER_RED,
                                    height = 38.dp,
                                    onClick = onCancelCloud,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            hybridState.actionsUrl?.let { url ->
                                NeonButton(
                                    text = "Actions ↗",
                                    style = NeonButtonStyle.CYBER_OUTLINE,
                                    height = 38.dp,
                                    onClick = { onOpenActions(url) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val readyPath = hybridState.artifactFilePath
                            val readyName = hybridState.artifactFileName
                            if (!readyPath.isNullOrBlank() && !readyName.isNullOrBlank() && readyName.endsWith(".apk", true)) {
                                NeonButton(
                                    text = "تثبيت 📲",
                                    style = NeonButtonStyle.EMERALD_SUCCESS,
                                    height = 38.dp,
                                    onClick = { onInstallCloudArtifact(readyPath) },
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HybridTelemetryChip(
    label: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(0.8.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontFamily = TajawalFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = color,
            maxLines = 1
        )
    }
}
