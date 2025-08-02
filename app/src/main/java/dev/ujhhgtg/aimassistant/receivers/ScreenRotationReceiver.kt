package dev.ujhhgtg.aimassistant.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.ujhhgtg.aimassistant.services.OverlayService

// restart overlay service if screen rotates
class ScreenRotationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_CONFIGURATION_CHANGED)
            return
        if (!OverlayService.Companion.isRunning.value)
            return

        val serviceIntent = Intent(context, OverlayService::class.java)
        context.stopService(serviceIntent)
        context.startService(serviceIntent)
    }
}