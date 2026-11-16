package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.space.NeonButton
import com.example.ui.components.space.NeonButtonStyle
import com.example.ui.components.space.SpaceDialog
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.SpaceColors

@Composable
fun ModernColorPickerDialog(
    title: String = "لون التمييز",
    initialHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    val initialHsv = remember(initialHex) {
        val hsv = FloatArray(3)
        try {
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(initialHex), hsv)
        } catch (_: Exception) {
            hsv[0] = 190f
            hsv[1] = 0.88f
            hsv[2] = 1.0f
        }
        hsv
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember { mutableStateOf(initialHex.uppercase()) }

    fun currentColor(): Color {
        val intCol = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
        return Color(intCol)
    }

    fun currentHex(): String {
        val intCol = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
        val r = (intCol shr 16) and 0xFF
        val g = (intCol shr 8) and 0xFF
        val b = intCol and 0xFF
        return String.format("#%02X%02X%02X", r, g, b)
    }

    val swatches = listOf(
        "#00E5FF",
        "#1677FF",
        "#A020F0",
        "#00E6A0",
        "#FFB52E",
        "#FF365E",
        "#061329"
    )

    SpaceDialog(
        onDismissRequest = onDismiss,
        title = title,
        subtitle = "انتقاء تدرج ولون النيون المخصص",
        icon = Icons.Filled.Palette,
        accentColor = SpaceColors.NeonCyan,
        secondaryAccent = SpaceColors.NeonPurple,
        testTag = "color_picker_dialog"
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(12.dp, RoundedCornerShape(14.dp), spotColor = currentColor())
                    .clip(RoundedCornerShape(14.dp))
                    .background(currentColor())
                    .border(1.4.dp, SpaceColors.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.height(16.dp))

            val hueColors = listOf(
                Color(0xFFFF0000),
                Color(0xFFFFFF00),
                Color(0xFF00FF00),
                Color(0xFF00FFFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000)
            )

            var hueBarSize by remember { mutableStateOf(IntSize.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .onSizeChanged { hueBarSize = it }
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.horizontalGradient(hueColors))
                    .border(1.dp, SpaceColors.White.copy(alpha = 0.25f), RoundedCornerShape(13.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (hueBarSize.width > 0) {
                                val fraction = (offset.x / hueBarSize.width.toFloat()).coerceIn(0f, 1f)
                                hue = fraction * 360f
                                hexInput = currentHex()
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            if (hueBarSize.width > 0) {
                                val fraction = (change.position.x / hueBarSize.width.toFloat()).coerceIn(0f, 1f)
                                hue = fraction * 360f
                                hexInput = currentHex()
                            }
                        }
                    }
            ) {
                val thumbOffsetFraction = (hue / 360f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(thumbOffsetFraction),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, SpaceColors.DeepSpace, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            var shadeBarSize by remember { mutableStateOf(IntSize.Zero) }
            val pureHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .onSizeChanged { shadeBarSize = it }
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.White, pureHueColor, Color(0xFF0A0E24), Color.Black)
                        )
                    )
                    .border(1.dp, SpaceColors.White.copy(alpha = 0.25f), RoundedCornerShape(13.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (shadeBarSize.width > 0) {
                                val fraction = (offset.x / shadeBarSize.width.toFloat()).coerceIn(0f, 1f)
                                if (fraction < 0.5f) {
                                    saturation = (fraction * 2f).coerceIn(0f, 1f)
                                    brightness = 1f
                                } else {
                                    saturation = 1f
                                    brightness = (1f - (fraction - 0.5f) * 2f).coerceIn(0.1f, 1f)
                                }
                                hexInput = currentHex()
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            if (shadeBarSize.width > 0) {
                                val fraction = (change.position.x / shadeBarSize.width.toFloat()).coerceIn(0f, 1f)
                                if (fraction < 0.5f) {
                                    saturation = (fraction * 2f).coerceIn(0f, 1f)
                                    brightness = 1f
                                } else {
                                    saturation = 1f
                                    brightness = (1f - (fraction - 0.5f) * 2f).coerceIn(0.1f, 1f)
                                }
                                hexInput = currentHex()
                            }
                        }
                    }
            ) {
                val fraction = if (brightness < 1f) {
                    0.5f + (1f - brightness) * 0.5f
                } else {
                    saturation * 0.5f
                }.coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(3.dp, SpaceColors.DeepSpace, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = SpaceColors.SurfaceDeep,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SpaceColors.GlassBorderCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = hexInput,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = SpaceColors.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ألوان فضائية سريعة",
                fontFamily = CairoFontFamily,
                fontSize = 12.sp,
                color = SpaceColors.MutedText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                swatches.forEach { hex ->
                    val parsed = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(SpaceColors.NeonCyan)
                    val isSelected = hexInput.equals(hex, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parsed)
                            .border(
                                width = if (isSelected) 2.6.dp else 1.2.dp,
                                color = if (isSelected) SpaceColors.White else SpaceColors.GlassBorderSubtle,
                                shape = CircleShape
                            )
                            .clickable {
                                hexInput = hex
                                runCatching {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(hex), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    brightness = hsv[2]
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeonButton(
                    text = "إلغاء",
                    style = NeonButtonStyle.CYBER_OUTLINE,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                NeonButton(
                    text = "تم",
                    style = NeonButtonStyle.PRIMARY_GLOW,
                    onClick = {
                        onColorSelected(hexInput)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}
