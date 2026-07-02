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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.blur
import androidx.hilt.navigation.compose.hiltViewModel
import com.helpofai.videoplayer.core.model.Video
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
                    val titleText = if (selectedTab == 0) "" else if (selectedTab == 1) (selectedFolder ?: "Folders") else (selectedFolder ?: "Categories")
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
                            Text(titleText, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis) 
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
                    icon = { Icon(Icons.Default.Movie, contentDescription = "Categories", modifier = Modifier.size(24.dp)) },
                    label = { Text("Categories", style = MaterialTheme.typography.labelMedium) }
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

                    // 4. Most Watched & Continue Watching
                    val continueWatching = state.videos.filter { it.lastPlayedPosition > 0 }.sortedByDescending { it.lastPlayedPosition }
                    if (continueWatching.isNotEmpty()) {
                        SectionTitle("Continue Watching")
                        val listCols = if (isTablet) 2 else 1
                        val chunkedContinue = continueWatching.take(if (isTablet) 4 else 3).chunked(listCols)
                        
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

                    // 7. Collections (Chips)
                    SectionTitle("Categories")
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            CollectionChip("All Videos", "${state.videos.size} items", modifier = Modifier.width(140.dp), onClick = {
                                selectedTab = 2
                                selectedFolder = null
                            })
                        }
                        val categories = state.videos.groupBy { it.category }
                        categories.forEach { (category, vids) ->
                            item {
                                CollectionChip(category, "${vids.size} items", modifier = Modifier.width(140.dp), onClick = {
                                    selectedTab = 2
                                    selectedFolder = category
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
                            chunkedFolders.forEach { rowFolders ->
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
                            chunkedVideos.forEach { rowVideos ->
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
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                } else if (selectedTab == 2) {
                    // Categories View
                    val categories = state.videos.groupBy { it.category }
                    
                    if (selectedFolder == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val catList = categories.toList()
                        val catCols = if (isTablet) 4 else 2
                        val chunkedCats = catList.chunked(catCols)
                        
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            chunkedCats.forEach { rowCats ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    rowCats.forEach { (catName, videosInCat) ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            FolderItemCard(
                                                folderName = catName,
                                                videos = videosInCat,
                                                onClick = { selectedFolder = catName }
                                            )
                                        }
                                    }
                                    val emptySlots = catCols - rowCats.size
                                    for (i in 0 until emptySlots) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    } else {
                        val videosInCat = categories[selectedFolder] ?: emptyList()
                        Spacer(modifier = Modifier.height(16.dp))
                        val listCols = if (isTablet) 2 else 1
                        val chunkedVideos = videosInCat.chunked(listCols)
                        
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            chunkedVideos.forEach { rowVideos ->
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
                                Text(v2.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    .videoFrameMillis(5000) // Fetch frame at 5 seconds for hero
                    .crossfade(true)
                    .build(),
                contentDescription = "Video Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            // Dark Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            
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
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                        .videoFrameMillis(2000)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
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
                modifier = Modifier.weight(1f),
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
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(video.uri)
                    .videoFrameMillis(2000)
                    .crossfade(true)
                    .build(),
                contentDescription = "Video Thumbnail",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
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
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
            )
            
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
            .videoFrameMillis(1000)
            .crossfade(true)
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
                                                .videoFrameMillis(1000)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(video.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
