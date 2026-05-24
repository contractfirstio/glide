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

    fun load() {
        _settings.value = readPersistedAppSettings()
    }

    fun save(settings: AppSettings) {
        val trimmed = AppSettings(
            legalCompanyName = settings.legalCompanyName.trim(),
            fpsNumber = settings.fpsNumber.trim(),
            companyEmail = settings.companyEmail.trim(),
            companyPhone = settings.companyPhone.trim(),
        )
        if (!trimmed.isConfigured) return
        _settings.value = trimmed
        persistAppSettings(trimmed)
    }
}

internal expect fun readPersistedAppSettings(): AppSettings

internal expect fun persistAppSettings(settings: AppSettings)
