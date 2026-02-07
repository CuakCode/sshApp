package org.cuak.sshapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun RtspVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    // Opcional: callback para errores o estado
    onStatusChange: (String) -> Unit = {}
)