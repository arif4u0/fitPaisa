package com.fitpaisa.app.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages AdMob rewarded ads. The step→coin Convert flow requires watching
 * an ad: the app shows the ad first, and only calls the server to credit
 * coins after the user earns the reward.
 *
 * Uses Google's test ad unit ID. Replace with the real one from
 * admob.google.com before release (see AdsConfig).
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    init {
        MobileAds.initialize(context) {}
    }

    /** Preloads a rewarded ad so it's ready when the user taps Convert. */
    fun preload() {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            AdsConfig.REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Shows the rewarded ad. Returns true only if the user watched it to the
     * end (earned the reward), false if it failed, was dismissed early, or
     * wasn't loaded.
     */
    suspend fun showRewarded(activity: Activity): Boolean =
        suspendCancellableCoroutine { cont ->
            val ad = rewardedAd
            if (ad == null) {
                // No ad ready — preload for next time and report failure.
                preload()
                cont.resume(false)
                return@suspendCancellableCoroutine
            }
            rewardedAd = null // consume it
            preload() // load the next one in the background

            var settled = false
            fun settle(result: Boolean) {
                if (!settled) {
                    settled = true
                    cont.resume(result)
                }
            }

            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // If dismissed without the reward callback firing,
                    // the user skipped early.
                    settle(false)
                }

                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    settle(false)
                }
            }
            ad.show(activity) {
                // onUserEarnedReward: fired only when fully watched.
                settle(true)
            }
        }

    /** True if an ad is preloaded and ready to show. */
    fun isReady(): Boolean = rewardedAd != null
}
