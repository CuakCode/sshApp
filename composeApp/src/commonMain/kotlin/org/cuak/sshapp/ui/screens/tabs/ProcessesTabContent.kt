package org.cuak.sshapp.ui.screens.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cuak.sshapp.models.ProcessInfo
import org.cuak.sshapp.models.ProcessSortOption
import org.cuak.sshapp.ui.theme.StatusError
import org.cuak.sshapp.ui.theme.StatusSuccess
import org.cuak.sshapp.ui.theme.StatusWarning
import org.jetbrains.compose.resources.stringResource
import sshapp.composeapp.generated.resources.Res
import sshapp.composeapp.generated.resources.processes_tab_col_cpu
import sshapp.composeapp.generated.resources.processes_tab_col_mem
import sshapp.composeapp.generated.resources.processes_tab_col_name
import sshapp.composeapp.generated.resources.processes_tab_col_pid
import sshapp.composeapp.generated.resources.processes_tab_refresh_desc
import sshapp.composeapp.generated.resources.processes_tab_total
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProcessesTabContent(
    processes: List<ProcessInfo>,
    isLoading: Boolean,
    sortOption: ProcessSortOption,
    onSort: (ProcessSortOption) -> Unit,
    onRefresh: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.processes_tab_total, processes.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, stringResource(Res.string.processes_tab_refresh_desc))
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            
            if (isLoading && processes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    
                    stickyHeader {
                        ProcessHeaderRow(sortOption, onSort)
                    }

                    items(processes, key = { it.pid }) { process ->
                        ProcessRow(process)
                        Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProcessHeaderRow(
    currentSort: ProcessSortOption,
    onSort: (ProcessSortOption) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp) 
            .background(MaterialTheme.colorScheme.surface) 
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        
        HeaderCell(
            text = stringResource(Res.string.processes_tab_col_pid),
            weight = 0.15f,
            align = Alignment.CenterStart,
            isSelected = currentSort == ProcessSortOption.PID,
            onClick = { onSort(ProcessSortOption.PID) }
        )
        
        HeaderCell(
            text = stringResource(Res.string.processes_tab_col_name),
            weight = 0.45f,
            align = Alignment.CenterStart,
            isSelected = currentSort == ProcessSortOption.NAME,
            onClick = { onSort(ProcessSortOption.NAME) }
        )
        
        HeaderCell(
            text = stringResource(Res.string.processes_tab_col_cpu),
            weight = 0.2f,
            align = Alignment.CenterEnd,
            isSelected = currentSort == ProcessSortOption.CPU,
            onClick = { onSort(ProcessSortOption.CPU) }
        )
        
        HeaderCell(
            text = stringResource(Res.string.processes_tab_col_mem),
            weight = 0.2f,
            align = Alignment.CenterEnd,
            isSelected = currentSort == ProcessSortOption.MEM,
            onClick = { onSort(ProcessSortOption.MEM) }
        )
    }
    Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.primaryContainer)
}

@Composable
fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    align: Alignment,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    
    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxHeight()
            .clickable(onClick = onClick) 
            .padding(vertical = 4.dp),
        contentAlignment = align
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ProcessRow(proc: ProcessInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        Text(
            text = proc.pid,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(0.15f),
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Start 
        )

        
        Column(modifier = Modifier.weight(0.45f)) {
            Text(
                text = proc.command.split(" ").first(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = proc.user,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }

        
        Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.CenterEnd) {
            UsageBadge(proc.cpuUsage, thresholdWarning = 20.0, thresholdError = 50.0)
        }

        
        Box(modifier = Modifier.weight(0.2f), contentAlignment = Alignment.CenterEnd) {
            UsageBadge(proc.memUsage, thresholdWarning = 20.0, thresholdError = 50.0)
        }
    }
}

@Composable
fun UsageBadge(value: Double, thresholdWarning: Double, thresholdError: Double) {
    val color = when {
        value >= thresholdError -> StatusError
        value >= thresholdWarning -> StatusWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isHigh = value >= thresholdWarning

    
    val displayText = "${formatDecimal(value, 2)}%"

    if (isHigh) {
        
        Surface(
            color = color.copy(alpha = 0.15f),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
        ) {
            Text(
                text = displayText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium,
            color = color.copy(alpha = 0.8f),
            
            fontFamily = FontFamily.Monospace
        )
    }
}


fun formatDecimal(value: Double, decimals: Int): String {
    val factor = 10.0.pow(decimals)
    val rounded = (value * factor).roundToInt() / factor
    val string = rounded.toString()

    
    val parts = string.split(".")
    val integerPart = parts[0]
    val fractionalPart = if (parts.size > 1) parts[1] else ""

    val paddedFraction = fractionalPart.padEnd(decimals, '0')
    return "$integerPart.$paddedFraction"
}