package glide.calendar.google

import glide.data.persistence.glideApplicationSupportDir
import glide.data.persistence.glideJson
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

@Serializable
internal data class GoogleOAuthTokens(
    val refreshToken: String,
    val accessToken: String = "",
    val accessTokenExpiresAtMillis: Long = 0L,
    val accountEmail: String = "",
)

internal object GoogleTokenStore {
    private val tokenFile: File
        get() = File(glideApplicationSupportDir(), "google-calendar-tokens.json")

    fun load(): GoogleOAuthTokens? {
        val file = tokenFile
        if (!file.isFile) return null
        return runCatching {
            glideJson.decodeFromString<GoogleOAuthTokens>(file.readText())
        }.getOrNull()?.takeIf { it.refreshToken.isNotBlank() }
    }

    fun save(tokens: GoogleOAuthTokens) {
        val file = tokenFile
        file.parentFile?.mkdirs()
        file.writeText(glideJson.encodeToString(tokens))
        restrictPermissions(file)
    }

    fun clear() {
        val file = tokenFile
        if (file.isFile) {
            file.delete()
        }
    }

    fun isConnected(): Boolean = load()?.refreshToken?.isNotBlank() == true

    private fun restrictPermissions(file: File) {
        runCatching {
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
        }
    }
}
