package glide.data

data class AppSettings(
    val legalCompanyName: String = "",
    val fpsNumber: String = "",
    val companyEmail: String = "",
    val companyPhone: String = "",
    /** After the first app session, open/close backup prompts are offered. */
    val hasCompletedFirstSession: Boolean = false,
    val windowBounds: WindowBounds = WindowBounds(),
    val workspaceUi: WorkspaceUiSettings = WorkspaceUiSettings(),
    val googleCalendarSyncEnabled: Boolean = false,
    val googleCalendarSyncLastSyncMillis: Long = 0L,
    val googleCalendarSyncLastMessage: String = "",
    val googleCalendarAccountEmail: String = "",
    val googleCalendarId: String = "",
) {
    val isConfigured: Boolean
        get() = legalCompanyName.isNotBlank() &&
            fpsNumber.isNotBlank() &&
            companyEmail.isNotBlank() &&
            companyPhone.isNotBlank()
}
