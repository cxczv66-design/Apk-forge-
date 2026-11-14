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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.model.AppProject
import com.example.engine.ScrapedSiteIdentity
import com.example.engine.UrlScraperEngine
import com.example.ui.theme.*
import com.example.ui.util.strings

private const val SUPPORT_EMAIL = "ziorio950@gmail.com"

/**
 * زر الدعم المباشر ثلاثي الأبعاد مع توهج وحركة طفو ناعمة:
 * يفتح واجهة "دعم فني" (مع إرفاق لقطة شاشة عبر Photo Picker) و"اطلب ميزة".
 */
@Composable
fun OmniSupport3dButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = strings()
    val infinite = rememberInfiniteTransition(label = "support_3d")
    val floatOffset by infinite.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float_y"
    )
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .graphicsLayer { translationY = floatOffset }
            .shadow(elevation = 14.dp, shape = RoundedCornerShape(24.dp), ambientColor = CyanAccent, spotColor = OrangePrimary)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0EA5E9), Color(0xFF2563EB), Color(0xFF7C3AED))
                )
            )
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = glowAlpha),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("support_3d_button")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.SupportAgent, contentDescription = s.supportAndFeatureReq, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(s.technicalSupport, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                Text(s.requestFeature, color = Color(0xFFE0F2FE), fontSize = 10.sp)
            }
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = دعم فني, 1 = اطلب ميزة
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF7C3AED)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(s.supportAndFeatureReq, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            Text("فريق التطوير المباشر • $SUPPORT_EMAIL", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceElevated,
                    contentColor = Color(0xFF38BDF8),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(s.technicalSupport, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(s.requestFeature, fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(Modifier.height(14.dp))

                if (selectedTab == 0) {
                    Text("نوع المشكلة", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("مشكلة بناء أو توقيع", "معاينة الويب", "واجهة وتخصيص").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                } else {
                    Text("أولوية الميزة المقترحة", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
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
                    Spacer(Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(if (selectedTab == 0) "عنوان المشكلة" else "اسم الميزة المقترحة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text(if (selectedTab == 0) "اشرح الخطوات أو الخطأ الظاهر بالتفصيل..." else "اشرح كيف ستفيدك هذه الميزة في بناء التطبيقات...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (selectedTab == 0) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (screenshotUri == null) "إرفاق لقطة شاشة (اختياري)" else "تغيير لقطة الشاشة المرفقة ✓")
                    }

                    screenshotUri?.let { uri ->
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
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
                                Icon(Icons.Filled.Cancel, contentDescription = "إزالة", tint = ErrorRed)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("send_support_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9))
                ) {
                    Icon(Icons.Filled.Send, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedTab == 0) "إرسال طلب الدعم الفني" else "إرسال طلب الميزة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
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

/**
 * محرر وتخصيص مواقع الويب و PWA محلياً:
 * يسمح للمستخدم بتعديل النصوص، زر الإجراء، التدرج اللوني، إخفاء عناصر CSS، وإدراج صورة بانر
 * ثم تحويل الموقع فوراً لمشروع محلي قابل للمعاينة الحية والبناء.
 */
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 540.dp),
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, StatusGreen.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WebAsset, contentDescription = null, tint = StatusGreen)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("تحويل وتخصيص الموقع (PWA Local Editor)", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            Text("يحوّل الموقع لملفات محلية قابلة للتعديل والبناء الفوري", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                OutlinedTextField(
                    value = heroTitle,
                    onValueChange = { heroTitle = it },
                    label = { Text("عنوان الواجهة الرئيسية (Hero Title)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = primaryBtnText,
                    onValueChange = { primaryBtnText = it },
                    label = { Text("نص الزر الرئيسي (CTA Button)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = hideSelectors,
                    onValueChange = { hideSelectors = it },
                    label = { Text("عناصر CSS لإخفائها (مثل: .ads, footer, #cookie)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("خلفية التدرج اللوني الذكي (Gradient)", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("$gradientStart → $gradientEnd", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(checked = gradientEnabled, onCheckedChange = { gradientEnabled = it })
                }

                if (gradientEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = gradientStart,
                            onValueChange = { gradientStart = it },
                            label = { Text("لون البداية") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = gradientEnd,
                            onValueChange = { gradientEnd = it },
                            label = { Text("لون النهاية") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        bannerPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (bannerUri.isNullOrBlank()) "اختيار صورة بانر مخصصة" else "تم اختيار صورة بانر ✓")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val built = buildMaterializedProject()
                            onApplyAndSave(built)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ التعديلات", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val built = buildMaterializedProject()
                            onApplyAndPreview(built)
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusGreen)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(4.dp))
                        Text("معاينة فورية", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
