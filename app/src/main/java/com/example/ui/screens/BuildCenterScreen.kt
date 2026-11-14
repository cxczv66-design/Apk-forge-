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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.AppProject
import com.example.engine.BuildAuditItem
import com.example.engine.BuildOptimizationOptions
import com.example.engine.GitHubCloudBuildEngine
import com.example.engine.ProjectExporter
import com.example.engine.QuantumBuildEngine
import com.example.engine.QuantumDiagnosticReport
import com.example.engine.ToolchainManager
import com.example.ui.components.AdaptiveBuildPipelineBoard
import com.example.ui.components.AdaptiveIconAndUiStudioDialog
import com.example.ui.components.BridgeAndCloudConfigDialog
import com.example.ui.components.BuildHistoryCard
import com.example.ui.components.CloudAndBridgeFactoryCard
import com.example.ui.components.GitHubCloudBuildDialog
import com.example.ui.components.InAppToolchainTerminalCard
import com.example.ui.components.PlayStorePrepDialog
import com.example.ui.components.ProStudioHubCard
import com.example.ui.components.SecurityAndBridgeV2Dialog
import com.example.ui.components.TerminalLogView
import com.example.ui.components.WebCustomizerDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch

import com.example.ui.components.BuildCompleteBottomSheet
import com.example.ui.components.LiveBuildTerminalDialog
import com.example.ui.components.OmniBuildTargetSuiteCard

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
    onBuildCompleted: (updatedProject: AppProject, fileName: String, outputPath: String, sizeText: String) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val effectiveToolchain = toolchainManager ?: remember(context) { ToolchainManager(context) }
    val projectExporter = remember(context) { ProjectExporter(context) }
    val githubCloudEngine = remember(context) { GitHubCloudBuildEngine(context) }
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

    val isBuilding = engineState is QuantumBuildEngine.EngineStatus.Running

    fun launchAutomatedBuild(targetType: String) {
        buildSessionStarted = true
        showLiveTerminalModal = true
        if (onStartAutomatedBuild != null) {
            onStartAutomatedBuild.invoke(activeProject, targetType, buildOptions, autoBumpVersion)
        } else {
            // Safe background fallback when invoked without ViewModel
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
        containerColor = DarkBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, enabled = !isBuilding) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = if (isBuilding) TextMuted else TextPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = !isBuilding) { onEditProject() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "تعديل",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeProject.name,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onOpenDownloads) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "التنزيلات",
                        tint = TextPrimary
                    )
                }
            }
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
                // Adaptive 2-Pane Workbench for tablets/foldables: Controls on right, Live Pipeline + Console on left
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
                            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
                        ) {
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
                            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
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
                // Adaptive Handheld Flow: Live Pipeline right below controls so user watches live progress
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp)
                ) {
                    item {
                        InteractivePhonePreviewCard(activeProject, onOpenInteractivePhone)
                    }

                    // Live Adaptive Pipeline Board (Always visible so user sees live build stages & assembly)
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

                    // Powerful Automated Build Suite + Gateway to Live 3D Sandbox
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

                    // Pre-Build Automated Doctor & 1-Tap Fix
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

                    // Warning Banner (Shown until keystore generated)
                    if (!keystoreGenerated) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF231C13))
                                    .border(1.dp, Color(0x66FF7A2F), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "لا يوجد مفتاح توقيع في التنزيلات بعد، أنشئ واحداً أدناه وسيوثق به كل APK و AAB تبنيه - احتفظ به بأمان، فقدانه يوقف تحديثات كل التطبيقات المبنية به.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Card 3: مفتاح التوقيع
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

                    // Cloud CI + JS Bridge + Google Play Prep Factory Card
                    item {
                        CloudAndBridgeFactoryCard(
                            project = activeProject,
                            onExportAndroidStudioZip = { exportStudioZipAndShare() },
                            onOpenBridgeConfig = { showBridgeDialog = true },
                            onOpenPlayStorePrep = { showPlayPrepDialog = true },
                            onExportApkForgeJson = { exportApkForgeProjectFile() }
                        )
                    }

                    // Unified Organized 4-Tile Pro Studio Hub
                    item {
                        ProStudioHubCard(
                            project = activeProject,
                            onOpenWebCustomizer = { showWebCustomizerDialog = true },
                            onOpenSecurityAndBridgeV2 = { showSecurityV2Dialog = true },
                            onOpenIconAndUiStudio = { showIconUiStudioDialog = true },
                            onOpenGitHubCloudTrigger = { showGitHubCloudDialog = true }
                        )
                    }

                    // Build Version History (per-project)
                    if (activeProject.buildHistory.isNotEmpty()) {
                        item {
                            BuildHistoryCard(history = activeProject.buildHistory)
                        }
                    }

                    // In-App Build & Signing Toolchain Installer + Organized Linear Terminal
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

                    // Quantum On-Device Pipeline & Security Matrix Card
                    item {
                        QuantumEngineDirectCard(
                            quantumReport = quantumReport,
                            isBuilding = isBuilding,
                            onRunQuantumBuild = { launchAutomatedBuild("release") },
                            onOpenDiagnostic = onOpenDiagnostic
                        )
                    }

                    // Terminal Logs Window
                    item {
                        Text(
                            text = "سجل أوامر البناء (Build Engine Log):",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
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
            Dialog(
                onDismissRequest = { showScriptDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    color = Color(0xFF141925),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.8f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مواصفات أوامر Quantum المدمجة (aapt2 + d8 + zipalign + apksig)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showScriptDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(TerminalBackground)
                                .border(1.dp, Color(0xFF232C3E), RoundedCornerShape(12.dp))
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

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("ApkForge Build Script", fullScript))
                                Toast.makeText(context, "تم نسخ السكربت الكامل للحافظة ✓", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("نسخ السكربت الكامل", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Quantum KeyVault Encrypted Keystore Generator Dialog
        if (showKeystoreDialog) {
            val keytoolCmd = remember(keyAlias, keyPassword, keyOrg, keyValidityYears) {
                val safeAlias = keyAlias.trim().ifBlank { "apkforge_release" }
                "quantum-keyvault seal --alias=$safeAlias --cipher=AndroidKeyStore-AES256-GCM --cert=BouncyCastle-X509-RSA2048 --org=\"$keyOrg\" --validity=${keyValidityYears}y"
            }
            Dialog(
                onDismissRequest = { showKeystoreDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { showKeystoreDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = Color(0xFF1B1D2C),
                        shape = RoundedCornerShape(22.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clickable(enabled = false) {}
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(OrangePrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(26.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "خزنة التوقيع المشفّرة (Quantum KeyVault)",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "يستخدم لتوقيع تطبيقات APK و AAB للنشر في Google Play",
                                fontFamily = TajawalFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = keyAlias,
                                onValueChange = { keyAlias = it },
                                label = { Text("اسم المفتاح (Key Alias)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = keyPassword,
                                onValueChange = { keyPassword = it },
                                label = { Text("كلمة المرور (Key Password)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = keyOrg,
                                onValueChange = { keyOrg = it },
                                label = { Text("اسم المطور / المؤسسة (Organization)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = keyValidityYears,
                                onValueChange = { keyValidityYears = it },
                                label = { Text("مدة الصلاحية بالسنوات (Validity)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(
                                color = TerminalBackground,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232C3E)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = keytoolCmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = TerminalGreen,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showKeystoreDialog = false },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("إلغاء", fontFamily = CairoFontFamily, color = TextMuted)
                                }

                                Button(
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
                                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("توليد وحفظ ✓", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
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
                onDismiss = { showGitHubCloudDialog = false }
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
    val accent = parseColorSafe(project.accentColorHex, OrangePrimary)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF1E172E), Color(0xFF151928))))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable { onOpenInteractivePhone() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "محاكي الهاتف التفاعلي المتطور",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "المس وجرب التطبيق والتنقل حياً قبل البناء",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Button(
                onClick = onOpenInteractivePhone,
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "معاينة 📱",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
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
        Surface(
            color = Color(0xFF131924),
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.35f)),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "أدوات البناء التلقائية المتقدمة",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "تحسين الحجم، تقسيم المعالجات، ورفع الإصدار v${activeProject.packageConfig.versionCode}",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onShowScript) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "عرض السكربت",
                            tint = OrangePrimary
                        )
                    }
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

                Button(
                    onClick = onDualBuild,
                    enabled = !isBuilding,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("build_dual_auto_btn")
                ) {
                    Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "بناء تلقائي شامل (APK + AAB معاً) ⚡",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OptChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) OrangePrimary.copy(alpha = 0.18f) else Color(0xFF0E1218),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) OrangePrimary else Color(0xFF232C3E)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) OrangePrimary else TextMuted,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 11.sp,
                color = if (selected) TextPrimary else TextSecondary
            )
        }
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
        Surface(
            color = Color(0xFF131924),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (allPassed) StatusGreen.copy(alpha = 0.45f) else Color(0xFF283245)
            ),
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
                            tint = if (allPassed) StatusGreen else WarningYellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "فاحص الجاهزية التلقائي ($passedCount/${auditItems.size})",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (allPassed) "المشروع جاهز 100% للنشر والبناء" else "توجد تحسينات مقترحة يمكن إصلاحها بضغطة",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (!allPassed) {
                        Button(
                            onClick = onAutoFixAll,
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "إصلاح تلقائي ⚡",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
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
                                    .background(Color(0xFF0E131C))
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (item.passed) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (item.passed) StatusGreen else OrangePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.titleAr,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = item.descriptionAr,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 10.sp,
                                        color = TextSecondary
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = running.currentTask,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = GoldAccent
            )
            Text(
                text = "${(running.progress * 100).toInt()}%",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = OrangePrimary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { running.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = OrangePrimary,
            trackColor = DarkSurface
        )
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
    val statusColor = if (ready) StatusGreen else WarningYellow
    val statusText = if (ready) {
        "جاهز للبناء الذاتي داخل التطبيق (v1-v4 • ${quantumReport.vaultProtectionMode})"
    } else {
        quantumReport.diagnosticSummaryAr
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(
                1.dp,
                if (ready) StatusGreen.copy(alpha = 0.4f) else GlassBorderSubtle,
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = statusColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "نظام البناء والأمان المدمج (Quantum Engine)",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = statusText,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = statusColor
                        )
                    }
                }
            }

            // Quantum Capabilities Badges
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
                    Surface(
                        color = if (active) StatusGreen.copy(alpha = 0.14f) else Color(0xFF141B27),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) StatusGreen.copy(alpha = 0.45f) else Color(0xFF253042)
                        )
                    ) {
                        Text(
                            text = if (active) "✓ $label" else label,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) StatusGreen else TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDiagnostic,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = "تشغيل فحص المحرك ✓",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = onRunQuantumBuild,
                    enabled = !isBuilding,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp)
                ) {
                    Text(
                        text = if (isBuilding) "جارٍ البناء..." else "بناء وتوقيع Release",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = OrangePrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Button(
                onClick = onBuildClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .defaultMinSize(minWidth = 78.dp, minHeight = 42.dp)
                    .testTag(testTag)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = buttonText,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectOutputPackageCard(
    fileName: String,
    onInspect: () -> Unit
) {
    Surface(
        color = Color(0xFF0E1826),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ManageSearch, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "فحص الحزمة النهائية (APK Inspector)",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "تحليل classes.dex و AndroidManifest لـ $fileName",
                        fontFamily = TajawalFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onInspect,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("فحص ✓", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

