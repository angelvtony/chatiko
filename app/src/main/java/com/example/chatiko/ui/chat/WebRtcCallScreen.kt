package com.example.chatiko.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import com.example.chatiko.ui.chat.viewmodel.ChatViewModel

@Composable
fun WebRtcCallScreen(
    viewModel: ChatViewModel,
    callType: String,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var isMicMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(callType == "video") }

    val remoteVideoTrack by viewModel.remoteVideoTrack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (callType == "video") {
            // Remote Video View (Full Screen)
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        viewModel.webRtcManager?.initRemoteSurfaceView(this)
                        // Note: Track binding happens when track is received
                    }
                },
                update = { view ->
                    remoteVideoTrack?.addSink(view)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Local Video View (Picture-in-Picture mode)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            viewModel.webRtcManager?.initLocalSurfaceView(this)
                            viewModel.webRtcManager?.startLocalVideo(this, true)
                        }
                    },
                    modifier = Modifier
                        .size(120.dp, 160.dp)
                        .background(Color.Gray)
                )
            }
        } else {
            // Audio-only mode: Start audio but don't show SurfaceView
            LaunchedEffect(Unit) {
                // Initialize local streams internally if possible,
                // but we might need dummy view or just context.
                // Our manager starts video only if `isVideoCall` is true.
                viewModel.webRtcManager?.startLocalVideo(SurfaceViewRenderer(context), false)
            }
        }

        // Call Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { 
                    isSpeakerOn = !isSpeakerOn
                    viewModel.webRtcManager?.toggleSpeaker(isSpeakerOn)
                },
                containerColor = if (isSpeakerOn) Color.White else Color.Gray,
                contentColor = if (isSpeakerOn) Color.Black else Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Speaker"
                )
            }

            if (callType == "video") {
                FloatingActionButton(
                    onClick = { viewModel.webRtcManager?.switchCamera() },
                    containerColor = Color.Gray,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Flip Camera"
                    )
                }
            }

            FloatingActionButton(
                onClick = { 
                    isMicMuted = !isMicMuted
                    viewModel.webRtcManager?.toggleMute(isMicMuted)
                },
                containerColor = if (isMicMuted) Color.White else Color.Gray,
                contentColor = if (isMicMuted) Color.Black else Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Mute"
                )
            }

            FloatingActionButton(
                onClick = {
                    viewModel.endCall()
                    onEndCall()
                },
                containerColor = Color.Red,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
