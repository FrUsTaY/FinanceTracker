package com.emix.financetracker.ui.screens.inflation

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InflationScreen(
    canonicalName: String,
    unit: String,
    onBack: () -> Unit,
    viewModel: InflationViewModel = hiltViewModel()
) {
    val priceHistory by viewModel.priceHistory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$canonicalName ($unit)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (priceHistory.size) {
                0, 1 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Недостаточно данных.\nКупите ещё раз, чтобы увидеть динамику.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (priceHistory.size == 1) {
                            Text(
                                text = "Текущая цена: ${priceHistory[0].price} ₽",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val minPrice = priceHistory.minOf { it.price }
                        val maxPrice = priceHistory.maxOf { it.price }
                        val firstPrice = priceHistory.first().price
                        val lastPrice = priceHistory.last().price
                        val change = ((lastPrice - firstPrice) / firstPrice) * 100
                        val changeColor = if (change <= 0) Color.Green else Color.Red

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Text("Мин: $minPrice ₽")
                                Text("Макс: $maxPrice ₽")
                                Text(
                                    text = "Изм: ${String.format("%.1f", change)}%",
                                    color = changeColor
                                )
                            }
                        }

                        EnhancedLineChart(
                            data = priceHistory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedLineChart(
    data: List<PricePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = Color.Gray.copy(alpha = 0.3f),
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    if (data.size < 2) return

    val density = LocalDensity.current
    val textPaint = remember(density) {
        Paint().apply {
            color = textColor.toArgb()
            textSize = density.run { 10.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 48f
        val paddingRight = 48f
        val paddingTop = 24f
        val paddingBottom = 40f
        val graphWidth = width - paddingLeft - paddingRight
        val graphHeight = height - paddingTop - paddingBottom

        if (graphWidth <= 0 || graphHeight <= 0) return@Canvas

        val prices = data.map { it.price }
        val maxValue = prices.maxOrNull()?.toFloat() ?: 1f
        val minValue = prices.minOrNull()?.toFloat() ?: 0f
        val valueRange = if (maxValue == minValue) 1f else maxValue - minValue

        for (i in 0..4) {
            val y = paddingTop + (i * graphHeight / 4)
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
            val value = maxValue - (i * valueRange / 4)
            drawContext.canvas.nativeCanvas.drawText(
                "${value.roundToInt()}",
                paddingLeft - 8,
                y + 4,
                textPaint
            )
        }

        val step = graphWidth / (data.size - 1)
        for (i in data.indices) {
            val x = paddingLeft + i * step
            drawLine(
                color = gridColor,
                start = Offset(x, paddingTop),
                end = Offset(x, height - paddingBottom),
                strokeWidth = 0.5f
            )
        }

        val points = data.mapIndexed { index, point ->
            val x = paddingLeft + index * step
            val y = paddingTop + graphHeight - ((point.price.toFloat() - minValue) / valueRange * graphHeight)
            Offset(x, y.coerceIn(paddingTop, paddingTop + graphHeight))
        }

        val smoothPoints = catmullRomSpline(points, 20)
        for (i in 0 until smoothPoints.size - 1) {
            drawLine(
                color = lineColor,
                start = smoothPoints[i],
                end = smoothPoints[i + 1],
                strokeWidth = 3f
            )
        }

        for ((index, point) in points.withIndex()) {
            drawCircle(
                color = lineColor,
                radius = 6f,
                center = point
            )
            val dateText = data[index].date.substring(5)
            drawContext.canvas.nativeCanvas.drawText(
                dateText,
                point.x,
                height - paddingBottom + 16,
                textPaint
            )
        }
    }
}

private fun catmullRomSpline(points: List<Offset>, segmentsPerSegment: Int = 10): List<Offset> {
    if (points.size < 2) return points
    val result = mutableListOf<Offset>()
    for (i in 0 until points.size - 1) {
        val p0 = if (i == 0) points[i] else points[i - 1]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = if (i + 2 < points.size) points[i + 2] else points[i + 1]
        for (t in 0..segmentsPerSegment) {
            val tNorm = t.toFloat() / segmentsPerSegment
            val t2 = tNorm * tNorm
            val t3 = t2 * tNorm
            val x = 0.5f * ((2 * p1.x) +
                    (-p0.x + p2.x) * tNorm +
                    (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
                    (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3)
            val y = 0.5f * ((2 * p1.y) +
                    (-p0.y + p2.y) * tNorm +
                    (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
                    (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3)
            result.add(Offset(x, y))
        }
    }
    return result
}