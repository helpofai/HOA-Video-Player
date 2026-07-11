package com.helpofai.videoplayer.feature.watch_party.domain.models

data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long
)

data class Reaction(
    val id: String,
    val senderId: String,
    val emoji: String,
    val timestamp: Long
)
