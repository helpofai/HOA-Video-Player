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

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Composable-friendly trigger for interstitial ads.
 *
 * Designed for use in nav-graph route composables. When [trigger] flips to true
 * (e.g. the user taps a video), this composable shows the interstitial (if cooldown
 * has elapsed) and then calls [onComplete] to proceed with navigation.
 *
 * This keeps all ad logic out of the UI layer — the UI just sets a Boolean.
 *
 * Example:
 * ```kotlin
 * var showAd by remember { mutableStateOf(false) }
 *
 * InterstitialAdTrigger(
 *     trigger    = showAd,
 *     onComplete = {
 *         showAd = false
 *         navController.navigate("player/$uri")
 *     }
 * )
 *
 * // Somewhere in a click handler:
 * showAd = true
 * ```
 */
@Composable
fun InterstitialAdTrigger(
    trigger:    Boolean,
    onComplete: () -> Unit
) {
    val context  = LocalContext.current
    val activity = remember(context) { context as? Activity }

    LaunchedEffect(trigger) {
        if (!trigger) return@LaunchedEffect
        if (activity != null) {
            AdManager.showInterstitialAd(activity = activity, onComplete = onComplete)
        } else {
            // Not an Activity context — skip ad and proceed.
            onComplete()
        }
    }
}
