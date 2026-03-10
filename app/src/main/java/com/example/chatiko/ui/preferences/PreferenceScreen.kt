package com.example.chatiko.ui.preferences

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

data class Mood(
    val name: String,
    val icon: ImageVector,
    val colors: List<Color>
)

@Composable
fun PreferenceScreen(navController: NavController, viewModel: PreferencesViewModel = viewModel()) {
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    val moods = listOf(
        Mood(
            "Happy",
            Icons.Default.SentimentVerySatisfied,
            listOf(Color(0xBEFAE196), Color(0xFFFFC107))
        ),
        Mood(
            "Sad",
            Icons.Default.SentimentDissatisfied,
            listOf(Color(0xFF81B7F3), Color(0xFF1565C0))
        ),
        Mood(
            "Stressed",
            Icons.Default.Warning,
            listOf(Color(0xFFECC9C9), Color(0xFFD30909))
        ),
        Mood(
            "Vibing",
            Icons.Default.MusicNote,
            listOf(Color(0xFFC57DCE), Color(0xFF750D85))
        ),
        Mood(
            "Bored",
            Icons.Default.HourglassEmpty,
            listOf(Color(0xFFB0BEC5), Color(0xFF78909C))
        ),
        Mood(
            "Working",
            Icons.Default.Work,
            listOf(Color(0xFF2A0707), Color(0xFF807676))
        ),
        Mood(
            "Idle",
            Icons.Default.Hotel,
            listOf(Color(0xFF80DEEA), Color(0xFF26C6DA))
        ),
        Mood(
            "Normal",
            Icons.Default.SentimentNeutral,
            listOf(Color(0xFF81C784), Color(0xFF4DB6AC))
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "What's your mood right now?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            modifier = Modifier.weight(1f),
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(moods) { mood ->
                MoodItem(
                    mood = mood,
                    isSelected = mood == selectedMood,
                    onClick = { selectedMood = mood }
                )
            }
        }
        val context = LocalContext.current
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val jwtToken = sharedPref.getString("jwt_token", null) // default null if not stored
        Button(
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent),
            onClick = {
                selectedMood?.let { mood ->
                    val preference = PreferenceModel(mood.name)
                    viewModel.savePreferences(jwtToken, preference)
                    navController.navigate("nearby_vibes/${mood.name}")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)

                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00C6FF), Color(0xFF6A82FB))
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(
                text = "Confirm mood",
                color = Color.White
            )
        }

    }
}

@Composable
fun MoodItem(
    mood: Mood,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        label = ""
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isSelected) 20.dp else 0.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isSelected) mood.colors.last() else Color.Transparent,
                spotColor = if (isSelected) mood.colors.last() else Color.Transparent
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(mood.colors)
            )
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) mood.colors.last() else Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = mood.icon,
                contentDescription = mood.name,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = mood.name,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

//@Preview(showBackground = true)
//@Composable
//fun PreviewPreferenceScreen() {
//    PreferenceScreen(null)
//}