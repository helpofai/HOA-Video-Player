package com.helpofai.videoplayer.feature.filemanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.helpofai.videoplayer.core.data.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

enum class FileManagerSortOption { NAME, SIZE, DATE, TYPE }
enum class FileManagerFilterOption { ALL, VIDEOS, AUDIO, SUBTITLES, IMAGES }

data class FileManagerClipboard(
    val paths: List<String>,
    val isCut: Boolean,
    val isSaf: Boolean = false
)

data class RecycleBinItem(
    val originalPath: String,
    val trashPath: String,
    val deletedTime: Long
)

data class SafBookmark(
    val uriString: String,
    val displayName: String
)

data class ExtractionProgress(
    val fileName: String = "",
    val percentage: Int = 0,
    val progressFraction: Float = 0f,
    val isActive: Boolean = false
)

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val videoBytes: Long,
    val audioBytes: Long,
    val imageBytes: Long,
    val otherBytes: Long
)

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _nodes = MutableStateFlow<List<FileManagerNode>>(emptyList())
    val nodes = _nodes.asStateFlow()

    private val _currentRoot = MutableStateFlow<File>(Environment.getExternalStorageDirectory())
    val currentRoot = _currentRoot.asStateFlow()

    // ── SAF Browsing States ──────────────────────────────────────────────────
    private val _isBrowsingSaf = MutableStateFlow(false)
    val isBrowsingSaf = _isBrowsingSaf.asStateFlow()

    private val _activeSafTree = MutableStateFlow<SafBookmark?>(null)
    val activeSafTree = _activeSafTree.asStateFlow()

    private val _activeSafCurrentUri = MutableStateFlow<String?>(null) // null means tree root
    val activeSafCurrentUri = _activeSafCurrentUri.asStateFlow()

    private val _safBookmarks = MutableStateFlow<List<SafBookmark>>(emptyList())
    val safBookmarks = _safBookmarks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode = _isMultiSelectMode.asStateFlow()

    private val _selectedNodes = MutableStateFlow<Set<String>>(emptySet())
    val selectedNodes = _selectedNodes.asStateFlow()

    // ── Sorting and Filtering States ──────────────────────────────────────────
    private val _sortOption = MutableStateFlow(FileManagerSortOption.NAME)
    val sortOption = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(FileManagerFilterOption.ALL)
    val filterOption = _filterOption.asStateFlow()

    // ── Storage and Media Visualizer States ────────────────────────────────────
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats = _storageStats.asStateFlow()

    private val _recentFiles = MutableStateFlow<List<FileManagerNode>>(emptyList())
    val recentFiles = _recentFiles.asStateFlow()

    private val _showHiddenFiles = MutableStateFlow(false)
    val showHiddenFiles = _showHiddenFiles.asStateFlow()

    // ── Clipboard State ───────────────────────────────────────────────────────
    private val _clipboard = MutableStateFlow<FileManagerClipboard?>(null)
    val clipboard = _clipboard.asStateFlow()

    // ── Extraction Progress State ─────────────────────────────────────────────
    private val _extractionProgress = MutableStateFlow(ExtractionProgress())
    val extractionProgress = _extractionProgress.asStateFlow()

    fun dismissExtractionProgress() {
        _extractionProgress.value = ExtractionProgress()
    }

    // ── Favorites / Pinned Folders ────────────────────────────────────────────
    private val _pinnedPaths = MutableStateFlow<Set<String>>(emptySet())
    val pinnedPaths = _pinnedPaths.asStateFlow()

    // ── Recycle Bin State ─────────────────────────────────────────────────────
    private val _trashItems = MutableStateFlow<List<RecycleBinItem>>(emptyList())
    val trashItems = _trashItems.asStateFlow()
    
    private val trashFolder = File(context.filesDir, "trash_bin")

    private val videoExtensions = setOf(
        "mp4", "mkv", "avi", "mov", "flv", "wmv", "webm", "3gp", 
        "mpeg", "mpg", "ts", "m2ts", "m4v", "ogv", "vob", "asf", "rmvb"
    )
    
    private val audioExtensions = setOf(
        "mp3", "aac", "flac", "wav", "ogg", "opus", "m4a", "ac3", "eac3", "dts"
    )

    private val subtitleExtensions = setOf(
        "srt", "ass", "ssa", "vtt", "sub"
    )

    private val imageExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "gif", "bmp"
    )

    init {
        if (!trashFolder.exists()) trashFolder.mkdirs()
        loadPinnedPaths()
        loadSafBookmarks()
        loadRootDirectory()
        calculateStorageStats()
        loadRecentFiles()
    }

    fun loadRootDirectory(directory: File = _currentRoot.value) {
        _isBrowsingSaf.value = false
        _activeSafTree.value = null
        _activeSafCurrentUri.value = null
        _currentRoot.value = directory
        _nodes.value = emptyList()
        _selectedNodes.value = emptySet()
        _isMultiSelectMode.value = false
        
        viewModelScope.launch(Dispatchers.IO) {
            val rootNodes = loadNodeContents(directory, depth = 0)
            _nodes.value = rootNodes
        }
    }

    // ── SAF Directory Actions ────────────────────────────────────────────────
    fun loadSafDirectory(bookmark: SafBookmark, folderUriString: String? = null) {
        _isBrowsingSaf.value = true
        _activeSafTree.value = bookmark
        _activeSafCurrentUri.value = folderUriString // null indicates root of tree
        _nodes.value = emptyList()
        _selectedNodes.value = emptySet()
        _isMultiSelectMode.value = false

        viewModelScope.launch(Dispatchers.IO) {
            val rootNodes = loadSafNodes(bookmark.uriString, folderUriString, depth = 0)
            _nodes.value = rootNodes
        }
    }

    fun registerSafTreeUri(uri: Uri) {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            
            // Get display name
            val doc = DocumentFile.fromTreeUri(context, uri)
            val name = doc?.name ?: "Custom Folder"
            
            val prefs = context.getSharedPreferences("filemanager_prefs", Context.MODE_PRIVATE)
            val current = prefs.getStringSet("saf_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add("$uri|$name")
            prefs.edit().putStringSet("saf_uris", current).apply()
            
            loadSafBookmarks()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unregisterSafBookmark(bookmark: SafBookmark) {
        val prefs = context.getSharedPreferences("filemanager_prefs", Context.MODE_PRIVATE)
        val current = prefs.getStringSet("saf_uris", emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove("${bookmark.uriString}|${bookmark.displayName}")
        prefs.edit().putStringSet("saf_uris", current).apply()
        loadSafBookmarks()
        if (_isBrowsingSaf.value && _activeSafTree.value?.uriString == bookmark.uriString) {
            loadRootDirectory()
        }
    }

    private fun loadSafBookmarks() {
        val prefs = context.getSharedPreferences("filemanager_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getStringSet("saf_uris", emptySet()) ?: emptySet()
        _safBookmarks.value = raw.map { entry ->
            val parts = entry.split("|")
            SafBookmark(
                uriString = parts[0],
                displayName = parts.getOrNull(1) ?: "Custom Folder"
            )
        }
    }

    fun navigateUp() {
        if (_isBrowsingSaf.value) {
            val activeTree = _activeSafTree.value ?: return
            val currentUri = _activeSafCurrentUri.value
            if (currentUri == null || currentUri == activeTree.uriString) {
                // At root of SAF, go back to local storage root
                loadRootDirectory()
            } else {
                viewModelScope.launch(Dispatchers.IO) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(currentUri))
                    val parentDoc = doc?.parentFile
                    withContext(Dispatchers.Main) {
                        if (parentDoc == null || parentDoc.uri.toString() == activeTree.uriString) {
                            loadSafDirectory(activeTree, null)
                        } else {
                            loadSafDirectory(activeTree, parentDoc.uri.toString())
                        }
                    }
                }
            }
        } else {
            val parent = _currentRoot.value.parentFile
            if (parent != null && parent.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)) {
                loadRootDirectory(parent)
            }
        }
    }

    private fun loadNodeContents(directory: File, depth: Int): List<FileManagerNode> {
        val files = directory.listFiles() ?: return emptyList()
        
        val filteredFiles = files.filter { file ->
            val ext = file.extension.lowercase()
            val matchesFilter = when (_filterOption.value) {
                FileManagerFilterOption.ALL -> true
                FileManagerFilterOption.VIDEOS -> file.isDirectory || ext in videoExtensions
                FileManagerFilterOption.AUDIO -> file.isDirectory || ext in audioExtensions
                FileManagerFilterOption.SUBTITLES -> file.isDirectory || ext in subtitleExtensions
                FileManagerFilterOption.IMAGES -> file.isDirectory || ext in imageExtensions
            }
            val isHidden = file.name.startsWith(".") || file.isHidden
            val matchesHidden = _showHiddenFiles.value || !isHidden
            matchesFilter && matchesHidden
        }

        val mappedNodes = filteredFiles.map { file ->
            val ext = file.extension.lowercase()
            val isDir = file.isDirectory
            val childCount = if (isDir) file.listFiles()?.size ?: 0 else 0
            
            FileManagerNode(
                file = file,
                isDirectory = isDir,
                isExpanded = false,
                depth = depth,
                isVideo = ext in videoExtensions,
                isSubtitle = ext in subtitleExtensions,
                isAudio = ext in audioExtensions,
                childCount = childCount,
                formattedSize = if (!isDir) formatSize(file.length()) else "$childCount items"
            )
        }

        return sortNodes(mappedNodes)
    }

    private fun loadSafNodes(treeUriString: String, parentUriString: String?, depth: Int): List<FileManagerNode> {
        val rootDoc = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return emptyList()
        val parentDoc = if (parentUriString == null) {
            rootDoc
        } else {
            DocumentFile.fromSingleUri(context, Uri.parse(parentUriString))
        } ?: return emptyList()

        val files = parentDoc.listFiles()
        
        val filteredFiles = files.filter { file ->
            val ext = file.name?.substringAfterLast('.', "")?.lowercase() ?: ""
            val matchesFilter = when (_filterOption.value) {
                FileManagerFilterOption.ALL -> true
                FileManagerFilterOption.VIDEOS -> file.isDirectory || ext in videoExtensions
                FileManagerFilterOption.AUDIO -> file.isDirectory || ext in audioExtensions
                FileManagerFilterOption.SUBTITLES -> file.isDirectory || ext in subtitleExtensions
                FileManagerFilterOption.IMAGES -> file.isDirectory || ext in imageExtensions
            }
            val isHidden = (file.name ?: "").startsWith(".")
            val matchesHidden = _showHiddenFiles.value || !isHidden
            matchesFilter && matchesHidden
        }

        val mappedNodes = filteredFiles.map { file ->
            val name = file.name ?: "Unknown"
            val ext = name.substringAfterLast('.', "").lowercase()
            val isDir = file.isDirectory
            
            FileManagerNode(
                file = null,
                uriString = file.uri.toString(),
                isDirectory = isDir,
                isExpanded = false,
                depth = depth,
                isVideo = ext in videoExtensions,
                isSubtitle = ext in subtitleExtensions,
                isAudio = ext in audioExtensions,
                childCount = 0,
                formattedSize = if (!isDir) formatSize(file.length()) else "Folder",
                customName = name,
                customExtension = ext,
                customPath = file.uri.toString()
            )
        }

        return sortNodes(mappedNodes)
    }

    private fun sortNodes(nodesList: List<FileManagerNode>): List<FileManagerNode> {
        val folders = nodesList.filter { it.isDirectory }
        val files = nodesList.filter { !it.isDirectory }

        val sortSelector: (FileManagerNode) -> Comparable<*>? = { node ->
            when (_sortOption.value) {
                FileManagerSortOption.NAME -> node.name.lowercase()
                FileManagerSortOption.SIZE -> {
                    if (node.file != null) {
                        if (node.isDirectory) 0L else node.file.length()
                    } else {
                        // SAF File length
                        0L
                    }
                }
                FileManagerSortOption.DATE -> {
                    if (node.file != null) node.file.lastModified()
                    else 0L
                }
                FileManagerSortOption.TYPE -> node.extension
            }
        }

        val sortedFolders = when (_sortOption.value) {
            FileManagerSortOption.NAME -> folders.sortedWith(compareBy(sortSelector))
            FileManagerSortOption.DATE -> folders.sortedWith(compareByDescending(sortSelector))
            else -> folders.sortedWith(compareBy(sortSelector))
        }

        val sortedFiles = when (_sortOption.value) {
            FileManagerSortOption.DATE -> files.sortedWith(compareByDescending(sortSelector))
            FileManagerSortOption.SIZE -> files.sortedWith(compareByDescending(sortSelector))
            else -> files.sortedWith(compareBy(sortSelector))
        }

        return sortedFolders + sortedFiles
    }

    fun toggleFolder(node: FileManagerNode) {
        if (_searchQuery.value.isNotEmpty()) return

        val currentList = _nodes.value.toMutableList()
        val index = currentList.indexOfFirst { it.path == node.path }
        if (index == -1) return

        val parent = currentList[index]
        if (parent.isExpanded) {
            val toRemove = mutableListOf<FileManagerNode>()
            for (i in index + 1 until currentList.size) {
                val item = currentList[i]
                if (item.depth > parent.depth) {
                    toRemove.add(item)
                } else {
                    break
                }
            }
            currentList.removeAll(toRemove)
            currentList[index] = parent.copy(isExpanded = false)
            _nodes.value = currentList
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val children = if (_isBrowsingSaf.value) {
                    loadSafNodes(_activeSafTree.value!!.uriString, parent.uriString, depth = parent.depth + 1)
                } else {
                    loadNodeContents(parent.file!!, depth = parent.depth + 1)
                }
                withContext(Dispatchers.Main) {
                    val updatedList = _nodes.value.toMutableList()
                    val freshIndex = updatedList.indexOfFirst { it.path == parent.path }
                    if (freshIndex != -1) {
                        updatedList[freshIndex] = parent.copy(isExpanded = true)
                        updatedList.addAll(freshIndex + 1, children)
                        _nodes.value = updatedList
                    }
                }
            }
        }
    }

    fun updateSortOption(option: FileManagerSortOption) {
        _sortOption.value = option
        refreshCurrentDir()
    }

    fun updateFilterOption(option: FileManagerFilterOption) {
        _filterOption.value = option
        refreshCurrentDir()
    }

    private fun refreshCurrentDir() {
        if (_isBrowsingSaf.value) {
            loadSafDirectory(_activeSafTree.value!!, _activeSafCurrentUri.value)
        } else {
            loadRootDirectory()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            refreshCurrentDir()
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                val results = if (_isBrowsingSaf.value) {
                    searchSafFilesRecursively(_activeSafTree.value!!.uriString, null, query)
                } else {
                    searchFilesRecursively(_currentRoot.value, query)
                }
                _nodes.value = results
            }
        }
    }

    private fun searchFilesRecursively(directory: File, query: String): List<FileManagerNode> {
        val results = mutableListOf<FileManagerNode>()
        val files = directory.listFiles() ?: return emptyList()
        for (file in files) {
            val ext = file.extension.lowercase()
            val matchesFilter = when (_filterOption.value) {
                FileManagerFilterOption.ALL -> true
                FileManagerFilterOption.VIDEOS -> file.isDirectory || ext in videoExtensions
                FileManagerFilterOption.AUDIO -> file.isDirectory || ext in audioExtensions
                FileManagerFilterOption.SUBTITLES -> file.isDirectory || ext in subtitleExtensions
                FileManagerFilterOption.IMAGES -> file.isDirectory || ext in imageExtensions
            }
            if (matchesFilter && file.name.contains(query, ignoreCase = true)) {
                val isDir = file.isDirectory
                val childCount = if (isDir) file.listFiles()?.size ?: 0 else 0
                results.add(
                    FileManagerNode(
                        file = file,
                        isDirectory = isDir,
                        isExpanded = false,
                        depth = 0,
                        isVideo = ext in videoExtensions,
                        isSubtitle = ext in subtitleExtensions,
                        isAudio = ext in audioExtensions,
                        childCount = childCount,
                        formattedSize = if (!isDir) formatSize(file.length()) else "$childCount items"
                    )
                )
            }
            if (file.isDirectory) {
                results.addAll(searchFilesRecursively(file, query))
            }
        }
        return sortNodes(results)
    }

    private fun searchSafFilesRecursively(treeUriString: String, parentUriString: String?, query: String): List<FileManagerNode> {
        val results = mutableListOf<FileManagerNode>()
        val rootDoc = DocumentFile.fromTreeUri(context, Uri.parse(treeUriString)) ?: return emptyList()
        val parentDoc = if (parentUriString == null) rootDoc else DocumentFile.fromSingleUri(context, Uri.parse(parentUriString))
        val files = parentDoc?.listFiles() ?: return emptyList()
        
        for (file in files) {
            val name = file.name ?: ""
            val ext = name.substringAfterLast('.', "").lowercase()
            val matchesFilter = when (_filterOption.value) {
                FileManagerFilterOption.ALL -> true
                FileManagerFilterOption.VIDEOS -> file.isDirectory || ext in videoExtensions
                FileManagerFilterOption.AUDIO -> file.isDirectory || ext in audioExtensions
                FileManagerFilterOption.SUBTITLES -> file.isDirectory || ext in subtitleExtensions
                FileManagerFilterOption.IMAGES -> file.isDirectory || ext in imageExtensions
            }
            if (matchesFilter && name.contains(query, ignoreCase = true)) {
                results.add(
                    FileManagerNode(
                        file = null,
                        uriString = file.uri.toString(),
                        isDirectory = file.isDirectory,
                        isExpanded = false,
                        depth = 0,
                        isVideo = ext in videoExtensions,
                        isSubtitle = ext in subtitleExtensions,
                        isAudio = ext in audioExtensions,
                        childCount = 0,
                        formattedSize = if (!file.isDirectory) formatSize(file.length()) else "Folder",
                        customName = name,
                        customExtension = ext,
                        customPath = file.uri.toString()
                    )
                )
            }
            if (file.isDirectory) {
                results.addAll(searchSafFilesRecursively(treeUriString, file.uri.toString(), query))
            }
        }
        return sortNodes(results)
    }

    // ── Create Folder / File ──────────────────────────────────────────────────
    fun createFolder(name: String) {
        if (name.isBlank()) return
        if (_isBrowsingSaf.value) {
            viewModelScope.launch(Dispatchers.IO) {
                val activeTree = _activeSafTree.value ?: return@launch
                val currentUri = _activeSafCurrentUri.value ?: activeTree.uriString
                val parentDoc = DocumentFile.fromSingleUri(context, Uri.parse(currentUri))
                parentDoc?.createDirectory(name)
                withContext(Dispatchers.Main) {
                    refreshCurrentDir()
                }
            }
        } else {
            val target = File(_currentRoot.value, name)
            if (!target.exists()) {
                val created = target.mkdirs()
                if (created) loadRootDirectory()
            }
        }
    }

    fun createEmptyFile(name: String) {
        if (name.isBlank()) return
        if (_isBrowsingSaf.value) {
            viewModelScope.launch(Dispatchers.IO) {
                val activeTree = _activeSafTree.value ?: return@launch
                val currentUri = _activeSafCurrentUri.value ?: activeTree.uriString
                val parentDoc = DocumentFile.fromSingleUri(context, Uri.parse(currentUri))
                val mime = when (name.substringAfterLast('.', "").lowercase()) {
                    "txt" -> "text/plain"
                    "srt" -> "text/plain"
                    "vtt" -> "text/plain"
                    "mp4" -> "video/mp4"
                    "mp3" -> "audio/mpeg"
                    else -> "application/octet-stream"
                }
                parentDoc?.createFile(mime, name)
                withContext(Dispatchers.Main) {
                    refreshCurrentDir()
                }
            }
        } else {
            val target = File(_currentRoot.value, name)
            if (!target.exists()) {
                try {
                    val created = target.createNewFile()
                    if (created) loadRootDirectory()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // ── Clipboard Operations (Copy / Cut / Paste) ─────────────────────────────
    fun copySelectedNodes() {
        val paths = if (_isMultiSelectMode.value) _selectedNodes.value.toList() else emptyList()
        if (paths.isNotEmpty()) {
            _clipboard.value = FileManagerClipboard(paths, isCut = false, isSaf = _isBrowsingSaf.value)
            _isMultiSelectMode.value = false
            _selectedNodes.value = emptySet()
        }
    }

    fun cutSelectedNodes() {
        val paths = if (_isMultiSelectMode.value) _selectedNodes.value.toList() else emptyList()
        if (paths.isNotEmpty()) {
            _clipboard.value = FileManagerClipboard(paths, isCut = true, isSaf = _isBrowsingSaf.value)
            _isMultiSelectMode.value = false
            _selectedNodes.value = emptySet()
        }
    }

    fun copySingleNode(node: FileManagerNode) {
        _clipboard.value = FileManagerClipboard(listOf(node.path), isCut = false, isSaf = _isBrowsingSaf.value)
    }

    fun cutSingleNode(node: FileManagerNode) {
        _clipboard.value = FileManagerClipboard(listOf(node.path), isCut = true, isSaf = _isBrowsingSaf.value)
    }

    fun pasteClipboard() {
        val clip = _clipboard.value ?: return
        val isDestSaf = _isBrowsingSaf.value
        
        // Resolve destination parameters
        val destLocalDir = if (!isDestSaf) _currentRoot.value else null
        val destSafUri = if (isDestSaf) {
            _activeSafCurrentUri.value ?: _activeSafTree.value?.uriString
        } else null

        viewModelScope.launch(Dispatchers.IO) {
            var mediaRefresh = false
            val resolver = context.contentResolver

            clip.paths.forEach { srcPath ->
                try {
                    val srcName: String
                    val isSrcDir: Boolean

                    if (clip.isSaf) {
                        val srcDoc = DocumentFile.fromSingleUri(context, Uri.parse(srcPath)) ?: return@forEach
                        srcName = srcDoc.name ?: "Unknown"
                        isSrcDir = srcDoc.isDirectory
                    } else {
                        val srcFile = File(srcPath)
                        srcName = srcFile.name
                        isSrcDir = srcFile.isDirectory
                    }

                    if (srcName.lowercase().let { it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".avi") }) {
                        mediaRefresh = true
                    }

                    if (isSrcDir) {
                        if (isDestSaf) {
                            val destFolderDoc = DocumentFile.fromSingleUri(context, Uri.parse(destSafUri!!))
                            destFolderDoc?.createDirectory(srcName)
                        } else {
                            val destFolderFile = File(destLocalDir!!, srcName)
                            destFolderFile.mkdirs()
                        }
                    } else {
                        // Dynamic Byte stream copying engine (works across SAF and standard IO)
                        val ins = if (clip.isSaf) {
                            resolver.openInputStream(Uri.parse(srcPath))
                        } else {
                            FileInputStream(File(srcPath))
                        }

                        val outs = if (isDestSaf) {
                            val destFolderDoc = DocumentFile.fromSingleUri(context, Uri.parse(destSafUri!!))
                            val mime = when (srcName.substringAfterLast('.', "").lowercase()) {
                                "mp4" -> "video/mp4"
                                "mp3" -> "audio/mpeg"
                                "txt" -> "text/plain"
                                "srt" -> "text/plain"
                                else -> "application/octet-stream"
                            }
                            val newFileDoc = destFolderDoc?.createFile(mime, srcName)
                            if (newFileDoc != null) resolver.openOutputStream(newFileDoc.uri) else null
                        } else {
                            val destFile = File(destLocalDir!!, srcName)
                            FileOutputStream(destFile)
                        }

                        if (ins != null && outs != null) {
                            ins.use { input ->
                                outs.use { output ->
                                    val buffer = ByteArray(8192)
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1) {
                                        output.write(buffer, 0, read)
                                    }
                                }
                            }
                        }
                    }

                    // Cut delete
                    if (clip.isCut) {
                        if (clip.isSaf) {
                            val srcDoc = DocumentFile.fromSingleUri(context, Uri.parse(srcPath))
                            srcDoc?.delete()
                        } else {
                            File(srcPath).deleteRecursively()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            withContext(Dispatchers.Main) {
                _clipboard.value = null
                refreshCurrentDir()
                if (mediaRefresh) {
                    videoRepository.refreshVideos()
                }
            }
        }
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    // ── Zip / Unzip Operations ────────────────────────────────────────────────
    fun zipNode(node: FileManagerNode) {
        if (_isBrowsingSaf.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val destZip = File(node.file!!.parentFile, "${node.name}.zip")
            ZipOutputStream(FileOutputStream(destZip)).use { zipOut ->
                addFileToZip(node.file, node.file.name, zipOut)
            }
            withContext(Dispatchers.Main) {
                loadRootDirectory()
            }
        }
    }

    private fun addFileToZip(fileToZip: File, fileName: String, zipOut: ZipOutputStream) {
        if (fileToZip.isHidden) return
        if (fileToZip.isDirectory) {
            val children = fileToZip.listFiles() ?: return
            for (child in children) {
                addFileToZip(child, "$fileName/${child.name}", zipOut)
            }
            return
        }
        FileInputStream(fileToZip).use { fis ->
            val zipEntry = ZipEntry(fileName)
            zipOut.putNextEntry(zipEntry)
            val bytes = ByteArray(4096)
            var length: Int
            while (fis.read(bytes).also { length = it } >= 0) {
                zipOut.write(bytes, 0, length)
            }
        }
    }

    fun unzipNode(node: FileManagerNode) {
        if (_isBrowsingSaf.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _extractionProgress.value = ExtractionProgress(fileName = "Scanning zip...", percentage = 0, progressFraction = 0f, isActive = true)
            val destDir = File(node.file!!.parentFile, node.name.substringBeforeLast("."))
            if (!destDir.exists()) destDir.mkdirs()

            try {
                var totalUncompressedSize = 0L
                ZipInputStream(FileInputStream(node.file)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        totalUncompressedSize += entry.size
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                var bytesExtracted = 0L
                ZipInputStream(FileInputStream(node.file)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val filePath = File(destDir, entry.name)
                        if (!entry.isDirectory) {
                            filePath.parentFile?.mkdirs()
                            FileOutputStream(filePath).use { fos ->
                                val buffer = ByteArray(4096)
                                var len: Int
                                while (zipIn.read(buffer).also { len = it } > 0) {
                                    fos.write(buffer, 0, len)
                                    bytesExtracted += len
                                    val percent = (bytesExtracted.toFloat() / totalUncompressedSize.coerceAtLeast(1L) * 100).toInt().coerceIn(0, 100)
                                    _extractionProgress.value = ExtractionProgress(
                                        fileName = entry.name,
                                        percentage = percent,
                                        progressFraction = bytesExtracted.toFloat() / totalUncompressedSize.coerceAtLeast(1L),
                                        isActive = true
                                    )
                                }
                            }
                        } else {
                            filePath.mkdirs()
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                _extractionProgress.value = _extractionProgress.value.copy(percentage = 100, progressFraction = 1f)
                delay(800)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _extractionProgress.value = ExtractionProgress(isActive = false)
            }

            withContext(Dispatchers.Main) {
                loadRootDirectory()
                videoRepository.refreshVideos()
            }
        }
    }

    fun unrarNode(node: FileManagerNode) {
        if (_isBrowsingSaf.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _extractionProgress.value = ExtractionProgress(fileName = "Scanning rar...", percentage = 0, progressFraction = 0f, isActive = true)
            val destDir = File(node.file!!.parentFile, node.name.substringBeforeLast("."))
            if (!destDir.exists()) destDir.mkdirs()

            try {
                val archive = com.github.junrar.Archive(node.file)
                try {
                    val headers = archive.fileHeaders
                    val totalUncompressedSize = headers.sumOf { it.fullUnpackSize }
                    var bytesExtracted = 0L

                    for (header in headers) {
                        val destFile = File(destDir, header.fileNameString)
                        if (header.isDirectory) {
                            destFile.mkdirs()
                        } else {
                            destFile.parentFile?.mkdirs()
                            FileOutputStream(destFile).use { fos ->
                                archive.extractFile(header, fos)
                            }
                            bytesExtracted += header.fullUnpackSize
                            val percent = (bytesExtracted.toFloat() / totalUncompressedSize.coerceAtLeast(1L) * 100).toInt().coerceIn(0, 100)
                            _extractionProgress.value = ExtractionProgress(
                                fileName = header.fileNameString,
                                percentage = percent,
                                progressFraction = bytesExtracted.toFloat() / totalUncompressedSize.coerceAtLeast(1L),
                                isActive = true
                            )
                        }
                    }
                    _extractionProgress.value = _extractionProgress.value.copy(percentage = 100, progressFraction = 1f)
                    delay(800)
                } finally {
                    archive.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _extractionProgress.value = ExtractionProgress(isActive = false)
            }

            withContext(Dispatchers.Main) {
                loadRootDirectory()
                videoRepository.refreshVideos()
            }
        }
    }

    // ── Favorites / Pinned Folders ────────────────────────────────────────────
    fun togglePinDirectory(node: FileManagerNode) {
        val current = _pinnedPaths.value.toMutableSet()
        if (current.contains(node.path)) {
            current.remove(node.path)
        } else {
            current.add(node.path)
        }
        _pinnedPaths.value = current
        savePinnedPaths(current)
    }

    private fun savePinnedPaths(paths: Set<String>) {
        val prefs = context.getSharedPreferences("filemanager_prefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("pinned_paths", paths).apply()
    }

    private fun loadPinnedPaths() {
        val prefs = context.getSharedPreferences("filemanager_prefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("pinned_paths", null)
        if (saved == null) {
            val defaults = mutableSetOf<String>()
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists()) defaults.add(downloadDir.absolutePath)
            val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val cameraDir = File(dcimDir, "Camera")
            if (cameraDir.exists()) defaults.add(cameraDir.absolutePath)
            else if (dcimDir.exists()) defaults.add(dcimDir.absolutePath)
            val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (moviesDir.exists()) defaults.add(moviesDir.absolutePath)
            val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
            if (musicDir.exists()) defaults.add(musicDir.absolutePath)

            _pinnedPaths.value = defaults
            savePinnedPaths(defaults)
        } else {
            _pinnedPaths.value = saved
        }
    }

    // ── Recycle Bin Logic ─────────────────────────────────────────────────────
    fun moveToTrash(node: FileManagerNode) {
        if (_isBrowsingSaf.value) return
        viewModelScope.launch(Dispatchers.IO) {
            val trashFile = File(trashFolder, "${System.currentTimeMillis()}_${node.name}")
            val moved = node.file!!.renameTo(trashFile)
            if (moved) {
                val trashItem = RecycleBinItem(
                    originalPath = node.path,
                    trashPath = trashFile.absolutePath,
                    deletedTime = System.currentTimeMillis()
                )
                withContext(Dispatchers.Main) {
                    val currentTrash = _trashItems.value.toMutableList()
                    currentTrash.add(trashItem)
                    _trashItems.value = currentTrash
                    
                    val current = _nodes.value.toMutableList()
                    current.removeAll { it.path == node.path || it.path.startsWith(node.path + File.separator) }
                    _nodes.value = current
                    
                    if (node.isVideo) {
                        videoRepository.refreshVideos()
                    }
                }
            }
        }
    }

    fun restoreFromTrash(item: RecycleBinItem) {
        viewModelScope.launch(Dispatchers.IO) {
            val trashFile = File(item.trashPath)
            val destFile = File(item.originalPath)
            destFile.parentFile?.mkdirs()
            val restored = trashFile.renameTo(destFile)
            if (restored) {
                withContext(Dispatchers.Main) {
                    val currentTrash = _trashItems.value.toMutableList()
                    currentTrash.remove(item)
                    _trashItems.value = currentTrash
                    loadRootDirectory()
                    
                    if (destFile.name.lowercase().let { it.endsWith(".mp4") || it.endsWith(".mkv") }) {
                        videoRepository.refreshVideos()
                    }
                }
            }
        }
    }

    // ── Standard Delete & Rename ──────────────────────────────────────────────
    fun deleteNode(node: FileManagerNode) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleted = if (_isBrowsingSaf.value) {
                val doc = DocumentFile.fromSingleUri(context, Uri.parse(node.uriString!!))
                doc?.delete() ?: false
            } else {
                node.file!!.deleteRecursively()
            }
            if (deleted) {
                withContext(Dispatchers.Main) {
                    val current = _nodes.value.toMutableList()
                    current.removeAll { it.path == node.path || it.path.startsWith(node.path + File.separator) }
                    _nodes.value = current
                    if (node.isVideo) {
                        videoRepository.refreshVideos()
                    }
                }
            }
        }
    }

    fun renameNode(node: FileManagerNode, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val renamed = if (_isBrowsingSaf.value) {
                val doc = DocumentFile.fromSingleUri(context, Uri.parse(node.uriString!!))
                doc?.renameTo(newName) != null
            } else {
                val parentDir = node.file!!.parentFile ?: return@launch
                val destFile = File(parentDir, newName)
                node.file.renameTo(destFile)
            }
            if (renamed) {
                withContext(Dispatchers.Main) {
                    refreshCurrentDir()
                    videoRepository.refreshVideos()
                }
            }
        }
    }

    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        _selectedNodes.value = emptySet()
    }

    fun toggleSelectNode(node: FileManagerNode) {
        val current = _selectedNodes.value.toMutableSet()
        if (current.contains(node.path)) {
            current.remove(node.path)
        } else {
            current.add(node.path)
        }
        _selectedNodes.value = current
    }

    fun deleteSelectedNodes() {
        val selectedPaths = _selectedNodes.value
        if (selectedPaths.isEmpty()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            var refreshNeeded = false
            selectedPaths.forEach { path ->
                if (_isBrowsingSaf.value) {
                    val doc = DocumentFile.fromSingleUri(context, Uri.parse(path))
                    doc?.delete()
                } else {
                    val file = File(path)
                    if (file.name.lowercase().let { it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".avi") }) {
                        refreshNeeded = true
                    }
                    file.deleteRecursively()
                }
            }
            
            withContext(Dispatchers.Main) {
                _selectedNodes.value = emptySet()
                _isMultiSelectMode.value = false
                refreshCurrentDir()
                if (refreshNeeded) {
                    videoRepository.refreshVideos()
                }
            }
        }
    }

    fun loadTextPreview(node: FileManagerNode, onLoaded: (String) -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = if (node.file != null) {
                    node.file.readText(Charsets.UTF_8).take(10000)
                } else {
                    context.contentResolver.openInputStream(Uri.parse(node.uriString!!)).use { ins ->
                        ins?.bufferedReader(Charsets.UTF_8)?.readText()?.take(10000) ?: ""
                    }
                }
                withContext(Dispatchers.Main) {
                    onLoaded(content)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError()
                }
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.2f MB", mb)
            kb >= 1.0 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }

    fun toggleShowHiddenFiles() {
        _showHiddenFiles.value = !_showHiddenFiles.value
        refreshCurrentDir()
    }

    fun calculateStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = Environment.getExternalStorageDirectory().path
                val stat = android.os.StatFs(path)
                val totalBytes = stat.blockSizeLong * stat.blockCountLong
                val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
                
                var videoBytes = 0L
                var audioBytes = 0L
                var imageBytes = 0L

                val contentResolver = context.contentResolver
                
                contentResolver.query(
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(android.provider.MediaStore.Video.Media.SIZE),
                    null, null, null
                )?.use { cursor ->
                    val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Video.Media.SIZE)
                    while (cursor.moveToNext()) {
                        videoBytes += cursor.getLong(sizeCol)
                    }
                }

                contentResolver.query(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(android.provider.MediaStore.Audio.Media.SIZE),
                    null, null, null
                )?.use { cursor ->
                    val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Audio.Media.SIZE)
                    while (cursor.moveToNext()) {
                        audioBytes += cursor.getLong(sizeCol)
                    }
                }

                contentResolver.query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(android.provider.MediaStore.Images.Media.SIZE),
                    null, null, null
                )?.use { cursor ->
                    val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.SIZE)
                    while (cursor.moveToNext()) {
                        imageBytes += cursor.getLong(sizeCol)
                    }
                }

                val usedBytes = totalBytes - freeBytes
                val otherBytes = (usedBytes - videoBytes - audioBytes - imageBytes).coerceAtLeast(0L)

                _storageStats.value = StorageStats(
                    totalBytes = totalBytes,
                    freeBytes = freeBytes,
                    videoBytes = videoBytes,
                    audioBytes = audioBytes,
                    imageBytes = imageBytes,
                    otherBytes = otherBytes
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadRecentFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<FileManagerNode>()
            val contentResolver = context.contentResolver
            val uri = android.provider.MediaStore.Files.getContentUri("external")
            val projection = arrayOf(
                android.provider.MediaStore.Files.FileColumns.DATA,
                android.provider.MediaStore.Files.FileColumns.SIZE,
                android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED
            )
            val selection = "${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE} != ${android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"
            val sortOrder = "${android.provider.MediaStore.Files.FileColumns.DATE_MODIFIED} DESC LIMIT 15"

            try {
                contentResolver.query(uri, projection, selection, null, sortOrder)?.use { cursor ->
                    val dataCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.SIZE)
                    while (cursor.moveToNext() && list.size < 10) {
                        val path = cursor.getString(dataCol) ?: continue
                        val file = File(path)
                        if (file.exists() && !file.isDirectory) {
                            val ext = file.extension.lowercase()
                            list.add(
                                FileManagerNode(
                                    file = file,
                                    isDirectory = false,
                                    isExpanded = false,
                                    depth = 0,
                                    isVideo = ext in videoExtensions,
                                    isSubtitle = ext in subtitleExtensions,
                                    isAudio = ext in audioExtensions,
                                    childCount = 0,
                                    formattedSize = formatSize(cursor.getLong(sizeCol))
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            _recentFiles.value = list
        }
    }
}
