package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.model.PeopleGroupType

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
        if (ScheduledClassStore.findById(classId) == null) return
        selectedSoldPlanId = null
        selectedTermFilterId = null
        selectedLocationFilterId = null
        selectedClassId = classId
    }

    /** Sold plan selected in the Sold Plans panel — filters classes and syncs term, calendar, and location. */
    fun onSoldPlanSelected(groupId: String) {
        val group = PeopleGroupStore.findById(groupId) ?: return
        if (group.type != PeopleGroupType.CUSTOMER) return
        selectedSoldPlanId = groupId
        selectedTermFilterId = null
        selectedLocationFilterId = null
        selectedClassId = ScheduledClassStore.findClassContainingCustomerGroup(groupId)?.id
    }

    /** Term selected in the Terms panel — filters classes, sold plans, and locations. */
    fun onTermSelected(termId: String) {
        if (TermStore.findById(termId) == null) return
        selectedSoldPlanId = null
        selectedClassId = null
        selectedLocationFilterId = null
        selectedTermFilterId = termId
    }

    /** Location selected in the Locations panel — filters classes, sold plans, and terms. */
    fun onLocationSelected(locationId: String) {
        if (LocationStore.findById(locationId) == null) return
        selectedSoldPlanId = null
        selectedClassId = null
        selectedTermFilterId = null
        selectedLocationFilterId = locationId
    }

    /** Updates class context without clearing the sold-plan filter. */
    fun syncClassContext(classId: String) {
        if (ScheduledClassStore.findById(classId) != null) {
            selectedClassId = classId
        }
    }

    /** Clears the sold-plan filter from the Classes panel. Does not change Sold Plans panel selection. */
    fun clearSoldPlanFilter() {
        selectedSoldPlanId = null
        selectedClassId = null
    }

    /** Clears the class filter from the Sold Plans panel. Does not change Classes panel selection. */
    fun clearClassFilter() {
        selectedClassId = null
        restoreClassContextFromSoldPlan()
    }

    /** Clears the location filter from the Terms panel. Does not change Locations panel selection. */
    fun clearLocationFilter() {
        selectedLocationFilterId = null
    }

    /** Clears the term filter from the Locations panel. Does not change Terms panel selection. */
    fun clearTermFilter() {
        selectedTermFilterId = null
    }

    fun onClassCleared() {
        selectedClassId = null
        restoreClassContextFromSoldPlan()
    }

    fun onSoldPlanCleared() {
        selectedSoldPlanId = null
        selectedClassId = null
    }

    fun onTermCleared() {
        selectedTermFilterId = null
    }

    fun onLocationCleared() {
        selectedLocationFilterId = null
    }

    fun clear() {
        selectedClassId = null
        selectedSoldPlanId = null
        selectedTermFilterId = null
        selectedLocationFilterId = null
    }

    private fun restoreClassContextFromSoldPlan() {
        selectedSoldPlanId?.let { soldPlanId ->
            selectedClassId = ScheduledClassStore.findClassContainingCustomerGroup(soldPlanId)?.id
        }
    }
}
