package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog

/**
 * Cross-panel scheduling filters — selections in classes, sold plans, terms, and locations
 * drive filtering and context sync across the scheduling view.
 */
object SchedulePanelState {
    var selectedClassId by mutableStateOf<String?>(null)
        private set
    var selectedSoldPlanId by mutableStateOf<String?>(null)
        private set
    var selectedTermFilterId by mutableStateOf<String?>(null)
        private set
    var selectedLocationFilterId by mutableStateOf<String?>(null)
        private set

    /** Class selected in the Classes panel — filters sold plans and syncs term, calendar, and location. */
    fun onClassSelected(classId: String) {
        if (ClassStore.findById(classId) == null) {
            panelStateLog(GlidePanelDebug.Panel.CLASSES, "onClassSelected.miss", "classId=$classId")
            return
        }
        panelStateLog(GlidePanelDebug.Panel.CLASSES, "onClassSelected", "classId=$classId")
        selectedSoldPlanId = null
        selectedTermFilterId = null
        selectedLocationFilterId = null
        selectedClassId = classId
    }

    /** Sold plan selected in the Sold Plans panel — filters classes and syncs term, calendar, and location. */
    fun onSoldPlanSelected(groupId: String) {
        val group = findSoldPlanById(groupId)
        if (group == null) {
            panelStateLog(GlidePanelDebug.Panel.SOLD_PLANS_SCHED, "onSoldPlanSelected.miss", "soldPlanId=$groupId")
            return
        }
        panelStateLog(GlidePanelDebug.Panel.SOLD_PLANS_SCHED, "onSoldPlanSelected", "soldPlanId=$groupId")
        selectedSoldPlanId = groupId
        selectedTermFilterId = null
        selectedLocationFilterId = null
        selectedClassId = ClassStore.findClassContainingSoldPlan(groupId)?.id
    }

    /** Term selected in the Terms panel — filters classes, sold plans, and locations. */
    fun onTermSelected(termId: String) {
        if (TermStore.findById(termId) == null) {
            panelStateLog(GlidePanelDebug.Panel.TERMS, "onTermSelected.miss", "termId=$termId")
            return
        }
        panelStateLog(GlidePanelDebug.Panel.TERMS, "onTermSelected", "termId=$termId")
        selectedSoldPlanId = null
        selectedClassId = null
        selectedLocationFilterId = null
        selectedTermFilterId = termId
    }

    /** Location selected in the Locations panel — filters classes, sold plans, and terms. */
    fun onLocationSelected(locationId: String) {
        if (LocationStore.findById(locationId) == null) {
            panelStateLog(GlidePanelDebug.Panel.LOCATIONS, "onLocationSelected.miss", "locationId=$locationId")
            return
        }
        panelStateLog(GlidePanelDebug.Panel.LOCATIONS, "onLocationSelected", "locationId=$locationId")
        selectedSoldPlanId = null
        selectedClassId = null
        selectedTermFilterId = null
        selectedLocationFilterId = locationId
    }

    /** Updates class context without clearing the sold-plan filter. */
    fun syncClassContext(classId: String) {
        if (ClassStore.findById(classId) != null) {
            panelStateLog(GlidePanelDebug.Panel.CLASSES, "syncClassContext", "classId=$classId")
            selectedClassId = classId
        }
    }

    /** Clears the sold-plan filter from the Classes panel. Does not change Sold Plans panel selection. */
    fun clearSoldPlanFilter() {
        panelStateLog(GlidePanelDebug.Panel.CLASSES, "clearSoldPlanFilter")
        selectedSoldPlanId = null
        selectedClassId = null
    }

    /** Clears the class filter from the Sold Plans panel. Does not change Classes panel selection. */
    fun clearClassFilter() {
        panelStateLog(GlidePanelDebug.Panel.SOLD_PLANS_SCHED, "clearClassFilter")
        selectedClassId = null
        restoreClassContextFromSoldPlan()
    }

    /** Clears the location filter from the Terms panel. Does not change Locations panel selection. */
    fun clearLocationFilter() {
        panelStateLog(GlidePanelDebug.Panel.TERMS, "clearLocationFilter")
        selectedLocationFilterId = null
    }

    /** Clears the term filter from the Locations panel. Does not change Terms panel selection. */
    fun clearTermFilter() {
        panelStateLog(GlidePanelDebug.Panel.LOCATIONS, "clearTermFilter")
        selectedTermFilterId = null
    }

    fun onClassCleared() {
        panelStateLog(GlidePanelDebug.Panel.CLASSES, "onClassCleared")
        selectedClassId = null
        restoreClassContextFromSoldPlan()
    }

    fun onSoldPlanCleared() {
        panelStateLog(GlidePanelDebug.Panel.SOLD_PLANS_SCHED, "onSoldPlanCleared")
        selectedSoldPlanId = null
        selectedClassId = null
    }

    fun onTermCleared() {
        panelStateLog(GlidePanelDebug.Panel.TERMS, "onTermCleared")
        selectedTermFilterId = null
    }

    fun onLocationCleared() {
        panelStateLog(GlidePanelDebug.Panel.LOCATIONS, "onLocationCleared")
        selectedLocationFilterId = null
    }

    fun clear() {
        panelStateLog(GlidePanelDebug.Panel.CLASSES, "clear", "scheduling filters reset")
        selectedClassId = null
        selectedSoldPlanId = null
        selectedTermFilterId = null
        selectedLocationFilterId = null
    }

    private fun restoreClassContextFromSoldPlan() {
        selectedSoldPlanId?.let { soldPlanId ->
            selectedClassId = ClassStore.findClassContainingSoldPlan(soldPlanId)?.id
        }
    }
}
