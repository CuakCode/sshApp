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
import androidx.compose.ui.graphics.vector.ImageVector
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
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import sshapp.composeapp.generated.resources.*

enum class ServerTab(val titleRes: StringResource, val icon: ImageVector) {
    CAMERA(Res.string.server_detail_tab_camera, Icons.Default.Videocam),
    MONITOR(Res.string.server_detail_tab_monitor, Icons.Default.Speed),
    PROCESSES(Res.string.server_detail_tab_processes, Icons.Default.Memory),
    TERMINAL(Res.string.server_detail_tab_terminal, Icons.Default.Terminal),
    FILES(Res.string.server_detail_tab_files, Icons.Default.Folder)
}

data class ServerDetailScreen(val serverId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ServerDetailViewModel>()

        LaunchedEffect(serverId) { viewModel.loadServer(serverId) }

        val device = viewModel.device
        val isCamera = device is Camera

        val tabs = remember(isCamera) {
            if (isCamera) {
                listOf(ServerTab.CAMERA, ServerTab.MONITOR, ServerTab.PROCESSES, ServerTab.TERMINAL, ServerTab.FILES)
            } else {
                listOf(ServerTab.MONITOR, ServerTab.PROCESSES, ServerTab.TERMINAL, ServerTab.FILES)
            }
        }

        var selectedTabIndex by remember { mutableStateOf(0) }
        var showShutdownDialog by remember { mutableStateOf(false) }

        if (showShutdownDialog) {
            AlertDialog(
                onDismissRequest = { showShutdownDialog = false },
                icon = { Icon(Icons.Default.Warning, null) },
                title = { Text(stringResource(Res.string.server_detail_shutdown_dialog_title)) },
                text = { Text(stringResource(Res.string.server_detail_shutdown_dialog_text)) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.shutdownServer(); showShutdownDialog = false; navigator.pop() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(stringResource(Res.string.server_detail_shutdown_dialog_confirm)) }
                },
                dismissButton = {
                    TextButton(onClick = { showShutdownDialog = false }) {
                        Text(stringResource(Res.string.server_detail_shutdown_dialog_cancel))
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = device?.name ?: stringResource(Res.string.server_detail_loading),
                                style = MaterialTheme.typography.titleMedium
                            )
                            device?.let { Text(it.ip, style = MaterialTheme.typography.bodySmall) }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.server_detail_back_desc))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showShutdownDialog = true }) {
                            Icon(
                                Icons.Default.PowerSettingsNew,
                                contentDescription = stringResource(Res.string.server_detail_shutdown_desc),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = {
                            // --- 4. LÓGICA DE REFRESCO SEGURA POR TIPO DE ENUM ---
                            when (tabs.getOrNull(selectedTabIndex)) {
                                ServerTab.MONITOR -> viewModel.fetchMetrics()
                                ServerTab.PROCESSES -> viewModel.fetchProcesses()
                                else -> { }
                            }
                        }) {
                            Icon(Icons.Default.Refresh, stringResource(Res.string.server_detail_refresh_desc))
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
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(stringResource(tab.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                icon = { Icon(tab.icon, contentDescription = null) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    val currentTab = tabs.getOrNull(selectedTabIndex)

                    when (currentTab) {
                        ServerTab.CAMERA -> {
                            (device as? Camera)?.let { cam ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    RtspVideoPlayer(
                                        url = cam.rtspUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        onStatusChange = { status ->
                                            println("Estado cámara: $status")
                                        }
                                    )
                                }
                            }
                        }
                        ServerTab.MONITOR -> {
                            LaunchedEffect(Unit) { viewModel.fetchMetrics() }
                            MonitorTabContent(viewModel.uiState) { viewModel.fetchMetrics() }
                        }
                        ServerTab.PROCESSES -> {
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
                        ServerTab.TERMINAL -> {
                            TerminalTabContent(
                                output = viewModel.terminalOutput,
                                onSendInput = { viewModel.sendInput(it) },
                                onStart = { viewModel.startTerminal() }
                            )
                        }
                        ServerTab.FILES -> {
                            if (device != null) {
                                val fileManagerViewModel = koinScreenModel<FileManagerViewModel> { parametersOf(device) }
                                FileManagerTabContent(viewModel = fileManagerViewModel)
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        null -> {}
                    }
                }
            }
        }
    }
}