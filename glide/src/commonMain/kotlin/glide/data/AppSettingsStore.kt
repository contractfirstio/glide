package glide.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

object AppSettingsStore {
    private val _settings = mutableStateOf(AppSettings())

    val settingsState: MutableState<AppSettings> = _settings

    val settings: AppSettings get() = _settings.value

    val legalCompanyName: String get() = settings.legalCompanyName
    val fpsNumber: String get() = settings.fpsNumber
    val companyEmail: String get() = settings.companyEmail
    val companyPhone: String get() = settings.companyPhone

    val isConfigured: Boolean get() = settings.isConfigured

    val hasCompletedFirstSession: Boolean get() = settings.hasCompletedFirstSession

    val windowBounds: WindowBounds get() = settings.windowBounds

    fun shouldOfferBackupPrompt(): Boolean =
        isConfigured && hasCompletedFirstSession

    fun load() {
        _settings.value = readPersistedAppSettings()
    }

    fun save(settings: AppSettings) {
        val trimmed = AppSettings(
            legalCompanyName = settings.legalCompanyName.trim(),
            fpsNumber = settings.fpsNumber.trim(),
            companyEmail = settings.companyEmail.trim(),
            companyPhone = settings.companyPhone.trim(),
            hasCompletedFirstSession = _settings.value.hasCompletedFirstSession,
            windowBounds = _settings.value.windowBounds,
            workspaceUi = _settings.value.workspaceUi,
            googleCalendarSyncEnabled = _settings.value.googleCalendarSyncEnabled,
            googleCalendarSyncLastSyncMillis = _settings.value.googleCalendarSyncLastSyncMillis,
            googleCalendarSyncLastMessage = _settings.value.googleCalendarSyncLastMessage,
            googleCalendarAccountEmail = _settings.value.googleCalendarAccountEmail,
            googleCalendarId = _settings.value.googleCalendarId,
        )
        if (!trimmed.isConfigured) return
        _settings.value = trimmed
        persistAppSettings(trimmed)
    }

    fun saveGoogleCalendarSyncEnabled(enabled: Boolean) {
        val updated = _settings.value.copy(googleCalendarSyncEnabled = enabled)
        _settings.value = updated
        persistAppSettings(updated)
    }

    fun saveGoogleCalendarSyncStatus(message: String, syncedAtMillis: Long = System.currentTimeMillis()) {
        val updated = _settings.value.copy(
            googleCalendarSyncLastMessage = message,
            googleCalendarSyncLastSyncMillis = syncedAtMillis,
        )
        _settings.value = updated
        persistAppSettings(updated)
    }

    fun saveGoogleCalendarConnection(accountEmail: String, calendarId: String) {
        val updated = _settings.value.copy(
            googleCalendarAccountEmail = accountEmail.trim(),
            googleCalendarId = calendarId.trim(),
        )
        _settings.value = updated
        persistAppSettings(updated)
    }

    fun clearGoogleCalendarConnection() {
        val updated = _settings.value.copy(
            googleCalendarSyncEnabled = false,
            googleCalendarAccountEmail = "",
            googleCalendarId = "",
            googleCalendarSyncLastMessage = "",
            googleCalendarSyncLastSyncMillis = 0L,
        )
        _settings.value = updated
        persistAppSettings(updated)
    }

    fun saveWindowBounds(bounds: WindowBounds) {
        val updated = _settings.value.copy(windowBounds = bounds)
        _settings.value = updated
        persistAppSettings(updated)
    }

    fun saveWorkspaceUi(workspaceUi: WorkspaceUiSettings) {
        val updated = _settings.value.copy(workspaceUi = workspaceUi)
        _settings.value = updated
        persistAppSettings(updated)
    }

    fun markFirstSessionCompleted() {
        if (hasCompletedFirstSession) return
        val updated = _settings.value.copy(hasCompletedFirstSession = true)
        _settings.value = updated
        persistAppSettings(updated)
    }
}

internal expect fun readPersistedAppSettings(): AppSettings

internal expect fun persistAppSettings(settings: AppSettings)
