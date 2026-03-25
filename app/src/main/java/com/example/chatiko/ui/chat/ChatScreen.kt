package com.example.chatiko.ui.chat

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.draw.scale
import android.view.HapticFeedbackConstants
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chatiko.network.MessageDto
import com.example.chatiko.ui.chat.viewmodel.ChatViewModel
import com.example.chatiko.ui.chat.viewmodel.ChatViewModelFactory


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ChatScreen(
    navController: NavController?,
    userId: String?,
    otherUserId: String?,
    username: String?
) {
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(context, userId, otherUserId)
    )

    val messages = viewModel.messages
    val replyingTo by viewModel.replyingTo

    val incomingCallType by viewModel.incomingCallType
    val outgoingCallType by viewModel.outgoingCallType
    val callAccepted by viewModel.callAccepted
    val callDeclined by viewModel.callDeclined

    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val token = sharedPref.getString("jwt_token", null)

    val sharedRoomId = if (userId != null && otherUserId != null) {
        if (userId < otherUserId) "${userId}_$otherUserId" else "${otherUserId}_$userId"
    } else {
        "chatiko_random_room"
    }

    val activeCallType by viewModel.activeCallType

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (!granted) {
            Toast.makeText(context, "Camera & Mic permissions are required for calling", Toast.LENGTH_SHORT).show()
        }
    }

    // Initialize WebRTC and request permissions
    LaunchedEffect(Unit) {
        viewModel.initWebRtc(context)
        permissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )
    }

    if (activeCallType != null) {
        WebRtcCallScreen(
            viewModel = viewModel,
            callType = activeCallType!!,
            onEndCall = { viewModel.endCall() }
        )
    } else {
        // Only show Chat Screen if no active call

    Scaffold(
        topBar = { ChatTopBar(username, viewModel) },
        bottomBar = {
            MessageInputBar(
                replyingTo = replyingTo,
                onCancelReply = { viewModel.clearReply() },
                onSend = { text -> viewModel.sendMessage(text) }
            )
        }
    ) { paddingValues ->
        val reversedMessages = messages.reversed()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding()
                    )
                ),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(
                items = reversedMessages,
                key = { _, msg -> msg.id }
            ) { index, message ->
                val currentMsgDate = getDateString(message.createdAt)
                val nextMsgDate = reversedMessages.getOrNull(index + 1)?.let { getDateString(it.createdAt) }

                Column {
                    if (currentMsgDate != nextMsgDate) {
                        DateHeader(currentMsgDate)
                    }

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        SwipeToReplyWrapper(
                            onReply = { viewModel.setReply(message) }
                        ) {
                            MessageBubble(
                                message = message,
                                onDelete = { viewModel.deleteMessage(message.id) },
                                onReact = { reaction ->
                                    viewModel.reactToMessage(message.id, reaction = reaction)
                                },
                                onReply = { viewModel.setReply(message) }
                            )
                        }
                    } // End AnimatedVisibility
                } // End Column
            } // End itemsIndexed
        } // End LazyColumn
    } // End Scaffold
    } // End else branch

    // Show Dialogs
    if (incomingCallType != null) {
        IncomingCallDialog(
            callerName = username ?: "Unknown",
            callType = incomingCallType!!,
            onAccept = {
                val type = incomingCallType ?: "video"
                val isVideo = type == "video"
                viewModel.acceptWebRtcCall(isVideo)
                viewModel.sendCallSignaling("CALL_ACCEPTED", type)
                viewModel.resetCallStates()
            },
            onDecline = {
                viewModel.sendCallSignaling("CALL_DECLINED")
                viewModel.resetCallStates()
            }
        )
    }

    if (outgoingCallType != null) {
        OutgoingCallDialog(
            receiverName = username ?: "Unknown",
            callType = outgoingCallType!!,
            onCancel = {
                viewModel.sendCallSignaling("CALL_DECLINED")
                viewModel.resetCallStates()
            }
        )
    }
}

fun getDateString(createdAt: String?): String {
    if (createdAt.isNullOrEmpty()) return "Today"
    return try {
        val timeInMillis = createdAt.toLong()
        val date = Date(timeInMillis)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val today = cal.timeInMillis
        val yesterday = today - 86400000L

        when {
            timeInMillis >= today -> "Today"
            timeInMillis >= yesterday -> "Yesterday"
            else -> format.format(date)
        }
    } catch(e: Exception) {
        "Unknown Date"
    }
}

