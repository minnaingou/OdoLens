package com.mndublo.odolens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mndublo.odolens.data.SettingsRepository
import com.mndublo.odolens.ui.main.MainScreen
import com.mndublo.odolens.ui.permission.NotificationPermissionScreen
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(
    openExtendSheet: Boolean = false,
    onExtendSheetOpened: () -> Unit = {},
    openParkingTab: Boolean = false,
    onParkingTabOpened: () -> Unit = {},
    autoScanTarget: String? = null,
    onAutoScanHandled: () -> Unit = {}
) {
  val context = LocalContext.current.applicationContext
  val settingsRepository = remember { SettingsRepository(context) }
  val promptDoneState by settingsRepository.notificationPromptDone.collectAsState(initial = null)
  val scope = rememberCoroutineScope()
  val promptDone = promptDoneState

  // Wait for the persisted flag before choosing the start screen, so the
  // permission screen never flashes on later launches.
  if (promptDone == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val needsPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    !promptDone &&
    ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.POST_NOTIFICATIONS
    ) != PackageManager.PERMISSION_GRANTED

  val backStack = rememberNavBackStack(if (needsPrompt) Permission else Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Permission> {
          NotificationPermissionScreen(
            onDone = {
              scope.launch { settingsRepository.setNotificationPromptDone(true) }
              backStack.removeLastOrNull()
              backStack.add(Main)
            },
            promptDone = promptDone
          )
        }
        entry<Main> {
          MainScreen(
            onItemClick = { navKey -> backStack.add(navKey) },
            modifier = Modifier,
            openExtendSheet = openExtendSheet,
            onExtendSheetOpened = onExtendSheetOpened,
            openParkingTab = openParkingTab,
            onParkingTabOpened = onParkingTabOpened,
            autoScanTarget = autoScanTarget,
            onAutoScanHandled = onAutoScanHandled
          )
        }
      },
  )
}
