package glide.ui.scheduling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.PackEnrollmentStore
import glide.data.PeopleGroupStore
import glide.data.ScheduledClassStore
import glide.data.UnassignedSoldPack
import glide.data.findSoldPacksNotAssignedToClass
import glide.data.openUnassignedSoldPack
import glide.data.unassignedSoldPacksMessage
import glide.ui.theme.GlideTextButton

@Composable
fun rememberUnassignedSoldPacks(): List<UnassignedSoldPack> {
    PackEnrollmentStore.all
    PeopleGroupStore.all
    ScheduledClassStore.classes
    return findSoldPacksNotAssignedToClass()
}

@Composable
fun UnassignedSoldPackAlertBanner(
    unassigned: List<UnassignedSoldPack>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (unassigned.isEmpty()) return
    val packCount = unassigned.size
    val first = unassigned.first()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = unassignedSoldPacksMessage(packCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        if (!compact) {
            val preview = unassigned.take(3).joinToString(" · ") { item ->
                "${item.customerLabel} (${item.packName})"
            }
            val suffix = if (unassigned.size > 3) " · …" else ""
            Text(
                text = preview + suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlideTextButton(onClick = { openUnassignedSoldPack(first) }) {
                Text(
                    text = if (compact) "Open" else "Open first sold plan",
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
