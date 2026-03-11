package com.example.chatiko.ui.vibes

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.chatiko.network.LocationHelper
import com.example.chatiko.network.RegistrationServices
import kotlin.math.cos
import kotlin.math.sin

// 7. Dummy data structure
data class NearbyVibeUser(
    val id: String?,
    val username: String,
    val mood: String?,
    val moodEmoji: String,
    val latitude: Double,
    val longitude: Double,
    val isOnline: Boolean,
)

@Composable
fun NearbyVibesScreen(
    navController: NavController,
    api: RegistrationServices?,
    userId: String?,
    initialMood: String?
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }

    val viewModel: NearbyVibesViewModel = viewModel(
        factory = NearbyVibesViewModelFactory(
            context,
            api,
            userId,
            locationHelper,
            initialMood
        )
    )

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d("NearbyScreen", "Permission granted, starting updates")
            viewModel.startNearbyUpdates()
        } else {
            Log.e("NearbyScreen", "Permission denied")
        }
    }

    // Check permission once when Composable enters composition
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Already granted, start updates immediately
            viewModel.startNearbyUpdates()
        } else {
            // Ask for permission
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val selectedMood by viewModel.selectedMood.collectAsState()
    val filteredUsers by viewModel.filteredUsers.collectAsState()
    val moods = viewModel.moods

    // Scaffold + UI
    Scaffold(
        topBar = {
            NearbyVibesTopBar(
                selectedMood = selectedMood,
                nearbyCount = filteredUsers.size,
                moods = moods,
                viewModel = viewModel
            )
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
                    myLat = viewModel.myLat,
                    myLng = viewModel.myLng,
                    onUserClick = { user ->
                        navController.navigate("chatscreen/${user.id}")
                    }
                )
            }

            items(filteredUsers) { user ->
                UserVibeCard(
                    user = user,
                    myLat = viewModel.myLat,
                    myLng = viewModel.myLng,
                    onClick = {
                        navController.navigate("chatscreen/${user.id}")
                    }
                )
            }
        }
    }
}

@Composable
fun NearbyVibesTopBar(
    selectedMood: String,
    nearbyCount: Int,
    moods: List<String>,
    viewModel: NearbyVibesViewModel
) {
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
                text = "$nearbyCount people nearby",
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
                        .clip(CircleShape),
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
    myLat: Double,
    myLng: Double,
    onUserClick: (NearbyVibeUser) -> Unit
) {

    val radarRadius = 120f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.White, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            drawCircle(
                color = Color(0xFF6A82FB).copy(alpha = 0.2f),
                radius = size.minDimension / 2
            )
        }

        users.take(8).forEach { user ->

            val results = FloatArray(3)

            Location.distanceBetween(
                myLat,
                myLng,
                user.latitude,
                user.longitude,
                results
            )

            val distance = results[0]
            val bearing = results[1]

            val normalizedDistance =
                (distance / 1000f).coerceAtMost(1f)

            val radarDistance =
                normalizedDistance * radarRadius

            val x =
                cos(Math.toRadians(bearing.toDouble())) *
                        radarDistance

            val y =
                sin(Math.toRadians(bearing.toDouble())) *
                        radarDistance

            Box(
                modifier = Modifier
                    .offset(x.dp, y.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6A82FB))
                    .clickable { onUserClick(user) }
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
