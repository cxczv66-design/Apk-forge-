package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AiArchitectPlan
import com.example.domain.model.AiSmartQuestion
import com.example.ui.AiWizardUiState
import com.example.ui.components.space.*
import com.example.ui.theme.*

private val AI_IDEA_PRESETS = listOf(
    "متجر إلكتروني" to "متجر ذكي مع سلة تسوق، تتبع الطلبات، وإشعارات العروض",
    "مدونة تقنية" to "منصة مقالات ومراجعات مع وضع قراءة أوفلاين وتعليقات",
    "منصة تعليمية" to "أكاديمية دورات ومحاضرات مع اختبارات وشهادات للطلاب",
    "مطعم وتوصيل" to "قائمة طعام مصورة وحجز طاولات وتتبع التوصيل على الخريطة",
    "تطبيق خدمات" to "حجز مواعيد وخدمات منزلية مع تواصل مباشر وتنبيهات",
    "محفظة أعمال" to "معرض أعمال 3D احترافي وسيرة ذاتية ونموذج تواصل"
)

/**
 * مساعد الذكاء الاصطناعي المستقبلي (AI Wizard Space Edition):
 * - روبوت AI فضائي تفاعلي
 * - اقتراحات جاهزة مضيئة + حقل إدخال زجاجي (NeonTextField)
 * - مؤشر توليد فضائي متحرك (FuturisticLoading)
 * - عرض الأسئلة الذكية وخارطة المراحل المنظمة + زر البناء الفوري
 */
