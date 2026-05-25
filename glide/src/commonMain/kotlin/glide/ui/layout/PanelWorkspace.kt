package glide.ui.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.data.PanelLayoutSnapshot
import glide.data.WorkspacePreset
import glide.data.WorkspaceUiSettings
import glide.ui.components.PanelZOrder
import java.util.UUID

object PanelWorkspace {
    private class ModeState {
        val layoutSnapshots = mutableMapOf<Int, PanelLayoutSnapshot>()
        var maximizedSlot by mutableStateOf<Int?>(null)
        var tabActiveSlot by mutableStateOf<Int?>(null)
        val dockedSlots: SnapshotStateList<Int> = mutableStateListOf()
    }

    private val customerState = ModeState()
    private val schedulingState = ModeState()

    private var hiddenCustomerPanels by mutableStateOf(setOf<Int>())
    private var hiddenSchedulingPanels by mutableStateOf(setOf<Int>())
    private val presets = mutableStateListOf<WorkspacePreset>()

    /** Slot IDs overlap between view modes — expand requests must include the mode. */
    var expandFromDockRequest by mutableStateOf<Pair<AppViewMode, Int>?>(null)

    var layoutApplyRevision by mutableStateOf(0)
        private set

    val workspacePresets: List<WorkspacePreset> get() = presets

    val maximizedSlot: Int?
        get() = modeState().maximizedSlot

    val tabActiveSlot: Int?
        get() = modeState().tabActiveSlot

    val dockedPanelSlots: List<Int>
        get() = modeState().dockedSlots

