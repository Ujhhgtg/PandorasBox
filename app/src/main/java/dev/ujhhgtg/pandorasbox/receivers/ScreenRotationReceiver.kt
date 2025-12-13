package dev.ujhhgtg.pandorasbox.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.ujhhgtg.pandorasbox.services.CrosshairOverlayService

// restart overlay service if screen rotates
class ScreenRotationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_CONFIGURATION_CHANGED)
            return
        if (!CrosshairOverlayService.isRunning.value)
            return

        val serviceIntent = Intent(context, CrosshairOverlayService::class.java)
        context.stopService(serviceIntent)
        context.startService(serviceIntent)
    }
}