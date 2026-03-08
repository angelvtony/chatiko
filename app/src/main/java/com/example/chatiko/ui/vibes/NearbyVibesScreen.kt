package com.example.chatiko.ui.vibes

import android.location.Location
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlin.math.cos
import kotlin.math.sin

// 7. Dummy data structure
data class NearbyVibeUser(
    val id: String,
    val username: String,
    val mood: String,
    val moodEmoji: String,
    val latitude: Double,
    val longitude: Double,
    val isOnline: Boolean,
    val color: Color
)

@Composable
fun NearbyVibesScreen(
    navController: NavController,
    viewModel: NearbyVibesViewModel = viewModel()
) {

    val selectedMood by viewModel.selectedMood.collectAsState()
    val filteredUsers by viewModel.filteredUsers.collectAsState()

    val moods = viewModel.moods

    val myLat = 1.3521
    val myLng = 103.8198

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {

                    Text(
                        text = "$selectedMood Vibe Nearby ${getEmoji(selectedMood)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "${filteredUsers.size} people nearby",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    items(moods) { mood ->

                        FilterChip(
                            label = mood,
                            isSelected = mood == selectedMood,
                            onClick = { viewModel.selectMood(mood) }
                        )

                    }
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                NearbyRadar(
                    users = filteredUsers,
                    onUserClick = { user ->
                        navController.navigate("chatscreen/${user.id}")
                    }
                )
            }

            items(filteredUsers) { user ->

                UserVibeCard(
                    user = user,
                    myLat = myLat,
                    myLng = myLng,
                    onClick = {
                        navController.navigate("chatscreen")
                    }
                )

            }

        }

    }
}

@Composable
fun UserVibeCard(user: NearbyVibeUser, myLat: Double, myLng: Double, onClick: () -> Unit) {
    // 5. Distance Calculation
    val distance = remember(user.latitude, user.longitude) {
        val results = FloatArray(1)
        Location.distanceBetween(myLat, myLng, user.latitude, user.longitude, results)
        results[0]
    }

    // 6. Distance Formatting
    val distanceText = remember(distance) {
        if (distance < 1000) {
            "${distance.toInt()} m away"
        } else {
            String.format("%.1f km away", distance / 1000f)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Online Indicator
            Box {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(user.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
                
                // 3. Online Indicator
                if (user.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color.White, CircleShape)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = user.moodEmoji, fontSize = 16.sp)
                }
                
                Text(
                    text = distanceText,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Chat Button
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A82FB).copy(alpha = 0.1f),
                    contentColor = Color(0xFF6A82FB)
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                elevation = null
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val bgColor by animateColorAsState(
        if (isSelected) Color(0xFF6A82FB) else Color(0xFFF1F3FD),
        label = ""
    )

    val textColor by animateColorAsState(
        if (isSelected) Color.White else Color(0xFF6A82FB),
        label = ""
    )

    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(40.dp)
    ) {

        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "${getEmoji(label)} $label",
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun NearbyRadar(
    users: List<NearbyVibeUser>,
    onUserClick: (NearbyVibeUser) -> Unit
) {

    val infiniteTransition = rememberInfiniteTransition()

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ), label = ""
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ), label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.White, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2

            drawCircle(
                color = Color(0xFF6A82FB).copy(alpha = 0.2f),
                radius = radius
            )

            drawCircle(
                color = Color(0xFF6A82FB).copy(alpha = 0.1f),
                radius = radius * pulse
            )

            rotate(rotation) {
                drawLine(
                    color = Color(0xFF6A82FB),
                    start = center,
                    end = Offset(center.x, 0f),
                    strokeWidth = 6f
                )
            }
        }

        // Center user
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(Color(0xFF6A82FB), CircleShape)
        )

        // Nearby user dots
        users.take(6).forEachIndexed { index, user ->

            // Simple angle-distance placement
            val angle = (index * 60).toFloat() // degrees
            val distance = 60 + (index * 15)   // px offset

            val x = cos(Math.toRadians(angle.toDouble())) * distance
            val y = sin(Math.toRadians(angle.toDouble())) * distance

            Box(
                modifier = Modifier
                    .offset(x.dp, y.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(user.color)
                    .border(1.dp, Color.White, CircleShape)
                    .clickable { onUserClick(user) } // <-- tap callback
            )
        }
    }
}

fun getEmoji(mood: String): String {
    return when (mood) {
        "Coffee" -> "☕"
        "Study" -> "📚"
        "Gaming" -> "🎮"
        "Chill" -> "🏖️"
        else -> "✨"
    }
}
