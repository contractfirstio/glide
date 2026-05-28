package glide.billing

import java.io.File
import java.util.Base64
import java.util.UUID

/** Builds a MIME draft (.eml) so Mail and other clients render HTML instead of showing raw tags. */
internal object InvoiceEmlDraft {
    fun write(
        to: String,
        from: String,
        subject: String,
        plainBody: String,
        htmlBody: String,
        pdf: File,
    ): File {
        val mixedBoundary = "glide-mixed-${UUID.randomUUID()}"
        val altBoundary = "glide-alt-${UUID.randomUUID()}"
        val pdfBase64 = Base64.getMimeEncoder(76, "\r\n".toByteArray())
            .encodeToString(pdf.readBytes())

        val eml = buildString {
            appendHeader("To", to)
            appendHeader("From", from)
            appendHeader("Subject", subject)
            appendLine("MIME-Version: 1.0")
            appendLine("Content-Type: multipart/mixed; boundary=\"$mixedBoundary\"")
            appendLine()
            appendLine("--$mixedBoundary")
            appendLine("Content-Type: multipart/alternative; boundary=\"$altBoundary\"")
            appendLine()
            appendAlternativePart(altBoundary, "text/plain; charset=UTF-8", plainBody)
            appendAlternativePart(altBoundary, "text/html; charset=UTF-8", htmlBody)
            appendLine("--$altBoundary--")
            appendLine()
            appendLine("--$mixedBoundary")
            appendLine("Content-Type: application/pdf; name=\"${pdf.name}\"")
            appendLine("Content-Disposition: attachment; filename=\"${pdf.name}\"")
            appendLine("Content-Transfer-Encoding: base64")
            appendLine()
            appendLine(pdfBase64)
            appendLine()
            appendLine("--$mixedBoundary--")
        }

        val file = File.createTempFile("glide-invoice-", ".eml")
        file.writeText(eml.toCrLf(), Charsets.UTF_8)
        file.deleteOnExit()
        return file
    }

    private fun StringBuilder.appendHeader(name: String, value: String) {
        appendLine("$name: $value")
    }

    private fun StringBuilder.appendAlternativePart(
        boundary: String,
        contentType: String,
        body: String,
    ) {
        appendLine("--$boundary")
        appendLine("Content-Type: $contentType")
        appendLine("Content-Transfer-Encoding: 8bit")
        appendLine()
        append(body.toCrLf().trimEnd())
        appendLine()
    }

    private fun String.toCrLf(): String = replace("\r\n", "\n").replace("\n", "\r\n")
}
