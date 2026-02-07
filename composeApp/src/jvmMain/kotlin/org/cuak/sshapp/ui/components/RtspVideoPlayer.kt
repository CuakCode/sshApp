package org.cuak.sshapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
actual fun RtspVideoPlayer(url: String, modifier: Modifier) {
    val scope = rememberCoroutineScope()

    // Gestionamos el ciclo de vida del proceso ffplay
    DisposableEffect(url) {
        var process: Process? = null

        // Lanzamos el proceso en un hilo IO para no bloquear la UI
        val job = scope.launch(Dispatchers.IO) {
            try {
                // COMANDO EXACTO QUE HAS COMPROBADO QUE FUNCIONA:
                // ffplay -fflags nobuffer -flags low_delay -framedrop -i <URL>
                // Añadimos:
                // -autoexit: Para que se cierre si falla el stream
                // -window_title: Para identificar la ventana
                // -x 1280 -y 720: (Opcional) Forzar tamaño inicial si se quiere

                val command = listOf(
                    "ffplay",
                    "-fflags", "nobuffer",
                    "-flags", "low_delay",
                    "-framedrop",
                    "-autoexit",
                    "-window_title", "Cámara en Vivo - $url",
                    "-i", url
                )

                println("[FFPLAY] Iniciando: ${command.joinToString(" ")}")

                process = ProcessBuilder(command)
                    // Redirigimos logs para ver errores en la consola del IDE si hace falta
                    .redirectErrorStream(true)
                    .start()

            } catch (e: Exception) {
                e.printStackTrace()
                println("[FFPLAY] Error al lanzar ffplay: ${e.message}")
            }
        }

        // Al salir de la pantalla (onDispose), matamos el proceso
        onDispose {
            println("[FFPLAY] Cerrando proceso...")
            process?.destroy() // Intento suave
            process?.destroyForcibly() // Asegurar cierre
            job.cancel()
        }
    }

    // UI Placeholder dentro de la App
    // Como ffplay abre su propia ventana flotante, en la app mostramos que está activo.
    Box(
        modifier = modifier
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.height(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Reproduciendo en ventana externa...",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cierra esta pestaña para detener el vídeo.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}