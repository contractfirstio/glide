package glide.model

const val DEFAULT_CURRENCY_CODE = "GBP"

/** Whole currency units (e.g. pounds) to minor units (pence). */
fun majorToMinor(major: Double): Long = (major * 100.0).toLong()

fun parseMajorAmount(input: String): Double? {
    val trimmed = input.trim().replace(",", "")
    if (trimmed.isBlank()) return null
    return trimmed.toDoubleOrNull()?.takeIf { it >= 0 }
}

fun formatMoney(amountMinor: Long, currencyCode: String = DEFAULT_CURRENCY_CODE): String {
    val major = amountMinor / 100
    val minor = kotlin.math.abs(amountMinor % 100)
    val amount = "$major.${minor.toString().padStart(2, '0')}"
    return when (currencyCode) {
        "GBP" -> "£$amount"
        "USD" -> "$$amount"
        "EUR" -> "€$amount"
        else -> "$currencyCode $amount"
    }
}
