package glide.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import glide.data.AppViewMode
import glide.data.AppViewState
import glide.ui.layout.PanelSlots
import glide.ui.layout.SchedulingPanelSlots

object GlideAccents {
    val Leads = Color(0xFF8CB4DC)
    val Plans = Color(0xFF9EB8C8)
    val Students = Color(0xFF9AAFD4)
    val Clients = Color(0xFF8EC4D4)
    val SoldPlans = Color(0xFFA8C0D0)
    val Billing = Color(0xFFB8C8A0)
    val Schedule = Color(0xFF9BC4B8)
    val Terms = Color(0xFFB0A8D4)
    val SchedulingSoldPlans = Color(0xFFA8C0D0)
    val Locations = Color(0xFFC4B89C)
    val TermCalendar = Color(0xFFD4A8B8)
    val Attendance = Color(0xFFB8D4C8)

    /** Distinct accent for the active workspace view (chrome strip, label, canvas tint). */
    val CustomerManagementView = Color(0xFF8CB4DC)
    val SchedulingView = Color(0xFF9BC4B8)

    fun forViewMode(mode: AppViewMode): Color = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> CustomerManagementView
        AppViewMode.SCHEDULING -> SchedulingView
    }

    fun viewModeLabel(mode: AppViewMode, compact: Boolean = false): String = when (mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> if (compact) "Customers" else "Customer Management"
        AppViewMode.SCHEDULING -> if (compact) "Scheduling" else "Scheduling"
    }

    fun forPanel(slot: Int): Color = when (AppViewState.mode) {
        AppViewMode.CUSTOMER_MANAGEMENT -> when (slot) {
            PanelSlots.LEADS -> Leads
            PanelSlots.PLANS -> Plans
            PanelSlots.STUDENTS -> Students
            PanelSlots.CLIENTS -> Clients
            PanelSlots.SOLD_PLANS -> SoldPlans
            PanelSlots.BILLING -> Billing
            else -> Leads
        }
        AppViewMode.SCHEDULING -> when (slot) {
            SchedulingPanelSlots.SCHEDULE -> Schedule
            SchedulingPanelSlots.TERMS -> Terms
            SchedulingPanelSlots.SOLD_PLANS -> SchedulingSoldPlans
            SchedulingPanelSlots.LOCATIONS -> Locations
            SchedulingPanelSlots.CALENDAR -> TermCalendar
            SchedulingPanelSlots.ATTENDANCE -> Attendance
            else -> Schedule
        }
    }
}

@Composable
fun GlideCanvasBackground(
    mode: AppViewMode = AppViewState.mode,
    modifier: Modifier = Modifier,
) {
    val accent = GlideAccents.forViewMode(mode)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF151A22),
                        Color(0xFF12161C),
                        Color(0xFF101418),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.32f),
                            Color.Transparent,
                        ),
                        center = Offset(0.12f, 0.1f),
                        radius = 900f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = if (mode == AppViewMode.SCHEDULING) 0.22f else 0.12f),
                            Color.Transparent,
                        ),
                        center = Offset(if (mode == AppViewMode.SCHEDULING) 0.15f else 0.88f, 0.75f),
                        radius = 700f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                        center = Offset(0.5f, 0.4f),
                        radius = 600f,
                    ),
                ),
        )
    }
}
