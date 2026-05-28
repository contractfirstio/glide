package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog

/**
 * Cross-panel state from the Students panel — filters sold plans and clients for
 * [selectedStudentId].
 */
object StudentsPanelState {
    var selectedStudentId by mutableStateOf<String?>(null)
        private set

    fun onStudentSelected(personId: String) {
        if (StudentStore.findById(personId) != null) {
            panelStateLog(GlidePanelDebug.Panel.STUDENTS, "onStudentSelected", "studentId=$personId")
            BillingPanelState.onSoldPlanCleared()
            ClientsPanelState.clearClientFilter()
            PlansPanelState.clearPlanFilter()
            selectedStudentId = personId
        } else {
            panelStateLog(GlidePanelDebug.Panel.STUDENTS, "onStudentSelected.miss", "studentId=$personId")
        }
    }

    /** Clears the cross-panel filter. Does not change Students panel selection. */
    fun clearStudentFilter() {
        panelStateLog(GlidePanelDebug.Panel.STUDENTS, "clearStudentFilter", "was studentId=$selectedStudentId")
        selectedStudentId = null
    }
}
