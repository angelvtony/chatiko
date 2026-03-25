package com.example.chatiko.ui.chat.viewmodel

import android.R.attr.text
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.MutableState
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
import com.example.chatiko.network.SocketManager
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import com.example.chatiko.ui.chat.WebRtcManager
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


class ChatViewModel(
    private val context: Context?,
    private val userId: String?,
    private val otherUserId: String?
) : ViewModel() {

    // --- Chat State ---
    private val _messages = mutableStateListOf<MessageDto>()
    val messages: List<MessageDto> = _messages

    private val _replyingTo = mutableStateOf<MessageDto?>(null)
    val replyingTo: MutableState<MessageDto?> = _replyingTo

    private val _incomingCallType = mutableStateOf<String?>(null)
    val incomingCallType: MutableState<String?> = _incomingCallType

    private val _outgoingCallType = mutableStateOf<String?>(null)
    val outgoingCallType: MutableState<String?> = _outgoingCallType

    private val _callAccepted = mutableStateOf<String?>(null)
    val callAccepted: MutableState<String?> = _callAccepted

    private val _callDeclined = mutableStateOf(false)
    val callDeclined: MutableState<Boolean> = _callDeclined

    private val _activeCallType = mutableStateOf<String?>(null)
    val activeCallType: MutableState<String?> = _activeCallType

    val remoteVideoTrack = mutableStateOf<VideoTrack?>(null)

    var webRtcManager: WebRtcManager? = null
    private var socket: Socket? = SocketManager.getSocket()
    
    private var pendingOffer: String? = null
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var isRemoteReady = false
    private var cachedReceiverPublicKey: PublicKey? = null

    // --- Initialization ---
    init {
        CryptoManager.generateKeyPair()
        uploadPublicKey()
        connectSocket()
        fetchChatHistory()
    }

    // --- Socket Setup ---
    private fun connectSocket() {
        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
            }
            socket = IO.socket("http://10.63.1.4:3000", opts)
            socket?.connect()
            socket?.on(Socket.EVENT_CONNECT) { socket?.emit("join", userId) }

            socket?.on("receiveMessage") { args ->
                val msgJson = args[0] as JSONObject
                val senderIdFromMsg = msgJson.getString("senderId")
                val serverMessageId = msgJson.getString("_id")

                if (senderIdFromMsg == userId) {
                    val index = _messages.indexOfFirst { it.id.startsWith("temp-") }
                    if (index != -1) _messages[index] = _messages[index].copy(id = serverMessageId)
                    return@on
                }

                try {
                    val payloadObj = JSONObject(msgJson.getString("message"))
                    val payload = CryptoManager.EncryptedPayload(
                        encryptedKey = payloadObj.getString("encryptedKey"),
                        encryptedMessage = payloadObj.getString("encryptedMessage"),
                        iv = payloadObj.getString("iv")
                    )
                    val messageText = CryptoManager.decryptMessageHybrid(payload)
                    viewModelScope.launch {
                        handleIncomingMessage(messageText, msgJson, serverMessageId, senderIdFromMsg)
                    }
                } catch (e: Exception) {
                    Log.e("CHAT", "Failed to decrypt message", e)
                }
            }

            // Sync interactions from remote devices
            socket?.on("reactMessage") { args ->
                try {
                    val msgJson = args[0] as JSONObject
                    val messageId = msgJson.getString("messageId")
                    val reaction = msgJson.getString("reaction")
                    viewModelScope.launch {
                        val index = _messages.indexOfFirst { it.id == messageId }
                        if (index != -1) {
                            _messages[index] = _messages[index].copy(reaction = reaction)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            socket?.on("deleteMessage") { args ->
                try {
                    val msgJson = args[0] as JSONObject
                    val messageId = msgJson.getString("messageId")
                    viewModelScope.launch {
                        _messages.removeAll { it.id == messageId }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun handleIncomingMessage(messageText: String, msgJson: JSONObject, serverMessageId: String, senderId: String) {
        when {
            messageText.startsWith("CALL_REQUEST:") -> _incomingCallType.value = messageText.removePrefix("CALL_REQUEST:").trim()
            messageText.startsWith("CALL_ACCEPTED:") -> _callAccepted.value = messageText.removePrefix("CALL_ACCEPTED:").trim()
            messageText.startsWith("CALL_DECLINED:") -> {
                _callDeclined.value = true
                _outgoingCallType.value = null
                endCall()
            }
            messageText.startsWith("WEBRTC_") -> handleWebRtcMessage(messageText)
            else -> {
                val replyToObj = msgJson.optJSONObject("replyTo")
                val replyToDto = replyToObj?.let {
                    MessageDto(
                        id = it.optString("id", ""),
                        message = it.optString("message", ""),
                        senderId = it.optString("senderId", ""),
                        receiverId = "",
                        isMe = it.optString("senderId") == userId,
                        reaction = null,
                        replyTo = null,
                        createdAt = ""
                    )
                }

                _messages.add(
                    MessageDto(
                        id = serverMessageId,
                        message = messageText,
                        isMe = false,
                        senderId = senderId,
                        receiverId = msgJson.getString("receiverId"),
                        reaction = msgJson.optString("reaction", null),
                        replyTo = replyToDto,
                        createdAt = msgJson.optString("createdAt", "")
                    )
                )
            }
        }
    }

    private fun handleWebRtcMessage(message: String) {
        when {
            message.startsWith("WEBRTC_OFFER:") -> {
                val sdp = message.removePrefix("WEBRTC_OFFER:")
                if (_activeCallType.value != null) {
                    webRtcManager?.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, sdp)) {
                        viewModelScope.launch {
                            isRemoteReady = true
                            webRtcManager?.createAnswer()
                            pendingIceCandidates.forEach { webRtcManager?.addIceCandidate(it) }
                            pendingIceCandidates.clear()
                        }
                    }
                } else {
                    pendingOffer = sdp
                }
            }
            message.startsWith("WEBRTC_ANSWER:") -> {
                val sdp = message.removePrefix("WEBRTC_ANSWER:")
                webRtcManager?.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp)) {
                    viewModelScope.launch {
                        isRemoteReady = true
                        pendingIceCandidates.forEach { webRtcManager?.addIceCandidate(it) }
                        pendingIceCandidates.clear()
                    }
                }
            }
            message.startsWith("WEBRTC_ICE:") -> {
                val parts = message.removePrefix("WEBRTC_ICE:").split("|")
                if (parts.size == 3) {
                    val candidate = IceCandidate(parts[0], parts[1].toInt(), parts[2])
                    if (isRemoteReady) {
                        webRtcManager?.addIceCandidate(candidate)
                    } else {
                        pendingIceCandidates.add(candidate)
                    }
                }
            }
            message.startsWith("WEBRTC_END:") -> endCall()
        }
    }

    // --- Public Key Upload ---
    private fun uploadPublicKey() {
        viewModelScope.launch {
            try {
                val sharedPref = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val jwtToken = sharedPref?.getString("jwt_token", null)
                val authHeader = "Bearer $jwtToken"
                val key = CryptoManager.getPublicKeyString()
                RetrofitClient.instance.uploadPublicKey(PublicKeyRequest(key), authHeader)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- Chat History ---
    private fun fetchChatHistory() {
        viewModelScope.launch {
            try {
                val sharedPref = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val jwtToken = sharedPref?.getString("jwt_token", null)
                val authHeader = "Bearer $jwtToken"
                val response = RetrofitClient.instance.getMessages(otherUserId, authHeader)
                _messages.addAll(response.map {

                    val decrypted = decryptMessage(it.message ?: "")

                    if (decrypted.startsWith("CALL_REQUEST:") ||
                        decrypted.startsWith("CALL_ACCEPTED:") ||
                        decrypted.startsWith("CALL_DECLINED:") ||
                        decrypted.startsWith("WEBRTC_")
                    ) {
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

    fun decryptMessage(encrypted: String): String {
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, CryptoManager.getPrivateKey())
        val decoded = Base64.decode(encrypted, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decoded)
        return String(decryptedBytes)
    }

    // --- Message Sending ---
    fun sendMessage(text: String) = viewModelScope.launch {
        val receiverPublicKey = getReceiverPublicKey() ?: return@launch
        val payload = CryptoManager.encryptMessageHybrid(text, receiverPublicKey)

        val currentReplyTo = _replyingTo.value

        val json = JSONObject().apply {
            put("senderId", userId)
            put("receiverId", otherUserId)
            put("message", JSONObject().apply {
                put("encryptedKey", payload.encryptedKey)
                put("encryptedMessage", payload.encryptedMessage)
                put("iv", payload.iv)
            }.toString())

            currentReplyTo?.let { reply ->
                put("replyTo", JSONObject().apply {
                    put("id", reply.id)
                    put("message", reply.message)
                    put("senderId", reply.senderId)
                })
            }
        }

        val tempId = "temp-${System.currentTimeMillis()}"
        val msg = MessageDto(
            id = tempId,
            message = text,
            isMe = true,
            senderId = userId!!,
            receiverId = otherUserId!!,
            reaction = null,
            replyTo = currentReplyTo,
            createdAt = System.currentTimeMillis().toString()
        )
        _messages.add(msg)
        _replyingTo.value = null // Clear reply state after sending

        socket?.emit("sendMessage", json)
    }

    // --- Call & WebRTC Signaling ---
    fun sendCallSignaling(command: String, type: String = "") = viewModelScope.launch {
        val msg = if (type.isNotEmpty()) "$command:$type" else command
        sendHybridSignaling(msg)
        if (command == "CALL_REQUEST") _outgoingCallType.value = type
        else if (command == "CALL_ACCEPTED" || command == "CALL_DECLINED") _incomingCallType.value = null
    }

    private fun sendHybridSignaling(msg: String) = viewModelScope.launch {
        val receiverPublicKey = getReceiverPublicKey() ?: return@launch
        val payload = CryptoManager.encryptMessageHybrid(msg, receiverPublicKey)

        val json = JSONObject().apply {
            put("senderId", userId)
            put("receiverId", otherUserId)
            put("message", JSONObject().apply {
                put("encryptedKey", payload.encryptedKey)
                put("encryptedMessage", payload.encryptedMessage)
                put("iv", payload.iv)
            }.toString())
        }

        socket?.emit("sendMessage", json)
    }

    // --- WebRTC Management ---
    fun initWebRtc(context: Context) {
        if (webRtcManager == null) {
            webRtcManager = WebRtcManager(context, object : WebRtcManager.WebRtcListener {
                override fun onIceCandidate(candidate: IceCandidate) {
                    sendWebRtcSignaling("WEBRTC_ICE:${candidate.sdpMid}|${candidate.sdpMLineIndex}|${candidate.sdp}")
                }
                override fun onOfferReady(sessionDescription: SessionDescription) {
                    sendWebRtcSignaling("WEBRTC_OFFER:${sessionDescription.description}")
                }
                override fun onAnswerReady(sessionDescription: SessionDescription) {
                    sendWebRtcSignaling("WEBRTC_ANSWER:${sessionDescription.description}")
                }
                override fun onRemoteTrackAdded(track: VideoTrack) {
                    remoteVideoTrack.value = track
                }
            })
        }
    }

    private fun sendWebRtcSignaling(msg: String) = viewModelScope.launch {
        val receiverPublicKey = getReceiverPublicKey() ?: return@launch
        val payload = CryptoManager.encryptMessageHybrid(msg, receiverPublicKey)

        val json = JSONObject().apply {
            put("senderId", userId)
            put("receiverId", otherUserId)
            put("message", JSONObject().apply {
                put("encryptedKey", payload.encryptedKey)
                put("encryptedMessage", payload.encryptedMessage)
                put("iv", payload.iv)
            }.toString())
        }

        socket?.emit("sendMessage", json)
    }

    fun startWebRtcCall(isVideo: Boolean) {
        _activeCallType.value = if (isVideo) "video" else "audio"
        webRtcManager?.prepareMedia(isVideo)
        webRtcManager?.createPeerConnection(isVideo)
        webRtcManager?.createOffer()
    }

    fun acceptWebRtcCall(isVideo: Boolean) {
        _activeCallType.value = if (isVideo) "video" else "audio"
        webRtcManager?.prepareMedia(isVideo)
        webRtcManager?.createPeerConnection(isVideo)
        
        pendingOffer?.let { offer ->
            webRtcManager?.setRemoteDescription(SessionDescription(SessionDescription.Type.OFFER, offer)) {
                viewModelScope.launch {
                    isRemoteReady = true
                    webRtcManager?.createAnswer()
                    pendingIceCandidates.forEach { webRtcManager?.addIceCandidate(it) }
                    pendingIceCandidates.clear()
                }
            }
            pendingOffer = null
        }
    }

    fun endCall() {
        sendHybridSignaling("WEBRTC_END:")
        webRtcManager?.destroy()
        webRtcManager = null
        _activeCallType.value = null
        remoteVideoTrack.value = null
        pendingOffer = null
        pendingIceCandidates.clear()
        isRemoteReady = false
        resetCallStates()
        context?.let { initWebRtc(it) }
    }

    // --- Utilities ---
    fun setReply(message: MessageDto) { _replyingTo.value = message }

    fun resetCallStates() {
        _incomingCallType.value = null
        _outgoingCallType.value = null
        _callAccepted.value = null
        _callDeclined.value = false
    }

    private suspend fun getReceiverPublicKey(): PublicKey? {
        if (cachedReceiverPublicKey != null) return cachedReceiverPublicKey
        return try {
            val sharedPref = context?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val jwtToken = sharedPref?.getString("jwt_token", null) ?: return null
            val authHeader = "Bearer $jwtToken"
            val response = RetrofitClient.instance.getUserPublicKey(otherUserId, authHeader)
            val keyString = response.publicKey ?: return null
            val decoded = Base64.decode(keyString, Base64.DEFAULT)
            val factory = KeyFactory.getInstance("RSA")
            val spec = X509EncodedKeySpec(decoded)
            cachedReceiverPublicKey = factory.generatePublic(spec)
            cachedReceiverPublicKey
        } catch (e: Exception) {
            Log.e("DEBUG_KEY", "Failed to get receiver key: ${e.message}")
            null
        }
    }

    fun reactToMessage(messageId: String, reaction: String) {
        val index = _messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            _messages[index] = _messages[index].copy(reaction = reaction)
        }
        val json = JSONObject().apply {
            put("messageId", messageId)
            put("reaction", reaction)
            put("senderId", userId)
            put("receiverId", otherUserId)
        }
        socket?.emit("reactMessage", json)
    }

    fun deleteMessage(messageId: String) {
        _messages.removeAll { it.id == messageId }
        val data = mapOf(
            "messageId" to messageId,
            "senderId" to userId,
            "receiverId" to otherUserId
        )
        socket?.emit("deleteMessage", JSONObject(data))
    }

    fun clearReply() { _replyingTo.value = null }

    override fun onCleared() {
        super.onCleared()
        socket?.disconnect()
    }
}

// --- Factory ---
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