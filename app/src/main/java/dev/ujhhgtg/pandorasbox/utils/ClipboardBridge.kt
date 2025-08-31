package dev.ujhhgtg.pandorasbox.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.utils.StringUtils.takeWithEllipsis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Suppress("unused")
class ClipboardBridge(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    @JavascriptInterface
    fun copyText(text: String) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = ctx.getString(R.string.website_wants_to_copy, text.replace("\n", " ").takeWithEllipsis(30)),
                actionLabel = ctx.getString(R.string.allow),
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )

            if (result == SnackbarResult.ActionPerformed) {
                val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Copied from WebView", text)
                clipboard.setPrimaryClip(clip)
            }
        }
    }
}
