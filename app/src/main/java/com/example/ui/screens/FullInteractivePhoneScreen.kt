package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.AppProject
import com.example.ui.components.ConsoleLevel
import com.example.ui.components.DevConsoleEntry
import com.example.ui.components.DevToolsConsoleDrawer
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.GlowingButton
import com.example.ui.components.LiveWebViewContainer
import com.example.ui.components.nowTimeLabel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FullInteractivePhoneScreen(
    project: AppProject,
    onBack: () -> Unit,
    onBuild: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val accentColor = remember(project.accentColorHex) {
        parseColorSafe(project.accentColorHex, OrangePrimary)
    }

    // Phone state
    var isSplashPlaying by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionType by remember { mutableStateOf("الكاميرا") }
    var permissionGranted by remember { mutableStateOf(false) }
    var showPushBanner by remember { mutableStateOf(false) }
    var isHomeScreenMode by remember { mutableStateOf(false) }
    var interactiveCounter by remember { mutableIntStateOf(1) }

    // Real WebView + DevTools + Offline Simulator state
    var isLiveWebViewMode by remember { mutableStateOf(false) }
    var isOfflineSimulated by remember { mutableStateOf(false) }
    var showDevToolsDrawer by remember { mutableStateOf(false) }
    var webReloadTrigger by remember { mutableIntStateOf(0) }
    var jsCommandTrigger by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val consoleLogs = remember { mutableStateListOf<DevConsoleEntry>() }

    fun appendConsoleLog(level: ConsoleLevel, msg: String) {
        consoleLogs.add(
            0,
            DevConsoleEntry(
                id = "${System.nanoTime()}_${consoleLogs.size}",
                level = level,
                message = msg,
                time = nowTimeLabel()
            )
        )
        if (consoleLogs.size > 40) {
            consoleLogs.removeAt(consoleLogs.lastIndex)
        }
    }

    // Onboarding state
    var currentOnboardingSlide by remember { mutableIntStateOf(0) }
    var hasCompletedOnboarding by remember {
        mutableStateOf(!project.onboardingConfig.enabled || project.onboardingConfig.slides.isEmpty())
    }

    BackHandler {
        onBack()
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "📱 محاكي الهاتف التفاعلي المتطور",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${project.name} • ${project.packageConfig.packageName}",
                            fontFamily = TajawalFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل",
                            tint = accentColor
                        )
                    }
                }

                // Primary controls row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Simulated UI vs Real Embedded WebView
                    Surface(
                        color = if (isLiveWebViewMode) accentColor.copy(alpha = 0.22f) else DarkSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLiveWebViewMode) accentColor else GlassBorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .clickable {
                                isLiveWebViewMode = !isLiveWebViewMode
                                if (isLiveWebViewMode) {
                                    isHomeScreenMode = false
                                    isSplashPlaying = false
                                }
                            }
                            .testTag("toggle_live_webview_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = if (isLiveWebViewMode) accentColor else TextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLiveWebViewMode) "WebView حي ✓" else "WebView حقيقي",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isLiveWebViewMode) accentColor else TextPrimary
                            )
                        }
                    }

                    // Offline simulation toggle
                    Surface(
                        color = if (isOfflineSimulated) Color(0xFF3A1A1A) else DarkSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOfflineSimulated) ErrorRed else GlassBorderSubtle
                        ),
                        modifier = Modifier
                            .weight(0.9f)
                            .clickable {
                                isOfflineSimulated = !isOfflineSimulated
                                isLiveWebViewMode = true
                                appendConsoleLog(
                                    if (isOfflineSimulated) ConsoleLevel.WARN else ConsoleLevel.NETWORK,
                                    if (isOfflineSimulated) "تم تفعيل وضع عدم الاتصال (Offline Simulator)" else "تمت استعادة اتصال الشبكة ✓"
                                )
                            }
                            .testTag("toggle_offline_sim_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isOfflineSimulated) Icons.Default.WifiOff else Icons.Default.Wifi,
                                contentDescription = null,
                                tint = if (isOfflineSimulated) ErrorRed else StatusGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOfflineSimulated) "أوفلاين" else "متصل",
                                fontFamily = CairoFontFamily,
                                fontSize = 11.sp,
                                color = if (isOfflineSimulated) ErrorRed else TextPrimary
                            )
                        }
                    }

                    // DevTools Console Drawer toggle
                    Surface(
                        color = if (showDevToolsDrawer) OrangePrimary.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (showDevToolsDrawer) OrangePrimary else GlassBorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showDevToolsDrawer = !showDevToolsDrawer
                                if (showDevToolsDrawer) isLiveWebViewMode = true
                            }
                            .testTag("toggle_devtools_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DevTools", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }

                // Secondary controls row (Splash / Launcher / Test Push)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = DarkSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isLiveWebViewMode = false
                                isHomeScreenMode = false
                                isSplashPlaying = true
                                coroutineScope.launch {
                                    delay(2200)
                                    isSplashPlaying = false
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إعادة Splash", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextPrimary)
                        }
                    }

                    Surface(
                        color = if (isHomeScreenMode) accentColor.copy(alpha = 0.2f) else DarkSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isHomeScreenMode) accentColor else GlassBorderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                isLiveWebViewMode = false
                                isHomeScreenMode = !isHomeScreenMode
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                if (isHomeScreenMode) Icons.Default.PhoneAndroid else Icons.Default.Home,
                                contentDescription = null,
                                tint = if (isHomeScreenMode) accentColor else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isHomeScreenMode) "عرض التطبيق" else "شاشة المشغل",
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                color = if (isHomeScreenMode) accentColor else TextPrimary
                            )
                        }
                    }

                    if (project.pushConfig.enabled) {
                        Surface(
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showPushBanner = true
                                    coroutineScope.launch {
                                        delay(4000)
                                        showPushBanner = false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إشعار تجريبي", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = DarkBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlowingButton(
                        text = "⚡ بناء التطبيق الآن (APK / AAB)",
                        onClick = onBuild,
                        brush = Brush.horizontalGradient(listOf(accentColor, OrangeNeon)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Realistic Smartphone Chassis
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(36.dp),
                        spotColor = accentColor.copy(alpha = 0.5f),
                        ambientColor = Color.Black
                    )
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFF141620))
                    .border(
                        width = 3.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                accentColor,
                                Color(0xFF2E3246),
                                accentColor.copy(alpha = 0.5f)
                            )
                        ),
                        shape = RoundedCornerShape(36.dp)
                    )
                    .padding(8.dp)
            ) {
                // Inner Screen Display
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF090A0F))
                ) {
                    // Content inside the phone
                    if (isLiveWebViewMode) {
                        LiveWebViewContainer(
                            project = project,
                            isOfflineMode = isOfflineSimulated,
                            reloadTrigger = webReloadTrigger,
                            jsCommandToRun = jsCommandTrigger,
                            accentColor = accentColor,
                            onRetryOnline = {
                                isOfflineSimulated = false
                                webReloadTrigger++
                            },
                            onLog = { level, msg -> appendConsoleLog(level, msg) },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 28.dp, bottom = 14.dp)
                        )
                    } else if (isHomeScreenMode) {
                        // Phone Home Screen simulation with app icon
                        HomeScreenSimulation(
                            appName = project.name,
                            accentColor = accentColor,
                            iconUri = project.iconImageUri,
                            onLaunchApp = {
                                isHomeScreenMode = false
                                isSplashPlaying = true
                                coroutineScope.launch {
                                    delay(2000)
                                    isSplashPlaying = false
                                }
                            }
                        )
                    } else if (isSplashPlaying) {
                        // Custom Splash Screen simulation
                        SplashSimulation(
                            appName = project.name,
                            accentColor = accentColor,
                            iconUri = project.iconImageUri,
                            splashBgColor = parseColorSafe(project.splashConfig.backgroundColorHex, Color(0xFF0F1318)),
                            fadeBackground = project.splashConfig.fadeBackground,
                            indicatorStyle = project.splashConfig.indicatorStyle,
                            iconSize = project.splashConfig.iconSizeDp
                        )
                    } else if (!hasCompletedOnboarding && project.onboardingConfig.enabled && project.onboardingConfig.slides.isNotEmpty()) {
                        // Custom Onboarding Screen simulation
                        OnboardingSimulation(
                            slides = project.onboardingConfig.slides,
                            currentSlide = currentOnboardingSlide,
                            accentColor = accentColor,
                            onNextSlide = {
                                if (currentOnboardingSlide < project.onboardingConfig.slides.size - 1) {
                                    currentOnboardingSlide++
                                } else {
                                    hasCompletedOnboarding = true
                                }
                            },
                            onSkip = {
                                hasCompletedOnboarding = true
                            }
                        )
                    } else {
                        // Active Main App Interface
                        ActiveAppSimulation(
                            project = project,
                            accentColor = accentColor,
                            selectedBottomTab = selectedBottomTab,
                            onTabSelected = { selectedBottomTab = it },
                            isRefreshing = isRefreshing,
                            onRefresh = {
                                isRefreshing = true
                                coroutineScope.launch {
                                    delay(1500)
                                    isRefreshing = false
                                    interactiveCounter++
                                    Toast.makeText(context, "تم تحديث الصفحة بنجاح ✓", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onRequestPermission = { perm ->
                                permissionType = perm
                                showPermissionDialog = true
                            },
                            counter = interactiveCounter
                        )
                    }

                    // Phone Top Notch / Dynamic Island & Status Bar
                    PhoneStatusBarAndNotch(accentColor = accentColor)

                    // Phone Bottom Navigation Pill
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .width(110.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                    )

                    // Push Notification Dropdown Banner
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showPushBanner,
                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 34.dp, start = 12.dp, end = 12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1E2130),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                            shadowElevation = 10.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(accentColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!project.iconImageUri.isNullOrBlank() && project.iconImageUri != "preset_anime") {
                                        AsyncImage(
                                            model = project.iconImageUri,
                                            contentDescription = project.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${project.name} • ${project.pushConfig.provider} • الآن",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = project.pushConfig.testTitle.ifBlank { "مرحباً بك!" },
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = project.pushConfig.testBody.ifBlank { "يعمل نظام الإشعارات ${project.pushConfig.provider} بكفاءة عالية." },
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Android Permission Dialog Simulator
                    if (showPermissionDialog) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f))
                                .clickable { showPermissionDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = Color(0xFF1F2232),
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clickable(enabled = false) {}
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (permissionType) {
                                            "الكاميرا" -> Icons.Default.CameraAlt
                                            "الموقع الجغرافي" -> Icons.Default.LocationOn
                                            else -> Icons.Default.Mic
                                        },
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "هل تسمح لتطبيق ${project.name} بالوصول إلى $permissionType؟",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            showPermissionDialog = false
                                            permissionGranted = true
                                            Toast.makeText(context, "تم منح إذن $permissionType بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("أثناء استخدام التطبيق فقط", fontFamily = CairoFontFamily, fontSize = 12.sp, color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedButton(
                                        onClick = { showPermissionDialog = false },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("عدم السماح", fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showDevToolsDrawer) {
                DevToolsConsoleDrawer(
                    logs = consoleLogs,
                    onClear = { consoleLogs.clear() },
                    onRunJs = { js ->
                        val nextId = (jsCommandTrigger?.first ?: 0) + 1
                        jsCommandTrigger = nextId to js
                    }
                )
            }
        }
    }
}

@Composable
private fun PhoneStatusBarAndNotch(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Clock
        Text(
            text = "12:00",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // Dynamic Island / Camera hole
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color.Black)
                .border(0.5.dp, Color(0xFF2A2D3C), RoundedCornerShape(9.dp))
        )

        // Icons: 5G, Wi-Fi, Battery
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
            Icon(Icons.Default.NetworkCell, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
            Icon(Icons.Default.BatteryFull, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun HomeScreenSimulation(
    appName: String,
    accentColor: Color,
    iconUri: String?,
    onLaunchApp: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF020617))
                )
            )
            .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Widget Clock
            Text(
                text = "12:00",
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "الأحد، 15 نوفمبر",
                fontFamily = CairoFontFamily,
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // User App Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onLaunchApp() }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = accentColor)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconUri != null && iconUri != "preset_anime") {
                        AsyncImage(
                            model = iconUri,
                            contentDescription = appName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = appName,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "اضغط على أيقونة التطبيق لفتحه 👆",
                fontFamily = TajawalFontFamily,
                fontSize = 11.sp,
                color = accentColor
            )
        }
    }
}

