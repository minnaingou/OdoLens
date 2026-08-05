package com.mndublo.odolens.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val viewModel: SettingsViewModel = viewModel(
        factory = remember(context) { SettingsViewModel.factory(context.applicationContext) }
    )
    val uiState by viewModel.uiState.collectAsState()

    // One-shot feedback: confirmation toast after saving
    LaunchedEffect(uiState.saveFeedback) {
        if (uiState.saveFeedback) {
            Toast.makeText(context, context.getString(com.mndublo.odolens.R.string.settings_saved_toast), Toast.LENGTH_SHORT).show()
            viewModel.consumeSaveFeedback()
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Hero Banner
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(com.mndublo.odolens.R.string.settings_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(com.mndublo.odolens.R.string.settings_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Section 1: AI Settings
                item {
                    ApiKeySection(
                        apiKeyInput = uiState.apiKeyInput,
                        onApiKeyChange = viewModel::onApiKeyChange
                    )
                }

                // Section 2: Time Format
                item {
                    FormatSection(
                        use12hFormat = uiState.use12hFormat,
                        onUse12hChange = viewModel::onUse12hChange
                    )
                }

                // Section 3: App Theme
                item {
                    ThemeSection(
                        themeMode = uiState.themeMode,
                        onThemeModeChange = viewModel::onThemeModeChange,
                        dynamicColor = uiState.dynamicColor,
                        onDynamicColorChange = viewModel::onDynamicColorChange
                    )
                }

                // Save Action
                item {
                    Button(
                        onClick = { viewModel.saveAll() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(com.mndublo.odolens.R.string.settings_save_button))
                    }
                }

                // Section 4: Debug Logs (Debug Menu)
                item {
                    DebugSection()
                }
            }
        }
    }
}
