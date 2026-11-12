package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.OrangePrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ModernColorPickerDialog(
    title: String = "لون التمييز",
    initialHex: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    // Parse initial color into HSV
    val initialHsv = remember(initialHex) {
        val hsv = FloatArray(3)
        try {
            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(initialHex), hsv)
        } catch (e: Exception) {
            hsv[0] = 24f
            hsv[1] = 0.82f
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

    // Palette swatches matching Screenshot 2
    val swatches = listOf(
        "#000000",
        "#1E293B",
        "#334155",
        "#64748B",
        "#94A3B8",
        "#FFFFFF",
        "#FF7A2F",
        "#FFE236",
        "#10B981",
        "#00D2FF",
        "#8B5CF6",
        "#EC4899"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = Color(0xFF202028),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333647)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clickable(enabled = false) {}
                    .testTag("color_picker_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                ) {
                    // Dialog Title
                    Text(
                        text = title,
                        fontFamily = CairoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Current Color Preview Block (as in Screenshot 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentColor())
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Slider 1: Rainbow Hue Bar
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
                        // White circle thumb for Hue
                        val thumbOffsetFraction = (hue / 360f).coerceIn(0f, 1f)
                        val thumbSize = 24.dp
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(thumbOffsetFraction),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(thumbSize)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(3.dp, Color(0xFF202028), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slider 2: Saturation / Shade Bar (from white/light to hue to dark)
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
                                    listOf(
                                        Color.White,
                                        pureHueColor,
                                        Color(0xFF1B0000),
                                        Color.Black
                                    )
                                )
                            )
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (shadeBarSize.width > 0) {
                                        val fraction = (offset.x / shadeBarSize.width.toFloat()).coerceIn(0f, 1f)
                                        // Adjust saturation and brightness based on position
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
                        // White circle thumb for Shade
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
                                    .border(3.dp, Color(0xFF202028), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Hex Text Display / Input Box (matching Screenshot 2)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF14141B))
                            .border(1.dp, Color(0xFF333647), RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = hexInput,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Preset palette swatches title
                    Text(
                        text = "لون التمييز",
                        fontFamily = CairoFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Swatches row (Scrollable or spaced)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        swatches.take(7).forEach { hex ->
                            val parsed = try {
                                Color(android.graphics.Color.parseColor(hex))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                            val isSelected = hexInput.equals(hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) OrangePrimary else Color(0xFF44475A),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        hexInput = hex
                                        try {
                                            val hsv = FloatArray(3)
                                            android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(hex), hsv)
                                            hue = hsv[0]
                                            saturation = hsv[1]
                                            brightness = hsv[2]
                                        } catch (_: Exception) {}
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons (Screenshot 2: 'إلغاء' and 'تم')
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                onColorSelected(hexInput)
                                onDismiss()
                            }
                        ) {
                            Text(
                                text = "تم",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = OrangePrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "إلغاء",
                                fontFamily = CairoFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = OrangePrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
