package com.fitpaisa.app.feature.levels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitpaisa.app.core.economy.EconomyConfig
import com.fitpaisa.app.core.economy.LEVELS
import com.fitpaisa.app.core.ui.PilotData
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold
import com.fitpaisa.app.ui.theme.formatNumber

/**
 * Levels: the five earning tiers. The current level is highlighted gold;
 * locked levels show their lifetime-step requirement plus the daily perks
 * each tier unlocks. All numbers come from [EconomyConfig].
 *
 * M1: current level comes from the server profile; tiers mirror
 * supabase/seed.sql.
 */
@Composable
fun LevelsScreen() {
    // M1: replace with the server-provided level.
    val currentLevel = PilotData.LEVEL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Text(
            text = "Levels",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Walk more, level up — bigger daily step caps, more ads, and bonus coins.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSage
        )
        Spacer(Modifier.height(16.dp))

        LEVELS.forEach { level ->
            val isCurrent = level.number == currentLevel
            val isUnlocked = level.number <= currentLevel
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) Gold else ForestGreenCard
                ),
                border = if (isCurrent) null else BorderStroke(1.dp, Gold.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val contentColor = if (isCurrent) OnGold else CreamWhite
                    val subColor = if (isCurrent) OnGold.copy(alpha = 0.75f) else MutedSage
                    Icon(
                        imageVector = if (isUnlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                        contentDescription = null,
                        tint = if (isCurrent) OnGold else Gold,
                        modifier = Modifier.size(40.dp)
                    )
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Level ${level.number} · ${level.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                        Text(
                            text = if (level.lifetimeStepsRequired == 0L) {
                                "Your starting level"
                            } else {
                                "Reach ${formatNumber(level.lifetimeStepsRequired.toInt())} lifetime steps"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = subColor
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${formatNumber(level.dailyStepCap)} steps/day · " +
                                "${level.dailyAdLimit} ads/day · " +
                                "${EconomyConfig.adCoinsForLevel(level.number)} coins per ad",
                            style = MaterialTheme.typography.bodySmall,
                            color = subColor
                        )
                        if (isCurrent) {
                            Text(
                                text = "Current level",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnGold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
