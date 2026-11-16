package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.space.QuantumStatusCard
import com.example.ui.components.space.SpaceApkForgeLogoMark
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.TajawalFontFamily

@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    isQuantumReady: Boolean,
    onDiagnosticClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SpaceApkForgeLogoMark(size = 44.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ApkForge",
                    fontFamily = CairoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 20.sp,
                    color = SpaceColors.White
                )
                Text(
                    text = "Build • Edit • Create",
                    fontFamily = TajawalFontFamily,
                    fontSize = 11.sp,
                    color = SpaceColors.NeonCyan
                )
            }
        }

        QuantumStatusCard(
            isActive = isQuantumReady,
            onClick = onDiagnosticClick,
            compact = true,
            testTag = "quantum_status_pill"
        )
    }
}
