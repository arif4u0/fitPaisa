package com.fitpaisa.app.core.auth

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "fitpaisa_session")

/**
 * Persists the signed-in flag (and user id) in DataStore so the session state
 * survives process death and restart. The actual auth tokens are persisted by
 * [DataStoreSessionManager] (wired into the Supabase Auth plugin); this flag
 * gates the cold-start routing without a network round-trip.
 */
@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val USER_ID = stringPreferencesKey("user_id")
        val WELCOME_ACCEPTED = booleanPreferencesKey("welcome_accepted")
    }

    val signedIn: Flow<Boolean> =
        context.sessionDataStore.data.map { it[Keys.SIGNED_IN] == true }

    val userId: Flow<String?> =
        context.sessionDataStore.data.map { it[Keys.USER_ID] }

    val welcomeAccepted: Flow<Boolean> =
        context.sessionDataStore.data.map { it[Keys.WELCOME_ACCEPTED] == true }

    suspend fun isSignedIn(): Boolean = signedIn.first()

    suspend fun setSignedIn(userId: String) {
        context.sessionDataStore.edit {
            it[Keys.SIGNED_IN] = true
            it[Keys.USER_ID] = userId
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }

    /**
     * Ends the session but keeps the welcome-onboarding flag: onboarding is a
     * device-level step, signing out must not force the user through it again.
     */
    suspend fun clearSession() {
        context.sessionDataStore.edit {
            it.remove(Keys.SIGNED_IN)
            it.remove(Keys.USER_ID)
        }
    }

    suspend fun isWelcomeAccepted(): Boolean = welcomeAccepted.first()

    suspend fun setWelcomeAccepted() {
        context.sessionDataStore.edit { it[Keys.WELCOME_ACCEPTED] = true }
    }
}
