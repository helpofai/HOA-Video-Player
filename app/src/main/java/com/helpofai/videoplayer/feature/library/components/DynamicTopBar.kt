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
package com.helpofai.videoplayer.feature.library.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.basicMarquee
import com.helpofai.videoplayer.core.theme.frostedGlass
import com.helpofai.videoplayer.core.theme.ToolIconPalette

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
    isRefreshing:         Boolean = false,
    scrollBehavior:       TopAppBarScrollBehavior,
    onBackClick:          () -> Unit,
    onHabitsClick:        () -> Unit,
    onSortFilterClick:    () -> Unit,
    onSearchClick:        () -> Unit,
    onSettingsClick:      () -> Unit,
    onBookmarksClick:     () -> Unit = {},
    onTrashClick:         () -> Unit = {},
    onCreateNewClick:     () -> Unit = {},
    onRefreshClick:       () -> Unit = {}
) {
    // ── Derive context ────────────────────────────────────────────────────────
    val isHome         = selectedTab == 0
    val isFolderRoot   = selectedTab == 1 && selectedFolder == null
    val isFolderDetail = selectedTab == 1 && selectedFolder != null
    val isPlaylistRoot = selectedTab == 2 && selectedFolder == null
    val isPlaylistDetail = selectedTab == 2 && selectedFolder != null
    val hasBackButton  = isFolderDetail || isPlaylistDetail

    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(density) {
        // Fix scroll limit manually without passing behavior to TopAppBar to prevent double translation
        scrollBehavior.state.heightOffsetLimit = with(density) { -80.dp.toPx() }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        val offset = scrollBehavior.state.heightOffset
        Box(
            modifier = Modifier
                .graphicsLayer { translationY = offset }
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(56.dp)
                .frostedGlass(cornerRadius = 32.dp, surfaceAlpha = 0.4f, surfaceColor = Color.Black)
                .clip(RoundedCornerShape(32.dp))
        ) {
            TopAppBar(
                modifier = Modifier.fillMaxSize(),
                windowInsets = WindowInsets(0, 0, 0, 0),
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
                        tint = Color.White
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
                        // Home tab: animated logo only (badge removed)
                        tab == 0 -> {
                            AppLogoAnimated()
                        }
                        // Folder detail: folder name
                        tab == 1 && folder != null -> {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = ToolIconPalette.Folders,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = folder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
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
                                tint = ToolIconPalette.Queue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = pTitle ?: folder,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee(Int.MAX_VALUE)
                            )
                        }
                        // Playlist root
                        tab == 2 -> {
                            TabTitleText("Playlists")
                        }
                        // Transfers tab
                        tab == 3 -> {
                            TabTitleText("Transfers")
                        }
                        // Watch Party tab
                        tab == 4 -> {
                            TabTitleText("Watch Party")
                        }
                        // Explorer tab
                        tab == 5 -> {
                            TabTitleText("Explorer")
                        }
                        else -> {
                            TabTitleText("HOA Player")
                        }
                    }
                }
            }
        },

        // ── Actions (right side) ──────────────────────────────────────────────
        actions = {
            val isExplorer = selectedTab == 5

            DynamicActionIcon(visible = isExplorer) {
                IconButton(onClick = onBookmarksClick) {
                    Icon(
                        Icons.Default.Bookmarks,
                        contentDescription = "Bookmarks",
                        tint = ToolIconPalette.Bookmarks
                    )
                }
            }
            DynamicActionIcon(visible = isExplorer) {
                IconButton(onClick = onTrashClick) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "Trash Bin",
                        tint = ToolIconPalette.Trash
                    )
                }
            }
            DynamicActionIcon(visible = isExplorer) {
                IconButton(onClick = onCreateNewClick) {
                    Icon(
                        Icons.Default.CreateNewFolder,
                        contentDescription = "Create folder",
                        tint = ToolIconPalette.Folders
                    )
                }
            }

            // Sort & Filter — Home and Folder root
            DynamicActionIcon(visible = isHome || isFolderRoot) {
                IconButton(onClick = onSortFilterClick) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Sort & Filter",
                        tint = ToolIconPalette.More
                    )
                }
            }

            // Watching Habits — Home and Playlists root
            DynamicActionIcon(visible = isHome || isPlaylistRoot) {
                IconButton(onClick = onHabitsClick) {
                    Icon(
                        Icons.Default.Insights,
                        contentDescription = "Watching Habits",
                        tint = ToolIconPalette.Insights
                    )
                }
            }

            // Search — only visible in media tabs (Home, Folders, Playlists)
            DynamicActionIcon(visible = isHome || isFolderRoot || isFolderDetail || isPlaylistRoot || isPlaylistDetail) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ToolIconPalette.Search
                    )
                }
            }

            // Refresh — media scan icon
            DynamicActionIcon(visible = isHome || isFolderRoot) {
                IconButton(
                    onClick = { if (!isRefreshing) onRefreshClick() },
                    enabled = !isRefreshing
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRefreshing) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRefreshing) {
                            // Animated spinning refresh icon while scanning
                            val infiniteTransition = rememberInfiniteTransition(label = "RefreshSpin")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, delayMillis = 0, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "RefreshRotation"
                            )
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Scanning Media...",
                                tint = ToolIconPalette.Refresh,
                                modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rotation }
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Scan Media",
                                tint = ToolIconPalette.Refresh,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                            tint = ToolIconPalette.Settings,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor         = Color.Transparent, // Fully transparent
            scrolledContainerColor = Color.Transparent, // Let frostedGlass handle the background
            navigationIconContentColor = Color.White,
            titleContentColor      = Color.White,
            actionIconContentColor = Color.White
        )
            )
        }
    }
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
    Box(modifier = Modifier
        .height(36.dp)
        .width(80.dp),
        contentAlignment = Alignment.Center
    ) {
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
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
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
            color      = Color.White
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