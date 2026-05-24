import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import glide.data.AppSettingsStore
import glide.ui.App
import glide.ui.layout.GlideLayout

fun main() {
    System.setProperty("apple.awt.application.name", "Glide")

    application {
        AppSettingsStore.load()

        Window(
            onCloseRequest = ::exitApplication,
            title = "Glide",
            state = rememberWindowState(size = GlideLayout.DefaultWindowSize),
        ) {
            App()
        }
    }
}
