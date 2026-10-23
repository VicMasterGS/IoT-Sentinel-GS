package com.example.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.main.ChartPoint

@Composable
fun SensorChart(
    points: List<ChartPoint>,
    lineColor: Color,
    unit: String,
    modifier: Modifier = Modifier,
    minY: Float? = null,
    maxY: Float? = null
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Recibiendo primeras muestras...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        return
    }

    val values = points.map { it.value }
    val calculatedMin = minY ?: (values.minOrNull()?.let { (it - 2).coerceAtLeast(0f) } ?: 0f)
    val calculatedMax = maxY ?: (values.maxOrNull()?.let { it + 2 } ?: 100f)
    val range = if (calculatedMax - calculatedMin > 0.01f) calculatedMax - calculatedMin else 1f

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 3 Horizontal Grid lines
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (i in 0..2) {
                val y = height * (i / 2f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                    pathEffect = dashEffect
                )
            }

            if (points.size >= 2) {
                val strokePath = Path()
                val fillPath = Path()

                val stepX = width / (points.size - 1)

                var prevX = 0f
                var prevY = height - ((points[0].value - calculatedMin) / range * height)

                strokePath.moveTo(prevX, prevY)
                fillPath.moveTo(prevX, height)
                fillPath.lineTo(prevX, prevY)

                for (i in 1 until points.size) {
                    val currX = i * stepX
                    val currY = (height - ((points[i].value - calculatedMin) / range * height)).coerceIn(0f, height)

                    val midX = (prevX + currX) / 2f
                    strokePath.cubicTo(midX, prevY, midX, currY, currX, currY)
                    fillPath.cubicTo(midX, prevY, midX, currY, currX, currY)

                    prevX = currX
                    prevY = currY
                }

                fillPath.lineTo(prevX, height)
                fillPath.close()

                // Draw Gradient Fill
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.35f),
                            lineColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw Line Stroke
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight latest point
                drawCircle(
                    color = lineColor,
                    radius = 5.dp.toPx(),
                    center = Offset(prevX, prevY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(prevX, prevY)
                )
            }
        }

        // Overlay Labels for min and max
        Text(
            text = "${calculatedMax.toInt()} $unit",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Text(
            text = "${calculatedMin.toInt()} $unit",
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
