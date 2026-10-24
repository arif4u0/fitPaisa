package com.fitpaisa.app.core.ads

import android.app.Activity
import android.content.Context
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages rewarded ads. AdMob is tried first; Unity is the fallback.
 * The step→coin Convert flow shows an ad first, and only calls the
 * server to credit coins after the user watches it to the end.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val admobManager: AdMobManager
) {
    private var isInitialized = false
    private var isLoading = false
    private var isLoaded = false
    var lastError: String = "Not initialized yet"
        private set

    init {
        UnityAds.initialize(
            context,
            AdsConfig.UNITY_GAME_ID,
            AdsConfig.UNITY_TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitialized = true
                    preload()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    isInitialized = false
                    lastError = "Init failed: $error - $message"
                }
            }
        )
    }

    /** Preloads a rewarded ad so it's ready when the user taps Convert. */
    fun preload() {
        admobManager.preload()
        if (!isInitialized || isLoaded || isLoading) return
        isLoading = true
        UnityAds.load(
            AdsConfig.UNITY_REWARDED_AD_UNIT_ID,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(adUnitId: String) {
                    isLoaded = true
                    isLoading = false
                    lastError = "Ad loaded OK"
                }

                override fun onUnityAdsFailedToLoad(
                    adUnitId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    isLoaded = false
                    isLoading = false
                    lastError = "Load failed: $error - $message"
                }
            }
        )
    }

    /**
     * Shows the rewarded ad: AdMob first, Unity as fallback.
     *
     * The old Boolean result mixed up two very different failures: an ad
     * that was unavailable, and an ad the user closed early. Callers need
     * to tell them apart, because an unavailable ad should not block the
     * core walk-to-earn loop while an ad network is unreliable.
     */
    suspend fun showRewarded(activity: Activity): AdOutcome {
        val admobOutcome = admobManager.showRewarded(activity)
        if (admobOutcome == AdOutcome.WATCHED || admobOutcome == AdOutcome.SKIPPED) {
            return admobOutcome
        }
        // AdMob had nothing ready: fall back to Unity.
        lastError = admobManager.lastError
        return showUnityRewarded(activity)
    }

    /** Unity fallback path (kept separate so AdMob stays first). */
    private suspend fun showUnityRewarded(activity: Activity): AdOutcome =
        suspendCancellableCoroutine { cont ->
            if (!isLoaded) {
                preload()
                cont.resume(AdOutcome.NOT_READY)
                return@suspendCancellableCoroutine
            }
            isLoaded = false // consume it
            preload() // load the next one

            var settled = false
            fun settle(result: AdOutcome) {
                if (!settled) {
                    settled = true
                    cont.resume(result)
                }
            }

            UnityAds.show(
                activity,
                AdsConfig.UNITY_REWARDED_AD_UNIT_ID,
                UnityAdsShowOptions(),
                object : IUnityAdsShowListener {
                    override fun onUnityAdsShowFailure(
                        adUnitId: String,
                        error: UnityAds.UnityAdsShowError,
                        message: String
                    ) {
                        lastError = "Show failed: $error - $message"
                        settle(AdOutcome.NOT_READY)
                    }

                    override fun onUnityAdsShowStart(adUnitId: String) {}

                    override fun onUnityAdsShowClick(adUnitId: String) {}

                    override fun onUnityAdsShowComplete(
                        adUnitId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        // COMPLETED = watched to the end.
                        settle(
                            if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                AdOutcome.WATCHED
                            } else {
                                AdOutcome.SKIPPED
                            }
                        )
                    }
                }
            )
        }

    /** True if an ad is preloaded and ready to show. */
    fun isReady(): Boolean = admobManager.isReady() || isLoaded
}

/** What happened when a rewarded ad was requested. */
enum class AdOutcome {
    /** The user watched the ad to the end. */
    WATCHED,

    /** No ad was ready or it failed to load/show. Not the user's fault. */
    NOT_READY,

    /** An ad started but the user closed it before it finished. */
    SKIPPED
}
