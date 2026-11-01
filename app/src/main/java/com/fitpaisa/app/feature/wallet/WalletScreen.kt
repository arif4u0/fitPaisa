package com.fitpaisa.app.feature.wallet

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.fitpaisa.app.core.economy.EconomyConfig
import com.fitpaisa.app.core.rewards.RequestOutcome
import com.fitpaisa.app.core.rewards.RewardsRepository
import com.fitpaisa.app.ui.theme.CardBorder
import com.fitpaisa.app.ui.theme.CardBorderGlow
import com.fitpaisa.app.ui.theme.CoinBalanceCard
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
import com.fitpaisa.app.ui.theme.RingTrack
import com.fitpaisa.app.ui.theme.SuccessGreen
import com.fitpaisa.app.ui.theme.formatNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Modern Luxury Wallet Dashboard.
 */
@Composable
fun WalletScreen(viewModel: WalletViewModel = hiltViewModel()) {
    val coins = viewModel.coins
    val maturing = viewModel.maturingCoins
    val minCoins = EconomyConfig.CASHOUT_MIN_COINS
    val cashoutProgress = (coins.toFloat() / minCoins.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 28.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "My Wallet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CreamWhite,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Direct payouts to Easypaisa & JazzCash",
                    style = MaterialTheme.typography.bodySmall,
                    color = GoldLight
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ForestGreenCard)
                    .border(1.dp, CardBorderGlow, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "100 Coins = Rs 1",
                    color = GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Luxury Gold Balance Card
        CoinBalanceCard(
            coins = coins,
            modifier = Modifier.fillMaxWidth()
        )

        // Maturing Coins Card
        if (maturing > 0) {
            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockClock,
                            contentDescription = null,
                            tint = Gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "+${formatNumber(maturing)} Coins Maturing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CreamWhite
                        )
                        Text(
                            text = "Unlocks in 7 days after anti-cheat validation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSage
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Cashout Action Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                                .background(NeonEmerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Payments,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Cash Payout",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = CreamWhite
                            )
                            Text(
                                text = "JazzCash · Easypaisa · Bank",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedSage
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (EconomyConfig.CASHOUT_ENABLED) NeonEmerald.copy(alpha = 0.15f) else Gold.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (EconomyConfig.CASHOUT_ENABLED) "Active" else "Pilot Mode",
                            color = if (EconomyConfig.CASHOUT_ENABLED) NeonEmerald else GoldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Progress to min cashout
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Cashout Threshold",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedSage
                        )
                        Text(
                            text = "${formatNumber(coins)} / ${formatNumber(minCoins)} Coins (Rs 100)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldLight
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { cashoutProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (coins >= minCoins) NeonEmerald else Gold,
                        trackColor = RingTrack
                    )
                }

                Spacer(Modifier.height(18.dp))

                val canCashout = EconomyConfig.CASHOUT_ENABLED && coins >= minCoins
                Button(
                    onClick = { viewModel.openCashout() },
                    enabled = EconomyConfig.CASHOUT_ENABLED,
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
                    Text(
                        text = if (canCashout) "Request Cash Withdrawal" else "Earn ${formatNumber(minCoins)} Coins to Withdraw",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }

                if (!EconomyConfig.CASHOUT_ENABLED) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "🔒 Direct cash withdrawals open nationwide soon. Your accumulated coins remain 100% safe & verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedSage,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Security & Earning Architecture
        Text(
            text = "FitPaisa Trust & Security",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )

        Spacer(Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SecurityRow(
                    icon = Icons.Filled.TrendingUp,
                    title = "Fair Earning Rate",
                    description = "1,000 steps convert into 1 FitCoin. Reach higher tier levels to boost your daily earning cap."
                )
                SecurityRow(
                    icon = Icons.Filled.Security,
                    title = "Cloud Ledger Integrity",
                    description = "Coin records are posted securely to cryptographic PostgreSQL ledger tables with anti-cheat duplicate protection."
                )
                SecurityRow(
                    icon = Icons.Filled.CheckCircle,
                    title = "Real Human Rewards",
                    description = "Funded transparently through digital ad sponsor revenue. Real cash sent straight to your Pakistan mobile wallet."
                )
            }
        }
    }

    if (viewModel.showCashoutForm) {
        CashoutDialog(viewModel = viewModel)
    }

    viewModel.cashoutMessage?.let { msg ->
        if (!viewModel.showCashoutForm) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ForestGreenCard)
                    .border(1.dp, CardBorderGlow, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(msg, style = MaterialTheme.typography.bodyMedium, color = GoldLight)
            }
        }
    }
}

