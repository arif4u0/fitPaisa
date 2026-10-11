package com.fitpaisa.app.core.ads

/**
 * Ad network configuration.
 *
 * Unity Ads test IDs are used for now — safe to build and test with.
 * Before release: create a project at dashboard.unity3d.com and
 * replace UNITY_GAME_ID and UNITY_REWARDED_AD_UNIT_ID with your real IDs.
 * (AdMob is kept as a fallback; its account is currently closed.)
 */
object AdsConfig {
    // Google's official test rewarded ad unit ID (AdMob fallback).
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Unity Ads test Game ID (Android). Replace with your real one.
    const val UNITY_GAME_ID = "800394765"
    // Unity Ads test rewarded ad unit ID. Replace with your real one.
    const val UNITY_REWARDED_AD_UNIT_ID = "BP_Rewarded_Android"
    // Set to false before release.
    const val UNITY_TEST_MODE = false
}
