package com.fitpaisa.app.core.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authSessionDataStore by preferencesDataStore(name = "fitpaisa_auth_session")

/**
 * Persists the Supabase auth session (access + refresh tokens) in DataStore so
 * sign-in survives app restarts. The supabase-kt Auth plugin only keeps the
 * session in memory unless a [SessionManager] is supplied — without this,
 * every cold start forces a fresh login.
 *
 * Wired in [SupabaseClientProvider]: Auth auto-loads the session at startup,
 * auto-saves on change (sign-in / token refresh), and deletes it on sign-out.
 */
@Singleton
class DataStoreSessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SessionManager {

    private object Keys {
        val SESSION_JSON = stringPreferencesKey("user_session_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveSession(session: UserSession) {
        val raw = Json.encodeToString(UserSession.serializer(), session)
        context.authSessionDataStore.edit { it[Keys.SESSION_JSON] = raw }
    }

    override suspend fun loadSession(): UserSession? {
        val raw = context.authSessionDataStore.data
            .map { it[Keys.SESSION_JSON] }
            .first() ?: return null
        return try {
            json.decodeFromString(UserSession.serializer(), raw)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        context.authSessionDataStore.edit { it.remove(Keys.SESSION_JSON) }
    }
}
