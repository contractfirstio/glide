package glide.data

import java.io.File
import java.util.Properties

internal actual fun readPersistedAppSettings(): AppSettings {
    val file = settingsFile()
    if (!file.isFile) return AppSettings()
    return runCatching {
        val properties = Properties().apply { file.inputStream().use { load(it) } }
        AppSettings(
            legalCompanyName = properties.getProperty(PROPERTY_LEGAL_COMPANY_NAME, "").orEmpty(),
            fpsNumber = properties.getProperty(PROPERTY_FPS_NUMBER, "").orEmpty(),
            companyEmail = properties.getProperty(PROPERTY_COMPANY_EMAIL, "").orEmpty(),
            companyPhone = properties.getProperty(PROPERTY_COMPANY_PHONE, "").orEmpty(),
        )
    }.getOrDefault(AppSettings())
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
    file.outputStream().use { properties.store(it, "Glide app settings") }
}

private const val PROPERTY_LEGAL_COMPANY_NAME = "legalCompanyName"
private const val PROPERTY_FPS_NUMBER = "fpsNumber"
private const val PROPERTY_COMPANY_EMAIL = "companyEmail"
private const val PROPERTY_COMPANY_PHONE = "companyPhone"

private fun settingsFile(): File {
    val documents = File(System.getProperty("user.home"), "Documents")
    return File(documents, "Glide/settings.properties")
}
