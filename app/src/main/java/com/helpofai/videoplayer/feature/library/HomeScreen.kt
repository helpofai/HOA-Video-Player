package com.helpofai.videoplayer.feature.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import coil.request.videoFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.material.icons.filled.Insights
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.blur
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.library.components.AdvancedVideoTags
import com.helpofai.videoplayer.feature.library.components.BadgeTag
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.scale
import com.helpofai.videoplayer.feature.library.components.LibrarySkeletonLoader
import com.helpofai.videoplayer.feature.library.components.VideoThumbnailCard

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun HomeScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onVideoClick: (Video) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val privacyRepository = remember { com.helpofai.videoplayer.core.data.PrivacyRepository(context) }
    var isLocked by remember { mutableStateOf(privacyRepository.isLockEnabled()) }
    
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

    androidx.activity.compose.BackHandler(enabled = true) {
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
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { 
                    val activePlaylist = if (selectedTab == 2 && selectedFolder != null) com.helpofai.videoplayer.feature.playlist.SmartPlaylistEngine.generatePlaylists(state.videos).find { it.id == selectedFolder } else null
                    val titleText = if (selectedTab == 0) "" else if (selectedTab == 1) (selectedFolder ?: "Folders") else (activePlaylist?.title ?: "Playlists")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedTab == 0) {
                            Box(modifier = Modifier.size(48.dp)) {
                                @OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
                                val image = androidx.compose.animation.graphics.vector.AnimatedImageVector.animatedVectorResource(id = com.helpofai.videoplayer.R.drawable.ic_logo_animated)
                                var atEnd by remember { mutableStateOf(false) }
                                
                                LaunchedEffect(Unit) {
                                    atEnd = true
                                }
                                
                                @OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter(image, atEnd = atEnd),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (titleText.isNotEmpty()) Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (titleText.isNotEmpty()) {
                            Text(titleText, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)) 
                        }
                    }
                },
                navigationIcon = {
                    if ((selectedTab == 1 || selectedTab == 2) && selectedFolder != null) {
                        IconButton(onClick = { selectedFolder = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isLocked) {
                                privacyRepository.removePin()
                                isLocked = false
                            } else {
                                privacyRepository.setPin("1234")
                                isLocked = true
                            }
                        }
                    ) {
                        val icon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen
                        Icon(icon, contentDescription = "Privacy Lock")
                    }
                    IconButton(onClick = { viewModel.analyzeStorage() }) {
                        Icon(Icons.Default.Storage, contentDescription = "Storage Dashboard")
                    }
                    IconButton(onClick = { showSortFilter = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Sort and Filter")
                    }
                    IconButton(onClick = { showHabitReport = true }) {
                        Icon(Icons.Default.Insights, contentDescription = "Watching Habits")
                    }
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Column {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface
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
                }
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
                    .verticalScroll(rememberScrollState())
            ) {
                if (selectedTab == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 1. Hero Section (Last Played / Featured)
                    state.videos.firstOrNull()?.let { firstVideo ->
                        HeroVideoCard(
                            video = firstVideo, 
                            onClick = { onVideoClick(firstVideo) },
                            onFavoriteClick = { onFavoriteClick(firstVideo) }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 1.5 Continue Watching (Smart Resume)
                    val continueWatching = state.videos.filter { it.lastPlayedPosition > 0 && it.duration > 0 && it.lastPlayedPosition < it.duration - 5000 }.sortedByDescending { it.lastPlayedTimestamp }.take(5)
                    if (continueWatching.isNotEmpty()) {
                        SectionTitle("Continue Watching")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(continueWatching) { video ->
                                com.helpofai.videoplayer.feature.library.components.ContinueWatchingCard(
                                    video = video,
                                    onClick = { onVideoClick(video) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 1.8 Recommended For You
                    val recommendations = com.helpofai.videoplayer.feature.recommendation.RecommendationEngine.getRecommendations(state.videos)
                    if (recommendations.isNotEmpty()) {
                        SectionTitle("Recommended For You")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(recommendations) { video ->
                                VideoInfoCard(
                                    video = video, 
                                    onClick = { onVideoClick(video) },
                                    onFavoriteClick = { onFavoriteClick(video) },
                                    onRenameClick = { videoToRename = video },
                                    onDeleteClick = { videoToDelete = video },
                                    onShareClick = { onShareClick(video) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        com.helpofai.videoplayer.core.ads.NativeAdCard(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth())
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 2. All Videos / Recently Added
                    val recentlyAdded = state.videos.sortedByDescending { it.dateAdded }.take(10)
                    if (recentlyAdded.isNotEmpty()) {
                        SectionTitle("Recently Added")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(recentlyAdded) { video ->
                                VideoInfoCard(
                                    video = video, 
                                    onClick = { onVideoClick(video) },
                                    onFavoriteClick = { onFavoriteClick(video) },
                                    onRenameClick = { videoToRename = video },
                                    onDeleteClick = { videoToDelete = video },
                                    onShareClick = { onShareClick(video) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        com.helpofai.videoplayer.core.ads.AdBanner(modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth())
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 3. Favorites (Squared Cards)
                    val favorites = state.videos.filter { it.isFavorite }
                    if (favorites.isNotEmpty()) {
                        SectionTitle("Favorites")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(favorites) { video ->
                                FavoriteVideoCard(
                                    video = video, 
                                    onClick = { onVideoClick(video) },
                                    onFavoriteClick = { onFavoriteClick(video) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 4. Most Watched & Legacy Continue Watching
                    val legacyContinueWatching = state.videos.filter { it.lastPlayedPosition > 0 }.sortedByDescending { it.lastPlayedPosition }
                    if (legacyContinueWatching.isNotEmpty()) {
                        SectionTitle("Resume Playback")
                        val listCols = if (isTablet) 2 else 1
                        val chunkedContinue = legacyContinueWatching.take(if (isTablet) 4 else 3).chunked(listCols)
                        
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunkedContinue.forEach { rowVideos ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowVideos.forEach { video ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CompactVideoListItem(
                                                video = video, 
                                                onClick = { onVideoClick(video) },
                                                onFavoriteClick = { onFavoriteClick(video) },
                                                onRenameClick = { videoToRename = video },
                                                onDeleteClick = { videoToDelete = video },
                                                onShareClick = { onShareClick(video) }
                                            )
                                        }
                                    }
                                    val emptySlots = listCols - rowVideos.size
                                    for (i in 0 until emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 5. Large Files (Movies)
                    val largeFiles = state.videos.sortedByDescending { it.size }.take(10)
                    if (largeFiles.isNotEmpty()) {
                        SectionTitle("Large Files")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(largeFiles) { video ->
                                VideoInfoCard(
                                    video = video, 
                                    onClick = { onVideoClick(video) },
                                    onFavoriteClick = { onFavoriteClick(video) },
                                    onRenameClick = { videoToRename = video },
                                    onDeleteClick = { videoToDelete = video },
                                    onShareClick = { onShareClick(video) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 6. Short Clips
                    val shortClips = state.videos.filter { it.duration in 1..60000 }.take(15)
                    if (shortClips.isNotEmpty()) {
                        SectionTitle("Short Clips")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(shortClips) { video ->
                                FavoriteVideoCard(
                                    video = video, 
                                    onClick = { onVideoClick(video) },
                                    onFavoriteClick = { onFavoriteClick(video) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // 7. Smart Playlists (Chips)
                    SectionTitle("Smart Playlists")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            CollectionChip("All Playlists", "Browse all", modifier = Modifier.width(140.dp), onClick = {
                                selectedTab = 2
                                selectedFolder = null
                            })
                        }
                        val homePlaylists = com.helpofai.videoplayer.feature.playlist.SmartPlaylistEngine.generatePlaylists(state.videos)
                        homePlaylists.forEach { playlist ->
                            item {
                                CollectionChip(playlist.title, "${playlist.videos.size} items", modifier = Modifier.width(160.dp), onClick = {
                                    selectedTab = 2
                                    selectedFolder = playlist.id
                                })
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                } else if (selectedTab == 1) {
                    // Folders View
                    val folders = state.videos.groupBy { java.io.File(it.path).parentFile?.name ?: "Internal Storage" }
                    
                    if (selectedFolder == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val folderList = folders.toList()
                        val folderCols = if (isTablet) 4 else 2
                        val chunkedFolders = folderList.chunked(folderCols)
                        
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            chunkedFolders.forEachIndexed { index, rowFolders ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowFolders.forEach { (folderName, videosInFolder) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            FolderItemCard(
                                                folderName = folderName,
                                                videos = videosInFolder,
                                                onClick = { selectedFolder = folderName }
                                            )
                                        }
                                    }
                                    val emptySlots = folderCols - rowFolders.size
                                    for (i in 0 until emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                if ((index + 1) % 4 == 0) {
                                    if (index % 8 == 3) {
                                        com.helpofai.videoplayer.core.ads.NativeAdCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                                    } else {
                                        com.helpofai.videoplayer.core.ads.AdBanner(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                        val videosInFolder = folders[selectedFolder] ?: emptyList()
                        Spacer(modifier = Modifier.height(16.dp))
                        val listCols = if (isTablet) 2 else 1
                        val chunkedVideos = videosInFolder.chunked(listCols)
                        
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunkedVideos.forEachIndexed { index, rowVideos ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowVideos.forEach { video ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CompactVideoListItem(
                                                video = video, 
                                                onClick = { onVideoClick(video) },
                                                onFavoriteClick = { onFavoriteClick(video) },
                                                onRenameClick = { videoToRename = video },
                                                onDeleteClick = { videoToDelete = video },
                                                onShareClick = { onShareClick(video) }
                                            )
                                        }
                                    }
                                    val emptySlots = listCols - rowVideos.size
                                    for (i in 0 until emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                if ((index + 1) % 6 == 0) {
                                    if (index % 12 == 5) {
                                        com.helpofai.videoplayer.core.ads.NativeAdCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                                    } else {
                                        com.helpofai.videoplayer.core.ads.AdBanner(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                } else if (selectedTab == 2) {
                    // Playlists View - Professional Redesign
                    val playlists = com.helpofai.videoplayer.feature.playlist.SmartPlaylistEngine.generatePlaylists(state.videos)
                    
                    if (selectedFolder == null) {
                        // === PLAYLIST GRID VIEW ===
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Header Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                        ) {
                            Text(
                                text = "Smart Collections",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Auto-organized by your viewing habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Stats Row
                        val totalPlaylistVideos = playlists.sumOf { it.videos.size }
                        val totalDurationMs = playlists.flatMap { it.videos }.distinctBy { it.path }.sumOf { it.duration }
                        val totalHours = totalDurationMs / 3600000
                        val totalMins = (totalDurationMs % 3600000) / 60000
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Stat Chip - Collections
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = Color(0xFF6C63FF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "${playlists.size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Collections",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                            
                            // Stat Chip - Videos
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color(0xFFFF6B35),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "$totalPlaylistVideos",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Videos",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                            
                            // Stat Chip - Duration
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            if (totalHours > 0) "${totalHours}h" else "${totalMins}m",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "Total",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Featured Playlist (first one gets a large card)
                        if (playlists.isNotEmpty()) {
                            val featured = playlists.first()
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Featured",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(featured.accentColor)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                com.helpofai.videoplayer.feature.playlist.components.SmartPlaylistCard(
                                    playlist = featured,
                                    onClick = { selectedFolder = featured.id }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        // Remaining playlists in a grid
                        val remainingPlaylists = if (playlists.size > 1) playlists.subList(1, playlists.size) else emptyList()
                        if (remainingPlaylists.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "All Collections",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        "${remainingPlaylists.size} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val listCols = if (isTablet) 3 else 2
                                val chunkedPlaylists = remainingPlaylists.chunked(listCols)
                                
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    chunkedPlaylists.forEachIndexed { index, rowPlaylists ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowPlaylists.forEach { playlist ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    com.helpofai.videoplayer.feature.playlist.components.SmartPlaylistCard(
                                                        playlist = playlist,
                                                        onClick = { selectedFolder = playlist.id }
                                                    )
                                                }
                                            }
                                            val emptySlots = listCols - rowPlaylists.size
                                            for (i in 0 until emptySlots) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                        if ((index + 1) % 3 == 0) {
                                            com.helpofai.videoplayer.core.ads.NativeAdCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Empty state
                        if (playlists.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No Collections Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Add videos to see smart playlists appear",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                        // === PLAYLIST DETAIL VIEW (videos inside a playlist) ===
                        val activePlaylist = playlists.find { it.id == selectedFolder }
                        val videosInCat = activePlaylist?.videos ?: emptyList()
                        
                        // Playlist detail header
                        if (activePlaylist != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = activePlaylist.accentColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(activePlaylist.accentColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = activePlaylist.icon,
                                            contentDescription = null,
                                            tint = activePlaylist.accentColor,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activePlaylist.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${videosInCat.size} videos • ${activePlaylist.description}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    // Play all button
                                    if (videosInCat.isNotEmpty()) {
                                        FilledTonalButton(
                                            onClick = { onVideoClick(videosInCat.first()) },
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = activePlaylist.accentColor.copy(alpha = 0.15f)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Play All",
                                                tint = activePlaylist.accentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Play",
                                                color = activePlaylist.accentColor,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val listCols = if (isTablet) 2 else 1
                        val chunkedVideos = videosInCat.chunked(listCols)
                        
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunkedVideos.forEachIndexed { index, rowVideos ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowVideos.forEach { video ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            CompactVideoListItem(
                                                video = video, 
                                                onClick = { onVideoClick(video) },
                                                onFavoriteClick = { onFavoriteClick(video) },
                                                onRenameClick = { videoToRename = video },
                                                onDeleteClick = { videoToDelete = video },
                                                onShareClick = { onShareClick(video) }
                                            )
                                        }
                                    }
                                    val emptySlots = listCols - rowVideos.size
                                    for (i in 0 until emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                if ((index + 1) % 6 == 0) {
                                    if (index % 12 == 5) {
                                        com.helpofai.videoplayer.core.ads.NativeAdCard(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                                    } else {
                                        com.helpofai.videoplayer.core.ads.AdBanner(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
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
        StorageDashboardDialog(
            report = report,
            onDismissRequest = { viewModel.clearStorageReport() },
            onDeleteClick = { video -> 
                viewModel.deleteVideo(video)
                viewModel.analyzeStorage() // refresh
            }
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun HeroVideoCard(video: Video, onClick: () -> Unit = {}, onFavoriteClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 12.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(video.uri)
                    .crossfade(true)
                    .size(512) // Memory optimization
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Video Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            // Dark Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF001F3F).copy(alpha = 0.5f))
            )
            
            // Advanced Tags
            AdvancedVideoTags(
                video = video,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )

            // View Memory Progress Bar
            if (video.lastPlayedPosition > 0 && video.duration > 0) {
                val progress = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }
            
            // Play Button Floating
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Info Overlay
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color(0xFF001F3F).copy(alpha = 0.8f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${video.formattedDuration} • ${video.formattedSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                IconButton(onClick = onFavoriteClick) {
                    val icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                    Icon(icon, contentDescription = "Favorite", tint = if (video.isFavorite) Color.Red else Color.White)
                }
            }
        }
    }
}

@Composable
fun VideoInfoCard(
    video: Video, 
    onClick: () -> Unit = {}, 
    onFavoriteClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMergeClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        VideoThumbnailCard(video = video)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(180.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                Text(
                    text = video.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onFavoriteClick) {
                val icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                Icon(icon, contentDescription = "Favorite", tint = if (video.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = showMenu, 
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xCC1E293B)) // 80% transparent
                ) {
                    DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; onShareClick() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRenameClick() })
                    DropdownMenuItem(text = { Text("Merge with...") }, onClick = { showMenu = false; onMergeClick() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDeleteClick() })
                }
            }
        }
    }
}

@Composable
fun FavoriteVideoCard(video: Video, onClick: () -> Unit = {}, onFavoriteClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier.size(90.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(video.uri)
                        .crossfade(true)
                        .size(512)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )

                AdvancedVideoTags(
                    video = video,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .scale(0.8f) // Scale down for compact view
                )

                if (video.lastPlayedPosition > 0 && video.duration > 0) {
                    val progress = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF001F3F).copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.width(90.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = onFavoriteClick, modifier = Modifier.size(24.dp)) {
                val icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                Icon(icon, contentDescription = "Favorite", tint = if (video.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CompactVideoListItem(
    video: Video, 
    onClick: () -> Unit = {}, 
    onFavoriteClick: () -> Unit = {},
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onMergeClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(video.uri)
                        .crossfade(true)
                        .size(256)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                AdvancedVideoTags(
                    video = video,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp)
                        .scale(0.8f) // Scale down for compact view
                )

                if (video.lastPlayedPosition > 0 && video.duration > 0) {
                    val progress = (video.lastPlayedPosition.toFloat() / video.duration.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${video.formattedDuration} • ${video.formattedSize}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        
        IconButton(onClick = onFavoriteClick) {
            val icon = if (video.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder
            Icon(icon, contentDescription = "Favorite", tint = if (video.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface)
        }
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = showMenu, 
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xCC1E293B)) // 80% transparent
            ) {
                DropdownMenuItem(text = { Text("Share") }, onClick = { showMenu = false; onShareClick() })
                DropdownMenuItem(text = { Text("Rename") }, onClick = { showMenu = false; onRenameClick() })
                DropdownMenuItem(text = { Text("Merge with...") }, onClick = { showMenu = false; onMergeClick() })
                DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDeleteClick() })
            }
        }
    }
}

@Composable
fun CollectionChip(title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        modifier = modifier.height(64.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun FolderItemCard(
    folderName: String,
    videos: List<Video>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 2x2 Grid of thumbnails representing the folder contents
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.weight(1f)) {
                    val v1 = videos.getOrNull(0)
                    val v2 = videos.getOrNull(1)
                    if (v1 != null) FolderThumbnail(v1, Modifier.weight(1f).fillMaxHeight())
                    if (v2 != null) FolderThumbnail(v2, Modifier.weight(1f).fillMaxHeight())
                }
                Row(modifier = Modifier.weight(1f)) {
                    val v3 = videos.getOrNull(2)
                    val v4 = videos.getOrNull(3)
                    if (v3 != null) FolderThumbnail(v3, Modifier.weight(1f).fillMaxHeight())
                    if (v4 != null) FolderThumbnail(v4, Modifier.weight(1f).fillMaxHeight())
                }
            }
            
            // Premium dark gradient overlay for text readability
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF001F3F).copy(alpha = 0.9f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
            )
            
            // New Badge if any video is new
            if (videos.any { it.playCount == 0 }) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    BadgeTag(
                        text = "NEW ITEMS",
                        gradient = Brush.linearGradient(listOf(Color(0xFFFF0055), Color(0xFFFF5500)))
                    )
                }
            }
            
            // Text Content at Bottom
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${videos.size} video" + if (videos.size > 1) "s" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        }
    }
}

@Composable
fun FolderThumbnail(video: Video, modifier: Modifier = Modifier) {
    coil.compose.AsyncImage(
        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
            .data(video.uri)
            .crossfade(true)
            .size(256) // Memory optimization
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
            .build(),
        contentDescription = null,
        modifier = modifier,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageDashboardDialog(
    report: com.helpofai.videoplayer.core.scanner.ScannerStorageAnalyzer.StorageReport,
    onDismissRequest: () -> Unit,
    onDeleteClick: (Video) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Storage Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val totalGb = report.totalSize / (1024.0 * 1024.0 * 1024.0)
            Text("${report.totalVideos} videos • ${String.format("%.2f GB", totalGb)} total", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Exact Duplicates
            if (report.exactDuplicates.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exact Duplicates (${report.exactDuplicates.size} groups)", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                report.exactDuplicates.forEach { group ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Exact Duplicates", fontWeight = FontWeight.Bold)
                                if (group.size > 1) {
                                    TextButton(
                                        onClick = {
                                            // Delete all except the first one
                                            group.drop(1).forEach { onDeleteClick(it) }
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Smart Clean (Keep 1)")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            group.forEach { video ->
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                .data(video.uri)
                                                .crossfade(true)
                                                .size(256)
                                                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.title, maxLines = 1, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
                                        Text("${video.formattedSize} • ${video.formattedDuration}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { onDeleteClick(video) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Suspected Corrupted
            if (report.suspectedCorrupted.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Suspected Corrupted (${report.suspectedCorrupted.size})", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                report.suspectedCorrupted.forEach { video ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Size: ${video.formattedSize}, Duration: 0s", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { onDeleteClick(video) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Unused Large Videos
            if (report.unusedLargeVideos.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unused Large Videos", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.height(8.dp))
                report.unusedLargeVideos.forEach { video ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Never played • ${video.formattedSize}", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { onDeleteClick(video) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            
            if (report.exactDuplicates.isEmpty() && report.suspectedCorrupted.isEmpty() && report.unusedLargeVideos.isEmpty()) {
                Text("Your storage looks great! No duplicates or corrupted files found.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
