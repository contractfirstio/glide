package glide.model

/** Frozen catalog fields at enrollment time — survives plan edits later. */
data class PlanSnapshot(
    val planId: String,
    val planName: String,
    val kind: PlanKind,
    val lessonCount: Int,
    val rolling: Boolean,
    /** Per-person rate frozen at enrollment. */
    val priceAmountMinor: Long,
    val currencyCode: String,
) {
    fun totalAmountMinor(householdSize: Int): Long =
        priceAmountMinor * householdSize.coerceAtLeast(1)

    /** Per-person credit for one missed class session (pack price ÷ class count). */
    fun perSessionCreditPerPersonMinor(): Long =
        if (lessonCount <= 0) priceAmountMinor else priceAmountMinor / lessonCount

    companion object {
        fun from(plan: Plan): PlanSnapshot = PlanSnapshot(
            planId = plan.id,
            planName = plan.name,
            kind = plan.kind,
            lessonCount = plan.lessonCount,
            rolling = plan.rolling,
            priceAmountMinor = plan.priceAmountMinor,
            currencyCode = plan.currencyCode,
        )
    }
}
