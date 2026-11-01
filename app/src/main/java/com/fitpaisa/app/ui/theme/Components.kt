package com.fitpaisa.app.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium 3D-styled Gold Coin logo mark.
 */
@Composable
fun CoinLogo(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = size.toPx() / 2f
        val center = Offset(radius, radius)

        // Outer ambient glow ring
        drawCircle(
            color = GoldGlow,
            radius = radius,
            center = center
        )

        // Outer gold disc with gradient look
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(GoldLight, Gold, GoldDark),
                center = center,
                radius = radius * 0.95f
            ),
            radius = radius * 0.92f,
            center = center
        )

        // Inner rim
        drawCircle(
            color = OnGold.copy(alpha = 0.35f),
            radius = radius * 0.72f,
            center = center,
            style = Stroke(width = radius * 0.08f)
        )

        // Center 5-point star
        drawPath(
            path = starPath(center, radius * 0.46f, radius * 0.20f),
            color = OnGold
        )
    }
}

private fun starPath(center: Offset, outerR: Float, innerR: Float): Path {
    val path = Path()
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
 * Modern luxury Gold Gradient Balance Card.
 * High-gloss gradient, currency equivalency chip, watermark coin, and sleek typography.
 */
@Composable
fun CoinBalanceCard(
    coins: Int,
    modifier: Modifier = Modifier
) {
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFDF7A),
            Color(0xFFFFC72C),
            Color(0xFFE5A812)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp), spotColor = GoldGlow)
            .clip(RoundedCornerShape(26.dp))
            .background(cardGradient)
            .padding(22.dp)
    ) {
        // Decorative background coin watermark
        Canvas(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.TopEnd)
        ) {
            val r = size.minDimension / 2f
            drawCircle(
                color = OnGold.copy(alpha = 0.06f),
                radius = r,
                center = Offset(r + 20f, -10f)
            )
            drawCircle(
                color = OnGold.copy(alpha = 0.04f),
                radius = r * 0.75f,
                center = Offset(r + 20f, -10f),
                style = Stroke(width = 8f)
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glass-pill label
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(OnGold.copy(alpha = 0.90f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "FitPaisa Wallet",
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                // Verified chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(OnGold.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = OnGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Verified Ledger",
                        color = OnGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Main coin count with high impact
            Text(
                text = "${formatNumber(coins)} FitCoins",
                color = OnGold,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )

            Spacer(Modifier.height(8.dp))

            // Approximate PKR Cashout value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OnGold.copy(alpha = 0.14f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Estimated Cash Value:",
                    color = OnGold.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Rs ${formatNumber(coins / 100)} PKR",
                    color = OnGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Modern glassmorphic info card with sleek border stroke. */
@Composable
fun OutlineInfoCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = ForestGreenCard
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(CardBorderGlow, CardBorder)
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.Start,
            content = content
        )
    }
}

fun formatNumber(value: Int): String = "%,d".format(Locale.US, value)
fun formatNumber(value: Long): String = "%,d".format(Locale.US, value)

/**
 * Draws modern rounded gradient arc segment with glowing background track.
 */
fun DrawScope.goldArcSegment(
    sweepDegrees: Float,
    strokeWidthPx: Float,
    trackColor: Color = RingTrack,
    progressColor: Color = Gold
) {
    // Background track
    drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(
            width = strokeWidthPx,
            cap = StrokeCap.Round
        )
    )

    // Active progress arc
    if (sweepDegrees > 0f) {
        // Subtle outer glow arc
        drawArc(
            color = GoldGlow,
            startAngle = -90f,
            sweepAngle = sweepDegrees,
            useCenter = false,
            style = Stroke(
                width = strokeWidthPx + 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
        // Main arc
        drawArc(
            color = progressColor,
            startAngle = -90f,
            sweepAngle = sweepDegrees,
            useCenter = false,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round
            )
        )
    }
}
