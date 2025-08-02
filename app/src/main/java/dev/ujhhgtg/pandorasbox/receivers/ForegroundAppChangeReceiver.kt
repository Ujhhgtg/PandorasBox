package dev.ujhhgtg.pandorasbox.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ForegroundAppChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra("packageName")
        // TODO: Handle app change
    }
}