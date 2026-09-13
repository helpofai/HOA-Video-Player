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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color


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
import com.helpofai.videoplayer.core.theme.frostedGlass

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed

@Composable
fun LibraryFoldersTab(
    state: LibraryState,
    selectedFolder: String?,
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    onFolderClick: (String) -> Unit,
    onViewModeChange: (String) -> Unit,
    onVideoClick: (Video) -> Unit,
    onFavoriteClick: (Video) -> Unit,
    onRenameClick: (Video) -> Unit,
    onDeleteClick: (Video) -> Unit,
    onShareClick: (Video) -> Unit,
    onVaultClick: (Video) -> Unit,
    onNavigateToExplorer: () -> Unit = {},
    selectionState: com.helpofai.videoplayer.core.ui.MediaSelectionState? = null,
    onFolderLongClick: ((String, List<Video>) -> Unit)? = null,
    onVideoLongClick: ((Video) -> Unit)? = null
) {
    val folders = remember(state.videos) { state.videos.groupBy { it.resolvedFolderName } }
    val folderViewMode = state.folderViewMode

    var isTreeViewActive by remember { mutableStateOf(false) }

    if (isTreeViewActive) {
        Column(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToExplorer() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "File Tree Explorer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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
        val folderList = remember(folders) { folders.toList() }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "folders_header") {
                // ── View Mode Toggle & Tree Explorer Button ───────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .frostedGlass(cornerRadius = 12.dp, surfaceAlpha = 0.2f, surfaceColor = Color.Black)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
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
            }

            if (folderViewMode == "list") {
                itemsIndexed(folderList, key = { _, pair -> "folder_${pair.first}" }) { index, (folderName, videosInFolder) ->
                    val isFolderSelected = selectionState?.isFolderSelected(folderName) ?: false
                    val isSelectionMode = selectionState?.isSelectionMode ?: false

                    LibraryFolderListItem(
                        folderName = folderName,
                        videos = videosInFolder,
                        onClick = {
                            if (selectionState != null && selectionState.isSelectionMode) {
                                selectionState.toggleFolder(folderName, videosInFolder)
                            } else {
                                onFolderClick(folderName)
                            }
                        },
                        onLongClick = {
                            if (selectionState != null && selectionState.isSelectionMode) {
                                selectionState.toggleFolder(folderName, videosInFolder)
                            } else {
                                onFolderLongClick?.invoke(folderName, videosInFolder)
                            }
                        },
                        isSelected = isFolderSelected,
                        isSelectionMode = isSelectionMode
                    )
                    // Ad after every 3 items
                    InlineItemAd(itemIndex = index, adInterval = 3, nativeEvery = 2, bannerEvery = 2)
                }
            } else {
                val folderCols = if (isTablet) 4 else 2
                val chunkedFolders = folderList.chunked(folderCols)
                itemsIndexed(chunkedFolders, key = { index, _ -> "folder_row_$index" }) { index, rowFolders ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowFolders.forEach { (folderName, videosInFolder) ->
                            val isFolderSelected = selectionState?.isFolderSelected(folderName) ?: false
                            val isSelectionMode = selectionState?.isSelectionMode ?: false

                            Box(modifier = Modifier.weight(1f)) {
                                LibraryFolderCard(
                                    folderName = folderName,
                                    videos = videosInFolder,
                                    onClick = {
                                        if (selectionState != null && selectionState.isSelectionMode) {
                                            selectionState.toggleFolder(folderName, videosInFolder)
                                        } else {
                                            onFolderClick(folderName)
                                        }
                                    },
                                    onLongClick = {
                                        if (selectionState != null && selectionState.isSelectionMode) {
                                            selectionState.toggleFolder(folderName, videosInFolder)
                                        } else {
                                            onFolderLongClick?.invoke(folderName, videosInFolder)
                                        }
                                    },
                                    isSelected = isFolderSelected,
                                    isSelectionMode = isSelectionMode
                                )
                            }
                        }
                        val emptySlots = folderCols - rowFolders.size
                        for (i in 0 until emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    InlineRowAd(rowIndex = index, nativeEvery = 0, bannerEvery = 3, nativeOffset = 0)
                }
            }
        }
    } else {
        val videosInFolder = remember(folders, selectedFolder) { folders[selectedFolder] ?: emptyList() }
        val listCols = if (isTablet) 2 else 1
        val chunkedVideos = remember(videosInFolder, listCols) { videosInFolder.chunked(listCols) }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 80.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(chunkedVideos, key = { index, _ -> "folder_vid_row_$index" }) { index, rowVideos ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowVideos.forEach { video ->
                        val isVidSelected = selectionState?.isVideoSelected(video) ?: false
                        val isSelectionMode = selectionState?.isSelectionMode ?: false

                        Box(modifier = Modifier.weight(1f)) {
                            LibraryCompactVideoListItem(
                                video = video,
                                onClick = {
                                    if (selectionState != null && selectionState.isSelectionMode) {
                                        selectionState.toggleVideo(video)
                                    } else {
                                        onVideoClick(video)
                                    }
                                },
                                onLongClick = {
                                    if (selectionState != null && selectionState.isSelectionMode) {
                                        selectionState.toggleVideo(video)
                                    } else {
                                        onVideoLongClick?.invoke(video)
                                    }
                                },
                                isSelected = isVidSelected,
                                isSelectionMode = isSelectionMode,
                                onFavoriteClick = { onFavoriteClick(video) },
                                onRenameClick = { onRenameClick(video) },
                                onDeleteClick = { onDeleteClick(video) },
                                onShareClick = { onShareClick(video) },
                                onVaultClick = { onVaultClick(video) }
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
    }
}
