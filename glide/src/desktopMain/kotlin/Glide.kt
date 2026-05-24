import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import glide.data.AppSettingsStore
import glide.data.persistence.GlideDataRepository
import glide.data.persistence.flushPendingSave
import glide.ui.App
import glide.ui.layout.GlideLayout

fun main() {
    System.setProperty("apple.awt.application.name", "Glide")

    application {
        AppSettingsStore.load()
        GlideDataRepository.loadIntoStores()

        Window(
            onCloseRequest = {
                flushPendingSave()
                GlideDataRepository.saveNow()
                exitApplication()
            },
            title = "Glide",
            state = rememberWindowState(size = GlideLayout.DefaultWindowSize),
        ) {
            App()
        }
    }
}
