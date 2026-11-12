package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.*
import com.example.ui.theme.*

@Composable
fun PhoneMockupFrame(
    step: Int,
    appName: String,
    accentColor: Color,
    iconUri: String?,
    splashBgColor: Color = Color(0xFF0F1318),
    splashFadeBg: Boolean = false,
    splashIndicatorStyle: String = "حلقة",
    splashIconSize: Int = 120,
    splashIconCorner: Int = 16,
    splashIndicatorSize: Int = 48,
    splashSpacing: Int = 28,
    onboardingEnabled: Boolean = false,
    onboardingSlides: List<OnboardingSlide> = emptyList(),
    fullScreen: Boolean = false,
    pullToRefresh: Boolean = false,
    bottomNavEnabled: Boolean = false,
    bottomDestinations: List<BottomNavDestination> = emptyList(),
    packageName: String = "com.zk.app",
    versionName: String = "1.0",
    permCamera: Boolean = false,
    permLocation: Boolean = false,
    permMicrophone: Boolean = false,
    pushEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    height: Dp = 260.dp,
    isInteractive: Boolean = false
) {
    // Outer smartphone bezel frame
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = accentColor.copy(alpha = 0.35f),
                ambientColor = Color.Black
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF151620))
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        accentColor.copy(alpha = 0.8f),
                        Color(0xFF2C2F40),
                        accentColor.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(6.dp)
    ) {
        // Inner Screen Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0A0B10))
        ) {
            // Live step content
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "phone_step_content"
            ) { targetStep ->
                when (targetStep) {
                    1 -> PhoneSourceStepPreview(accentColor)
                    2 -> PhoneLauncherStepPreview(appName, accentColor, iconUri)
                    3 -> PhoneSplashStepPreview(
                        accentColor = accentColor,
                        iconUri = iconUri,
                        bgColor = splashBgColor,
                        indicatorStyle = splashIndicatorStyle,
                        iconSize = splashIconSize,
                        iconCorner = splashIconCorner,
                        indicatorSize = splashIndicatorSize,
                        spacing = splashSpacing
                    )
                    4 -> PhoneOnboardingStepPreview(appName, accentColor, onboardingEnabled, onboardingSlides)
                    5 -> PhoneFeaturesStepPreview(appName, accentColor, fullScreen, pullToRefresh)
                    6 -> PhoneBottomNavStepPreview(appName, accentColor, bottomNavEnabled, bottomDestinations)
                    7 -> PhonePackagingStepPreview(appName, packageName, versionName, accentColor)
                    8 -> PhonePermissionsStepPreview(appName, permCamera, permLocation, permMicrophone, accentColor)
                    9 -> PhonePushStepPreview(appName, iconUri, accentColor, pushEnabled)
                    else -> PhoneLauncherStepPreview(appName, accentColor, iconUri)
                }
            }

            // Top Status Bar (unless in fullScreen mode)
            if (!fullScreen || step != 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12:45",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    // Dynamic Notch / Camera Hole
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
                        Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(11.dp))
                    }
                }
            }

            // Bottom Navigation Gesture Pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .width(48.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 1 PREVIEW: Source Code & Extraction
// -------------------------------------------------------------
@Composable
private fun PhoneSourceStepPreview(accentColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E15))
            .padding(top = 28.dp, start = 14.dp, end = 14.dp, bottom = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Archive, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "استيراد الأصول والمصدر",
                fontFamily = CairoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color.White
            )
            Text(
                text = "جاهز لمعالجة ملفات HTML / ZIP وتحويلها لكود أندرويد",
                fontFamily = TajawalFontFamily,
                fontSize = 10.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -------------------------------------------------------------
// STEP 2 PREVIEW: Android Home Screen Launcher with App Icon & Name
// -------------------------------------------------------------
@Composable
private fun PhoneLauncherStepPreview(
    appName: String,
    accentColor: Color,
    iconUri: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A1829),
                        Color(0xFF0F172A),
                        Color(0xFF0B0C14)
                    )
                )
            )
            .padding(top = 26.dp, start = 14.dp, end = 14.dp, bottom = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Clock & Date Widget on launcher
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "الخميس، 12 نوفمبر",
                    fontFamily = CairoFontFamily,
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "12:45",
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Light,
                    fontSize = 28.sp,
                    color = Color.White
                )
            }

            // Central Highlight: The User's App on Home Screen!
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = accentColor)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!iconUri.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(iconUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = appName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (appName.isBlank()) "تطبيقك" else appName,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }

            // Launcher Dock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Icon(Icons.Default.Message, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Icon(Icons.Default.Language, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3 PREVIEW: Live Splash Screen with Custom Colors & Spinner
