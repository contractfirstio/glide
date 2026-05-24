package glide.data.persistence

import java.io.File
import kotlinx.serialization.encodeToString

private fun dataDir(): File {
    val dir = File(glideApplicationSupportDir(), "data")
    dir.mkdirs()
    return dir
}

private fun databaseFile(): File = File(dataDir(), "glide-db.json")

private fun backupFile(): File = File(dataDir(), "glide-db.json.bak")

private fun tempFile(): File = File(dataDir(), ".glide-db.json.tmp")

actual fun readDataSnapshot(): GlideDataSnapshot? {
    val primary = databaseFile()
    val candidates = listOf(primary, backupFile()).filter { it.isFile }
    for (file in candidates) {
        val parsed = runCatching {
            glideJson.decodeFromString<GlideDataSnapshot>(file.readText())
        }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

actual fun writeDataSnapshot(snapshot: GlideDataSnapshot) {
    val target = databaseFile()
    val tmp = tempFile()
    val json = glideJson.encodeToString(snapshot)
    tmp.writeText(json)
    if (target.isFile) {
        runCatching { target.copyTo(backupFile(), overwrite = true) }
    }
    if (!tmp.renameTo(target)) {
        target.writeText(json)
        tmp.delete()
    }
}
