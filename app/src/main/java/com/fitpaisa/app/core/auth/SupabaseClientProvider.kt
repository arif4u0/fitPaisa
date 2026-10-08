package com.fitpaisa.app.core.auth

import android.content.Context
import com.fitpaisa.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single Supabase client for the app. URL and anon key come from BuildConfig,
 * which is fed by android/local.properties (git-ignored) or CI env vars.
 * Real secrets are never hardcoded — see README.
 *
 * The client is created lazily (on first use, not at app start) so a missing
 * or mistyped key can never crash the app on launch. The worst case is a
 * human-readable error at sign-in time. Values are trimmed to survive
 * accidental whitespace from copy-paste.
 *
 * Auth sessions are persisted via [DataStoreSessionManager] so sign-in
 * survives app restarts (the SDK only keeps the session in memory by default).
 */
@Singleton
class SupabaseClientProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: DataStoreSessionManager
) {

    val client: SupabaseClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val rawUrl = BuildConfig.SUPABASE_URL.trim().removePrefix("supabase.url=")
        val cleanUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            rawUrl
        } else {
            "https://$rawUrl"
        }
        createSupabaseClient(
            supabaseUrl = cleanUrl,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY.trim()
        ) {
            install(Postgrest)
            install(Auth) {
                sessionManager = this@SupabaseClientProvider.sessionManager
            }
            install(Functions)
            install(Storage)
        }
    }

    /** True when real project coordinates are configured (not placeholders). */
    fun isConfigured(): Boolean =
        !BuildConfig.SUPABASE_URL.contains("YOUR_SUPABASE") &&
            !BuildConfig.SUPABASE_ANON_KEY.contains("YOUR_SUPABASE")
}
