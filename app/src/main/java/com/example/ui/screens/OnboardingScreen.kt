package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.space.*
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPageData(
            title = "مصنع تطبيقات متكامل على هاتفك",
            description = "حوّل مواقع الويب وملفات HTML ومشاريعك إلى تطبيقات أندرويد حقيقية بصيغة APK و AAB بأعلى سرعة وكفاءة.",
            icon = Icons.Default.Build,
            badge = "الجيل الجديد من App Builders",
            accent = SpaceColors.NeonCyan,
            secondary = SpaceColors.ElectricBlue
        ),
        OnboardingPageData(
            title = "محرك Quantum للبناء داخل التطبيق",
            description = "يجمع التطبيق أدوات aapt2 و d8 و zipalign ومكتبة Google apksig الرسمية (v1-v4) مع تشفير AndroidKeyStore AES-256-GCM مباشرة داخل هاتفك دون الحاجة لأي تطبيق خارجي.",
            icon = Icons.Default.Terminal,
            badge = "بناء وتوقيع مدمج 100%",
            accent = SpaceColors.NeonPurple,
            secondary = SpaceColors.NeonCyan
        ),
        OnboardingPageData(
            title = "معالج تخصيص من 9 خطوات",
            description = "تحكم كامل في الهوية، الأيقونة، شاشات البداية، ميزات WebView، الشريط السفلي، والأذونات مع واجهة فضائية مستقبلية فائقة.",
            icon = Icons.Default.Speed,
            badge = "تصميم Space 3D فاخر",
            accent = SpaceColors.Gold,
            secondary = SpaceColors.Emerald
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Logo + Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SpaceApkForgeLogoMark(size = 34.dp)
                    Column {
                        Text(
                            text = "ApkForge",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontStyle = FontStyle.Italic,
                            fontSize = 18.sp,
                            color = SpaceColors.White
                        )
                        Text(
                            text = "Space Studio",
                            fontFamily = TajawalFontFamily,
                            fontSize = 10.sp,
                            color = SpaceColors.NeonCyan
                        )
                    }
                }

                if (currentPage < pages.size - 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SpaceColors.SurfaceElevated.copy(alpha = 0.7f))
                            .border(1.dp, SpaceColors.GlassBorderSubtle, RoundedCornerShape(12.dp))
                            .clickable { onFinish() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "تخطي",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SpaceColors.MutedText
                        )
                    }
                }
            }

            // Center Animated Content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (fadeIn() + slideInHorizontally { width -> if (targetState > initialState) width else -width })
                        .togetherWith(fadeOut() + slideOutHorizontally { width -> if (targetState > initialState) -width else width })
                },
                label = "onboarding_animation"
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 3D Neon Glass Showcase
                    NeonGlassCard(
                        accentColor = page.accent,
                        secondaryAccent = page.secondary,
                        shape = RoundedCornerShape(34.dp),
                        elevation = 20.dp,
                        modifier = Modifier.size(152.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Space3DFeatureBadge(
                                icon = page.icon,
                                accentColor = page.accent,
                                secondaryColor = page.secondary,
                                size = 86.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Neon Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(page.accent.copy(alpha = 0.16f))
                            .border(1.dp, page.accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = page.badge,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = page.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = page.title,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 23.sp,
                        color = SpaceColors.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = page.description,
                        fontFamily = TajawalFontFamily,
                        fontSize = 15.sp,
                        color = SpaceColors.MutedText,
                        textAlign = TextAlign.Center,
                        lineHeight = 25.sp
                    )
                }
            }

            // Bottom Navigation and Action Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Neon Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = index == currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(7.dp)
                                .width(if (isSelected) 28.dp else 7.dp)
                                .shadow(
                                    elevation = if (isSelected) 8.dp else 0.dp,
                                    shape = CircleShape,
                                    spotColor = SpaceColors.NeonCyan
                                )
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(listOf(SpaceColors.NeonCyan, SpaceColors.NeonPurple))
                                    } else {
                                        Brush.horizontalGradient(listOf(SpaceColors.SurfaceElevated, SpaceColors.SurfaceElevated))
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                NeonButton(
                    text = if (currentPage == pages.size - 1) "ابدأ الاستخدام الآن" else "التالي",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    style = if (currentPage == pages.size - 1) NeonButtonStyle.PURPLE_AI else NeonButtonStyle.PRIMARY_GLOW,
                    onClick = {
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "onboarding_action_button"
                )
            }
        }
    }
}

private data class OnboardingPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val badge: String,
    val accent: Color,
    val secondary: Color
)
