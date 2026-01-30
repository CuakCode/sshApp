package org.cuak.sshapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.cuak.sshapp.models.Server
import org.cuak.sshapp.models.ServerStatus
import org.cuak.sshapp.ui.theme.*
import org.jetbrains.compose.ui.tooling.preview.Preview


// ui/components/ServerCard.kt
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerCard(
    server: Server,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val statusColor = if (server.status == ServerStatus.ONLINE) StatusSuccess else StatusError
    val icon = getIconByName(server.iconName)

    ElevatedCard(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .height(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        )
        {

            Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor
                )

            Text(
                text = server.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, // Centra el texto si ocupa varias líneas
                maxLines = 3, // Permite hasta 2 líneas (puedes aumentarlo o quitarlo)
                overflow = TextOverflow.Ellipsis // Añade "..." si el nombre es extremadamente largo
            )
            Text(
                text = server.ip,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

}
fun getIconByName(name: String): ImageVector {
    // Usamos lowercase para evitar problemas si se guarda en mayúsculas por error
    return when (name.lowercase()) {
        // --- Servidores / Redes ---
        "dns" -> Icons.Default.Dns
        "computer" -> Icons.Default.Computer
        "router" -> Icons.Default.Router
        "cloud" -> Icons.Default.Cloud
        "storage" -> Icons.Default.Storage
        "memory" -> Icons.Default.Memory

        // --- Cámaras / Video / Seguridad ---
        "videocam" -> Icons.Default.Videocam
        "camera_alt" -> Icons.Default.CameraAlt
        "security" -> Icons.Default.Security
        "cast_connected" -> Icons.Default.CastConnected

        // --- Otros ---
        "smart_toy" -> Icons.Default.SmartToy

        // Fallback por defecto si el nombre no existe o es antiguo
        else -> Icons.Default.Dns
    }
}