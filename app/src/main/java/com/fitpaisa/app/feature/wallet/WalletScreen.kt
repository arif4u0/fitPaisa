package com.fitpaisa.app.feature.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitpaisa.app.core.economy.EconomyConfig
import com.fitpaisa.app.core.rewards.RequestOutcome
import com.fitpaisa.app.core.rewards.RewardsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.fitpaisa.app.ui.theme.CoinBalanceCard
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold

/**
 * Wallet: coin balance, cash-out (opens when payouts are enabled), and a
 * plain-language explainer of how earning works.
 *
 * Balance comes ONLY from the server ledger via [RewardsRepository].
 * Cash-out opens when [EconomyConfig.CASHOUT_ENABLED] is true
 * (mirrors app_config.cashout_enabled).
 */
@Composable
fun WalletScreen(viewModel: WalletViewModel = hiltViewModel()) {
    val coins = viewModel.coins
    val maturing = viewModel.maturingCoins

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Text(
            text = "Wallet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(16.dp))

        CoinBalanceCard(
            coins = coins,
            modifier = Modifier.fillMaxWidth()
        )
        if (maturing > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "+$maturing coins maturing (available in 7 days)",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.openCashout() },
            enabled = EconomyConfig.CASHOUT_ENABLED,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = OnGold,
                disabledContainerColor = Gold.copy(alpha = 0.25f),
                disabledContentColor = CreamWhite.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = "Cash out",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(8.dp))
        if (EconomyConfig.CASHOUT_ENABLED) {
            Text(
                text = "Minimum ${EconomyConfig.CASHOUT_MIN_COINS} coins (Rs 100). " +
                    "We review each request and send the money to your account.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )
        } else {
            Text(
                text = "Cashout opens soon — minimum ${EconomyConfig.coinsToRupeesText(EconomyConfig.CASHOUT_MIN_COINS)} " +
                    "(${EconomyConfig.CASHOUT_MIN_COINS} coins). Keep earning; your coins are counted and safe.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )
        }
        Spacer(Modifier.height(20.dp))

        Text(
            text = "How earning works",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EarnRow(
                    title = "Walk every day",
                    body = "${EconomyConfig.STEPS_PER_COIN} steps = 1 FitCoin, up to your level's daily step cap. " +
                        "100 coins = Rs 1."
                )
                EarnRow(
                    title = "Level up",
                    body = "More lifetime steps unlock higher levels — bigger daily caps, more ads, and bonus coins."
                )
                EarnRow(
                    title = "Counted on our server",
                    body = "Coins are verified and stored on fitPaisa's secure server, never on your phone, so nobody can fake them."
                )
            }
        }
    }

    if (viewModel.showCashoutForm) {
        CashoutDialog(viewModel = viewModel)
    }
    viewModel.cashoutMessage?.let { msg ->
        if (!viewModel.showCashoutForm) {
            Spacer(Modifier.height(8.dp))
            Text(text = msg, style = MaterialTheme.typography.bodySmall, color = Gold)
        }
    }
}

@Composable
private fun CashoutDialog(viewModel: WalletViewModel) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!viewModel.cashoutBusy) viewModel.closeCashout() },
        title = { Text("Cash out", color = CreamWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "100 coins = Rs 1. Minimum ${EconomyConfig.CASHOUT_MIN_COINS} coins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
                androidx.compose.material3.OutlinedTextField(
                    value = viewModel.cashoutCoins,
                    onValueChange = { viewModel.onCashoutCoinsChange(it) },
                    label = { Text("Coins") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val methods = listOf("easypaisa" to "Easypaisa", "jazzcash" to "JazzCash", "bank" to "Bank")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (value, label) ->
                        androidx.compose.material3.FilterChip(
                            selected = viewModel.cashoutMethod == value,
                            onClick = { viewModel.onCashoutMethodChange(value) },
                            label = { Text(label) }
                        )
                    }
                }
                androidx.compose.material3.OutlinedTextField(
                    value = viewModel.cashoutTitle,
                    onValueChange = { viewModel.onCashoutTitleChange(it) },
                    label = { Text("Account title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.material3.OutlinedTextField(
                    value = viewModel.cashoutNumber,
                    onValueChange = { viewModel.onCashoutNumberChange(it) },
                    label = { Text("Account / mobile number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                viewModel.cashoutMessage?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = Gold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.submitCashout() },
                enabled = !viewModel.cashoutBusy,
                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = OnGold)
            ) {
                Text(if (viewModel.cashoutBusy) "Sending..." else "Send request")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = { viewModel.closeCashout() }) {
                Text("Cancel", color = MutedSage)
            }
        },
        containerColor = ForestGreenCard
    )
}

@Composable
private fun EarnRow(title: String, body: String) {
    Row {
        Text(
            text = "● ",
            color = Gold,
            style = MaterialTheme.typography.bodyMedium
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CreamWhite
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedSage
            )
        }
    }
}

/**
 * Wallet balance comes ONLY from the server ledger — never computed on the phone.
 */
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
            cashoutMessage = "Minimum is ${EconomyConfig.CASHOUT_MIN_COINS} coins (Rs 100)."
            return
        }
        if (cashoutTitle.isBlank() || cashoutNumber.isBlank()) {
            cashoutMessage = "Enter the account title and number."
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
