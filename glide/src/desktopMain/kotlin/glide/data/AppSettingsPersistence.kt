package glide.data

import glide.data.persistence.glideApplicationSupportDir
import glide.data.persistence.glideJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import glide.data.persistence.legacyDocumentsGlideDir
import java.io.File
import java.util.Properties

internal actual fun readPersistedAppSettings(): AppSettings {
    val file = settingsFile()
    if (file.isFile) {
        return readProperties(file)
    }
    val legacy = legacySettingsFile()
    if (legacy.isFile) {
        val settings = readProperties(legacy)
        persistAppSettings(settings)
        return settings
    }
    return AppSettings()
}

internal actual fun persistAppSettings(settings: AppSettings) {
    val file = settingsFile()
    file.parentFile?.mkdirs()
    val properties = Properties()
    if (file.isFile) {
        runCatching { file.inputStream().use { properties.load(it) } }
    }
    properties.setProperty(PROPERTY_LEGAL_COMPANY_NAME, settings.legalCompanyName)
    properties.setProperty(PROPERTY_FPS_NUMBER, settings.fpsNumber)
    properties.setProperty(PROPERTY_COMPANY_EMAIL, settings.companyEmail)
    properties.setProperty(PROPERTY_COMPANY_PHONE, settings.companyPhone)
    properties.setProperty(
        PROPERTY_HAS_COMPLETED_FIRST_SESSION,
        settings.hasCompletedFirstSession.toString(),
    )
    properties.setProperty(PROPERTY_WINDOW_BOUNDS, glideJson.encodeToString(settings.windowBounds))
    properties.setProperty(PROPERTY_WORKSPACE_UI, glideJson.encodeToString(settings.workspaceUi))
    properties.setProperty(PROPERTY_GOOGLE_CALENDAR_SYNC_ENABLED, settings.googleCalendarSyncEnabled.toString())
    properties.setProperty(
        PROPERTY_GOOGLE_CALENDAR_SYNC_LAST_SYNC_MILLIS,
        settings.googleCalendarSyncLastSyncMillis.toString(),
    )
    properties.setProperty(PROPERTY_GOOGLE_CALENDAR_SYNC_LAST_MESSAGE, settings.googleCalendarSyncLastMessage)
    properties.setProperty(PROPERTY_GOOGLE_CALENDAR_ACCOUNT_EMAIL, settings.googleCalendarAccountEmail)
    properties.setProperty(PROPERTY_GOOGLE_CALENDAR_ID, settings.googleCalendarId)
    properties.setProperty(
        PROPERTY_NOTIFICATION_DEFERRALS,
        glideJson.encodeToString(settings.notificationDeferrals),
    )
    file.outputStream().use { properties.store(it, "Glide app settings") }
}

private fun readProperties(file: File): AppSettings = runCatching {
    val properties = Properties().apply { file.inputStream().use { load(it) } }
    val legalCompanyName = properties.getProperty(PROPERTY_LEGAL_COMPANY_NAME, "").orEmpty()
    val fpsNumber = properties.getProperty(PROPERTY_FPS_NUMBER, "").orEmpty()
    val companyEmail = properties.getProperty(PROPERTY_COMPANY_EMAIL, "").orEmpty()
    val companyPhone = properties.getProperty(PROPERTY_COMPANY_PHONE, "").orEmpty()
    val hasCompletedFirstSession = when (properties.getProperty(PROPERTY_HAS_COMPLETED_FIRST_SESSION)) {
        "true" -> true
        "false" -> false
        null -> legalCompanyName.isNotBlank() &&
            fpsNumber.isNotBlank() &&
            companyEmail.isNotBlank() &&
            companyPhone.isNotBlank()
        else -> false
    }
    val windowBounds = properties.getProperty(PROPERTY_WINDOW_BOUNDS)?.let { json ->
        runCatching { glideJson.decodeFromString<WindowBounds>(json) }.getOrDefault(WindowBounds())
    } ?: WindowBounds()
    val workspaceUi = properties.getProperty(PROPERTY_WORKSPACE_UI)?.let { json ->
        runCatching { glideJson.decodeFromString<WorkspaceUiSettings>(json) }.getOrDefault(WorkspaceUiSettings())
    } ?: WorkspaceUiSettings()
    val googleCalendarSyncEnabled =
        properties.getProperty(PROPERTY_GOOGLE_CALENDAR_SYNC_ENABLED, "false") == "true"
    val googleCalendarSyncLastSyncMillis =
        properties.getProperty(PROPERTY_GOOGLE_CALENDAR_SYNC_LAST_SYNC_MILLIS)?.toLongOrNull() ?: 0L
    val googleCalendarSyncLastMessage =
        properties.getProperty(PROPERTY_GOOGLE_CALENDAR_SYNC_LAST_MESSAGE, "").orEmpty()
    val googleCalendarAccountEmail =
        properties.getProperty(PROPERTY_GOOGLE_CALENDAR_ACCOUNT_EMAIL, "").orEmpty()
    val googleCalendarId = properties.getProperty(PROPERTY_GOOGLE_CALENDAR_ID, "").orEmpty()
    val notificationDeferrals = properties.getProperty(PROPERTY_NOTIFICATION_DEFERRALS)?.let { json ->
        runCatching { glideJson.decodeFromString<NotificationDeferrals>(json) }
            .getOrDefault(NotificationDeferrals())
    } ?: NotificationDeferrals()
    AppSettings(
        legalCompanyName = legalCompanyName,
        fpsNumber = fpsNumber,
        companyEmail = companyEmail,
        companyPhone = companyPhone,
        hasCompletedFirstSession = hasCompletedFirstSession,
        windowBounds = windowBounds,
        workspaceUi = workspaceUi,
        googleCalendarSyncEnabled = googleCalendarSyncEnabled,
        googleCalendarSyncLastSyncMillis = googleCalendarSyncLastSyncMillis,
        googleCalendarSyncLastMessage = googleCalendarSyncLastMessage,
        googleCalendarAccountEmail = googleCalendarAccountEmail,
        googleCalendarId = googleCalendarId,
        notificationDeferrals = notificationDeferrals,
    )
}.getOrDefault(AppSettings())

private const val PROPERTY_LEGAL_COMPANY_NAME = "legalCompanyName"
private const val PROPERTY_FPS_NUMBER = "fpsNumber"
private const val PROPERTY_COMPANY_EMAIL = "companyEmail"
private const val PROPERTY_COMPANY_PHONE = "companyPhone"
private const val PROPERTY_HAS_COMPLETED_FIRST_SESSION = "hasCompletedFirstSession"
private const val PROPERTY_WINDOW_BOUNDS = "windowBounds"
private const val PROPERTY_WORKSPACE_UI = "workspaceUi"
private const val PROPERTY_GOOGLE_CALENDAR_SYNC_ENABLED = "googleCalendarSyncEnabled"
private const val PROPERTY_GOOGLE_CALENDAR_SYNC_LAST_SYNC_MILLIS = "googleCalendarSyncLastSyncMillis"
private const val PROPERTY_GOOGLE_CALENDAR_SYNC_LAST_MESSAGE = "googleCalendarSyncLastMessage"
private const val PROPERTY_GOOGLE_CALENDAR_ACCOUNT_EMAIL = "googleCalendarAccountEmail"
private const val PROPERTY_GOOGLE_CALENDAR_ID = "googleCalendarId"
private const val PROPERTY_NOTIFICATION_DEFERRALS = "notificationDeferrals"

private fun settingsFile(): File = File(glideApplicationSupportDir(), "settings.properties")

private fun legacySettingsFile(): File = File(legacyDocumentsGlideDir(), "settings.properties")
