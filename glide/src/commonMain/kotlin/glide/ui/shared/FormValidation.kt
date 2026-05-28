package glide.ui.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import glide.ui.leads.DateOfBirthField
import glide.ui.theme.GlideDimensions
import glide.ui.theme.GlideOutlinedField
import kotlinx.coroutines.CoroutineScope

@Stable
class FormValidationState {
    private var invalidKeys by mutableStateOf(emptySet<String>())
    private var scrollTargetKey by mutableStateOf<String?>(null)

    fun isInvalid(key: String): Boolean = key in invalidKeys

    fun clear() {
        invalidKeys = emptySet()
        scrollTargetKey = null
    }

    fun clearKey(key: String) {
        if (key in invalidKeys) {
            invalidKeys = invalidKeys - key
        }
    }

    /**
     * Marks [keys] invalid (in order) and scrolls the first key into view inside the nearest scroll parent.
     */
    fun reportInvalid(keys: List<String>, @Suppress("UNUSED_PARAMETER") scope: CoroutineScope) {
        if (keys.isEmpty()) {
            clear()
            return
        }
        invalidKeys = keys.toSet()
        scrollTargetKey = keys.first()
    }

    internal val pendingScrollKey: String? get() = scrollTargetKey

    internal fun consumeScrollTarget() {
        scrollTargetKey = null
    }
}

@Composable
fun rememberFormValidation(): FormValidationState = remember { FormValidationState() }

@Composable
fun FormValidationAnchor(
    validation: FormValidationState,
    fieldKey: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val requester = remember(fieldKey) { BringIntoViewRequester() }

    LaunchedEffect(validation.pendingScrollKey, fieldKey) {
        if (validation.pendingScrollKey == fieldKey) {
            requester.bringIntoView()
            validation.consumeScrollTarget()
        }
    }

    Box(
        modifier = modifier.bringIntoViewRequester(requester),
    ) {
        content()
    }
}

@Composable
fun FormValidationState.ValidatedGlideOutlinedField(
    fieldKey: String,
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
    fieldModifier: Modifier = Modifier,
    required: Boolean = false,
) {
    FormValidationAnchor(validation = this, fieldKey = fieldKey, modifier = modifier) {
        GlideOutlinedField(
            value = value,
            onValueChange = {
                clearKey(fieldKey)
                onValueChange(it)
            },
            label = label,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            placeholder = placeholder,
            fieldHeight = fieldHeight,
            fieldModifier = fieldModifier,
            isError = isInvalid(fieldKey),
            required = required,
        )
    }
}

@Composable
fun FormValidationState.ValidatedIsoDateField(
    fieldKey: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    readOnly: Boolean = false,
    required: Boolean = false,
) {
    FormValidationAnchor(validation = this, fieldKey = fieldKey, modifier = modifier) {
        IsoDateField(
            label = label,
            value = value,
            onValueChange = {
                clearKey(fieldKey)
                onValueChange(it)
            },
            readOnly = readOnly,
            isError = isInvalid(fieldKey),
            required = required,
        )
    }
}

@Composable
fun FormValidationState.ValidatedIsoDateRangeField(
    startFieldKey: String,
    endFieldKey: String,
    label: String,
    startValue: String,
    endValue: String,
    onValueChange: (startIso: String, endIso: String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    readOnly: Boolean = false,
    required: Boolean = false,
) {
    FormValidationAnchor(validation = this, fieldKey = startFieldKey, modifier = modifier) {
        IsoDateRangeField(
            label = label,
            startValue = startValue,
            endValue = endValue,
            onValueChange = { startIso, endIso ->
                clearKey(startFieldKey)
                clearKey(endFieldKey)
                onValueChange(startIso, endIso)
            },
            readOnly = readOnly,
            isError = isInvalid(startFieldKey) || isInvalid(endFieldKey),
            required = required,
        )
    }
}

@Composable
fun FormValidationState.ValidatedDateOfBirthField(
    fieldKey: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    FormValidationAnchor(validation = this, fieldKey = fieldKey, modifier = modifier) {
        DateOfBirthField(
            value = value,
            onValueChange = {
                clearKey(fieldKey)
                onValueChange(it)
            },
            isError = isInvalid(fieldKey),
        )
    }
}
