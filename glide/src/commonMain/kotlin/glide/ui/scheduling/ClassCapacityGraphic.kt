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
