package org.cuak.sshapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.cuak.sshapp.ui.screens.HomeScreen
import org.cuak.sshapp.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Inicializamos el Navegador de Voyager con la pantalla de inicio
            Navigator(HomeScreen()) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}