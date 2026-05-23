package glide.ui.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Tracks draft vs saved snapshot so create/save can stay disabled until the form is dirty. */
@Stable
class FormDirtyTracker<T>(initial: T) {
    var snapshot by mutableStateOf(initial)
        private set
    var draft by mutableStateOf(initial)

    val isDirty: Boolean
        get() = draft != snapshot

    fun load(value: T) {
        snapshot = value
        draft = value
    }

    fun commit() {
        snapshot = draft
    }
}

@Composable
fun <T> rememberFormDirtyTracker(initial: T): FormDirtyTracker<T> =
    remember { FormDirtyTracker(initial) }
