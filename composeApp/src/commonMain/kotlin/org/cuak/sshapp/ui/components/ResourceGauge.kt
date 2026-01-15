package org.cuak.sshapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.cuak.sshapp.ui.theme.*


@Composable
fun ResourceGauge(
    label: String,
    percentage: Double,
    modifier: Modifier = Modifier
) {
    // Definición de colores según los umbrales solicitados
    val color = when {
        percentage < 60.0 -> StatusSuccess // Verde: Menos del 60%
        percentage <= 80.0 -> StatusWarning // Amarillo: Entre 60% y 80%
        else -> StatusError // Rojo: Superior al 80%
    }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Indicador circular de progreso
            CircularProgressIndicator(
                progress = (percentage / 100.0).toFloat(),
                modifier = Modifier.size(80.dp),
                color = color,
                strokeWidth = 8.dp,
                trackColor = color.copy(alpha = 0.2f)
            )
            // Texto con el porcentaje centrado
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Etiqueta del recurso (CPU, RAM, etc.)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}