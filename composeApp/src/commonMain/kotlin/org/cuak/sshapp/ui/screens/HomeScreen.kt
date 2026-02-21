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
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.Camera
import org.cuak.sshapp.ui.components.ServerCard
import org.cuak.sshapp.ui.screens.viewModels.HomeViewModel

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<HomeViewModel>()

        HomeScreenContent(
            viewModel = viewModel,
            onDeviceClick = { device ->
                // Ambos tienen ID, así que la navegación se mantiene igual
                navigator.push(ServerDetailScreen(serverId = device.id))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    viewModel: HomeViewModel,
    onDeviceClick: (Device) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val selectedDevice = viewModel.selectedDevice

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Dispositivos") },
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
                Icon(Icons.Default.Add, contentDescription = "Añadir Dispositivo")
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.devices.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.devices.isEmpty() -> {
                    Text(
                        text = "No hay dispositivos configurados.\nPulsa + para añadir uno.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(state.devices, key = { it.id }) { device ->
                            // --- MAGIA DE LA INTERFAZ SEALED ---
                            // Te permite decidir cómo pintar el componente según su tipo real.
                            // NOTA: Asegúrate de que ServerCard esté modificado para aceptar 'Device'
                            // en lugar de 'Server', o crea un componente nuevo.
                            when (device) {
                                is Server -> {
                                    ServerCard(
                                        device = device, // Asumiendo que ServerCard acepta el tipo Server o Device
                                        onClick = { onDeviceClick(device) },
                                        onLongClick = { viewModel.showDeviceOptions(device) }
                                    )
                                }
                                is Camera -> {
                                    // Aquí podrías usar un CameraCard diferente en el futuro
                                    // que muestre un fotograma RTSP miniatura, por ejemplo.
                                    // Por ahora reutilizamos ServerCard (casteándolo si fuera necesario)
                                    ServerCard(
                                        device = device,
                                        onClick = { onDeviceClick(device) },
                                        onLongClick = { viewModel.showDeviceOptions(device) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheet de Opciones
        if (selectedDevice != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissOptions() },
                sheetState = sheetState
            ) {
                DeviceOptionsContent(
                    onEdit = { showEditDialog = true },
                    onDelete = { viewModel.deleteDevice(selectedDevice.id) }
                )
            }
        }

        // Diálogo de Edición
        if (showEditDialog && selectedDevice != null) {
            ServerFormDialog(
                device = selectedDevice,
                onDismiss = {
                    showEditDialog = false
                    viewModel.dismissOptions()
                },
                onConfirm = { updatedDevice ->
                    viewModel.updateDevice(updatedDevice)
                    showEditDialog = false
                }
            )
        }

        // Diálogo de Añadir
        if (showAddDialog) {
            ServerFormDialog(
                device = null,
                onDismiss = { showAddDialog = false },
                onConfirm = { newDevice ->
                    viewModel.addDevice(newDevice)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun DeviceOptionsContent(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        ListItem(
            headlineContent = { Text("Editar Dispositivo") },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
            modifier = Modifier.clickable { onEdit() }
        )
        ListItem(
            headlineContent = { Text("Eliminar Dispositivo", color = MaterialTheme.colorScheme.error) },
            leadingContent = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier.clickable { onDelete() }
        )
    }
}