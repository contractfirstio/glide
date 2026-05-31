package glide.calendar

expect object GoogleCalendarSyncService {
    fun isConnected(): Boolean

    fun connectionLabel(): String

    fun connect(): CalendarSyncResult

    fun disconnect(): CalendarSyncResult

    fun setEnabled(enabled: Boolean): CalendarSyncResult

    fun syncNow(): CalendarSyncResult

    fun scheduleSyncIfEnabled()
}
