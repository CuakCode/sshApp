package org.cuak.sshapp.ui.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.ProcessSortOption
import org.cuak.sshapp.ui.theme.StatusError
import org.cuak.sshapp.ui.theme.StatusSuccess
import org.cuak.sshapp.ui.theme.StatusWarning
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProcessesTabContent(
    processes: List<ProcessInfo>,
    isLoading: Boolean,
    sortOption: ProcessSortOption,
    onSort: (ProcessSortOption) -> Unit,
    onRefresh: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Barra superior: Contador y botón de refrescar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ${processes.size} procesos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, "Refrescar")
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Lista de procesos
            if (isLoading && processes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Cabecera Fija (Sticky)
                    stickyHeader {
                        ProcessHeaderRow(sortOption, onSort)
                    }

                    items(processes, key = { it.pid }) { process ->
                        ProcessRow(process)
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessHeaderRow(
    currentSort: ProcessSortOption,
    onSort: (ProcessSortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp) // Altura fija para asegurar área de click uniforme
            .background(MaterialTheme.colorScheme.surface) // Fondo opaco para sticky
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Celdas de cabecera con pesos y alineación específica
        // PID: Izquierda
        HeaderCell(
            text = "PID",
            weight = 0.15f,
            align = Alignment.CenterStart,
            isSelected = currentSort == ProcessSortOption.PID,
            onClick = { onSort(ProcessSortOption.PID) }
        )
        // Nombre: Izquierda
        HeaderCell(
            text = "Nombre",
            weight = 0.45f,
            align = Alignment.CenterStart,
            isSelected = currentSort == ProcessSortOption.NAME,
            onClick = { onSort(ProcessSortOption.NAME) }
        )
        // CPU: Derecha (números)
        HeaderCell(
            text = "CPU%",
            weight = 0.2f,
            align = Alignment.CenterEnd,
            isSelected = currentSort == ProcessSortOption.CPU,
            onClick = { onSort(ProcessSortOption.CPU) }
        )
        // MEM: Derecha (números)
        HeaderCell(
            text = "MEM%",
            weight = 0.2f,
            align = Alignment.CenterEnd,
            isSelected = currentSort == ProcessSortOption.MEM,
            onClick = { onSort(ProcessSortOption.MEM) }
        )
    }
    Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.primaryContainer)
}

@Composable
fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: Alignment,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Usamos un Box que llene la altura para que el click sea fácil
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clickable(onClick = onClick) // Click en el contenedor
            .padding(vertical = 4.dp),
        contentAlignment = align
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Flecha indicadora de orden
            if (isSelected) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProcessRow(proc: ProcessInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // PID
        Text(
            text = proc.pid,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(0.15f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start // Aseguramos alineación izquierda
        )

        // Nombre y Usuario
        Column(modifier = Modifier.weight(0.45f)) {
            Text(
                text = proc.command.split(" ").first(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = proc.user,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }

        // CPU Badge
        Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.CenterEnd) {
            UsageBadge(proc.cpuUsage, thresholdWarning = 20.0, thresholdError = 50.0)
        }

        // MEM Badge
        Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.CenterEnd) {
            UsageBadge(proc.memUsage, thresholdWarning = 20.0, thresholdError = 50.0)
        }
    }
}

@Composable
fun UsageBadge(value: Double, thresholdWarning: Double, thresholdError: Double) {
    val color = when {
        value >= thresholdError -> StatusError
        value >= thresholdWarning -> StatusWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isHigh = value >= thresholdWarning

    // Formateamos siempre a 2 decimales (ej: 0.0 -> "0.00%", 5.2 -> "5.20%")
    val displayText = "${formatDecimal(value, 2)}%"

    if (isHigh) {
        // Estilo "Alerta" con fondo
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
        ) {
            Text(
                text = displayText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        // Estilo "Normal"
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = color.copy(alpha = 0.8f),
            // Usamos fuente monoespaciada para que los números se alineen bien verticalmente
            fontFamily = FontFamily.Monospace
        )
    }
}

// Función helper para formatear decimales en KMP (Common)
fun formatDecimal(value: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = (value * factor).roundToInt() / factor
    val string = rounded.toString()

    // Asegurar que siempre tenga los decimales pedidos (rellenar con ceros)
    val parts = string.split(".")
    val integerPart = parts[0]
    val fractionalPart = if (parts.size > 1) parts[1] else ""

    val paddedFraction = fractionalPart.padEnd(decimals, '0')
    return "$integerPart.$paddedFraction"
}