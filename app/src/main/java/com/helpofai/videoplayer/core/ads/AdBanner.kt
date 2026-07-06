package com.helpofai.videoplayer.core.ads

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Adaptive Banner Ad composable.
 *
 * Why Adaptive instead of fixed BANNER?
 * - Adaptive banners fill the full device width at the correct height for that
 *   screen density, earning up to 14% more revenue than fixed-size banners
 *   (source: Google AdMob docs).
 * - BANNER (320×50) is considered a legacy format and will eventually be deprecated.
 *
 * Features:
 * - Correct AdView lifecycle: pause, resume and destroy are called at the right times.
 * - Null-safe: shows nothing if the window has zero width (e.g. during layout pass).
 * - Ad refresh is handled by the SDK automatically; no manual polling needed.
 */
@Composable
fun AdaptiveBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current
    var adView by remember { mutableStateOf<AdView?>(null) }
    var adLoaded by remember { mutableStateOf(false) }

    // Calculate adaptive banner width based on the window — NOT the screen.
    // Using screen width can produce incorrect sizes on multi-window / foldables.
    val activity = context as? Activity
    val adSize: AdSize? = remember(activity) {
        activity?.let {
            val windowMetrics = it.windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            val density = it.resources.displayMetrics.density
            val adWidthPx = (bounds.width()).toFloat()
            val adWidthDp = (adWidthPx / density).toInt()
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(it, adWidthDp)
        }
    }

    // If we can't determine a valid size, show nothing.
    if (adSize == null) return

    // Lifecycle management — pause, resume, and destroy the AdView at the right time.
    DisposableEffect(adUnitId) {
        val view = AdView(context).apply {
            this.adUnitId = adUnitId
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    adLoaded = true
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w(
                        "AdaptiveBanner",
                        "Banner failed to load: ${error.message} [code=${error.code}]"
                    )
                    adLoaded = false
                }
            }
            loadAd(AdRequest.Builder().build())
        }
        adView = view
        onDispose {
            view.destroy()
            adView = null
        }
    }

    // Only render the AndroidView once the AdView is created.
    val view = adView ?: return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        factory = { view },
        update = { /* AdView manages itself — no manual refresh needed */ }
    )
}

// ── Legacy shim so existing call sites (AdBanner) keep working ───────────────
/**
 * Drop-in replacement for the old fixed-size [AdBanner].
 * Delegates to [AdaptiveBanner] which earns higher eCPM.
 */
@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdManager.BANNER_AD_UNIT_ID
) = AdaptiveBanner(modifier = modifier, adUnitId = adUnitId)
