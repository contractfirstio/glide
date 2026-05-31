package glide.calendar.google

import glide.data.persistence.glideApplicationSupportDir
import glide.data.persistence.glideJson
import java.io.File
import java.util.Properties
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class GoogleDownloadedOAuthJson(
    val installed: GoogleDownloadedOAuthSection? = null,
    val web: GoogleDownloadedOAuthSection? = null,
)

@Serializable
private data class GoogleDownloadedOAuthSection(
    @SerialName("client_id") val clientId: String = "",
    @SerialName("client_secret") val clientSecret: String? = null,
)

internal data class ResolvedGoogleOAuthCredentials(
    val clientId: String,
    val clientSecret: String? = null,
    val source: String,
    val isDesktopClient: Boolean,
)

internal object GoogleOAuthCredentials {
    fun resolve(): ResolvedGoogleOAuthCredentials? {
        resolveFromSystem()?.let { return it }
        resolveFromJsonFile()?.let { return it }
        resolveFromPropertiesFile()?.let { return it }
        return null
    }

    private fun resolveFromSystem(): ResolvedGoogleOAuthCredentials? {
        val clientId = System.getProperty("glide.google.oauth.clientId")?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv("GLIDE_GOOGLE_OAUTH_CLIENT_ID")?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null
        val clientSecret = System.getProperty("glide.google.oauth.clientSecret")?.trim()?.takeIf { it.isNotEmpty() }
            ?: System.getenv("GLIDE_GOOGLE_OAUTH_CLIENT_SECRET")?.trim()?.takeIf { it.isNotEmpty() }
        return ResolvedGoogleOAuthCredentials(
            clientId = clientId,
            clientSecret = clientSecret,
            source = "system",
            isDesktopClient = clientSecret.isNullOrBlank(),
        )
    }

    private fun resolveFromJsonFile(): ResolvedGoogleOAuthCredentials? {
        val jsonFile = File(glideApplicationSupportDir(), "google-oauth-client.json")
        if (!jsonFile.isFile) return null
        val parsed = runCatching {
            glideJson.decodeFromString<GoogleDownloadedOAuthJson>(jsonFile.readText())
        }.getOrNull() ?: return null

        parsed.installed?.clientId?.trim()?.takeIf { it.isNotEmpty() }?.let { clientId ->
            val secret = parsed.installed.clientSecret?.trim()?.takeIf { it.isNotEmpty() }
            return ResolvedGoogleOAuthCredentials(
                clientId = clientId,
                clientSecret = secret,
                source = "json-installed",
                isDesktopClient = true,
            )
        }
        parsed.web?.clientId?.trim()?.takeIf { it.isNotEmpty() }?.let { clientId ->
            val secret = parsed.web.clientSecret?.trim()?.takeIf { it.isNotEmpty() }
            return ResolvedGoogleOAuthCredentials(
                clientId = clientId,
                clientSecret = secret,
                source = "json-web",
                isDesktopClient = false,
            )
        }
        return null
    }

    private fun resolveFromPropertiesFile(): ResolvedGoogleOAuthCredentials? {
        val propertiesFile = File(glideApplicationSupportDir(), "google-oauth-client.properties")
        if (!propertiesFile.isFile) return null
        val properties = runCatching {
            Properties().apply { propertiesFile.inputStream().use { load(it) } }
        }.getOrNull() ?: return null
        val clientId = properties.getProperty("clientId")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val clientSecret = properties.getProperty("clientSecret")?.trim()?.takeIf { it.isNotEmpty() }
            ?: properties.getProperty("client_secret")?.trim()?.takeIf { it.isNotEmpty() }
        return ResolvedGoogleOAuthCredentials(
            clientId = clientId,
            clientSecret = clientSecret,
            source = "properties",
            isDesktopClient = clientSecret.isNullOrBlank(),
        )
    }
}
