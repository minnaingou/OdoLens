package com.mndublo.odolens.ui.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * First-launch screen explaining why notification access is needed for parking
 * reminders, with a button that triggers the system default permission request.
 *
 * Skips itself (calls [onDone]) when the prompt was already completed
 * ([promptDone]), notifications don't apply (Android 12 and below), or the
 * permission is already granted — so it only ever shows once.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationPermissionScreen(
    onDone: () -> Unit,
    promptDone: Boolean = false
) {
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onDone()
            } else {
                denied = true
            }
        }
    )

    // Auto-skip: prompt already completed, on Android 12 and below
    // POST_NOTIFICATIONS doesn't apply, or the permission is already granted.
    LaunchedEffect(Unit) {
        val shouldPrompt = !promptDone &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (!shouldPrompt) {
            onDone()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Parking reminders need notifications",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "OdoLens sends you an alert before your free parking time expires, " +
                    "so you can move your car on time — even when the app is closed. " +
                    "Notification access is required for these reminders.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (denied) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Parking notifications won't be available",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You can still check the countdown timer in the app itself " +
                                "to keep track of your parking time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDone,
                    // M3 Expressive Medium tier (56dp) for the primary hero action.
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MediumContainerHeight)
                ) {
                    Text("Continue")
                }
            } else {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MediumContainerHeight)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Enable Notifications")
                }
            }
        }
    }
}