@Composable
private fun SecurityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Gold.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = CreamWhite
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun CashoutDialog(viewModel: WalletViewModel) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!viewModel.cashoutBusy) viewModel.closeCashout() },
        title = {
            Text(
                "Cash Withdrawal",
                color = CreamWhite,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Exchange rate: 100 coins = Rs 1. Minimum withdrawal is ${EconomyConfig.CASHOUT_MIN_COINS} coins (Rs 100).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )

                OutlinedTextField(
                    value = viewModel.cashoutCoins,
                    onValueChange = { viewModel.onCashoutCoinsChange(it) },
                    label = { Text("Coins to Cash Out", color = MutedSage) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamWhite,
                        unfocusedTextColor = CreamWhite,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                val methods = listOf("easypaisa" to "Easypaisa", "jazzcash" to "JazzCash", "bank" to "Bank")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (value, label) ->
                        FilterChip(
                            selected = viewModel.cashoutMethod == value,
                            onClick = { viewModel.onCashoutMethodChange(value) },
                            label = { Text(label, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold,
                                selectedLabelColor = OnGold,
                                containerColor = ForestGreenSurface,
                                labelColor = CreamWhite
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = viewModel.cashoutTitle,
                    onValueChange = { viewModel.onCashoutTitleChange(it) },
                    label = { Text("Account Holder Name", color = MutedSage) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamWhite,
                        unfocusedTextColor = CreamWhite,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = viewModel.cashoutNumber,
                    onValueChange = { viewModel.onCashoutNumberChange(it) },
                    label = { Text("Mobile/Account (e.g. 03001234567)", color = MutedSage) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamWhite,
                        unfocusedTextColor = CreamWhite,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = CardBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                viewModel.cashoutMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = GoldLight)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.submitCashout() },
                enabled = !viewModel.cashoutBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = OnGold
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (viewModel.cashoutBusy) {
                    CircularProgressIndicator(color = OnGold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm Payout", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.closeCashout() }) {
                Text("Cancel", color = MutedSage)
            }
        },
        containerColor = ForestGreenCard,
        shape = RoundedCornerShape(24.dp)
    )
}

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val rewardsRepository: RewardsRepository
) : ViewModel() {
    var coins by mutableStateOf(0)
        private set
    var maturingCoins by mutableStateOf(0)
        private set
    var showCashoutForm by mutableStateOf(false)
        private set
    var cashoutCoins by mutableStateOf("")
        private set
    var cashoutMethod by mutableStateOf("easypaisa")
        private set
    var cashoutTitle by mutableStateOf("")
        private set
    var cashoutNumber by mutableStateOf("")
        private set
    var cashoutBusy by mutableStateOf(false)
        private set
    var cashoutMessage by mutableStateOf<String?>(null)
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val b = rewardsRepository.getBalance()
            coins = b.availableCoins
            maturingCoins = b.maturingCoins
        }
    }

    fun openCashout() {
        cashoutMessage = null
        showCashoutForm = true
    }

    fun closeCashout() {
        showCashoutForm = false
    }

    fun onCashoutCoinsChange(v: String) { cashoutCoins = v.filter { it.isDigit() }.take(7) }
    fun onCashoutMethodChange(v: String) { cashoutMethod = v }
    fun onCashoutTitleChange(v: String) { cashoutTitle = v }
    fun onCashoutNumberChange(v: String) { cashoutNumber = v }

    fun submitCashout() {
        val amount = cashoutCoins.toIntOrNull() ?: 0
        if (amount < EconomyConfig.CASHOUT_MIN_COINS) {
            cashoutMessage = "Minimum withdrawal is ${EconomyConfig.CASHOUT_MIN_COINS} coins (Rs 100)."
            return
        }
        if (cashoutTitle.isBlank() || cashoutNumber.isBlank()) {
            cashoutMessage = "Please complete account title and mobile number."
            return
        }
        cashoutBusy = true
        cashoutMessage = null
        viewModelScope.launch {
            val result = rewardsRepository.requestCashout(
                coins = amount,
                method = cashoutMethod,
                accountTitle = cashoutTitle.trim(),
                accountNumber = cashoutNumber.trim()
            )
            cashoutBusy = false
            when (result) {
                is RequestOutcome.Success -> {
                    cashoutMessage = result.message
                    showCashoutForm = false
                    refresh()
                }
                is RequestOutcome.Failed -> cashoutMessage = result.message
            }
        }
    }
}