@Composable
private fun SplashSimulation(
    appName: String,
    accentColor: Color,
    iconUri: String?,
    splashBgColor: Color,
    fadeBackground: Boolean,
    indicatorStyle: String,
    iconSize: Int
) {
    var startFadeOut by remember { mutableStateOf(false) }
    LaunchedEffect(fadeBackground) {
        startFadeOut = false
        if (fadeBackground) {
            delay(1350)
            startFadeOut = true
        }
    }

    val splashAlpha by animateFloatAsState(
        targetValue = if (fadeBackground && startFadeOut) 0f else 1f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "splashSimFade"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900), repeatMode = RepeatMode.Reverse),
        label = "haloPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashBgColor.copy(alpha = splashAlpha))
            .let { mod ->
                if (fadeBackground) {
                    mod.background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.22f * haloPulse * splashAlpha),
                                splashBgColor.copy(alpha = splashAlpha),
                                Color.Transparent
                            )
                        )
                    )
                } else mod
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size((iconSize / 1.6f).dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accentColor)
                    .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (iconUri != null && iconUri != "preset_anime") {
                    AsyncImage(
                        model = iconUri,
                        contentDescription = appName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = appName,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(28.dp))

            when (indicatorStyle) {
                "حلقة" -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = accentColor,
                        strokeWidth = 3.dp
                    )
                }
                "شريط" -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width(120.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accentColor,
                        trackColor = Color(0xFF222533)
                    )
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingSimulation(
    slides: List<com.example.domain.model.OnboardingSlide>,
    currentSlide: Int,
    accentColor: Color,
    onNextSlide: () -> Unit,
    onSkip: () -> Unit
) {
    val slide = slides.getOrNull(currentSlide) ?: slides.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
            .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "تخطي",
                fontFamily = CairoFontFamily,
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.clickable { onSkip() }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.5.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = slide.title,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = slide.description,
                fontFamily = TajawalFontFamily,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in slides.indices) {
                    Box(
                        modifier = Modifier
                            .size(if (i == currentSlide) 18.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (i == currentSlide) accentColor else Color(0xFF2A2D3C))
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onNextSlide,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (currentSlide == slides.size - 1) "البدء الآن" else "متابعة",
                    fontFamily = CairoFontFamily,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun ActiveAppSimulation(
    project: AppProject,
    accentColor: Color,
    selectedBottomTab: Int,
    onTabSelected: (Int) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onRequestPermission: (String) -> Unit,
    counter: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0E16))
    ) {
        // App Top Bar (if not fullscreen)
        if (!project.webViewConfig.fullScreen) {
            Surface(
                color = Color(0xFF131622),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 8.dp, start = 14.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (project.iconImageUri != null && project.iconImageUri != "preset_anime") {
                                AsyncImage(
                                    model = project.iconImageUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = project.name,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (project.webViewConfig.pullToRefresh) {
                            IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = accentColor, modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Search, contentDescription = "بحث", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Pull to refresh animated loader
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF1E2130),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = accentColor,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري تحديث المحتوى...", fontFamily = TajawalFontFamily, fontSize = 11.sp, color = TextPrimary)
                    }
                }
            }
        }

        // Body Content switching by bottom navigation tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedBottomTab) {
                0 -> {
                    // Home / WebView
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            // Banner
                            Surface(
                                color = accentColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "⚡ تطبيق تفاعلي جاهز للبناء",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = accentColor
                                    )
                                    Text(
                                        text = "المصدر: ${project.sourceValue} • تم التحديث $counter مرة",
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        // Test interactive buttons inside the phone
                        item {
                            Text(
                                text = "اختبار ميزات النظام والصلاحيات:",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }

                        if (project.permissionsConfig.camera) {
                            item {
                                TestFeatureButton(
                                    icon = Icons.Default.CameraAlt,
                                    title = "تشغيل الكاميرا داخل التطبيق",
                                    subtitle = "يطلب صلاحية android.permission.CAMERA",
                                    color = accentColor,
                                    onClick = { onRequestPermission("الكاميرا") }
                                )
                            }
                        }

                        if (project.permissionsConfig.location) {
                            item {
                                TestFeatureButton(
                                    icon = Icons.Default.LocationOn,
                                    title = "تحديد الموقع الجغرافي (GPS)",
                                    subtitle = "يطلب صلاحية ACCESS_FINE_LOCATION",
                                    color = Color(0xFF38BDF8),
                                    onClick = { onRequestPermission("الموقع الجغرافي") }
                                )
                            }
                        }

                        if (project.permissionsConfig.microphone) {
                            item {
                                TestFeatureButton(
                                    icon = Icons.Default.Mic,
                                    title = "تسجيل الصوت / الميكروفون",
                                    subtitle = "يطلب صلاحية RECORD_AUDIO",
                                    color = Color(0xFFA855F7),
                                    onClick = { onRequestPermission("الميكروفون") }
                                )
                            }
                        }

                        if (project.permissionsConfig.notifications) {
                            item {
                                TestFeatureButton(
                                    icon = Icons.Default.Notifications,
                                    title = "اختبار الإشعارات الفورية",
                                    subtitle = "يطلب صلاحية POST_NOTIFICATIONS",
                                    color = Color(0xFFF59E0B),
                                    onClick = { onRequestPermission("الإشعارات") }
                                )
                            }
                        }

                        if (project.permissionsConfig.vibration) {
                            item {
                                TestFeatureButton(
                                    icon = Icons.Default.Vibration,
                                    title = "تجربة الردود اللمسية والاهتزاز",
                                    subtitle = "يطلب صلاحية VIBRATE",
                                    color = Color(0xFF10B981),
                                    onClick = { onRequestPermission("الاهتزاز") }
                                )
                            }
                        }

                        if (project.permissionsConfig.storage) {
                            item {
                                TestFeatureButton(
                                    icon = Icons.Default.Folder,
                                    title = "تنزيل وحفظ الملفات",
                                    subtitle = "يطلب صلاحية READ_EXTERNAL_STORAGE",
                                    color = Color(0xFF0EA5E9),
                                    onClick = { onRequestPermission("الملفات والتخزين") }
                                )
                            }
                        }

                        if (project.permissionsConfig.bluetooth) {
                            item {
                                TestFeatureButton(
                                    icon = Icons.Default.Bluetooth,
                                    title = "البحث عن أجهزة البلوتوث",
                                    subtitle = "يطلب صلاحية BLUETOOTH",
                                    color = Color(0xFF6366F1),
                                    onClick = { onRequestPermission("البلوتوث") }
                                )
                            }
                        }

                        // Code / Web page simulation card
                        item {
                            Surface(
                                color = Color(0xFF141724),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("index.html (WebView)", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = StatusGreen)
                                        Text("HTTP 200 OK", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TextMuted)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "<!DOCTYPE html>\n<html>\n  <head><title>${project.name}</title></head>\n  <body>\n    <h1>Welcome to ${project.name}</h1>\n  </body>\n</html>",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Explore / Files Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = accentColor, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("مستعرض الملفات والأصول", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("أصول المشروع جاهزة للحزم داخل APK", fontFamily = TajawalFontFamily, fontSize = 12.sp, color = TextSecondary)
                    }
                }

                else -> {
                    // Profile / Settings Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = accentColor, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("إعدادات تطبيق ${project.name}", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text("الإصدار ${project.packageConfig.versionName} (${project.packageConfig.versionCode})", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

        // Bottom Navigation Bar (if enabled)
        if (project.bottomNavConfig.enabled && project.bottomNavConfig.destinations.isNotEmpty()) {
            Surface(
                color = Color(0xFF131520),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF242738)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    project.bottomNavConfig.destinations.forEachIndexed { index, dest ->
                        val isSelected = selectedBottomTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onTabSelected(index) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Home
                                    1 -> Icons.Default.Explore
                                    2 -> Icons.Default.Person
                                    else -> Icons.Default.Apps
                                },
                                contentDescription = dest.title,
                                tint = if (isSelected) accentColor else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dest.title.ifBlank { "تبويب ${index + 1}" },
                                fontFamily = CairoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestFeatureButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF171A28),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                Text(text = subtitle, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}
