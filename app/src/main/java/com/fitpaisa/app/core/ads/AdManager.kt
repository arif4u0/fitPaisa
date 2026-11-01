package com.fitpaisa.app.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.fitpaisa.app.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsShowOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manages rewarded ads via Unity Ads with automatic preloading,
 * failure retry backoff, and a warm-up wait window for immediate ad playback.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FitPaisaAds"
        private const val MAX_AWAIT_TIME_MS = 6000L
        private const val POLL_INTERVAL_MS = 250L
    }

    private val scope = CoroutineScope(Dispatchers.Main)
    private var isInitialized = false
    private var isLoading = false
    private var isLoaded = false
    private var retryAttempt = 0

    var lastError: String = "Not initialized yet"
        private set

    init {
        initialize()
    }

    private fun initialize() {
        Log.d(TAG, "Initializing Unity Ads. GameID: ${AdsConfig.UNITY_GAME_ID}, TestMode: ${AdsConfig.UNITY_TEST_MODE}")
        UnityAds.setDebugMode(BuildConfig.DEBUG)
        UnityAds.initialize(
            context,
            AdsConfig.UNITY_GAME_ID,
            AdsConfig.UNITY_TEST_MODE,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitialized = true
                    lastError = "Unity Ads initialized successfully"
                    Log.d(TAG, "Unity Ads initialization completed successfully.")
                    preload()
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    isInitialized = false
                    lastError = "Init failed: $error - $message"
                    Log.e(TAG, "Unity Ads initialization failed: $error - $message")
                }
            }
        )
    }

    /** Preloads a rewarded ad so it's ready when the user taps Convert or Spin. */
    fun preload() {
        if (!isInitialized) {
            Log.d(TAG, "Preload requested but Unity Ads is not initialized yet. Calling initialize().")
            initialize()
            return
        }
        if (isLoaded) {
            Log.d(TAG, "Rewarded ad is already loaded and ready.")
            return
        }
        if (isLoading) {
            Log.d(TAG, "Rewarded ad is currently loading. Skipping duplicate preload.")
            return
        }

        isLoading = true
        Log.d(TAG, "Requesting Unity Ads load for placement: ${AdsConfig.UNITY_REWARDED_AD_UNIT_ID}")

        UnityAds.load(
            AdsConfig.UNITY_REWARDED_AD_UNIT_ID,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(adUnitId: String) {
                    isLoaded = true
                    isLoading = false
                    retryAttempt = 0
                    lastError = "Ad loaded OK"
                    Log.d(TAG, "Unity rewarded ad loaded successfully for placement: $adUnitId")
                }

                override fun onUnityAdsFailedToLoad(
                    adUnitId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String
                ) {
                    isLoaded = false
                    isLoading = false
                    lastError = "Load failed: $error - $message"
                    Log.e(TAG, "Unity Ads failed to load: $error - $message (Placement: $adUnitId)")

                    // Auto-retry with backoff up to 30 seconds
                    retryAttempt++
                    val backoffSeconds = (retryAttempt * 5).coerceAtMost(30)
                    Log.d(TAG, "Scheduling retry #$retryAttempt in ${backoffSeconds}s")
                    scope.launch {
                        delay(backoffSeconds * 1000L)
                        if (!isLoaded && !isLoading) {
                            preload()
                        }
                    }
                }
            }
        )
    }

    /**
     * Shows the rewarded ad.
     * If the ad is still loading, awaits up to 3.5s before reporting NOT_READY,
     * preventing false "ad not ready" dialogs right after app open.
     */
    suspend fun showRewarded(activity: Activity): AdOutcome {
        // If not loaded, give it a warm-up window of up to 3.5 seconds
        if (!isLoaded) {
            Log.d(TAG, "Ad not loaded at tap time. Waiting up to ${MAX_AWAIT_TIME_MS}ms for load completion...")
            preload()
            val loadedInTime = withTimeoutOrNull(MAX_AWAIT_TIME_MS) {
                while (!isLoaded && isLoading) {
                    delay(POLL_INTERVAL_MS)
                }
                isLoaded
            } ?: false

            if (!loadedInTime && !isLoaded) {
                Log.w(TAG, "Ad was not ready after warm-up wait. Returning NOT_READY.")
                return AdOutcome.NOT_READY
            }
        }

        return showUnityRewarded(activity)
    }

    /** Unity rewarded show execution. */
    private suspend fun showUnityRewarded(activity: Activity): AdOutcome =
        suspendCancellableCoroutine { cont ->
            if (!isLoaded) {
                preload()
                cont.resume(AdOutcome.NOT_READY)
                return@suspendCancellableCoroutine
            }

            // Consume currently loaded ad
            isLoaded = false
            var settled = false

            fun settle(result: AdOutcome) {
                if (!settled) {
                    settled = true
                    // Immediately pre-fetch the next ad for future actions
                    preload()
                    cont.resume(result)
                }
            }

            Log.d(TAG, "Showing Unity rewarded ad on placement: ${AdsConfig.UNITY_REWARDED_AD_UNIT_ID}")

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
                        Log.e(TAG, "Unity Ads show failed: $error - $message")
                        settle(AdOutcome.NOT_READY)
                    }

                    override fun onUnityAdsShowStart(adUnitId: String) {
                        Log.d(TAG, "Unity Ads show started.")
                    }

                    override fun onUnityAdsShowClick(adUnitId: String) {
                        Log.d(TAG, "Unity Ads ad clicked by user.")
                    }

                    override fun onUnityAdsShowComplete(
                        adUnitId: String,
                        state: UnityAds.UnityAdsShowCompletionState
                    ) {
                        Log.d(TAG, "Unity Ads show completed with state: $state")
                        if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                            settle(AdOutcome.WATCHED)
                        } else {
                            settle(AdOutcome.SKIPPED)
                        }
                    }
                }
            )
        }

    /** True if a Unity ad is preloaded and ready to show. */
    fun isReady(): Boolean = isLoaded
}

/** What happened when a rewarded ad was requested. */
enum class AdOutcome {
    /** The user watched the ad to the end. Reward granted. */
    WATCHED,
    /** No ad was ready or it failed to load/show. */
    NOT_READY,
    /** An ad started but the user closed it before it finished. No reward. */
    SKIPPED
}
