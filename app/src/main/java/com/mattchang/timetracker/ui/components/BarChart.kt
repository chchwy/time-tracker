package com.mattchang.timetracker.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattchang.timetracker.ui.analytics.DailyBarEntry

/**
 * Animated vertical bar chart.
 * Each bar represents a [DailyBarEntry]; today is highlighted with a different color.
 */
@Composable
fun BarChart(
    entries: List<DailyBarEntry>,
    barColor: Color,
    todayColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    barCornerRadius: Dp = 4.dp,
    barSpacingFraction: Float = 0.3f   // fraction of bar width used as gap
) {
    val progress = remember(entries) { Animatable(0f) }
    LaunchedEffect(entries) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    val textMeasurer = rememberTextMeasurer()
    val p = progress.value
    val maxMinutes = (entries.maxOfOrNull { it.totalMinutes } ?: 0).coerceAtLeast(1)
    val labelStyle = TextStyle(fontSize = 9.sp, color = labelColor)

    Canvas(modifier = modifier) {
        if (entries.isEmpty()) return@Canvas

        val labelAreaHeight = 20.dp.toPx()
        val chartHeight = size.height - labelAreaHeight
        val totalWidth = size.width
        val n = entries.size
        val slotWidth = totalWidth / n
        val barWidth = slotWidth * (1f - barSpacingFraction)
        val cornerPx = barCornerRadius.toPx()

        entries.forEachIndexed { i, entry ->
            val barHeightFraction = entry.totalMinutes.toFloat() / maxMinutes
            val barHeight = (chartHeight * barHeightFraction * p).coerceAtLeast(if (entry.totalMinutes > 0) cornerPx * 2 else 0f)
            val left = i * slotWidth + (slotWidth - barWidth) / 2f
            val top = chartHeight - barHeight

            if (barHeight > 0f) {
                drawRoundRect(
                    color = if (entry.isToday) todayColor else barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            }

            // Label
            if (entry.label.isNotEmpty()) {
                drawLabel(textMeasurer, entry.label, labelStyle, left + slotWidth / 2f - (slotWidth - barWidth) / 2f, chartHeight + 4.dp.toPx(), barWidth)
            }
        }
    }
}

private fun DrawScope.drawLabel(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    centerX: Float,
    y: Float,
    maxWidth: Float
) {
    val measured = measurer.measure(text, style)
    val x = (centerX - measured.size.width / 2f).coerceAtLeast(0f)
    drawText(measured, topLeft = Offset(x, y))
}
