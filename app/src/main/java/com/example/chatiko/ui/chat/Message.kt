package com.example.chatiko.ui.chat

import java.util.UUID

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isMe: Boolean,
    val reaction: String? = null,
    val replyTo: Message? = null
)
