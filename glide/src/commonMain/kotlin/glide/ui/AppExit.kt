package glide.ui

import glide.backup.DataBackupResult
import glide.backup.DataBackupService
import glide.data.AppSettingsStore

fun runEmailDataBackup(onError: (String) -> Unit): Boolean =
    when (val result = DataBackupService.backupAndEmail()) {
        is DataBackupResult.Success -> true
        is DataBackupResult.Failure -> {
            onError(result.message)
            false
        }
    }

fun handleAppCloseRequest(
    onExitApplication: () -> Unit,
    onShowCloseBackupOffer: () -> Unit,
) {
    if (!AppSettingsStore.hasCompletedFirstSession) {
        AppSettingsStore.markFirstSessionCompleted()
        onExitApplication()
        return
    }
    if (AppSettingsStore.shouldOfferBackupPrompt()) {
        onShowCloseBackupOffer()
    } else {
        onExitApplication()
    }
}
