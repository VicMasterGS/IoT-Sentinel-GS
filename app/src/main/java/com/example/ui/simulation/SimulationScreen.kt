package com.example.ui.simulation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.main.IoTViewModel
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedAlert
import kotlin.math.roundToInt

@Composable
fun SimulationScreen(
    viewModel: IoTViewModel,
    modifier: Modifier = Modifier
) {
    val simConfig by viewModel.sensorSimulator.config.collectAsState()
    val simReading by viewModel.sensorSimulator.currentReading.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Controls Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("simulation_control_panel"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (simReading.isRunning) EmeraldAccent else AmberWarning)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (simReading.isRunning) "SIMULACIÓN ACTIVA" else "SIMULACIÓN DETENIDA",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (simReading.isRunning) EmeraldAccent else AmberWarning
                        )
                    }

                    Text(
                        text = "100% Autónomo",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Control Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!simReading.isRunning) {
                        Button(
                            onClick = { viewModel.startSimulation() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("start_simulation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Iniciar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.pauseSimulation() },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("pause_simulation_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pausar", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetSimulation() },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("reset_simulation_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reiniciar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Live Simulated Values Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Valores Generados en Tiempo Real (Tick 1.5s)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "DHT11 Temp: ${simReading.temperatureC?.let { "${it}°C" } ?: "Desactivado"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Humedad: ${simReading.humidityPercent?.let { "${it}%" } ?: "Desactivado"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "MQ-135 Raw: ${simReading.mq135Raw ?: "Desactivado"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Estado: ${simReading.airQualityStatus}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (simReading.airQualityStatus) {
                            "ALERTA" -> RedAlert
                            "PRECAUCIÓN" -> AmberWarning
                            else -> EmeraldAccent
                        }
                    )
                }
            }
        }

        // DHT11 Simulation Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Thermostat, contentDescription = null, tint = CyanAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simular Sensor DHT11",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = simConfig.simulateDht11,
                        onCheckedChange = { checked ->
                            viewModel.updateSimulationConfig { it.copy(simulateDht11 = checked) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent),
                        modifier = Modifier.testTag("switch_simulate_dht11")
                    )
                }

                if (simConfig.simulateDht11) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Base Temperature Slider
                    Text(
                        text = "Temperatura Base: ${simConfig.baseTemperature.roundToInt()} °C",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = simConfig.baseTemperature.toFloat(),
                        onValueChange = { value ->
                            viewModel.updateSimulationConfig { it.copy(baseTemperature = value.toDouble()) }
                        },
                        valueRange = 0f..50f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                        modifier = Modifier.testTag("slider_base_temp")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Base Humidity Slider
                    Text(
                        text = "Humedad Base: ${simConfig.baseHumidity.roundToInt()} %",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = simConfig.baseHumidity.toFloat(),
                        onValueChange = { value ->
                            viewModel.updateSimulationConfig { it.copy(baseHumidity = value.toDouble()) }
                        },
                        valueRange = 10f..95f,
                        colors = SliderDefaults.colors(thumbColor = EmeraldAccent, activeTrackColor = EmeraldAccent),
                        modifier = Modifier.testTag("slider_base_humidity")
                    )
                }
            }
        }

        // MQ-135 Simulation Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Air, contentDescription = null, tint = AmberWarning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Simular Sensor MQ-135",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = simConfig.simulateMq135,
                        onCheckedChange = { checked ->
                            viewModel.updateSimulationConfig { it.copy(simulateMq135 = checked) }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AmberWarning),
                        modifier = Modifier.testTag("switch_simulate_mq135")
                    )
                }

                if (simConfig.simulateMq135) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // Base MQ-135 Raw Slider
                    Text(
                        text = "Lectura Analógica Base: ${simConfig.baseMq135Raw} ADC",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = simConfig.baseMq135Raw.toFloat(),
                        onValueChange = { value ->
                            viewModel.updateSimulationConfig { it.copy(baseMq135Raw = value.toInt()) }
                        },
                        valueRange = 100f..1200f,
                        colors = SliderDefaults.colors(thumbColor = AmberWarning, activeTrackColor = AmberWarning),
                        modifier = Modifier.testTag("slider_base_mq135")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulate Spikes Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Picos Ocasionales de Contaminación",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Simula fugas o humo transitorio (dispara alerta)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = simConfig.simulateSpikes,
                            onCheckedChange = { checked ->
                                viewModel.updateSimulationConfig { it.copy(simulateSpikes = checked) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = RedAlert),
                            modifier = Modifier.testTag("switch_simulate_spikes")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
