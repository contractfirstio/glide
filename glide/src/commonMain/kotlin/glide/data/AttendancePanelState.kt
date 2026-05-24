package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import glide.model.ClassSessionKey
import java.time.LocalDate

/**
 * Optional Class attendance panel — opened from a class block on the term calendar.
 */
object AttendancePanelState {
    var visible by mutableStateOf(false)
        private set
    var scheduledClassId by mutableStateOf<String?>(null)
        private set
    var sessionDate by mutableStateOf<String?>(null)
        private set

    val sessionKey: ClassSessionKey?
        get() {
            val classId = scheduledClassId ?: return null
            val date = sessionDate ?: return null
            return ClassSessionKey(scheduledClassId = classId, sessionDate = date)
        }

    fun open(scheduledClassId: String, sessionDate: LocalDate) {
        if (ScheduledClassStore.findById(scheduledClassId) == null) return
        this.scheduledClassId = scheduledClassId
        this.sessionDate = sessionDate.toString()
        visible = AppViewState.mode == AppViewMode.SCHEDULING
    }

    fun openForReminder(scheduledClassId: String, sessionDate: LocalDate) {
        this.scheduledClassId = scheduledClassId
        this.sessionDate = sessionDate.toString()
        visible = true
    }

    fun close() {
        visible = false
    }

    fun clear() {
        scheduledClassId = null
        sessionDate = null
        visible = false
    }
}
