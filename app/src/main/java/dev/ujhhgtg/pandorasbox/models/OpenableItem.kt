package dev.ujhhgtg.pandorasbox.models

import java.io.File

sealed class OpenableItem {
    data class FileItem(val file: File) : OpenableItem()
    data class UrlItem(val url: String) : OpenableItem()
}
