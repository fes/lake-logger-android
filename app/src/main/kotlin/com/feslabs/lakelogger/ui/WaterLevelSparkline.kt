package com.feslabs.lakelogger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A minimal line chart drawn directly with Canvas, avoiding a third-party
 * charting dependency for a simple "water level over time" trend line.
 */
@Composable
fun WaterLevelSparkline(points: List<Pair<Long, Double>>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.height(180.dp)) {
        if (points.size < 2) return@Canvas

        val minX = points.minOf { it.first }.toFloat()
        val maxX = points.maxOf { it.first }.toFloat()
        val minY = points.minOf { it.second }.toFloat()
        val maxY = points.maxOf { it.second }.toFloat()

        val xRange = (maxX - minX).takeIf { it > 0f } ?: 1f
        val yRange = (maxY - minY).takeIf { it > 0f } ?: 1f

        fun toOffset(point: Pair<Long, Double>): Offset {
            val x = ((point.first - minX) / xRange) * size.width
            // Invert Y since canvas origin is top-left but higher water level should draw higher.
            val y = size.height - ((point.second.toFloat() - minY) / yRange) * size.height
            return Offset(x, y)
        }

        val path = androidx.compose.ui.graphics.Path()
        points.forEachIndexed { index, point ->
            val offset = toOffset(point)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }

        drawPath(path = path, color = lineColor, style = Stroke(width = 4f))
    }
}
