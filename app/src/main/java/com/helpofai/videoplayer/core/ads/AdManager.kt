package com.helpofai.videoplayer.core.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.helpofai.videoplayer.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Central ad manager for HOA Video Player.
 *
 * Design principles:
 * - Thread-safe: all state fields are either @Volatile, AtomicXxx, or confined to main thread.
 * - Frequency-capped: interstitials are shown at most once every [INTERSTITIAL_COOLDOWN_MS].
 * - Pre-loading: next ad starts loading immediately after the current one is consumed.
 * - Lifecycle-aware: App Open Ads respect foreground/background via ActivityLifecycleCallbacks.
 * - Observable: exposes StateFlow<AdState> so the UI can react to ad availability changes.
 * - Graceful: every callback checks for nullability and logs failures with context.
 */
object AdManager {

    private const val TAG = "AdManager"

    // ── Ad Unit IDs ───────────────────────────────────────────────────────────
    val BANNER_AD_UNIT_ID       = BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL_AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_ID
    val REWARDED_AD_UNIT_ID     = BuildConfig.ADMOB_REWARDED_ID
    val NATIVE_AD_UNIT_ID       = BuildConfig.ADMOB_NATIVE_ID

    // ── Frequency / Timing ────────────────────────────────────────────────────
    /** Minimum gap (ms) between two interstitial shows. Default: 3 minutes. */
    private const val INTERSTITIAL_COOLDOWN_MS = 3 * 60 * 1000L

    /** Maximum number of videos played before forcing an interstitial show. */
    private const val INTERSTITIAL_MAX_SKIP = 4

    // ── Internal state ────────────────────────────────────────────────────────
    private var isInitialized = false

    // Interstitial
    @Volatile private var interstitialAd: InterstitialAd? = null
    private val isInterstitialLoading = AtomicBoolean(false)
    private val lastInterstitialShowMs  = AtomicLong(0L)
    private val videosSinceInterstitial = AtomicInteger(0)

    // Rewarded Interstitial
    @Volatile private var rewardedAd: RewardedInterstitialAd? = null
    private val isRewardedLoading = AtomicBoolean(false)

    // Native Ad
    @Volatile var cachedNativeAd: NativeAd? = null
        private set
    private val isNativeLoading = AtomicBoolean(false)

    // Current foreground Activity (updated by lifecycle callbacks)
    @Volatile private var currentActivity: Activity? = null

    // ── Observable state ──────────────────────────────────────────────────────
    data class AdAvailability(
        val interstitialReady: Boolean = false,
        val rewardedReady:     Boolean = false,
        val nativeReady:       Boolean = false
    )

