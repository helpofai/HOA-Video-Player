package com.helpofai.videoplayer.feature.workspace.transfers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.core.model.Video
import kotlinx.coroutines.launch

private val TcBgDeep    = Color(0xFF090B10)
private val TcAccentC   = Color(0xFF00CEC9)
private val TcAccentP   = Color(0xFF7C5CE7)
private val TcTextPri   = Color(0xFFECF0F1)
private val TcTextSub   = Color(0xFF8E9CB0)

private data class TransferPage(
    val title: String,
    val icon: ImageVector
)

private val TRANSFER_PAGES = listOf(
    TransferPage("Nearby Share", Icons.Default.Devices),
    TransferPage("Wi-Fi Portal", Icons.Default.Wifi),
    TransferPage("Network Drive", Icons.Default.Dns),
    TransferPage("Queue", Icons.Default.Speed),
    TransferPage("Controls", Icons.Default.Tune),
    TransferPage("Channels", Icons.Default.Hub),
)

@Composable
fun TransfersTab(
    isTablet: Boolean,
    videos: List<Video>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { TRANSFER_PAGES.size })
    val scope = rememberCoroutineScope()
    val activeQueue = remember { mutableStateListOf<ActiveTransfer>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TcBgDeep)
    ) {
        androidx.compose.material3.SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.White.copy(alpha = 0.03f),
            edgePadding = 8.dp,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            TRANSFER_PAGES.forEachIndexed { index, page ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    selectedContentColor = TcAccentC,
                    unselectedContentColor = TcTextSub,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = page.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = page.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> TransfersNearbyShareView(
                    videos = videos,
                    onStartSimulatedTransfer = { video ->
                        activeQueue.add(ActiveTransfer(video.title, 0f, "45s", false))
                        scope.launch { pagerState.animateScrollToPage(3) }
                    }
                )
                1 -> TransfersWifiPortalView()
                2 -> TransfersNetworkDriveView()
                3 -> TransfersQueueView(
                    activeQueue = activeQueue,
                    onPauseToggle = { idx ->
                        if (idx in activeQueue.indices) {
                            val item = activeQueue[idx]
                            activeQueue[idx] = item.copy(isPaused = !item.isPaused)
                        }
                    }
                )
                4 -> TransfersControlsView()
                5 -> TransfersChannelsView()
            }
        }
    }
}


data class ActiveTransfer(
    val name: String,
    val progress: Float,
    val eta: String,
    val isPaused: Boolean
)
