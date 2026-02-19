package com.example.chatiko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen() {
    Box(
        Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.7f) // Takes up slightly more than half the screen
            ) {
                // Curved Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CurvedBottomShape())
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFD6F0FF), // Light cyan
                                    Color(0xFFEFE6FF), // Light purple
                                    Color(0xFFFEF3D2)  // Light yellow
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                )

                // Canvas Panda Illustration (No assets needed!)
                PandaIllustration(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        // Pushed slightly down so the bottom perfectly rests on the background edge
                        .padding(bottom = 16.dp)
                        .size(220.dp)
                )
            }

            // Bottom Section: Typography
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.9f)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Stay Anonymous.",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111111),
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Feel Connected.",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111111),
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "No profiles, no usernames,\nno pressure.",
                    fontSize = 18.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen()
}