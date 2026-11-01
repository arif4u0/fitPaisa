package com.fitpaisa.app.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitpaisa.app.core.auth.AuthRepository
import com.fitpaisa.app.core.economy.EconomyConfig
import com.fitpaisa.app.core.rewards.ConvertOutcome
import com.fitpaisa.app.core.rewards.RewardsRepository
import com.fitpaisa.app.core.sensor.StepSensorRepository
import com.fitpaisa.app.core.ui.PilotData
import com.fitpaisa.app.ui.theme.CardBorder
import com.fitpaisa.app.ui.theme.CardBorderGlow
import com.fitpaisa.app.ui.theme.CoinBalanceCard
import com.fitpaisa.app.ui.theme.CoinLogo
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.ForestGreenSurface
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.GoldDark
import com.fitpaisa.app.ui.theme.GoldGlow
import com.fitpaisa.app.ui.theme.GoldLight
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.NeonEmerald
import com.fitpaisa.app.ui.theme.OnGold
import com.fitpaisa.app.ui.theme.OutlineInfoCard
import com.fitpaisa.app.ui.theme.RingTrack
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
    val coins: Int = 0,
    val maturingCoins: Int = 0,
    val balanceLoaded: Boolean = false,
    val level: Int = PilotData.LEVEL,
    val levelName: String = PilotData.LEVEL_NAME,
    val lifetimeSteps: Long = 0,
    val lastSyncText: String = "Live Sensor Active",
    val hasPermission: Boolean = true,
    val isSensorAvailable: Boolean = true,
    val convertState: ConvertUiState = ConvertUiState.Idle,
    val dailyAdsWatched: Int = 0,
    val dailyAdLimit: Int = 5,
    val dailyAdCoins: Int = 10,
    val dailyAdBusy: Boolean = false,
    val dailyAdMessage: String? = null,
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

    fun refreshBalance() {
        viewModelScope.launch {
            val b = rewardsRepository.getBalance()
            uiState = uiState.copy(
                coins = b.availableCoins,
                maturingCoins = b.maturingCoins,
                balanceLoaded = true,
                level = if (b.ok) b.level else uiState.level,
                levelName = if (b.ok) b.levelName else uiState.levelName,
                lifetimeSteps = if (b.ok) b.lifetimeSteps else uiState.lifetimeSteps,
                dailyAdsWatched = if (b.ok) b.dailyAdsWatched else uiState.dailyAdsWatched,
                dailyAdLimit = if (b.ok) b.dailyAdLimit else uiState.dailyAdLimit,
                dailyAdCoins = if (b.ok) b.dailyAdCoins else uiState.dailyAdCoins
            )
        }
    }

    fun onWatchAdClicked(activity: android.app.Activity) {
        if (uiState.dailyAdBusy) return
        if (uiState.dailyAdsWatched >= uiState.dailyAdLimit) {
            uiState = uiState.copy(
                dailyAdMessage = "You have watched all of today's ads. Come back tomorrow!"
            )
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(dailyAdBusy = true, dailyAdMessage = null)
            val adOutcome = adManager.showRewarded(activity)
            when (adOutcome) {
                com.fitpaisa.app.core.ads.AdOutcome.SKIPPED -> {
                    uiState = uiState.copy(
                        dailyAdBusy = false,
                        dailyAdMessage = "You closed the ad early. Watch fully to claim FitCoins."
                    )
                }
                com.fitpaisa.app.core.ads.AdOutcome.NOT_READY -> {
                    uiState = uiState.copy(
                        dailyAdBusy = false,
                        dailyAdMessage = "Ad is warming up. Please try again in 5 seconds."
                    )
                }
                com.fitpaisa.app.core.ads.AdOutcome.WATCHED -> {
                    when (val outcome = rewardsRepository.watchAd()) {
                        is RewardsRepository.AdRewardOutcome.Credited -> {
                            uiState = uiState.copy(
                                dailyAdBusy = false,
                                dailyAdMessage = "+${outcome.coins} FitCoins credited! (${outcome.watched}/${outcome.limit} today)",
                                dailyAdsWatched = outcome.watched,
                                dailyAdLimit = outcome.limit
                            )
                            refreshBalance()
                        }
                        is RewardsRepository.AdRewardOutcome.LimitReached -> {
                            uiState = uiState.copy(
                                dailyAdBusy = false,
                                dailyAdMessage = "You have watched all of today's ads. Come back tomorrow!",
                                dailyAdsWatched = outcome.watched,
                                dailyAdLimit = outcome.limit
                            )
                        }
                        is RewardsRepository.AdRewardOutcome.Failed -> {
                            uiState = uiState.copy(
                                dailyAdBusy = false,
                                dailyAdMessage = outcome.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun dismissDailyAdMessage() {
        uiState = uiState.copy(dailyAdMessage = null)
    }

    fun onConvertClicked(activity: android.app.Activity) {
        if (uiState.convertState == ConvertUiState.Converting) return
        viewModelScope.launch {
            uiState = uiState.copy(convertState = ConvertUiState.Converting)
            val adOutcome = adManager.showRewarded(activity)
            if (adOutcome == com.fitpaisa.app.core.ads.AdOutcome.SKIPPED) {
                uiState = uiState.copy(
                    convertState = ConvertUiState.Done(
                        "Ad was closed early. Complete the video to convert steps."
                    )
                )
                return@launch
            }

            val outcome = rewardsRepository.convertTodaySteps(uiState.stepsToday)
            val message = when (outcome) {
                is ConvertOutcome.Converted -> {
                    val base = "🎉 +${outcome.coins} FitCoins earned! Maturing in 7 days."
                    val streak = if (outcome.streakDays > 1) " 🔥 ${outcome.streakDays}-day streak!" else ""
                    val bonus = if (outcome.streakBonus > 0) " +${outcome.streakBonus} bonus!" else ""
                    val challenge = if (outcome.challengeBonus > 0) " 🎯 Challenge complete +${outcome.challengeBonus}!" else ""
                    base + streak + bonus + challenge
                }
                ConvertOutcome.NothingNew -> "All current steps already converted. Keep walking!"
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

    fun checkForUpdates() {
        viewModelScope.launch {
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
 * Modern High-Impact Home Dashboard.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state = viewModel.uiState
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.onPermissionGranted()
    }

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
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        // Modern Header with Logo and Profile Pill
        TopBar(
            userName = state.userName,
            levelName = state.levelName
        )

        Spacer(Modifier.height(20.dp))

        // Greeting Section
        Column {
            Text(
                text = "${dayGreeting()}, ${state.userName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CreamWhite
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Turn daily movement into real cash rewards",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = GoldLight
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // Activity Permission Prompt
        if (!state.hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionCard(onRequestPermission = {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            })
            Spacer(Modifier.height(18.dp))
        }

        // Modern Glowing Step Counter Ring
        StepsRing(
            steps = state.stepsToday,
            goal = state.stepsGoal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        // High-Conversion Action Button
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

        Spacer(Modifier.height(22.dp))

        // Luxury Coin Balance Card
        CoinBalanceCard(
            coins = state.coins,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.maturingCoins > 0) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ForestGreenCard.copy(alpha = 0.6f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "+${formatNumber(state.maturingCoins)} coins maturing (available in 7 days)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Daily Challenge Card
        ChallengeCard(
            stepsToday = state.stepsToday,
            target = state.challengeTarget,
            bonus = state.challengeBonus,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Lucky Spin Card
        SpinEntryCard(
            onClick = { viewModel.openSpin() },
            freeAvailable = viewModel.freeSpinAvailable,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Rewarded Daily Ads Card
        DailyAdsCard(
            watched = state.dailyAdsWatched,
            limit = state.dailyAdLimit,
            coinsPerAd = state.dailyAdCoins,
            levelName = state.levelName,
            busy = state.dailyAdBusy,
            message = state.dailyAdMessage,
            onWatch = {
                val activity = context as? android.app.Activity
                if (activity != null) viewModel.onWatchAdClicked(activity)
            },
            onDismissMessage = viewModel::dismissDailyAdMessage,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(18.dp))

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlineInfoCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Level ${state.level}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreamWhite
                        )
                        Text(
                            text = state.levelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldLight
                        )
                    }
                }
            }

            OutlineInfoCard(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NeonEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Sensors,
                            contentDescription = null,
                            tint = NeonEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Sensor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreamWhite
                        )
                        Text(
                            text = if (state.isSensorAvailable) "Active Sync" else "Unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.isSensorAvailable) NeonEmerald else MutedSage
                        )
                    }
                }
            }
        }
    }

    // Lucky Spin Dialog
    if (viewModel.showSpinDialog) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
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

    // App Update Dialog
    viewModel.updateInfo?.let { update ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { if (!update.forceUpdate) viewModel.dismissUpdate() },
            title = {
                Text(
                    "Update Available",
                    color = CreamWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "fitPaisa ${update.latestVersion} is ready with faster sync and new rewards.",
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
                    Text("Update Now", fontWeight = FontWeight.Bold)
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
private fun TopBar(userName: String, levelName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoinLogo(size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "fitPaisa",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = CreamWhite,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Earn Real Cash By Walking",
                style = MaterialTheme.typography.labelSmall,
                color = GoldLight,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.weight(1f))

        // Level pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ForestGreenCard)
                .border(1.dp, CardBorderGlow, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = levelName,
                color = CreamWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Modern Circular Step Meter with progress, km & calorie estimations.
 */
@Composable
private fun StepsRing(steps: Int, goal: Int, modifier: Modifier = Modifier) {
    val progress = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)
    val distanceKm = String.format(java.util.Locale.US, "%.1f", steps * 0.00075f)
    val calories = (steps * 0.04f).toInt()
    val percentage = (progress * 100).toInt()

    Box(
        modifier = modifier.height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            goldArcSegment(
                sweepDegrees = 360f * progress,
                strokeWidthPx = 22.dp.toPx(),
                trackColor = RingTrack,
                progressColor = Gold
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon + Percentage Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Gold.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = NeonEmerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "$percentage% of Goal",
                    color = NeonEmerald,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            // Large Step Number
            Text(
                text = formatNumber(steps),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = CreamWhite,
                letterSpacing = (-1).sp
            )

            Text(
                text = "Target: ${formatNumber(goal)} steps",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MutedSage
            )

            Spacer(Modifier.height(10.dp))

            // Sub-metrics (km & calories)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "≈ $distanceKm km",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldLight
                )
                Text(
                    text = "•",
                    color = MutedSage
                )
                Text(
                    text = "≈ $calories kcal",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldLight
                )
            }
        }
    }
}

/**
 * Modern Convert Button with glowing gradient, clear coin preview, and feedback.
 */
@Composable
private fun ConvertSection(
    steps: Int,
    level: Int,
    convertState: ConvertUiState,
    onConvert: () -> Unit,
    onDismiss: () -> Unit
) {
    val cap = EconomyConfig.levelInfo(level).dailyStepCap
    val eligibleSteps = steps.coerceAtMost(cap)
    val estimatedCoins = EconomyConfig.stepsToCoins(eligibleSteps)
    val canConvert = steps >= EconomyConfig.STEPS_PER_COIN &&
        convertState != ConvertUiState.Converting

    val buttonGradient = if (canConvert) {
        Brush.horizontalGradient(listOf(GoldLight, Gold, GoldDark))
    } else {
        Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.25f), Gold.copy(alpha = 0.20f)))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .shadow(
                    elevation = if (canConvert) 10.dp else 0.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = GoldGlow
                )
                .clip(RoundedCornerShape(20.dp))
                .background(buttonGradient)
                .clickable(enabled = canConvert, onClick = onConvert),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (convertState == ConvertUiState.Converting) {
                    CircularProgressIndicator(
                        color = OnGold,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Converting Steps…",
                        color = OnGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = if (canConvert) OnGold else CreamWhite.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (canConvert) {
                            "Watch Ad & Convert (${formatNumber(steps)} Steps → $estimatedCoins Coins)"
                        } else {
                            "Walk at least 1,000 steps to convert"
                        },
                        color = if (canConvert) OnGold else CreamWhite.copy(alpha = 0.5f),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (convertState is ConvertUiState.Done) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ForestGreenCard)
                    .border(1.dp, CardBorderGlow, RoundedCornerShape(14.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = convertState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldLight
                )
            }
            LaunchedEffect(convertState) {
                kotlinx.coroutines.delay(4500)
                onDismiss()
            }
        } else {
            Text(
                text = "⚡ Coins are posted to your ledger & mature in 7 days to ensure anti-cheat fairness.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Gold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Motion Sensor Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CreamWhite
                )
                Text(
                    text = "Enable physical step counting to earn FitCoins in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = OnGold
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Enable", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Modern Daily Challenge Card with progress bar.
 */
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Daily Challenge",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CreamWhite
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (completed) NeonEmerald.copy(alpha = 0.2f) else Gold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (completed) "Done! 🎉" else "+$bonus Coins",
                        color = if (completed) NeonEmerald else GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (completed)
                    "Challenge achieved! Bonus credited to your account."
                else
                    "Reach ${formatNumber(target)} steps today to unlock $bonus instant bonus coins.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSage
            )

            Spacer(Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (completed) NeonEmerald else Gold,
                trackColor = RingTrack
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatNumber(stepsToday)} steps walked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
                Text(
                    text = "Goal: ${formatNumber(target)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldLight,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Modern Lucky Spin Entry Card.
 */
@Composable
private fun SpinEntryCard(
    onClick: () -> Unit,
    freeAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Gold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎡", fontSize = 24.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Lucky Spin Wheel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreamWhite
                        )
                        if (freeAvailable) {
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(NeonEmerald)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FREE",
                                    color = OnGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                    Text(
                        text = "Spin and win up to 100 FitCoins instantly",
                        style = MaterialTheme.typography.bodySmall,
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

/**
 * Modern Daily Rewarded Ads Card.
 */
@Composable
private fun DailyAdsCard(
    watched: Int,
    limit: Int,
    coinsPerAd: Int,
    levelName: String,
    busy: Boolean,
    message: String?,
    onWatch: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remaining = (limit - watched).coerceAtLeast(0)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "📺", fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Rewarded Ads",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreamWhite
                        )
                        Text(
                            text = "$levelName Tier · +$coinsPerAd FitCoins each",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSage
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Gold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$watched / $limit Watched",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onWatch,
                enabled = remaining > 0 && !busy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = OnGold,
                    disabledContainerColor = ForestGreenSurface,
                    disabledContentColor = MutedSage
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = OnGold,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Loading Unity Ad…", fontWeight = FontWeight.Bold)
                } else if (remaining > 0) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Video (+ $coinsPerAd Coins)", fontWeight = FontWeight.Bold)
                } else {
                    Text("All Daily Ads Completed 🎉", fontWeight = FontWeight.SemiBold)
                }
            }

            if (message != null) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ForestGreenSurface)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldLight,
                        fontWeight = FontWeight.Medium
                    )
                }
                LaunchedEffect(message) {
                    kotlinx.coroutines.delay(4000)
                    onDismissMessage()
                }
            }
        }
    }
}

private fun dayGreeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}
