package com.fitpaisa.app.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitpaisa.app.core.auth.AuthRepository
import com.fitpaisa.app.core.economy.EconomyConfig
import com.fitpaisa.app.core.rewards.ConvertOutcome
import com.fitpaisa.app.core.rewards.RewardsRepository
import com.fitpaisa.app.core.sensor.StepSensorRepository
import com.fitpaisa.app.core.ui.PilotData
import com.fitpaisa.app.ui.theme.CoinBalanceCard
import com.fitpaisa.app.ui.theme.CoinLogo
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold
import com.fitpaisa.app.ui.theme.OutlineInfoCard
import com.fitpaisa.app.ui.theme.SuccessGreen
import com.fitpaisa.app.ui.theme.formatNumber
import com.fitpaisa.app.ui.theme.goldArcSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/** Convert button state. */
sealed interface ConvertUiState {
    data object Idle : ConvertUiState
    data object Converting : ConvertUiState
    data class Done(val message: String) : ConvertUiState
}

data class HomeUiState(
    val userName: String = "friend",
    val stepsToday: Int = 0,
    val stepsGoal: Int = PilotData.STEPS_GOAL,
    /** Coin balance comes ONLY from the server ledger — never computed on the phone. */
    val coins: Int = 0,
    val maturingCoins: Int = 0,
    val balanceLoaded: Boolean = false,
    val level: Int = PilotData.LEVEL,
    val levelName: String = PilotData.LEVEL_NAME,
    val lastSyncText: String = "Live Sensor Active",
    val hasPermission: Boolean = true,
    val isSensorAvailable: Boolean = true,
    val convertState: ConvertUiState = ConvertUiState.Idle,
    /** Daily challenge: matches app_config daily_challenge_target/bonus (server awards it). */
    val challengeTarget: Int = 8000,
    val challengeBonus: Int = 50
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val stepSensorRepository: StepSensorRepository,
    val rewardsRepository: RewardsRepository,
    private val updateChecker: com.fitpaisa.app.core.update.UpdateChecker,
    val adManager: com.fitpaisa.app.core.ads.AdManager
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set
    var updateInfo by mutableStateOf<com.fitpaisa.app.core.update.UpdateInfo?>(null)
        private set

    init {
        // Preload the rewarded ad so it's ready when the user taps Convert.
        adManager.preload()
        viewModelScope.launch {
            val email = authRepository.currentUserEmail()
            val name = email
                ?.substringBefore("@")
                ?.replaceFirstChar { it.uppercaseChar() }
                ?.takeIf { it.isNotBlank() }
                ?: "friend"
            uiState = uiState.copy(userName = name)
        }

        viewModelScope.launch {
            stepSensorRepository.hasPermission.collectLatest { hasPerm ->
                uiState = uiState.copy(hasPermission = hasPerm)
            }
        }

        viewModelScope.launch {
            stepSensorRepository.isSensorAvailable.collectLatest { available ->
                uiState = uiState.copy(isSensorAvailable = available)
            }
        }

        viewModelScope.launch {
            stepSensorRepository.todaySteps.collectLatest { sensorSteps ->
                uiState = uiState.copy(stepsToday = sensorSteps)
            }
        }

        refreshBalance()
    }

    /** Pulls the real balance from the server ledger. */
    fun refreshBalance() {
        viewModelScope.launch {
            val b = rewardsRepository.getBalance()
            uiState = uiState.copy(
                coins = b.availableCoins,
                maturingCoins = b.maturingCoins,
                balanceLoaded = true
            )
        }
    }

    /**
     * Convert tap: sends today's steps to the server. The SERVER decides how
     * many coins are credited (level cap, anti-replay) — the phone never does.
     */
    fun onConvertClicked(activity: android.app.Activity) {
        if (uiState.convertState == ConvertUiState.Converting) return
        viewModelScope.launch {
            uiState = uiState.copy(convertState = ConvertUiState.Converting)
            // Step 1: user must watch a rewarded ad to convert.
            // (Temporary: if the ad system is broken, allow convert anyway
            // so the core walk-to-earn loop works while Unity is fixed.)
            val watched = adManager.showRewarded(activity)
            val adsBroken = adManager.lastError.contains("INVALID_ARGUMENT") ||
                    adManager.lastError.contains("Init failed") ||
                    adManager.lastError == "Not initialized yet"
            if (!watched && !adsBroken) {
                uiState = uiState.copy(
                    convertState = ConvertUiState.Done(
                        "Ad not ready: ${adManager.lastError}"
                    )
                )
                return@launch
            }
            // Step 2: ad watched — credit the coins on the server.
            val outcome = rewardsRepository.convertTodaySteps(uiState.stepsToday)
            val message = when (outcome) {
                is ConvertOutcome.Converted -> {
                    val base = "+${outcome.coins} FitCoins added! They mature in 7 days."
                    val streak = if (outcome.streakDays > 1) {
                        " 🔥 ${outcome.streakDays}-day streak!"
                    } else ""
                    val bonus = if (outcome.streakBonus > 0) {
                        " +${outcome.streakBonus} streak bonus!"
                    } else ""
                    val challenge = if (outcome.challengeBonus > 0) {
                        " 🎯 Challenge complete! +${outcome.challengeBonus} bonus coins!"
                    } else ""
                    base + streak + bonus + challenge
                }
                ConvertOutcome.NothingNew ->
                    "Nothing new to convert yet — keep walking!"
                is ConvertOutcome.Failed -> outcome.message
            }
            uiState = uiState.copy(convertState = ConvertUiState.Done(message))
            refreshBalance()
        }
    }

    fun dismissConvertMessage() {
        uiState = uiState.copy(convertState = ConvertUiState.Idle)
    }

    fun onPermissionGranted() {
        stepSensorRepository.updatePermissionStatus()
    }

    fun refreshStepCount() {
        stepSensorRepository.refresh()
    }

    fun dismissUpdate() {
        updateInfo = null
    }

    // --- Lucky Spin ---
    var showSpinDialog by mutableStateOf(false)
        private set
    var freeSpinAvailable by mutableStateOf(true)

    fun openSpin() {
        viewModelScope.launch {
            freeSpinAvailable = rewardsRepository.isFreeSpinAvailable()
            showSpinDialog = true
        }
    }

    fun dismissSpin() {
        showSpinDialog = false
    }

    /** Checks for a new version; call on every resume so the popup appears. */
    fun checkForUpdates() {
        viewModelScope.launch {
            // Only check if we haven't already found an update.
            if (updateInfo == null) {
                updateInfo = updateChecker.checkForUpdate()
            }
        }
    }

    fun downloadUpdate() {
        updateInfo?.let { updateChecker.downloadAndInstall(it) }
        if (updateInfo?.forceUpdate != true) {
            updateInfo = null
        }
    }
}

