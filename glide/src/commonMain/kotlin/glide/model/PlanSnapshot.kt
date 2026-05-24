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
    /** Plan price × number of class participants (attending members). */
    fun totalAmountMinor(classParticipantCount: Int): Long =
        priceAmountMinor * classParticipantCount.coerceAtLeast(1)

    /** Per-person credit for one missed class session (plan price ÷ class count). */
    fun perSessionCreditPerPersonMinor(): Long =
        if (lessonCount <= 0) priceAmountMinor else priceAmountMinor / lessonCount

    /**
     * Max class sessions for this plan on a single class, or null if unlimited (rolling).
     */
    fun classSessionLimit(): Int? = when {
        rolling -> null
        kind == PlanKind.SINGLE_LESSON_PLAN -> 1
        else -> lessonCount.coerceAtLeast(1)
    }

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
