package glide.ui.scheduling

import androidx.compose.ui.graphics.Color
import glide.model.Class
import kotlin.math.absoluteValue

/** Palette tuned for the dark Glide canvas. */
val ClassCalendarPalette: List<Int> = listOf(
    0xFF7EB0E0.toInt(),
    0xFF9BC4B8.toInt(),
    0xFFB0A8D4.toInt(),
    0xFFE8B87A.toInt(),
    0xFFE89A8E.toInt(),
    0xFFA8D4C0.toInt(),
    0xFFD4A8C8.toInt(),
    0xFF8EC4D4.toInt(),
    0xFFC4B89C.toInt(),
    0xFFB8C878.toInt(),
    0xFF9AAFD4.toInt(),
    0xFFD0A878.toInt(),
)

fun defaultCalendarColorArgb(classId: String): Int {
    val index = classId.hashCode().absoluteValue % ClassCalendarPalette.size
    return ClassCalendarPalette[index]
}

fun Class.resolvedCalendarColorArgb(): Int =
    calendarColorArgb ?: defaultCalendarColorArgb(id)

fun Class.resolvedCalendarColor(): Color =
    Color(resolvedCalendarColorArgb())

fun Int.toCalendarColor(): Color = Color(this)
