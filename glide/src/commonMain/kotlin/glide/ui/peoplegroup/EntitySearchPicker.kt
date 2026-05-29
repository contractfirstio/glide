package glide.ui.peoplegroup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.ui.theme.GlideOutlinedField

data class SearchResultItem(
    val id: String,
    val primaryLabel: String,
    val secondaryLabel: String? = null,
)

@Composable
fun EntitySearchPicker(
    label: String,
    placeholder: String,
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchResultItem>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    noResultsText: String = "No matches.",
    emptyQueryText: String = "Type to search.",
    isError: Boolean = false,
) {
    val hasQuery = query.trim().isNotEmpty()
    Column(modifier = modifier.fillMaxWidth()) {
        GlideOutlinedField(
            value = query,
            onValueChange = onQueryChange,
            label = label,
            placeholder = placeholder,
            isError = isError,
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (!hasQuery) {
            Text(
                text = emptyQueryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (results.isEmpty()) {
            Text(
                text = noResultsText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        MaterialTheme.shapes.small,
                    )
                    .verticalScroll(rememberScrollState()),
            ) {
                results.take(12).forEach { item ->
                    SearchResultRow(
                        item = item,
                        onClick = {
                            onSelect(item.id)
                            onQueryChange("")
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    item: SearchResultItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = item.primaryLabel,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        item.secondaryLabel?.let { secondary ->
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun String.matchesEntitySearch(query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return true
    return lowercase().contains(needle)
}
