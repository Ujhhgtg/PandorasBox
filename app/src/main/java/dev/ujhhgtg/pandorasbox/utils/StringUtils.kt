package dev.ujhhgtg.pandorasbox.utils

object StringUtils {
    fun String.takeWithEllipsis(n: Int): String {
        require(n >= 0) { "Requested character count $n is less than zero." }
        if (this.length <= n) {
            return this
        }

        return this.take(n) + "…"
    }
}