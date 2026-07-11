package com.helpofai.videoplayer.feature.watch_party.ui.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.helpofai.videoplayer.feature.watch_party.domain.models.ChatMessage
import com.helpofai.videoplayer.feature.watch_party.session.WatchPartySessionManager

@Composable
fun ChatOverlay(
    sessionManager: WatchPartySessionManager = WatchPartySessionManager.getInstance()
) {
    val messages by sessionManager.chatMessages.collectAsState()
    var text by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(messages) { msg ->
                Text("${msg.senderName}: ${msg.text}")
            }
        }
        Row {
            TextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f))
            Button(onClick = {
                if (text.isNotBlank()) {
                    sessionManager.sendMessage(text)
                    text = ""
                }
            }) {
                Text("Send")
            }
        }
    }
}
