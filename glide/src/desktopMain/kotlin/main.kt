import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import glide.startup.StartupGreeting
import glide.ui.App
import glide.ui.layout.GlideLayout

fun main() = application {
    StartupGreeting.playOnLaunch()

    Window(
        onCloseRequest = ::exitApplication,
        title = "Glide",
        state = rememberWindowState(size = GlideLayout.DefaultWindowSize),
    ) {
        App()
    }
}