    private fun modeState(mode: AppViewMode = AppViewState.mode): ModeState = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> customerState
        AppViewMode.SCHEDULING -> schedulingState
    }

    fun loadPresets(settings: WorkspaceUiSettings) {
        presets.clear()
        presets.addAll(settings.presets)
    }

    /** Fresh session: 3×2 grid, every panel visible, default positions. */
    fun startSession(settings: WorkspaceUiSettings) {
        hiddenCustomerPanels = emptySet()
        hiddenSchedulingPanels = emptySet()
        clearModeState(customerState)
        clearModeState(schedulingState)
        expandFromDockRequest = null
        loadPresets(settings)
        layoutApplyRevision++
    }

    private fun clearModeState(state: ModeState) {
        state.layoutSnapshots.clear()
        state.dockedSlots.clear()
        state.maximizedSlot = null
        state.tabActiveSlot = null
    }

    fun toSettings(): WorkspaceUiSettings = WorkspaceUiSettings(
        hiddenCustomerPanels = hiddenCustomerPanels,
        hiddenSchedulingPanels = hiddenSchedulingPanels,
        presets = presets.toList(),
    )

    fun isVisible(slot: Int, mode: AppViewMode = AppViewState.mode): Boolean = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> slot !in hiddenCustomerPanels
        AppViewMode.SCHEDULING -> slot !in hiddenSchedulingPanels
    }

    fun setVisible(slot: Int, visible: Boolean, mode: AppViewMode = AppViewState.mode) {
        when (mode) {
            AppViewMode.CUSTOMER_MANAGEMENT -> {
                hiddenCustomerPanels = if (visible) {
                    hiddenCustomerPanels - slot
                } else {
                    hiddenCustomerPanels + slot
                }
            }
            AppViewMode.SCHEDULING -> {
                hiddenSchedulingPanels = if (visible) {
                    hiddenSchedulingPanels - slot
                } else {
                    hiddenSchedulingPanels + slot
                }
            }
        }
        val state = modeState(mode)
        if (!visible && state.maximizedSlot == slot) {
            state.maximizedSlot = null
        }
        if (!visible && state.tabActiveSlot == slot) {
            state.tabActiveSlot = firstVisibleSlot(mode)
        }
    }

    fun shouldCompose(slot: Int, mode: AppViewMode = AppViewState.mode): Boolean {
        if (!isVisible(slot, mode)) return false
        if (modeState(mode).maximizedSlot != null && modeState(mode).maximizedSlot != slot) return false
        return true
    }

    fun shouldShowFloatingSurface(
        slot: Int,
        windowWidthPx: Int,
        mode: AppViewMode = AppViewState.mode,
    ): Boolean {
        if (!shouldCompose(slot, mode)) return false
        if (slot in modeState(mode).dockedSlots) return false
        return true
    }

    fun setMaximized(slot: Int?, mode: AppViewMode = AppViewState.mode) {
        val state = modeState(mode)
        state.maximizedSlot = slot
        if (slot != null) {
            PanelZOrder.bringToFront(slot)
            undock(slot, mode)
        }
    }

    fun clearMaximized(mode: AppViewMode = AppViewState.mode) {
        modeState(mode).maximizedSlot = null
    }

    fun toggleMaximized(slot: Int, mode: AppViewMode = AppViewState.mode) {
        if (isMaximized(slot, mode)) {
            clearMaximized(mode)
        } else {
            setMaximized(slot, mode)
        }
    }

    fun isMaximized(slot: Int, mode: AppViewMode = AppViewState.mode): Boolean =
        modeState(mode).maximizedSlot == slot

    fun dock(slot: Int, mode: AppViewMode = AppViewState.mode) {
        val state = modeState(mode)
        if (slot !in state.dockedSlots) {
            state.dockedSlots.add(slot)
        }
        if (state.maximizedSlot == slot) {
            state.maximizedSlot = null
        }
    }

    fun undock(slot: Int, mode: AppViewMode = AppViewState.mode) {
        modeState(mode).dockedSlots.remove(slot)
    }

    fun isDocked(slot: Int, mode: AppViewMode = AppViewState.mode): Boolean =
        slot in modeState(mode).dockedSlots

    fun expandFromDock(slot: Int, mode: AppViewMode = AppViewState.mode) {
        undock(slot, mode)
        expandFromDockRequest = mode to slot
        PanelZOrder.bringToFront(slot)
    }

    fun clearExpandFromDockRequest() {
        expandFromDockRequest = null
    }

    fun updateLayoutSnapshot(
        slot: Int,
        snapshot: PanelLayoutSnapshot,
        mode: AppViewMode = AppViewState.mode,
    ) {
        modeState(mode).layoutSnapshots[slot] = snapshot
    }

    fun layoutSnapshot(slot: Int, mode: AppViewMode = AppViewState.mode): PanelLayoutSnapshot? =
        modeState(mode).layoutSnapshots[slot]

    fun focusPanel(
        slot: Int,
        windowWidthPx: Int,
        fill: Boolean = false,
        mode: AppViewMode = AppViewState.mode,
    ) {
        if (!isVisible(slot, mode)) {
            setVisible(slot, visible = true, mode = mode)
        }
        undock(slot, mode)
        PanelZOrder.bringToFront(slot)
        if (GlideLayout.layoutTierFromPx(windowWidthPx) == LayoutTier.Tabbed || fill) {
            modeState(mode).tabActiveSlot = slot
        }
        if (fill) {
            setMaximized(slot, mode)
        }
    }

    fun ensureTabSlot(mode: AppViewMode, windowWidthPx: Int) {
        val state = modeState(mode)
        val tier = GlideLayout.layoutTierFromPx(windowWidthPx)
        if (tier != LayoutTier.Tabbed) {
            state.tabActiveSlot = null
            return
        }
        val current = state.tabActiveSlot
        if (current == null || !isVisible(current, mode)) {
            state.tabActiveSlot = firstVisibleSlot(mode)
        }
    }

    fun selectTab(slot: Int, mode: AppViewMode = AppViewState.mode) {
        val state = modeState(mode)
        state.tabActiveSlot = slot
        PanelZOrder.bringToFront(slot)
        state.maximizedSlot = null
    }

    fun firstVisibleSlot(mode: AppViewMode): Int? =
        PanelCatalog.primarySlots(mode).firstOrNull { isVisible(it, mode) }

    fun savePreset(name: String, mode: AppViewMode = AppViewState.mode): WorkspacePreset {
        val visible = PanelCatalog.primarySlots(mode).filter { isVisible(it, mode) }
        val snapshots = modeState(mode).layoutSnapshots
        val preset = WorkspacePreset(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            mode = mode.name,
            visibleSlots = visible,
            layouts = snapshots.filterKeys { it in visible }.toMap(),
        )
        presets.add(preset)
        return preset
    }

    fun applyPreset(preset: WorkspacePreset) {
        val mode = runCatching { AppViewMode.valueOf(preset.mode) }.getOrNull() ?: return
        if (AppViewState.mode != mode) {
            AppViewState.switchTo(mode)
        }
        val state = modeState(mode)
        val allSlots = PanelCatalog.primarySlots(mode)
        allSlots.forEach { slot ->
            setVisible(slot, slot in preset.visibleSlots, mode)
        }
        state.layoutSnapshots.clear()
        state.layoutSnapshots.putAll(preset.layouts)
        state.maximizedSlot = null
        state.dockedSlots.clear()
        state.tabActiveSlot = firstVisibleSlot(mode)
        layoutApplyRevision++
        preset.layouts.keys.forEach { PanelZOrder.bringToFront(it) }
    }

    fun deletePreset(id: String) {
        presets.removeAll { it.id == id }
    }

    fun restoreDefaultLayout(mode: AppViewMode = AppViewState.mode) {
        when (mode) {
            AppViewMode.CUSTOMER_MANAGEMENT -> hiddenCustomerPanels = emptySet()
            AppViewMode.SCHEDULING -> hiddenSchedulingPanels = emptySet()
        }
        clearModeState(modeState(mode))
        layoutApplyRevision++
    }
}
