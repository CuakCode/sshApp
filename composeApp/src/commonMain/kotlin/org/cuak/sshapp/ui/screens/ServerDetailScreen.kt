package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.ui.components.ResourceGauge

data class ServerDetailScreen(val serverId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ServerDetailViewModel>()

        // Cargar datos al iniciar la pantalla
        LaunchedEffect(serverId) {
            viewModel.loadServer(serverId)
        }

        val server = viewModel.server
        val state = viewModel.uiState

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(server?.name ?: "Detalles") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.fetchMetrics() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is DetailUiState.Idle -> Text("Listo para conectar...")

                    is DetailUiState.Loading -> CircularProgressIndicator()

                    is DetailUiState.Error -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error de conexión:\n${state.message}",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                            Button(onClick = { viewModel.fetchMetrics() }) {
                                Text("Reintentar")
                            }
                        }
                    }

                    is DetailUiState.Success -> {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                "Métricas en tiempo real",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ResourceGauge("CPU", state.metrics.cpuPercentage)
                                ResourceGauge("RAM", state.metrics.ramPercentage)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Mostramos el uso del disco raíz (primer elemento)
                                val disk = state.metrics.diskUsage.firstOrNull() ?: 0.0
                                ResourceGauge("Disco", disk)

                                // Placeholder para temperatura (si la tienes en el map)
                                val temp = state.metrics.temperatures.values.firstOrNull() ?: 0.0
                                ResourceGauge("Temp", temp)
                            }
                        }
                    }
                }
            }
        }
    }
}