package com.helpofai.videoplayer.core.ads

import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.helpofai.videoplayer.R

@Composable
fun NativeAdCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    
    DisposableEffect(Unit) {
        nativeAd = AdManager.consumeNativeAd(context)
        onDispose {
            nativeAd?.destroy()
        }
    }
    
    if (nativeAd != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    // For a complete app, you should inflate an XML layout containing a NativeAdView
                    // Here we create a simple programmatic NativeAdView for demonstration
                    val adView = NativeAdView(ctx)
                    
                    val adTitle = TextView(ctx).apply { 
                        textSize = 16f
                        setTextColor(android.graphics.Color.BLACK)
                    }
                    val adBody = TextView(ctx).apply { 
                        textSize = 12f
                        setTextColor(android.graphics.Color.DKGRAY)
                    }
                    val adCallToAction = Button(ctx)
                    val adIcon = ImageView(ctx)
                    
                    // Simple vertical layout inside the NativeAdView
                    val layout = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(16, 16, 16, 16)
                        
                        addView(adTitle)
                        addView(adBody)
                        addView(adCallToAction)
                    }
                    adView.addView(layout)
                    
                    // Assign views to NativeAdView
                    adView.headlineView = adTitle
                    adView.bodyView = adBody
                    adView.callToActionView = adCallToAction
                    adView.iconView = adIcon
                    
                    // Populate views with ad data
                    nativeAd?.let { ad ->
                        adTitle.text = ad.headline
                        adBody.text = ad.body
                        if (ad.callToAction == null) {
                            adCallToAction.visibility = android.view.View.INVISIBLE
                        } else {
                            adCallToAction.visibility = android.view.View.VISIBLE
                            adCallToAction.text = ad.callToAction
                        }
                        
                        adView.setNativeAd(ad)
                    }
                    
                    adView
                }
            )
        }
    }
}
