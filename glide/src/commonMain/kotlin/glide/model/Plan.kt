package glide.model

import java.util.UUID

enum class PlanKind(val label: String) {
    MULTI_LESSON_PACK("Multi Lesson Pack"),
}

data class Plan(
    val id: String = UUID.randomUUID().toString(),
    val kind: PlanKind,
    val name: String,
    val lessonCount: Int,
    val rolling: Boolean = true,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)

fun Plan.summaryLine(): String = when (kind) {
    PlanKind.MULTI_LESSON_PACK -> {
        val rollingLabel = if (rolling) "Rolling" else "Fixed"
        "$lessonCount classes · $rollingLabel"
    }
}