// -------------------------------------------------------------
@Composable
private fun PhoneSplashStepPreview(
    accentColor: Color,
    iconUri: String?,
    bgColor: Color,
    indicatorStyle: String,
    iconSize: Int,
    iconCorner: Int,
    indicatorSize: Int,
    spacing: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(top = 28.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Scaled custom app icon
            Box(
                modifier = Modifier
                    .size((iconSize * 0.38f).coerceIn(36f, 72f).dp)
                    .clip(RoundedCornerShape((iconCorner * 0.38f).dp))
                    .background(accentColor)
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape((iconCorner * 0.38f).dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!iconUri.isNullOrBlank()) {
                    AsyncImage(
                        model = iconUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height((spacing * 0.35f).coerceIn(8f, 26f).dp))

            // Spinner styles matching user's choice
            when (indicatorStyle) {
                "بدون" -> {}
                "شريط" -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .width((indicatorSize * 0.7f).dp)
                            .height(4.dp)
                            .clip(CircleShape),
                        color = accentColor,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
                "نبض" -> {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                }
                else -> { // "حلقة", "حلقة - L", "حلقة - S"
                    CircularProgressIndicator(
                        modifier = Modifier.size((indicatorSize * 0.35f).coerceIn(16f, 34f).dp),
                        color = accentColor,
                        strokeWidth = 2.5.dp
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4 PREVIEW: Onboarding Carousel inside Phone
// -------------------------------------------------------------
@Composable
private fun PhoneOnboardingStepPreview(
    appName: String,
    accentColor: Color,
    enabled: Boolean,
    slides: List<OnboardingSlide>
) {
    val activeSlide = slides.firstOrNull() ?: OnboardingSlide("مرحباً بك", "استكشف تطبيقك الجديد")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1118))
            .padding(top = 26.dp, start = 14.dp, end = 14.dp, bottom = 12.dp)
    ) {
        if (!enabled) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "شاشات التعريف غير مفعلة",
                    fontFamily = CairoFontFamily,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Stars, contentDescription = null, tint = accentColor, modifier = Modifier.size(30.dp))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = activeSlide.title,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeSlide.description,
                        fontFamily = TajawalFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                // Slide button & dots
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(12.dp, 4.dp).clip(CircleShape).background(accentColor))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ابدأ الاستخدام",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 5 PREVIEW: Features & WebView Browser Simulator
// -------------------------------------------------------------
@Composable
private fun PhoneFeaturesStepPreview(
    appName: String,
    accentColor: Color,
    fullScreen: Boolean,
    pullToRefresh: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF13151F))
            .padding(top = if (fullScreen) 8.dp else 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Simulated App Top Bar
            if (!fullScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1B1D2B))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = appName, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            // Pull to refresh indicator simulation if enabled
            if (pullToRefresh) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            // Simulated web content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF222538)))
                Box(modifier = Modifier.fillMaxWidth().height(18.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1C1E2E)))
                Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF1C1E2E)))
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 6 PREVIEW: Live Bottom Navigation with Clickable Tabs
// -------------------------------------------------------------
@Composable
private fun PhoneBottomNavStepPreview(
    appName: String,
    accentColor: Color,
    enabled: Boolean,
    destinations: List<BottomNavDestination>
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App content area showing active page
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val currentTitle = destinations.getOrNull(selectedIndex)?.title ?: "الرئيسية"
                    Text(
                        text = "صفحة: $currentTitle",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يتم التبديل بسلاسة داخل التطبيق",
                        fontFamily = TajawalFontFamily,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            // The Bottom Navigation Bar
            if (enabled && destinations.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(Color(0xFF1A1C29))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.take(5).forEachIndexed { index, dest ->
                        val isSelected = index == selectedIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 6.dp)
                        ) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Home
                                    1 -> Icons.Default.Favorite
                                    2 -> Icons.Default.Store
                                    3 -> Icons.Default.Person
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null,
                                tint = if (isSelected) accentColor else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = dest.title,
                                fontFamily = CairoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) accentColor else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 7 PREVIEW: Android Settings > App Info Screen
// -------------------------------------------------------------
@Composable
private fun PhonePackagingStepPreview(
    appName: String,
    packageName: String,
    versionName: String,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF11121A))
            .padding(top = 26.dp, start = 12.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "معلومات التطبيق", fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = appName, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            Text(text = "الإصدار $versionName", fontFamily = TajawalFontFamily, fontSize = 9.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A1C29))
                    .padding(8.dp)
            ) {
                Text(
                    text = "الحزمة: $packageName",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = accentColor,
                    maxLines = 1
                )
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 8 PREVIEW: Android Runtime Permission Dialog
// -------------------------------------------------------------
@Composable
private fun PhonePermissionsStepPreview(
    appName: String,
    camera: Boolean,
    location: Boolean,
    mic: Boolean,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D14))
            .padding(top = 24.dp, start = 12.dp, end = 12.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1E202E))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = when {
                        camera -> Icons.Default.CameraAlt
                        location -> Icons.Default.LocationOn
                        mic -> Icons.Default.Mic
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "هل تسمح لتطبيق $appName بالوصول إلى أذونات الجهاز؟",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "أثناء استخدام التطبيق فقط", fontFamily = CairoFontFamily, fontSize = 9.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "عدم السماح", fontFamily = CairoFontFamily, fontSize = 9.sp, color = TextSecondary)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 9 PREVIEW: Push Notification Lock Screen Banner
// -------------------------------------------------------------
@Composable
private fun PhonePushStepPreview(
    appName: String,
    iconUri: String?,
    accentColor: Color,
    pushEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF161528), Color(0xFF0C0D14)))
            )
            .padding(top = 26.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(text = "12:45", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 20.sp, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))

            // Notification Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF202334))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = appName, fontFamily = CairoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                            Text(text = "الآن", fontFamily = TajawalFontFamily, fontSize = 8.sp, color = TextMuted)
                        }
                        Text(
                            text = if (pushEnabled) "إشعار تجريبي: مرحباً بك في تطبيقك الجديد!" else "الإشعارات جاهزة للتفعيل عبر OneSignal",
                            fontFamily = TajawalFontFamily,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
