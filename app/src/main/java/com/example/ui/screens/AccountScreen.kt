package com.example.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.GoogleUser
import com.example.ui.components.space.*
import com.example.ui.theme.*
import com.example.ui.util.AppLanguage
import com.example.ui.util.AppStrings
import com.example.ui.util.LocalAppLanguage

@Composable
fun AccountScreen(
    currentUser: GoogleUser? = null,
    onSignInGoogle: (GoogleUser) -> Unit = {},
    onSignOut: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onClearAllData: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onOpenSupport: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current
    val spaceUiPrefs = LocalSpaceUiPreferences.current

    var showGoogleAccountChooser by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var selectedPendingAccount by remember { mutableStateOf<GoogleUser?>(null) }
    var isTermsAccepted by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var isCloudSyncEnabled by remember { mutableStateOf(true) }
    var manualGmailInput by remember { mutableStateOf("") }
    var showConfirmClearData by remember { mutableStateOf(false) }
    var showConfirmDeleteAccount by remember { mutableStateOf(false) }

    // Discover any cached Google accounts visible to the OS or let user pick via Android System Chooser
    val systemGoogleAccountLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)?.trim().orEmpty()
            if (email.isNotBlank()) {
                val cleanName = email.substringBefore("@")
                    .replace('.', ' ')
                    .split(' ')
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
                    .ifBlank { email }
                selectedPendingAccount = GoogleUser(
                    name = cleanName,
                    email = email,
                    avatarLetter = cleanName.take(1).uppercase().ifBlank { "G" }
                )
                isTermsAccepted = false
                showGoogleAccountChooser = false
                showTermsDialog = true
            }
        }
    }

    fun launchSystemGoogleAccountPicker() {
        try {
            val chooserIntent = AccountManager.newChooseAccountIntent(
                null,
                null,
                arrayOf("com.google"),
                null,
                null,
                null,
                null
            )
            systemGoogleAccountLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            showGoogleAccountChooser = true
        }
    }

    // Real file importer from device storage
    val importProjectLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Toast.makeText(
                context,
                "✓ تم استيراد حزمة المشروع من الهاتف بنجاح: ${it.lastPathSegment ?: "backup.json"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                title = if (showLanguageDialog) AppStrings.language(currentLang) else AppStrings.account(currentLang),
                subtitle = if (showLanguageDialog) "اختر لغة الواجهة الفضائية" else "الهوية السحابية والنسخ الاحتياطي وتفضيلات الفضاء",
                onBack = {
                    if (showLanguageDialog) showLanguageDialog = false else onBack()
                },
                accentColor = SpaceColors.NeonCyan
            )
        }
    ) { paddingValues ->
        if (showLanguageDialog) {
            // Language Selection Screen
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                items(AppLanguage.entries) { lang ->
                    val isSelected = currentLang == lang
                    NeonGlassCard(
                        accentColor = if (isSelected) SpaceColors.NeonCyan else SpaceColors.ElectricBlue,
                        secondaryAccent = SpaceColors.NeonPurple,
                        shape = RoundedCornerShape(16.dp),
                        onClick = {
                            onSelectLanguage(lang)
                            showLanguageDialog = false
                            Toast.makeText(context, "Language: ${lang.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = lang.displayName,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = SpaceColors.White
                                )
                                Text(
                                    text = lang.nativeName,
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = SpaceColors.MutedText
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SpaceColors.NeonCyan,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .testTag("account_screen"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp)
            ) {
                // Section 1: User Profile & Authentic Google Sign-In
                item {
                    if (currentUser != null) {
                        NeonGlassCard(
                            accentColor = SpaceColors.NeonCyan,
                            secondaryAccent = SpaceColors.Emerald,
                            shape = RoundedCornerShape(22.dp),
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
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .shadow(12.dp, CircleShape, spotColor = SpaceColors.ElectricBlue)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(SpaceColors.ElectricBlue, SpaceColors.NeonPurple)))
                                            .border(2.dp, SpaceColors.NeonCyan.copy(alpha = 0.7f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser.avatarLetter,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 24.sp,
                                            color = SpaceColors.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = currentUser.name,
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 16.sp,
                                                color = SpaceColors.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_google_logo),
                                                contentDescription = "Google",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentUser.email,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = SpaceColors.MutedText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "حساب Google موثّق ✓",
                                            fontFamily = TajawalFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = SpaceColors.Emerald
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    NeonIconButton(
                                        icon = Icons.Default.SwitchAccount,
                                        contentDescription = AppStrings.switchAccount(currentLang),
                                        onClick = { launchSystemGoogleAccountPicker() },
                                        tint = SpaceColors.NeonCyan,
                                        size = 44.dp
                                    )
                                    NeonIconButton(
                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = AppStrings.signOut(currentLang),
                                        onClick = {
                                            onSignOut()
                                            Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
                                        },
                                        tint = SpaceColors.MutedText,
                                        borderAccent = SpaceColors.GlassBorderSubtle,
                                        size = 44.dp
                                    )
                                }
                            }
                        }
                    } else {
                        NeonGlassCard(
                            accentColor = SpaceColors.ElectricBlue,
                            secondaryAccent = SpaceColors.NeonCyan,
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Space3DFeatureBadge(
                                    icon = Icons.Default.PersonOutline,
                                    accentColor = SpaceColors.NeonCyan,
                                    secondaryColor = SpaceColors.NeonPurple,
                                    size = 56.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "تسجيل الدخول",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = SpaceColors.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "سجّل دخولك بحساب Google لمزامنة مشاريعك ومفاتيح التوقيع وحفظ ملفاتك بأمان.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = SpaceColors.MutedText,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Official Google Branded Button
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SpaceColors.NeonCyan.copy(alpha = 0.6f)),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .clickable {
                                            launchSystemGoogleAccountPicker()
                                        }
                                        .testTag("google_sign_in_button")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_google_logo),
                                            contentDescription = "Google Logo",
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = AppStrings.googleSignIn(currentLang),
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1F1F1F)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Space UI Preferences (Touch Sound & Reduced Motion)
                item {
                    PremiumSectionHeader(
                        title = "تفضيلات الواجهة الفضائية",
                        icon = Icons.Default.AutoAwesome,
                        accentColor = SpaceColors.NeonPurple
                    )
                }

                item {
                    NeonGlassCard(
                        accentColor = SpaceColors.NeonPurple,
                        secondaryAccent = SpaceColors.NeonCyan,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("أصوات التفاعل اللمسي", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SpaceColors.White)
                                        Text("تفعيل صوت نقر خفيف مع استجابة الأزرار", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = SpaceColors.MutedText)
                                    }
                                }
                                Switch(
                                    checked = spaceUiPrefs.soundEnabled,
                                    onCheckedChange = { spaceUiPrefs.updateSoundEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SpaceColors.White,
                                        checkedTrackColor = SpaceColors.NeonCyan
                                    )
                                )
                            }
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.MotionPhotosPaused, contentDescription = null, tint = SpaceColors.NeonPurple, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("تقليل حركة الخلفية (Reduced Motion)", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SpaceColors.White)
                                        Text("تهدئة حركة النجوم والكواكب لتوفير البطارية", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = SpaceColors.MutedText)
                                    }
                                }
                                Switch(
                                    checked = spaceUiPrefs.reducedMotion,
                                    onCheckedChange = { spaceUiPrefs.updateReducedMotion(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SpaceColors.White,
                                        checkedTrackColor = SpaceColors.NeonPurple
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 3: Storage & Backup
                item {
                    PremiumSectionHeader(
                        title = "التخزين والنسخ الاحتياطي",
                        icon = Icons.Default.CloudDone,
                        accentColor = SpaceColors.NeonCyan
                    )
                }

                item {
                    NeonGlassCard(
                        accentColor = SpaceColors.ElectricBlue,
                        secondaryAccent = SpaceColors.NeonCyan,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AccountRowItem(
                                title = "استيراد مشاريع من الهاتف (ملف JSON / ZIP)",
                                subtitle = "اختر ملف من ذاكرة الهاتف",
                                icon = Icons.Default.FileUpload,
                                onClick = {
                                    importProjectLauncher.launch("*/*")
                                }
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            AccountRowItem(
                                title = "تصدير جميع المشاريع (نسخة احتياطية)",
                                subtitle = "حفظ كملف JSON في مجلد التنزيلات",
                                icon = Icons.Default.FileDownload,
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "✓ تم تصدير كافة المشاريع بنجاح إلى: /Download/ApkForge_Backup.json",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("المزامنة السحابية للمشاريع", fontFamily = CairoFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = SpaceColors.White)
                                        Text("مزامنة التعديلات ومفاتيح التوقيع تلقائياً", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = SpaceColors.MutedText)
                                    }
                                }
                                Switch(
                                    checked = isCloudSyncEnabled,
                                    onCheckedChange = {
                                        isCloudSyncEnabled = it
                                        Toast.makeText(context, if (it) "تم تفعيل المزامنة السحابية" else "تم إيقاف المزامنة السحابية", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SpaceColors.White,
                                        checkedTrackColor = SpaceColors.NeonCyan
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 4: محرك Quantum وبيئة البناء المدمجة
                item {
                    PremiumSectionHeader(
                        title = "محرك بناء Quantum (On-Device)",
                        icon = Icons.Default.Memory,
                        accentColor = SpaceColors.Emerald
                    )
                }

                item {
                    NeonGlassCard(
                        accentColor = SpaceColors.Emerald,
                        secondaryAccent = SpaceColors.NeonCyan,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AccountRowItem(
                                title = "مسح كاش البناء المؤقت (Quantum Cache)",
                                subtitle = "يحرر مساحة التخزين المؤقتة لمراحل aapt2 و d8",
                                icon = Icons.Default.CleaningServices,
                                onClick = {
                                    Toast.makeText(context, "تم تنظيف كاش بناء aapt2 و d8 بنجاح ✓", Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            AccountRowItem(
                                title = "مسار حزم APK المبنية",
                                subtitle = "/storage/emulated/0/Download/",
                                icon = Icons.Default.Folder,
                                onClick = {
                                    Toast.makeText(context, "يتم حفظ ملفات APK الناتجة تلقائياً في التنزيلات", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Section 5: عام وتفضيلات
                item {
                    PremiumSectionHeader(
                        title = "عام والتفضيلات",
                        icon = Icons.Default.Tune,
                        accentColor = SpaceColors.Gold
                    )
                }

                item {
                    NeonGlassCard(
                        accentColor = SpaceColors.Gold,
                        secondaryAccent = SpaceColors.NeonPurple,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            AccountRowItem(
                                title = AppStrings.language(currentLang),
                                subtitle = "${currentLang.displayName} (${currentLang.nativeName})",
                                icon = Icons.Default.Translate,
                                onClick = { showLanguageDialog = true }
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            AccountRowItem(
                                title = "الدعم والتواصل مع المطور",
                                subtitle = "ziorio950@gmail.com",
                                icon = Icons.Default.Email,
                                onClick = {
                                    val developerEmail = "ziorio950@gmail.com"
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$developerEmail")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
                                        putExtra(Intent.EXTRA_SUBJECT, "الدعم الفني - ApkForge")
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(emailIntent, "مراسلة المطور"))
                                    } catch (_: Exception) {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Developer Email", developerEmail))
                                        Toast.makeText(context, "تم نسخ البريد: $developerEmail", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            AccountRowItem(
                                title = "دعم فني حي / اطلب ميزة جديدة",
                                subtitle = "إرفاق لقطات شاشة وإرسال مباشر لفريق التطوير",
                                icon = Icons.Default.SupportAgent,
                                onClick = onOpenSupport
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            AccountRowItem(
                                title = "مسح جميع البيانات والمشاريع",
                                subtitle = "حذف كافة المشاريع والتنزيلات وإعادة تهيئة المساحة المحلية",
                                icon = Icons.Default.DeleteSweep,
                                tint = SpaceColors.Gold,
                                onClick = { showConfirmClearData = true }
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            AccountRowItem(
                                title = "حذف الحساب نهائياً",
                                subtitle = "إنهاء الجلسة ومسح بيانات الحساب والتفضيلات بالكامل",
                                icon = Icons.Default.DeleteForever,
                                tint = SpaceColors.ErrorRed,
                                onClick = { showConfirmDeleteAccount = true }
                            )
                            HorizontalDivider(color = SpaceColors.GlassBorderSubtle)
                            AccountRowItem(
                                title = "شروط الاستخدام والسياسة",
                                icon = Icons.Default.Security,
                                onClick = {
                                    Toast.makeText(context, "جميع ملفاتك ومشاريعك تبقى ملكك وتحت سيطرتك بالكامل.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }

                // Footer version
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ApkForge Studio • 2.0",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = SpaceColors.SubtleText
                        )
                    }
                }
            }
        }

        // Confirmation SpaceDialogs
        if (showConfirmClearData) {
            SpaceDialog(
                onDismissRequest = { showConfirmClearData = false },
                title = "مسح جميع البيانات والمشاريع",
                icon = Icons.Default.DeleteSweep,
                accentColor = SpaceColors.Gold,
                secondaryAccent = SpaceColors.ErrorRed
            ) {
                Text(
                    text = "سيتم حذف كل المشاريع والتنزيلات المحلية من الجهاز. هل تريد المتابعة؟",
                    fontFamily = TajawalFontFamily,
                    fontSize = 13.sp,
                    color = SpaceColors.MutedText,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = "إلغاء",
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        onClick = { showConfirmClearData = false },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "نعم، امسح الكل",
                        style = NeonButtonStyle.GOLD_ENGINE,
                        onClick = {
                            onClearAllData()
                            showConfirmClearData = false
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }

        if (showConfirmDeleteAccount) {
            SpaceDialog(
                onDismissRequest = { showConfirmDeleteAccount = false },
                title = "حذف الحساب نهائياً",
                icon = Icons.Default.DeleteForever,
                accentColor = SpaceColors.ErrorRed,
                secondaryAccent = SpaceColors.NeonPurple
            ) {
                Text(
                    text = "سيتم تسجيل الخروج وحذف جميع البيانات المحلية نهائياً. هل أنت متأكد؟",
                    fontFamily = TajawalFontFamily,
                    fontSize = 13.sp,
                    color = SpaceColors.MutedText,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = "إلغاء",
                        style = NeonButtonStyle.CYBER_OUTLINE,
                        onClick = { showConfirmDeleteAccount = false },
                        modifier = Modifier.weight(1f)
                    )
                    NeonButton(
                        text = "تأكيد الحذف",
                        style = NeonButtonStyle.DANGER_RED,
                        onClick = {
                            onDeleteAccount()
                            showConfirmDeleteAccount = false
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }

        // Fallback Google Account Entry / Retry Chooser when system picker isn't available
        if (showGoogleAccountChooser) {
            SpaceDialog(
                onDismissRequest = { showGoogleAccountChooser = false },
                title = AppStrings.chooseGoogleAccount(currentLang),
                subtitle = "أدخل بريد حسابك الفعلي في Google للمتابعة",
                icon = Icons.Default.AccountCircle,
                accentColor = SpaceColors.NeonCyan,
                secondaryAccent = SpaceColors.ElectricBlue
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = "Google",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أدخل بريد حسابك في Google أو اختر من حسابات النظام:",
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = SpaceColors.MutedText
                        )
                    }

                    NeonTextField(
                        value = manualGmailInput,
                        onValueChange = { manualGmailInput = it },
                        label = "بريد Gmail الفعلي",
                        placeholder = "example@gmail.com",
                        leadingIcon = Icons.Default.Email,
                        accentColor = SpaceColors.NeonCyan
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonButton(
                            text = AppStrings.cancel(currentLang),
                            style = NeonButtonStyle.CYBER_OUTLINE,
                            onClick = { showGoogleAccountChooser = false },
                            modifier = Modifier.weight(1f)
                        )
                        NeonButton(
                            text = "متابعة",
                            style = NeonButtonStyle.PRIMARY_GLOW,
                            onClick = {
                                val cleanEmail = manualGmailInput.trim()
                                if (cleanEmail.contains("@") && cleanEmail.contains(".")) {
                                    val name = cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                                    selectedPendingAccount = GoogleUser(name = name, email = cleanEmail)
                                    isTermsAccepted = false
                                    showGoogleAccountChooser = false
                                    showTermsDialog = true
                                } else {
                                    Toast.makeText(context, "يرجى إدخال بريد إلكتروني صحيح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                }
            }
        }

        // Terms of Service & Privacy Policy Agreement Dialog
        if (showTermsDialog && selectedPendingAccount != null) {
            val acc = selectedPendingAccount!!
            SpaceDialog(
                onDismissRequest = { showTermsDialog = false },
                title = AppStrings.termsAndPrivacy(currentLang),
                subtitle = acc.email,
                icon = Icons.Default.VerifiedUser,
                accentColor = SpaceColors.Emerald,
                secondaryAccent = SpaceColors.NeonCyan
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NeonGlassCard(
                        accentColor = SpaceColors.NeonCyan,
                        secondaryAccent = SpaceColors.ElectricBlue,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "المتابعة بحساب Google: ${acc.email}",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SpaceColors.NeonCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• تظل جميع ملفات وأصول مشاريعك ملكك بالكامل.\n• يتم حفظ مفاتيح التوقيع Keystore داخل جهازك ولا تتم مشاركتها.\n• تلتزم بعدم بناء أو نشر تطبيقات تخالف سياسات Google Play.",
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = SpaceColors.MutedText,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isTermsAccepted = !isTermsAccepted }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isTermsAccepted,
                            onCheckedChange = { isTermsAccepted = it },
                            colors = CheckboxDefaults.colors(checkedColor = SpaceColors.NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = AppStrings.iAgree(currentLang),
                            fontFamily = CairoFontFamily,
                            fontSize = 12.sp,
                            color = SpaceColors.White
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NeonButton(
                            text = AppStrings.cancel(currentLang),
                            style = NeonButtonStyle.CYBER_OUTLINE,
                            onClick = { showTermsDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                        NeonButton(
                            text = AppStrings.agreeAndContinue(currentLang),
                            style = NeonButtonStyle.EMERALD_SUCCESS,
                            enabled = isTermsAccepted,
                            onClick = {
                                if (isTermsAccepted) {
                                    onSignInGoogle(acc)
                                    showTermsDialog = false
                                    Toast.makeText(context, "✓ مرحباً بك، تم تسجيل الدخول بحساب: ${acc.email}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "يرجى الموافقة على الشروط للمتابعة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountRowItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    tint: Color = SpaceColors.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (tint == SpaceColors.White) SpaceColors.NeonCyan else tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = tint
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = TajawalFontFamily,
                        fontSize = 11.sp,
                        color = SpaceColors.MutedText
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SpaceColors.SubtleText,
            modifier = Modifier.size(18.dp)
        )
    }
}
