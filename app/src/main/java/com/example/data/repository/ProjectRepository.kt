package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class DownloadedApk(
    val id: String,
    val projectId: String = "",
    val projectName: String,
    val fileName: String,
    val filePath: String,
    val sizeText: String,
    val downloadedAt: Long,
    val packageName: String = "com.zk.app",
    val versionName: String = "1.0",
    val versionCode: Int = 1,
    val buildType: String = "APK",
    val permissionsCount: Int = 2,
    val jsBridgeEnabled: Boolean = true,
    val pushEnabled: Boolean = false
)

class ProjectRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("apkforge_projects_prefs", Context.MODE_PRIVATE)

    private val _projects = MutableStateFlow<List<AppProject>>(emptyList())
    val projects: StateFlow<List<AppProject>> = _projects.asStateFlow()

    private val _downloads = MutableStateFlow<List<DownloadedApk>>(emptyList())
    val downloads: StateFlow<List<DownloadedApk>> = _downloads.asStateFlow()

    init {
        loadProjects()
        loadDownloads()
    }

    private fun loadProjects() {
        val jsonString = prefs.getString("saved_projects", null)
        if (jsonString.isNullOrBlank()) {
            _projects.value = emptyList()
        } else {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<AppProject>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val proj = parseProject(obj)
                    if (proj.id != "proj_rio_001" && proj.name != "Rio") {
                        list.add(proj)
                    }
                }
                _projects.value = list
                saveToPrefs(list)
            } catch (e: Exception) {
                _projects.value = emptyList()
            }
        }
    }

    private fun loadDownloads() {
        val jsonString = prefs.getString("saved_downloads", null)
        if (jsonString.isNullOrBlank()) {
            _downloads.value = emptyList()
        } else {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<DownloadedApk>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        DownloadedApk(
                            id = obj.getString("id"),
                            projectId = obj.optString("projectId", ""),
                            projectName = obj.getString("projectName"),
                            fileName = obj.getString("fileName"),
                            filePath = obj.getString("filePath"),
                            sizeText = obj.optString("sizeText", "8.4 MB"),
                            downloadedAt = obj.optLong("downloadedAt", System.currentTimeMillis()),
                            packageName = obj.optString("packageName", "com.zk.app"),
                            versionName = obj.optString("versionName", "1.0"),
                            versionCode = obj.optInt("versionCode", 1),
                            buildType = obj.optString("buildType", "APK"),
                            permissionsCount = obj.optInt("permissionsCount", 2),
                            jsBridgeEnabled = obj.optBoolean("jsBridgeEnabled", true),
                            pushEnabled = obj.optBoolean("pushEnabled", false)
                        )
                    )
                }
                _downloads.value = list
            } catch (e: Exception) {
                _downloads.value = emptyList()
            }
        }
    }

    fun saveProject(project: AppProject) {
        val current = _projects.value.toMutableList()
        val index = current.indexOfFirst { it.id == project.id }
        if (index >= 0) {
            current[index] = project
        } else {
            current.add(0, project)
        }
        _projects.value = current
        saveToPrefs(current)
    }

    fun deleteProject(projectId: String) {
        val current = _projects.value.filter { it.id != projectId }
        _projects.value = current
        saveToPrefs(current)
    }

    fun getProjectById(id: String): AppProject? {
        return _projects.value.find { it.id == id }
    }

    fun addDownloadedApk(apk: DownloadedApk) {
        val current = _downloads.value.toMutableList()
        current.add(0, apk)
        _downloads.value = current
        saveDownloadsToPrefs(current)
    }

    fun removeDownload(downloadId: String) {
        val current = _downloads.value.filterNot { it.id == downloadId }
        _downloads.value = current
        saveDownloadsToPrefs(current)
    }

    fun updateBuildStatus(
        projectId: String,
        status: BuildStatus,
        apkPath: String?,
        customFileName: String? = null,
        sizeText: String = "8.4 MB"
    ) {
        val project = getProjectById(projectId) ?: return
        val now = System.currentTimeMillis()
        val resolvedFileName = customFileName ?: "${project.name}_v${project.packageConfig.versionName}.apk"
        val isAab = resolvedFileName.endsWith(".aab", ignoreCase = true)
        val buildTypeLabel = if (isAab) "AAB" else "APK"

        val updatedHistory = if (status == BuildStatus.SUCCESS && apkPath != null) {
            val entry = BuildHistoryEntry(
                id = "hist_${now}_${resolvedFileName.hashCode()}",
                versionCode = project.packageConfig.versionCode,
                versionName = project.packageConfig.versionName,
                targetType = buildTypeLabel,
                fileName = resolvedFileName,
                sizeText = sizeText,
                timestamp = now
            )
            (listOf(entry) + project.buildHistory).take(15)
        } else {
            project.buildHistory
        }

        val updated = project.copy(
            lastBuildStatus = status,
            lastBuiltAt = now,
            lastOutputApk = apkPath ?: project.lastOutputApk,
            buildHistory = updatedHistory
        )
        saveProject(updated)

        if (status == BuildStatus.SUCCESS && apkPath != null) {
            val p = project.permissionsConfig
            val permCount = 2 + // INTERNET + ACCESS_NETWORK_STATE
                listOf(p.camera, p.location, p.microphone, p.notifications || project.pushConfig.enabled, p.storage, p.vibration, p.bluetooth).count { it }

            addDownloadedApk(
                DownloadedApk(
                    id = "dl_${now}_${resolvedFileName.hashCode()}",
                    projectId = project.id,
                    projectName = project.name,
                    fileName = resolvedFileName,
                    filePath = apkPath,
                    sizeText = sizeText,
                    downloadedAt = now,
                    packageName = project.packageConfig.packageName,
                    versionName = project.packageConfig.versionName,
                    versionCode = project.packageConfig.versionCode,
                    buildType = buildTypeLabel,
                    permissionsCount = permCount,
                    jsBridgeEnabled = project.bridgeConfig.jsBridgeEnabled,
                    pushEnabled = project.pushConfig.enabled
                )
            )
        }
    }

    /**
     * تصدير مشروع كامل بصيغة .apkforge JSON (تشمل جميع الخطوات الـ 9 والجسر والسجل)
     */
    fun exportProjectToJson(project: AppProject): String {
        val wrapper = JSONObject().apply {
            put("format", "apkforge_project_v2")
            put("exportedAt", System.currentTimeMillis())
            put("project", projectToJson(project))
        }
        return wrapper.toString(2)
    }

    /**
     * استيراد مشروع من ملف/نص .apkforge JSON
     */
    fun importProjectFromJson(rawJson: String): AppProject? {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.isBlank()) return null
            val root = JSONObject(trimmed)
            val projJson = if (root.has("project")) root.getJSONObject("project") else root
            val parsed = parseProject(projJson)
            // Assign fresh unique ID if colliding with existing project
            val finalId = if (_projects.value.any { it.id == parsed.id }) {
                "proj_${System.currentTimeMillis()}"
            } else {
                parsed.id
            }
            val imported = parsed.copy(id = finalId)
            saveProject(imported)
            imported
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToPrefs(list: List<AppProject>) {
        val array = JSONArray()
        list.forEach { proj -> array.put(projectToJson(proj)) }
        prefs.edit().putString("saved_projects", array.toString()).apply()
    }

    private fun saveDownloadsToPrefs(list: List<DownloadedApk>) {
        val array = JSONArray()
        list.forEach { dl ->
            val obj = JSONObject().apply {
                put("id", dl.id)
                put("projectId", dl.projectId)
                put("projectName", dl.projectName)
                put("fileName", dl.fileName)
                put("filePath", dl.filePath)
                put("sizeText", dl.sizeText)
                put("downloadedAt", dl.downloadedAt)
                put("packageName", dl.packageName)
                put("versionName", dl.versionName)
                put("versionCode", dl.versionCode)
                put("buildType", dl.buildType)
                put("permissionsCount", dl.permissionsCount)
                put("jsBridgeEnabled", dl.jsBridgeEnabled)
                put("pushEnabled", dl.pushEnabled)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_downloads", array.toString()).apply()
    }

    private fun projectToJson(proj: AppProject): JSONObject {
        return JSONObject().apply {
            put("id", proj.id)
            put("name", proj.name)
            put("description", proj.description)
            put("iconName", proj.iconName)
            put("iconImageUri", proj.iconImageUri ?: "")
            put("primaryColorHex", proj.primaryColorHex)
            put("accentColorHex", proj.accentColorHex)
            put("sourceType", proj.sourceType.name)
            put("sourceValue", proj.sourceValue)
            put("createdAt", proj.createdAt)
            put("lastBuiltAt", proj.lastBuiltAt ?: 0L)
            put("lastBuildStatus", proj.lastBuildStatus.name)
            put("lastOutputApk", proj.lastOutputApk ?: "")

            // Step 3: Splash
            put("splashConfig", JSONObject().apply {
                put("enabled", proj.splashConfig.enabled)
                put("backgroundColorHex", proj.splashConfig.backgroundColorHex)
                put("fadeBackground", proj.splashConfig.fadeBackground)
                put("indicatorStyle", proj.splashConfig.indicatorStyle)
                put("iconSizeDp", proj.splashConfig.iconSizeDp)
                put("iconCornerRadiusDp", proj.splashConfig.iconCornerRadiusDp)
                put("indicatorSizeDp", proj.splashConfig.indicatorSizeDp)
                put("spacingDp", proj.splashConfig.spacingDp)
            })

            // Step 4: Onboarding
            put("onboardingConfig", JSONObject().apply {
                put("enabled", proj.onboardingConfig.enabled)
                put("layoutType", proj.onboardingConfig.layoutType)
                put("imageShape", proj.onboardingConfig.imageShape)
                put("gradientBackground", proj.onboardingConfig.gradientBackground)
                put("accentColorHex", proj.onboardingConfig.accentColorHex)
                put("textColorHex", proj.onboardingConfig.textColorHex)
                put("cardColorHex", proj.onboardingConfig.cardColorHex)
                put("showSkipButton", proj.onboardingConfig.showSkipButton)
                put("slides", JSONArray().apply {
                    proj.onboardingConfig.slides.forEach { s ->
                        put(JSONObject().apply {
                            put("title", s.title)
                            put("description", s.description)
                            put("iconName", s.iconName)
                            put("imageUri", s.imageUri ?: "")
                        })
                    }
                })
            })

            // Step 5: WebView
            put("webViewConfig", JSONObject().apply {
                put("fullScreen", proj.webViewConfig.fullScreen)
                put("pullToRefresh", proj.webViewConfig.pullToRefresh)
                put("supportZoom", proj.webViewConfig.supportZoom)
                put("cacheEnabled", proj.webViewConfig.cacheEnabled)
                put("fileDownload", proj.webViewConfig.fileDownload)
                put("offlineFallback", proj.webViewConfig.offlineFallback)
                put("pageLoader", JSONObject().apply {
                    put("enabled", proj.webViewConfig.pageLoader.enabled)
                    put("style", proj.webViewConfig.pageLoader.style)
                    put("sizeDp", proj.webViewConfig.pageLoader.sizeDp)
                    put("thicknessDp", proj.webViewConfig.pageLoader.thicknessDp)
                    put("showOnCard", proj.webViewConfig.pageLoader.showOnCard)
                    put("cardColorHex", proj.webViewConfig.pageLoader.cardColorHex)
                    put("cardOpacityPercent", proj.webViewConfig.pageLoader.cardOpacityPercent)
                    put("cardCornerRadiusDp", proj.webViewConfig.pageLoader.cardCornerRadiusDp)
                })
            })

            // Step 6: Bottom Nav
            put("bottomNavConfig", JSONObject().apply {
                put("enabled", proj.bottomNavConfig.enabled)
                put("barStyle", proj.bottomNavConfig.barStyle)
                put("showLabels", proj.bottomNavConfig.showLabels)
                put("activeColorHex", proj.bottomNavConfig.activeColorHex)
                put("inactiveColorHex", proj.bottomNavConfig.inactiveColorHex)
                put("barColorHex", proj.bottomNavConfig.barColorHex)
                put("destinations", JSONArray().apply {
                    proj.bottomNavConfig.destinations.forEach { d ->
                        put(JSONObject().apply {
                            put("id", d.id)
                            put("title", d.title)
                            put("targetUrl", d.targetUrl)
                            put("iconName", d.iconName)
                        })
                    }
                })
            })

            // Step 7: Package
            put("packageName", proj.packageConfig.packageName)
            put("versionName", proj.packageConfig.versionName)
            put("versionCode", proj.packageConfig.versionCode)
            put("packageConfig", JSONObject().apply {
                put("packageName", proj.packageConfig.packageName)
                put("versionName", proj.packageConfig.versionName)
                put("versionCode", proj.packageConfig.versionCode)
                put("minSdk", proj.packageConfig.minSdk)
                put("targetSdk", proj.packageConfig.targetSdk)
            })

            // Step 8: Permissions
            put("permissionsConfig", JSONObject().apply {
                put("camera", proj.permissionsConfig.camera)
                put("location", proj.permissionsConfig.location)
                put("microphone", proj.permissionsConfig.microphone)
                put("notifications", proj.permissionsConfig.notifications)
                put("storage", proj.permissionsConfig.storage)
                put("vibration", proj.permissionsConfig.vibration)
                put("bluetooth", proj.permissionsConfig.bluetooth)
                put("networkState", proj.permissionsConfig.networkState)
                put("autoDetected", proj.permissionsConfig.autoDetected)
                put("storeJustificationNotes", proj.permissionsConfig.storeJustificationNotes)
            })

            // Step 9: Push
            put("pushConfig", JSONObject().apply {
                put("enabled", proj.pushConfig.enabled)
                put("provider", proj.pushConfig.provider)
                put("appId", proj.pushConfig.appId)
                put("channelName", proj.pushConfig.channelName)
                put("soundEnabled", proj.pushConfig.soundEnabled)
                put("vibrationEnabled", proj.pushConfig.vibrationEnabled)
                put("badgeEnabled", proj.pushConfig.badgeEnabled)
                put("openUrlOnTap", proj.pushConfig.openUrlOnTap)
                put("autoPromptOnFirstLaunch", proj.pushConfig.autoPromptOnFirstLaunch)
                put("testTitle", proj.pushConfig.testTitle)
                put("testBody", proj.pushConfig.testBody)
            })

            // Signing Config
            put("signingConfig", JSONObject().apply {
                put("useDebugKey", proj.signingConfig.useDebugKey)
                put("keyAlias", proj.signingConfig.keyAlias)
                put("keystorePath", proj.signingConfig.keystorePath)
            })

            // Bridge & Cloud CI Config
            put("bridgeConfig", JSONObject().apply {
                put("jsBridgeEnabled", proj.bridgeConfig.jsBridgeEnabled)
                put("handleExternalIntents", proj.bridgeConfig.handleExternalIntents)
                put("deepLinkDomain", proj.bridgeConfig.deepLinkDomain)
                put("adMobAppId", proj.bridgeConfig.adMobAppId)
                put("remoteConfigUrl", proj.bridgeConfig.remoteConfigUrl)
                put("gitHubCiEnabled", proj.bridgeConfig.gitHubCiEnabled)
            })

            // Pro Studio Config (Web Customizer, Bridge v2, Adaptive Icon, Security, Drawer/FAB)
            put("proConfig", JSONObject().apply {
                put("customCss", proj.proConfig.customCss)
                put("customJs", proj.proConfig.customJs)
                put("forceDarkCss", proj.proConfig.forceDarkCss)
                put("adAndPopupBlocker", proj.proConfig.adAndPopupBlocker)
                put("blockedHostsCsv", proj.proConfig.blockedHostsCsv)
                put("biometricAuthBridge", proj.proConfig.biometricAuthBridge)
                put("qrScannerBridge", proj.proConfig.qrScannerBridge)
                put("inAppReviewBridge", proj.proConfig.inAppReviewBridge)
                put("iconShape", proj.proConfig.iconShape)
                put("iconBgColorHex", proj.proConfig.iconBgColorHex)
                put("flagSecureEnabled", proj.proConfig.flagSecureEnabled)
                put("sslPinningEnabled", proj.proConfig.sslPinningEnabled)
                put("assetPreloaderEnabled", proj.proConfig.assetPreloaderEnabled)
                put("drawerEnabled", proj.proConfig.drawerEnabled)
                put("drawerHeaderSubtitle", proj.proConfig.drawerHeaderSubtitle)
                put("fabMenuEnabled", proj.proConfig.fabMenuEnabled)
                put("fabWhatsappNumber", proj.proConfig.fabWhatsappNumber)
            })

            // Build History
            put("buildHistory", JSONArray().apply {
                proj.buildHistory.forEach { h ->
                    put(JSONObject().apply {
                        put("id", h.id)
                        put("versionCode", h.versionCode)
                        put("versionName", h.versionName)
                        put("targetType", h.targetType)
                        put("fileName", h.fileName)
                        put("sizeText", h.sizeText)
                        put("timestamp", h.timestamp)
                    })
                }
            })
        }
    }

    private fun parseProject(obj: JSONObject): AppProject {
        val stName = obj.optString("sourceType", SourceType.ZIP_ARCHIVE.name)
        val st = try { SourceType.valueOf(stName) } catch (e: Exception) { SourceType.ZIP_ARCHIVE }
        val statusName = obj.optString("lastBuildStatus", BuildStatus.IDLE.name)
        val bs = try { BuildStatus.valueOf(statusName) } catch (e: Exception) { BuildStatus.IDLE }

        val splashObj = obj.optJSONObject("splashConfig")
        val splash = if (splashObj != null) {
            SplashConfig(
                enabled = splashObj.optBoolean("enabled", true),
                backgroundColorHex = splashObj.optString("backgroundColorHex", "#0F1318"),
                fadeBackground = splashObj.optBoolean("fadeBackground", false),
                indicatorStyle = splashObj.optString("indicatorStyle", "حلقة"),
                iconSizeDp = splashObj.optInt("iconSizeDp", 120),
                iconCornerRadiusDp = splashObj.optInt("iconCornerRadiusDp", 16),
                indicatorSizeDp = splashObj.optInt("indicatorSizeDp", 48),
                spacingDp = splashObj.optInt("spacingDp", 28)
            )
        } else SplashConfig()

        val onbObj = obj.optJSONObject("onboardingConfig")
        val onboarding = if (onbObj != null) {
            val slidesArr = onbObj.optJSONArray("slides")
            val slides = mutableListOf<OnboardingSlide>()
            if (slidesArr != null) {
                for (i in 0 until slidesArr.length()) {
                    val s = slidesArr.getJSONObject(i)
                    slides.add(
                        OnboardingSlide(
                            title = s.optString("title", "العنوان"),
                            description = s.optString("description", "الوصف"),
                            iconName = s.optString("iconName", "bolt"),
                            imageUri = s.optString("imageUri").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
            OnboardingConfig(
                enabled = onbObj.optBoolean("enabled", false),
                slides = slides.ifEmpty { listOf(OnboardingSlide("العنوان", "الوصف", "bolt")) },
                layoutType = onbObj.optString("layoutType", "بطاقة سفلية"),
                imageShape = onbObj.optString("imageShape", "دائرة"),
                gradientBackground = onbObj.optBoolean("gradientBackground", false),
                accentColorHex = onbObj.optString("accentColorHex", "#FFE236"),
                textColorHex = onbObj.optString("textColorHex", "#111827"),
                cardColorHex = onbObj.optString("cardColorHex", "#FFFFFF"),
                showSkipButton = onbObj.optBoolean("showSkipButton", true)
            )
        } else OnboardingConfig()

        val wvObj = obj.optJSONObject("webViewConfig")
        val webView = if (wvObj != null) {
            val loaderObj = wvObj.optJSONObject("pageLoader")
            val loader = if (loaderObj != null) {
                PageLoaderConfig(
                    enabled = loaderObj.optBoolean("enabled", false),
                    style = loaderObj.optString("style", "حلقة"),
                    sizeDp = loaderObj.optInt("sizeDp", 78),
                    thicknessDp = loaderObj.optInt("thicknessDp", 7),
                    showOnCard = loaderObj.optBoolean("showOnCard", true),
                    cardColorHex = loaderObj.optString("cardColorHex", "#FFFFFF"),
                    cardOpacityPercent = loaderObj.optInt("cardOpacityPercent", 100),
                    cardCornerRadiusDp = loaderObj.optInt("cardCornerRadiusDp", 12)
                )
            } else PageLoaderConfig()
            WebViewConfig(
                fullScreen = wvObj.optBoolean("fullScreen", false),
                pullToRefresh = wvObj.optBoolean("pullToRefresh", false),
                pageLoader = loader,
                supportZoom = wvObj.optBoolean("supportZoom", false),
                cacheEnabled = wvObj.optBoolean("cacheEnabled", true),
                fileDownload = wvObj.optBoolean("fileDownload", true),
                offlineFallback = wvObj.optBoolean("offlineFallback", true)
            )
        } else WebViewConfig()

        val bnObj = obj.optJSONObject("bottomNavConfig")
        val bottomNav = if (bnObj != null) {
            val destArr = bnObj.optJSONArray("destinations")
            val dests = mutableListOf<BottomNavDestination>()
            if (destArr != null) {
                for (i in 0 until destArr.length()) {
                    val d = destArr.getJSONObject(i)
                    dests.add(
                        BottomNavDestination(
                            id = d.optString("id", "${i + 1}"),
                            title = d.optString("title", "Home"),
                            targetUrl = d.optString("targetUrl", "/"),
                            iconName = d.optString("iconName", "home")
                        )
                    )
                }
            }
            BottomNavConfig(
                enabled = bnObj.optBoolean("enabled", false),
                destinations = dests.ifEmpty { listOf(BottomNavDestination("1", "Home", "/")) },
                barStyle = bnObj.optString("barStyle", "مسطح"),
                showLabels = bnObj.optBoolean("showLabels", true),
                activeColorHex = bnObj.optString("activeColorHex", "#FF7A2F"),
                inactiveColorHex = bnObj.optString("inactiveColorHex", "#9CA3AF"),
                barColorHex = bnObj.optString("barColorHex", "#FFFFFF")
            )
        } else BottomNavConfig()

        val pkgObj = obj.optJSONObject("packageConfig")
        val pkg = if (pkgObj != null) {
            PackageConfig(
                packageName = pkgObj.optString("packageName", "com.zk.app"),
                versionName = pkgObj.optString("versionName", "1.0"),
                versionCode = pkgObj.optInt("versionCode", 1),
                minSdk = pkgObj.optInt("minSdk", 24),
                targetSdk = pkgObj.optInt("targetSdk", 34)
            )
        } else {
            PackageConfig(
                packageName = obj.optString("packageName", "com.zk.app"),
                versionName = obj.optString("versionName", "1.0"),
                versionCode = obj.optInt("versionCode", 1)
            )
        }

        val permObj = obj.optJSONObject("permissionsConfig")
        val perms = if (permObj != null) {
            PermissionsConfig(
                camera = permObj.optBoolean("camera", false),
                location = permObj.optBoolean("location", false),
                microphone = permObj.optBoolean("microphone", false),
                notifications = permObj.optBoolean("notifications", false),
                storage = permObj.optBoolean("storage", false),
                vibration = permObj.optBoolean("vibration", false),
                bluetooth = permObj.optBoolean("bluetooth", false),
                networkState = permObj.optBoolean("networkState", false),
                autoDetected = permObj.optBoolean("autoDetected", false),
                storeJustificationNotes = permObj.optString("storeJustificationNotes", "")
            )
        } else PermissionsConfig()

        val pushObj = obj.optJSONObject("pushConfig")
        val push = if (pushObj != null) {
            PushNotificationConfig(
                enabled = pushObj.optBoolean("enabled", false),
                provider = pushObj.optString("provider", "OneSignal"),
                appId = pushObj.optString("appId", ""),
                channelName = pushObj.optString("channelName", "التنبيهات العامة"),
                soundEnabled = pushObj.optBoolean("soundEnabled", true),
                vibrationEnabled = pushObj.optBoolean("vibrationEnabled", true),
                badgeEnabled = pushObj.optBoolean("badgeEnabled", true),
                openUrlOnTap = pushObj.optBoolean("openUrlOnTap", true),
                autoPromptOnFirstLaunch = pushObj.optBoolean("autoPromptOnFirstLaunch", true),
                testTitle = pushObj.optString("testTitle", "مرحباً بك في التطبيق!"),
                testBody = pushObj.optString("testBody", "تم تفعيل الإشعارات الفورية بنجاح.")
            )
        } else PushNotificationConfig()

        val signObj = obj.optJSONObject("signingConfig")
        val signing = if (signObj != null) {
            SigningKeyConfig(
                useDebugKey = signObj.optBoolean("useDebugKey", true),
                keyAlias = signObj.optString("keyAlias", "apkforge_key"),
                keystorePath = signObj.optString("keystorePath", "")
            )
        } else SigningKeyConfig()

        val bridgeObj = obj.optJSONObject("bridgeConfig")
        val bridge = if (bridgeObj != null) {
            BridgeAndCloudConfig(
                jsBridgeEnabled = bridgeObj.optBoolean("jsBridgeEnabled", true),
                handleExternalIntents = bridgeObj.optBoolean("handleExternalIntents", true),
                deepLinkDomain = bridgeObj.optString("deepLinkDomain", ""),
                adMobAppId = bridgeObj.optString("adMobAppId", ""),
                remoteConfigUrl = bridgeObj.optString("remoteConfigUrl", ""),
                gitHubCiEnabled = bridgeObj.optBoolean("gitHubCiEnabled", true)
            )
        } else BridgeAndCloudConfig()

        val proObj = obj.optJSONObject("proConfig")
        val pro = if (proObj != null) {
            ProStudioConfig(
                customCss = proObj.optString("customCss", ""),
                customJs = proObj.optString("customJs", ""),
                forceDarkCss = proObj.optBoolean("forceDarkCss", false),
                adAndPopupBlocker = proObj.optBoolean("adAndPopupBlocker", true),
                blockedHostsCsv = proObj.optString("blockedHostsCsv", "doubleclick.net,googlesyndication.com,adservice.google.com"),
                biometricAuthBridge = proObj.optBoolean("biometricAuthBridge", true),
                qrScannerBridge = proObj.optBoolean("qrScannerBridge", true),
                inAppReviewBridge = proObj.optBoolean("inAppReviewBridge", true),
                iconShape = proObj.optString("iconShape", "squircle"),
                iconBgColorHex = proObj.optString("iconBgColorHex", "#1A1E29"),
                flagSecureEnabled = proObj.optBoolean("flagSecureEnabled", false),
                sslPinningEnabled = proObj.optBoolean("sslPinningEnabled", false),
                assetPreloaderEnabled = proObj.optBoolean("assetPreloaderEnabled", true),
                drawerEnabled = proObj.optBoolean("drawerEnabled", false),
                drawerHeaderSubtitle = proObj.optString("drawerHeaderSubtitle", "القائمة السريعة"),
                fabMenuEnabled = proObj.optBoolean("fabMenuEnabled", false),
                fabWhatsappNumber = proObj.optString("fabWhatsappNumber", "")
            )
        } else ProStudioConfig()

        val histArr = obj.optJSONArray("buildHistory")
        val history = mutableListOf<BuildHistoryEntry>()
        if (histArr != null) {
            for (i in 0 until histArr.length()) {
                val h = histArr.getJSONObject(i)
                history.add(
                    BuildHistoryEntry(
                        id = h.optString("id", "hist_$i"),
                        versionCode = h.optInt("versionCode", 1),
                        versionName = h.optString("versionName", "1.0"),
                        targetType = h.optString("targetType", "APK"),
                        fileName = h.optString("fileName", "app.apk"),
                        sizeText = h.optString("sizeText", "8.4 MB"),
                        timestamp = h.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        return AppProject(
            id = obj.optString("id", "proj_${System.currentTimeMillis()}"),
            name = obj.optString("name", "App"),
            description = obj.optString("description", ""),
            iconName = obj.optString("iconName", "default_app"),
            iconImageUri = obj.optString("iconImageUri").takeIf { it.isNotBlank() },
            primaryColorHex = obj.optString("primaryColorHex", "#FF7A2F"),
            accentColorHex = obj.optString("accentColorHex", "#FF7A2F"),
            sourceType = st,
            sourceValue = obj.optString("sourceValue", "MegaEditor_New.zip"),
            splashConfig = splash,
            onboardingConfig = onboarding,
            webViewConfig = webView,
            bottomNavConfig = bottomNav,
            packageConfig = pkg,
            permissionsConfig = perms,
            pushConfig = push,
            signingConfig = signing,
            bridgeConfig = bridge,
            proConfig = pro,
            buildHistory = history,
            lastBuildStatus = bs,
            lastBuiltAt = if (obj.has("lastBuiltAt") && obj.getLong("lastBuiltAt") > 0L) obj.getLong("lastBuiltAt") else null,
            lastOutputApk = obj.optString("lastOutputApk").takeIf { !it.isNullOrBlank() },
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }
}
