package com.example.chatiko.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chatiko.network.MessageDto
import com.example.chatiko.ui.chat.viewmodel.ChatViewModel
import com.example.chatiko.ui.chat.viewmodel.ChatViewModelFactory


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

    Scaffold(
        topBar = { ChatTopBar(username) },
        bottomBar = {
            MessageInputBar(
                replyingTo = replyingTo,
                onCancelReply = { viewModel.clearReply() },
                onSend = { text -> viewModel.sendMessage(text) }
            )
        }
    ) { paddingValues ->
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
            items(
                items = messages.reversed(),
                key = { it.id }
            ) { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically()
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(username: String?) {
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
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Videocam, contentDescription = "Video Call")
            }
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Call, contentDescription = "Call")
            }
        }
    )
}

@Composable
fun MessageBubble(
    message: MessageDto,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val bubbleColor = if (message.isMe)
        MaterialTheme.colorScheme.primary
    else
        Color.White

    val textColor = if (message.isMe) Color.White else Color.Black

    val alignment = if (message.isMe)
        Alignment.CenterEnd else Alignment.CenterStart

    val bubbleShape = if (message.isMe) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { showMenu = true }
            ),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {

            // Reply preview
            message.replyTo?.let {
                Surface(
                    color = Color(0x11000000),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = it.message ?: "",
                        modifier = Modifier.padding(6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            Box {
                // Message bubble
                Surface(
                    color = bubbleColor,
                    shape = bubbleShape,
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                ) {
                    Text(
                        text = message.message ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Instagram-style reaction badge
                message.reaction?.takeIf { it.isNotEmpty() && it != "null" }?.let { reaction ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(50),
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp) // slightly overlapping bubble
                    ) {
                        Text(
                            text = reaction,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Long press menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Reply") },
                onClick = { onReply(); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("React 👍") },
                onClick = { onReact("👍"); showMenu = false }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { onDelete(); showMenu = false }
            )
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

            replyingTo?.let {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Replying to: ${it.message}",
                        maxLines = 1
                    )
                    TextButton(onClick = onCancelReply) {
                        Text("Cancel")
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
                    maxLines = 4
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