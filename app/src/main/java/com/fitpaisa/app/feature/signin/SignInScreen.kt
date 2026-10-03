package com.fitpaisa.app.feature.signin

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitpaisa.app.core.auth.AuthRepository
import com.fitpaisa.app.ui.theme.CoinLogo
import com.fitpaisa.app.ui.theme.CreamWhite
import com.fitpaisa.app.ui.theme.ErrorRed
import com.fitpaisa.app.ui.theme.ForestGreenCard
import com.fitpaisa.app.ui.theme.Gold
import com.fitpaisa.app.ui.theme.GoogleBlue
import com.fitpaisa.app.ui.theme.MutedSage
import com.fitpaisa.app.ui.theme.OnGold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUiState(
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    var uiState by mutableStateOf(SignInUiState())
        private set

    fun signIn(activity: Activity, onSuccess: () -> Unit) {
        if (uiState.loading) return
        uiState = SignInUiState(loading = true)
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activity)
            uiState = if (result.isSuccess) {
                onSuccess()
                SignInUiState()
            } else {
                SignInUiState(error = result.exceptionOrNull()?.message ?: "Sign-in failed")
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(error = null)
    }
}

/**
 * Sign-in: brand mark, "Welcome back", and a single white
 * "Continue with Google" button. Shows a spinner while signing in and a
 * plain-language error if it fails.
 */
@Composable
fun SignInScreen(
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState
    val activity = context as? Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CoinLogo(size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sign in to keep your steps, coins, and level safe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedSage,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ForestGreenCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.loading) {
                    CircularProgressIndicator(color = Gold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Signing you in…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSage
                    )
                } else {
                    Button(
                        onClick = {
                            viewModel.clearError()
                            activity?.let { viewModel.signIn(it, onSignedIn) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CreamWhite,
                            contentColor = OnGold
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "G",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = GoogleBlue
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                state.error?.let { error ->
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = friendlyError(error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Closed pilot for Pakistan · 18+ only",
            style = MaterialTheme.typography.bodySmall,
            color = MutedSage
        )
    }
}

/** Turns technical failures into plain-language messages. */
private fun friendlyError(raw: String): String = when {
    raw.contains("cancelled", ignoreCase = true) ->
        "Sign-in was cancelled. Tap the button to try again."
    raw.contains("not configured", ignoreCase = true) ->
        "The app is not connected yet. Please finish the setup steps first."
    raw.contains("network", ignoreCase = true) ->
        "No internet connection. Check your connection and try again."
    else -> "Sign-in didn't work: $raw"
}
