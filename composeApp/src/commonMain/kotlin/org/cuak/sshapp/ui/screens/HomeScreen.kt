package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings 
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
import org.jetbrains.compose.resources.stringResource
import sshapp.composeapp.generated.resources.*

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<HomeViewModel>()

        HomeScreenContent(
            viewModel = viewModel,
            onDeviceClick = { device ->
                
                navigator.push(ServerDetailScreen(serverId = device.id))
            },
            onSettingsClick = {
                
                navigator.push(SettingsScreen())
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    viewModel: HomeViewModel,
    onDeviceClick: (Device) -> Unit,
    onSettingsClick: () -> Unit 
) {
    val state by viewModel.uiState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val selectedDevice = viewModel.selectedDevice

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(Res.string.home_settings_desc)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.home_add_device_desc))
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
                        text = stringResource(Res.string.home_empty_state),
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
                            when (device) {
                                is Server -> {
                                    ServerCard(
                                        device = device,
                                        onClick = { onDeviceClick(device) },
                                        onLongClick = { viewModel.showDeviceOptions(device) }
                                    )
                                }
                                is Camera -> {
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
            headlineContent = { Text(stringResource(Res.string.home_edit_device)) },
            leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
            modifier = Modifier.clickable { onEdit() }
        )
        ListItem(
            headlineContent = { Text(stringResource(Res.string.home_delete_device), color = MaterialTheme.colorScheme.error) },
            leadingContent = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            modifier = Modifier.clickable { onDelete() }
        )
    }
}