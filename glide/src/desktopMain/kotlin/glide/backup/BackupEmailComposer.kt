package glide.backup

import java.awt.Desktop
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object BackupEmailComposer {
    private val osName = System.getProperty("os.name", "").lowercase()

    fun compose(recipient: String, zip: File): DataBackupResult {
        val subject = backupEmailSubject(zip)
        val plainBody = backupEmailPlainBody(zip)

        return when {
            osName.contains("mac") || osName.contains("darwin") ->
                composeOnMac(recipient, subject, plainBody, zip)
            osName.contains("win") ->
                composeOnWindows(recipient, subject, plainBody, zip)
            else ->
                composeOnLinux(recipient, subject, plainBody, zip)
        }
    }

    private fun composeOnMac(
        recipient: String,
        subject: String,
        plainBody: String,
        zip: File,
    ): DataBackupResult {
        if (openEmlDraft(recipient, subject, plainBody, zip)) {
            return DataBackupResult.Success
        }
        if (runMacMailScript(recipient, subject, plainBody, zip)) {
            return DataBackupResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, plainBody, zip)
    }

    private fun openEmlDraft(
        recipient: String,
        subject: String,
        plainBody: String,
        zip: File,
    ): Boolean = runCatching {
        val eml = BackupEmlDraft.write(
            to = recipient,
            subject = subject,
            plainBody = plainBody,
            attachment = zip,
            attachmentContentType = "application/zip",
        )
        runProcess(listOf("open", eml.absolutePath))
    }.getOrDefault(false)

    private fun runMacMailScript(
        recipient: String,
        subject: String,
        plainBody: String,
        zip: File,
    ): Boolean = runCatching {
        val plainFile = File.createTempFile("glide-backup-", ".txt")
        plainFile.writeText(plainBody, Charsets.UTF_8)
        plainFile.deleteOnExit()
        val plainPath = escapeAppleScript(plainFile.absolutePath)
        val zipPath = escapeAppleScript(zip.absolutePath)
        val script = """
            set plainText to read POSIX file "$plainPath" as «class utf8»
            tell application "Mail"
                activate
                set newMessage to make new outgoing message with properties {subject:"${escapeAppleScript(subject)}", content:plainText, visible:true}
                tell newMessage
                    make new to recipient at end of to recipients with properties {address:"${escapeAppleScript(recipient)}"}
                    make new attachment with properties {file name:(POSIX file "$zipPath")}
                end tell
            end tell
        """.trimIndent()
        runProcess(listOf("osascript", "-e", script))
    }.getOrDefault(false)

    private fun composeOnWindows(
        recipient: String,
        subject: String,
        plainBody: String,
        zip: File,
    ): DataBackupResult {
        if (runOutlookComposeScript(recipient, subject, plainBody, zip)) {
            return DataBackupResult.Success
        }
        return composeWithMailtoFallback(recipient, subject, plainBody, zip)
    }

    private fun runOutlookComposeScript(
        recipient: String,
        subject: String,
        plainBody: String,
        zip: File,
    ): Boolean = runCatching {
        val path = zip.absolutePath.replace("'", "''")
        val script = """
            ${'$'}outlook = New-Object -ComObject Outlook.Application
            ${'$'}mail = ${'$'}outlook.CreateItem(0)
            ${'$'}mail.To = '${recipient.replace("'", "''")}'
            ${'$'}mail.Subject = '${subject.replace("'", "''")}'
            ${'$'}mail.Body = '${plainBody.replace("'", "''")}'
            ${'$'}mail.Attachments.Add('${path}')
            ${'$'}mail.Display() | Out-Null
        """.trimIndent()
        runProcess(listOf("powershell", "-NoProfile", "-Command", script))
    }.getOrDefault(false)

    private fun composeOnLinux(
        recipient: String,
        subject: String,
        plainBody: String,
        zip: File,
    ): DataBackupResult {
        val sent = runProcess(
            listOf(
                "xdg-email",
                "--attach", zip.absolutePath,
                "--subject", subject,
                "--body", plainBody,
                recipient,
            ),
        )
        return if (sent) {
            DataBackupResult.Success
        } else {
            composeWithMailtoFallback(recipient, subject, plainBody, zip)
        }
    }

    private fun composeWithMailtoFallback(
        recipient: String,
        subject: String,
        plainBody: String,
        zip: File,
    ): DataBackupResult {
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
                return DataBackupResult.Failure("Could not open your email client.")
            }
        }

        revealInFileManager(zip)
        return DataBackupResult.Success
    }

    private fun openMailtoCommand(mailtoUri: String): List<String> =
        when {
            osName.contains("mac") || osName.contains("darwin") -> listOf("open", mailtoUri)
            osName.contains("win") -> listOf("cmd", "/c", "start", "", mailtoUri)
            else -> listOf("xdg-open", mailtoUri)
        }

    private fun revealInFileManager(file: File) {
        when {
            osName.contains("mac") || osName.contains("darwin") ->
                runProcess(listOf("open", "-R", file.absolutePath))
            osName.contains("win") ->
                runProcess(listOf("explorer.exe", "/select,", file.absolutePath))
            else ->
                runCatching {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file.parentFile)
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

    private fun backupEmailSubject(zip: File): String =
        "Glide data backup — ${zip.name}"

    private fun backupEmailPlainBody(zip: File): String = """
        Please find attached a backup of your Glide data.

        The zip archive (${zip.name}) contains:
        • application-support/ — database and app settings
        • documents/ — exported invoices and receipts

        Save this email or store the attachment somewhere safe.

        Generated by Glide on ${java.time.LocalDateTime.now()}.
    """.trimIndent()
}
