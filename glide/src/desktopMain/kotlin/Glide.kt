import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

        var closeRequested by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = { closeRequested = true },
            title = "Glide",
            state = rememberWindowState(size = GlideLayout.DefaultWindowSize),
        ) {
            App(
                closeRequested = closeRequested,
                onCloseRequestHandled = { closeRequested = false },
                onExitApplication = {
                    flushPendingSave()
                    GlideDataRepository.saveNow()
                    exitApplication()
                },
            )
        }
    }
}
