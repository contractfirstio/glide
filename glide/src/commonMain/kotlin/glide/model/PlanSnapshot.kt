package glide.model

/** Frozen catalog fields at enrollment time — survives plan edits later. */
data class PlanSnapshot(
    val planId: String,
    val planName: String,
    val kind: PlanKind,
    val lessonCount: Int,
    val rolling: Boolean,
    val priceAmountMinor: Long,
    val currencyCode: String,
) {
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
