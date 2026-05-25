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
) {
    val isConfigured: Boolean
        get() = legalCompanyName.isNotBlank() &&
            fpsNumber.isNotBlank() &&
            companyEmail.isNotBlank() &&
            companyPhone.isNotBlank()
}
