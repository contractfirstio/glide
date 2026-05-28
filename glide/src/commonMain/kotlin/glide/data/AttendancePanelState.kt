package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.debug.GlidePanelDebug
import glide.debug.panelStateLog
import glide.model.AttendanceSessionKey
import java.time.LocalDate

/**
 * Optional Class attendance panel — opened from a class block on the term calendar.
 */
object AttendancePanelState {
    var visible by mutableStateOf(false)
        private set
    var classId by mutableStateOf<String?>(null)
        private set
    var sessionDate by mutableStateOf<String?>(null)
        private set

    val sessionKey: AttendanceSessionKey?
        get() {
            val classId = classId ?: return null
            val date = sessionDate ?: return null
            return AttendanceSessionKey(classId = classId, sessionDate = date)
        }

    fun open(classId: String, sessionDate: LocalDate) {
        if (ClassStore.findById(classId) == null) {
            panelStateLog(
                GlidePanelDebug.Panel.ATTENDANCE,
                "open.miss",
                "classId=$classId date=$sessionDate",
            )
            return
        }
        panelStateLog(
            GlidePanelDebug.Panel.ATTENDANCE,
            "open",
            "classId=$classId date=$sessionDate mode=${AppViewState.mode}",
        )
        this.classId = classId
        this.sessionDate = sessionDate.toString()
        visible = AppViewState.mode == AppViewMode.SCHEDULING
    }

    fun openForReminder(classId: String, sessionDate: LocalDate) {
        panelStateLog(
            GlidePanelDebug.Panel.ATTENDANCE,
            "openForReminder",
            "classId=$classId date=$sessionDate",
        )
        this.classId = classId
        this.sessionDate = sessionDate.toString()
        visible = true
    }

    fun close() {
        panelStateLog(GlidePanelDebug.Panel.ATTENDANCE, "close", "classId=$classId date=$sessionDate")
        visible = false
    }

    fun clear() {
        panelStateLog(GlidePanelDebug.Panel.ATTENDANCE, "clear", "classId=$classId date=$sessionDate")
        classId = null
        sessionDate = null
        visible = false
    }
}
