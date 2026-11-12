package com.example.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.engine.ExtractedAssetItem
import com.example.engine.LocalWorkspaceEngine
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    fileName: String = "index.html",
    workspacePath: String? = null,
    entryHtmlPath: String? = null,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Real High-Fidelity WebView, 1 = Extracted Assets Tree
    var desktopMode by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf(fileName) }
    var pageProgress by remember { mutableIntStateOf(100) }
    var showSplashFadeVeil by remember { mutableStateOf(true) }
    var inspectedFile by remember { mutableStateOf<Pair<ExtractedAssetItem, String>?>(null) }

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
            containerColor = DarkBackground,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated)
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "معاينة حية حقيقية",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "$pageTitle • ${realAssets.count { !it.isDirectory }} ملف",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = StatusGreen,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(onClick = { desktopMode = !desktopMode }) {
                                Icon(
                                    imageVector = if (desktopMode) Icons.Default.PhoneAndroid else Icons.Default.DesktopWindows,
                                    contentDescription = "تبديل وضع العرض",
                                    tint = if (desktopMode) OrangePrimary else TextSecondary
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    showSplashFadeVeil = true
                                    webViewRef?.reload()
                                    delay(500)
                                    showSplashFadeVeil = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "إعادة تحميل",
                                    tint = OrangePrimary
                                )
                            }
                        }
                    }

                    // Dual Switcher Tabs
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
                        .background(DarkSurfaceElevated)
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (resolvedEntryFile != null) {
                            "معاينة عالية الدقة للملف الحقيقي: ${resolvedEntryFile.name}"
                        } else {
                            "يرجى اختيار ملف HTML أو ZIP للمعاينة الحقيقية"
                        },
                        fontFamily = TajawalFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("preview_screen")
            ) {
                if (selectedTab == 0) {
                    if (resolvedEntryFile != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                update = { wv ->
                                    wv.settings.useWideViewPort = desktopMode
                                    wv.settings.loadWithOverviewMode = true
                                },
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.databaseEnabled = true
                                        settings.allowFileAccess = true
                                        settings.allowContentAccess = true
                                        @Suppress("DEPRECATION")
                                        settings.allowFileAccessFromFileURLs = true
                                        @Suppress("DEPRECATION")
                                        settings.allowUniversalAccessFromFileURLs = true
                                        settings.useWideViewPort = true
                                        settings.loadWithOverviewMode = true
                                        settings.builtInZoomControls = true
                                        settings.displayZoomControls = false
                                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                pageProgress = newProgress
                                            }

                                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                                if (!title.isNullOrBlank()) pageTitle = title
                                            }
                                        }

                                        webViewClient = object : WebViewClient() {
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                showSplashFadeVeil = false
                                            }
                                        }

                                        loadUrl(Uri.fromFile(resolvedEntryFile).toString())
                                        webViewRef = this
                                    }
                                }
                            )

                            if (pageProgress < 100) {
                                LinearProgressIndicator(
                                    progress = { pageProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                                    color = OrangePrimary,
                                    trackColor = Color.Transparent
                                )
                            }

                            // Smooth animated crossfade veil transition when opening imported page
                            if (splashVeilAlpha > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(splashVeilAlpha)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(DarkBackground, Color(0xFF141B29), DarkBackground)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = OrangePrimary, strokeWidth = 3.dp)
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "جارٍ تشغيل ${resolvedEntryFile.name} بأعلى دقة...",
                                            fontFamily = CairoFontFamily,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم يتم العثور على صفحة entry HTML صالحة داخل هذا المسار.",
                                fontFamily = CairoFontFamily,
                                fontSize = 14.sp,
                                color = WarningYellow
                            )
                        }
                    }
                } else {
                    // Extracted Files & Real Content Inspector
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (realAssets.isEmpty()) {
                            item {
                                Text(
                                    text = "لا توجد ملفات مستخرجة لعرضها.",
                                    fontFamily = TajawalFontFamily,
                                    fontSize = 13.sp,
                                    color = TextMuted
                                )
                            }
                        } else {
                            items(realAssets, key = { it.relativePath }) { asset ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurfaceElevated)
                                        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
                                        .clickable(enabled = !asset.isDirectory) {
                                            val rootDir = workspacePath?.let { File(it) } ?: return@clickable
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
                                        tint = if (asset.isDirectory) GoldAccent else OrangePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = asset.relativePath,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (asset.isDirectory) "مجلد" else "${(asset.sizeBytes / 1024.0).let { "%.1f KB".format(it) }} • ${asset.extension.uppercase()}",
                                            fontFamily = TajawalFontFamily,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    if (!asset.isDirectory) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
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
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { inspectedFile = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.75f)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.relativePath, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                        IconButton(onClick = { inspectedFile = null }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                        }
                    }
                    Surface(
                        color = TerminalBackground,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        Text(
                            text = contentText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TerminalGreen,
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp)
                        )
                    }
                }
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
        color = if (selected) OrangePrimary.copy(alpha = 0.2f) else DarkSurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) OrangePrimary else GlassBorderSubtle),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(
                text = title,
                fontFamily = CairoFontFamily,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                color = if (selected) OrangePrimary else TextSecondary
            )
        }
    }
}
