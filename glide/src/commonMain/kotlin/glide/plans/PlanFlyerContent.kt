package glide.plans

import glide.data.AppSettingsStore
import glide.model.Plan
import glide.model.PlanKind
import glide.model.formatMoney
import glide.model.summaryLine

data class PlanFlyerContent(
    val studioName: String,
    val planName: String,
    val planTypeLabel: String,
    val summaryLine: String,
    val priceLabel: String,
    val rollingDetail: String?,
    val notes: String,
)

fun Plan.toPlanFlyerContent(): PlanFlyerContent {
    val rollingDetail = when (kind) {
        PlanKind.MULTI_LESSON_PLAN -> if (rolling) {
            "Rolling plan — unused classes stay on your balance within the plan."
        } else {
            "Fixed plan — use your classes within the plan period."
        }
        PlanKind.SINGLE_LESSON_PLAN -> null
        PlanKind.CAMP -> "Fixed camp — runs over $lessonCount consecutive days."
    }
    return PlanFlyerContent(
        studioName = AppSettingsStore.legalCompanyName,
        planName = name.ifBlank { "Dance plan" },
        planTypeLabel = kind.label,
        summaryLine = summaryLine(),
        priceLabel = "${formatMoney(priceAmountMinor, currencyCode)} per person",
        rollingDetail = rollingDetail,
        notes = notes.trim(),
    )
}
