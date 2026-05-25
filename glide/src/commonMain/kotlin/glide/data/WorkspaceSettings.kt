package glide.data

import kotlinx.serialization.Serializable

@Serializable
data class WindowBounds(
    val widthDp: Float? = null,
    val heightDp: Float? = null,
    val positionX: Int? = null,
    val positionY: Int? = null,
)

@Serializable
data class PanelLayoutSnapshot(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val widthPx: Float = 0f,
    val heightPx: Float = 0f,
    val expandedHeightPx: Float = 0f,
    val isCollapsed: Boolean = false,
)

@Serializable
data class WorkspacePreset(
    val id: String,
    val name: String,
    val mode: String,
    val visibleSlots: List<Int> = emptyList(),
    val layouts: Map<Int, PanelLayoutSnapshot> = emptyMap(),
)

@Serializable
data class WorkspaceUiSettings(
    val hiddenCustomerPanels: Set<Int> = emptySet(),
    val hiddenSchedulingPanels: Set<Int> = emptySet(),
    val presets: List<WorkspacePreset> = emptyList(),
)
