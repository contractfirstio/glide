import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import glide.data.AppSettingsStore
import glide.data.SampleData
import glide.ui.App
import glide.ui.layout.GlideLayout

fun main() = application {
    AppSettingsStore.load()
    SampleData.loadIfEmpty()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Glide",
        state = rememberWindowState(size = GlideLayout.DefaultWindowSize),
    ) {
        App()
    }
}
