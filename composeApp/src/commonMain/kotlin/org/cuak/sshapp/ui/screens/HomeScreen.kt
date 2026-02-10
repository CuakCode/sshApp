package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.ui.components.ServerCard
import org.cuak.sshapp.ui.screens.viewModels.HomeViewModel

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<HomeViewModel>()

        HomeScreenContent(
            viewModel = viewModel,
            onServerClick = { server ->
                navigator.push(ServerDetailScreen(serverId = server.id))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    viewModel: HomeViewModel,
    onServerClick: (Server) -> Unit
) {
    // 1. CAMBIO: Observamos el estado completo (Carga + Datos)
    val state by viewModel.uiState.collectAsState()

    // Estado local para diálogos
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val selectedServer = viewModel.selectedServer

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Servidores") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Servidor")
            }
        }
    ) { padding ->

        // 2. MEJORA: Gestión de estados de UI (Cargando vs Vacío vs Contenido)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.servers.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.servers.isEmpty() -> {
                    Text(
                        text = "No hay servidores configurados.\nPulsa + para añadir uno.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp), // Ajustado para mejor visualización
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.servers, key = { it.id }) { server ->
                            ServerCard(
                                server = server,
                                // El estado (Online/Offline) ya viene inyectado en server.status desde el ViewModel
                                onClick = { onServerClick(server) },
                                onLongClick = { viewModel.showServerOptions(server) }
                            )
                        }
                    }
                }
            }
        }

        // --- Gestión de Diálogos y BottomSheets ---

        // 3. Bottom Sheet de Opciones
        if (selectedServer != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissOptions() },
                sheetState = sheetState
            ) {
                ServerOptionsContent(
                    onEdit = { showEditDialog = true }, // El diálogo se superpondrá
                    onDelete = { viewModel.deleteServer(selectedServer.id) }
                )
            }
        }

        // 4. Diálogo de Edición
        if (showEditDialog && selectedServer != null) {
            ServerFormDialog(
                server = selectedServer,
                onDismiss = {
                    showEditDialog = false
                    viewModel.dismissOptions()
                },
                onConfirm = { updatedServer ->
                    // Pasamos el objeto completo al ViewModel
                    viewModel.updateServer(updatedServer)
                    showEditDialog = false
                }
            )
        }

        // 5. Diálogo de Añadir
        if (showAddDialog) {
            ServerFormDialog(
                server = null, // Es nuevo, así que null
                onDismiss = { showAddDialog = false },
                onConfirm = { newServer ->
                    // Pasamos el objeto completo al ViewModel
                    viewModel.addServer(newServer)
                    showAddDialog = false
                }
            )
        }
    }
}

/**
 * Componente extraído para limpiar el código principal.
 * Muestra las opciones disponibles para un servidor.
 */
@Composable
private fun ServerOptionsContent(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        ListItem(
            headlineContent = { Text("Editar Servidor") },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
            modifier = Modifier.clickable { onEdit() }
        )
        ListItem(
            headlineContent = { Text("Eliminar Servidor", color = MaterialTheme.colorScheme.error) },
            leadingContent = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier.clickable { onDelete() }
        )
    }
}