package com.example.ui.components

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Locale

/**
 * أدوات حماية واستقرار محرك عرض الويب (WebView / Chromium Renderer):
 * - تمنع أعطال عملية التصيير (aw_browser_terminator.cc Renderer crash code -1)
 *   على المحاكيات ووحدات المعالجة الرسومية الافتراضية (SwiftShader / Cuttlefish / ranchu / goldfish).
 * - تحمي استدعاءات JavaScript وإتلاف العرض عند مغادرة الشاشة أو تبديل التبويبات.
 */
object WebViewSafety {

    @Volatile
    private var sessionRendererCrashCount: Int = 0

    fun recordRendererCrash() {
        sessionRendererCrashCount++
    }

    fun isSessionRendererBlocked(): Boolean = sessionRendererCrashCount >= 2

    fun resetRendererSession() {
        sessionRendererCrashCount = 0
    }

    fun isLikelyVirtualGpuOrEmulator(): Boolean = isVirtualGpuEnvironment(
        fingerprint = Build.FINGERPRINT,
        model = Build.MODEL,
        manufacturer = Build.MANUFACTURER,
        brand = Build.BRAND,
        device = Build.DEVICE,
        product = Build.PRODUCT,
        hardware = Build.HARDWARE,
        board = Build.BOARD,
        supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty()
    )

    internal fun isVirtualGpuEnvironment(
        fingerprint: String,
        model: String,
        manufacturer: String,
        brand: String,
        device: String,
        product: String,
        hardware: String,
        board: String = "",
        supportedAbis: List<String> = emptyList()
    ): Boolean {
        val fp = fingerprint.lowercase(Locale.US)
        val mdl = model.lowercase(Locale.US)
        val mfr = manufacturer.lowercase(Locale.US)
        val br = brand.lowercase(Locale.US)
        val dev = device.lowercase(Locale.US)
        val prod = product.lowercase(Locale.US)
        val hw = hardware.lowercase(Locale.US)
        val brd = board.lowercase(Locale.US)
        val hasX86 = supportedAbis.any { it.contains("x86", ignoreCase = true) }

        return fp.startsWith("generic") ||
            fp.startsWith("unknown") ||
            fp.contains("emulator") ||
            fp.contains("test-keys") ||
            fp.contains("aosp_cf") ||
            fp.contains("cutf") ||
            mdl.contains("google_sdk") ||
            mdl.contains("emulator") ||
            mdl.contains("cuttlefish") ||
            mdl.contains("android sdk built for") ||
            mfr.contains("genymotion") ||
            (br.startsWith("generic") && dev.startsWith("generic")) ||
            prod.contains("sdk_gphone") ||
            prod.contains("sdk_phone") ||
            prod.contains("emulator") ||
            prod.contains("vbox") ||
            prod.contains("cf_") ||
            prod.contains("cutf") ||
            prod.contains("vsoc") ||
            dev.contains("vsoc") ||
            dev.contains("cutf") ||
            hw.contains("goldfish") ||
            hw.contains("ranchu") ||
            hw.contains("vbox") ||
            hw.contains("nox") ||
            hw.contains("cutf") ||
            hw.contains("vsoc") ||
            hw.contains("crosvm") ||
            hw.contains("virtio") ||
            hw.contains("qemu") ||
            hw.contains("gce") ||
            brd.contains("cutf") ||
            brd.contains("goldfish") ||
            (hasX86 && (prod.contains("aosp") || br.contains("google") || fp.contains("dev-keys") || fp.contains("userdebug")))
    }

    /**
     * في بيئات المحاكيات أو GPU الافتراضي أو بعد حدوث عطل سابق نعتمد الوضع البرمجي الآمن لتفادي انهيار Chromium GPU Renderer.
     */
    fun shouldDefaultToSoftwareLayer(): Boolean =
        sessionRendererCrashCount > 0 || isLikelyVirtualGpuOrEmulator()

    /**
     * ضبط سياسة أولوية الـ Renderer دون التنازل المفاجئ عند التحويلات البرمجية في Compose لتجنب SIGKILL (code -1).
     */
    fun configureSafeRendererPolicy(webView: WebView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
            }
        }
    }

    /**
     * تطبيق إعدادات استقرار Chromium للمعاينة المحلية وعلى المحاكيات السحابية.
     */
    fun applySafeWebSettings(webView: WebView, useSoftwareLayer: Boolean) {
        if (useSoftwareLayer) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
        configureSafeRendererPolicy(webView)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { webView.settings.offscreenPreRaster = false }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { webView.settings.safeBrowsingEnabled = false }
        }
    }

    fun canSafelyInteract(
        webView: WebView?,
        destroyedSet: Set<WebView>,
        isRendererFailed: Boolean
    ): Boolean {
        return webView != null &&
            !isRendererFailed &&
            !destroyedSet.contains(webView) &&
            webView.isAttachedToWindow
    }

    fun destroyWebViewSafely(
        webView: WebView?,
        destroyedSet: MutableSet<WebView>,
        jsBridgeName: String? = null
    ) {
        if (webView == null) return
        if (!destroyedSet.add(webView)) return
        runCatching {
            webView.stopLoading()
            webView.onPause()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            if (!jsBridgeName.isNullOrBlank()) {
                webView.removeJavascriptInterface(jsBridgeName)
            }
            webView.clearHistory()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.removeAllViews()
            webView.destroy()
        }
    }
}
