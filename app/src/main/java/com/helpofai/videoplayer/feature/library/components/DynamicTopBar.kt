package com.helpofai.videoplayer.feature.library.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.basicMarquee

/**
 * Dynamic context-aware TopAppBar for HOA Video Player.
 *
 * ─── Always visible ──────────────────────────────────────────────────────────
 *   • App logo (animated, home tab) / folder icon (folders tab) / play icon
 *     (playlists tab) — transitions smoothly between states
 *   • Settings icon (right side, always)
 *
 * ─── Home tab (selectedTab == 0, no sub-folder) ──────────────────────────────
 *   • Lock / Unlock icon
 *   • Storage Dashboard icon
 *   • Watching Habits icon
 *   • Sort & Filter icon
 *   • Search icon
 *
 * ─── Folders tab root (selectedTab == 1, selectedFolder == null) ─────────────
 *   • Search icon
 *   • Sort & Filter icon
 *
 * ─── Folder detail (selectedTab == 1, selectedFolder != null) ────────────────
 *   • Back arrow in navigation icon slot
 *   • Search icon (search within folder)
 *
 * ─── Playlists tab root (selectedTab == 2, selectedFolder == null) ───────────
 *   • Watching Habits icon
 *   • Search icon
 *
 * ─── Playlist detail (selectedTab == 2, selectedFolder != null) ──────────────
 *   • Back arrow in navigation icon slot
 *   • Search icon
 *
 * All action icons animate in/out with fade + scale.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTopBar(
    selectedTab:          Int,
    selectedFolder:       String?,
    playlistTitle:        String?,
    isLocked:             Boolean,
    videoCount:           Int      = 0,
    scrollBehavior:       TopAppBarScrollBehavior,
    onBackClick:          () -> Unit,
    onLockClick:          () -> Unit,
    onStorageClick:       () -> Unit,
    onHabitsClick:        () -> Unit,
    onSortFilterClick:    () -> Unit,
    onSearchClick:        () -> Unit,
    onSettingsClick:      () -> Unit
) {
    // ── Derive context ────────────────────────────────────────────────────────
    val isHome         = selectedTab == 0
    val isFolderRoot   = selectedTab == 1 && selectedFolder == null
    val isFolderDetail = selectedTab == 1 && selectedFolder != null
    val isPlaylistRoot = selectedTab == 2 && selectedFolder == null
    val isPlaylistDetail = selectedTab == 2 && selectedFolder != null
    val hasBackButton  = isFolderDetail || isPlaylistDetail

    TopAppBar(
        // ── Navigation icon (Back) ────────────────────────────────────────────
        navigationIcon = {
            AnimatedVisibility(
                visible = hasBackButton,
                enter   = fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it },
                exit    = fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it }
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },

        // ── Title / Logo area ─────────────────────────────────────────────────
        title = {
            AnimatedContent(
                targetState = Triple(selectedTab, selectedFolder, playlistTitle),
                transitionSpec = {
                    (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f))
                        .togetherWith(fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.92f))
                },
                label = "TopBarTitle"
            ) { (tab, folder, pTitle) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        // Home tab: animated logo + video count badge
                        tab == 0 -> {
                            AppLogoAnimated()
                            Spacer(Modifier.width(10.dp))
                            if (videoCount > 0) {
                                VideoBadge(count = videoCount)
                            }
                        }
                        // Folder detail: folder name
                        tab == 1 && folder != null -> {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                            )
                        }
                        // Folder root
                        tab == 1 -> {
                            TabTitleText("Folders")
                        }
                        // Playlist detail
                        tab == 2 && folder != null -> {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = pTitle ?: folder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                            )
                        }
                        // Playlist root
                        else -> {
                            TabTitleText("Playlists")
                        }
                    }
                }
            }
        },

        // ── Actions (right side) ──────────────────────────────────────────────
        actions = {
            // Lock — Home only
            DynamicActionIcon(visible = isHome) {
                IconButton(onClick = onLockClick) {
                    val icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen
                    val tint = if (isLocked)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(icon, contentDescription = if (isLocked) "Unlock" else "Lock", tint = tint)
                }
            }

            // Storage Dashboard — Home only
            DynamicActionIcon(visible = isHome) {
                IconButton(onClick = onStorageClick) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = "Storage Dashboard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sort & Filter — Home and Folder root
            DynamicActionIcon(visible = isHome || isFolderRoot) {
                IconButton(onClick = onSortFilterClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Sort & Filter",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Watching Habits — Home and Playlists root
            DynamicActionIcon(visible = isHome || isPlaylistRoot) {
                IconButton(onClick = onHabitsClick) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = "Watching Habits",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Search — always visible in every context
            DynamicActionIcon(visible = true) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Settings — always visible in every context
            DynamicActionIcon(visible = true) {
                IconButton(onClick = onSettingsClick) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor      = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        scrollBehavior = scrollBehavior
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: animated action icon wrapper
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DynamicActionIcon(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.7f),
        exit    = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.7f)
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: animated app logo (home tab)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
@Composable
private fun AppLogoAnimated() {
    Box(modifier = Modifier.size(36.dp)) {
        val image = androidx.compose.animation.graphics.vector.AnimatedImageVector
            .animatedVectorResource(id = com.helpofai.videoplayer.R.drawable.ic_logo_animated)
        var atEnd by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { atEnd = true }
        Image(
            painter = androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(
                animatedImageVector = image,
                atEnd = atEnd
            ),
            contentDescription = "HOA Video Player",
            modifier = Modifier.size(36.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: tab title text with gradient accent underline
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TabTitleText(title: String) {
    Column {
        Text(
            text       = title,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color      = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: video count badge next to logo on home
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VideoBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text      = "$count",
            fontSize  = 11.sp,
            fontWeight = FontWeight.Bold,
            color     = MaterialTheme.colorScheme.primary
        )
    }
}
