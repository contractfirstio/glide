package glide.calendar.google

import glide.data.persistence.glideApplicationSupportDir
import java.io.File

internal object GoogleCalendarConfig {
    const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
    const val GLIDE_CALENDAR_TITLE = "Glide"
    const val GLIDE_UID_PROPERTY = "glideUid"
    const val OAUTH_REDIRECT_PORT = 8765

    /** Standard Google Desktop loopback redirect URI. */
    val redirectUri: String
        get() = "http://127.0.0.1:$OAUTH_REDIRECT_PORT/"

    val oauthRedirectPath: String
        get() = "/"

    private val credentials: ResolvedGoogleOAuthCredentials?
        get() = GoogleOAuthCredentials.resolve()

    fun clientId(): String? = credentials?.clientId

    fun clientSecret(): String? = credentials?.clientSecret

    fun isDesktopClient(): Boolean = credentials?.isDesktopClient == true

    fun logOAuthConfig() {
        val resolved = credentials
        if (resolved == null) {
            GoogleCalendarSyncDebug.log("oauth.config", "credentials=(not configured)")
            return
        }
        GoogleCalendarSyncDebug.log(
            "oauth.config",
            buildString {
                append("source=").append(resolved.source)
                append(" desktop=").append(resolved.isDesktopClient)
                append(" hasClientSecret=").append(!resolved.clientSecret.isNullOrBlank())
                append(" clientId=").append(resolved.clientId)
                append(" redirectUri=").append(redirectUri)
            },
        )
    }

    fun clientIdOrError(): Result<String> =
        credentialsOrError().map { it.clientId }

    fun credentialsOrError(): Result<ResolvedGoogleOAuthCredentials> {
        val credentials = credentials
            ?: return Result.failure(
                IllegalStateException(
                    "Google OAuth is not configured. Add " +
                        "${File(glideApplicationSupportDir(), "google-oauth-client.properties").absolutePath} " +
                        "with clientId and clientSecret, or save Google's Download JSON as " +
                        "${File(glideApplicationSupportDir(), "google-oauth-client.json").absolutePath}.",
                ),
            )
        if (credentials.clientSecret.isNullOrBlank()) {
            return Result.failure(
                IllegalStateException(
                    "Google OAuth client secret is not configured. Google requires clientSecret for Desktop clients. " +
                        "Copy it from Google Cloud → Credentials → your Desktop client, then add clientSecret=... to " +
                        "google-oauth-client.properties or use Download JSON as google-oauth-client.json.",
                ),
            )
        }
        return Result.success(credentials)
    }
}
