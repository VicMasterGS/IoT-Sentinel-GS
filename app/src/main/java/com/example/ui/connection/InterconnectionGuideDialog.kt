package com.example.ui.connection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldAccent

@Composable
fun InterconnectionGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Guía de Prueba de Interconexión — CE 2.1.2 / 2.1.4",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = CyanAccent
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Sigue estos pasos para verificar la comunicación inalámbrica local real entre dos teléfonos o teléfono + ESP32:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideStepCard(
                    step = "Paso 1",
                    title = "Configurar Teléfono A (Estación)",
                    body = "1. Selecciona Rol: 'Estación de Monitoreo'.\n2. Elige Transporte: 'Wi-Fi TCP' o 'Bluetooth RFCOMM'.\n3. Si usas Wi-Fi: pulsa 'Iniciar Servidor TCP (Escuchar)'. Anota la IP mostrada en pantalla."
                )

                Spacer(modifier = Modifier.height(8.dp))

                GuideStepCard(
                    step = "Paso 2",
                    title = "Configurar Teléfono B (Nodo Sensor)",
                    body = "1. Selecciona Rol: 'Nodo Sensor'.\n2. En Transporte, elige el mismo que en el Teléfono A.\n3. Si usas Wi-Fi: escribe la IP del Teléfono A y pulsa 'Conectar a Estación'. Si usas Bluetooth: selecciona el Teléfono A de la lista de emparejados."
                )

                Spacer(modifier = Modifier.height(8.dp))

                GuideStepCard(
                    step = "Paso 3",
                    title = "Verificar Recepción en Vivo",
                    body = "El Nodo Sensor comenzará a emitir tramas JSON cada 1.5s. La Estación recibirá y actualizará las tarjetas DHT11 y MQ-135 en tiempo real de forma automática, sin pulsar ningún botón de recarga."
                )

                Spacer(modifier = Modifier.height(8.dp))

                GuideStepCard(
                    step = "Paso 4",
                    title = "Prueba de Tolerancia y Desconexión",
                    body = "Desconecta el Teléfono B o apaga su conexión. Tras 6 segundos sin paquetes entrantes, la Estación detectará automáticamente la pérdida de señal y cambiará su estado a 'CONEXIÓN PERDIDA'."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                modifier = Modifier.testTag("dismiss_guide_button")
            ) {
                Text("Entendido", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun GuideStepCard(step: String, title: String, body: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "$step: $title",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}
