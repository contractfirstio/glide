package glide.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

object GlideLayout {
    /** Side-by-side list + form below this width stacks vertically. */
    val CompactWidthBreakpoint = 680.dp

    const val PanelCount = 5
    const val PanelGridColumns = 3
    const val PanelGridRows = 2
    const val StartupWindowScale = 3

    val PanelMargin = 12.dp * StartupWindowScale
    val PanelGap = 16.dp * StartupWindowScale
    val PanelTitleBarHeight = 28.dp
    val PanelMinWidth = 280.dp
    val PanelMinHeight = 240.dp
    val PanelResizeHandleSize = 18.dp

    /** Base cell size at 1× scale — startup window is [StartupWindowScale]× this. */
    private val BasePanelWidth = 340.dp
    private val BasePanelHeight = 420.dp

    private val BaseWindowWidth =
        12.dp * 2 +
            BasePanelWidth * PanelGridColumns +
            16.dp * (PanelGridColumns - 1)

    private val BaseWindowHeight =
        12.dp * 2 +
            BasePanelHeight * PanelGridRows +
            16.dp * (PanelGridRows - 1)

    val DefaultWindowWidth = BaseWindowWidth * StartupWindowScale
    val DefaultWindowHeight = BaseWindowHeight * StartupWindowScale
    val DefaultWindowSize = DpSize(DefaultWindowWidth, DefaultWindowHeight)

    /** Panel width/height in px so each grid cell fills the window evenly. */
    fun computeGridPanelSizePx(
        windowWidthPx: Float,
        windowHeightPx: Float,
        marginPx: Float,
        gapPx: Float,
    ): Pair<Float, Float> {
        val width = (windowWidthPx - marginPx * 2 - gapPx * (PanelGridColumns - 1)) / PanelGridColumns
        val height = (windowHeightPx - marginPx * 2 - gapPx * (PanelGridRows - 1)) / PanelGridRows
        return width to height
    }

    /**
     * Grid slots for five panels:
     * ```
     * [ Leads ] [ Plans  ] [ Related ]
     * [ People] [        ] [ Customers ]
     * ```
     */
    fun panelGridCell(slot: Int): Pair<Int, Int> = when (slot) {
        PanelSlots.LEADS -> 0 to 0
        PanelSlots.PLANS -> 0 to 1
        PanelSlots.RELATED -> 0 to 2
        PanelSlots.PEOPLE -> 1 to 0
        PanelSlots.BILLING -> 1 to 1
        PanelSlots.CUSTOMERS -> 1 to 2
        else -> 0 to 0
    }

    fun computePanelOriginPx(
        slot: Int,
        panelWidthPx: Float,
        panelHeightPx: Float,
        windowWidthPx: Float,
        windowHeightPx: Float,
        marginPx: Float,
        gapPx: Float,
    ): Pair<Float, Float> {
        val (row, col) = panelGridCell(slot)
        val contentWidth = PanelGridColumns * panelWidthPx + (PanelGridColumns - 1) * gapPx
        val contentHeight = PanelGridRows * panelHeightPx + (PanelGridRows - 1) * gapPx
        val startX = marginPx + ((windowWidthPx - marginPx * 2) - contentWidth) / 2f
        val startY = marginPx + ((windowHeightPx - marginPx * 2) - contentHeight) / 2f
        val x = startX + col * (panelWidthPx + gapPx)
        val y = startY + row * (panelHeightPx + gapPx)
        return x.coerceAtLeast(marginPx) to y.coerceAtLeast(marginPx)
    }

    data class Spacing(
        val outer: Dp,
        val section: Dp,
        val field: Dp,
        val listItemVertical: Dp,
        val listItemHorizontal: Dp,
    )

    val compact = Spacing(
        outer = 6.dp,
        section = 4.dp,
        field = 3.dp,
        listItemVertical = 4.dp,
        listItemHorizontal = 6.dp,
    )

    val comfortable = Spacing(
        outer = 8.dp,
        section = 6.dp,
        field = 4.dp,
        listItemVertical = 5.dp,
        listItemHorizontal = 8.dp,
    )
}

object PanelSlots {
    const val LEADS = 0
    const val PLANS = 1
    const val RELATED = 2
    const val PEOPLE = 3
    const val CUSTOMERS = 4
    const val BILLING = 5
}
