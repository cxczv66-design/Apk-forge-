package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.AppProject
import com.example.engine.ScrapedSiteIdentity
import com.example.engine.UrlScraperEngine
import com.example.ui.components.space.*
import com.example.ui.theme.*
import com.example.ui.util.strings

private const val SUPPORT_EMAIL = "ziorio950@gmail.com"

/**
 * مساعد الدعم العائم المطابق للصورة المرجعية:
 * كرة روبوت AI متوهجة + فقاعة كلام زجاجية "أحتاج إلى مساعدة؟"
 */
@Composable
fun OmniSupport3dButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val prefs = LocalSpaceUiPreferences.current
    val infinite = rememberInfiniteTransition(label = "support_3d")
    val floatOffset by if (!prefs.reducedMotion) {
        infinite.animateFloat(
            initialValue = -3.5f,
            targetValue = 3.5f,
            animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "float_y"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    Row(
        modifier = modifier
            .graphicsLayer { translationY = floatOffset }
            .clip(RoundedCornerShape(30.dp))
            .clickable {
                prefs.triggerTouchFeedback(view, isLongPress = false)
                onClick()
            }
            .testTag("support_3d_button"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpaceFloatingRobotAvatar(
            size = 56.dp,
            accentColor = SpaceColors.NeonCyan,
            showOnlineDot = true
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            color = SpaceColors.MidnightBlue.copy(alpha = 0.94f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                1.2.dp,
                Brush.horizontalGradient(listOf(SpaceColors.NeonCyan.copy(alpha = 0.8f), SpaceColors.NeonPurple.copy(alpha = 0.55f)))
            ),
            modifier = Modifier.shadow(14.dp, RoundedCornerShape(18.dp), spotColor = SpaceColors.NeonCyan)
        ) {
            Text(
                text = "أحتاج إلى مساعدة؟",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = SpaceColors.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun OmniSupportDialog(
    activeProjectName: String = "ApkForge Omni IDE",
    onDismiss: () -> Unit
) {
    val s = strings()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("مشكلة بناء أو توقيع") }
    var priority by remember { mutableStateOf("متوسطة") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) screenshotUri = uri
    }

    SpaceDialog(
        onDismissRequest = onDismiss,
        title = s.supportAndFeatureReq,
        subtitle = "فريق التطوير المباشر • $SUPPORT_EMAIL",
        icon = Icons.Filled.SupportAgent,
        accentColor = SpaceColors.NeonCyan,
        secondaryAccent = SpaceColors.NeonPurple
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SpaceColors.SurfaceDeep,
                contentColor = SpaceColors.NeonCyan,
                modifier = Modifier.clip(RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(s.technicalSupport, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(s.requestFeature, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (selectedTab == 0) {
                Text("نوع المشكلة", color = SpaceColors.MutedText, fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("مشكلة بناء أو توقيع", "معاينة الويب", "واجهة وتخصيص").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text("أولوية الميزة المقترحة", color = SpaceColors.MutedText, fontFamily = CairoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("عالية", "متوسطة", "تحسين").forEach { pr ->
                        FilterChip(
                            selected = priority == pr,
                            onClick = { priority = pr },
                            label = { Text(pr, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            NeonTextField(
                value = subject,
                onValueChange = { subject = it },
                label = if (selectedTab == 0) "عنوان المشكلة" else "اسم الميزة المقترحة",
                accentColor = SpaceColors.NeonCyan
            )

            NeonTextField(
                value = message,
                onValueChange = { message = it },
                label = if (selectedTab == 0) "اشرح الخطوات أو الخطأ الظاهر بالتفصيل..." else "اشرح كيف ستفيدك هذه الميزة في بناء التطبيقات...",
                singleLine = false,
                minLines = 3,
                maxLines = 5,
                accentColor = SpaceColors.NeonPurple
            )

            if (selectedTab == 0) {
                NeonButton(
                    text = if (screenshotUri == null) "إرفاق لقطة شاشة (اختياري)" else "تغيير لقطة الشاشة المرفقة ✓",
                    icon = Icons.Filled.AddPhotoAlternate,
                    style = NeonButtonStyle.CYBER_OUTLINE,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                screenshotUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, SpaceColors.GlassBorderCyan, RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "لقطة شاشة المشكلة",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { screenshotUri = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = "إزالة", tint = SpaceColors.ErrorRed)
                        }
                    }
                }
            }

            NeonButton(
                text = if (selectedTab == 0) "إرسال طلب الدعم الفني" else "إرسال طلب الميزة",
                icon = Icons.Filled.Send,
                style = NeonButtonStyle.PRIMARY_GLOW,
                onClick = {
                    val finalSubject = if (selectedTab == 0) {
                        "[ApkForge Support - $category] ${subject.ifBlank { activeProjectName }}"
                    } else {
                        "[ApkForge Feature Request - $priority] ${subject.ifBlank { "ميزة جديدة" }}"
                    }
                    val fullBody = buildString {
                        appendLine("المشروع: $activeProjectName")
                        appendLine("النوع: ${if (selectedTab == 0) category else "Feature Request ($priority)"}")
                        appendLine("الجهاز: ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})")
                        appendLine("---")
                        appendLine(message.ifBlank { "(لم يتم كتابة تفاصيل إضافية)" })
                    }
                    dispatchSupportEmail(context, finalSubject, fullBody, screenshotUri)
                    onDismiss()
                },
                testTag = "send_support_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun dispatchSupportEmail(
    context: Context,
    subject: String,
    body: String,
    attachmentUri: Uri?
) {
    try {
        val intent = if (attachmentUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, attachmentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
        }
        val chooser = Intent.createChooser(intent, "إرسال إلى فريق ApkForge").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (_: Exception) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Support Email", "$SUPPORT_EMAIL\n$subject\n$body"))
        Toast.makeText(context, "تم نسخ تفاصيل الدعم وبريد المطور ($SUPPORT_EMAIL)", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun PwaSiteCustomizerDialog(
    initialProject: AppProject,
    scrapedIdentity: ScrapedSiteIdentity? = null,
    onDismiss: () -> Unit,
    onApplyAndPreview: (AppProject) -> Unit,
    onApplyAndSave: (AppProject) -> Unit
) {
    val context = LocalContext.current
    val scraper = remember { UrlScraperEngine() }

    var heroTitle by remember { mutableStateOf(initialProject.omniConfig.customHeroTitle.ifBlank { initialProject.name }) }
    var primaryBtnText by remember { mutableStateOf(initialProject.omniConfig.customPrimaryButtonText.ifBlank { "ابدأ الآن" }) }
    var hideSelectors by remember { mutableStateOf(initialProject.omniConfig.customHideSelectorsCsv) }
    var gradientEnabled by remember { mutableStateOf(initialProject.omniConfig.gradientEnabled) }
    var gradientStart by remember { mutableStateOf(initialProject.omniConfig.gradientStartHex) }
    var gradientEnd by remember { mutableStateOf(initialProject.omniConfig.gradientEndHex) }
    var bannerUri by remember { mutableStateOf(initialProject.omniConfig.customBannerImageUri) }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) bannerUri = uri.toString()
    }

    fun buildMaterializedProject(): AppProject {
        val configured = initialProject.copy(
            name = heroTitle.ifBlank { initialProject.name },
            omniConfig = initialProject.omniConfig.copy(
                gradientEnabled = gradientEnabled,
                gradientStartHex = gradientStart,
                gradientEndHex = gradientEnd,
                customHeroTitle = heroTitle,
                customPrimaryButtonText = primaryBtnText,
                customHideSelectorsCsv = hideSelectors,
                customBannerImageUri = bannerUri
            )
        )
        return scraper.materializeWebsiteAsLocalProject(
            context = context,
            project = configured,
            identity = scrapedIdentity
        )
    }

    SpaceDialog(
        onDismissRequest = onDismiss,
        title = "محرر PWA المحلي الفوري",
        subtitle = "يحوّل الموقع لملفات محلية قابلة للتعديل والبناء الفوري",
        icon = Icons.Filled.WebAsset,
        accentColor = SpaceColors.Emerald,
        secondaryAccent = SpaceColors.NeonCyan
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeonTextField(
                value = heroTitle,
                onValueChange = { heroTitle = it },
                label = "عنوان الواجهة الرئيسية (Hero Title)",
                accentColor = SpaceColors.Emerald
            )

            NeonTextField(
                value = primaryBtnText,
                onValueChange = { primaryBtnText = it },
                label = "نص الزر الرئيسي (CTA Button)",
                accentColor = SpaceColors.NeonCyan
            )

            NeonTextField(
                value = hideSelectors,
                onValueChange = { hideSelectors = it },
                label = "عناصر CSS لإخفائها (.ads, footer)",
                accentColor = SpaceColors.NeonPurple
            )

            NeonGlassCard(
                accentColor = SpaceColors.NeonCyan,
                shape = RoundedCornerShape(14.dp),
                elevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("خلفية التدرج اللوني الذكي (Gradient)", color = SpaceColors.White, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("$gradientStart → $gradientEnd", color = SpaceColors.MutedText, fontSize = 11.sp)
                    }
                    Switch(checked = gradientEnabled, onCheckedChange = { gradientEnabled = it })
                }
            }

            if (gradientEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    NeonTextField(
                        value = gradientStart,
                        onValueChange = { gradientStart = it },
                        label = "لون البداية",
                        accentColor = SpaceColors.NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    NeonTextField(
                        value = gradientEnd,
                        onValueChange = { gradientEnd = it },
                        label = "لون النهاية",
                        accentColor = SpaceColors.NeonPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            NeonButton(
                text = if (bannerUri.isNullOrBlank()) "اختيار صورة بانر مخصصة" else "تم اختيار صورة بانر ✓",
                icon = Icons.Filled.Image,
                style = NeonButtonStyle.CYBER_OUTLINE,
                onClick = {
                    bannerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                NeonButton(
                    text = "حفظ التعديلات",
                    style = NeonButtonStyle.CYBER_OUTLINE,
                    onClick = { onApplyAndSave(buildMaterializedProject()) },
                    modifier = Modifier.weight(1f)
                )
                NeonButton(
                    text = "معاينة فورية",
                    icon = Icons.Filled.PlayArrow,
                    style = NeonButtonStyle.EMERALD_SUCCESS,
                    onClick = { onApplyAndPreview(buildMaterializedProject()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
