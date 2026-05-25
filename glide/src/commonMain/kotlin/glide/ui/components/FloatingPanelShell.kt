package glide.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import glide.data.AppViewState
import glide.ui.layout.GlideLayout
import glide.ui.layout.LayoutTier
import glide.ui.layout.PanelWorkspace
import glide.ui.theme.GlideAccents
import glide.ui.theme.GlideTextButton
import kotlin.math.roundToInt

private data class GridPanelLayout(
    val widthPx: Float,
    val heightPx: Float,
    val xPx: Float,
    val yPx: Float,
)

private data class SavedPanelLayout(
    val offsetX: Float,
    val offsetY: Float,
    val widthPx: Float,
    val heightPx: Float,
    val expandedHeightPx: Float,
    val wasCollapsed: Boolean,
)

@Composable
fun FloatingPanelShell(
    title: String,
    slot: Int,
    windowWidthPx: Int,
    windowHeightPx: Int,
    modifier: Modifier = Modifier,
    initiallyCollapsed: Boolean = false,
    onClose: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    if (windowWidthPx <= 0 || windowHeightPx <= 0) return

    val density = LocalDensity.current
    val windowWidthDp = with(density) { windowWidthPx.toDp() }
    val layoutTier = GlideLayout.layoutTier(windowWidthDp)
    val gridTier = LayoutTier.Wide
    val marginPx = with(density) { GlideLayout.panelMargin(gridTier).toPx() }
    val gapPx = with(density) { GlideLayout.panelGap(gridTier).toPx() }
    val titleBarHeightPx = with(density) { GlideLayout.PanelTitleBarHeight.toPx() }
    val minWidthPx = with(density) { GlideLayout.PanelMinWidth.toPx() }
    val minHeightPx = with(density) { GlideLayout.PanelMinHeight.toPx() }
    val maxPanelWidthPx = windowWidthPx - marginPx * 2
    val maxPanelHeightPx = windowHeightPx - marginPx * 2

    val viewMode = AppViewState.mode
    val defaultLayout = remember(windowWidthPx, windowHeightPx, slot, marginPx, gapPx, viewMode, gridTier) {
        val (gridWidthPx, gridHeightPx) = GlideLayout.computeGridPanelSizePx(
            windowWidthPx = windowWidthPx.toFloat(),
            windowHeightPx = windowHeightPx.toFloat(),
            marginPx = marginPx,
            gapPx = gapPx,
            mode = viewMode,
            tier = gridTier,
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
            mode = viewMode,
            tier = gridTier,
        )
        GridPanelLayout(widthPx, heightPx, xPx, yPx)
    }

    var offsetX by remember(viewMode, slot) { mutableFloatStateOf(defaultLayout.xPx) }
    var offsetY by remember(viewMode, slot) { mutableFloatStateOf(defaultLayout.yPx) }
    var panelWidthPx by remember(viewMode, slot) { mutableFloatStateOf(defaultLayout.widthPx) }
    var panelHeightPx by remember(viewMode, slot) { mutableFloatStateOf(defaultLayout.heightPx) }
    var expandedHeightPx by remember(viewMode, slot) { mutableFloatStateOf(defaultLayout.heightPx) }
    var isCollapsed by remember(viewMode, slot) { mutableStateOf(initiallyCollapsed) }
    var userAdjustedLayout by remember(viewMode, slot) { mutableStateOf(false) }
    var savedLayout by remember(viewMode, slot) { mutableStateOf<SavedPanelLayout?>(null) }

    val isMaximized = PanelWorkspace.isMaximized(slot, viewMode)
    val showSurface = PanelWorkspace.shouldShowFloatingSurface(slot, windowWidthPx, viewMode)
    val showFillHint = windowWidthDp < 1200.dp || isMaximized

    fun bringToFront() = PanelZOrder.bringToFront(slot)

    fun applyMaximizedLayout() {
        offsetX = marginPx
        offsetY = marginPx
        panelWidthPx = maxPanelWidthPx
        panelHeightPx = maxPanelHeightPx
        expandedHeightPx = maxPanelHeightPx
        isCollapsed = false
        PanelWorkspace.undock(slot, viewMode)
    }

    fun toggleMaximized() {
        bringToFront()
        if (isMaximized) {
            savedLayout?.let { saved ->
                offsetX = saved.offsetX
                offsetY = saved.offsetY
                panelWidthPx = saved.widthPx
                panelHeightPx = saved.heightPx
                expandedHeightPx = saved.expandedHeightPx
                isCollapsed = saved.wasCollapsed
                if (saved.wasCollapsed) {
                    PanelWorkspace.dock(slot, viewMode)
                } else {
                    PanelWorkspace.undock(slot, viewMode)
                }
            }
            PanelWorkspace.clearMaximized(viewMode)
            savedLayout = null
        } else {
            savedLayout = SavedPanelLayout(
                offsetX = offsetX,
                offsetY = offsetY,
                widthPx = panelWidthPx,
                heightPx = panelHeightPx,
                expandedHeightPx = expandedHeightPx,
                wasCollapsed = isCollapsed,
            )
            userAdjustedLayout = true
            PanelWorkspace.setMaximized(slot, viewMode)
            applyMaximizedLayout()
        }
    }

    fun persistLayoutSnapshot() {
        PanelWorkspace.updateLayoutSnapshot(
            slot,
            glide.data.PanelLayoutSnapshot(
                offsetX = offsetX,
                offsetY = offsetY,
                widthPx = panelWidthPx,
                heightPx = panelHeightPx,
                expandedHeightPx = expandedHeightPx,
                isCollapsed = isCollapsed,
            ),
            mode = viewMode,
        )
    }

    LaunchedEffect(
        offsetX,
        offsetY,
        panelWidthPx,
        panelHeightPx,
        expandedHeightPx,
        isCollapsed,
    ) {
        persistLayoutSnapshot()
    }

    LaunchedEffect(PanelWorkspace.expandFromDockRequest, viewMode, slot) {
        val request = PanelWorkspace.expandFromDockRequest
        if (request?.first == viewMode && request.second == slot) {
            isCollapsed = false
            panelHeightPx = expandedHeightPx.coerceIn(minHeightPx, maxPanelHeightPx)
            PanelWorkspace.clearExpandFromDockRequest()
        }
    }

    LaunchedEffect(PanelWorkspace.layoutApplyRevision, viewMode, slot) {
        PanelWorkspace.layoutSnapshot(slot, viewMode)?.let { snapshot ->
            offsetX = snapshot.offsetX
            offsetY = snapshot.offsetY
            panelWidthPx = snapshot.widthPx
            panelHeightPx = snapshot.heightPx
            expandedHeightPx = snapshot.expandedHeightPx
            isCollapsed = snapshot.isCollapsed
            userAdjustedLayout = true
            if (snapshot.isCollapsed) {
                PanelWorkspace.dock(slot, viewMode)
            } else {
                PanelWorkspace.undock(slot, viewMode)
            }
        }
    }

    LaunchedEffect(defaultLayout, userAdjustedLayout, isMaximized) {
        if (!userAdjustedLayout && !isMaximized) {
            offsetX = defaultLayout.xPx
            offsetY = defaultLayout.yPx
            panelWidthPx = defaultLayout.widthPx
            panelHeightPx = defaultLayout.heightPx
            expandedHeightPx = defaultLayout.heightPx
        }
    }

    LaunchedEffect(windowWidthPx, windowHeightPx, isMaximized, marginPx, maxPanelWidthPx, maxPanelHeightPx) {
        if (isMaximized) {
            applyMaximizedLayout()
        }
    }

    if (!PanelWorkspace.shouldCompose(slot, viewMode)) return

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
    val accent = GlideAccents.forPanel(slot)
    val panelOrder = PanelZOrder.order
    val isSelected = PanelZOrder.isFocused(slot)
    val panelShape = MaterialTheme.shapes.medium

    val panelBorder = panelBorderStroke(
        selected = isSelected,
        accent = accent,
        outline = MaterialTheme.colorScheme.outline,
    )

    if (!showSurface) return

    Surface(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(width = panelWidth, height = panelHeight)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                            start = Offset.Zero,
                            end = Offset(80f, 80f),
                        ),
                        shape = panelShape,
                    )
                } else {
                    Modifier
                },
            )
            .pointerInput(slot, panelOrder) {
                detectTapGestures(onPress = { bringToFront() })
            },
        shape = panelShape,
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = if (isSelected) 0.98f else 0.94f,
        ),
        tonalElevation = if (isSelected) 8.dp else 2.dp,
        shadowElevation = if (isSelected) 28.dp else 12.dp,
        border = panelBorder,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(GlideLayout.PanelTitleBarHeight)
                        .background(
                            Brush.horizontalGradient(
                                colors = if (isSelected) {
                                    listOf(
                                        accent.copy(alpha = 0.38f),
                                        accent.copy(alpha = 0.14f),
                                        Color.Transparent,
                                    )
                                } else {
                                    listOf(
                                        accent.copy(alpha = 0.28f),
                                        accent.copy(alpha = 0.08f),
                                        Color.Transparent,
                                    )
                                },
                            ),
                        )
                        .then(
                            if (!isMaximized) {
                                Modifier.pointerInput(slot, maxOffsetX, maxOffsetY, isCollapsed) {
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
                            } else {
                                Modifier
                            },
                        )
                        .pointerInput(slot, isMaximized) {
                            detectTapGestures(onDoubleTap = { toggleMaximized() })
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 5.dp else 4.dp)
                            .height(GlideLayout.PanelTitleBarHeight)
                            .background(
                                Brush.verticalGradient(
                                    colors = if (isSelected) {
                                        listOf(
                                            accent.copy(alpha = 1f),
                                            accent.copy(alpha = 0.65f),
                                        )
                                    } else {
                                        listOf(
                                            accent,
                                            accent.copy(alpha = 0.45f),
                                        )
                                    },
                                ),
                            ),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                    )
                    if (showFillHint && !isCollapsed) {
                        Text(
                            text = if (isMaximized) "Double-click to restore" else "Double-click to fill",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent.copy(alpha = 0.75f),
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                    if (onClose != null) {
                        GlideTextButton(
                            onClick = {
                                bringToFront()
                                onClose()
                            },
                        ) {
                            Text("Close")
                        }
                    }
                    GlideTextButton(
                        onClick = {
                            bringToFront()
                            if (isMaximized) {
                                toggleMaximized()
                                return@GlideTextButton
                            }
                            if (isCollapsed) {
                                if (expandedHeightPx.isNaN()) {
                                    expandedHeightPx = minHeightPx
                                }
                                panelHeightPx = expandedHeightPx.coerceIn(minHeightPx, maxPanelHeightPx)
                                isCollapsed = false
                                PanelWorkspace.undock(slot, viewMode)
                            } else {
                                expandedHeightPx = panelHeightPx
                                isCollapsed = true
                                PanelWorkspace.dock(slot, viewMode)
                            }
                        },
                    ) {
                        Text(
                            when {
                                isMaximized -> "Restore"
                                isCollapsed -> "Expand"
                                else -> "Collapse"
                            },
                        )
                    }
                }

                if (!isCollapsed) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(slot, panelOrder) {
                                detectTapGestures(onPress = { bringToFront() })
                            },
                    ) {
                        content()
                    }
                }
            }

            if (!isCollapsed && !isMaximized) {
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

private fun panelBorderStroke(
    selected: Boolean,
    accent: Color,
    outline: Color,
): BorderStroke {
    if (selected) {
        return BorderStroke(
            width = 3.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE8F2FA).copy(alpha = 0.7f),
                    accent.copy(alpha = 0.95f),
                    accent.copy(alpha = 0.55f),
                    Color(0xFF0A1018).copy(alpha = 0.55f),
                ),
                start = Offset.Zero,
                end = Offset(280f, 280f),
            ),
        )
    }
    return BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.55f),
                outline.copy(alpha = 0.35f),
                accent.copy(alpha = 0.2f),
            ),
        ),
    )
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
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        )
    }
}
