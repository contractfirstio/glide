package glide.data

import glide.model.SoldPlanEnrollment
import glide.model.dateRange
import glide.model.isOngoing
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val ROLLING_TERM_WARNING_WEEKS = 15

data class RollingTermCoverageAlert(
    val soldPlanId: String,
    val customerLabel: String,
    val planName: String,
    val weeksRemaining: Int,
    val blocksBilling: Boolean,
)

fun findRollingTermCoverageAlerts(
    warningWeeks: Int = ROLLING_TERM_WARNING_WEEKS,
    today: LocalDate = LocalDate.now(),
): List<RollingTermCoverageAlert> =
    SoldPlanEnrollmentStore.all
        .asSequence()
        .filter { it.status.isOngoing() && it.planSnapshot.rolling }
        .mapNotNull { it.toRollingTermCoverageAlertOrNull(warningWeeks, today) }
        .sortedWith(
            compareByDescending<RollingTermCoverageAlert> { it.blocksBilling }
                .thenBy { it.weeksRemaining }
                .thenBy { it.customerLabel.lowercase() },
        )
        .toList()

fun rollingTermCoverageAlertForSoldPlan(
    soldPlanId: String,
    warningWeeks: Int = ROLLING_TERM_WARNING_WEEKS,
    today: LocalDate = LocalDate.now(),
): RollingTermCoverageAlert? =
    SoldPlanEnrollmentStore.forSoldPlan(soldPlanId)
        ?.takeIf { it.planSnapshot.rolling }
        ?.toRollingTermCoverageAlertOrNull(warningWeeks, today)

fun rollingTermsBlockBillIssuance(
    soldPlanId: String,
    today: LocalDate = LocalDate.now(),
): Boolean =
    rollingTermCoverageAlertForSoldPlan(
        soldPlanId = soldPlanId,
        warningWeeks = ROLLING_TERM_WARNING_WEEKS,
        today = today,
    )?.blocksBilling == true

fun rollingTermsBlockBillIssuanceMessage(): String =
    "No future rolling terms are available. Add a new term before issuing or adding bills."

fun rollingTermCoverageWarningMessage(weeksRemaining: Int): String =
    if (weeksRemaining <= 0) {
        rollingTermsBlockBillIssuanceMessage()
    } else {
        "Only $weeksRemaining week${if (weeksRemaining == 1) "" else "s"} of rolling terms remain. " +
            "Add a new term soon to avoid billing interruption."
    }

private fun SoldPlanEnrollment.toRollingTermCoverageAlertOrNull(
    warningWeeks: Int,
    today: LocalDate,
): RollingTermCoverageAlert? {
    val soldPlan = findSoldPlanById(soldPlanId) ?: return null
    val scheduledClass = ClassStore.findClassContainingSoldPlan(soldPlanId) ?: return null
    val linkedTerms = scheduledClass.termIds
        .mapNotNull { termId -> TermStore.findById(termId) }
        .filter { it.acceptsRollingPlans }
    val latestRollingTermEnd = linkedTerms
        .mapNotNull { term -> term.dateRange()?.endInclusive }
        .maxOrNull()

    val weeksRemaining = if (latestRollingTermEnd == null || latestRollingTermEnd.isBefore(today)) {
        0
    } else {
        val daysRemaining = ChronoUnit.DAYS.between(today, latestRollingTermEnd).coerceAtLeast(0)
        ((daysRemaining + 6) / 7).toInt()
    }
    if (weeksRemaining > warningWeeks) return null

    val main = soldPlan.resolveMainClient()
    val customerLabel = main?.name?.ifBlank { "Customer" } ?: "Customer"
    val planName = planSnapshot.planName.takeIf { it.isNotBlank() } ?: "Plan"
    return RollingTermCoverageAlert(
        soldPlanId = soldPlanId,
        customerLabel = customerLabel,
        planName = planName,
        weeksRemaining = weeksRemaining,
        blocksBilling = weeksRemaining <= 0,
    )
}
