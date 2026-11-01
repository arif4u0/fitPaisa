package com.fitpaisa.app.core.ads

import com.fitpaisa.app.BuildConfig

/**
 * Ad network configuration for FitPaisa.
 *
 * Unity Ads is the primary rewarded video provider.
 * Game ID and Rewarded Ad Unit ID match the active placement in the Unity Dashboard.
 *
 * UNITY_TEST_MODE is automatically set to:
 * - true in DEBUG builds (test ads with watermark, safe for local testing)
 * - false in RELEASE signed builds (serves REAL live ads and pays real revenue)
 */
object AdsConfig {
    // Unity Ads Game ID from Unity Dashboard (Project: 800410563)
    const val UNITY_GAME_ID = "800410563"

    // Unity Ads Rewarded Placement ID (Active placement: BP_Rewarded_Android)
    const val UNITY_REWARDED_AD_UNIT_ID = "BP_Rewarded_Android"

    // Automatic environment toggle:
    // Debug builds -> test ads; Release builds (signed release APK) -> REAL production ads
    val UNITY_TEST_MODE: Boolean = BuildConfig.DEBUG

    // Legacy AdMob IDs preserved for future multi-network mediation fallback
    const val ADMOB_REWARDED_AD_UNIT_ID = "ca-app-pub-7360446604215991/5669568491"
    const val ADMOB_TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    const val REWARDED_AD_UNIT_ID = ADMOB_TEST_REWARDED_AD_UNIT_ID
}
