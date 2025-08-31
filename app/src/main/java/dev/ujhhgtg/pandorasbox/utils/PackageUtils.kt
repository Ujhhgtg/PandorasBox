package dev.ujhhgtg.pandorasbox.utils

import android.content.Context
import android.util.Log

private const val TAG: String = "PB.PackageUtils"

object PackageUtils {
    fun getAppName(context: Context, packageName: String?): String? {
        if (packageName == null) {
            return null
        }

        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            Log.e(TAG, "exception while getting app name of $packageName", e)
            null
        }
    }
}