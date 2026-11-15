package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Expand
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.AdaptivePhase
import com.example.domain.model.AppProject
import com.example.domain.model.DynamicPhaseFeature
import com.example.domain.model.SourceType
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TajawalFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.parseColorSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class PhasePhoneShellMode {
    ADAPTIVE_PHASE,
    HOME_LAUNCHER,
    SPLASH_SCREEN
}

private enum class PhasePreviewKind(val badgeLabel: String) {
    WEB_SOURCE("معاينة المصدر التفاعلية"),
    WEB_DESIGN("استوديو التصميم والثيم"),
    WEB_IMAGES("معاينة الصور المكتشفة"),
    WEB_BUTTONS("معاينة الأزرار والتفاعل"),
    WEB_TEXTS("معاينة النصوص والعناوين"),
    WEB_ENGINE_NAV("محاكي التنقل والمحرك"),
    NATIVE_SECURITY_PERMS("محاكي الحزمة والأمان"),
    APK_IDENTITY("هوية وأيقونة APK"),
    APK_PACKAGE("معلومات الحزمة والتوقيع"),
    APK_PERMISSIONS("نظام أذونات أندرويد"),
    APK_HARDENING("التحصين والحماية الحية")
}

private fun resolvePhasePreviewKind(phase: AdaptivePhase, project: AppProject): PhasePreviewKind {
    val keys = phase.features.map { it.bindingKey }
    if (project.sourceType == SourceType.APK_PACKAGE) {
        return when {
            keys.any { it.startsWith("pkg.") } -> PhasePreviewKind.APK_PACKAGE
            keys.any { it.startsWith("perm.") } -> PhasePreviewKind.APK_PERMISSIONS
            keys.any { it.contains("flagSecure") || it.contains("sslPinning") || it.contains("adAndPopupBlocker") } ->
                PhasePreviewKind.APK_HARDENING
            else -> PhasePreviewKind.APK_IDENTITY
        }
    }
    return when {
        !phase.supportsLiveWebPreview -> PhasePreviewKind.NATIVE_SECURITY_PERMS
        keys.any { it.startsWith("dom.img.") } -> PhasePreviewKind.WEB_IMAGES
        keys.any { it.startsWith("dom.btn.") } -> PhasePreviewKind.WEB_BUTTONS
        keys.any { it.startsWith("dom.text.") } -> PhasePreviewKind.WEB_TEXTS
        phase.badge == "التصميم" || keys.any { it == "app.accentColorHex" || it == "omni.gradientEnabled" } ->
            PhasePreviewKind.WEB_DESIGN
        phase.badge == "المحرك" || keys.any { it == "bottomNav.enabled" || it == "pro.drawerEnabled" || it == "omni.apiBaseUrl" } ->
            PhasePreviewKind.WEB_ENGINE_NAV
        else -> PhasePreviewKind.WEB_SOURCE
    }
}

/**
 * شاشة الهاتف الحقيقية المتقدمة للمعاينة الحية التفاعلية داخل كل مرحلة:
 * - تتكيف كلياً حسب المرحلة والميزات والمحتوى (صور / أزرار / نصوص / تصميم / محرك / أمان / APK / AI).
 * - تتفاعل مباشرةً مع نقرات المستخدم داخل الهاتف ومع تعديلات الميزات لحظياً.
 * - آمنة تماماً ضد أعطال WebView (تعطيل الميل 3D عند عرض WebView الفعلي).
 */
