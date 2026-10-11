package com.fitpaisa.app.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.fitpaisa.app.core.rewards.RewardsRepository
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Prize segments on the wheel (coins). */
private val PRIZES = listOf(5, 10, 15, 25, 50, 100)
private val SEGMENT_COLORS = listOf(
    Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C),
    Color(0xFF43A047), Color(0xFF4CAF50), Color(0xFF66BB6A)
)

/**
 * Lucky Spin dialog: 1 free spin/day + extra spins via rewarded ad.
 * The SERVER picks the prize; the wheel animates to land on it.
 */
@Composable
fun SpinWheelDialog(
    rewardsRepository: RewardsRepository,
    adManager: com.fitpaisa.app.core.ads.AdManager,
    activity: android.app.Activity?,
    freeSpinAvailable: Boolean,
    onFreeSpinUsed: () -> Unit,
    onDismiss: () -> Unit,
    onPrizeWon: (Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf<String?>(null) }
    var freeAvailable by remember { mutableStateOf(freeSpinAvailable) }
    val rotation = remember { Animatable(0f) }

    fun doSpin(spinType: String) {
        if (spinning) return
        spinning = true
        resultText = null
        scope.launch {
            // Start the wheel spinning immediately (visual).
            val startRotation = rotation.value % 360f
            // Call the server for the real prize.
            val outcome = rewardsRepository.spin(spinType)
            val prize = when (outcome) {
                is RewardsRepository.SpinOutcome.Won -> outcome.prizeCoins
                is RewardsRepository.SpinOutcome.FreeSpinUsed -> {
                    resultText = "Free spin already used today — try an ad spin!"
                    spinning = false
                    return@launch
                }
                is RewardsRepository.SpinOutcome.Failed -> {
                    resultText = outcome.message
                    spinning = false
                    return@launch
                }
            }
            if (spinType == "free") {
                freeAvailable = false
                onFreeSpinUsed()
            }
            onPrizeWon(prize)

            // Land the wheel on the winning segment.
            val prizeIndex = PRIZES.indexOf(prize).let { if (it < 0) 0 else it }
            // Segment i spans [i*60, (i+1)*60) degrees, drawn clockwise from top.
            // Pointer is at top (0°). Target: center of segment at top.
            val segmentCenter = prizeIndex * 60f + 30f
            val targetMod = (360f - segmentCenter) % 360f
            // Spin 5 full turns + land on target.
            val currentMod = ((startRotation % 360f) + 360f) % 360f
            var delta = (targetMod - currentMod + 360f) % 360f
            delta += 360f * 5
            rotation.animateTo(
                targetValue = startRotation + delta,
                animationSpec = tween(durationMillis = 3500)
            )
            resultText = "🎉 You won $prize FitCoins!"
            spinning = false
        }
    }

    Dialog(onDismissRequest = { if (!spinning) onDismiss() }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🍀 Lucky Spin",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (freeAvailable)
                        "1 FREE spin today — extra spins by watching an ad!"
                    else
                        "Free spin used — watch an ad for another spin!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSage
                )
                Spacer(Modifier.height(16.dp))

                // The wheel.
                SpinWheel(rotation = rotation.value)
                Spacer(Modifier.height(16.dp))

                resultText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = Gold
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (freeAvailable && !spinning) {
                        Button(
                            onClick = { doSpin("free") },
                            colors = ButtonDefaults.buttonColors(containerColor = Gold)
                        ) {
                            Text("FREE Spin", color = Color.Black)
                        }
                    }
                    if (!spinning) {
                        OutlinedButton(onClick = {
                            if (activity != null) {
                                scope.launch {
                                    val watched = adManager.showRewarded(activity)
                                    if (watched) doSpin("ad")
                                }
                            }
                        }) {
                            Text("📺 Ad Spin", color = Gold)
                        }
                    }
                }
                if (!spinning) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = MutedSage)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpinWheel(rotation: Float) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier.size(240.dp)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        rotate(degrees = rotation, pivot = center) {
            val sweep = 360f / PRIZES.size
            PRIZES.forEachIndexed { i, prize ->
                // Draw segment.
                drawArc(
                    color = SEGMENT_COLORS[i % SEGMENT_COLORS.size],
                    startAngle = i * sweep - 90f,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                // Draw prize text.
                val angleRad = Math.toRadians((i * sweep + sweep / 2 - 90).toDouble())
                val textRadius = radius * 0.65f
                val tx = center.x + cos(angleRad).toFloat() * textRadius
                val ty = center.y + sin(angleRad).toFloat() * textRadius
                val style = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                val layout = textMeasurer.measure("$prize", style)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset(tx - layout.size.width / 2f, ty - layout.size.height / 2f)
                )
            }
            // Center hub.
            drawCircle(color = Gold, radius = radius * 0.12f, center = center)
        }
        // Pointer at top (not rotating).
        val pointerY = center.y - radius - 8.dp.toPx()
        drawCircle(color = Color.Red, radius = 10.dp.toPx(), center = Offset(center.x, pointerY))
    }
}
