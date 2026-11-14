package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.engine.LocalWorkspaceEngine
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.model.*
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.GlowingButton
import com.example.ui.components.ModernColorPickerDialog
import com.example.ui.components.PhoneMockupFrame
import com.example.ui.theme.*

@Composable
fun WizardScreen(
    initialProject: AppProject? = null,
    onFinish: (AppProject) -> Unit,
    onCancel: () -> Unit,
    onOpenInteractivePhone: ((AppProject) -> Unit)? = null
) {
    // Current active step (1 to 9)
    var currentStep by remember { mutableIntStateOf(2) }
    var isPhoneExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Step 1: Source
    var sourceType by remember { mutableStateOf(initialProject?.sourceType ?: SourceType.ZIP_ARCHIVE) }
    var sourceValue by remember { mutableStateOf(initialProject?.sourceValue ?: "archive.zip") }
    var localWorkspacePath by remember { mutableStateOf(initialProject?.localWorkspacePath) }

    // Step 2: Identity (matching screenshots with real data and modern features)
    var appName by remember { mutableStateOf(initialProject?.name ?: "") }
    var accentColorHex by remember { mutableStateOf(initialProject?.accentColorHex ?: "#FFE236") }
    var selectedIconUri by remember { mutableStateOf<String?>(initialProject?.iconImageUri) }
    var isAdaptiveIconEnabled by remember { mutableStateOf(true) }
    var iconMaskShape by remember { mutableStateOf(initialProject?.proConfig?.iconShape ?: "squircle") } // "squircle", "circle", "rounded", "teardrop"
    var iconBgColorHex by remember { mutableStateOf(initialProject?.proConfig?.iconBgColorHex ?: "#FFFFFF") }
    var showLiveSystemPreview by remember { mutableStateOf(true) }

    // Real Gallery image picker (Zero-Permission Android Photo Picker + internal copy persistence)
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            scope.launch {
                val persisted = LocalWorkspaceEngine.persistImageUriToInternalFile(context, pickedUri)
                selectedIconUri = persisted ?: pickedUri.toString()
                Toast.makeText(context, "تم حفظ أيقونة التطبيق الحقيقية بنجاح ✓", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Real Source archive / HTML picker
    val sourceArchivePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            scope.launch {
                val displayName = LocalWorkspaceEngine.getDisplayName(context, pickedUri, "source")
                val isZip = displayName.endsWith(".zip", ignoreCase = true)
                val res = if (isZip) {
                    LocalWorkspaceEngine.importZipUri(context, pickedUri)
                } else {
                    LocalWorkspaceEngine.importHtmlUri(context, pickedUri)
                }
                res.onSuccess { ws ->
                    sourceType = if (isZip) SourceType.ZIP_ARCHIVE else SourceType.HTML_FOLDER
                    sourceValue = ws.displayFileName
                    localWorkspacePath = ws.workspaceDirPath
                    if (appName.isBlank() && ws.detectedTitle.isNotBlank()) {
                        appName = ws.detectedTitle.take(28)
                    }
                    Toast.makeText(context, "✓ تم استيراد ${ws.displayFileName} (${ws.assets.size} ملفات)", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    sourceValue = displayName
                    Toast.makeText(context, "تم تحديد الملف: ${err.message ?: displayName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Step 3: Splash Screen
    var splashBgColorHex by remember { mutableStateOf(initialProject?.splashConfig?.backgroundColorHex ?: "#0F1318") }
    var splashFadeBg by remember { mutableStateOf(initialProject?.splashConfig?.fadeBackground ?: false) }
    var splashIndicatorStyle by remember { mutableStateOf(initialProject?.splashConfig?.indicatorStyle ?: "حلقة - L") }
    var splashIconSize by remember { mutableIntStateOf(initialProject?.splashConfig?.iconSizeDp ?: 120) }
    var splashIconCorner by remember { mutableIntStateOf(initialProject?.splashConfig?.iconCornerRadiusDp ?: 16) }
    var splashIndicatorSize by remember { mutableIntStateOf(initialProject?.splashConfig?.indicatorSizeDp ?: 48) }
    var splashSpacing by remember { mutableIntStateOf(initialProject?.splashConfig?.spacingDp ?: 28) }

    // Step 4: Onboarding (Matching Screenshots 1, 2, 3)
    var onboardingEnabled by remember { mutableStateOf(initialProject?.onboardingConfig?.enabled ?: false) }
    var onboardingSlides by remember {
        mutableStateOf(
            initialProject?.onboardingConfig?.slides ?: listOf(
                OnboardingSlide("العنوان", "الوصف", "bolt")
            )
        )
    }
    var onboardingActiveSlideIndex by remember { mutableIntStateOf(0) }
    var onboardingLayoutType by remember { mutableStateOf(initialProject?.onboardingConfig?.layoutType ?: "بطاقة سفلية") }
    var onboardingImageShape by remember { mutableStateOf(initialProject?.onboardingConfig?.imageShape ?: "دائرة") }
    var onboardingGradientBg by remember { mutableStateOf(initialProject?.onboardingConfig?.gradientBackground ?: false) }
    var onboardingAccentColorHex by remember { mutableStateOf(initialProject?.onboardingConfig?.accentColorHex ?: "#FFE236") }
    var onboardingTextColorHex by remember { mutableStateOf(initialProject?.onboardingConfig?.textColorHex ?: "#111827") }
    var onboardingCardColorHex by remember { mutableStateOf(initialProject?.onboardingConfig?.cardColorHex ?: "#FFFFFF") }
    var onboardingShowSkipButton by remember { mutableStateOf(initialProject?.onboardingConfig?.showSkipButton ?: true) }

    // Dialog states for Onboarding
    var showOnboardingAccentColorPicker by remember { mutableStateOf(false) }
    var showOnboardingTextColorPicker by remember { mutableStateOf(false) }
    var showOnboardingCardColorPicker by remember { mutableStateOf(false) }
    var showLayoutTypeDialog by remember { mutableStateOf(false) }
    var showImageShapeDialog by remember { mutableStateOf(false) }
    var showIconPickerDialogForSlideIndex by remember { mutableStateOf<Int?>(null) }

    // Launcher for selecting a slide image using Photo Picker + persistent internal file copy
    val slideImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            val idx = showIconPickerDialogForSlideIndex ?: onboardingActiveSlideIndex
            scope.launch {
                val persisted = LocalWorkspaceEngine.persistImageUriToInternalFile(context, pickedUri)
                if (idx in onboardingSlides.indices) {
                    val updated = onboardingSlides.toMutableList()
                    updated[idx] = updated[idx].copy(imageUri = persisted ?: pickedUri.toString())
                    onboardingSlides = updated
                }
                showIconPickerDialogForSlideIndex = null
            }
        }
    }

    // Step 5: Features (Matching Screenshots 1, 2, 3)
    var fullScreen by remember { mutableStateOf(initialProject?.webViewConfig?.fullScreen ?: false) }
    var pullToRefresh by remember { mutableStateOf(initialProject?.webViewConfig?.pullToRefresh ?: false) }
    var pageLoaderEnabled by remember { mutableStateOf(initialProject?.webViewConfig?.pageLoader?.enabled ?: false) }
    var pageLoaderStyle by remember { mutableStateOf(initialProject?.webViewConfig?.pageLoader?.style ?: "حلقة") }
    var pageLoaderSize by remember { mutableIntStateOf(initialProject?.webViewConfig?.pageLoader?.sizeDp ?: 78) }
    var pageLoaderThickness by remember { mutableIntStateOf(initialProject?.webViewConfig?.pageLoader?.thicknessDp ?: 7) }
    var pageLoaderOnCard by remember { mutableStateOf(initialProject?.webViewConfig?.pageLoader?.showOnCard ?: true) }
    var pageLoaderCardColorHex by remember { mutableStateOf(initialProject?.webViewConfig?.pageLoader?.cardColorHex ?: "#FFFFFF") }
    var pageLoaderCardOpacity by remember { mutableIntStateOf(initialProject?.webViewConfig?.pageLoader?.cardOpacityPercent ?: 100) }
    var pageLoaderCardCornerRadius by remember { mutableIntStateOf(initialProject?.webViewConfig?.pageLoader?.cardCornerRadiusDp ?: 12) }

    // Advanced Features for Step 5:
    var supportZoom by remember { mutableStateOf(initialProject?.webViewConfig?.supportZoom ?: false) }
    var offlineFallback by remember { mutableStateOf(initialProject?.webViewConfig?.offlineFallback ?: true) }

    // Interactive Pull-to-refresh simulation state inside Step 5 phone preview
    var isSimulatedRefreshing by remember { mutableStateOf(false) }

    // Dialog state for Page Loader Card Color
    var showPageLoaderCardColorPicker by remember { mutableStateOf(false) }

    // Step 6: Bottom Navigation
    var bottomNavEnabled by remember { mutableStateOf(initialProject?.bottomNavConfig?.enabled ?: false) }
    var bottomDestinations by remember {
        mutableStateOf(
            initialProject?.bottomNavConfig?.destinations ?: listOf(
                BottomNavDestination("1", "Home", "/")
            )
        )
    }
    var bottomNavBarStyle by remember { mutableStateOf(initialProject?.bottomNavConfig?.barStyle ?: "مسطح") }
    var bottomNavShowLabels by remember { mutableStateOf(initialProject?.bottomNavConfig?.showLabels ?: true) }
    var bottomNavActiveColorHex by remember { mutableStateOf(initialProject?.bottomNavConfig?.activeColorHex ?: "#FF7A2F") }
    var bottomNavInactiveColorHex by remember { mutableStateOf(initialProject?.bottomNavConfig?.inactiveColorHex ?: "#9CA3AF") }
    var bottomNavBarColorHex by remember { mutableStateOf(initialProject?.bottomNavConfig?.barColorHex ?: "#FFFFFF") }

    // Interactive preview tab index
    var selectedBottomNavPreviewIndex by remember { mutableIntStateOf(0) }

    // Dialog states for Step 6
    var showBottomNavBarStyleDialog by remember { mutableStateOf(false) }
    var showBottomNavActiveColorPicker by remember { mutableStateOf(false) }
    var showBottomNavInactiveColorPicker by remember { mutableStateOf(false) }
    var showBottomNavBarColorPicker by remember { mutableStateOf(false) }
    var showBottomNavIconPickerDialogForIndex by remember { mutableStateOf<Int?>(null) }

    // Step 7: Packaging
    var packageName by remember {
        mutableStateOf(
            initialProject?.packageConfig?.packageName
                ?: if (appName.isNotBlank()) "com.${appName.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }.ifBlank { "company" }}.app" else "com.zk.app"
        )
    }
    var versionCode by remember { mutableIntStateOf(initialProject?.packageConfig?.versionCode ?: 1) }
    var versionName by remember { mutableStateOf(initialProject?.packageConfig?.versionName ?: "1.0") }
    var minSdk by remember { mutableIntStateOf(initialProject?.packageConfig?.minSdk ?: 24) }
    var targetSdk by remember { mutableIntStateOf(initialProject?.packageConfig?.targetSdk ?: 34) }
    var buildFormat by remember { mutableStateOf("APK & AAB") }
    var showMinSdkDialog by remember { mutableStateOf(false) }

    // Step 8: Permissions (Enhanced with diverse permissions + 2 new features)
    var permCamera by remember { mutableStateOf(initialProject?.permissionsConfig?.camera ?: false) }
    var permLocation by remember { mutableStateOf(initialProject?.permissionsConfig?.location ?: false) }
    var permMicrophone by remember { mutableStateOf(initialProject?.permissionsConfig?.microphone ?: false) }
    var permNotifications by remember { mutableStateOf(initialProject?.permissionsConfig?.notifications ?: false) }
    var permStorage by remember { mutableStateOf(initialProject?.permissionsConfig?.storage ?: false) }
    var permVibration by remember { mutableStateOf(initialProject?.permissionsConfig?.vibration ?: false) }
    var permBluetooth by remember { mutableStateOf(initialProject?.permissionsConfig?.bluetooth ?: false) }
    var permNetworkState by remember { mutableStateOf(initialProject?.permissionsConfig?.networkState ?: false) }

    // Feature 1: Smart Auto-Detection of permissions from project source
    var isAutoScanningPermissions by remember { mutableStateOf(false) }
    var autoScanCompleted by remember { mutableStateOf(false) }
    var autoDetectedCount by remember { mutableIntStateOf(0) }

    // Feature 2: Google Play Justification Generator
    var showJustificationDialog by remember { mutableStateOf(false) }

    // Step 9: Push (matching screenshot + new advanced features)
    var pushEnabled by remember { mutableStateOf(initialProject?.pushConfig?.enabled ?: false) }
    var pushAppId by remember { mutableStateOf(initialProject?.pushConfig?.appId ?: "") }
    var pushProvider by remember { mutableStateOf(initialProject?.pushConfig?.provider ?: "OneSignal") }
    var pushChannelName by remember { mutableStateOf(initialProject?.pushConfig?.channelName ?: "التنبيهات العامة") }
    var pushSoundEnabled by remember { mutableStateOf(initialProject?.pushConfig?.soundEnabled ?: true) }
    var pushVibrationEnabled by remember { mutableStateOf(initialProject?.pushConfig?.vibrationEnabled ?: true) }
    var pushBadgeEnabled by remember { mutableStateOf(initialProject?.pushConfig?.badgeEnabled ?: true) }
    var pushOpenUrlOnTap by remember { mutableStateOf(initialProject?.pushConfig?.openUrlOnTap ?: true) }
    var pushAutoPrompt by remember { mutableStateOf(initialProject?.pushConfig?.autoPromptOnFirstLaunch ?: true) }
    var pushTestTitle by remember { mutableStateOf(initialProject?.pushConfig?.testTitle ?: "مرحباً بك في التطبيق!") }
    var pushTestBody by remember { mutableStateOf(initialProject?.pushConfig?.testBody ?: "تم تفعيل الإشعارات الفورية بنجاح.") }
    var showLivePushBanner by remember { mutableStateOf(false) }

    // Dialog state
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var showSplashColorPickerDialog by remember { mutableStateOf(false) }
    var showIndicatorDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (currentStep > 2) {
            currentStep--
        } else {
            onCancel()
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header bar with back button & title "إعداد تطبيقك"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentStep > 2) currentStep-- else onCancel()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إعداد تطبيقك",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Circular Stepper 1 to 9 (matching video at 01:06 - 02:44)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (step in 1..9) {
                        val isCurrent = step == currentStep
                        val isDone = step < currentStep

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> OrangePrimary
                                        isDone -> StatusGreen.copy(alpha = 0.25f)
                                        else -> Color(0xFF232530)
                                    }
                                )
                                .border(
                                    1.dp,
                                    when {
                                        isCurrent -> OrangePrimary
                                        isDone -> StatusGreen
                                        else -> Color(0xFF373A4D)
                                    },
                                    CircleShape
                                )
                                .clickable {
                                    currentStep = step
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$step",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = when {
                                    isCurrent -> Color.White
                                    isDone -> StatusGreen
                                    else -> TextMuted
                                }
                            )
                        }

                        if (step < 9) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(2.dp)
                                    .background(if (step < currentStep) StatusGreen else Color(0xFF282B3A))
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Wizard Bottom Navigation Bar: رجوع > | < التالي / إنهاء وحفظ
            Surface(
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Button(
                        onClick = {
                            if (currentStep > 2) currentStep-- else onCancel()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "< رجوع",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

                    // Next / Finish button (bright orange in video)
                    Button(
                        onClick = {
                            if (currentStep == 6 && bottomNavEnabled && bottomDestinations.any { it.title.isBlank() }) {
                                Toast.makeText(context, "املأ كل وجهة، أو احذف الفارغة", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (currentStep == 7) {
                                val cleanPkg = packageName.trim()
                                val isPkgValid = cleanPkg.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))
                                if (!isPkgValid) {
                                    Toast.makeText(context, "يجب أن يكون معرّف الحزمة بصيغة صحيحة مثل com.company.app", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (versionName.isBlank()) {
                                    Toast.makeText(context, "يرجى إدخال اسم الإصدار (مثال: 1.0)", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                            }

                            if (currentStep < 9) {
                                currentStep++
                            } else {
                                // Final step: finish and save
                                Toast.makeText(context, "🎉 تم حفظ المشروع", Toast.LENGTH_SHORT).show()
                                val project = (initialProject ?: AppProject(
                                    id = "proj_${System.currentTimeMillis()}",
                                    name = appName
                                )).copy(
                                    name = appName.ifBlank { initialProject?.name ?: "تطبيق جديد" },
                                    sourceType = sourceType,
                                    sourceValue = sourceValue,
                                    localWorkspacePath = localWorkspacePath,
                                    iconImageUri = selectedIconUri,
                                    accentColorHex = accentColorHex,
                                    primaryColorHex = accentColorHex,
                                    splashConfig = SplashConfig(
                                        enabled = true,
                                        backgroundColorHex = splashBgColorHex,
                                        fadeBackground = splashFadeBg,
                                        indicatorStyle = splashIndicatorStyle,
                                        iconSizeDp = splashIconSize,
                                        iconCornerRadiusDp = splashIconCorner,
                                        indicatorSizeDp = splashIndicatorSize,
                                        spacingDp = splashSpacing
                                    ),
                                    onboardingConfig = OnboardingConfig(
                                        enabled = onboardingEnabled,
                                        slides = onboardingSlides,
                                        layoutType = onboardingLayoutType,
                                        imageShape = onboardingImageShape,
                                        gradientBackground = onboardingGradientBg,
                                        accentColorHex = onboardingAccentColorHex,
                                        textColorHex = onboardingTextColorHex,
                                        cardColorHex = onboardingCardColorHex,
                                        showSkipButton = onboardingShowSkipButton
                                    ),
                                    webViewConfig = WebViewConfig(
                                        fullScreen = fullScreen,
                                        pullToRefresh = pullToRefresh,
                                        pageLoader = PageLoaderConfig(
                                            enabled = pageLoaderEnabled,
                                            style = pageLoaderStyle,
                                            sizeDp = pageLoaderSize,
                                            thicknessDp = pageLoaderThickness,
                                            showOnCard = pageLoaderOnCard,
                                            cardColorHex = pageLoaderCardColorHex,
                                            cardOpacityPercent = pageLoaderCardOpacity,
                                            cardCornerRadiusDp = pageLoaderCardCornerRadius
                                        ),
                                        supportZoom = supportZoom,
                                        cacheEnabled = true,
                                        fileDownload = true,
                                        offlineFallback = offlineFallback
                                    ),
                                    bottomNavConfig = BottomNavConfig(
                                        enabled = bottomNavEnabled,
                                        destinations = bottomDestinations,
                                        barStyle = bottomNavBarStyle,
                                        showLabels = bottomNavShowLabels,
                                        activeColorHex = bottomNavActiveColorHex,
                                        inactiveColorHex = bottomNavInactiveColorHex,
                                        barColorHex = bottomNavBarColorHex
                                    ),
                                    packageConfig = PackageConfig(
                                        packageName = packageName.trim(),
                                        versionName = versionName.trim(),
                                        versionCode = versionCode,
                                        minSdk = minSdk,
                                        targetSdk = targetSdk
                                    ),
                                    permissionsConfig = PermissionsConfig(
                                        camera = permCamera,
                                        location = permLocation,
                                        microphone = permMicrophone,
                                        notifications = permNotifications,
                                        storage = permStorage,
                                        vibration = permVibration,
                                        bluetooth = permBluetooth,
                                        networkState = permNetworkState
                                    ),
                                    pushConfig = PushNotificationConfig(
                                        enabled = pushEnabled,
                                        provider = pushProvider,
                                        appId = pushAppId.trim(),
                                        channelName = pushChannelName.trim().ifBlank { "التنبيهات العامة" },
                                        soundEnabled = pushSoundEnabled,
                                        vibrationEnabled = pushVibrationEnabled,
                                        badgeEnabled = pushBadgeEnabled,
                                        openUrlOnTap = pushOpenUrlOnTap,
                                        autoPromptOnFirstLaunch = pushAutoPrompt,
                                        testTitle = pushTestTitle.trim(),
                                        testBody = pushTestBody.trim()
                                    ),
                                    bridgeConfig = initialProject?.bridgeConfig ?: BridgeAndCloudConfig(),
                                    proConfig = (initialProject?.proConfig ?: ProStudioConfig()).copy(
                                        iconShape = iconMaskShape,
                                        iconBgColorHex = iconBgColorHex
                                    ),
                                    buildHistory = initialProject?.buildHistory ?: emptyList()
                                )
                                onFinish(project)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp)
                            .testTag("wizard_next_btn")
                    ) {
                        Text(
                            text = if (currentStep == 9) "إنهاء وحفظ ✓" else "التالي >",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .testTag("wizard_step_content"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
        ) {
            // Live Interactive Smartphone Display Header (Shown when expanded)
            if (isPhoneExpanded) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = parseColorSafe(accentColorHex, OrangePrimary),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شاشة هاتف تفاعلية كاملة (المرحلة $currentStep من 9)",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (onOpenInteractivePhone != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(parseColorSafe(accentColorHex, OrangePrimary).copy(alpha = 0.15f))
                                            .clickable {
                                                val currentProj = (initialProject ?: AppProject(id = "preview_proj", name = appName)).copy(
                                                    name = appName.ifBlank { initialProject?.name ?: "تطبيق جديد" },
                                                    sourceType = sourceType,
                                                    sourceValue = sourceValue,
                                                    localWorkspacePath = localWorkspacePath,
                                                    accentColorHex = accentColorHex,
                                                    iconImageUri = selectedIconUri,
                                                    splashConfig = SplashConfig(
                                                        enabled = true,
                                                        backgroundColorHex = splashBgColorHex,
                                                        fadeBackground = splashFadeBg,
                                                        indicatorStyle = splashIndicatorStyle,
                                                        iconSizeDp = splashIconSize,
                                                        iconCornerRadiusDp = splashIconCorner,
                                                        indicatorSizeDp = splashIndicatorSize,
                                                        spacingDp = splashSpacing
                                                    ),
                                                    onboardingConfig = OnboardingConfig(
                                                        enabled = onboardingEnabled,
                                                        slides = onboardingSlides,
                                                        layoutType = onboardingLayoutType,
                                                        imageShape = onboardingImageShape,
                                                        gradientBackground = onboardingGradientBg,
                                                        accentColorHex = onboardingAccentColorHex,
                                                        textColorHex = onboardingTextColorHex,
                                                        cardColorHex = onboardingCardColorHex,
                                                        showSkipButton = onboardingShowSkipButton
                                                    ),
                                                    webViewConfig = WebViewConfig(
                                                        fullScreen = fullScreen,
                                                        pullToRefresh = pullToRefresh,
                                                        pageLoader = PageLoaderConfig(
                                                            enabled = pageLoaderEnabled,
                                                            style = pageLoaderStyle,
                                                            sizeDp = pageLoaderSize,
                                                            thicknessDp = pageLoaderThickness,
                                                            showOnCard = pageLoaderOnCard,
                                                            cardColorHex = pageLoaderCardColorHex,
                                                            cardOpacityPercent = pageLoaderCardOpacity,
                                                            cardCornerRadiusDp = pageLoaderCardCornerRadius
                                                        ),
                                                        supportZoom = supportZoom,
                                                        cacheEnabled = true,
                                                        fileDownload = true,
                                                        offlineFallback = offlineFallback
                                                    ),
                                                    bottomNavConfig = BottomNavConfig(
                                                        enabled = bottomNavEnabled,
                                                        destinations = bottomDestinations,
                                                        barStyle = bottomNavBarStyle,
                                                        showLabels = bottomNavShowLabels,
                                                        activeColorHex = bottomNavActiveColorHex,
                                                        inactiveColorHex = bottomNavInactiveColorHex,
                                                        barColorHex = bottomNavBarColorHex
                                                    ),
                                                    packageConfig = PackageConfig(packageName = packageName, versionName = versionName, versionCode = versionCode, minSdk = minSdk, targetSdk = targetSdk),
                                                    permissionsConfig = PermissionsConfig(
                                                        camera = permCamera,
                                                        location = permLocation,
                                                        microphone = permMicrophone,
                                                        notifications = permNotifications,
                                                        storage = permStorage,
                                                        vibration = permVibration,
                                                        bluetooth = permBluetooth,
                                                        networkState = permNetworkState
                                                    ),
                                                    pushConfig = PushNotificationConfig(
                                                        enabled = pushEnabled,
                                                        provider = pushProvider,
                                                        appId = pushAppId.trim(),
                                                        channelName = pushChannelName,
                                                        soundEnabled = pushSoundEnabled,
                                                        vibrationEnabled = pushVibrationEnabled,
                                                        badgeEnabled = pushBadgeEnabled,
                                                        openUrlOnTap = pushOpenUrlOnTap,
                                                        autoPromptOnFirstLaunch = pushAutoPrompt,
                                                        testTitle = pushTestTitle,
                                                        testBody = pushTestBody
                                                    ),
                                                    bridgeConfig = initialProject?.bridgeConfig ?: BridgeAndCloudConfig(),
                                                    proConfig = (initialProject?.proConfig ?: ProStudioConfig()).copy(
                                                        iconShape = iconMaskShape,
                                                        iconBgColorHex = iconBgColorHex
                                                    ),
                                                    buildHistory = initialProject?.buildHistory ?: emptyList()
                                                )
                                                onOpenInteractivePhone(currentProj)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "محاكي كامل 📱",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = parseColorSafe(accentColorHex, OrangePrimary)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { isPhoneExpanded = false },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إخفاء",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        PhoneMockupFrame(
                            step = currentStep,
                            appName = appName,
                            accentColor = parseColorSafe(accentColorHex, OrangePrimary),
                            iconUri = selectedIconUri,
                            splashBgColor = parseColorSafe(splashBgColorHex, Color(0xFF0F1318)),
                            splashFadeBg = splashFadeBg,
                            splashIndicatorStyle = splashIndicatorStyle,
                            splashIconSize = splashIconSize,
                            splashIconCorner = splashIconCorner,
                            splashIndicatorSize = splashIndicatorSize,
                            splashSpacing = splashSpacing,
                            onboardingEnabled = onboardingEnabled,
                            onboardingSlides = onboardingSlides,
                            fullScreen = fullScreen,
                            pullToRefresh = pullToRefresh,
                            bottomNavEnabled = bottomNavEnabled,
                            bottomDestinations = bottomDestinations,
                            packageName = packageName,
                            versionName = versionName,
                            permCamera = permCamera,
                            permLocation = permLocation,
                            permMicrophone = permMicrophone,
                            pushEnabled = pushEnabled,
                            height = 420.dp,
                            isInteractive = true
                        )
                    }
                }
            }

            when (currentStep) {
                // STEP 1: نوع المصدر
                1 -> {
                    item {
                        StepTitleHeader(title = "نوع المصدر", subtitle = "اختر الأرشيف أو مجلد HTML أو الرابط")
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("نوع المشروع:", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (sourceType == SourceType.ZIP_ARCHIVE) OrangePrimary.copy(alpha = 0.2f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (sourceType == SourceType.ZIP_ARCHIVE) OrangePrimary else GlassBorderSubtle),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { sourceType = SourceType.ZIP_ARCHIVE }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Archive, contentDescription = null, tint = OrangePrimary)
                                        Text("أرشيف ZIP", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                    }
                                }

                                Surface(
                                    color = if (sourceType == SourceType.HTML_FOLDER) OrangePrimary.copy(alpha = 0.2f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (sourceType == SourceType.HTML_FOLDER) OrangePrimary else GlassBorderSubtle),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { sourceType = SourceType.HTML_FOLDER }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Code, contentDescription = null, tint = OrangePrimary)
                                        Text("مجلد HTML", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                    }
                                }

                                Surface(
                                    color = if (sourceType == SourceType.WEB_URL) OrangePrimary.copy(alpha = 0.2f) else DarkSurface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (sourceType == SourceType.WEB_URL) OrangePrimary else GlassBorderSubtle),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { sourceType = SourceType.WEB_URL }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, tint = OrangePrimary)
                                        Text("رابط URL", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text("الملف / الرابط المحدد: $sourceValue", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextSecondary)

                            Button(
                                onClick = {
                                    try {
                                        sourceArchivePicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "text/html", "*/*"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "تعذر فتح منتقي الملفات", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("استيراد من ذاكرة الهاتف", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }

                // STEP 2: الهوية (Matching Screenshot 1 & 2 exactly with real data)
                2 -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "الهوية",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "الاسم وأيقونة المشغّل",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            // Quick phone simulator preview toggle
                            Surface(
                                color = if (isPhoneExpanded) OrangePrimary.copy(alpha = 0.2f) else Color(0xFF1E212D),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isPhoneExpanded) OrangePrimary else GlassBorderSubtle),
                                modifier = Modifier.clickable { isPhoneExpanded = !isPhoneExpanded }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (isPhoneExpanded) OrangePrimary else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPhoneExpanded) "إخفاء الهاتف" else "معاينة الهاتف",
                                        fontFamily = CairoFontFamily,
                                        fontSize = 11.sp,
                                        color = if (isPhoneExpanded) OrangePrimary else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // 1. App Name Input (Screenshot 1)
                    item {
                        Column {
                            OutlinedTextField(
                                value = appName,
                                onValueChange = {
                                    if (it.length <= 30) appName = it
                                },
                                label = {
                                    Text(
                                        text = "اسم التطبيق",
                                        fontFamily = CairoFontFamily,
                                        fontSize = 13.sp
                                    )
                                },
                                trailingIcon = {
                                    if (appName.isNotEmpty()) {
                                        IconButton(onClick = { appName = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = OrangePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("app_name_field"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = OrangePrimary,
                                    focusedLabelColor = OrangePrimary,
                                    unfocusedLabelColor = OrangePrimary.copy(alpha = 0.85f),
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = OrangePrimary,
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Character counter on left (Screenshot 1: 0/30)
                                Text(
                                    text = "${appName.length}/30",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )

                                // Helper note on right (Screenshot 1)
                                Text(
                                    text = "يظهر تحت أيقونة المشغّل. أبقه أقل من 30 حرفًا.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // 2. Icon Selection Section (Screenshot 1)
                    item {
                        Text(
                            text = "اختيار أيقونة",
                            fontFamily = CairoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Left side: Button + note
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Surface(
                                        color = Color(0xFF1A1E2B),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF33384D)),
                                        modifier = Modifier
                                            .clickable {
                                                try {
                                                    galleryPicker.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "الرجاء اختيار صورة من المعرض", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "استبدال الأيقونة",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "صورة PNG مربعة هي الأفضل. تُقصّ الصور غير المربعة.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Right side: Large icon preview (Screenshot 1: Android lime green robot or picked image)
                                val iconShape = when (iconMaskShape) {
                                    "circle" -> CircleShape
                                    "rounded" -> RoundedCornerShape(8.dp)
                                    "teardrop" -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 24.dp)
                                    else -> RoundedCornerShape(18.dp)
                                }

                                Box(
                                    modifier = Modifier
                                        .size(76.dp)
                                        .clip(iconShape)
                                        .background(
                                            if (selectedIconUri.isNullOrBlank()) Color(0xFFA4C639)
                                            else parseColorSafe(iconBgColorHex, Color(0xFF1E212E))
                                        )
                                        .border(
                                            1.5.dp,
                                            if (selectedIconUri.isNullOrBlank()) Color(0xFFA4C639)
                                            else parseColorSafe(accentColorHex, OrangePrimary),
                                            iconShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!selectedIconUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = selectedIconUri,
                                            contentDescription = "أيقونة التطبيق",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Android,
                                            contentDescription = "Android Robot",
                                            tint = Color.White,
                                            modifier = Modifier.size(52.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Accent Color Card (Screenshot 1)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .clickable { showColorPickerDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Color swatch square on left + hex text
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(parseColorSafe(accentColorHex, OrangePrimary))
                                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = accentColorHex.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }

                                // Label on right
                                Text(
                                    text = "لون التمييز",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // ====== NEW FEATURE 1: ميزة تخصيص قناع الأيقونة التكيفية لأندرويد ======
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Palette, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "ميزة 1: قناع وتخصيص الأيقونة التكيفية (Adaptive)",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "تعديل قناع قص الأيقونة التلقائي لأجهزة أندرويد الحديثة",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Switch(
                                    checked = isAdaptiveIconEnabled,
                                    onCheckedChange = { isAdaptiveIconEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
                                )
                            }

                            if (isAdaptiveIconEnabled) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "شكل قناع الأيقونة:",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "squircle" to "مربع مستدير",
                                        "circle" to "دائري",
                                        "rounded" to "مربع ناعم",
                                        "teardrop" to "دمعة"
                                    ).forEach { (shapeKey, shapeLabel) ->
                                        val isSelected = iconMaskShape == shapeKey
                                        Surface(
                                            color = if (isSelected) OrangePrimary.copy(alpha = 0.2f) else Color(0xFF161924),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) OrangePrimary else GlassBorderSubtle),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { iconMaskShape = shapeKey }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                val miniShape = when (shapeKey) {
                                                    "circle" -> CircleShape
                                                    "rounded" -> RoundedCornerShape(4.dp)
                                                    "teardrop" -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 1.dp, bottomEnd = 8.dp)
                                                    else -> RoundedCornerShape(6.dp)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(miniShape)
                                                        .background(if (isSelected) OrangePrimary else Color(0xFFA4C639))
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = shapeLabel,
                                                    fontFamily = CairoFontFamily,
                                                    fontSize = 10.sp,
                                                    color = if (isSelected) OrangePrimary else TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "خلفية الأيقونة (للأيقونات الشفافة):",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(
                                            "#FFFFFF" to Color.White,
                                            "#000000" to Color.Black,
                                            accentColorHex to parseColorSafe(accentColorHex, OrangePrimary),
                                            "#A4C639" to Color(0xFFA4C639)
                                        ).forEach { (hex, col) ->
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(col)
                                                    .border(
                                                        width = if (iconBgColorHex.equals(hex, ignoreCase = true)) 2.dp else 1.dp,
                                                        color = if (iconBgColorHex.equals(hex, ignoreCase = true)) OrangePrimary else Color(0xFF44475A),
                                                        shape = CircleShape
                                                    )
                                                    .clickable { iconBgColorHex = hex }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ====== NEW FEATURE 2: محاكي الظهور المباشر واقتراح أسماء ذكية ======
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceElevated)
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ميزة 2: محاكي ظهور التطبيق بالنظام",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }

                                Surface(
                                    color = Color(0xFF1E212D),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        val suggestions = listOf("بوابة الويب", "المحرر الذكي", "تطبيق النخبة", "Zk Studio", "متصفحي المتقدم", "تطبيق Rio")
                                        val nextIndex = ((suggestions.indexOf(appName) + 1) % suggestions.size).coerceAtLeast(0)
                                        appName = suggestions[nextIndex]
                                        Toast.makeText(context, "تم اختيار: $appName", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Stars, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "اقتراح اسم",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 11.sp,
                                            color = OrangePrimary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 1) Simulated System Notification Card
                            Surface(
                                color = Color(0xFF171926),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF282C3D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (selectedIconUri.isNullOrBlank()) Color(0xFFA4C639) else parseColorSafe(accentColorHex, OrangePrimary)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!selectedIconUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = selectedIconUri,
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Android, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (appName.isNotBlank()) appName else "اسم التطبيق",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(TextMuted))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "الآن",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(parseColorSafe(accentColorHex, OrangePrimary))
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "تطبيقك مبني وجاهز للتشغيل بكامل المميزات.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2) Launcher Drawer Icon Preview
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF141722))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val currentShape = when (iconMaskShape) {
                                        "circle" -> CircleShape
                                        "rounded" -> RoundedCornerShape(8.dp)
                                        "teardrop" -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
                                        else -> RoundedCornerShape(12.dp)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(currentShape)
                                            .background(
                                                if (selectedIconUri.isNullOrBlank()) Color(0xFFA4C639)
                                                else parseColorSafe(iconBgColorHex, Color(0xFF1E212E))
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!selectedIconUri.isNullOrBlank()) {
                                            AsyncImage(
                                                model = selectedIconUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Default.Android, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (appName.isNotBlank()) appName.take(10) else "تطبيقي",
                                        fontFamily = CairoFontFamily,
                                        fontSize = 10.sp,
                                        color = TextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = "معاينة الأيقونة على الشاشة الرئيسية",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "هكذا سيراها المستخدمون بعد التثبيت في شاشة هواتفهم.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 3: شاشة البداية (Matching Screenshots 1, 2, 3 exactly)
                3 -> {
                    item {
                        StepTitleHeader(title = "شاشة البداية", subtitle = "أول شاشة أثناء فتح التطبيق")
                    }

                    // Central Phone Preview (Matching Screenshot 1 exactly)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(370.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Realistic smartphone bezel
                            Box(
                                modifier = Modifier
                                    .width(210.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color(0xFF131520))
                                    .border(2.5.dp, Color(0xFF282C3D), RoundedCornerShape(32.dp))
                                    .padding(6.dp)
                            ) {
                                // Inner screen with splash background & live fade preview
                                val splashTransition = rememberInfiniteTransition(label = "step3_splash_fade")
                                val step3FadeAlpha by splashTransition.animateFloat(
                                    initialValue = 1f,
                                    targetValue = 0.25f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1600, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "step3_veil"
                                )
                                val splashBaseColor = parseColorSafe(splashBgColorHex, Color(0xFF0F1318))
                                val splashIndicatorColor = parseColorSafe(accentColorHex, OrangePrimary)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(
                                            if (splashFadeBg) {
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        splashIndicatorColor.copy(alpha = 0.22f * step3FadeAlpha),
                                                        splashBaseColor.copy(alpha = step3FadeAlpha),
                                                        Color(0xFF0A0C12)
                                                    )
                                                )
                                            } else {
                                                Brush.verticalGradient(listOf(splashBaseColor, splashBaseColor))
                                            }
                                        )
                                ) {
                                    // Top speaker ear-piece slit
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(top = 8.dp)
                                            .width(46.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0xFF2B2E3E))
                                    )

                                    // Centered Splash content
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        // App icon with user's selected shape and real asset
                                        val iconScaledSize = (splashIconSize * 0.7f).dp
                                        val iconScaledCorner = (splashIconCorner * 0.7f).dp

                                        Box(
                                            modifier = Modifier
                                                .size(iconScaledSize)
                                                .clip(RoundedCornerShape(iconScaledCorner))
                                                .background(
                                                    if (selectedIconUri.isNullOrBlank()) Color(0xFFA4C639)
                                                    else parseColorSafe(iconBgColorHex, Color(0xFF1E212E))
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!selectedIconUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = selectedIconUri,
                                                    contentDescription = "أيقونة التطبيق",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Android,
                                                    contentDescription = "Android Robot",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(iconScaledSize * 0.68f)
                                                )
                                            }
                                        }

                                        // Spacing between icon and loader
                                        Spacer(modifier = Modifier.height((splashSpacing * 0.7f).dp))

                                        // Spinner indicator matching selected style
                                        val indicatorScaledSize = (splashIndicatorSize * 0.7f).dp
                                        val indicatorColor = parseColorSafe(accentColorHex, OrangePrimary)

                                        when (splashIndicatorStyle) {
                                            "بدون" -> {}
                                            "شريط" -> {
                                                LinearProgressIndicator(
                                                    modifier = Modifier
                                                        .width((indicatorScaledSize * 1.5f).coerceAtLeast(36.dp))
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp)),
                                                    color = indicatorColor,
                                                    trackColor = indicatorColor.copy(alpha = 0.25f)
                                                )
                                            }
                                            "نبض" -> {
                                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                                val pulseAlpha by infiniteTransition.animateFloat(
                                                    initialValue = 0.2f,
                                                    targetValue = 1f,
                                                    animationSpec = infiniteRepeatable(
                                                        animation = tween(600, easing = FastOutSlowInEasing),
                                                        repeatMode = RepeatMode.Reverse
                                                    ),
                                                    label = "alpha"
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(indicatorScaledSize)
                                                        .clip(CircleShape)
                                                        .background(indicatorColor.copy(alpha = pulseAlpha))
                                                )
                                            }
                                            "حلقة - S" -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(indicatorScaledSize * 0.65f),
                                                    color = indicatorColor,
                                                    strokeWidth = 2.dp
                                                )
                                            }
                                            "حلقة" -> {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(indicatorScaledSize * 0.85f),
                                                    color = indicatorColor,
                                                    strokeWidth = 2.5.dp
                                                )
                                            }
                                            else -> { // "حلقة - L" default
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(indicatorScaledSize),
                                                    color = indicatorColor,
                                                    strokeWidth = 3.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Card 1: Configuration Card (Screenshot 1 & 3)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column {
                                // Row 1: خلفية شاشة البداية
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showSplashColorPickerDialog = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: Swatch + Hex
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(parseColorSafe(splashBgColorHex, Color(0xFF0F1318)))
                                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = splashBgColorHex.uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )
                                    }

                                    // Right: Title
                                    Text(
                                        text = "خلفية شاشة البداية",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                }

                                HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                // Row 2: تلاشي الخلفية
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = splashFadeBg,
                                        onCheckedChange = { splashFadeBg = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "تلاشي الخلفية",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                // Row 3: نمط المؤشر
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showIndicatorDialog = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = splashIndicatorStyle,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "نمط المؤشر",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: 4 Sliders Card (Screenshot 3)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                DottedSliderWithLabel(
                                    label = "حجم الأيقونة",
                                    value = splashIconSize,
                                    range = 60..200
                                ) { splashIconSize = it }

                                DottedSliderWithLabel(
                                    label = "زوايا الأيقونة",
                                    value = splashIconCorner,
                                    range = 0..50
                                ) { splashIconCorner = it }

                                DottedSliderWithLabel(
                                    label = "حجم المؤشر",
                                    value = splashIndicatorSize,
                                    range = 20..100
                                ) { splashIndicatorSize = it }

                                DottedSliderWithLabel(
                                    label = "التباعد",
                                    value = splashSpacing,
                                    range = 10..80
                                ) { splashSpacing = it }
                            }
                        }
                    }
                }

                // STEP 4: شاشات التعريف (Matching Screenshots 1, 2, 3 exactly)
                4 -> {
                    item {
                        StepTitleHeader(title = "شاشات التعريف", subtitle = "جولة تعريفية اختيارية عند أول تشغيل")
                    }

                    // Card 1: Toggle Switch Card (Screenshot 1)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = onboardingEnabled,
                                    onCheckedChange = { onboardingEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = OrangePrimary,
                                        uncheckedThumbColor = Color(0xFF6B7280),
                                        uncheckedTrackColor = Color(0xFF232838)
                                    )
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "إظهار شاشات التعريف عند أول تشغيل",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.RocketLaunch,
                                        contentDescription = null,
                                        tint = if (onboardingEnabled) OrangePrimary else Color(0xFF7A869A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (onboardingEnabled) {
                        val activeSlide = onboardingSlides.getOrElse(onboardingActiveSlideIndex.coerceIn(0, (onboardingSlides.size - 1).coerceAtLeast(0))) {
                            onboardingSlides.firstOrNull() ?: OnboardingSlide("العنوان", "الوصف", "bolt")
                        }

                        // Central Phone Mockup (Screenshot 2)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(370.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Realistic phone frame with bezel
                                Box(
                                    modifier = Modifier
                                        .width(210.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(Color(0xFF131520))
                                        .border(2.5.dp, Color(0xFF282C3D), RoundedCornerShape(32.dp))
                                        .padding(6.dp)
                                ) {
                                    val cardColor = parseColorSafe(onboardingCardColorHex, Color.White)
                                    val accentColor = parseColorSafe(onboardingAccentColorHex, Color(0xFFFFE236))
                                    val textColor = parseColorSafe(onboardingTextColorHex, Color(0xFF111827))

                                    // Inner phone screen
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(
                                                if (onboardingGradientBg) {
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            cardColor,
                                                            cardColor,
                                                            accentColor.copy(alpha = 0.25f)
                                                        )
                                                    )
                                                } else {
                                                    Brush.verticalGradient(listOf(cardColor, cardColor))
                                                }
                                            )
                                    ) {
                                        // Top speaker notch
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 8.dp)
                                                .width(46.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF2B2E3E))
                                        )

                                        // Skip button (Advanced Feature 1)
                                        if (onboardingShowSkipButton) {
                                            Text(
                                                text = "تخطي",
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.sp,
                                                color = textColor.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 16.dp, end = 12.dp)
                                                    .clickable {
                                                        onboardingActiveSlideIndex = (onboardingSlides.size - 1).coerceAtLeast(0)
                                                    }
                                            )
                                        }

                                        // Main slide content
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp, vertical = 20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Spacer(modifier = Modifier.height(18.dp))

                                            // Center illustration / icon area (Screenshot 2)
                                            val shapeModifier = when (onboardingImageShape) {
                                                "مربع منحني" -> RoundedCornerShape(20.dp)
                                                "كبسولة" -> RoundedCornerShape(50)
                                                else -> CircleShape // "دائرة"
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(105.dp)
                                                    .clip(shapeModifier)
                                                    .background(Color(0xFFFEFCE8)) // Light warm cream circle as in Screenshot 2
                                                    .border(1.dp, Color(0xFFFEF08A).copy(alpha = 0.6f), shapeModifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (!activeSlide.imageUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = activeSlide.imageUri,
                                                        contentDescription = "صورة الشاشة",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    val iconVector = when (activeSlide.iconName) {
                                                        "rocket" -> Icons.Default.RocketLaunch
                                                        "star" -> Icons.Default.Star
                                                        "shield" -> Icons.Default.Shield
                                                        "diamond" -> Icons.Default.Diamond
                                                        "notification" -> Icons.Default.Notifications
                                                        "web" -> Icons.Default.Language
                                                        "design" -> Icons.Default.Palette
                                                        else -> Icons.Default.Bolt
                                                    }
                                                    Icon(
                                                        imageVector = iconVector,
                                                        contentDescription = null,
                                                        tint = OrangePrimary,
                                                        modifier = Modifier.size(52.dp)
                                                    )
                                                }
                                            }

                                            // Text block (Headline + Description)
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = activeSlide.title.ifBlank { "Headline" },
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = textColor
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = activeSlide.description.ifBlank { "A sentence about this screen" },
                                                    fontFamily = TajawalFontFamily,
                                                    fontSize = 10.5.sp,
                                                    color = textColor.copy(alpha = 0.65f),
                                                    maxLines = 2
                                                )
                                            }

                                            // Page indicator dots inside phone
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                for (i in onboardingSlides.indices) {
                                                    val isCur = i == onboardingActiveSlideIndex
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (isCur) 6.dp else 4.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isCur) OrangePrimary else textColor.copy(alpha = 0.25f)
                                                            )
                                                    )
                                                }
                                            }

                                            // Bottom Action Pill Button (Screenshot 2: rounded yellow/accent pill)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.85f)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(17.dp))
                                                    .background(accentColor)
                                                    .clickable {
                                                        if (onboardingSlides.isNotEmpty()) {
                                                            onboardingActiveSlideIndex = (onboardingActiveSlideIndex + 1) % onboardingSlides.size
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val isLast = onboardingActiveSlideIndex == onboardingSlides.size - 1
                                                Text(
                                                    text = if (isLast) "ابدأ الآن" else "التالي",
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = if (accentColor.red * 0.299f + accentColor.green * 0.587f + accentColor.blue * 0.114f > 0.5f) Color(0xFF111827) else Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Dots row below phone (Screenshot 2 & 3)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in onboardingSlides.indices) {
                                    val isCur = i == onboardingActiveSlideIndex
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 3.dp)
                                            .size(if (isCur) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCur) OrangePrimary else Color(0xFF4B5563)
                                            )
                                            .clickable { onboardingActiveSlideIndex = i }
                                    )
                                }
                            }
                        }

                        // Slide Editor Card (Screenshot 2 bottom & Screenshot 3)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // Header: Title "الشاشة X" on right, Delete button on left
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (onboardingSlides.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    val newSlides = onboardingSlides.toMutableList()
                                                    newSlides.removeAt(onboardingActiveSlideIndex)
                                                    onboardingSlides = newSlides
                                                    onboardingActiveSlideIndex = (onboardingActiveSlideIndex - 1).coerceAtLeast(0)
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "حذف الشاشة",
                                                    tint = Color(0xFFEF4444)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(32.dp))
                                        }

                                        Text(
                                            text = "الشاشة ${onboardingActiveSlideIndex + 1}",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFFD1D5DB)
                                        )
                                    }

                                    // Row with Title input on left, Big Square Icon Button on right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Title TextField
                                        OutlinedTextField(
                                            value = activeSlide.title,
                                            onValueChange = { newTitle ->
                                                val newSlides = onboardingSlides.toMutableList()
                                                newSlides[onboardingActiveSlideIndex] = newSlides[onboardingActiveSlideIndex].copy(title = newTitle)
                                                onboardingSlides = newSlides
                                            },
                                            placeholder = {
                                                Text("العنوان", fontFamily = CairoFontFamily, color = TextMuted)
                                            },
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = OrangePrimary,
                                                unfocusedBorderColor = Color(0xFF262C3D),
                                                focusedContainerColor = Color(0xFF0F121C),
                                                unfocusedContainerColor = Color(0xFF0F121C),
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Right: Square Icon / Image Box (Screenshot 3)
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF1B2030))
                                                .border(1.dp, Color(0xFF323B50), RoundedCornerShape(14.dp))
                                                .clickable {
                                                    showIconPickerDialogForSlideIndex = onboardingActiveSlideIndex
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!activeSlide.imageUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = activeSlide.imageUri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            } else {
                                                val iconVector = when (activeSlide.iconName) {
                                                    "rocket" -> Icons.Default.RocketLaunch
                                                    "star" -> Icons.Default.Star
                                                    "shield" -> Icons.Default.Shield
                                                    "diamond" -> Icons.Default.Diamond
                                                    "notification" -> Icons.Default.Notifications
                                                    "web" -> Icons.Default.Language
                                                    "design" -> Icons.Default.Palette
                                                    else -> Icons.Default.Bolt
                                                }
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = "أيقونة",
                                                    tint = OrangePrimary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Description TextField
                                    OutlinedTextField(
                                        value = activeSlide.description,
                                        onValueChange = { newDesc ->
                                            val newSlides = onboardingSlides.toMutableList()
                                            newSlides[onboardingActiveSlideIndex] = newSlides[onboardingActiveSlideIndex].copy(description = newDesc)
                                            onboardingSlides = newSlides
                                        },
                                        placeholder = {
                                            Text("الوصف", fontFamily = CairoFontFamily, color = TextMuted)
                                        },
                                        minLines = 2,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = Color(0xFF262C3D),
                                            focusedContainerColor = Color(0xFF0F121C),
                                            unfocusedContainerColor = Color(0xFF0F121C),
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Add Screen Button (Screenshot 3)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                                    .clickable {
                                        onboardingSlides = onboardingSlides + OnboardingSlide("العنوان", "الوصف", "bolt")
                                        onboardingActiveSlideIndex = onboardingSlides.size - 1
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "إضافة شاشة",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Configuration / Styling Card (Screenshot 3: 6 rows)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                            ) {
                                Column {
                                    // Row 1: التخطيط
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showLayoutTypeDialog = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = onboardingLayoutType,
                                            fontFamily = CairoFontFamily,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "التخطيط",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.GridView,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 2: شكل الصورة
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showImageShapeDialog = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = onboardingImageShape,
                                            fontFamily = CairoFontFamily,
                                            fontSize = 13.sp,
                                            color = TextSecondary
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "شكل الصورة",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.Image,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 3: خلفية متدرجة
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = onboardingGradientBg,
                                            onCheckedChange = { onboardingGradientBg = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = OrangePrimary,
                                                uncheckedThumbColor = Color(0xFF6B7280),
                                                uncheckedTrackColor = Color(0xFF232838)
                                            )
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "خلفية متدرجة",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.Palette,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 4: التمييز
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showOnboardingAccentColorPicker = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(parseColorSafe(onboardingAccentColorHex, Color(0xFFFFE236)))
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = onboardingAccentColorHex.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "التمييز",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 5: لون النص
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showOnboardingTextColorPicker = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(parseColorSafe(onboardingTextColorHex, Color(0xFF111827)))
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = onboardingTextColorHex.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "لون النص",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 6: لون البطاقة
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showOnboardingCardColorPicker = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(parseColorSafe(onboardingCardColorHex, Color.White))
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = onboardingCardColorHex.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "لون البطاقة",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Additional Advanced Feature: زر التخطي السريع
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = onboardingShowSkipButton,
                                            onCheckedChange = { onboardingShowSkipButton = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = OrangePrimary,
                                                uncheckedThumbColor = Color(0xFF6B7280),
                                                uncheckedTrackColor = Color(0xFF232838)
                                            )
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "زر التخطي السريع",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.FastForward,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 5: تفعيل الميزات (Matching Screenshots 1, 2, 3 exactly)
                5 -> {
                    item {
                        StepTitleHeader(title = "تفعيل الميزات", subtitle = "ملء الشاشة، والسحب للتحديث، ومؤشر تحميل الصفحة")
                    }

                    // Card 1: ملء الشاشة & اسحب للأسفل للتحديث (Screenshot 1)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column {
                                // Row 1: ملء الشاشة
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = fullScreen,
                                        onCheckedChange = { fullScreen = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = OrangePrimary,
                                            uncheckedThumbColor = Color(0xFF6B7280),
                                            uncheckedTrackColor = Color(0xFF232838)
                                        )
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "ملء الشاشة",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "إخفاء شريطي الحالة والتنقل؛ اسحب من الحافة لإظهارهما",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.Fullscreen,
                                            contentDescription = null,
                                            tint = if (fullScreen) OrangePrimary else Color(0xFF7A869A),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                // Row 2: اسحب للأسفل للتحديث
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = pullToRefresh,
                                        onCheckedChange = { pullToRefresh = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = OrangePrimary,
                                            uncheckedThumbColor = Color(0xFF6B7280),
                                            uncheckedTrackColor = Color(0xFF232838)
                                        )
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "اسحب للأسفل للتحديث",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = if (pullToRefresh) OrangePrimary else Color(0xFF7A869A),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Card 2: إظهار مؤشر أثناء تحميل الصفحات (Screenshot 1 & 2)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = pageLoaderEnabled,
                                    onCheckedChange = { pageLoaderEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = OrangePrimary,
                                        uncheckedThumbColor = Color(0xFF6B7280),
                                        uncheckedTrackColor = Color(0xFF232838)
                                    )
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "إظهار مؤشر أثناء تحميل الصفحات",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (pageLoaderEnabled) OrangePrimary else Color(0xFF7A869A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // When pageLoaderEnabled is true: Phone Mockup, Style Cards, Sliders, Card container (Screenshots 2 & 3)
                    if (pageLoaderEnabled) {
                        val indicatorColor = parseColorSafe(accentColorHex, Color(0xFFFFE236))
                        val cardBg = parseColorSafe(pageLoaderCardColorHex, Color.White).copy(alpha = (pageLoaderCardOpacity / 100f).coerceIn(0.1f, 1f))

                        // Central Phone Mockup (Screenshot 2)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(370.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(210.dp)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(32.dp))
                                        .background(Color(0xFF131520))
                                        .border(2.5.dp, Color(0xFF282C3D), RoundedCornerShape(32.dp))
                                        .padding(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(26.dp))
                                            .background(Color.White)
                                    ) {
                                        // Top speaker notch (hides if fullScreen for immersion)
                                        if (!fullScreen) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopCenter)
                                                    .padding(top = 8.dp)
                                                    .width(46.dp)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(0xFF2B2E3E))
                                            )
                                        }

                                        // Webpage Content Simulation (Skeleton article lines)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(top = if (fullScreen) 16.dp else 26.dp, start = 14.dp, end = 14.dp, bottom = 14.dp)
                                        ) {
                                            // Simulated headline
                                            Box(
                                                modifier = Modifier
                                                    .width(100.dp)
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFCBD5E1))
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            // Simulated paragraphs
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.95f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFFE2E8F0))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.85f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFFE2E8F0))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.6f)
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFFE2E8F0))
                                            )
                                        }

                                        // Live Loader in the center of the web page (Screenshot 2)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                        ) {
                                            val loaderContent: @Composable () -> Unit = {
                                                when (pageLoaderStyle) {
                                                    "شريط" -> {
                                                        LinearProgressIndicator(
                                                            modifier = Modifier
                                                                .width((pageLoaderSize * 0.9f).coerceIn(40f, 130f).dp)
                                                                .height((pageLoaderThickness * 0.7f).coerceIn(3f, 16f).dp)
                                                                .clip(RoundedCornerShape(4.dp)),
                                                            color = indicatorColor,
                                                            trackColor = indicatorColor.copy(alpha = 0.25f)
                                                        )
                                                    }
                                                    "حلقة بالشعار" -> {
                                                        Box(
                                                            modifier = Modifier.size((pageLoaderSize * 0.65f).coerceIn(40f, 100f).dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.fillMaxSize(),
                                                                color = indicatorColor,
                                                                strokeWidth = (pageLoaderThickness * 0.6f).coerceIn(2.5f, 10f).dp
                                                            )
                                                            if (!selectedIconUri.isNullOrBlank()) {
                                                                AsyncImage(
                                                                    model = selectedIconUri,
                                                                    contentDescription = null,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier
                                                                        .size((pageLoaderSize * 0.32f).coerceIn(18f, 40f).dp)
                                                                        .clip(CircleShape)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    imageVector = Icons.Default.Android,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFFA4C639),
                                                                    modifier = Modifier.size((pageLoaderSize * 0.35f).coerceIn(18f, 40f).dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    "شريط بالشعار" -> {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            if (!selectedIconUri.isNullOrBlank()) {
                                                                AsyncImage(
                                                                    model = selectedIconUri,
                                                                    contentDescription = null,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier
                                                                        .size(32.dp)
                                                                        .clip(CircleShape)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    imageVector = Icons.Default.Android,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFFA4C639),
                                                                    modifier = Modifier.size(32.dp)
                                                                )
                                                            }
                                                            LinearProgressIndicator(
                                                                modifier = Modifier
                                                                    .width((pageLoaderSize * 0.9f).coerceIn(40f, 120f).dp)
                                                                    .height((pageLoaderThickness * 0.7f).coerceIn(3f, 14f).dp)
                                                                    .clip(RoundedCornerShape(3.dp)),
                                                                color = indicatorColor,
                                                                trackColor = indicatorColor.copy(alpha = 0.25f)
                                                            )
                                                        }
                                                    }
                                                    else -> { // "حلقة" (default Screenshot 2)
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size((pageLoaderSize * 0.65f).coerceIn(36f, 100f).dp),
                                                            color = indicatorColor,
                                                            strokeWidth = (pageLoaderThickness * 0.7f).coerceIn(2.5f, 12f).dp
                                                        )
                                                    }
                                                }
                                            }

                                            if (pageLoaderOnCard) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape((pageLoaderCardCornerRadius * 0.75f).dp))
                                                        .background(cardBg)
                                                        .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape((pageLoaderCardCornerRadius * 0.75f).dp))
                                                        .padding(14.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    loaderContent()
                                                }
                                            } else {
                                                loaderContent()
                                            }
                                        }

                                        // Pull to refresh banner simulator (Feature 1)
                                        if (pullToRefresh) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 10.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                                                    .clickable {
                                                        Toast.makeText(context, "جارٍ محاكاة السحب للتحديث...", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("جرب السحب للأسفل للتحديث", fontFamily = TajawalFontFamily, fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Loader Style Selection Cards (Screenshot 2)
                        val stylesList = listOf(
                            "حلقة" to Icons.Default.Schedule,
                            "شريط" to Icons.Default.HorizontalRule,
                            "حلقة بالشعار" to Icons.Default.AutoAwesome,
                            "شريط بالشعار" to Icons.Default.AutoAwesome
                        )

                        items(stylesList.size) { idx ->
                            val (style, icon) = stylesList[idx]
                            val isSel = pageLoaderStyle == style

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) Color(0xFF261D15) else Color(0xFF131622))
                                    .border(
                                        width = if (isSel) 1.5.dp else 1.dp,
                                        color = if (isSel) OrangePrimary else GlassBorderSubtle,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { pageLoaderStyle = style }
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: Orange Checkmark if selected
                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "محدد",
                                            tint = OrangePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(20.dp))
                                    }

                                    // Right: Style Name + Icon
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = style,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isSel) OrangePrimary else TextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSel) OrangePrimary else TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Card: أشرطة المؤشر (Screenshot 3)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    DottedSliderWithLabel(
                                        label = "حجم المؤشر",
                                        value = pageLoaderSize,
                                        range = 30..150
                                    ) { pageLoaderSize = it }

                                    DottedSliderWithLabel(
                                        label = "السماكة",
                                        value = pageLoaderThickness,
                                        range = 2..20
                                    ) { pageLoaderThickness = it }
                                }
                            }
                        }

                        // Card: إعدادات البطاقة (Screenshot 3)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    // Row 1: عرض على بطاقة
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = pageLoaderOnCard,
                                            onCheckedChange = { pageLoaderOnCard = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = OrangePrimary,
                                                uncheckedThumbColor = Color(0xFF6B7280),
                                                uncheckedTrackColor = Color(0xFF232838)
                                            )
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "عرض على بطاقة",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.GridView,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    if (pageLoaderOnCard) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Row 2: لون البطاقة
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showPageLoaderCardColorPicker = true }
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(parseColorSafe(pageLoaderCardColorHex, Color.White))
                                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = pageLoaderCardColorHex.uppercase(),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    color = TextSecondary
                                                )
                                            }

                                            Text(
                                                text = "لون البطاقة",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Slider 3: شفافية البطاقة
                                        DottedSliderWithLabel(
                                            label = "شفافية البطاقة",
                                            value = pageLoaderCardOpacity,
                                            range = 10..100
                                        ) { pageLoaderCardOpacity = it }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // Slider 4: زوايا البطاقة
                                        DottedSliderWithLabel(
                                            label = "زوايا البطاقة",
                                            value = pageLoaderCardCornerRadius,
                                            range = 0..30
                                        ) { pageLoaderCardCornerRadius = it }
                                    }
                                }
                            }
                        }
                    }

                    // Advanced Feature 2: محرك الويب المتقدم (Smart Web Engine Card)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column {
                                // Row 1: حفظ الذاكرة المؤقتة (Offline Cache)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = offlineFallback,
                                        onCheckedChange = { offlineFallback = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = OrangePrimary,
                                            uncheckedThumbColor = Color(0xFF6B7280),
                                            uncheckedTrackColor = Color(0xFF232838)
                                        )
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "التصفح دون إنترنت (Offline Cache)",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "حفظ نسخة مؤقتة وتوفير صفحة بديلة عند انقطاع الشبكة",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = if (offlineFallback) OrangePrimary else Color(0xFF7A869A),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                // Row 2: التحكم في التكبير (Zoom Support)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = supportZoom,
                                        onCheckedChange = { supportZoom = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = OrangePrimary,
                                            uncheckedThumbColor = Color(0xFF6B7280),
                                            uncheckedTrackColor = Color(0xFF232838)
                                        )
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "التحكم في التكبير (Pinch to Zoom)",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "السماح للمستخدم بتكبير وتصغير محتوى الصفحات",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.ZoomIn,
                                            contentDescription = null,
                                            tint = if (supportZoom) OrangePrimary else Color(0xFF7A869A),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 6: التنقل السفلي (02:28 - 02:34) - matching screenshots perfectly
                6 -> {
                    item {
                        StepTitleHeader(title = "التنقل السفلي", subtitle = "حتى خمس وجهات")
                    }

                    // Card 1: Toggle Switch (Screenshot 1 & 2)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = bottomNavEnabled,
                                    onCheckedChange = { bottomNavEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = OrangePrimary,
                                        uncheckedThumbColor = Color(0xFF6B7280),
                                        uncheckedTrackColor = Color(0xFF232838)
                                    )
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "إظهار شريط تنقل سفلي",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Icon(
                                        imageVector = Icons.Default.GridView,
                                        contentDescription = null,
                                        tint = if (bottomNavEnabled) OrangePrimary else Color(0xFF7A869A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (bottomNavEnabled) {
                        // Central Smartphone Mockup (Screenshot 2) with Interactive Tab Switching (Advanced Feature 1)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(190.dp)
                                        .height(280.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(Color(0xFF0F111A))
                                        .border(2.5.dp, Color(0xFF2B3045), RoundedCornerShape(26.dp))
                                ) {
                                    // Screen inside bezel
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color.White)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Top section with notch and skeleton web lines
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp, start = 12.dp, end = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Speaker / Camera notch
                                                Box(
                                                    modifier = Modifier
                                                        .width(42.dp)
                                                        .height(7.dp)
                                                        .clip(RoundedCornerShape(3.5.dp))
                                                        .background(Color(0xFF1E2433))
                                                )

                                                Spacer(modifier = Modifier.height(14.dp))

                                                // Web page skeleton lines matching Screenshot 2
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(4.5.dp)
                                                ) {
                                                    Box(modifier = Modifier.width(90.dp).height(3.5.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE2E8F0)))
                                                    Box(modifier = Modifier.fillMaxWidth().height(3.5.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE2E8F0)))
                                                    Box(modifier = Modifier.fillMaxWidth().height(3.5.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE2E8F0)))
                                                    Box(modifier = Modifier.width(85.dp).height(3.5.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE2E8F0)))
                                                    Box(modifier = Modifier.width(130.dp).height(3.5.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE2E8F0)))
                                                    Box(modifier = Modifier.width(60.dp).height(3.5.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFE2E8F0)))
                                                }

                                                Spacer(modifier = Modifier.height(18.dp))

                                                // Dynamic active tab card simulation
                                                val safeIdx = selectedBottomNavPreviewIndex.coerceIn(0, (bottomDestinations.size - 1).coerceAtLeast(0))
                                                val activeDest = bottomDestinations.getOrNull(safeIdx)
                                                if (activeDest != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(Color(0xFFF8FAFC))
                                                            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                                            .padding(8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                imageVector = getBottomNavIcon(activeDest.iconName),
                                                                contentDescription = null,
                                                                tint = parseColorSafe(bottomNavActiveColorHex, OrangePrimary),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                            Text(
                                                                text = activeDest.title.ifBlank { "Home" },
                                                                fontFamily = CairoFontFamily,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF1E293B)
                                                            )
                                                            Text(
                                                                text = activeDest.targetUrl.ifBlank { "/" },
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 8.sp,
                                                                color = Color(0xFF64748B)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Bottom Navigation Bar inside Phone Mockup
                                            val barBgColor = parseColorSafe(bottomNavBarColorHex, Color.White)
                                            val activeColor = parseColorSafe(bottomNavActiveColorHex, OrangePrimary)
                                            val inactiveColor = parseColorSafe(bottomNavInactiveColorHex, Color(0xFF9CA3AF))

                                            val barShapeModifier = when (bottomNavBarStyle) {
                                                "عائم" -> Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(barBgColor)
                                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                                "كبسولة" -> Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(barBgColor)
                                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                                else -> Modifier
                                                    .fillMaxWidth()
                                                    .background(barBgColor)
                                                    .border(
                                                        width = 0.5.dp,
                                                        color = Color(0xFFE2E8F0)
                                                    )
                                            }

                                            Box(
                                                modifier = barShapeModifier.padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceAround,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    bottomDestinations.take(5).forEachIndexed { idx, dest ->
                                                        val isSelected = idx == selectedBottomNavPreviewIndex.coerceIn(0, (bottomDestinations.size - 1).coerceAtLeast(0))
                                                        val tintColor = if (isSelected) activeColor else inactiveColor

                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            modifier = Modifier
                                                                .clickable {
                                                                    selectedBottomNavPreviewIndex = idx
                                                                }
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            if (bottomDestinations.size == 1) {
                                                                // Single item dot matching Screenshot 2
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(6.dp)
                                                                        .clip(CircleShape)
                                                                        .background(tintColor)
                                                                )
                                                            } else {
                                                                Icon(
                                                                    imageVector = getBottomNavIcon(dest.iconName),
                                                                    contentDescription = dest.title,
                                                                    tint = tintColor,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }

                                                            if (bottomNavShowLabels) {
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(
                                                                    text = dest.title.ifBlank { "Home" },
                                                                    fontFamily = CairoFontFamily,
                                                                    fontSize = 8.sp,
                                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                    color = tintColor,
                                                                    maxLines = 1
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
                        }

                        // Destination Cards (Screenshot 2 & 3)
                        items(bottomDestinations.size) { idx ->
                            val dest = bottomDestinations[idx]
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Row 1: Red Delete button + Label OutlinedTextField + Square Icon button
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Red delete button (far left)
                                        IconButton(
                                            onClick = {
                                                if (bottomDestinations.size > 1) {
                                                    val updated = bottomDestinations.filterIndexed { i, _ -> i != idx }
                                                    bottomDestinations = updated
                                                    if (selectedBottomNavPreviewIndex >= updated.size) {
                                                        selectedBottomNavPreviewIndex = updated.size - 1
                                                    }
                                                } else {
                                                    Toast.makeText(context, "يجب الإبقاء على وجهة واحدة على الأقل", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            enabled = bottomDestinations.size > 1,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "حذف الوجهة",
                                                tint = if (bottomDestinations.size > 1) Color(0xFFEF4444) else Color(0xFF4B5563),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Label OutlinedTextField (middle)
                                        OutlinedTextField(
                                            value = dest.title,
                                            onValueChange = { newTitle ->
                                                val updated = bottomDestinations.toMutableList()
                                                updated[idx] = dest.copy(title = newTitle)
                                                bottomDestinations = updated
                                            },
                                            label = { Text("التسمية", fontFamily = TajawalFontFamily, fontSize = 12.sp) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontFamily = CairoFontFamily,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = OrangePrimary,
                                                unfocusedBorderColor = Color(0xFF262C40),
                                                focusedLabelColor = OrangePrimary,
                                                unfocusedLabelColor = Color(0xFF7A869A),
                                                cursorColor = OrangePrimary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f)
                                        )

                                        // Square Icon Button (far right) - opens icon selector dialog
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF1E2235))
                                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                                                .clickable {
                                                    showBottomNavIconPickerDialogForIndex = idx
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getBottomNavIcon(dest.iconName),
                                                contentDescription = "أيقونة الوجهة",
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    // Row 2: Target URL TextField with Link icon (Screenshot 2 & 3)
                                    OutlinedTextField(
                                        value = dest.targetUrl,
                                        onValueChange = { newUrl ->
                                            val updated = bottomDestinations.toMutableList()
                                            updated[idx] = dest.copy(targetUrl = newUrl)
                                            bottomDestinations = updated
                                        },
                                        placeholder = { Text("العنوان", fontFamily = TajawalFontFamily, fontSize = 12.sp, color = TextMuted) },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = CairoFontFamily,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        ),
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = Color(0xFF7A869A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = OrangePrimary,
                                            unfocusedBorderColor = Color(0xFF262C40),
                                            cursorColor = OrangePrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Full-width "+ إضافة وجهة" Button (Screenshot 2 & 3)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF131622))
                                    .border(
                                        width = 1.dp,
                                        color = if (bottomDestinations.size < 5) GlassBorderSubtle else Color(0xFF232838),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = bottomDestinations.size < 5) {
                                        if (bottomDestinations.size < 5) {
                                            val newIdx = bottomDestinations.size + 1
                                            val (defTitle, defUrl, defIcon) = when (newIdx) {
                                                2 -> Triple("Search", "/search", "search")
                                                3 -> Triple("Cart", "/cart", "shopping_cart")
                                                4 -> Triple("Account", "/profile", "person")
                                                5 -> Triple("Settings", "/settings", "settings")
                                                else -> Triple("Tab $newIdx", "/page$newIdx", "explore")
                                            }
                                            bottomDestinations = bottomDestinations + BottomNavDestination(
                                                id = "${System.currentTimeMillis()}",
                                                title = defTitle,
                                                targetUrl = defUrl,
                                                iconName = defIcon
                                            )
                                            selectedBottomNavPreviewIndex = bottomDestinations.size - 1
                                        } else {
                                            Toast.makeText(context, "الحد الأقصى هو 5 وجهات", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (bottomDestinations.size < 5) "إضافة وجهة" else "تم بلوغ الحد الأقصى (5 وجهات)",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (bottomDestinations.size < 5) Color.White else Color(0xFF6B7280)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (bottomDestinations.size < 5) Color.White else Color(0xFF6B7280),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Navigation Styling Card (Screenshot 3)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                            ) {
                                Column {
                                    // Row 1: نمط الشريط (مسطح)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showBottomNavBarStyleDialog = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = bottomNavBarStyle,
                                            fontFamily = CairoFontFamily,
                                            fontSize = 13.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "نمط الشريط",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = null,
                                                tint = Color(0xFF7A869A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 2: إظهار التسميات
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = bottomNavShowLabels,
                                            onCheckedChange = { bottomNavShowLabels = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = OrangePrimary,
                                                uncheckedThumbColor = Color(0xFF6B7280),
                                                uncheckedTrackColor = Color(0xFF232838)
                                            )
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "إظهار التسميات",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Default.Label,
                                                contentDescription = null,
                                                tint = if (bottomNavShowLabels) OrangePrimary else Color(0xFF7A869A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 3: اللون النشط
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showBottomNavActiveColorPicker = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp, 22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(parseColorSafe(bottomNavActiveColorHex, OrangePrimary))
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bottomNavActiveColorHex.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "اللون النشط",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 4: اللون غير النشط
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showBottomNavInactiveColorPicker = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp, 22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(parseColorSafe(bottomNavInactiveColorHex, Color(0xFF9CA3AF)))
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bottomNavInactiveColorHex.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "اللون غير النشط",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                    // Row 5: لون الشريط
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showBottomNavBarColorPicker = true }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp, 22.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(parseColorSafe(bottomNavBarColorHex, Color.White))
                                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bottomNavBarColorHex.uppercase(),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "لون الشريط",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Advanced Feature 2: Smart Navigation Preset Templates (قوالب التبويبات الذكية)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF131622))
                                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ميزة ذكية",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OrangePrimary
                                        )
                                        Text(
                                            text = "قوالب تبويبات جاهزة بضغطة واحدة",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Preset 1: متجر إلكتروني
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF1A1F30))
                                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    bottomDestinations = listOf(
                                                        BottomNavDestination("1", "الرئيسية", "/", "home"),
                                                        BottomNavDestination("2", "المتجر", "/shop", "shopping_bag"),
                                                        BottomNavDestination("3", "السلة", "/cart", "shopping_cart"),
                                                        BottomNavDestination("4", "حسابي", "/account", "person")
                                                    )
                                                    selectedBottomNavPreviewIndex = 0
                                                    Toast.makeText(context, "تم تطبيق قالب المتجر الإلكتروني", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("متجر", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                            }
                                        }

                                        // Preset 2: إخباري / مدونة
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF1A1F30))
                                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    bottomDestinations = listOf(
                                                        BottomNavDestination("1", "الرئيسية", "/", "home"),
                                                        BottomNavDestination("2", "استكشاف", "/explore", "explore"),
                                                        BottomNavDestination("3", "محفوظات", "/bookmarks", "bookmark"),
                                                        BottomNavDestination("4", "إعدادات", "/settings", "settings")
                                                    )
                                                    selectedBottomNavPreviewIndex = 0
                                                    Toast.makeText(context, "تم تطبيق قالب المدونة الإخبارية", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Article, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("إخباري", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                            }
                                        }

                                        // Preset 3: خدمات وتواصل
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF1A1F30))
                                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
                                                .clickable {
                                                    bottomDestinations = listOf(
                                                        BottomNavDestination("1", "الرئيسية", "/", "home"),
                                                        BottomNavDestination("2", "الخدمات", "/services", "dashboard"),
                                                        BottomNavDestination("3", "محادثة", "/chat", "chat"),
                                                        BottomNavDestination("4", "اتصل بنا", "/contact", "phone")
                                                    )
                                                    selectedBottomNavPreviewIndex = 0
                                                    Toast.makeText(context, "تم تطبيق قالب الخدمات والتواصل", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Chat, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("خدمات", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 7: الحزمة (02:35 - 02:38) - matching screenshot perfectly
                7 -> {
                    item {
                        StepTitleHeader(title = "الحزمة", subtitle = "معرّف الحزمة والإصدار")
                    }

                    // Package ID Outlined Field with 3D Cube Icon (Screenshot)
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = packageName,
                                onValueChange = { packageName = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '.' || c == '_' } },
                                label = { Text("معرّف الحزمة", fontFamily = TajawalFontFamily, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                ),
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ViewInAr,
                                        contentDescription = null,
                                        tint = Color(0xFF7A869A),
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = OrangePrimary.copy(alpha = 0.85f),
                                    focusedLabelColor = OrangePrimary,
                                    unfocusedLabelColor = OrangePrimary,
                                    cursorColor = OrangePrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Warning Notice row matching Screenshot
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "يجب أن يكون بصيغة com.company.app — لا يمكن تغييره أبدًا بعد النشر.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFFBBF24)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Side-by-side fields: رمز الإصدار (Left in RTL) & اسم الإصدار (Right in RTL) matching Screenshot
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Left Box in RTL: رمز الإصدار
                            OutlinedTextField(
                                value = "$versionCode",
                                onValueChange = { newVal ->
                                    versionCode = newVal.filter { it.isDigit() }.toIntOrNull() ?: versionCode
                                },
                                label = { Text("رمز الإصدار", fontFamily = TajawalFontFamily, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color(0xFF262C40),
                                    focusedLabelColor = OrangePrimary,
                                    unfocusedLabelColor = Color(0xFF7A869A),
                                    cursorColor = OrangePrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Right Box in RTL: اسم الإصدار
                            OutlinedTextField(
                                value = versionName,
                                onValueChange = { versionName = it.trim() },
                                label = { Text("اسم الإصدار", fontFamily = TajawalFontFamily, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OrangePrimary,
                                    unfocusedBorderColor = Color(0xFF262C40),
                                    focusedLabelColor = OrangePrimary,
                                    unfocusedLabelColor = Color(0xFF7A869A),
                                    cursorColor = OrangePrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Advanced Feature 1: أدوات المعرف الذكي وفاحص Google Play
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isPkgValid = packageName.trim().matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$"))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPkgValid) Icons.Default.CheckCircle else Icons.Default.Info,
                                            contentDescription = null,
                                            tint = if (isPkgValid) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isPkgValid) "صالح لمتجر Google Play ✓" else "صيغة تحتاج تصحيح",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            color = if (isPkgValid) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    }

                                    Text(
                                        text = "أدوات التوليد الذكي للمعرف",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Prefix chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("com.", "app.", "io.", "net.", "org.").forEach { prefix ->
                                        val isCurrent = packageName.startsWith(prefix)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isCurrent) OrangePrimary.copy(alpha = 0.2f) else Color(0xFF1E2235))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isCurrent) OrangePrimary else GlassBorderSubtle,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    val parts = packageName.split(".").filter { it.isNotBlank() }
                                                    val suffix = if (parts.size > 1) parts.drop(1).joinToString(".") else "app"
                                                    packageName = "$prefix$suffix"
                                                }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = prefix,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = if (isCurrent) OrangePrimary else TextSecondary
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Auto-generate from app name button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E2235))
                                        .clickable {
                                            val cleanBase = if (appName.isNotBlank()) {
                                                appName.lowercase()
                                                    .replace(" ", "")
                                                    .filter { it in 'a'..'z' || it in '0'..'9' }
                                                    .ifBlank { "mycompany" }
                                            } else {
                                                "mycompany"
                                            }
                                            packageName = "com.$cleanBase.app"
                                            Toast.makeText(context, "تم توليد المعرّف: com.$cleanBase.app", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 7.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "توليد معرّف تلقائي من اسم التطبيق ⚡",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OrangePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Advanced Feature 2: أدوات ترقية الإصدار السريعة
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الرمز الحالي: $versionCode | الاسم: $versionName",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "أدوات ترقية الإصدار السريعة",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // +1 رمز الإصدار
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E2235))
                                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                                            .clickable {
                                                versionCode += 1
                                                Toast.makeText(context, "تمت زيادة رمز الإصدار إلى $versionCode", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+1 رمز الإصدار", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                    }

                                    // Patch 1.0.1
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E2235))
                                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                                            .clickable {
                                                val parts = versionName.split(".")
                                                val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
                                                val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                                val patch = (parts.getOrNull(2)?.toIntOrNull() ?: 0) + 1
                                                versionName = "$major.$minor.$patch"
                                                versionCode += 1
                                                Toast.makeText(context, "تم تحديث الإصدار إلى $versionName", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Patch (+0.0.1)", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                    }

                                    // Minor 1.1.0
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF1E2235))
                                            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(8.dp))
                                            .clickable {
                                                val parts = versionName.split(".")
                                                val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
                                                val minor = (parts.getOrNull(1)?.toIntOrNull() ?: 0) + 1
                                                versionName = "$major.$minor.0"
                                                versionCode += 1
                                                Toast.makeText(context, "تم تحديث الإصدار إلى $versionName", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Minor (+0.1.0)", fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Advanced Feature 3: بطاقة أندرويد وتوافق SDK
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF131622))
                                .border(1.dp, GlassBorderSubtle, RoundedCornerShape(16.dp))
                        ) {
                            Column {
                                // Row 1: الحد الأدنى للنظام (minSdk)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showMinSdkDialog = true }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "API $minSdk",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = OrangePrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "الحد الأدنى لنظام التشغيل (minSdk)",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = when (minSdk) {
                                                    24 -> "Android 7.0 Nougat (يدعم 98.2% من الأجهزة)"
                                                    26 -> "Android 8.0 Oreo (يدعم 95.8% من الأجهزة)"
                                                    29 -> "Android 10 (يدعم 91.4% من الأجهزة)"
                                                    else -> "Android API $minSdk"
                                                },
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.PhoneAndroid,
                                            contentDescription = null,
                                            tint = Color(0xFF7A869A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                // Row 2: النظام المستهدف (targetSdk)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "API 34 (Android 14)",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "النظام المستهدف (targetSdk)",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "متوافق مع أحدث اشتراطات متجر Google Play",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = GlassBorderSubtle.copy(alpha = 0.5f))

                                // Row 3: صيغة بناء التطبيق
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("APK & AAB", "AAB", "APK").forEach { format ->
                                            val isSel = buildFormat == format
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSel) OrangePrimary.copy(alpha = 0.2f) else Color(0xFF1A1F30))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSel) OrangePrimary else GlassBorderSubtle,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { buildFormat = format }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = format,
                                                    fontFamily = CairoFontFamily,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) OrangePrimary else TextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "صيغة الملف الناتج",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 8: صلاحيات الجهاز (02:39 - 02:43) - مطابقة للصورة بدقة مع 5 صلاحيات جديدة وميزتين
                8 -> {
                    item {
                        StepTitleHeader(title = "صلاحيات الجهاز", subtitle = "فقط ما يستخدمه محتواك فعلاً")
                    }

                    // ميزة 1: الفحص الذكي التلقائي للصلاحيات من الكود
                    item {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Surface(
                                color = Color(0xFF131B2A),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (autoScanCompleted) Color(0xFF10B981) else Color(0xFF26354A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(if (autoScanCompleted) Color(0xFF10B981).copy(alpha = 0.2f) else OrangePrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (autoScanCompleted) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = if (autoScanCompleted) Color(0xFF10B981) else OrangePrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "الفحص التلقائي الذكي للصلاحيات",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = if (autoScanCompleted) "تم فحص الكود واكتشاف $autoDetectedCount أذونات موصى بها" else "فحص أكواد HTML/JS واقتراح الأذونات اللازمة",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            isAutoScanningPermissions = true
                                            val lowerSource = sourceValue.lowercase()
                                            val detectCamera = lowerSource.contains("cam") || lowerSource.contains("photo") || lowerSource.contains("upload") || sourceType == SourceType.ZIP_ARCHIVE
                                            val detectLocation = lowerSource.contains("map") || lowerSource.contains("loc") || lowerSource.contains("gps") || lowerSource.contains("delivery")
                                            val detectMic = lowerSource.contains("voice") || lowerSource.contains("audio") || lowerSource.contains("call") || lowerSource.contains("chat")
                                            val detectNotifications = pushEnabled || lowerSource.contains("notify") || lowerSource.contains("push")
                                            val detectStorage = sourceType == SourceType.ZIP_ARCHIVE || sourceType == SourceType.HTML_FOLDER || lowerSource.contains("pdf") || lowerSource.contains("file")
                                            val detectBluetooth = lowerSource.contains("pos") || lowerSource.contains("print") || lowerSource.contains("ble")
                                            val detectVibration = true
                                            val detectNetwork = sourceType == SourceType.WEB_URL || sourceType == SourceType.ZIP_ARCHIVE || sourceType == SourceType.HTML_FOLDER

                                            permCamera = permCamera || detectCamera
                                            permLocation = permLocation || detectLocation
                                            permMicrophone = permMicrophone || detectMic
                                            permNotifications = permNotifications || detectNotifications
                                            permStorage = permStorage || detectStorage
                                            permVibration = permVibration || detectVibration
                                            permBluetooth = permBluetooth || detectBluetooth
                                            permNetworkState = permNetworkState || detectNetwork

                                            autoDetectedCount = listOf(
                                                permCamera, permLocation, permMicrophone,
                                                permNotifications, permStorage, permVibration,
                                                permBluetooth, permNetworkState
                                            ).count { it }
                                            autoScanCompleted = true
                                            isAutoScanningPermissions = false
                                            Toast.makeText(context, "تم فحص المشروع واكتشاف $autoDetectedCount صلاحيات متوافقة ✓", Toast.LENGTH_SHORT).show()
                                        },
                                        enabled = !isAutoScanningPermissions,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (autoScanCompleted) Color(0xFF10B981) else OrangePrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (autoScanCompleted) "تم الفحص ✓" else "فحص الكود ⚡",
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

                    // بطاقة الصلاحيات الموحدة المطابقة للصورة تماماً مع الصلاحيات الخمس الجديدة
                    item {
                        Surface(
                            color = Color(0xFF131924),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222C3D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 1. الكاميرا (كما في الصورة)
                                ExactPermissionRow(
                                    title = "الكاميرا",
                                    subtitle = "التقاط الصور عبر <input type=\"file\">",
                                    icon = Icons.Default.Image,
                                    checked = permCamera,
                                    onCheckedChange = { permCamera = it },
                                    showDivider = true
                                )

                                // 2. الموقع (كما في الصورة)
                                ExactPermissionRow(
                                    title = "الموقع",
                                    subtitle = "طلبات تحديد الموقع من صفحاتك",
                                    icon = Icons.Default.Public,
                                    checked = permLocation,
                                    onCheckedChange = { permLocation = it },
                                    showDivider = true
                                )

                                // 3. الميكروفون (كما في الصورة)
                                ExactPermissionRow(
                                    title = "الميكروفون",
                                    subtitle = "مكالمات صوتية ومرئية عبر getUserMedia",
                                    icon = Icons.Default.ChatBubbleOutline,
                                    checked = permMicrophone,
                                    onCheckedChange = { permMicrophone = it },
                                    showDivider = true
                                )

                                // 4. الإشعارات الفورية (جديدة 1)
                                ExactPermissionRow(
                                    title = "الإشعارات الفورية",
                                    subtitle = "إرسال إشعارات وتحديثات تفاعلية للمستخدمين",
                                    icon = Icons.Default.NotificationsActive,
                                    checked = permNotifications,
                                    onCheckedChange = { permNotifications = it },
                                    showDivider = true
                                )

                                // 5. حفظ وتنزيل الملفات (جديدة 2)
                                ExactPermissionRow(
                                    title = "حفظ وتنزيل الملفات",
                                    subtitle = "تنزيل المستندات والوسائط إلى ذاكرة الهاتف",
                                    icon = Icons.Default.Download,
                                    checked = permStorage,
                                    onCheckedChange = { permStorage = it },
                                    showDivider = true
                                )

                                // 6. الاهتزاز والتفاعل اللمسي (جديدة 3)
                                ExactPermissionRow(
                                    title = "الاهتزاز والتفاعل اللمسي",
                                    subtitle = "ردود فعل اهتزازية عند النقر والأزرار التفاعلية",
                                    icon = Icons.Default.Vibration,
                                    checked = permVibration,
                                    onCheckedChange = { permVibration = it },
                                    showDivider = true
                                )

                                // 7. البلوتوث والأجهزة القريبة (جديدة 4)
                                ExactPermissionRow(
                                    title = "البلوتوث والأجهزة القريبة",
                                    subtitle = "الاتصال بالأجهزة المحيطة والطابعات المحمولة",
                                    icon = Icons.Default.Bluetooth,
                                    checked = permBluetooth,
                                    onCheckedChange = { permBluetooth = it },
                                    showDivider = true
                                )

                                // 8. مراقبة حالة الإنترنت (جديدة 5)
                                ExactPermissionRow(
                                    title = "مراقبة حالة الإنترنت",
                                    subtitle = "كشف انقطاع الشبكة وعرض شاشة دون اتصال (Offline)",
                                    icon = Icons.Default.Wifi,
                                    checked = permNetworkState,
                                    onCheckedChange = { permNetworkState = it },
                                    showDivider = false
                                )
                            }
                        }
                    }

                    // بطاقة التحذير المطابقة تماماً للصورة
                    item {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF261D15))
                                    .border(1.dp, OrangePrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = OrangePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "كل صلاحية تضيفها يجب تبريرها في صفحة المتجر. لا تضف ما لا يستخدمه محتواك.",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // ميزة 2: مولّد مبررات سياسة متجر Google Play
                    item {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Surface(
                                color = Color(0xFF161E2E),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showJustificationDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(OrangePrimary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = OrangePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "📋 مولّد مبررات الصلاحيات لـ Google Play",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "توليد نصوص تبرير قانونية رسمية جاهزة للنسخ لسياسة المتجر",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = OrangePrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "عرض المبررات 📄",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = OrangePrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 9: الإشعارات الفورية (مطابقة للصورة مع ميزات جديدة)
                9 -> {
                    item {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            StepTitleHeader(title = "الإشعارات الفورية", subtitle = "أرسل إشعارات عبر OneSignal")
                        }
                    }

                    // 1. بطاقة التفعيل الرئيسية المطابقة تماماً للصورة (الأيقونة يمين، النص في الوسط، المفتاح يسار)
                    item {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Surface(
                                color = Color(0xFF131924),
                                shape = RoundedCornerShape(18.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222C3D)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { pushEnabled = !pushEnabled }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Notifications,
                                            contentDescription = "الإشعارات الفورية",
                                            tint = if (pushEnabled) Color(0xFF9EABB8) else Color(0xFF707D8F),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "تفعيل الإشعارات الفورية",
                                            fontFamily = CairoFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                    }

                                    Switch(
                                        checked = pushEnabled,
                                        onCheckedChange = { pushEnabled = it },
                                        modifier = Modifier.testTag("push_enable_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF0A0E14),
                                            checkedTrackColor = OrangePrimary,
                                            uncheckedThumbColor = Color(0xFF9EABB8),
                                            uncheckedTrackColor = Color(0xFF1E2836),
                                            uncheckedBorderColor = Color(0xFF2E3A4B)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // 2. حقل إدخال معرّف تطبيق OneSignal والنص التوضيحي المطابق تماماً للصورة
                    item {
                        val uuidRegex = remember {
                            Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
                        }
                        val isValidUuid = pushAppId.isNotBlank() && uuidRegex.matches(pushAppId.trim())

                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = pushAppId,
                                    onValueChange = { pushAppId = it },
                                    placeholder = {
                                        Text(
                                            text = "معرّف تطبيق $pushProvider",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 14.sp,
                                            color = Color(0xFF7A869A)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.VpnKey,
                                            contentDescription = null,
                                            tint = if (isValidUuid) Color(0xFF10B981) else Color(0xFF8B9BB4),
                                            modifier = Modifier.size(21.dp)
                                        )
                                    },
                                    trailingIcon = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(end = 4.dp)
                                        ) {
                                            if (pushAppId.isNotBlank()) {
                                                Icon(
                                                    imageVector = if (isValidUuid) Icons.Default.CheckCircle else Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = if (isValidUuid) Color(0xFF10B981) else OrangePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                                    if (!clipText.isNullOrBlank()) {
                                                        pushAppId = clipText
                                                        Toast.makeText(context, "تم لصق المعرّف ✓", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        pushAppId = "a4f8c92e-7b13-4d9a-9e21-8c3f5b10294a"
                                                        Toast.makeText(context, "تم إدراج معرّف تجريبي للمعاينة ⚡", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentPaste,
                                                    contentDescription = "لصق المعرّف",
                                                    tint = OrangePrimary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0E1218),
                                        unfocusedContainerColor = Color(0xFF0E1218),
                                        focusedBorderColor = if (isValidUuid) Color(0xFF10B981) else OrangePrimary,
                                        unfocusedBorderColor = Color(0xFF222C3D),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = OrangePrimary
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onesignal_app_id_input")
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "أنشئ تطبيق OneSignal مجانيًا ثم الصق معرّفه هنا. سيطلب تطبيقك إذن الإشعارات عند أول تشغيل.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    lineHeight = 19.sp
                                )

                                // شريط حالة التحقق من المعرّف عند الإدخال
                                AnimatedVisibility(visible = pushAppId.isNotBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isValidUuid) Icons.Default.Verified else Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = if (isValidUuid) Color(0xFF10B981) else OrangePrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isValidUuid) "تنسيق UUID صالح وجاهز للربط مع $pushProvider ✓" else "يُفضّل إدخال معرّف UUID قياسي (مثال: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 11.sp,
                                            color = if (isValidUuid) Color(0xFF10B981) else OrangePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ميزة جديدة 1: إعدادات القناة المتقدمة ومزود الإشعارات الذكي
                    item {
                        AnimatedVisibility(visible = pushEnabled) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Surface(
                                    color = Color(0xFF131924),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222C3D)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                tint = OrangePrimary,
                                                modifier = Modifier.size(19.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "تخصيص قناة وسلوك الإشعارات",
                                                fontFamily = CairoFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = TextPrimary
                                            )
                                        }

                                        // اختيار مزود الخدمة
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listOf("OneSignal", "Firebase FCM", "مزدوج").forEach { provider ->
                                                val selected = pushProvider == provider
                                                Surface(
                                                    color = if (selected) OrangePrimary.copy(alpha = 0.16f) else Color(0xFF0E1218),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        if (selected) OrangePrimary else Color(0xFF222C3D)
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { pushProvider = provider }
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = provider,
                                                            fontFamily = CairoFontFamily,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 11.sp,
                                                            color = if (selected) OrangePrimary else TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFF1E2633))

                                        // خيارات القناة السريعة
                                        PushSubOptionRow(
                                            title = "صوت التنبيه الافتراضي",
                                            subtitle = "تشغيل نغمة النظام عند وصول إشعار",
                                            checked = pushSoundEnabled,
                                            onCheckedChange = { pushSoundEnabled = it }
                                        )
                                        PushSubOptionRow(
                                            title = "الاهتزاز مع الإشعار",
                                            subtitle = "تضمين إذن VIBRATE لتنبيه ملموس",
                                            checked = pushVibrationEnabled,
                                            onCheckedChange = { pushVibrationEnabled = it }
                                        )
                                        PushSubOptionRow(
                                            title = "شارة العداد على الأيقونة (Badge)",
                                            subtitle = "إظهار عدد التنبيهات غير المقروءة",
                                            checked = pushBadgeEnabled,
                                            onCheckedChange = { pushBadgeEnabled = it }
                                        )
                                        PushSubOptionRow(
                                            title = "فتح الروابط العميقة (Deep Link)",
                                            subtitle = "توجيه المستخدم للرابط المرفق داخل التطبيق",
                                            checked = pushOpenUrlOnTap,
                                            onCheckedChange = { pushOpenUrlOnTap = it }
                                        )
                                        PushSubOptionRow(
                                            title = "طلب الإذن تلقائياً عند التشغيل",
                                            subtitle = "متوافق مع أندرويد 13+ (POST_NOTIFICATIONS)",
                                            checked = pushAutoPrompt,
                                            onCheckedChange = { pushAutoPrompt = it }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ميزة جديدة 2: استوديو معاينة وتجربة الإشعار الفوري المباشر
                    item {
                        AnimatedVisibility(visible = pushEnabled) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Surface(
                                    color = Color(0xFF131B2A),
                                    shape = RoundedCornerShape(16.dp),
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
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = OrangePrimary,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "معاينة وتجربة الإشعار الحي",
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                            }
                                            Surface(
                                                color = OrangePrimary.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "تجربة فورية",
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    color = OrangePrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        // قوالب سريعة للإشعار
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val presets = listOf(
                                                Triple("ترحيب 👋", "مرحباً بك في ${appName.ifBlank { "تطبيقنا" }}!", "تم تفعيل الإشعارات الفورية بنجاح."),
                                                Triple("عرض خاص 🔥", "خصم 25% لفترة محدودة", "تصفح العروض الحصرية داخل التطبيق الآن."),
                                                Triple("تحديث 🚀", "تتوفر محتويات جديدة", "اضغط هنا لاستعراض أحدث الإضافات.")
                                            )
                                            presets.forEach { (chipLabel, pTitle, pBody) ->
                                                Surface(
                                                    color = Color(0xFF0E1218),
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222C3D)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            pushTestTitle = pTitle
                                                            pushTestBody = pBody
                                                        }
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 7.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = chipLabel,
                                                            fontFamily = CairoFontFamily,
                                                            fontSize = 11.sp,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // حقول تخصيص رسالة الاختبار
                                        OutlinedTextField(
                                            value = pushTestTitle,
                                            onValueChange = { pushTestTitle = it },
                                            label = { Text("عنوان الإشعار", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF0E1218),
                                                unfocusedContainerColor = Color(0xFF0E1218),
                                                focusedBorderColor = OrangePrimary,
                                                unfocusedBorderColor = Color(0xFF222C3D),
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = pushTestBody,
                                            onValueChange = { pushTestBody = it },
                                            label = { Text("نص الإشعار", fontFamily = TajawalFontFamily, fontSize = 11.sp) },
                                            maxLines = 2,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF0E1218),
                                                unfocusedContainerColor = Color(0xFF0E1218),
                                                focusedBorderColor = OrangePrimary,
                                                unfocusedBorderColor = Color(0xFF222C3D),
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // بطاقة معاينة شكل إشعار أندرويد الحقيقي
                                        Surface(
                                            color = Color(0xFF1C2333),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C364B)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(parseColorSafe(accentColorHex, OrangePrimary)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.NotificationsActive,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${appName.ifBlank { "تطبيقك" }} • $pushProvider",
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 10.sp,
                                                            color = TextSecondary
                                                        )
                                                        Text(
                                                            text = "الآن",
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 10.sp,
                                                            color = Color(0xFF7A869A)
                                                        )
                                                    }
                                                    Text(
                                                        text = pushTestTitle.ifBlank { "عنوان الإشعار" },
                                                        fontFamily = CairoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = pushTestBody.ifBlank { "نص الإشعار التجريبي سيظهر هنا" },
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 11.sp,
                                                        color = TextSecondary,
                                                        lineHeight = 16.sp
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                showLivePushBanner = true
                                                Toast.makeText(context, "تم إرسال إشعار المعاينة بنجاح 🔔", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("test_live_push_btn")
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "إرسال معاينة إشعار حي الآن 🔔",
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
                    }

                    if (onOpenInteractivePhone != null) {
                        item {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Surface(
                                    color = Color(0xFF1E172E),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, parseColorSafe(accentColorHex, OrangePrimary)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val currentProj = (initialProject ?: AppProject(id = "preview_proj", name = appName)).copy(
                                                name = appName.ifBlank { initialProject?.name ?: "تطبيق جديد" },
                                                sourceType = sourceType,
                                                sourceValue = sourceValue,
                                                localWorkspacePath = localWorkspacePath,
                                                accentColorHex = accentColorHex,
                                                primaryColorHex = accentColorHex,
                                                iconImageUri = selectedIconUri,
                                                splashConfig = SplashConfig(
                                                    enabled = true,
                                                    backgroundColorHex = splashBgColorHex,
                                                    fadeBackground = splashFadeBg,
                                                    indicatorStyle = splashIndicatorStyle,
                                                    iconSizeDp = splashIconSize,
                                                    iconCornerRadiusDp = splashIconCorner,
                                                    indicatorSizeDp = splashIndicatorSize,
                                                    spacingDp = splashSpacing
                                                ),
                                                onboardingConfig = OnboardingConfig(
                                                    enabled = onboardingEnabled,
                                                    slides = onboardingSlides,
                                                    layoutType = onboardingLayoutType,
                                                    imageShape = onboardingImageShape,
                                                    gradientBackground = onboardingGradientBg,
                                                    accentColorHex = onboardingAccentColorHex,
                                                    textColorHex = onboardingTextColorHex,
                                                    cardColorHex = onboardingCardColorHex,
                                                    showSkipButton = onboardingShowSkipButton
                                                ),
                                                webViewConfig = WebViewConfig(
                                                    fullScreen = fullScreen,
                                                    pullToRefresh = pullToRefresh,
                                                    pageLoader = PageLoaderConfig(
                                                        enabled = pageLoaderEnabled,
                                                        style = pageLoaderStyle,
                                                        sizeDp = pageLoaderSize,
                                                        thicknessDp = pageLoaderThickness,
                                                        showOnCard = pageLoaderOnCard,
                                                        cardColorHex = pageLoaderCardColorHex,
                                                        cardOpacityPercent = pageLoaderCardOpacity,
                                                        cardCornerRadiusDp = pageLoaderCardCornerRadius
                                                    ),
                                                    supportZoom = supportZoom,
                                                    cacheEnabled = true,
                                                    fileDownload = true,
                                                    offlineFallback = offlineFallback
                                                ),
                                                bottomNavConfig = BottomNavConfig(
                                                    enabled = bottomNavEnabled,
                                                    destinations = bottomDestinations,
                                                    barStyle = bottomNavBarStyle,
                                                    showLabels = bottomNavShowLabels,
                                                    activeColorHex = bottomNavActiveColorHex,
                                                    inactiveColorHex = bottomNavInactiveColorHex,
                                                    barColorHex = bottomNavBarColorHex
                                                ),
                                                packageConfig = PackageConfig(packageName = packageName, versionName = versionName, versionCode = versionCode),
                                                permissionsConfig = PermissionsConfig(
                                                    camera = permCamera,
                                                    location = permLocation,
                                                    microphone = permMicrophone,
                                                    notifications = permNotifications,
                                                    storage = permStorage,
                                                    vibration = permVibration,
                                                    bluetooth = permBluetooth,
                                                    networkState = permNetworkState
                                                ),
                                                pushConfig = PushNotificationConfig(
                                                    enabled = pushEnabled,
                                                    provider = pushProvider,
                                                    appId = pushAppId.trim(),
                                                    channelName = pushChannelName,
                                                    soundEnabled = pushSoundEnabled,
                                                    vibrationEnabled = pushVibrationEnabled,
                                                    badgeEnabled = pushBadgeEnabled,
                                                    openUrlOnTap = pushOpenUrlOnTap,
                                                    autoPromptOnFirstLaunch = pushAutoPrompt,
                                                    testTitle = pushTestTitle,
                                                    testBody = pushTestBody
                                                ),
                                                bridgeConfig = initialProject?.bridgeConfig ?: BridgeAndCloudConfig(),
                                                proConfig = (initialProject?.proConfig ?: ProStudioConfig()).copy(
                                                    iconShape = iconMaskShape,
                                                    iconBgColorHex = iconBgColorHex
                                                ),
                                                buildHistory = initialProject?.buildHistory ?: emptyList()
                                            )
                                            onOpenInteractivePhone(currentProj)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(46.dp)
                                                    .clip(CircleShape)
                                                    .background(parseColorSafe(accentColorHex, OrangePrimary).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhoneAndroid,
                                                    contentDescription = null,
                                                    tint = parseColorSafe(accentColorHex, OrangePrimary),
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "📱 تجربة في الهاتف الافتراضي المتطور",
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "المس التطبيق، جرب التنقل السفلي والإشعارات حياً",
                                                    fontFamily = TajawalFontFamily,
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronLeft,
                                            contentDescription = null,
                                            tint = parseColorSafe(accentColorHex, OrangePrimary)
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

    // شريط الإشعار الحي المنبثق عند تجربة الإشعار من الخطوة 9
    if (showLivePushBanner) {
        LaunchedEffect(showLivePushBanner) {
            kotlinx.coroutines.delay(3500)
            showLivePushBanner = false
        }
        Dialog(
            onDismissRequest = { showLivePushBanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showLivePushBanner = false }
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        color = Color(0xFF1C2333),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, OrangePrimary),
                        shadowElevation = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(parseColorSafe(accentColorHex, OrangePrimary)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${appName.ifBlank { "ApkForge App" }} • $pushProvider • الآن",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 10.sp,
                                    color = OrangePrimary
                                )
                                Text(
                                    text = pushTestTitle.ifBlank { "مرحباً بك!" },
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = pushTestBody.ifBlank { "تم إرسال الإشعار التجريبي بنجاح." },
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = { showLivePushBanner = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Color Picker Dialog (matching Screenshot 2 with HSV sliders, Hex input, and palette)
    if (showColorPickerDialog) {
        ModernColorPickerDialog(
            title = "لون التمييز",
            initialHex = accentColorHex,
            onDismiss = { showColorPickerDialog = false },
            onColorSelected = { newHex ->
                accentColorHex = newHex
            }
        )
    }

    // Splash Background Color Picker Dialog (Matching Screenshot 2 exactly)
    if (showSplashColorPickerDialog) {
        ModernColorPickerDialog(
            title = "خلفية شاشة البداية",
            initialHex = splashBgColorHex,
            onDismiss = { showSplashColorPickerDialog = false },
            onColorSelected = { newHex ->
                splashBgColorHex = newHex
            }
        )
    }

    // Indicator Style Dialog (matching video at 01:54 & Screenshot 1)
    if (showIndicatorDialog) {
        val styles = listOf("بدون", "حلقة", "شريط", "حلقة - L", "حلقة - S", "نبض")
        Dialog(
            onDismissRequest = { showIndicatorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E212E))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "نمط المؤشر",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    styles.forEach { style ->
                        val isSel = splashIndicatorStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    splashIndicatorStyle = style
                                    showIndicatorDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    splashIndicatorStyle = style
                                    showIndicatorDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = style,
                                fontFamily = CairoFontFamily,
                                fontSize = 14.sp,
                                color = if (isSel) OrangePrimary else TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showIndicatorDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "إلغاء", color = OrangePrimary, fontFamily = CairoFontFamily)
                    }
                }
            }
        }
    }

    // Onboarding Color Pickers
    if (showOnboardingAccentColorPicker) {
        ModernColorPickerDialog(
            title = "التمييز",
            initialHex = onboardingAccentColorHex,
            onDismiss = { showOnboardingAccentColorPicker = false },
            onColorSelected = { newHex ->
                onboardingAccentColorHex = newHex
            }
        )
    }

    if (showOnboardingTextColorPicker) {
        ModernColorPickerDialog(
            title = "لون النص",
            initialHex = onboardingTextColorHex,
            onDismiss = { showOnboardingTextColorPicker = false },
            onColorSelected = { newHex ->
                onboardingTextColorHex = newHex
            }
        )
    }

    if (showOnboardingCardColorPicker) {
        ModernColorPickerDialog(
            title = "لون البطاقة",
            initialHex = onboardingCardColorHex,
            onDismiss = { showOnboardingCardColorPicker = false },
            onColorSelected = { newHex ->
                onboardingCardColorHex = newHex
            }
        )
    }

    // Layout Type Dialog
    if (showLayoutTypeDialog) {
        val layouts = listOf("بطاقة سفلية", "كامل الشاشة", "بطاقة وسطية")
        Dialog(
            onDismissRequest = { showLayoutTypeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E212E))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "نمط التخطيط",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    layouts.forEach { layout ->
                        val isSel = onboardingLayoutType == layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onboardingLayoutType = layout
                                    showLayoutTypeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    onboardingLayoutType = layout
                                    showLayoutTypeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = layout,
                                fontFamily = CairoFontFamily,
                                fontSize = 14.sp,
                                color = if (isSel) OrangePrimary else TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showLayoutTypeDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "إلغاء", color = OrangePrimary, fontFamily = CairoFontFamily)
                    }
                }
            }
        }
    }

    // Image Shape Dialog
    if (showImageShapeDialog) {
        val shapes = listOf("دائرة", "مربع منحني", "كبسولة")
        Dialog(
            onDismissRequest = { showImageShapeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E212E))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "شكل الصورة",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    shapes.forEach { shape ->
                        val isSel = onboardingImageShape == shape
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onboardingImageShape = shape
                                    showImageShapeDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    onboardingImageShape = shape
                                    showImageShapeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = shape,
                                fontFamily = CairoFontFamily,
                                fontSize = 14.sp,
                                color = if (isSel) OrangePrimary else TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showImageShapeDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "إلغاء", color = OrangePrimary, fontFamily = CairoFontFamily)
                    }
                }
            }
        }
    }

    // Icon & Image Selector Dialog (Advanced Feature 2)
    if (showIconPickerDialogForSlideIndex != null) {
        val targetIndex = showIconPickerDialogForSlideIndex!!
        Dialog(
            onDismissRequest = { showIconPickerDialogForSlideIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1B2030))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "تخصيص أيقونة وصورة الشاشة",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اختر صورة حقيقية من هاتفك أو اختر من الأيقونات العصرية",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            slideImagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اختيار صورة من معرض الهاتف",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "أو اختر أيقونة متجهة",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val iconOptions = listOf(
                        "bolt" to Icons.Default.Bolt,
                        "rocket" to Icons.Default.RocketLaunch,
                        "shield" to Icons.Default.Shield,
                        "star" to Icons.Default.Star,
                        "notification" to Icons.Default.Notifications,
                        "web" to Icons.Default.Language,
                        "diamond" to Icons.Default.Diamond,
                        "design" to Icons.Default.Palette
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        iconOptions.take(4).forEach { (name, icon) ->
                            IconButton(
                                onClick = {
                                    if (targetIndex in onboardingSlides.indices) {
                                        val updated = onboardingSlides.toMutableList()
                                        updated[targetIndex] = updated[targetIndex].copy(iconName = name, imageUri = null)
                                        onboardingSlides = updated
                                    }
                                    showIconPickerDialogForSlideIndex = null
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF242A3E))
                            ) {
                                Icon(icon, contentDescription = name, tint = OrangePrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        iconOptions.drop(4).forEach { (name, icon) ->
                            IconButton(
                                onClick = {
                                    if (targetIndex in onboardingSlides.indices) {
                                        val updated = onboardingSlides.toMutableList()
                                        updated[targetIndex] = updated[targetIndex].copy(iconName = name, imageUri = null)
                                        onboardingSlides = updated
                                    }
                                    showIconPickerDialogForSlideIndex = null
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF242A3E))
                            ) {
                                Icon(icon, contentDescription = name, tint = OrangePrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = { showIconPickerDialogForSlideIndex = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("إلغاء", color = TextSecondary, fontFamily = CairoFontFamily)
                    }
                }
            }
        }
    }

    // Page Loader Card Color Picker Dialog
    if (showPageLoaderCardColorPicker) {
        ModernColorPickerDialog(
            title = "لون البطاقة",
            initialHex = pageLoaderCardColorHex,
            onDismiss = { showPageLoaderCardColorPicker = false },
            onColorSelected = { newHex ->
                pageLoaderCardColorHex = newHex
            }
        )
    }

    // Step 6: Bottom Navigation Bar Style Dialog
    if (showBottomNavBarStyleDialog) {
        val styles = listOf("مسطح", "عائم", "كبسولة")
        Dialog(
            onDismissRequest = { showBottomNavBarStyleDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E212E))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "نمط شريط التنقل السفلي",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    styles.forEach { style ->
                        val isSel = bottomNavBarStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    bottomNavBarStyle = style
                                    showBottomNavBarStyleDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    bottomNavBarStyle = style
                                    showBottomNavBarStyleDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = style,
                                fontFamily = CairoFontFamily,
                                fontSize = 14.sp,
                                color = if (isSel) OrangePrimary else TextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showBottomNavBarStyleDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "إلغاء", color = OrangePrimary, fontFamily = CairoFontFamily)
                    }
                }
            }
        }
    }

    // Step 6: Active Color Picker
    if (showBottomNavActiveColorPicker) {
        ModernColorPickerDialog(
            title = "اللون النشط للتنقل السفلي",
            initialHex = bottomNavActiveColorHex,
            onDismiss = { showBottomNavActiveColorPicker = false },
            onColorSelected = { newHex ->
                bottomNavActiveColorHex = newHex
            }
        )
    }

    // Step 6: Inactive Color Picker
    if (showBottomNavInactiveColorPicker) {
        ModernColorPickerDialog(
            title = "اللون غير النشط للتنقل السفلي",
            initialHex = bottomNavInactiveColorHex,
            onDismiss = { showBottomNavInactiveColorPicker = false },
            onColorSelected = { newHex ->
                bottomNavInactiveColorHex = newHex
            }
        )
    }

    // Step 6: Bar Background Color Picker
    if (showBottomNavBarColorPicker) {
        ModernColorPickerDialog(
            title = "لون شريط التنقل",
            initialHex = bottomNavBarColorHex,
            onDismiss = { showBottomNavBarColorPicker = false },
            onColorSelected = { newHex ->
                bottomNavBarColorHex = newHex
            }
        )
    }

    // Step 6: Destination Icon & Preset Picker Dialog
    val iconPickerTargetIndex = showBottomNavIconPickerDialogForIndex
    if (iconPickerTargetIndex != null && iconPickerTargetIndex in bottomDestinations.indices) {
        val dest = bottomDestinations[iconPickerTargetIndex]
        val iconChoices = listOf(
            "home" to Icons.Default.Home,
            "search" to Icons.Default.Search,
            "shopping_bag" to Icons.Default.ShoppingBag,
            "shopping_cart" to Icons.Default.ShoppingCart,
            "person" to Icons.Default.Person,
            "notifications" to Icons.Default.Notifications,
            "settings" to Icons.Default.Settings,
            "favorite" to Icons.Default.Favorite,
            "explore" to Icons.Default.Explore,
            "dashboard" to Icons.Default.Dashboard,
            "bookmark" to Icons.Default.Bookmark,
            "chat" to Icons.Default.Chat,
            "info" to Icons.Default.Info,
            "article" to Icons.Default.Article,
            "phone" to Icons.Default.Phone,
            "map" to Icons.Default.Place
        )

        val quickPaths = listOf("/", "/shop", "/cart", "/account", "/search", "/settings", "/categories")

        Dialog(
            onDismissRequest = { showBottomNavIconPickerDialogForIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E212E))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تخصيص أيقونة الوجهة",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        IconButton(onClick = { showBottomNavIconPickerDialogForIndex = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اختر الأيقونة المناسبة لـ: ${dest.title.ifBlank { "الوجهة" }}",
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4x4 Grid of Icons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0 until 4) {
                                    val itemIdx = row * 4 + col
                                    if (itemIdx < iconChoices.size) {
                                        val (name, iconVector) = iconChoices[itemIdx]
                                        val isSel = dest.iconName.equals(name, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) OrangePrimary.copy(alpha = 0.2f) else Color(0xFF262C40))
                                                .border(
                                                    width = if (isSel) 1.5.dp else 1.dp,
                                                    color = if (isSel) OrangePrimary else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    val updated = bottomDestinations.toMutableList()
                                                    updated[iconPickerTargetIndex] = dest.copy(iconName = name)
                                                    bottomDestinations = updated
                                                    showBottomNavIconPickerDialogForIndex = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = iconVector,
                                                contentDescription = name,
                                                tint = if (isSel) OrangePrimary else Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick URL path suggestions
                    Text(
                        text = "اقتراحات المسارات السريعة:",
                        fontFamily = TajawalFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPaths.take(4).forEach { path ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF262C40))
                                    .clickable {
                                        val updated = bottomDestinations.toMutableList()
                                        updated[iconPickerTargetIndex] = dest.copy(targetUrl = path)
                                        bottomDestinations = updated
                                        Toast.makeText(context, "تم تحديد المسار: $path", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = path,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = OrangePrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Step 7: Min SDK Selection Dialog
    if (showMinSdkDialog) {
        val sdkOptions = listOf(
            Triple(24, "Android 7.0 Nougat", "98.2% من الأجهزة"),
            Triple(26, "Android 8.0 Oreo (موصى به)", "95.8% من الأجهزة"),
            Triple(28, "Android 9.0 Pie", "93.1% من الأجهزة"),
            Triple(29, "Android 10", "91.4% من الأجهزة"),
            Triple(31, "Android 12", "82.0% من الأجهزة"),
            Triple(33, "Android 13", "74.5% من الأجهزة")
        )

        Dialog(
            onDismissRequest = { showMinSdkDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E212E))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "الحد الأدنى لنظام أندرويد (minSdk)",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    sdkOptions.forEach { (api, name, coverage) ->
                        val isSel = minSdk == api
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    minSdk = api
                                    showMinSdkDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = coverage,
                                fontFamily = TajawalFontFamily,
                                fontSize = 11.sp,
                                color = if (isSel) OrangePrimary else TextSecondary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$name (API $api)",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 13.sp,
                                    color = if (isSel) OrangePrimary else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                RadioButton(
                                    selected = isSel,
                                    onClick = {
                                        minSdk = api
                                        showMinSdkDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showMinSdkDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(text = "إلغاء", color = OrangePrimary, fontFamily = CairoFontFamily)
                    }
                }
            }
        }
    }

    // Feature 2: Play Store Permissions Justification Dialog
    if (showJustificationDialog) {
        val activePerms = mutableListOf<Pair<String, String>>()
        if (permCamera) activePerms.add("الكاميرا (CAMERA)" to "يستخدم التطبيق الكاميرا للسماح للمستخدم بالتقاط الصور وتصوير المستندات ورفعها عبر نماذج الويب.\n• EN: The app requests CAMERA permission strictly to enable users to capture photos and scan documents directly within web upload forms.")
        if (permLocation) activePerms.add("الموقع الجغرافي (LOCATION)" to "يطلب التطبيق إذن الموقع عند قيام المستخدم بالبحث الجغرافي أو تصفح الخرائط والخدمات القريبة.\n• EN: ACCESS_FINE_LOCATION is accessed only when the user explicitly triggers location-based services or maps inside the web interface.")
        if (permMicrophone) activePerms.add("الميكروفون (RECORD_AUDIO)" to "يُطلب الميكروفون لتسجيل الرسائل الصوتية وإجراء المكالمات التفاعلية عبر بروتوكول WebRTC.\n• EN: RECORD_AUDIO is utilized solely for voice messaging and audio/video calls via WebRTC within the application.")
        if (permNotifications) activePerms.add("الإشعارات (POST_NOTIFICATIONS)" to "يُطلب لإرسال تنبيهات المتابعة والرسائل الهامة والتحديثات الفورية للمستخدم.\n• EN: POST_NOTIFICATIONS is required to deliver real-time order updates, critical announcements, and app alerts.")
        if (permStorage) activePerms.add("التخزين والملفات (STORAGE)" to "يستخدم لحفظ الملفات وتنزيل التقارير والمستندات إلى مجلد التنزيلات بالجهاز.\n• EN: READ_EXTERNAL_STORAGE is used exclusively to allow users to select local attachments and download generated reports.")
        if (permVibration) activePerms.add("الاهتزاز (VIBRATE)" to "لتقديم ردود فعل لمسية تعزز تجربة الاستخدام عند الضغط على الأزرار والأحداث الهامة.\n• EN: VIBRATE permission enhances UX through subtle haptic feedback during interactive button taps.")
        if (permBluetooth) activePerms.add("البلوتوث (BLUETOOTH)" to "للاتصال بالطابعات المحمولة أو الملحقات الذكية المتوافقة مع الويب.\n• EN: BLUETOOTH is used to connect with thermal receipt printers or smart accessories supported by the web app.")
        if (permNetworkState) activePerms.add("حالة الشبكة (ACCESS_NETWORK_STATE)" to "للتحقق من الاتصال بالإنترنت وتوفير واجهة بديلة تعمل دون اتصال عند انقطاع الشبكة.\n• EN: ACCESS_NETWORK_STATE monitors connectivity to seamlessly switch to offline cached content when disconnected.")

        Dialog(
            onDismissRequest = { showJustificationDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF19202E))
                    .border(1.dp, GlassBorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نصوص تبرير الصلاحيات لـ Google Play",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        IconButton(onClick = { showJustificationDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activePerms.isEmpty()) {
                        Text(
                            text = "لم يتم تفعيل أي صلاحيات خاصة بعد. عند تفعيل أي صلاحية، ستظهر هنا نصوص التبرير القانونية المعتمدة لنسخها في Google Play Console.",
                            fontFamily = TajawalFontFamily,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .heightIn(max = 340.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(activePerms.size) { index ->
                                val (title, justification) = activePerms[index]
                                Surface(
                                    color = Color(0xFF101520),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222C3D)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OrangePrimary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = justification, fontFamily = TajawalFontFamily, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val fullText = activePerms.joinToString("\n\n") { "【${it.first}】\n${it.second}" }
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Permissions Justifications", fullText))
                                Toast.makeText(context, "تم نسخ جميع مبررات الصلاحيات للحافظة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                showJustificationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "نسخ جميع المبررات للحافظة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExactPermissionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on the right (start in RTL - matches user screenshot)
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (checked) OrangePrimary else Color(0xFF8B9BB4),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Text in the middle (aligned right in RTL)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Switch on the left (end in RTL - matches user screenshot)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = OrangePrimary,
                    uncheckedThumbColor = Color(0xFF9EABB8),
                    uncheckedTrackColor = Color(0xFF1E2836),
                    uncheckedBorderColor = Color(0xFF2E3A4B)
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFF1E2633),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun PushSubOptionRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontFamily = TajawalFontFamily,
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF0A0E14),
                checkedTrackColor = OrangePrimary,
                uncheckedThumbColor = Color(0xFF9EABB8),
                uncheckedTrackColor = Color(0xFF1E2836),
                uncheckedBorderColor = Color(0xFF2E3A4B)
            )
        )
    }
}

private fun getBottomNavIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "home", "رئيسية", "الرئيسية" -> Icons.Default.Home
        "search", "بحث", "البحث" -> Icons.Default.Search
        "shopping_bag", "shop", "متجر", "المتجر" -> Icons.Default.ShoppingBag
        "shopping_cart", "cart", "سلة", "السلة" -> Icons.Default.ShoppingCart
        "person", "account", "profile", "حساب", "حسابي" -> Icons.Default.Person
        "notifications", "تنبيهات", "إشعارات" -> Icons.Default.Notifications
        "settings", "إعدادات", "الإعدادات" -> Icons.Default.Settings
        "favorite", "heart", "مفضلة", "المفضلة" -> Icons.Default.Favorite
        "explore", "استكشاف", "بوصلة" -> Icons.Default.Explore
        "dashboard", "لوحة", "تحكم" -> Icons.Default.Dashboard
        "bookmark", "إشارات", "محفوظات" -> Icons.Default.Bookmark
        "chat", "رسائل", "محادثة" -> Icons.Default.Chat
        "info", "معلومات", "حول" -> Icons.Default.Info
        "article", "مقالات", "أخبار" -> Icons.Default.Article
        "phone", "اتصال", "تواصل" -> Icons.Default.Phone
        "map", "خريطة", "موقع" -> Icons.Default.Place
        else -> Icons.Default.Home
    }
}

@Composable
private fun StepTitleHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, fontFamily = TajawalFontFamily, fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun FeatureSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontFamily = TajawalFontFamily, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = OrangePrimary)
        )
    }
}

@Composable
private fun SliderWithLabel(
    label: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextSecondary)
            Text(text = "${value}dp", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = OrangePrimary)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = OrangePrimary,
                activeTrackColor = OrangePrimary
            )
        )
    }
}

@Composable
private fun DottedSliderWithLabel(
    label: String,
    value: Int,
    range: IntRange,
    accentColor: Color = OrangePrimary,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${value}dp",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = accentColor
            )
            Text(
                text = label,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color(0xFFD0D5DD)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        var trackWidthPx by remember { mutableFloatStateOf(0f) }
        val normalizedFraction = ((value - range.first).toFloat() / (range.last - range.first).toFloat()).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .pointerInput(range) {
                    detectTapGestures { offset ->
                        if (trackWidthPx > 0) {
                            val frac = (offset.x / trackWidthPx).coerceIn(0f, 1f)
                            val newVal = range.first + ((range.last - range.first) * frac).toInt()
                            onValueChange(newVal.coerceIn(range.first, range.last))
                        }
                    }
                }
                .pointerInput(range) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        if (trackWidthPx > 0) {
                            val frac = (change.position.x / trackWidthPx).coerceIn(0f, 1f)
                            val newVal = range.first + ((range.last - range.first) * frac).toInt()
                            onValueChange(newVal.coerceIn(range.first, range.last))
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Dotted capsule track matching Screenshot 3
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF261B16))
                    .border(1.dp, Color(0xFF4A3428).copy(alpha = 0.5f), RoundedCornerShape(7.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = 28
                    for (i in 0 until dotCount) {
                        Box(
                            modifier = Modifier
                                .size(3.5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF7A2F))
                        )
                    }
                }
            }

            // Vertical orange thumb bar (matching Screenshot 3: vertical bar with rounded edges)
            val thumbOffsetDp = if (trackWidthPx > 0) {
                val paddingOffset = 4f
                val usableWidth = (trackWidthPx - 16f).coerceAtLeast(0f)
                ((paddingOffset + usableWidth * normalizedFraction) / (LocalContext.current.resources.displayMetrics.density)).dp
            } else {
                (normalizedFraction * 260).dp
            }

            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetDp - 3.dp)
                    .width(6.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accentColor)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
            )
        }
    }
}

