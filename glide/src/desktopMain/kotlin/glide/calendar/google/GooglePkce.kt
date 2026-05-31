package glide.calendar.google

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal data class GooglePkceChallenge(
    val verifier: String,
    val challenge: String,
) {
    companion object {
        fun create(): GooglePkceChallenge {
            val verifier = generateVerifier()
            return GooglePkceChallenge(
                verifier = verifier,
                challenge = sha256Base64Url(verifier),
            )
        }

        private fun generateVerifier(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        private fun sha256Base64Url(value: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(StandardCharsets.US_ASCII))
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        }
    }
}
