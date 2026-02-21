package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import org.cuak.sshapp.models.Device
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.Camera
import org.cuak.sshapp.ui.components.getIconByName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormDialog(
    device: Device? = null, // Ahora usamos la interfaz base
    onDismiss: () -> Unit,
    onConfirm: (Device) -> Unit // Devolvemos la interfaz base
) {
    // --- ESTADOS COMUNES ---
    var name by remember { mutableStateOf(device?.name ?: "") }
    var ip by remember { mutableStateOf(device?.ip ?: "") }
    var port by remember { mutableStateOf(device?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(device?.username ?: "") }
    var password by remember { mutableStateOf(device?.password ?: "") }
    var sshKeyPath by remember { mutableStateOf(device?.sshKeyPath ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }

    // --- ESTADOS DE TIPO Y UI ---
    // Determinamos si es cámara comprobando la clase del objeto inicial
    var isCameraType by remember { mutableStateOf(device is Camera) }
    var selectedIcon by remember { mutableStateOf(device?.iconName ?: "dns") }

    // Control de dropdowns
    var typeExpanded by remember { mutableStateOf(false) }
    var iconExpanded by remember { mutableStateOf(false) }

    // --- ESTADOS ESPECÍFICOS DE CÁMARA ---
    // Intentamos castear el device inicial a Camera para sacar sus valores si los tiene
    val initialCamera = device as? Camera
    var cameraProtocol by remember { mutableStateOf(initialCamera?.cameraProtocol ?: "RTSP") }
    var cameraPort by remember { mutableStateOf(initialCamera?.cameraPort?.toString() ?: "8554") }
    var cameraStream by remember { mutableStateOf(initialCamera?.cameraStream ?: "ch0_0.h264") }

    // Lista de iconos disponibles
    val iconOptions = listOf(
        "dns", "videocam", "camera_alt", "security", "cast_connected",
        "storage", "computer", "router", "cloud", "memory", "smart_toy"
    )

    // --- LÓGICA AUTOMÁTICA ---
    // Si cambiamos a CÁMARA y es un dispositivo nuevo, sugerimos icono
    LaunchedEffect(isCameraType) {
        if (isCameraType && device == null) {
            if (selectedIcon == "dns") selectedIcon = "videocam"
        } else if (!isCameraType && device == null) {
            if (selectedIcon == "videocam") selectedIcon = "dns"
        }
    }

    // --- FILE PICKER (Common) ---
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("pem", "key", "ppk", "pub")),
        title = "Seleccionar Clave Privada"
    ) { file ->
        file?.path?.let { sshKeyPath = it }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 750.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (device == null) "Nuevo Dispositivo" else "Editar Dispositivo",
                    style = MaterialTheme.typography.headlineSmall
                )

                HorizontalDivider()

                // --- 1. SELECTOR DE TIPO ---
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = if (!isCameraType) "Servidor Linux" else "Cámara Yi Hack",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Dispositivo") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (!isCameraType) Icons.Default.Dns else Icons.Default.Videocam,
                                contentDescription = null
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Servidor Linux", style = MaterialTheme.typography.bodyLarge)
                                    Text("VPS, Raspberry Pi, Ubuntu...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            onClick = { isCameraType = false; typeExpanded = false },
                            leadingIcon = { Icon(Icons.Default.Dns, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Cámara Yi Hack", style = MaterialTheme.typography.bodyLarge)
                                    Text("Yi Home/Kami (Allwinner/MStar)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            onClick = { isCameraType = true; typeExpanded = false },
                            leadingIcon = { Icon(Icons.Default.Videocam, null) }
                        )
                    }
                }

                // --- 2. SELECTOR DE ICONO ---
                ExposedDropdownMenuBox(
                    expanded = iconExpanded,
                    onExpandedChange = { iconExpanded = !iconExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedIcon.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Icono Visual") },
                        leadingIcon = { Icon(getIconByName(selectedIcon), contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = iconExpanded,
                        onDismissRequest = { iconExpanded = false }
                    ) {
                        iconOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.replaceFirstChar { it.uppercase() }) },
                                onClick = { selectedIcon = option; iconExpanded = false },
                                leadingIcon = { Icon(getIconByName(option), contentDescription = null) }
                            )
                        }
                    }
                }

                // --- 3. CAMPOS COMUNES ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP / Host") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { if (it.all { c -> c.isDigit() }) port = it },
                        label = { Text("Puerto SSH") },
                        modifier = Modifier.width(110.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario SSH") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { if (isCameraType) Text("root") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña SSH (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(image, null)
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = sshKeyPath,
                    onValueChange = { sshKeyPath = it },
                    label = { Text("Ruta Clave Privada (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Seleccionar archivo...") },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { launcher.launch() }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Seleccionar archivo")
                        }
                    }
                )

                // --- 4. CAMPOS ESPECÍFICOS DE CÁMARA ---
                if (isCameraType) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Configuración de Video RTSP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cameraProtocol,
                                    onValueChange = { cameraProtocol = it },
                                    label = { Text("Protocolo") },
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                )
                                OutlinedTextField(
                                    value = cameraPort,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) cameraPort = it },
                                    label = { Text("Puerto Video") },
                                    modifier = Modifier.width(110.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }

                            // Selector de Calidad (Main/Sub Stream)
                            var expandedStream by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = if (cameraStream == "ch0_0.h264") "Alta Calidad (Main)" else "Baja Calidad (Sub)",
                                    onValueChange = {},
                                    label = { Text("Calidad Stream") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                                )
                                Box(modifier = Modifier
                                    .matchParentSize()
                                    .clickable { expandedStream = true })

                                DropdownMenu(
                                    expanded = expandedStream,
                                    onDismissRequest = { expandedStream = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Alta Calidad (ch0_0.h264)") },
                                        onClick = { cameraStream = "ch0_0.h264"; expandedStream = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Baja Calidad (ch0_1.h264)") },
                                        onClick = { cameraStream = "ch0_1.h264"; expandedStream = false }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 5. BOTONES DE ACCIÓN ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // AQUÍ ESTÁ LA MAGIA:
                            // Instanciamos el objeto concreto en función del tipo seleccionado
                            val finalDevice = if (isCameraType) {
                                Camera(
                                    id = device?.id ?: 0,
                                    name = name,
                                    ip = ip,
                                    port = port.toIntOrNull() ?: 22,
                                    username = username,
                                    password = password.ifBlank { null },
                                    sshKeyPath = sshKeyPath.ifBlank { null },
                                    iconName = selectedIcon,
                                    cameraProtocol = cameraProtocol,
                                    cameraPort = cameraPort.toIntOrNull() ?: 8554,
                                    cameraStream = cameraStream
                                )
                            } else {
                                Server(
                                    id = device?.id ?: 0,
                                    name = name,
                                    ip = ip,
                                    port = port.toIntOrNull() ?: 22,
                                    username = username,
                                    password = password.ifBlank { null },
                                    sshKeyPath = sshKeyPath.ifBlank { null },
                                    iconName = selectedIcon
                                )
                            }
                            onConfirm(finalDevice)
                        },
                        enabled = name.isNotBlank() && ip.isNotBlank() && username.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}