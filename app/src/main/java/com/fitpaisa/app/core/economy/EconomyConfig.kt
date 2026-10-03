package com.fitpaisa.app.core.economy

/**
 * fitPaisa economics — the single source of truth for every earning number
 * in the app. These are the CONFIRMED commercial rates (2026-10-03).
 *
 * To tune the economy (rates, caps, limits), change the numbers here only —
 * no other code needs to change. The server enforces the same rules via
 * reward_rules / app_config in Supabase; keep both sides in sync.
 *
 * Money rule: 1 FitCoin = 1 paisa. Coins are integers everywhere — no floats.
 */
object EconomyConfig {

    // ---------- Steps → coins ----------
    /** Steps needed to earn one FitCoin. */
    const val STEPS_PER_COIN = 100

    /** Paisa value of one FitCoin (1 coin = 1 paisa, 100 coins = Rs 1). */
    const val COIN_Paisa_VALUE = 1

    // ---------- Ads ----------
    /** Base coins per watched ad, before the level bonus multiplier. */
    const val AD_BASE_COINS = 10

    // ---------- Weekly leaderboard prizes (coins) ----------
    const val LEADERBOARD_PRIZE_1ST = 100
    const val LEADERBOARD_PRIZE_2ND = 50
    const val LEADERBOARD_PRIZE_3RD = 20

    // ---------- Cashout ----------
    /** Minimum coins for one cashout (10,000 coins = Rs 100). */
    const val CASHOUT_MIN_COINS = 10_000

    /**
     * Master switch for real-money payouts. Stays false until the
     * Easypaisa / JazzCash / bank payout flow is set up and approved.
     * Mirrors app_config.cashout_enabled on the server.
     */
    const val CASHOUT_ENABLED = false

    // ---------- Helpers ----------

    /** Whole coins earned for a step count (server is the authority; this mirrors it for display). */
    fun stepsToCoins(steps: Int): Int = (steps / STEPS_PER_COIN).coerceAtLeast(0)

    /** Rupee value of a coin balance, e.g. 1250 coins -> "Rs 12.50". */
    fun coinsToRupeesText(coins: Int): String {
        val totalPaisa = coins * COIN_Paisa_VALUE
        val rupees = totalPaisa / 100
        val paisa = totalPaisa % 100
        return if (paisa == 0) "Rs $rupees" else "Rs $rupees.${paisa.toString().padStart(2, '0')}"
    }

    /** Coins for one watched ad at the given level (1-5). */
    fun adCoinsForLevel(level: Int): Int {
        val multiplier = LEVELS.find { it.number == level }?.adBonusMultiplier ?: 1f
        return (AD_BASE_COINS * multiplier).toInt()
    }

    /** Level info for a level number (1-5), defaulting to Level 1. */
    fun levelInfo(level: Int): LevelInfo =
        LEVELS.find { it.number == level } ?: LEVELS.first()
}

/**
 * One earning tier. Higher levels unlock bigger daily step caps, more ads
 * per day, and a bonus multiplier on ad coins.
 */
data class LevelInfo(
    val number: Int,
    val name: String,
    val lifetimeStepsRequired: Long,
    val dailyStepCap: Int,
    val dailyAdLimit: Int,
    val adBonusMultiplier: Float
)

/** The five earning levels, confirmed 2026-10-03. Mirrors supabase/seed.sql. */
val LEVELS = listOf(
    LevelInfo(1, "Starter", 0L, 10_000, 5, 1.0f),
    LevelInfo(2, "Walker", 50_000L, 12_000, 6, 1.1f),
    LevelInfo(3, "Active", 150_000L, 15_000, 8, 1.25f),
    LevelInfo(4, "Athlete", 300_000L, 20_000, 10, 1.5f),
    LevelInfo(5, "Champion", 500_000L, 25_000, 12, 2.0f)
)
