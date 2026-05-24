package glide.backup

expect object DataBackupService {
    /** Zips Glide data and opens a draft email to the company address with the archive attached. */
    fun backupAndEmail(): DataBackupResult
}
