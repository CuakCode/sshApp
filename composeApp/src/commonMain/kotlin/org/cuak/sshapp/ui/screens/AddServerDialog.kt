package org.cuak.sshapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
// Importaciones corregidas según el código fuente de FileKit
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.FileKitType
import org.cuak.sshapp.ui.components.getIconByName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServerDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        ip: String,
        port: Int,
        user: String,
        pass: String?,
        keyPath: String?,
        icon: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var sshKeyPath by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val iconOptions = listOf("dns", "storage", "computer", "router", "cloud")
    var selectedIcon by remember { mutableStateOf(iconOptions[0]) }

    // Configuración corregida: rememberFilePickerLauncher y FileKitType
    val pickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("key", "pem", "pub", "ppk")),
        title = "Selecciona tu clave SSH"
    ) { file ->
        // file es de tipo PlatformFile?
        file?.path?.let { sshKeyPath = it }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Servidor") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Servidor") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP / Host") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { if (it.all { c -> c.isDigit() }) port = it },
                        label = { Text("Puerto") },
                        modifier = Modifier.width(90.dp)
                    )
                }

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Usuario SSH") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Contraseña con icono de visibilidad
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña (Opcional)") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = "Toggle password")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // SSH Key con FileKit
                OutlinedTextField(
                    value = sshKeyPath,
                    onValueChange = { sshKeyPath = it },
                    label = { Text("Ruta SSH Key") },
                    placeholder = { Text("Seleccionar archivo...") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { pickerLauncher.launch() }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Abrir explorador")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de Icono
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedIcon.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Icono") },
                        leadingIcon = { Icon(getIconByName(selectedIcon), contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        iconOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedIcon = option
                                    expanded = false
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
                        name, ip, port.toIntOrNull() ?: 22, user,
                        password.ifBlank { null }, sshKeyPath.ifBlank { null }, selectedIcon
                    )
                },
                enabled = name.isNotBlank() && ip.isNotBlank() && user.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}