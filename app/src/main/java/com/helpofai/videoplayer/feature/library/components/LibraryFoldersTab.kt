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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.library.LibraryState
import com.helpofai.videoplayer.feature.library.ads.InlineItemAd
import com.helpofai.videoplayer.feature.library.ads.InlineRowAd
import com.helpofai.videoplayer.feature.filemanager.FileManagerScreen

@Composable
fun LibraryFoldersTab(
    state: LibraryState,
    selectedFolder: String?,
    isTablet: Boolean,
    onFolderClick: (String) -> Unit,
    onViewModeChange: (String) -> Unit,
    onVideoClick: (Video) -> Unit,
    onFavoriteClick: (Video) -> Unit,
    onRenameClick: (Video) -> Unit,
    onDeleteClick: (Video) -> Unit,
    onShareClick: (Video) -> Unit
) {
    val folders = state.videos.groupBy { java.io.File(it.path).parentFile?.name ?: "Internal Storage" }
    val folderViewMode = state.folderViewMode

    var isTreeViewActive by remember { mutableStateOf(false) }

    if (isTreeViewActive) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "File Tree Explorer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { isTreeViewActive = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Show Folders", fontWeight = FontWeight.Bold)
                }
            }
            FileManagerScreen(
                onVideoClick = onVideoClick,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    if (selectedFolder == null) {
        Spacer(modifier = Modifier.height(16.dp))
        val folderList = folders.toList()

        // ── View Mode Toggle & Tree Explorer Button ───────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { isTreeViewActive = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Advanced Tree Explorer", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "List",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (folderViewMode == "list") FontWeight.Bold else FontWeight.Normal,
                    color = if (folderViewMode == "list") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { onViewModeChange("list") }
                        .padding(horizontal = 8.dp)
                )
                Text(
                    " | ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    "Grid",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (folderViewMode == "grid") FontWeight.Bold else FontWeight.Normal,
                    color = if (folderViewMode == "grid") MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { onViewModeChange("grid") }
                        .padding(horizontal = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (folderViewMode == "list") {
            // ── List Mode: vertical stack, ad every 3 items ────
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                folderList.forEachIndexed { index, (folderName, videosInFolder) ->
                    LibraryFolderListItem(
                        folderName = folderName,
                        videos = videosInFolder,
                        onClick = { onFolderClick(folderName) }
                    )
                    // Ad after every 3 items
                    InlineItemAd(itemIndex = index, adInterval = 3, nativeEvery = 2, bannerEvery = 2)
                }
            }
        } else {
            // ── Grid Mode: 2×2 mosaic, ad every 3rd row (= 6 items) ────
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
                                LibraryFolderCard(
                                    folderName = folderName,
                                    videos = videosInFolder,
                                    onClick = { onFolderClick(folderName) }
                                )
                            }
                        }
                        val emptySlots = folderCols - rowFolders.size
                        for (i in 0 until emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    // Ad every 3rd row (= every 6 items in 2-col)
                    InlineRowAd(rowIndex = index, nativeEvery = 0, bannerEvery = 3, nativeOffset = 0)
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
                            LibraryCompactVideoListItem(
                                video = video,
                                onClick = { onVideoClick(video) },
                                onFavoriteClick = { onFavoriteClick(video) },
                                onRenameClick = { onRenameClick(video) },
                                onDeleteClick = { onDeleteClick(video) },
                                onShareClick = { onShareClick(video) }
                            )
                        }
                    }
                    val emptySlots = listCols - rowVideos.size
                    for (i in 0 until emptySlots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                // Folder detail: native every 5th row, banner every 10th
                InlineRowAd(rowIndex = index, nativeEvery = 5, bannerEvery = 10, nativeOffset = 4)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
