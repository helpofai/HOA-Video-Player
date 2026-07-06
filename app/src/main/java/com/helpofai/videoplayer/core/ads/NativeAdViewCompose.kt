/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
|
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
|
| This file is part of the HelpOfAi Professional Software Suite.
| Unauthorized copying, modification, redistribution, reverse engineering,
| decompilation, or commercial use of this source code, in whole or in part,
| is strictly prohibited without prior written permission from the copyright owner.
|
| Author      : Rajib Adhikary
| Organization: HelpOfAi (HOA)
| Website     : https://helpofai.com
| Location    : Basta Purba Para, Aranghata, Nadia, West Bengal, India
|
| This source code contains proprietary and confidential information.
| Any unauthorized access or distribution may violate applicable copyright laws.
|
|--------------------------------------------------------------------------
*/
package com.helpofai.videoplayer.core.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.helpofai.videoplayer.R

/**
 * Composable wrapper for an AdMob Native Ad.
 *
 * AdMob REQUIREMENT (fixes "Advertiser assets outside native ad view"):
 * Every view registered on [NativeAdView] via headlineView, bodyView, iconView,
 * mediaView, advertiserView, callToActionView, starRatingView, priceView, or
 * storeView MUST be a physical descendant of the [NativeAdView] root. This
 * implementation uses an XML layout ([R.layout.ad_native_view]) where the
 * [NativeAdView] IS the root element, guaranteeing all assets are inside it.
 *
 * Programmatic view creation (the old approach) is avoided because it makes it
 * easy to accidentally register a view that was never added to the hierarchy.
 */
@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    DisposableEffect(Unit) {
        // AdManager.consumeNativeAd() returns a pre-loaded ad from the pool,
        // or null if none is available yet.
        nativeAd = AdManager.consumeNativeAd(context)
        onDispose {
            // Must destroy() to free the native ad when composition leaves.
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    val ad = nativeAd ?: return  // Nothing to show if no ad is ready.

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                // ── Step 1: Inflate the XML layout.
                // The root element IS a NativeAdView, so every child view is
                // automatically inside it — satisfying AdMob's containment rule.
                val adView = LayoutInflater.from(ctx)
                    .inflate(R.layout.ad_native_view, null, false) as NativeAdView

                // ── Step 2: Locate every asset view by ID.
                val adIcon       = adView.findViewById<ImageView>(R.id.ad_icon)
                val adHeadline   = adView.findViewById<TextView>(R.id.ad_headline)
                val adAdvertiser = adView.findViewById<TextView>(R.id.ad_advertiser)
                val adMedia      = adView.findViewById<MediaView>(R.id.ad_media)
                val adBody       = adView.findViewById<TextView>(R.id.ad_body)
                val adStars      = adView.findViewById<RatingBar>(R.id.ad_stars)
                val adPrice      = adView.findViewById<TextView>(R.id.ad_price)
                val adCta        = adView.findViewById<Button>(R.id.ad_call_to_action)

                // ── Step 3: Register asset views on the NativeAdView.
                // Registration tells the SDK which views correspond to which asset
                // so click tracking, viewability, and privacy disclosures work.
                adView.headlineView      = adHeadline
                adView.bodyView          = adBody
                adView.iconView          = adIcon
                adView.mediaView         = adMedia
                adView.advertiserView    = adAdvertiser
                adView.callToActionView  = adCta
                adView.starRatingView    = adStars
                adView.priceView         = adPrice

                // ── Step 4: Populate each view, hiding it if the asset is absent.
                // Optional assets (icon, body, stars, price, advertiser) may be
                // null for a given ad. They MUST be hidden (INVISIBLE or GONE)
                // rather than left empty, to avoid layout artifacts.

                // Headline is always present for native ads.
                adHeadline.text = ad.headline

                // Body (optional)
                if (ad.body.isNullOrEmpty()) {
                    adBody.visibility = View.GONE
                } else {
                    adBody.visibility = View.VISIBLE
                    adBody.text = ad.body
                }

                // Icon (optional)
                val icon = ad.icon
                if (icon?.drawable != null) {
                    adIcon.setImageDrawable(icon.drawable)
                    adIcon.visibility = View.VISIBLE
                } else {
                    adIcon.visibility = View.GONE
                }

                // Advertiser name (optional)
                if (ad.advertiser.isNullOrEmpty()) {
                    adAdvertiser.visibility = View.GONE
                } else {
                    adAdvertiser.visibility = View.VISIBLE
                    adAdvertiser.text = ad.advertiser
                }

                // Star rating (optional — only present for app-install ads)
                val starRating = ad.starRating
                if (starRating != null) {
                    adStars.rating = starRating.toFloat()
                    adStars.visibility = View.VISIBLE
                } else {
                    adStars.visibility = View.GONE
                }

                // Price (optional)
                if (ad.price.isNullOrEmpty()) {
                    adPrice.visibility = View.GONE
                } else {
                    adPrice.visibility = View.VISIBLE
                    adPrice.text = ad.price
                }

                // Call-To-Action (optional but nearly always present)
                if (ad.callToAction.isNullOrEmpty()) {
                    adCta.visibility = View.INVISIBLE  // INVISIBLE not GONE — preserve row height
                } else {
                    adCta.visibility = View.VISIBLE
                    adCta.text = ad.callToAction
                }

                // Media view: set media content if available, else hide it.
                // setMediaContent MUST be called before setNativeAd so the SDK
                // can properly size and render the media asset.
                val mediaContent = ad.mediaContent
                if (mediaContent != null) {
                    adMedia.mediaContent = mediaContent
                    adMedia.visibility = View.VISIBLE
                } else {
                    adMedia.visibility = View.GONE
                }

                // ── Step 5: Bind the populated NativeAd to the NativeAdView.
                // This call finalises click area registration and enables
                // SDK-controlled overlays (AdChoices, etc.). It MUST come AFTER
                // all view assignments and AFTER media content is set.
                adView.setNativeAd(ad)

                adView
            },
            // update block: called on recomposition. Since the ad object itself
            // is stable during the lifecycle, we only re-bind if it changed.
            update = { adView ->
                // Re-populate in case the ad reference changed (rare but possible
                // if the DisposableEffect restarts due to a key change).
                adView.setNativeAd(ad)
            }
        )
    }
}
