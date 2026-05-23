import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import glide.ui.App
import glide.ui.layout.GlideLayout

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Glide",
        state = rememberWindowState(size = GlideLayout.DefaultWindowSize),
    ) {
        App()
    }
}
