package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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

        // Inyectamos el ViewModel usando la extensión de Voyager para Koin
        // Esto asegura que el ViewModel esté ligado al ciclo de vida de esta Screen
        val viewModel = koinScreenModel<HomeViewModel>()

        // Llamamos al contenido de la pantalla
        HomeScreenContent(
            viewModel = viewModel,
            onServerClick = { server ->
                // Ejemplo de navegación a detalles (debes tener creada esta Screen)
                // navigator.push(ServerDetailScreen(serverId = server.id))
                println("Navegando a detalles de: ${server.name}")
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

        if (showAddDialog) {
            AddServerDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, ip, user, icon ->
                    viewModel.addServer(name, ip, user, icon)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, ip: String, user: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val iconOptions = listOf("dns", "storage", "computer", "router", "cloud")
    var selectedIcon by remember { mutableStateOf(iconOptions[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Servidor") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP o Dominio") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Usuario SSH") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedIcon.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Icono") },
                        leadingIcon = {
                            Icon(getIconByName(selectedIcon), contentDescription = null)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        iconOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedIcon = option
                                    expanded = false
                                },
                                leadingIcon = {
                                    Icon(getIconByName(option), contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, ip, user, selectedIcon) },
                enabled = name.isNotBlank() && ip.isNotBlank() && user.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}