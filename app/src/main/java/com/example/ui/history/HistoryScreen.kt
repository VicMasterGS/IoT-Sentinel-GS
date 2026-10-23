package com.example.ui.history

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MeasurementEntity
import com.example.ui.main.IoTViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedAlert
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: IoTViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val measurements by viewModel.measurementHistory.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var itemPendingDelete by remember { mutableStateOf<MeasurementEntity?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // Statistical calculations
    val totalCount = measurements.size
    val validTemps = measurements.mapNotNull { it.temperatureC }
    val avgTemp = if (validTemps.isNotEmpty()) String.format(Locale.US, "%.1f", validTemps.average()) else "--"
    val validHums = measurements.mapNotNull { it.humidityPercent }
    val avgHum = if (validHums.isNotEmpty()) String.format(Locale.US, "%.1f", validHums.average()) else "--"

    // 1. Dialog for individual measurement deletion confirmation
    itemPendingDelete?.let { item ->
        val dateFormatFull = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormatFull.format(Date(item.timestamp))

        AlertDialog(
            onDismissRequest = { itemPendingDelete = null },
            title = {
                Text(
                    text = "¿Eliminar medición individual?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = RedAlert
                )
            },
            text = {
                Column {
                    Text(
                        text = "¿Deseas eliminar permanentemente esta medición del registro local?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "• Identificador: Registro #${item.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "• Fecha/Hora: $formattedDate", fontSize = 11.5.sp)
                            Text(text = "• Nodo: ${item.deviceId} (${if (item.source == "REAL") "Origen Real" else "Simulado"})", fontSize = 11.5.sp)
                            Text(text = "• Temperatura: ${item.temperatureC?.let { "$it °C" } ?: "N/D"}", fontSize = 11.5.sp)
                            Text(text = "• Humedad: ${item.humidityPercent?.let { "$it %" } ?: "N/D"}", fontSize = 11.5.sp)
                            Text(text = "• MQ-135 Raw: ${item.mq135Raw ?: "N/D"} (${item.airQualityStatus})", fontSize = 11.5.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta acción eliminará únicamente este registro de la base de datos local Room. Los demás registros y configuraciones permanecerán intactos.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idToDelete = item.id
                        viewModel.deleteMeasurement(
                            id = idToDelete,
                            onSuccess = {
                                Toast.makeText(context, "Medición #$idToDelete eliminada", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                        itemPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                    modifier = Modifier.testTag("confirm_delete_single_button")
                ) {
                    Text("Eliminar Registro", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemPendingDelete = null },
                    modifier = Modifier.testTag("cancel_delete_single_button")
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 2. Dialog for clearing ALL history
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = {
                Text(
                    text = "¿Borrar todo el historial?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = RedAlert
                )
            },
            text = {
                Column {
                    Text(
                        text = "Esta acción eliminará permanentemente todas las $totalCount mediciones registradas en la base de datos local Room.",
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Las cuentas de usuario y los ajustes de alertas se conservarán intactos.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearDatabaseHistory()
                        showClearAllConfirmDialog = false
                        Toast.makeText(context, "Historial de mediciones vaciado", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                    modifier = Modifier.testTag("confirm_clear_all_button")
                ) {
                    Text("Eliminar Todo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAllConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_clear_all_button")
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Historial Local",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent
            )
            Text(
                text = "Base de datos Room offline (sin Internet)",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons Row (Adaptive: Both fit in portrait & landscape without horizontal clipping)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Borrar Todo button (out of screen issue fixed with adaptive weight)
            OutlinedButton(
                onClick = { showClearAllConfirmDialog = true },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, RedAlert.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = RedAlert
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Borrar historial",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Exportar CSV button
            Button(
                onClick = {
                    scope.launch {
                        val csvContent = viewModel.sensorRepository.exportToCsv()
                        shareCsvIntent(context, csvContent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .testTag("export_csv_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Exportar CSV",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(title = "Total Registros", value = totalCount.toString(), color = CyanAccent, modifier = Modifier.weight(1f))
            StatCard(title = "Temp Prom.", value = "$avgTemp °C", color = EmeraldAccent, modifier = Modifier.weight(1f))
            StatCard(title = "Humedad Prom.", value = "$avgHum %", color = AmberWarning, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Source Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FilterList, contentDescription = "Filtro", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            listOf("TODOS", "REAL", "SIMULATED").forEach { source ->
                FilterChip(
                    selected = uiState.historyFilterSource == source,
                    onClick = { viewModel.setHistoryFilter(source) },
                    label = {
                        Text(
                            text = when (source) {
                                "TODOS" -> "Todos"
                                "REAL" -> "Reales"
                                else -> "Simulados"
                            },
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent.copy(alpha = 0.2f),
                        selectedLabelColor = CyanAccent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Measurements List (Adaptive height, scrollable, with generous bottom padding for navigation bar)
        if (measurements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No hay mediciones en el historial",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Inicia la simulación o conecta un nodo para comenzar a almacenar.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(measurements, key = { it.id }) { item ->
                    MeasurementItemCard(
                        item = item,
                        onDeleteClick = { selectedItem ->
                            itemPendingDelete = selectedItem
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun MeasurementItemCard(
    item: MeasurementEntity,
    onDeleteClick: (MeasurementEntity) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.timestamp))
    val isReal = item.source == "REAL"

    val statusColor = when (item.airQualityStatus) {
        "ALERTA" -> RedAlert
        "PRECAUCIÓN" -> AmberWarning
        else -> EmeraldAccent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isReal) EmeraldAccent.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isReal) "REAL" else "SIM",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReal) EmeraldAccent else AmberWarning
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "#${item.id} • ${item.deviceId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Temp: ${item.temperatureC?.let { "${it}°C" } ?: "--"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent
                    )
                    Text(
                        text = "Hum: ${item.humidityPercent?.let { "${it}%" } ?: "--"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent
                    )
                    Text(
                        text = "MQ: ${item.mq135Raw ?: "--"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Air Quality Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.airQualityStatus,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Individual Delete Icon Button
                IconButton(
                    onClick = { onDeleteClick(item) },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_item_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Eliminar registro #${item.id}",
                        tint = RedAlert.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun shareCsvIntent(context: Context, csvContent: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, csvContent)
        putExtra(Intent.EXTRA_SUBJECT, "IoT Sentinel - Registro de Mediciones Ambientales")
        type = "text/csv"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Exportar historial CSV")
    context.startActivity(shareIntent)
}
