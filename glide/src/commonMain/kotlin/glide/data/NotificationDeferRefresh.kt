package glide.data

import java.time.Duration
import java.time.LocalDateTime

/** Wake after local midnight so deferred alerts reappear on their remind date. */
fun millisUntilNextNotificationDeferCheck(): Long {
    val now = LocalDateTime.now()
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
    return Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1L)
}
