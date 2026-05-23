package glide.ui.shared

import glide.ui.leads.formatIsoDateForDisplay

fun formatPersonLabel(name: String, dateOfBirth: String): String {
    val displayDob = formatIsoDateForDisplay(dateOfBirth)
    return if (displayDob.isNotBlank()) "$name · $displayDob" else name
}
