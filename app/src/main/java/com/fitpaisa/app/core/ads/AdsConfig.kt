package com.fitpaisa.app.core.ads

/**
 * Ad network configuration.
 *
 * AdMob is now the primary network (real IDs from admob.google.com).
 * Unity Ads stays as a fallback. Debug builds use Google's official
 * AdMob test ad unit so testing never counts as a real-ad click.
 */
object AdsConfig {
    // AdMob: real fitPaisa rewarded ad unit (release builds only).
    const val ADMOB_REWARDED_AD_UNIT_ID = "ca-app-pub-7360446604215991/5669568491"

    // Google's official test rewarded ad unit (debug builds only).
    const val ADMOB_TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Kept for reference; use ADMOB_REWARDED_AD_UNIT_ID for real ads.
    const val REWARDED_AD_UNIT_ID = ADMOB_TEST_REWARDED_AD_UNIT_ID

    // Unity Ads (fallback). Replace with your real one.
    const val UNITY_GAME_ID = "800396858"
    // Unity Ads test rewarded ad unit ID. Replace with your real one.
    const val UNITY_REWARDED_AD_UNIT_ID = "BP_Rewarded_Android"
    // Set to false before release.
    const val UNITY_TEST_MODE = true
}
