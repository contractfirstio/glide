package glide.plans

import glide.data.AppSettingsStore
import glide.model.Plan
import glide.model.PlanKind
import glide.model.formatMoney
import glide.model.summaryLine

data class PackFlyerContent(
    val studioName: String,
    val packName: String,
    val planTypeLabel: String,
    val summaryLine: String,
    val priceLabel: String,
    val rollingDetail: String?,
    val notes: String,
)

fun Plan.toPackFlyerContent(): PackFlyerContent {
    val rollingDetail = when (kind) {
        PlanKind.MULTI_LESSON_PACK -> if (rolling) {
            "Rolling pack — unused classes stay on your balance within the pack."
        } else {
            "Fixed pack — use your classes within the pack period."
        }
        PlanKind.SINGLE_LESSON_PACK -> null
        PlanKind.CAMP -> "Fixed camp — runs over $lessonCount consecutive days."
    }
    return PackFlyerContent(
        studioName = AppSettingsStore.legalCompanyName,
        packName = name.ifBlank { "Dance pack" },
        planTypeLabel = kind.label,
        summaryLine = summaryLine(),
        priceLabel = "${formatMoney(priceAmountMinor, currencyCode)} per person",
        rollingDetail = rollingDetail,
        notes = notes.trim(),
    )
}
