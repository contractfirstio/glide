package glide.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.ui.layout.GlideLayout

enum class FormPanelSectionRole {
    Primary,
    Secondary,
    Tertiary,
}

/** Strong visual break between distinct domains on a panel form. */
@Composable
fun FormPanelSectionsDivider(
    label: String,
    spacing: GlideLayout.Spacing,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = spacing.section),
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
        Spacer(modifier = Modifier.height(spacing.field))
        Text(
            text = label,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.field))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
            thickness = 1.dp,
        )
    }
}

/** Top-level block separating a distinct domain on a panel form. */
@Composable
fun FormPanelSection(
    title: String,
    description: String,
    spacing: GlideLayout.Spacing,
    role: FormPanelSectionRole,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accentColor = when (role) {
        FormPanelSectionRole.Primary -> MaterialTheme.colorScheme.primary
        FormPanelSectionRole.Secondary -> MaterialTheme.colorScheme.tertiary
        FormPanelSectionRole.Tertiary -> MaterialTheme.colorScheme.secondary
    }
    val backgroundColor = when (role) {
        FormPanelSectionRole.Primary ->
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        FormPanelSectionRole.Secondary ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        FormPanelSectionRole.Tertiary ->
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val borderColor = when (role) {
        FormPanelSectionRole.Primary -> accentColor.copy(alpha = 0.45f)
        FormPanelSectionRole.Secondary -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        FormPanelSectionRole.Tertiary -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    }
    val titleStyle = when (role) {
        FormPanelSectionRole.Primary -> MaterialTheme.typography.titleMedium
        FormPanelSectionRole.Secondary -> MaterialTheme.typography.titleSmall
        FormPanelSectionRole.Tertiary -> MaterialTheme.typography.titleSmall
    }
    val accentWidth = when (role) {
        FormPanelSectionRole.Primary -> 4.dp
        FormPanelSectionRole.Secondary -> 3.dp
        FormPanelSectionRole.Tertiary -> 3.dp
    }
    val borderWidth = when (role) {
        FormPanelSectionRole.Primary -> 1.5.dp
        FormPanelSectionRole.Secondary -> 1.dp
        FormPanelSectionRole.Tertiary -> 1.dp
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .border(borderWidth, borderColor, MaterialTheme.shapes.medium),
    ) {
        Box(
            modifier = Modifier
                .width(accentWidth)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor,
                            accentColor.copy(alpha = 0.55f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(spacing.outer),
        ) {
            Text(
                text = title,
                style = titleStyle,
                fontWeight = FontWeight.SemiBold,
                color = when (role) {
                    FormPanelSectionRole.Primary -> MaterialTheme.colorScheme.onPrimaryContainer
                    FormPanelSectionRole.Secondary,
                    FormPanelSectionRole.Tertiary,
                    -> MaterialTheme.colorScheme.onSurface
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(spacing.section))
            content()
        }
    }
}

/** Highlighted inner card for a selected or primary entity within a section. */
@Composable
fun FormPanelSummaryCard(
    role: FormPanelSectionRole,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accentColor = when (role) {
        FormPanelSectionRole.Primary -> MaterialTheme.colorScheme.primary
        FormPanelSectionRole.Secondary -> MaterialTheme.colorScheme.tertiary
        FormPanelSectionRole.Tertiary -> MaterialTheme.colorScheme.secondary
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                MaterialTheme.shapes.small,
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

/** Container for items already linked to the current record. */
@Composable
fun FormPanelLinkedBox(
    role: FormPanelSectionRole,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val accentColor = when (role) {
        FormPanelSectionRole.Primary -> MaterialTheme.colorScheme.primary
        FormPanelSectionRole.Secondary -> MaterialTheme.colorScheme.tertiary
        FormPanelSectionRole.Tertiary -> MaterialTheme.colorScheme.secondary
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                MaterialTheme.shapes.small,
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

/** Nested block distinguishing search/link from create-new within a section. */
@Composable
fun FormPanelActionSubsection(
    title: String,
    description: String,
    spacing: GlideLayout.Spacing,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                MaterialTheme.shapes.small,
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(spacing.outer),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(spacing.field))
        content()
    }
}
