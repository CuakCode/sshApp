package org.cuak.sshapp.ui.components

import android.net.Uri
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

@Composable
actual fun RtspVideoPlayer(
    url: String,
    modifier: Modifier
) {
    val context = LocalContext.current

    // 1. Configuración de Baja Latencia para LibVLC
    val options = remember {
        arrayListOf(
            "--rtsp-tcp",        // Forzar TCP (evita corrupción de imagen por pérdida de paquetes UDP)
            "--network-caching=300", // 300ms de buffer (equilibrio ideal latencia/estabilidad)
            "--clock-jitter=0",  // Reducir jitter
            "--clock-synchro=0", // Desactivar sincronización de reloj estricta
            "-vvv"               // Verbose logs (útil para debug)
        )
    }

    // 2. Inicialización de recursos (Sobrevive recomposiciones)
    val libVlc = remember { LibVLC(context, options) }
    val mediaPlayer = remember { MediaPlayer(libVlc) }

    // 3. Gestión del Ciclo de Vida
    DisposableEffect(url) {
        val media = Media(libVlc, Uri.parse(url)).apply {
            // Opciones específicas del medio para aceleración por hardware
            addOption(":mediacodec-all")
            addOption(":mediacodec-auto")
        }

        mediaPlayer.media = media
        media.release() // El player ya tiene la referencia

        mediaPlayer.play()

        onDispose {
            mediaPlayer.stop()
            mediaPlayer.release()
            libVlc.release()
        }
    }

    // 4. Renderizado Nativo
    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                // Vinculamos la superficie de video de VLC a esta vista
                val vout = mediaPlayer.vlcVout
                vout.setVideoView(this)
                vout.attachViews()
            }
        },
        modifier = modifier
    )
}