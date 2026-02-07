package org.cuak.sshapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegLogCallback
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage

@Composable
actual fun RtspVideoPlayer(
    url: String,
    modifier: Modifier,
    onStatusChange: (String) -> Unit
) {
    var currentFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var connectionStatus by remember { mutableStateOf("Initializing...") }
    var isError by remember { mutableStateOf(false) }

    // Habilitar logs nativos de FFmpeg para ver errores detallados en consola
    LaunchedEffect(Unit) {
        try {
            FFmpegLogCallback.set()
        } catch (e: Exception) {
            println("No se pudo establecer el callback de log de FFmpeg: ${e.message}")
        }
    }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            val converter = Java2DFrameConverter()

            // Bucle infinito para RECONEXIÓN automática
            while (isActive) {
                var grabber: FFmpegFrameGrabber? = null
                try {
                    isError = false
                    connectionStatus = "Connecting..."
                    onStatusChange("Connecting to $url...")
                    println("RTSP: Iniciando conexión a -> $url")

                    grabber = FFmpegFrameGrabber(url)

                    // --- OPTIMIZACIÓN DE LATENCIA Y TRANSPORTE ---
                    grabber.format = "rtsp"
                    grabber.setOption("rtsp_transport", "tcp") // TCP reduce artefactos visuales (pantalla gris)

                    // Flags de baja latencia
                    grabber.setOption("fflags", "nobuffer")
                    grabber.setOption("flags", "low_delay")

                    // Timeouts (en microsegundos)
                    // stimeout: Timeout para esperar datos (socket timeout). 5 segundos.
                    grabber.setOption("stimeout", "5000000")

                    // Acelerar apertura
                    grabber.setOption("probesize", "32")
                    grabber.setOption("analyzeduration", "0")

                    // Frame dropping: Si la CPU no aguanta, descartar frames viejos
                    grabber.setVideoOption("framedrop", "1")

                    grabber.start()

                    connectionStatus = "Online"
                    onStatusChange("Connected")
                    println("RTSP: Conexión establecida.")

                    // Bucle de captura de frames
                    while (isActive) {
                        try {
                            // grabImage() es más eficiente que grab() si no necesitamos audio
                            val frame = grabber.grabImage()

                            if (frame != null) {
                                val bufferedImage: BufferedImage? = converter.convert(frame)
                                if (bufferedImage != null) {
                                    val composeBitmap = bufferedImage.toComposeImageBitmap()
                                    currentFrame = composeBitmap
                                }
                            } else {
                                // Si grabImage devuelve null pero no saltó excepción, puede ser fin de stream o error temporal
                                // Verificamos si seguimos conectados
                            }
                        } catch (e: Exception) {
                            println("RTSP Frame Error (recuperable?): ${e.message}")
                            // Si falla un frame, intentamos seguir. Si es grave, el bucle externo lo capturará.
                            throw e
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    isError = true
                    connectionStatus = "Error: ${e.message?.take(50)}..."
                    onStatusChange("Error: ${e.message}")

                    // Limpieza antes de reintentar
                    try {
                        grabber?.stop()
                        grabber?.release()
                    } catch (releaseEx: Exception) { /* Ignorar error al cerrar */ }

                    currentFrame = null // Limpiar pantalla o dejar el último frame congelado según preferencia

                    println("RTSP: Conexión perdida. Reintentando en 3 segundos...")
                    delay(3000) // Esperar antes de reconectar
                } finally {
                    try {
                        grabber?.stop()
                        grabber?.release()
                    } catch (e: Exception) { /* Ignorar */ }
                }
            }
            converter.close()
        }
    }

    // UI Render
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (currentFrame != null) {
            Image(
                bitmap = currentFrame!!,
                contentDescription = "RTSP Live Stream",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Overlay de información (Cargando o Error)
        if (currentFrame == null || isError) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isError) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = connectionStatus,
                    color = if (isError) Color.Red else Color.White,
                    style = MaterialTheme.typography.bodySmall
                )

                if (isError) {
                    Text(
                        text = "Retrying...",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Indicador discreto "LIVE" si está funcionando
        if (currentFrame != null && !isError) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Red.copy(alpha = 0.7f), shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}