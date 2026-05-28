import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import glide.data.AppSettingsStore
import glide.data.WindowBounds
import glide.data.persistence.GlideDataRepository
import glide.data.persistence.flushPendingSave
import glide.ui.App
import glide.ui.layout.GlideLayout
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlin.math.roundToInt

fun main() {
    System.setProperty("apple.awt.application.name", "Glide")

    application {
        AppSettingsStore.load()

        remember {
            GlideDataRepository.loadIntoStores()
            Unit
        }

        var closeRequested by remember { mutableStateOf(false) }
        val savedBounds = AppSettingsStore.windowBounds
        val minStartupWidthDp = 1400f
        val useDefaultWindowSize = savedBounds.widthDp == null ||
            savedBounds.widthDp < minStartupWidthDp
        val windowState = rememberWindowState(
            width = if (useDefaultWindowSize) {
                GlideLayout.DefaultWindowWidth
            } else {
                savedBounds.widthDp!!.dp
            },
            height = if (useDefaultWindowSize) {
                GlideLayout.DefaultWindowHeight
            } else {
                savedBounds.heightDp?.dp ?: GlideLayout.DefaultWindowHeight
            },
            position = if (savedBounds.positionX != null && savedBounds.positionY != null) {
                WindowPosition(savedBounds.positionX.dp, savedBounds.positionY.dp)
            } else {
                WindowPosition.PlatformDefault
            },
            placement = WindowPlacement.Floating,
        )

        @OptIn(FlowPreview::class)
        LaunchedEffect(windowState) {
            snapshotFlow {
                WindowBounds(
                    widthDp = windowState.size.width.value,
                    heightDp = windowState.size.height.value,
                    positionX = windowState.position.x.value.roundToInt(),
                    positionY = windowState.position.y.value.roundToInt(),
                )
            }
                .debounce(400)
                .collect { bounds ->
                    AppSettingsStore.saveWindowBounds(bounds)
                }
        }

        Window(
            onCloseRequest = { closeRequested = true },
            title = "Glide",
            state = windowState,
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
