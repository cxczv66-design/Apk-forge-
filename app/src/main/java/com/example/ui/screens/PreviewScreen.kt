package com.example.ui.screens

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.engine.ExtractedAssetItem
import com.example.engine.LocalWorkspaceEngine
import com.example.ui.components.WebViewSafety
import com.example.ui.components.space.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections
import java.util.WeakHashMap

/**
 * شاشة المعاينة الحية بإطار هاتف زجاجي فضائي ثلاثي الأبعاد:
 * - تحافظ 100% على WebView الحقيقي، Desktop/Mobile Mode، Reload، مستكشف الملفات، وتعافي الـ Renderer
 * - بدون أي دوران 3D حول WebView لضمان عدم حدوث أعطال Chromium Renderer
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    fileName: String = "index.html",
    workspacePath: String? = null,
    entryHtmlPath: String? = null,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var desktopMode by remember { mutableStateOf(false) }
    var fullFrameMode by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf(fileName) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var showSplashFadeVeil by remember { mutableStateOf(true) }
    var inspectedFile by remember { mutableStateOf<Pair<ExtractedAssetItem, String>?>(null) }
    var rendererSessionKey by remember { mutableIntStateOf(0) }
    var rendererCrashCount by remember { mutableIntStateOf(0) }
    var useSoftwareLayer by remember { mutableStateOf(WebViewSafety.shouldDefaultToSoftwareLayer()) }
    var isRendererFailed by remember { mutableStateOf(WebViewSafety.isSessionRendererBlocked()) }
    var pendingRendererRecovery by remember { mutableStateOf(false) }
    val destroyedWebViews = remember { Collections.newSetFromMap(WeakHashMap<WebView, Boolean>()) }

    fun destroyWebViewSafely(wv: WebView?) {
        WebViewSafety.destroyWebViewSafely(wv, destroyedWebViews)
    }

    fun triggerRendererRecovery() {
        showSplashFadeVeil = false
        val canRetrySoftware = !useSoftwareLayer && rendererCrashCount < 1 && !WebViewSafety.isSessionRendererBlocked()
        if (canRetrySoftware) {
            rendererCrashCount++
            useSoftwareLayer = true
            mainHandler.postDelayed({ rendererSessionKey++ }, 350)
        } else {
            isRendererFailed = true
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    if (pendingRendererRecovery) {
                        pendingRendererRecovery = false
                        triggerRendererRecovery()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> {
                    mainHandler.removeCallbacksAndMessages(null)
                    destroyWebViewSafely(webViewRef)
                    webViewRef = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            mainHandler.removeCallbacksAndMessages(null)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val resolvedEntryFile = remember(workspacePath, entryHtmlPath) {
        when {
            !entryHtmlPath.isNullOrBlank() && File(entryHtmlPath).exists() -> File(entryHtmlPath)
            !workspacePath.isNullOrBlank() -> LocalWorkspaceEngine.findEntryHtml(File(workspacePath))
            else -> null
        }
    }

    val realAssets = remember(workspacePath) {
        LocalWorkspaceEngine.listWorkspaceAssets(workspacePath)
    }

    LaunchedEffect(resolvedEntryFile) {
        showSplashFadeVeil = true
        delay(450)
        showSplashFadeVeil = false
    }

    val splashVeilAlpha by animateFloatAsState(
        targetValue = if (showSplashFadeVeil) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "previewSplashFade"
    )

    BackHandler {
        if (webViewRef?.canGoBack() == true && selectedTab == 0) {
            webViewRef?.goBack()
        } else {
            onBack()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeonIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            onClick = onBack,
                            borderAccent = SpaceColors.NeonCyan
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "معاينة حية حقيقية",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = SpaceColors.White
                            )
                            Text(
                                text = "$pageTitle • ${realAssets.count { !it.isDirectory }} ملف",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = SpaceColors.Emerald,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NeonIconButton(
                                icon = if (fullFrameMode) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "ملء الشاشة",
                                tint = if (fullFrameMode) SpaceColors.NeonCyan else SpaceColors.MutedText,
                                borderAccent = SpaceColors.NeonPurple,
                                onClick = { fullFrameMode = !fullFrameMode }
                            )
                            NeonIconButton(
                                icon = if (desktopMode) Icons.Default.PhoneAndroid else Icons.Default.DesktopWindows,
                                contentDescription = "تبديل وضع العرض",
                                tint = if (desktopMode) SpaceColors.Gold else SpaceColors.MutedText,
                                borderAccent = SpaceColors.ElectricBlue,
                                onClick = { desktopMode = !desktopMode }
                            )
                            NeonIconButton(
                                icon = Icons.Default.Refresh,
                                contentDescription = "إعادة تحميل",
                                tint = SpaceColors.NeonCyan,
                                borderAccent = SpaceColors.NeonCyan,
                                onClick = {
                                    scope.launch {
                                        showSplashFadeVeil = true
                                        webViewRef?.reload()
                                        delay(500)
                                        showSplashFadeVeil = false
                                    }
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PreviewTabPill(
                            title = "عرض الموقع الفعلي 🌐",
                            selected = selectedTab == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 0 }
                        )
                        PreviewTabPill(
                            title = "ملفات الأرشيف (${realAssets.size}) 📁",
                            selected = selectedTab == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedTab = 1 }
                        )
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
                                listOf(SpaceColors.MidnightBlue.copy(alpha = 0.94f), SpaceColors.DeepSpace)
                            )
                        )
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(SpaceColors.NeonCyan.copy(alpha = 0.45f), SpaceColors.NeonPurple.copy(alpha = 0.45f))
                            ),
                            RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (resolvedEntryFile != null) SpaceColors.Emerald else SpaceColors.Gold)
                        )
                        Text(
                            text = if (resolvedEntryFile != null) {
                                "معاينة حية: ${resolvedEntryFile.name} • ${if (desktopMode) "سطح المكتب" else "محاكي هاتف"}"
                            } else {
                                "يرجى اختيار ملف HTML أو ZIP للمعاينة الحقيقية"
                            },
                            fontFamily = TajawalFontFamily,
                            fontSize = 12.sp,
                            color = SpaceColors.MutedText
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = if (fullFrameMode) 4.dp else 14.dp, vertical = if (fullFrameMode) 4.dp else 10.dp)
                    .testTag("preview_screen"),
                contentAlignment = Alignment.Center
            ) {
                if (selectedTab == 0) {
                    if (resolvedEntryFile != null) {
                        if (isRendererFailed) {
                            NeonGlassCard(
                                accentColor = SpaceColors.ErrorRed,
                                secondaryAccent = SpaceColors.Gold
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Space3DFeatureBadge(
                                        icon = Icons.Default.Refresh,
                                        accentColor = SpaceColors.NeonCyan,
                                        size = 56.dp
                                    )
                                    Text(
                                        text = "توقف محرك المعاينة مؤقتاً",
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = SpaceColors.White
                                    )
                                    NeonButton(
                                        text = "إعادة تحميل المعاينة",
                                        icon = Icons.Default.Refresh,
                                        style = NeonButtonStyle.PRIMARY_GLOW,
                                        onClick = {
                                            rendererCrashCount = 0
                                            isRendererFailed = false
                                            rendererSessionKey++
                                        }
                                    )
                                }
                            }
                        } else {
                            // إطار هاتف زجاجي فضائي (بدون دوران 3D حول WebView لتفادي أعطال Chromium)
                            val phoneCorner = if (fullFrameMode) 18.dp else 30.dp
                            val bezelPadding = if (fullFrameMode) 4.dp else 8.dp
                            Box(
                                modifier = Modifier
                                    .widthIn(max = if (desktopMode || fullFrameMode) 900.dp else 430.dp)
                                    .fillMaxSize()
                                    .shadow(
                                        elevation = 20.dp,
                                        shape = RoundedCornerShape(phoneCorner),
                                        spotColor = SpaceColors.NeonCyan.copy(alpha = 0.45f),
                                        ambientColor = SpaceColors.NeonPurple.copy(alpha = 0.3f)
                                    )
                                    .clip(RoundedCornerShape(phoneCorner))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(SpaceColors.MidnightBlue, SpaceColors.DeepSpace)
                                        )
                                    )
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                SpaceColors.NeonCyan.copy(alpha = 0.85f),
                                                SpaceColors.ElectricBlue.copy(alpha = 0.45f),
                                                SpaceColors.NeonPurple.copy(alpha = 0.80f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(phoneCorner)
                                    )
                                    .padding(bezelPadding)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape((phoneCorner.value - 6f).coerceAtLeast(8f).dp))
                                        .background(Color(0xFF0A0F1D))
                                ) {
                                    if (!fullFrameMode) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(SpaceColors.DeepSpace)
                                                .padding(horizontal = 14.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "12:45",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = SpaceColors.White.copy(alpha = 0.85f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(46.dp)
                                                    .height(10.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.Black)
                                                    .border(0.6.dp, SpaceColors.NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.Wifi, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(11.dp))
                                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = SpaceColors.Emerald, modifier = Modifier.size(11.dp))
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                        if (isRendererFailed) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "توقفت جلسة المعاينة مؤقتاً",
                                                    fontFamily = CairoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = SpaceColors.White
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "تمت حماية التطبيق من الإغلاق ويمكنك إعادة تشغيل المعاينة الآن",
                                                    fontFamily = TajawalFontFamily,
                                                    fontSize = 12.sp,
                                                    color = SpaceColors.MutedText
                                                )
                                                Spacer(modifier = Modifier.height(14.dp))
                                                NeonButton(
                                                    text = "إعادة تشغيل المعاينة",
                                                    onClick = {
                                                        WebViewSafety.resetRendererSession()
                                                        rendererCrashCount = 0
                                                        isRendererFailed = false
                                                        useSoftwareLayer = true
                                                        rendererSessionKey++
                                                    }
                                                )
                                            }
                                        } else {
                                        key(rendererSessionKey) {
                                            AndroidView(
                                                modifier = Modifier.fillMaxSize(),
                                                onRelease = { wv ->
                                                    destroyWebViewSafely(wv)
                                                    if (webViewRef === wv) webViewRef = null
                                                },
                                                update = { wv ->
                                                    if (!destroyedWebViews.contains(wv) &&
                                                        wv.isAttachedToWindow &&
                                                        wv.settings.useWideViewPort != desktopMode
                                                    ) {
                                                        wv.settings.useWideViewPort = desktopMode
                                                        wv.settings.loadWithOverviewMode = true
                                                    }
                                                },
                                                factory = { ctx ->
                                                    WebView(ctx).apply {
                                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                        )
                                                        setBackgroundColor(android.graphics.Color.parseColor("#080E1E"))
                                                        WebViewSafety.applySafeWebSettings(this, useSoftwareLayer)
                                                        settings.javaScriptEnabled = true
                                                        settings.domStorageEnabled = true
                                                        settings.databaseEnabled = true
                                                        settings.allowFileAccess = true
                                                        settings.allowContentAccess = true
                                                        @Suppress("DEPRECATION")
                                                        settings.allowFileAccessFromFileURLs = true
                                                        @Suppress("DEPRECATION")
                                                        settings.allowUniversalAccessFromFileURLs = true
                                                        settings.useWideViewPort = desktopMode
                                                        settings.loadWithOverviewMode = true
                                                        settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                                                        settings.builtInZoomControls = true
                                                        settings.displayZoomControls = false
                                                        settings.setGeolocationEnabled(false)
                                                        settings.mediaPlaybackRequiresUserGesture = true
                                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                                                        webChromeClient = object : WebChromeClient() {
                                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                                if (view != null && !destroyedWebViews.contains(view)) {
                                                                    pageProgress = newProgress
                                                                }
                                                            }

                                                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                                                if (view != null && !destroyedWebViews.contains(view) && !title.isNullOrBlank()) {
                                                                    pageTitle = title
                                                                }
                                                            }
                                                        }

                                                        webViewClient = object : WebViewClient() {
                                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                                if (view != null && !destroyedWebViews.contains(view)) {
                                                                    showSplashFadeVeil = false
                                                                }
                                                            }

                                                            @TargetApi(Build.VERSION_CODES.O)
                                                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                                                WebViewSafety.recordRendererCrash()
                                                                mainHandler.post {
                                                                    destroyWebViewSafely(view)
                                                                    if (webViewRef === view) webViewRef = null
                                                                    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                                                                        triggerRendererRecovery()
                                                                    } else {
                                                                        pendingRendererRecovery = true
                                                                    }
                                                                }
                                                                return true
                                                            }
                                                        }

                                                        loadUrl(Uri.fromFile(resolvedEntryFile).toString())
                                                        webViewRef = this
                                                    }
                                                }
                                            )
                                        }
                                        }

                                        if (pageProgress < 100) {
                                            NeonProgressIndicator(
                                                progress = pageProgress / 100f,
                                                height = 3.dp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.TopCenter)
                                            )
                                        }

                                        if (splashVeilAlpha > 0.01f) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .alpha(splashVeilAlpha)
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(SpaceColors.DeepSpace, SpaceColors.MidnightBlue, SpaceColors.DeepSpace)
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    SpaceQuantumAtomIcon(size = 44.dp, tint = SpaceColors.NeonCyan)
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    Text(
                                                        text = "جارٍ تشغيل ${resolvedEntryFile.name} بأعلى دقة...",
                                                        fontFamily = CairoFontFamily,
                                                        fontSize = 13.sp,
                                                        color = SpaceColors.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        NeonGlassCard(accentColor = SpaceColors.Gold) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لم يتم العثور على صفحة entry HTML صالحة داخل هذا المسار.",
                                    fontFamily = CairoFontFamily,
                                    fontSize = 14.sp,
                                    color = SpaceColors.Gold
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = SpaceDimensions.MaxContentWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        if (realAssets.isEmpty()) {
                            item {
                                Text(
                                    text = "لا توجد ملفات مستخرجة لعرضها.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp,
                                    color = SpaceColors.MutedText
                                )
                            }
                        } else {
                            items(realAssets, key = { it.relativePath }) { asset ->
                                NeonGlassCard(
                                    accentColor = if (asset.isDirectory) SpaceColors.Gold else SpaceColors.NeonCyan,
                                    secondaryAccent = SpaceColors.ElectricBlue,
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = 6.dp,
                                    onClick = if (!asset.isDirectory) {
                                        {
                                            val rootDir = workspacePath?.let { File(it) }
                                            if (rootDir != null) {
                                                val target = File(rootDir, asset.relativePath)
                                                val previewText = if (target.exists() && target.length() < 120_000 &&
                                                    asset.extension in setOf("html", "htm", "css", "js", "json", "txt", "svg", "xml", "md")
                                                ) {
                                                    target.readText(Charsets.UTF_8).take(12_000)
                                                } else {
                                                    "ملف ثنائي/وسائط (${asset.sizeBytes} بايت) • ${asset.relativePath}"
                                                }
                                                inspectedFile = asset to previewText
                                            }
                                        }
                                    } else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                asset.isDirectory -> Icons.Default.Folder
                                                asset.extension in setOf("html", "htm") -> Icons.Default.Html
                                                asset.extension in setOf("png", "jpg", "jpeg", "webp", "svg") -> Icons.Default.Image
                                                else -> Icons.Default.Description
                                            },
                                            contentDescription = null,
                                            tint = if (asset.isDirectory) SpaceColors.Gold else SpaceColors.NeonCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = asset.relativePath,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = SpaceColors.White
                                            )
                                            Text(
                                                text = if (asset.isDirectory) "مجلد" else "${(asset.sizeBytes / 1024.0).let { "%.1f KB".format(it) }} • ${asset.extension.uppercase()}",
                                                fontFamily = TajawalFontFamily,
                                                fontSize = 10.sp,
                                                color = SpaceColors.MutedText
                                            )
                                        }
                                        if (!asset.isDirectory) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SpaceColors.NeonCyan, modifier = Modifier.size(18.dp))
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

    inspectedFile?.let { (item, contentText) ->
        SpaceDialog(
            title = item.relativePath,
            accentColor = SpaceColors.NeonCyan,
            onDismissRequest = { inspectedFile = null }
        ) {
            Surface(
                color = SpaceColors.DeepSpace,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, SpaceColors.NeonCyan.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                Text(
                    text = contentText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = SpaceColors.Emerald,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewTabPill(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) SpaceColors.NeonCyan.copy(alpha = 0.22f) else SpaceColors.SurfaceElevated.copy(alpha = 0.72f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) SpaceColors.NeonCyan else SpaceColors.GlassBorderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                color = if (selected) SpaceColors.NeonCyan else SpaceColors.MutedText
            )
        }
    }
}
