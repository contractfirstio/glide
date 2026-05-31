package glide.calendar.google

import com.sun.net.httpserver.HttpServer
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.awt.Desktop
import java.net.InetSocketAddress
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0L,
    @SerialName("token_type") val tokenType: String = "",
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
private data class GoogleUserInfo(
    val email: String = "",
)

internal class GoogleOAuthClient(
    private val httpClient: HttpClient = defaultHttpClient(),
) {
    fun authorize(): Result<GoogleOAuthTokens> = synchronized(authorizeLock) {
        val credentials = GoogleCalendarConfig.credentialsOrError().getOrElse { return Result.failure(it) }
        val pkce = GooglePkceChallenge.create()
        val authCode = waitForAuthorizationCode(credentials.clientId, pkce).getOrElse { return Result.failure(it) }
        exchangeAuthorizationCode(credentials.clientId, authCode, pkce.verifier)
    }

    suspend fun refreshAccessToken(refreshToken: String): Result<GoogleOAuthTokens> {
        val credentials = GoogleCalendarConfig.credentialsOrError().getOrElse { return Result.failure(it) }
        return runCatching {
            val response = httpClient.submitForm(
                url = "https://oauth2.googleapis.com/token",
                formParameters = tokenFormParameters(credentials.clientId) {
                    append("refresh_token", refreshToken)
                    append("grant_type", "refresh_token")
                },
            )
            if (!response.status.isSuccess()) {
                error("Token refresh failed (${response.status.value}). Reconnect Google Calendar.")
            }
            val tokenResponse = response.body<GoogleTokenResponse>()
            if (tokenResponse.error != null) {
                error(tokenResponse.errorDescription ?: tokenResponse.error)
            }
            val existing = GoogleTokenStore.load()
            GoogleOAuthTokens(
                refreshToken = refreshToken,
                accessToken = tokenResponse.accessToken,
                accessTokenExpiresAtMillis = System.currentTimeMillis() + tokenResponse.expiresIn * 1000L,
                accountEmail = existing?.accountEmail.orEmpty(),
            )
        }
    }

    suspend fun ensureValidAccessToken(tokens: GoogleOAuthTokens): Result<GoogleOAuthTokens> {
        if (tokens.accessToken.isNotBlank() &&
            tokens.accessTokenExpiresAtMillis > System.currentTimeMillis() + 60_000L
        ) {
            return Result.success(tokens)
        }
        val refreshed = refreshAccessToken(tokens.refreshToken).getOrElse { return Result.failure(it) }
        val merged = refreshed.copy(accountEmail = tokens.accountEmail.ifBlank { refreshed.accountEmail })
        GoogleTokenStore.save(merged)
        return Result.success(merged)
    }

    private fun waitForAuthorizationCode(clientId: String, pkce: GooglePkceChallenge): Result<String> {
        val authCodeRef = AtomicReference<String?>(null)
        val errorRef = AtomicReference<String?>(null)
        val latch = CountDownLatch(1)
        var server: HttpServer? = null
        return runCatching {
            stopActiveOAuthServer()
            server = HttpServer.create(
                InetSocketAddress("127.0.0.1", GoogleCalendarConfig.OAUTH_REDIRECT_PORT),
                0,
            )
            activeOAuthServer = server
            server!!.createContext(GoogleCalendarConfig.oauthRedirectPath) { exchange ->
                if (authCodeRef.get() != null || errorRef.get() != null) {
                    exchange.sendResponseHeaders(409, -1)
                    exchange.close()
                    return@createContext
                }
                val query = exchange.requestURI.rawQuery.orEmpty()
                val params = parseQuery(query)
                val code = params["code"]
                val error = params["error"]
                if (!code.isNullOrBlank()) {
                    authCodeRef.set(code)
                    GoogleCalendarSyncDebug.log("oauth.callback.received", "codeLength=${code.length}")
                    val responseBytes = CALLBACK_HTML.toByteArray(StandardCharsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                    exchange.sendResponseHeaders(200, responseBytes.size.toLong())
                    exchange.responseBody.use { it.write(responseBytes) }
                } else {
                    errorRef.set(error ?: "Authorization was cancelled.")
                    val responseBytes = FAILURE_HTML.toByteArray(StandardCharsets.UTF_8)
                    exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                    exchange.sendResponseHeaders(400, responseBytes.size.toLong())
                    exchange.responseBody.use { it.write(responseBytes) }
                }
                latch.countDown()
            }
            server!!.start()
            openAuthorizationUrl(clientId, pkce)
            if (!latch.await(5, TimeUnit.MINUTES)) {
                error("Timed out waiting for Google authorization.")
            }
            errorRef.get()?.let { error(it) }
            authCodeRef.get()?.takeIf { it.isNotBlank() }
                ?: error("Google authorization did not return an authorization code.")
        }.also {
            stopActiveOAuthServer(server)
        }
    }

    private fun openAuthorizationUrl(clientId: String, pkce: GooglePkceChallenge) {
        val authUrl = buildString {
            append("https://accounts.google.com/o/oauth2/v2/auth?")
            append("client_id=").append(urlEncode(clientId))
            append("&redirect_uri=").append(urlEncode(GoogleCalendarConfig.redirectUri))
            append("&response_type=code")
            append("&scope=").append(urlEncode(GoogleCalendarConfig.CALENDAR_SCOPE))
            append("&access_type=offline")
            append("&prompt=consent")
            append("&code_challenge=").append(urlEncode(pkce.challenge))
            append("&code_challenge_method=S256")
        }
        GoogleCalendarSyncDebug.log("oauth.openBrowser", authUrl)
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(authUrl))
        } else {
            ProcessBuilder("open", authUrl).start()
        }
    }

    private fun exchangeAuthorizationCode(
        clientId: String,
        authCode: String,
        codeVerifier: String,
    ): Result<GoogleOAuthTokens> {
        return runCatching {
            val response = kotlinx.coroutines.runBlocking {
                httpClient.submitForm(
                    url = "https://oauth2.googleapis.com/token",
                    formParameters = tokenFormParameters(clientId) {
                        append("code", authCode)
                        append("redirect_uri", GoogleCalendarConfig.redirectUri)
                        append("grant_type", "authorization_code")
                        append("code_verifier", codeVerifier)
                    },
                )
            }
            if (!response.status.isSuccess()) {
                val body = kotlinx.coroutines.runBlocking { response.bodyAsText() }
                GoogleCalendarSyncDebug.log("oauth.tokenExchange.failed", "status=${response.status.value} body=$body")
                error(tokenExchangeErrorMessage(response.status.value, body))
            }
            val tokenResponse = kotlinx.coroutines.runBlocking { response.body<GoogleTokenResponse>() }
            if (tokenResponse.error != null) {
                error(tokenResponse.errorDescription ?: tokenResponse.error)
            }
            val refreshToken = tokenResponse.refreshToken
                ?: error("Google did not return a refresh token. Try disconnecting and connecting again.")
            val accountEmail = fetchAccountEmail(tokenResponse.accessToken)
            GoogleOAuthTokens(
                refreshToken = refreshToken,
                accessToken = tokenResponse.accessToken,
                accessTokenExpiresAtMillis = System.currentTimeMillis() + tokenResponse.expiresIn * 1000L,
                accountEmail = accountEmail,
            )
        }
    }

    private fun fetchAccountEmail(accessToken: String): String {
        return runCatching {
            val response = kotlinx.coroutines.runBlocking {
                httpClient.get("https://www.googleapis.com/oauth2/v2/userinfo") {
                    header("Authorization", "Bearer $accessToken")
                }
            }
            if (!response.status.isSuccess()) return ""
            kotlinx.coroutines.runBlocking { response.body<GoogleUserInfo>().email }
        }.getOrDefault("")
    }

    private fun parseQuery(rawQuery: String): Map<String, String> =
        rawQuery.split("&")
            .mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.size != 2) return@mapNotNull null
                pieces[0] to java.net.URLDecoder.decode(pieces[1], StandardCharsets.UTF_8)
            }
            .toMap()

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)

    private fun tokenFormParameters(
        clientId: String,
        block: ParametersBuilder.() -> Unit,
    ): Parameters = Parameters.build {
        append("client_id", clientId)
        GoogleCalendarConfig.clientSecret()?.let { append("client_secret", it) }
        block()
    }

    companion object {
        private val authorizeLock = Any()

        @Volatile
        private var activeOAuthServer: HttpServer? = null

        private fun stopActiveOAuthServer(server: HttpServer? = activeOAuthServer) {
            server?.stop(0)
            if (activeOAuthServer === server) {
                activeOAuthServer = null
            }
        }

        private fun tokenExchangeErrorMessage(statusCode: Int, body: String): String {
            val parsed = runCatching {
                defaultJson.decodeFromString<GoogleTokenResponse>(body)
            }.getOrNull()
            val detail = parsed?.errorDescription ?: parsed?.error ?: body.take(200)
            return when {
                detail.contains("redirect_uri_mismatch", ignoreCase = true) ->
                    "Google OAuth redirect URI mismatch. Add ${GoogleCalendarConfig.redirectUri} to your Desktop OAuth client."
                detail.contains("invalid_grant", ignoreCase = true) ->
                    "Google authorization expired or was already used. Quit Glide, click Connect once, and complete sign-in without refreshing the browser tab."
                detail.contains("client_secret is missing", ignoreCase = true) ->
                    "Google OAuth client secret is missing from Glide config. Copy the Client secret from " +
                        "Google Cloud → Credentials → your Desktop client and add clientSecret to " +
                        "google-oauth-client.properties, or use Download JSON as google-oauth-client.json."
                else -> "Google token exchange failed ($statusCode): $detail"
            }
        }

        private val defaultJson = Json { ignoreUnknownKeys = true; isLenient = true }

        private fun defaultHttpClient(): HttpClient =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(defaultJson)
                }
            }

        private val CALLBACK_HTML = """
            <!DOCTYPE html>
            <html><body style="font-family: sans-serif; padding: 2rem;">
            <h1>Sign-in received</h1>
            <p>Return to Glide — it is finishing the Google Calendar connection now.</p>
            <p>If Glide still shows Not connected, read the error message in the sync dialog.</p>
            </body></html>
        """.trimIndent()

        private val SUCCESS_HTML = """
            <!DOCTYPE html>
            <html><body style="font-family: sans-serif; padding: 2rem;">
            <h1>Google Calendar connected</h1>
            <p>You can close this tab and return to Glide.</p>
            </body></html>
        """.trimIndent()

        private val FAILURE_HTML = """
            <!DOCTYPE html>
            <html><body style="font-family: sans-serif; padding: 2rem;">
            <h1>Google Calendar connection failed</h1>
            <p>Return to Glide and try again.</p>
            </body></html>
        """.trimIndent()
    }
}
