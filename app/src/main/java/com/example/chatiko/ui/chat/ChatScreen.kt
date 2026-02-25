package com.example.chatiko.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chatiko.ui.chat.viewmodel.ChatViewModel


@Composable
fun ChatScreen(
    navController: NavController?,
    viewModel: ChatViewModel = viewModel()
) {

    val messages = viewModel.messages
    val replyingTo by viewModel.replyingTo

    Scaffold(
        topBar = { ChatTopBar() },

        bottomBar = {
            MessageInputBar(
                replyingTo = replyingTo,
                onCancelReply = { viewModel.clearReply() },
                onSend = { text ->
                    viewModel.sendMessage(text)
                }
            )
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            reverseLayout = true
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
                        onDelete = { viewModel.deleteMessage(message) },
                        onReact = { emoji ->
                            viewModel.addReaction(message, emoji)
                        },
                        onReply = {
                            viewModel.setReply(message)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar() {
    TopAppBar(
        modifier = Modifier.shadow(elevation = 8.dp),
        title = {
            Column {
                Text(
                    text = "Arathi",
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
    message: Message,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onReply: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val bubbleColor = if (message.isMe)
        MaterialTheme.colorScheme.primary
    else
        Color.White

    val textColor = if (message.isMe)
        Color.White
    else
        Color.Black

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
            horizontalAlignment = if (message.isMe)
                Alignment.End else Alignment.Start,
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
                        text = it.text,
                        modifier = Modifier.padding(6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            Surface(
                color = bubbleColor,
                shape = bubbleShape,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Reaction badge
            message.reaction?.let {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .padding(top = 4.dp)
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

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
    replyingTo: Message?,
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
                        text = "Replying to: ${it.text}",
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

@Preview(showBackground = true)
@Composable
fun PreviewHomeScreen() {
    ChatScreen(null)
}