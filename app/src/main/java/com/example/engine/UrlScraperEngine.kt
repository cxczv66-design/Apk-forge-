package com.example.engine

import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit

data class ScrapedSiteIdentity(
    val normalizedUrl: String,
    val title: String,
    val description: String,
    val themeColorHex: String,
    val iconUrl: String?,
    val suggestedPackageName: String,
    val hasPwaManifest: Boolean,
    val host: String
)

class UrlScraperEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    /**
     * تحليل رابط الموقع واستخراج الاسم، اللون الأساسي، الوصف، وبيانات manifest.json تلقائياً
     */
    suspend fun scrapeSiteIdentity(rawUrl: String): Result<ScrapedSiteIdentity> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl(rawUrl)
            val uri = URI(url)
            val host = uri.host ?: return@withContext Result.failure(IllegalArgumentException("رابط غير صالح"))

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0 Mobile Safari/537.36")
                .build()

            val html = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    // Graceful offline/blocked fallback using host metadata
                    return@withContext Result.success(fallbackFromHost(url, host))
                }
                resp.body?.string()?.take(200_000) ?: ""
            }

            var extractedTitle = extractTitle(html)
            var extractedDesc = extractMetaContent(html, "description")
                ?: extractMetaContent(html, "og:description")
                ?: "تطبيق أندرويد ذكي لموقع $host"
            var themeColor = normalizeHexColor(extractMetaContent(html, "theme-color"))
            var iconHref = extractIconHref(html)
            var hasManifest = false

            // Inspect linked PWA manifest.json if available
            val manifestHref = extractManifestHref(html)
            if (!manifestHref.isNullOrBlank()) {
                val manifestUrl = resolveRelativeUrl(url, manifestHref)
                try {
                    val mReq = Request.Builder().url(manifestUrl).build()
                    client.newCall(mReq).execute().use { mResp ->
                        if (mResp.isSuccessful) {
                            val mJson = JSONObject(mResp.body?.string().orEmpty())
                            hasManifest = true
                            val shortName = mJson.optString("short_name").ifBlank { mJson.optString("name") }
                            if (shortName.isNotBlank()) extractedTitle = shortName
                            val mDesc = mJson.optString("description")
                            if (mDesc.isNotBlank()) extractedDesc = mDesc
                            val mTheme = normalizeHexColor(mJson.optString("theme_color"))
                            if (mTheme != null) themeColor = mTheme
                            val iconsArr = mJson.optJSONArray("icons")
                            if (iconsArr != null && iconsArr.length() > 0) {
                                val src = iconsArr.optJSONObject(iconsArr.length() - 1)?.optString("src")
                                if (!src.isNullOrBlank()) iconHref = src
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }

            val cleanTitle = extractedTitle?.trim()?.take(28)?.ifBlank { null }
                ?: host.removePrefix("www.").substringBefore(".").replaceFirstChar { it.uppercase() }

            val resolvedColor = themeColor ?: "#FF7A2F"
            val resolvedIcon = iconHref?.let { resolveRelativeUrl(url, it) }
            val suggestedPkg = buildPackageFromHost(host)

            Result.success(
                ScrapedSiteIdentity(
                    normalizedUrl = url,
                    title = cleanTitle,
                    description = extractedDesc.take(160),
                    themeColorHex = resolvedColor,
                    iconUrl = resolvedIcon,
                    suggestedPackageName = suggestedPkg,
                    hasPwaManifest = hasManifest,
                    host = host
                )
            )
        } catch (e: Exception) {
            // If network unavailable in offline/sandbox mode, still return clean deterministic identity from domain!
            val normalized = normalizeUrl(rawUrl)
            val host = try { URI(normalized).host } catch (_: Exception) { null }
            if (!host.isNullOrBlank()) {
                Result.success(fallbackFromHost(normalized, host))
            } else {
                Result.failure(e)
            }
        }
    }

    fun parseHtmlMetadata(url: String, html: String): ScrapedSiteIdentity {
        val uri = URI(normalizeUrl(url))
        val host = uri.host ?: "example.com"
        val title = extractTitle(html)?.trim()?.take(28)?.ifBlank { null }
            ?: host.removePrefix("www.").substringBefore(".").replaceFirstChar { it.uppercase() }
        val desc = extractMetaContent(html, "description") ?: "تطبيق أندرويد لموقع $host"
        val color = normalizeHexColor(extractMetaContent(html, "theme-color")) ?: "#FF7A2F"
        val icon = extractIconHref(html)?.let { resolveRelativeUrl(normalizeUrl(url), it) }
        return ScrapedSiteIdentity(
            normalizedUrl = normalizeUrl(url),
            title = title,
            description = desc,
            themeColorHex = color,
            iconUrl = icon,
            suggestedPackageName = buildPackageFromHost(host),
            hasPwaManifest = extractManifestHref(html) != null,
            host = host
        )
    }

    fun toPreConfiguredProject(identity: ScrapedSiteIdentity): AppProject {
        return AppProject(
            id = "proj_url_${System.currentTimeMillis()}",
            name = identity.title,
            description = identity.description,
            iconImageUri = identity.iconUrl,
            primaryColorHex = identity.themeColorHex,
            accentColorHex = identity.themeColorHex,
            sourceType = SourceType.WEB_URL,
            sourceValue = identity.normalizedUrl,
            splashConfig = SplashConfig(
                enabled = true,
                backgroundColorHex = "#0F1318",
                indicatorStyle = "حلقة"
            ),
            webViewConfig = WebViewConfig(
                fullScreen = false,
                pullToRefresh = true,
                cacheEnabled = true,
                offlineFallback = true
            ),
            bottomNavConfig = BottomNavConfig(
                enabled = true,
                activeColorHex = identity.themeColorHex,
                destinations = listOf(
                    BottomNavDestination("home", "الرئيسية", "/", "home"),
                    BottomNavDestination("explore", "تصفح", "/explore", "explore"),
                    BottomNavDestination("contact", "تواصل", "/contact", "person")
                )
            ),
            packageConfig = PackageConfig(
                packageName = identity.suggestedPackageName,
                versionName = "1.0.0",
                versionCode = 1,
                minSdk = 24,
                targetSdk = 34
            ),
            permissionsConfig = PermissionsConfig(networkState = true, vibration = true),
            bridgeConfig = BridgeAndCloudConfig(
                jsBridgeEnabled = true,
                handleExternalIntents = true,
                deepLinkDomain = identity.host,
                gitHubCiEnabled = true
            ),
            proConfig = ProStudioConfig(
                iconBgColorHex = identity.themeColorHex,
                adAndPopupBlocker = true,
                assetPreloaderEnabled = true
            )
        )
    }

    private fun fallbackFromHost(url: String, host: String): ScrapedSiteIdentity {
        val brand = host.removePrefix("www.").substringBefore(".").replaceFirstChar { it.uppercase() }
        return ScrapedSiteIdentity(
            normalizedUrl = url,
            title = brand.ifBlank { "WebApp" },
            description = "تطبيق ويب ذكي لموقع $host",
            themeColorHex = "#FF7A2F",
            iconUrl = "https://$host/favicon.ico",
            suggestedPackageName = buildPackageFromHost(host),
            hasPwaManifest = false,
            host = host
        )
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun buildPackageFromHost(host: String): String {
        val parts = host.removePrefix("www.")
            .lowercase()
            .split('.')
            .map { it.filter { c -> c in 'a'..'z' || c in '0'..'9' } }
            .filter { it.isNotBlank() }
        if (parts.size < 2) return "com.apkforge.webapp"
        val reversed = parts.reversed().joinToString(".") { p ->
            if (p.first().isDigit()) "app$p" else p
        }
        return reversed
    }

    private fun extractTitle(html: String): String? {
        val m = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).find(html)
        return m?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")
    }

    private fun extractMetaContent(html: String, name: String): String? {
        val r1 = Regex("""<meta[^>]+(?:name|property)=["']$name["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val r2 = Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+(?:name|property)=["']$name["']""", RegexOption.IGNORE_CASE)
        return r1.find(html)?.groupValues?.getOrNull(1) ?: r2.find(html)?.groupValues?.getOrNull(1)
    }

    private fun extractManifestHref(html: String): String? {
        val r = Regex("""<link[^>]+rel=["']manifest["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return r.find(html)?.groupValues?.getOrNull(1)
    }

    private fun extractIconHref(html: String): String? {
        val r = Regex("""<link[^>]+rel=["'](?:apple-touch-icon|icon|shortcut icon)["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return r.find(html)?.groupValues?.getOrNull(1)
    }

    private fun normalizeHexColor(raw: String?): String? {
        val c = raw?.trim().orEmpty()
        return if (Regex("^#[0-9A-Fa-f]{6}$").matches(c)) c.uppercase() else null
    }

    private fun resolveRelativeUrl(base: String, rel: String): String {
        return try {
            URI(base).resolve(rel.trim()).toString()
        } catch (_: Exception) {
            rel
        }
    }
}
