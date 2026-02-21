package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.Camera
import org.cuak.sshapp.ui.components.RtspVideoPlayer
import org.cuak.sshapp.ui.screens.tabs.FileManagerTabContent
import org.cuak.sshapp.ui.screens.viewModels.FileManagerViewModel
import org.cuak.sshapp.ui.screens.tabs.MonitorTabContent
import org.cuak.sshapp.ui.screens.tabs.ProcessesTabContent
import org.cuak.sshapp.ui.screens.tabs.TerminalTabContent
import org.cuak.sshapp.ui.screens.viewModels.ServerDetailViewModel
import org.koin.core.parameter.parametersOf

data class ServerDetailScreen(val serverId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ServerDetailViewModel>()

        LaunchedEffect(serverId) { viewModel.loadServer(serverId) }

        val device = viewModel.device

        // --- 1. LÓGICA DE PESTAÑAS DINÁMICAS ---
        // Comprobamos directamente si la clase de 'device' es una 'Camera'
        val isCamera = device is Camera

        val tabs = remember(isCamera) {
            if (isCamera) {
                listOf("Cámara", "Monitor", "Procesos", "Terminal", "Archivos")
            } else {
                listOf("Monitor", "Procesos", "Terminal", "Archivos")
            }
        }

        var selectedTabIndex by remember { mutableStateOf(0) }
        var showShutdownDialog by remember { mutableStateOf(false) }

        if (showShutdownDialog) {
            AlertDialog(
                onDismissRequest = { showShutdownDialog = false },
                icon = { Icon(Icons.Default.Warning, null) },
                title = { Text("¿Apagar Dispositivo?") },
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
                            Text(device?.name ?: "Cargando...", style = MaterialTheme.typography.titleMedium)
                            device?.let { Text(it.ip, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                    },
                    actions = {
                        IconButton(onClick = { showShutdownDialog = true }) {
                            Icon(Icons.Default.PowerSettingsNew, "Apagar", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = {
                            when (tabs.getOrNull(selectedTabIndex)) {
                                "Monitor" -> viewModel.fetchMetrics()
                                "Procesos" -> viewModel.fetchProcesses()
                                else -> { }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, "Refrescar")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                if (tabs.isNotEmpty()) {
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                icon = {
                                    val icon = when (title) {
                                        "Cámara" -> Icons.Default.Videocam
                                        "Monitor" -> Icons.Default.Speed
                                        "Procesos" -> Icons.Default.Memory
                                        "Terminal" -> Icons.Default.Terminal
                                        "Archivos" -> Icons.Default.Folder
                                        else -> Icons.Default.Circle
                                    }
                                    Icon(icon, null)
                                }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val currentTabTitle = tabs.getOrNull(selectedTabIndex)

                    when (currentTabTitle) {
                        "Cámara" -> {
                            // Smart cast maravilloso de Kotlin: como ya sabemos que debe ser una Camera, lo casteamos.
                            // Si device es nulo o no es Camera, simplemente no pinta el reproductor.
                            (device as? Camera)?.let { cam ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RtspVideoPlayer(
                                        url = cam.rtspUrl, // Acceso directo a rtspUrl gracias al cast
                                        modifier = Modifier.fillMaxSize(),
                                        onStatusChange = { status ->
                                            println("Estado cámara: $status")
                                        }
                                    )
                                }
                            }
                        }
                        "Monitor" -> {
                            LaunchedEffect(Unit) { viewModel.fetchMetrics() }
                            MonitorTabContent(viewModel.uiState) { viewModel.fetchMetrics() }
                        }
                        "Procesos" -> {
                            LaunchedEffect(Unit) {
                                if (viewModel.processes.isEmpty()) viewModel.fetchProcesses()
                            }
                            ProcessesTabContent(
                                processes = viewModel.processes,
                                isLoading = viewModel.isProcessesLoading,
                                sortOption = viewModel.processSortOption,
                                onSort = { viewModel.sortProcesses(it) },
                                onRefresh = { viewModel.fetchProcesses() }
                            )
                        }
                        "Terminal" -> {
                            TerminalTabContent(
                                output = viewModel.terminalOutput,
                                onSendInput = { viewModel.sendInput(it) },
                                onStart = { viewModel.startTerminal() }
                            )
                        }
                        "Archivos" -> {
                            if (device != null) {
                                // NOTA: Asegúrate de que FileManagerViewModel acepta 'Device' en parametersOf
                                val fileManagerViewModel = koinScreenModel<FileManagerViewModel> { parametersOf(device) }
                                FileManagerTabContent(viewModel = fileManagerViewModel)
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}