package glide.backup

import java.io.File
import java.util.Base64
import java.util.UUID

/** Builds a MIME draft (.eml) with a file attachment for Mail and similar clients. */
internal object BackupEmlDraft {
    fun write(
        to: String,
        subject: String,
        plainBody: String,
        attachment: File,
        attachmentContentType: String,
    ): File {
        val mixedBoundary = "glide-mixed-${UUID.randomUUID()}"
        val attachmentBase64 = Base64.getMimeEncoder(76, "\r\n".toByteArray())
            .encodeToString(attachment.readBytes())

        val eml = buildString {
            appendLine("To: $to")
            appendLine("Subject: $subject")
            appendLine("MIME-Version: 1.0")
            appendLine("Content-Type: multipart/mixed; boundary=\"$mixedBoundary\"")
            appendLine()
            appendLine("--$mixedBoundary")
            appendLine("Content-Type: text/plain; charset=UTF-8")
            appendLine("Content-Transfer-Encoding: 8bit")
            appendLine()
            append(plainBody.toCrLf().trimEnd())
            appendLine()
            appendLine()
            appendLine("--$mixedBoundary")
            appendLine("Content-Type: $attachmentContentType; name=\"${attachment.name}\"")
            appendLine("Content-Disposition: attachment; filename=\"${attachment.name}\"")
            appendLine("Content-Transfer-Encoding: base64")
            appendLine()
            appendLine(attachmentBase64)
            appendLine()
            appendLine("--$mixedBoundary--")
        }

        val file = File.createTempFile("glide-backup-", ".eml")
        file.writeText(eml.toCrLf(), Charsets.UTF_8)
        file.deleteOnExit()
        return file
    }

    private fun String.toCrLf(): String = replace("\r\n", "\n").replace("\n", "\r\n")
}
