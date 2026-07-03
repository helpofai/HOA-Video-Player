package com.helpofai.videoplayer.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
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

object AdManager {
    private const val TAG = "AdManager"
    
    // Ad Unit IDs from BuildConfig
    val BANNER_AD_UNIT_ID = BuildConfig.ADMOB_BANNER_ID
    val INTERSTITIAL_AD_UNIT_ID = BuildConfig.ADMOB_INTERSTITIAL_ID
    val REWARDED_AD_UNIT_ID = BuildConfig.ADMOB_REWARDED_ID
    val NATIVE_AD_UNIT_ID = BuildConfig.ADMOB_NATIVE_ID

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    private var rewardedAd: RewardedInterstitialAd? = null
    private var isRewardedLoading = false
    
    // Cache one native ad for quick display
    var cachedNativeAd: NativeAd? = null
        private set
    private var isNativeLoading = false

    fun init(context: Context) {
        MobileAds.initialize(context) { status ->
            Log.d(TAG, "AdMob Initialized: ${status.adapterStatusMap}")
            // Pre-load ads immediately for quick showing
            loadInterstitialAd(context)
            loadRewardedAd(context)
            loadNativeAd(context)
        }
    }

    private fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialLoading) return

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded.")
                    interstitialAd = ad
                    isInterstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Interstitial ad failed to load: ${error.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad was dismissed.")
                    interstitialAd = null
                    loadInterstitialAd(activity) // Pre-load next one
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.d(TAG, "Interstitial ad failed to show: ${error.message}")
                    interstitialAd = null
                    onAdDismissed()
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            onAdDismissed()
            loadInterstitialAd(activity)
        }
    }

    private fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedLoading) return
        
        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        
        RewardedInterstitialAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedInterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedInterstitialAd) {
                Log.d(TAG, "Rewarded interstitial ad loaded.")
                rewardedAd = ad
                isRewardedLoading = false
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.d(TAG, "Rewarded interstitial ad failed to load: ${error.message}")
                rewardedAd = null
                isRewardedLoading = false
            }
        })
    }
    
    fun showRewardedAd(activity: Activity, onRewarded: (Int, String) -> Unit, onDismissed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd(activity)
                    onDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    rewardedAd = null
                    onDismissed()
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                onRewarded(rewardItem.amount, rewardItem.type)
            }
        } else {
            Log.d(TAG, "Rewarded ad wasn't ready yet.")
            onDismissed()
            loadRewardedAd(activity)
        }
    }
    
    fun loadNativeAd(context: Context) {
        if (cachedNativeAd != null || isNativeLoading) return
        
        isNativeLoading = true
        val adLoader = AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                // Ensure any previously cached ad is destroyed
                cachedNativeAd?.destroy()
                cachedNativeAd = nativeAd
                isNativeLoading = false
                Log.d(TAG, "Native ad loaded.")
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d(TAG, "Native ad failed to load: ${error.message}")
                    isNativeLoading = false
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            
        adLoader.loadAd(AdRequest.Builder().build())
    }
    
    fun consumeNativeAd(context: Context): NativeAd? {
        val ad = cachedNativeAd
        cachedNativeAd = null
        loadNativeAd(context) // Preload next one
        return ad
    }
}