/**
 * Home dashboard, matching the approved design: greeting, big gold steps
 * ring, gold coin-balance card, level + sync cards, permission request prompt.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state = viewModel.uiState

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onPermissionGranted()
        }
    }

    // The hardware counter keeps counting while the app is closed. Force a fresh
    // reading every time the dashboard becomes visible so it never shows a stale count.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStepCount()
                viewModel.checkForUpdates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

        if (!state.hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionCard(onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            })
            Spacer(Modifier.height(16.dp))
        }

        StepsRing(
            steps = state.stepsToday,
            goal = state.stepsGoal,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))

        val context = androidx.compose.ui.platform.LocalContext.current
        ConvertSection(
            steps = state.stepsToday,
            level = state.level,
            convertState = state.convertState,
            onConvert = {
                val activity = context as? android.app.Activity
                if (activity != null) viewModel.onConvertClicked(activity)
            },
            onDismiss = viewModel::dismissConvertMessage
        )
        Spacer(Modifier.height(20.dp))

        CoinBalanceCard(
            coins = state.coins,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.maturingCoins > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "+${formatNumber(state.maturingCoins)} coins maturing (available in 7 days)",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )
        }
        Spacer(Modifier.height(16.dp))

        ChallengeCard(
            stepsToday = state.stepsToday,
            target = state.challengeTarget,
            bonus = state.challengeBonus,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        // Lucky Spin entry card.
        SpinEntryCard(
            onClick = { viewModel.openSpin() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // Spin dialog.
        if (viewModel.showSpinDialog) {
            val ctx = context
            SpinWheelDialog(
                rewardsRepository = viewModel.rewardsRepository,
                adManager = viewModel.adManager,
                activity = ctx as? android.app.Activity,
                freeSpinAvailable = viewModel.freeSpinAvailable,
                onFreeSpinUsed = { viewModel.freeSpinAvailable = false },
                onDismiss = viewModel::dismissSpin,
                onPrizeWon = { viewModel.refreshBalance() }
            )
        }

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
                            text = if (state.isSensorAvailable) "Live Tracking" else "No Hardware Sensor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gold
                        )
                    }
                }
            }
        }
    }

    // In-app update prompt (for sideloaded APKs — Play Store handles its own updates).
    viewModel.updateInfo?.let { update ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!update.forceUpdate) viewModel.dismissUpdate() },
            title = {
                Text(
                    "New version available",
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "fitPaisa ${update.latestVersion} is ready.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CreamWhite
                    )
                    if (update.changelog.isNotBlank()) {
                        Text(
                            update.changelog,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSage
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadUpdate() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = OnGold
                    )
                ) {
                    Text("Download update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!update.forceUpdate) {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.dismissUpdate() }
                    ) {
                        Text("Later", color = MutedSage)
                    }
                }
            },
            containerColor = ForestGreenCard
        )
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(36.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Activity Tracking Off",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CreamWhite
                )
                Text(
                    text = "Grant fitness permission so fitPaisa can auto-count your physical steps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
            }
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = OnGold
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Allow", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ConvertSection(
    steps: Int,
    level: Int,
    convertState: ConvertUiState,
    onConvert: () -> Unit,
    onDismiss: () -> Unit
) {
    // Estimate only — the server decides the real credit (level cap, replays).
    val cap = EconomyConfig.levelInfo(level).dailyStepCap
    val estimate = EconomyConfig.stepsToCoins(steps.coerceAtMost(cap))
    val canConvert = steps >= EconomyConfig.STEPS_PER_COIN &&
        convertState != ConvertUiState.Converting

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onConvert,
            enabled = canConvert,
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = OnGold,
                disabledContainerColor = Gold.copy(alpha = 0.25f),
                disabledContentColor = CreamWhite.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (convertState == ConvertUiState.Converting) {
                    "Converting…"
                } else {
                    "Watch ad → Convert ${formatNumber(steps)} steps"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (convertState is ConvertUiState.Done) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = convertState.message,
                style = MaterialTheme.typography.bodyMedium,
                color = Gold
            )
            LaunchedEffect(convertState) {
                kotlinx.coroutines.delay(4000)
                onDismiss()
            }
        } else {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Coins are credited by our server and mature in 7 days.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )
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

/** Entry card that opens the Lucky Spin dialog. */
@Composable
private fun SpinEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🍀", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Lucky Spin",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Win up to 100 coins!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSage
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Gold
            )
        }
    }
}

/** Today's Challenge card: walk the target steps to earn bonus coins. */
@Composable
private fun ChallengeCard(
    stepsToday: Int,
    target: Int,
    bonus: Int,
    modifier: Modifier = Modifier
) {
    val progress = (stepsToday.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val completed = stepsToday >= target
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = Gold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Today's Challenge",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Text(
                    text = if (completed) "Done! 🎉" else "+$bonus coins",
                    style = MaterialTheme.typography.titleMedium,
                    color = Gold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (completed)
                    "Challenge complete — bonus on its way!"
                else
                    "Walk ${formatNumber(target)} steps today to earn $bonus bonus coins.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSage
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                color = Gold,
                trackColor = Color.White.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${formatNumber(stepsToday)} / ${formatNumber(target)} steps",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )
        }
    }
}
