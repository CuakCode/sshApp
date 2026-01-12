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
                onConfirm = { name, ip, port, user, pass, key, icon ->
                    viewModel.addServer(name, ip, port, user, pass, key, icon)
                    showAddDialog = false
                }
            )
        }
    }
}
