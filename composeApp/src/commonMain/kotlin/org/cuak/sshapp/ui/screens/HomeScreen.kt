package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cuak.sshapp.ui.components.ServerCard
import org.cuak.sshapp.models.Server

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onServerClick: (Server) -> Unit,
    onAddServer: () -> Unit
) {
    val servers by viewModel.servers.collectAsState()

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
            FloatingActionButton(onClick = onAddServer) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Servidor")
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 180.dp),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.padding(padding)
        ) {
            items(servers) { server ->
                ServerCard(
                    server = server,
                    onClick = { onServerClick(server) },
                    onLongClick = { viewModel.showServerOptions(server) } // Implementar diálogo de borrar/editar
                )
            }
        }
    }
}
@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, ip: String, user: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir Servidor") },
        text = {
            Column {
                TextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                TextField(value = ip, onValueChange = { ip = it }, label = { Text("IP") })
                TextField(value = user, onValueChange = { user = it }, label = { Text("Usuario SSH") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, ip, user) }) { Text("Guardar") }
        }
    )
}