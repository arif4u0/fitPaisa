package com.fitpaisa.app.feature.profile

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedButton
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
import com.fitpaisa.app.BuildConfig
import com.fitpaisa.app.core.auth.AuthRepository
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ErrorRed
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val rewardsRepository: com.fitpaisa.app.core.rewards.RewardsRepository
) : ViewModel() {

    var email by mutableStateOf<String?>(null)
        private set
    var isAdmin by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            email = authRepository.currentUserEmail()
            // Founder fallback: always treat the founder's email as admin,
            // even if the profiles read is blocked by RLS.
            isAdmin = rewardsRepository.isAdmin() ||
                email.equals("arifhassas33@gmail.com", ignoreCase = true)
        }
        loadReferralCode()
    }

    fun signOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onDone()
        }
    }

    // Referrals
    var referralCode by mutableStateOf<String?>(null)
        private set
    var referralInput by mutableStateOf("")
        private set
    var referralMessage by mutableStateOf<String?>(null)
        private set
    var referralBusy by mutableStateOf(false)
        private set

    fun loadReferralCode() {
        viewModelScope.launch {
            referralCode = rewardsRepository.myReferralCode()
        }
    }

    fun onReferralInputChange(v: String) {
        referralInput = v.uppercase().filter { it.isLetterOrDigit() }.take(8)
        referralMessage = null
    }

    fun claimReferral() {
        if (referralBusy || referralInput.isBlank()) return
        viewModelScope.launch {
            referralBusy = true
            val (ok, message) = rewardsRepository.claimReferral(referralInput)
            referralMessage = message
            referralBusy = false
            if (ok) referralInput = ""
        }
    }
}

/**
 * Profile: signed-in account, app version, launch note, and sign out.
 */
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onAdminClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Signed in as",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = viewModel.email ?: "Loading…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CreamWhite
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "App version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedSage
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "fitPaisa ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CreamWhite
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Made for Pakistan. Walk daily, earn FitCoins, and cash out " +
                        "your earnings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSage
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        // Referrals: share your code, or enter a friend's code.
        ReferralCard(viewModel = viewModel)

        Spacer(Modifier.height(12.dp))

        if (viewModel.isAdmin) {
            Button(
                onClick = onAdminClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Gold,
                    contentColor = OnGold
                )
            ) {
                Text(
                    text = "Admin panel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = { viewModel.signOut(onSignedOut) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
        ) {
            Text(
                text = "Sign out",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ReferralCard(viewModel: ProfileViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Invite friends, earn coins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CreamWhite
            )
            Text(
                text = "Share your code — you both get 500 FitCoins (Rs 5) when they join.",
                style = MaterialTheme.typography.bodySmall,
                color = MutedSage
            )

            // My code + share button.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = viewModel.referralCode ?: "…",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val code = viewModel.referralCode ?: return@Button
                        val text = "Join me on fitPaisa — walk and earn real rewards! " +
                            "Use my code $code when you sign up and we both get 500 FitCoins."
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // WhatsApp not installed — fall back to generic share.
                            val generic = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(generic, "Share invite")
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = OnGold
                    )
                ) {
                    Text("Share", fontWeight = FontWeight.Bold)
                }
            }

            androidx.compose.material3.HorizontalDivider(color = MutedSage.copy(alpha = 0.3f))

            // Enter a friend's code.
            Text(
                text = "Have a friend's code?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = CreamWhite
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = viewModel.referralInput,
                    onValueChange = viewModel::onReferralInputChange,
                    label = { Text("Enter code", color = MutedSage) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CreamWhite,
                        unfocusedTextColor = CreamWhite,
                        focusedBorderColor = Gold,
                        unfocusedBorderColor = MutedSage
                    )
                )
                Button(
                    onClick = viewModel::claimReferral,
                    enabled = !viewModel.referralBusy && viewModel.referralInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = OnGold
                    )
                ) {
                    Text(
                        if (viewModel.referralBusy) "…" else "Apply",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            viewModel.referralMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("+")) Gold else Color(0xFFE57373)
                )
            }
        }
    }
}
