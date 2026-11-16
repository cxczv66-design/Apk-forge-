package com.example.ui.components.space

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.SpaceShapes
import com.example.ui.theme.TajawalFontFamily

/**
 * نافذة حوار زجاجية فضائية موحدة بحدود نيون ثلاثية الأبعاد
 */
@Composable
fun SpaceDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = SpaceColors.NeonCyan,
    secondaryAccent: Color = SpaceColors.NeonPurple,
    widthFraction: Float = 0.93f,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceColors.DeepSpace.copy(alpha = 0.82f))
                .clickable(onClick = onDismissRequest)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color.Transparent,
                shape = SpaceShapes.Dialog,
                border = BorderStroke(
                    width = 1.4.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.85f),
                            SpaceColors.White.copy(alpha = 0.16f),
                            secondaryAccent.copy(alpha = 0.65f)
                        )
                    )
                ),
                modifier = modifier
                    .fillMaxWidth(widthFraction)
                    .widthIn(max = 560.dp)
                    .shadow(24.dp, SpaceShapes.Dialog, spotColor = accentColor.copy(alpha = 0.65f))
                    .clip(SpaceShapes.Dialog)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                SpaceColors.SurfaceElevated,
                                SpaceColors.MidnightBlue,
                                SpaceColors.DeepSpace
                            )
                        )
                    )
                    .clickable(enabled = false) {}
                    .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (icon != null) {
                                Space3DFeatureBadge(
                                    icon = icon,
                                    accentColor = accentColor,
                                    secondaryColor = secondaryAccent,
                                    size = 40.dp
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontFamily = CairoFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = SpaceColors.White
                                )
                                if (!subtitle.isNullOrBlank()) {
                                    Text(
                                        text = subtitle,
                                        fontFamily = TajawalFontFamily,
                                        fontSize = 11.sp,
                                        color = SpaceColors.MutedText
                                    )
                                }
                            }
                        }

                        NeonIconButton(
                            icon = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            onClick = onDismissRequest,
                            tint = SpaceColors.MutedText,
                            borderAccent = SpaceColors.GlassBorderSubtle,
                            size = 44.dp
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(accentColor.copy(alpha = 0.5f), secondaryAccent.copy(alpha = 0.4f), Color.Transparent)
                                )
                            )
                    )
                    Spacer(Modifier.height(14.dp))

                    content()
                }
            }
        }
    }
}
