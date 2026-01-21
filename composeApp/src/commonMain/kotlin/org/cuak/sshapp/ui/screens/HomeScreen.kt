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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.ui.components.ServerCard
import org.cuak.sshapp.ui.components.getIconByName

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        // Obtenemos el navegador de Voyager
        val navigator = LocalNavigator.currentOrThrow

        // Inyectamos el ViewModel
        val viewModel = koinScreenModel<HomeViewModel>()

        // Llamamos al contenido de la pantalla
        HomeScreenContent(
            viewModel = viewModel,
            onServerClick = { server ->
                // IMPLEMENTACIÓN ACTUALIZADA: Navegamos a la pantalla de detalles
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
    val servers by viewModel.servers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val selectedServer = viewModel.selectedServer

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mis Servidores",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Servidor")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(servers, key = { it.id }) { server ->
                ServerCard(
                    server = server,
                    onClick = { onServerClick(server) },
                    onLongClick = { viewModel.showServerOptions(server) }
                )
            }
        }

        // 1. Bottom Sheet de Opciones
        if (selectedServer != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissOptions() },
                sheetState = sheetState
            ) {
                ListItem(
                    headlineContent = { Text("Editar") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showEditDialog = true
                        // El bottom sheet se cierra automáticamente al abrir el diálogo
                    }
                )
                ListItem(
                    headlineContent = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        viewModel.deleteServer(selectedServer.id)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 2. Diálogo de Edición
        if (showEditDialog && selectedServer != null) {
            ServerFormDialog(
                serverToEdit = selectedServer,
                onDismiss = {
                    showEditDialog = false
                    viewModel.dismissOptions()
                },
                onConfirm = { name, ip, port, user, pass, key, icon ->
                    viewModel.updateServer(selectedServer.id, name, ip, port, user, pass, key, icon)
                    showEditDialog = false
                }
            )
        }

        // 3. Diálogo de Añadir (Nuevo)
        if (showAddDialog) {
            ServerFormDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, ip, port, user, pass, key, icon ->
                    viewModel.addServer(name, ip, port, user, pass, key, icon)
                    showAddDialog = false
                }
            )
        }
    }
}