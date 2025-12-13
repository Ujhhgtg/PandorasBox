package dev.ujhhgtg.pandorasbox.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.ujhhgtg.pandorasbox.services.AimBotService
import dev.ujhhgtg.pandorasbox.services.CrosshairOverlayService
import dev.ujhhgtg.pandorasbox.services.DlnaServerService
import dev.ujhhgtg.pandorasbox.services.DownloadService
import dev.ujhhgtg.pandorasbox.services.InputMapperService
import java.security.InvalidParameterException

class StopServiceReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val service = intent.getIntExtra("service", -1)
        ctx.stopService(
            Intent(
                ctx, when (service) {
                    CrosshairOverlayService.ID -> CrosshairOverlayService::class.java
                    AimBotService.ID -> AimBotService::class.java
                    InputMapperService.ID -> InputMapperService::class.java
                    DlnaServerService.ID -> DlnaServerService::class.java
                    DownloadService.ID -> DownloadService::class.java
                    else -> throw InvalidParameterException("invalid service id")
                }
            )
        )
    }
}
