package com.fitpaisa.app.core.network

/**
 * Canonical names of the Supabase Edge Functions (see supabase/functions/).
 * Invoke via SupabaseClientProvider.client.functions.invoke(name, body).
 * M0 ships the function sources; M1+ wires the client calls.
 */
object EdgeFunctions {
    const val SYNC_STEPS = "sync-steps"
    const val LEVELPLAY_CALLBACK = "levelplay-callback"
    const val OFFERWALL_CALLBACK = "offerwall-callback"
    const val CREATE_CASHOUT = "create-cashout"
    const val REVIEW_CASHOUT = "review-cashout"
    const val DELETE_ACCOUNT = "delete-account"
}
