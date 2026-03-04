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
    
    val color = when {
        percentage < 60.0 -> StatusSuccess 
        percentage <= 80.0 -> StatusWarning 
        else -> StatusError 
    }

    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            
            CircularProgressIndicator(
                progress = (percentage / 100.0).toFloat(),
                modifier = Modifier.size(80.dp),
                color = color,
                strokeWidth = 8.dp,
                trackColor = color.copy(alpha = 0.2f)
            )
            
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}