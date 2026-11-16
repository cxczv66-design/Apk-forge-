package com.example.ui.components.space

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.LocalSpaceUiPreferences
import com.example.ui.theme.SpaceColors

data class SpaceBottomNavItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val accent: Color,
    val testTag: String,
    val onClick: () -> Unit
)

/**
 * شريط تنقل سفلي فضائي زجاجي مضيء مع خط إضاءة سيان-أرجواني فوقه
 */
@Composable
fun FuturisticBottomBar(
    items: List<SpaceBottomNavItem>,
    selectedId: String,
    modifier: Modifier = Modifier,
    tagline: String? = null
) {
    val view = LocalView.current
    val prefs = LocalSpaceUiPreferences.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .shadow(18.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), spotColor = SpaceColors.NeonCyan)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        SpaceColors.MidnightBlue.copy(alpha = 0.96f),
                        SpaceColors.DeepSpace.copy(alpha = 0.98f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        SpaceColors.NeonCyan.copy(alpha = 0.65f),
                        SpaceColors.ElectricBlue.copy(alpha = 0.45f),
                        SpaceColors.NeonPurple.copy(alpha = 0.65f)
                    )
                ),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
    ) {
        if (!tagline.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(SpaceColors.NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = tagline,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    color = SpaceColors.MutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(SpaceColors.NeonPurple.copy(alpha = 0.8f), RoundedCornerShape(2.dp))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.id == selectedId
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "nav_scale_${item.id}"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) item.accent else SpaceColors.MutedText,
                    label = "nav_tint_${item.id}"
                )
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) item.accent.copy(alpha = 0.14f) else Color.Transparent
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(color = item.accent.copy(alpha = 0.45f)),
                            onClick = {
                                prefs.triggerTouchFeedback(view, isLongPress = false)
                                item.onClick()
                            }
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = iconTint,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        fontFamily = CairoFontFamily,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontSize = 10.sp,
                        color = if (isSelected) SpaceColors.White else SpaceColors.MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun defaultSpaceBottomNavItems(
    onHome: () -> Unit,
    onAiWizard: () -> Unit,
    onBuildStudio: () -> Unit,
    onDownloads: () -> Unit,
    onAccount: () -> Unit
): List<SpaceBottomNavItem> = listOf(
    SpaceBottomNavItem("dashboard", "الرئيسية", Icons.Filled.Home, SpaceColors.NeonCyan, "bottom_nav_dashboard", onHome),
    SpaceBottomNavItem("ai_wizard", "الذكاء AI", Icons.Filled.AutoAwesome, SpaceColors.NeonPurple, "bottom_nav_ai", onAiWizard),
    SpaceBottomNavItem("build_studio", "البناء", Icons.Filled.Build, SpaceColors.Gold, "bottom_nav_build", onBuildStudio),
    SpaceBottomNavItem("downloads", "التنزيلات", Icons.Filled.DownloadDone, SpaceColors.Emerald, "bottom_nav_downloads", onDownloads),
    SpaceBottomNavItem("account", "الحساب", Icons.Filled.AccountCircle, SpaceColors.ElectricBlue, "bottom_nav_account", onAccount)
)
