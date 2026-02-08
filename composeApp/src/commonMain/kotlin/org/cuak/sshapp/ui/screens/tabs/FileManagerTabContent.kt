// composeApp/src/commonMain/kotlin/org/cuak/sshapp/ui/screens/tabs/FileManagerTabContent.kt
package org.cuak.sshapp.ui.screens.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
// Eliminamos imports de Koin/Voyager aquí porque ya no se usan
import org.cuak.sshapp.models.SftpFile
import org.cuak.sshapp.ui.screens.tabs.FileManagerViewModel

@Composable
fun FileManagerTabContent(viewModel: FileManagerViewModel) { // <--- CAMBIO: Recibe ViewModel

    // Consumimos el estado del ViewModel pasado por parámetro
    val localFiles by viewModel.localFiles.collectAsState()
    val localPath by viewModel.localPath.collectAsState()
    val remoteFiles by viewModel.remoteFiles.collectAsState()
    val remotePath by viewModel.remotePath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    Column(Modifier.fillMaxSize()) {

        if (isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (statusMessage.isNotEmpty()) {
            Text(
                text = statusMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Row(Modifier.weight(1f)) {
            // PANEL IZQUIERDO: LOCAL
            FilePanel(
                modifier = Modifier.weight(1f),
                title = "Local",
                path = localPath,
                files = localFiles,
                onUpClick = { viewModel.navigateLocalUp() },
                onItemClick = { if (it.isDirectory) viewModel.navigateLocal(it.path) },
                onTransferClick = { viewModel.upload(it) },
                transferIcon = Icons.AutoMirrored.Filled.ArrowForward
            )

            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // PANEL DERECHO: REMOTO
            FilePanel(
                modifier = Modifier.weight(1f),
                title = "Remoto",
                path = remotePath,
                files = remoteFiles,
                onUpClick = { viewModel.navigateRemoteUp() },
                onItemClick = {
                    if (it.isDirectory) {
                        val separator = if (remotePath.endsWith("/")) "" else "/"
                        viewModel.navigateRemote("$remotePath$separator${it.name}")
                    }
                },
                onTransferClick = { viewModel.download(it) },
                transferIcon = Icons.AutoMirrored.Filled.ArrowBack
            )
        }
    }
}

// ... (Las funciones FilePanel y FileItemRow se mantienen igual)
@Composable
private fun FilePanel(
    modifier: Modifier,
    title: String,
    path: String,
    files: List<SftpFile>,
    onUpClick: () -> Unit,
    onItemClick: (SftpFile) -> Unit,
    onTransferClick: (SftpFile) -> Unit,
    transferIcon: ImageVector
) {
    Column(modifier = modifier.padding(4.dp)) {
        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = path, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        IconButton(onClick = onUpClick, modifier = Modifier.size(32.dp).align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Subir nivel", modifier = Modifier.size(18.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(files) { file ->
                FileItemRow(file, { onItemClick(file) }, { onTransferClick(file) }, transferIcon)
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: SftpFile,
    onClick: () -> Unit,
    onTransfer: () -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            null,
            tint = if (file.isDirectory) MaterialTheme.colorScheme.secondary else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(file.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!file.isDirectory) {
                Text(file.sizeFormatted(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        if (!file.isDirectory) {
            IconButton(onClick = onTransfer, modifier = Modifier.size(24.dp)) {
                Icon(icon, "Transferir", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}