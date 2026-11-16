package com.example.ui.components.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.SpaceDimensions
import com.example.ui.theme.TajawalFontFamily

/**
 * شريط علوي فضائي موحد يدعم زر الرجوع وعنوان الشاشة وإجراء يمين/يسار اختياري مع statusBarsPadding
 */
@Composable
fun PremiumTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    backTestTag: String = "topbar_back_btn",
    accentColor: Color = SpaceColors.NeonCyan,
    trailingContent: (@Composable RowScope.() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SpaceColors.DeepSpace.copy(alpha = 0.96f),
                        SpaceColors.MidnightBlue.copy(alpha = 0.85f),
                        Color.Transparent
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SpaceDimensions.TopBarHeight)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onBack != null) {
                NeonIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    onClick = onBack,
                    tint = SpaceColors.White,
                    borderAccent = accentColor,
                    testTag = backTestTag
                )
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = SpaceColors.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontFamily = TajawalFontFamily,
                        fontSize = 11.sp,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailingContent != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = trailingContent
                )
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        // Subtle glowing cyan/purple horizon hairline under top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.55f),
                            SpaceColors.NeonPurple.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}
