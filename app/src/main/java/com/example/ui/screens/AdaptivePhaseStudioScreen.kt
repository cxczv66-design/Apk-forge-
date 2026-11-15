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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.*
import com.example.engine.AdaptiveSourceAnalyzer
import com.example.engine.LocalWorkspaceEngine
import com.example.ui.components.PhaseInteractivePhoneCard
import com.example.ui.components.PwaSiteCustomizerDialog
import com.example.ui.components.RealAppIconAvatar
import com.example.ui.components.ThreeDStatPill
import com.example.ui.components.ThreeDSurfaceCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * استوديو المراحل التكيفية الموحد (Omni-Adaptive Phase Studio):
 * - يكيّف عدد المراحل والميزات تلقائياً حسب المصدر وحجمه (4 للـ APK، 5-100 للويب، 12+ للذكاء الاصطناعي).
 * - يعرض معاينة ويب حية تفاعلية في كل مرحلة ويب مع تحديث فوري لتعديلات الألوان والنصوص والصور والأزرار.
 * - ينقل المستخدم في النهاية إلى شاشة المعاينة الحية 3D ومنها حصراً إلى البناء النهائي (APK / AAB / APKS).
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
        parseColorSafe(currentPhase.accentHex, parseColorSafe(activeProject.accentColorHex, OrangePrimary))
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
        containerColor = DarkBackground,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onSaveProject(activeProject)
                            onBack()
                        },
                        modifier = Modifier.testTag("adaptive_studio_back_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activeProject.name,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TextPrimary,
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

                    IconButton(
                        onClick = { showAllPhasesDialog = true },
                        modifier = Modifier.testTag("open_all_phases_btn")
                    ) {
                        Icon(Icons.Filled.ViewList, contentDescription = "كل المراحل", tint = OrangePrimary)
                    }
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = {
                        ((currentPhaseIndex + 1).toFloat() / adaptivePlan.phases.size.coerceAtLeast(1)).coerceIn(0f, 1f)
                    },
                    color = phaseAccent,
                    trackColor = DarkSurfaceElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Spacer(Modifier.height(8.dp))

                // Adaptive phase chips bar
                LazyRow(
                    state = chipListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(adaptivePlan.phases) { idx, phase ->
                        val selected = idx == currentPhaseIndex
                        val chipColor = parseColorSafe(phase.accentHex, OrangePrimary)
                        Surface(
                            color = if (selected) chipColor.copy(alpha = 0.22f) else DarkSurfaceElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (selected) chipColor else GlassBorderSubtle),
                            modifier = Modifier
                                .clickable { currentPhaseIndex = idx }
                                .testTag("adaptive_phase_chip_$idx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (selected) chipColor else Color.White.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${phase.index}",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    text = phase.badge,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (selected) TextPrimary else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                border = BorderStroke(1.dp, GlassBorderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPhaseIndex > 0) {
                        OutlinedButton(
                            onClick = { currentPhaseIndex-- },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("adaptive_prev_phase_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("السابق", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    val isLastPhase = currentPhaseIndex >= adaptivePlan.phases.lastIndex
                    Button(
                        onClick = {
                            onSaveProject(activeProject)
                            if (!isLastPhase) {
                                currentPhaseIndex++
                            } else {
                                onProceedToLiveSandbox(activeProject, currentPhaseIndex)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .background(
                                Brush.horizontalGradient(
                                    if (isLastPhase) listOf(StatusGreen, CyanAccent) else listOf(phaseAccent, OrangePrimary)
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .testTag(if (isLastPhase) "adaptive_finish_to_sandbox_btn" else "adaptive_next_phase_btn")
                    ) {
                        Text(
                            text = if (isLastPhase) "المرحلة النهائية: المعاينة الحية 3D ← البناء 📱" else "المرحلة التالية (${currentPhaseIndex + 2}/${adaptivePlan.phases.size})",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isLastPhase) {
                        FilledTonalIconButton(
                            onClick = {
                                onSaveProject(activeProject)
                                onProceedToLiveSandbox(activeProject, currentPhaseIndex)
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("adaptive_quick_sandbox_btn")
                        ) {
                            Icon(Icons.Filled.PhoneAndroid, contentDescription = "معاينة حية وبناء", tint = CyanAccent)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 30.dp)
            ) {
                // Complexity report telemetry header
                item {
                    val rep = adaptivePlan.report
                    ThreeDSurfaceCard(accentColor = phaseAccent, elevation = 10.dp) {
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
                                    color = TextPrimary
                                )
                                Text(
                                    text = currentPhase.subtitle,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ThreeDStatPill("المراحل", "${adaptivePlan.phases.size}", phaseAccent, Modifier.weight(1f))
                                if (activeProject.sourceType == SourceType.APK_PACKAGE) {
                                    ThreeDStatPill("الصلاحيات", "${rep.permissionCount}", CyanAccent, Modifier.weight(1f))
                                    ThreeDStatPill("الميزات", "${currentPhase.features.size}", StatusGreen, Modifier.weight(1f))
                                } else {
                                    ThreeDStatPill("الصور", "${rep.imageCount}", CyanAccent, Modifier.weight(1f))
                                    ThreeDStatPill("الأزرار", "${rep.buttonCount}", OrangePrimary, Modifier.weight(1f))
                                    ThreeDStatPill("النصوص", "${rep.textBlockCount}", Color(0xFFA855F7), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // 3D APK Identity Card inside APK phases
                if (!currentPhase.supportsLiveWebPreview && activeProject.sourceType == SourceType.APK_PACKAGE) {
                item {
                    ThreeDSurfaceCard(accentColor = StatusGreen, elevation = 10.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RealAppIconAvatar(project = activeProject, size = 56.dp, cornerRadius = 16.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(activeProject.name, fontFamily = CairoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)
                                Text(activeProject.packageConfig.packageName, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = CyanAccent)
                                Text(
                                    text = "الإصدار ${activeProject.packageConfig.versionName} (${activeProject.packageConfig.versionCode})",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Surface(color = StatusGreen.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
                                Text(
                                    text = "APK Mode",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = StatusGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dynamic features inside the current adaptive phase
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

    if (showAllPhasesDialog) {
        Dialog(
            onDismissRequest = { showAllPhasesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, phaseAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.78f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جميع المراحل المتولدة (${adaptivePlan.phases.size})",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        IconButton(onClick = { showAllPhasesDialog = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = TextSecondary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(adaptivePlan.phases) { idx, ph ->
                            val isCurr = idx == currentPhaseIndex
                            val c = parseColorSafe(ph.accentHex, OrangePrimary)
                            Surface(
                                color = if (isCurr) c.copy(alpha = 0.2f) else DarkSurfaceElevated,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isCurr) c else GlassBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentPhaseIndex = idx
                                        showAllPhasesDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("${ph.index}", color = c, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ph.title, color = TextPrimary, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${ph.features.size} ميزة تفاعلية", color = TextSecondary, fontFamily = TajawalFontFamily, fontSize = 11.sp)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c, modifier = Modifier.size(16.dp))
                                }
                            }
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

    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dynamic_feature_${feature.id}")
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
                        Surface(color = accentColor.copy(alpha = 0.16f), shape = RoundedCornerShape(6.dp)) {
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
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (feature.subtitle.isNotBlank()) {
                        Text(
                            text = feature.subtitle,
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                if (feature.type == DynamicFeatureType.SWITCH) {
                    Switch(
                        checked = localBool,
                        onCheckedChange = { checked ->
                            localBool = checked
                            onUpdateFeature(checked, localInt, localText)
                        }
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
                        OutlinedTextField(
                            value = localText,
                            onValueChange = { localText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { onUpdateFeature(localBool, localInt, localText) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = { onUpdateFeature(localBool, localInt, localText) },
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text(
                                text = if (isModified) "حفظ ✓" else "تطبيق",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                DynamicFeatureType.SLIDER -> {
                    Slider(
                        value = localInt.toFloat(),
                        onValueChange = { localInt = it.toInt() },
                        onValueChangeFinished = { onUpdateFeature(localBool, localInt, localText) },
                        valueRange = feature.minInt.toFloat()..feature.maxInt.toFloat()
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
                                            if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                                            CircleShape
                                        )
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "مختار",
                                            tint = Color.White,
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
                            FilterChip(
                                selected = localText == opt,
                                onClick = {
                                    localText = opt
                                    onUpdateFeature(localBool, localInt, opt)
                                },
                                label = { Text(opt, fontFamily = TajawalFontFamily, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                DynamicFeatureType.ACTION_TRIGGER -> {
                    Button(
                        onClick = { onTriggerAction(feature.bindingKey) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("تنفيذ الآن", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                DynamicFeatureType.SWITCH -> Unit
            }
        }
    }
}
