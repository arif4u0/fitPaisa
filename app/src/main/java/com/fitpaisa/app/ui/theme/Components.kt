package com.fitpaisa.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Gold coin logo mark: gold disc, darker inner ring, five-point star. */
@Composable
fun CoinLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = size.toPx() / 2f
        val center = Offset(radius, radius)
        // Outer gold disc
        drawCircle(color = Gold, radius = radius, center = center)
        // Inner ring
        drawCircle(
            color = GoldDark,
            radius = radius * 0.78f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = radius * 0.08f)
        )
        // Star in the middle
        drawPath(
            path = starPath(center, radius * 0.52f, radius * 0.22f),
            color = OnGold
        )
    }
}

private fun starPath(center: Offset, outerR: Float, innerR: Float): Path {
    val path = Path()
    // 5-point star, starting at the top (-90 degrees)
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = Math.toRadians(-90.0 + i * 36.0)
        val x = center.x + (r * cos(angle)).toFloat()
        val y = center.y + (r * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Gold gradient balance card, as in the approved design: gold gradient
 * background, dark-green "Coin Balance" pill, big dark-green coin count,
 * rupee estimate below.
 */
@Composable
fun CoinBalanceCard(
    coins: Int,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.verticalGradient(listOf(GoldLight, Gold))
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Coin Balance",
                color = Gold,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(OnGold)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "${formatNumber(coins)} coins",
                color = OnGold,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "≈ Rs ${formatNumber(coins / 10)}",
                color = OnGold.copy(alpha = 0.75f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** Dark-green info card with a thin gold outline (level / sync cards). */
@Composable
fun OutlineInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
            content = content
        )
    }
}

fun formatNumber(value: Int): String = "%,d".format(Locale.US, value)

/** Draws a gold arc segment; used by the steps progress ring. */
fun DrawScope.goldArcSegment(
    sweepDegrees: Float,
    strokeWidthPx: Float,
    trackColor: Color = RingTrack,
    progressColor: Color = Gold
) {
    drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = strokeWidthPx,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )
    if (sweepDegrees > 0f) {
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepDegrees,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidthPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}
