package glide.ui.scheduling

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.DayCapacity

/** Visual room capacity: chairs show occupants when filled. */
@Composable
fun ClassCapacityGraphic(
    occupiedCount: Int,
    maxCapacity: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (maxCapacity == null) {
            UnboundedCapacityGraphic(occupiedCount = occupiedCount)
            return
        }

        val capacity = maxCapacity.coerceAtLeast(1)
        val occupied = occupiedCount.coerceIn(0, capacity)
        val available = capacity - occupied
        val isFull = occupied >= capacity

        Text(
            text = buildString {
                append("$occupied of $capacity seats filled")
                if (available > 0) append(" · $available available")
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (isFull) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Spacer(modifier = Modifier.height(10.dp))

        SeatGrid(
            capacity = capacity,
            occupied = occupied,
        )

        Spacer(modifier = Modifier.height(8.dp))
        CapacityLegend()
    }
}

/** Per-day capacity for weekly classes; each day has its own seat pool. */
@Composable
fun WeeklyClassCapacityGraphic(
    dayCapacities: List<DayCapacity>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Capacity is per day",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        dayCapacities.forEachIndexed { index, dayCapacity ->
            WeeklyDayCapacityRow(dayCapacity = dayCapacity)
            if (index < dayCapacities.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
        if (dayCapacities.any { it.maxCapacity != null }) {
            Spacer(modifier = Modifier.height(8.dp))
            CapacityLegend()
        }
    }
}

@Composable
fun WeeklyDayCapacityRow(
    dayCapacity: DayCapacity,
    addingHeadcount: Int = 0,
) {
    val occupied = dayCapacity.occupied
    val maxCapacity = dayCapacity.maxCapacity
    val projected = occupied + addingHeadcount

    if (maxCapacity == null) {
        Text(
            text = buildString {
                append(dayCapacity.day.shortLabel)
                append(" · ")
                append(if (occupied == 0) "no enrollment" else "$occupied enrolled")
                if (addingHeadcount > 0) append(" (+$addingHeadcount if linked)")
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val capacity = maxCapacity.coerceAtLeast(1)
    val displayOccupied = occupied.coerceIn(0, capacity)
    val available = (capacity - occupied).coerceAtLeast(0)
    val wouldExceed = dayCapacity.wouldExceed(addingHeadcount)
    val isFull = occupied >= capacity

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = buildString {
                append(dayCapacity.day.shortLabel)
                append(" · ")
                if (addingHeadcount > 0) {
                    append("$occupied of $capacity seats filled")
                    append(" → $projected if linked")
                } else {
                    append("$occupied of $capacity seats filled")
                    if (available > 0) append(" · $available available")
                }
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = when {
                wouldExceed -> MaterialTheme.colorScheme.error
                isFull -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (addingHeadcount == 0) {
            Spacer(modifier = Modifier.height(6.dp))
            SeatGrid(
                capacity = capacity,
                occupied = displayOccupied,
            )
        } else if (wouldExceed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Full — not enough seats for $addingHeadcount more",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${capacity - projected} seats left after linking",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnboundedCapacityGraphic(occupiedCount: Int) {
    Column {
        Text(
            text = if (occupiedCount == 0) {
                "No enrollment yet"
            } else {
                "$occupiedCount enrolled · no room capacity set for this location"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (occupiedCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            SeatGrid(
                capacity = occupiedCount.coerceAtMost(32),
                occupied = occupiedCount.coerceAtMost(32),
                overflowLabel = if (occupiedCount > 32) "+${occupiedCount - 32}" else null,
            )
        }
    }
}

@Composable
private fun SeatGrid(
    capacity: Int,
    occupied: Int,
    overflowLabel: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(capacity) { index ->
                ChairSeat(filled = index < occupied)
            }
            overflowLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun ChairSeat(filled: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    val seatColor = if (filled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    }
    val backColor = if (filled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 16.dp, height = 11.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(backColor)
                .border(
                    width = 1.dp,
                    color = outline.copy(alpha = if (filled) 0.45f else 0.85f),
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                ),
        )
        Box(
            modifier = Modifier
                .size(width = 24.dp, height = 11.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(seatColor)
                .border(
                    width = 1.dp,
                    color = outline.copy(alpha = if (filled) 0.4f else 0.85f),
                    shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (filled) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary),
                )
            }
        }
    }
}

@Composable
private fun CapacityLegend() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(filled = true, label = "Occupied")
        LegendItem(filled = false, label = "Available")
    }
}

@Composable
private fun LegendItem(filled: Boolean, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChairSeat(filled = filled)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
