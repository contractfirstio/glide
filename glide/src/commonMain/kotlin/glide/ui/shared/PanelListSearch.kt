package glide.ui.shared

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import glide.ui.theme.GlideOutlinedField

/** Case-insensitive substring match across any non-blank field; empty query matches all. */
fun matchesPanelListSearch(query: String, vararg fields: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    return fields.any { field ->
        field.isNotBlank() && field.lowercase().contains(needle)
    }
}

fun panelListCountLabel(
    singular: String,
    plural: String,
    filteredCount: Int,
    totalCount: Int,
    searchActive: Boolean,
): String {
    val noun = if (filteredCount == 1) singular else plural
    return if (searchActive && filteredCount != totalCount) {
        "$filteredCount of $totalCount $noun"
    } else {
        "$filteredCount $noun"
    }
}

@Composable
fun PanelListSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
) {
    GlideOutlinedField(
        value = query,
        onValueChange = onQueryChange,
        label = "Search",
        placeholder = placeholder,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
fun PanelListSearchSpacer() {
    Spacer(modifier = Modifier.height(8.dp))
}
