package com.fitpaisa.app.core.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.fitpaisa.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google sign-in via Credential Manager, federated into Supabase Auth.
 *
 * Flow: Credential Manager returns a Google ID token -> exchanged with
 * Supabase Auth (IDToken provider) -> Supabase session persisted by the SDK.
 * The DataStore flag in [SessionStore] mirrors it for fast cold-start routing.
 */
@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabaseProvider: SupabaseClientProvider,
    private val sessionStore: SessionStore
) {
    private val client get() = supabaseProvider.client

    /** True when a usable session exists (M0 exit criterion: survives restart). */
    suspend fun isSignedIn(): Boolean {
        if (!sessionStore.isSignedIn()) return false
        return try {
            // The SDK restores the saved session on a background coroutine at startup.
            // Wait for that to finish first; otherwise every cold start looks "logged out"
            // even though the session is stored correctly.
            client.auth.awaitInitialization()
            client.auth.retrieveUserForCurrentSession(updateSession = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun currentUserEmail(): String? =
        try {
            client.auth.retrieveUserForCurrentSession(updateSession = false)?.email
        } catch (e: Exception) {
            null
        }

    suspend fun signInWithGoogle(activity: Activity): Result<Unit> = runCatching {
        if (!supabaseProvider.isConfigured()) {
            error("Supabase is not configured. Set values in android/local.properties (see README).")
        }
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = try {
            credentialManager.getCredential(context = activity, request = request)
        } catch (e: GetCredentialException) {
            throw IllegalStateException("Google sign-in was cancelled or failed: ${e.message}")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        client.auth.signInWith(IDToken) {
            idToken = googleCredential.idToken
            provider = Google
        }
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Sign-in succeeded but no user was returned.")
        sessionStore.setSignedIn(userId)
    }

    suspend fun signOut() {
        runCatching { client.auth.signOut() }
        sessionStore.clearSession()
    }
}
