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
import org.cuak.sshapp.models.DeviceType
import org.cuak.sshapp.models.Server

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormDialog(
    server: Server? = null,
    onDismiss: () -> Unit,
    onConfirm: (Server) -> Unit
) {
    // Estados básicos
    var name by remember { mutableStateOf(server?.name ?: "") }
    var ip by remember { mutableStateOf(server?.ip ?: "") }
    var port by remember { mutableStateOf(server?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(server?.username ?: "") }
    var password by remember { mutableStateOf(server?.password ?: "") }

    // --- RECUPERADO: Estado para la ruta de la clave SSH ---
    var sshKeyPath by remember { mutableStateOf(server?.sshKeyPath ?: "") }

    // Estado del tipo de dispositivo
    var type by remember { mutableStateOf(server?.type ?: DeviceType.SERVER) }

    // Estados específicos de CÁMARA
    var cameraProtocol by remember { mutableStateOf(server?.cameraProtocol ?: "RTSP") }
    var cameraPort by remember { mutableStateOf(server?.cameraPort?.toString() ?: "8554") }
    var cameraStream by remember { mutableStateOf(server?.cameraStream ?: "ch0_0.h264") }

    var passwordVisible by remember { mutableStateOf(false) }

    // Configuración del Selector de Archivos (FileKit)
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("pem", "key", "ppk", "pub")), // Filtro opcional
        title = "Seleccionar Clave Privada"
    ) { file ->
        // Actualizamos la ruta si se selecciona un archivo
        file?.path?.let { sshKeyPath = it }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = 700.dp), // Aumentamos un poco la altura máxima
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (server == null) "Nuevo Dispositivo" else "Editar Dispositivo",
                    style = MaterialTheme.typography.titleLarge
                )

                // Selector de Tipo de Dispositivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == DeviceType.SERVER,
                        onClick = { type = DeviceType.SERVER },
                        label = { Text("Servidor Linux") },
                        leadingIcon = { Icon(Icons.Default.Dns, null) }
                    )
                    FilterChip(
                        selected = type == DeviceType.CAMERA,
                        onClick = { type = DeviceType.CAMERA },
                        label = { Text("Cámara") },
                        leadingIcon = { Icon(Icons.Default.Videocam, null) }
                    )
                }

                HorizontalDivider()

                // Campos Comunes
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
                        onValueChange = { port = it.filter { c -> c.isDigit() } },
                        label = { Text("Puerto SSH") },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario SSH") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña SSH") },
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

                // --- RECUPERADO: Campo para Clave SSH ---
                OutlinedTextField(
                    value = sshKeyPath,
                    onValueChange = { sshKeyPath = it },
                    label = { Text("Ruta Clave Privada (Opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("/ruta/a/id_rsa") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { launcher.launch() }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Seleccionar archivo")
                        }
                    }
                )

                // --- SECCIÓN ESPECÍFICA DE CÁMARA ---
                if (type == DeviceType.CAMERA) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Configuración de Video", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = cameraProtocol,
                        onValueChange = { cameraProtocol = it },
                        label = { Text("Protocolo") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cameraPort,
                            onValueChange = { cameraPort = it.filter { c -> c.isDigit() } },
                            label = { Text("Puerto Video") },
                            modifier = Modifier.width(120.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        // Selector de Calidad (Alta/Baja)
                        var expandedStream by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = if (cameraStream == "ch0_0.h264") "Alta (Main)" else "Baja (Sub)",
                                onValueChange = {},
                                label = { Text("Calidad Stream") },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { expandedStream = true })

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

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de Acción
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
                            val newServer = Server(
                                id = server?.id ?: 0,
                                name = name,
                                ip = ip,
                                port = port.toIntOrNull() ?: 22,
                                username = username,
                                password = password.ifBlank { null },
                                type = type,
                                // Guardamos la clave SSH
                                sshKeyPath = sshKeyPath.ifBlank { null },
                                // Guardar campos de cámara
                                cameraProtocol = if (type == DeviceType.CAMERA) cameraProtocol else null,
                                cameraPort = if (type == DeviceType.CAMERA) (cameraPort.toIntOrNull() ?: 8554) else null,
                                cameraStream = if (type == DeviceType.CAMERA) cameraStream else null
                            )
                            onConfirm(newServer)
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
