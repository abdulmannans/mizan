package app.mizan.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.mizan.android.core.Formatters
import app.mizan.android.domain.PricePoint
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ChartRange(val label: String, val days: Long?) {
    MONTH("1M", 30),
    HALF_YEAR("6M", 182),
    YEAR("1Y", 365),
    THREE_YEARS("3Y", 1095),
    ALL("All", null),
}

/**
 * NAV / score chart you can read: drag across it and the header reports the value on that exact
 * day, which is the question a chart is usually being asked.
 */
@Composable
fun LineChart(
    points: List<PricePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
    height: Int = 200,
    valueFormatter: (Double) -> String = { Formatters.nav(it) },
    fixedRange: ClosedFloatingPointRange<Float>? = null,
    showRanges: Boolean = true,
) {
    if (points.size < 2) {
        EmptyState("Not enough history yet.", modifier)
        return
    }

    val ranges = remember(points) { ChartRange.entries.filter { it.isUsefulFor(points) } }
    var range by remember(points) {
        mutableStateOf(if (ChartRange.YEAR in ranges) ChartRange.YEAR else ranges.last())
    }
    val window = remember(points, range) { range.windowOf(points) }
    val values = remember(window) { window.map { it.value.toFloat() } }

    var touched by remember { mutableStateOf<Int?>(null) }
    val index = touched?.coerceIn(0, window.lastIndex)
    val shown = index?.let { window[it] } ?: window.last()

    val opening = window.first().value
    val changePercent = if (opening > 0.0) (shown.value / opening - 1.0) * 100.0 else null
    val trendColor = lineColor ?: when {
        changePercent == null || changePercent >= 0.0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val markerFill = MaterialTheme.colorScheme.surface
    val haptics = LocalHapticFeedback.current

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                valueFormatter(shown.value),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                Formatters.percentSigned(changePercent),
                style = MaterialTheme.typography.bodySmall,
                color = trendColor,
            )
        }
        Text(
            if (index != null) {
                Formatters.date(shown.date)
            } else {
                "Drag across the chart to read any day"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(10.dp))

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .pointerInput(window) {
                    awaitEachGesture {
                        fun indexAt(x: Float): Int =
                            ((x / size.width.toFloat()) * window.lastIndex)
                                .roundToInt()
                                .coerceIn(0, window.lastIndex)

                        val down = awaitFirstDown(requireUnconsumed = false)
                        touched = indexAt(down.position.x)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed || change.isConsumed) break

                            touched = indexAt(change.position.x)
                            val dx = abs(change.position.x - down.position.x)
                            val dy = abs(change.position.y - down.position.y)
                            // Claim the gesture only once it is clearly horizontal, so the page
                            // underneath can still be scrolled vertically.
                            if (dx > dy && dx > viewConfiguration.touchSlop) change.consume()
                        }
                        touched = null
                    }
                }
        ) {
            val minValue = fixedRange?.start ?: values.min()
            val maxValue = fixedRange?.endInclusive ?: values.max()
            val span = (maxValue - minValue).takeIf { it > 0f } ?: 1f
            val stepX = size.width / window.lastIndex.toFloat()
            fun yFor(value: Float) = size.height - ((value - minValue) / span) * size.height

            val linePath = Path()
            val fillPath = Path()
            values.forEachIndexed { position, value ->
                val x = position * stepX
                val y = yFor(value)
                if (position == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    listOf(trendColor.copy(alpha = 0.18f), Color.Transparent),
                ),
            )
            drawPath(linePath, color = trendColor, style = Stroke(width = 2.dp.toPx()))

            drawLine(
                color = gridColor,
                start = Offset(0f, yFor(values.first())),
                end = Offset(size.width, yFor(values.first())),
                strokeWidth = 1.dp.toPx() / 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )

            if (index != null) {
                val x = index * stepX
                val y = yFor(values[index])
                drawLine(
                    color = trendColor.copy(alpha = 0.55f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
                drawCircle(markerFill, radius = 5.dp.toPx(), center = Offset(x, y))
                drawCircle(
                    trendColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${Formatters.shortDate(window.first().date)} · low ${valueFormatter(values.min().toDouble())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "high ${valueFormatter(values.max().toDouble())} · ${Formatters.shortDate(window.last().date)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showRanges && ranges.size > 1) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ranges.forEach { option ->
                    val selected = option == range
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.clickable {
                            range = option
                            touched = null
                        },
                    ) {
                        Text(
                            option.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun ChartRange.windowOf(points: List<PricePoint>): List<PricePoint> {
    val days = days ?: return points
    val cutoff = points.last().date.minusDays(days)
    return points.filter { !it.date.isBefore(cutoff) }.takeIf { it.size >= 2 } ?: points.takeLast(2)
}

/** A range is only offered when the history actually reaches past it. */
private fun ChartRange.isUsefulFor(points: List<PricePoint>): Boolean {
    val days = days ?: return true
    val cutoff = points.last().date.minusDays(days)
    return points.first().date.isBefore(cutoff) && points.count { !it.date.isBefore(cutoff) } >= 2
}
