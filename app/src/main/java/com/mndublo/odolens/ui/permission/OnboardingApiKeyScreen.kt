package com.mndublo.odolens.ui.permission

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Onboarding card explaining why Gemini API key is needed and guiding the user to create one.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingApiKeyScreen(
    onDone: (openSettings: Boolean) -> Unit
) {
    val context = LocalContext.current
    var openedBrowser by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Set Up Gemini AI Reading",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "OdoLens uses Google's free Gemini AI to read dashboard photos and parking tickets automatically.\n\nWithout an API key:\n• Parking ticket scanner won't function\n• Fuel economy readings will be inaccurate",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy & Trust Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Privacy",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Your key is stored only on your device and sent directly to Google Gemini. OdoLens never stores, logs, or misuses your key or photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (!openedBrowser) {
                Button(
                    onClick = {
                        openedBrowser = true
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MediumContainerHeight)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get Free Gemini API Key")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { onDone(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MediumContainerHeight)
                ) {
                    Text("I Already Have a Key → Go to Settings")
                }
            } else {
                Button(
                    onClick = { onDone(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MediumContainerHeight)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("I Have My Key → Add It Now")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ButtonDefaults.MediumContainerHeight)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-open API Key Page")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = { onDone(false) }
            ) {
                Text("Skip for Now", color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