@Composable
fun PhaseInteractivePhoneCard(
    project: AppProject,
    currentPhase: AdaptivePhase,
    totalPhases: Int,
    phaseAccent: Color,
    isExpandedPane: Boolean,
    reloadTrigger: Int,
    onToggleCollapsePane: () -> Unit,
    onReloadWebView: () -> Unit,
    onProceedToFullSandbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isTallPhone by remember { mutableStateOf(false) }
    var shellMode by remember(project.id) { mutableStateOf(PhasePhoneShellMode.ADAPTIVE_PHASE) }
    var isOfflineSim by remember { mutableStateOf(false) }
    var isDrawerOpen by remember(currentPhase.index) { mutableStateOf(false) }
    var isFabMenuOpen by remember(currentPhase.index) { mutableStateOf(false) }
    var activeNavTab by remember { mutableIntStateOf(0) }
    var showPushBanner by remember { mutableStateOf(false) }
    var isPullRefreshing by remember { mutableStateOf(false) }
    var activePermissionPrompt by remember { mutableStateOf<String?>(null) }
    var grantedSimPermissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var authLoggedInUser by remember { mutableStateOf<String?>(null) }
    var phoneToastText by remember { mutableStateOf<String?>(null) }
    var jsCommandTrigger by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var lastWebLog by remember { mutableStateOf<String?>(null) }
    var showFlagSecureToast by remember { mutableStateOf(false) }

    val previewKind = remember(currentPhase, project.sourceType) {
        resolvePhasePreviewKind(currentPhase, project)
    }
    val usesLiveWebView = currentPhase.supportsLiveWebPreview && shellMode == PhasePhoneShellMode.ADAPTIVE_PHASE

    fun showPhoneToast(msg: String) {
        phoneToastText = msg
    }

    LaunchedEffect(phoneToastText) {
        if (phoneToastText != null) {
            delay(2600)
            phoneToastText = null
        }
    }

    LaunchedEffect(showFlagSecureToast) {
        if (showFlagSecureToast) {
            delay(2200)
            showFlagSecureToast = false
        }
    }

    fun dispatchJs(js: String) {
        val next = (jsCommandTrigger?.first ?: 0) + 1
        jsCommandTrigger = next to js
    }

    // تمييز وتمرير تلقائي للعناصر الموافقة للمرحلة الحالية داخل WebView
    LaunchedEffect(currentPhase.index, currentPhase.features, usesLiveWebView) {
        if (!usesLiveWebView) return@LaunchedEffect
        delay(280)
        val keys = currentPhase.features.map { it.bindingKey }
        when {
            keys.any { it.startsWith("dom.img.") } -> {
                val firstIdx = keys.firstNotNullOfOrNull {
                    if (it.startsWith("dom.img.visible.")) it.substringAfterLast(".").toIntOrNull() else null
                } ?: 0
                dispatchJs(
                    "(function(){var imgs=document.querySelectorAll('img');var t=imgs[$firstIdx];if(t){t.scrollIntoView({behavior:'smooth',block:'center'});var o=t.style.outline;t.style.outline='3px solid #38BDF8';setTimeout(function(){t.style.outline=o;},1500);}})();"
                )
            }
            keys.any { it.startsWith("dom.btn.") } -> {
                val firstIdx = keys.firstNotNullOfOrNull {
                    if (it.startsWith("dom.btn.text.")) it.substringAfterLast(".").toIntOrNull() else null
                } ?: 0
                dispatchJs(
                    "(function(){var bs=document.querySelectorAll('button,a.btn,.btn,[role=\"button\"]');var t=bs[$firstIdx];if(t){t.scrollIntoView({behavior:'smooth',block:'center'});var o=t.style.outline;t.style.outline='3px solid #F59E0B';setTimeout(function(){t.style.outline=o;},1500);}})();"
                )
            }
            keys.any { it.startsWith("dom.text.") } -> {
                val firstIdx = keys.firstNotNullOfOrNull {
                    if (it.startsWith("dom.text.replace.")) it.substringAfterLast(".").toIntOrNull() else null
                } ?: 0
                dispatchJs(
                    "(function(){var ts=document.querySelectorAll('h1,h2,h3,h4,p');var t=ts[$firstIdx];if(t){t.scrollIntoView({behavior:'smooth',block:'center'});var o=t.style.outline;t.style.outline='2px dashed #A855F7';setTimeout(function(){t.style.outline=o;},1500);}})();"
                )
            }
            keys.any { it == "omni.customHeroTitle" || it == "omni.customPrimaryButtonText" } -> {
                dispatchJs(
                    "(function(){var h=document.querySelector('[data-omni-hero],h1,.hero-title');if(h){h.scrollIntoView({behavior:'smooth',block:'start'});}})();"
                )
            }
        }
    }

    val appAccent = remember(project.accentColorHex, phaseAccent) {
        parseColorSafe(project.accentColorHex, phaseAccent)
    }

    val phoneHeight by animateDpAsState(
        targetValue = if (isTallPhone) 390.dp else 255.dp,
        animationSpec = tween(260),
        label = "phasePhoneHeight"
    )

    ThreeDSurfaceCard(
        accentColor = phaseAccent,
        elevation = 12.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // شريط التحكم العلوي في الهاتف التفاعلي للمرحلة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOfflineSim) ErrorRed else StatusGreen)
                    )
                    Column {
                        Text(
                            text = "هاتف المعاينة الحية • المرحلة ${currentPhase.index}/$totalPhases",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = phaseAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = previewKind.badgeLabel,
                            fontFamily = TajawalFontFamily,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExpandedPane) {
                        IconButton(
                            onClick = { isTallPhone = !isTallPhone },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("phase_phone_resize_btn")
                        ) {
                            Icon(
                                imageVector = if (isTallPhone) Icons.Filled.Compress else Icons.Filled.Expand,
                                contentDescription = if (isTallPhone) "تصغير الهاتف" else "تكبير الهاتف",
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (usesLiveWebView) {
                            IconButton(
                                onClick = {
                                    onReloadWebView()
                                    showPhoneToast("تم تحديث المحتوى الحي ✓")
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "تحديث الهاتف",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = onToggleCollapsePane,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("toggle_live_preview_pane_btn")
                    ) {
                        Icon(
                            imageVector = if (isExpandedPane) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpandedPane) "طي شاشة الهاتف" else "إظهار شاشة الهاتف",
                            tint = phaseAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpandedPane) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // شريط اختصارات أنظمة الهاتف والتفاعل السريع مع المرحلة
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            PhaseQuickChip(
                                label = "شاشة المرحلة",
                                icon = Icons.Filled.PhoneAndroid,
                                selected = shellMode == PhasePhoneShellMode.ADAPTIVE_PHASE,
                                accent = phaseAccent,
                                onClick = { shellMode = PhasePhoneShellMode.ADAPTIVE_PHASE }
                            )
                        }
                        item {
                            PhaseQuickChip(
                                label = "سطح الهاتف",
                                icon = Icons.Filled.Home,
                                selected = shellMode == PhasePhoneShellMode.HOME_LAUNCHER,
                                accent = CyanAccent,
                                onClick = {
                                    shellMode = if (shellMode == PhasePhoneShellMode.HOME_LAUNCHER) {
                                        PhasePhoneShellMode.ADAPTIVE_PHASE
                                    } else {
                                        PhasePhoneShellMode.HOME_LAUNCHER
                                    }
                                }
                            )
                        }
                        item {
                            PhaseQuickChip(
                                label = "Splash",
                                icon = Icons.Filled.RocketLaunch,
                                selected = shellMode == PhasePhoneShellMode.SPLASH_SCREEN,
                                accent = OrangePrimary,
                                onClick = {
                                    shellMode = PhasePhoneShellMode.SPLASH_SCREEN
                                    scope.launch {
                                        delay(2000)
                                        if (shellMode == PhasePhoneShellMode.SPLASH_SCREEN) {
                                            shellMode = PhasePhoneShellMode.ADAPTIVE_PHASE
                                        }
                                    }
                                }
                            )
                        }
                        if (usesLiveWebView) {
                            item {
                                PhaseQuickChip(
                                    label = if (isOfflineSim) "أوفلاين" else "اتصال حي",
                                    icon = if (isOfflineSim) Icons.Filled.WifiOff else Icons.Filled.Wifi,
                                    selected = isOfflineSim,
                                    accent = if (isOfflineSim) ErrorRed else StatusGreen,
                                    onClick = {
                                        isOfflineSim = !isOfflineSim
                                        showPhoneToast(if (isOfflineSim) "تفعيل وضع أوفلاين" else "استعادة الشبكة")
                                    }
                                )
                            }
                        }
                        if (project.pushConfig.enabled || currentPhase.features.any { it.bindingKey == "push.enabled" }) {
                            item {
                                PhaseQuickChip(
                                    label = "إشعار فوري",
                                    icon = Icons.Filled.NotificationsActive,
                                    selected = showPushBanner,
                                    accent = StatusGreen,
                                    onClick = {
                                        showPushBanner = true
                                        scope.launch {
                                            delay(3600)
                                            showPushBanner = false
                                        }
                                    }
                                )
                            }
                        }
                        if (project.proConfig.flagSecureEnabled || currentPhase.features.any { it.bindingKey == "pro.flagSecureEnabled" }) {
                            item {
                                PhaseQuickChip(
                                    label = "فحص الحماية",
                                    icon = Icons.Filled.Lock,
                                    selected = showFlagSecureToast,
                                    accent = StatusGreen,
                                    onClick = { showFlagSecureToast = true }
                                )
                            }
                        }
                    }

                    // هيكل الهاتف الحقيقي ثلاثي الأبعاد المتطور
                    Box(
                        modifier = Modifier
                            .widthIn(max = 350.dp)
                            .fillMaxWidth()
                            .height(phoneHeight)
                            .threeDPerspectiveTilt(!usesLiveWebView)
                            .shadow(
                                elevation = 18.dp,
                                shape = RoundedCornerShape(30.dp),
                                spotColor = phaseAccent.copy(alpha = 0.45f),
                                ambientColor = Color.Black
                            )
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color(0xFF151824))
                            .border(
                                width = 2.5.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        phaseAccent.copy(alpha = 0.9f),
                                        Color(0xFF333950),
                                        appAccent.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .padding(6.dp)
                            .testTag("phase_real_phone_chassis")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF0A0C12))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // شريط حالة أندرويد الحقيقي + الجزيرة الديناميكية
                                PhasePhoneStatusBar(
                                    accent = appAccent,
                                    isOffline = isOfflineSim,
                                    isFlagSecure = project.proConfig.flagSecureEnabled,
                                    isSslPinned = project.proConfig.sslPinningEnabled
                                )

                                // جسم الشاشة الداخلي (يتبدل حسب وضع الشاشة والمرحلة)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    when (shellMode) {
                                        PhasePhoneShellMode.HOME_LAUNCHER -> {
                                            PhaseLauncherHomeContent(
                                                project = project,
                                                accent = appAccent,
                                                onLaunchApp = {
                                                    shellMode = PhasePhoneShellMode.SPLASH_SCREEN
                                                    scope.launch {
                                                        delay(1800)
                                                        shellMode = PhasePhoneShellMode.ADAPTIVE_PHASE
                                                    }
                                                }
                                            )
                                        }

                                        PhasePhoneShellMode.SPLASH_SCREEN -> {
                                            PhaseSplashContent(
                                                project = project,
                                                accent = appAccent
                                            )
                                        }

                                        PhasePhoneShellMode.ADAPTIVE_PHASE -> {
                                            if (currentPhase.supportsLiveWebPreview) {
                                                AdaptiveWebPhoneContent(
                                                    project = project,
                                                    currentPhase = currentPhase,
                                                    previewKind = previewKind,
                                                    appAccent = appAccent,
                                                    phaseAccent = phaseAccent,
                                                    isOfflineSim = isOfflineSim,
                                                    reloadTrigger = reloadTrigger,
                                                    jsCommandToRun = jsCommandTrigger,
                                                    activeNavTab = activeNavTab,
                                                    isPullRefreshing = isPullRefreshing,
                                                    isFabMenuOpen = isFabMenuOpen,
                                                    onRetryOnline = { isOfflineSim = false },
                                                    onSelectNavTab = { idx, tabLabel ->
                                                        activeNavTab = idx
                                                        showPhoneToast("تبويب: $tabLabel")
                                                        dispatchJs(
                                                            "(function(){var sec=document.querySelectorAll('section,header,footer')[$idx];if(sec)sec.scrollIntoView({behavior:'smooth'});})();"
                                                        )
                                                    },
                                                    onToggleDrawer = { isDrawerOpen = !isDrawerOpen },
                                                    onToggleFab = { isFabMenuOpen = !isFabMenuOpen },
                                                    onTriggerPullRefresh = {
                                                        isPullRefreshing = true
                                                        onReloadWebView()
                                                        scope.launch {
                                                            delay(900)
                                                            isPullRefreshing = false
                                                            showPhoneToast("تم تحديث الصفحة ✓")
                                                        }
                                                    },
                                                    onFocusDomElement = { jsCmd, label ->
                                                        dispatchJs(jsCmd)
                                                        showPhoneToast(label)
                                                    },
                                                    onActionNotice = { showPhoneToast(it) },
                                                    onWebLog = { lastWebLog = it }
                                                )
                                            } else {
                                                AdaptiveNativePhasePhoneContent(
                                                    project = project,
                                                    currentPhase = currentPhase,
                                                    previewKind = previewKind,
                                                    appAccent = appAccent,
                                                    phaseAccent = phaseAccent,
                                                    authLoggedInUser = authLoggedInUser,
                                                    grantedSimPermissions = grantedSimPermissions,
                                                    onLoginDemoUser = { user ->
                                                        authLoggedInUser = user
                                                        showPhoneToast("تم تسجيل الدخول كـ $user ✓")
                                                    },
                                                    onLogoutDemoUser = {
                                                        authLoggedInUser = null
                                                        showPhoneToast("تم تسجيل الخروج")
                                                    },
                                                    onRequestPermDialog = { permLabel ->
                                                        activePermissionPrompt = permLabel
                                                    },
                                                    onTriggerPush = {
                                                        showPushBanner = true
                                                        scope.launch {
                                                            delay(3500)
                                                            showPushBanner = false
                                                        }
                                                    },
                                                    onTestHardening = { msg -> showPhoneToast(msg) },
                                                    onOpenFullSandbox = onProceedToFullSandbox
                                                )
                                            }
                                        }
                                    }

                                    // القائمة الجانبية التفاعلية داخل الهاتف (Navigation Drawer)
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isDrawerOpen && shellMode == PhasePhoneShellMode.ADAPTIVE_PHASE,
                                        enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                                        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.45f))
                                                .clickable { isDrawerOpen = false }
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .width(195.dp)
                                                    .background(Color(0xFF161925))
                                                    .border(1.dp, appAccent.copy(alpha = 0.4f))
                                                    .clickable(enabled = false) {}
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    RealAppIconAvatar(project = project, size = 34.dp, cornerRadius = 10.dp)
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = project.name,
                                                            fontFamily = CairoFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = TextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "v${project.packageConfig.versionName}",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.sp,
                                                            color = appAccent
                                                        )
                                                    }
                                                }
                                                listOf("الرئيسية", "الأقسام", "المفضلة", "الإعدادات").forEachIndexed { idx, item ->
                                                    Surface(
                                                        color = if (activeNavTab == idx) appAccent.copy(alpha = 0.2f) else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                activeNavTab = idx
                                                                isDrawerOpen = false
                                                                showPhoneToast("تم فتح: $item")
                                                            }
                                                    ) {
                                                        Text(
                                                            text = item,
                                                            fontFamily = CairoFontFamily,
                                                            fontSize = 11.sp,
                                                            color = TextPrimary,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // إشعار فوري منبثق (Push Notification Shade)
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showPushBanner,
                                        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                                        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xFF1F2335),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, appAccent.copy(alpha = 0.6f)),
                                            shadowElevation = 10.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showPushBanner = false }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(appAccent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = project.pushConfig.testTitle.ifBlank { "${project.name} • تنبيه مباشر" },
                                                        fontFamily = CairoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = TextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = project.pushConfig.testBody.ifBlank { "نظام الإشعارات الفوري يعمل داخل تطبيقك بنجاح." },
                                                        fontFamily = TajawalFontFamily,
                                                        fontSize = 10.sp,
                                                        color = TextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // نافذة طلب صلاحية أندرويد الحقيقية داخل الهاتف
                                    if (activePermissionPrompt != null) {
                                        val permName = activePermissionPrompt ?: "الكاميرا"
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.72f))
                                                .clickable { activePermissionPrompt = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                color = Color(0xFF1E2233),
                                                shape = RoundedCornerShape(18.dp),
                                                border = BorderStroke(1.dp, appAccent.copy(alpha = 0.55f)),
                                                modifier = Modifier
                                                    .fillMaxWidth(0.88f)
                                                    .clickable(enabled = false) {}
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AdminPanelSettings,
                                                        contentDescription = null,
                                                        tint = appAccent,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                    Text(
                                                        text = "هل تسمح لتطبيق ${project.name} بالوصول إلى $permName؟",
                                                        fontFamily = CairoFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = TextPrimary,
                                                        textAlign = TextAlign.Center
                                                    )
                                                    if (project.omniConfig.smartPermissionsRationaleEnabled) {
                                                        Text(
                                                            text = "الشرح الذكي: يُستخدم هذا الإذن فقط لتفعيل ميزة $permName بأمان.",
                                                            fontFamily = TajawalFontFamily,
                                                            fontSize = 10.sp,
                                                            color = CyanAccent,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                grantedSimPermissions = grantedSimPermissions - permName
                                                                activePermissionPrompt = null
                                                                showPhoneToast("تم رفض إذن $permName")
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("رفض", fontFamily = CairoFontFamily, fontSize = 10.sp, color = TextSecondary)
                                                        }
                                                        Button(
                                                            onClick = {
                                                                grantedSimPermissions = grantedSimPermissions + permName
                                                                activePermissionPrompt = null
                                                                showPhoneToast("تم السماح لـ $permName ✓")
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = appAccent),
                                                            shape = RoundedCornerShape(10.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("سماح", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // محاكاة حظر لقطات الشاشة (FLAG_SECURE)
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = showFlagSecureToast,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                        modifier = Modifier.align(Alignment.Center)
                                    ) {
                                        Surface(
                                            color = Color(0xEE111420),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(1.dp, StatusGreen)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Lock, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = if (project.proConfig.flagSecureEnabled) {
                                                        "FLAG_SECURE نشط: تم منع التقاط الشاشة 🛡️"
                                                    } else {
                                                        "التقاط الشاشة مسموح حالياً"
                                                    },
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // رسالة إشعار داخل شاشة الهاتف (In-Phone Toast)
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = phoneToastText != null,
                                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                                        exit = fadeOut(),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 8.dp, start = 12.dp, end = 12.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xE6181C2A),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(1.dp, phaseAccent.copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                text = phoneToastText.orEmpty(),
                                                fontFamily = CairoFontFamily,
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // شريط أزرار التنقل النظامي السفلي للهاتف (Back / Home Pill / Switcher)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp)
                                        .background(Color(0xFF07090E))
                                        .padding(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp, 20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                when {
                                                    activePermissionPrompt != null -> activePermissionPrompt = null
                                                    isDrawerOpen -> isDrawerOpen = false
                                                    isFabMenuOpen -> isFabMenuOpen = false
                                                    shellMode != PhasePhoneShellMode.ADAPTIVE_PHASE -> shellMode = PhasePhoneShellMode.ADAPTIVE_PHASE
                                                    usesLiveWebView -> dispatchJs("if(history.length>1)history.back();")
                                                    else -> Toast.makeText(context, "رجوع داخل التطبيق", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "زر الرجوع في الهاتف",
                                            tint = Color.White.copy(alpha = 0.65f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    // Gesture Pill -> الضغط يبدّل بين شاشة التطبيق وسطحه
                                    Box(
                                        modifier = Modifier
                                            .width(78.dp)
                                            .height(16.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                shellMode = if (shellMode == PhasePhoneShellMode.HOME_LAUNCHER) {
                                                    PhasePhoneShellMode.ADAPTIVE_PHASE
                                                } else {
                                                    PhasePhoneShellMode.HOME_LAUNCHER
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(64.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color.White.copy(alpha = 0.68f))
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp, 20.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onProceedToFullSandbox() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Expand,
                                            contentDescription = "فتح المعاينة النهائية الكاملة",
                                            tint = CyanAccent.copy(alpha = 0.85f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!lastWebLog.isNullOrBlank() && usesLiveWebView) {
                        Text(
                            text = "سجل التفاعل: $lastWebLog",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseQuickChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.22f) else DarkSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (selected) accent else GlassBorderSubtle),
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) accent else TextSecondary, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (selected) TextPrimary else TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PhasePhoneStatusBar(
    accent: Color,
    isOffline: Boolean,
    isFlagSecure: Boolean,
    isSslPinned: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color(0xFF0A0C12))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "12:00",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            if (isFlagSecure) {
                Icon(Icons.Filled.Lock, contentDescription = "محمي", tint = StatusGreen, modifier = Modifier.size(10.dp))
            }
        }

        // Dynamic Island
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.Black)
                .border(0.6.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E3A8A))
            )
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isSslPinned) {
                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(10.dp))
            }
            Icon(
                imageVector = if (isOffline) Icons.Filled.WifiOff else Icons.Filled.Wifi,
                contentDescription = null,
                tint = if (isOffline) ErrorRed else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(10.dp)
            )
            Icon(Icons.Filled.NetworkCell, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(10.dp))
            Icon(Icons.Filled.BatteryFull, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(11.dp))
        }
    }
}

@Composable
private fun AdaptiveWebPhoneContent(
    project: AppProject,
    currentPhase: AdaptivePhase,
    previewKind: PhasePreviewKind,
    appAccent: Color,
    phaseAccent: Color,
    isOfflineSim: Boolean,
    reloadTrigger: Int,
    jsCommandToRun: Pair<Int, String>?,
    activeNavTab: Int,
    isPullRefreshing: Boolean,
    isFabMenuOpen: Boolean,
    onRetryOnline: () -> Unit,
    onSelectNavTab: (Int, String) -> Unit,
    onToggleDrawer: () -> Unit,
    onToggleFab: () -> Unit,
    onTriggerPullRefresh: () -> Unit,
    onFocusDomElement: (js: String, label: String) -> Unit,
    onActionNotice: (String) -> Unit,
    onWebLog: (String) -> Unit
) {
    val showAppHeader = !project.webViewConfig.fullScreen
    val showBottomNav = project.bottomNavConfig.enabled && !project.webViewConfig.fullScreen

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // شريط التطبيق العلوي المتفاعل مع الهوية والقائمة الجانبية والسحب للتحديث
            if (showAppHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF121624),
                                    appAccent.copy(alpha = if (project.omniConfig.gradientEnabled) 0.25f else 0.12f)
                                )
                            )
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (project.proConfig.drawerEnabled) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "فتح القائمة الجانبية",
                                tint = appAccent,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(onClick = onToggleDrawer)
                            )
                        }
                        RealAppIconAvatar(project = project, size = 20.dp, cornerRadius = 6.dp)
                        Text(
                            text = project.name,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (project.omniConfig.appAcceleratorEnabled) {
                            Surface(color = StatusGreen.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    text = "⚡ سريع",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 8.sp,
                                    color = StatusGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (project.webViewConfig.pullToRefresh) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "تحديث",
                                tint = CyanAccent,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable(onClick = onTriggerPullRefresh)
                            )
                        }
                    }
                }
            }

            // شريط سياق المرحلة التفاعلي (يتغير محتواه كلياً حسب المرحلة!)
            PhaseContextBannerInPhone(
                project = project,
                currentPhase = currentPhase,
                previewKind = previewKind,
                phaseAccent = phaseAccent,
                appAccent = appAccent,
                onFocusDomElement = onFocusDomElement,
                onActionNotice = onActionNotice
            )

            // محتوى الويب التفاعلي الحي
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                key(project.id) {
                    LiveWebViewContainer(
                        project = project,
                        isOfflineMode = isOfflineSim,
                        reloadTrigger = reloadTrigger,
                        jsCommandToRun = jsCommandToRun,
                        accentColor = appAccent,
                        onRetryOnline = onRetryOnline,
                        onLog = { _, msg -> onWebLog(msg) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isPullRefreshing) {
                    LinearProgressIndicator(
                        color = appAccent,
                        trackColor = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                    )
                }

                // زر التواصل العائم الحقيقي داخل شاشة الهاتف
                if (project.proConfig.fabMenuEnabled) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                    ) {
                        AnimatedVisibility(visible = isFabMenuOpen) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = Color(0xEE181C2A),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, appAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        onToggleFab()
                                        onActionNotice("مشاركة رابط ${project.name} ✓")
                                    }
                                ) {
                                    Text("مشاركة سريعة", fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    color = Color(0xEE181C2A),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.clickable {
                                        onToggleFab()
                                        onActionNotice("فتح محادثة الدعم الفوري")
                                    }
                                ) {
                                    Text("دعم مباشر", fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                        FloatingActionButton(
                            onClick = onToggleFab,
                            containerColor = appAccent,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isFabMenuOpen) Icons.Filled.Close else Icons.Filled.Add,
                                contentDescription = "زر الإجراء العائم",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // شريط التنقل السفلي الأصلي للتطبيق (Bottom Navigation Bar)
            if (showBottomNav) {
                val configured = project.bottomNavConfig.destinations
                val tabs = if (configured.size >= 2) {
                    configured.take(4)
                } else {
                    listOf(
                        com.example.domain.model.BottomNavDestination("1", configured.firstOrNull()?.title ?: "الرئيسية", "/", "home"),
                        com.example.domain.model.BottomNavDestination("2", "الأقسام", "/categories", "explore"),
                        com.example.domain.model.BottomNavDestination("3", "المفضلة", "/favorites", "favorite"),
                        com.example.domain.model.BottomNavDestination("4", "حسابي", "/profile", "person")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Color(0xFF10131F))
                        .border(0.5.dp, appAccent.copy(alpha = 0.25f))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { idx, item ->
                        val active = idx == activeNavTab
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) appAccent.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onSelectNavTab(idx, item.title) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = when (item.iconName.lowercase()) {
                                    "explore", "search" -> Icons.Filled.Explore
                                    "favorite", "heart" -> Icons.Filled.Favorite
                                    "person", "account" -> Icons.Filled.Person
                                    else -> Icons.Filled.Home
                                },
                                contentDescription = item.title,
                                tint = if (active) appAccent else TextMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = item.title,
                                fontFamily = CairoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) appAccent else TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhaseContextBannerInPhone(
    project: AppProject,
    currentPhase: AdaptivePhase,
    previewKind: PhasePreviewKind,
    phaseAccent: Color,
    appAccent: Color,
    onFocusDomElement: (js: String, label: String) -> Unit,
    onActionNotice: (String) -> Unit
) {
    when (previewKind) {
        PhasePreviewKind.WEB_IMAGES -> {
            val imgIndices = remember(currentPhase) {
                currentPhase.features
                    .filter { it.bindingKey.startsWith("dom.img.visible.") }
                    .map {
                        val idx = it.bindingKey.substringAfterLast(".").toIntOrNull() ?: 0
                        idx to it.boolValue
                    }
            }
            if (imgIndices.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0E1320))
                ) {
                    itemsIndexed(imgIndices) { _, (idx, visible) ->
                        Surface(
                            color = if (visible) CyanAccent.copy(alpha = 0.16f) else ErrorRed.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (visible) CyanAccent.copy(alpha = 0.5f) else ErrorRed.copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                onFocusDomElement(
                                    "(function(){var imgs=document.querySelectorAll('img');var t=imgs[$idx];if(t){t.scrollIntoView({behavior:'smooth',block:'center'});t.style.outline='3px solid #38BDF8';setTimeout(function(){t.style.outline='';},1400);}})();",
                                    "تركيز على الصورة #${idx + 1}"
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null, tint = if (visible) CyanAccent else ErrorRed, modifier = Modifier.size(11.dp))
                                Text("صورة ${idx + 1}", fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        PhasePreviewKind.WEB_BUTTONS -> {
            val btns = remember(currentPhase) {
                currentPhase.features
                    .filter { it.bindingKey.startsWith("dom.btn.text.") }
                    .map {
                        val idx = it.bindingKey.substringAfterLast(".").toIntOrNull() ?: 0
                        idx to it.textValue
                    }
            }
            if (btns.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16130C))
                ) {
                    itemsIndexed(btns) { _, (idx, label) ->
                        Surface(
                            color = OrangePrimary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.55f)),
                            modifier = Modifier.clickable {
                                onFocusDomElement(
                                    "(function(){var bs=document.querySelectorAll('button,a.btn,.btn,[role=\"button\"]');var t=bs[$idx];if(t){t.scrollIntoView({behavior:'smooth',block:'center'});t.style.outline='3px solid #F59E0B';setTimeout(function(){t.style.outline='';},1400);}})();",
                                    "معاينة الزر: ${label.take(16)}"
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.SmartButton, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(11.dp))
                                Text(label.take(12), fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        PhasePreviewKind.WEB_TEXTS -> {
            val headings = remember(currentPhase) {
                currentPhase.features
                    .filter { it.bindingKey.startsWith("dom.text.replace.") }
                    .map {
                        val idx = it.bindingKey.substringAfterLast(".").toIntOrNull() ?: 0
                        idx to it.textValue
                    }
            }
            if (headings.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF150E20))
                ) {
                    itemsIndexed(headings) { _, (idx, txt) ->
                        Surface(
                            color = Color(0xFFA855F7).copy(alpha = 0.18f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable {
                                onFocusDomElement(
                                    "(function(){var ts=document.querySelectorAll('h1,h2,h3,h4,p');var t=ts[$idx];if(t){t.scrollIntoView({behavior:'smooth',block:'center'});t.style.outline='2px dashed #A855F7';setTimeout(function(){t.style.outline='';},1400);}})();",
                                    "انتقال للنص #${idx + 1}"
                                )
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.TextFields, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(11.dp))
                                Text(txt.take(14), fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        PhasePreviewKind.WEB_DESIGN -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14111A))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(appAccent))
                Text(
                    text = "خط ${project.omniConfig.fontScalePercent}% • ${if (project.proConfig.forceDarkCss) "ليلي قسري" else "ثيم متناسق"}",
                    fontFamily = CairoFontFamily,
                    fontSize = 9.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                if (project.omniConfig.gradientEnabled) {
                    Text("تدرج 3D ✓", fontFamily = CairoFontFamily, fontSize = 9.sp, color = CyanAccent)
                }
            }
        }

        PhasePreviewKind.WEB_ENGINE_NAV -> {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E171D))
            ) {
                item {
                    Surface(
                        color = CyanAccent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            onFocusDomElement(
                                "window.ApkForge && ApkForge.vibrate(25); 'BRIDGE_OK';",
                                if (project.bridgeConfig.jsBridgeEnabled) "JS Bridge v2 متصل ✓" else "الجسر البرمجي معطّل"
                            )
                        }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.Code, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(11.dp))
                            Text("اختبار JS Bridge", fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
                item {
                    Surface(
                        color = (if (project.omniConfig.localDbEnabled) StatusGreen else TextMuted).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable {
                            onActionNotice(if (project.omniConfig.localDbEnabled) "قاعدة البيانات المحلية جاهزة (KV DB)" else "فعّل قاعدة البيانات من الأسفل")
                        }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Icon(Icons.Filled.Storage, contentDescription = null, tint = if (project.omniConfig.localDbEnabled) StatusGreen else TextMuted, modifier = Modifier.size(11.dp))
                            Text(if (project.omniConfig.localDbEnabled) "DB نشطة" else "DB متوقفة", fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
                if (project.omniConfig.apiBaseUrl.isNotBlank()) {
                    item {
                        Surface(
                            color = OrangePrimary.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable { onActionNotice("REST API: ${project.omniConfig.apiBaseUrl.take(24)}") }
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                Icon(Icons.Filled.Http, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(11.dp))
                                Text("API متصل", fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0E111B))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentPhase.title,
                    fontFamily = CairoFontFamily,
                    fontSize = 9.sp,
                    color = phaseAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (project.omniConfig.customHeroTitle.isNotBlank()) {
                    Text(
                        text = "Hero مخصص ✓",
                        fontFamily = CairoFontFamily,
                        fontSize = 8.sp,
                        color = StatusGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun AdaptiveNativePhasePhoneContent(
    project: AppProject,
    currentPhase: AdaptivePhase,
    previewKind: PhasePreviewKind,
    appAccent: Color,
    phaseAccent: Color,
    authLoggedInUser: String?,
    grantedSimPermissions: Set<String>,
    onLoginDemoUser: (String) -> Unit,
    onLogoutDemoUser: () -> Unit,
    onRequestPermDialog: (String) -> Unit,
    onTriggerPush: () -> Unit,
    onTestHardening: (String) -> Unit,
    onOpenFullSandbox: () -> Unit
) {
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F1320), Color(0xFF090B11))
                )
            )
            .verticalScroll(scroll)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (previewKind) {
            PhasePreviewKind.APK_IDENTITY -> {
                // شاشة هاتف حقيقية تفاعلية لهوية APK وأيقونته و Splash
                Surface(
                    color = Color(0xFF151A2A),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, appAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RealAppIconAvatar(project = project, size = 54.dp, cornerRadius = 15.dp)
                        Text(
                            text = project.name,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = project.packageConfig.packageName,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = CyanAccent
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = appAccent.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    text = if (project.splashConfig.enabled) "Splash مفعل ✓" else "Splash معطل",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 9.sp,
                                    color = appAccent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = StatusGreen.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable(onClick = onOpenFullSandbox)
                            ) {
                                Text(
                                    text = "تشغيل تجريبي ▶",
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = StatusGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            PhasePreviewKind.APK_PACKAGE -> {
                val signed = !project.signingConfig.useDebugKey && project.signingConfig.keystorePath.isNotBlank()
                Surface(
                    color = Color(0xFF151A2A),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Android, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("معلومات التطبيق (App Info)", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                Text(project.packageConfig.packageName, fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = CyanAccent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoMiniPill("الإصدار", project.packageConfig.versionName, OrangePrimary, Modifier.weight(1f))
                            InfoMiniPill("Build", "${project.packageConfig.versionCode}", CyanAccent, Modifier.weight(1f))
                            InfoMiniPill("التوقيع", if (signed) "AES-256 ✓" else "تلقائي", if (signed) StatusGreen else TextSecondary, Modifier.weight(1f))
                        }
                        FilledTonalButton(
                            onClick = { onTestHardening("الحزمة ${project.packageConfig.packageName} جاهزة للتجميع ✓") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("فحص توافق الحزمة v1-v4", fontFamily = CairoFontFamily, fontSize = 10.sp)
                        }
                    }
                }
            }

            PhasePreviewKind.APK_PERMISSIONS -> {
                Text(
                    text = "أذونات التطبيق النشطة (اضغط لاختبار نافذة أندرويد):",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextPrimary
                )
                val perms = listOf(
                    Triple("الكاميرا", Icons.Filled.CameraAlt, project.permissionsConfig.camera),
                    Triple("الموقع الجغرافي", Icons.Filled.LocationOn, project.permissionsConfig.location),
                    Triple("الميكروفون", Icons.Filled.Mic, project.permissionsConfig.microphone),
                    Triple("الإشعارات", Icons.Filled.Notifications, project.permissionsConfig.notifications),
                    Triple("البلوتوث", Icons.Filled.Bluetooth, project.permissionsConfig.bluetooth),
                    Triple("حالة الشبكة", Icons.Filled.Wifi, project.permissionsConfig.networkState)
                )
                perms.forEach { (label, icon, enabledInManifest) ->
                    val simGranted = label in grantedSimPermissions
                    Surface(
                        color = if (enabledInManifest) Color(0xFF172030) else Color(0xFF13151F),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (enabledInManifest) appAccent.copy(alpha = 0.45f) else GlassBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = enabledInManifest) { onRequestPermDialog(label) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(icon, contentDescription = null, tint = if (enabledInManifest) appAccent else TextMuted, modifier = Modifier.size(16.dp))
                            Text(label, fontFamily = CairoFontFamily, fontSize = 11.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text(
                                text = when {
                                    !enabledInManifest -> "غير مدرج"
                                    simGranted -> "مسموح ✓"
                                    else -> "اختبار 👆"
                                },
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = when {
                                    !enabledInManifest -> TextMuted
                                    simGranted -> StatusGreen
                                    else -> CyanAccent
                                }
                            )
                        }
                    }
                }
            }

            PhasePreviewKind.APK_HARDENING -> {
                HardeningMonitorCard(
                    project = project,
                    appAccent = appAccent,
                    onTestHardening = onTestHardening,
                    onTriggerRationalePerm = { onRequestPermDialog("الكاميرا") }
                )
            }

            PhasePreviewKind.NATIVE_SECURITY_PERMS -> {
                // مرحلة الأمان / المصادقة / الإشعارات / الحزم لـ Web و AI
                if (project.omniConfig.authBuilderEnabled) {
                    Surface(
                        color = Color(0xFF151A2A),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, appAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = appAccent, modifier = Modifier.size(18.dp))
                                Text("شاشة تسجيل الدخول التفاعلية", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                            }
                            if (authLoggedInUser != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("مسجّل باسم: $authLoggedInUser ✓", fontFamily = CairoFontFamily, fontSize = 10.sp, color = StatusGreen)
                                    FilledTonalButton(onClick = onLogoutDemoUser, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                                        Text("خروج", fontFamily = CairoFontFamily, fontSize = 9.sp)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onLoginDemoUser("مستخدم تجريبي") },
                                    colors = ButtonDefaults.buttonColors(containerColor = appAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(34.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("تجربة تسجيل الدخول عبر Google", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                HardeningMonitorCard(
                    project = project,
                    appAccent = phaseAccent,
                    onTestHardening = onTestHardening,
                    onTriggerRationalePerm = {
                        val activePerm = when {
                            project.permissionsConfig.camera -> "الكاميرا"
                            project.permissionsConfig.location -> "الموقع الجغرافي"
                            project.permissionsConfig.microphone -> "الميكروفون"
                            else -> "الإشعارات"
                        }
                        onRequestPermDialog(activePerm)
                    }
                )

                if (project.pushConfig.enabled) {
                    FilledTonalButton(
                        onClick = onTriggerPush,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("إرسال إشعار فوري تجريبي على الهاتف", fontFamily = CairoFontFamily, fontSize = 10.sp)
                    }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun HardeningMonitorCard(
    project: AppProject,
    appAccent: Color,
    onTestHardening: (String) -> Unit,
    onTriggerRationalePerm: () -> Unit
) {
    Surface(
        color = Color(0xFF151927),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, appAccent.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = appAccent, modifier = Modifier.size(16.dp))
                Text("حالة الحماية والأذونات المباشرة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoMiniPill("FLAG_SECURE", if (project.proConfig.flagSecureEnabled) "محمي ✓" else "عادي", if (project.proConfig.flagSecureEnabled) StatusGreen else TextMuted, Modifier.weight(1f))
                InfoMiniPill("SSL Pinning", if (project.proConfig.sslPinningEnabled) "مشدد ✓" else "قياسي", if (project.proConfig.sslPinningEnabled) CyanAccent else TextMuted, Modifier.weight(1f))
                InfoMiniPill("توضيح الأذونات", if (project.omniConfig.smartPermissionsRationaleEnabled) "ذكي ✓" else "نظام", if (project.omniConfig.smartPermissionsRationaleEnabled) OrangePrimary else TextMuted, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onTriggerRationalePerm,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("اختبار إذن", fontFamily = CairoFontFamily, fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        onTestHardening(
                            if (project.proConfig.sslPinningEnabled) "تم التحقق من شهادة TLS Pinning بنجاح ✓"
                            else "اتصال HTTPS قياسي نشط"
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = appAccent),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("فحص SSL", fontFamily = CairoFontFamily, fontSize = 10.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun InfoMiniPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontFamily = TajawalFontFamily, fontSize = 8.sp, color = TextSecondary, maxLines = 1)
            Text(value, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun PhaseLauncherHomeContent(
    project: AppProject,
    accent: Color,
    onLaunchApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF020617)))
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("12:00", fontSize = 34.sp, fontWeight = FontWeight.Light, color = Color.White)
        Text("اضغط على أيقونة تطبيقك لتشغيله", fontFamily = TajawalFontFamily, fontSize = 10.sp, color = TextSecondary)
        Spacer(Modifier.height(18.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onLaunchApp)
                .padding(10.dp)
        ) {
            RealAppIconAvatar(project = project, size = 56.dp, cornerRadius = 16.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = project.name,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = "فتح التطبيق ▶",
                fontFamily = CairoFontFamily,
                fontSize = 9.sp,
                color = accent
            )
        }
    }
}

@Composable
private fun PhaseSplashContent(
    project: AppProject,
    accent: Color
) {
    val bgColor = parseColorSafe(project.splashConfig.backgroundColorHex, Color(0xFF0F1318))
    val inf = rememberInfiniteTransition(label = "phase_splash")
    val pulse by inf.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow((14 * pulse).dp, RoundedCornerShape(18.dp), spotColor = accent)
            ) {
                RealAppIconAvatar(project = project, size = 60.dp, cornerRadius = 18.dp)
            }
            Text(
                text = project.name,
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color.White
            )
            CircularProgressIndicator(
                color = accent,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
