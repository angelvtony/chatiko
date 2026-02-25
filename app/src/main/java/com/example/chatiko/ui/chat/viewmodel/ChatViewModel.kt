package com.example.chatiko.ui.chat.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chatiko.ui.chat.Message
import androidx.compose.runtime.State

class ChatViewModel : ViewModel() {

    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> = _messages

    private val _replyingTo = mutableStateOf<Message?>(null)
    val replyingTo: State<Message?> = _replyingTo

    init {
        _messages.addAll(
            listOf(
                Message(text = "Hi Arathi 👋", isMe = true),
                Message(text = "Hey! How are you?", isMe = false),
                Message(text = "I'm good. What about you?", isMe = true),
                Message(text = "Doing great! 😊", isMe = false),
            )
        )
    }

    fun sendMessage(text: String) {
        _messages.add(
            Message(
                text = text,
                isMe = true,
                replyTo = _replyingTo.value
            )
        )
        _replyingTo.value = null
    }

    fun deleteMessage(message: Message) {
        _messages.remove(message)
    }

    fun addReaction(message: Message, emoji: String) {
        val index = _messages.indexOf(message)
        if (index != -1) {
            _messages[index] = _messages[index].copy(reaction = emoji)
        }
    }

    fun setReply(message: Message) {
        _replyingTo.value = message
    }

    fun clearReply() {
        _replyingTo.value = null
    }
}