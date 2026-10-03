package com.fitpaisa.app.feature.welcome

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitpaisa.app.ui.theme.CoinLogo
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold

/**
 * Welcome / onboarding: brand promise, Pakistan launch notice, 18+
 * confirmation and terms agreement. Both boxes must be ticked before the
 * gold "Get Started" button enables.
 */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    var adultConfirmed by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    val canContinue = adultConfirmed && termsAccepted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoinLogo(size = 88.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "fitPaisa",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Walk. Earn. Repeat.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = Gold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Turn your daily steps into coins. Level up from Starter to " +
                "Champion and cash out to Easypaisa, JazzCash, or your bank account.",
            style = MaterialTheme.typography.bodyLarge,
            color = CreamWhite,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Made for Pakistan. Walk daily, turn your steps into FitCoins, " +
                "and cash out your earnings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSage,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        CheckRow(
            checked = adultConfirmed,
            onCheckedChange = { adultConfirmed = it },
            label = "I confirm I am 18 years or older."
        )
        Spacer(Modifier.height(8.dp))
        CheckRow(
            checked = termsAccepted,
            onCheckedChange = { termsAccepted = it },
            label = "I agree to the Terms of Service and Privacy Policy."
        )
        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onGetStarted,
            enabled = canContinue,
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
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        if (!canContinue) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Please tick both boxes to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Gold,
                checkmarkColor = OnGold,
                uncheckedColor = MutedSage
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CreamWhite,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
