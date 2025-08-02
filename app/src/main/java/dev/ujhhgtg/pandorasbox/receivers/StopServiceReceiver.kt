package dev.ujhhgtg.pandorasbox.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.ujhhgtg.pandorasbox.services.AimBotService
import dev.ujhhgtg.pandorasbox.services.OverlayService
import java.security.InvalidParameterException

class StopServiceReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val service = intent.getIntExtra("service", -1)
        when (service) {
            0 -> ctx.stopService(Intent(ctx,
                    OverlayService::class.java))
            1 -> ctx.stopService(Intent(ctx,
                    AimBotService::class.java))
            else ->
                throw InvalidParameterException("invalid service id")
        }
    }
}
