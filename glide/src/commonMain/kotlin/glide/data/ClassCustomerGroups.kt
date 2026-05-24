package glide.data

import glide.model.Class

sealed class AddSoldPlanResult {
    data object Success : AddSoldPlanResult()
    data object AlreadyAssigned : AddSoldPlanResult()
    data object GroupNotFound : AddSoldPlanResult()
    data object NotASoldPlan : AddSoldPlanResult()
    data class AlreadyOnAnotherClass(val className: String) : AddSoldPlanResult()
    data class PlanCannotBeFullyScheduled(
        val planSessions: Int,
        val availableSessions: Int,
    ) : AddSoldPlanResult()
    data class CapacityExceeded(
        val currentHeadcount: Int,
        val groupHeadcount: Int,
        val maxCapacity: Int,
    ) : AddSoldPlanResult()
    data class RollingPlanNotAllowedOnClass(val message: String) : AddSoldPlanResult()
}

fun headcountForSoldPlans(soldPlanIds: List<String>): Int =
    soldPlanIds.sumOf { groupId ->
        findSoldPlanById(groupId)?.classAttendeeCount() ?: 0
    }

fun Class.enrolledHeadcount(): Int = headcountForSoldPlans(soldPlanIds)

fun isSoldPlanAvailableForClass(groupId: String, classId: String?): Boolean =
    ClassStore.findClassContainingSoldPlan(groupId, excludeClassId = classId) == null

fun validateSoldPlansForClass(soldPlanIds: List<String>, classId: String?): String? {
    for (groupId in soldPlanIds) {
        val other = ClassStore.findClassContainingSoldPlan(groupId, excludeClassId = classId)
            ?: continue
        val label = findSoldPlanById(groupId)?.resolveMainClient()?.name?.takeIf { it.isNotBlank() }
            ?: "This group"
        return "$label is already on \"${other.name}\". Each sold plan can only be on one class."
    }
    return null
}

fun tryAddSoldPlan(
    currentGroupIds: List<String>,
    groupId: String,
    locationId: String?,
    classId: String? = null,
    scheduledClass: Class? = null,
): AddSoldPlanResult {
    if (groupId in currentGroupIds) return AddSoldPlanResult.AlreadyAssigned
    val group = findSoldPlanById(groupId) ?: return AddSoldPlanResult.GroupNotFound

    ClassStore.findClassContainingSoldPlan(groupId, excludeClassId = classId)?.let { other ->
        return AddSoldPlanResult.AlreadyOnAnotherClass(other.name)
    }

    val cls = scheduledClass ?: classId?.let { ClassStore.findById(it) }
    if (cls != null) {
        validateRollingSoldPlanEnrollmentForClass(groupId, cls)?.let { message ->
            return AddSoldPlanResult.RollingPlanNotAllowedOnClass(message)
        }
        planScheduleCheckForClass(groupId, cls)?.let { check ->
            if (!check.canFullySchedule) {
                return AddSoldPlanResult.PlanCannotBeFullyScheduled(
                    planSessions = check.requiredSessions,
                    availableSessions = check.availableSessions,
                )
            }
        }
    }

    val maxCapacity = locationId?.let { LocationStore.findById(it)?.maxCapacity }
    if (maxCapacity != null) {
        val current = headcountForSoldPlans(currentGroupIds)
        val adding = group.classAttendeeCount()
        if (current + adding > maxCapacity) {
            return AddSoldPlanResult.CapacityExceeded(
                currentHeadcount = current,
                groupHeadcount = adding,
                maxCapacity = maxCapacity,
            )
        }
    }
    return AddSoldPlanResult.Success
}

fun AddSoldPlanResult.toUserMessage(): String = when (this) {
    AddSoldPlanResult.Success -> "Sold plan added to class."
    AddSoldPlanResult.AlreadyAssigned -> "This group is already on the class."
    AddSoldPlanResult.GroupNotFound -> "Sold plan not found."
    AddSoldPlanResult.NotASoldPlan -> "Only sold plans can be assigned to classes."
    is AddSoldPlanResult.AlreadyOnAnotherClass ->
        "This group is already assigned to \"$className\". A sold plan can only be on one class."
    is AddSoldPlanResult.PlanCannotBeFullyScheduled ->
        planCannotFullyScheduleMessage(planSessions, availableSessions)
    is AddSoldPlanResult.CapacityExceeded ->
        "Room capacity exceeded ($currentHeadcount + $groupHeadcount > $maxCapacity)."
    is AddSoldPlanResult.RollingPlanNotAllowedOnClass -> message
}
