package glide.debug

import glide.data.AppViewState
import glide.data.AttendancePanelState
import glide.data.BillingPanelState
import glide.data.ClientsPanelState
import glide.data.LeadNavigation
import glide.data.PlansPanelState
import glide.data.SchedulePanelState
import glide.data.StudentsPanelState
import glide.data.BillStore
import glide.data.ClassStore
import glide.data.ClientStore
import glide.data.LeadStore
import glide.data.PlanStore
import glide.data.SoldPlanStore
import glide.data.StudentStore
import glide.data.TermStore
import glide.data.LocationStore

/**
 * Unified debug logging for Glide panels and cross-panel state.
 * Output appears in the terminal when running `./gradlew :glide:run`.
 *
 * Copy lines starting with `[Glide:` when reporting issues.
 */
object GlidePanelDebug {
    const val TAG = "[Glide]"

    /** Set false to silence all panel debug logs. */
    var enabled: Boolean = true

    object Panel {
        const val APP = "App"
        const val WORKSPACE = "Workspace"
        const val NAV = "Nav"
        const val LEADS = "Leads"
        const val SOLD_PLANS = "SoldPlans"
        const val SOLD_PLANS_SCHED = "SoldPlansSched"
        const val PLANS = "Plans"
        const val CLIENTS = "Clients"
        const val STUDENTS = "Students"
        const val BILLING = "Billing"
        const val CLASSES = "Classes"
        const val TERMS = "Terms"
        const val LOCATIONS = "Locations"
        const val CALENDAR = "Calendar"
        const val ATTENDANCE = "Attendance"
        const val FLOATING = "Floating"
        const val DATA = "Data"
    }

    fun log(panel: String, event: String, detail: String = "") {
        if (!enabled) return
        val message = buildString {
            append(TAG)
            append(':')
            append(panel)
            append(' ')
            append(event)
            if (detail.isNotEmpty()) {
                append(" | ")
                append(detail)
            }
        }
        println(message)
    }

    /** Cross-panel filters and store counts — appended to panel state logs. */
    fun globalSnapshot(): String = buildString {
        append("mode=${AppViewState.mode}")
        append(" billing={visible=${BillingPanelState.visible} soldPlan=${BillingPanelState.soldPlanId}}")
        append(" plans.filter=${PlansPanelState.selectedPlanId}")
        append(" clients.filter=${ClientsPanelState.selectedClientId}")
        append(" students.filter=${StudentsPanelState.selectedStudentId}")
        append(" schedule={class=${SchedulePanelState.selectedClassId}")
        append(" soldPlan=${SchedulePanelState.selectedSoldPlanId}")
        append(" term=${SchedulePanelState.selectedTermFilterId}")
        append(" loc=${SchedulePanelState.selectedLocationFilterId}}")
        append(" attendance={visible=${AttendancePanelState.visible}")
        append(" session=${AttendancePanelState.sessionKey}}")
        append(" nav={lead=${LeadNavigation.pendingLeadId} soldPlan=${LeadNavigation.pendingSoldPlanId}}")
        append(" counts={plans=${PlanStore.plans.size} leads=${LeadStore.all.size}")
        append(" soldPlans=${SoldPlanStore.all.size} clients=${ClientStore.all.size}")
        append(" students=${StudentStore.all.size} classes=${ClassStore.classes.size}")
        append(" terms=${TermStore.terms.size} locations=${LocationStore.locations.size}")
        append(" bills=${BillStore.all.size}}")
    }

    fun logAction(panel: String, action: String, detail: String = "") {
        val full = if (detail.isEmpty()) globalSnapshot() else "$detail || ${globalSnapshot()}"
        log(panel, action, full)
    }
}
