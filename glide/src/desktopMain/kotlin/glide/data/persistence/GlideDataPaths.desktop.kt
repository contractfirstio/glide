package glide.data.persistence

import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val saveScheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
    Thread(runnable, "glide-save").apply { isDaemon = true }
}

@Volatile
private var pendingSave: java.util.concurrent.ScheduledFuture<*>? = null

actual fun glideApplicationSupportDir(): String {
    val dir = File(System.getProperty("user.home"), "Library/Application Support/Glide")
    dir.mkdirs()
    return dir.absolutePath
}

/** User-visible exports (invoices, receipts) stay under Documents. */
actual fun glideDocumentsExportDir(): String {
    val dir = File(System.getProperty("user.home"), "Documents/Glide")
    dir.mkdirs()
    return dir.absolutePath
}

actual fun scheduleDebouncedSave(delayMs: Long, block: () -> Unit) {
    synchronized(saveScheduler) {
        pendingSave?.cancel(false)
        pendingSave = saveScheduler.schedule(block, delayMs, TimeUnit.MILLISECONDS)
    }
}

actual fun flushPendingSave() {
    synchronized(saveScheduler) {
        pendingSave?.let { future ->
            future.cancel(false)
            runCatching { future.get() }
        }
        pendingSave = null
    }
}

internal fun legacyDocumentsGlideDir(): File =
    File(System.getProperty("user.home"), "Documents/Glide")
