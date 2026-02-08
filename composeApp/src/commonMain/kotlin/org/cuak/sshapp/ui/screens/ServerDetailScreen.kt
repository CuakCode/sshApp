package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.models.DeviceType
import org.cuak.sshapp.models.rtspUrl
import org.cuak.sshapp.ui.components.RtspVideoPlayer
import org.cuak.sshapp.ui.screens.tabs.FileManagerTabContent // <--- NUEVO IMPORT
import org.cuak.sshapp.ui.screens.tabs.FileManagerViewModel
import org.cuak.sshapp.ui.screens.tabs.MonitorTabContent
import org.cuak.sshapp.ui.screens.tabs.ProcessesTabContent
import org.cuak.sshapp.ui.screens.tabs.TerminalTabContent
import org.koin.core.parameter.parametersOf

data class ServerDetailScreen(val serverId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ServerDetailViewModel>()

        // Carga inicial del servidor
        LaunchedEffect(serverId) { viewModel.loadServer(serverId) }

        val server = viewModel.server

        // --- 1. LÓGICA DE PESTAÑAS DINÁMICAS ---
        val isCamera = server?.type == DeviceType.CAMERA

        // Si es cámara, insertamos "Cámara" al principio.
        // AÑADIDO: "Archivos" al final de la lista.
        val tabs = remember(isCamera) {
            if (isCamera) {
                listOf("Cámara", "Monitor", "Procesos", "Terminal", "Archivos")
            } else {
                listOf("Monitor", "Procesos", "Terminal", "Archivos")
            }
        }

        var selectedTabIndex by remember { mutableStateOf(0) }
        var showShutdownDialog by remember { mutableStateOf(false) }

        // Diálogo de confirmación de apagado
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
                        IconButton(onClick = { navigator.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") }
                    },
                    actions = {
                        IconButton(onClick = { showShutdownDialog = true }) {
                            Icon(Icons.Default.PowerSettingsNew, "Apagar", tint = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = {
                            // Refrescar según la pestaña activa
                            when (tabs.getOrNull(selectedTabIndex)) {
                                "Monitor" -> viewModel.fetchMetrics()
                                "Procesos" -> viewModel.fetchProcesses()
                                // FileManager tiene su propio botón de refresco interno (navegación),
                                // pero podrías conectarlo aquí si quisieras forzar recarga.
                                else -> { /* No requiere refresco manual global */ }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, "Refrescar")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {

                // Barra de Pestañas Scrollable para asegurar que caben todas
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 0.dp // Opcional: ajusta el padding inicial
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                            icon = {
                                val icon = when (title) {
                                    "Cámara" -> Icons.Default.Videocam
                                    "Monitor" -> Icons.Default.Speed
                                    "Procesos" -> Icons.Default.Memory
                                    "Terminal" -> Icons.Default.Terminal
                                    "Archivos" -> Icons.Default.Folder // <--- Icono para Archivos
                                    else -> Icons.Default.Circle
                                }
                                Icon(icon, null)
                            }
                        )
                    }
                }

                // Contenido de las Pestañas
                Box(modifier = Modifier.fillMaxSize()) {
                    val currentTabTitle = tabs.getOrNull(selectedTabIndex)

                    when (currentTabTitle) {
                        "Cámara" -> {
                            server?.let { srv ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RtspVideoPlayer(
                                        url = srv.rtspUrl,
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
                        "Terminal" -> {
                            TerminalTabContent(
                                output = viewModel.terminalOutput,
                                onSendInput = { viewModel.sendInput(it) },
                                onStart = { viewModel.startTerminal() }
                            )
                        }
                        "Archivos" -> { // <--- NUEVO BLOQUE DE CONTENIDO
                            // Necesitamos que server no sea null para iniciar el cliente SFTP
                            server?.let { srv ->
                                val fileManagerViewModel = koinScreenModel<FileManagerViewModel> { parametersOf(srv) }
                                FileManagerTabContent(viewModel = fileManagerViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}