package com.mndublo.odolens.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mndublo.odolens.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles notification action button taps:
 *   - ACTION_EXTEND_1H  → extend the active timer by 60 minutes
 *   - ACTION_CANCEL     → cancel the active timer entirely
 */
class ParkingExtendReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_EXTEND_1H = "com.mndublo.odolens.ACTION_EXTEND_1H"
        const val ACTION_CANCEL    = "com.mndublo.odolens.ACTION_CANCEL_PARKING"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val settingsRepository = SettingsRepository(context.applicationContext)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_EXTEND_1H -> {
                        ParkingTimerManager.extendTimer(
                            context = context.applicationContext,
                            settings = settingsRepository,
                            additionalMinutes = 60
                        )
                    }
                    ACTION_CANCEL -> {
                        ParkingTimerManager.cancelTimer(
                            context = context.applicationContext,
                            settings = settingsRepository
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