@Composable
fun DateHeader(dateStr: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = if (isSystemInDarkTheme()) Color(0xFF2A3942) else Color(0xFFF1F3FD),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 1.dp
        ) {
            Text(
                text = dateStr,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SwipeToReplyWrapper(
    onReply: () -> Unit,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = ""
    )

    val replyThreshold = 150f
    var triggered by remember { mutableStateOf(false) }
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        offsetX = 0f
                        triggered = false
                    },
                    onDragCancel = {
                        offsetX = 0f
                        triggered = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        if (dragAmount > 0 || offsetX > 0) {
                            val newOffset = (offsetX + dragAmount).coerceIn(0f, 250f)
                            offsetX = newOffset

                            if (offsetX > replyThreshold && !triggered) {
                                triggered = true
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onReply()
                            }
                        }
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 16.dp)
        ) {
            val progress = (animatedOffsetX / replyThreshold).coerceIn(0f, 1f)
            if (progress > 0.1f) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp).scale(progress.coerceAtLeast(0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Reply",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
        ) {
            content()
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(username: String?, viewModel: ChatViewModel) {
    TopAppBar(
        modifier = Modifier.shadow(elevation = 8.dp),
        title = {
            Column {
                Text(
                    text = username ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Chat Expires in 22h",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = {
                viewModel.sendCallSignaling("CALL_REQUEST", "video")
                viewModel.startWebRtcCall(true)
            }) {
                Icon(Icons.Filled.Videocam, contentDescription = "Video Call")
            }
            IconButton(onClick = {
                viewModel.sendCallSignaling("CALL_REQUEST", "audio")
                viewModel.startWebRtcCall(false)
            }) {
                Icon(Icons.Filled.Call, contentDescription = "Call")
            }
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MessageBubble(
    message: MessageDto,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit
) {

    var showMenu by remember { mutableStateOf(false) }
    var showReactions by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()

    val bubbleColor = if (message.isMe) {
        if (isDark) Color(0xFF005C4B) else Color(0xFFDCF8C6)
    } else {
        if (isDark) Color(0xFF202C33) else Color.White
    }

    val textColor = if (isDark) Color.White else Color.Black

    val alignment =
        if (message.isMe) Alignment.CenterEnd else Alignment.CenterStart

    val bubbleShape =
        if (message.isMe)
            RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
        else
            RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showReactions = true }
            ),
        contentAlignment = alignment
    ) {

        Column(
            horizontalAlignment =
                if (message.isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {

            message.replyTo?.let { repliedMsg ->
                val repliedName = if (repliedMsg.isMe) "You" else "Friend"
                Surface(
                    color = if (isDark) Color(0x33000000) else Color(0x11000000),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp).fillMaxWidth(0.9f)
                ) {
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                         Box(
                             modifier = Modifier
                                 .fillMaxHeight()
                                 .width(4.dp)
                                 .background(MaterialTheme.colorScheme.primary)
                         )
                         Column(modifier = Modifier.padding(8.dp)) {
                             Text(
                                 text = repliedName, 
                                 color = MaterialTheme.colorScheme.primary, 
                                 fontWeight = FontWeight.Bold, 
                                 fontSize = 12.sp
                             )
                             Text(
                                 text = repliedMsg.message ?: "",
                                 color = textColor.copy(alpha = 0.8f),
                                 fontSize = 12.sp,
                                 maxLines = 1,
                                 overflow = TextOverflow.Ellipsis
                             )
                         }
                    }
                }
            }

            Box {

                Surface(
                    color = bubbleColor,
                    shape = bubbleShape,
                    tonalElevation = 1.dp,
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = message.message ?: "",
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                        ) {
                            val timeText = try {
                                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                sdf.format(java.util.Date(message.createdAt?.toLong() ?: System.currentTimeMillis()))
                            } catch (e: Exception) {
                                "12:00"
                            }

                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                            if (message.isMe) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read",
                                    tint = Color(0xFF34B7F1),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                message.reaction?.takeIf { it.isNotEmpty() && it != "null" }?.let { reaction ->
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (isDark) Color(0xFF2A3942) else Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 8.dp)
                    ) {
                        Text(
                            text = reaction,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Floating reaction bar
        AnimatedVisibility(
            visible = showReactions,
            enter = fadeIn()
        ) {

            Row(
                modifier = Modifier
                    .offset(y = (-45).dp)
                    .background(if (isSystemInDarkTheme()) Color(0xFF2A3942) else Color.White, RoundedCornerShape(30.dp))
                    .shadow(8.dp, RoundedCornerShape(30.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val quickReactions =
                    listOf("👍", "❤️", "😂", "😮", "😢", "🙏")

                quickReactions.forEach { emoji ->

                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .padding(6.dp)
                            .clickable {
                                onReact(emoji)
                                showReactions = false
                            }
                    )
                }

                // PLUS BUTTON
                Text(
                    text = "➕",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(6.dp)
                        .clickable {
                            showEmojiSheet = true
                            showReactions = false
                        }
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {

            DropdownMenuItem(
                text = { Text("Reply") },
                onClick = {
                    onReply()
                    showMenu = false
                }
            )

            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
        }
    }

    // Emoji picker bottom sheet
    if (showEmojiSheet) {

        ModalBottomSheet(
            onDismissRequest = { showEmojiSheet = false }
        ) {

            EmojiPicker { emoji ->

                onReact(emoji)

                showEmojiSheet = false
            }
        }
    }
}

@Composable
fun MessageInputBar(
    replyingTo: MessageDto?,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }

    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column {

            replyingTo?.let { repliedMsg ->
                val repliedName = if (repliedMsg.isMe) "You" else "Friend"
                Surface(
                    color = if (isSystemInDarkTheme()) Color(0xFF1E2930) else Color(0xFFF0F2F5),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(4.dp)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = repliedName, 
                                    color = MaterialTheme.colorScheme.primary, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = repliedMsg.message ?: "", 
                                    fontSize = 12.sp, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSystemInDarkTheme()) Color.LightGray else Color.DarkGray
                                )
                            }
                            IconButton(onClick = onCancelReply) { 
                                Icon(Icons.Default.Close, contentDescription = "Cancel Reply") 
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(50),
                    maxLines = 4,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2A3942) else Color.White,
                        unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF2A3942) else Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (message.isNotBlank()) {
                            onSend(message)
                            message = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                }
            }
        }
    }
}