package glide.model

import java.util.UUID

data class Location(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Maximum occupancy for this room; null means unset. */
    val maxCapacity: Int? = null,
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    fun formattedAddressLines(): List<String> = listOfNotNull(
        addressLine1.takeIf { it.isNotBlank() },
        addressLine2.takeIf { it.isNotBlank() },
        city.takeIf { it.isNotBlank() },
    )
}
