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
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.fitpaisa.app.core.economy.EconomyConfig
import com.fitpaisa.app.core.economy.LEVELS
import com.fitpaisa.app.core.rewards.RewardsRepository
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
 * The current level and lifetime steps now come from the server profile
 * (via get-balance), not from a placeholder.
 */
@Composable
fun LevelsScreen(
    levelViewModel: LevelViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val currentLevel = levelViewModel.level
    val lifetimeSteps = levelViewModel.lifetimeSteps
    val nextLevel = LEVELS.firstOrNull { it.number > currentLevel }

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
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Lifetime steps: ${formatNumber(lifetimeSteps.toInt())}",
            style = MaterialTheme.typography.bodyMedium,
            color = CreamWhite
        )
        if (nextLevel != null) {
            Text(
                text = "${formatNumber((nextLevel.lifetimeStepsRequired - lifetimeSteps).coerceAtLeast(0).toInt())} more steps to Level ${nextLevel.number} · ${nextLevel.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = Gold
            )
        } else {
            Text(
                text = "You reached the highest level.",
                style = MaterialTheme.typography.bodyMedium,
                color = Gold
            )
        }
        Spacer(Modifier.height(16.dp))

        // Weekly leaderboard.
        WeeklyLeaderboard()
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

@HiltViewModel
class LevelViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository
) : ViewModel() {
    var level by mutableStateOf(1)
        private set
    var levelName by mutableStateOf("Starter")
        private set
    var lifetimeSteps by mutableStateOf(0L)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val balance = rewardsRepository.getBalance()
            if (balance.ok) {
                level = balance.level
                levelName = balance.levelName
                lifetimeSteps = balance.lifetimeSteps
            }
        }
    }
}

/** Weekly leaderboard: top walkers this week. */
@Composable
private fun WeeklyLeaderboard(
    viewModel: LeaderboardViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val entries = viewModel.entries
    val leaderboardError = viewModel.error
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = "🏆 Weekly Leaderboard",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CreamWhite
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Top walkers this week",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )
            Spacer(Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text(
                    text = leaderboardError?.let {
                        "Couldn't load the leaderboard. Pull the screen again or check your connection."
                    } ?: "No walkers yet this week — be the first!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSage
                )
            } else {
                entries.take(10).forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "${index + 1}."
                        }
                        Text(
                            text = medal,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(40.dp)
                        )
                        Text(
                            text = entry.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CreamWhite,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${formatNumber(entry.weekSteps.toInt())} steps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gold
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val rewardsRepository: com.fitpaisa.app.core.rewards.RewardsRepository
) : ViewModel() {
    var entries by mutableStateOf<List<com.fitpaisa.app.core.rewards.LeaderboardEntry>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            entries = rewardsRepository.getLeaderboard()
            error = rewardsRepository.lastLeaderboardError
        }
    }
}
