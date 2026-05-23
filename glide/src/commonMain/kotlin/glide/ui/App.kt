package glide.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelsHost
import glide.ui.theme.GlideCanvasBackground
import glide.ui.theme.GlideTheme

@Composable
fun App() {
    GlideTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            GlideCanvasBackground()
            FloatingPanelsHost()
        }
    }
}
