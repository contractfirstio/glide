package glide.data.persistence

internal expect fun readDataSnapshot(): GlideDataSnapshot?

internal expect fun writeDataSnapshot(snapshot: GlideDataSnapshot)
