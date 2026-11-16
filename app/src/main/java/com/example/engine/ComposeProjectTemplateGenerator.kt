package com.example.engine

import android.net.Uri
import com.example.domain.model.AppProject
import com.example.domain.model.SourceType

/**
 * مولّد قوالب Jetpack Compose + Material 3 الأصلية للمشاريع البرمجية (`KOTLIN_TEMPLATE` و `AI_SCRATCH`)
 * عند تصديرها كمشروع Android Studio أو بنائها سحابياً عبر Gradle + Android SDK.
 */
object ComposeProjectTemplateGenerator {

    fun isComposeSource(project: AppProject): Boolean =
        project.sourceType == SourceType.KOTLIN_TEMPLATE || project.sourceType == SourceType.AI_SCRATCH

    fun buildAppGradleKts(
        project: AppProject,
        options: BuildOptimizationOptions
    ): String {
        val pkg = project.packageConfig.packageName
        val biometricDep = if (project.proConfig.biometricAuthBridge) {
            """    implementation("androidx.biometric:biometric:1.1.0")"""
        } else ""
        val playReviewDep = if (project.proConfig.inAppReviewBridge) {
            """    implementation("com.google.android.play:review-ktx:2.0.1")"""
        } else ""

        return """
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }

            android {
                namespace = "$pkg"
                compileSdk = ${maxOf(project.packageConfig.targetSdk, 34)}

                defaultConfig {
                    applicationId = "$pkg"
                    minSdk = ${project.packageConfig.minSdk}
                    targetSdk = ${maxOf(project.packageConfig.targetSdk, 34)}
                    versionCode = ${project.packageConfig.versionCode}
                    versionName = "${project.packageConfig.versionName}"
                }

                val envKeystorePath = System.getenv("APKFORGE_KEYSTORE_PATH")
                val hasCustomReleaseKeystore = !envKeystorePath.isNullOrBlank() && file(envKeystorePath).exists()
                signingConfigs {
                    if (hasCustomReleaseKeystore) {
                        create("release") {
                            storeFile = file(envKeystorePath!!)
                            storePassword = System.getenv("APKFORGE_STORE_PASS") ?: ""
                            keyAlias = System.getenv("APKFORGE_KEY_ALIAS") ?: "${project.signingConfig.keyAlias.ifBlank { "apkforge_release" }}"
                            keyPassword = System.getenv("APKFORGE_KEY_PASS") ?: ""
                        }
                    }
                }

                buildTypes {
                    release {
                        signingConfig = if (hasCustomReleaseKeystore) {
                            signingConfigs.getByName("release")
                        } else {
                            signingConfigs.getByName("debug")
                        }
                        isMinifyEnabled = ${options.r8Minify}
                        isShrinkResources = ${options.shrinkResources}
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                }

                ndk {
                    abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
                }

                buildFeatures {
                    compose = true
                }
                composeOptions {
                    kotlinCompilerExtensionVersion = "1.5.14"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlinOptions {
                    jvmTarget = "17"
                }
            }

            dependencies {
                val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
                implementation(composeBom)
                implementation("androidx.activity:activity-compose:1.9.1")
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-graphics")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.core:core-ktx:1.13.1")
                implementation("androidx.appcompat:appcompat:1.7.0")
                implementation("com.google.android.material:material:1.12.0")
            $biometricDep
            $playReviewDep
            }
        """.trimIndent()
    }

