package dev.ujhhgtg.aimassistant.utils

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.ujhhgtg.aimassistant.R
import rikka.shizuku.Shizuku

class PermissionManager {
    companion object {
        fun checkNotifications(ctx: Context): Boolean {
            return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
        }

        fun checkAndRequestNotifications(ctx: Activity): Boolean {
            if (!checkNotifications(ctx)) {
                Toast.makeText(ctx, ctx.getString(R.string.notification_perm_required), Toast.LENGTH_SHORT).show()
                ActivityCompat.requestPermissions(
                    ctx,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
                return false
            }

            return true
        }

        fun checkOverlay(ctx: Context): Boolean {
            return Settings.canDrawOverlays(ctx)
        }

        fun checkAndRequestOverlay(ctx: Activity): Boolean {
            if (!checkOverlay(ctx)) {
                Toast.makeText(ctx, ctx.getString(R.string.overlay_perm_required), Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${ctx.packageName}".toUri()
                )
                ctx.startActivity(intent)
                return false
            }

            return true
        }

        fun checkUsageStats(ctx: Context): Boolean {
            return (ctx.getSystemService(APP_OPS_SERVICE) as AppOpsManager).checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            ) != AppOpsManager.MODE_ALLOWED
        }

        fun checkAndRequestUsageStats(ctx: Activity): Boolean {
            if (!checkUsageStats(ctx)) {
                Toast.makeText(ctx, ctx.getString(R.string.usage_stats_perm_required), Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                ctx.startActivity(intent)
                return false
            }

            return true
        }

        fun checkCamera(ctx: Context): Boolean {
            return ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        }

        fun checkShizuku(): Boolean {
            return Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED
        }

        fun checkAndRequestShizuku(): Boolean {
            if (!checkShizuku()) {
                Shizuku.requestPermission(0)
                return false
            }

            return true
        }
    }
}