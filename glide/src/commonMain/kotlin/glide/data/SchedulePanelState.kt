package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.model.PeopleGroupType

/**
 * Cross-panel scheduling filters — class selection drives sold plans, term, calendar, and location;
 * sold plan selection drives classes, term, calendar, and location.
 */
object SchedulePanelState {
    var selectedClassId by mutableStateOf<String?>(null)
        private set
    var selectedSoldPlanId by mutableStateOf<String?>(null)
        private set

    /** Class selected in the Classes panel — filters sold plans and syncs term, calendar, and location. */
    fun onClassSelected(classId: String) {
        if (ScheduledClassStore.findById(classId) == null) return
        selectedSoldPlanId = null
        selectedClassId = classId
    }

    /** Sold plan selected in the Sold Plans panel — filters classes and syncs term, calendar, and location. */
    fun onSoldPlanSelected(groupId: String) {
        val group = PeopleGroupStore.findById(groupId) ?: return
        if (group.type != PeopleGroupType.CUSTOMER) return
        selectedSoldPlanId = groupId
        selectedClassId = ScheduledClassStore.findClassContainingCustomerGroup(groupId)?.id
    }

    /** Updates term/calendar/location context without clearing the sold-plan filter. */
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
        selectedSoldPlanId?.let { soldPlanId ->
            selectedClassId = ScheduledClassStore.findClassContainingCustomerGroup(soldPlanId)?.id
        }
    }

    fun onClassCleared() {
        selectedClassId = null
        selectedSoldPlanId?.let { soldPlanId ->
            selectedClassId = ScheduledClassStore.findClassContainingCustomerGroup(soldPlanId)?.id
        }
    }

    fun onSoldPlanCleared() {
        selectedSoldPlanId = null
        selectedClassId = null
    }

    fun clear() {
        selectedClassId = null
        selectedSoldPlanId = null
    }
}
