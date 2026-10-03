package com.fitpaisa.app.core.ui

/**
 * Placeholder dashboard numbers shown in the UI until M1 lands.
 *
 * M1: every value here must be replaced with live data —
 *  - stepsToday / stepsGoal <- Health Connect StepsRecord + reward_rules.daily_cap
 *  - coins <- server ledger balance (never computed on the phone)
 *  - level / levelName <- server profile level
 *  - lastSyncText <- WorkManager sync worker status
 * Keeping them in one object avoids the screens drifting apart.
 */
object PilotData {
    const val STEPS_TODAY = 7432
    const val STEPS_GOAL = 10000
    const val COINS = 1240
    const val LEVEL = 1
    const val LEVEL_NAME = "Starter"
    const val LAST_SYNC_TEXT = "Synced 5m ago"
}

/** The five earning levels, matching supabase/seed.sql. */
data class AppLevel(
    val number: Int,
    val name: String,
    val lifetimeStepsRequired: Long
)

val APP_LEVELS = listOf(
    AppLevel(1, "Starter", 0L),
    AppLevel(2, "Walker", 25_000L),
    AppLevel(3, "Strider", 75_000L),
    AppLevel(4, "Athlete", 150_000L),
    AppLevel(5, "Champion", 300_000L)
)
