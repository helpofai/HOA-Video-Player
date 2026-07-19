package com.helpofai.videoplayer.feature.filemanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.documentfile.provider.DocumentFile
import com.helpofai.videoplayer.core.model.Video
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ExplorerBgDeep    = Color(0xFF090B10)
private val ExplorerBgCard    = Color(0xFF111520)
private val ExplorerAccentC   = Color(0xFF00CEC9)
private val ExplorerAccentG   = Color(0xFF00B894)
private val ExplorerAccentP   = Color(0xFF7C5CE7)
private val ExplorerTextPri   = Color(0xFFECF0F1)
private val ExplorerTextSub   = Color(0xFF8E9CB0)
private val ExplorerDivider   = Color(0xFF1E2535)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    onVideoClick: (Video) -> Unit,
    onNavigateToTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FileManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val nodes by viewModel.nodes.collectAsState()
    val currentRoot by viewModel.currentRoot.collectAsState()
    val isBrowsingSaf by viewModel.isBrowsingSaf.collectAsState()
    val activeSafTree by viewModel.activeSafTree.collectAsState()
    val activeSafCurrentUri by viewModel.activeSafCurrentUri.collectAsState()
    val safBookmarks by viewModel.safBookmarks.collectAsState()
    
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedNodes by viewModel.selectedNodes.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val pinnedPaths by viewModel.pinnedPaths.collectAsState()
    val trashItems by viewModel.trashItems.collectAsState()

    // Dialog & UI Visibility states
    var activeNodeMenu by remember { mutableStateOf<FileManagerNode?>(null) }
    var activeRenameNode by remember { mutableStateOf<FileManagerNode?>(null) }
    var activeInfoNode by remember { mutableStateOf<FileManagerNode?>(null) }
    var renameInput by remember { mutableStateOf("") }
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var createNameInput by remember { mutableStateOf("") }
    var createTypeFolder by remember { mutableStateOf(true) }

    var showTrashDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    
    // File Previews states
    var previewTextNode by remember { mutableStateOf<FileManagerNode?>(null) }
    var previewImageNode by remember { mutableStateOf<FileManagerNode?>(null) }
    var previewAudioNode by remember { mutableStateOf<FileManagerNode?>(null) }
    var textPreviewContent by remember { mutableStateOf("") }

    val textExtensions = setOf("txt", "srt", "vtt", "ass", "xml", "json", "html", "css", "ini", "log", "csv")
    val imgExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    // SAF directory launcher
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            viewModel.registerSafTreeUri(it)
            Toast.makeText(context, "Added storage bookmark", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission checking helper
    fun checkStoragePermission(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    var hasPermission by remember { mutableStateOf(checkStoragePermission(context)) }

    // Re-verify permission on Resume
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = checkStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Android 10 and below permission request launcher
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
    }

    if (!hasPermission) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ExplorerBgDeep)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(ExplorerAccentP.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Shield, null, tint = ExplorerAccentP, modifier = Modifier.size(40.dp))
                }
                
                Text(
                    "All Files Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ExplorerTextPri,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    "To perform file operations like copy, paste, delete, rename, zip compression, and subfolder scans, Android requires 'All Files Access' permission.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExplorerTextSub,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                context.startActivity(intent)
                            }
                        } else {
                            storagePermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExplorerAccentC, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                ) {
                    Text("Grant All Files Access", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(ExplorerBgDeep)
        ) {
        // ── Top Action Toolbar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Explorer",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ExplorerTextPri
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Bookmarks Manager Icon
                IconButton(onClick = { showBookmarksDialog = true }) {
                    Box {
                        Icon(Icons.Default.Bookmarks, "Bookmarks", tint = ExplorerAccentC)
                        if (safBookmarks.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ExplorerAccentC)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
                // Recycle Bin Icon
                IconButton(onClick = { showTrashDialog = true }) {
                    Box {
                        Icon(Icons.Default.DeleteSweep, "Trash Bin", tint = ExplorerAccentP)
                        if (trashItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
                // Add Folder/File Icon
                IconButton(onClick = {
                    createNameInput = ""
                    createTypeFolder = true
                    showCreateDialog = true
                }) {
                    Icon(Icons.Default.CreateNewFolder, "Create folder/file", tint = ExplorerAccentC)
                }
            }
        }

        // ── Search & Filter Panel ─────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search files & folders...", color = ExplorerTextSub) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ExplorerTextSub) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = ExplorerTextSub)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ExplorerTextPri,
                unfocusedTextColor = ExplorerTextPri,
                focusedBorderColor = ExplorerAccentC,
                unfocusedBorderColor = ExplorerDivider,
                focusedContainerColor = ExplorerBgCard,
                unfocusedContainerColor = ExplorerBgCard
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        // ── Breadcrumb Path Navigation ────────────────────────────────────────
        if (isBrowsingSaf) {
            val activeTreeName = activeSafTree?.displayName ?: "Storage"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateUp() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ExplorerTextPri)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        activeTreeName,
                        color = ExplorerAccentC,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { viewModel.loadSafDirectory(activeSafTree!!, null) }
                    )
                    activeSafCurrentUri?.let { uri ->
                        Text(" > ", color = ExplorerTextSub, fontSize = 11.sp)
                        val name = DocumentFile.fromSingleUri(context, Uri.parse(uri))?.name ?: "Folder"
                        Text(
                            name,
                            color = ExplorerAccentC,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            FileManagerPathHeader(
                currentRoot = currentRoot,
                onPathClick = { path -> viewModel.loadRootDirectory(path) },
                onNavigateUp = { viewModel.navigateUp() }
            )
        }

        // ── Pinned Favorites horizontal bar ───────────────────────────────────
        if (pinnedPaths.isNotEmpty() && searchQuery.isEmpty() && !isBrowsingSaf) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    "Pinned Folders",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExplorerAccentC,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pinnedPaths.toList()) { path ->
                        val folderFile = File(path)
                        Surface(
                            modifier = Modifier.clickable {
                                viewModel.loadRootDirectory(folderFile)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = ExplorerBgCard,
                            border = BorderStroke(1.dp, ExplorerDivider)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Folder, null, tint = ExplorerAccentC, modifier = Modifier.size(16.dp))
                                Text(
                                    folderFile.name.ifEmpty { "Root" },
                                    fontSize = 11.sp,
                                    color = ExplorerTextPri,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Sorting & Filtering Chips Row ─────────────────────────────────────
        FileManagerControlsRow(
            sortOption = sortOption,
            filterOption = filterOption,
            onSortChange = { viewModel.updateSortOption(it) },
            onFilterChange = { viewModel.updateFilterOption(it) }
        )

        // ── Multi-Select Actions ──────────────────────────────────────────────
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = ExplorerAccentP.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, ExplorerAccentP.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedNodes.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExplorerTextPri
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isBrowsingSaf) {
                            IconButton(onClick = { viewModel.copySelectedNodes() }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ExplorerAccentC)
                            }
                            IconButton(onClick = { viewModel.cutSelectedNodes() }) {
                                Icon(Icons.Default.ContentCut, contentDescription = "Cut", tint = ExplorerAccentC)
                            }
                        }
                        IconButton(
                            onClick = {
                                if (selectedNodes.isNotEmpty()) {
                                    viewModel.deleteSelectedNodes()
                                    Toast.makeText(context, "Selected files deleted", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = selectedNodes.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                        }
                        IconButton(onClick = { viewModel.toggleMultiSelectMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = ExplorerTextPri)
                        }
                    }
                }
            }
        }

        // ── File Tree View ────────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            if (nodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null, tint = ExplorerTextSub, modifier = Modifier.size(44.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No results found" else "Empty Folder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExplorerTextSub
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(nodes, key = { _, node -> node.path }) { index, node ->
                        val isPinned = pinnedPaths.contains(node.path)
                        FileManagerNodeRow(
                            node = node,
                            nodeIndex = index,
                            nodesList = nodes,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = selectedNodes.contains(node.path),
                            isPinned = isPinned,
                            onNodeClick = { clickedNode ->
                                if (isMultiSelectMode) {
                                    viewModel.toggleSelectNode(clickedNode)
                                } else {
                                    if (clickedNode.isDirectory) {
                                        if (isBrowsingSaf) {
                                            viewModel.loadSafDirectory(activeSafTree!!, clickedNode.path)
                                        } else {
                                            viewModel.toggleFolder(clickedNode)
                                        }
                                    } else if (clickedNode.isVideo) {
                                        val video = Video(
                                            id = clickedNode.file?.lastModified() ?: System.currentTimeMillis(),
                                            uri = if (clickedNode.file != null) Uri.fromFile(clickedNode.file) else Uri.parse(clickedNode.path),
                                            title = clickedNode.name,
                                            duration = 0L,
                                            size = clickedNode.file?.length() ?: 0L,
                                            dateAdded = (clickedNode.file?.lastModified() ?: System.currentTimeMillis()) / 1000L,
                                            path = clickedNode.path
                                        )
                                        onVideoClick(video)
                                    } else if (clickedNode.extension in textExtensions) {
                                        viewModel.loadTextPreview(
                                            node = clickedNode,
                                            onLoaded = { content ->
                                                textPreviewContent = content
                                                previewTextNode = clickedNode
                                            },
                                            onError = {
                                                Toast.makeText(context, "Cannot preview file", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else if (clickedNode.extension in imgExtensions) {
                                        previewImageNode = clickedNode
                                    } else if (clickedNode.isAudio) {
                                        previewAudioNode = clickedNode
                                    } else {
                                        Toast.makeText(context, "Clicked file: ${clickedNode.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onNodeLongClick = { clickedNode ->
                                if (!isMultiSelectMode) {
                                    activeNodeMenu = clickedNode
                                }
                            },
                            onExploreFolder = { clickedNode ->
                                if (isBrowsingSaf) {
                                    viewModel.loadSafDirectory(activeSafTree!!, clickedNode.path)
                                } else {
                                    viewModel.loadRootDirectory(clickedNode.file!!)
                                }
                            }
                        )
                    }
                }
            }

            // Floating Clipboard Paste Bar
            clipboard?.let { clip ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = ExplorerBgCard,
                    border = BorderStroke(1.dp, ExplorerAccentC),
                    tonalElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (clip.isCut) "Moving items..." else "Copying items...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ExplorerTextPri
                            )
                            Text("${clip.paths.size} items in clipboard", fontSize = 10.sp, color = ExplorerTextSub)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.clearClipboard() }) {
                                Text("Cancel", color = Color.Red)
                            }
                            Button(
                                onClick = { viewModel.pasteClipboard() },
                                colors = ButtonDefaults.buttonColors(containerColor = ExplorerAccentG, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Paste Here", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Bookmarks Manager ─────────────────────────────────────────────
    if (showBookmarksDialog) {
        Dialog(onDismissRequest = { showBookmarksDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Storage Bookmarks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExplorerTextPri)
                        IconButton(onClick = { showBookmarksDialog = false }) {
                            Icon(Icons.Default.Close, null, tint = ExplorerTextSub)
                        }
                    }
                    HorizontalDivider(color = ExplorerDivider)

                    // Add Bookmark Button
                    Button(
                        onClick = {
                            safLauncher.launch(null)
                            showBookmarksDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExplorerAccentC, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null)
                            Text("Select SAF Directory", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Registered Bookmarks", fontSize = 11.sp, color = ExplorerTextSub, fontWeight = FontWeight.Bold)

                    if (safBookmarks.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No custom directories added", color = ExplorerTextSub)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Primary Storage Option
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (!isBrowsingSaf) ExplorerAccentC.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.loadRootDirectory()
                                            showBookmarksDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Storage, null, tint = ExplorerAccentC)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Primary Storage", style = MaterialTheme.typography.bodyMedium, color = ExplorerTextPri, fontWeight = FontWeight.Bold)
                                        Text("Internal filesystem root", fontSize = 10.sp, color = ExplorerTextSub)
                                    }
                                }
                            }

                            items(safBookmarks) { bookmark ->
                                val active = isBrowsingSaf && activeSafTree?.uriString == bookmark.uriString
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (active) ExplorerAccentC.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.02f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            viewModel.loadSafDirectory(bookmark, null)
                                            showBookmarksDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.FolderSpecial, null, tint = ExplorerAccentP)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(bookmark.displayName, style = MaterialTheme.typography.bodyMedium, color = ExplorerTextPri, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(bookmark.uriString, fontSize = 8.sp, color = ExplorerTextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.unregisterSafBookmark(bookmark) }) {
                                        Icon(Icons.Default.BookmarkRemove, "Remove", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Create Folder or Empty File ───────────────────────────────────
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Create New Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExplorerTextPri)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { createTypeFolder = true }) {
                            RadioButton(selected = createTypeFolder, onClick = { createTypeFolder = true })
                            Text("Folder", color = ExplorerTextPri)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { createTypeFolder = false }) {
                            RadioButton(selected = !createTypeFolder, onClick = { createTypeFolder = false })
                            Text("File", color = ExplorerTextPri)
                        }
                    }

                    OutlinedTextField(
                        value = createNameInput,
                        onValueChange = { createNameInput = it },
                        placeholder = { Text(if (createTypeFolder) "Folder Name" else "File Name (e.g. text.txt)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ExplorerTextPri,
                            unfocusedTextColor = ExplorerTextPri,
                            focusedBorderColor = ExplorerAccentC,
                            unfocusedBorderColor = ExplorerDivider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = ExplorerTextSub)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (createTypeFolder) {
                                    viewModel.createFolder(createNameInput)
                                } else {
                                    viewModel.createEmptyFile(createNameInput)
                                }
                                showCreateDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExplorerAccentC, contentColor = Color.Black)
                        ) {
                            Text("Create", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Recycle Bin / Trash ───────────────────────────────────────────
    if (showTrashDialog) {
        Dialog(onDismissRequest = { showTrashDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recycle Bin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExplorerTextPri)
                        IconButton(onClick = { showTrashDialog = false }) {
                            Icon(Icons.Default.Close, null, tint = ExplorerTextSub)
                        }
                    }
                    HorizontalDivider(color = ExplorerDivider)

                    if (trashItems.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Recycle bin is empty", color = ExplorerTextSub)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(trashItems) { item ->
                                val origFile = File(item.originalPath)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(origFile.name, style = MaterialTheme.typography.bodyMedium, color = ExplorerTextPri, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(origFile.parent ?: "", fontSize = 9.sp, color = ExplorerTextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { viewModel.restoreFromTrash(item) }) {
                                        Icon(Icons.Default.Restore, "Restore", tint = ExplorerAccentG)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Text File Preview ─────────────────────────────────────────────
    previewTextNode?.let { node ->
        Dialog(onDismissRequest = { previewTextNode = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(node.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ExplorerAccentC)
                        IconButton(onClick = { previewTextNode = null }) {
                            Icon(Icons.Default.Close, null, tint = ExplorerTextSub)
                        }
                    }
                    HorizontalDivider(color = ExplorerDivider)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = textPreviewContent.ifEmpty { "Empty File" },
                            fontSize = 11.sp,
                            color = ExplorerTextPri,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // ── Dialog: Image File Preview ────────────────────────────────────────────
    previewImageNode?.let { node ->
        Dialog(onDismissRequest = { previewImageNode = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = if (node.file != null) node.file else Uri.parse(node.path),
                        contentDescription = "Image preview",
                        modifier = Modifier.fillMaxSize().padding(12.dp)
                    )
                    IconButton(
                        onClick = { previewImageNode = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
            }
        }
    }

    // ── Dialog: Audio File Preview (Mini Player) ──────────────────────────────
    previewAudioNode?.let { node ->
        Dialog(onDismissRequest = { previewAudioNode = null }) {
            var isPlaying by remember { mutableStateOf(false) }
            var currentPosition by remember { mutableStateOf(0) }
            var duration by remember { mutableStateOf(1) }
            val mediaPlayer = remember { MediaPlayer() }
            
            DisposableEffect(node) {
                try {
                    if (node.file != null) {
                        mediaPlayer.setDataSource(node.path)
                    } else {
                        mediaPlayer.setDataSource(context, Uri.parse(node.uriString!!))
                    }
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                    isPlaying = true
                    duration = mediaPlayer.duration.coerceAtLeast(1)
                } catch (e: Exception) {
                    Toast.makeText(context, "Cannot play audio file", Toast.LENGTH_SHORT).show()
                }
                
                onDispose {
                    mediaPlayer.stop()
                    mediaPlayer.release()
                }
            }

            // Polling progress position coroutine
            LaunchedEffect(isPlaying) {
                while (isPlaying) {
                    currentPosition = mediaPlayer.currentPosition
                    delay(300)
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Audio Player", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExplorerTextPri)
                        IconButton(onClick = { previewAudioNode = null }) {
                            Icon(Icons.Default.Close, null, tint = ExplorerTextSub)
                        }
                    }
                    HorizontalDivider(color = ExplorerDivider)

                    // Disc Icon
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(ExplorerAccentP.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = ExplorerAccentP, modifier = Modifier.size(36.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(node.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ExplorerTextPri, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(node.formattedSize, fontSize = 10.sp, color = ExplorerTextSub)
                    }

                    // Progress Slider
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = currentPosition.toFloat(),
                            onValueChange = {
                                mediaPlayer.seekTo(it.toInt())
                                currentPosition = it.toInt()
                            },
                            valueRange = 0f..duration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = ExplorerAccentP,
                                activeTrackColor = ExplorerAccentP
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = String.format("%d:%02d", (currentPosition / 1000) / 60, (currentPosition / 1000) % 60),
                                fontSize = 9.sp,
                                color = ExplorerTextSub
                            )
                            Text(
                                text = String.format("%d:%02d", (duration / 1000) / 60, (duration / 1000) % 60),
                                fontSize = 9.sp,
                                color = ExplorerTextSub
                            )
                        }
                    }

                    // Audio Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    mediaPlayer.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ExplorerAccentP)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: Folder/File Options Context Menu ──────────────────────────────
    activeNodeMenu?.let { node ->
        val isPinned = pinnedPaths.contains(node.path)
        Dialog(onDismissRequest = { activeNodeMenu = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        node.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExplorerTextPri,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    HorizontalDivider(color = ExplorerDivider)

                    if (node.isDirectory) {
                        FileManagerMenuItem(
                            title = "Explore inside",
                            icon = Icons.Default.Launch,
                            color = ExplorerAccentC
                        ) {
                            activeNodeMenu = null
                            if (isBrowsingSaf) {
                                viewModel.loadSafDirectory(activeSafTree!!, node.path)
                            } else {
                                viewModel.loadRootDirectory(node.file!!)
                            }
                        }
                        
                        if (!isBrowsingSaf) {
                            FileManagerMenuItem(
                                title = if (isPinned) "Unpin from favorites" else "Pin to favorites",
                                icon = if (isPinned) Icons.Default.BookmarkRemove else Icons.Default.BookmarkAdd,
                                color = ExplorerTextPri
                            ) {
                                viewModel.togglePinDirectory(node)
                                activeNodeMenu = null
                            }
                        }
                    }

                    FileManagerMenuItem(
                        title = "Rename",
                        icon = Icons.Default.Edit,
                        color = ExplorerTextPri
                    ) {
                        renameInput = node.name
                        activeRenameNode = node
                        activeNodeMenu = null
                    }

                    if (!isBrowsingSaf) {
                        FileManagerMenuItem(
                            title = "Copy",
                            icon = Icons.Default.ContentCopy,
                            color = ExplorerTextPri
                        ) {
                            viewModel.copySingleNode(node)
                            activeNodeMenu = null
                            Toast.makeText(context, "Copied ${node.name}", Toast.LENGTH_SHORT).show()
                        }

                        FileManagerMenuItem(
                            title = "Cut (Move)",
                            icon = Icons.Default.ContentCut,
                            color = ExplorerTextPri
                        ) {
                            viewModel.cutSingleNode(node)
                            activeNodeMenu = null
                            Toast.makeText(context, "Cut ${node.name}", Toast.LENGTH_SHORT).show()
                        }

                        // ZIP/UNZIP
                        if (node.extension == "zip") {
                            FileManagerMenuItem(
                                title = "Extract ZIP",
                                icon = Icons.Default.Unarchive,
                                color = ExplorerAccentG
                            ) {
                                viewModel.unzipNode(node)
                                activeNodeMenu = null
                                Toast.makeText(context, "Extracting zip...", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            FileManagerMenuItem(
                                title = "Compress to ZIP",
                                icon = Icons.Default.Archive,
                                color = ExplorerAccentP
                            ) {
                                viewModel.zipNode(node)
                                activeNodeMenu = null
                                Toast.makeText(context, "Compressing to zip...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    FileManagerMenuItem(
                        title = "Share",
                        icon = Icons.Default.Share,
                        color = ExplorerTextPri
                    ) {
                        activeNodeMenu = null
                        val fileUri = if (node.file != null) Uri.fromFile(node.file) else Uri.parse(node.path)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (node.isVideo) "video/*" else if (node.isAudio) "audio/*" else "*/*"
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share via"))
                    }

                    FileManagerMenuItem(
                        title = "Details",
                        icon = Icons.Default.Info,
                        color = ExplorerTextPri
                    ) {
                        activeInfoNode = node
                        activeNodeMenu = null
                    }

                    if (!node.isDirectory) {
                        if (node.isVideo) {
                            FileManagerMenuItem(
                                title = "Play in Watch Party Room",
                                icon = Icons.Default.Group,
                                color = ExplorerAccentP
                            ) {
                                activeNodeMenu = null
                                val video = Video(
                                    id = node.file?.lastModified() ?: System.currentTimeMillis(),
                                    uri = if (node.file != null) Uri.fromFile(node.file) else Uri.parse(node.path),
                                    title = node.name,
                                    duration = 0L,
                                    size = node.file?.length() ?: 0L,
                                    dateAdded = (node.file?.lastModified() ?: System.currentTimeMillis()) / 1000L,
                                    path = node.path
                                )
                                val sessionMgr = com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager.getInstance()
                                if (sessionMgr.activeSession.value == null) {
                                    val ip = getLocalIpAddress()
                                    val device = android.os.Build.MODEL
                                    sessionMgr.createSession(
                                        name = "Quick Room",
                                        hostIp = ip,
                                        hostDeviceName = device,
                                        video = video,
                                        securityToken = "HOA_SECURE_TOKEN"
                                    )
                                } else {
                                    sessionMgr.setStreamingVideo(video)
                                }
                                Toast.makeText(context, "Loaded in Watch Party", Toast.LENGTH_SHORT).show()
                                onNavigateToTab(4)
                            }
                        }
                    }

                    FileManagerMenuItem(
                        title = "Multi-select Mode",
                        icon = Icons.Default.Checklist,
                        color = ExplorerTextPri
                    ) {
                        viewModel.toggleMultiSelectMode()
                        viewModel.toggleSelectNode(node)
                        activeNodeMenu = null
                    }

                    HorizontalDivider(color = ExplorerDivider)
                    
                    if (!isBrowsingSaf) {
                        FileManagerMenuItem(
                            title = "Move to Trash (Recycle Bin)",
                            icon = Icons.Default.DeleteSweep,
                            color = Color.Yellow
                        ) {
                            activeNodeMenu = null
                            viewModel.moveToTrash(node)
                            Toast.makeText(context, "${node.name} moved to trash", Toast.LENGTH_SHORT).show()
                        }
                    }
                    
                    FileManagerMenuItem(
                        title = "Delete Permanently",
                        icon = Icons.Default.Delete,
                        color = Color.Red
                    ) {
                        activeNodeMenu = null
                        viewModel.deleteNode(node)
                        Toast.makeText(context, "${node.name} permanently deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // ── Dialog: Rename Overlay ────────────────────────────────────────────────
    activeRenameNode?.let { node ->
        Dialog(onDismissRequest = { activeRenameNode = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Rename Item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExplorerTextPri)
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ExplorerTextPri,
                            unfocusedTextColor = ExplorerTextPri,
                            focusedBorderColor = ExplorerAccentC,
                            unfocusedBorderColor = ExplorerDivider
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { activeRenameNode = null }) {
                            Text("Cancel", color = ExplorerTextSub)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.renameNode(node, renameInput)
                                activeRenameNode = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExplorerAccentC, contentColor = Color.Black)
                        ) {
                            Text("Rename", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── Dialog: File Details Sheet ────────────────────────────────────────────
    activeInfoNode?.let { node ->
        Dialog(onDismissRequest = { activeInfoNode = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = ExplorerBgCard,
                border = BorderStroke(1.dp, ExplorerDivider),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ExplorerTextPri)
                        IconButton(onClick = { activeInfoNode = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = ExplorerTextSub)
                        }
                    }
                    HorizontalDivider(color = ExplorerDivider)

                    FileManagerInfoRow(label = "Name", value = node.name)
                    FileManagerInfoRow(label = "Path", value = node.path)
                    FileManagerInfoRow(label = "Size", value = node.formattedSize)
                    
                    if (node.file != null) {
                        FileManagerInfoRow(
                            label = "Last Modified",
                            value = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(node.file.lastModified()))
                        )
                        
                        // Permissions Handling
                        val r = if (node.file.canRead()) "Yes" else "No"
                        val w = if (node.file.canWrite()) "Yes" else "No"
                        val x = if (node.file.canExecute()) "Yes" else "No"
                        FileManagerInfoRow(label = "Permissions", value = "Read: $r • Write: $w • Execute: $x")
                    } else {
                        FileManagerInfoRow(label = "Storage", value = "Storage Access Framework Tree Document")
                    }
                    
                    if (node.isVideo) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ExplorerAccentC.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, ExplorerAccentC.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = ExplorerAccentC)
                                Column {
                                    Text("Video Health Status", style = MaterialTheme.typography.labelSmall, color = ExplorerAccentC, fontWeight = FontWeight.Bold)
                                    Text("Container parsed. Offline decoder is ready to stream.", fontSize = 10.sp, color = ExplorerTextSub)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun FileManagerControlsRow(
    sortOption: FileManagerSortOption,
    filterOption: FileManagerFilterOption,
    onSortChange: (FileManagerSortOption) -> Unit,
    onFilterChange: (FileManagerFilterOption) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sort toggle chip
        Box {
            AssistChip(
                onClick = { showSortMenu = true },
                label = { Text("Sort: ${sortOption.name.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = ExplorerTextPri) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = ExplorerTextSub, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = ExplorerBgCard),
                border = BorderStroke(1.dp, ExplorerDivider)
            )
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                FileManagerSortOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onSortChange(option)
                            showSortMenu = false
                        }
                    )
                }
            }
        }

        // Filter toggle chip
        Box {
            AssistChip(
                onClick = { showFilterMenu = true },
                label = { Text("Filter: ${filterOption.name.lowercase().replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = ExplorerTextPri) },
                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = ExplorerTextSub, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = ExplorerBgCard),
                border = BorderStroke(1.dp, ExplorerDivider)
            )
            DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                FileManagerFilterOption.values().forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onFilterChange(option)
                            showFilterMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileManagerPathHeader(
    currentRoot: File,
    onPathClick: (File) -> Unit,
    onNavigateUp: () -> Unit
) {
    val externalStoragePath = Environment.getExternalStorageDirectory().absolutePath
    val relativePath = currentRoot.absolutePath.removePrefix(externalStoragePath)
    val parts = relativePath.split(File.separator).filter { it.isNotEmpty() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateUp) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ExplorerTextPri)
        }
        
        Spacer(modifier = Modifier.width(6.dp))

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Storage",
                color = if (parts.isEmpty()) ExplorerAccentC else ExplorerTextSub,
                fontWeight = if (parts.isEmpty()) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onPathClick(Environment.getExternalStorageDirectory()) }
            )
            
            parts.forEachIndexed { index, part ->
                Text(" > ", color = ExplorerTextSub, fontSize = 11.sp)
                val resolvedPath = StringBuilder(externalStoragePath)
                for (i in 0..index) {
                    resolvedPath.append(File.separator).append(parts[i])
                }
                val file = File(resolvedPath.toString())
                Text(
                    text = part,
                    color = if (index == parts.lastIndex) ExplorerAccentC else ExplorerTextSub,
                    fontWeight = if (index == parts.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onPathClick(file) }
                )
            }
        }
    }
}

@Composable
private fun TreeConnectorLines(
    nodeIndex: Int,
    nodeDepth: Int,
    nodesList: List<FileManagerNode>,
    modifier: Modifier = Modifier
) {
    val columnWidthDp = 18.dp
    val columnWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { columnWidthDp.toPx() }
    val lineColor = ExplorerDivider

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .width((nodeDepth * 18).dp)
            .fillMaxHeight()
    ) {
        val height = size.height
        val midY = height / 2f

        for (d in 0 until nodeDepth) {
            val startX = d * columnWidthPx + columnWidthPx / 2f
            
            if (d == nodeDepth - 1) {
                // Draw vertical line from top to midY
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(startX, 0f),
                    end = androidx.compose.ui.geometry.Offset(startX, midY),
                    strokeWidth = 2f
                )

                // Check if this is the last sibling in the list at this depth
                var hasMoreSiblings = false
                for (idx in nodeIndex + 1 until nodesList.size) {
                    val nextNode = nodesList[idx]
                    if (nextNode.depth < nodeDepth) {
                        break
                    }
                    if (nextNode.depth == nodeDepth) {
                        hasMoreSiblings = true
                        break
                    }
                }

                if (hasMoreSiblings) {
                    // Continue vertical line to the bottom
                    drawLine(
                        color = lineColor,
                        start = androidx.compose.ui.geometry.Offset(startX, midY),
                        end = androidx.compose.ui.geometry.Offset(startX, height),
                        strokeWidth = 2f
                    )
                }

                // Draw horizontal line to the right
                drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(startX, midY),
                    end = androidx.compose.ui.geometry.Offset(startX + columnWidthPx / 2f, midY),
                    strokeWidth = 2f
                )
            } else {
                // Ancestor depth. Draw full vertical line if parent has subsequent siblings
                var drawVertical = false
                for (idx in nodeIndex + 1 until nodesList.size) {
                    val nextNode = nodesList[idx]
                    if (nextNode.depth < d + 1) {
                        break
                    }
                    if (nextNode.depth == d + 1) {
                        drawVertical = true
                        break
                    }
                }
                
                if (drawVertical) {
                    drawLine(
                        color = lineColor,
                        start = androidx.compose.ui.geometry.Offset(startX, 0f),
                        end = androidx.compose.ui.geometry.Offset(startX, height),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}

private fun resolveFileIconAndColor(node: FileManagerNode): Pair<ImageVector, Color> {
    if (node.isDirectory) {
        return Pair(Icons.Default.Folder, Color(0xFF00CEC9)) // ExplorerAccentC
    }
    val ext = node.extension.lowercase()
    return when {
        node.isVideo || ext in setOf("mp4", "mkv", "avi", "mov", "flv", "webm", "3gp", "ts", "m4v") -> {
            Pair(Icons.Default.PlayCircle, Color(0xFF2ECC71)) // Emerald Green
        }
        node.isAudio || ext in setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus") -> {
            Pair(Icons.Default.MusicNote, Color(0xFF9B59B6)) // Amethyst Purple
        }
        node.isSubtitle || ext in setOf("srt", "ass", "vtt", "sub", "ssa") -> {
            Pair(Icons.Default.Subtitles, Color(0xFFF1C40F)) // Yellow
        }
        ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg") -> {
            Pair(Icons.Default.Image, Color(0xFF1ABC9C)) // Turquoise
        }
        ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> {
            Pair(Icons.Default.Archive, Color(0xFFE67E22)) // Orange
        }
        ext in setOf("kt", "java", "py", "js", "ts", "cpp", "c", "h", "cs", "go", "sh", "bat", "html", "css", "xml", "json") -> {
            Pair(Icons.Default.Code, Color(0xFF3498DB)) // Blue
        }
        ext in setOf("apk", "aab") -> {
            Pair(Icons.Default.Android, Color(0xFF3DDC84)) // Android Green
        }
        ext == "pdf" -> {
            Pair(Icons.Default.PictureInPicture, Color(0xFFE74C3C)) // Red
        }
        ext in setOf("doc", "docx", "rtf", "odt", "txt") -> {
            Pair(Icons.Default.Article, Color(0xFF2980B9)) // Word Blue
        }
        ext in setOf("xls", "xlsx", "csv", "ods") -> {
            Pair(Icons.Default.List, Color(0xFF27AE60)) // Excel Green
        }
        else -> {
            Pair(Icons.Default.Description, Color(0xFF8E9CB0)) // ExplorerTextSub
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FileManagerNodeRow(
    node: FileManagerNode,
    nodeIndex: Int,
    nodesList: List<FileManagerNode>,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    isPinned: Boolean,
    onNodeClick: (FileManagerNode) -> Unit,
    onNodeLongClick: (FileManagerNode) -> Unit,
    onExploreFolder: (FileManagerNode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .combinedClickable(
                onClick = { onNodeClick(node) },
                onLongClick = { onNodeLongClick(node) }
            )
            .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (node.depth > 0) {
            TreeConnectorLines(
                nodeIndex = nodeIndex,
                nodeDepth = node.depth,
                nodesList = nodesList,
                modifier = Modifier.fillMaxHeight()
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onNodeClick(node) },
                colors = CheckboxDefaults.colors(
                    checkedColor = ExplorerAccentP,
                    uncheckedColor = ExplorerTextSub
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // Expanded/Collapsed caret for folders
        if (node.isDirectory) {
            Icon(
                imageVector = if (node.isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = ExplorerTextSub,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onNodeClick(node) }
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else {
            Spacer(modifier = Modifier.width(24.dp))
        }

        // File/Folder type icon resolved dynamically
        val (icon, iconColor) = resolveFileIconAndColor(node)

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Name and details column
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (node.isDirectory) FontWeight.Bold else FontWeight.Normal,
                    color = ExplorerTextPri,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = ExplorerAccentC,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            Text(
                text = node.formattedSize,
                fontSize = 10.sp,
                color = ExplorerTextSub
            )
        }

        // Option dots or directory enter button
        if (node.isDirectory && !isMultiSelectMode) {
            IconButton(
                onClick = { onExploreFolder(node) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ArrowForwardIos, contentDescription = "Enter", tint = ExplorerTextSub, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun FileManagerMenuItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
private fun FileManagerInfoRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = ExplorerTextSub)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ExplorerTextPri)
    }
}

private fun getLocalIpAddress(): String {
    try {
        val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            if (networkInterface.isLoopback || !networkInterface.isUp) continue
            val addresses = networkInterface.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                    return address.hostAddress ?: "127.0.0.1"
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "127.0.0.1"
}
