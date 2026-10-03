package com.fitpaisa.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitpaisa.app.core.auth.AuthRepository
import com.fitpaisa.app.core.ui.PilotData
import com.fitpaisa.app.ui.theme.CoinBalanceCard
import com.fitpaisa.app.ui.theme.CoinLogo
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OutlineInfoCard
import com.fitpaisa.app.ui.theme.SuccessGreen
import com.fitpaisa.app.ui.theme.formatNumber
import com.fitpaisa.app.ui.theme.goldArcSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * Dashboard numbers. Everything here is a placeholder until M1 wires the
 * real pipeline — see [PilotData] for the replacement map.
 */
data class HomeUiState(
    val userName: String = "friend",
    val stepsToday: Int = PilotData.STEPS_TODAY,       // M1: replace with Health Connect data
    val stepsGoal: Int = PilotData.STEPS_GOAL,         // M1: replace with reward_rules.daily_cap
    val coins: Int = PilotData.COINS,                 // M1: replace with server ledger balance
    val level: Int = PilotData.LEVEL,                 // M1: replace with server level
    val levelName: String = PilotData.LEVEL_NAME,
    val lastSyncText: String = PilotData.LAST_SYNC_TEXT // M1: replace with sync worker status
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState = mutableStateOf(HomeUiState())
        private set

    init {
        viewModelScope.launch {
            val email = authRepository.currentUserEmail()
            val name = email
                ?.substringBefore("@")
                ?.replaceFirstChar { it.uppercaseChar() }
                ?.takeIf { it.isNotBlank() }
                ?: "friend"
            uiState.value = uiState.value.copy(userName = name)
        }
    }
}

/**
 * Home dashboard, matching the approved design: greeting, big gold steps
 * ring, gold coin-balance card, level + sync cards.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state = viewModel.uiState.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        TopBar()
        Spacer(Modifier.height(16.dp))

        Text(
            text = "${dayGreeting()}, ${state.userName}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Text(
            text = "Let's earn while you walk",
            style = MaterialTheme.typography.titleMedium,
            color = Gold
        )
        Spacer(Modifier.height(20.dp))

        StepsRing(
            steps = state.stepsToday,
            goal = state.stepsGoal,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(20.dp))

        CoinBalanceCard(
            coins = state.coins,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlineInfoCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Gold,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Level ${state.level}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreamWhite
                        )
                        Text(
                            text = state.levelName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gold
                        )
                    }
                }
            }
            OutlineInfoCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "Sync status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreamWhite
                        )
                        Text(
                            text = state.lastSyncText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinLogo(size = 40.dp)
        Text(
            text = "fitPaisa",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = CreamWhite,
            modifier = Modifier.padding(start = 10.dp)
        )
        Spacer(Modifier.weight(1f))
        // Decorative in v2 — notifications arrive with M1+ messaging.
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            tint = Gold,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun StepsRing(steps: Int, goal: Int, modifier: Modifier = Modifier) {
    val progress = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            goldArcSegment(
                sweepDegrees = 360f * progress,
                strokeWidthPx = 26.dp.toPx()
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatNumber(steps),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = CreamWhite
            )
            Text(
                text = "of ${formatNumber(goal)} steps",
                style = MaterialTheme.typography.titleMedium,
                color = Gold
            )
        }
    }
}

private fun dayGreeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
