package glide.ui.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import glide.ui.layout.GlideLayout
import glide.ui.theme.GlideTextButton

/**
 * Responsive list + form layout for entity panels.
 * Wide: side-by-side. Medium: stacked. Narrow: list with sliding form drawer.
 */
@Composable
fun ListFormPanelLayout(
    hasSelection: Boolean,
    spacing: GlideLayout.Spacing,
    modifier: Modifier = Modifier,
    onCloseForm: () -> Unit = {},
    listSection: @Composable (Modifier) -> Unit,
    formSection: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val sideBySide = maxWidth >= GlideLayout.CompactWidthBreakpoint
        val drawerMode = maxWidth < GlideLayout.DrawerWidthBreakpoint

        when {
            sideBySide -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.section),
                ) {
                    listSection(Modifier.weight(0.42f).fillMaxHeight())
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                    formSection(Modifier.weight(0.58f).fillMaxHeight())
                }
            }
            drawerMode -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    listSection(Modifier.fillMaxSize())
                    AnimatedVisibility(
                        visible = hasSelection,
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it },
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = spacing.outer, vertical = spacing.field),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                GlideTextButton(onClick = onCloseForm) {
                                    Text("Back to list")
                                }
                            }
                            HorizontalDivider()
                            formSection(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(spacing.outer),
                            )
                        }
                    }
                    if (!hasSelection) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(spacing.outer)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                                    MaterialTheme.shapes.small,
                                )
                                .padding(horizontal = spacing.listItemHorizontal, vertical = spacing.field),
                        ) {
                            Text(
                                text = "Select an item to edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    listSection(Modifier.fillMaxWidth())
                    HorizontalDivider(modifier = Modifier.padding(vertical = spacing.section))
                    formSection(Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}
