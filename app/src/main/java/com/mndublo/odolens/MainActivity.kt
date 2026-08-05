package com.mndublo.odolens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mndublo.odolens.data.AppLogger
import com.mndublo.odolens.notification.NotificationHelper
import com.mndublo.odolens.theme.TripAndTicketOCRTheme

class MainActivity : ComponentActivity() {

    // Signals to open the extend sheet, jump to parking tab, or open camera automatically
    private var openExtendSheet by mutableStateOf(false)
    private var openParkingTab by mutableStateOf(false)
    private var autoScanTarget by mutableStateOf<String?>(null) // "trips" or "parking"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLogger.init(applicationContext)
        AppLogger.log("Application started.")

        // Handle notification taps and app shortcuts when app was not running
        openExtendSheet = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_EXTEND_SHEET, false) == true
        openParkingTab = intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_PARKING_TAB, false) == true || openExtendSheet
        autoScanTarget = intent?.getStringExtra("scan_target")

        enableEdgeToEdge()
        val settingsRepository = com.mndublo.odolens.data.SettingsRepository(applicationContext)
        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = 0)
            val dynamicColor by settingsRepository.dynamicColor.collectAsState(initial = false)
            TripAndTicketOCRTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainNavigation(
                        openExtendSheet = openExtendSheet,
                        onExtendSheetOpened = { openExtendSheet = false },
                        openParkingTab = openParkingTab,
                        onParkingTabOpened = { openParkingTab = false },
                        autoScanTarget = autoScanTarget,
                        onAutoScanHandled = { autoScanTarget = null }
                    )
                }
            }
        }
    }

    // Handle notification tap / app shortcut when app is already running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_EXTEND_SHEET, false)) {
            openExtendSheet = true
            openParkingTab = true
        } else if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_PARKING_TAB, false)) {
            openParkingTab = true
        }
        intent.getStringExtra("scan_target")?.let { target ->
            autoScanTarget = target
        }
    }
}
