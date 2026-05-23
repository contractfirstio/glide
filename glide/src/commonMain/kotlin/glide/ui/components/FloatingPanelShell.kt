package glide.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import glide.ui.layout.GlideLayout
import glide.ui.theme.GlideTextButton
import kotlin.math.roundToInt

private data class GridPanelLayout(
    val widthPx: Float,
    val heightPx: Float,
    val xPx: Float,
    val yPx: Float,
)

@Composable
fun FloatingPanelShell(
    title: String,
    slot: Int,
    windowWidthPx: Int,
    windowHeightPx: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (windowWidthPx <= 0 || windowHeightPx <= 0) return

    val density = LocalDensity.current
    val marginPx = with(density) { GlideLayout.PanelMargin.toPx() }
    val gapPx = with(density) { GlideLayout.PanelGap.toPx() }
    val titleBarHeightPx = with(density) { GlideLayout.PanelTitleBarHeight.toPx() }
    val minWidthPx = with(density) { GlideLayout.PanelMinWidth.toPx() }
    val minHeightPx = with(density) { GlideLayout.PanelMinHeight.toPx() }
    val maxPanelWidthPx = windowWidthPx - marginPx * 2
    val maxPanelHeightPx = windowHeightPx - marginPx * 2
    val showDragHint = with(density) { windowWidthPx.toDp() } >= 900.dp

    val defaultLayout = remember(windowWidthPx, windowHeightPx, slot, marginPx, gapPx) {
        val (gridWidthPx, gridHeightPx) = GlideLayout.computeGridPanelSizePx(
            windowWidthPx = windowWidthPx.toFloat(),
            windowHeightPx = windowHeightPx.toFloat(),
            marginPx = marginPx,
            gapPx = gapPx,
        )
        val widthPx = gridWidthPx.coerceIn(minWidthPx, maxPanelWidthPx)
        val heightPx = gridHeightPx.coerceIn(minHeightPx, maxPanelHeightPx)
        val (xPx, yPx) = GlideLayout.computePanelOriginPx(
            slot = slot,
            panelWidthPx = widthPx,
            panelHeightPx = heightPx,
            windowWidthPx = windowWidthPx.toFloat(),
            windowHeightPx = windowHeightPx.toFloat(),
            marginPx = marginPx,
            gapPx = gapPx,
        )
        GridPanelLayout(widthPx, heightPx, xPx, yPx)
    }

    var offsetX by remember(slot) { mutableFloatStateOf(defaultLayout.xPx) }
    var offsetY by remember(slot) { mutableFloatStateOf(defaultLayout.yPx) }
    var panelWidthPx by remember(slot) { mutableFloatStateOf(defaultLayout.widthPx) }
    var panelHeightPx by remember(slot) { mutableFloatStateOf(defaultLayout.heightPx) }
    var expandedHeightPx by remember(slot) { mutableFloatStateOf(defaultLayout.heightPx) }
    var isCollapsed by remember(slot) { mutableStateOf(false) }
    var userAdjustedLayout by remember(slot) { mutableStateOf(false) }

    LaunchedEffect(defaultLayout, userAdjustedLayout) {
        if (!userAdjustedLayout) {
            offsetX = defaultLayout.xPx
            offsetY = defaultLayout.yPx
            panelWidthPx = defaultLayout.widthPx
            panelHeightPx = defaultLayout.heightPx
            expandedHeightPx = defaultLayout.heightPx
        }
    }

    val effectiveHeightPx = if (isCollapsed) titleBarHeightPx else panelHeightPx
    val maxOffsetX = (windowWidthPx - panelWidthPx).coerceAtLeast(0f)
    val maxOffsetY = (windowHeightPx - effectiveHeightPx).coerceAtLeast(0f)

    LaunchedEffect(panelWidthPx, effectiveHeightPx, maxOffsetX, maxOffsetY, userAdjustedLayout) {
        if (userAdjustedLayout) {
            offsetX = offsetX.coerceIn(0f, maxOffsetX)
            offsetY = offsetY.coerceIn(0f, maxOffsetY)
        }
    }

    val panelWidth = with(density) { panelWidthPx.toDp() }
    val panelHeight = with(density) { effectiveHeightPx.toDp() }

    fun bringToFront() = PanelZOrder.bringToFront(slot)

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width = panelWidth, height = panelHeight)
            .pointerInput(slot) {
                detectTapGestures(onPress = { bringToFront() })
            },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GlideLayout.PanelTitleBarHeight)
                        .pointerInput(slot, maxOffsetX, maxOffsetY, isCollapsed) {
                            detectDragGestures(
                                onDragStart = { bringToFront() },
                            ) { change, dragAmount ->
                                change.consume()
                                userAdjustedLayout = true
                                bringToFront()
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxOffsetX)
                                offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxOffsetY)
                            }
                        }
                        .padding(start = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp),
                    )
                    if (showDragHint && !isCollapsed) {
                        Text(
                            text = "Drag to move",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    GlideTextButton(
                        onClick = {
                            bringToFront()
                            if (isCollapsed) {
                                if (expandedHeightPx.isNaN()) {
                                    expandedHeightPx = minHeightPx
                                }
                                panelHeightPx = expandedHeightPx.coerceIn(minHeightPx, maxPanelHeightPx)
                                isCollapsed = false
                            } else {
                                expandedHeightPx = panelHeightPx
                                isCollapsed = true
                            }
                        },
                    ) {
                        Text(if (isCollapsed) "Expand" else "Collapse")
                    }
                }

                if (!isCollapsed) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(slot) {
                                detectTapGestures(onPress = { bringToFront() })
                            },
                    ) {
                        content()
                    }
                }
            }

            if (!isCollapsed) {
                PanelResizeHandle(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onResizeStart = { bringToFront() },
                    onResize = { deltaWidth, deltaHeight ->
                        userAdjustedLayout = true
                        panelWidthPx = (panelWidthPx + deltaWidth)
                            .coerceIn(minWidthPx, maxPanelWidthPx)
                        val newHeight = (panelHeightPx + deltaHeight)
                            .coerceIn(minHeightPx, maxPanelHeightPx)
                        panelHeightPx = newHeight
                        expandedHeightPx = newHeight
                    },
                )
            }
        }
    }
}

@Composable
private fun PanelResizeHandle(
    onResize: (deltaWidth: Float, deltaHeight: Float) -> Unit,
    onResizeStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(GlideLayout.PanelResizeHandleSize)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onResizeStart() },
                ) { change, dragAmount ->
                    change.consume()
                    onResize(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "◢",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
