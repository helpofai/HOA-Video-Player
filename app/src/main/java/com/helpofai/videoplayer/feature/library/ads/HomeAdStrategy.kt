package com.helpofai.videoplayer.feature.library.ads

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.ads.AdBanner
import com.helpofai.videoplayer.core.ads.NativeAdCard

/**
 * Centralised ad placement strategy for HomeScreen.
 *
 * Rules per section type:
 * ┌──────────────────────────────┬───────────────────────────────┐
 * │ Section                      │ Ad Type                        │
 * ├──────────────────────────────┼───────────────────────────────┤
 * │ After Hero Card              │ Adaptive Banner (non-intrusive)│
 * │ After Continue Watching row  │ Native Ad (high CPM)           │
 * │ After Recommended row        │ Native Ad (already placed)     │
 * │ After Recently Added row     │ Adaptive Banner (already placed│
 * │ After Favorites row          │ Native Ad                      │
 * │ After Resume Playback list   │ Adaptive Banner                │
 * │ After Large Files row        │ Native Ad                      │
 * │ After Short Clips row        │ Adaptive Banner                │
 * │ After Smart Playlists chips  │ Native Ad                      │
 * │ Folders grid — every 3 rows  │ alternating Banner / Native    │
 * │ Folder detail — every 5 rows │ alternating Native / Banner    │
 * │ Playlist grid — every 3 rows │ Native Ad                      │
 * │ Playlist detail— every 5 rows│ Adaptive Banner                │
 * └──────────────────────────────┴───────────────────────────────┘
 *
 * All ad composables fade+slide in with a short animation so they
 * feel natural rather than jarring.
 */

// ─────────────────────────────────────────────────────────────────────────────
// Ad slot types — gives call sites semantic meaning
// ─────────────────────────────────────────────────────────────────────────────
enum class HomeAdSlot {
    AFTER_HERO,
    AFTER_CONTINUE_WATCHING,
    AFTER_FAVORITES,
    AFTER_RESUME_PLAYBACK,
    AFTER_LARGE_FILES,
    AFTER_SHORT_CLIPS,
    AFTER_SMART_PLAYLISTS,
    FOLDER_INLINE,          // inside folder grid rows
    FOLDER_DETAIL_INLINE,   // inside folder detail list rows
    PLAYLIST_INLINE,        // inside playlist grid rows
    PLAYLIST_DETAIL_INLINE  // inside playlist detail list rows
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated wrapper — fade + slide for all in-feed ads
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnimatedAdSlot(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 2 }
    ) {
        Box(modifier = modifier) { content() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// "Sponsored" label strip shown above in-feed native ads
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SponsoredLabel(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Default.Verified,
            contentDescription = null,
            tint = Color(0xFF7C3AED),
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Sponsored",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Banner slot — with label + rounded clip + gentle bottom spacing
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeBannerAd(modifier: Modifier = Modifier) {
    AnimatedAdSlot(modifier = modifier.fillMaxWidth()) {
        Column {
            SponsoredLabel()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                AdBanner(modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Native ad slot — with label + card border
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeNativeAd(modifier: Modifier = Modifier) {
    AnimatedAdSlot(modifier = modifier.fillMaxWidth()) {
        Column {
            SponsoredLabel()
            NativeAdCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section-specific ad composables
// ─────────────────────────────────────────────────────────────────────────────

/** After the hero card — a quiet banner fits the top-of-feed tone */
@Composable
fun AdAfterHero() = HomeBannerAd()

/** After Continue Watching — native ad earns higher CPM in this warm-up zone */
@Composable
fun AdAfterContinueWatching() = HomeNativeAd()

/** After Favorites — premium audience, use native */
@Composable
fun AdAfterFavorites() = HomeNativeAd()

/** After Resume Playback list — banner keeps it lightweight */
@Composable
fun AdAfterResumePlayback() = HomeBannerAd()

/** After Large Files section — native fits a "discovery" ad well */
@Composable
fun AdAfterLargeFiles() = HomeNativeAd()

/** After Short Clips — banner suits the quick-scroll nature */
@Composable
fun AdAfterShortClips() = HomeBannerAd()

/** After Smart Playlists chips — native as a soft landing before the fold */
@Composable
fun AdAfterSmartPlaylists() = HomeNativeAd()

/**
 * Inline ad for folder/playlist grids and detail lists.
 *
 * @param rowIndex  0-based index of the current row
 * @param nativeEvery   show native ad every N rows (0 = never)
 * @param bannerEvery   show banner ad every N rows (0 = never)
 * @param nativeOffset  row offset at which native wins (vs banner)
 */
@Composable
fun InlineRowAd(
    rowIndex: Int,
    nativeEvery:  Int = 4,
    bannerEvery:  Int = 8,
    nativeOffset: Int = 3
) {
    val showNative = nativeEvery > 0 && (rowIndex + 1) % nativeEvery == 0
                     && rowIndex % (nativeEvery * 2) == nativeOffset
    val showBanner = !showNative && bannerEvery > 0 && (rowIndex + 1) % bannerEvery == 0

    when {
        showNative -> HomeNativeAd()
        showBanner -> HomeBannerAd()
    }
}

/**
 * Per-item inline ad for list-mode views where each item is a single row.
 *
 * Shows an alternating native/banner ad every N items starting from item 1.
 *
 * @param itemIndex     0-based index of the current item
 * @param adInterval    show ad every N items (e.g. 3 → after every 3rd item)
 * @param nativeEvery   show native ad every N ads (0 = never use native)
 * @param bannerEvery   show banner ad every N ads (0 = never use banner)
 */
@Composable
fun InlineItemAd(
    itemIndex: Int,
    adInterval:    Int = 3,
    nativeEvery:   Int = 2,
    bannerEvery:   Int = 2,
    nativeOffset:  Int = 0
) {
    val oneBased = itemIndex + 1
    if (adInterval <= 0 || oneBased % adInterval != 0) return

    val adNumber = oneBased / adInterval
    val showNative = nativeEvery > 0 && adNumber % nativeEvery == (nativeOffset + 1) % nativeEvery
    val showBanner = !showNative && bannerEvery > 0 && adNumber % bannerEvery == 0

    when {
        showNative -> HomeNativeAd()
        showBanner -> HomeBannerAd()
    }
}
