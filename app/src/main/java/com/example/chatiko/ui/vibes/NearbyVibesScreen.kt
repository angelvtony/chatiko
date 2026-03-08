package com.example.chatiko.ui.vibes

import android.location.Location
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

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
fun NearbyVibesScreen(navController: NavController) {
    // Current User Location (Mocked for calculation)
    val myLat = 1.3521
    val myLng = 103.8198

    // State for Filter
    var selectedMood by remember { mutableStateOf("Coffee") }
    val moods = listOf("Coffee", "Study", "Gaming", "Chill")

    // 10. Sample dummy data for testing
    val allUsers = remember {
        listOf(
            NearbyVibeUser("1", "User_7af", "Coffee", "☕", 1.3530, 103.8205, true, Color(0xFF8B4513)),
            NearbyVibeUser("2", "User_847", "Coffee", "☕", 1.3510, 103.8150, true, Color(0xFF6F4E37)),
            NearbyVibeUser("3", "User_192", "Study", "📚", 1.3580, 103.8300, false, Color(0xFF4682B4)),
            NearbyVibeUser("4", "User_k21", "Gaming", "🎮", 1.3450, 103.8100, true, Color(0xFF32CD32)),
            NearbyVibeUser("5", "User_m90", "Chill", "🏖️", 1.3600, 103.8250, true, Color(0xFF40E0D0)),
            NearbyVibeUser("6", "User_v04", "Coffee", "☕", 1.3550, 103.8220, false, Color(0xFFD2691E)),
            NearbyVibeUser("7", "User_x55", "Study", "📚", 1.3480, 103.8180, true, Color(0xFF4169E1)),
            NearbyVibeUser("8", "User_p88", "Gaming", "🎮", 1.3505, 103.8210, true, Color(0xFF228B22)),
            NearbyVibeUser("9", "User_q12", "Chill", "🧘", 1.3540, 103.8190, false, Color(0xFF87CEEB)),
            NearbyVibeUser("10", "User_z33", "Coffee", "☕", 1.3620, 103.8400, true, Color(0xFF5D4037))
        )
    }

    // Filter and Sort Logic (Sorting nearest first)
    val filteredUsers = allUsers.filter { it.mood == selectedMood }
        .sortedBy { user ->
            val results = FloatArray(1)
            Location.distanceBetween(myLat, myLng, user.latitude, user.longitude, results)
            results[0]
        }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                // 1. Top Header
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "$selectedMood Vibe Nearby ${getEmoji(selectedMood)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${filteredUsers.size} people nearby",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 10. Filter Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(moods) { mood ->
                        FilterChip(
                            label = mood,
                            isSelected = mood == selectedMood,
                            onClick = { selectedMood = mood }
                        )
                    }
                }
            }
        }
    ) { padding ->
        // 2. Main List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(filteredUsers) { user ->
                // 4. User Card Design
                UserVibeCard(
                    user = user,
                    myLat = myLat,
                    myLng = myLng,
                    onClick = {
                        // 8. Clicking a user
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
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF6A82FB) else Color(0xFFF1F3FD),
        modifier = Modifier.height(40.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${getEmoji(label)} $label",
                color = if (isSelected) Color.White else Color(0xFF6A82FB),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
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
