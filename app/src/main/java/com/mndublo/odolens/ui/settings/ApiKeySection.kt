package com.mndublo.odolens.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** "Gemini AI Config" card: API key input, paste button, guide link, trust info, and missing warning. */
@Composable
fun ApiKeySection(
    apiKeyInput: String,
    settingsLoaded: Boolean = true,
    onApiKeyChange: (String) -> Unit,
    focusRequester: FocusRequester? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showGuide by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(com.mndublo.odolens.R.string.settings_ai_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = { showGuide = !showGuide }) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "How to get a key",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            AnimatedVisibility(
                visible = showGuide,
                enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
                    expandVertically(animationSpec = tween(durationMillis = 250))
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "How to get a free Gemini API Key:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Sign in to Google AI Studio with your Google account.\n2. Tap 'Create API key'.\n3. Copy the key and paste it below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Google AI Studio")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var isKeyVisible by remember { mutableStateOf(false) }

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyChange,
                label = { Text(stringResource(com.mndublo.odolens.R.string.settings_ai_key_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (isKeyVisible) {
                    androidx.compose.ui.text.input.VisualTransformation.None
                } else {
                    androidx.compose.ui.text.input.PasswordVisualTransformation('*')
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isKeyVisible = !isKeyVisible }
                        ) {
                            Icon(
                                imageVector = if (isKeyVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (isKeyVisible) "Hide API Key" else "Show API Key",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()
                                if (clip != null) {
                                    onApiKeyChange(clip.text)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste from clipboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Warning if missing — animated in so the user notices it. Gated on the
            // settings having loaded so it doesn't flash on launch before the key arrives.
            AnimatedVisibility(
                visible = settingsLoaded && apiKeyInput.isBlank(),
                enter = fadeIn(animationSpec = tween(durationMillis = 250)) +
                    expandVertically(animationSpec = tween(durationMillis = 250))
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Without an API key, fuel economy readings will be inaccurate and parking scanning won't work.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Key is stored locally on device and used solely for OCR reading.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
