package glide.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppViewMode {
    CUSTOMER_MANAGEMENT,
    SCHEDULING,
}

object AppViewState {
    var mode by mutableStateOf(AppViewMode.CUSTOMER_MANAGEMENT)
        private set

    fun switchTo(newMode: AppViewMode) {
        if (mode == newMode) return
        when (newMode) {
            AppViewMode.SCHEDULING -> BillingPanelState.close()
            AppViewMode.CUSTOMER_MANAGEMENT -> {
                if (BillingPanelState.peopleGroupId != null) {
                    BillingPanelState.reopenForCurrentGroup()
                }
            }
        }
        mode = newMode
    }
}
