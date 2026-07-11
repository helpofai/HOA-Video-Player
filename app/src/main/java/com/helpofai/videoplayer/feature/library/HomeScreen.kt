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
package com.helpofai.videoplayer.feature.library

import com.helpofai.videoplayer.feature.watch_party.ui.WatchPartyMainTab

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.library.components.DynamicTopBar
import com.helpofai.videoplayer.feature.library.components.LibrarySkeletonLoader
import com.helpofai.videoplayer.feature.library.components.LibraryStorageDashboard
import com.helpofai.videoplayer.feature.permissions.hasRequiredPermissions
import com.helpofai.videoplayer.feature.playlist.SmartPlaylistEngine

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun HomeScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onVideoClick: (Video) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val onFavoriteClick: (Video) -> Unit = { video -> viewModel.toggleFavorite(video) }
    
    val onShareClick: (Video) -> Unit = { video ->
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(android.content.Intent.EXTRA_STREAM, video.uri)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Video"))
    }
    
    var videoToRename by remember { mutableStateOf<Video?>(null) }
    var newVideoName by remember { mutableStateOf("") }
    var videoToDelete by remember { mutableStateOf<Video?>(null) }
    var videoToMerge by remember { mutableStateOf<Video?>(null) }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var showHabitReport by remember { mutableStateOf(false) }
    var showSortFilter by remember { mutableStateOf(false) }
    var showExitPopup by remember { mutableStateOf(false) }
    val activity = context as? android.app.Activity ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity

    androidx.activity.compose.BackHandler(enabled = selectedFolder != null || selectedTab != 0) {
        if (selectedFolder != null) {
            selectedFolder = null
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            showExitPopup = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            viewModel.onPermissionResult(granted)
        }
    )

    LaunchedEffect(Unit) {
        val hasPermissions = hasRequiredPermissions(context)
        viewModel.onPermissionResult(hasPermissions)
        if (!hasPermissions) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_VIDEO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            permissionLauncher.launch(permission)
        }
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DynamicTopBar(
                selectedTab       = selectedTab,
                selectedFolder    = selectedFolder,
                playlistTitle     = if (selectedTab == 2 && selectedFolder != null)
                    SmartPlaylistEngine.generatePlaylists(state.videos).find { it.id == selectedFolder }?.title
                    else null,
                videoCount        = state.videos.size,
                scrollBehavior    = scrollBehavior,
                onBackClick       = { selectedFolder = null },
                onHabitsClick     = { showHabitReport = true },
                onSortFilterClick = { showSortFilter = true },
                onSearchClick     = { /* TODO: open search */ },
                onSettingsClick   = onSettingsClick
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
                    label = { Text("Home", style = MaterialTheme.typography.labelMedium) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        if (selectedTab == 1) selectedFolder = null
                        selectedTab = 1 
                    },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Folders", modifier = Modifier.size(24.dp)) },
                    label = { Text("Folders", style = MaterialTheme.typography.labelMedium) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        if (selectedTab == 2) selectedFolder = null
                        selectedTab = 2 
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "Playlists", modifier = Modifier.size(24.dp)) },
                    label = { Text("Playlists", style = MaterialTheme.typography.labelMedium) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Transfers", modifier = Modifier.size(24.dp)) },
                    label = { Text("Transfers", style = MaterialTheme.typography.labelMedium) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Watch Party", modifier = Modifier.size(24.dp)) },
                    label = { Text("Watch Party", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                LibrarySkeletonLoader()
            }
        } else if (!state.permissionGranted) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Storage permission is required to find videos.")
            }
        } else if (state.videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No videos found on this device.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .then(
                        if (selectedTab != 3 && selectedTab != 4) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    )
            ) {
                if (selectedTab == 0) {
                    com.helpofai.videoplayer.feature.library.components.LibraryHomeTab(
                        state = state,
                        isTablet = isTablet,
                        onVideoClick = onVideoClick,
                        onFavoriteClick = onFavoriteClick,
                        onRenameClick = { videoToRename = it },
                        onDeleteClick = { videoToDelete = it },
                        onShareClick = onShareClick,
                        onNavigateToPlaylists = {
                            selectedTab = 2
                            selectedFolder = it
                        }
                    )
                } else if (selectedTab == 1) {
                    com.helpofai.videoplayer.feature.library.components.LibraryFoldersTab(
                        state = state,
                        selectedFolder = selectedFolder,
                        isTablet = isTablet,
                        onFolderClick = { selectedFolder = it },
                        onViewModeChange = { viewModel.updateFolderViewMode(it) },
                        onVideoClick = onVideoClick,
                        onFavoriteClick = onFavoriteClick,
                        onRenameClick = { videoToRename = it },
                        onDeleteClick = { videoToDelete = it },
                        onShareClick = onShareClick
                    )
                } else if (selectedTab == 2) {
                    com.helpofai.videoplayer.feature.library.components.LibraryPlaylistsTab(
                        state = state,
                        selectedFolder = selectedFolder,
                        isTablet = isTablet,
                        onPlaylistClick = { selectedFolder = it },
                        onVideoClick = onVideoClick,
                        onFavoriteClick = onFavoriteClick,
                        onRenameClick = { videoToRename = it },
                        onDeleteClick = { videoToDelete = it },
                        onShareClick = onShareClick
                    )
                } else if (selectedTab == 3) {
                    com.helpofai.videoplayer.feature.workspace.transfers.TransfersTab(
                        isTablet = isTablet,
                        videos = state.videos
                    )
                } else if (selectedTab == 4) {
                    WatchPartyMainTab(
                        videos = state.videos,
                        onVideoClick = onVideoClick
                    )
                }
            }
        }
    }

    // Rename Dialog
    videoToRename?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToRename = null },
            title = { Text("Rename Video") },
            text = {
                OutlinedTextField(
                    value = newVideoName,
                    onValueChange = { newVideoName = it },
                    label = { Text("New name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newVideoName.isNotBlank()) {
                        viewModel.renameVideo(video, newVideoName)
                    }
                    videoToRename = null
                }) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { videoToRename = null }) { Text("Cancel") }
            }
        )
    }

    if (showHabitReport) {
        val report = com.helpofai.videoplayer.feature.analysis.HabitAnalyzer.analyze(state.allVideos)
        com.helpofai.videoplayer.feature.analysis.components.HabitReportSheet(
            report = report,
            onDismissRequest = { showHabitReport = false }
        )
    }

    if (showSortFilter) {
        com.helpofai.videoplayer.feature.library.components.SortFilterSheet(
            currentSort = state.sortOption,
            currentFilter = state.filterOption,
            onSortSelected = { viewModel.updateSortOption(it) },
            onFilterSelected = { viewModel.updateFilterOption(it) },
            onDismissRequest = { showSortFilter = false }
        )
    }

    if (showExitPopup) {
        com.helpofai.videoplayer.feature.library.components.ExitPopup(
            onDismiss = { showExitPopup = false },
            onBackground = { 
                showExitPopup = false
                activity?.moveTaskToBack(true)
            },
            onExit = { activity?.finish() }
        )
    }

    // Delete Dialog
    videoToDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text("Delete Video") },
            text = { Text("Are you sure you want to delete '${video.title}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVideo(video)
                        videoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Merge Dialog
    videoToMerge?.let { video1 ->
        var selectedVideoToMerge by remember { mutableStateOf<Video?>(null) }
        val otherVideos = state.videos.filter { it.id != video1.id }

        AlertDialog(
            onDismissRequest = { videoToMerge = null },
            title = { Text("Merge Videos") },
            text = {
                Column {
                    Text("Select a video to merge with '${video1.title}':")
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(otherVideos) { v2 ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedVideoToMerge = v2 }
                                    .background(if (selectedVideoToMerge == v2) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .padding(8.dp)
                            ) {
                                Text(v2.title, maxLines = 1, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedVideoToMerge?.let { v2 ->
                            viewModel.mergeVideos(context, video1, v2)
                        }
                        videoToMerge = null
                    },
                    enabled = selectedVideoToMerge != null
                ) { Text("Merge") }
            },
            dismissButton = {
                TextButton(onClick = { videoToMerge = null }) { Text("Cancel") }
            }
        )
    }

    // Storage Dashboard Sheet
    val storageReport by viewModel.storageReport.collectAsState()
    storageReport?.let { report ->
        LibraryStorageDashboard(
            report = report,
            onDismissRequest = { viewModel.clearStorageReport() },
            onDeleteClick = { video ->
                viewModel.deleteVideo(video)
                viewModel.analyzeStorage() // refresh
            }
        )
    }
}
// â”€â”€ Inline composables extracted to separate files â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// SectionTitle           â†’ components/LibraryVideoCards.kt  (LibrarySectionTitle)
// HeroVideoCard          â†’ components/LibraryHeroCard.kt    (LibraryHeroCard)
// VideoInfoCard          â†’ components/LibraryVideoCards.kt  (LibraryVideoInfoCard)
// FavoriteVideoCard      â†’ components/LibraryVideoCards.kt  (LibraryFavoriteVideoCard)
// CompactVideoListItem   â†’ components/LibraryVideoCards.kt  (LibraryCompactVideoListItem)
// CollectionChip         â†’ components/LibraryVideoCards.kt  (LibraryCollectionChip)
// FolderItemCard         â†’ components/LibraryFolderCard.kt  (LibraryFolderCard)
// FolderThumbnail        â†’ components/LibraryFolderCard.kt  (LibraryFolderThumbnail)
// StorageDashboardDialog â†’ components/LibraryStorageDashboard.kt (LibraryStorageDashboard)