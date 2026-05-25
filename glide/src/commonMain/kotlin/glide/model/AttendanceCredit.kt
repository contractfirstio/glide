package glide.model

import java.util.UUID

import kotlinx.serialization.Serializable
/** Credit toward the next plan bill for a sold plan enrollment. */
@Serializable
data class AttendanceCredit(
    val id: String = UUID.randomUUID().toString(),
    val enrollmentId: String,
    val soldPlanId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val description: String,
    val classId: String,
    val sessionDate: String,
    val attendeeKey: String,
    val appliedToBillId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)