    private val _availability = MutableStateFlow(AdAvailability())
    val availability: StateFlow<AdAvailability> = _availability.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Initialisation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Call once from [Application.onCreate].
     * Initialises the Mobile Ads SDK then pre-loads all ad types.
     */
    fun init(application: Application) {
        if (isInitialized) return
        isInitialized = true

        MobileAds.initialize(application) { status ->
            Log.d(TAG, "AdMob SDK initialised: ${status.adapterStatusMap}")
            // Pre-load all ad types immediately after init.
            loadInterstitialAd(application)
            loadRewardedAd(application)
            loadNativeAd(application)
        }

        // Register lifecycle callbacks to track the current foreground Activity.
        application.registerActivityLifecycleCallbacks(AppLifecycleObserver(this))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Banner Ad
    // (No pre-loading needed — AdView handles its own lifecycle)
    // ─────────────────────────────────────────────────────────────────────────
    // See AdaptiveBannerCompose.kt for the Compose wrapper.

    // ─────────────────────────────────────────────────────────────────────────
    // Interstitial Ad
    // ─────────────────────────────────────────────────────────────────────────

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || !isInterstitialLoading.compareAndSet(false, true)) return
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded")
                    interstitialAd = ad
                    isInterstitialLoading.set(false)
                    _availability.value = _availability.value.copy(interstitialReady = true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed: ${error.message} [code=${error.code}]")
                    interstitialAd = null
                    isInterstitialLoading.set(false)
                    // Retry after 30 s using a background thread-safe approach
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        { loadInterstitialAd(context) }, 30_000L
                    )
                }
            }
        )
    }

    /**
     * Show an interstitial with frequency capping.
     *
     * Rules:
     * 1. Must be at least [INTERSTITIAL_COOLDOWN_MS] since last show.
     * 2. OR the user has played [INTERSTITIAL_MAX_SKIP] videos without seeing one.
     *
     * In either case [onComplete] is always called — even when no ad is shown.
     */
    fun showInterstitialAd(activity: Activity, onComplete: () -> Unit) {
        val now = System.currentTimeMillis()
        val msSinceLast = now - lastInterstitialShowMs.get()
        val videoCount  = videosSinceInterstitial.incrementAndGet()

        val cooldownOk = msSinceLast >= INTERSTITIAL_COOLDOWN_MS
        val forcedByCount = videoCount >= INTERSTITIAL_MAX_SKIP

        val ad = interstitialAd
        if (ad != null && (cooldownOk || forcedByCount)) {
            ad.fullScreenContentCallback = buildFullScreenCallback(
                tag         = "Interstitial",
                onDismissed = {
                    interstitialAd = null
                    lastInterstitialShowMs.set(System.currentTimeMillis())
                    videosSinceInterstitial.set(0)
                    _availability.value = _availability.value.copy(interstitialReady = false)
                    loadInterstitialAd(activity.applicationContext)
                    onComplete()
                },
                onFailed = {
                    interstitialAd = null
                    _availability.value = _availability.value.copy(interstitialReady = false)
                    onComplete()
                }
            )
            ad.show(activity)
        } else {
            // Cooldown active or no ad loaded — proceed immediately.
            onComplete()
            // Ensure we're loading one for next time.
            if (ad == null) loadInterstitialAd(activity.applicationContext)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rewarded Interstitial Ad
    // ─────────────────────────────────────────────────────────────────────────

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || !isRewardedLoading.compareAndSet(false, true)) return
        RewardedInterstitialAd.load(
            context.applicationContext,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedInterstitialAd) {
                    Log.d(TAG, "Rewarded interstitial loaded")
                    rewardedAd = ad
                    isRewardedLoading.set(false)
                    _availability.value = _availability.value.copy(rewardedReady = true)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed: ${error.message} [code=${error.code}]")
                    rewardedAd = null
                    isRewardedLoading.set(false)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        { loadRewardedAd(context) }, 30_000L
                    )
                }
            }
        )
    }

    /**
     * Show the rewarded interstitial ad.
     *
     * @param onRewarded Called with (amount, type) when the user earns the reward.
     * @param onDismissed Called after the ad closes regardless of reward status.
     * @param onNotAvailable Called immediately when no ad is loaded.
     */
    fun showRewardedAd(
        activity: Activity,
        onRewarded:      (amount: Int, type: String) -> Unit,
        onDismissed:     () -> Unit,
        onNotAvailable:  () -> Unit = {}
    ) {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready")
            onNotAvailable()
            loadRewardedAd(activity.applicationContext)
            return
        }
        var rewarded = false
        ad.fullScreenContentCallback = buildFullScreenCallback(
            tag         = "Rewarded",
            onDismissed = {
                rewardedAd = null
                _availability.value = _availability.value.copy(rewardedReady = false)
                loadRewardedAd(activity.applicationContext)
                onDismissed()
            },
            onFailed = {
                rewardedAd = null
                _availability.value = _availability.value.copy(rewardedReady = false)
                onDismissed()
            }
        )
        ad.show(activity) { rewardItem ->
            rewarded = true
            onRewarded(rewardItem.amount, rewardItem.type)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Native Ad
    // ─────────────────────────────────────────────────────────────────────────

    fun loadNativeAd(context: Context) {
        if (cachedNativeAd != null || !isNativeLoading.compareAndSet(false, true)) return
        val adLoader = com.google.android.gms.ads.AdLoader.Builder(
            context.applicationContext, NATIVE_AD_UNIT_ID
        )
            .forNativeAd { nativeAd ->
                cachedNativeAd?.destroy()
                cachedNativeAd = nativeAd
                isNativeLoading.set(false)
                _availability.value = _availability.value.copy(nativeReady = true)
                Log.d(TAG, "Native ad loaded")
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native failed: ${error.message} [code=${error.code}]")
                    isNativeLoading.set(false)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                        { loadNativeAd(context) }, 60_000L
                    )
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                    .setRequestMultipleImages(false)
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    /** Consumes the cached native ad (takes ownership) and pre-loads the next one. */
    fun consumeNativeAd(context: Context): NativeAd? {
        val ad = cachedNativeAd
        cachedNativeAd = null
        _availability.value = _availability.value.copy(nativeReady = false)
        loadNativeAd(context)
        return ad
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildFullScreenCallback(
        tag: String,
        onDismissed: () -> Unit,
        onFailed:    () -> Unit
    ) = object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
            Log.d(TAG, "$tag dismissed")
            onDismissed()
        }
        override fun onAdFailedToShowFullScreenContent(error: AdError) {
            Log.w(TAG, "$tag failed to show: ${error.message}")
            onFailed()
        }
        override fun onAdShowedFullScreenContent() {
            Log.d(TAG, "$tag shown")
        }
        override fun onAdImpression() {
            Log.d(TAG, "$tag impression recorded")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Activity lifecycle observer — tracks current foreground Activity.
    // ─────────────────────────────────────────────────────────────────────────

    private class AppLifecycleObserver(
        private val adManager: AdManager
    ) : Application.ActivityLifecycleCallbacks {

        override fun onActivityStarted(activity: Activity)  { adManager.currentActivity = activity }
        override fun onActivityResumed(activity: Activity)  { adManager.currentActivity = activity }
        override fun onActivityDestroyed(activity: Activity) {
            if (adManager.currentActivity === activity) adManager.currentActivity = null
        }

        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
    }
}
