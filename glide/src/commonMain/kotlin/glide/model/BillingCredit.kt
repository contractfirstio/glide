package glide.model

import java.util.UUID

/** Credit toward the next pack bill for a customer group enrollment. */
data class BillingCredit(
    val id: String = UUID.randomUUID().toString(),
    val enrollmentId: String,
    val peopleGroupId: String,
    val amountMinor: Long,
    val currencyCode: String,
    val description: String,
    val scheduledClassId: String,
    val sessionDate: String,
    val attendeeKey: String,
    val appliedToBillId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
