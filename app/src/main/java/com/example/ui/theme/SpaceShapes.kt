package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object SpaceShapes {
    val CardSmall = RoundedCornerShape(14.dp)
    val CardMedium = RoundedCornerShape(18.dp)
    val CardLarge = RoundedCornerShape(24.dp)
    val HeroBanner = RoundedCornerShape(26.dp)
    val Dialog = RoundedCornerShape(26.dp)
    val Pill = RoundedCornerShape(50)
    val Button = RoundedCornerShape(16.dp)
    val TextField = RoundedCornerShape(15.dp)
    val NavBar = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

val AppSpaceShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = SpaceShapes.CardMedium,
    large = SpaceShapes.CardLarge,
    extraLarge = RoundedCornerShape(28.dp)
)
