package com.mndublo.odolens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.mndublo.odolens.data.SettingsRepository
import com.mndublo.odolens.ui.dashboard.AllTripsScreen
import com.mndublo.odolens.ui.main.MainScreen
import com.mndublo.odolens.ui.permission.NotificationPermissionScreen
import com.mndublo.odolens.ui.permission.OnboardingApiKeyScreen
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
  val apiKeySetting by settingsRepository.geminiApiKey.collectAsState(initial = null)
  val scope = rememberCoroutineScope()
  val promptDone = promptDoneState

  var openSettingsTabFromOnboarding by remember { mutableStateOf(false) }

  // Wait for the persisted flags before choosing the start screen, so the
  // permission/onboarding screen never flashes on later launches.
  if (promptDone == null || apiKeySetting == null) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val needsNotifPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
    !promptDone &&
    ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.POST_NOTIFICATIONS
    ) != PackageManager.PERMISSION_GRANTED

  val needsApiKeyPrompt = apiKeySetting.isNullOfBlank()

  val initialKey = when {
    needsNotifPrompt -> Permission
    needsApiKeyPrompt -> ApiKeySetup
    else -> Main
  }

  val backStack = rememberNavBackStack(initialKey)

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
              if (needsApiKeyPrompt) {
                backStack.add(ApiKeySetup)
              } else {
                backStack.add(Main)
              }
            },
            promptDone = promptDone
          )
        }
        entry<ApiKeySetup> {
          OnboardingApiKeyScreen(
            onDone = { openSettings ->
              if (openSettings) {
                openSettingsTabFromOnboarding = true
              }
              backStack.removeLastOrNull()
              backStack.add(Main)
            }
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
            openSettingsTab = openSettingsTabFromOnboarding,
            onSettingsTabOpened = { openSettingsTabFromOnboarding = false },
            autoScanTarget = autoScanTarget,
            onAutoScanHandled = onAutoScanHandled
          )
        }
        entry<AllTrips> {
          AllTripsScreen(
            onBack = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}

private fun String?.isNullOfBlank(): Boolean = this == null || this.isBlank()
