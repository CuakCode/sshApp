package org.cuak.sshapp.ui.components

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

private const val TAG = "RtspPlayerAndroid"

@OptIn(UnstableApi::class)
@Composable
actual fun RtspVideoPlayer(
    url: String,
    modifier: Modifier,
    onStatusChange: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estados de UI
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    DisposableEffect(url) {
        Log.d(TAG, "Iniciando modo Ultra-Low-Latency (No Audio) para: $url")
        onStatusChange("Connecting...")

        // 1. BUFFER AGRESIVO (Ultra Low Latency)
        // Reducimos los tiempos al mínimo absoluto.
        // minBufferMs: Cuánto video necesita para arrancar (50ms es casi instantáneo)
        // maxBufferMs: No queremos que acumule retraso, así que lo limitamos mucho.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50,    // Min buffer (Antes 1000)
                1000,  // Max buffer (Antes 2000)
                50,    // Buffer for playback start
                50     // Buffer for rebuffer
            )
            .build()

        // 2. FUENTE RTSP (TCP Forzado)
        val rtspFactory = RtspMediaSource.Factory()
            .setForceUseRtpTcp(true) // Obligatorio para tu cámara (evita error 461)
            .setTimeoutMs(3000)      // Timeout rápido si no conecta

        val mediaItem = MediaItem.fromUri(url)
        val mediaSource = rtspFactory.createMediaSource(mediaItem)

        // 3. SELECTOR DE PISTAS (LA CLAVE: DESACTIVAR AUDIO)
        // Esto evita que el player se quede colgado esperando audio que nunca llega bien.
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true) // <--- ESTO ES CRÍTICO
            )
        }

        // 4. CREACIÓN DEL PLAYER
        val player = ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector) // Aplicamos el selector sin audio
            .build()
            .apply {
                playWhenReady = true
                setMediaSource(mediaSource)
                prepare()
            }

        exoPlayer = player

        // 5. LISTENER DE EVENTOS
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        onStatusChange("Buffering...")
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        onStatusChange("Online")
                        Log.d(TAG, "Video Arrancado (Latencia mínima)")
                    }
                    Player.STATE_ENDED -> onStatusChange("Ended")
                    Player.STATE_IDLE -> onStatusChange("Idle")
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val cleanMsg = "Error: ${error.errorCodeName}"
                errorMessage = cleanMsg
                onStatusChange(cleanMsg)
                Log.e(TAG, "Error ExoPlayer: ${error.message}", error)
            }
        }
        player.addListener(listener)

        onDispose {
            Log.d(TAG, "Limpiando Player")
            player.removeListener(listener)
            player.release()
            exoPlayer = null
        }
    }

    // GESTIÓN CICLO DE VIDA (Pausar si minimizas la app)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer?.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer?.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // RENDERIZADO UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT // O _ZOOM para llenar pantalla
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setKeepScreenOn(true)
                }
            },
            update = { view ->
                if (view.player != exoPlayer) view.player = exoPlayer
            }
        )

        // Indicador de carga
        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(color = Color.White)
        }

        // Mensaje de Error
        errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp)
                    .align(Alignment.Center)
            )
        }

        // Etiqueta LIVE
        if (!isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Red.copy(alpha = 0.8f), shape = MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}