    fun buildThemeKt(project: AppProject): String {
        val pkg = project.packageConfig.packageName
        val primaryHex = project.primaryColorHex.ifBlank { "#FF7A2F" }
        val accentHex = project.accentColorHex.ifBlank { primaryHex }
        val secondaryHex = project.omniConfig.gradientEndHex.ifBlank { "#7C3AED" }
        val defaultDark = project.proConfig.forceDarkCss || project.omniConfig.gradientEnabled

        return """
            package $pkg.ui.theme

            import androidx.compose.foundation.isSystemInDarkTheme
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.darkColorScheme
            import androidx.compose.material3.lightColorScheme
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.graphics.Color

            fun parseHexColor(hex: String, fallback: Color): Color {
                return try {
                    Color(android.graphics.Color.parseColor(hex))
                } catch (_: Exception) {
                    fallback
                }
            }

            @Composable
            fun ApkForgeTheme(
                darkTheme: Boolean = $defaultDark || isSystemInDarkTheme(),
                content: @Composable () -> Unit
            ) {
                val primary = parseHexColor("$accentHex", Color(0xFFFF7A2F))
                val secondary = parseHexColor("$secondaryHex", Color(0xFF7C3AED))
                val brandBar = parseHexColor("$primaryHex", primary)

                val colorScheme = if (darkTheme) {
                    darkColorScheme(
                        primary = primary,
                        secondary = secondary,
                        tertiary = brandBar,
                        background = Color(0xFF090D16),
                        surface = Color(0xFF131A29),
                        surfaceVariant = Color(0xFF1C263B),
                        onPrimary = Color.White,
                        onBackground = Color(0xFFF4F8FF),
                        onSurface = Color(0xFFF4F8FF)
                    )
                } else {
                    lightColorScheme(
                        primary = primary,
                        secondary = secondary,
                        tertiary = brandBar,
                        background = Color(0xFFF6F8FC),
                        surface = Color.White,
                        onPrimary = Color.White
                    )
                }

                MaterialTheme(
                    colorScheme = colorScheme,
                    content = content
                )
            }
        """.trimIndent()
    }

