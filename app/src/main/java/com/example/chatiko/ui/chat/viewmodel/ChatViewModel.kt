package com.example.chatiko.ui.chat.viewmodel

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.chatiko.network.CryptoManager
import com.example.chatiko.network.MessageDto
import com.example.chatiko.network.PublicKeyRequest
import com.example.chatiko.network.RetrofitClient
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


class ChatViewModel(
    private val context: Context?,
    private val userId: String?,
    private val otherUserId: String?
) : ViewModel() {

    private val _messages = mutableStateListOf<MessageDto>()
    val messages: List<MessageDto> = _messages

    private val _replyingTo = mutableStateOf<MessageDto?>(null)
    val replyingTo: State<MessageDto?> = _replyingTo

    private val _incomingCallType = mutableStateOf<String?>(null)
    val incomingCallType: State<String?> = _incomingCallType

    private val _outgoingCallType = mutableStateOf<String?>(null)
    val outgoingCallType: State<String?> = _outgoingCallType

    private val _callAccepted = mutableStateOf<String?>(null)
    val callAccepted: State<String?> = _callAccepted

    private val _callDeclined = mutableStateOf(false)
    val callDeclined: State<Boolean> = _callDeclined

    private lateinit var socket: Socket

    init {
        CryptoManager.generateKeyPair()
        uploadPublicKey()
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
                val msgJson = args[0] as JSONObject
                val senderIdFromMsg = msgJson.getString("senderId")
                val serverMessageId = msgJson.getString("_id")

                if (senderIdFromMsg == userId) {
                    // Update the temp message ID to server-generated ID
                    val index = _messages.indexOfFirst { it.id.startsWith("temp-") }
                    if (index != -1) {
                        val oldMsg = _messages[index]
                        _messages[index] = oldMsg.copy(id = serverMessageId)
                    }
                    return@on
                }

                // Message from other user — decrypt it
                val encryptedMessage = msgJson.getString("message")
                val messageText: String = try {
                    decryptMessage(encryptedMessage)
                } catch (e: Exception) {
                    Log.e("CHAT", "Decryption failed", e)
                    encryptedMessage
                }

                if (messageText.startsWith("CALL_REQUEST:")) {
                    val type = messageText.removePrefix("CALL_REQUEST:")
                    _incomingCallType.value = type
                    return@on
                }

                if (messageText.startsWith("CALL_ACCEPTED:")) {
                    val type = messageText.removePrefix("CALL_ACCEPTED:")
                    _callAccepted.value = type
                    return@on
                }

                if (messageText.startsWith("CALL_DECLINED:")) {
                    _callDeclined.value = true
                    // Reset outgoing call state since it was declined
                    _outgoingCallType.value = null
                    return@on
                }

                val msg = MessageDto(
                    id = serverMessageId,
                    message = messageText,
                    isMe = false,
                    senderId = senderIdFromMsg,
                    receiverId = msgJson.getString("receiverId"),
                    reaction = msgJson.optString("reaction", null),
                    replyTo = null,
                    createdAt = msgJson.optString("createdAt", "")
                )

                _messages.add(msg)
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

    private fun uploadPublicKey() {

        viewModelScope.launch {

            try{

                val sharedPref = context?.getSharedPreferences(
                    "user_prefs",
                    Context.MODE_PRIVATE
                )

                val jwtToken = sharedPref?.getString("jwt_token",null)

                val authHeader = "Bearer $jwtToken"

                val key = CryptoManager.getPublicKeyString()

                RetrofitClient.instance.uploadPublicKey(
                    PublicKeyRequest(key),
                    authHeader
                )

            }catch(e:Exception){
                e.printStackTrace()
            }

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

                    val decrypted =
                        decryptMessage(it.message ?: "")

                    if (decrypted.startsWith("CALL_REQUEST:") || 
                        decrypted.startsWith("CALL_ACCEPTED:") || 
                        decrypted.startsWith("CALL_DECLINED:")) {
                        return@map null
                    }

                    MessageDto(
                        id = it.id,
                        senderId = it.senderId,
                        receiverId = it.receiverId,
                        message = decrypted,
                        isMe = it.senderId == userId,
                        reaction = it.reaction,
                        replyTo = it.replyTo,
                        createdAt = it.createdAt
                    )
                }.filterNotNull())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun encryptMessage(message: String, publicKey: PublicKey): String {

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val encryptedBytes = cipher.doFinal(message.toByteArray())

        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            val receiverPublicKey = getReceiverPublicKey()
            if (receiverPublicKey == null) {
                Log.e("CHAT", "Receiver public key missing")
                return@launch
            }

            val encrypted = encryptMessage(text, receiverPublicKey)

            val json = JSONObject().apply {
                put("senderId", userId)
                put("receiverId", otherUserId)
                put("message", encrypted)
            }

            // Add to local messages immediately
            val tempId = "temp-${System.currentTimeMillis()}"
            val msg = MessageDto(
                id = tempId,
                message = text,  // plain text for sender
                isMe = true,
                senderId = userId!!,
                receiverId = otherUserId!!,
                reaction = null,
                replyTo = null,
                createdAt = System.currentTimeMillis().toString()
            )
            _messages.add(msg)

            // Send to server
            socket.emit("sendMessage", json)
        }
    }

    fun sendCallSignaling(command: String, type: String = "") {
        viewModelScope.launch {
            val receiverPublicKey = getReceiverPublicKey() ?: return@launch
            val signalMsg = if (type.isNotEmpty()) "$command:$type" else command
            val encrypted = encryptMessage(signalMsg, receiverPublicKey)
            val json = JSONObject().apply {
                put("senderId", userId)
                put("receiverId", otherUserId)
                put("message", encrypted)
            }
            socket.emit("sendMessage", json)
            
            if (command == "CALL_REQUEST") {
                _outgoingCallType.value = type
            } else if (command == "CALL_ACCEPTED" || command == "CALL_DECLINED") {
                _incomingCallType.value = null
            }
        }
    }
    
    fun resetCallStates() {
        _incomingCallType.value = null
        _outgoingCallType.value = null
        _callAccepted.value = null
        _callDeclined.value = false
    }

    fun setReply(message: MessageDto) {
        _replyingTo.value = message
    }

    private suspend fun getReceiverPublicKey(): PublicKey? {
        return try {
            val sharedPref = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val jwtToken = sharedPref?.getString("jwt_token", null) ?: return null
            val authHeader = "Bearer $jwtToken"
            val response = RetrofitClient.instance.getUserPublicKey(otherUserId, authHeader)

            val keyString = response.publicKey
            Log.d("DEBUG_KEY", "Receiver public key (raw): $keyString")

            if (keyString == null) {
                Log.w("DEBUG_KEY", "Receiver public key is null!")
                return null
            }

            val decoded = Base64.decode(keyString, Base64.DEFAULT)
            val factory = KeyFactory.getInstance("RSA")
            val spec = X509EncodedKeySpec(decoded)
            val publicKey = factory.generatePublic(spec)
            Log.d("DEBUG_KEY", "Receiver public key successfully decoded: $publicKey")
            publicKey
        } catch (e: Exception) {
            Log.e("DEBUG_KEY", "Failed to get receiver key: ${e.message}")
            null
        }
    }

    fun decryptMessage(encrypted: String): String {

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")

        cipher.init(Cipher.DECRYPT_MODE, CryptoManager.getPrivateKey())

        val decoded = Base64.decode(encrypted, Base64.DEFAULT)

        val decryptedBytes = cipher.doFinal(decoded)

        return String(decryptedBytes)
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