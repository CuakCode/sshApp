package org.cuak.sshapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.SwingUtilities

@Composable
actual fun RtspVideoPlayer(
    url: String,
    modifier: Modifier
) {
    println("[RTSP DEBUG] Composable iniciado. URL objetivo: $url")

    val vlcFound = remember { NativeDiscovery().discover() }

    if (!vlcFound) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("VLC no encontrado. Instala VLC Media Player en tu sistema.")
        }
        return
    }

    val mediaPlayerComponent = remember {
        object : EmbeddedMediaPlayerComponent() {}
    }

    DisposableEffect(url) {
        val player = mediaPlayerComponent.mediaPlayer()

        // Debug events
        val eventAdapter = object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer?) {
                println("[RTSP DEBUG] VLC Event: PLAYING")
            }
            override fun error(mediaPlayer: MediaPlayer?) {
                println("[RTSP DEBUG] VLC Event: ERROR")
            }
        }
        player.events().addMediaPlayerEventListener(eventAdapter)

        val options = arrayOf(
            ":rtsp-tcp",
            ":network-caching=300",
            ":clock-jitter=0",
            ":clock-synchro=0",
            "-vvv"
        )

        var isPlayingAttempted = false

        val hierarchyListener = object : HierarchyListener {
            override fun hierarchyChanged(e: HierarchyEvent) {
                if ((e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong()) != 0L) {
                    SwingUtilities.invokeLater {
                        if (mediaPlayerComponent.isDisplayable && !isPlayingAttempted) {
                            isPlayingAttempted = true
                            println("[RTSP DEBUG] Play (Hierarchy)")
                            player.media().play(url, *options)
                        }
                    }
                }
            }
        }

        mediaPlayerComponent.addHierarchyListener(hierarchyListener)

        SwingUtilities.invokeLater {
            if (mediaPlayerComponent.isDisplayable && !isPlayingAttempted) {
                isPlayingAttempted = true
                println("[RTSP DEBUG] Play (Immediate)")
                player.media().play(url, *options)
            }
        }

        onDispose {
            println("[RTSP DEBUG] Limpiando recursos...")
            mediaPlayerComponent.removeHierarchyListener(hierarchyListener)
            player.events().removeMediaPlayerEventListener(eventAdapter)

            SwingUtilities.invokeLater {
                try {
                    // 1. Detenemos la reproducción
                    if (player.status().isPlaying) {
                        player.controls().stop()
                    }

                    // 2. CRUCIAL: Solo liberamos el componente.
                    // NO llamar a player.release() aquí, el componente lo hace internamente.
                    mediaPlayerComponent.release()

                    println("[RTSP DEBUG] Componente liberado correctamente.")
                } catch (e: Exception) {
                    println("[RTSP DEBUG] Excepción al liberar: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    SwingPanel(
        background = Color.Black,
        modifier = modifier.fillMaxSize(),
        factory = { mediaPlayerComponent }
    )
}