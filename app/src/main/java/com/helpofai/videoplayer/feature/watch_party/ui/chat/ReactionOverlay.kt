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
