package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppProject
import com.example.ui.AppScreen
import com.example.ui.theme.*
import com.example.ui.util.strings

data class OmniNavDestination(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badge: String? = null,
    val accent: Color = OrangePrimary,
    val onClick: () -> Unit
)

/**
 * شريط الاستوديو الجانبي الموحّد لكل مراحل IDE:
 * يعرض NavigationRail على الشاشات العريضة و ModalNavigationDrawer على الهواتف.
 */
@Composable
fun OmniStudioSidebarScaffold(
    drawerState: DrawerState,
    currentScreen: AppScreen,
    activeProject: AppProject?,
    projectsCount: Int,
    downloadsCount: Int,
    onNavigate: (AppScreen) -> Unit,
    onOpenUrlScraper: () -> Unit,
    onOpenSupport: () -> Unit,
    onCloseDrawer: () -> Unit,
    content: @Composable () -> Unit
) {
    val s = strings()
    val fallbackProject = activeProject ?: AppProject(
        id = "omni_sandbox_default",
        name = "Omni Studio App",
        sourceValue = "https://example.com"
    )

    val items = listOf(
        OmniNavDestination(
            id = "dashboard",
            label = s.navHome,
            icon = Icons.Filled.SpaceDashboard,
            badge = if (projectsCount > 0) "$projectsCount" else null,
            onClick = { onNavigate(AppScreen.Dashboard); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "html_picker",
            label = s.htmlImport,
            icon = Icons.Filled.Code,
            accent = CyanAccent,
            onClick = { onNavigate(AppScreen.HtmlPicker); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "zip_picker",
            label = s.zipImport,
            icon = Icons.Filled.FolderZip,
            accent = Color(0xFF818CF8),
            onClick = { onNavigate(AppScreen.ZipPicker); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "pwa_scraper",
            label = s.webScraperAndPwa,
            icon = Icons.Filled.Language,
            accent = StatusGreen,
            onClick = { onCloseDrawer(); onOpenUrlScraper() }
        ),
        OmniNavDestination(
            id = "apk_choice",
            label = "استيراد / تعديل APK",
            icon = Icons.Filled.Android,
            accent = StatusGreen,
            onClick = { onNavigate(AppScreen.ApkImportChoice); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "ai_wizard",
            label = "مساعد البناء AI ✨",
            icon = Icons.Filled.AutoAwesome,
            badge = "12+",
            accent = Color(0xFFA855F7),
            onClick = { onNavigate(AppScreen.AiWizard); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "adaptive_studio",
            label = "الاستوديو التكيفي",
            icon = Icons.Filled.Tune,
            accent = GoldAccent,
            onClick = { onNavigate(AppScreen.AdaptiveStudio(fallbackProject, 0)); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "sandbox",
            label = s.livePreviewSandbox,
            icon = Icons.Filled.Smartphone,
            badge = "APK/AAB",
            accent = CyanAccent,
            onClick = {
                onNavigate(AppScreen.InteractivePhone(fallbackProject, fromScreen = AppScreen.Dashboard))
                onCloseDrawer()
            }
        ),
        OmniNavDestination(
            id = "build_studio",
            label = "التشخيص والخزنة",
            icon = Icons.Filled.Security,
            accent = OrangePrimary,
            onClick = { onNavigate(AppScreen.BuildCenter(fallbackProject)); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "downloads",
            label = s.navDownloads,
            icon = Icons.Filled.DownloadDone,
            badge = if (downloadsCount > 0) "$downloadsCount" else null,
            accent = StatusGreen,
            onClick = { onNavigate(AppScreen.Downloads); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "support",
            label = s.supportAndFeatureReq,
            icon = Icons.Filled.SupportAgent,
            accent = Color(0xFF38BDF8),
            onClick = { onCloseDrawer(); onOpenSupport() }
        ),
        OmniNavDestination(
            id = "account",
            label = s.navAccount,
            icon = Icons.Filled.ManageAccounts,
            accent = TextSecondary,
            onClick = { onNavigate(AppScreen.Account); onCloseDrawer() }
        )
    )

    val selectedId = when (currentScreen) {
        is AppScreen.Dashboard -> "dashboard"
        is AppScreen.HtmlPicker -> "html_picker"
        is AppScreen.ZipPicker -> "zip_picker"
        is AppScreen.ApkImportChoice -> "apk_choice"
        is AppScreen.AiWizard -> "ai_wizard"
        is AppScreen.AdaptiveStudio, is AppScreen.Wizard -> "adaptive_studio"
        is AppScreen.InteractivePhone -> "sandbox"
        is AppScreen.BuildCenter -> "build_studio"
        is AppScreen.Downloads -> "downloads"
        is AppScreen.Account -> "account"
        else -> "dashboard"
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(DarkSurface)
                        .border(1.dp, GlassBorderSubtle),
                    containerColor = DarkSurface
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "OMNI",
                        color = OrangePrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    items.take(8).forEach { dest ->
                        NavigationRailItem(
                            selected = selectedId == dest.id,
                            onClick = dest.onClick,
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = dest.accent,
                                indicatorColor = dest.accent.copy(alpha = 0.2f),
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("rail_${dest.id}")
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content()
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.widthIn(max = 310.dp),
                        drawerContainerColor = DarkSurface,
                        drawerContentColor = TextPrimary
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(OrangePrimary.copy(alpha = 0.22f), CyanAccent.copy(alpha = 0.14f))
                                        )
                                    )
                                    .border(1.dp, OrangePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "ApkForge Omni IDE", tint = OrangePrimary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("ApkForge Omni IDE", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text(s.ideStudioSubtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            items.forEach { dest ->
                                val isSel = selectedId == dest.id
                                NavigationDrawerItem(
                                    selected = isSel,
                                    onClick = dest.onClick,
                                    icon = { Icon(dest.icon, contentDescription = dest.label, tint = if (isSel) dest.accent else TextSecondary) },
                                    label = {
                                        Text(
                                            dest.label,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    badge = dest.badge?.let { b ->
                                        {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = dest.accent.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    b,
                                                    color = dest.accent,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = dest.accent.copy(alpha = 0.14f),
                                        selectedTextColor = TextPrimary,
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .testTag("drawer_${dest.id}")
                                )
                            }
                        }
                    }
                }
            ) {
                content()
            }
        }
    }
}
