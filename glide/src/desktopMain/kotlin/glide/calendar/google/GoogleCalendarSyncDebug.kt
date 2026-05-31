package glide.calendar.google

import glide.data.persistence.glideApplicationSupportDir
import java.io.File
import java.time.Instant

internal object GoogleCalendarSyncDebug {
    private const val TAG = "[Glide:GoogleCalendar]"
    private const val MAX_LOG_BYTES = 512 * 1024

    fun log(event: String, detail: String = "") {
        val line = buildString {
            append(TAG)
            append(' ')
            append(event)
            if (detail.isNotEmpty()) {
                append(" | ")
                append(detail)
            }
        }
        println(line)
        appendToLogFile(line)
    }

    private fun appendToLogFile(line: String) {
        runCatching {
            val file = File(glideApplicationSupportDir(), "google-calendar-sync.log")
            file.parentFile?.mkdirs()
            trimLogIfNeeded(file)
            file.appendText("${Instant.now()} $line\n")
        }
    }

    private fun trimLogIfNeeded(file: File) {
        if (!file.isFile || file.length() <= MAX_LOG_BYTES) return
        val lines = file.readLines()
        val trimmed = lines.takeLast(500).joinToString("\n") + "\n"
        file.writeText(trimmed)
    }
}
