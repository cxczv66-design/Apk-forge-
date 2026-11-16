package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SpaceColors

@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = SpaceColors.SurfaceElevated,
    borderColor: Color = SpaceColors.NeonCyan,
    borderWidth: Dp = 1.2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(14.dp, shape, spotColor = borderColor.copy(alpha = 0.42f))
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0.92f),
                        SpaceColors.MidnightBlue.copy(alpha = 0.90f),
                        SpaceColors.DeepSpace.copy(alpha = 0.94f)
                    )
                )
            )
            .border(
                border = BorderStroke(
                    borderWidth,
                    Brush.linearGradient(
                        colors = listOf(
                            borderColor.copy(alpha = 0.80f),
                            SpaceColors.White.copy(alpha = 0.14f),
                            SpaceColors.NeonPurple.copy(alpha = 0.50f)
                        )
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}
