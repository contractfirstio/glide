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
            return InvoiceExportResult.Failure("Main client has no email address.")
        }
        if (!recipient.contains('@')) {
            return InvoiceExportResult.Failure("Main client email address is not valid.")
        }

        val subject = invoiceEmailSubject(content)
        val htmlBody = content.formatInvoiceEmailHtmlBody()
        val plainBody = content.formatInvoiceEmailPlainTextBody()

        return when {
            osName.contains("mac") || osName.contains("darwin") ->
                composeOnMac(recipient, subject, htmlBody, plainBody, pdf)
            osName.contains("win") ->
                composeOnWindows(recipient, subject, htmlBody, plainBody, pdf)
            else ->
                composeOnLinux(recipient, subject, htmlBody, plainBody, pdf)
        }
    }

    private fun composeOnMac(
        recipient: String,
        subject: String,
        htmlBody: String,
        plainBody: String,
        pdf: File,
    ): InvoiceExportResult {
        if (openEmlDraftInMail(recipient, subject, htmlBody, plainBody, pdf)) {
            return InvoiceExportResult.Success
        }
        if (runMacMailScript(recipient, subject, htmlBody, plainBody, pdf)) {
            return InvoiceExportResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, plainBody, pdf, revealInFinder = true)
    }

    private fun openEmlDraftInMail(
        recipient: String,
        subject: String,
        htmlBody: String,
        plainBody: String,
        pdf: File,
    ): Boolean = runCatching {
        val eml = InvoiceEmlDraft.write(
            to = recipient,
            subject = subject,
            plainBody = plainBody,
            htmlBody = htmlBody,
            pdf = pdf,
        )
        runProcess(listOf("open", eml.absolutePath))
    }.getOrDefault(false)

    private fun composeOnWindows(
        recipient: String,
        subject: String,
        htmlBody: String,
        plainBody: String,
        pdf: File,
    ): InvoiceExportResult {
        if (runOutlookComposeScript(recipient, subject, htmlBody, plainBody, pdf)) {
            return InvoiceExportResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, plainBody, pdf, revealInFinder = true)
    }

    private fun composeOnLinux(
        recipient: String,
        subject: String,
        htmlBody: String,
        plainBody: String,
        pdf: File,
    ): InvoiceExportResult {
        if (runXdgEmail(recipient, subject, htmlBody, plainBody, pdf)) {
            return InvoiceExportResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, plainBody, pdf, revealInFinder = false)
    }

    private fun runMacMailScript(
        recipient: String,
        subject: String,
        htmlBody: String,
        plainBody: String,
        pdf: File,
    ): Boolean = withTempEmailFiles(htmlBody, plainBody) { htmlFile, plainFile ->
        val htmlPath = escapeAppleScript(htmlFile.absolutePath)
        val plainPath = escapeAppleScript(plainFile.absolutePath)
        val script = """
            set htmlText to read POSIX file "$htmlPath" as «class utf8»
            set plainText to read POSIX file "$plainPath" as «class utf8»
            tell application "Mail"
                activate
                set newMessage to make new outgoing message with properties {subject:"${escapeAppleScript(subject)}", content:plainText, visible:true}
                tell newMessage
                    make new to recipient at end of to recipients with properties {address:"${escapeAppleScript(recipient)}"}
                    make new attachment with properties {file name:(POSIX file "${escapeAppleScript(pdf.absolutePath)}")}
                end tell
                if htmlText is not "" then
                    delay 0.3
                    try
                        tell newMessage to set html content to htmlText
                    end try
                end if
            end tell
        """.trimIndent()
        runProcess(listOf("osascript", "-e", script))
    }

    private fun runOutlookComposeScript(
        recipient: String,
        subject: String,
        htmlBody: String,
        plainBody: String,
        pdf: File,
    ): Boolean = withTempEmailFiles(htmlBody, plainBody) { htmlFile, plainFile ->
        val htmlPath = htmlFile.absolutePath.replace("'", "''")
        val plainPath = plainFile.absolutePath.replace("'", "''")
        val path = pdf.absolutePath.replace("'", "''")
        val script = """
            ${'$'}html = Get-Content -LiteralPath '${htmlPath}' -Raw -Encoding UTF8
            ${'$'}plain = Get-Content -LiteralPath '${plainPath}' -Raw -Encoding UTF8
            ${'$'}outlook = New-Object -ComObject Outlook.Application
            ${'$'}mail = ${'$'}outlook.CreateItem(0)
            ${'$'}mail.To = '${recipient.replace("'", "''")}'
            ${'$'}mail.Subject = '${subject.replace("'", "''")}'
            ${'$'}mail.BodyFormat = 2
            if (-not [string]::IsNullOrWhiteSpace(${'$'}html)) {
                ${'$'}mail.HTMLBody = ${'$'}html
            } else {
                ${'$'}mail.Body = ${'$'}plain
            }
            ${'$'}mail.Attachments.Add('${path}')
            ${'$'}mail.Display() | Out-Null
        """.trimIndent()
        runProcess(listOf("powershell", "-NoProfile", "-Command", script))
    }

    private fun runXdgEmail(
        recipient: String,
        subject: String,
        htmlBody: String,
        plainBody: String,
        pdf: File,
    ): Boolean = withTempEmailFiles(htmlBody, plainBody) { htmlFile, _ ->
        val htmlAttempt = listOf(
            "xdg-email",
            "--attach", pdf.absolutePath,
            "--subject", subject,
            "--body", htmlFile.absolutePath,
            "--content-type", "text/html",
            recipient,
        )
        if (runProcess(htmlAttempt)) return@withTempEmailFiles true

        val plainAttempt = listOf(
            "xdg-email",
            "--attach", pdf.absolutePath,
            "--subject", subject,
            "--body", plainBody,
            recipient,
        )
        runProcess(plainAttempt)
    }

    private inline fun <T> withTempEmailFiles(
        htmlBody: String,
        plainBody: String,
        block: (htmlFile: File, plainFile: File) -> T,
    ): T {
        val htmlFile = File.createTempFile("glide-invoice-", ".html")
        val plainFile = File.createTempFile("glide-invoice-", ".txt")
        htmlFile.writeText(htmlBody, Charsets.UTF_8)
        plainFile.writeText(plainBody, Charsets.UTF_8)
        htmlFile.deleteOnExit()
        plainFile.deleteOnExit()
        return block(htmlFile, plainFile)
    }

    private fun composeWithMailtoFallback(
        recipient: String,
        subject: String,
        plainBody: String,
        pdf: File,
        revealInFinder: Boolean,
    ): InvoiceExportResult {
        val mailtoUri = buildMailtoUri(recipient, subject, plainBody)
        val openedMail = runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().mail(URI(mailtoUri))
                true
            } else {
                false
            }
        }.getOrDefault(false)

        if (!openedMail) {
            val launched = runCatching {
                val process = ProcessBuilder(openMailtoCommand(mailtoUri)).start()
                process.waitFor() == 0
            }.getOrDefault(false)
            if (!launched) {
                return InvoiceExportResult.Failure("Could not open your email client.")
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
