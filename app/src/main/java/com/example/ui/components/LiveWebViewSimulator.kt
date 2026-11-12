package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType
import com.example.ui.theme.*
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ConsoleLevel(val tag: String, val color: Color) {
    INFO("INFO", Color(0xFF38BDF8)),
    BRIDGE("BRIDGE", Color(0xFFFF7A2F)),
    NETWORK("NET", Color(0xFF10B981)),
    WARN("WARN", Color(0xFFF59E0B)),
    ERROR("ERR", Color(0xFFEF4444))
}

data class DevConsoleEntry(
    val id: String,
    val level: ConsoleLevel,
    val message: String,
    val time: String
)

private class PreviewJsBridge(
    private val context: Context,
    private val isOffline: () -> Boolean,
    private val onLog: (ConsoleLevel, String) -> Unit
) {
    @JavascriptInterface
    fun showToast(msg: String) {
        onLog(ConsoleLevel.BRIDGE, "ApkForge.showToast(\"$msg\")")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun vibrate(ms: Long) {
        onLog(ConsoleLevel.BRIDGE, "ApkForge.vibrate($ms ms)")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (v != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms.coerceIn(20, 600), VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } catch (_: Exception) {}
        }
    }

    @JavascriptInterface
    fun share(title: String, text: String) {
        onLog(ConsoleLevel.BRIDGE, "ApkForge.share(\"$title\", \"$text\")")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    @JavascriptInterface
    fun getNetworkStatus(): Boolean {
        val status = !isOffline()
        onLog(ConsoleLevel.BRIDGE, "ApkForge.getNetworkStatus() -> $status")
        return status
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        val info = "{\"brand\":\"${Build.BRAND}\",\"sdk\":${Build.VERSION.SDK_INT}}"
        onLog(ConsoleLevel.BRIDGE, "ApkForge.getDeviceInfo() -> $info")
        return info
    }

    @JavascriptInterface
    fun authenticateBiometric() {
        onLog(ConsoleLevel.BRIDGE, "ApkForge.authenticateBiometric() -> verified:true")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "🔐 تم التحقق بالبصمة بنجاح (محاكاة حية)", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun scanQrCode(): String {
        val demoQr = "APKFORGE-QR-8492"
        onLog(ConsoleLevel.BRIDGE, "ApkForge.scanQrCode() -> \"$demoQr\"")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "📷 نتيجة مسح QR: $demoQr", Toast.LENGTH_SHORT).show()
        }
        return demoQr
    }

    @JavascriptInterface
    fun requestStoreRating() {
        onLog(ConsoleLevel.BRIDGE, "ApkForge.requestStoreRating() invoked")
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, "⭐ تم فتح نافذة تقييم Google Play", Toast.LENGTH_SHORT).show()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveWebViewContainer(
    project: AppProject,
    isOfflineMode: Boolean,
    reloadTrigger: Int,
    jsCommandToRun: Pair<Int, String>?,
    accentColor: Color,
    onRetryOnline: () -> Unit,
    onLog: (ConsoleLevel, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val pro = project.proConfig
    val blockedHosts = remember(pro.blockedHostsCsv) {
        pro.blockedHostsCsv.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
    }

    LaunchedEffect(jsCommandToRun?.first) {
        val cmd = jsCommandToRun?.second
        if (!cmd.isNullOrBlank() && !isOfflineMode) {
            onLog(ConsoleLevel.INFO, "eval> $cmd")
            webViewRef?.evaluateJavascript(cmd) { result ->
                if (!result.isNullOrBlank() && result != "null") {
                    onLog(ConsoleLevel.INFO, "< $result")
                }
            }
        }
    }

    LaunchedEffect(reloadTrigger, isOfflineMode) {
        if (!isOfflineMode) {
            webViewRef?.reload()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0C0E16))) {
        if (isOfflineMode && project.webViewConfig.offlineFallback) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = "غير متصل",
                    tint = OrangePrimary,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "لا يوجد اتصال بالإنترنت",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "شاشة الطوارئ (Offline Fallback) لحماية تجربة المستخدم في تطبيق ${project.name}",
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onLog(ConsoleLevel.NETWORK, "إعادة محاولة الاتصال بالشبكة...")
                        onRetryOnline()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إعادة المحاولة", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportZoom(project.webViewConfig.supportZoom)
                        settings.cacheMode = if (pro.assetPreloaderEnabled) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT

                        if (project.bridgeConfig.jsBridgeEnabled) {
                            addJavascriptInterface(
                                PreviewJsBridge(ctx, { isOfflineMode }, onLog),
                                "ApkForge"
                            )
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                val msg = consoleMessage ?: return false
                                val lvl = when (msg.messageLevel()) {
                                    ConsoleMessage.MessageLevel.ERROR -> ConsoleLevel.ERROR
                                    ConsoleMessage.MessageLevel.WARNING -> ConsoleLevel.WARN
                                    else -> ConsoleLevel.INFO
                                }
                                onLog(lvl, "[JS L${msg.lineNumber()}] ${msg.message()}")
                                return true
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                if (pro.adAndPopupBlocker) {
                                    val host = request?.url?.host?.lowercase() ?: ""
                                    if (blockedHosts.any { host.contains(it) }) {
                                        onLog(ConsoleLevel.WARN, "AdBlocker حظر: $host")
                                        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isLoading = true
                                onLog(ConsoleLevel.NETWORK, "GET ${url?.take(60) ?: "local"}")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                onLog(ConsoleLevel.NETWORK, "200 OK • اكتمل تحميل الصفحة")

                                val darkCss = if (pro.forceDarkCss) {
                                    "html{filter:invert(0.92) hue-rotate(180deg)!important;background:#0d1117!important;}img,video{filter:invert(1) hue-rotate(180deg)!important;}"
                                } else ""
                                val mergedCss = "$darkCss ${pro.customCss}".trim().replace("'", "\\'").replace("\n", " ")
                                if (mergedCss.isNotBlank()) {
                                    view?.evaluateJavascript(
                                        "(function(){var s=document.createElement('style');s.innerHTML='$mergedCss';document.head.appendChild(s);})();",
                                        null
                                    )
                                    onLog(ConsoleLevel.INFO, "🎨 تم حقن CSS المخصص (${mergedCss.length} حرف)")
                                }
                                if (pro.customJs.isNotBlank()) {
                                    view?.evaluateJavascript(pro.customJs, null)
                                    onLog(ConsoleLevel.INFO, "⚡ تم تنفيذ JS المخصص")
                                }
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    onLog(ConsoleLevel.ERROR, "خطأ تحميل: ${error?.description}")
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                if (project.bridgeConfig.handleExternalIntents &&
                                    (url.startsWith("tel:") || url.startsWith("mailto:") || url.startsWith("whatsapp:") || url.contains("wa.me/"))
                                ) {
                                    onLog(ConsoleLevel.BRIDGE, "External Intent Intercept -> $url")
                                    Toast.makeText(ctx, "توجيه خارجي ذكي: $url", Toast.LENGTH_SHORT).show()
                                    return true
                                }
                                return false
                            }
                        }

                        if (project.sourceType == SourceType.WEB_URL &&
                            (project.sourceValue.startsWith("http://") || project.sourceValue.startsWith("https://"))
                        ) {
                            loadUrl(project.sourceValue)
                        } else {
                            loadDataWithBaseURL(
                                "https://apkforge.local/",
                                buildInteractivePreviewHtml(project),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                        webViewRef = this
                    }
                }
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                    color = accentColor,
                    trackColor = Color.Transparent
                )
            }

            // Navigation Drawer Trigger & Preview
            if (pro.drawerEnabled) {
                Surface(
                    color = Color(0xCC111622),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(36.dp)
                        .clickable { isDrawerOpen = !isDrawerOpen }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Menu, contentDescription = "القائمة الجانبية", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Floating Action Menu (FAB) Preview
            if (pro.fabMenuEnabled) {
                Surface(
                    color = accentColor,
                    shape = CircleShape,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp)
                        .size(44.dp)
                        .clickable {
                            val msg = if (pro.fabWhatsappNumber.isNotBlank()) {
                                "فتح دعم واتساب: ${pro.fabWhatsappNumber}"
                            } else {
                                "تم تحديث الصفحة عبر الزر العائم ✓"
                            }
                            onLog(ConsoleLevel.BRIDGE, "FAB Action -> $msg")
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (pro.fabWhatsappNumber.isBlank()) webViewRef?.reload()
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (pro.fabWhatsappNumber.isNotBlank()) Icons.Default.Chat else Icons.Default.Refresh,
                            contentDescription = "إجراء سريع",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Drawer Slide-Out Overlay inside simulated screen
            if (isDrawerOpen && pro.drawerEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable { isDrawerOpen = false }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.68f)
                            .background(Color(0xFF141925))
                            .border(1.dp, accentColor.copy(alpha = 0.35f))
                            .clickable(enabled = false) {}
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(project.name, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                        Text(pro.drawerHeaderSubtitle, fontFamily = TajawalFontFamily, fontSize = 11.sp, color = accentColor)
                        HorizontalDivider(color = GlassBorderSubtle)
                        project.bottomNavConfig.destinations.forEach { dest ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1D2434))
                                    .clickable {
                                        isDrawerOpen = false
                                        onLog(ConsoleLevel.INFO, "Drawer -> ${dest.title} (${dest.targetUrl})")
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ArrowForwardIos, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(dest.title, fontFamily = CairoFontFamily, fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevToolsConsoleDrawer(
    logs: List<DevConsoleEntry>,
    onClear: () -> Unit,
    onRunJs: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var jsInput by remember { mutableStateOf("ApkForge.showToast('اختبار حي!')") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            color = Color(0xFF0A0E14),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232C3E)),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = OrangePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DevTools Console (${logs.size})",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "مسح السجل",
                        fontFamily = CairoFontFamily,
                        fontSize = 11.sp,
                        color = OrangePrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClear() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickJsChip("Toast") { onRunJs("ApkForge.showToast('مرحباً من DevTools!')") }
                    QuickJsChip("Biometric") { onRunJs("ApkForge.authenticateBiometric()") }
                    QuickJsChip("Scan QR") { onRunJs("console.log('QR=' + ApkForge.scanQrCode())") }
                    QuickJsChip("Rate App") { onRunJs("ApkForge.requestStoreRating()") }
                    QuickJsChip("Vibrate") { onRunJs("ApkForge.vibrate(120)") }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06090D))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "> في انتظار أحداث WebView و ApkForge JS Bridge v2...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        items(logs, key = { it.id }) { entry ->
                            Text(
                                text = "[${entry.time} ${entry.level.tag}] ${entry.message}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = entry.level.color,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = jsInput,
                        onValueChange = { jsInput = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        placeholder = { Text("تنفيذ أمر JavaScript...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = { if (jsInput.isNotBlank()) onRunJs(jsInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("تشغيل", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickJsChip(label: String, onClick: () -> Unit) {
    Surface(
        color = Color(0xFF151C28),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, OrangePrimary.copy(alpha = 0.4f)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = OrangePrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun buildInteractivePreviewHtml(project: AppProject): String {
    val accent = project.accentColorHex
    val pro = project.proConfig
    val bridgeDemoButtons = if (project.bridgeConfig.jsBridgeEnabled) {
        """
        <div class="card">
          <h3>🔌 اختبار ApkForge Bridge v2</h3>
          <button onclick="ApkForge.showToast('أهلاً بك في ${project.name}!')">Toast</button>
          <button onclick="ApkForge.vibrate(140)">اهتزاز</button>
          ${if (pro.biometricAuthBridge) "<button onclick=\"ApkForge.authenticateBiometric()\">البصمة 🔐</button>" else ""}
          ${if (pro.qrScannerBridge) "<button onclick=\"ApkForge.scanQrCode()\">مسح QR 📷</button>" else ""}
          ${if (pro.inAppReviewBridge) "<button onclick=\"ApkForge.requestStoreRating()\">تقييم ⭐</button>" else ""}
        </div>
        """
    } else ""

    return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <style>
            body { margin: 0; font-family: system-ui, sans-serif; background: #0d1117; color: #f1f5f9; padding: 16px; }
            .hero { background: linear-gradient(135deg, ${accent}33, #161b22); border: 1px solid ${accent}66; border-radius: 16px; padding: 16px; text-align: center; }
            .card { background: #161b22; border: 1px solid #263042; border-radius: 14px; padding: 14px; margin-top: 12px; }
            h2 { margin: 0 0 6px 0; color: #fff; font-size: 18px; }
            h3 { margin: 0 0 10px 0; color: $accent; font-size: 14px; }
            p { margin: 0; font-size: 12px; color: #94a3b8; line-height: 1.6; }
            button { background: $accent; color: #fff; border: none; border-radius: 10px; padding: 8px 12px; font-size: 12px; font-weight: bold; margin: 4px; cursor: pointer; }
          </style>
        </head>
        <body>
          <div class="hero">
            <h2>${project.name}</h2>
            <p>WebView حقيقي • AdBlock=${pro.adAndPopupBlocker} • DarkCSS=${pro.forceDarkCss} • Secure=${pro.flagSecureEnabled}</p>
          </div>
          $bridgeDemoButtons
          <script>
            console.log('ApkForge Pro WebView Ready for ${project.packageConfig.packageName}');
          </script>
        </body>
        </html>
    """.trimIndent()
}

fun nowTimeLabel(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
