package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.*
import com.example.engine.AdaptiveSourceAnalyzer
import com.example.engine.LocalWorkspaceEngine
import com.example.ui.components.PhaseInteractivePhoneCard
import com.example.ui.components.PwaSiteCustomizerDialog
import com.example.ui.components.RealAppIconAvatar
import com.example.ui.components.ThreeDStatPill
import com.example.ui.components.space.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * استوديو المراحل التكيفية الفضائي (Omni-Adaptive Phase Studio):
 * - يحافظ 100% على منطق التحليل التكيفي والمعاينة الحية داخل الهاتف وتوليد Keystore
 * - واجهة فضائية متكاملة مع NeonProgressIndicator وNeonGlassCard وNeonButton وSpaceDialog
 */
@Composable
fun AdaptivePhaseStudioScreen(
    initialProject: AppProject,
    activeAiPlan: AiArchitectPlan? = null,
    initialPhaseIndex: Int = 0,
    onSaveProject: (AppProject) -> Unit,
    onGenerateKeystore: (AppProject, (AppProject) -> Unit) -> Unit,
    onProceedToLiveSandbox: (AppProject, Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeProject by remember(initialProject.id) { mutableStateOf(initialProject) }
    LaunchedEffect(initialProject) {
        if (initialProject.id == activeProject.id && initialProject != activeProject) {
            activeProject = initialProject
        }
    }

    val adaptivePlan = remember(activeProject, activeAiPlan) {
        AdaptiveSourceAnalyzer.buildAdaptivePlan(
            project = activeProject,
            aiPlan = activeAiPlan
        )
    }

    var currentPhaseIndex by rememberSaveable(initialProject.id) {
        mutableIntStateOf(initialPhaseIndex.coerceIn(0, (adaptivePlan.phases.size - 1).coerceAtLeast(0)))
    }
    LaunchedEffect(adaptivePlan.phases.size) {
        val maxIdx = (adaptivePlan.phases.size - 1).coerceAtLeast(0)
        if (currentPhaseIndex > maxIdx) {
            currentPhaseIndex = maxIdx
        }
    }
    val currentPhase = adaptivePlan.phases.getOrNull(currentPhaseIndex) ?: return

    val chipListState = rememberLazyListState()
    val featureListState = rememberLazyListState()
    LaunchedEffect(currentPhaseIndex) {
        runCatching { chipListState.animateScrollToItem(currentPhaseIndex) }
        runCatching { featureListState.scrollToItem(0) }
    }

    var showPreviewPane by rememberSaveable(initialProject.id) { mutableStateOf(true) }
    var webReloadTrigger by remember { mutableIntStateOf(0) }
    var showAllPhasesDialog by remember { mutableStateOf(false) }
    var showPwaEditorDialog by remember { mutableStateOf(false) }

    val phaseAccent = remember(currentPhase.accentHex, activeProject.accentColorHex) {
        parseColorSafe(currentPhase.accentHex, parseColorSafe(activeProject.accentColorHex, SpaceColors.NeonCyan))
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val localUri = LocalWorkspaceEngine.persistImageUriToInternalFile(context, uri) ?: uri.toString()
                val updated = activeProject.copy(iconImageUri = localUri)
                activeProject = updated
                onSaveProject(updated)
                Toast.makeText(context, "تم تحديث أيقونة التطبيق الحقيقية ✓", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler {
        if (currentPhaseIndex > 0) {
            currentPhaseIndex--
        } else {
            onSaveProject(activeProject)
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SpaceColors.DeepSpace.copy(alpha = 0.96f),
                                SpaceColors.MidnightBlue.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        onClick = {
                            onSaveProject(activeProject)
                            onBack()
                        },
                        borderAccent = phaseAccent,
                        testTag = "adaptive_studio_back_btn"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeProject.name,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = SpaceColors.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${adaptivePlan.sourceKind.labelAr} • المرحلة ${currentPhaseIndex + 1} من ${adaptivePlan.phases.size}",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = phaseAccent
                        )
                    }

                    NeonIconButton(
                        icon = Icons.Filled.ViewList,
                        contentDescription = "كل المراحل",
                        onClick = { showAllPhasesDialog = true },
                        tint = SpaceColors.NeonCyan,
                        borderAccent = SpaceColors.NeonPurple,
                        testTag = "open_all_phases_btn"
                    )
                }

                Spacer(Modifier.height(8.dp))

                NeonProgressIndicator(
                    progress = ((currentPhaseIndex + 1).toFloat() / adaptivePlan.phases.size.coerceAtLeast(1)).coerceIn(0f, 1f),
                    height = 5.dp,
                    startColor = phaseAccent,
                    endColor = SpaceColors.NeonCyan
                )

                Spacer(Modifier.height(10.dp))

                // Adaptive phase chips bar
                LazyRow(
                    state = chipListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(adaptivePlan.phases) { idx, phase ->
                        val selected = idx == currentPhaseIndex
                        val chipColor = parseColorSafe(phase.accentHex, SpaceColors.NeonCyan)
                        Surface(
                            color = if (selected) chipColor.copy(alpha = 0.24f) else SpaceColors.MidnightBlue.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (selected) 1.4.dp else 1.dp,
                                color = if (selected) chipColor else SpaceColors.GlassBorderSubtle
                            ),
                            modifier = Modifier
                                .shadow(if (selected) 8.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = chipColor)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { currentPhaseIndex = idx }
                                .testTag("adaptive_phase_chip_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (selected) chipColor else SpaceColors.SurfaceElevated,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${phase.index}",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = SpaceColors.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = phase.badge,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (selected) SpaceColors.White else SpaceColors.MutedText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SpaceColors.MidnightBlue.copy(alpha = 0.95f),
                                SpaceColors.DeepSpace.copy(alpha = 0.98f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(SpaceColors.NeonCyan.copy(alpha = 0.55f), phaseAccent.copy(alpha = 0.65f))
                        ),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPhaseIndex > 0) {
                        NeonButton(
                            text = "السابق",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            style = NeonButtonStyle.CYBER_OUTLINE,
                            height = 50.dp,
                            testTag = "adaptive_prev_phase_btn",
                            onClick = { currentPhaseIndex-- }
                        )
                    }

                    val isLastPhase = currentPhaseIndex >= adaptivePlan.phases.lastIndex
                    NeonButton(
                        text = if (isLastPhase) "المرحلة النهائية: المعاينة الحية 3D ← البناء 📱" else "المرحلة التالية (${currentPhaseIndex + 2}/${adaptivePlan.phases.size})",
                        icon = if (isLastPhase) Icons.Default.RocketLaunch else Icons.AutoMirrored.Filled.ArrowForward,
                        style = if (isLastPhase) NeonButtonStyle.EMERALD_SUCCESS else NeonButtonStyle.PRIMARY_GLOW,
                        height = 50.dp,
                        testTag = if (isLastPhase) "adaptive_finish_to_sandbox_btn" else "adaptive_next_phase_btn",
                        onClick = {
                            onSaveProject(activeProject)
                            if (!isLastPhase) {
                                currentPhaseIndex++
                            } else {
                                onProceedToLiveSandbox(activeProject, currentPhaseIndex)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (!isLastPhase) {
                        NeonIconButton(
                            icon = Icons.Filled.PhoneAndroid,
                            contentDescription = "معاينة حية وبناء",
                            onClick = {
                                onSaveProject(activeProject)
                                onProceedToLiveSandbox(activeProject, currentPhaseIndex)
                            },
                            tint = SpaceColors.NeonCyan,
                            borderAccent = SpaceColors.NeonCyan,
                            size = 50.dp,
                            testTag = "adaptive_quick_sandbox_btn"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = SpaceDimensions.MaxContentWidth)
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .testTag("adaptive_phase_studio_screen")
            ) {
                PhaseInteractivePhoneCard(
                    project = activeProject,
                    currentPhase = currentPhase,
                    totalPhases = adaptivePlan.phases.size,
                    phaseAccent = phaseAccent,
                    isExpandedPane = showPreviewPane,
                    reloadTrigger = webReloadTrigger,
                    onToggleCollapsePane = { showPreviewPane = !showPreviewPane },
                    onReloadWebView = { webReloadTrigger++ },
                    onProceedToFullSandbox = {
                        onSaveProject(activeProject)
                        onProceedToLiveSandbox(activeProject, currentPhaseIndex)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 6.dp)
                )

                LazyColumn(
                    state = featureListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 30.dp)
                ) {
                    item {
                        val rep = adaptivePlan.report
                        NeonGlassCard(
                            accentColor = phaseAccent,
                            secondaryAccent = SpaceColors.NeonCyan,
                            elevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = currentPhase.title,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = SpaceColors.White
                                    )
                                    Text(
                                        text = currentPhase.subtitle,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        color = SpaceColors.MutedText
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ThreeDStatPill("المراحل", "${adaptivePlan.phases.size}", phaseAccent, Modifier.weight(1f))
                                    if (activeProject.sourceType == SourceType.APK_PACKAGE) {
                                        ThreeDStatPill("الصلاحيات", "${rep.permissionCount}", SpaceColors.NeonCyan, Modifier.weight(1f))
                                        ThreeDStatPill("الميزات", "${currentPhase.features.size}", SpaceColors.Emerald, Modifier.weight(1f))
                                    } else {
                                        ThreeDStatPill("الصور", "${rep.imageCount}", SpaceColors.NeonCyan, Modifier.weight(1f))
                                        ThreeDStatPill("الأزرار", "${rep.buttonCount}", SpaceColors.Gold, Modifier.weight(1f))
                                        ThreeDStatPill("النصوص", "${rep.textBlockCount}", SpaceColors.NeonPurple, Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    if (!currentPhase.supportsLiveWebPreview && activeProject.sourceType == SourceType.APK_PACKAGE) {
                        item {
                            NeonGlassCard(
                                accentColor = SpaceColors.Emerald,
                                secondaryAccent = SpaceColors.NeonCyan,
                                elevation = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    RealAppIconAvatar(project = activeProject, size = 56.dp, cornerRadius = 16.dp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(activeProject.name, fontFamily = CairoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SpaceColors.White)
                                        Text(activeProject.packageConfig.packageName, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = SpaceColors.NeonCyan)
                                        Text(
                                            text = "الإصدار ${activeProject.packageConfig.versionName} (${activeProject.packageConfig.versionCode})",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            color = SpaceColors.MutedText
                                        )
                                    }
                                    Surface(
                                        color = SpaceColors.Emerald.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, SpaceColors.Emerald.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = "APK Mode",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = SpaceColors.Emerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(currentPhase.features, key = { "${currentPhase.index}_${it.id}" }) { feature ->
                        DynamicFeatureCard(
                            feature = feature,
                            accentColor = phaseAccent,
                            onUpdateFeature = { newBool, newInt, newText ->
                                val updated = AdaptiveSourceAnalyzer.applyFeatureEdit(
                                    context = context,
                                    project = activeProject,
                                    feature = feature,
                                    newBool = newBool,
                                    newInt = newInt,
                                    newText = newText
                                )
                                activeProject = updated
                                onSaveProject(updated)
                                if (currentPhase.supportsLiveWebPreview) {
                                    webReloadTrigger++
                                }
                            },
                            onTriggerAction = { bindingKey ->
                                when (bindingKey) {
                                    "action.open_pwa_editor" -> showPwaEditorDialog = true
                                    "action.pick_icon" -> iconPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                    "action.generate_keystore" -> onGenerateKeystore(activeProject) { signedProj ->
                                        activeProject = signedProj
                                        onSaveProject(signedProj)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAllPhasesDialog) {
        SpaceDialog(
            title = "جميع المراحل المتولدة (${adaptivePlan.phases.size})",
            accentColor = phaseAccent,
            onDismissRequest = { showAllPhasesDialog = false }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(adaptivePlan.phases) { idx, ph ->
                    val isCurr = idx == currentPhaseIndex
                    val c = parseColorSafe(ph.accentHex, SpaceColors.NeonCyan)
                    NeonGlassCard(
                        accentColor = if (isCurr) c else SpaceColors.ElectricBlue.copy(alpha = 0.45f),
                        secondaryAccent = c,
                        elevation = if (isCurr) 10.dp else 4.dp,
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            currentPhaseIndex = idx
                            showAllPhasesDialog = false
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = c.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${ph.index}",
                                    color = SpaceColors.White,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ph.title, color = SpaceColors.White, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${ph.features.size} ميزة تفاعلية", color = SpaceColors.MutedText, fontFamily = TajawalFontFamily, fontSize = 11.sp)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    if (showPwaEditorDialog) {
        PwaSiteCustomizerDialog(
            initialProject = activeProject,
            onDismiss = { showPwaEditorDialog = false },
            onApplyAndPreview = { editedProj ->
                activeProject = editedProj
                onSaveProject(editedProj)
                showPwaEditorDialog = false
                webReloadTrigger++
            },
            onApplyAndSave = { editedProj ->
                activeProject = editedProj
                onSaveProject(editedProj)
                showPwaEditorDialog = false
                webReloadTrigger++
                Toast.makeText(context, "تم تحديث محرر الموقع المحلي ✓", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun DynamicFeatureCard(
    feature: DynamicPhaseFeature,
    accentColor: Color,
    onUpdateFeature: (Boolean, Int, String) -> Unit,
    onTriggerAction: (String) -> Unit
) {
    var localBool by remember(feature.id, feature.boolValue) { mutableStateOf(feature.boolValue) }
    var localInt by remember(feature.id, feature.intValue) { mutableIntStateOf(feature.intValue) }
    var localText by remember(feature.id, feature.textValue) { mutableStateOf(feature.textValue) }

    NeonGlassCard(
        accentColor = accentColor,
        secondaryAccent = SpaceColors.NeonPurple,
        elevation = 8.dp,
        shape = RoundedCornerShape(18.dp),
        testTag = "dynamic_feature_${feature.id}"
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = accentColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = feature.categoryTag,
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (feature.type == DynamicFeatureType.SLIDER) "${feature.title} ($localInt)" else feature.title,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SpaceColors.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (feature.subtitle.isNotBlank()) {
                        Text(
                            text = feature.subtitle,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = SpaceColors.MutedText
                        )
                    }
                }

                if (feature.type == DynamicFeatureType.SWITCH) {
                    Switch(
                        checked = localBool,
                        onCheckedChange = { checked ->
                            localBool = checked
                            onUpdateFeature(checked, localInt, localText)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SpaceColors.DeepSpace,
                            checkedTrackColor = accentColor,
                            uncheckedTrackColor = SpaceColors.SurfaceCard
                        )
                    )
                }
            }

            when (feature.type) {
                DynamicFeatureType.TEXT_INPUT -> {
                    val isModified = localText != feature.textValue
                    LaunchedEffect(localText, feature.id) {
                        if (localText != feature.textValue) {
                            delay(550)
                            onUpdateFeature(localBool, localInt, localText)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonTextField(
                            value = localText,
                            onValueChange = { localText = it },
                            accentColor = accentColor,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { onUpdateFeature(localBool, localInt, localText) }
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        NeonButton(
                            text = if (isModified) "حفظ ✓" else "تطبيق",
                            style = if (isModified) NeonButtonStyle.EMERALD_SUCCESS else NeonButtonStyle.CYBER_OUTLINE,
                            height = 50.dp,
                            onClick = { onUpdateFeature(localBool, localInt, localText) }
                        )
                    }
                }

                DynamicFeatureType.SLIDER -> {
                    Slider(
                        value = localInt.toFloat(),
                        onValueChange = { localInt = it.toInt() },
                        onValueChangeFinished = { onUpdateFeature(localBool, localInt, localText) },
                        valueRange = feature.minInt.toFloat()..feature.maxInt.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = SpaceColors.SurfaceDeep
                        )
                    )
                }

                DynamicFeatureType.COLOR_PICKER -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(feature.options) { hex ->
                            val c = parseColorSafe(hex, accentColor)
                            val selected = localText.equals(hex, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        localText = hex
                                        onUpdateFeature(localBool, localInt, hex)
                                    }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            if (selected) 3.dp else 1.dp,
                                            if (selected) SpaceColors.White else SpaceColors.White.copy(alpha = 0.25f),
                                            CircleShape
                                        )
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "مختار",
                                            tint = SpaceColors.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                DynamicFeatureType.CHIP_GROUP -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(feature.options) { opt ->
                            val chosen = localText == opt
                            FilterChip(
                                selected = chosen,
                                onClick = {
                                    localText = opt
                                    onUpdateFeature(localBool, localInt, opt)
                                },
                                label = { Text(opt, fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = SpaceColors.SurfaceDeep,
                                    labelColor = SpaceColors.MutedText,
                                    selectedContainerColor = accentColor.copy(alpha = 0.24f),
                                    selectedLabelColor = SpaceColors.White
                                )
                            )
                        }
                    }
                }

                DynamicFeatureType.ACTION_TRIGGER -> {
                    NeonButton(
                        text = "تنفيذ الآن",
                        icon = Icons.Filled.Tune,
                        style = NeonButtonStyle.PRIMARY_GLOW,
                        height = 48.dp,
                        onClick = { onTriggerAction(feature.bindingKey) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DynamicFeatureType.SWITCH -> Unit
            }
        }
    }
}
