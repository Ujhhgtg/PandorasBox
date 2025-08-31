package dev.ujhhgtg.pandorasbox.models

import android.webkit.WebView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import java.util.UUID

const val DEFAULT_HOME_URL: String = "https://www.duckduckgo.com"
const val DEFAULT_SEARCH_ENGINE: String = "https://duckduckgo.com/?q=%s"

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: MutableState<String> = mutableStateOf("New tab"),
    val url: MutableState<String> = mutableStateOf(DEFAULT_HOME_URL),
    val webView: WebView,
    val loadingProgress: MutableState<Float> = mutableFloatStateOf(0f)
)
