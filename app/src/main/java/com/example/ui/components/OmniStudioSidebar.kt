package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppProject
import com.example.ui.AppScreen
import com.example.ui.components.space.SpaceApkForgeLogoMark
import com.example.ui.theme.*
import com.example.ui.util.strings

data class OmniNavDestination(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val badge: String? = null,
    val accent: Color = SpaceColors.NeonCyan,
    val onClick: () -> Unit
)

/**
 * القائمة الجانبية الفضائية الموحّدة (SpaceNavigationDrawer + NavigationRail للأجهزة اللوحية)
 */
@Composable
fun SpaceNavigationDrawer(
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
) = OmniStudioSidebarScaffold(
    drawerState = drawerState,
    currentScreen = currentScreen,
    activeProject = activeProject,
    projectsCount = projectsCount,
    downloadsCount = downloadsCount,
    onNavigate = onNavigate,
    onOpenUrlScraper = onOpenUrlScraper,
    onOpenSupport = onOpenSupport,
    onCloseDrawer = onCloseDrawer,
    content = content
)

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
            accent = SpaceColors.NeonCyan,
            onClick = { onNavigate(AppScreen.Dashboard); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "html_picker",
            label = s.htmlImport,
            icon = Icons.Filled.Code,
            accent = SpaceColors.ElectricBlue,
            onClick = { onNavigate(AppScreen.HtmlPicker); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "zip_picker",
            label = s.zipImport,
            icon = Icons.Filled.FolderZip,
            accent = SpaceColors.NeonCyan,
            onClick = { onNavigate(AppScreen.ZipPicker); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "pwa_scraper",
            label = s.webScraperAndPwa,
            icon = Icons.Filled.Language,
            accent = SpaceColors.Emerald,
            onClick = { onCloseDrawer(); onOpenUrlScraper() }
        ),
        OmniNavDestination(
            id = "apk_choice",
            label = "استيراد / تعديل APK",
            icon = Icons.Filled.Android,
            accent = SpaceColors.Emerald,
            onClick = { onNavigate(AppScreen.ApkImportChoice); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "ai_wizard",
            label = "مساعد البناء AI ✨",
            icon = Icons.Filled.AutoAwesome,
            badge = "12+",
            accent = SpaceColors.NeonPurple,
            onClick = { onNavigate(AppScreen.AiWizard); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "adaptive_studio",
            label = "الاستوديو التكيفي",
            icon = Icons.Filled.Tune,
            accent = SpaceColors.Gold,
            onClick = { onNavigate(AppScreen.AdaptiveStudio(fallbackProject, 0)); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "sandbox",
            label = s.livePreviewSandbox,
            icon = Icons.Filled.Smartphone,
            badge = "APK/AAB",
            accent = SpaceColors.NeonCyan,
            onClick = {
                onNavigate(AppScreen.InteractivePhone(fallbackProject, fromScreen = AppScreen.Dashboard))
                onCloseDrawer()
            }
        ),
        OmniNavDestination(
            id = "build_studio",
            label = "التشخيص والخزنة",
            icon = Icons.Filled.Security,
            accent = SpaceColors.Gold,
            onClick = { onNavigate(AppScreen.BuildCenter(fallbackProject)); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "downloads",
            label = s.navDownloads,
            icon = Icons.Filled.DownloadDone,
            badge = if (downloadsCount > 0) "$downloadsCount" else null,
            accent = SpaceColors.Emerald,
            onClick = { onNavigate(AppScreen.Downloads); onCloseDrawer() }
        ),
        OmniNavDestination(
            id = "support",
            label = s.supportAndFeatureReq,
            icon = Icons.Filled.SupportAgent,
            accent = SpaceColors.NeonCyan,
            onClick = { onCloseDrawer(); onOpenSupport() }
        ),
        OmniNavDestination(
            id = "account",
            label = s.navAccount,
            icon = Icons.Filled.ManageAccounts,
            accent = SpaceColors.ElectricBlue,
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
                        .background(
                            Brush.verticalGradient(listOf(SpaceColors.DeepSpace, SpaceColors.MidnightBlue))
                        )
                        .border(1.dp, SpaceColors.GlassBorderCyan),
                    containerColor = Color.Transparent
                ) {
                    Spacer(Modifier.height(14.dp))
                    SpaceApkForgeLogoMark(size = 36.dp)
                    Spacer(Modifier.height(10.dp))
                    items.take(9).forEach { dest ->
                        NavigationRailItem(
                            selected = selectedId == dest.id,
                            onClick = dest.onClick,
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = {
                                Text(
                                    dest.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = CairoFontFamily,
                                    fontSize = 10.sp
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = SpaceColors.White,
                                selectedTextColor = dest.accent,
                                indicatorColor = dest.accent.copy(alpha = 0.22f),
                                unselectedIconColor = SpaceColors.MutedText,
                                unselectedTextColor = SpaceColors.SubtleText
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
                        modifier = Modifier
                            .widthIn(max = 312.dp)
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(SpaceColors.NeonCyan.copy(alpha = 0.55f), SpaceColors.NeonPurple.copy(alpha = 0.45f))
                                ),
                                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                            ),
                        drawerContainerColor = SpaceColors.DeepSpace.copy(alpha = 0.96f),
                        drawerContentColor = SpaceColors.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(14.dp, RoundedCornerShape(18.dp), spotColor = SpaceColors.NeonCyan.copy(alpha = 0.5f))
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                SpaceColors.SurfaceElevated,
                                                SpaceColors.MidnightBlue,
                                                SpaceColors.Violet.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                                    .border(1.2.dp, SpaceColors.NeonBorderGradient, RoundedCornerShape(18.dp))
                                    .padding(14.dp)
                            ) {
                                SpaceApkForgeLogoMark(size = 42.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "ApkForge",
                                        color = SpaceColors.White,
                                        fontFamily = CairoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontStyle = FontStyle.Italic,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Build • Edit • Create",
                                        color = SpaceColors.NeonCyan,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            items.forEach { dest ->
                                val isSel = selectedId == dest.id
                                NavigationDrawerItem(
                                    selected = isSel,
                                    onClick = dest.onClick,
                                    icon = {
                                        Icon(
                                            dest.icon,
                                            contentDescription = dest.label,
                                            tint = if (isSel) dest.accent else SpaceColors.MutedText
                                        )
                                    },
                                    label = {
                                        Text(
                                            dest.label,
                                            fontFamily = CairoFontFamily,
                                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    badge = dest.badge?.let { b ->
                                        {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = dest.accent.copy(alpha = 0.2f),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, dest.accent.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    b,
                                                    color = dest.accent,
                                                    fontFamily = CairoFontFamily,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = dest.accent.copy(alpha = 0.16f),
                                        selectedTextColor = SpaceColors.White,
                                        unselectedContainerColor = Color.Transparent,
                                        unselectedTextColor = SpaceColors.MutedText
                                    ),
                                    shape = RoundedCornerShape(14.dp),
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
