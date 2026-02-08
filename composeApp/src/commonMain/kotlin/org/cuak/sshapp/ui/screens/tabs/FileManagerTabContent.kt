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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import org.cuak.sshapp.models.SftpFile
import org.cuak.sshapp.ui.screens.tabs.FileManagerViewModel
import org.cuak.sshapp.ui.screens.tabs.SortDirection
import org.cuak.sshapp.ui.screens.tabs.SortOption

@Composable
fun FileManagerTabContent(viewModel: FileManagerViewModel) {

    val localFiles by viewModel.localFiles.collectAsState()
    val localPath by viewModel.localPath.collectAsState()
    val remoteFiles by viewModel.remoteFiles.collectAsState()
    val remotePath by viewModel.remotePath.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()

    Column(Modifier.fillMaxSize()) {

        // --- Barra de Ordenación ---
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ordenar:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 8.dp))
            SortChip("Nombre", SortOption.NAME, viewModel)
            SortChip("Tam.", SortOption.SIZE, viewModel)
            SortChip("Fecha", SortOption.DATE, viewModel)
        }
        Divider()

        // --- Barra de Estado y Progreso ---
        if (transferProgress != null) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transfiriendo...", style = MaterialTheme.typography.labelSmall)
                    Text("${(transferProgress!! * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = transferProgress!!, modifier = Modifier.fillMaxWidth())
            }
        } else if (isLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (statusMessage.isNotEmpty() && transferProgress == null) {
            Text(
                text = statusMessage,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(4.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }

        // --- Paneles ---
        Row(Modifier.weight(1f)) {
            FilePanel(
                modifier = Modifier.weight(1f),
                title = "Local",
                path = localPath,
                files = localFiles,
                onUpClick = { viewModel.navigateLocalUp() },
                onItemClick = {
                    if (it.isDirectory) viewModel.navigateLocal(it.path)
                    else viewModel.openLocalFile(it) // Abrir archivo
                },
                onTransferClick = { viewModel.upload(it) },
                transferIcon = Icons.AutoMirrored.Filled.ArrowForward
            )

            VerticalDivider(modifier = Modifier.fillMaxHeight())

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

@Composable
fun SortChip(label: String, option: SortOption, viewModel: FileManagerViewModel) {
    val isSelected = viewModel.sortOption == option
    FilterChip(
        selected = isSelected,
        onClick = { viewModel.toggleSort(option) },
        label = { Text(label) },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    if (viewModel.sortDirection == SortDirection.ASC) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
        },
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

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
        Text(path, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        IconButton(onClick = onUpClick, modifier = Modifier.size(24.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Subir", modifier = Modifier.size(16.dp))
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