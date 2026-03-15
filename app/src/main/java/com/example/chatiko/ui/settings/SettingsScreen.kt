package com.example.chatiko.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable

fun SettingsScreen(navController: NavController?,) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Left
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Left Icon
            Icon(
                imageVector = Icons.Default.PersonOutline,
                contentDescription = "Nickname Icon",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Text(
                text = "Change Nickname",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f) // pushes arrow to end
            )

            // Right Arrow
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Go",
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }

        Divider(
            color = Color.LightGray,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Left Icon
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = "Nickname Icon",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Text(
                text = "Radius selection",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f) // pushes arrow to end
            )

            // Right Arrow
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Go",
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }
        RadiusSelector()

        Divider(
            color = Color.LightGray,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Left Icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Nickname Icon",
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Text(
                text = "Privacy Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f) // pushes arrow to end
            )

            // Right Arrow
            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Go",
                modifier = Modifier.size(24.dp),
                tint = Color.Gray
            )
        }
        Divider(
            color = Color.LightGray,
            thickness = 1.dp
        )

        val context = LocalContext.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {

                    val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

                    // Clear saved session
                    sharedPref.edit().clear().apply()

                    // Navigate to login screen
                    navController?.navigate("registration") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Left Icon
            Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Logout Icon",
                modifier = Modifier.size(24.dp),
                tint = Color.Red
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text
            Text(
                text = "Logout",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
        }

    }
}

@Composable
fun RadiusSelector() {

    val options = listOf("1 km", "3 km", "5 km")
    var selectedOption by remember { mutableStateOf("3 km") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = Color(0xFFEDEDED),
                shape = RoundedCornerShape(50) // Big oval container
            )
            .padding(6.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            options.forEach { option ->

                val isSelected = option == selectedOption

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .background(
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(50) // Small oval selection
                        )
                        .clickable { selectedOption = option }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        color = if (isSelected) Color.Black else Color.Gray,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}