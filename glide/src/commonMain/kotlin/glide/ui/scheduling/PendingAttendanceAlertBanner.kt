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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import glide.data.AttendanceStore
import glide.data.PendingAttendanceSession
import glide.data.ClassStore
import glide.data.TermStore
import glide.data.findPastSessionsNeedingAttendance
import glide.data.millisUntilNextAttendanceReminderCheck
import glide.data.openPendingAttendanceSession
import glide.ui.leads.formatIsoDateForDisplay
import glide.ui.theme.GlideTextButton
import kotlinx.coroutines.delay

@Composable
fun rememberPendingAttendanceSessions(): List<PendingAttendanceSession> {
    var refreshTick by remember { mutableIntStateOf(0) }
    AttendanceStore.records
    ClassStore.classes
    TermStore.terms
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextAttendanceReminderCheck())
            refreshTick++
        }
    }
    refreshTick // timer-only refresh; attendance edits recompose via store reads above
    return findPastSessionsNeedingAttendance()
}

@Composable
fun PendingAttendanceAlertBanner(
    pending: List<PendingAttendanceSession>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (pending.isEmpty()) return
    val sessionCount = pending.size
    val first = pending.first()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = if (sessionCount == 1) {
                "Attendance not submitted for 1 past class"
            } else {
                "Attendance not submitted for $sessionCount past classes"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        if (!compact) {
            val preview = pending.take(3).joinToString(" · ") { item ->
                "${item.className} (${formatIsoDateForDisplay(item.session.sessionDate)})"
            }
            val suffix = if (pending.size > 3) " · …" else ""
            Text(
                text = preview + suffix,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
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
            GlideTextButton(onClick = { openPendingAttendanceSession(first) }) {
                Text(
                    text = if (compact) "Open" else "Open first class",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}
