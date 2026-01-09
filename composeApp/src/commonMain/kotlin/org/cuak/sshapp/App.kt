package org.cuak.sshapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.cuak.sshapp.ui.screens.HomeScreen
import org.cuak.sshapp.ui.screens.HomeViewModel
import org.cuak.sshapp.ui.theme.AppTheme // Importa tu nuevo tema
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    // 1. Instanciamos el ViewModel para las pruebas
    val viewModel = remember { HomeViewModel() }

    // 2. Usamos AppTheme para el soporte de tema oscuro
    AppTheme {
        // 3. Surface aplica el color de fondo del tema automáticamente
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            HomeScreen(
                viewModel = viewModel,
                onServerClick = { server ->
                    println("Navegando a detalles de: ${server.name}")
                },
                onAddServer = {
                    println("Abrir formulario de nuevo servidor")
                }
            )
        } // Cierre de Surface
    } // Cierre de AppTheme
}