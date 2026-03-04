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
                    title = { Text("Configuración") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                    label = { Text("Días de almacenamiento de métricas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                
                OutlinedTextField(
                    value = settings.pingTimeoutMs.toString(),
                    onValueChange = { it.toIntOrNull()?.let { ms -> viewModel.updatePingTimeout(ms) } },
                    label = { Text("Timeout de ping (ms)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tema global", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = settings.isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme(it) },
                        thumbContent = if (settings.isDarkTheme) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.DarkMode,
                                    contentDescription = "Modo Oscuro",
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Filled.LightMode,
                                    contentDescription = "Modo Claro",
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        }
                    )
                }

                
                OutlinedTextField(
                    value = settings.language,
                    onValueChange = { },
                    label = { Text("Idioma") },
                    enabled = false, 
                    modifier = Modifier.fillMaxWidth()
                )

                
                OutlinedTextField(
                    value = settings.databasePath,
                    onValueChange = { viewModel.updateDatabasePath(it) },
                    label = { Text("Ruta de la base de datos (.db)") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Requiere reiniciar la app para aplicar los cambios.") }
                )
            }
        }
    }
}