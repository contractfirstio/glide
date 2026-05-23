package glide.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlideFieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
    )
}

@Composable
fun GlideOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else maxOf(minLines, 4),
    placeholder: String? = null,
    fieldHeight: Dp = GlideDimensions.fieldHeight,
    shape: Shape = MaterialTheme.shapes.small,
    trailingContent: @Composable (() -> Unit)? = null,
    fieldModifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodySmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )
    val placeholderStyle = textStyle.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = MaterialTheme.colorScheme.primary,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    )

    Column(modifier = modifier) {
        GlideFieldLabel(label)
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            readOnly = readOnly,
            enabled = true,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = textStyle,
            placeholder = placeholder?.let { hint -> { Text(hint, style = placeholderStyle) } },
            trailingIcon = trailingContent,
            shape = shape,
            colors = fieldColors,
            modifier = Modifier
                .fillMaxWidth()
                .then(fieldModifier)
                .defaultMinSize(minHeight = fieldHeight)
                .then(
                    if (singleLine) {
                        Modifier
                    } else {
                        Modifier.heightIn(min = fieldHeight, max = fieldHeight * 2)
                    },
                ),
        )
    }
}

/** Dark text on solid pale-blue primary buttons. */
@Composable
private fun GlideFilledButtonText(content: @Composable () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onPrimary
    CompositionLocalProvider(
        LocalContentColor provides textColor,
        LocalTextStyle provides MaterialTheme.typography.labelMedium.copy(color = textColor),
    ) {
        content()
    }
}

/** Light text on translucent outlined / text buttons (explicit [Text] colors, e.g. error red, still apply). */
@Composable
private fun GlideTranslucentButtonText(
    style: TextStyle,
    content: @Composable () -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    CompositionLocalProvider(
        LocalContentColor provides textColor,
        LocalTextStyle provides style.copy(color = textColor),
    ) {
        content()
    }
}

@Composable
fun GlideButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = GlideDimensions.buttonHeight)
            .heightIn(min = GlideDimensions.buttonHeight),
        contentPadding = GlideDimensions.buttonPadding,
        shape = MaterialTheme.shapes.small,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        content = { GlideFilledButtonText(content) },
    )
}

@Composable
fun GlideOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .defaultMinSize(minHeight = GlideDimensions.buttonHeight)
            .heightIn(min = GlideDimensions.buttonHeight),
        contentPadding = GlideDimensions.buttonPadding,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        content = {
            GlideTranslucentButtonText(MaterialTheme.typography.labelMedium, content)
        },
    )
}

@Composable
fun GlideTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = GlideDimensions.textButtonPadding,
    content: @Composable () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = GlideDimensions.buttonHeight),
        contentPadding = contentPadding,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        content = {
            GlideTranslucentButtonText(MaterialTheme.typography.labelSmall, content)
        },
    )
}
