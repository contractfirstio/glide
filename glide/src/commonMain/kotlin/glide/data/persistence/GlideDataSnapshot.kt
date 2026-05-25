package glide.data.persistence

import glide.model.AttendanceCredit
import glide.model.AttendanceRecord
import glide.model.AttendanceSessionKey
import glide.model.Bill
import glide.model.Class
import glide.model.Client
import glide.model.Lead
import glide.model.Location
import glide.model.Payment
import glide.model.Plan
import glide.model.SoldPlan
import glide.model.SoldPlanClassSchedule
import glide.model.SoldPlanEnrollment
import glide.model.Student
import glide.model.Term
import kotlinx.serialization.Serializable

@Serializable
data class GlideDataSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val savedAtMillis: Long = System.currentTimeMillis(),
    val plans: List<Plan> = emptyList(),
    val clients: List<Client> = emptyList(),
    val students: List<Student> = emptyList(),
    val leads: List<Lead> = emptyList(),
    val soldPlans: List<SoldPlan> = emptyList(),
    val soldPlanEnrollments: List<SoldPlanEnrollment> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val attendanceCredits: List<AttendanceCredit> = emptyList(),
    val terms: List<Term> = emptyList(),
    val locations: List<Location> = emptyList(),
    val classes: List<Class> = emptyList(),
    val soldPlanClassSchedules: List<SoldPlanClassSchedule> = emptyList(),
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val submittedAttendanceSessions: List<AttendanceSessionKey> = emptyList(),
)

const val CURRENT_SCHEMA_VERSION = 1
