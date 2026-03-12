package com.example.chatiko.ui.chat.viewmodel

import ads_mobile_sdk.au
import ads_mobile_sdk.nu
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.chatiko.ui.chat.Message
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.RegistrationServices
import com.example.chatiko.network.RetrofitClient
import io.socket.client.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import io.socket.client.Socket


class ChatViewModel(
    private val context: Context?,
    private val userId: String?,
    private val otherUserId: String?
) : ViewModel() {

    private val _messages = mutableStateListOf<Message>()
    val messages: List<Message> = _messages

    private val _replyingTo = mutableStateOf<Message?>(null)
    val replyingTo: State<Message?> = _replyingTo

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
            socket = IO.socket("http://10.63.1.4:3000", opts) // Replace with your server IP

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
                    val msg = Message(
                        id = msgJson.getString("_id"),
                        text = msgJson.getString("message"),
                        isMe = msgJson.getString("senderId") == userId
                    )
                    _messages.add(msg)
                }
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
                    Message(
                        id = it.id,
                        text = it.message,
                        isMe = it.senderId == userId
                    )
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(text: String) {
        val replyToId = _replyingTo.value?.id
        val msg = Message(
            id = System.currentTimeMillis().toString(),
            text = text,
            isMe = true,
            replyTo = _replyingTo.value
        )
        _messages.add(msg)
        _replyingTo.value = null

        // Emit to server
        val json = JSONObject().apply {
            put("senderId", userId)
            put("receiverId", otherUserId)
            put("message", text)
            replyToId?.let { put("replyTo", it) }
        }
        socket.emit("sendMessage", json)
    }

    fun setReply(message: Message) {
        _replyingTo.value = message
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
            return ChatViewModel(context,userId, otherUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}