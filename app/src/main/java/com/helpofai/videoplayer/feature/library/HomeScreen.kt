/*
|--------------------------------------------------------------------------
| HelpOfAi (HOA) Professional Software
|--------------------------------------------------------------------------
| Copyright (c) 2026 Rajib Adhikary. All Rights Reserved.
*/
package com.helpofai.videoplayer.feature.library

import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.core.theme.frostedGlass
import com.helpofai.videoplayer.feature.library.components.DynamicTopBar
import com.helpofai.videoplayer.feature.library.components.LibrarySkeletonLoader
import com.helpofai.videoplayer.feature.library.components.LibraryStorageDashboard
import com.helpofai.videoplayer.feature.watch_party.ui.WatchPartyMainTab
import com.helpofai.videoplayer.tools.vault.VaultViewModel

// Safely maps integers required by DynamicTopBar to a type-safe structure
data class TabItem(val index: Int, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
val APP_TABS = listOf(
    TabItem(0, "Home", Icons.Default.Home),
    TabItem(1, "Folders", Icons.Default.Folder),
    TabItem(5, "Files", Icons.Default.Description),
    TabItem(4, "Watch Party", Icons.Default.Group),
    TabItem(6, "Tools", Icons.Default.Build)
)

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun HomeScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    vaultViewModel: VaultViewModel = hiltViewModel(),
    onVideoClick: (Video) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onVaultClick: () -> Unit = {},
    onEditorClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Original is permanently deleted
        }
    }
    
    val onFavoriteClick: (Video) -> Unit = { video -> viewModel.toggleFavorite(video) }
    val onShareClick: (Video) -> Unit = { video ->
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(android.content.Intent.EXTRA_STREAM, video.uri)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share Video"))
    }
    
    // UI State - Dialogs (Video objects can't be reliably Parcelized by rememberSaveable)
    var videoToRename by remember { mutableStateOf<Video?>(null) }
    var videoToDelete by remember { mutableStateOf<Video?>(null) }
    var videoToMerge by remember { mutableStateOf<Video?>(null) }
    
    // Robust State Survival for primitive UI flags
    var newVideoName by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var selectedFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var showHabitReport by rememberSaveable { mutableStateOf(false) }
    var showSortFilter by rememberSaveable { mutableStateOf(false) }
    var showExitPopup by rememberSaveable { mutableStateOf(false) }
    var showBookmarksDialog by rememberSaveable { mutableStateOf(false) }
    var showTrashDialog by rememberSaveable { mutableStateOf(false) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    
    val activity = context as? android.app.Activity ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
    val isMiniPlayerActive by com.helpofai.videoplayer.core.playback.GlobalMiniPlayerManager.getInstance().isMiniPlayerActive.collectAsState()

    androidx.activity.compose.BackHandler(enabled = true) {
        if (selectedFolder != null) {
            selectedFolder = null
        } else if (selectedTab != 0) {
            selectedTab = 0
        } else {
            showExitPopup = true
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.onPermissionResult(com.helpofai.videoplayer.feature.permissions.hasRequiredPermissions(context))
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            DynamicTopBar(
                selectedTab       = selectedTab,
                selectedFolder    = selectedFolder,
                playlistTitle     = null,
                videoCount        = state.videos.size,
                scrollBehavior    = scrollBehavior,
                onBackClick       = { selectedFolder = null },
                onHabitsClick     = { showHabitReport = true },
                onSortFilterClick = { showSortFilter = true },
                onSearchClick     = { /* TODO: open search */ },
                onSettingsClick   = onSettingsClick,
                onBookmarksClick  = { showBookmarksDialog = true },
                onTrashClick      = { showTrashDialog = true },
                onCreateNewClick  = { showCreateDialog = true },
                onRefreshClick    = { viewModel.refreshVideos() }
            )
        },
        bottomBar = {
            val collapsedFraction = scrollBehavior.state.collapsedFraction
            val bottomNavOffset = 130.dp * collapsedFraction
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = bottomNavOffset)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 6.dp)
                    .frostedGlass(cornerRadius = 32.dp, surfaceAlpha = 0.4f, surfaceColor = Color.Black)
            ) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    tonalElevation = 0.dp,
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                ) {
                    APP_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab.index,
                            onClick = { 
                                if (selectedTab == tab.index && tab.index == 1) selectedFolder = null
                                selectedTab = tab.index 
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(24.dp)) },
                            alwaysShowLabel = false,
                            label = { 
                                Text(
                                    text = tab.title, 
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                ) 
                            },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF00CEC9),
                                selectedTextColor = Color(0xFF00CEC9),
                                indicatorColor = Color.Transparent, // Transparent indicator so glass shows
                                unselectedIconColor = Color.White,
                                unselectedTextColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refreshVideos() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (state.isLoading && state.videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    LibrarySkeletonLoader()
                }
            } else if (state.videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No videos found on this device.",
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        } else {
            val isScrollableTab = selectedTab == 0 || selectedTab == 1
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isScrollableTab) Modifier.verticalScroll(rememberScrollState())
                        else Modifier
                    )
            ) {
                if (isScrollableTab) {
                    // This Spacer must be INSIDE the scrollable Column so content starts below the app bar
                    // but can scroll up behind it seamlessly.
                    Spacer(Modifier.height(paddingValues.calculateTopPadding()))
                }
                
                val onVaultMoveClick: (Video) -> Unit = { video ->
                    vaultViewModel.encryptFileToVault(video.uri, deleteOriginal = false)
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        try {
                            val intentSender = MediaStore.createDeleteRequest(
                                context.contentResolver, 
                                listOf(video.uri)
                            ).intentSender
                            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        val originalFile = java.io.File(video.path)
                        if (originalFile.exists()) {
                            originalFile.delete()
                        }
                    }
                }

                when (selectedTab) {
                    0 -> com.helpofai.videoplayer.feature.library.components.LibraryHomeTab(
                        state = state,
                        isTablet = isTablet,
                        onVideoClick = onVideoClick,
                        onFavoriteClick = onFavoriteClick,
                        onRenameClick = { videoToRename = it },
                        onDeleteClick = { videoToDelete = it },
                        onShareClick = onShareClick,
                        onVaultClick = onVaultMoveClick
                    )
                    1 -> com.helpofai.videoplayer.feature.library.components.LibraryFoldersTab(
                        state = state,
                        selectedFolder = selectedFolder,
                        isTablet = isTablet,
                        onFolderClick = { selectedFolder = it },
                        onViewModeChange = { viewModel.updateFolderViewMode(it) },
                        onVideoClick = onVideoClick,
                        onFavoriteClick = onFavoriteClick,
                        onRenameClick = { videoToRename = it },
                        onDeleteClick = { videoToDelete = it },
                        onShareClick = onShareClick,
                        onVaultClick = onVaultMoveClick
                    )
                    4 -> WatchPartyMainTab(
                        videos = state.videos,
                        paddingValues = paddingValues,
                        onVideoClick = onVideoClick
                    )
                    5 -> com.helpofai.videoplayer.feature.filemanager.FileManagerScreen(
                        paddingValues = paddingValues,
                        onVideoClick = onVideoClick,
                        onNavigateToTab = { tabIndex -> selectedTab = tabIndex },
                        showBookmarksDialog = showBookmarksDialog,
                        showTrashDialog = showTrashDialog,
                        showCreateDialog = showCreateDialog,
                        onDismissBookmarks = { showBookmarksDialog = false },
                        onDismissTrash = { showTrashDialog = false },
                        onDismissCreate = { showCreateDialog = false }
                    )
                    6 -> com.helpofai.videoplayer.tools.ToolsScreen(
                        paddingValues = paddingValues,
                        onToolClick = { tool ->
                            when (tool.title) {
                                "Private Vault" -> onVaultClick()
                                "Video to MP3" -> onEditorClick("Video to MP3")
                                "Video Trimmer" -> onEditorClick("Video Trimmer")
                                else -> {
                                    if (tool.title == "Change Resolution" || tool.title == "Make GIF") {
                                        onEditorClick(tool.title)
                                    }
                                }
                            }
                        }
                    )
                }
                
                if (isScrollableTab) {
                    // Spacer at the bottom so the last item can scroll fully into view above the floating bottom nav
                    Spacer(Modifier.height(paddingValues.calculateBottomPadding() + 80.dp))
                }
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
            onDismiss = { 
                showExitPopup = false
                if (isMiniPlayerActive) {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    activity?.enterPictureInPictureMode(params)
                } else {
                    activity?.moveTaskToBack(true)
                }
            },
            onNoClick = {
                showExitPopup = false
            },
            onBackground = { 
                showExitPopup = false
                if (isMiniPlayerActive) {
                    val params = android.app.PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build()
                    activity?.enterPictureInPictureMode(params)
                } else {
                    activity?.moveTaskToBack(true)
                }
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
                viewModel.analyzeStorage()
            }
        )
    }
}