package glide.billing

import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object InvoiceEmailComposer {
    private val osName = System.getProperty("os.name", "").lowercase()

    fun compose(content: InvoiceContent, pdf: File): InvoiceExportResult {
        val recipient = content.billToEmail.trim()
        if (recipient.isBlank()) {
            return InvoiceExportResult.Failure("Main contact has no email address.")
        }
        if (!recipient.contains('@')) {
            return InvoiceExportResult.Failure("Main contact email address is not valid.")
        }

        val subject = invoiceEmailSubject(content)
        val body = content.formatInvoiceEmailBody()

        return when {
            osName.contains("mac") || osName.contains("darwin") ->
                composeOnMac(recipient, subject, body, pdf)
            osName.contains("win") ->
                composeOnWindows(recipient, subject, body, pdf)
            else ->
                composeOnLinux(recipient, subject, body, pdf)
        }
    }

    private fun composeOnMac(
        recipient: String,
        subject: String,
        body: String,
        pdf: File,
    ): InvoiceExportResult {
        if (runMacMailScript(recipient, subject, body, pdf)) {
            return InvoiceExportResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, body, pdf, revealInFinder = true)
    }

    private fun composeOnWindows(
        recipient: String,
        subject: String,
        body: String,
        pdf: File,
    ): InvoiceExportResult {
        if (runOutlookComposeScript(recipient, subject, body, pdf)) {
            return InvoiceExportResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, body, pdf, revealInFinder = true)
    }

    private fun composeOnLinux(
        recipient: String,
        subject: String,
        body: String,
        pdf: File,
    ): InvoiceExportResult {
        if (runXdgEmail(recipient, subject, body, pdf)) {
            return InvoiceExportResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, body, pdf, revealInFinder = false)
    }

    private fun runMacMailScript(
        recipient: String,
        subject: String,
        body: String,
        pdf: File,
    ): Boolean {
        val script = """
            tell application "Mail"
                activate
                set newMessage to make new outgoing message with properties {subject:"${escapeAppleScript(subject)}", visible:true, content:"${escapeAppleScript(body)}"}
                tell newMessage
                    make new to recipient at end of to recipients with properties {address:"${escapeAppleScript(recipient)}"}
                    make new attachment with properties {file name:(POSIX file "${escapeAppleScript(pdf.absolutePath)}")}
                end tell
            end tell
        """.trimIndent()
        return runProcess(listOf("osascript", "-e", script))
    }

    private fun runOutlookComposeScript(
        recipient: String,
        subject: String,
        body: String,
        pdf: File,
    ): Boolean {
        val path = pdf.absolutePath.replace("'", "''")
        val script = """
            ${'$'}outlook = New-Object -ComObject Outlook.Application
            ${'$'}mail = ${'$'}outlook.CreateItem(0)
            ${'$'}mail.To = '${recipient.replace("'", "''")}'
            ${'$'}mail.Subject = '${subject.replace("'", "''")}'
            ${'$'}mail.Body = @'
$body
'@
            ${'$'}mail.Attachments.Add('${path}')
            ${'$'}mail.Display() | Out-Null
        """.trimIndent()
        return runProcess(listOf("powershell", "-NoProfile", "-Command", script))
    }

    private fun runXdgEmail(
        recipient: String,
        subject: String,
        body: String,
        pdf: File,
    ): Boolean {
        val xdgEmail = listOf("xdg-email", "--attach", pdf.absolutePath, "--subject", subject, "--body", body, recipient)
        if (runProcess(xdgEmail)) return true
        val mailtoUri = buildMailtoUri(recipient, subject, body)
        return runProcess(listOf("xdg-email", "--attach", pdf.absolutePath, mailtoUri))
    }

    private fun composeWithMailtoFallback(
        recipient: String,
        subject: String,
        body: String,
        pdf: File,
        revealInFinder: Boolean,
    ): InvoiceExportResult {
        val mailtoUri = buildMailtoUri(recipient, subject, body)
        val openedMail = runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().mail(URI(mailtoUri))
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (!openedMail) {
            runCatching {
                val process = ProcessBuilder(openMailtoCommand(mailtoUri)).start()
                process.waitFor() == 0
            }.getOrDefault(false).let { launched ->
                if (!launched) {
                    return InvoiceExportResult.Failure("Could not open your email client.")
                }
            }
        }

        if (revealInFinder) {
            revealPdfInFileManager(pdf)
        } else {
            runCatching {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdf.parentFile)
                }
            }
        }

        return InvoiceExportResult.Success
    }

    private fun openMailtoCommand(mailtoUri: String): List<String> =
        when {
            osName.contains("mac") || osName.contains("darwin") -> listOf("open", mailtoUri)
            osName.contains("win") -> listOf("cmd", "/c", "start", "", mailtoUri)
            else -> listOf("xdg-open", mailtoUri)
        }

    private fun revealPdfInFileManager(pdf: File) {
        when {
            osName.contains("mac") || osName.contains("darwin") ->
                runProcess(listOf("open", "-R", pdf.absolutePath))
            osName.contains("win") ->
                runProcess(listOf("explorer.exe", "/select,", pdf.absolutePath))
            else ->
                runCatching {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(pdf.parentFile)
                    }
                }
        }
    }

    private fun runProcess(command: List<String>): Boolean =
        runCatching {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.waitFor() == 0
        }.getOrDefault(false)

    private fun buildMailtoUri(recipient: String, subject: String, body: String): String {
        val params = buildList {
            add("subject=${encodeMailtoParam(subject)}")
            add("body=${encodeMailtoParam(body)}")
        }
        return "mailto:$recipient?${params.joinToString("&")}"
    }

    private fun encodeMailtoParam(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private fun escapeAppleScript(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun invoiceEmailSubject(content: InvoiceContent): String =
        "Invoice ${content.invoiceNumber} from ${content.fromName}"
}
