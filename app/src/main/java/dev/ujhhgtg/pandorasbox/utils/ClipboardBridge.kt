package dev.ujhhgtg.pandorasbox.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.JavascriptInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.ujhhgtg.pandorasbox.R
import dev.ujhhgtg.pandorasbox.utils.StringUtils.takeWithEllipsis


@Suppress("unused")
class ClipboardBridge(
    private val context: Context
) {
    @JavascriptInterface
    fun copyText(text: String) {
//        val themedContext = ContextThemeWrapper(context, R.style.Theme_PandorasBox)
        (context as Activity).runOnUiThread {
//            val builder = MaterialAlertDialogBuilder(context)
//            val dialogView = LayoutInflater.from(builder.context).inflate(R.layout.basic_dialog, null) as ViewGroup
//            dialogView.findViewById<TextView>(R.id.basic_dialog_title).setText(R.string.website_wants_to_copy)
//            dialogView.findViewById<TextView>(R.id.basic_dialog_msg).text =
//                text.replace("\n", " ").takeWithEllipsis(60)
//            builder
//                .setPositiveButton(R.string.yes) { _, _ ->
//                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//                    val clip = ClipData.newPlainText("Copied from WebView", text)
//                    clipboard.setPrimaryClip(clip)
//                }
//                .setNegativeButton(R.string.no) { _, _ -> }
//                .setCancelable(false)
//                .setView(dialogView)
//                .show()

            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.website_wants_to_copy))
                .setMessage(context.getString(R.string.website_wants_to_copy_xxx, text.replace("\n", " ").takeWithEllipsis(60)))
                .setPositiveButton(R.string.yes) { _, _ ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Copied from WebView", text)
                    clipboard.setPrimaryClip(clip)
                }
                .setNegativeButton(R.string.no) { _, _ -> }
                .setCancelable(false)
                .show()
        }
    }
}
