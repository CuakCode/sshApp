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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SortByAlpha
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
import org.cuak.sshapp.ui.screens.viewModels.FileManagerViewModel
import org.cuak.sshapp.ui.screens.viewModels.SortDirection
import org.cuak.sshapp.ui.screens.viewModels.SortOption

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

        // --- Barra de Estado y Progreso Global ---
        val currentProgress = transferProgress // Captura segura para evitar NPE
        if (currentProgress != null) {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.secondaryContainer).padding(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transfiriendo...", style = MaterialTheme.typography.labelSmall)
                    Text("${(currentProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { currentProgress }, modifier = Modifier.fillMaxWidth())
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

        // --- Paneles de Archivos ---
        Row(Modifier.weight(1f)) {
            // PANEL LOCAL
            FilePanel(
                modifier = Modifier.weight(1f),
                title = "Local",
                path = localPath,
                files = localFiles,
                sortOption = viewModel.localSortOption,
                sortDirection = viewModel.localSortDirection,
                onSortChange = { viewModel.toggleLocalSort(it) },
                onUpClick = { viewModel.navigateLocalUp() },
                onItemClick = {
                    if (it.isDirectory) viewModel.navigateLocal(it.path)
                    else viewModel.openLocalFile(it)
                },
                onTransferClick = { viewModel.upload(it) },
                transferIcon = Icons.AutoMirrored.Filled.ArrowForward
            )

            VerticalDivider(modifier = Modifier.fillMaxHeight())

            // PANEL REMOTO
            FilePanel(
                modifier = Modifier.weight(1f),
                title = "Remoto",
                path = remotePath,
                files = remoteFiles,
                sortOption = viewModel.remoteSortOption,
                sortDirection = viewModel.remoteSortDirection,
                onSortChange = { viewModel.toggleRemoteSort(it) },
                onUpClick = { viewModel.navigateRemoteUp() },

                // CAMBIO AQUÍ: Usamos la nueva función openRemoteFile
                onItemClick = { file ->
                    viewModel.openRemoteFile(file)
                },

                onTransferClick = { viewModel.download(it) },
                transferIcon = Icons.AutoMirrored.Filled.ArrowBack
            )
        }
    }
}

@Composable
private fun FilePanel(
    modifier: Modifier,
    title: String,
    path: String,
    files: List<SftpFile>,
    sortOption: SortOption,
    sortDirection: SortDirection,
    onSortChange: (SortOption) -> Unit,
    onUpClick: () -> Unit,
    onItemClick: (SftpFile) -> Unit,
    onTransferClick: (SftpFile) -> Unit,
    transferIcon: ImageVector
) {
    Column(modifier = modifier.padding(4.dp)) {
        // Cabecera: Título y Ruta
        Column(Modifier.padding(horizontal = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onUpClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Subir", modifier = Modifier.size(16.dp))
                }
            }
            Text(path, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray)
        }

        Spacer(Modifier.height(4.dp))

        // --- Botones de Ordenación Responsivos ---
        // Usamos BoxWithConstraints para saber el ancho real disponible de este panel
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Si el ancho del panel es menor a 280dp, usamos modo compacto (iconos)
            val isCompact = maxWidth < 280.dp

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SortChip(
                    label = "Nombre",
                    icon = Icons.Default.SortByAlpha,
                    option = SortOption.NAME,
                    currentOption = sortOption,
                    currentDirection = sortDirection,
                    isCompact = isCompact,
                    onSortChange = onSortChange
                )
                Spacer(Modifier.width(4.dp))
                SortChip(
                    label = "Tam.",
                    icon = Icons.Default.SdStorage,
                    option = SortOption.SIZE,
                    currentOption = sortOption,
                    currentDirection = sortDirection,
                    isCompact = isCompact,
                    onSortChange = onSortChange
                )
                Spacer(Modifier.width(4.dp))
                SortChip(
                    label = "Fecha",
                    icon = Icons.Default.DateRange,
                    option = SortOption.DATE,
                    currentOption = sortOption,
                    currentDirection = sortDirection,
                    isCompact = isCompact,
                    onSortChange = onSortChange
                )
            }
        }

        HorizontalDivider(Modifier.padding(top = 8.dp))

        // Lista de Archivos
        LazyColumn(Modifier.fillMaxSize()) {
            items(files) { file ->
                FileItemRow(file, { onItemClick(file) }, { onTransferClick(file) }, transferIcon)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortChip(
    label: String,
    icon: ImageVector,
    option: SortOption,
    currentOption: SortOption,
    currentDirection: SortDirection,
    isCompact: Boolean,
    onSortChange: (SortOption) -> Unit
) {
    val isSelected = currentOption == option

    FilterChip(
        selected = isSelected,
        onClick = { onSortChange(option) },
        label = {
            if (isCompact) {
                // Modo Compacto: Solo Icono de categoría
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                // Modo Normal: Texto
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        },
        trailingIcon = {
            if (isSelected) {
                Icon(
                    if (currentDirection == SortDirection.ASC) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        modifier = Modifier.height(32.dp),
        enabled = true,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = null
    )
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