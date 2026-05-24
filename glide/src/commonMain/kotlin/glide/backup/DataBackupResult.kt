package glide.backup

sealed class DataBackupResult {
    data object Success : DataBackupResult()

    data class Failure(val message: String) : DataBackupResult()
}
