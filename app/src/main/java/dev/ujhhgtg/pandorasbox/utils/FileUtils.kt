package dev.ujhhgtg.pandorasbox.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import java.io.File

object FileUtils {
    fun File.isChildOf(parent: File): Boolean {
        val parentPath = parent.canonicalFile.toPath()
        val childPath = this.canonicalFile.toPath()
        return childPath.startsWith(parentPath)
    }

    fun getInternalStorageInfo(): Triple<Long, Long, Long> {
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)

        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes

        return Triple(usedBytes, freeBytes, totalBytes)
    }

    fun formatBytes(context: Context, bytes: Long): String {
        return Formatter.formatFileSize(context, bytes)
    }
}