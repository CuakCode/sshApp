package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.ui.screens.viewModels.SettingsViewModel

// IMPORTS de Material3 para Dropdown y recursos (i18n)
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import org.jetbrains.compose.resources.stringResource
import sshapp.composeapp.generated.resources.*

class SettingsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<SettingsViewModel>()
        val settings by viewModel.settings.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(Res.string.settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.settings_back_desc))
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                OutlinedTextField(
                    value = settings.metricsRetentionDays.toString(),
                    onValueChange = { it.toIntOrNull()?.let { days -> viewModel.updateRetentionDays(days) } },
                    label = { Text(stringResource(Res.string.settings_metrics_retention)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = settings.pingTimeoutMs.toString(),
                    onValueChange = { it.toIntOrNull()?.let { ms -> viewModel.updatePingTimeout(ms) } },
                    label = { Text(stringResource(Res.string.settings_ping_timeout)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(Res.string.settings_global_theme), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme(it) },
                        thumbContent = if (settings.isDarkTheme) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.DarkMode,
                                    contentDescription = stringResource(Res.string.settings_dark_mode_desc),
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Filled.LightMode,
                                    contentDescription = stringResource(Res.string.settings_light_mode_desc),
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        }
                    )
                }

                var languageExpanded by remember { mutableStateOf(false) }

                val currentLangLabel = when (settings.language) {
                    "es" -> "Español"
                    "en" -> "English"
                    else -> "Sistema" // Por defecto (system)
                }

                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded }
                ) {
                    OutlinedTextField(
                        value = currentLangLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.settings_language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        modifier = Modifier.menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        ).fillMaxWidth(),
                        supportingText = { Text(stringResource(Res.string.settings_db_path_support)) }
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sistema") },
                            onClick = {
                                viewModel.updateLanguage("system")
                                languageExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Español") },
                            onClick = {
                                viewModel.updateLanguage("es")
                                languageExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = {
                                viewModel.updateLanguage("en")
                                languageExpanded = false
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = settings.databasePath,
                    onValueChange = { viewModel.updateDatabasePath(it) },
                    label = { Text(stringResource(Res.string.settings_db_path)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(Res.string.settings_db_path_support)) }
                )
            }
        }
    }
}