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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitpaisa.app.core.ui.PilotData
import com.fitpaisa.app.ui.theme.CoinBalanceCard
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold

/**
 * Wallet: coin balance, disabled cash-out (closed pilot), and a plain-
 * language explainer of how earning works.
 *
 * M1: balance comes from the server ledger; cash-out opens when the pilot
 * ends (app_config.cashout_enabled).
 */
@Composable
fun WalletScreen() {
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
            coins = PilotData.COINS,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { /* disabled during the closed pilot */ },
            enabled = false,
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
        Text(
            text = "Cashouts open after the pilot. Your coins are counted and safe until then.",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSage
        )
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
                    body = "Your eligible daily steps turn into coins, up to your level's daily cap."
                )
                EarnRow(
                    title = "Level up",
                    body = "More lifetime steps unlock higher levels — and bigger daily caps."
                )
                EarnRow(
                    title = "Counted on our server",
                    body = "Coins are verified and stored on fitPaisa's secure server, never on your phone, so nobody can fake them."
                )
            }
        }
    }
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
