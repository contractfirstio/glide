package glide.ui.layout

import glide.data.AppViewMode
import glide.data.AppViewState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

object GlideLayout {
    val AppChromeHeight = 44.dp
    /** Side-by-side list + form below this width stacks vertically. */
    val CompactWidthBreakpoint = 680.dp

    const val CustomerManagementPanelCount = 5
    const val SchedulingPanelCount = 5
    const val CustomerManagementGridColumns = 3
    const val CustomerManagementGridRows = 2
    const val SchedulingGridColumns = 3
    const val SchedulingGridRows = 2
    const val StartupWindowScale = 3

    fun gridColumns(mode: AppViewMode = AppViewState.mode): Int = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> CustomerManagementGridColumns
        AppViewMode.SCHEDULING -> SchedulingGridColumns
    }

    fun gridRows(mode: AppViewMode = AppViewState.mode): Int = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> CustomerManagementGridRows
        AppViewMode.SCHEDULING -> SchedulingGridRows
    }

    val PanelMargin = 12.dp * StartupWindowScale
    val PanelGap = 16.dp * StartupWindowScale
    val PanelTitleBarHeight = 28.dp
    val PanelMinWidth = 280.dp
    val PanelMinHeight = 240.dp
    val PanelResizeHandleSize = 18.dp

    /** Base cell size at 1× scale — startup window is [StartupWindowScale]× this. */
    private val BasePanelWidth = 340.dp
    private val BasePanelHeight = 420.dp

    private val BaseCustomerManagementWindowWidth =
        12.dp * 2 +
            BasePanelWidth * CustomerManagementGridColumns +
            16.dp * (CustomerManagementGridColumns - 1)

    private val BaseCustomerManagementWindowHeight =
        12.dp * 2 +
            BasePanelHeight * CustomerManagementGridRows +
            16.dp * (CustomerManagementGridRows - 1) +
            AppChromeHeight

    val DefaultWindowWidth = BaseCustomerManagementWindowWidth * StartupWindowScale
    val DefaultWindowHeight = BaseCustomerManagementWindowHeight * StartupWindowScale
    val DefaultWindowSize = DpSize(DefaultWindowWidth, DefaultWindowHeight)

    /** Panel width/height in px so each grid cell fills the window evenly. */
    fun computeGridPanelSizePx(
        windowWidthPx: Float,
        windowHeightPx: Float,
        marginPx: Float,
        gapPx: Float,
        mode: AppViewMode = AppViewState.mode,
    ): Pair<Float, Float> {
        val columns = gridColumns(mode)
        val rows = gridRows(mode)
        val width = (windowWidthPx - marginPx * 2 - gapPx * (columns - 1)) / columns
        val height = (windowHeightPx - marginPx * 2 - gapPx * (rows - 1)) / rows
        return width to height
    }

    /**
     * Customer Management grid:
     * ```
     * [ Leads    ] [ Plans  ] [ Clients ]
     * [ Customers] [ Billing] [ Related ]
     * ```
     */
    fun customerManagementPanelGridCell(slot: Int): Pair<Int, Int> = when (slot) {
        PanelSlots.LEADS -> 0 to 0
        PanelSlots.PLANS -> 0 to 1
        PanelSlots.CLIENTS -> 0 to 2
        PanelSlots.CUSTOMERS -> 1 to 0
        PanelSlots.BILLING -> 1 to 1
        PanelSlots.RELATED -> 1 to 2
        else -> 0 to 0
    }

    /**
     * Scheduling grid (2×3). Attendance opens as an overlay anchored top-right when a
     * calendar class is clicked.
     * ```
     * [ Classes       ] [ Term calendar ] [ Attendance* ]
     * [ Sold Plans    ] [ Terms         ] [ Locations     ]
     * ```
     */
    fun schedulingPanelGridCell(slot: Int): Pair<Int, Int> = when (slot) {
        SchedulingPanelSlots.SCHEDULE -> 0 to 0
        SchedulingPanelSlots.CALENDAR -> 0 to 1
        SchedulingPanelSlots.ATTENDANCE -> 0 to 2
        SchedulingPanelSlots.CUSTOMER_GROUPS -> 1 to 0
        SchedulingPanelSlots.TERMS -> 1 to 1
        SchedulingPanelSlots.LOCATIONS -> 1 to 2
        else -> 0 to 0
    }

    fun panelGridCell(slot: Int, mode: AppViewMode = AppViewState.mode): Pair<Int, Int> = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> customerManagementPanelGridCell(slot)
        AppViewMode.SCHEDULING -> schedulingPanelGridCell(slot)
    }

    fun computePanelOriginPx(
        slot: Int,
        panelWidthPx: Float,
        panelHeightPx: Float,
        windowWidthPx: Float,
        windowHeightPx: Float,
        marginPx: Float,
        gapPx: Float,
        mode: AppViewMode = AppViewState.mode,
    ): Pair<Float, Float> {
        val columns = gridColumns(mode)
        val rows = gridRows(mode)
        val (row, col) = panelGridCell(slot, mode)
        val contentWidth = columns * panelWidthPx + (columns - 1) * gapPx
        val contentHeight = rows * panelHeightPx + (rows - 1) * gapPx
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
    const val CLIENTS = 3
    const val CUSTOMERS = 4
    const val BILLING = 5
}
