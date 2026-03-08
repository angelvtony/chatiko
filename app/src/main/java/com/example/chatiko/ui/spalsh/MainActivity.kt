package com.example.chatiko.ui.spalsh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import com.example.chatiko.ui.theme.ChatikoTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatiko.ui.LoginScreen
import com.example.chatiko.ui.chat.ChatScreen
import com.example.chatiko.ui.preferences.PreferenceScreen
import com.example.chatiko.ui.settings.SettingsScreen
import com.example.chatiko.ui.tutorial.HomeScreen
import com.example.chatiko.ui.vibes.NearbyVibesScreen
import kotlinx.coroutines.delay


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatikoTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    composable("splash") {
                        SplashScreen(navController)
                    }

                    composable("home") {
                        HomeScreen(navController)
                    }

                    composable("login") {
                        LoginScreen(navController)
                    }

                    composable("preference") {
                        PreferenceScreen(navController)
                    }

                    composable(
                        route = "chatscreen/{userId}",
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userId = backStackEntry.arguments?.getString("userId")
                        ChatScreen(navController, userId ?: "")
                    }

                    composable("nearby_vibes") {
                        NearbyVibesScreen(navController)
                    }
                }
            }
        }

    }
}
@Composable
fun SplashScreen(navController: NavController) {

    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    val transition = rememberInfiniteTransition()

    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2000, easing = LinearEasing))
    )

    val translationY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    )

    val iconColor by transition.animateColor(
        initialValue = Color(0xFFFF0000),
        targetValue = Color(0xFFFF0000),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = FastOutSlowInEasing))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF00C6FF),
                        Color(0xFFEAC89B),
                    ),
                    start = Offset(0f, 0f),
                )
            )
            .padding(24.dp)
    ) {

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(20.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .scale(scale)
                    .graphicsLayer(
                        rotationZ = rotation,
                        translationY = translationY
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Logo",
                    tint = iconColor,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "NearYou",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Text(
            text = "Anonymous Nearby Mood Social Media",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}
