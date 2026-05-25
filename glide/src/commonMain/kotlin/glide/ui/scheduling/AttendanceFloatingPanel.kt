package glide.ui.scheduling

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.data.AttendancePanelState
import glide.data.ClassStore
import glide.model.AttendanceSessionKey
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.SchedulingPanelSlots

@Composable
fun AttendanceFloatingPanel(
    session: AttendanceSessionKey,
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    if (!AttendancePanelState.visible) return
    val stateSession = AttendancePanelState.sessionKey ?: return
    if (stateSession != session) return

    val scheduledClass = ClassStore.findById(session.classId)
    val title = attendancePanelTitle(scheduledClass, session.sessionDate)

    FloatingPanelShell(
        title = title,
        slot = SchedulingPanelSlots.ATTENDANCE,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
        onClose = { AttendancePanelState.close() },
    ) {
        AttendancePanel(
            session = session,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