    fun buildComposeMainActivity(
        project: AppProject,
        relEntryAsset: String,
        hasRealWorkspaceAssets: Boolean
    ): String {
        val pkg = project.packageConfig.packageName
        val pro = project.proConfig
        val omni = project.omniConfig
        val splash = project.splashConfig
        val onboarding = project.onboardingConfig
        val bottomNav = project.bottomNavConfig

        val escapedAppTitle = esc(project.name.ifBlank { "ApkForge Compose App" })
        val escapedDesc = esc(project.description.ifBlank { "تطبيق Kotlin + Jetpack Compose مبني عبر ApkForge" })
        val escapedDrawerSubtitle = esc(pro.drawerHeaderSubtitle.ifBlank { "القائمة الرئيسية" })
        val startUrl = if (project.sourceValue.startsWith("http")) {
            project.sourceValue
        } else {
            "file:///android_asset/$relEntryAsset"
        }
        val baseOrigin = if (startUrl.startsWith("http")) {
            val uri = Uri.parse(startUrl)
            "${uri.scheme}://${uri.host}" + if (uri.port != -1) ":${uri.port}" else ""
        } else {
            "file:///android_asset"
        }

        val slidesLiteral = if (onboarding.enabled && onboarding.slides.isNotEmpty()) {
            onboarding.slides.joinToString(", ") { slide ->
                "\"${esc(slide.title)}\" to \"${esc(slide.description)}\""
            }
        } else ""

        val navDestinations = if (bottomNav.enabled && bottomNav.destinations.isNotEmpty()) {
            bottomNav.destinations.take(5)
        } else {
            listOf(
                com.example.domain.model.BottomNavDestination("home", "الرئيسية", "/", "home"),
                com.example.domain.model.BottomNavDestination("features", "الوظائف", "/features", "explore"),
                com.example.domain.model.BottomNavDestination("settings", "الإعدادات", "/settings", "person")
            )
        }

        val navItemsLiteral = navDestinations.mapIndexed { idx, dest ->
            val rawTarget = dest.targetUrl.trim()
            val resolvedTarget = when {
                rawTarget.startsWith("http://") || rawTarget.startsWith("https://") || rawTarget.startsWith("file://") -> rawTarget
                startUrl.startsWith("http") && rawTarget.startsWith("/") -> "$baseOrigin$rawTarget"
                startUrl.startsWith("http") && rawTarget.isNotBlank() -> "$baseOrigin/$rawTarget"
                rawTarget.isNotBlank() && rawTarget != "/" -> "file:///android_asset/${rawTarget.trimStart('/')}"
                else -> startUrl
            }
            val safeTitle = esc(dest.title.ifBlank { "قسم ${idx + 1}" })
            val safeIcon = esc(dest.iconName.ifBlank { "home" })
            """NavTabSpec("$safeTitle", "$resolvedTarget", "$safeIcon")"""
        }.joinToString(", ")

        val permsList = buildList {
            val p = project.permissionsConfig
            if (p.camera || pro.qrScannerBridge) add("android.permission.CAMERA" to "الكاميرا لمسح QR والتقاط الصور")
            if (p.location) add("android.permission.ACCESS_FINE_LOCATION" to "الموقع الجغرافي لتخصيص الخدمات")
            if (p.microphone) add("android.permission.RECORD_AUDIO" to "الميكروفون للتسجيل الصوتي")
            if (p.notifications || project.pushConfig.enabled) add("android.permission.POST_NOTIFICATIONS" to "الإشعارات الفورية والتحديثات")
        }
        val permsArrayLiteral = permsList.joinToString(", ") { "\"${it.first}\"" }
        val permsSummaryText = esc(permsList.joinToString(" • ") { it.second })

        val showEmbeddedWeb = hasRealWorkspaceAssets || project.sourceType == SourceType.AI_SCRATCH || project.sourceValue.startsWith("http")
        val flagSecureLine = if (pro.flagSecureEnabled) {
            "window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)"
        } else ""

        return """
            package $pkg

            import android.annotation.SuppressLint
            import android.content.Context
            import android.content.Intent
            import android.content.pm.PackageManager
            import android.net.ConnectivityManager
            import android.os.Build
            import android.os.Bundle
            import android.os.VibrationEffect
            import android.os.Vibrator
            import android.view.WindowManager
            import android.webkit.JavascriptInterface
            import android.webkit.WebChromeClient
            import android.webkit.WebResourceRequest
            import android.webkit.WebSettings
            import android.webkit.WebView
            import android.webkit.WebViewClient
            import android.widget.Toast
            import androidx.activity.ComponentActivity
            import androidx.activity.compose.rememberLauncherForActivityResult
            import androidx.activity.compose.setContent
            import androidx.activity.enableEdgeToEdge
            import androidx.activity.result.contract.ActivityResultContracts
            import androidx.compose.foundation.background
            import androidx.compose.foundation.layout.*
            import androidx.compose.foundation.lazy.LazyColumn
            import androidx.compose.foundation.lazy.items
            import androidx.compose.foundation.shape.RoundedCornerShape
            import androidx.compose.material.icons.Icons
            import androidx.compose.material.icons.filled.*
            import androidx.compose.material3.*
            import androidx.compose.runtime.*
            import androidx.compose.ui.Alignment
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.graphics.Brush
            import androidx.compose.ui.graphics.Color
            import androidx.compose.ui.graphics.vector.ImageVector
            import androidx.compose.ui.platform.LocalContext
            import androidx.compose.ui.text.font.FontWeight
            import androidx.compose.ui.unit.dp
            import androidx.compose.ui.viewinterop.AndroidView
            import androidx.core.content.ContextCompat
            import $pkg.ui.theme.ApkForgeTheme
            import $pkg.ui.theme.parseHexColor
            import kotlinx.coroutines.delay
            import kotlinx.coroutines.launch

            private data class NavTabSpec(
                val title: String,
                val targetUrl: String,
                val iconKey: String
            )

            class MainActivity : ComponentActivity() {

                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    enableEdgeToEdge()
                    $flagSecureLine

                    setContent {
                        ApkForgeTheme {
                            ApkForgeComposeRoot()
                        }
                    }
                }
            }

            private fun iconForKey(key: String): ImageVector = when (key.lowercase()) {
                "explore", "categories", "grid" -> Icons.Filled.Explore
                "person", "profile", "account" -> Icons.Filled.Person
                "settings" -> Icons.Filled.Settings
                "bolt", "ai" -> Icons.Filled.Bolt
                else -> Icons.Filled.Home
            }

            @OptIn(ExperimentalMaterial3Api::class)
            @Composable
            private fun ApkForgeComposeRoot() {
                val context = LocalContext.current
                val prefs = remember { context.getSharedPreferences("apkforge_compose_prefs", Context.MODE_PRIVATE) }
                val coroutineScope = rememberCoroutineScope()

                val navTabs = remember { listOf($navItemsLiteral) }
                val onboardingSlides = remember { listOf($slidesLiteral) }

                var showSplash by remember { mutableStateOf(${splash.enabled}) }
                var showOnboarding by remember {
                    mutableStateOf(${onboarding.enabled && onboarding.slides.isNotEmpty()} && !prefs.getBoolean("onboarding_done", false))
                }
                var onboardingIndex by remember { mutableIntStateOf(0) }
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                var activeWebUrl by remember { mutableStateOf("$startUrl") }
                var showNativeWorkspaceOverlay by remember { mutableStateOf(${!showEmbeddedWeb}) }

                val reqPerms = remember { arrayOf($permsArrayLiteral) }
                var showPermissionCard by remember {
                    mutableStateOf(
                        ${omni.smartPermissionsRationaleEnabled} && reqPerms.any { rp ->
                            !(rp == "android.permission.POST_NOTIFICATIONS" && Build.VERSION.SDK_INT < 33) &&
                                ContextCompat.checkSelfPermission(context, rp) != PackageManager.PERMISSION_GRANTED
                        }
                    )
                }
                val permLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    showPermissionCard = false
                }

                // Local DB notes backed by SharedPreferences when omni.localDbEnabled
                var noteInput by remember { mutableStateOf("") }
                var savedNotes by remember {
                    mutableStateOf(
                        prefs.getString("${esc(omni.localDbTableName)}_rows", "")
                            ?.split("\n")
                            ?.filter { it.isNotBlank() }
                            ?: emptyList()
                    )
                }
                var signedInUser by remember {
                    mutableStateOf(prefs.getString("auth_user_email", "") ?: "")
                }

                LaunchedEffect(showSplash) {
                    if (showSplash) {
                        delay(1350L)
                        showSplash = false
                    }
                }

                val accentColor = parseHexColor("${project.accentColorHex}", MaterialTheme.colorScheme.primary)
                val gradientStart = parseHexColor("${omni.gradientStartHex}", accentColor)
                val gradientEnd = parseHexColor("${omni.gradientEndHex}", MaterialTheme.colorScheme.secondary)

                if (showSplash) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(parseHexColor("${splash.backgroundColorHex}", Color(0xFF090D16))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(${splash.spacingDp.coerceIn(8, 48)}.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bolt,
                                contentDescription = "$escapedAppTitle",
                                tint = accentColor,
                                modifier = Modifier.size(${splash.iconSizeDp.coerceIn(48, 128)}.dp)
                            )
                            Text(
                                text = "$escapedAppTitle",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if ("${esc(splash.indicatorStyle)}" != "بدون") {
                                CircularProgressIndicator(
                                    color = accentColor,
                                    modifier = Modifier.size(${splash.indicatorSizeDp.coerceIn(24, 64)}.dp)
                                )
                            }
                        }
                    }
                    return
                }

                if (showOnboarding && onboardingSlides.isNotEmpty()) {
                    val slide = onboardingSlides[onboardingIndex.coerceIn(0, onboardingSlides.lastIndex)]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ElevatedCard(
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${'$'}{onboardingIndex + 1} / ${'$'}{onboardingSlides.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor
                                )
                                Text(
                                    text = slide.first,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = slide.second,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (onboardingIndex < onboardingSlides.lastIndex) {
                                            onboardingIndex++
                                        } else {
                                            prefs.edit().putBoolean("onboarding_done", true).apply()
                                            showOnboarding = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (onboardingIndex < onboardingSlides.lastIndex) "التالي" else "ابدأ الاستخدام")
                                }
                            }
                        }
                    }
                    return
                }

                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

                val mainScaffold: @Composable () -> Unit = {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = "$escapedAppTitle",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                navigationIcon = {
                                    if (${pro.drawerEnabled}) {
                                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "القائمة")
                                        }
                                    }
                                },
                                actions = {
                                    if ($showEmbeddedWeb) {
                                        IconButton(onClick = { showNativeWorkspaceOverlay = !showNativeWorkspaceOverlay }) {
                                            Icon(
                                                imageVector = if (showNativeWorkspaceOverlay) Icons.Filled.Language else Icons.Filled.Bolt,
                                                contentDescription = "تبديل العرض"
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        val share = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "$escapedAppTitle — $escapedDesc")
                                        }
                                        context.startActivity(Intent.createChooser(share, "مشاركة"))
                                    }) {
                                        Icon(Icons.Filled.Share, contentDescription = "مشاركة")
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            if (${bottomNav.enabled} && navTabs.isNotEmpty()) {
                                NavigationBar {
                                    navTabs.forEachIndexed { idx, tab ->
                                        NavigationBarItem(
                                            selected = selectedTabIndex == idx,
                                            onClick = {
                                                selectedTabIndex = idx
                                                if (idx == 0 && $showEmbeddedWeb) {
                                                    activeWebUrl = tab.targetUrl
                                                }
                                            },
                                            icon = { Icon(iconForKey(tab.iconKey), contentDescription = tab.title) },
                                            label = if (${bottomNav.showLabels}) { { Text(tab.title) } } else null
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (${pro.fabMenuEnabled}) {
                                FloatingActionButton(
                                    onClick = {
                                        Toast.makeText(context, "$escapedAppTitle جاهز ✓", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "إجراء سريع")
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (showPermissionCard && reqPerms.isNotEmpty()) {
                                ElevatedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("الأذونات الذكية", fontWeight = FontWeight.Bold)
                                        Text(
                                            "$permsSummaryText",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(onClick = { permLauncher.launch(reqPerms) }) {
                                                Text("منح الصلاحيات")
                                            }
                                            TextButton(onClick = { showPermissionCard = false }) {
                                                Text("لاحقاً")
                                            }
                                        }
                                    }
                                }
                            }

                            if ($showEmbeddedWeb && !showNativeWorkspaceOverlay && selectedTabIndex == 0) {
                                EmbeddedComposeWebView(
                                    url = activeWebUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    brush = Brush.linearGradient(listOf(gradientStart, gradientEnd)),
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .padding(20.dp)
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = "$escapedAppTitle",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "$escapedDesc",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }

                                    if (${omni.authBuilderEnabled}) {
                                        item {
                                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Filled.Lock, contentDescription = null, tint = accentColor)
                                                        Text("وحدة المصادقة", fontWeight = FontWeight.Bold)
                                                    }
                                                    if (signedInUser.isBlank()) {
                                                        Button(onClick = {
                                                            signedInUser = "user@${pkg.substringAfterLast('.')}.app"
                                                            prefs.edit().putString("auth_user_email", signedInUser).apply()
                                                        }) {
                                                            Text("تسجيل الدخول السريع")
                                                        }
                                                    } else {
                                                        Text("مسجل كـ: ${'$'}signedInUser", color = accentColor)
                                                        TextButton(onClick = {
                                                            signedInUser = ""
                                                            prefs.edit().remove("auth_user_email").apply()
                                                        }) {
                                                            Text("تسجيل الخروج")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (${omni.localDbEnabled}) {
                                        item {
                                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Icon(Icons.Filled.Storage, contentDescription = null, tint = accentColor)
                                                        Text("سجل البيانات المحلي (${esc(omni.localDbTableName)})", fontWeight = FontWeight.Bold)
                                                    }
                                                    OutlinedTextField(
                                                        value = noteInput,
                                                        onValueChange = { noteInput = it },
                                                        label = { Text("أضف سجلاً جديداً") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true
                                                    )
                                                    Button(onClick = {
                                                        val trimmed = noteInput.trim()
                                                        if (trimmed.isNotEmpty()) {
                                                            val next = (listOf(trimmed) + savedNotes).take(25)
                                                            savedNotes = next
                                                            prefs.edit().putString("${esc(omni.localDbTableName)}_rows", next.joinToString("\n")).apply()
                                                            noteInput = ""
                                                        }
                                                    }) {
                                                        Text("حفظ السجل")
                                                    }
                                                }
                                            }
                                        }
                                        items(savedNotes) { entry ->
                                            Surface(
                                                tonalElevation = 2.dp,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accentColor)
                                                    Text(entry, modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }

                                    item {
                                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text("ملخص التهيئة السحابية", fontWeight = FontWeight.Bold)
                                                Text("الحزمة: $pkg", style = MaterialTheme.typography.bodySmall)
                                                Text("الإصدار: ${project.packageConfig.versionName} (${project.packageConfig.versionCode})", style = MaterialTheme.typography.bodySmall)
                                                Text("البناء: Jetpack Compose + Material 3", style = MaterialTheme.typography.bodySmall, color = accentColor)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (${pro.drawerEnabled}) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("$escapedAppTitle", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("$escapedDrawerSubtitle", style = MaterialTheme.typography.bodySmall, color = accentColor)
                                }
                                HorizontalDivider()
                                navTabs.forEachIndexed { idx, tab ->
                                    NavigationDrawerItem(
                                        label = { Text(tab.title) },
                                        selected = selectedTabIndex == idx,
                                        icon = { Icon(iconForKey(tab.iconKey), contentDescription = tab.title) },
                                        onClick = {
                                            selectedTabIndex = idx
                                            coroutineScope.launch { drawerState.close() }
                                        }
                                    )
                                }
                            }
                        },
                        content = mainScaffold
                    )
                } else {
                    mainScaffold()
                }
            }

            @SuppressLint("SetJavaScriptEnabled")
            @Composable
            private fun EmbeddedComposeWebView(
                url: String,
                modifier: Modifier = Modifier
            ) {
                val context = LocalContext.current
                val bridge = remember { ComposeNativeBridge(context) }
                AndroidView(
                    modifier = modifier,
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false

                                @android.annotation.TargetApi(Build.VERSION_CODES.O)
                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: android.webkit.RenderProcessGoneDetail?
                                ): Boolean {
                                    (view?.parent as? android.view.ViewGroup)?.removeView(view)
                                    view?.destroy()
                                    return true
                                }
                            }
                            if (${project.bridgeConfig.jsBridgeEnabled}) {
                                addJavascriptInterface(bridge, "ApkForge")
                            }
                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        if (webView.url != url) {
                            webView.loadUrl(url)
                        }
                    }
                )
            }

            private class ComposeNativeBridge(private val ctx: Context) {
                @JavascriptInterface
                fun showToast(message: String) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                    }
                }

                @JavascriptInterface
                fun vibrate(ms: Long) {
                    val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(ms.coerceIn(20L, 1000L), VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                }

                @JavascriptInterface
                fun getNetworkStatus(): Boolean {
                    val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
                    return cm.activeNetwork != null
                }
            }
        """.trimIndent()
    }

    private fun esc(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("\n", " ")
}
