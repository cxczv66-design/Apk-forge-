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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassmorphismCard
import com.example.ui.components.GlowingButton
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
            badge = "الجيل الجديد من App Builders"
        ),
        OnboardingPageData(
            title = "محرك Quantum للبناء داخل التطبيق",
            description = "يجمع التطبيق أدوات aapt2 و d8 و zipalign ومكتبة Google apksig الرسمية (v1-v4) مع تشفير AndroidKeyStore AES-256-GCM مباشرة داخل هاتفك دون الحاجة لأي تطبيق خارجي.",
            icon = Icons.Default.Terminal,
            badge = "بناء وتوقيع مدمج 100%"
        ),
        OnboardingPageData(
            title = "معالج تخصيص من 9 خطوات",
            description = "تحكم كامل في الهوية، الأيقونة، شاشات البداية، ميزات WebView، الشريط السفلي، والأذونات مع واجهة زجاجية عصرية.",
            icon = Icons.Default.Speed,
            badge = "تصميم Material 3 فاخر"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AppForge Studio",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AmberPrimary
                )

                if (currentPage < pages.size - 1) {
                    Text(
                        text = "تخطي",
                        fontFamily = CairoFontFamily,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable { onFinish() }
                            .padding(8.dp)
                    )
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
                    // Hero Glass Card with Icon
                    GlassmorphismCard(
                        modifier = Modifier
                            .size(140.dp)
                            .padding(8.dp),
                        backgroundColor = DarkSurfaceElevated,
                        borderColor = AmberPrimary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(32.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AmberPrimary.copy(alpha = 0.15f))
                            .border(1.dp, AmberPrimary.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = page.badge,
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = GoldAccent
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = page.title,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = page.description,
                        fontFamily = TajawalFontFamily,
                        fontSize = 15.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }

            // Bottom Navigation and Action Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = index == currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(6.dp)
                                .width(if (isSelected) 24.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) AmberPrimary else Color(0xFF333544))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                GlowingButton(
                    text = if (currentPage == pages.size - 1) "ابدأ الاستخدام الآن" else "التالي",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
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
    val badge: String
)

