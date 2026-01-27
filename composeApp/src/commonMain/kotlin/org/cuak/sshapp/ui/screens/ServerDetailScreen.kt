package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerMetrics
import org.cuak.sshapp.ui.components.ResourceGauge
import org.cuak.sshapp.ui.components.getIconByName
import org.cuak.sshapp.ui.theme.StatusError
import org.cuak.sshapp.ui.theme.StatusSuccess
import org.cuak.sshapp.ui.theme.StatusWarning
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.shape.RoundedCornerShape

data class ServerDetailScreen(val serverId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinScreenModel<ServerDetailViewModel>()

        LaunchedEffect(serverId) {
            viewModel.loadServer(serverId)
        }

        val server = viewModel.server
        val state = viewModel.uiState

        // Estado para las pestañas
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Monitor", "Procesos", "Terminal")

        var showShutdownDialog by remember { mutableStateOf(false) }
        if (showShutdownDialog) {
            AlertDialog(
                onDismissRequest = { showShutdownDialog = false },
                icon = { Icon(Icons.Default.Warning, contentDescription = null) },
                title = { Text("¿Apagar Servidor?") },
                text = { Text("Esta acción ejecutará 'sudo poweroff'. El servidor dejará de responder.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.shutdownServer()
                            showShutdownDialog = false
                            navigator.pop() // Volvemos a la lista
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Apagar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showShutdownDialog = false }) { Text("Cancelar") }
                }
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(server?.name ?: "Cargando...", style = MaterialTheme.typography.titleMedium)
                            if (server != null) {
                                Text(server.ip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showShutdownDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Apagar",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(onClick = { viewModel.fetchMetrics() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                        }
                    }
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Cabecera de Pestañas
                PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Default.Speed, null)
                                    1 -> Icon(Icons.Default.Memory, null)
                                    2 -> Icon(Icons.Default.Terminal, null)
                                }
                            }
                        )
                    }
                }

                // Contenido según la pestaña seleccionada
                Box(modifier = Modifier.fillMaxSize()) {
                    when (selectedTabIndex) {
                        0 -> MonitorTabContent(state) { viewModel.fetchMetrics() }
                        1 -> PlaceholderTab("Gestión de Procesos (Próximamente)")
                        // 2. Conectamos la nueva pestaña
                        2 -> TerminalTabContent(
                            output = viewModel.terminalOutput,
                            input = viewModel.terminalInput,
                            onInputChange = { viewModel.terminalInput = it },
                            onSend = { viewModel.sendTerminalCommand(it) },
                            onSpecialKey = { viewModel.sendSpecialKey(it) },
                            onStart = { viewModel.startTerminal() }
                        )
                    }
                }
            }
        }
    }
}

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
                // 1. FILTRADO PREVIO: Preparamos los datos válidos
                // Asumimos que 0.0 en RAM o Temperatura es un error de lectura.
                val validRam = if (state.metrics.ramPercentage > 0.0) state.metrics.ramPercentage else null

                // Filtramos discos que tengan valor (por si alguno falla y da 0.0, aunque 0% lleno es posible, es raro en root)
                // Si prefieres mostrar discos vacíos, quita el .filter
                val validDisks = state.metrics.diskUsage

                // Filtramos temperaturas: Solo mostramos las que sean mayores a 0°C
                val validTemps = state.metrics.temperatures.filterValues { it > 0.0 }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- TARJETA DE RENDIMIENTO (CPU / RAM) ---
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Rendimiento del Sistema",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // CPU siempre se muestra (0% es un valor válido)
                                ResourceGauge("CPU", state.metrics.cpuPercentage)

                                // RAM: Solo se muestra si es válida
                                if (validRam != null) {
                                    ResourceGauge("RAM", validRam)
                                } else {
                                    // Opcional: Mostrar un placeholder si no hay RAM
                                    Text("RAM N/A", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // --- TARJETA DE ALMACENAMIENTO ---
                    if (validDisks.isNotEmpty()) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Almacenamiento",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                validDisks.forEachIndexed { index, usage ->
                                    val label = if (index == 0) "Raíz (root)" else "Disco ${index + 1}"
                                    DiskUsageBar(label, usage)
                                    if (index < validDisks.size - 1) Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    // --- TARJETA DE SENSORES (TEMPERATURA) ---
                    // Lógica Clave: Solo pintamos la tarjeta si hay al menos una temperatura válida
                    if (validTemps.isNotEmpty()) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Sensores Térmicos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Iteramos solo sobre las temperaturas válidas
                                validTemps.forEach { (sensor, temp) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sensor, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "$temp°C",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            // Usamos tu función de color o la genérica
                                            color = getStatusColor(temp, 50.0, 75.0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Si validTemps está vacío, esta tarjeta simplemente no se renderiza.
                }
            }
        }
    }
}

@Composable
fun DiskUsageBar(label: String, percentage: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No se pudo conectar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
fun PlaceholderTab(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Construction, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

// Función auxiliar para colores (podrías moverla a utils si quieres reutilizarla fuera de ResourceGauge)
fun getStatusColor(value: Double, warningThreshold: Double, errorThreshold: Double): Color {
    return when {
        value < warningThreshold -> StatusSuccess
        value <= errorThreshold -> StatusWarning
        else -> StatusError
    }
}

@Composable
fun TerminalTabContent(
    output: String,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onSpecialKey: (String) -> Unit,
    onStart: () -> Unit
) {
    // Auto-scroll al final
    val scrollState = rememberScrollState()
    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Iniciar conexión al entrar (solo una vez)
    LaunchedEffect(Unit) {
        onStart()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E)) // Fondo oscuro tipo terminal
            .padding(8.dp)
    ) {
        // --- PANTALLA DE SALIDA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Text(
                text = output,
                color = Color(0xFF00FF00), // Verde hacker clásico
                fontFamily = FontFamily.Monospace,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                modifier = Modifier.verticalScroll(scrollState)
            )
        }

        // --- BARRA DE TECLAS ESPECIALES ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Códigos ASCII / ANSI
            val ctrlC = "\u0003"
            val arrowUp = "\u001B[A"
            val arrowDown = "\u001B[B"
            val ctrlR = "\u0012"
            val tab = "\u0009"

            TerminalButton("Ctrl+C", onClick = { onSpecialKey(ctrlC) }, color = MaterialTheme.colorScheme.error)
            TerminalButton("⬆", onClick = { onSpecialKey(arrowUp) })
            TerminalButton("⬇", onClick = { onSpecialKey(arrowDown) })
            TerminalButton("Ctrl+R", onClick = { onSpecialKey(ctrlR) })
            TerminalButton("TAB", onClick = { onSpecialKey(tab) })
        }

        // --- CAMPO DE ENTRADA ---
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Escribe un comando...", color = Color.Gray) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend(input) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00FF00),
                unfocusedBorderColor = Color.Gray,
                cursorColor = Color(0xFF00FF00)
            )
        )
    }
}

@Composable
fun TerminalButton(text: String, onClick: () -> Unit, color: Color = MaterialTheme.colorScheme.primaryContainer) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(text, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = Color.White)
    }
}