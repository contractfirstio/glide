package glide.ui.layout

import glide.data.AppViewMode
import glide.data.AppViewState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class LayoutTier {
    Wide,
    Medium,
    Compact,
    Tabbed,
}

object GlideLayout {
    val AppChromeHeight = 44.dp
    val ViewModeAccentStripHeight = 4.dp
    val CompactChromeWidthBreakpoint = 1100.dp
    /** Side-by-side list + form below this width stacks vertically. */
    val CompactWidthBreakpoint = 680.dp
    /** List-only with sliding form drawer below this width. */
    val DrawerWidthBreakpoint = 520.dp

    const val CustomerManagementPanelCount = 5
    const val SchedulingPanelCount = 5
    const val CustomerManagementGridColumns = 3
    const val CustomerManagementGridRows = 2
    const val SchedulingGridColumns = 3
    const val SchedulingGridRows = 2
    const val StartupWindowScale = 3

    private const val WideTierMinWidthPx = 1400
    private const val MediumTierMinWidthPx = 1000
    private const val CompactTierMinWidthPx = 900

    fun layoutTier(windowWidth: Dp): LayoutTier = when {
        windowWidth >= WideTierMinWidthPx.dp -> LayoutTier.Wide
        windowWidth >= MediumTierMinWidthPx.dp -> LayoutTier.Medium
        windowWidth >= CompactTierMinWidthPx.dp -> LayoutTier.Compact
        else -> LayoutTier.Tabbed
    }

    fun layoutTierFromPx(windowWidthPx: Int, densityScale: Float = 1f): LayoutTier {
        val widthDp = windowWidthPx / densityScale
        return when {
            widthDp >= WideTierMinWidthPx -> LayoutTier.Wide
            widthDp >= MediumTierMinWidthPx -> LayoutTier.Medium
            widthDp >= CompactTierMinWidthPx -> LayoutTier.Compact
            else -> LayoutTier.Tabbed
        }
    }

    fun gridColumns(
        mode: AppViewMode = AppViewState.mode,
        tier: LayoutTier = LayoutTier.Wide,
    ): Int = when (tier) {
        LayoutTier.Wide -> when (mode) {
            AppViewMode.CUSTOMER_MANAGEMENT -> CustomerManagementGridColumns
            AppViewMode.SCHEDULING -> SchedulingGridColumns
        }
        LayoutTier.Medium, LayoutTier.Compact -> 2
        LayoutTier.Tabbed -> 1
    }

    fun gridRows(
        mode: AppViewMode = AppViewState.mode,
        tier: LayoutTier = LayoutTier.Wide,
    ): Int = when (tier) {
        LayoutTier.Wide -> when (mode) {
            AppViewMode.CUSTOMER_MANAGEMENT -> CustomerManagementGridRows
            AppViewMode.SCHEDULING -> SchedulingGridRows
        }
        LayoutTier.Medium, LayoutTier.Compact -> 3
        LayoutTier.Tabbed -> 1
    }

    fun panelMargin(tier: LayoutTier): Dp = when (tier) {
        LayoutTier.Wide -> 12.dp * StartupWindowScale
        LayoutTier.Medium -> 12.dp * 2
        LayoutTier.Compact -> 12.dp
        LayoutTier.Tabbed -> 8.dp
    }

    fun panelGap(tier: LayoutTier): Dp = when (tier) {
        LayoutTier.Wide -> 16.dp * StartupWindowScale
        LayoutTier.Medium -> 16.dp * 2
        LayoutTier.Compact -> 12.dp
        LayoutTier.Tabbed -> 0.dp
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
        tier: LayoutTier = LayoutTier.Wide,
    ): Pair<Float, Float> {
        val columns = gridColumns(mode, tier)
        val rows = gridRows(mode, tier)
        val width = (windowWidthPx - marginPx * 2 - gapPx * (columns - 1)) / columns
        val height = (windowHeightPx - marginPx * 2 - gapPx * (rows - 1)) / rows
        return width to height
    }

    /**
     * Customer Management grid:
     * ```
     * [ Leads    ] [ Plans  ] [ Clients ]
     * [ Sold Plans] [ Billing] [ Students ]
     * ```
     */
    fun customerManagementPanelGridCell(slot: Int, tier: LayoutTier = LayoutTier.Wide): Pair<Int, Int> =
        when (tier) {
            LayoutTier.Wide -> when (slot) {
                PanelSlots.LEADS -> 0 to 0
                PanelSlots.PLANS -> 0 to 1
                PanelSlots.CLIENTS -> 0 to 2
                PanelSlots.SOLD_PLANS -> 1 to 0
                PanelSlots.BILLING -> 1 to 1
                PanelSlots.STUDENTS -> 1 to 2
                else -> 0 to 0
            }
            LayoutTier.Medium, LayoutTier.Compact -> when (slot) {
                PanelSlots.LEADS -> 0 to 0
                PanelSlots.PLANS -> 0 to 1
                PanelSlots.CLIENTS -> 1 to 0
                PanelSlots.SOLD_PLANS -> 1 to 1
                PanelSlots.STUDENTS -> 2 to 0
                PanelSlots.BILLING -> 2 to 1
                else -> 0 to 0
            }
            LayoutTier.Tabbed -> 0 to 0
        }

    /**
     * Scheduling grid (2×3). Attendance opens as an overlay anchored top-right when a
     * calendar class is clicked.
     * ```
     * [ Classes       ] [ Term calendar ] [ Attendance* ]
     * [ Sold Plans    ] [ Terms         ] [ Locations     ]
     * ```
     */
    fun schedulingPanelGridCell(slot: Int, tier: LayoutTier = LayoutTier.Wide): Pair<Int, Int> =
        when (tier) {
            LayoutTier.Wide -> when (slot) {
                SchedulingPanelSlots.SCHEDULE -> 0 to 0
                SchedulingPanelSlots.CALENDAR -> 0 to 1
                SchedulingPanelSlots.ATTENDANCE -> 0 to 2
                SchedulingPanelSlots.SOLD_PLANS -> 1 to 0
                SchedulingPanelSlots.TERMS -> 1 to 1
                SchedulingPanelSlots.LOCATIONS -> 1 to 2
                else -> 0 to 0
            }
            LayoutTier.Medium, LayoutTier.Compact -> when (slot) {
                SchedulingPanelSlots.SCHEDULE -> 0 to 0
                SchedulingPanelSlots.CALENDAR -> 0 to 1
                SchedulingPanelSlots.SOLD_PLANS -> 1 to 0
                SchedulingPanelSlots.TERMS -> 1 to 1
                SchedulingPanelSlots.LOCATIONS -> 2 to 0
                SchedulingPanelSlots.ATTENDANCE -> 2 to 1
                else -> 0 to 0
            }
            LayoutTier.Tabbed -> 0 to 0
        }

    fun panelGridCell(
        slot: Int,
        mode: AppViewMode = AppViewState.mode,
        tier: LayoutTier = LayoutTier.Wide,
    ): Pair<Int, Int> = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> customerManagementPanelGridCell(slot, tier)
        AppViewMode.SCHEDULING -> schedulingPanelGridCell(slot, tier)
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
        tier: LayoutTier = LayoutTier.Wide,
    ): Pair<Float, Float> {
        val columns = gridColumns(mode, tier)
        val rows = gridRows(mode, tier)
        val (row, col) = panelGridCell(slot, mode, tier)
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
    const val STUDENTS = 2
    const val CLIENTS = 3
    const val SOLD_PLANS = 4
    const val BILLING = 5
}
