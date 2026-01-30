package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.ui.screens.tabs.MonitorTabContent
import org.cuak.sshapp.ui.screens.tabs.ProcessesTabContent
import org.cuak.sshapp.ui.screens.tabs.TerminalTabContent

data class ServerDetailScreen(val serverId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ServerDetailViewModel>()

        LaunchedEffect(serverId) { viewModel.loadServer(serverId) }

        val server = viewModel.server
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Monitor", "Procesos", "Terminal")
        var showShutdownDialog by remember { mutableStateOf(false) }

        // Diálogo de Apagado
        if (showShutdownDialog) {
            AlertDialog(
                onDismissRequest = { showShutdownDialog = false },
                icon = { Icon(Icons.Default.Warning, null) },
                title = { Text("¿Apagar Servidor?") },
                text = { Text("Se ejecutará 'sudo poweroff'.") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.shutdownServer(); showShutdownDialog = false; navigator.pop() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Apagar") }
                },
                dismissButton = { TextButton(onClick = { showShutdownDialog = false }) { Text("Cancelar") } }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(server?.name ?: "Cargando...", style = MaterialTheme.typography.titleMedium)
                            server?.let { Text(it.ip, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) { Icon(Icons.Default.ArrowBack, "Volver") }
                    },
                    actions = {
                        IconButton(onClick = { showShutdownDialog = true }) {
                            Icon(Icons.Default.PowerSettingsNew, "Apagar", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = {
                            // Refrescar según la pestaña activa
                            if (selectedTabIndex == 1) viewModel.fetchProcesses() else viewModel.fetchMetrics()
                        }) {
                            Icon(Icons.Default.Refresh, "Refrescar")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Default.Speed, null)
                                    1 -> Icon(Icons.Default.Memory, null)
                                    2 -> Icon(Icons.Default.Terminal, null)
                                }
                            }
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTabIndex) {
                        0 -> {
                            LaunchedEffect(Unit) { viewModel.fetchMetrics() }
                            MonitorTabContent(viewModel.uiState) { viewModel.fetchMetrics() }
                        }
                        1 -> {
                            LaunchedEffect(Unit) {
                                if(viewModel.processes.isEmpty()) viewModel.fetchProcesses()
                            }
                            ProcessesTabContent(
                                processes = viewModel.processes,
                                isLoading = viewModel.isProcessesLoading,
                                sortOption = viewModel.processSortOption,
                                onSort = { viewModel.sortProcesses(it) },
                                onRefresh = { viewModel.fetchProcesses() }
                            )
                        }
                        2 -> TerminalTabContent(
                            output = viewModel.terminalOutput,
                            onSendInput = { viewModel.sendInput(it) },
                            onStart = { viewModel.startTerminal() }
                        )
                    }
                }
            }
        }
    }
}