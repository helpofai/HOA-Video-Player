package com.helpofai.videoplayer.feature.workspace.transfers

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.helpofai.videoplayer.core.model.Video
import com.helpofai.videoplayer.feature.nearbyshare.NearbyShareScreen

@Composable
fun TransfersNearbyShareView(
    videos: List<Video>,
    onStartSimulatedTransfer: (Video) -> Unit,
    modifier: Modifier = Modifier
) {
    // Delegate to the NearbyShareScreen feature
    NearbyShareScreen(
        videos = videos,
        modifier = modifier
    )
}
