package glide.data.persistence

internal expect fun glideApplicationSupportDir(): String

internal expect fun glideDocumentsExportDir(): String

internal expect fun scheduleDebouncedSave(delayMs: Long, block: () -> Unit)

internal expect fun flushPendingSave()
