package dev.ujhhgtg.pandorasbox.utils

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SnackbarUtils {
    fun SnackbarHostState.showShort(message: String, scope: CoroutineScope) {
        scope.launch {
            this@showShort.showSnackbar(
                message = message,
                actionLabel = null,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
        }
    }
}