package glide.data.persistence

import glide.calendar.GoogleCalendarSyncService
import glide.data.AttendanceCreditStore
import glide.data.AttendanceStore
import glide.data.BillStore
import glide.data.ClassStore
import glide.data.ClientStore
import glide.data.LeadStore
import glide.data.LocationStore
import glide.data.PaymentStore
import glide.data.PlanStore
import glide.data.SoldPlanClassScheduleStore
import glide.data.SoldPlanEnrollmentStore
import glide.data.SoldPlanStore
import glide.data.StudentStore
import glide.data.TermStore
import glide.debug.GlidePanelDebug

object GlideDataRepository {
    private const val SAVE_DEBOUNCE_MS = 400L

    fun loadIntoStores() {
        val snapshot = readDataSnapshot()
        if (snapshot == null) {
            GlidePanelDebug.log(GlidePanelDebug.Panel.DATA, "loadIntoStores", "no snapshot file")
            return
        }
        GlidePanelDebug.log(
            GlidePanelDebug.Panel.DATA,
            "loadIntoStores",
            "plans=${snapshot.plans.size} soldPlans=${snapshot.soldPlans.size} classes=${snapshot.classes.size}",
        )
        PlanStore.replaceAll(snapshot.plans)
        ClientStore.replaceAll(snapshot.clients)
        StudentStore.replaceAll(snapshot.students)
        LeadStore.replaceAll(snapshot.leads)
        SoldPlanStore.replaceAll(snapshot.soldPlans)
        SoldPlanEnrollmentStore.replaceAll(snapshot.soldPlanEnrollments)
        BillStore.replaceAll(snapshot.bills)
        PaymentStore.replaceAll(snapshot.payments)
        AttendanceCreditStore.replaceAll(snapshot.attendanceCredits)
        TermStore.replaceAll(snapshot.terms)
        LocationStore.replaceAll(snapshot.locations)
        ClassStore.replaceAll(snapshot.classes)
        SoldPlanClassScheduleStore.replaceAll(snapshot.soldPlanClassSchedules)
        AttendanceStore.replaceAll(
            records = snapshot.attendanceRecords,
            submittedSessions = snapshot.submittedAttendanceSessions,
        )
    }

    fun scheduleSave() {
        scheduleDebouncedSave(SAVE_DEBOUNCE_MS) {
            saveNow()
        }
    }

    fun saveNow() {
        val snapshot = collectSnapshotFromStores()
        GlidePanelDebug.log(
            GlidePanelDebug.Panel.DATA,
            "saveNow",
            "plans=${snapshot.plans.size} soldPlans=${snapshot.soldPlans.size} classes=${snapshot.classes.size}",
        )
        writeDataSnapshot(snapshot)
    }

    internal fun onStoresMutated() {
        scheduleSave()
        GoogleCalendarSyncService.scheduleSyncIfEnabled()
    }

    private fun collectSnapshotFromStores(): GlideDataSnapshot = GlideDataSnapshot(
        schemaVersion = CURRENT_SCHEMA_VERSION,
        savedAtMillis = System.currentTimeMillis(),
        plans = PlanStore.plans,
        clients = ClientStore.all,
        students = StudentStore.all,
        leads = LeadStore.all,
        soldPlans = SoldPlanStore.all,
        soldPlanEnrollments = SoldPlanEnrollmentStore.all,
        bills = BillStore.all,
        payments = PaymentStore.all,
        attendanceCredits = AttendanceCreditStore.all,
        terms = TermStore.terms,
        locations = LocationStore.locations,
        classes = ClassStore.classes,
        soldPlanClassSchedules = SoldPlanClassScheduleStore.all,
        attendanceRecords = AttendanceStore.records,
        submittedAttendanceSessions = AttendanceStore.submittedSessions,
    )
}
