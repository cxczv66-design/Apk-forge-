package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    isTermuxReady: Boolean,
    onDiagnosticClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Identity
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(AmberPrimary, OrangeNeon)
                        )
                    )
                    .border(1.dp, GoldAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "AppForge Logo",
                    tint = Color(0xFF140C00),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AppForge",
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmberPrimary.copy(alpha = 0.15f))
                            .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            fontFamily = CairoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = AmberPrimary
                        )
                    }
                }
                Text(
                    text = "مصنع التطبيقات • Termux Engine",
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Termux Connection Pill
        Box(
            modifier = Modifier
                .testTag("termux_status_pill")
                .clip(RoundedCornerShape(30.dp))
                .background(
                    if (isTermuxReady) SuccessGreen.copy(alpha = 0.12f)
                    else WarningYellow.copy(alpha = 0.15f)
                )
                .border(
                    1.dp,
                    if (isTermuxReady) SuccessGreen.copy(alpha = 0.4f)
                    else WarningYellow.copy(alpha = 0.5f),
                    RoundedCornerShape(30.dp)
                )
                .clickable { onDiagnosticClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isTermuxReady) SuccessGreen else WarningYellow)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isTermuxReady) "Termux متصل" else "توصيف Termux",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = if (isTermuxReady) SuccessGreen else WarningYellow
                )
            }
        }
    }
}
