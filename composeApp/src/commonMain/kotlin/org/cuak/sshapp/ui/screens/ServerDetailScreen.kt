package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.BorderStroke
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
import org.cuak.sshapp.ui.components.ResourceGauge
import org.cuak.sshapp.ui.theme.StatusError
import org.cuak.sshapp.ui.theme.StatusSuccess
import org.cuak.sshapp.ui.theme.StatusWarning
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.FlowRow // Asegúrate de que tu versión de Compose lo soporta (1.5+), si no usa un Grid
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.KeyboardType
import org.cuak.sshapp.models.PortInfo


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
                        0 -> {
                            LaunchedEffect(Unit) {
                                viewModel.fetchMetrics()
                            }
                            MonitorTabContent(state) { viewModel.fetchMetrics() }
                        }
                        1 -> PlaceholderTab("Gestión de Procesos (Próximamente)")
                        2 -> TerminalTabContent(
                            output = viewModel.terminalOutput,
                            onSendInput = { input -> viewModel.sendInput(input) },
                            onStart = { viewModel.startTerminal() }
                        )
                    }
                }
            }
        }
    }
}

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
                // 1. FILTRADO PREVIO
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
                                ResourceGauge("CPU", state.metrics.cpuPercentage)

                                if (validRam != null) {
                                    ResourceGauge("RAM", validRam)
                                } else {
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

                    // --- TARJETA DE PUERTOS ACTIVOS (NUEVA) ---
                    if (state.metrics.openPorts.isNotEmpty()) {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Puertos Activos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Renderizamos la rejilla colorida
                                PortsGrid(state.metrics.openPorts)
                            }
                        }
                    }

                    // --- TARJETA DE SENSORES (TEMPERATURA) ---
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
                                            color = getStatusColor(temp, 50.0, 75.0)
                                        )
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

// --- COMPONENTES AUXILIARES PARA EL DISEÑO DE PUERTOS ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortsGrid(ports: List<PortInfo>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        ports.forEach { portInfo ->
            PortChip(portInfo)
        }
    }
}

@Composable
fun PortChip(info: PortInfo) {
    // Generación de color determinista basado en el puerto
    val seed = info.port * 12345
    val baseColor = Color(
        red = (seed % 256) / 255f,
        green = ((seed / 256) % 256) / 255f,
        blue = ((seed / 65536) % 256) / 255f,
        alpha = 1f
    )

    // Fondo pastel suave
    val backgroundColor = baseColor.copy(alpha = 0.15f)
    // Borde un poco más fuerte
    val strokeColor = baseColor.copy(alpha = 0.5f)

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, strokeColor),
        modifier = Modifier.height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Indicador de estado
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(StatusSuccess, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = ":${info.port}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = info.processName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
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
// Asegúrate de añadir estos imports

@Composable
fun TerminalTabContent(
    output: String,
    onSendInput: (String) -> Unit,
    onStart: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Configuración del teclado invisible
    var hiddenInput by remember { mutableStateOf(" ") } // Espacio como "ancla"
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll al final
    LaunchedEffect(output) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    // Iniciar conexión y ABRIR TECLADO automáticamente al entrar
    LaunchedEffect(Unit) {
        onStart()
        // Pequeño delay para asegurar que la UI está lista antes de pedir foco
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(8.dp)
        // IMPORTANTE: Quitamos el clickable de aquí para permitir seleccionar texto
    ) {
        // --- PANTALLA DE SALIDA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
                .background(Color.Black, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            // Permitimos COPIAR texto
            SelectionContainer {
                Text(
                    text = output,
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                )
            }

            // --- TEXTFIELD INVISIBLE (El motor de escritura) ---
            TextField(
                value = hiddenInput,
                onValueChange = { newValue ->
                    // Lógica robusta: detectamos cambios respecto al estado anterior " "
                    if (newValue.length < 1) {
                        // Se borró el espacio -> Backspace
                        onSendInput("\u007F")
                    } else if (newValue.length > 1) {
                        // Se escribió algo nuevo
                        val char = newValue.substring(1) // Todo después del espacio inicial
                        onSendInput(char)
                    }
                    // Siempre reseteamos al ancla " "
                    hiddenInput = " "
                },
                modifier = Modifier
                    .alpha(0f) // Invisible
                    .size(1.dp) // Minúsculo
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false, // VITAL: Sin autocorrector
                    keyboardType = KeyboardType.Ascii, // Teclado simple
                    imeAction = ImeAction.None
                ),
                singleLine = true
            )
        }

        // --- BARRA DE CONTROL ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Botones de control
            TerminalButton("Esc", onClick = { onSendInput("\u001B") }, color = MaterialTheme.colorScheme.secondary)
            TerminalButton("Ctrl+C", onClick = { onSendInput("\u0003") }, color = MaterialTheme.colorScheme.error)

            // Botón para recuperar el teclado si se cierra
            TerminalButton("⌨️", onClick = {
                focusRequester.requestFocus()
                keyboardController?.show()
            })

            TerminalButton("⬆", onClick = { onSendInput("\u001B[A") })
            TerminalButton("⬇", onClick = { onSendInput("\u001B[B") })
            TerminalButton("TAB", onClick = { onSendInput("\u0009") })
            TerminalButton("Enter", onClick = { onSendInput("\n") })
        }

        Text(
            "Pulsa ⌨️ si el teclado se cierra",
            color = Color.Gray,
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
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