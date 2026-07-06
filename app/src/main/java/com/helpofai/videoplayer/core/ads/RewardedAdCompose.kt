package com.helpofai.videoplayer.core.ads

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// State machine for the rewarded ad flow
// ─────────────────────────────────────────────────────────────────────────────
sealed interface RewardedAdState {
    data object Idle       : RewardedAdState  // Waiting for user to trigger
    data object Loading    : RewardedAdState  // Ad loading in progress
    data object Ready      : RewardedAdState  // Ad ready — show watch button
    data object Watching   : RewardedAdState  // Ad is on screen
    data class  Rewarded(val amount: Int, val type: String) : RewardedAdState
    data class  Error(val message: String)   : RewardedAdState
}

/**
 * Bottom-sheet–style rewarded ad prompt card.
 *
 * Shows the user a "Watch Ad" offer, tracks loading state, plays the ad,
 * and either delivers the reward or explains why it failed — all in one surface.
 *
 * Usage:
 * ```
 * RewardedAdPromptCard(
 *     title       = "Watch an ad for extra features",
 *     description = "Watch a short ad to unlock X",
 *     onRewarded  = { amount, type -> grantReward() },
 *     onDismiss   = { showCard = false }
 * )
 * ```
 */
@Composable
fun RewardedAdPromptCard(
    title:       String = "Watch & Earn",
    description: String = "Watch a short video ad to unlock this feature",
    onRewarded:  (amount: Int, type: String) -> Unit = { _, _ -> },
    onDismiss:   () -> Unit = {}
) {
    val context  = LocalContext.current
    val activity = context as? Activity
    val adState  by AdManager.availability.collectAsState()

    var state: RewardedAdState by remember { mutableStateOf(
        if (adState.rewardedReady) RewardedAdState.Ready else RewardedAdState.Loading
    ) }

    // Transition to Ready when the ad becomes available.
    LaunchedEffect(adState.rewardedReady) {
        if (adState.rewardedReady && state is RewardedAdState.Loading) {
            state = RewardedAdState.Ready
        }
    }

    // Auto-dismiss the reward success card after 3 s.
    LaunchedEffect(state) {
        if (state is RewardedAdState.Rewarded) {
            delay(3_000)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = true,
        enter   = fadeIn() + scaleIn(initialScale = 0.92f),
        exit    = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier            = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row: icon + dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (state) {
                                is RewardedAdState.Rewarded -> Icons.Default.CheckCircle
                                else -> Icons.Default.CardGiftcard
                            },
                            contentDescription = null,
                            tint = if (state is RewardedAdState.Rewarded)
                                Color(0xFF22C55E) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Title
                Text(
                    text  = when (state) {
                        is RewardedAdState.Rewarded -> "Reward Earned! 🎉"
                        is RewardedAdState.Error    -> "Ad Not Available"
                        else -> title
                    },
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                // Description / status text
                Text(
                    text = when (val s = state) {
                        is RewardedAdState.Loading  -> "Loading ad, please wait…"
                        is RewardedAdState.Ready    -> description
                        is RewardedAdState.Watching -> "Please watch until the end to earn your reward"
                        is RewardedAdState.Rewarded -> "You earned ${s.amount} ${s.type}. Enjoy!"
                        is RewardedAdState.Error    -> s.message
                        else -> description
                    },
                    style     = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(24.dp))

                // Loading indicator
                if (state is RewardedAdState.Loading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // Action buttons
                when (state) {
                    is RewardedAdState.Ready -> {
                        Button(
                            onClick = {
                                if (activity != null) {
                                    state = RewardedAdState.Watching
                                    AdManager.showRewardedAd(
                                        activity    = activity,
                                        onRewarded  = { amount, type ->
                                            state = RewardedAdState.Rewarded(amount, type)
                                            onRewarded(amount, type)
                                        },
                                        onDismissed = {
                                            // If state is still Watching, the user skipped.
                                            if (state is RewardedAdState.Watching) {
                                                state = RewardedAdState.Error(
                                                    "Watch the full ad to earn your reward"
                                                )
                                            }
                                        },
                                        onNotAvailable = {
                                            state = RewardedAdState.Error("No ad available right now. Try again later.")
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape  = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.CardGiftcard,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Watch Ad", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick  = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Text("No Thanks")
                        }
                    }
                    is RewardedAdState.Watching -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color    = MaterialTheme.colorScheme.primary
                        )
                    }
                    is RewardedAdState.Rewarded -> {
                        Button(
                            onClick  = onDismiss,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(14.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
                        ) {
                            Text("Continue", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    is RewardedAdState.Error -> {
                        OutlinedButton(
                            onClick  = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Text("Close")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Countdown rewarded button — inline CTA for placing in lists / player UI
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A button that shows the remaining countdown before a rewarded ad can be shown.
 * When the countdown reaches zero or the ad is already ready it enables itself.
 *
 * Useful in the player UI as an "Unlock HD" or "Remove watermark" CTA.
 */
@Composable
fun RewardedAdButton(
    label:         String  = "Watch Ad to Unlock",
    cooldownSecs:  Int     = 0,
    modifier:      Modifier = Modifier,
    onAdRewarded:  (amount: Int, type: String) -> Unit = { _, _ -> },
    onClick:       () -> Unit = {}
) {
    val adState by AdManager.availability.collectAsState()
    var remaining by remember { mutableIntStateOf(cooldownSecs) }

    // Countdown timer
    LaunchedEffect(cooldownSecs) {
        while (remaining > 0) {
            delay(1_000)
            remaining--
        }
    }

    val isEnabled = adState.rewardedReady && remaining == 0

    Button(
        onClick  = onClick,
        enabled  = isEnabled,
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )
    ) {
        if (!isEnabled && remaining > 0) {
            Text("Wait ${remaining}s", fontWeight = FontWeight.SemiBold)
        } else if (!adState.rewardedReady) {
            CircularProgressIndicator(
                modifier  = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color     = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.width(8.dp))
            Text("Loading ad…")
        } else {
            Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}
