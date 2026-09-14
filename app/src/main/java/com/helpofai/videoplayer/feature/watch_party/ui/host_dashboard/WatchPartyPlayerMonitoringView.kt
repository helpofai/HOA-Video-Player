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
package com.helpofai.videoplayer.feature.watch_party.ui.host_dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.helpofai.videoplayer.core.theme.HoaMiniSwitch
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySession

/**
 * A compact monitoring view of the Watch Party session for the PlayerScreen overlay.
 */
@Composable
fun WatchPartyPlayerMonitoringView(
    session: WatchPartySession,
    isSyncModeEnabled: Boolean = false,
    onToggleSyncMode: ((Boolean) -> Unit)? = null,
    isDragging: Boolean = false
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        // Subtle drag handle indicator at top
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 6.dp)
                .size(width = 26.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDragging) Color(0xFF7C5CE7) else Color.White.copy(alpha = 0.25f))
        )

        // Title and member count + sleek mini switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Watch Party (${session.devices.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            if (onToggleSyncMode != null) {
                HoaMiniSwitch(
                    checked = isSyncModeEnabled,
                    onCheckedChange = onToggleSyncMode,
                    activeColor = Color(0xFF10B981)
                )
            }
        }

        // Sync mode status badge
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSyncModeEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSyncModeEnabled) "Sync Mode: ON (Streaming)" else "Sync Mode: OFF (Not Streaming)",
                    color = if (isSyncModeEnabled) Color(0xFF00FFCC) else Color(0xFFF59E0B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        
        LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
            items(session.devices) { device ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = device.name,
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${device.status} • ${session.currentPositionMs / 1000}s",
                        color = Color.Cyan,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
