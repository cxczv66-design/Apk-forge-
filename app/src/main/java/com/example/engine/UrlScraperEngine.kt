package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.util.concurrent.TimeUnit

data class ScrapedLinkItem(
    val title: String,
    val url: String,
    val relativePath: String
)

data class ScrapedSiteIdentity(
    val normalizedUrl: String,
    val title: String,
    val description: String,
    val themeColorHex: String,
    val iconUrl: String?,
    val suggestedPackageName: String,
    val hasPwaManifest: Boolean,
    val host: String,
    val backgroundColorHex: String = "#0F1318",
    val ogImageUrl: String? = null,
    val localCachedIconUri: String? = null,
    val detectedLinks: List<ScrapedLinkItem> = emptyList(),
    val detectedButtons: List<String> = emptyList(),
    val detectedPermissions: List<String> = emptyList(),
    val detectedExternalSchemes: List<String> = emptyList(),
    val formsCount: Int = 0,
    val scriptsCount: Int = 0,
    val stylesheetsCount: Int = 0,
    val hasServiceWorker: Boolean = false,
    val detectedLanguage: String = "ar",
    val networkFetched: Boolean = false,
    val httpStatusCode: Int = 200,
    val rawHtmlSnapshot: String = ""
)

class UrlScraperEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    /**
     * تحليل واستخراج حقيقي وكامل لهوية الموقع:
     * الاسم، الأيقونة، الألوان، الأزرار، الروابط، النماذج، السكربتات، والأذونات الفعلية المستخدمة في الصفحة.
     */
    suspend fun scrapeSiteIdentity(
        rawUrl: String,
        appContext: Context? = null
    ): Result<ScrapedSiteIdentity> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeUrl(rawUrl)
            val uri = URI(url)
            val host = uri.host ?: return@withContext Result.failure(IllegalArgumentException("رابط غير صالح"))

            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/128.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            var statusCode = 200
            var finalUrl = url
            val html = client.newCall(req).execute().use { resp ->
                statusCode = resp.code
                finalUrl = resp.request.url.toString()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("استجاب الخادم برمز HTTP $statusCode"))
                }
                resp.body?.string()?.take(350_000) ?: ""
            }

            if (html.isBlank()) {
                return@withContext Result.failure(IllegalStateException("الموقع أعاد صفحة فارغة"))
            }

            var parsed = parseHtmlMetadata(finalUrl, html).copy(
                networkFetched = true,
                httpStatusCode = statusCode,
                rawHtmlSnapshot = html
            )

            // Fetch linked PWA manifest.json / manifest.webmanifest if available
            val manifestHref = extractManifestHref(html)
            if (!manifestHref.isNullOrBlank()) {
                val manifestUrl = resolveRelativeUrl(finalUrl, manifestHref)
                try {
                    val mReq = Request.Builder().url(manifestUrl).build()
                    client.newCall(mReq).execute().use { mResp ->
                        if (mResp.isSuccessful) {
                            val mJson = JSONObject(mResp.body?.string().orEmpty())
                            val shortName = mJson.optString("short_name").ifBlank { mJson.optString("name") }
                            val mDesc = mJson.optString("description")
                            val mTheme = normalizeHexColor(mJson.optString("theme_color"))
                            val mBg = normalizeHexColor(mJson.optString("background_color"))
                            var mIconUrl = parsed.iconUrl
                            val iconsArr = mJson.optJSONArray("icons")
                            if (iconsArr != null && iconsArr.length() > 0) {
                                // Pick highest resolution png/webp icon
                                var bestSrc: String? = null
                                var bestScore = -1
                                for (i in 0 until iconsArr.length()) {
                                    val obj = iconsArr.optJSONObject(i) ?: continue
                                    val src = obj.optString("src")
                                    if (src.isBlank()) continue
                                    val sizes = obj.optString("sizes")
                                    val numeric = sizes.substringBefore("x").toIntOrNull() ?: 64
                                    val isSvg = src.endsWith(".svg", true)
                                    val score = if (isSvg) numeric - 50 else numeric
                                    if (score > bestScore) {
                                        bestScore = score
                                        bestSrc = src
                                    }
                                }
                                if (!bestSrc.isNullOrBlank()) {
                                    mIconUrl = resolveRelativeUrl(manifestUrl, bestSrc)
                                }
                            }
                            parsed = parsed.copy(
                                hasPwaManifest = true,
                                title = if (shortName.isNotBlank()) shortName.take(28) else parsed.title,
                                description = if (mDesc.isNotBlank()) mDesc.take(180) else parsed.description,
                                themeColorHex = mTheme ?: parsed.themeColorHex,
                                backgroundColorHex = mBg ?: parsed.backgroundColorHex,
                                iconUrl = mIconUrl
                            )
                        }
                    }
                } catch (_: Exception) {
                }
            }

            // Guarantee high-res icon URL fallback via favicon endpoint if HTML lacked icon
            val candidateIcons = listOfNotNull(
                parsed.iconUrl,
                parsed.ogImageUrl,
                "https://$host/apple-touch-icon.png",
                "https://$host/favicon.ico",
                "https://www.google.com/s2/favicons?sz=128&domain=$host"
            ).distinct()

            var verifiedRemoteIcon: String? = parsed.iconUrl ?: parsed.ogImageUrl
            var cachedLocalIcon: String? = null
            if (appContext != null) {
                for (iconCandidate in candidateIcons) {
                    val downloaded = downloadIconToInternalCache(appContext, host, iconCandidate)
                    if (downloaded != null) {
                        verifiedRemoteIcon = iconCandidate
                        cachedLocalIcon = downloaded
                        break
                    }
                }
            }

            Result.success(
                parsed.copy(
                    iconUrl = verifiedRemoteIcon,
                    localCachedIconUri = cachedLocalIcon
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseHtmlMetadata(url: String, html: String): ScrapedSiteIdentity {
        val normalized = normalizeUrl(url)
        val uri = URI(normalized)
        val host = uri.host ?: "example.com"

        val title = (extractMetaContent(html, "og:site_name")
            ?: extractMetaContent(html, "application-name")
            ?: extractTitle(html))
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(28)
            ?.ifBlank { null }
            ?: host.removePrefix("www.").substringBefore(".").replaceFirstChar { it.uppercase() }

        val desc = (extractMetaContent(html, "description")
            ?: extractMetaContent(html, "og:description")
            ?: "تطبيق أندرويد لموقع $host")
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(180)

        val color = normalizeHexColor(extractMetaContent(html, "theme-color"))
            ?: normalizeHexColor(extractMetaContent(html, "msapplication-TileColor"))
            ?: extractCssPrimaryColor(html)
            ?: "#FF7A2F"

        val iconHref = extractBestIconHref(html)?.let { resolveRelativeUrl(normalized, it) }
        val ogImgHref = extractMetaContent(html, "og:image")?.let { resolveRelativeUrl(normalized, it) }

        val links = extractLinks(normalized, host, html)
        val buttons = extractButtons(html)
        val perms = detectRealPermissions(html)
        val schemes = detectExternalSchemes(html)

        val formsCount = Regex("<form\\b", RegexOption.IGNORE_CASE).findAll(html).count()
        val scriptsCount = Regex("<script\\b", RegexOption.IGNORE_CASE).findAll(html).count()
        val stylesCount = Regex("<link[^>]+rel=[\"']stylesheet[\"']|<style\\b", RegexOption.IGNORE_CASE).findAll(html).count()
        val hasSw = html.contains("serviceWorker", ignoreCase = true) || html.contains("manifest", ignoreCase = true)
        val lang = Regex("""<html[^>]+lang=["']([a-zA-Z-]+)["']""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.getOrNull(1)?.lowercase()?.take(2) ?: "ar"

        return ScrapedSiteIdentity(
            normalizedUrl = normalized,
            title = title,
            description = desc,
            themeColorHex = color,
            iconUrl = iconHref,
            ogImageUrl = ogImgHref,
            suggestedPackageName = buildPackageFromHost(host),
            hasPwaManifest = extractManifestHref(html) != null,
            host = host,
            detectedLinks = links,
            detectedButtons = buttons,
            detectedPermissions = perms,
            detectedExternalSchemes = schemes,
            formsCount = formsCount,
            scriptsCount = scriptsCount,
            stylesheetsCount = stylesCount,
            hasServiceWorker = hasSw,
            detectedLanguage = lang
        )
    }

    fun toPreConfiguredProject(identity: ScrapedSiteIdentity): AppProject {
        val navDestinations = if (identity.detectedLinks.isNotEmpty()) {
            identity.detectedLinks.take(4).mapIndexed { index, link ->
                BottomNavDestination(
                    id = "nav_${index + 1}",
                    title = link.title.take(14),
                    targetUrl = link.relativePath.ifBlank { link.url },
                    iconName = when (index) {
                        0 -> "home"
                        1 -> "explore"
                        2 -> "store"
                        else -> "person"
                    }
                )
            }
        } else {
            listOf(
                BottomNavDestination("home", "الرئيسية", "/", "home"),
                BottomNavDestination("explore", "تصفح", "/explore", "explore"),
                BottomNavDestination("contact", "تواصل", "/contact", "person")
            )
        }

        val permsSet = identity.detectedPermissions.toSet()
        val effectiveIcon = identity.localCachedIconUri ?: identity.iconUrl

        return AppProject(
            id = "proj_url_${System.currentTimeMillis()}",
            name = identity.title,
            description = identity.description,
            iconImageUri = effectiveIcon,
            primaryColorHex = identity.themeColorHex,
            accentColorHex = identity.themeColorHex,
            sourceType = SourceType.WEB_URL,
            sourceValue = identity.normalizedUrl,
            splashConfig = SplashConfig(
                enabled = true,
                backgroundColorHex = identity.backgroundColorHex,
                fadeBackground = true,
                indicatorStyle = "حلقة"
            ),
            webViewConfig = WebViewConfig(
                fullScreen = false,
                pullToRefresh = true,
                cacheEnabled = true,
                offlineFallback = true,
                fileDownload = permsSet.contains("storage")
            ),
            bottomNavConfig = BottomNavConfig(
                enabled = true,
                activeColorHex = identity.themeColorHex,
                destinations = navDestinations
            ),
            packageConfig = PackageConfig(
                packageName = identity.suggestedPackageName,
                versionName = "1.0.0",
                versionCode = 1,
                minSdk = 24,
                targetSdk = 35
            ),
            permissionsConfig = PermissionsConfig(
                camera = permsSet.contains("camera"),
                location = permsSet.contains("location"),
                microphone = permsSet.contains("microphone"),
                notifications = permsSet.contains("notifications"),
                storage = permsSet.contains("storage"),
                vibration = permsSet.contains("vibration"),
                networkState = true,
                autoDetected = permsSet.isNotEmpty()
            ),
            pushConfig = PushNotificationConfig(
                enabled = permsSet.contains("notifications") || identity.hasServiceWorker
            ),
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
            ),
            omniConfig = OmniIdeConfig(
                gradientEnabled = false,
                gradientStartHex = identity.themeColorHex,
                gradientEndHex = identity.backgroundColorHex,
                customHeroTitle = identity.title,
                customPrimaryButtonText = identity.detectedButtons.firstOrNull().orEmpty(),
                i18nSupportedCodesCsv = identity.detectedLanguage
            )
        )
    }

    /**
     * تحويل موقع الويب أو التطبيق السريع (PWA) إلى مشروع محلي قابل للتعديل المباشر
     * داخل مساحة عمل الجهاز (`filesDir/workspaces/pwa_<id>`) مع توليد ملفات HTML/CSS/JS
     * المحقونة بتخصيصات المستخدم لتظهر فوراً في المعاينة الحية وتُدمج في البناء.
     */
    fun materializeWebsiteAsLocalProject(
        context: Context,
        project: AppProject,
        identity: ScrapedSiteIdentity? = null,
        rawHtmlOverride: String? = null
    ): AppProject {
        val wsId = project.id.filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "pwa_${System.currentTimeMillis()}" }
        val wsDir = File(context.filesDir, "workspaces/pwa_$wsId").apply { mkdirs() }

        val sourceHtml = rawHtmlOverride?.takeIf { it.isNotBlank() }
            ?: identity?.rawHtmlSnapshot?.takeIf { it.isNotBlank() }
            ?: File(wsDir, "index.html").takeIf { it.exists() }?.readText()
            ?: buildFallbackPwaTemplateHtml(project, identity)

        val cleanedHtml = injectOmniLinksIntoHtml(
            rawHtml = sourceHtml,
            baseUrl = identity?.normalizedUrl ?: project.sourceValue,
            appTitle = project.omniConfig.customHeroTitle.ifBlank { project.name }
        )
        File(wsDir, "index.html").writeText(cleanedHtml, Charsets.UTF_8)

        val updatedProject = project.copy(
            sourceType = if (project.sourceType == SourceType.WEB_URL) SourceType.HTML_FOLDER else project.sourceType,
            sourceValue = File(wsDir, "index.html").name,
            localWorkspacePath = wsDir.absolutePath
        )
        return applyCustomizationsToLocalWorkspace(updatedProject)
    }

    fun applyCustomizationsToLocalWorkspace(project: AppProject): AppProject {
        val wsPath = project.localWorkspacePath ?: return project
        val wsDir = File(wsPath).apply { mkdirs() }
        val omni = project.omniConfig

        val cssOverrides = buildString {
            appendLine("/* Omni IDE Visual Overrides */")
            appendLine(":root { --omni-primary: ${project.primaryColorHex}; --omni-accent: ${project.accentColorHex}; }")
            appendLine("html { font-size: ${omni.fontScalePercent.coerceIn(75, 145)}% !important; }")
            if (omni.gradientEnabled) {
                appendLine("body, header, .hero, .banner { background: linear-gradient(135deg, ${omni.gradientStartHex}, ${omni.gradientEndHex}) !important; }")
            }
            if (project.proConfig.forceDarkCss) {
                appendLine("body { background-color: #0D1117 !important; color: #F1F5F9 !important; }")
            }
            val hiddenSelectors = omni.customHideSelectorsCsv
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (hiddenSelectors.isNotEmpty()) {
                appendLine("${hiddenSelectors.joinToString(", ")} { display: none !important; }")
            }
            if (project.proConfig.customCss.isNotBlank()) {
                appendLine(project.proConfig.customCss)
            }
        }
        File(wsDir, "omni-overrides.css").writeText(cssOverrides, Charsets.UTF_8)

        val escapedHero = escapeJsSingle(omni.customHeroTitle)
        val escapedBtn = escapeJsSingle(omni.customPrimaryButtonText)
        val escapedBanner = escapeJsSingle(omni.customBannerImageUri.orEmpty())
        val acceleratorJs = if (omni.appAcceleratorEnabled && omni.acceleratorImageCompression) {
            "document.querySelectorAll('img').forEach(function(img){if(!img.getAttribute('loading'))img.setAttribute('loading','lazy');});"
        } else ""

        val jsOverrides = buildString {
            appendLine("// Omni IDE Runtime DOM & PWA Customizations")
            appendLine("document.addEventListener('DOMContentLoaded', function() {")
            if (escapedHero.isNotBlank()) {
                appendLine("  var h = document.querySelector('[data-omni-hero], h1, .hero-title');")
                appendLine("  if (h) h.textContent = '$escapedHero';")
            }
            if (escapedBtn.isNotBlank()) {
                appendLine("  var b = document.querySelector('[data-omni-cta], button.primary, .btn-primary, button, input[type=submit]');")
                appendLine("  if (b) { if (b.tagName === 'INPUT') b.value = '$escapedBtn'; else b.textContent = '$escapedBtn'; }")
            }
            if (escapedBanner.isNotBlank()) {
                appendLine("  var img = document.querySelector('[data-omni-banner], .hero img, header img');")
                appendLine("  if (img) img.src = '$escapedBanner';")
            }
            if (acceleratorJs.isNotBlank()) {
                appendLine("  $acceleratorJs")
            }
            appendLine("});")
        }
        File(wsDir, "omni-overrides.js").writeText(jsOverrides, Charsets.UTF_8)

        return project
    }

    private fun buildFallbackPwaTemplateHtml(project: AppProject, identity: ScrapedSiteIdentity?): String {
        val title = project.omniConfig.customHeroTitle.ifBlank { project.name }
        val desc = project.description.ifBlank { identity?.description ?: "تطبيق ويب محلي قابل للتخصيص عبر ApkForge Omni IDE" }
        val btnText = project.omniConfig.customPrimaryButtonText.ifBlank {
            identity?.detectedButtons?.firstOrNull() ?: "استكشاف الآن"
        }
        val linkCards = (identity?.detectedLinks.orEmpty().take(4)).joinToString("\n") { link ->
            """<a class="nav-card" href="${link.url}">${link.title}</a>"""
        }.ifBlank {
            """<a class="nav-card" href="#features">المميزات</a><a class="nav-card" href="#contact">التواصل</a>"""
        }
        return """
            <!DOCTYPE html>
            <html dir="rtl" lang="ar">
            <head>
              <meta charset="UTF-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover" />
              <title>$title</title>
              <style>
                body { margin:0; font-family: system-ui, sans-serif; background:#0F1318; color:#F8FAFC; padding:24px; }
                .hero { padding:26px; border-radius:20px; background:linear-gradient(135deg, ${project.primaryColorHex}, ${project.accentColorHex}); color:#fff; margin-bottom:18px; }
                .hero h1 { margin:0 0 8px 0; font-size:24px; }
                .hero p { margin:0 0 16px 0; opacity:0.92; font-size:14px; }
                .btn-primary { display:inline-block; background:#0F1318; color:#fff; border:none; border-radius:12px; padding:12px 20px; font-weight:700; cursor:pointer; }
                .grid { display:grid; grid-template-columns:1fr 1fr; gap:10px; }
                .nav-card { display:block; padding:14px; border-radius:14px; background:#192130; color:#E2E8F0; text-decoration:none; border:1px solid #2A3548; text-align:center; font-weight:600; }
              </style>
              <link rel="stylesheet" href="omni-overrides.css" />
            </head>
            <body>
              <section class="hero">
                <h1 data-omni-hero="true">$title</h1>
                <p>$desc</p>
                <button class="btn-primary" data-omni-cta="true" onclick="window.ApkForge && ApkForge.showToast ? ApkForge.showToast('$btnText') : alert('$btnText')">$btnText</button>
              </section>
              <div class="grid">$linkCards</div>
              <script src="omni-overrides.js"></script>
            </body>
            </html>
        """.trimIndent()
    }

    private fun injectOmniLinksIntoHtml(rawHtml: String, baseUrl: String, appTitle: String): String {
        var html = rawHtml
        val baseTag = if (baseUrl.startsWith("http", true) && !html.contains("<base ", true)) {
            """<base href="$baseUrl" />"""
        } else ""
        val headInject = """$baseTag<link rel="stylesheet" href="omni-overrides.css" /><title>$appTitle</title>"""
        html = when {
            html.contains("</head>", ignoreCase = true) ->
                html.replaceFirst(Regex("</head>", RegexOption.IGNORE_CASE), "$headInject</head>")
            html.contains("<body", ignoreCase = true) ->
                html.replaceFirst(Regex("<body", RegexOption.IGNORE_CASE), "<head>$headInject</head><body")
            else -> "<!DOCTYPE html><html><head>$headInject</head><body>$html</body></html>"
        }
        val scriptTag = """<script src="omni-overrides.js"></script>"""
        if (!html.contains("omni-overrides.js")) {
            html = if (html.contains("</body>", ignoreCase = true)) {
                html.replaceFirst(Regex("</body>", RegexOption.IGNORE_CASE), "$scriptTag</body>")
            } else {
                "$html\n$scriptTag"
            }
        }
        return html
    }

    private fun escapeJsSingle(raw: String): String =
        raw.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ").trim()

    private fun downloadIconToInternalCache(context: Context, host: String, iconUrl: String): String? {
        return try {
            val req = Request.Builder()
                .url(iconUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val contentType = resp.header("Content-Type").orEmpty().lowercase()
                val bytes = resp.body?.bytes() ?: return null
                if (bytes.size < 100 || contentType.contains("text/html")) return null
                val dir = File(context.filesDir, "app_icons").apply { mkdirs() }
                val safeHost = host.filter { it.isLetterOrDigit() || it == '_' }.take(24)
                val outFile = File(dir, "scraped_${safeHost}_${System.currentTimeMillis()}.png")
                FileOutputStream(outFile).use { it.write(bytes) }
                Uri.fromFile(outFile).toString()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractLinks(baseUrl: String, host: String, html: String): List<ScrapedLinkItem> {
        val regex = Regex("""<a\b[^>]*href=["']([^"'#]+)["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val result = mutableListOf<ScrapedLinkItem>()
        val seenPaths = mutableSetOf<String>()
        for (match in regex.findAll(html)) {
            val rawHref = match.groupValues[1].trim()
            if (rawHref.startsWith("javascript:", true) || rawHref.startsWith("mailto:", true) || rawHref.startsWith("tel:", true)) continue
            val rawText = stripTags(match.groupValues[2]).trim()
            if (rawText.length < 2 || rawText.length > 28) continue
            val resolved = resolveRelativeUrl(baseUrl, rawHref)
            val parsedUri = try { URI(resolved) } catch (_: Exception) { null }
            val path = parsedUri?.path?.ifBlank { "/" } ?: rawHref
            if (parsedUri?.host != null && !parsedUri.host.endsWith(host, ignoreCase = true) && !host.endsWith(parsedUri.host, ignoreCase = true)) {
                continue
            }
            if (seenPaths.add(path)) {
                result.add(ScrapedLinkItem(title = rawText, url = resolved, relativePath = path))
            }
            if (result.size >= 10) break
        }
        return result
    }

    private fun extractButtons(html: String): List<String> {
        val btnRegex = Regex("""<button\b[^>]*>(.*?)</button>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val inputRegex = Regex("""<input\b[^>]*type=["'](?:submit|button)["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val buttons = linkedSetOf<String>()
        btnRegex.findAll(html).forEach { m ->
            val label = stripTags(m.groupValues[1]).trim()
            if (label.length in 2..32) buttons.add(label)
        }
        inputRegex.findAll(html).forEach { m ->
            val label = m.groupValues[1].trim()
            if (label.length in 2..32) buttons.add(label)
        }
        return buttons.take(12)
    }

    private fun detectRealPermissions(html: String): List<String> {
        val perms = mutableListOf<String>()
        if (html.contains("capture=", true) || html.contains("video", true) && html.contains("getUserMedia", true)) perms.add("camera")
        if (html.contains("navigator.geolocation", true)) perms.add("location")
        if (html.contains("SpeechRecognition", true) || (html.contains("audio", true) && html.contains("getUserMedia", true))) perms.add("microphone")
        if (html.contains("Notification.requestPermission", true) || html.contains("pushManager", true)) perms.add("notifications")
        if (html.contains("type=\"file\"", true) || html.contains("type='file'", true) || html.contains("download=", true)) perms.add("storage")
        if (html.contains("navigator.vibrate", true)) perms.add("vibration")
        return perms
    }

    private fun detectExternalSchemes(html: String): List<String> {
        val list = mutableListOf<String>()
        if (html.contains("mailto:", true)) list.add("mailto:")
        if (html.contains("tel:", true)) list.add("tel:")
        if (html.contains("wa.me/", true) || html.contains("whatsapp:", true)) list.add("wa.me")
        if (html.contains("t.me/", true) || html.contains("tg://", true)) list.add("telegram")
        return list
    }

    private fun stripTags(raw: String): String =
        raw.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")

    private fun extractCssPrimaryColor(html: String): String? {
        val m = Regex("""--(?:primary|theme|brand|accent)[a-zA-Z0-9-]*\s*:\s*(#[0-9A-Fa-f]{6})""", RegexOption.IGNORE_CASE).find(html)
        return m?.groupValues?.getOrNull(1)?.uppercase()
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
        return parts.reversed().joinToString(".") { p ->
            if (p.first().isDigit()) "app$p" else p
        }
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
        val r1 = Regex("""<link[^>]+rel=["']manifest["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val r2 = Regex("""<link[^>]+href=["']([^"']+)["'][^>]+rel=["']manifest["']""", RegexOption.IGNORE_CASE)
        return r1.find(html)?.groupValues?.getOrNull(1) ?: r2.find(html)?.groupValues?.getOrNull(1)
    }

    private fun extractBestIconHref(html: String): String? {
        val priorityPatterns = listOf(
            """<link[^>]+rel=["']apple-touch-icon[^"']*["'][^>]+href=["']([^"']+)["']""",
            """<link[^>]+href=["']([^"']+)["'][^>]+rel=["']apple-touch-icon[^"']*["']""",
            """<link[^>]+rel=["'][^"']*icon[^"']*["'][^>]+href=["']([^"']+)["']""",
            """<link[^>]+href=["']([^"']+)["'][^>]+rel=["'][^"']*icon[^"']*["']"""
        )
        for (pat in priorityPatterns) {
            val match = Regex(pat, RegexOption.IGNORE_CASE).find(html)?.groupValues?.getOrNull(1)
            if (!match.isNullOrBlank()) return match
        }
        return null
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
