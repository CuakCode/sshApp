package org.cuak.sshapp.ui.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cuak.sshapp.models.PortInfo
import org.cuak.sshapp.ui.components.ResourceGauge
import org.cuak.sshapp.ui.screens.DetailUiState
import org.cuak.sshapp.ui.theme.StatusError
import org.cuak.sshapp.ui.theme.StatusSuccess
import org.cuak.sshapp.ui.theme.StatusWarning

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonitorTabContent(
    state: DetailUiState,
    onRetry: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is DetailUiState.Idle -> Text("Conectando...", style = MaterialTheme.typography.bodyLarge)
            is DetailUiState.Loading -> CircularProgressIndicator()
            is DetailUiState.Error -> ErrorView(state.message, onRetry)
            is DetailUiState.Success -> {
                val validRam = if (state.metrics.ramPercentage > 0.0) state.metrics.ramPercentage else null
                val validDisks = state.metrics.diskUsage
                val validTemps = state.metrics.temperatures.filterValues { it > 0.0 }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Rendimiento
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Rendimiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                ResourceGauge("CPU", state.metrics.cpuPercentage)
                                if (validRam != null) ResourceGauge("RAM", validRam)
                                else Text("RAM N/A", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Almacenamiento
                    if (validDisks.isNotEmpty()) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Almacenamiento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                validDisks.forEachIndexed { index, usage ->
                                    val label = if (index == 0) "Raíz" else "Disco ${index + 1}"
                                    DiskUsageBar(label, usage)
                                    if (index < validDisks.size - 1) Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // Puertos
                    if (state.metrics.openPorts.isNotEmpty()) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Puertos Activos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                PortsGrid(state.metrics.openPorts)
                            }
                        }
                    }

                    // Sensores
                    if (validTemps.isNotEmpty()) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Temperatura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                validTemps.forEach { (sensor, temp) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(sensor, style = MaterialTheme.typography.bodyMedium)
                                        Text("$temp°C", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = getStatusColor(temp, 50.0, 75.0))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -- Helpers Visuales --

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortsGrid(ports: List<PortInfo>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ports.forEach { PortChip(it) }
    }
}

@Composable
fun PortChip(info: PortInfo) {
    val seed = info.port * 12345
    val baseColor = Color(red = (seed % 256) / 255f, green = ((seed / 256) % 256) / 255f, blue = ((seed / 65536) % 256) / 255f, alpha = 1f)
    Surface(
        color = baseColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, baseColor.copy(alpha = 0.5f)),
        modifier = Modifier.height(40.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            Box(modifier = Modifier.size(8.dp).background(StatusSuccess, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(":${info.port}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(" ${info.processName.uppercase()}", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DiskUsageBar(label: String, percentage: Double) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("${percentage.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (percentage / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = getStatusColor(percentage, 60.0, 80.0),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No se pudo conectar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

fun getStatusColor(value: Double, warningThreshold: Double, errorThreshold: Double): Color {
    return when {
        value < warningThreshold -> StatusSuccess
        value <= errorThreshold -> StatusWarning
        else -> StatusError
    }
}