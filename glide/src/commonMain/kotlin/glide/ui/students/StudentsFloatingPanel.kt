package glide.ui.students

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import glide.ui.components.FloatingPanelShell
import glide.ui.layout.PanelSlots

@Composable
fun StudentsFloatingPanel(
    windowWidthPx: Int,
    windowHeightPx: Int,
) {
    FloatingPanelShell(
        title = "Students",
        slot = PanelSlots.STUDENTS,
        windowWidthPx = windowWidthPx,
        windowHeightPx = windowHeightPx,
    ) {
        StudentsPanel(modifier = Modifier.fillMaxSize())
    }
}
