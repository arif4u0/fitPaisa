package com.fitpaisa.app.core.ads

import android.app.Activity
import android.content.Context
import com.fitpaisa.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * AdMob rewarded ads (primary network).
 *
 * Debug builds always use Google's official test ad unit, so tapping
 * Convert while testing never counts as a real-ad click. Release builds
 * use the real fitPaisa ad unit from AdsConfig.
 */
@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    var lastError: String = "Not loaded yet"
        private set

    init {
        MobileAds.initialize(context) {}
        preload()
    }

    fun isReady(): Boolean = rewardedAd != null

    fun preload() {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        val adUnitId = if (BuildConfig.DEBUG) {
            AdsConfig.ADMOB_TEST_REWARDED_AD_UNIT_ID
        } else {
            AdsConfig.ADMOB_REWARDED_AD_UNIT_ID
        }
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    lastError = "Ad loaded OK"
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                    lastError = "Load failed: ${error.code} - ${error.message}"
                }
            }
        )
    }

    suspend fun showRewarded(activity: Activity): AdOutcome =
        suspendCancellableCoroutine { cont ->
            val ad = rewardedAd
            if (ad == null) {
                preload()
                cont.resume(AdOutcome.NOT_READY)
                return@suspendCancellableCoroutine
            }
            rewardedAd = null // consume it
            preload() // load the next one

            var settled = false
            var earned = false
            fun settle(result: AdOutcome) {
                if (!settled) {
                    settled = true
                    cont.resume(result)
                }
            }

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    settle(if (earned) AdOutcome.WATCHED else AdOutcome.SKIPPED)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    lastError = "Show failed: ${error.code} - ${error.message}"
                    settle(AdOutcome.NOT_READY)
                }
            }

            ad.show(
                activity,
                OnUserEarnedRewardListener { _: RewardItem ->
                    earned = true
                }
            )
        }
}
