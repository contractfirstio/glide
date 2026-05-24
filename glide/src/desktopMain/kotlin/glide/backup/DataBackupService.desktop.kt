package glide.backup

import glide.data.AppSettingsStore
import glide.data.persistence.GlideDataRepository
import glide.data.persistence.flushPendingSave
import glide.data.persistence.glideApplicationSupportDir
import glide.data.persistence.glideDocumentsExportDir
import glide.data.persistence.legacyDocumentsGlideDir
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual object DataBackupService {
    actual fun backupAndEmail(): DataBackupResult {
        if (!AppSettingsStore.isConfigured) {
            return DataBackupResult.Failure("Configure company settings before sending a backup.")
        }
        val recipient = AppSettingsStore.companyEmail.trim()
        if (!recipient.contains('@')) {
            return DataBackupResult.Failure("Company email address is not valid.")
        }

        flushPendingSave()
        GlideDataRepository.saveNow()

        val zip = runCatching { createBackupZip() }.getOrElse {
            return DataBackupResult.Failure("Could not create backup archive.")
        }

        return BackupEmailComposer.compose(recipient, zip)
    }

    private fun createBackupZip(): File {
        val backupsDir = File(glideDocumentsExportDir(), "backups").apply { mkdirs() }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"))
        val zipFile = File(backupsDir, "glide-backup-$timestamp.zip")
        val tempZip = File.createTempFile("glide-backup-", ".zip")

        ZipOutputStream(tempZip.outputStream()).use { zos ->
            zipDirectory(
                zos = zos,
                sourceDir = File(glideApplicationSupportDir()),
                zipPrefix = "application-support",
            )
            zipDirectory(
                zos = zos,
                sourceDir = File(glideDocumentsExportDir()),
                zipPrefix = "documents",
                excludeTopLevelNames = setOf("backups"),
            )
            val legacy = legacyDocumentsGlideDir()
            if (legacy.exists() && legacy.absolutePath != File(glideDocumentsExportDir()).absolutePath) {
                zipDirectory(zos = zos, sourceDir = legacy, zipPrefix = "documents-legacy")
            }
        }

        tempZip.copyTo(zipFile, overwrite = true)
        tempZip.delete()
        return zipFile
    }

    private fun zipDirectory(
        zos: ZipOutputStream,
        sourceDir: File,
        zipPrefix: String,
        excludeTopLevelNames: Set<String> = emptySet(),
    ) {
        if (!sourceDir.exists()) return
        sourceDir.walkTopDown()
            .onEnter { dir ->
                dir == sourceDir || dir.name !in excludeTopLevelNames
            }
            .forEach { path ->
                val relative = path.relativeTo(sourceDir).path.replace('\\', '/')
                val entryName = if (relative.isEmpty()) {
                    "$zipPrefix/"
                } else {
                    "$zipPrefix/$relative"
                }
                if (path.isDirectory) {
                    val dirEntry = if (entryName.endsWith("/")) entryName else "$entryName/"
                    zos.putNextEntry(ZipEntry(dirEntry))
                    zos.closeEntry()
                } else {
                    zos.putNextEntry(ZipEntry(entryName))
                    path.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
            }
    }
}
