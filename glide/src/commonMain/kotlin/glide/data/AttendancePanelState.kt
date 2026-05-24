package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        if (ClassStore.findById(classId) == null) return
        this.classId = classId
        this.sessionDate = sessionDate.toString()
        visible = AppViewState.mode == AppViewMode.SCHEDULING
    }

    fun openForReminder(classId: String, sessionDate: LocalDate) {
        this.classId = classId
        this.sessionDate = sessionDate.toString()
        visible = true
    }

    fun close() {
        visible = false
    }

    fun clear() {
        classId = null
        sessionDate = null
        visible = false
    }
}
