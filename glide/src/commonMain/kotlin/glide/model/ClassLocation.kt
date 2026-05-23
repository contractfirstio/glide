package glide.model

import java.util.UUID

data class ClassLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** Maximum occupancy for this room; null means unset. */
    val maxCapacity: Int? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
)
