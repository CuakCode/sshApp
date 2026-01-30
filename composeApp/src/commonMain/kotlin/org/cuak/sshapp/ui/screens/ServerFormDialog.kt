package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitType
import org.cuak.sshapp.models.DeviceType
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.ui.components.getIconByName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormDialog(
    serverToEdit: Server? = null,
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        ip: String,
        port: Int,
        user: String,
        pass: String?,
        keyPath: String?,
        icon: String,
        type: DeviceType // Nuevo parámetro
    ) -> Unit
) {
    // --- ESTADOS ---
    var name by remember { mutableStateOf(serverToEdit?.name ?: "") }
    var ip by remember { mutableStateOf(serverToEdit?.ip ?: "") }
    var port by remember { mutableStateOf(serverToEdit?.port?.toString() ?: "22") }
    var user by remember { mutableStateOf(serverToEdit?.username ?: "") }
    var password by remember { mutableStateOf(serverToEdit?.password ?: "") }
    var sshKeyPath by remember { mutableStateOf(serverToEdit?.sshKeyPath ?: "") }
    var selectedIcon by remember { mutableStateOf(serverToEdit?.iconName ?: "dns") }

    // Nuevo estado para el Tipo de Dispositivo
    var selectedType by remember(serverToEdit) {
        mutableStateOf(serverToEdit?.type ?: DeviceType.SERVER)
    }

    var passwordVisible by remember { mutableStateOf(false) }

    // Control de los dropdowns
    var iconExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }

    // Opciones de iconos (Añadido 'videocam' para las cámaras)
    val iconOptions = listOf(
        "dns",          // Servidor genérico
        "videocam",     // Cámara de video estándar
        "camera_alt",   // Cámara de fotos clásica
        "security",     // Escudo de seguridad
        "cast_connected", // Dispositivo de streaming
        "storage",      // NAS / Almacenamiento
        "computer",     // PC / Laptop
        "router",       // Router / Switch
        "cloud",        // Nube / VPS remoto
        "memory",       // Chip / IoT
        "smart_toy"     // Robot / Dispositivo inteligente genérico
    )

    // --- LOGICA AUXILIAR ---
    // Si el usuario cambia a modo cámara, sugerimos el icono de cámara y el puerto 22
    LaunchedEffect(selectedType) {
        if (selectedType == DeviceType.CAMERA && serverToEdit == null) {
            selectedIcon = "videocam"
            if (port.isEmpty() || port == "22") port = "22"
        } else if (selectedType == DeviceType.SERVER && serverToEdit == null && selectedIcon == "videocam") {
            selectedIcon = "dns"
        }
    }

    // --- FILE PICKER ---
    val pickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("key", "pem", "pub", "ppk")),
        title = "Selecciona tu clave SSH"
    ) { file ->
        file?.path?.let { sshKeyPath = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (serverToEdit == null) "Nuevo Dispositivo" else "Editar Dispositivo")
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // --- 1. SELECTOR VISUAL DE TIPO DE DISPOSITIVO ---
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedType == DeviceType.SERVER) "Servidor Linux" else "Cámara Yi Hack",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Dispositivo") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (selectedType == DeviceType.SERVER) Icons.Default.Dns else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        // Opción Servidor
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Servidor Linux", style = MaterialTheme.typography.bodyLarge)
                                    Text("VPS, Raspberry Pi, Ubuntu...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            onClick = {
                                selectedType = DeviceType.SERVER
                                typeExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Dns, null) }
                        )
                        HorizontalDivider()
                        // Opción Cámara
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text("Cámara Yi Hack", style = MaterialTheme.typography.bodyLarge)
                                    Text("Yi Home/Kami (Allwinner/MStar)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            },
                            onClick = {
                                selectedType = DeviceType.CAMERA
                                typeExpanded = false
                            },
                            leadingIcon = { Icon(Icons.Default.Videocam, null) }
                        )
                    }
                }

                // --- CAMPOS RESTANTES ---
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        label = { Text("Puerto") },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Usuario SSH") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { if(selectedType == DeviceType.CAMERA) Text("root") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña (Opcional)") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Toggle password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sshKeyPath,
                    onValueChange = { sshKeyPath = it },
                    label = { Text("Ruta SSH Key") },
                    placeholder = { Text("Seleccionar archivo...") },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { pickerLauncher.launch() }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Abrir explorador")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- SELECTOR DE ICONO ---
                ExposedDropdownMenuBox(
                    expanded = iconExpanded,
                    onExpandedChange = { iconExpanded = !iconExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedIcon.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Icono") },
                        leadingIcon = {
                            Icon(getIconByName(selectedIcon), contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = iconExpanded,
                        onDismissRequest = { iconExpanded = false }
                    ) {
                        iconOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedIcon = option
                                    iconExpanded = false
                                },
                                leadingIcon = {
                                    Icon(getIconByName(option), contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        ip,
                        port.toIntOrNull() ?: 22,
                        user,
                        password.ifBlank { null },
                        sshKeyPath.ifBlank { null },
                        selectedIcon,
                        selectedType // Enviamos el tipo seleccionado
                    )
                },
                enabled = name.isNotBlank() && ip.isNotBlank() && user.isNotBlank()
            ) {
                Text(if (serverToEdit == null) "Guardar" else "Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}