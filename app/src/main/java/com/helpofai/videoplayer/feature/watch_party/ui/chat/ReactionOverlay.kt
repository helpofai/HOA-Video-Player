package com.helpofai.videoplayer.feature.watch_party.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager

@Composable
fun ReactionOverlay(
    sessionManager: WatchPartySessionManager = WatchPartySessionManager.getInstance()
) {
    val session by sessionManager.activeSession.collectAsState()
    val localDev = remember(session) {
        val devId = sessionManager.getLocalDeviceId()
        session?.devices?.firstOrNull { it.id == devId }
    }
    val canReact = if (!sessionManager.isClientMode) true
        else (localDev?.hasReactionPermission ?: (session?.allowReactions ?: true))

    if (!canReact) return

    val reactions = listOf("❤️", "😂", "😮", "👍", "🔥")
    
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        reactions.forEach { emoji ->
            IconButton(onClick = {
                sessionManager.sendReaction(emoji)
            }) {
                Text(emoji, style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
