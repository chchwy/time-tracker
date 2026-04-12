package com.mattchang.timetracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class PieSlice(
    val color: Color,
    val fraction: Float,
    val label: String
)

/**
 * Animating donut/ring chart.
 * Renders each slice as an arc; the center is transparent (donut).
 */
@Composable
fun DonutChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 32.dp,
    gapDegrees: Float = 2f,
    trackColor: Color = Color.Transparent
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 900))
    }

    val emptyColor = trackColor
    val p = progress.value

    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val diameterPadding = strokePx / 2f
        val arcSize = Size(
            size.width - strokePx,
            size.height - strokePx
        )
        val topLeft = Offset(diameterPadding, diameterPadding)

        if (slices.isEmpty()) {
            // Draw empty ring
            drawArc(
                color = emptyColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = (slice.fraction * 360f - gapDegrees).coerceAtLeast(0f) * p
            if (sweep > 0f) {
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
            startAngle += slice.fraction * 360f
        }
    }
}
