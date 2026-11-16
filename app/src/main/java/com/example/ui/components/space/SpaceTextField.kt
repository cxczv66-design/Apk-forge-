package com.example.ui.components.space

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpaceColors
import com.example.ui.theme.SpaceShapes
import com.example.ui.theme.TajawalFontFamily

/**
 * حقل نصي زجاجي مضيء متوافق مع نظام الألوان الفضائي
 */
@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    accentColor: Color = SpaceColors.NeonCyan,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let {
            {
                Text(
                    text = it,
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp
                )
            }
        },
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    fontFamily = TajawalFontFamily,
                    fontSize = 12.sp,
                    color = SpaceColors.SubtleText
                )
            }
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(imageVector = it, contentDescription = null, tint = accentColor)
            }
        },
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = TextStyle(
            fontFamily = TajawalFontFamily,
            fontSize = 14.sp,
            color = SpaceColors.White
        ),
        shape = SpaceShapes.TextField,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SpaceColors.SurfaceDeep.copy(alpha = 0.85f),
            unfocusedContainerColor = SpaceColors.MidnightBlue.copy(alpha = 0.72f),
            focusedBorderColor = accentColor,
            unfocusedBorderColor = accentColor.copy(alpha = 0.35f),
            errorBorderColor = SpaceColors.ErrorRed,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = SpaceColors.MutedText,
            cursorColor = accentColor,
            focusedTextColor = SpaceColors.White,
            unfocusedTextColor = SpaceColors.White
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    )
}