@Composable
fun AiWizardScreen(
    uiState: AiWizardUiState,
    onGeneratePlan: (ideaPrompt: String, presetCategory: String?) -> Unit,
    onConfirmPlanAndMaterialize: (plan: AiArchitectPlan, answers: List<AiSmartQuestion>) -> Unit,
    onResetToPrompt: () -> Unit,
    onBack: () -> Unit
) {
    var ideaText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf<String?>(null) }
    var lastPresetDesc by remember { mutableStateOf<String?>(null) }
    var editableQuestions by remember { mutableStateOf<List<AiSmartQuestion>>(emptyList()) }

    LaunchedEffect(uiState) {
        if (uiState is AiWizardUiState.PlanReady) {
            editableQuestions = uiState.plan.smartQuestions
        }
    }

    BackHandler {
        if (uiState is AiWizardUiState.PlanReady) {
            onResetToPrompt()
        } else {
            onBack()
        }
    }

    val aiPurple = SpaceColors.NeonPurple
    val aiCyan = SpaceColors.NeonCyan

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = "مساعد البناء الذكي (AI Wizard)",
                subtitle = "بناء معمارية تطبيق كاملة من الصفر",
                onBack = {
                    if (uiState is AiWizardUiState.PlanReady) onResetToPrompt() else onBack()
                },
                backTestTag = "ai_wizard_back_btn",
                accentColor = aiPurple
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = SpaceDimensions.MaxContentWidth)
                    .imePadding()
                    .padding(horizontal = 16.dp)
                    .testTag("ai_wizard_screen"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                when (uiState) {
                    is AiWizardUiState.Idle, is AiWizardUiState.Error -> {
                        item {
                            NeonGlassCard(
                                accentColor = aiPurple,
                                secondaryAccent = aiCyan,
                                elevation = 16.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SpaceFloatingRobotAvatar(
                                        size = 78.dp,
                                        accentColor = aiCyan,
                                        showOnlineDot = true
                                    )
                                    Text(
                                        text = "حوّل فكرتك إلى تطبيق متكامل",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 19.sp,
                                        color = SpaceColors.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "اختر فكرة جاهزة أو صف تطبيقك بحرية. سيقوم الذكاء الاصطناعي ببناء خارطة طريق من 12 مرحلة وأسئلة تخصيص ذكية.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 13.sp,
                                        color = SpaceColors.MutedText,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }

                        if (uiState is AiWizardUiState.Error) {
                            item {
                                NeonGlassCard(
                                    accentColor = SpaceColors.ErrorRed,
                                    secondaryAccent = SpaceColors.ErrorRed,
                                    testTag = "ai_error_banner"
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = SpaceColors.ErrorRed)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "تعذر توليد الخطة الذكية",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = SpaceColors.ErrorRed
                                            )
                                            Text(
                                                text = uiState.message,
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 12.sp,
                                                color = SpaceColors.White
                                            )
                                        }
                                        if (ideaText.isNotBlank() || selectedPreset != null) {
                                            NeonButton(
                                                text = "إعادة",
                                                style = NeonButtonStyle.DANGER_RED,
                                                height = 48.dp,
                                                onClick = { onGeneratePlan(ideaText, selectedPreset) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            PremiumSectionHeader(
                                title = "أفكار سريعة جاهزة",
                                icon = Icons.Default.AutoAwesome,
                                accentColor = aiPurple
                            )
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(AI_IDEA_PRESETS) { (title, desc) ->
                                    val isSelected = selectedPreset == title
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                selectedPreset = null
                                                if (ideaText == desc) ideaText = ""
                                                lastPresetDesc = null
                                            } else {
                                                selectedPreset = title
                                                if (ideaText.isBlank() || ideaText == lastPresetDesc) {
                                                    ideaText = desc
                                                }
                                                lastPresetDesc = desc
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = title,
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Bolt,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = SpaceColors.MidnightBlue.copy(alpha = 0.8f),
                                            labelColor = SpaceColors.MutedText,
                                            selectedContainerColor = aiPurple.copy(alpha = 0.30f),
                                            selectedLabelColor = SpaceColors.White,
                                            selectedLeadingIconColor = aiCyan
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = aiCyan.copy(alpha = 0.25f),
                                            selectedBorderColor = aiPurple
                                        )
                                    )
                                }
                            }
                        }

                        item {
                            NeonGlassCard(
                                accentColor = aiCyan,
                                secondaryAccent = aiPurple,
                                elevation = 14.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "اكتب فكرة تطبيقك بالتفصيل",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SpaceColors.White
                                    )
                                    NeonTextField(
                                        value = ideaText,
                                        onValueChange = { ideaText = it },
                                        placeholder = "مثال: تطبيق حجز ملاعب كرة قدم مع دفع إلكتروني، إشعارات قبل الموعد، وخريطة قريبة...",
                                        accentColor = aiCyan,
                                        singleLine = false,
                                        minLines = 4,
                                        maxLines = 7,
                                        testTag = "ai_idea_input"
                                    )

                                    NeonButton(
                                        text = "توليد خطة العمل والأسئلة الذكية ✨",
                                        icon = Icons.Filled.AutoAwesome,
                                        style = NeonButtonStyle.PURPLE_AI,
                                        enabled = ideaText.isNotBlank() || selectedPreset != null,
                                        testTag = "ai_generate_roadmap_btn",
                                        onClick = { onGeneratePlan(ideaText, selectedPreset) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    is AiWizardUiState.GeneratingPlan, is AiWizardUiState.Materializing -> {
                        item {
                            FuturisticLoading(
                                title = if (uiState is AiWizardUiState.Materializing) {
                                    "جارٍ بناء المراحل التفاعلية وملفات التطبيق..."
                                } else {
                                    "الذكاء الاصطناعي يحلل فكرتك ويبني خارطة طريق من 12 مرحلة..."
                                },
                                subtitle = "توليد الميزات، الشاشات، والأسئلة الذكية الخاصة بفكرتك",
                                accentColor = aiPurple
                            )
                        }
                    }

                    is AiWizardUiState.PlanReady -> {
                        val plan = uiState.plan
                        val planAccent = parseColorSafe(plan.accentColorHex, aiPurple)

                        item {
                            NeonGlassCard(
                                accentColor = planAccent,
                                secondaryAccent = aiCyan,
                                elevation = 16.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = planAccent.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, planAccent.copy(alpha = 0.55f))
                                        ) {
                                            Text(
                                                text = "${plan.roadmap.size} مرحلة عمل • AI",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = planAccent,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                        TextButton(onClick = onResetToPrompt) {
                                            Text("تعديل الفكرة", fontFamily = CairoFontFamily, fontSize = 12.sp, color = aiCyan)
                                        }
                                    }
                                    Text(
                                        text = plan.appNameAr,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = SpaceColors.White
                                    )
                                    Text(
                                        text = plan.summaryAr,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 13.sp,
                                        color = SpaceColors.MutedText,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                        }

                        if (editableQuestions.isNotEmpty()) {
                            item {
                                PremiumSectionHeader(
                                    title = "أسئلة ذكية لتخصيص تطبيقك",
                                    icon = Icons.Default.Psychology,
                                    accentColor = aiCyan
                                )
                            }

                            items(editableQuestions.indices.toList()) { qIdx ->
                                val q = editableQuestions[qIdx]
                                NeonGlassCard(
                                    accentColor = aiCyan,
                                    secondaryAccent = aiPurple,
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (q.kind == "choice" && q.options.isNotEmpty()) {
                                            Text(
                                                text = q.questionAr,
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = SpaceColors.White
                                            )
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(q.options) { opt ->
                                                    val chosen = q.selectedOption == opt
                                                    FilterChip(
                                                        selected = chosen,
                                                        onClick = {
                                                            editableQuestions = editableQuestions.toMutableList().also { list ->
                                                                list[qIdx] = q.copy(selectedOption = opt)
                                                            }
                                                        },
                                                        label = {
                                                            Text(opt, fontFamily = TajawalFontFamily, fontSize = 12.sp)
                                                        },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            containerColor = SpaceColors.SurfaceDeep,
                                                            labelColor = SpaceColors.MutedText,
                                                            selectedContainerColor = aiCyan.copy(alpha = 0.24f),
                                                            selectedLabelColor = SpaceColors.White
                                                        )
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = q.questionAr,
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = SpaceColors.White,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(10.dp))
                                                Switch(
                                                    checked = q.boolAnswer,
                                                    onCheckedChange = { checked ->
                                                        editableQuestions = editableQuestions.toMutableList().also { list ->
                                                            list[qIdx] = q.copy(boolAnswer = checked)
                                                        }
                                                    },
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = SpaceColors.DeepSpace,
                                                        checkedTrackColor = aiCyan,
                                                        uncheckedTrackColor = SpaceColors.SurfaceCard
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            PremiumSectionHeader(
                                title = "خارطة العمل المولّدة (${plan.roadmap.size} مرحلة)",
                                icon = Icons.Default.Timeline,
                                accentColor = planAccent
                            )
                        }

                        items(plan.roadmap, key = { "${it.stageNumber}_${it.titleAr}" }) { stage ->
                            NeonGlassCard(
                                accentColor = planAccent,
                                secondaryAccent = aiCyan,
                                elevation = 8.dp,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = planAccent.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, planAccent.copy(alpha = 0.55f))
                                    ) {
                                        Text(
                                            text = "${stage.stageNumber}",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = SpaceColors.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stage.titleAr,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = SpaceColors.White
                                        )
                                        if (stage.descriptionAr.isNotBlank()) {
                                            Text(
                                                text = stage.descriptionAr,
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = SpaceColors.MutedText
                                            )
                                        }
                                    }
                                    Surface(
                                        color = aiCyan.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${stage.featureCountHint} ميزة",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 10.sp,
                                            color = aiCyan,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            NeonButton(
                                text = "إنشاء المراحل والميزات وفتح الاستوديو ⚡",
                                icon = Icons.Filled.RocketLaunch,
                                style = NeonButtonStyle.PRIMARY_GLOW,
                                height = 54.dp,
                                testTag = "ai_materialize_studio_btn",
                                onClick = { onConfirmPlanAndMaterialize(plan, editableQuestions) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
