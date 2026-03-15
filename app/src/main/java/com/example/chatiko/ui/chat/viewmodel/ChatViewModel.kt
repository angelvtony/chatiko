package com.example.chatiko.ui.chat.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.MessageDto
import com.example.chatiko.network.RetrofitClient
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.launch
import org.json.JSONObject


class ChatViewModel(
    private val context: Context?,
    private val userId: String?,
    private val otherUserId: String?
) : ViewModel() {

    private val _messages = mutableStateListOf<MessageDto>()
    val messages: List<MessageDto> = _messages

    private val _replyingTo = mutableStateOf<MessageDto?>(null)
    val replyingTo: State<MessageDto?> = _replyingTo

    private lateinit var socket: Socket

    init {
        connectSocket()
        fetchChatHistory()
    }

    private fun connectSocket() {
        try {
            val opts = IO.Options()
            opts.forceNew = true
            opts.reconnection = true
            socket = IO.socket("http://10.63.0.148:3000", opts) // Replace with your server IP

            socket.connect()

            socket.on(Socket.EVENT_CONNECT) {
                Log.d("ChatVM", "Socket connected: ${socket.id()}")
                // Join user room
                socket.emit("join", userId)
            }

            // Listen for incoming messages
            socket.on("receiveMessage") { args ->
                if (args.isNotEmpty()) {
                    val msgJson = args[0] as JSONObject
                    val msg = MessageDto(
                        id = msgJson.getString("_id"),
                        message = msgJson.getString("message"),
                        isMe = msgJson.getString("senderId") == userId,
                        senderId = msgJson.getString("senderId"),
                        receiverId = msgJson.getString("receiverId"),
                        reaction = msgJson.optString("reaction", null),
                        replyTo = null,
                        createdAt = ""

                    )
                    _messages.add(msg)
                }
            }
            socket.on("messageReaction") { args ->

                val json = args[0] as JSONObject
                val messageId = json.getString("_id")
                val reaction = json.optString("reaction")

                val index = _messages.indexOfFirst { it.id == messageId }

                if (index != -1) {
                    val oldMessage = _messages[index]
                    _messages[index] = oldMessage.copy(reaction = reaction)
                }
            }

            socket.on("messageDeleted") { args ->
                val data = args[0] as JSONObject
                val messageId = data.getString("messageId")

                _messages.removeAll { it.id == messageId }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchChatHistory() {
        viewModelScope.launch {
            try {
                val sharedPref = context?.getSharedPreferences(
                    "user_prefs",
                    Context.MODE_PRIVATE
                )

                val jwtToken =
                    sharedPref?.getString("jwt_token", null)
                val authHeader = "Bearer $jwtToken"
                val response = RetrofitClient.instance.getMessages(otherUserId, authHeader) // Make sure this is correct in your interface
                _messages.addAll(response.map {
                    MessageDto(
                        id = it.id,
                        senderId = it.senderId,
                        receiverId = it.receiverId,
                        message = it.message,
                        isMe = it.senderId == userId,
                        reaction = it.reaction,
                        replyTo = it.replyTo,
                        createdAt = it.createdAt


                    )
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(text: String) {

        val replyToId = _replyingTo.value?.id
        _replyingTo.value = null

        val json = JSONObject().apply {
            put("senderId", userId)
            put("receiverId", otherUserId)
            put("message", text)
            replyToId?.let { put("replyTo", it) }
        }

        socket.emit("sendMessage", json)
    }

    fun setReply(message: MessageDto) {
        _replyingTo.value = message
    }

    fun reactToMessage(messageId: String, reaction: String) {

        val json = JSONObject().apply {

            put("messageId", messageId)
            put("reaction", reaction)
            put("senderId", userId)
            put("receiverId", otherUserId)
        }

        socket.emit("reactMessage", json)
    }

    fun deleteMessage(messageId: String) {
        val data = mapOf(
            "messageId" to messageId,
            "senderId" to userId,
            "receiverId" to otherUserId
        )

        socket.emit("deleteMessage", JSONObject(data))
    }

    fun clearReply() {
        _replyingTo.value = null
    }

    override fun onCleared() {
        super.onCleared()
        socket.disconnect()
    }
}

class ChatViewModelFactory(
    private val context: Context?,
    private val userId: String?,
    private val otherUserId: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(context, userId, otherUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}