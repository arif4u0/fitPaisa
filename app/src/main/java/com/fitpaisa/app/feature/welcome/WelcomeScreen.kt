package com.fitpaisa.app.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitpaisa.app.ui.theme.CardBorder
import com.fitpaisa.app.ui.theme.CardBorderGlow
import com.fitpaisa.app.ui.theme.CoinLogo
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.GoldDark
import com.fitpaisa.app.ui.theme.GoldGlow
import com.fitpaisa.app.ui.theme.GoldLight
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.NeonEmerald
import com.fitpaisa.app.ui.theme.OnGold

/**
 * Modern High-Impact Welcome / Onboarding Screen.
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
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing Hero Icon
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Gold.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CoinLogo(size = 80.dp)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "fitPaisa",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = CreamWhite,
            letterSpacing = (-1).sp
        )

        Spacer(Modifier.height(6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Gold.copy(alpha = 0.15f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Gold,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Walk. Earn. Cash Out.",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = GoldLight
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = "Pakistan's #1 walk-to-earn fitness app. Turn physical footsteps into verified FitCoins and withdraw real cash to JazzCash & Easypaisa.",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedSage,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(24.dp))

        // Feature Highlights
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FeatureRow(
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    title = "1,000 Steps = 1 Coin",
                    subtitle = "Automated motion counting via your phone's sensor"
                )
                FeatureRow(
                    icon = Icons.Filled.Payments,
                    title = "Direct Mobile Payouts",
                    subtitle = "Easypaisa, JazzCash, or Pakistani bank transfer"
                )
                FeatureRow(
                    icon = Icons.Filled.Security,
                    title = "Cloud Verified & Safe",
                    subtitle = "Cryptographic ledger prevents duplication & fraud"
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Confirmation Checkboxes
        CheckRow(
            checked = adultConfirmed,
            onCheckedChange = { adultConfirmed = it },
            label = "I confirm I am 18 years of age or older."
        )

        Spacer(Modifier.height(8.dp))

        CheckRow(
            checked = termsAccepted,
            onCheckedChange = { termsAccepted = it },
            label = "I accept the fitPaisa Terms of Service & Privacy Policy."
        )

        Spacer(Modifier.height(24.dp))

        val buttonGradient = if (canContinue) {
            Brush.horizontalGradient(listOf(GoldLight, Gold, GoldDark))
        } else {
            Brush.horizontalGradient(listOf(Gold.copy(alpha = 0.25f), Gold.copy(alpha = 0.20f)))
        }

        Button(
            onClick = onGetStarted,
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (canContinue) 10.dp else 0.dp,
                    shape = RoundedCornerShape(18.dp),
                    spotColor = GoldGlow
                ),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = OnGold,
                disabledContainerColor = ForestGreenCard,
                disabledContentColor = MutedSage
            )
        ) {
            Text(
                text = "Get Started Now",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (canContinue) OnGold else MutedSage
            )
        }

        if (!canContinue) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Please check both boxes above to proceed.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
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
