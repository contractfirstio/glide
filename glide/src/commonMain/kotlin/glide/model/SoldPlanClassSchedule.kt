package glide.model

data class SoldPlanClassSchedule(
    val soldPlanId: String,
    val classId: String,
    /** ISO yyyy-MM-dd session dates, in chronological order. */
    val sessionDates: List<String>,
)
