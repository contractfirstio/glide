package glide.model

import java.util.UUID

enum class PlanKind(val label: String) {
    MULTI_LESSON_PLAN("Multi Lesson Plan"),
    SINGLE_LESSON_PLAN("Single Lesson Plan"),
    CAMP("Camp"),
}

data class Plan(
    val id: String = UUID.randomUUID().toString(),
    val kind: PlanKind,
    val name: String,
    val lessonCount: Int,
    val rolling: Boolean = true,
    /** Price per person in minor currency units (e.g. pence). */
    val priceAmountMinor: Long = 0L,
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

fun Plan.summaryLine(): String = when (kind) {
    PlanKind.MULTI_LESSON_PLAN -> {
        val rollingLabel = if (rolling) "Rolling" else "Fixed"
        "$lessonCount classes · $rollingLabel"
    }
    PlanKind.SINGLE_LESSON_PLAN -> "1 class"
    PlanKind.CAMP -> "$lessonCount days"
}
